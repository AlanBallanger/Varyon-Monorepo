package fr.varyon.bubble;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.EntityPart;
import com.hypixel.hytale.protocol.ModelParticle;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.protocol.packets.interface_.SetPage;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.protocol.packets.window.CloseWindow;
import com.hypixel.hytale.protocol.packets.world.SetPaused;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapVisible;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSpawner;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class BubbleManager {
   private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
   private static final double BUBBLE_Y_OFFSET = 2.8;
   private static final double BUBBLE_X_OFFSET = 0.0;
   private static final String INTRO_SMALL_SYSTEM = "Thinking_Intro_Small";
   private static final String INTRO_MEDIUM_SYSTEM = "Thinking_Intro_Medium";
   private static final long INTRO_MEDIUM_DELAY_MS = 167L;
   private static final long INTRO_MAIN_DELAY_MS = 445L;
   private static final long FIRST_SUSTAIN_DELAY_MS = 3500L;
   private static final long SUSTAIN_INTERVAL_MS = 4500L;
   private static final long STATIC_FIRST_SUSTAIN_DELAY_MS = 1800L;
   private static final long STATIC_SUSTAIN_INTERVAL_MS = 1200L;
   private static final double MOVEMENT_THRESHOLD_SQ = 0.25;
   private static final long IDLE_TIMEOUT_MS = 300000L;
   private static final String SLEEPY_PARTICLE = "Sleepy";
   private static final double SLEEPY_Y_OFFSET = 3.8;
   private static final long SPAWN_COOLDOWN_MS = 4000L;
   private static final long STATIC_SPAWN_COOLDOWN_MS = 1000L;

   private volatile double viewRange = 40.0;
   private volatile double viewRangeSq = 1600.0;
   private final Map<UUID, BubbleState> playerStates = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> playerNetworkIds = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lastSpawnTimes = new ConcurrentHashMap<>();
   private final Map<UUID, ScheduledFuture<?>> pendingRespawns = new ConcurrentHashMap<>();
   private final Map<UUID, Vector3d> lastPositions = new ConcurrentHashMap<>();
   private final Map<UUID, Long> introGeneration = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lastClearTimes = new ConcurrentHashMap<>();
   private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();
   private final Set<UUID> animDisabledPlayers = ConcurrentHashMap.newKeySet();
   private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Long> lastActivityTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Color> playerColors = new ConcurrentHashMap<>();
   private final Map<UUID, String> customPrefixes = new ConcurrentHashMap<>();
   private final Map<UUID, BubbleSpawnerFactory.ColorRegistration> customRegistrations = new ConcurrentHashMap<>();
   private ScheduledExecutorService scheduler;
   private volatile boolean diagnosticDone = false;
   private static Method bubbleChatHasActiveMethod;
   private static boolean bubbleChatChecked = false;

   public void setScheduler(ScheduledExecutorService scheduler) {
      this.scheduler = scheduler;
   }

   public void registerPacketWatchers() {
      PacketAdapters.registerOutbound((PlayerPacketWatcher)(playerRef, packet) -> {
         int id = packet.getId();
         if (id == SetPage.PACKET_ID) {
            SetPage setPage = (SetPage)packet;
            this.onPageChange(playerRef, setPage.page);
         } else if (id == CustomPage.PACKET_ID) {
            this.onCustomPageOpen(playerRef);
         }
      });
      ((Api)LOGGER.atInfo()).log("Registered outbound packet watchers (SetPage + CustomPage)");
      PacketAdapters.registerInbound((PlayerPacketWatcher)(playerRef, packet) -> {
         int id = packet.getId();
         if (id != Ping.PACKET_ID) {
            this.recordActivity(playerRef.getUuid());
         }

         if (id == SetPaused.PACKET_ID) {
            SetPaused setPaused = (SetPaused)packet;
            this.onPauseToggle(playerRef, setPaused.paused);
         } else if (id == UpdateWorldMapVisible.PACKET_ID) {
            UpdateWorldMapVisible updateWorldMapVisible = (UpdateWorldMapVisible)packet;
            this.onMapToggle(playerRef, updateWorldMapVisible.visible);
         } else if (id == ClientOpenWindow.PACKET_ID) {
            this.onClientOpenWindow(playerRef);
         } else if (id == CloseWindow.PACKET_ID) {
            this.onCloseWindow(playerRef);
         }
      });
      ((Api)LOGGER.atInfo()).log("Registered inbound packet watchers (SetPaused + ClientOpenWindow + CloseWindow)");
   }

   private void onPageChange(@Nonnull PlayerRef playerRef, @Nonnull Page page) {
      UUID uuid = playerRef.getUuid();
      if (this.disabledPlayers.contains(uuid)) {
         return;
      }

      BubbleState state;
      switch (page) {
         case Inventory:
            state = BubbleState.INVENTORY;
            break;
         case Bench:
            state = BubbleState.CONTAINER;
            break;
         case Map:
            state = BubbleState.MAP;
            break;
         case ToolsSettings:
         case ContentCreation:
         case MachinimaEditor:
            state = BubbleState.MENU;
            break;
         case None:
            BubbleState current = this.playerStates.get(uuid);
            if (current != null && current != BubbleState.PAUSED) {
               this.fadeoutAndClear(uuid);
            }

            return;
         default:
            return;
      }

      ((Api)LOGGER.atFine()).log("Page change: %s -> %s for %s", page, state, uuid);
      this.spawnIntroSequence(playerRef, uuid, state);
   }

   private void onCustomPageOpen(@Nonnull PlayerRef playerRef) {
      UUID uuid = playerRef.getUuid();
      if (!this.disabledPlayers.contains(uuid)) {
         ((Api)LOGGER.atFine()).log("Custom page opened for %s -> MENU", uuid);
         this.spawnIntroSequence(playerRef, uuid, BubbleState.MENU);
      }
   }

   private void onClientOpenWindow(@Nonnull PlayerRef playerRef) {
      UUID uuid = playerRef.getUuid();
      if (!this.disabledPlayers.contains(uuid)) {
         BubbleState state = this.playerStates.get(uuid);
         if (state == null) {
            ((Api)LOGGER.atFine()).log("ClientOpenWindow for %s -> INVENTORY", uuid);
            this.spawnIntroSequence(playerRef, uuid, BubbleState.INVENTORY);
         }
      }
   }

   private void onCloseWindow(@Nonnull PlayerRef playerRef) {
      UUID uuid = playerRef.getUuid();
      if (!this.disabledPlayers.contains(uuid)) {
         BubbleState state = this.playerStates.get(uuid);
         if (state == BubbleState.INVENTORY) {
            this.fadeoutAndClear(uuid);
         }
      }
   }

   private void onPauseToggle(@Nonnull PlayerRef playerRef, boolean paused) {
      UUID uuid = playerRef.getUuid();
      if (!this.disabledPlayers.contains(uuid)) {
         ((Api)LOGGER.atFine()).log("Pause toggle: paused=%s for %s (current=%s)", paused, uuid, this.playerStates.get(uuid));
         if (paused) {
            this.spawnIntroSequence(playerRef, uuid, BubbleState.PAUSED);
         } else {
            BubbleState state = this.playerStates.get(uuid);
            if (state == BubbleState.PAUSED) {
               this.fadeoutAndClear(uuid);
            }
         }
      }
   }

   private void onMapToggle(@Nonnull PlayerRef playerRef, boolean visible) {
      UUID uuid = playerRef.getUuid();
      if (!this.disabledPlayers.contains(uuid)) {
         ((Api)LOGGER.atFine()).log("Map toggle: visible=%s for %s", visible, uuid);
         if (visible) {
            this.spawnIntroSequence(playerRef, uuid, BubbleState.MAP);
         } else {
            BubbleState state = this.playerStates.get(uuid);
            if (state == BubbleState.MAP) {
               this.fadeoutAndClear(uuid);
            }
         }
      }
   }

   private void recordActivity(@Nonnull UUID uuid) {
      this.lastActivityTimes.put(uuid, System.currentTimeMillis());
      if (this.playerStates.get(uuid) == BubbleState.IDLE) {
         this.fadeoutAndClear(uuid);
         this.lastClearTimes.remove(uuid);
      }
   }

   private void spawnIntroSequence(@Nonnull PlayerRef playerRef, @Nonnull UUID uuid, @Nonnull BubbleState state) {
      if (hasBubbleChatActive(uuid)) {
         return;
      }

      BubbleState current = this.playerStates.get(uuid);
      if (current == state) {
         ((Api)LOGGER.atFine()).log("Duplicate bubble prevention: already in state %s for %s", state, uuid);
         return;
      }

      Long lastClear = this.lastClearTimes.get(uuid);
      long cooldown = this.animDisabledPlayers.contains(uuid) ? STATIC_SPAWN_COOLDOWN_MS : SPAWN_COOLDOWN_MS;
      if (lastClear != null && System.currentTimeMillis() - lastClear < cooldown) {
         long remaining = cooldown - (System.currentTimeMillis() - lastClear);
         ((Api)LOGGER.atFine()).log("Cooldown active for %s (%.1fs remaining)", uuid, remaining / 1000.0);
         return;
      }

      if (current != null) {
         this.clearPlayerState(uuid);
      }

      this.playerStates.put(uuid, state);
      this.lastClearTimes.remove(uuid);
      long generation = this.introGeneration.merge(uuid, 1L, Long::sum);
      ScheduledFuture<?> pending = this.pendingRespawns.remove(uuid);
      if (pending != null) {
         pending.cancel(false);
      }

      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
         return;
      }

      Store<EntityStore> store = ref.getStore();
      World world = ((EntityStore)store.getExternalData()).getWorld();
      world.execute(() -> {
         if (this.introGeneration.getOrDefault(uuid, 0L) != generation || !ref.isValid()) {
            return;
         }

         DeathComponent death = (DeathComponent)store.getComponent(ref, DeathComponent.getComponentType());
         if (death != null) {
            return;
         }

         TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
         if (transform == null) {
            return;
         }

         NetworkId networkId = (NetworkId)store.getComponent(ref, NetworkId.getComponentType());
         if (networkId == null) {
            return;
         }

         int netId = networkId.getId();
         this.playerNetworkIds.put(uuid, netId);
         this.ensureColorRegistered(uuid, netId);
         Vector3d position = transform.getPosition();
         this.lastPositions.put(uuid, new Vector3d(position));
         String introSmall = this.resolveSystem(uuid, INTRO_SMALL_SYSTEM);
         this.sendModelParticle(netId, introSmall, (float)BUBBLE_Y_OFFSET, uuid);

         CompletableFuture.runAsync(() -> world.execute(() -> {
            if (this.introGeneration.getOrDefault(uuid, 0L) == generation && ref.isValid()) {
               String introMedium = this.resolveSystem(uuid, INTRO_MEDIUM_SYSTEM);
               this.sendModelParticle(netId, introMedium, (float)BUBBLE_Y_OFFSET, uuid);
            }
         }), CompletableFuture.delayedExecutor(INTRO_MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS));

         boolean staticMode = this.animDisabledPlayers.contains(uuid);
         CompletableFuture.runAsync(() -> world.execute(() -> {
            if (this.introGeneration.getOrDefault(uuid, 0L) == generation && ref.isValid()) {
               String systemName = staticMode ? state.getLoopFrameSystem(3) : state.getParticleSystem();
               String resolved = this.resolveSystem(uuid, systemName);
               long nextDelay = staticMode ? STATIC_FIRST_SUSTAIN_DELAY_MS : FIRST_SUSTAIN_DELAY_MS;
               this.sendModelParticle(netId, resolved, (float)BUBBLE_Y_OFFSET, uuid);
               if (state == BubbleState.IDLE) {
                  this.sendModelParticle(netId, SLEEPY_PARTICLE, (float)SLEEPY_Y_OFFSET, uuid);
               }

               this.lastSpawnTimes.put(uuid, System.currentTimeMillis());
               this.scheduleNextLoop(uuid, nextDelay);
            }
         }), CompletableFuture.delayedExecutor(INTRO_MAIN_DELAY_MS, TimeUnit.MILLISECONDS));
      });
   }

   public void pollAndSpawn() {
      if (!this.diagnosticDone) {
         this.diagnosticDone = true;
         this.runDiagnostic();
      }

      for (PlayerRef playerRef : Universe.get().getPlayers()) {
         UUID uuid = playerRef.getUuid();
         if (!this.disabledPlayers.contains(uuid) && !this.playerStates.containsKey(uuid)) {
            Long lastActivity = this.lastActivityTimes.get(uuid);
            if (lastActivity == null) {
               this.lastActivityTimes.put(uuid, System.currentTimeMillis());
            } else if (System.currentTimeMillis() - lastActivity >= IDLE_TIMEOUT_MS) {
               ((Api)LOGGER.atInfo()).log("Player %s idle for 5+ minutes, spawning IDLE bubble", uuid);
               this.spawnIntroSequence(playerRef, uuid, BubbleState.IDLE);
            }
         }
      }

      if (this.playerStates.isEmpty()) {
         return;
      }

      for (Entry<UUID, BubbleState> entry : this.playerStates.entrySet()) {
         UUID uuid = entry.getKey();
         PlayerRef playerRef = this.findPlayerRef(uuid);
         if (playerRef == null) {
            this.clearPlayerState(uuid);
            continue;
         }

         Ref<EntityStore> ref = playerRef.getReference();
         if (ref == null || !ref.isValid()) {
            this.clearPlayerState(uuid);
            continue;
         }

         Store<EntityStore> store = ref.getStore();
         World world = ((EntityStore)store.getExternalData()).getWorld();
         world.execute(() -> {
            if (!ref.isValid()) {
               return;
            }

            BubbleState state = this.playerStates.get(uuid);
            if (state == null) {
               return;
            }

            DeathComponent death = (DeathComponent)store.getComponent(ref, DeathComponent.getComponentType());
            if (death != null) {
               this.clearPlayerState(uuid);
               return;
            }

            TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
               return;
            }

            Vector3d position = transform.getPosition();
            Vector3d last = this.lastPositions.get(uuid);
            if (last != null) {
               double dx = position.x() - last.x();
               double dz = position.z() - last.z();
               if (dx * dx + dz * dz > MOVEMENT_THRESHOLD_SQ) {
                  this.fadeoutAndClear(uuid);
               }
            }
         });
      }
   }

   private void runDiagnostic() {
      try {
         DefaultAssetMap<String, ParticleSystem> systemMap = ParticleSystem.getAssetMap();
         DefaultAssetMap<String, ParticleSpawner> spawnerMap = ParticleSpawner.getAssetMap();
         ((Api)LOGGER.atFine()).log("=== Particle asset diagnostic ===");

         for (String name : new String[]{INTRO_SMALL_SYSTEM, INTRO_MEDIUM_SYSTEM}) {
            Object asset = systemMap.getAsset(name);
            ((Api)LOGGER.atFine()).log("  System '%s' -> %s", name, asset != null ? "FOUND" : "NOT FOUND");
         }

         for (BubbleState state : BubbleState.values()) {
            Object main = systemMap.getAsset(state.getParticleSystem());
            ((Api)LOGGER.atFine()).log("  System '%s' -> %s", state.getParticleSystem(), main != null ? "FOUND" : "NOT FOUND");
            Object loop = systemMap.getAsset(state.getLoopParticleSystem());
            ((Api)LOGGER.atFine()).log("  System '%s' -> %s", state.getLoopParticleSystem(), loop != null ? "FOUND" : "NOT FOUND");
            Object fadeout = systemMap.getAsset(state.getFadeoutParticleSystem());
            ((Api)LOGGER.atFine()).log("  System '%s' -> %s", state.getFadeoutParticleSystem(), fadeout != null ? "FOUND" : "NOT FOUND");
         }

         String[] spawnerNames = new String[]{
            "ThoughtCloud_Intro_Small",
            "ThoughtCloud_Intro_Medium",
            "ThoughtCloud_Backdrop",
            "ThoughtCloud_Ellipsis_Inventory",
            "ThoughtCloud_Ellipsis_Container",
            "ThoughtCloud_Ellipsis_Paused",
            "ThoughtCloud_Ellipsis_Map",
            "ThoughtCloud_Ellipsis_Menu",
            "ThoughtCloud_Ellipsis_Base",
            "ThoughtCloud_Ellipsis_Chat",
            "ThoughtCloud_Ellipsis_Inventory_Loop",
            "ThoughtCloud_Ellipsis_Container_Loop",
            "ThoughtCloud_Ellipsis_Paused_Loop",
            "ThoughtCloud_Ellipsis_Map_Loop",
            "ThoughtCloud_Ellipsis_Menu_Loop",
            "ThoughtCloud_Ellipsis_Base_Loop",
            "ThoughtCloud_Ellipsis_Inventory_Fadeout",
            "ThoughtCloud_Ellipsis_Container_Fadeout",
            "ThoughtCloud_Ellipsis_Paused_Fadeout",
            "ThoughtCloud_Ellipsis_Map_Fadeout",
            "ThoughtCloud_Ellipsis_Menu_Fadeout",
            "ThoughtCloud_Ellipsis_Base_Fadeout"
         };

         for (String name : spawnerNames) {
            Object asset = spawnerMap.getAsset(name);
            ((Api)LOGGER.atFine()).log("  Spawner '%s' -> %s", name, asset != null ? "FOUND" : "NOT FOUND");
         }
      } catch (Exception e) {
         ((Api)LOGGER.atWarning()).log("Failed to check particle assets: %s", e.getMessage());
      }
   }

   private void scheduleNextLoop(UUID uuid, long delayMs) {
      if (this.scheduler == null) {
         return;
      }

      ScheduledFuture<?> future = this.scheduler.schedule(() -> {
         BubbleState state = this.playerStates.get(uuid);
         if (state == null) {
            return;
         }

         Integer netId = this.playerNetworkIds.get(uuid);
         if (netId == null) {
            return;
         }

         PlayerRef playerRef = this.findPlayerRef(uuid);
         if (playerRef == null) {
            return;
         }

         Ref<EntityStore> ref = playerRef.getReference();
         if (ref == null || !ref.isValid()) {
            return;
         }

         Store<EntityStore> store = ref.getStore();
         World world = ((EntityStore)store.getExternalData()).getWorld();
         world.execute(() -> {
            if (!ref.isValid()) {
               return;
            }

            BubbleState currentState = this.playerStates.get(uuid);
            if (currentState == null) {
               return;
            }

            if (hasBubbleChatActive(uuid)) {
               this.fadeoutAndClear(uuid);
               return;
            }

            DeathComponent death = (DeathComponent)store.getComponent(ref, DeathComponent.getComponentType());
            if (death != null) {
               this.clearPlayerState(uuid);
               return;
            }

            boolean staticMode = this.animDisabledPlayers.contains(uuid);
            String systemName = staticMode ? currentState.getLoopFrameSystem(3) : currentState.getLoopParticleSystem();
            String resolved = this.resolveSystem(uuid, systemName);
            long nextDelay = staticMode ? STATIC_SUSTAIN_INTERVAL_MS : SUSTAIN_INTERVAL_MS;
            this.sendModelParticle(netId, resolved, (float)BUBBLE_Y_OFFSET, uuid);
            if (currentState == BubbleState.IDLE) {
               this.sendModelParticle(netId, SLEEPY_PARTICLE, (float)SLEEPY_Y_OFFSET, uuid);
            }

            this.lastSpawnTimes.put(uuid, System.currentTimeMillis());
            this.scheduleNextLoop(uuid, nextDelay);
         });
      }, delayMs, TimeUnit.MILLISECONDS);
      ScheduledFuture<?> previous = this.pendingRespawns.put(uuid, future);
      if (previous != null) {
         previous.cancel(false);
      }
   }

   private void fadeoutAndClear(@Nonnull UUID uuid) {
      BubbleState state = this.playerStates.get(uuid);
      Integer netId = this.playerNetworkIds.get(uuid);
      if (state != null && netId != null) {
         String resolved = this.resolveSystem(uuid, state.getFadeoutParticleSystem());
         this.sendModelParticle(netId, resolved, (float)BUBBLE_Y_OFFSET, uuid);
      }

      this.clearPlayerState(uuid);
   }

   private void clearPlayerState(@Nonnull UUID uuid) {
      this.playerStates.remove(uuid);
      this.playerNetworkIds.remove(uuid);
      this.lastSpawnTimes.remove(uuid);
      this.lastPositions.remove(uuid);
      this.lastClearTimes.put(uuid, System.currentTimeMillis());
      ScheduledFuture<?> future = this.pendingRespawns.remove(uuid);
      if (future != null) {
         future.cancel(false);
      }
   }

   private PlayerRef findPlayerRef(UUID uuid) {
      for (PlayerRef playerRef : Universe.get().getPlayers()) {
         if (playerRef.getUuid().equals(uuid)) {
            return playerRef;
         }
      }

      return null;
   }

   private List<Ref<EntityStore>> getViewerRefs(@Nonnull UUID ownerUuid) {
      Vector3d ownerPos = this.lastPositions.get(ownerUuid);
      List<Ref<EntityStore>> result = new ArrayList<>();
      boolean debug = this.debugPlayers.contains(ownerUuid);
      PlayerRef owner = this.findPlayerRef(ownerUuid);
      UUID worldUuid = owner != null ? owner.getWorldUuid() : null;

      for (PlayerRef viewer : Universe.get().getPlayers()) {
         if (!viewer.getUuid().equals(ownerUuid) || debug) {
            Ref<EntityStore> ref = viewer.getReference();
            if (ref != null && ref.isValid() && (worldUuid == null || worldUuid.equals(viewer.getWorldUuid()))) {
               if (ownerPos != null && !viewer.getUuid().equals(ownerUuid)) {
                  Vector3d viewerPos = viewer.getTransform().getPosition();
                  double dx = viewerPos.x() - ownerPos.x();
                  double dz = viewerPos.z() - ownerPos.z();
                  if (dx * dx + dz * dz > this.viewRangeSq) {
                     continue;
                  }
               }

               result.add(ref);
            }
         }
      }

      return result;
   }

   private void sendModelParticle(int networkId, @Nonnull String systemId, float yOffset, @Nonnull UUID ownerUuid) {
      ModelParticle particle = new ModelParticle(systemId, 1.0F, null, EntityPart.Self, null, new Vector3f(0.0F, yOffset, 0.0F), null, false, false);
      SpawnModelParticles packet = new SpawnModelParticles(networkId, new ModelParticle[]{particle});
      this.sendPacketToViewers(packet, ownerUuid);
   }

   private void sendPacketToViewers(@Nonnull SpawnModelParticles packet, @Nonnull UUID ownerUuid) {
      Vector3d ownerPos = this.lastPositions.get(ownerUuid);
      boolean debug = this.debugPlayers.contains(ownerUuid);
      PlayerRef owner = this.findPlayerRef(ownerUuid);
      UUID worldUuid = owner != null ? owner.getWorldUuid() : null;

      for (PlayerRef viewer : Universe.get().getPlayers()) {
         if ((!viewer.getUuid().equals(ownerUuid) || debug)
            && viewer.getPacketHandler() != null
            && (worldUuid == null || worldUuid.equals(viewer.getWorldUuid()))) {
            if (ownerPos != null && !viewer.getUuid().equals(ownerUuid)) {
               Vector3d viewerPos = viewer.getTransform().getPosition();
               double dx = viewerPos.x() - ownerPos.x();
               double dz = viewerPos.z() - ownerPos.z();
               if (dx * dx + dz * dz > this.viewRangeSq) {
                  continue;
               }
            }

            viewer.getPacketHandler().write(packet);
         }
      }
   }

   public boolean togglePlayer(UUID uuid) {
      if (this.disabledPlayers.contains(uuid)) {
         this.disabledPlayers.remove(uuid);
         return true;
      } else {
         this.disabledPlayers.add(uuid);
         this.clearPlayerState(uuid);
         return false;
      }
   }

   public boolean isEnabled(UUID uuid) {
      return !this.disabledPlayers.contains(uuid);
   }

   public void setViewRange(double blocks) {
      this.viewRange = blocks;
      this.viewRangeSq = blocks * blocks;
   }

   public double getViewRange() {
      return this.viewRange;
   }

   public void setAnim(UUID uuid, boolean enabled) {
      if (enabled) {
         this.animDisabledPlayers.remove(uuid);
      } else {
         this.animDisabledPlayers.add(uuid);
      }
   }

   public boolean isAnimEnabled(UUID uuid) {
      return !this.animDisabledPlayers.contains(uuid);
   }

   public boolean toggleDebug(UUID uuid) {
      if (this.debugPlayers.contains(uuid)) {
         this.debugPlayers.remove(uuid);
         return false;
      } else {
         this.debugPlayers.add(uuid);
         return true;
      }
   }

   public boolean isDebugEnabled(UUID uuid) {
      return this.debugPlayers.contains(uuid);
   }

   public void removePlayer(UUID uuid) {
      this.clearPlayerState(uuid);
      this.lastClearTimes.remove(uuid);
      this.introGeneration.remove(uuid);
      this.lastActivityTimes.remove(uuid);
   }

   public void clearBubbleExternal(@Nonnull UUID uuid) {
      if (this.playerStates.containsKey(uuid)) {
         this.fadeoutAndClear(uuid);
      }
   }

   public boolean hasActiveBubble(@Nonnull UUID uuid) {
      return this.playerStates.containsKey(uuid);
   }

   public BubbleState getState(UUID uuid) {
      return this.playerStates.get(uuid);
   }

   public int getTrackedCount() {
      return this.playerStates.size();
   }

   private String resolveSystem(@Nonnull UUID uuid, @Nonnull String defaultSystemName) {
      String prefix = this.customPrefixes.get(uuid);
      return prefix == null ? defaultSystemName : prefix + "_" + defaultSystemName.substring(9);
   }

   private void ensureColorRegistered(@Nonnull UUID uuid, int netId) {
      Color color = this.playerColors.get(uuid);
      if (color != null && !this.customPrefixes.containsKey(uuid)) {
         this.registerCustomColor(uuid, netId, color);
      }
   }

   private void registerCustomColor(@Nonnull UUID uuid, int netId, @Nonnull Color tint) {
      BubbleSpawnerFactory.ColorRegistration registration = BubbleSpawnerFactory.createRegistration(netId, tint);
      this.customRegistrations.put(uuid, registration);
      this.customPrefixes.put(uuid, registration.prefix);

      for (PlayerRef playerRef : Universe.get().getPlayers()) {
         try {
            if (playerRef.getPacketHandler() != null) {
               playerRef.getPacketHandler().writeNoCache(registration.spawnerPacket);
               playerRef.getPacketHandler().writeNoCache(registration.systemPacket);
            }
         } catch (Exception ignored) {
         }
      }

      ((Api)LOGGER.atInfo()).log("Registered custom color spawners for %s (prefix=%s, tint=%s)", uuid, registration.prefix, colorToHex(tint));
   }

   public void setColor(@Nonnull UUID uuid, @Nonnull Color color) {
      this.playerColors.put(uuid, color);
      this.customPrefixes.remove(uuid);
      this.customRegistrations.remove(uuid);
      Integer netId = this.playerNetworkIds.get(uuid);
      if (netId != null) {
         this.registerCustomColor(uuid, netId, color);
      }
   }

   public void clearColor(@Nonnull UUID uuid) {
      this.playerColors.remove(uuid);
      this.customPrefixes.remove(uuid);
      this.customRegistrations.remove(uuid);
   }

   @Nullable
   public Color getColor(@Nonnull UUID uuid) {
      return this.playerColors.get(uuid);
   }

   public void onPlayerConnect(@Nonnull PlayerRef newPlayer) {
      for (BubbleSpawnerFactory.ColorRegistration registration : this.customRegistrations.values()) {
         try {
            if (newPlayer.getPacketHandler() != null) {
               newPlayer.getPacketHandler().writeNoCache(registration.spawnerPacket);
               newPlayer.getPacketHandler().writeNoCache(registration.systemPacket);
            }
         } catch (Exception ignored) {
         }
      }
   }

   @Nullable
   public static Color parseHexColor(@Nonnull String hex) {
      try {
         String digits = hex.startsWith("#") ? hex.substring(1) : hex;
         if (digits.length() != 6) {
            return null;
         }

         int r = Integer.parseInt(digits.substring(0, 2), 16);
         int g = Integer.parseInt(digits.substring(2, 4), 16);
         int b = Integer.parseInt(digits.substring(4, 6), 16);
         return new Color((byte)r, (byte)g, (byte)b);
      } catch (Exception e) {
         return null;
      }
   }

   private static boolean hasBubbleChatActive(UUID uuid) {
      if (!bubbleChatChecked) {
         bubbleChatChecked = true;

         try {
            Class<?> apiClass = Class.forName("com.bubblechat.BubbleChatAPI");
            bubbleChatHasActiveMethod = apiClass.getMethod("hasActiveBubble", UUID.class);
         } catch (Exception ignored) {
         }
      }

      if (bubbleChatHasActiveMethod != null) {
         try {
            return (Boolean)bubbleChatHasActiveMethod.invoke(null, uuid);
         } catch (Exception ignored) {
         }
      }

      return false;
   }

   @Nonnull
   public static String colorToHex(@Nonnull Color color) {
      int r = color.red & 255;
      int g = color.green & 255;
      int b = color.blue & 255;
      return String.format("#%02X%02X%02X", r, g, b);
   }
}
