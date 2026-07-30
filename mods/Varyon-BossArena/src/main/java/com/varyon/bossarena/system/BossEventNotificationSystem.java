package com.varyon.bossarena.system;

import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.loot.BossLootHandler;
import org.joml.Vector3d;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public final class BossEventNotificationSystem extends TickingSystem<EntityStore> {
    private static final long UPDATE_INTERVAL_MS = 1000L;
    private static final long MISSING_RECONCILE_GRACE_MS = 15_000L;
    private static final double MISSING_RECONCILE_PLAYER_RADIUS = 192.0d;

    private final BossTrackingSystem trackingSystem;
    private final BossArenaPlugin plugin;
    private final Map<UUID, Long> missingBossSince = new ConcurrentHashMap<>();
    private final Map<UUID, Long> missingAddSince = new ConcurrentHashMap<>();
    /**
     * tick() may fire more than once per real-time second (observed ~3-4x/s in production logs),
     * so pacing is done against a wall-clock timestamp rather than accumulated {@code dt} —
     * summing dt across redundant calls made the interval trigger several times too fast.
     */
    private volatile long nextRunAtMs;

    public BossEventNotificationSystem(BossTrackingSystem trackingSystem, BossArenaPlugin plugin) {
        this.trackingSystem = trackingSystem;
        this.plugin = plugin;
    }

    private static boolean isEntityMissing(World world, UUID entityUuid) {
        if (world == null || entityUuid == null) {
            return true;
        }
        try {
            var entityRef = world.getEntityRef(entityUuid);
            return entityRef == null || !entityRef.isValid();
        } catch (Exception ignored) {
            return true;
        }
    }

    /** Snapshot of player positions in a world, computed once per tick instead of once per tracked entity. */
    private static java.util.List<Vector3d> collectPlayerPositions(World world) {
        java.util.List<Vector3d> positions = new java.util.ArrayList<>();
        if (world == null) {
            return positions;
        }
        try {
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                if (playerRef == null) {
                    continue;
                }
                Ref<EntityStore> er = playerRef.getReference();
                if (er == null) {
                    continue;
                }
                Store<EntityStore> st = er.getStore();
                Player player = st.getComponent(er, Player.getComponentType());
                if (player == null) {
                    continue;
                }
                Object _tcObj = st.getComponent(er, TransformComponent.getComponentType());
                TransformComponent tc = _tcObj instanceof TransformComponent ? (TransformComponent) _tcObj : null;
                if (tc == null) {
                    continue;
                }
                org.joml.Vector3d rawPlayerPos = tc.getPosition();
                if (rawPlayerPos == null) {
                    continue;
                }
                positions.add(new Vector3d(rawPlayerPos.x, rawPlayerPos.y, rawPlayerPos.z));
            }
        } catch (Exception ignored) {
            // Best effort only; if this fails we treat it as no nearby players.
        }
        return positions;
    }

    private static boolean isMissingReconcileEligible(java.util.List<Vector3d> playerPositions, Vector3d anchor) {
        if (playerPositions.isEmpty() || anchor == null) {
            return false;
        }
        double radiusSquared = MISSING_RECONCILE_PLAYER_RADIUS * MISSING_RECONCILE_PLAYER_RADIUS;
        for (Vector3d playerPos : playerPositions) {
            if (playerPos.distanceSquared(anchor) <= radiusSquared) {
                return true;
            }
        }
        return false;
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

        long now = System.currentTimeMillis();
        if (now < nextRunAtMs) {
            return;
        }
        nextRunAtMs = now + UPDATE_INTERVAL_MS;

        reconcileMissingTrackedEntities(tickWorld);

        for (BossTrackingSystem.ActiveEventStatus event : trackingSystem.snapshotActiveEvents()) {
            if (event == null || event.world != tickWorld) {
                continue;
            }
            double notificationRadius = -1.0d;
            if (event.arenaId != null && !event.arenaId.isBlank()) {
                Arena arena = ArenaRegistry.get(event.arenaId);
                if (arena != null) {
                    notificationRadius = arena.getBannerRadius();
                }
            }
            if (!Double.isFinite(notificationRadius) || notificationRadius <= 0) {
                BossArenaConfig config = plugin != null ? plugin.getConfig() : null;
                notificationRadius = (config != null) ? config.getNotificationRadius() : 100.0d;
            }
            BossWaveNotificationService.notifyBossAliveStatus(
                    event.world,
                    event.eventCenter,
                    event.bossName,
                    event.aliveBossCount,
                    event.activeAddCount,
                    event.awaitingPrimaryBossSpawn ? "Preparing encounter" : null,
                    event.remainingCountdownMillis,
                    event.awaitingPrimaryBossSpawn || event.aliveBossCount > 0 || event.activeAddCount > 0,
                    true,
                    notificationRadius,
                    event.eventId,
                    event.currentWaveNumber,
                    event.totalWaveCount
            );
        }
    }

    private void reconcileMissingTrackedEntities(World tickWorld) {
        long now = System.currentTimeMillis();
        // retainAll against the global tracked sets (cheap UUID-set ops) so state for other worlds'
        // entities isn't purged just because this world happens to tick; the expensive per-entity
        // distance scan below is still scoped to tickWorld only.
        missingBossSince.keySet().retainAll(trackingSystem.snapshotTrackedBosses().keySet());
        missingAddSince.keySet().retainAll(trackingSystem.snapshotTrackedAdds().keySet());

        Map<UUID, BossTrackingSystem.BossData> trackedBosses = trackingSystem.snapshotTrackedBosses(tickWorld);
        Map<UUID, UUID> trackedAdds = trackingSystem.snapshotTrackedAdds(tickWorld);
        java.util.List<Vector3d> nearbyPlayerPositions = collectPlayerPositions(tickWorld);

        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry : trackedBosses.entrySet()) {
            UUID bossUuid = entry.getKey();
            BossTrackingSystem.BossData bossData = entry.getValue();
            if (bossUuid == null || bossData == null) {
                continue;
            }
            if (!isMissingReconcileEligible(nearbyPlayerPositions, bossData.spawnLocation)) {
                missingBossSince.remove(bossUuid);
                continue;
            }
            if (!isEntityMissing(bossData.world, bossUuid)) {
                missingBossSince.remove(bossUuid);
                continue;
            }
            long missingSince = missingBossSince.computeIfAbsent(bossUuid, ignored -> now);
            if ((now - missingSince) < MISSING_RECONCILE_GRACE_MS) {
                continue;
            }
            missingBossSince.remove(bossUuid);

            // Capture context BEFORE marking dead
            BossTrackingSystem.BossEventContext eventContext = trackingSystem.getEventContext(bossUuid);
            BossTrackingSystem.PendingLootData pendingLoot = trackingSystem.markBossDead(bossUuid);

            // Cleanup map marker
            if (plugin != null && plugin.getTimedBossMapMarkerService() != null) {
                plugin.getTimedBossMapMarkerService().onTimedBossDespawn(bossData.world, bossUuid);
            }

            if (pendingLoot != null) {
                // Clear any remaining boss markers if event completed
                if (plugin != null && plugin.getTimedBossMapMarkerService() != null) {
                    for (java.util.UUID uuid : pendingLoot.bossUuids) {
                        plugin.getTimedBossMapMarkerService().onTimedBossDespawn(pendingLoot.world, uuid);
                    }
                }

                BossWaveNotificationService.notifyBossAliveStatus(
                        pendingLoot.world,
                        pendingLoot.eventCenter != null ? pendingLoot.eventCenter : pendingLoot.spawnLocation,
                        pendingLoot.bossName,
                        0,
                        0,
                        null,
                        0L,
                        false,
                        true,
                        -1.0d,
                        pendingLoot.eventId,
                        0
                );
                BossLootHandler.queueLootSpawn(pendingLoot.world, pendingLoot.spawnLocation, pendingLoot.bossName, pendingLoot.eventId);
                continue;
            }

            if (eventContext != null) {
                BossWaveNotificationService.notifyBossAliveStatus(
                        eventContext.world,
                        eventContext.spawnLocation,
                        eventContext.bossName,
                        trackingSystem.getAliveBossCount(bossUuid),
                        trackingSystem.getActiveAddCountForEvent(bossUuid),
                        null,
                        eventContext.remainingCountdownMillis
                );
            }
        }

        for (Map.Entry<UUID, UUID> entry : trackedAdds.entrySet()) {
            UUID addUuid = entry.getKey();
            UUID bossUuid = entry.getValue();
            if (addUuid == null || bossUuid == null) {
                continue;
            }

            BossTrackingSystem.BossData bossData = trackingSystem.getBossData(bossUuid);
            BossTrackingSystem.BossEventContext eventContext = trackingSystem.getEventContext(bossUuid);
            World world = bossData != null ? bossData.world : (eventContext != null ? eventContext.world : null);
            Vector3d anchor = bossData != null ? bossData.spawnLocation : (eventContext != null ? eventContext.spawnLocation : null);
            if (!isMissingReconcileEligible(nearbyPlayerPositions, anchor)) {
                missingAddSince.remove(addUuid);
                continue;
            }
            if (!isEntityMissing(world, addUuid)) {
                missingAddSince.remove(addUuid);
                continue;
            }
            long missingSince = missingAddSince.computeIfAbsent(addUuid, ignored -> now);
            if ((now - missingSince) < MISSING_RECONCILE_GRACE_MS) {
                continue;
            }
            missingAddSince.remove(addUuid);

            // Capture context BEFORE marking dead
            eventContext = trackingSystem.getEventContext(bossUuid);
            BossTrackingSystem.PendingLootData pendingLoot = trackingSystem.handleTrackedAddDeath(addUuid);

            if (pendingLoot != null) {
                // Cleanup all boss markers for the completed event
                if (plugin != null && plugin.getTimedBossMapMarkerService() != null) {
                    for (java.util.UUID buuid : pendingLoot.bossUuids) {
                        plugin.getTimedBossMapMarkerService().onTimedBossDespawn(pendingLoot.world, buuid);
                    }
                }

                BossWaveNotificationService.notifyBossAliveStatus(
                        pendingLoot.world,
                        pendingLoot.eventCenter != null ? pendingLoot.eventCenter : pendingLoot.spawnLocation,
                        pendingLoot.bossName,
                        0,
                        0,
                        null,
                        0L,
                        false,
                        true,
                        -1.0d,
                        pendingLoot.eventId,
                        0
                );
                BossLootHandler.queueLootSpawn(pendingLoot.world, pendingLoot.spawnLocation, pendingLoot.bossName, pendingLoot.eventId);
                continue;
            }

            if (eventContext != null) {
                BossWaveNotificationService.notifyBossAliveStatus(
                        eventContext.world,
                        eventContext.spawnLocation,
                        eventContext.bossName,
                        trackingSystem.getAliveBossCount(bossUuid),
                        trackingSystem.getActiveAddCountForEvent(bossUuid),
                        null,
                        eventContext.remainingCountdownMillis
                );
            }
        }
    }
}
