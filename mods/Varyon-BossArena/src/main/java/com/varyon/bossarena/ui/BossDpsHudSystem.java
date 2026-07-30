package com.varyon.bossarena.ui;

import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.damagechart.BossDamageChartTracker;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.system.BossTrackingSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Toutes les secondes : pour chaque event de boss en cours, calcule le top 5 dégâts/DPS et met à jour
 * le HUD des joueurs à proximité (même rayon que les notifications de combat). Masque le HUD des joueurs
 * qui s'éloignent ou dont le combat s'est terminé.
 */
public final class BossDpsHudSystem extends TickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final long UPDATE_INTERVAL_MS = 1000L;

    private final BossTrackingSystem trackingSystem;
    private final BossDamageChartTracker damageChartTracker;
    private final BossArenaPlugin plugin;
    private final Set<UUID> playersWithHudShown = new HashSet<>();
    /**
     * Consecutive ticks a shown player was missing from the nearby set. Hiding the HUD after a
     * single miss made it flicker on isolated glitches (a transform/HP read failing for one tick
     * while the player stayed put) — require a few misses in a row before actually hiding it.
     */
    private final Map<UUID, Integer> consecutiveMissesByPlayer = new HashMap<>();
    private static final int MISSES_BEFORE_HIDE = 3;
    /**
     * tick() may fire more than once per real-time second, so pacing is done against a wall-clock
     * timestamp rather than accumulated {@code dt} — summing dt across redundant calls made the
     * interval trigger several times too fast, causing the HUD to flicker.
     */
    private volatile long nextRunAtMs;
    /**
     * Last combat snapshot shown to each player while their boss event was still active, so it can
     * be kept frozen on screen for POST_KILL_LINGER_MS after the boss dies (event removed from
     * tracking) instead of vanishing instantly.
     */
    private final Map<UUID, FightSnapshot> lastFightSnapshotByPlayer = new HashMap<>();
    private static final long POST_KILL_LINGER_MS = 10_000L;

    private record FightSnapshot(UUID eventId, String bossName, float bossHpCurrent, float bossHpMax,
                                 List<BossDpsHud.PlayerDamageRow> rows, int personalKills, long expiresAtMs) {
    }

    /** Drops all per-player tracking state for a disconnected player. Must run on the world thread. */
    public void onPlayerDisconnected(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        playersWithHudShown.remove(playerUuid);
        consecutiveMissesByPlayer.remove(playerUuid);
        lastFightSnapshotByPlayer.remove(playerUuid);
    }

    public BossDpsHudSystem(BossTrackingSystem trackingSystem,
                            BossDamageChartTracker damageChartTracker,
                            BossArenaPlugin plugin) {
        this.trackingSystem = trackingSystem;
        this.damageChartTracker = damageChartTracker;
        this.plugin = plugin;
    }

    @Override
    public void tick(float dt, int index, @Nonnull Store<EntityStore> store) {
        if (trackingSystem == null || damageChartTracker == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextRunAtMs) {
            return;
        }
        nextRunAtMs = now + UPDATE_INTERVAL_MS;

        Set<UUID> playersUpdatedThisTick = new HashSet<>();
        Set<UUID> activeEventIds = new HashSet<>();

        for (BossTrackingSystem.ActiveEventStatus event : trackingSystem.snapshotActiveEvents()) {
            if (event == null || event.world == null) {
                continue;
            }
            activeEventIds.add(event.eventId);
            if (event.awaitingPrimaryBossSpawn) {
                handleWavePhase(event, playersUpdatedThisTick);
                continue;
            }

            UUID primaryBossUuid = resolvePrimaryBossUuid(event.eventId);
            if (primaryBossUuid == null) {
                LOGGER.fine(() -> "HUD tick: no primary boss UUID resolved for event " + event.eventId);
                keepLastKnownFightVisible(event, playersUpdatedThisTick, now);
                continue;
            }

            float[] hp = readHp(event.world, primaryBossUuid);
            if (hp == null) {
                LOGGER.fine(() -> "HUD tick: readHp failed for boss " + primaryBossUuid);
                keepLastKnownFightVisible(event, playersUpdatedThisTick, now);
                continue;
            }

            // Follow the boss's current position, not its fixed spawn point — a chasing boss
            // (leash/aggro) can drift far from event.eventCenter while players stay next to it.
            Vector3d bossPosition = readPosition(event.world, primaryBossUuid);
            if (bossPosition == null) {
                LOGGER.fine(() -> "HUD tick: readPosition failed for boss " + primaryBossUuid + ", falling back to eventCenter");
            }
            Vector3d center = bossPosition != null ? bossPosition : event.eventCenter;

            double radius = resolveRadius(event.arenaId);
            List<PlayerRef> nearby = collectNearbyPlayers(event.world, center, radius);
            if (nearby.isEmpty()) {
                LOGGER.fine(() -> "HUD tick: no players in range (radius=" + radius + ", center=" + center + ")");
                continue;
            }

            List<BossDpsHud.PlayerDamageRow> rows = buildTopRows(event);

            for (PlayerRef playerRef : nearby) {
                Player player = resolvePlayer(playerRef);
                if (player == null) {
                    continue;
                }
                BossDpsHud hud = BossDpsHud.getOrCreate(player, playerRef);
                hud.updateFight(event.bossName, hp[0], hp[1], rows);
                playersUpdatedThisTick.add(playerRef.getUuid());
                playersWithHudShown.add(playerRef.getUuid());
                consecutiveMissesByPlayer.remove(playerRef.getUuid());
                int personalKills = trackingSystem.getEventMobKillsForPlayer(event.eventId, playerRef.getUuid());
                lastFightSnapshotByPlayer.put(playerRef.getUuid(),
                        new FightSnapshot(event.eventId, event.bossName, hp[0], hp[1], rows, personalKills,
                                now + POST_KILL_LINGER_MS));
            }
        }

        applyPostKillLinger(playersUpdatedThisTick, activeEventIds, now);
        hideStaleHuds(playersUpdatedThisTick);
    }

    /**
     * A player whose boss event just disappeared entirely (boss killed — event removed from
     * tracking) keeps seeing their last known fight state, frozen, for POST_KILL_LINGER_MS, instead
     * of the HUD vanishing the instant the boss dies. Only triggers when the event itself is gone —
     * a player merely out of range while the fight is still ongoing keeps using the shorter
     * MISSES_BEFORE_HIDE grace period in hideStaleHuds instead.
     */
    private void applyPostKillLinger(Set<UUID> playersUpdatedThisTick, Set<UUID> activeEventIds, long now) {
        var it = lastFightSnapshotByPlayer.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID playerUuid = entry.getKey();
            if (playersUpdatedThisTick.contains(playerUuid)) {
                continue;
            }
            FightSnapshot snapshot = entry.getValue();
            if (activeEventIds.contains(snapshot.eventId())) {
                // Fight still ongoing — player is just out of range; let hideStaleHuds handle it.
                continue;
            }
            if (now >= snapshot.expiresAtMs()) {
                it.remove();
                continue;
            }
            BossDpsHud hud = BossDpsHud.get(playerUuid);
            if (hud != null) {
                hud.updateFight(snapshot.bossName(), snapshot.bossHpCurrent(), snapshot.bossHpMax(),
                        snapshot.rows(), snapshot.personalKills());
            }
            playersUpdatedThisTick.add(playerUuid);
            playersWithHudShown.add(playerUuid);
            consecutiveMissesByPlayer.remove(playerUuid);
        }
    }

    /**
     * The fight is still active but this tick failed to resolve the boss/HP (transient lookup
     * hiccup — entity ref momentarily invalid, component read race, etc). Re-send each affected
     * player's last known snapshot for this event instead of silently skipping the update, so a
     * couple of bad ticks in a row don't burn through hideStaleHuds' miss counter and close the HUD.
     */
    private void keepLastKnownFightVisible(BossTrackingSystem.ActiveEventStatus event,
                                           Set<UUID> playersUpdatedThisTick, long now) {
        for (var entry : lastFightSnapshotByPlayer.entrySet()) {
            UUID playerUuid = entry.getKey();
            FightSnapshot snapshot = entry.getValue();
            if (!event.eventId.equals(snapshot.eventId())) {
                continue;
            }
            BossDpsHud hud = BossDpsHud.get(playerUuid);
            if (hud != null) {
                hud.updateFight(snapshot.bossName(), snapshot.bossHpCurrent(), snapshot.bossHpMax(),
                        snapshot.rows(), snapshot.personalKills());
            }
            playersUpdatedThisTick.add(playerUuid);
            playersWithHudShown.add(playerUuid);
            consecutiveMissesByPlayer.remove(playerUuid);
            entry.setValue(new FightSnapshot(snapshot.eventId(), snapshot.bossName(), snapshot.bossHpCurrent(),
                    snapshot.bossHpMax(), snapshot.rows(), snapshot.personalKills(), now + POST_KILL_LINGER_MS));
        }
    }

    private void hideStaleHuds(Set<UUID> playersUpdatedThisTick) {
        playersWithHudShown.removeIf(playerUuid -> {
            if (playersUpdatedThisTick.contains(playerUuid)) {
                return false;
            }
            int misses = consecutiveMissesByPlayer.merge(playerUuid, 1, Integer::sum);
            if (misses < MISSES_BEFORE_HIDE) {
                return false;
            }
            consecutiveMissesByPlayer.remove(playerUuid);
            lastFightSnapshotByPlayer.remove(playerUuid);
            BossDpsHud hud = BossDpsHud.get(playerUuid);
            if (hud != null) {
                hud.updateFight(null, 0f, 0f, List.of());
            }
            return true;
        });
    }

    /** Pre-boss wave phase: no boss tracked yet, so center proximity on the fixed event center. */
    private void handleWavePhase(BossTrackingSystem.ActiveEventStatus event, Set<UUID> playersUpdatedThisTick) {
        double radius = resolveRadius(event.arenaId);
        List<PlayerRef> nearby = collectNearbyPlayers(event.world, event.eventCenter, radius);
        if (nearby.isEmpty()) {
            return;
        }

        int currentWave = event.currentWaveNumber;
        int totalWaves = event.totalWaveCount;
        int aliveMobs = event.activeAddCount;
        int wavePlannedMobs = event.currentWavePlannedMobs;
        int totalKilled = event.totalMobKills;

        for (PlayerRef playerRef : nearby) {
            Player player = resolvePlayer(playerRef);
            if (player == null) {
                continue;
            }
            BossDpsHud hud = BossDpsHud.getOrCreate(player, playerRef);
            int personalKills = trackingSystem.getEventMobKillsForPlayer(event.eventId, playerRef.getUuid());
            hud.updateWaveProgress(currentWave, totalWaves, aliveMobs, wavePlannedMobs, totalKilled, personalKills);
            playersUpdatedThisTick.add(playerRef.getUuid());
            playersWithHudShown.add(playerRef.getUuid());
            consecutiveMissesByPlayer.remove(playerRef.getUuid());
        }
    }

    private UUID resolvePrimaryBossUuid(UUID eventId) {
        return trackingSystem.getPrimaryBossUuid(eventId);
    }

    private BossTrackingSystem.BossData resolvePrimaryBoss(UUID eventId) {
        UUID bossUuid = trackingSystem.getPrimaryBossUuid(eventId);
        return bossUuid != null ? trackingSystem.getBossData(bossUuid) : null;
    }

    /** @return {current, max} ou null si l'entité/ses stats sont indisponibles. */
    private static float[] readHp(World world, UUID bossUuid) {
        if (world == null || bossUuid == null) {
            return null;
        }
        try {
            Ref<EntityStore> ref = world.getEntityRef(bossUuid);
            if (ref == null || !ref.isValid()) {
                return null;
            }
            Store<EntityStore> store = world.getEntityStore().getStore();
            Object statMapObj = store.getComponent(ref, EntityStatMap.getComponentType());
            if (!(statMapObj instanceof EntityStatMap statMap)) {
                return null;
            }
            int healthIndex = DefaultEntityStatTypes.getHealth();
            if (healthIndex < 0) {
                return null;
            }
            EntityStatValue health = statMap.get(healthIndex);
            if (health == null) {
                return null;
            }
            return new float[]{health.get(), health.getMax()};
        } catch (Exception e) {
            return null;
        }
    }

    private static Vector3d readPosition(World world, UUID bossUuid) {
        if (world == null || bossUuid == null) {
            return null;
        }
        try {
            Ref<EntityStore> ref = world.getEntityRef(bossUuid);
            if (ref == null || !ref.isValid()) {
                return null;
            }
            Store<EntityStore> store = world.getEntityStore().getStore();
            Object transformObj = store.getComponent(ref, TransformComponent.getComponentType());
            if (!(transformObj instanceof TransformComponent transform)) {
                return null;
            }
            org.joml.Vector3d rawPos = transform.getPosition();
            if (rawPos == null) {
                return null;
            }
            return new Vector3d(rawPos.x, rawPos.y, rawPos.z);
        } catch (Exception e) {
            return null;
        }
    }

    private double resolveRadius(String arenaId) {
        double radius = -1.0d;
        if (arenaId != null && !arenaId.isBlank()) {
            Arena arena = ArenaRegistry.get(arenaId);
            if (arena != null) {
                radius = arena.getBannerRadius();
            }
        }
        if (!Double.isFinite(radius) || radius <= 0) {
            BossArenaConfig config = plugin != null ? plugin.getConfig() : null;
            radius = config != null ? config.getNotificationRadius() : 100.0d;
        }
        return radius;
    }

    private static List<PlayerRef> collectNearbyPlayers(World world, Vector3d center, double radius) {
        List<PlayerRef> out = new ArrayList<>();
        if (world == null || center == null || radius <= 0.0d) {
            return out;
        }
        double radiusSq = radius * radius;
        try {
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                if (playerRef == null || !playerRef.isValid()) {
                    continue;
                }
                var transform = playerRef.getTransform();
                if (transform == null) {
                    continue;
                }
                org.joml.Vector3d rawPos = transform.getPosition();
                if (rawPos == null) {
                    continue;
                }
                double dx = rawPos.x - center.x;
                double dy = rawPos.y - center.y;
                double dz = rawPos.z - center.z;
                if ((dx * dx) + (dy * dy) + (dz * dz) > radiusSq) {
                    continue;
                }
                out.add(playerRef);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static Player resolvePlayer(PlayerRef playerRef) {
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                return null;
            }
            Store<EntityStore> store = ref.getStore();
            Object playerObj = store.getComponent(ref, Player.getComponentType());
            return playerObj instanceof Player player ? player : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<BossDpsHud.PlayerDamageRow> buildTopRows(BossTrackingSystem.ActiveEventStatus event) {
        List<BossDpsHud.PlayerDamageRow> rows = new ArrayList<>();
        long fightSeconds = Math.max(1L, elapsedFightSeconds(event.eventId));

        List<UUID> playerUuids = damageChartTracker.snapshotPlayerUuidsForEvent(event.eventId);
        List<BossDamageChartTracker.DamageEntry> ranked = new ArrayList<>();
        for (UUID playerUuid : playerUuids) {
            long damage = damageChartTracker.getDamage(event.eventId, playerUuid);
            if (damage > 0L) {
                ranked.add(new BossDamageChartTracker.DamageEntry(playerUuid, damage));
            }
        }
        ranked.sort(Comparator.comparingLong(BossDamageChartTracker.DamageEntry::damage).reversed());

        int count = Math.min(5, ranked.size());
        for (int i = 0; i < count; i++) {
            BossDamageChartTracker.DamageEntry entry = ranked.get(i);
            String name = resolvePlayerName(event.world, entry.playerUuid());
            long dps = Math.round(entry.damage() / (double) fightSeconds);
            rows.add(new BossDpsHud.PlayerDamageRow(name, entry.damage(), dps));
        }
        return rows;
    }

    private long elapsedFightSeconds(UUID eventId) {
        BossTrackingSystem.BossData boss = resolvePrimaryBoss(eventId);
        if (boss == null || boss.spawnedAtEpochMs <= 0L) {
            return 1L;
        }
        return Math.max(1L, (System.currentTimeMillis() - boss.spawnedAtEpochMs) / 1000L);
    }

    private static String resolvePlayerName(World world, UUID playerUuid) {
        if (world == null || playerUuid == null) {
            return "?";
        }
        try {
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                if (playerRef != null && playerUuid.equals(playerRef.getUuid())) {
                    return playerRef.getUsername();
                }
            }
        } catch (Exception ignored) {
        }
        return "?";
    }
}
