package fr.varyon.vrpg.ui.classes;

import javax.annotation.Nullable;

public record SkillStatDisplay(
    @Nullable Integer weaponDamagePct,
    @Nullable String cooldown,
    @Nullable Integer staminaCost,
    @Nullable Integer manaCost,
    @Nullable String fallbackText
) {
    public static SkillStatDisplay fallback(@Nullable String text) {
        return new SkillStatDisplay(null, null, null, null, text);
    }

    public boolean usesIconLayout() {
        if (fallbackText != null && weaponDamagePct == null && cooldown == null
            && staminaCost == null && manaCost == null) {
            return false;
        }
        return cooldown != null && (weaponDamagePct != null || staminaCost != null || manaCost != null);
    }

    public boolean showDamage() {
        return weaponDamagePct != null;
    }

    public boolean showCooldown() {
        return cooldown != null && !cooldown.isBlank();
    }

    public boolean showStamina() {
        return staminaCost != null && manaCost == null;
    }

    public boolean showMana() {
        return manaCost != null;
    }
}
