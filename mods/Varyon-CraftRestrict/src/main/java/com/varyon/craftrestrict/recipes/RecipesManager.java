package com.varyon.craftrestrict.recipes;

import com.varyon.craftrestrict.Main;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class RecipesManager {

    private static HytaleLogger logger;
    private static Field cachedIdField;
    private static Field cachedAssetMapField;

    public static void init() {
        logger = Main.getPluginInstance().getLogger();
    }

    public static void preWarmCache() {
        DefaultAssetMap assetMap = CraftingRecipe.getAssetMap();
        int totalInRegistry = assetMap.getAssetMap().size();
        logger.at(Level.INFO).log("Pre-warming recipe cache...");
        Map<String, List<CraftingRecipe>> pendingByPack = new LinkedHashMap<>();
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
                RecipesManager.buildRestrictedRecipe(original, pendingByPack);
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("CRITICAL ERROR during pre-warm discovery: " + e.getMessage());
        }
        int count = pendingByPack.values().stream().mapToInt(List::size).sum();
        RecipesManager.flushPending(pendingByPack);
        logger.at(Level.INFO).log(String.format("[Cache] Status: %d recipes restricted (%d newly discovered) | Registry total: %d", RecipesCache.getSize(), count, totalInRegistry));
    }

    private static void buildRestrictedRecipe(CraftingRecipe original, Map<String, List<CraftingRecipe>> pendingByPack) {
        String originalId = original.getId();
        String restrictedId = originalId + "_Restricted";
        if (RecipesCache.hasRestrictedVersion(originalId)) {
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
                    tempList.add(new BenchRequirement(req.type, req.id, newCategories, 999, req.requiredAugmentTags));
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
            RecipesCache.register(originalId, restrictedId);
            String packName = CraftingRecipe.getAssetMap().getAssetPack(originalId);
            if (packName == null) {
                packName = "Hytale:Hytale";
            }
            pendingByPack.computeIfAbsent(packName, key -> new ArrayList<>()).add(restrictedRecipe);
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Failed to build restricted recipe: " + restrictedId);
            logger.at(Level.SEVERE).log(e.getMessage());
        }
    }

    private static void flushPending(Map<String, List<CraftingRecipe>> pendingByPack) {
        for (Map.Entry<String, List<CraftingRecipe>> entry : pendingByPack.entrySet()) {
            try {
                CraftingRecipe.getAssetStore().loadAssets(entry.getKey(), entry.getValue(), AssetUpdateQuery.DEFAULT, true);
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Failed to batch-load " + entry.getValue().size() + " restricted recipes for pack " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private static void forceSetId(CraftingRecipe recipe, String id) throws Exception {
        Field idField = RecipesManager.cachedIdField(recipe.getClass());
        if (idField != null) {
            idField.set(recipe, id);
        }
    }

    private static void injectIntoInternalRegistry(String id, CraftingRecipe recipe) throws Exception {
        DefaultAssetMap assetMap = CraftingRecipe.getAssetMap();
        Field mapField = RecipesManager.cachedAssetMapField(assetMap.getClass());
        if (mapField != null) {
            Map internalMap = (Map) mapField.get(assetMap);
            internalMap.put(id, recipe);
        }
    }

    private static Field cachedIdField(Class<?> clazz) {
        if (cachedIdField == null) {
            cachedIdField = RecipesManager.findField(clazz, "id");
            if (cachedIdField != null) {
                cachedIdField.setAccessible(true);
            }
        }
        return cachedIdField;
    }

    private static Field cachedAssetMapField(Class<?> clazz) {
        if (cachedAssetMapField == null) {
            cachedAssetMapField = RecipesManager.findField(clazz, "assetMap");
            if (cachedAssetMapField != null) {
                cachedAssetMapField.setAccessible(true);
            }
        }
        return cachedAssetMapField;
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
