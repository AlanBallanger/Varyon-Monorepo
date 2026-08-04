package com.varyon.craftrestrict.recipes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RecipesCache {

    private static final Map<String, String> recipeMapping = new ConcurrentHashMap<>();

    public static boolean hasRestrictedVersion(String originalId) {
        return recipeMapping.containsKey(originalId);
    }

    public static String getRestrictedId(String originalId) {
        return recipeMapping.get(originalId);
    }

    public static void register(String originalId, String restrictedId) {
        recipeMapping.put(originalId, restrictedId);
    }

    public static int getSize() {
        return recipeMapping.size();
    }
}
