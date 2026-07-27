package com.varyon.bossarena.util;

import com.varyon.bossarena.spawn.BossSpawnService;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

/**
 * Applies BossArena HP multipliers without destroying zone/world HP scaling.
 * {@code Varyon_Health} / RPG mods are removed (they sum-stack badly with ours) but their
 * contribution is baked into {@code BossArena.HealthMultiplier} via a captured world factor:
 * {@code finalMax ≈ assetMax × worldFactor × bossHpMult}.
 */
public final class BossHealthScale {
    private static final String ZONE_HEALTH_KEY = "Varyon_Health";
    private static final String RPG_HEALTH_KEY = "RPGLeveling.HPModifier";
    private static final float MIN_FACTOR = 0.01f;
    private static final float MAX_FACTOR = 1000.0f;

    private BossHealthScale() {}

    /**
     * @param knownWorldFactor {@code <= 0} to capture from current stats; otherwise reuse stored factor
     * @return world factor to persist on the tracked entity
     */
    public static float apply(EntityStatMap statMap, float bossHpMult, float knownWorldFactor) {
        // First call for this entity (factor not yet captured): fills to max, since HP hasn't been
        // dealt any damage yet at this point in the spawn flow.
        boolean firstCapture = !(knownWorldFactor > MIN_FACTOR);
        float worldFactor = applyModifierOnly(statMap, bossHpMult, knownWorldFactor);
        if (firstCapture) {
            int healthIndex = DefaultEntityStatTypes.getHealth();
            if (healthIndex >= 0) {
                fillToMax(statMap, healthIndex);
            }
        }
        return worldFactor;
    }

    /**
     * Re-asserts the MAX HP modifier for the given multiplier/world factor without touching the
     * current HP value. Safe to call repeatedly (e.g. from a periodic resync system) — unlike
     * {@link #apply}, it never fills the boss back to full HP, so it won't undo player damage.
     * @param knownWorldFactor {@code <= 0} to capture from current stats; otherwise reuse stored factor
     * @return world factor to persist on the tracked entity
     */
    public static float applyModifierOnly(EntityStatMap statMap, float bossHpMult, float knownWorldFactor) {
        if (statMap == null) {
            return 1.0f;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex < 0) {
            return 1.0f;
        }

        float bossMult = sanitizeBossMult(bossHpMult);
        float worldFactor = knownWorldFactor > MIN_FACTOR
                ? clampFactor(knownWorldFactor)
                : captureWorldFactor(statMap, healthIndex);

        stripCompetingModifiers(statMap, healthIndex);

        float effective = clampFactor(worldFactor * bossMult);
        StaticModifier healthMod = new StaticModifier(
                Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.MULTIPLICATIVE,
                effective
        );
        statMap.putModifier(
                EntityStatMap.Predictable.ALL,
                healthIndex,
                BossSpawnService.HEALTH_MODIFIER_KEY,
                healthMod
        );
        return worldFactor;
    }

    /** Re-assert baked modifier using a previously captured world factor. */
    public static void enforce(EntityStatMap statMap, float bossHpMult, float worldFactor) {
        apply(statMap, bossHpMult, worldFactor > MIN_FACTOR ? worldFactor : 0.0f);
    }

    private static float captureWorldFactor(EntityStatMap statMap, int healthIndex) {
        EntityStatValue health = statMap.get(healthIndex);
        if (health == null) {
            return 1.0f;
        }
        float maxBefore = health.getMax();
        if (!Float.isFinite(maxBefore) || maxBefore <= 0f) {
            return 1.0f;
        }

        float prevBoss = readMultiplicativeAmount(
                statMap.getModifier(healthIndex, BossSpawnService.HEALTH_MODIFIER_KEY),
                1.0f
        );
        float worldMax = maxBefore / prevBoss;

        stripCompetingModifiers(statMap, healthIndex);

        EntityStatValue after = statMap.get(healthIndex);
        float assetMax = after != null ? after.getMax() : 0f;
        if (!Float.isFinite(assetMax) || assetMax <= MIN_FACTOR) {
            return 1.0f;
        }
        return clampFactor(worldMax / assetMax);
    }

    private static void stripCompetingModifiers(EntityStatMap statMap, int healthIndex) {
        removeModifierQuiet(statMap, healthIndex, ZONE_HEALTH_KEY);
        removeModifierQuiet(statMap, healthIndex, RPG_HEALTH_KEY);
        removeModifierQuiet(statMap, healthIndex, BossSpawnService.HEALTH_MODIFIER_KEY);
    }

    private static void removeModifierQuiet(EntityStatMap statMap, int healthIndex, String key) {
        if (statMap.getModifier(healthIndex, key) == null) {
            return;
        }
        statMap.removeModifier(EntityStatMap.Predictable.ALL, healthIndex, key);
    }

    private static void fillToMax(EntityStatMap statMap, int healthIndex) {
        statMap.maximizeStatValue(EntityStatMap.Predictable.ALL, healthIndex);
        EntityStatValue health = statMap.get(healthIndex);
        if (health == null) {
            return;
        }
        float current = health.get();
        float max = health.getMax();
        if (max > 0f && current + 0.5f < max) {
            statMap.setStatValue(EntityStatMap.Predictable.ALL, healthIndex, max);
        }
    }

    private static float readMultiplicativeAmount(Modifier modifier, float fallback) {
        if (modifier instanceof StaticModifier staticModifier
                && staticModifier.getTarget() == Modifier.ModifierTarget.MAX
                && staticModifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE) {
            float amount = staticModifier.getAmount();
            if (Float.isFinite(amount) && amount > MIN_FACTOR) {
                return amount;
            }
        }
        return fallback;
    }

    private static float sanitizeBossMult(float bossHpMult) {
        if (!Float.isFinite(bossHpMult) || bossHpMult <= 0f) {
            return 1.0f;
        }
        return bossHpMult;
    }

    private static float clampFactor(float value) {
        if (!Float.isFinite(value)) {
            return 1.0f;
        }
        if (value < MIN_FACTOR) {
            return MIN_FACTOR;
        }
        if (value > MAX_FACTOR) {
            return MAX_FACTOR;
        }
        return value;
    }
}
