package com.varyon.bossarena.system;

import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Teleports tracked bosses and wave adds back to the event center when they leave the
 * arena leash radius (arena {@code lootRadius}, else {@link #DEFAULT_LEASH_RADIUS}).
 */
public final class BossLeashSystem extends TickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final float CHECK_INTERVAL_SECONDS = 0.5f;
    /** Used when the arena has no loot radius configured. */
    static final double DEFAULT_LEASH_RADIUS = 40.0d;

    private final BossTrackingSystem trackingSystem;
    private final Map<String, Float> elapsedByWorld = new ConcurrentHashMap<>();

    public BossLeashSystem(BossTrackingSystem trackingSystem) {
        this.trackingSystem = trackingSystem;
    }

    @Override
    public void tick(float dt, int index, @Nonnull Store<EntityStore> store) {
        if (trackingSystem == null) {
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
        if (elapsed < CHECK_INTERVAL_SECONDS) {
            return;
        }
        elapsedByWorld.put(worldKey, 0f);

        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry : trackingSystem.snapshotTrackedBosses().entrySet()) {
            UUID bossUuid = entry.getKey();
            BossTrackingSystem.BossData data = entry.getValue();
            if (bossUuid == null || data == null || data.world != tickWorld) {
                continue;
            }
            LeashAnchor anchor = resolveBossAnchor(bossUuid, data);
            leashIfNeeded(bossUuid, tickWorld, store, anchor);
        }

        for (Map.Entry<UUID, UUID> entry : trackingSystem.snapshotTrackedAdds().entrySet()) {
            UUID addUuid = entry.getKey();
            UUID bossUuid = entry.getValue();
            if (addUuid == null || bossUuid == null) {
                continue;
            }
            BossTrackingSystem.BossData owner = trackingSystem.getBossData(bossUuid);
            World ownerWorld = owner != null ? owner.world : null;
            if (ownerWorld == null) {
                BossTrackingSystem.BossEventContext ctx = trackingSystem.getEventContext(bossUuid);
                ownerWorld = ctx != null ? ctx.world : null;
            }
            if (ownerWorld != tickWorld) {
                continue;
            }
            LeashAnchor anchor = resolveBossAnchor(bossUuid, owner);
            leashIfNeeded(addUuid, tickWorld, store, anchor);
        }

        for (Map.Entry<UUID, UUID> entry : trackingSystem.snapshotPendingPreBossAdds().entrySet()) {
            UUID addUuid = entry.getKey();
            UUID eventId = entry.getValue();
            if (addUuid == null || eventId == null) {
                continue;
            }
            LeashAnchor anchor = resolveEventAnchor(eventId);
            if (anchor == null || anchor.world != tickWorld) {
                continue;
            }
            leashIfNeeded(addUuid, tickWorld, store, anchor);
        }
    }

    private LeashAnchor resolveBossAnchor(UUID bossUuid, @Nullable BossTrackingSystem.BossData data) {
        BossTrackingSystem.BossEventContext ctx = trackingSystem.getEventContext(bossUuid);
        World world = null;
        Vector3d center = null;
        String arenaId = data != null ? data.arenaId : null;

        if (ctx != null) {
            world = ctx.world;
            center = ctx.spawnLocation;
        }
        if (world == null && data != null) {
            world = data.world;
        }
        if (center == null && data != null) {
            center = data.spawnLocation;
        }

        UUID eventId = trackingSystem.getEventIdForTrackedEntity(bossUuid);
        if (eventId != null) {
            LeashAnchor fromEvent = resolveEventAnchor(eventId);
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
        return new LeashAnchor(world, center, arenaId, resolveRadius(arenaId));
    }

    @Nullable
    private LeashAnchor resolveEventAnchor(UUID eventId) {
        BossTrackingSystem.ActiveEventStatus status = trackingSystem.getActiveEventStatus(eventId);
        if (status == null || status.world == null || status.eventCenter == null) {
            return null;
        }
        return new LeashAnchor(
                status.world,
                status.eventCenter,
                status.arenaId,
                resolveRadius(status.arenaId)
        );
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
        return DEFAULT_LEASH_RADIUS;
    }

    private void leashIfNeeded(
            UUID entityUuid,
            World world,
            Store<EntityStore> store,
            @Nullable LeashAnchor anchor
    ) {
        if (entityUuid == null || world == null || store == null || anchor == null || anchor.center == null) {
            return;
        }
        if (anchor.radius <= 0.0d || !Double.isFinite(anchor.radius)) {
            return;
        }

        try {
            Ref<EntityStore> entityRef = world.getEntityRef(entityUuid);
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Object transformObj = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (!(transformObj instanceof TransformComponent transform)) {
                return;
            }

            Vector3d pos = transform.getPosition();
            if (pos == null) {
                return;
            }

            double dx = pos.x - anchor.center.x;
            double dz = pos.z - anchor.center.z;
            double distSq = (dx * dx) + (dz * dz);
            double radiusSq = anchor.radius * anchor.radius;
            if (distSq <= radiusSq) {
                return;
            }

            Vector3d back = new Vector3d(anchor.center.x, anchor.center.y, anchor.center.z);
            transform.teleportPosition(back);
            transform.markChunkDirty(store);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to leash entity " + entityUuid, e);
        }
    }

    private static final class LeashAnchor {
        final World world;
        final Vector3d center;
        final String arenaId;
        final double radius;

        LeashAnchor(World world, Vector3d center, String arenaId, double radius) {
            this.world = world;
            this.center = center;
            this.arenaId = arenaId;
            this.radius = radius;
        }
    }
}
