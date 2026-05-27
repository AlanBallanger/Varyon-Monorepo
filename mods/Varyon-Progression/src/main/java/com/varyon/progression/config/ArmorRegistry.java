package com.varyon.progression.config;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ArmorRegistry {

    private static final Map<String, Integer> itemLevel = new HashMap<>();

    static {
        itemLevel.put("Armor_Iron_Head", 1);
        itemLevel.put("Armor_Iron_Chest", 1);
        itemLevel.put("Armor_Iron_Hands", 1);
        itemLevel.put("Armor_Iron_Legs", 1);

        itemLevel.put("Armor_Thorium_Head", 2);
        itemLevel.put("Armor_Thorium_Chest", 2);
        itemLevel.put("Armor_Thorium_Hands", 2);
        itemLevel.put("Armor_Thorium_Legs", 2);

        itemLevel.put("Armor_Cobalt_Head", 3);
        itemLevel.put("Armor_Cobalt_Chest", 3);
        itemLevel.put("Armor_Cobalt_Hands", 3);
        itemLevel.put("Armor_Cobalt_Legs", 3);

        itemLevel.put("Armor_Adamantite_Head", 4);
        itemLevel.put("Armor_Adamantite_Chest", 4);
        itemLevel.put("Armor_Adamantite_Hands", 4);
        itemLevel.put("Armor_Adamantite_Legs", 4);

        itemLevel.put("Armor_Mithril_Head", 5);
        itemLevel.put("Armor_Mithril_Chest", 5);
        itemLevel.put("Armor_Mithril_Hands", 5);
        itemLevel.put("Armor_Mithril_Legs", 5);

        itemLevel.put("Armor_Onyxium_Head", 5);
        itemLevel.put("Armor_Onyxium_Chest", 5);
        itemLevel.put("Armor_Onyxium_Hands", 5);
        itemLevel.put("Armor_Onyxium_Legs", 5);

        var map = Item.getAssetMap().getAssetMap();
        map.keySet().stream()
                .filter(key -> key.startsWith("BlameJared_MinersHelmet_Armor_"))
                .forEach(key -> itemLevel.put(key, 1));
    }

    public static int getArmorLevel(String itemId) {
        return itemLevel.getOrDefault(itemId, 0);
    }

    public static int getArmorLevel(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        return getArmorLevel(itemStack.getItemId());
    }
}
