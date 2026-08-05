package com.varyon.bossarena.system;

import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.util.MobAggroForcer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically pulls idle event mobs onto the nearest player inside the arena.
 *
 * <p>Spawned mobs have a short natural aggro range, so players fighting from across the arena are
 * never noticed. This walks the tracked bosses/adds of each event and, for any mob with no valid
 * target, points it at the closest player within the arena radius. Mobs that already hold a target
 * are skipped, so a Rempart taunt (Varyon-RPG) is never overridden.
 */
public final class BossForcedAggroSystem extends TickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    /** Used when the arena has no loot radius configured; matches {@link BossLeashSystem}. */
    private static final double DEFAULT_AGGRO_RADIUS = 40.0d;

    private final BossTrackingSystem trackingSystem;
    private final BossArenaConfig config;
    private final Map<String, Float> elapsedByWorld = new ConcurrentHashMap<>();

    public BossForcedAggroSystem(BossTrackingSystem trackingSystem, BossArenaConfig config) {
        this.trackingSystem = trackingSystem;
        this.config = config;
    }

    @Override
    public void tick(float dt, int index, @Nonnull Store<EntityStore> store) {
        if (trackingSystem == null || config == null || !MobAggroForcer.isAvailable()) {
            return;
        }

        double interval = config.getForcedAggroIntervalSeconds();
        if (interval <= 0.0d) {
            return;
        }

        EntityStore external = store.getExternalData();
        World tickWorld = external != null ? external.getWorld() : null;
        if (tickWorld == null || !tickWorld.isAlive()) {
            return;
        }
        String worldKey = tickWorld.getName();
        if (worldKey == null || worldKey.isBlank()) {
            worldKey = Integer.toHexString(System.identityHashCode(tickWorld));
        }

        float elapsed = elapsedByWorld.merge(worldKey, Math.max(0f, dt), Float::sum);
        if (elapsed < interval) {
            return;
        }
        elapsedByWorld.put(worldKey, 0f);

        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry
                : trackingSystem.snapshotTrackedBosses(tickWorld).entrySet()) {
            AggroAnchor anchor = resolveBossAnchor(entry.getKey(), entry.getValue());
            forceAggroIfIdle(entry.getKey(), tickWorld, store, anchor);
        }

        for (Map.Entry<UUID, UUID> entry : trackingSystem.snapshotTrackedAdds(tickWorld).entrySet()) {
            UUID bossUuid = entry.getValue();
            AggroAnchor anchor = resolveBossAnchor(bossUuid, trackingSystem.getBossData(bossUuid));
            forceAggroIfIdle(entry.getKey(), tickWorld, store, anchor);
        }

        for (Map.Entry<UUID, UUID> entry : trackingSystem.snapshotPendingPreBossAdds(tickWorld).entrySet()) {
            AggroAnchor anchor = resolveEventAnchor(entry.getValue());
            if (anchor == null || anchor.world != tickWorld) {
                continue;
            }
            forceAggroIfIdle(entry.getKey(), tickWorld, store, anchor);
        }
    }

    @Nullable
    private AggroAnchor resolveBossAnchor(@Nullable UUID bossUuid, @Nullable BossTrackingSystem.BossData data) {
        if (bossUuid == null) {
            return null;
        }
        BossTrackingSystem.BossEventContext ctx = trackingSystem.getEventContext(bossUuid);
        World world = ctx != null ? ctx.world : null;
        Vector3d center = ctx != null ? ctx.spawnLocation : null;
        String arenaId = data != null ? data.arenaId : null;

        if (world == null && data != null) {
            world = data.world;
        }
        if (center == null && data != null) {
            center = data.spawnLocation;
        }

        UUID eventId = trackingSystem.getEventIdForTrackedEntity(bossUuid);
        if (eventId != null) {
            AggroAnchor fromEvent = resolveEventAnchor(eventId);
            if (fromEvent != null) {
                if (world == null) {
                    world = fromEvent.world;
                }
                if (center == null) {
                    center = fromEvent.center;
                }
                if (arenaId == null || arenaId.isBlank()) {
                    arenaId = fromEvent.arenaId;
                }
            }
        }

        if (world == null || center == null) {
            return null;
        }
        return new AggroAnchor(world, center, arenaId, resolveRadius(arenaId));
    }

    @Nullable
    private AggroAnchor resolveEventAnchor(@Nullable UUID eventId) {
        if (eventId == null) {
            return null;
        }
        BossTrackingSystem.ActiveEventStatus status = trackingSystem.getActiveEventStatus(eventId);
        if (status == null || status.world == null || status.eventCenter == null) {
            return null;
        }
        return new AggroAnchor(status.world, status.eventCenter, status.arenaId, resolveRadius(status.arenaId));
    }

    private static double resolveRadius(@Nullable String arenaId) {
        if (arenaId != null && !arenaId.isBlank()) {
            Arena arena = ArenaRegistry.get(arenaId);
            if (arena != null) {
                double loot = arena.getLootRadius();
                if (loot > 0.0d) {
                    return loot;
                }
            }
        }
        return DEFAULT_AGGRO_RADIUS;
    }

    private void forceAggroIfIdle(
            @Nullable UUID entityUuid,
            World world,
            Store<EntityStore> store,
            @Nullable AggroAnchor anchor
    ) {
        if (entityUuid == null || world == null || store == null || anchor == null || anchor.center == null) {
            return;
        }
        if (anchor.radius <= 0.0d || !Double.isFinite(anchor.radius)) {
            return;
        }

        try {
            Ref<EntityStore> mobRef = world.getEntityRef(entityUuid);
            if (mobRef == null || !mobRef.isValid()) {
                return;
            }

            NPCEntity npc = store.getComponent(mobRef, NPCEntity.getComponentType());
            if (npc == null) {
                return;
            }
            Object role = npc.getRole();
            if (role == null) {
                return;
            }

            Ref<EntityStore> targetRef = findNearestPlayerRef(world, store, anchor);
            if (targetRef == null) {
                return;
            }

            MobAggroForcer.forceTargetIfIdle(role, targetRef);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to force aggro for entity " + entityUuid, e);
        }
    }

    /** Nearest player to the arena center, within the arena radius; null when the arena is empty. */
    @Nullable
    private Ref<EntityStore> findNearestPlayerRef(World world, Store<EntityStore> store, AggroAnchor anchor) {
        double radiusSq = anchor.radius * anchor.radius;
        double bestDistSq = Double.MAX_VALUE;
        Ref<EntityStore> best = null;

        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef == null) {
                continue;
            }
            Vector3d pos = playerRef.getTransform() != null ? playerRef.getTransform().getPosition() : null;
            if (pos == null) {
                continue;
            }

            double dx = pos.x - anchor.center.x;
            double dy = pos.y - anchor.center.y;
            double dz = pos.z - anchor.center.z;
            double distSq = (dx * dx) + (dy * dy) + (dz * dz);
            if (distSq > radiusSq || distSq >= bestDistSq) {
                continue;
            }

            UUID playerUuid = playerRef.getUuid();
            if (playerUuid == null) {
                continue;
            }
            Ref<EntityStore> ref = world.getEntityRef(playerUuid);
            if (ref == null || !ref.isValid()) {
                continue;
            }
            if (store.getComponent(ref, TransformComponent.getComponentType()) == null) {
                continue;
            }

            bestDistSq = distSq;
            best = ref;
        }

        return best;
    }

    private static final class AggroAnchor {
        final World world;
        final Vector3d center;
        final String arenaId;
        final double radius;

        AggroAnchor(World world, Vector3d center, String arenaId, double radius) {
            this.world = world;
            this.center = center;
            this.arenaId = arenaId;
            this.radius = radius;
        }
    }
}
