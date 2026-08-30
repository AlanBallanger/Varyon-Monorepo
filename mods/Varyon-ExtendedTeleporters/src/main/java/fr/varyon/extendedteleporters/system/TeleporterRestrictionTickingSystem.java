package fr.varyon.extendedteleporters.system;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public final class TeleporterRestrictionTickingSystem extends EntityTickingSystem<EntityStore> {
   private static final double OWNER_ACTIVATION_RADIUS = 5.0;
   private static final double OWNER_ACTIVATION_RADIUS_SQ = 25.0;
   private static final long CHUNK_RELOAD_THRESHOLD_TICKS = 100L;
   private final Map<String, Boolean> teleporterActiveState = new ConcurrentHashMap<>();
   private final Map<Long, Long> seenChunks = new ConcurrentHashMap<>();
   private long currentTick = 0L;

   public void tick(
      float deltaTime,
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer
   ) {
      this.currentTick++;
      Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
      PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
      Player player = (Player)store.getComponent(ref, Player.getComponentType());
      if (playerRef != null && player != null) {
         UUID playerUuid = playerRef.getUuid();
         Vector3d playerPos = playerRef.getTransform().getPosition();
         World world = player.getWorld();
         String dimension = world.getName();
         TeleporterManager manager = TeleporterManager.getInstance();
         List<TeleporterInfo> ownedTeleporters = manager.getPlayerTeleporters(playerUuid);
         List<TeleporterInfo> trustedTeleporters = manager.getTeleportersWhereTrusted(playerUuid);

         for (TeleporterInfo info : ownedTeleporters) {
            this.processRestrictedTeleporter(info, playerPos, world, dimension, true);
         }

         for (TeleporterInfo info : trustedTeleporters) {
            this.processRestrictedTeleporter(info, playerPos, world, dimension, false);
         }
      }
   }

   private void processRestrictedTeleporter(TeleporterInfo info, Vector3d playerPos, World world, String dimension, boolean isOwner) {
      if (info.isRestricted() && info.dimension().equals(dimension)) {
         long chunkIndex = ChunkUtil.indexChunkFromBlock(info.blockX(), info.blockZ());
         boolean chunkReloaded = this.detectChunkReload(chunkIndex);
         double dx = playerPos.x - (info.blockX() + 0.5);
         double dy = playerPos.y - (info.blockY() + 0.5);
         double dz = playerPos.z - (info.blockZ() + 0.5);
         double distSq = dx * dx + dy * dy + dz * dz;
         boolean playerNearby = distSq <= 25.0;
         String locationKey = info.locationKey();
         Boolean wasActive = this.teleporterActiveState.get(locationKey);
         if (playerNearby) {
            if (wasActive == null || !wasActive || chunkReloaded || this.isComponentStateOutOfSync(world, info, true)) {
               this.updateTeleporterState(world, info, true);
               this.teleporterActiveState.put(locationKey, true);
            }
         } else if (isOwner) {
            boolean anyTrustedNearby = this.isAnyTrustedPlayerNearby(info, world);
            if (anyTrustedNearby) {
               if (wasActive == null || !wasActive || chunkReloaded) {
                  this.teleporterActiveState.put(locationKey, true);
               }
            } else {
               boolean stateChanged = wasActive == null || wasActive || chunkReloaded;
               if (!stateChanged) {
                  stateChanged = this.isComponentStateOutOfSync(world, info, false);
               }

               if (stateChanged) {
                  this.updateTeleporterState(world, info, false);
                  this.teleporterActiveState.put(locationKey, false);
               }
            }
         }
      }
   }

   private boolean isAnyTrustedPlayerNearby(TeleporterInfo info, World world) {
      Set<UUID> trustedPlayers = info.getTrustedPlayers();
      if (trustedPlayers.isEmpty()) {
         return false;
      }

      double teleporterX = info.blockX() + 0.5;
      double teleporterY = info.blockY() + 0.5;
      double teleporterZ = info.blockZ() + 0.5;
      String teleporterDimension = info.dimension();
      Iterator var11 = Universe.get().getPlayers().iterator();

      while (true) {
         Vector3d playerPos;
         while (true) {
            if (!var11.hasNext()) {
               return false;
            }

            PlayerRef playerRef = (PlayerRef)var11.next();
            if (trustedPlayers.contains(playerRef.getUuid())) {
               try {
                  playerPos = playerRef.getTransform().getPosition();
                  if (playerPos == null) {
                     continue;
                  }
                  break;
               } catch (Exception e) {
               }
            }
         }

         double dx = playerPos.x - teleporterX;
         double dy = playerPos.y - teleporterY;
         double dz = playerPos.z - teleporterZ;
         double distSq = dx * dx + dy * dy + dz * dz;
         if (distSq <= 25.0) {
            return true;
         }
      }
   }

   private boolean detectChunkReload(long chunkIndex) {
      Long lastSeen = this.seenChunks.get(chunkIndex);
      this.seenChunks.put(chunkIndex, this.currentTick);
      return lastSeen == null || this.currentTick - lastSeen > 100L;
   }

   private boolean isComponentStateOutOfSync(World world, TeleporterInfo info, boolean shouldBeActive) {
      try {
         Teleporter teleporter = this.getTeleporterComponent(world, info);
         if (teleporter == null) {
            return false;
         }

         String currentWarp = teleporter.getWarp();
         boolean componentHasWarp = currentWarp != null && !currentWarp.isEmpty();
         return componentHasWarp != shouldBeActive;
      } catch (Exception e) {
         return false;
      }
   }

   private void updateTeleporterState(World world, TeleporterInfo info, boolean activate) {
      int blockX = info.blockX();
      int blockY = info.blockY();
      int blockZ = info.blockZ();
      if (activate) {
         String persistedWarp = info.warpDestination();
         world.execute(() -> {
            Teleporter teleporter = this.getTeleporterComponent(world, info);
            if (teleporter != null) {
               String warpToRestore = persistedWarp;
               if (warpToRestore == null || warpToRestore.isEmpty()) {
                  String componentWarp = teleporter.getWarp();
                  if (componentWarp != null && !componentWarp.isEmpty()) {
                     warpToRestore = componentWarp;
                     info.setWarpDestination(componentWarp);
                     TeleporterManager.getInstance().markDirty();
                  }
               }

               if (warpToRestore != null && !warpToRestore.isEmpty()) {
                  teleporter.setWarp(warpToRestore);
                  this.updateBlockState(world, blockX, blockY, blockZ, true);
               } else {
                  this.updateBlockState(world, blockX, blockY, blockZ, false);
               }
            }
         });
      } else {
         world.execute(() -> {
            Teleporter teleporter = this.getTeleporterComponent(world, info);
            if (teleporter != null) {
               String currentWarp = teleporter.getWarp();
               if (currentWarp != null && !currentWarp.isEmpty()) {
                  info.setWarpDestination(currentWarp);
                  TeleporterManager.getInstance().markDirty();
               }

               teleporter.setWarp(null);
               this.updateBlockState(world, blockX, blockY, blockZ, false);
            }
         });
      }
   }

   private void updateBlockState(World world, int blockX, int blockY, int blockZ, boolean isActive) {
      try {
         ChunkStore chunkStore = world.getChunkStore();
         if (chunkStore == null) {
            return;
         }

         long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
         WorldChunk blockComponentChunk = (WorldChunk)chunkStore.getChunkComponent(chunkIndex, WorldChunk.getComponentType());
         if (blockComponentChunk == null) {
            return;
         }

         Ref<ChunkStore> blockRef = blockComponentChunk.getBlockComponentEntity(blockX, blockY, blockZ);
         if (blockRef == null || !blockRef.isValid()) {
            return;
         }

         BlockStateInfo blockStateInfo = (BlockStateInfo)chunkStore.getStore().getComponent(blockRef, BlockStateInfo.getComponentType());
         if (blockStateInfo == null) {
            return;
         }

         Ref<ChunkStore> chunkRef = blockStateInfo.getSectionRef();
         if (chunkRef == null || !chunkRef.isValid()) {
            return;
         }

         WorldChunk worldChunk = (WorldChunk)chunkRef.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
         if (worldChunk == null) {
            return;
         }

         String newState = isActive ? "active" : "default";
         BlockType blockType = worldChunk.getBlockType(blockX, blockY, blockZ);
         if (blockType != null) {
            String currentState = blockType.getStateForBlock(blockType);
            if (currentState == null || !currentState.equals(newState)) {
               BlockType variantBlockType = blockType.getBlockForState(newState);
               if (variantBlockType != null) {
                  worldChunk.setBlockInteractionState(blockX, blockY, blockZ, variantBlockType, newState, true);
               }
            }
         }
      } catch (Exception var19) {
      }
   }

   private Teleporter getTeleporterComponent(World world, TeleporterInfo info) {
      try {
         ChunkStore chunkStore = world.getChunkStore();
         long chunkIndex = ChunkUtil.indexChunkFromBlock(info.blockX(), info.blockZ());
         WorldChunk blockChunk = (WorldChunk)chunkStore.getChunkComponent(chunkIndex, WorldChunk.getComponentType());
         if (blockChunk == null) {
            return null;
         }

         Ref<ChunkStore> blockRef = blockChunk.getBlockComponentEntity(info.blockX(), info.blockY(), info.blockZ());
         return blockRef != null && blockRef.isValid() ? (Teleporter)chunkStore.getStore().getComponent(blockRef, Teleporter.getComponentType()) : null;
      } catch (Exception e) {
         return null;
      }
   }

   public void onTeleporterRegistered(TeleporterInfo info, World world) {
      if (info.isRestricted() && info.hasOwner()) {
         String locationKey = info.locationKey();
         this.updateTeleporterState(world, info, false);
         this.teleporterActiveState.put(locationKey, false);
      }
   }

   public void onRestrictionChanged(TeleporterInfo info, World world, boolean isNowRestricted) {
      String locationKey = info.locationKey();
      if (isNowRestricted) {
         this.updateTeleporterState(world, info, false);
         this.teleporterActiveState.put(locationKey, false);
      } else {
         this.teleporterActiveState.remove(locationKey);
         String warpToRestore = info.warpDestination();
         if (warpToRestore != null && !warpToRestore.isEmpty()) {
            String finalWarpToRestore = warpToRestore;
            int blockX = info.blockX();
            int blockY = info.blockY();
            int blockZ = info.blockZ();
            world.execute(() -> {
               Teleporter teleporter = this.getTeleporterComponent(world, info);
               if (teleporter != null) {
                  teleporter.setWarp(finalWarpToRestore);
               }

               this.updateBlockState(world, blockX, blockY, blockZ, true);
            });
         }
      }
   }

   public void onTeleporterRemoved(String locationKey) {
      this.teleporterActiveState.remove(locationKey);
   }

   public void onChunkUnloaded(long chunkIndex) {
      this.seenChunks.remove(chunkIndex);
   }

   @NullableDecl
   public Query<EntityStore> getQuery() {
      return PlayerRef.getComponentType();
   }
}
