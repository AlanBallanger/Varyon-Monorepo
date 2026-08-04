package com.faiizer.craftrestrict.recipes;

import com.faiizer.craftrestrict.Main;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class RecipesManager {

    private static HytaleLogger logger;

    public static void init() {
        logger = Main.getPluginInstance().getLogger();
    }

    public static void preWarmCache() {
        DefaultAssetMap assetMap = CraftingRecipe.getAssetMap();
        int count = 0;
        int totalInRegistry = assetMap.getAssetMap().size();
        logger.at(Level.INFO).log("Pre-warming recipe cache...");
        try {
            Map internalMap = assetMap.getAssetMap();
            ArrayList recipeIds = new ArrayList(internalMap.keySet());
            for (Object idObj : recipeIds) {
                String recipeId = (String) idObj;
                if (recipeId == null || recipeId.endsWith("_Restricted") || RecipesCache.hasRestrictedVersion(recipeId)) {
                    continue;
                }
                CraftingRecipe original = (CraftingRecipe) assetMap.getAsset(recipeId);
                if (original == null) {
                    continue;
                }
                RecipesManager.addRestrictedRecipe(original);
                ++count;
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("CRITICAL ERROR during pre-warm discovery: " + e.getMessage());
        }
        logger.at(Level.INFO).log(String.format("[Cache] Status: %d recipes restricted (%d newly discovered) | Registry total: %d", RecipesCache.getSize(), count, totalInRegistry));
    }

    public static void addRestrictedRecipe(CraftingRecipe original) {
        String originalId = original.getId();
        String restrictedId = originalId + "_Restricted";
        if (RecipesCache.hasRestrictedVersion(originalId)) {
            RecipesCache.getRestrictedId(originalId);
            return;
        }
        try {
            BenchRequirement[] restrictedReqs;
            BenchRequirement[] originalReqs = original.getBenchRequirement();
            if (originalReqs != null && originalReqs.length > 0) {
                ArrayList<BenchRequirement> tempList = new ArrayList<>();
                for (BenchRequirement req : originalReqs) {
                    if (req.type.name().equalsIgnoreCase("StructuralCrafting")) {
                        continue;
                    }
                    String[] newCategories = req.categories;
                    if (req.categories != null) {
                        newCategories = Arrays.stream(req.categories)
                                .filter(cat -> !cat.equalsIgnoreCase("Tools"))
                                .toArray(String[]::new);
                    }
                    tempList.add(new BenchRequirement(req.type, req.id, newCategories, 999));
                }
                restrictedReqs = tempList.toArray(new BenchRequirement[0]);
            } else {
                restrictedReqs = new BenchRequirement[]{};
            }
            CraftingRecipe restrictedRecipe = new CraftingRecipe(
                    original.getInput(),
                    original.getPrimaryOutput(),
                    original.getOutputs(),
                    original.getPrimaryOutput().getQuantity(),
                    restrictedReqs,
                    original.getTimeSeconds(),
                    original.isKnowledgeRequired(),
                    original.getRequiredMemoriesLevel());
            RecipesManager.forceSetId(restrictedRecipe, restrictedId);
            RecipesManager.injectIntoInternalRegistry(restrictedId, restrictedRecipe);
            RecipesManager.syncWithAssetStore(originalId, restrictedRecipe);
            RecipesCache.register(originalId, restrictedId);
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Failed to inject restricted recipe: " + restrictedId);
            logger.at(Level.SEVERE).log(e.getMessage());
        }
    }

    private static void forceSetId(CraftingRecipe recipe, String id) throws Exception {
        Field idField = RecipesManager.findField(recipe.getClass(), "id");
        if (idField != null) {
            idField.setAccessible(true);
            idField.set(recipe, id);
        }
    }

    private static void injectIntoInternalRegistry(String id, CraftingRecipe recipe) throws Exception {
        DefaultAssetMap assetMap = CraftingRecipe.getAssetMap();
        Field mapField = RecipesManager.findField(assetMap.getClass(), "assetMap");
        if (mapField != null) {
            mapField.setAccessible(true);
            Map internalMap = (Map) mapField.get(assetMap);
            internalMap.put(id, recipe);
        }
    }

    private static void syncWithAssetStore(String originalId, CraftingRecipe restricted) {
        String packName = CraftingRecipe.getAssetMap().getAssetPack(originalId);
        if (packName == null) {
            packName = "Hytale:Hytale";
        }
        CraftingRecipe.getAssetStore().loadAssets(packName, Collections.singletonList(restricted), AssetUpdateQuery.DEFAULT, true);
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
