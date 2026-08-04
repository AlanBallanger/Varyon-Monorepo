package com.varyon.easyhunger.interactions;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.components.ThirstComponent;
import com.varyon.easyhunger.ui.EasyWaterHud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DrinkWaterInteraction extends SimpleInstantInteraction {
    
    private float thirstRestoreAmount = 0.0f;
    
    public static final BuilderCodec<DrinkWaterInteraction> CODEC = 
        ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(
            DrinkWaterInteraction.class, 
            DrinkWaterInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("ThirstRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.thirstRestoreAmount = value, 
                interaction -> interaction.thirstRestoreAmount)
            .add())
        ).build();
    
    public DrinkWaterInteraction() {
        super();
    }
    
    @Override
    protected void firstRun(@Nonnull InteractionType type, 
                           @Nonnull InteractionContext context, 
                           @Nonnull CooldownHandler cooldownHandler) {
        try {
            Ref<EntityStore> entityRef = context.getEntity();
            if (entityRef == null || !entityRef.isValid()) {
                context.getState().state = InteractionState.Failed;
                return;
            }
            
            Store<EntityStore> store = entityRef.getStore();
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            
            if (playerRef == null) {
                // Try fallback search? No, Component Access is safer.
                context.getState().state = InteractionState.Failed;
                return;
            }
            
            ThirstComponent thirst = store.getComponent(entityRef, ThirstComponent.getComponentType());
            
            if (thirst != null) {
                float max = EasyHunger.get().getConfig().getMaxThirst();
                if (thirst.getThirstLevel() < max) {
                    // Default: use CODEC value from JSON, fallback to 15.0
                    float restoreAmount = (this.thirstRestoreAmount > 0) ? this.thirstRestoreAmount : 15.0f;
                    
                    // Get item ID using getOriginalItemType() - always available even for last item in stack
                    com.hypixel.hytale.server.core.asset.type.item.config.Item item = context.getOriginalItemType();
                    String itemId = (item != null) ? item.getId() : null;
                    
                    // Fallback to getHeldItem if original is null
                    if (itemId == null && context.getHeldItem() != null) {
                        itemId = context.getHeldItem().getItemId();
                    }
                    
                    // Clean up item ID and look up in config
                    if (itemId != null) {
                        // Remove leading asterisk if present (Hytale adds this for state variants)
                        if (itemId.startsWith("*")) {
                            itemId = itemId.substring(1);
                        }
                        
                        // Try full ID first (e.g. Deco_Tankard_State_Filled_Water)
                        Float configValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
                        
                        // If not found, try without state suffix (e.g. Deco_Tankard)
                        if (configValue <= 0 && itemId.contains(":")) {
                            String baseId = itemId.substring(0, itemId.indexOf(":"));
                            configValue = EasyHunger.get().getDrinksConfig().getDrinkValue(baseId);
                        }
                        
                        if (configValue > 0) {
                            restoreAmount = configValue;
                        }
                    }
                    
                    
                    thirst.drink(restoreAmount);
                    
                    // Update HUD and clear preview
                    EasyWaterHud.updatePlayerThirstLevel(playerRef, thirst.getThirstLevel());
                    EasyWaterHud.updatePlayerThirstPreview(playerRef, 0.0f);
                } else {
                     // Debug: EasyHunger.logInfo("[DrinkInteraction] Thirst Full.");
                }
            }
            
            context.getState().state = InteractionState.Finished;
            
        } catch (Exception e) {
            EasyHunger.logInfo("DRINK WATER ERROR: " + e.toString());
            context.getState().state = InteractionState.Failed;
        }
    }
}
