package com.varyon.elite;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.elite.component.EliteComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Rolls and applies a weapon + armour set to an elite NPC, mirroring RPGMobs'
 * {@code RPGMobsEquipmentService}: pick a per-tier material and a per-class weapon family,
 * then write the item stacks into the NPC's {@code Armor} / {@code Hotbar} containers. Every
 * id is checked against {@link Item#getAssetMap()} first, so a build without a given asset
 * simply skips that piece. The applied ids are recorded on {@link EliteComponent} so
 * {@link EliteDeeliteSystem} can remove exactly what was added.
 */
public final class EliteEquipment {

    private static final com.hypixel.hytale.logger.HytaleLogger LOGGER =
            com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final String[] ARMOR_SUFFIX = {"Head", "Chest", "Hands", "Legs"};

    /** Armour material candidates by tier (1..5), best-effort fallbacks handled at apply time. */
    private static final String[][] ARMOR_MATERIALS = {
            {},
            {"Leather_Light", "Leather_Soft", "Copper", "Bronze"},
            {"Leather_Medium", "Bronze_Ornate", "Iron", "Bronze"},
            {"Cobalt", "Thorium", "Iron", "Bronze_Ornate"},
            {"Cobalt", "Adamantite", "Thorium"},
            {"Mithril", "Onyxium", "Prisma"},
    };

    /** Weapon material progression by tier (1..5). */
    private static final String[] WEAPON_MATERIAL = {
            "", "Iron", "Iron", "Cobalt", "Adamantite", "Mithril",
    };

    private EliteEquipment() {}

    private static String[] weaponFamilies(@Nonnull MobClass mobClass) {
        return switch (mobClass) {
            case BAGARREUR -> new String[]{"Weapon_Club_", "Weapon_Mace_", "Weapon_Knuckles_"};
            case BERSERKER -> new String[]{"Weapon_Battleaxe_", "Weapon_Axe_"};
            case ASSASSIN -> new String[]{"Weapon_Daggers_"};
            case ARCHER -> new String[]{"Weapon_Shortbow_", "Weapon_Crossbow_"};
            case SEIGNEUR_DE_GUERRE -> new String[]{"Weapon_Longsword_", "Weapon_Spear_", "Weapon_Sword_"};
            case SOIGNEUR -> new String[]{"Weapon_Staff_", "Weapon_Mace_"};
            case BOSS -> new String[]{"Weapon_Longsword_", "Weapon_Battleaxe_", "Weapon_Sword_"};
        };
    }

    /** Number of armour pieces by tier. */
    private static int armorPieces(int tier) {
        return switch (Math.max(1, Math.min(EliteConfig.TIER_COUNT, tier))) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 4;
        };
    }

    public static void apply(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                             @Nonnull NPCEntity npc, @Nonnull MobClass mobClass, int tier, long seed,
                             @Nonnull EliteComponent elite, @Nullable EliteConfig.MobRuleDef rule,
                             boolean humanoid) {
        // Only humanoid elites get gear at all - weapon AND armour. Beasts / constructs / void
        // things keep their bare model (they fight with their scaled stats + abilities).
        if (!humanoid) {
            elite.setEquipment(List.of());
            return;
        }

        Random rng = new Random(seed ^ 0x9E3779B97F4A7C15L);
        List<String> applied = new ArrayList<>();

        // A mob rule narrows the weapon families and armour materials, and can override piece count.
        String[] families = rule != null && !rule.getAllowedWeaponFamilies().isEmpty()
                ? rule.getAllowedWeaponFamilies().toArray(new String[0])
                : weaponFamilies(mobClass);
        List<String> armorMats = rule != null && !rule.getAllowedArmorMaterials().isEmpty()
                ? rule.getAllowedArmorMaterials()
                : null;
        // -1 (default, no override) = tier-based piece count. 0 = the rule explicitly forbids
        // armour (e.g. a praetorian keeps its weapon but never gets armour). >0 = forced count.
        int overrideSlots = rule != null ? rule.getArmorSlotOverride() : -1;
        int armorSlots = overrideSlots >= 0 ? Math.min(4, overrideSlots) : armorPieces(tier);

        try {
            applyWeapon(ref, store, npc, tier, families, rng, applied);
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("elite weapon apply failed: " + e.getMessage());
        }
        if (armorSlots > 0) {
            try {
                applyArmor(ref, store, tier, armorMats, armorSlots, rng, applied);
            } catch (Exception e) {
                LOGGER.at(Level.FINE).log("elite armour apply failed: " + e.getMessage());
            }
        }

        elite.setEquipment(applied);
    }

    /** True if at least one armour piece (not just a weapon) was applied - i.e. the mob gets the
     *  vanilla armour system's own {@code DamageResistance}, so the elite tier's HP multiplier
     *  should be skipped for it (it would double-compensate for survivability). */
    public static boolean hasArmorPiece(@Nonnull List<String> equipment) {
        for (String id : equipment) {
            if (id.startsWith("Armor_")) {
                return true;
            }
        }
        return false;
    }

    public static void strip(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                             @Nonnull List<String> equipment) {
        if (equipment.isEmpty()) {
            return;
        }
        clearMatching(store.getComponent(ref, InventoryComponent.Armor.getComponentType()), equipment);
        clearMatching(store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()), equipment);
    }

    private static void applyWeapon(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                    @Nonnull NPCEntity npc, int tier, @Nonnull String[] families,
                                    @Nonnull Random rng, @Nonnull List<String> applied) {
        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            return;
        }
        ItemContainer container = hotbar.getInventory();
        if (container == null || container.getCapacity() <= 0) {
            return;
        }
        String weaponId = pickWeapon(families, tier, rng);
        if (weaponId == null) {
            return;
        }
        byte slot = hotbar.getActiveSlot();
        if (slot < 0 || slot >= container.getCapacity()) {
            slot = 0;
            try {
                hotbar.setActiveSlot((byte) 0, ref, store);
            } catch (Exception ignored) {
            }
        }
        container.setItemStackForSlot((short) slot, new ItemStack(weaponId, 1));
        applied.add(weaponId);
    }

    private static void applyArmor(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                   int tier, @Nullable List<String> ruleMaterials, int slots,
                                   @Nonnull Random rng, @Nonnull List<String> applied) {
        InventoryComponent.Armor armor = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        if (armor == null) {
            return;
        }
        ItemContainer container = armor.getInventory();
        if (container == null || container.getCapacity() <= 0) {
            return;
        }
        String material = pickArmorMaterial(tier, ruleMaterials, rng);
        if (material == null) {
            return;
        }
        int pieces = Math.min(slots, container.getCapacity());
        for (int i = 0; i < pieces && i < ARMOR_SUFFIX.length; i++) {
            String id = "Armor_" + material + "_" + ARMOR_SUFFIX[i];
            if (!assetExists(id)) {
                continue;
            }
            container.setItemStackForSlot((short) i, new ItemStack(id, 1));
            applied.add(id);
        }
    }

    @Nullable
    private static String pickWeapon(@Nonnull String[] families, int tier, @Nonnull Random rng) {
        int t = Math.max(1, Math.min(EliteConfig.TIER_COUNT, tier));
        // Preferred: family + tier material. Fallbacks: family + lower materials, then any tier variant.
        List<String> materials = new ArrayList<>();
        for (int m = t; m >= 1; m--) {
            materials.add(WEAPON_MATERIAL[m]);
        }
        materials.add("Copper");
        materials.add("Crude");
        materials.add("Iron");
        for (String family : shuffled(families, rng)) {
            for (String mat : materials) {
                String id = family + mat;
                if (assetExists(id)) {
                    return id;
                }
            }
        }
        return null;
    }

    @Nullable
    private static String pickArmorMaterial(int tier, @Nullable List<String> ruleMaterials, @Nonnull Random rng) {
        if (ruleMaterials != null && !ruleMaterials.isEmpty()) {
            List<String> shuffled = new ArrayList<>(ruleMaterials);
            java.util.Collections.shuffle(shuffled, rng);
            for (String mat : shuffled) {
                if (assetExists("Armor_" + mat + "_Chest")) {
                    return mat;
                }
            }
        }
        int t = Math.max(1, Math.min(EliteConfig.TIER_COUNT, tier));
        for (int band = t; band >= 1; band--) {
            for (String mat : shuffled(ARMOR_MATERIALS[band], rng)) {
                if (assetExists("Armor_" + mat + "_Chest")) {
                    return mat;
                }
            }
        }
        return null;
    }

    private static void clearMatching(@Nullable InventoryComponent component, @Nonnull List<String> ids) {
        if (component == null) {
            return;
        }
        ItemContainer container = component.getInventory();
        if (container == null) {
            return;
        }
        for (short i = 0; i < container.getCapacity(); i++) {
            try {
                ItemStack stack = container.getItemStack(i);
                if (stack != null && !stack.isEmpty() && ids.contains(stack.getItemId())) {
                    container.setItemStackForSlot(i, null);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean assetExists(@Nonnull String itemId) {
        try {
            return Item.getAssetMap().getAsset(itemId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static String[] shuffled(@Nonnull String[] src, @Nonnull Random rng) {
        String[] copy = src.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            String tmp = copy[i];
            copy[i] = copy[j];
            copy[j] = tmp;
        }
        return copy;
    }
}
