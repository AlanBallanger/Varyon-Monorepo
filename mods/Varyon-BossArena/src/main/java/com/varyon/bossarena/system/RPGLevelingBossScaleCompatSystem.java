package com.varyon.bossarena.system;

import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.util.BossHealthScale;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.common.semver.SemverRange;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps BossArena HP/level overrides on tracked bosses when RPGLeveling / zone scaling is present.
 * Re-applies the baked world HP factor so zone scale is not lost when competing modifiers are stripped.
 */
public final class RPGLevelingBossScaleCompatSystem extends TickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final long RESYNC_INTERVAL_MS = 250L;

    private final BossTrackingSystem trackingSystem;
    /**
     * tick() may fire more than once per real-time interval, so pacing is done against a wall-clock
     * timestamp rather than accumulated {@code dt} — summing dt across redundant calls made this
     * resync run several times too fast, which could re-trigger the "was full" HP fill-up (maximizeStatValue)
     * shortly after a hit instead of once per 0.25s, making the boss look like it regenerates instantly.
     */
    private volatile long nextRunAtMs;
    private boolean lastRpgLevelingLoaded;
    private boolean loadStateKnown;
    private final Map<UUID, Integer> appliedLevelOverrides = new HashMap<>();
    /**
     * Last effective (worldFactor × bossMult) applied per boss. Re-asserting the MAX HP modifier
     * every resync cycle even when nothing changed forces the engine to strip and re-add it, which
     * can transiently clamp current HP down to the un-multiplied max in between — skip the
     * strip/re-add entirely when the effective multiplier hasn't moved.
     */
    private final Map<UUID, Float> lastEffectiveHpMultiplierByBoss = new ConcurrentHashMap<>();
    private Method rpgGetMethod;
    private Method rpgPutSpawnLevelMethod;
    private Method rpgRemoveSpawnLevelMethod;
    private boolean rpgLevelApiResolved;

    public RPGLevelingBossScaleCompatSystem(BossTrackingSystem trackingSystem) {
        this.trackingSystem = trackingSystem;
    }

    @Override
    public void tick(float dt, int index, @Nonnull Store<EntityStore> store) {
        if (trackingSystem == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextRunAtMs) {
            return;
        }
        nextRunAtMs = now + RESYNC_INTERVAL_MS;

        Map<UUID, BossTrackingSystem.BossData> trackedBosses = trackingSystem.snapshotTrackedBosses();
        Set<UUID> activeBossUuids = new HashSet<>(trackedBosses.keySet());
        lastEffectiveHpMultiplierByBoss.keySet().retainAll(activeBossUuids);
        boolean rpgLoaded = isRpgLevelingLoaded();
        Object rpgPluginInstance = rpgLoaded ? resolveRpgLevelingPluginInstance() : null;

        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry : trackedBosses.entrySet()) {
            enforceBossHpScale(entry.getKey(), entry.getValue());
            if (rpgLoaded) {
                enforceBossLevelOverride(entry.getKey(), entry.getValue(), rpgPluginInstance);
            }
        }
        if (rpgLoaded) {
            pruneStaleLevelOverrides(activeBossUuids, rpgPluginInstance);
        }
    }

    private void enforceBossHpScale(UUID bossUuid, BossTrackingSystem.BossData bossData) {
        if (bossUuid == null || bossData == null || bossData.world == null || bossData.modifiers == null) {
            return;
        }

        float desiredMultiplier = Math.max(0.01f, bossData.modifiers.hpMultiplier());

        try {
            var bossRef = bossData.world.getEntityRef(bossUuid);
            if (bossRef == null || !bossRef.isValid()) {
                return;
            }

            var entityStore = bossData.world.getEntityStore().getStore();
            Object statMapObj = entityStore.getComponent(bossRef, EntityStatMap.getComponentType());
            if (!(statMapObj instanceof EntityStatMap statMap)) {
                return;
            }

            int healthIndex = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthBefore = statMap.get(healthIndex);
            float currentBefore = healthBefore != null ? healthBefore.get() : 0f;
            float maxBefore = healthBefore != null ? healthBefore.getMax() : 0f;
            boolean wasFull = healthBefore != null && maxBefore > 0f && currentBefore + 1f >= maxBefore;

            float knownFactor = bossData.worldHealthFactor;
            float effectiveMultiplier = Math.max(0.01f, knownFactor) * desiredMultiplier;
            Float lastEffective = lastEffectiveHpMultiplierByBoss.get(bossUuid);
            boolean multiplierUnchanged = lastEffective != null
                    && Math.abs(lastEffective - effectiveMultiplier) < 0.0001f;

            float worldFactor;
            if (multiplierUnchanged) {
                // Nothing to (re)apply: skip the strip/re-add of the MAX modifier entirely, since
                // that churn is what was clamping current HP down and re-triggering the fill-up below.
                worldFactor = knownFactor > 0.01f ? knownFactor : 1.0f;
            } else {
                // applyModifierOnly (not apply): this resync runs periodically for as long as the boss is
                // tracked, so it must not unconditionally fill HP back to max — that would erase player
                // damage every cycle. The wasFull/stuckAtBasePool checks below still catch the legitimate
                // cases (post-spawn, or HP stuck at a stale pool after a real multiplier change).
                worldFactor = BossHealthScale.applyModifierOnly(statMap, desiredMultiplier, knownFactor);
                if (worldFactor > 0.01f && Math.abs(worldFactor - knownFactor) > 0.0001f) {
                    trackingSystem.setWorldHealthFactor(bossUuid, worldFactor);
                }
                lastEffectiveHpMultiplierByBoss.put(bossUuid, Math.max(0.01f, worldFactor) * desiredMultiplier);
            }

            EntityStatValue healthAfter = statMap.get(healthIndex);
            if (healthAfter == null) {
                return;
            }
            float maxAfter = healthAfter.getMax();
            float currentAfter = healthAfter.get();
            float minAfter = healthAfter.getMin();
            if (!Float.isFinite(currentAfter) || currentAfter <= minAfter + 1f) {
                return;
            }
            float bakedFactor = Math.max(0.01f, worldFactor);
            float baseApprox = maxAfter / (bakedFactor * desiredMultiplier);
            boolean stuckAtBasePool = !multiplierUnchanged
                    && (bakedFactor * desiredMultiplier) > 1.01f
                    && maxAfter > baseApprox * 1.5f
                    && Math.abs(currentAfter - baseApprox) <= 1f;
            boolean filled = (wasFull || stuckAtBasePool) && currentAfter + 0.5f < maxAfter;
            if (filled) {
                statMap.maximizeStatValue(EntityStatMap.Predictable.ALL, healthIndex);
                LOGGER.info("BossArena compat filled HP for boss " + bossUuid
                        + ": " + currentAfter + "/" + maxAfter
                        + " (reason=" + (wasFull ? "wasFull" : "stuckAtBasePool")
                        + ", bossMult=" + desiredMultiplier + ", worldFactor=" + worldFactor + ")");
            }
            LOGGER.info("[HPDIAG] resync boss=" + bossUuid
                    + " before(cur=" + currentBefore + ",max=" + maxBefore + ",wasFull=" + wasFull + ")"
                    + " after(cur=" + currentAfter + ",max=" + maxAfter + ")"
                    + " knownFactor=" + knownFactor + " worldFactorNow=" + worldFactor
                    + " desiredMult=" + desiredMultiplier + " multiplierUnchanged=" + multiplierUnchanged
                    + " baseApprox=" + baseApprox + " stuckAtBasePool=" + stuckAtBasePool
                    + " filled=" + filled);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to enforce boss HP scale compatibility for " + bossUuid, e);
        }
    }

    private void enforceBossLevelOverride(UUID bossUuid,
                                          BossTrackingSystem.BossData bossData,
                                          Object rpgPluginInstance) {
        if (bossUuid == null || bossData == null || rpgPluginInstance == null) {
            return;
        }
        if (rpgPutSpawnLevelMethod == null || rpgRemoveSpawnLevelMethod == null) {
            return;
        }

        int desiredLevel = bossData.levelOverride >= 1 ? bossData.levelOverride : 0;
        Integer currentLevel = appliedLevelOverrides.get(bossUuid);

        try {
            if (desiredLevel >= 1) {
                if (currentLevel != null && currentLevel == desiredLevel) {
                    return;
                }
                rpgPutSpawnLevelMethod.invoke(rpgPluginInstance, bossUuid, desiredLevel);
                appliedLevelOverrides.put(bossUuid, desiredLevel);
                return;
            }

            if (currentLevel == null) {
                return;
            }
            rpgRemoveSpawnLevelMethod.invoke(rpgPluginInstance, bossUuid);
            appliedLevelOverrides.remove(bossUuid);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to enforce RPGLeveling level override for " + bossUuid, e);
        }
    }

    private void pruneStaleLevelOverrides(Set<UUID> activeBossUuids, Object rpgPluginInstance) {
        if (appliedLevelOverrides.isEmpty()) {
            return;
        }
        if (rpgPluginInstance == null || rpgRemoveSpawnLevelMethod == null) {
            appliedLevelOverrides.clear();
            return;
        }

        for (UUID bossUuid : new HashSet<>(appliedLevelOverrides.keySet())) {
            if (activeBossUuids.contains(bossUuid)) {
                continue;
            }
            try {
                rpgRemoveSpawnLevelMethod.invoke(rpgPluginInstance, bossUuid);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to clear stale RPGLeveling level override for " + bossUuid, e);
            }
            appliedLevelOverrides.remove(bossUuid);
        }
    }

    private Object resolveRpgLevelingPluginInstance() {
        resolveRpgLevelingLevelApi();
        if (rpgGetMethod == null) {
            return null;
        }
        try {
            return rpgGetMethod.invoke(null);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to get RPGLeveling plugin instance", e);
            return null;
        }
    }

    private void resolveRpgLevelingLevelApi() {
        if (rpgLevelApiResolved) {
            return;
        }
        rpgLevelApiResolved = true;
        try {
            Class<?> pluginClass = Class.forName("org.zuxaw.plugin.RPGLevelingPlugin");
            rpgGetMethod = pluginClass.getMethod("get");
            rpgPutSpawnLevelMethod = pluginClass.getMethod("putSpawnLevelForEntity", UUID.class, int.class);
            rpgRemoveSpawnLevelMethod = pluginClass.getMethod("removeSpawnLevelForEntity", UUID.class);
        } catch (Exception e) {
            rpgGetMethod = null;
            rpgPutSpawnLevelMethod = null;
            rpgRemoveSpawnLevelMethod = null;
            LOGGER.log(Level.INFO, "BossArena compat could not resolve RPGLeveling level override API", e);
        }
    }

    private void resetRpgLevelingCompatState() {
        appliedLevelOverrides.clear();
        rpgGetMethod = null;
        rpgPutSpawnLevelMethod = null;
        rpgRemoveSpawnLevelMethod = null;
        rpgLevelApiResolved = false;
    }

    private boolean isRpgLevelingLoaded() {
        PluginManager pluginManager = PluginManager.get();
        boolean loaded = pluginManager != null
                && pluginManager.hasPlugin(BossArenaPlugin.RPG_LEVELING_PLUGIN_ID, SemverRange.WILDCARD);
        if (!loadStateKnown || loaded != lastRpgLevelingLoaded) {
            if (!loaded) {
                resetRpgLevelingCompatState();
            } else if (loadStateKnown && !lastRpgLevelingLoaded) {
                rpgLevelApiResolved = false;
            }
            LOGGER.info("BossArena compat RPGLeveling loaded state: " + loaded);
            loadStateKnown = true;
            lastRpgLevelingLoaded = loaded;
        }
        return loaded;
    }
}
