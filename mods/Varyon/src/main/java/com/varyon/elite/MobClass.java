package com.varyon.elite;

import com.varyon.elite.ability.EliteAbility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Elite classes. Each carries an active-ability {@code pool} (abilities are rolled from it at
 * spawn) and a class <b>passive</b>:
 * <ul>
 *   <li>{@link #passiveEffectId} - an infinite {@code EntityEffect} asset applied for the
 *       lifetime of the elite (speed, damage resistance, HP regen, knockback...).</li>
 *   <li>{@link #dynamicEnrage} - the Berserker's enrage, handled in code (a damage boost that
 *       kicks in below 30% HP) rather than a permanent effect.</li>
 * </ul>
 */
public enum MobClass {

    BAGARREUR(1, 5, "Varyon_Elite_Passive_Bagarreur", false, List.of(
            EliteAbility.CHARGE_LEAP, EliteAbility.MULTI_SLASH_SHORT, EliteAbility.MULTI_SLASH_MEDIUM,
            EliteAbility.UPPERCUT, EliteAbility.PILEDRIVER)),

    BERSERKER(1, 5, null, true, List.of(
            EliteAbility.LEAP, EliteAbility.FORWARD_CHARGE, EliteAbility.WAR_CRY)),

    ASSASSIN(1, 5, "Varyon_Elite_Passive_Assassin", false, List.of(
            EliteAbility.DASH, EliteAbility.BLEED, EliteAbility.DODGE_ROLL)),

    ARCHER(1, 5, "Varyon_Elite_Passive_Archer", false, List.of(
            EliteAbility.VOLLEY, EliteAbility.ARROW_RAIN)),

    SEIGNEUR_DE_GUERRE(2, 5, "Varyon_Elite_Passive_SeigneurDeGuerre", false, List.of(
            EliteAbility.LEAP, EliteAbility.FORWARD_CHARGE, EliteAbility.ARROW_RAIN,
            EliteAbility.SUMMON_MINIONS)),

    SOIGNEUR(2, 5, "Varyon_Elite_Passive_Soigneur", false, List.of(
            EliteAbility.HEALING_AURA, EliteAbility.HEAL_LEAP, EliteAbility.SHIELD)),

    BOSS(4, 5, "Varyon_Elite_Passive_Boss", true, List.of(
            EliteAbility.CHARGE_LEAP, EliteAbility.MULTI_SLASH_LONG, EliteAbility.ARROW_RAIN,
            EliteAbility.SUMMON_MINIONS, EliteAbility.HEALING_AURA, EliteAbility.PILEDRIVER,
            EliteAbility.WAR_CRY));

    private final int minTier;
    private final int maxTier;
    @Nullable private final String passiveEffectId;
    private final boolean dynamicEnrage;
    private final List<EliteAbility> pool;

    MobClass(int minTier, int maxTier, @Nullable String passiveEffectId, boolean dynamicEnrage,
             @Nonnull List<EliteAbility> pool) {
        this.minTier = minTier;
        this.maxTier = maxTier;
        this.passiveEffectId = passiveEffectId;
        this.dynamicEnrage = dynamicEnrage;
        this.pool = pool;
    }

    public int getMinTier() {
        return minTier;
    }

    public int getMaxTier() {
        return maxTier;
    }

    /** Infinite effect asset id applied for this class, or {@code null} (e.g. Berserker uses {@link #hasEnrage()}). */
    @Nullable
    public String getPassiveEffectId() {
        return passiveEffectId;
    }

    /** True if this class gets the in-code "below 30% HP -> +damage" enrage. */
    public boolean hasEnrage() {
        return dynamicEnrage;
    }

    @Nonnull
    public List<EliteAbility> getPool() {
        return pool;
    }

    public boolean allowsTier(int tier) {
        return tier >= minTier && tier <= maxTier;
    }

    /** Number of active abilities pulled from the pool for a given elite tier. */
    public int abilityCountForTier(int tier) {
        int count = tier;
        return Math.min(count, pool.size());
    }

    public boolean isNaturalSpawnOnly() {
        return this == BOSS;
    }

    /** Short label shown on the nameplate before the mob name (e.g. "Assassin Skeleton"). */
    @Nonnull
    public String displayName() {
        return switch (this) {
            case BAGARREUR -> "Combattant";
            case BERSERKER -> "Berserker";
            case ASSASSIN -> "Assassin";
            case ARCHER -> "Archer";
            case SEIGNEUR_DE_GUERRE -> "Seigneur de Guerre";
            case SOIGNEUR -> "Soigneur";
            case BOSS -> "Boss";
        };
    }
}
