package com.faiizer.craftrestrict.recipes;

import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Lists distinct craftable item ids (the primary output of every registered CraftingRecipe),
 * used by the CraftRestrict UI to let admins pick items to restrict.
 */
public final class CraftableItemsIndex {

    private CraftableItemsIndex() {
    }

    @SuppressWarnings("unchecked")
    public static List<String> allCraftableItemIds() {
        try {
            Map<String, CraftingRecipe> internalMap = CraftingRecipe.getAssetMap().getAssetMap();
            if (internalMap == null || internalMap.isEmpty()) {
                return List.of();
            }
            TreeSet<String> ids = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (Map.Entry<String, CraftingRecipe> entry : internalMap.entrySet()) {
                String recipeId = entry.getKey();
                if (recipeId == null || recipeId.endsWith("_Restricted")) {
                    continue;
                }
                CraftingRecipe recipe = entry.getValue();
                if (recipe == null || recipe.getPrimaryOutput() == null) {
                    continue;
                }
                String outputId = recipe.getPrimaryOutput().getItemId();
                if (outputId != null && !outputId.isBlank()) {
                    ids.add(outputId);
                }
            }
            return new ArrayList<>(ids);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<String> filterItemIds(String query, int limit) {
        List<String> all = allCraftableItemIds();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String itemId : all) {
            if (needle.isEmpty() || itemId.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(itemId);
                if (matches.size() >= Math.max(1, limit)) {
                    break;
                }
            }
        }
        return matches;
    }

    public static List<String> listWorldNames() {
        try {
            var worlds = com.hypixel.hytale.server.core.universe.Universe.get().getWorlds();
            if (worlds == null || worlds.isEmpty()) {
                return List.of();
            }
            List<String> names = new ArrayList<>(worlds.keySet());
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            return names;
        } catch (Exception e) {
            return List.of();
        }
    }
}
