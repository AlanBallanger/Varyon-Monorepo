package com.varyon.bossarena;

import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.data.BossDefinition;
import com.varyon.bossarena.data.BossRegistry;
import com.varyon.bossarena.command.BossArenaCommand;
import com.varyon.bossarena.command.BossArenaShortCommand;
import com.varyon.bossarena.spawn.BossSpawnService;
import com.varyon.bossarena.spawn.BossTimedSpawnScheduler;
import com.varyon.bossarena.spawn.TimedBossMapMarkerService;
import com.varyon.bossarena.damagechart.BossDamageChartTracker;
import com.varyon.bossarena.damagechart.BossArenaDamageChartOpener;
import com.varyon.bossarena.damagechart.BossDamageChartHpSnapshotSystem;
import com.varyon.bossarena.damagechart.BossDamageChartRecordingSystem;
import com.varyon.bossarena.damagechart.DamageChartOpener;
import com.varyon.bossarena.system.BossTrackingSystem;
import com.varyon.bossarena.system.BossDeathSystem;
import com.varyon.bossarena.system.BossDamageScalingSystem;
import com.varyon.bossarena.system.BossEventNotificationSystem;
import com.varyon.bossarena.system.BossEntityRemovedSystem;
import com.varyon.bossarena.system.BossLeashSystem;
import com.varyon.bossarena.system.BossSpeedScalingSystem;
import com.varyon.bossarena.system.LootSpawnSystem;
import com.varyon.bossarena.system.RPGLevelingBossScaleCompatSystem;
import com.varyon.bossarena.loot.LootRegistry;
import com.varyon.bossarena.loot.BossLootHandler;
import com.varyon.bossarena.loot.BossLootChestBlock;
import com.varyon.bossarena.loot.BossLootChestOpener;
import com.varyon.bossarena.loot.BossLootChestUseSystem;
import com.varyon.bossarena.loot.OrphanItemContainerBlockCleanupSystem;
import com.varyon.bossarena.util.BossArenaCleanup;
import com.varyon.bossarena.loot.OpenBossChestInteraction;
import com.varyon.bossarena.music.BossFightMusicApplySystem;
import com.varyon.bossarena.music.BossFightMusicManager;
import com.varyon.bossarena.shop.BossShopConfig;
import com.varyon.bossarena.shop.BossArenaShopPage;
import com.varyon.bossarena.shop.OpenBossShopNpcInteraction;
import com.varyon.bossarena.config.BossArenaConfig;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityUseBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.bossarena.util.VecUtil;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import com.google.gson.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class BossArenaPlugin extends JavaPlugin {
    public static final String ASSET_PACK_ID = "com.varyon:Varyon-BossArena";
    public static final String NO_DEATH_DROPS_INTERACTION_ID = "BossArena_NoDeathDrops";
    public static final String SHOP_OPEN_INTERACTION_ID = "BossArena_OpenShopNpc";
    public static final String SHOP_NPC_TYPE_ID = "bossarena_shop_guard";
    public static final PluginIdentifier RPG_LEVELING_PLUGIN_ID = new PluginIdentifier("Zuxaw", "RPGLeveling");
    private static final Path MOD_ROOT = Path.of("mods", "Varyon-BossArena");
    private static final ScheduledExecutorService SHOP_REBIND_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BossArena-ShopRebind");
                t.setDaemon(true);
                return t;
            });
    private static BossArenaPlugin INSTANCE;
    private final java.util.concurrent.atomic.AtomicBoolean cleanedUp = new java.util.concurrent.atomic.AtomicBoolean(false);
    private BossTrackingSystem trackingSystem;
    private BossDamageChartTracker damageChartTracker;
    private BossArenaConfig config = new BossArenaConfig();
    private BossShopConfig shopConfig = new BossShopConfig();
    private BossSpawnService bossSpawnService;
    private BossTimedSpawnScheduler timedSpawnScheduler;
    private TimedBossMapMarkerService timedBossMapMarkerService;
    private BossFightMusicManager fightMusicManager;
    private Path bossesJsonPath;
    private Path arenasJsonPath;
    private Path lootTablesPath;
    private Path lootChestStatePath;
    private Path bossFightStatePath;
    private Path timedSpawnStatePath;
    private Path shopJsonPath;

    public BossArenaPlugin(JavaPluginInit init) {
        super(init);
    }

    private static void copyMissingTree(Path sourceRoot, Path targetRoot) throws IOException {
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceRoot.relativize(dir);
                Files.createDirectories(targetRoot.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceRoot.relativize(file);
                Path destination = targetRoot.resolve(relative);
                if (!Files.exists(destination)
                        || Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(destination)) > 0) {
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isLegacyBossArenaDirectory(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString().toLowerCase();
        return name.equals("com.bossarena_bossarena")
                || name.equals("com.varyon_varyon-bossarena");
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isShopOpenInteraction(InteractionType type) {
        return type == InteractionType.Use;
    }

    private static boolean isShopNpcTypeId(String npcTypeId, String configuredId) {
        if (npcTypeId == null || npcTypeId.isBlank() || configuredId == null || configuredId.isBlank()) {
            return false;
        }
        String normalizedNpcTypeId = npcTypeId.toLowerCase(Locale.ROOT);
        String normalizedConfiguredId = configuredId.toLowerCase(Locale.ROOT);
        return normalizedNpcTypeId.equals(normalizedConfiguredId)
                || normalizedNpcTypeId.endsWith(":" + normalizedConfiguredId)
                || normalizedNpcTypeId.endsWith("/" + normalizedConfiguredId);
    }

    private static NPCEntity resolveTargetNpc(Entity targetEntity, Ref<EntityStore> targetRef, Store<EntityStore> store) {
        if (targetRef != null) {
            Object npcObj = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npcObj instanceof NPCEntity npc) {
                return npc;
            }
        }
        if (targetEntity instanceof NPCEntity npc) {
            return npc;
        }
        return null;
    }

    private static UUID resolveTargetUuid(Entity targetEntity, Ref<EntityStore> targetRef, Store<EntityStore> store, NPCEntity npc) {
        if (targetRef != null) {
            Object uuidObj = store.getComponent(targetRef, UUIDComponent.getComponentType());
            if (uuidObj instanceof UUIDComponent uuidComponent) {
                return uuidComponent.getUuid();
            }
        }
        if (targetEntity != null) {
            @SuppressWarnings("removal")
            UUID legacy = targetEntity.getUuid();
            return legacy;
        }
        if (npc == null) {
            return null;
        }
        @SuppressWarnings("removal")
        UUID npcUuid = npc.getUuid();
        return npcUuid;
    }

    private static TransformComponent resolveTargetTransform(Entity targetEntity, Ref<EntityStore> targetRef, Store<EntityStore> store, NPCEntity npc) {
        if (npc != null) {
            Ref<EntityStore> npcRef = npc.getReference();
            if (npcRef != null) {
                Object t = store.getComponent(npcRef, TransformComponent.getComponentType());
                if (t instanceof TransformComponent tc) return tc;
            }
        }
        if (targetRef != null) {
            Object transformObj = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (transformObj instanceof TransformComponent transform) {
                return transform;
            }
        }
        if (targetEntity != null) {
            Ref<EntityStore> entRef = targetEntity.getReference();
            if (entRef != null) {
                Object t = store.getComponent(entRef, TransformComponent.getComponentType());
                if (t instanceof TransformComponent tc) return tc;
            }
        }
        return null;
    }

    public static BossArenaPlugin getInstance() {
        return INSTANCE;
    }

    public BossTrackingSystem getTrackingSystem() {
        return trackingSystem;
    }

    public BossDamageChartTracker getDamageChartTracker() {
        return damageChartTracker;
    }

    public TimedBossMapMarkerService getTimedBossMapMarkerService() {
        return timedBossMapMarkerService;
    }

    public BossFightMusicManager getFightMusicManager() {
        return fightMusicManager;
    }

    /**
     * Lifecycle: setup() registers codecs, interactions, asset pack, blocks, ECS systems, commands, and paths;
     * then loads config and starts async {@link #startBossArenaSystems()} (persistence, bosses/arenas/loot, timed spawns).
     * Shutdown clears tracked entities and loot chests via {@link com.varyon.bossarena.util.BossArenaCleanup}.
     */
    @Override
    public void setup() {
        INSTANCE = this;
        getLogger().atInfo().log("BossArena setup() called");

        registerCustomCodecs();
        registerCustomInteractions();
        registerAssetPack();
        registerCustomBlocks();

        Path modRoot = getModRootDirectory();
        this.bossesJsonPath = modRoot.resolve("bosses.json");
        this.arenasJsonPath = modRoot.resolve("arenas.json");
        this.lootTablesPath = modRoot.resolve("loot_tables.json");
        this.lootChestStatePath = modRoot.resolve("loot_chests_state.json");
        this.bossFightStatePath = modRoot.resolve("boss_fights_state.json");
        this.timedSpawnStatePath = modRoot.resolve("timed_spawn_state.json");
        this.shopJsonPath = modRoot.resolve("shop.json");

        // Create tracking system
        this.trackingSystem = new BossTrackingSystem();
        this.damageChartTracker = new BossDamageChartTracker();

        // Register ECS systems
        this.getEntityStoreRegistry().registerSystem(new LootSpawnSystem());
        this.getEntityStoreRegistry().registerSystem(new BossDamageScalingSystem(trackingSystem));
        this.getEntityStoreRegistry().registerSystem(new BossDamageChartHpSnapshotSystem(trackingSystem));
        this.getEntityStoreRegistry().registerSystem(new BossDamageChartRecordingSystem(trackingSystem, damageChartTracker));
        this.getEntityStoreRegistry().registerSystem(new BossSpeedScalingSystem(trackingSystem));
        this.getEntityStoreRegistry().registerSystem(new BossLeashSystem(trackingSystem));
        this.getEntityStoreRegistry().registerSystem(new BossDeathSystem(trackingSystem, this));
        this.getEntityStoreRegistry().registerSystem(new BossEventNotificationSystem(trackingSystem, this));
        this.getEntityStoreRegistry().registerSystem(new BossEntityRemovedSystem(trackingSystem, this));
        this.getEntityStoreRegistry().registerSystem(new RPGLevelingBossScaleCompatSystem(trackingSystem));
        this.fightMusicManager = new BossFightMusicManager(getModRootDirectory().resolve("music"));
        this.getEntityStoreRegistry().registerSystem(new BossFightMusicApplySystem(fightMusicManager));
        this.getEntityStoreRegistry().registerSystem(new BossLootChestUseSystem());
        getLogger().atInfo().log("Registered BossArena HP scale compatibility system "
                + "(activates only when RPGLeveling is loaded)");
        getLogger().atInfo().log("Successfully registered boss systems");

        // Fallback chest open (UseBlockEvent.Pre is the primary path via BossLootChestUseSystem)
        this.getEventRegistry().registerGlobal(
                LivingEntityUseBlockEvent.class,
                this::onBlockInteract
        );
        getLogger().atInfo().log("Registered chest interaction listener");
        this.getEventRegistry().registerGlobal(
                PlayerInteractEvent.class,
                this::onPlayerInteract
        );
        getLogger().atInfo().log("Registered player interaction listener");
        this.getEventRegistry().registerGlobal(
                AddPlayerToWorldEvent.class,
                this::onAddPlayerToWorld
        );
        getLogger().atInfo().log("Registered world player add listener");
        this.getEventRegistry().registerGlobal(EventPriority.FIRST, ShutdownEvent.class, event -> {
            getLogger().atInfo().log("ShutdownEvent received (priority FIRST)");
            handleShutdown();
        });
        getLogger().atInfo().log("Registered shutdown listener (priority FIRST)");

        config.load();
        if (config.timedMapMarker != null) {
            String markerImage = config.timedMapMarker.markerImage != null
                    ? config.timedMapMarker.markerImage.trim()
                    : "";
            if (markerImage.isEmpty() || markerImage.equalsIgnoreCase("Spawn.png")) {
                config.timedMapMarker.markerImage = BossArenaConfig.DEFAULT_TIMED_MAP_MARKER_IMAGE;
                config.save();
            }
        }
        shopConfig.load(shopJsonPath);
        if (shopConfig.applyRuntimeCurrencyDetection(resolveFallbackCurrencyItemId())) {
            saveShopConfig();
            getLogger().atInfo().log("Detected currency provider at startup: " + shopConfig.currencyProvider
                    + " (item fallback: " + shopConfig.currencyItemId + ")");
        }

        // Register commands
        try {
            var cm = CommandManager.get();
            if (cm != null) {
                cm.register(new BossArenaCommand(this));
                cm.register(new BossArenaShortCommand(this));
                getLogger().atInfo().log("BossArena commands registered successfully");
            } else {
                getLogger().atWarning().log("CommandManager is null, commands not registered!");
            }
        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("Failed to register BossArena commands");
        }

        this.bossSpawnService = new BossSpawnService(trackingSystem, config);
        this.timedSpawnScheduler = new BossTimedSpawnScheduler(bossSpawnService, trackingSystem);
        this.timedBossMapMarkerService = new TimedBossMapMarkerService(this, trackingSystem, timedSpawnScheduler);
        this.timedSpawnScheduler.setMapMarkerService(timedBossMapMarkerService);
        this.timedSpawnScheduler.setOneShotDisableHandler(this::disableTimedRuleOneShot);
        this.timedSpawnScheduler.setConfigSupplier(this::getConfig);

        DamageChartOpener chartOpener = createDamageChartOpener();
        BossLootHandler.setDamageChartDependencies(damageChartTracker, chartOpener);

        // Async startup
        CompletableFuture.runAsync(() -> {
            while (com.hypixel.hytale.server.core.universe.Universe.get() == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            }
            startBossArenaSystems();
        });
    }

    private DamageChartOpener createDamageChartOpener() {
        getLogger().atInfo().log("Damage chart will be sent via chat only (top 10 rows).");
        return new BossArenaDamageChartOpener();
    }

    private void registerCustomBlocks() {
        try {
            getLogger().atInfo().log("Registering Boss Arena custom blocks...");

        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("Failed to register custom blocks");
        }
    }

    private void registerCustomInteractions() {
        try {
            getLogger().atInfo().log("Registering Boss Arena custom interactions...");

            RootInteraction rootInteraction = new RootInteraction("BossArena_OpenChest", "BossArena_OpenChest");
            RootInteraction.getAssetStore().loadAssets(ASSET_PACK_ID, List.of(rootInteraction));
            getLogger().atInfo().log("Registered RootInteraction: BossArena_OpenChest");


            RootInteraction shopOpenInteraction = new RootInteraction(
                    SHOP_OPEN_INTERACTION_ID,
                    SHOP_OPEN_INTERACTION_ID
            );
            RootInteraction.getAssetStore().loadAssets(ASSET_PACK_ID, List.of(shopOpenInteraction));
            getLogger().atInfo().log("Registered RootInteraction: " + SHOP_OPEN_INTERACTION_ID);

            RootInteraction noDeathDrops = new RootInteraction(NO_DEATH_DROPS_INTERACTION_ID, NO_DEATH_DROPS_INTERACTION_ID);
            RootInteraction.getAssetStore().loadAssets(ASSET_PACK_ID, List.of(noDeathDrops));
            getLogger().atInfo().log("Registered RootInteraction: " + NO_DEATH_DROPS_INTERACTION_ID);

            Interaction.getAssetStore().loadAssets(ASSET_PACK_ID, List.of(
                    new OpenBossChestInteraction(),
                    new OpenBossShopNpcInteraction(),
                    new SimpleInteraction(NO_DEATH_DROPS_INTERACTION_ID)
            ));
            getLogger().atInfo().log("Registered interaction assets for BossArena_OpenChest and "
                    + SHOP_OPEN_INTERACTION_ID);

        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("Failed to register custom interactions");
        }
    }

    private void registerCustomCodecs() {
        try {
            getLogger().atInfo().log("Registering Boss Arena chunk components and interactions...");

            ComponentType<ChunkStore, BossLootChestBlock> bossLootChestType =
                    getChunkStoreRegistry().registerComponent(
                            BossLootChestBlock.class,
                            "BossLootChestBlock",
                            BossLootChestBlock.CODEC
                    );
            BossLootChestBlock.setComponentType(bossLootChestType);
            getChunkStoreRegistry().registerSystem(new OrphanItemContainerBlockCleanupSystem());
            getLogger().atInfo().log("Registered orphan ItemContainerBlock cleanup system");

            // Register OpenBossChestInteraction codec with Interaction system
            // The string must match the interaction ID in the JSON
            Interaction.CODEC.register(
                    "BossArena_OpenChest",
                    OpenBossChestInteraction.class,
                    OpenBossChestInteraction.CODEC
            );
            Interaction.CODEC.register(
                    SHOP_OPEN_INTERACTION_ID,
                    OpenBossShopNpcInteraction.class,
                    OpenBossShopNpcInteraction.CODEC
            );
            Interaction.CODEC.register(
                    NO_DEATH_DROPS_INTERACTION_ID,
                    SimpleInteraction.class,
                    SimpleInteraction.CODEC
            );

            getLogger().atInfo().log("✅ Successfully registered custom codecs");

        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("❌ Failed to register custom codecs");
        }
    }

    private void registerAssetPack() {
        try {
            Path modRoot = getModRootDirectory();
            Files.deleteIfExists(modRoot.resolve("manifest.json"));
            migrateLegacyDataDirectory(modRoot);
            BossArenaAssetSetup.register(this);
        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("Failed to register BossArena asset pack");
        }
    }

    private Path getModRootDirectory() {
        return MOD_ROOT;
    }

    /** Copies a single resource to a path; used only for legacy data migration. */
    private void copyResourceForMigration(String resourcePath, Path destination) throws IOException {
        java.nio.file.Files.createDirectories(destination.getParent());
        try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                java.nio.file.Files.copy(in, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void migrateLegacyDataDirectory(Path canonicalRoot) {
        try {
            Path legacyFlatMods = Path.of("mods", "BossArena");
            Path canonicalPath = canonicalRoot.toAbsolutePath().normalize();
            if (Files.isDirectory(legacyFlatMods)) {
                Path legacyFlat = legacyFlatMods.toAbsolutePath().normalize();
                if (!legacyFlat.equals(canonicalPath)) {
                    Files.createDirectories(canonicalRoot);
                    copyMissingTree(legacyFlatMods, canonicalRoot);
                    getLogger().atInfo().log("Migrated BossArena data from mods/BossArena to " + canonicalRoot);
                }
            }
            Path legacyRoot = getDataDirectory();
            if (legacyRoot == null) {
                return;
            }

            Path canonical = canonicalRoot.toAbsolutePath().normalize();
            Path legacy = legacyRoot.toAbsolutePath().normalize();
            if (legacy.equals(canonical) || !Files.isDirectory(legacy)) {
                return;
            }

            Files.createDirectories(canonical);
            copyMissingTree(legacy, canonical);
            copyResourceForMigration("manifest.json", canonical.resolve("manifest.json"));
            getLogger().atInfo().log("Migrated BossArena data from legacy path " + legacy + " to " + canonical);
            if (isLegacyBossArenaDirectory(legacy)) {
                deleteTree(legacy);
                getLogger().atInfo().log("Removed legacy BossArena directory at " + legacy);
            }
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("Failed to migrate legacy BossArena data directory");
        }
    }

    private void onBlockInteract(LivingEntityUseBlockEvent event) {
        if (event == null || event.getRef() == null || event.getRef().getStore() == null) {
            return;
        }

        // Always ensure we are on the world thread for component access and world state checks
        World world = event.getRef().getStore().getExternalData() != null ?
                ((EntityStore) event.getRef().getStore().getExternalData()).getWorld() : null;

        if (world != null && !world.isInThread()) {
            world.execute(() -> onBlockInteract(event));
            return;
        }

        String blockType = event.getBlockType();

        // Check if it's a boss loot chest (custom or fallback vanilla legendary)
        if (!blockType.contains("Boss_Arena_Chest")
                && !blockType.contains("Chest_Legendary")
                && !blockType.contains("Furniture_Dungeon_Chest")) {
            return;
        }

        getLogger().atInfo().log("Player interacted with chest! Block type: " + blockType);

        // Get player
        Ref<EntityStore> playerRef = event.getRef();
        Store<EntityStore> store = playerRef.getStore();

        // Get player's world
        EntityStore entityStoreData = store.getExternalData();
        World playerWorld = entityStoreData.getWorld();

        // Get player position to find nearby chest
        Object transformObj = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (!(transformObj instanceof TransformComponent)) {
            return;
        }

        org.joml.Vector3d rawPlayerPos = ((TransformComponent) transformObj).getPosition();
        Vector3d playerPos = VecUtil.toJoml(rawPlayerPos);

        // Prefer loot keyed near the player; OpenBossChestInteraction uses exact block coords.
        Vector3d chestLoc = BossLootHandler.getChestLocationNear(playerWorld, playerPos);
        if (chestLoc == null) {
            getLogger().atInfo().log("No boss loot chest nearby");
            return;
        }

        int x = (int) Math.floor(chestLoc.x);
        int y = (int) Math.floor(chestLoc.y);
        int z = (int) Math.floor(chestLoc.z);

        if (BossLootHandler.ensureBossLootChestBlock(playerWorld, x, y, z) == null) {
            getLogger().atWarning().log("Chest nearby but could not attach BossLootChestBlock at "
                    + x + "," + y + "," + z);
            return;
        }

        getLogger().atInfo().log("Opening BossLootChestBlock at " + x + "," + y + "," + z);
        BossLootChestOpener.open(playerWorld, playerRef, store, null, x, y, z);
    }

    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayerRef() == null || event.getPlayerRef().getStore() == null) {
            return;
        }

        // Always ensure we are on the world thread for interaction handling
        World world = event.getPlayerRef().getStore().getExternalData() != null ?
                ((EntityStore) event.getPlayerRef().getStore().getExternalData()).getWorld() : null;

        if (world != null && !world.isInThread()) {
            world.execute(() -> onPlayerInteract(event));
            return;
        }

        if (!isShopOpenInteraction(event.getActionType())) {
            return;
        }

        Ref<EntityStore> playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        Store<EntityStore> store = playerRef.getStore();
        Entity targetEntity = event.getTargetEntity();
        Ref<EntityStore> targetRef = event.getTargetRef();

        NPCEntity npc = resolveTargetNpc(targetEntity, targetRef, store);
        UUID targetUuid = resolveTargetUuid(targetEntity, targetRef, store, npc);
        if (shopConfig == null) {
            return;
        }

        boolean knownShopNpc = targetUuid != null && shopConfig.isShopNpcUuid(targetUuid.toString());
        boolean matchesConfiguredType = npc != null
                && isShopNpcTypeId(npc.getNPCTypeId(), resolveShopNpcId());
        if (!knownShopNpc && !matchesConfiguredType) {
            if (targetEntity != null || targetRef != null) {
                getLogger().atInfo().log(
                        "Shop interact ignored: action=" + event.getActionType()
                                + ", targetEntity=" + (targetEntity != null ? targetEntity.getClass().getSimpleName() : "null")
                                + ", npcType=" + (npc != null ? npc.getNPCTypeId() : "null")
                                + ", targetUuid=" + (targetUuid != null ? targetUuid : "null")
                                + ", knownShopNpc=" + knownShopNpc
                                + ", matchesConfiguredType=" + matchesConfiguredType
                );
            }
            return;
        }
        Object playerObj = store.getComponent(playerRef, Player.getComponentType());
        if (!(playerObj instanceof Player player)) {
            return;
        }

        TransformComponent targetTransform = resolveTargetTransform(targetEntity, targetRef, store, npc);
        if (targetUuid != null && targetTransform != null) {
            String worldName = player.getWorld() != null ? player.getWorld().getName() : null;
            if (worldName != null && !worldName.isBlank()) {
                org.joml.Vector3d rawTargetPos = targetTransform.getPosition();
                recordShopLocation(
                        worldName,
                        new Vector3i(
                                (int) Math.floor(rawTargetPos.x),
                                (int) Math.floor(rawTargetPos.y),
                                (int) Math.floor(rawTargetPos.z)
                        ),
                        targetUuid
                );
            } else if (!knownShopNpc && matchesConfiguredType) {
                recordShopNpcUuid(targetUuid);
            }
        } else if (!knownShopNpc && matchesConfiguredType && targetUuid != null) {
            recordShopNpcUuid(targetUuid);
        }

        getLogger().atInfo().log(
                "Opening shop from interact: action=" + event.getActionType()
                        + ", npcType=" + (npc != null ? npc.getNPCTypeId() : "null")
                        + ", targetUuid=" + (targetUuid != null ? targetUuid : "null")
                        + ", hasTransform=" + (targetTransform != null)
        );
        openShopPage(playerRef, store, player, targetTransform);
        event.setCancelled(true);
    }

    private void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
        if (event == null) {
            return;
        }
        World world = event.getWorld();
        if (world == null) {
            return;
        }
        if (shopConfig != null) {
            scheduleShopRebind(world, 0);
        }
        if (timedBossMapMarkerService != null) {
            timedBossMapMarkerService.registerForWorld(world);
        }
    }

    private void scheduleShopRebind(World world, int attempt) {
        if (world == null || !world.isAlive() || shopConfig == null || shopConfig.shops == null || shopConfig.shops.isEmpty()) {
            return;
        }

        long delayMs = switch (attempt) {
            case 0 -> 0L;
            case 1 -> 750L;
            case 2 -> 2000L;
            default -> 5000L;
        };

        SHOP_REBIND_EXECUTOR.schedule(() -> {
            if (!world.isAlive()) {
                return;
            }
            world.execute(() -> {
                int rebound = rebindShopInteractions(world, attempt);
                if (rebound == 0 && attempt < 3) {
                    scheduleShopRebind(world, attempt + 1);
                }
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private int rebindShopInteractions(World world, int attempt) {
        if (world == null || shopConfig == null || shopConfig.shops == null || shopConfig.shops.isEmpty()) {
            return 0;
        }

        Store<EntityStore> store = world.getEntityStore() != null ? world.getEntityStore().getStore() : null;
        if (store == null) {
            return 0;
        }

        String worldName = world.getName();
        int rebound = 0;
        boolean changed = false;
        for (BossShopConfig.ShopLocation location : new ArrayList<>(shopConfig.shops)) {
            if (location == null || location.worldName == null || !location.worldName.equalsIgnoreCase(worldName)) {
                continue;
            }

            String uuidText = location.uuid != null ? location.uuid.trim() : "";
            Ref<EntityStore> ref = null;
            if (!uuidText.isEmpty()) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    ref = world.getEntityRef(uuid);
                } catch (IllegalArgumentException ignored) {
                    // malformed uuid; fall back to location lookup
                }
            }

            if (ref == null) {
                ref = findShopNpcRefNearLocation(store, location, resolveShopNpcId());
            }

            if (ref == null) {
                // On early attempts, just wait for chunks/NPCs to finish loading.
                // Only treat the shop NPC as truly missing on later retries.
                if (attempt >= 2) {
                    // If the shop NPC is still not found after multiple retries, respawn it.
                    respawnShopNpc(world, location);
                }
                continue;
            }

            if (bindShopNpcInteractionInternal(store, ref, resolveVendorDisplayName(location))) {
                rebound++;
            }

            Object uuidObj = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidObj instanceof UUIDComponent uuidComponent) {
                String resolvedUuid = uuidComponent.getUuid() != null ? uuidComponent.getUuid().toString() : "";
                if (!resolvedUuid.isEmpty() && !resolvedUuid.equalsIgnoreCase(uuidText)) {
                    location.uuid = resolvedUuid;
                    changed = true;
                }
            }
        }

        if (changed) {
            saveShopConfig();
        }

        if (rebound > 0) {
            getLogger().atInfo().log("Rebound shop interaction for " + rebound + " guard(s) in world " + worldName);
        }
        return rebound;
    }

    private static final double SHOP_NPC_SEARCH_RADIUS_BLOCKS = 4.0d;

    private Ref<EntityStore> findShopNpcRefNearLocation(Store<EntityStore> store,
                                                        BossShopConfig.ShopLocation location,
                                                        String configuredShopNpcId) {
        if (store == null || location == null) {
            return null;
        }

        final Ref<EntityStore>[] nearestRef = new Ref[]{null};
        final double[] nearestDistanceSq = new double[]{Double.MAX_VALUE};
        final double maxDistanceSq = SHOP_NPC_SEARCH_RADIUS_BLOCKS * SHOP_NPC_SEARCH_RADIUS_BLOCKS;

        store.forEachChunk(
                com.hypixel.hytale.component.query.Query.and(
                        TransformComponent.getComponentType(),
                        NPCEntity.getComponentType()
                ),
                (chunk, ignored) -> {
                    for (int i = 0; i < chunk.size(); i++) {
                        NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                        if (npc == null || !isShopNpcTypeId(npc.getNPCTypeId(), configuredShopNpcId)) {
                            continue;
                        }

                        TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
                        if (transform == null) {
                            continue;
                        }
                        org.joml.Vector3d rawPos = transform.getPosition();
                        double dx = rawPos.x - location.x;
                        double dy = rawPos.y - location.y;
                        double dz = rawPos.z - location.z;
                        double distanceSq = (dx * dx) + (dy * dy) + (dz * dz);
                        if (distanceSq > maxDistanceSq || distanceSq >= nearestDistanceSq[0]) {
                            continue;
                        }
                        nearestDistanceSq[0] = distanceSq;
                        nearestRef[0] = chunk.getReferenceTo(i);
                    }
                }
        );
        return nearestRef[0];
    }

    public void bindShopNpcInteraction(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        bindShopNpcInteractionInternal(store, entityRef, resolveVendorDisplayNameNear(store, entityRef));
    }

    public void bindShopNpcInteraction(Store<EntityStore> store, Ref<EntityStore> entityRef, String vendorName) {
        bindShopNpcInteractionInternal(store, entityRef, vendorName);
    }

    public void refreshShopNpcInteractionHint(BossShopConfig.ShopLocation location) {
        if (location == null || location.worldName == null || location.worldName.isBlank()) {
            return;
        }
        World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(location.worldName);
        if (world == null) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore() != null ? world.getEntityStore().getStore() : null;
        if (store == null) {
            return;
        }

        Ref<EntityStore> ref = null;
        String uuidText = location.uuid != null ? location.uuid.trim() : "";
        if (!uuidText.isEmpty()) {
            try {
                ref = world.getEntityRef(UUID.fromString(uuidText));
            } catch (IllegalArgumentException ignored) {
                // fall back to proximity lookup
            }
        }
        if (ref == null) {
            ref = findShopNpcRefNearLocation(store, location, resolveShopNpcId());
        }
        if (ref != null) {
            bindShopNpcInteractionInternal(store, ref, resolveVendorDisplayName(location));
        }
    }

    private boolean bindShopNpcInteractionInternal(Store<EntityStore> store,
                                                     Ref<EntityStore> entityRef,
                                                     String vendorName) {
        if (store == null || entityRef == null) {
            return false;
        }

        Object npcObj = store.getComponent(entityRef, NPCEntity.getComponentType());
        if (npcObj instanceof NPCEntity npc && !isShopNpcTypeId(npc.getNPCTypeId(), resolveShopNpcId())) {
            return false;
        }

        String displayName = vendorName != null && !vendorName.isBlank()
                ? vendorName.trim()
                : resolveVendorDisplayNameNear(store, entityRef);

        store.ensureComponent(entityRef, Interactable.getComponentType());
        Interactions interactions = store.ensureAndGetComponent(entityRef, Interactions.getComponentType());
        interactions.setInteractionId(InteractionType.Use, SHOP_OPEN_INTERACTION_ID);
        interactions.setInteractionHint("Parle au vendeur " + displayName);
        return true;
    }

    @Nonnull
    private static String resolveVendorDisplayName(@Nullable BossShopConfig.ShopLocation location) {
        if (location != null && location.name != null && !location.name.isBlank()) {
            return location.name.trim();
        }
        return "Vendeur";
    }

    @Nonnull
    private String resolveVendorDisplayNameNear(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (store == null || entityRef == null || shopConfig == null || shopConfig.shops == null) {
            return "Vendeur";
        }
        Object transformObj = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (!(transformObj instanceof TransformComponent transform)) {
            return "Vendeur";
        }
        org.joml.Vector3d pos = transform.getPosition();
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);

        BossShopConfig.ShopLocation best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BossShopConfig.ShopLocation location : shopConfig.shops) {
            if (location == null) {
                continue;
            }
            double dx = location.x - x;
            double dy = location.y - y;
            double dz = location.z - z;
            double distSq = (dx * dx) + (dy * dy) + (dz * dz);
            if (distSq <= (SHOP_NPC_SEARCH_RADIUS_BLOCKS * SHOP_NPC_SEARCH_RADIUS_BLOCKS) && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = location;
            }
        }
        return resolveVendorDisplayName(best);
    }

    private String resolveShopNpcId() {
        if (shopConfig != null && shopConfig.shopNpcId != null && !shopConfig.shopNpcId.isBlank()) {
            return shopConfig.shopNpcId.trim();
        }
        return SHOP_NPC_TYPE_ID;
    }

    private void openShopPage(Ref<EntityStore> playerRef,
                              Store<EntityStore> store,
                              Player player,
                              TransformComponent targetTransform) {
        if (targetTransform == null) {
            BossArenaShopPage.open(playerRef, store, player, this);
            return;
        }

        org.joml.Vector3d npcPosition = targetTransform.getPosition();
        Vector3i shopAnchor = new Vector3i(
                (int) Math.floor(npcPosition.x),
                (int) Math.floor(npcPosition.y),
                (int) Math.floor(npcPosition.z)
        );

        String worldName = player.getWorld() != null ? player.getWorld().getName() : null;
        if (worldName != null && !worldName.isBlank()) {
            recordShopLocation(worldName, shopAnchor);
            BossArenaShopPage.openAtTable(
                    playerRef,
                    store,
                    player,
                    this,
                    worldName,
                    shopAnchor.x,
                    shopAnchor.y,
                    shopAnchor.z
            );
            return;
        }
        BossArenaShopPage.open(playerRef, store, player, this);
    }

    private void startBossArenaSystems() {
        try {
            getLogger().atInfo().log("Starting BossArena systems...");

            if (fightMusicManager != null) {
                fightMusicManager.start();
            }

            BossLootHandler.initializePersistence(lootChestStatePath);
            getLogger().atInfo().log("Loot chest persistence initialized at " + lootChestStatePath);
            trackingSystem.initializePersistence(bossFightStatePath);
            trackingSystem.setMissingEntityHandler(new MissingEntityRestorationHandler());
            getLogger().atInfo().log("Boss fight persistence initialized at " + bossFightStatePath);
            if (timedSpawnScheduler != null) {
                timedSpawnScheduler.initializePersistence(timedSpawnStatePath);
                getLogger().atInfo().log("Timed spawn persistence initialized at " + timedSpawnStatePath);
            }

            if (Files.notExists(bossesJsonPath)) {
                writeDefaultBosses();
            }

            try {
                if (Files.notExists(lootTablesPath)) {
                    LootRegistry.createDefaults();
                    LootRegistry.saveToFile(lootTablesPath);
                    getLogger().atInfo().log("Created default loot_tables.json");
                } else {
                    LootRegistry.loadFromFile(lootTablesPath);
                }
            } catch (IOException e) {
                getLogger().atWarning().withCause(e).log("Failed to load loot tables");
                LootRegistry.createDefaults();
            }
            getLogger().atInfo().log("Loot system initialized with " + LootRegistry.size() + " loot tables");

            reloadBossDefinitions().thenRun(() -> {
                reloadArenas().thenRun(() -> {
                    if (BossDefinition.migrateProximityToArenas()) {
                        saveBossDefinitions();
                        saveArenas();
                        getLogger().atInfo().log("Migrated legacy boss proximity settings onto arenas.");
                    }
                    refreshTimedBossSpawns();
                    if (timedBossMapMarkerService != null) {
                        timedBossMapMarkerService.registerForAllWorlds();
                    }
                    getLogger().atInfo().log("BossArena fully initialized: " +
                            BossRegistry.size() + " bosses, " +
                            ArenaRegistry.size() + " arenas");
                });
            }).exceptionally(err -> {
                getLogger().atSevere().withCause(err).log("Failed to load boss definitions");
                return null;
            });

        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("Initialization failed for BossArena");
        }
    }

    @Override
    protected void shutdown() {
        handleShutdown();

        try {
            // Shutdown all executors and services.
            if (SHOP_REBIND_EXECUTOR != null) {
                SHOP_REBIND_EXECUTOR.shutdown();
                try {
                    if (!SHOP_REBIND_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                        SHOP_REBIND_EXECUTOR.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    SHOP_REBIND_EXECUTOR.shutdownNow();
                }
            }

            if (bossSpawnService != null) {
                bossSpawnService.shutdown();
            }

            if (timedSpawnScheduler != null) {
                timedSpawnScheduler.shutdown();
            }

            if (fightMusicManager != null) {
                fightMusicManager.shutdown();
            }

            if (timedBossMapMarkerService != null) {
                timedBossMapMarkerService.clearAllMarkers();
            }

            getLogger().atInfo().log("BossArena disabled and mod entities cleaned up.");
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("Failed to safely shutdown BossArena");
        }
    }

    private void handleShutdown() {
        if (!cleanedUp.compareAndSet(false, true)) {
            getLogger().atInfo().log("handleShutdown() called but already cleaned up.");
            return;
        }

        getLogger().atInfo().log("handleShutdown() sequence initiated...");
        try {
            // 1. Save all current state BEFORE removing entities.
            // This ensures we know what to restore on the next startup.
            if (trackingSystem != null) {
                trackingSystem.shutdownPersistence();
            }
            if (shopConfig != null) {
                saveShopConfig().join();
            }
            BossLootHandler.flushPersistence();

            // 2. Automated Cleanup for Safe Uninstallation.
            // Remove mod-specific entities (bosses, shops) from all worlds.
            cleanupModEntities();

            getLogger().atInfo().log("BossArena cleanup completed.");
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("Failed to safely shutdown BossArena");
        }
    }

    private void respawnShopNpc(World world, BossShopConfig.ShopLocation location) {
        if (world == null || location == null) return;

        String shopNpcId = resolveShopNpcId();
        int baseX = location.x;
        int baseY = location.y;
        int baseZ = location.z;
        double spawnY = baseY;
        try {
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType at =
                    world.getBlockType(baseX, baseY, baseZ);
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType below =
                    world.getBlockType(baseX, baseY - 1, baseZ);
            if ((at != null && at.getId() != null && at.getId().toLowerCase(java.util.Locale.ROOT).contains("snow"))
                    || (below != null && below.getId() != null && below.getId().toLowerCase(java.util.Locale.ROOT).contains("snow"))) {
                // If the saved anchor is on or just above snow, lift the spawn Y so the guard
                // doesn't sink one block lower than when it was placed.
                spawnY = baseY + 1.0d;
            }
        } catch (Exception ignored) {
            // If anything goes wrong, fall back to the raw saved Y.
        }

        org.joml.Vector3d spawnPos = new org.joml.Vector3d(baseX + 0.5d, spawnY, baseZ + 0.5d);
        // Face south by default for respawn if we don't know the original rotation.
        com.hypixel.hytale.math.vector.Rotation3f rotation = new com.hypixel.hytale.math.vector.Rotation3f(0, (float) Math.PI, 0);

        world.execute(() -> {
            // Best-effort cleanup: remove any existing shop NPCs of this type very close to the saved location
            // so we never stack multiple guards at the same shop.
            Store<EntityStore> store = world.getEntityStore() != null ? world.getEntityStore().getStore() : null;
            if (store != null) {
                store.forEachChunk(
                        com.hypixel.hytale.component.query.Query.and(
                                TransformComponent.getComponentType(),
                                NPCEntity.getComponentType()
                        ),
                        (chunk, ignored) -> {
                            for (int i = 0; i < chunk.size(); i++) {
                                NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                                if (npc == null || !isShopNpcTypeId(npc.getNPCTypeId(), shopNpcId)) {
                                    continue;
                                }
                                TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
                                if (transform == null) {
                                    continue;
                                }
                                org.joml.Vector3d rawNpcPos = transform.getPosition();
                                double dx = rawNpcPos.x - location.x;
                                double dy = rawNpcPos.y - location.y;
                                double dz = rawNpcPos.z - location.z;
                                double distanceSq = (dx * dx) + (dy * dy) + (dz * dz);
                                double maxSq = SHOP_NPC_SEARCH_RADIUS_BLOCKS * SHOP_NPC_SEARCH_RADIUS_BLOCKS;
                                if (distanceSq > maxSq) {
                                    continue;
                                }
                                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                                try {
                                    world.getEntityStore().getStore().removeEntity(ref, com.hypixel.hytale.component.RemoveReason.REMOVE);
                                } catch (Exception ignoredRemoval) {
                                    // Best-effort; if removal fails we still attempt to spawn a fresh guard.
                                }
                            }
                        }
                );
            }

            var result = com.hypixel.hytale.server.npc.NPCPlugin.get().spawnNPC(
                    world.getEntityStore().getStore(),
                    shopNpcId,
                    null,
                    spawnPos,
                    rotation
            );

            if (result != null) {
                bindShopNpcInteraction(world.getEntityStore().getStore(), result.first(), resolveVendorDisplayName(location));
                Object uuidObj = world.getEntityStore().getStore().getComponent(result.first(), UUIDComponent.getComponentType());
                if (uuidObj instanceof UUIDComponent uuidComp) {
                    location.uuid = uuidComp.getUuid() != null ? uuidComp.getUuid().toString() : "";
                    saveShopConfig();
                }
                getLogger().atInfo().log("Respawned missing shop NPC at " + location.x + ", " + location.y + ", " + location.z);
            }
        });
    }

    private void cleanupModEntities() {
        getLogger().atInfo().log("Starting automated cleanup of mod entities for all worlds...");
        BossTrackingSystem tracking = getTrackingSystem();
        java.util.Set<UUID> trackedUuids = new java.util.HashSet<>();
        if (tracking != null) {
            trackedUuids.addAll(tracking.snapshotTrackedBosses().keySet());
            trackedUuids.addAll(tracking.snapshotTrackedAdds().keySet());
        }

        com.hypixel.hytale.server.core.universe.Universe universe = com.hypixel.hytale.server.core.universe.Universe.get();
        if (universe == null) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (World world : universe.getWorlds().values()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            futures.add(future);

            world.execute(() -> {
                getLogger().atInfo().log("Executing cleanup for world: " + world.getName());
                try {
                    int removedCount = BossArenaCleanup.removeBossArenaEntitiesInWorld(world, trackedUuids, true);
                    if (removedCount > 0) {
                        getLogger().atInfo().log("Cleaned up " + removedCount + " mod entities in world: " + world.getName());
                    } else {
                        getLogger().atInfo().log("No mod entities found for cleanup in world: " + world.getName());
                    }
                    future.complete(null);
                } catch (Exception e) {
                    getLogger().atSevere().withCause(e).log("Cleanup failed for world: " + world.getName());
                    future.completeExceptionally(e);
                }
            });
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);
            getLogger().atInfo().log("Mod entity and chest cleanup completed successfully.");
        } catch (Exception e) {
            getLogger().atWarning().log("Cleanup did not complete in time: " + e.getMessage());
        }
    }

    public BossSpawnService getBossSpawnService() {
        return bossSpawnService;
    }

    /** @deprecated Use {@link #getConfig()} instead. */
    @Deprecated
    public BossArenaConfig cfg() {
        return config;
    }

    public BossShopConfig getShopConfig() {
        return shopConfig;
    }

    public void reloadShopConfig() {
        shopConfig.load(shopJsonPath);
        if (shopConfig.applyRuntimeCurrencyDetection(resolveFallbackCurrencyItemId())) {
            saveShopConfig();
        }
    }

    private String resolveFallbackCurrencyItemId() {
        if (config == null) {
            return "Ingredient_Bar_Iron";
        }
        if (config.fallbackCurrencyItemId != null && !config.fallbackCurrencyItemId.isBlank()) {
            return config.fallbackCurrencyItemId.trim();
        }
        return "Ingredient_Bar_Iron";
    }

    public CompletableFuture<Void> saveShopConfig() {
        return CompletableFuture.runAsync(() -> {
            try {
                shopConfig.save(shopJsonPath);
                getLogger().atInfo().log("Saved shop config");
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to save shop config");
                throw new RuntimeException(e);
            }
        });
    }

    public void recordShopLocation(String worldName, Vector3i position) {
        recordShopLocation(worldName, position, null);
    }

    public void recordShopLocation(String worldName, Vector3i position, UUID uuid) {
        if (position == null || worldName == null || worldName.isBlank()) {
            return;
        }
        if (shopConfig == null) {
            return;
        }
        boolean changed = shopConfig.recordShopLocation(
                uuid != null ? uuid.toString() : "",
                worldName,
                position.x,
                position.y,
                position.z
        );
        if (!changed) {
            return;
        }
        saveShopConfig();
    }

    public void recordShopNpcUuid(UUID uuid) {
        if (uuid == null || shopConfig == null) {
            return;
        }
        boolean changed = shopConfig.recordShopNpcUuid(uuid.toString());
        if (changed) {
            saveShopConfig();
        }
    }

    /** @deprecated Use {@link #getConfig()} instead. */
    @Deprecated
    public BossArenaConfig getConfigHandle() {
        return config;
    }

    /** Returns the main BossArena config. */
    public BossArenaConfig getConfig() {
        return config;
    }

    public void refreshTimedBossSpawns() {
        if (timedSpawnScheduler == null) {
            return;
        }
        timedSpawnScheduler.reloadFromConfig(config);
        timedSpawnScheduler.start();
        if (timedBossMapMarkerService != null) {
            timedBossMapMarkerService.registerForAllWorlds();
        }
    }

    private void disableTimedRuleOneShot(BossArenaConfig.TimedBossSpawn match) {
        if (config == null || match == null) {
            return;
        }
        String bossId = match.bossId == null ? "" : match.bossId.trim();
        String arenaId = match.arenaId == null ? "" : match.arenaId.trim();
        List<BossArenaConfig.TimedBossSpawn> rows = config.getTimedBossSpawns();
        boolean changed = false;
        for (BossArenaConfig.TimedBossSpawn rule : rows) {
            if (rule == null || !rule.enabled || !rule.isFixedTimesMode()) {
                continue;
            }
            String ruleBoss = rule.bossId == null ? "" : rule.bossId.trim();
            String ruleArena = rule.arenaId == null ? "" : rule.arenaId.trim();
            if (bossId.equalsIgnoreCase(ruleBoss) && arenaId.equalsIgnoreCase(ruleArena)) {
                rule.enabled = false;
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }
        config.timedBossSpawns = rows;
        config.save();
        refreshTimedBossSpawns();
    }

    public BossTimedSpawnScheduler getTimedSpawnScheduler() {
        return timedSpawnScheduler;
    }

    public Path getLootTablesPath() {
        return lootTablesPath;
    }

    public Path getBossesJsonPath() {
        return bossesJsonPath;
    }

    public CompletableFuture<Integer> reloadBossDefinitions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                getLogger().atInfo().log("Loading boss definitions from: " + bossesJsonPath);

                Gson gson = new Gson();
                String json = Files.readString(bossesJsonPath, StandardCharsets.UTF_8);
                BossDefinition[] bosses = gson.fromJson(json, BossDefinition[].class);

                BossRegistry.clear();

                if (bosses != null) {
                    for (BossDefinition def : bosses) {
                        BossRegistry.register(def);
                        getLogger().atInfo().log("Registered boss: " + def.bossName);
                    }
                }

                getLogger().atInfo().log("Loaded " + BossRegistry.size() + " boss definitions");
                return BossRegistry.size();
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to load boss definitions");
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Integer> reloadArenas() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.notExists(arenasJsonPath)) {
                    Files.createDirectories(arenasJsonPath.getParent());
                    Files.writeString(arenasJsonPath, "[]", StandardCharsets.UTF_8);
                    getLogger().atInfo().log("Created empty arenas.json");
                    return 0;
                }

                getLogger().atInfo().log("Loading arenas from: " + arenasJsonPath);

                Gson gson = new Gson();
                String json = Files.readString(arenasJsonPath, StandardCharsets.UTF_8);
                Arena[] arenas = gson.fromJson(json, Arena[].class);

                ArenaRegistry.clear();

                if (arenas != null) {
                    for (Arena arena : arenas) {
                        ArenaRegistry.register(arena);
                        getLogger().atInfo().log("Registered arena: " + arena.arenaId);
                    }
                }

                getLogger().atInfo().log("Loaded " + ArenaRegistry.size() + " arenas");
                return ArenaRegistry.size();
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to load arenas");
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> saveArenas() {
        return CompletableFuture.runAsync(() -> {
            try {
                Collection<Arena> arenas = ArenaRegistry.getAll();
                String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(arenas);
                Files.createDirectories(arenasJsonPath.getParent());
                Files.writeString(arenasJsonPath, prettyJson, StandardCharsets.UTF_8);
                getLogger().atInfo().log("Saved " + arenas.size() + " arenas");
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to save arenas");
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Integer> reloadLootTables() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.notExists(lootTablesPath)) {
                    LootRegistry.createDefaults();
                    LootRegistry.saveToFile(lootTablesPath);
                    return LootRegistry.size();
                }
                return LootRegistry.loadFromFile(lootTablesPath);
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to load loot tables");
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> saveLootTables() {
        return CompletableFuture.runAsync(() -> {
            try {
                LootRegistry.saveToFile(lootTablesPath);
                getLogger().atInfo().log("Saved " + LootRegistry.size() + " loot tables");
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to save loot tables");
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> saveBossDefinitions() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<BossDefinition> bosses = new ArrayList<>(BossRegistry.getAll().values());
                String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(bosses);
                Files.createDirectories(bossesJsonPath.getParent());
                Files.writeString(bossesJsonPath, prettyJson, StandardCharsets.UTF_8);
                getLogger().atInfo().log("Saved " + bosses.size() + " boss definitions");
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Failed to save boss definitions");
                throw new RuntimeException(e);
            }
        });
    }

    public void deletePersistentState() {
        try {
            Files.deleteIfExists(lootChestStatePath);
            Files.deleteIfExists(bossFightStatePath);
            Files.deleteIfExists(timedSpawnStatePath);
            getLogger().atInfo().log("Deleted persistent runtime state files (fights, loot chests, timed spawns).");
            getLogger().atInfo().log("User configuration files (bosses, arenas, loot tables, shop) were preserved.");
        } catch (IOException e) {
            getLogger().atSevere().withCause(e).log("Failed to delete some persistent state files");
        }
    }

    private void writeDefaultBosses() throws IOException {
        BossDefinition exampleBoss = new BossDefinition();
        exampleBoss.bossName = "Example Boss";
        exampleBoss.npcId = "Bat";
        exampleBoss.tier = "common";
        exampleBoss.amount = 1;
        exampleBoss.levelOverride = 0;

        exampleBoss.modifiers = new BossDefinition.Modifiers();
        exampleBoss.modifiers.hp = 2.0f;
        exampleBoss.modifiers.damage = 1.5f;
        exampleBoss.modifiers.movementSpeed = 1.0f;
        exampleBoss.modifiers.size = 1.0f;
        exampleBoss.modifiers.attackRate = 1.0f;
        exampleBoss.modifiers.abilityCooldown = 1.0f;
        exampleBoss.modifiers.knockbackGiven = 1.0f;
        exampleBoss.modifiers.knockbackTaken = 1.0f;
        exampleBoss.modifiers.turnRate = 1.0f;
        exampleBoss.modifiers.regen = 0.0f;

        exampleBoss.perPlayerIncrease = new BossDefinition.PerPlayerIncrease();
        exampleBoss.perPlayerIncrease.hp = 1.0f;
        exampleBoss.perPlayerIncrease.damage = 1.0f;
        exampleBoss.perPlayerIncrease.movementSpeed = 1.0f;
        exampleBoss.perPlayerIncrease.size = 1.0f;
        exampleBoss.perPlayerIncrease.attackRate = 1.0f;
        exampleBoss.perPlayerIncrease.abilityCooldown = 1.0f;
        exampleBoss.perPlayerIncrease.knockbackGiven = 1.0f;
        exampleBoss.perPlayerIncrease.knockbackTaken = 1.0f;
        exampleBoss.perPlayerIncrease.turnRate = 1.0f;
        exampleBoss.perPlayerIncrease.regen = 0.0f;

        exampleBoss.extraMobs = new BossDefinition.ExtraMobs();
        exampleBoss.extraMobs.npcId = "Bat";
        exampleBoss.extraMobs.timeLimitMs = 30000;
        exampleBoss.extraMobs.waves = 2;
        exampleBoss.extraMobs.mobsPerWave = 5;
        BossDefinition.ExtraMobs.WaveAdd exampleAdd = new BossDefinition.ExtraMobs.WaveAdd();
        exampleAdd.npcId = "Bat";
        exampleAdd.mobsPerWave = 5;
        exampleAdd.everyWave = 1;
        exampleBoss.extraMobs.adds.add(exampleAdd);
        exampleBoss.extraMobs.sanitize();

        BossDefinition[] bosses = new BossDefinition[]{exampleBoss};

        String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(bosses);
        Files.createDirectories(bossesJsonPath.getParent());
        Files.writeString(bossesJsonPath, prettyJson, StandardCharsets.UTF_8);
        getLogger().atInfo().log("Created default bosses.json");
    }

    private final class MissingEntityRestorationHandler implements BossTrackingSystem.MissingEntityHandler {
        @Override
        public boolean handleMissingBoss(BossTrackingSystem.PersistedBoss persisted) {
            if (persisted == null || bossSpawnService == null) return false;

            World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(persisted.world);
            if (world == null) return false;

            BossDefinition def = BossRegistry.get(persisted.bossName);
            if (def == null) return false;

            Vector3d spawnPos = new Vector3d(persisted.spawnX, persisted.spawnY, persisted.spawnZ);
            com.varyon.bossarena.boss.BossModifiers mods = new com.varyon.bossarena.boss.BossModifiers(
                    persisted.hpMultiplier, persisted.damageMultiplier, persisted.speedMultiplier,
                    persisted.scaleMultiplier, persisted.attackRateMultiplier, persisted.abilityCooldownMultiplier,
                    persisted.knockbackGivenMultiplier, persisted.knockbackTakenMultiplier, persisted.turnRateMultiplier,
                    persisted.regenMultiplier
            );

            UUID eventId = null;
            try {
                eventId = UUID.fromString(persisted.eventId);
            } catch (Exception ignored) {
            }

            getLogger().atInfo().log("Restoring missing boss: " + persisted.bossName + " at " + spawnPos);

            // This is now called on the World Thread via retryPendingRestore(world)
            bossSpawnService.spawnBossNow(
                    world,
                    def,
                    spawnPos,
                    persisted.arenaId,
                    mods,
                    0,
                    new java.util.ArrayList<>(),
                    new java.util.ArrayList<>(),
                    new java.util.concurrent.atomic.AtomicInteger(0),
                    eventId,
                    null
            );

            return true;
        }

        @Override
        public boolean handleMissingAdd(BossTrackingSystem.PersistedAddLink persisted) {
            return false;
        }
    }
}
