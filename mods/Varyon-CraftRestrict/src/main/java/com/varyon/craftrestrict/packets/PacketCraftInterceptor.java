package com.varyon.craftrestrict.packets;

import com.varyon.craftrestrict.utils.PermissionsUtils;
import com.hypixel.hytale.protocol.packets.window.CraftRecipeAction;
import com.hypixel.hytale.protocol.packets.window.SendWindowAction;
import com.hypixel.hytale.protocol.packets.window.WindowAction;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import java.util.logging.Level;

public class PacketCraftInterceptor extends CraftRestrictPacketInterceptor {

    @Override
    public void register() {
        this.init();
        this.packetFilter = PacketAdapters.registerInbound((PlayerPacketFilter) (playerRef, packet) -> {
            if (!(packet instanceof SendWindowAction swa)) {
                return false;
            }
            WindowAction action = swa.action;
            if (!(action instanceof CraftRecipeAction recipeAction)) {
                return false;
            }
            String recipeId = recipeAction.recipeId;
            if (recipeId == null || recipeId.isEmpty()) {
                return false;
            }
            if (config.isDebug()) {
                this.logger.at(Level.INFO).log("[CraftRestrict] Inbound attempt: " + playerRef.getUsername() + " -> " + recipeId);
            }
            CraftingRecipe recipeAsset = CraftingRecipe.getAssetMap().getAsset(recipeId);
            if (recipeAsset != null && recipeAsset.getPrimaryOutput() != null) {
                String outputId = recipeAsset.getPrimaryOutput().getItemId();
                if (outputId == null || outputId.isEmpty()) {
                    return false;
                }
                String permNode = "craftrestrict.recipe." + outputId.toLowerCase();
                String mode = config.getRestrictionMode().toUpperCase();
                boolean shouldRestrict = PermissionsUtils.shouldRestrict(playerRef, outputId, "recipe");
                boolean canCraft = !shouldRestrict;
                if (config.isDebug()) {
                    this.logger.at(Level.INFO).log("========================================");
                    this.logger.at(Level.INFO).log("[CraftRestrict] Inbound Craft Attempt");
                    this.logger.at(Level.INFO).log("- Player: " + playerRef.getUsername());
                    this.logger.at(Level.INFO).log("- Item ID: " + outputId);
                    this.logger.at(Level.INFO).log("- Permission: " + permNode);
                    this.logger.at(Level.INFO).log("- Restriction Mode: " + mode);
                    this.logger.at(Level.INFO).log("- Can Craft: " + canCraft);
                    this.logger.at(Level.INFO).log("========================================");
                }
                if (shouldRestrict) {
                    if (config.isDebug()) {
                        this.logger.at(Level.WARNING).log("[CraftRestrict] BLOCKED: " + playerRef.getUsername() + " tried to bypass UI for " + recipeId);
                    }
                    PermissionsUtils.sendRestrictionNotifications(playerRef, "recipe");
                    return true;
                }
            }
            return false;
        });
    }
}
