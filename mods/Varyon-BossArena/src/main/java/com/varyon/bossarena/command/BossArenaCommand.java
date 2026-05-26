package com.varyon.bossarena.command;

import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.config.BossArenaConfigPage;
import com.varyon.bossarena.util.BossArenaCleanup;
import com.varyon.bossarena.util.VecUtil;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.data.BossDefinition;
import com.varyon.bossarena.data.BossRegistry;
import com.varyon.bossarena.spawn.BossSpawnService;
import com.varyon.bossarena.system.BossTrackingSystem;
import com.varyon.bossarena.shop.BossArenaShopPage;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.npc.NPCPlugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ArchetypeChunk;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BossArenaCommand extends AbstractCommand {

    public static final String ADMIN_PERMISSION = "bossarena.admin";
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private final BossArenaPlugin plugin;

    public BossArenaCommand(BossArenaPlugin plugin) {
        super("bossarena", "BossArena admin commands");
        this.plugin = plugin;
        requireAdminPermission(this);

        addSubCommand(new ArenaRoot(plugin));
        addSubCommand(new SpawnBoss(plugin));
        addSubCommand(new Reload(plugin));
        addSubCommand(new Config(plugin));
        addSubCommand(new ShopRoot(plugin));
        addSubCommand(new Cleanup(plugin));
    }

    private static void requireAdminPermission(AbstractCommand command) {
        if (command != null) {
            command.requirePermission(ADMIN_PERMISSION);
        }
    }

    /** Uses reflection to bridge CommandContext/Player APIs that may differ across Hytale versions. */
    @SuppressWarnings("JavaReflectionMemberAccess")
    private static CommandSender getSender(CommandContext ctx) {
        if (ctx == null) return null;
        try {
            Method m = ctx.getClass().getMethod("getSender");
            Object o = m.invoke(ctx);
            if (o instanceof CommandSender cs) return cs;
        } catch (Throwable ignored) {
        }

        try {
            if (ctx.isPlayer()) {
                com.hypixel.hytale.component.Ref<EntityStore> ref = ctx.senderAsPlayerRef();
                if (ref != null) {
                    Object pr = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                    if (pr instanceof PlayerRef playerRef) return playerRef;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Uses reflection to resolve PlayerRef and transform for position; fallback (0,0,0) on API mismatch. */
    @SuppressWarnings("SpellCheckingInspection")
    private static Vector3d getPlayerPosition(Player player) {
        if (player == null) return new Vector3d(0, 0, 0);

        try {
            Method getPlayerRef = player.getClass().getMethod("getPlayerRef");
            Object refObj = getPlayerRef.invoke(player);
            PlayerRef playerRef = refObj instanceof PlayerRef ? (PlayerRef) refObj : null;
            if (playerRef != null) {
                Transform transform = playerRef.getTransform();
                if (transform != null) {
                    org.joml.Vector3d position = transform.getPosition();
                    return VecUtil.toJoml(position);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to get player position via reflection", t);
        }

        LOGGER.fine("Falling back to default position (0,0,0)");
        return new Vector3d(0, 0, 0);
    }

    /** Uses reflection to resolve PlayerRef and transform for rotation; fallback (0,0,0) on API mismatch. */
    @SuppressWarnings("SpellCheckingInspection")
    private static com.hypixel.hytale.math.vector.Rotation3f getPlayerRotation(Player player) {
        if (player == null) return new com.hypixel.hytale.math.vector.Rotation3f(0, 0, 0);

        try {
            Method getPlayerRef = player.getClass().getMethod("getPlayerRef");
            Object refObj = getPlayerRef.invoke(player);
            PlayerRef playerRef = refObj instanceof PlayerRef ? (PlayerRef) refObj : null;
            if (playerRef != null) {
                Transform transform = playerRef.getTransform();
                if (transform != null && transform.getRotation() != null) {
                    return transform.getRotation();
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to get player rotation via reflection", t);
        }

        LOGGER.fine("Falling back to default rotation (0,0,0)");
        return new com.hypixel.hytale.math.vector.Rotation3f(0, 0, 0);
    }

    private static CompletableFuture<Void> spawnShopNpc(@Nonnull CommandContext ctx, BossArenaPlugin plugin) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Player-only command"));
            return CompletableFuture.completedFuture(null);
        }

        com.hypixel.hytale.component.Ref<EntityStore> _shopPlayerRef = ctx.senderAsPlayerRef();
        if (_shopPlayerRef == null) {
            ctx.sendMessage(Message.raw("Could not resolve player"));
            return CompletableFuture.completedFuture(null);
        }
        PlayerRef _shopPR = _shopPlayerRef.getStore().getComponent(_shopPlayerRef, PlayerRef.getComponentType());
        World world = _shopPR != null ? Universe.get().getWorld(_shopPR.getWorldUuid()) : null;
        Player player = _shopPlayerRef.getStore().getComponent(_shopPlayerRef, Player.getComponentType());
        if (world == null) {
            ctx.sendMessage(Message.raw("Could not resolve player world"));
            return CompletableFuture.completedFuture(null);
        }

        Vector3d playerPosition = getPlayerPosition(player);
        com.hypixel.hytale.math.vector.Rotation3f playerRotation = getPlayerRotation(player);
        float playerYaw = playerRotation.yaw();
        if (Float.isNaN(playerYaw)) {
            playerYaw = 0f;
        }

        // Spawn exactly 2 blocks forward from the player's facing direction (horizontal plane).
        org.joml.Vector3d forward = Transform.getDirection(0f, playerYaw);
        org.joml.Vector3d spawnPosition = new org.joml.Vector3d(
                playerPosition.x + (forward.x * 2.0d),
                playerPosition.y,
                playerPosition.z + (forward.z * 2.0d)
        );

        // Make the guard face back toward the player.
        float npcYaw = playerYaw + (float) Math.PI;
        com.hypixel.hytale.math.vector.Rotation3f npcRotation = new com.hypixel.hytale.math.vector.Rotation3f(0f, npcYaw, 0f);
        String shopNpcId = plugin.getShopConfig() != null
                && plugin.getShopConfig().shopNpcId != null
                && !plugin.getShopConfig().shopNpcId.isBlank()
                ? plugin.getShopConfig().shopNpcId.trim()
                : BossArenaPlugin.SHOP_NPC_TYPE_ID;

        world.execute(() -> {
            var result = NPCPlugin.get().spawnNPC(
                    world.getEntityStore().getStore(),
                    shopNpcId,
                    null,
                    spawnPosition,
                    npcRotation
            );

            if (result == null) {
                ctx.sendMessage(Message.raw("Failed to spawn shop NPC (" + shopNpcId + ")."));
                return;
            }

            Store<EntityStore> store = world.getEntityStore().getStore();
            plugin.bindShopNpcInteraction(store, result.first());

            Object uuidCompObj = store.getComponent(result.first(), UUIDComponent.getComponentType());
            UUID spawnedUuid = null;
            if (uuidCompObj instanceof UUIDComponent uuidComp) {
                spawnedUuid = uuidComp.getUuid();
            }

            int x = (int) Math.floor(spawnPosition.x);
            int y = (int) Math.floor(spawnPosition.y);
            int z = (int) Math.floor(spawnPosition.z);
            plugin.recordShopLocation(
                    world.getName(),
                    new Vector3i(x, y, z),
                    spawnedUuid
            );
            ctx.sendMessage(Message.raw(
                    "Spawned shop NPC (" + shopNpcId + ") at "
                            + x + ", " + y + ", " + z + " in " + world.getName()
            ));
        });
        return CompletableFuture.completedFuture(null);
    }

    private static UUID findNearestShopNpcUuid(BossArenaPlugin plugin, World world, Vector3d origin) {
        if (plugin == null || plugin.getShopConfig() == null || plugin.getShopConfig().shops == null) {
            return null;
        }

        UUID nearest = null;
        double bestDistSq = Double.MAX_VALUE;

        for (var location : plugin.getShopConfig().shops) {
            if (location == null || location.uuid == null || location.uuid.isBlank()) {
                continue;
            }
            String rawUuid = location.uuid;
            if (rawUuid == null || rawUuid.isBlank()) {
                continue;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(rawUuid.trim());
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            Ref<EntityStore> entityRef = world.getEntityRef(uuid);
            Store<EntityStore> wstore = world.getEntityStore().getStore();
            if (entityRef == null) {
                continue;
            }
            Object transformObj = wstore.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (!(transformObj instanceof com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform)) {
                continue;
            }

            org.joml.Vector3d rawPos = transform.getPosition();
            double dx = rawPos.x - origin.x;
            double dy = rawPos.y - origin.y;
            double dz = rawPos.z - origin.z;
            double distSq = (dx * dx) + (dy * dy) + (dz * dz);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = uuid;
            }
        }

        return nearest;
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw(
                "Use: /bossarena arena <create|delete|list> OR /bossarena spawn <bossId> <arenaId|here> OR /bossarena reload OR /bossarena config OR /bossarena shop <open|place|delete> OR /bossarena cleanup"
        ));
        return CompletableFuture.completedFuture(null);
    }

    // ============================================================
    // /bossarena arena ...
    // ============================================================
    static final class ArenaRoot extends AbstractCommand {

        ArenaRoot(BossArenaPlugin plugin) {
            super("arena", "Arena management");
            requireAdminPermission(this);
            addSubCommand(new ArenaCreate(plugin));
            addSubCommand(new ArenaDelete(plugin));
            addSubCommand(new ArenaList(plugin));
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            ctx.sendMessage(Message.raw("Use: /bossarena arena <create|delete|list>"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ArenaCreate extends AbstractCommand {
        private final BossArenaPlugin plugin;
        private final RequiredArg<String> idArg;

        ArenaCreate(BossArenaPlugin plugin) {
            super("create", "Create arena at your location: /bossarena arena create <arenaId>");
            this.plugin = plugin;
            requireAdminPermission(this);
            this.idArg = withRequiredArg("arenaId", "Unique arena identifier", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Only players can create arenas"));
                return CompletableFuture.completedFuture(null);
            }

            com.hypixel.hytale.component.Ref<EntityStore> _createRef = ctx.senderAsPlayerRef();
            PlayerRef _createPR = _createRef != null ? _createRef.getStore().getComponent(_createRef, PlayerRef.getComponentType()) : null;
            Player player = _createRef != null ? _createRef.getStore().getComponent(_createRef, Player.getComponentType()) : null;
            String arenaId = ctx.get(idArg);

            if (arenaId == null || arenaId.isBlank()) {
                ctx.sendMessage(Message.raw("Arena ID cannot be empty"));
                return CompletableFuture.completedFuture(null);
            }

            if (ArenaRegistry.exists(arenaId)) {
                ctx.sendMessage(Message.raw("Arena '" + arenaId + "' already exists!"));
                return CompletableFuture.completedFuture(null);
            }

            World world = _createPR != null ? Universe.get().getWorld(_createPR.getWorldUuid()) : null;
            if (world == null) {
                ctx.sendMessage(Message.raw("Could not resolve player world"));
                return CompletableFuture.completedFuture(null);
            }

            Vector3d position = BossArenaCommand.getPlayerPosition(player);
            String worldName = world.getName(); // Instead of world.getWorldConfig().getName()

            Arena arena = new Arena(arenaId, worldName, position);
            ArenaRegistry.register(arena);

            plugin.saveArenas().thenRun(() -> {
                ctx.sendMessage(Message.raw("✓ Created arena '" + arenaId + "' at your location"));
                ctx.sendMessage(Message.raw(String.format("  Position: %.1f, %.1f, %.1f in %s",
                        position.x, position.y, position.z, worldName)));
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ArenaDelete extends AbstractCommand {
        private final BossArenaPlugin plugin;
        private final RequiredArg<String> idArg;

        ArenaDelete(BossArenaPlugin plugin) {
            super("delete", "Delete an arena: /bossarena arena delete <arenaId>");
            this.plugin = plugin;
            requireAdminPermission(this);
            this.idArg = withRequiredArg("arenaId", "Arena to delete", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            String arenaId = ctx.get(idArg);

            if (arenaId == null || arenaId.isBlank()) {
                ctx.sendMessage(Message.raw("Usage: /bossarena arena delete <arenaId>"));
                return CompletableFuture.completedFuture(null);
            }

            if (!ArenaRegistry.exists(arenaId)) {
                ctx.sendMessage(Message.raw("Arena '" + arenaId + "' does not exist!"));
                return CompletableFuture.completedFuture(null);
            }

            Arena removed = ArenaRegistry.remove(arenaId);

            plugin.saveArenas().thenRun(() -> {
                ctx.sendMessage(Message.raw("✓ Deleted arena '" + arenaId + "'"));
                if (removed != null) {
                    ctx.sendMessage(Message.raw("  Was at: " + removed.toString()));
                }
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ArenaList extends AbstractCommand {
        ArenaList(BossArenaPlugin plugin) {
            super("list", "List all arenas: /bossarena arena list");
            requireAdminPermission(this);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            Collection<Arena> arenas = ArenaRegistry.getAll();

            if (arenas.isEmpty()) {
                ctx.sendMessage(Message.raw("No arenas registered"));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sendMessage(Message.raw("=== Registered Arenas (" + arenas.size() + ") ==="));
            for (Arena arena : arenas) {
                ctx.sendMessage(Message.raw("  • " + arena.toString()));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    // ============================================================
    // /bossarena spawn <bossId>
    // ============================================================
    static final class SpawnBoss extends AbstractCommand {
        private final BossArenaPlugin plugin;
        private final RequiredArg<String> bossIdArg;
        private final RequiredArg<String> locationArg;  // Changed to required

        SpawnBoss(BossArenaPlugin plugin) {
            super("spawn", "Spawn a boss: /spawn <bossId> <arena|here>");
            this.plugin = plugin;
            requireAdminPermission(this);

            this.bossIdArg = withRequiredArg("bossId", "Boss ID to spawn", ArgTypes.STRING);
            this.locationArg = withRequiredArg("location", "Arena ID or 'here' for current location", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("This command can only be used by players."));
                return CompletableFuture.completedFuture(null);
            }

            com.hypixel.hytale.component.Ref<EntityStore> _spawnRef = ctx.senderAsPlayerRef();
            PlayerRef _spawnPR = _spawnRef != null ? _spawnRef.getStore().getComponent(_spawnRef, PlayerRef.getComponentType()) : null;
            Player player = _spawnRef != null ? _spawnRef.getStore().getComponent(_spawnRef, Player.getComponentType()) : null;
            String bossId = ctx.get(bossIdArg);
            String location = ctx.get(locationArg);

            World world = _spawnPR != null ? Universe.get().getWorld(_spawnPR.getWorldUuid()) : null;
            Vector3d spawnPos = BossArenaCommand.getPlayerPosition(player);
            BossDefinition def = BossRegistry.get(bossId);

            if (def == null) {
                ctx.sendMessage(Message.raw("§cBoss '" + bossId + "' not found in registry."));
                return CompletableFuture.completedFuture(null);
            }

            if (def.npcId == null || def.npcId.isBlank()) {
                ctx.sendMessage(Message.raw("§cBoss '" + bossId + "' has no npcId set."));
                return CompletableFuture.completedFuture(null);
            }

            // Check if using arena or current location
            String finalArenaId = null;
            if (!location.equalsIgnoreCase("here")) {
                // It's an arena ID
                var arena = ArenaRegistry.get(location);
                if (arena == null) {
                    ctx.sendMessage(Message.raw("§cArena '" + location + "' not found. Use 'here' to spawn at your location."));
                    return CompletableFuture.completedFuture(null);
                }
                spawnPos = arena.getPosition();
                finalArenaId = location;
                ctx.sendMessage(Message.raw("Spawning at arena: " + location));
            } else {
                ctx.sendMessage(Message.raw("Spawning at your location"));
            }

            final Vector3d finalSpawnPos = spawnPos;
            final String arenaId = finalArenaId;

            world.execute(() -> {
                UUID uuid = plugin.getBossSpawnService().spawnBossFromJson(
                        _spawnPR,
                        bossId,
                        world,
                        finalSpawnPos,
                        arenaId,
                        null,
                        null,
                        true
                );

                if (uuid == null) {
                    ctx.sendMessage(Message.raw("§cSpawn failed for '" + bossId + "'."));
                } else if (BossSpawnService.DEFERRED_SPAWN_UUID.equals(uuid)) {
                    ctx.sendMessage(Message.raw("§aSpawn sequence started for '" + bossId + "'. Boss will spawn after pre-boss waves."));
                } else {
                    ctx.sendMessage(Message.raw("§aSpawned boss: " + bossId + " (UUID: " + uuid + ")"));
                }
            });

            ctx.sendMessage(Message.raw("Spawning boss: " + bossId + "..."));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ============================================================
    // /bossarena reload
    // ============================================================
    static final class Reload extends AbstractCommand {
        private final BossArenaPlugin plugin;

        Reload(BossArenaPlugin plugin) {
            super("reload", "Reload config and boss definitions");
            this.plugin = plugin;
            requireAdminPermission(this);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            try {
                var config = plugin.getConfig();
                config.load();
                plugin.refreshTimedBossSpawns();
                plugin.reloadShopConfig();
                ctx.sendMessage(Message.raw("✓ Config reloaded"));
            } catch (Exception err) {
                ctx.sendMessage(Message.raw("✗ Config reload failed"));
            }

            plugin.reloadBossDefinitions().handle((count, err) -> {
                if (err != null) {
                    ctx.sendMessage(Message.raw("✗ Boss definitions reload failed"));
                } else {
                    ctx.sendMessage(Message.raw("✓ Reloaded " + count + " boss definitions"));
                }
                return null;
            });

            plugin.reloadArenas().handle((count, err) -> {
                if (err != null) {
                    ctx.sendMessage(Message.raw("✗ Arena reload failed"));
                } else {
                    ctx.sendMessage(Message.raw("✓ Reloaded " + count + " arenas"));
                }
                return null;
            });

            // ADD THIS BLOCK:
            plugin.reloadLootTables().handle((count, err) -> {
                if (err != null) {
                    ctx.sendMessage(Message.raw("✗ Loot tables reload failed"));
                } else {
                    ctx.sendMessage(Message.raw("✓ Reloaded " + count + " loot tables"));
                }
                return null;
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    // ============================================================
    // /bossarena config
    // ============================================================
    static final class Config extends AbstractCommand {
        private final BossArenaPlugin plugin;

        Config(BossArenaPlugin plugin) {
            super("config", "Open the BossArena config GUI");
            this.plugin = plugin;
            requireAdminPermission(this);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Player-only command"));
                return CompletableFuture.completedFuture(null);
            }

            com.hypixel.hytale.component.Ref<EntityStore> _configRef = ctx.senderAsPlayerRef();
            PlayerRef _configPR = _configRef != null ? _configRef.getStore().getComponent(_configRef, PlayerRef.getComponentType()) : null;
            Player _configPlayer = _configRef != null ? _configRef.getStore().getComponent(_configRef, Player.getComponentType()) : null;
            World world = _configPR != null ? Universe.get().getWorld(_configPR.getWorldUuid()) : null;
            if (world == null) {
                ctx.sendMessage(Message.raw("Could not resolve player world"));
                return CompletableFuture.completedFuture(null);
            }

            final Ref<EntityStore> _configRefFinal = _configRef;
            final Player _configPlayerFinal = _configPlayer;
            world.execute(() -> {
                if (_configRefFinal == null) {
                    return;
                }

                Store<EntityStore> store = _configRefFinal.getStore();
                BossArenaConfigPage.open(_configRefFinal, store, _configPlayerFinal, plugin);
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    // ============================================================
    // /bossarena shop ...
    // ============================================================
    static final class ShopRoot extends AbstractCommand {

        ShopRoot(BossArenaPlugin plugin) {
            super("shop", "Shop management");
            requireAdminPermission(this);
            addSubCommand(new ShopOpen(plugin));
            addSubCommand(new ShopPlace(plugin));
            addSubCommand(new ShopDelete(plugin));
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            ctx.sendMessage(Message.raw("Use: /bossarena shop <open|place|delete>"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ShopOpen extends AbstractCommand {
        private final BossArenaPlugin plugin;

        ShopOpen(BossArenaPlugin plugin) {
            super("open", "Open the shop GUI: /bossarena shop open");
            this.plugin = plugin;
            requireAdminPermission(this);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Player-only command"));
                return CompletableFuture.completedFuture(null);
            }

            com.hypixel.hytale.component.Ref<EntityStore> _shopOpenRef = ctx.senderAsPlayerRef();
            PlayerRef _shopOpenPR = _shopOpenRef != null ? _shopOpenRef.getStore().getComponent(_shopOpenRef, PlayerRef.getComponentType()) : null;
            Player _shopOpenPlayer = _shopOpenRef != null ? _shopOpenRef.getStore().getComponent(_shopOpenRef, Player.getComponentType()) : null;
            World world = _shopOpenPR != null ? Universe.get().getWorld(_shopOpenPR.getWorldUuid()) : null;
            if (world == null) {
                ctx.sendMessage(Message.raw("Could not resolve player world"));
                return CompletableFuture.completedFuture(null);
            }

            final Ref<EntityStore> _shopOpenRefFinal = _shopOpenRef;
            final Player _shopOpenPlayerFinal = _shopOpenPlayer;
            world.execute(() -> {
                if (_shopOpenRefFinal == null) {
                    return;
                }

                Store<EntityStore> store = _shopOpenRefFinal.getStore();
                BossArenaShopPage.open(_shopOpenRefFinal, store, _shopOpenPlayerFinal, plugin);
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ShopPlace extends AbstractCommand {
        private final BossArenaPlugin plugin;

        ShopPlace(BossArenaPlugin plugin) {
            super("place", "Spawn the shop NPC at your location: /bossarena shop place");
            this.plugin = plugin;
            requireAdminPermission(this);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            return spawnShopNpc(ctx, plugin);
        }
    }

    private static final class ShopDelete extends AbstractCommand {
        private final BossArenaPlugin plugin;

        ShopDelete(BossArenaPlugin plugin) {
            super("delete", "Delete nearest spawned shop NPC: /bossarena shop delete");
            this.plugin = plugin;
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Player-only command"));
                return CompletableFuture.completedFuture(null);
            }

            com.hypixel.hytale.component.Ref<EntityStore> _delRef = ctx.senderAsPlayerRef();
            PlayerRef _delPR = _delRef != null ? _delRef.getStore().getComponent(_delRef, PlayerRef.getComponentType()) : null;
            Player player = _delRef != null ? _delRef.getStore().getComponent(_delRef, Player.getComponentType()) : null;
            World world = _delPR != null ? Universe.get().getWorld(_delPR.getWorldUuid()) : null;
            if (world == null) {
                ctx.sendMessage(Message.raw("Could not resolve player world"));
                return CompletableFuture.completedFuture(null);
            }

            Vector3d playerPosition = getPlayerPosition(player);
            UUID nearest = findNearestShopNpcUuid(plugin, world, playerPosition);
            if (nearest == null) {
                boolean removedShopLocation = false;
                if (plugin.getShopConfig() != null) {
                    int x = (int) Math.floor(playerPosition.x);
                    int y = (int) Math.floor(playerPosition.y);
                    int z = (int) Math.floor(playerPosition.z);
                    removedShopLocation = plugin.getShopConfig().removeNearestShopLocation(world.getName(), x, y, z, 4);
                    if (removedShopLocation) {
                        plugin.saveShopConfig();
                    }
                }
                if (removedShopLocation) {
                    ctx.sendMessage(Message.raw("No tracked shop NPC found. Removed nearest saved shop location entry near your position."));
                } else {
                    ctx.sendMessage(Message.raw("No tracked shop NPC found in this world."));
                }
                return CompletableFuture.completedFuture(null);
            }

            world.execute(() -> {
                Ref<EntityStore> ref = world.getEntityRef(nearest);
                if (ref == null) {
                    if (plugin.getShopConfig() != null && plugin.getShopConfig().removeShopNpcUuid(nearest.toString())) {
                        plugin.saveShopConfig();
                    }
                    ctx.sendMessage(Message.raw("Shop NPC entity not found (stale UUID): " + nearest));
                    return;
                }

                boolean changed = false;
                Store<EntityStore> wstore = world.getEntityStore().getStore();
                Entity entity = EntityUtils.getEntity(ref, wstore);
                if (plugin.getShopConfig() != null) {
                    changed |= plugin.getShopConfig().removeShopNpcUuid(nearest.toString());
                    Object _tc = wstore.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                    if (_tc instanceof com.hypixel.hytale.server.core.modules.entity.component.TransformComponent _transform) {
                        org.joml.Vector3d position = _transform.getPosition();
                        int x = (int) Math.floor(position.x);
                        int y = (int) Math.floor(position.y);
                        int z = (int) Math.floor(position.z);
                        String worldName = world.getName();
                        boolean removedExact = plugin.getShopConfig().removeShopLocation(worldName, x, y, z);
                        changed |= removedExact;
                        if (!removedExact) {
                            changed |= plugin.getShopConfig().removeNearestShopLocation(worldName, x, y, z, 3);
                        }
                    }
                }
                if (changed) {
                    plugin.saveShopConfig();
                }

                world.getEntityStore().getStore().removeEntity(ref, RemoveReason.REMOVE);
                ctx.sendMessage(Message.raw("Deleted shop NPC: " + nearest));
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class Cleanup extends AbstractCommand {
        private final BossArenaPlugin plugin;
        private final FlagArg confirmArg;

        public Cleanup(BossArenaPlugin plugin) {
            super("cleanup", "Force clear all BossArena entities and items for uninstallation.");
            this.plugin = plugin;
            requirePermission(ADMIN_PERMISSION);
            this.confirmArg = withFlagArg("confirm", "Required for execution");
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            CommandSender sender = ctx.sender();

            if (!ctx.get(confirmArg)) {
                sender.sendMessage(Message.raw("§c[BossArena] WARNING: This will permanently delete ALL active bosses, mobs, arenas, and shop keepers!"));
                sender.sendMessage(Message.raw("§c[BossArena] To proceed, run: §f/ba cleanup --confirm"));
                return CompletableFuture.completedFuture(null);
            }

            sender.sendMessage(Message.raw("§e[BossArena] Starting deep cleanup..."));

            // 1. Clear Registries (Permanent config removal)
            ArenaRegistry.clear();
            plugin.saveArenas();
            sender.sendMessage(Message.raw("§a- All arenas cleared from registry and saved."));

            if (plugin.getShopConfig() != null && plugin.getShopConfig().shops != null) {
                plugin.getShopConfig().shops.clear();
                plugin.saveShopConfig();
                sender.sendMessage(Message.raw("§a- All shop locations removed from config and saved."));
            }

            // 2. Clear Map Markers
            if (plugin.getTimedBossMapMarkerService() != null) {
                plugin.getTimedBossMapMarkerService().clearAllMarkers();
                sender.sendMessage(Message.raw("§a- Map markers cleared."));
            }

            // 3. Prepare entities to remove
            BossTrackingSystem tracking = plugin.getTrackingSystem();
            Set<UUID> trackedUuids = new HashSet<>();
            if (tracking != null) {
                trackedUuids.addAll(tracking.snapshotTrackedBosses().keySet());
                trackedUuids.addAll(tracking.snapshotTrackedAdds().keySet());
                tracking.clearRuntimeState();
            }

            // 4. Process worlds for deep entity sweep and items
            for (World world : Universe.get().getWorlds().values()) {
                world.execute(() -> {
                    BossArenaCleanup.removeBossArenaEntitiesInWorld(world, trackedUuids, true);

                    int inventoryCount = 0;
                    for (PlayerRef pr : world.getPlayerRefs()) {
                        if (pr == null) {
                            continue;
                        }
                        Ref<EntityStore> er = pr.getReference();
                        if (er == null) {
                            continue;
                        }
                        Store<EntityStore> st = er.getStore();
                        for (var invType : new com.hypixel.hytale.component.ComponentType[]{
                                com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage.getComponentType(),
                                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType()
                        }) {
                            @SuppressWarnings("unchecked")
                            Object invObj = st.getComponent(er, invType);
                            if (!(invObj instanceof com.hypixel.hytale.server.core.inventory.InventoryComponent inv)) {
                                continue;
                            }
                            ItemContainer container = inv.getInventory();
                            if (container == null) continue;
                            for (short i = 0; i < container.getCapacity(); i++) {
                                var stack = container.getItemStack(i);
                                if (stack != null && stack.getItem() != null) {
                                    String id = stack.getItem().getId();
                                    if (id != null && (id.contains("BossArena") || id.contains("boss_arena_chest"))) {
                                        container.setItemStackForSlot(i, null);
                                        inventoryCount++;
                                    }
                                }
                            }
                        }
                    }

                    sender.sendMessage(Message.raw("§a- Cleanup completed in world: " + world.getName()));
                });
            }

            // 5. Delete persistent runtime state files
            plugin.deletePersistentState();
            sender.sendMessage(Message.raw("§a- Persistent runtime state cleared."));

            sender.sendMessage(Message.raw("§6[BossArena] Deep cleanup scheduled on world threads."));
            sender.sendMessage(Message.raw("§e(Note: Boss definitions were preserved. Restart recommended.)"));
            return CompletableFuture.completedFuture(null);
        }
    }
}
