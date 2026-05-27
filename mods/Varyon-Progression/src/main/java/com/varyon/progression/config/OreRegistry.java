package com.varyon.progression.config;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OreRegistry {

    private static final Map<String, Integer> itemLevel = new HashMap<>();
    private static final Map<String, Integer> blockLevel = new HashMap<>();

    static {
        var rocks = Arrays.asList("Basalt", "Shale", "Slate", "Stone", "Volcanic", "Sandstone", "Magma");

        itemLevel.put("Tool_Pickaxe_Copper", 1);
        itemLevel.put("Tool_Pickaxe_Iron", 2);
        itemLevel.put("Tool_Pickaxe_Thorium", 3);
        itemLevel.put("Tool_Pickaxe_Cobalt", 4);
        itemLevel.put("Tool_Pickaxe_Adamantite", 5);
        itemLevel.put("Tool_Pickaxe_Mithril", 6);
        itemLevel.put("Tool_Pickaxe_Onyxium", 6);

        rocks.forEach(type -> {
            blockLevel.put("Ore_Iron_" + type, 1);
            blockLevel.put("Ore_Thorium_" + type, 2);

            blockLevel.put("Ore_Gold_" + type, 3);
            blockLevel.put("Ore_Cobalt_" + type, 3);

            blockLevel.put("Ore_Silver_" + type, 4);
            blockLevel.put("Ore_Adamantite_" + type, 4);

            blockLevel.put("Ore_Mithril_" + type, 5);
            blockLevel.put("Ore_Onyxium_" + type, 5);
        });
    }

    public static int getPickaxeLevel(String itemId) {
        return itemLevel.getOrDefault(itemId, 0);
    }

    public static int getPickaxeLevel(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        return getPickaxeLevel(itemStack.getItemId());
    }

    public static int getOreLevel(String blockId) {
        return blockLevel.getOrDefault(blockId, 0);
    }
}
