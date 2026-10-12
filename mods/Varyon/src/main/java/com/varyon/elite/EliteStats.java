package com.varyon.elite;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

import javax.annotation.Nonnull;

/**
 * Per-tier elite combat maths and the Health modifier plumbing. Kept deliberately simple:
 * HP is scaled with a multiplicative {@code Elite_Health} modifier layered on top of Varyon's
 * {@code Varyon_Health} zone modifier; damage dealt is handled by {@code EliteDamageDealtSystem}
 * reading the elite's rolled damage multiplier. Tankiness comes only from the class passive's
 * {@code DamageResistance} - there is deliberately no per-tier damage-taken multiplier here, it
 * used to stack with the class resistance and make high-tier elites near-untouchable.
 */
public final class EliteStats {

    public static final String HEALTH_MODIFIER_KEY = "Elite_Health";

    private EliteStats() {}

    /**
     * Rolls concrete HP / damage / size multipliers for an elite from its tier's configured
     * range, using the elite's deterministic seed so the same spawn always yields the same values.
     */
    public static Rolled roll(@Nonnull EliteConfig config, int tier, long seed) {
        EliteConfig.ScalingDef def = config.getScaling(tier);
        return new Rolled(
                def.rollHp(EliteRoller.roll01(seed, 0x48A3L)),
                def.rollDamage(EliteRoller.roll01(seed, 0x9C71L)),
                def.rollSize(EliteRoller.roll01(seed, 0xE24DL)));
    }

    public record Rolled(float hpMultiplier, float damageMultiplier, float sizeMultiplier) {}

    /** Extra outgoing damage while enraged (Berserker passive / ENRAGE ability). */
    public static float enrageBonusMultiplier() {
        return 1.5f;
    }

    /** HP fraction under which an enrage trigger fires. */
    public static float enrageThreshold() {
        return 0.30f;
    }

    /**
     * Applies (or re-applies) the {@code Elite_Health} MAX modifier with the given rolled
     * multiplier and sets current HP to the new maximum, so the elite spawns full instead of
     * regenerating up from its pre-elite max. Mirrors {@code MobScalingRefSystem}: the target
     * value is computed from the max captured <em>before</em> the modifier is added, since the
     * stat map does not always recompute {@code getMax()} synchronously on {@code putModifier}.
     */
    public static void applyHealthAtSpawn(@Nonnull EntityStatMap statMap, float hpMultiplier) {
        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex < 0) {
            return;
        }
        float mult = Math.max(0.01f, hpMultiplier);

        EntityStatValue before = statMap.get(healthIndex);
        float baseMax = before != null ? before.getMax() : 0f;

        StaticModifier mod = new StaticModifier(
                Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.MULTIPLICATIVE,
                mult);
        statMap.putModifier(EntityStatMap.Predictable.ALL, healthIndex, HEALTH_MODIFIER_KEY, mod);

        // Prefer the freshly recomputed max if the engine did update it; otherwise fall back
        // to baseMax * mult from the captured value.
        EntityStatValue after = statMap.get(healthIndex);
        float target = 0f;
        if (after != null && after.getMax() > baseMax + 0.5f) {
            target = after.getMax();
        } else if (baseMax > 0f) {
            target = baseMax * mult;
        }
        if (target > 0f) {
            statMap.setStatValue(EntityStatMap.Predictable.ALL, healthIndex, target);
        }
    }

    /** Removes the elite Health modifier; the engine clamps current HP back to the lower MAX. */
    public static void stripHealth(@Nonnull EntityStatMap statMap) {
        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex < 0) {
            return;
        }
        if (statMap.getModifier(healthIndex, HEALTH_MODIFIER_KEY) != null) {
            statMap.removeModifier(EntityStatMap.Predictable.ALL, healthIndex, HEALTH_MODIFIER_KEY);
        }
    }

    public static float healthFraction(@Nonnull EntityStatMap statMap) {
        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex < 0) {
            return 1.0f;
        }
        EntityStatValue health = statMap.get(healthIndex);
        if (health == null || health.getMax() <= 0f) {
            return 1.0f;
        }
        return health.get() / health.getMax();
    }

    public static void heal(@Nonnull EntityStatMap statMap, float amount) {
        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex < 0 || amount <= 0f) {
            return;
        }
        EntityStatValue health = statMap.get(healthIndex);
        if (health == null) {
            return;
        }
        float target = Math.min(health.getMax(), health.get() + amount);
        statMap.setStatValue(EntityStatMap.Predictable.ALL, healthIndex, target);
    }
}
