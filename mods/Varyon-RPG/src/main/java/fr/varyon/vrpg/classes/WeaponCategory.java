package fr.varyon.vrpg.classes;

import javax.annotation.Nullable;

public enum WeaponCategory {
    EPEE,
    DAGUE,
    HACHE,
    DEUX_MAINS,
    DISTANCE,
    MAGIE,
    AUTRE;

    @Nullable
    public static WeaponCategory fromItemId(@Nullable String itemId) {
        if (itemId == null) return null;
        String id = itemId.toLowerCase();

        if (id.startsWith("weapon_daggers") || id.startsWith("weapon_dagger")) return DAGUE;
        if (id.startsWith("weapon_longsword") || id.startsWith("weapon_greatsword") || id.startsWith("weapon_leaf_spear") || id.startsWith("weapon_spear")) return DEUX_MAINS;
        if (id.startsWith("weapon_sword")) return EPEE;
        if (id.startsWith("weapon_axe")) return HACHE;
        if (id.startsWith("weapon_shortbow") || id.startsWith("weapon_crossbow") || id.startsWith("weapon_bow")) return DISTANCE;
        if (id.startsWith("weapon_staff") || id.startsWith("weapon_wand") || id.startsWith("weapon_spellbook")) return MAGIE;

        return AUTRE;
    }

    public double getMultiplierFor(@Nullable PlayerSpecialization spec) {
        if (spec == null) return 1.0;
        return switch (this) {
            case EPEE       -> spec.getWeaponEpeeMult();
            case DAGUE      -> spec.getWeaponDagueMult();
            case HACHE      -> spec.getWeaponHacheMult();
            case DEUX_MAINS -> spec.getWeaponDeuxMainsMult();
            case DISTANCE   -> spec.getWeaponDistanceMult();
            case MAGIE      -> spec.getWeaponMagieMult();
            case AUTRE      -> 1.0;
        };
    }
}
