package com.varyon.elite.ability;

/**
 * Active abilities an elite can roll into its pool. Class passives are no longer abilities -
 * they are infinite effects tied to {@link com.varyon.elite.MobClass}.
 */
public enum EliteAbility {

    CHARGE_LEAP(true),
    LEAP(true),
    FORWARD_CHARGE(true),
    DASH(false),
    DODGE_ROLL(false),
    WAR_CRY(false),
    UPPERCUT(false),
    PILEDRIVER(true),
    MULTI_SLASH_SHORT(false),
    MULTI_SLASH_MEDIUM(false),
    MULTI_SLASH_LONG(true),
    VOLLEY(false),
    ARROW_RAIN(true),
    HEALING_AURA(false),
    HEAL_LEAP(false),
    SHIELD(false),
    BLEED(false),
    SUMMON_MINIONS(true);

    private final boolean telegraphed;

    EliteAbility(boolean telegraphed) {
        this.telegraphed = telegraphed;
    }

    /** Heavy attacks show a visual telegraph before firing; light attacks do not. */
    public boolean isTelegraphed() {
        return telegraphed;
    }
}
