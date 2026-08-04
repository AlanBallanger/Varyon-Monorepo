package com.varyon.easyhunger.interactions;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.ui.EasyHungerHud;

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
 * Interaction that runs when player STARTS eating (before the eating animation completes).
 * Shows a preview of how much hunger will be restored on the HUD.
 */
public class StartFeedingInteraction extends SimpleInstantInteraction {
    
    private float hungerRestoreAmount = 10.0f;
    
    public static final BuilderCodec<StartFeedingInteraction> CODEC = 
        ((BuilderCodec.Builder<StartFeedingInteraction>) BuilderCodec.builder(
            StartFeedingInteraction.class, 
            StartFeedingInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("HungerRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.hungerRestoreAmount = value, 
                interaction -> interaction.hungerRestoreAmount)
            .add()
        ).build();
    
    public StartFeedingInteraction() {
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
            Item item = context.getOriginalItemType();
            
            if (playerRef == null || item == null) return;
            
            // Get hunger restoration value from config or JSON
            String itemId = item.getId();
            float restoreAmount = EasyHunger.get().getFoodsConfig().getFoodValue(itemId);
            if (restoreAmount <= 0) {
                restoreAmount = this.hungerRestoreAmount;
            }
            
            // Show preview on HUD
            EasyHungerHud.updatePlayerHungerPreview(playerRef, restoreAmount);
            
        } catch (Exception e) {
            EasyHunger.logInfo("START FEEDING ERROR: " + e.toString());
        }
    }
}
