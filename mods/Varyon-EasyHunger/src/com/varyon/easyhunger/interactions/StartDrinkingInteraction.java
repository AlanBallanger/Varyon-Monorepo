package com.varyon.easyhunger.interactions;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.ui.EasyWaterHud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Interaction that runs when player STARTS drinking (before the drinking animation completes).
 * Shows a preview of how much thirst will be restored on the HUD.
 */
public class StartDrinkingInteraction extends SimpleInstantInteraction {
    
    private float thirstRestoreAmount = 10.0f;
    
    public static final BuilderCodec<StartDrinkingInteraction> CODEC = 
        ((BuilderCodec.Builder<StartDrinkingInteraction>) BuilderCodec.builder(
            StartDrinkingInteraction.class, 
            StartDrinkingInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("ThirstRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.thirstRestoreAmount = value, 
                interaction -> interaction.thirstRestoreAmount)
            .add()
        ).build();
    
    public StartDrinkingInteraction() {
        super();
    }
    
    @Override
    protected void firstRun(@Nonnull InteractionType type, 
                           @Nonnull InteractionContext context, 
                           @Nonnull CooldownHandler cooldownHandler) {
        try {
            Ref<EntityStore> entityRef = context.getEntity();
            if (entityRef == null || !entityRef.isValid()) return;
            
            Store<EntityStore> store = entityRef.getStore();
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            
            if (playerRef == null) return;
            
            // Get thirst restoration value from config or JSON
            float restoreAmount = this.thirstRestoreAmount;
            
            // Try to get item ID from held item
            if (context.getHeldItem() != null) {
                String itemId = context.getHeldItem().getItemId();
                // Remove leading asterisk if present (Hytale adds this for state variants)
                if (itemId != null && itemId.startsWith("*")) {
                    itemId = itemId.substring(1);
                }
                // Remove state suffix (e.g., ":Filled_Water")
                if (itemId != null && itemId.contains(":")) {
                    itemId = itemId.substring(0, itemId.indexOf(":"));
                }
                Float configValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
                if (configValue > 0) {
                    restoreAmount = configValue;
                }
            } else if (context.getOriginalItemType() != null) {
                // Fallback to original item type
                String itemId = context.getOriginalItemType().getId();
                Float configValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
                if (configValue > 0) {
                    restoreAmount = configValue;
                }
            }
            
            // Show preview on HUD
            EasyWaterHud.updatePlayerThirstPreview(playerRef, restoreAmount);
            
        } catch (Exception e) {
            EasyHunger.logInfo("START DRINKING ERROR: " + e.toString());
        }
    }
}
