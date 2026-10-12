package com.varyon.elite.ability;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tuning per {@link EliteAbility}: cooldown, range band, telegraph wind-up, effect payload.
 * Values are deliberate constants - the dashboard exposes them read-only for now.
 */
public final class EliteAbilitySpec {

    public final long cooldownMs;
    public final double minRange;
    public final double maxRange;
    public final long telegraphMs;
    public final double radius;
    public final float basePower;

    private EliteAbilitySpec(long cooldownMs, double minRange, double maxRange,
                             long telegraphMs, double radius, float basePower) {
        this.cooldownMs = cooldownMs;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.telegraphMs = telegraphMs;
        this.radius = radius;
        this.basePower = basePower;
    }

    private static final Map<EliteAbility, EliteAbilitySpec> SPECS = new EnumMap<>(EliteAbility.class);

    static {
        SPECS.put(EliteAbility.CHARGE_LEAP, new EliteAbilitySpec(9000, 4, 14, 700, 3.0, 8f));
        SPECS.put(EliteAbility.LEAP, new EliteAbilitySpec(8000, 4, 12, 600, 2.5, 6f));
        SPECS.put(EliteAbility.FORWARD_CHARGE, new EliteAbilitySpec(9000, 5, 16, 700, 2.5, 7f));
        SPECS.put(EliteAbility.DASH, new EliteAbilitySpec(5000, 3, 10, 250, 2.0, 4f));
        SPECS.put(EliteAbility.DODGE_ROLL, new EliteAbilitySpec(6000, 0, 6, 200, 0, 0f));
        SPECS.put(EliteAbility.WAR_CRY, new EliteAbilitySpec(14000, 0, 8, 500, 6.0, 0f));
        SPECS.put(EliteAbility.UPPERCUT, new EliteAbilitySpec(6000, 0, 3.5, 350, 2.0, 6f));
        SPECS.put(EliteAbility.PILEDRIVER, new EliteAbilitySpec(11000, 0, 3.5, 900, 3.5, 12f));
        SPECS.put(EliteAbility.MULTI_SLASH_SHORT, new EliteAbilitySpec(4500, 0, 3.0, 250, 2.0, 3f));
        SPECS.put(EliteAbility.MULTI_SLASH_MEDIUM, new EliteAbilitySpec(6000, 0, 4.5, 300, 3.0, 4f));
        SPECS.put(EliteAbility.MULTI_SLASH_LONG, new EliteAbilitySpec(9000, 0, 6.0, 800, 4.0, 6f));
        SPECS.put(EliteAbility.VOLLEY, new EliteAbilitySpec(6000, 6, 22, 300, 2.0, 4f));
        SPECS.put(EliteAbility.ARROW_RAIN, new EliteAbilitySpec(12000, 6, 24, 900, 5.0, 5f));
        SPECS.put(EliteAbility.HEALING_AURA, new EliteAbilitySpec(15000, 0, 12, 400, 8.0, 0f));
        SPECS.put(EliteAbility.HEAL_LEAP, new EliteAbilitySpec(13000, 0, 12, 500, 0, 0f));
        SPECS.put(EliteAbility.SHIELD, new EliteAbilitySpec(16000, 0, 10, 300, 0, 0f));
        SPECS.put(EliteAbility.BLEED, new EliteAbilitySpec(7000, 0, 4.0, 300, 2.0, 3f));
        SPECS.put(EliteAbility.SUMMON_MINIONS, new EliteAbilitySpec(20000, 0, 16, 1000, 0, 0f));
    }

    @Nonnull
    public static EliteAbilitySpec of(@Nonnull EliteAbility ability) {
        return SPECS.getOrDefault(ability, new EliteAbilitySpec(8000, 0, 6, 400, 2.0, 4f));
    }
}
