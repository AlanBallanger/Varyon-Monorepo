package com.faiizer.craftrestrict.packets;

import com.faiizer.craftrestrict.utils.PermissionsUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.protocol.packets.window.UpdateWindow;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import java.util.logging.Level;

public class PacketUpdateWindowInterceptor extends CraftRestrictPacketInterceptor {

    @Override
    public void register() {
        this.init();
        this.packetFilter = PacketAdapters.registerOutbound((PlayerPacketFilter) (playerRef, packet) -> {
            if (!(packet instanceof UpdateWindow uw)) {
                return false;
            }
            if (uw.windowData != null && uw.windowData.contains("\"id\":\"Builders\"")) {
                try {
                    JsonObject json = JsonParser.parseString(uw.windowData).getAsJsonObject();
                    if (json.has("optionSlotRecipes")) {
                        JsonArray originalRecipes = json.getAsJsonArray("optionSlotRecipes");
                        JsonArray filteredRecipes = new JsonArray();
                        for (JsonElement element : originalRecipes) {
                            String recipeId = element.getAsString();
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
                            if (config.isDebug()) {
                                this.logger.at(Level.INFO).log("========================================");
                                this.logger.at(Level.INFO).log("Item ID: " + outputId);
                                this.logger.at(Level.INFO).log("Can craft: " + shouldRestrict);
                                this.logger.at(Level.INFO).log("========================================");
                            }
                            if (shouldRestrict) {
                                continue;
                            }
                            filteredRecipes.add(recipeId);
                        }
                        json.add("optionSlotRecipes", filteredRecipes);
                        uw.windowData = json.toString();
                        if (config.isDebug()) {
                            this.logger.at(Level.INFO).log("========================================");
                            this.logger.at(Level.INFO).log("[CraftRestrict] UpdateWindow Intercepted");
                            this.logger.at(Level.INFO).log("- Player: " + playerRef.getUsername());
                            this.logger.at(Level.INFO).log("- Window ID: " + uw.id);
                            this.logger.at(Level.INFO).log("- Data Filtered: " + uw.windowData);
                            this.logger.at(Level.INFO).log("========================================");
                        }
                    }
                } catch (Exception e) {
                    this.logger.at(Level.SEVERE).log("Failed to filter Builder's Bench recipes: " + e.getMessage());
                }
            }
            return false;
        });
    }
}
