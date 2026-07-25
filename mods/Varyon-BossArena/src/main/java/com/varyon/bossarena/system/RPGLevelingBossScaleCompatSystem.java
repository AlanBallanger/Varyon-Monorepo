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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps BossArena HP/level overrides on tracked bosses when RPGLeveling / zone scaling is present.
 * Re-applies the baked world HP factor so zone scale is not lost when competing modifiers are stripped.
 */
public final class RPGLevelingBossScaleCompatSystem extends TickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final float RESYNC_INTERVAL_SECONDS = 0.25f;

    private final BossTrackingSystem trackingSystem;
    private float elapsedSeconds;
    private boolean lastRpgLevelingLoaded;
    private boolean loadStateKnown;
    private final Map<UUID, Integer> appliedLevelOverrides = new HashMap<>();
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

        elapsedSeconds += Math.max(0f, dt);
        if (elapsedSeconds < RESYNC_INTERVAL_SECONDS) {
            return;
        }
        elapsedSeconds = 0f;

        Map<UUID, BossTrackingSystem.BossData> trackedBosses = trackingSystem.snapshotTrackedBosses();
        Set<UUID> activeBossUuids = new HashSet<>(trackedBosses.keySet());
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
            float worldFactor = BossHealthScale.apply(statMap, desiredMultiplier, knownFactor);
            if (worldFactor > 0.01f && Math.abs(worldFactor - knownFactor) > 0.0001f) {
                trackingSystem.setWorldHealthFactor(bossUuid, worldFactor);
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
            boolean stuckAtBasePool = (bakedFactor * desiredMultiplier) > 1.01f
                    && maxAfter > baseApprox * 1.5f
                    && Math.abs(currentAfter - baseApprox) <= 1f;
            if (wasFull || stuckAtBasePool) {
                if (currentAfter + 0.5f < maxAfter) {
                    statMap.maximizeStatValue(EntityStatMap.Predictable.ALL, healthIndex);
                    LOGGER.log(
                            Level.INFO,
                            "BossArena compat filled scaled HP for boss {0}: {1} -> {2} (bossMult={3}, worldFactor={4})",
                            new Object[]{
                                    bossUuid,
                                    String.format("%.1f/%.1f", currentAfter, maxAfter),
                                    String.format("%.1f/%.1f", healthAfter.get(), healthAfter.getMax()),
                                    String.format("%.4f", desiredMultiplier),
                                    String.format("%.4f", worldFactor)
                            }
                    );
                }
            }
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
