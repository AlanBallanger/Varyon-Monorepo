package fr.varyon.vrpg.classes;

import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import javax.annotation.Nullable;

public enum WeaponCategory {
    EPEE,
    DAGUE,
    HACHE,
    DEUX_MAINS,
    LANCE,
    DISTANCE,
    MAGIE,
    BOUCLIER,
    GANTS,
    AUTRE;

    @Nullable
    public static WeaponCategory fromItemId(@Nullable String itemId) {
        if (itemId == null) return null;
        String id = itemId.toLowerCase();

        if (matchesAny(id, "daggers", "dagger")) return DAGUE;
        if (matchesAny(id, "leaf_spear", "spear")) return LANCE;
        if (matchesAny(id, "longsword", "greatsword", "club", "battleaxe")) return DEUX_MAINS;
        if (matchesAny(id, "sword")) return EPEE;
        if (matchesAny(id, "axe")) return HACHE;
        if (matchesAny(id, "shortbow", "crossbow", "bow")) return DISTANCE;
        if (matchesAny(id, "staff", "wand", "spellbook")) return MAGIE;
        if (matchesAny(id, "shield")) return BOUCLIER;
        if (matchesAny(id, "knuckle", "knuckles", "gloves")) return GANTS;

        return AUTRE;
    }

    public static boolean isCrossbow(@Nullable String itemId) {
        if (itemId == null) return false;
        return matchesAny(itemId.toLowerCase(), "crossbow");
    }

    public static boolean isShortbow(@Nullable String itemId) {
        if (itemId == null) return false;
        String id = itemId.toLowerCase();
        if (matchesAny(id, "crossbow")) return false;
        return matchesAny(id, "shortbow", "bow");
    }

    private static boolean matchesAny(String id, String... keywords) {
        for (String keyword : keywords) {
            if (matchesKeyword(id, keyword)) return true;
        }
        return false;
    }

    private static boolean matchesKeyword(String id, String keyword) {
        if (id.equals(keyword)) return true;
        if (id.startsWith(keyword + "_")) return true;
        if (id.endsWith("_" + keyword)) return true;
        if (id.contains("_" + keyword + "_")) return true;
        return id.contains(keyword);
    }

    @Nullable
    public static WeaponCategory heldCategory(@Nullable com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        if (playerRef == null) return null;
        try {
            var hotbar = playerRef.getComponent(
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return null;
            byte slot = hotbar.getActiveSlot();
            com.hypixel.hytale.server.core.inventory.ItemStack held = hotbar.getInventory().getItemStack((short) slot);
            if (held == null || held.isEmpty()) return null;
            return fromItemId(held.getItemId());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean specCanUseHeldWeapon(@Nullable PlayerSpecialization spec,
                                                @Nullable com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        if (spec == null) return false;
        WeaponCategory cat = heldCategory(playerRef);
        if (cat == null || cat == AUTRE) return false;
        return cat.getMultiplierFor(spec) > 1.0;
    }

    public double getMultiplierFor(@Nullable PlayerSpecialization spec) {
        if (spec == null) return 1.0;
        return switch (this) {
            case EPEE       -> spec.getWeaponEpeeMult();
            case DAGUE      -> spec.getWeaponDagueMult();
            case HACHE      -> spec.getWeaponHacheMult();
            case DEUX_MAINS -> spec.getWeaponDeuxMainsMult();
            case LANCE      -> spec.getWeaponLanceMult();
            case DISTANCE   -> spec.getWeaponDistanceMult();
            case MAGIE      -> spec.getWeaponMagieMult();
            case BOUCLIER   -> 1.0;
            case GANTS      -> spec == PlayerSpecialization.BAGARREUR ? 1.50 : 0.75;
            case AUTRE      -> 1.0;
        };
    }
}
