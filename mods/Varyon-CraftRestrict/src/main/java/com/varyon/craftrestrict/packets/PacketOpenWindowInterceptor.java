package com.varyon.craftrestrict.packets;

import com.varyon.craftrestrict.recipes.RecipesCache;
import com.varyon.craftrestrict.utils.PermissionsUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.protocol.packets.window.OpenWindow;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import java.util.logging.Level;

public class PacketOpenWindowInterceptor extends CraftRestrictPacketInterceptor {

    @Override
    public void register() {
        this.init();
        this.packetFilter = PacketAdapters.registerOutbound((PlayerPacketFilter) (playerRef, packet) -> {
            if (!(packet instanceof OpenWindow openWindow)) {
                return false;
            }
            if (openWindow.windowData == null) {
                return false;
            }
            if (openWindow.windowType == WindowType.BasicCrafting) {
                try {
                    if (!openWindow.windowData.contains("categories")) {
                        return false;
                    }
                    JsonObject data = JsonParser.parseString(openWindow.windowData).getAsJsonObject();
                    JsonArray categories = data.getAsJsonArray("categories");
                    for (JsonElement catElem : categories) {
                        JsonObject category = catElem.getAsJsonObject();
                        JsonArray craftableRecipes = category.getAsJsonArray("craftableRecipes");
                        JsonArray filteredRecipes = new JsonArray();
                        for (JsonElement recipeElem : craftableRecipes) {
                            String recipeId = recipeElem.getAsString();
                            if (recipeId.endsWith("_Restricted")) {
                                continue;
                            }
                            String restrictedId = RecipesCache.getRestrictedId(recipeId);
                            CraftingRecipe recipeAsset = CraftingRecipe.getAssetMap().getAsset(recipeId);
                            if (recipeAsset == null || recipeAsset.getPrimaryOutput() == null) {
                                filteredRecipes.add(recipeId);
                                continue;
                            }
                            String outputId = recipeAsset.getPrimaryOutput().getItemId();
                            if (outputId == null) {
                                filteredRecipes.add(recipeId);
                                continue;
                            }
                            boolean shouldRestrict = PermissionsUtils.shouldRestrict(playerRef, outputId, "recipe");
                            if (shouldRestrict && restrictedId != null) {
                                if (!config.isLockedItemsAppearsInCraftingList()) {
                                    continue;
                                }
                                filteredRecipes.add(restrictedId);
                                continue;
                            }
                            filteredRecipes.add(recipeId);
                        }
                        category.add("craftableRecipes", filteredRecipes);
                    }
                    openWindow.windowData = data.toString();
                } catch (Exception e) {
                    this.logger.at(Level.SEVERE).log("Error intercepting OpenWindow BasicCrafting packet: " + e.getMessage());
                }
                return false;
            }
            return false;
        });
    }
}
