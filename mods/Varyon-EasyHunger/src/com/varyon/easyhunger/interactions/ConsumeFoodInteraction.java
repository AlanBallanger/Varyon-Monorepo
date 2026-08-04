package com.varyon.easyhunger.interactions;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.components.HungerComponent;
import com.varyon.easyhunger.ui.EasyHungerHud;

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

public class ConsumeFoodInteraction extends SimpleInstantInteraction {
    
    private float hungerRestoreAmount = 10.0f;
    
    public static final BuilderCodec<ConsumeFoodInteraction> CODEC = 
        ((BuilderCodec.Builder<ConsumeFoodInteraction>) BuilderCodec.builder(
            ConsumeFoodInteraction.class, 
            ConsumeFoodInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("HungerRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.hungerRestoreAmount = value, 
                interaction -> interaction.hungerRestoreAmount)
            .add()
        ).build();
    
    public ConsumeFoodInteraction() {
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
                context.getState().state = InteractionState.Failed;
                return;
            }
            
            HungerComponent hunger = store.getComponent(entityRef, HungerComponent.getComponentType());
            
            if (hunger != null) {
                float max = EasyHunger.get().getConfig().getMaxHunger();
                if (hunger.getHungerLevel() < max) {
                    // Get item ID using getOriginalItemType() - always available even for last item in stack
                    com.hypixel.hytale.server.core.asset.type.item.config.Item item = context.getOriginalItemType();
                    String itemId = (item != null) ? item.getId() : "unknown";
                    
                    // Check config for this item's value - only restore if configured
                    float restoreAmount = EasyHunger.get().getFoodsConfig().getFoodValue(itemId);
                    if (restoreAmount <= 0) {
                        // Item not in FoodsConfig - don't restore hunger (might be a drink like Food_Drink_*)
                        context.getState().state = InteractionState.Finished;
                        return;
                    }
                    
                    hunger.feed(restoreAmount);
                    
                    // Update HUD and clear preview
                    EasyHungerHud.updatePlayerHungerLevel(playerRef, hunger.getHungerLevel());
                    EasyHungerHud.updatePlayerHungerPreview(playerRef, 0.0f);
                }
            }
            
            context.getState().state = InteractionState.Finished;
            
        } catch (Exception e) {
            EasyHunger.logInfo("CONSUME FOOD ERROR: " + e.toString());
            context.getState().state = InteractionState.Failed;
        }
    }
}
