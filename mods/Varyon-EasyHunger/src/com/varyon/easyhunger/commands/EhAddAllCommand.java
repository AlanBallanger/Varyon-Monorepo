package com.varyon.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.protocol.InteractionType;
import com.varyon.easyhunger.EasyHunger;

import javax.annotation.Nonnull;
import java.util.Map;

public class EhAddAllCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.command.addall";

    private final OptionalArg<Float> valueArg = this.withOptionalArg("value", "Default value (default: 5.0)", ArgTypes.FLOAT);

    public EhAddAllCommand() {
        super("addall", "Auto-detect and add all food/drink items", false);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Float valueOpt = this.valueArg.get(context);
        float defaultValue = (valueOpt != null) ? valueOpt : 5.0f;

        context.sendMessage(Message.raw("Scanning all server items for food/drink interactions..."));

        int foodCount = 0;
        int drinkCount = 0;
        int skippedCount = 0;
        int totalScanned = 0;

        try {
            Map<?, ?> allItems = Item.getAssetMap().getAssetMap();
            totalScanned = allItems.size();

            for (Map.Entry<?, ?> entry : allItems.entrySet()) {
                String itemId = (String) entry.getKey();
                Item item = (Item) entry.getValue();
                if (item == null || itemId == null) continue;

                // Clean up item ID - remove leading asterisk (Hytale state variant prefix)
                if (itemId.startsWith("*")) {
                    itemId = itemId.substring(1);
                }

                // Skip template items
                if (itemId.contains("Template")) continue;

                // Check interactions for food or drink
                boolean isFood = false;
                boolean isDrink = false;

                Map<InteractionType, String> interactions = item.getInteractions();
                if (interactions != null) {
                    for (String interactionId : interactions.values()) {
                        if (interactionId == null) continue;
                        String lower = interactionId.toLowerCase();
                        if (lower.contains("consume_food")) {
                            isFood = true;
                        }
                        if (lower.contains("consume_drink") || lower.contains("startdrinking")) {
                            isDrink = true;
                        }
                    }
                }

                // Skip items without food/drink interaction
                if (!isFood && !isDrink) continue;

                // Calculate custom value based on Item Quality (Rarity)
                float calculatedValue = defaultValue;
                java.util.concurrent.ThreadLocalRandom rand = java.util.concurrent.ThreadLocalRandom.current();
                
                try {
                    com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality quality = 
                        com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
                    
                    if (quality != null) {
                        String qId = quality.getId();
                        if (qId != null) {
                            String lowerQ = qId.toLowerCase();
                            
                            if (lowerQ.contains("uncommon")) {
                                calculatedValue = 10.0f + rand.nextFloat() * (15.0f - 10.0f);
                            } else if (lowerQ.contains("rare")) {
                                calculatedValue = 15.0f + rand.nextFloat() * (25.0f - 15.0f);
                            } else if (lowerQ.contains("epic")) {
                                calculatedValue = 25.0f + rand.nextFloat() * (35.0f - 25.0f);
                            } else if (lowerQ.contains("legendary") || lowerQ.contains("mythic")) {
                                calculatedValue = 45.0f;
                            } else {
                                // Explicitly Common/Default (fallback)
                                calculatedValue = 5.0f + rand.nextFloat() * (8.0f - 5.0f);
                            }
                        }
                    } else {
                        // Without any Rarity
                        calculatedValue = 5.0f + rand.nextFloat() * (8.0f - 5.0f);
                    }
                } catch (Exception e) {
                    // Fallback to default if quality fetch fails
                    calculatedValue = 5.0f + rand.nextFloat() * (8.0f - 5.0f);
                }
                
                // Arredonda para 1 casa decimal
                calculatedValue = Math.round(calculatedValue * 10.0f) / 10.0f;

                // Add to config, skip if already exists in EITHER config
                boolean alreadyInFood = EasyHunger.get().getFoodsConfig().getFoodValue(itemId) > 0;
                boolean alreadyInDrink = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId) > 0;

                if (alreadyInFood || alreadyInDrink) {
                    skippedCount++;
                    continue;
                }

                if (isDrink) {
                    EasyHunger.get().getDrinksConfig().setDrinkValue(itemId, calculatedValue);
                    drinkCount++;
                    EasyHunger.logInfo("[AddAll] Added drink: " + itemId + " (value: " + calculatedValue + ")");
                } else {
                    EasyHunger.get().getFoodsConfig().setFoodValue(itemId, calculatedValue);
                    foodCount++;
                    EasyHunger.logInfo("[AddAll] Added food: " + itemId + " (value: " + calculatedValue + ")");
                }
            }

            // Save configs
            if (foodCount > 0) EasyHunger.get().saveFoodsConfig();
            if (drinkCount > 0) EasyHunger.get().saveDrinksConfig();

            // Report
            StringBuilder msg = new StringBuilder();
            msg.append("Scan complete (").append(totalScanned).append(" items scanned):\n");
            if (foodCount > 0) msg.append("  + ").append(foodCount).append(" food items added (value: ").append(defaultValue).append(")\n");
            if (drinkCount > 0) msg.append("  + ").append(drinkCount).append(" drink items added (value: ").append(defaultValue).append(")\n");
            if (skippedCount > 0) msg.append("  ~ ").append(skippedCount).append(" items skipped (already configured)\n");
            if (foodCount == 0 && drinkCount == 0 && skippedCount == 0) {
                msg.append("  No food/drink items found.");
            }
            context.sendMessage(Message.raw(msg.toString()));

        } catch (Exception e) {
            context.sendMessage(Message.raw("Error: " + e.getMessage()));
            EasyHunger.logInfo("EhAddAllCommand error: " + e.toString());
        }
    }
}
