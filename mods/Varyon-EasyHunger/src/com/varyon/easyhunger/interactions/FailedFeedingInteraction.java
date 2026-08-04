package com.varyon.easyhunger.interactions;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.ui.EasyHungerHud;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Interaction that runs when player STOPS eating without finishing (cancelled).
 * Clears the hunger preview from the HUD.
 */
public class FailedFeedingInteraction extends SimpleInstantInteraction {
    
    public static final BuilderCodec<FailedFeedingInteraction> CODEC = 
        BuilderCodec.builder(
            FailedFeedingInteraction.class, 
            FailedFeedingInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();
    
    public FailedFeedingInteraction() {
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
            
            // Clear preview on HUD
            EasyHungerHud.updatePlayerHungerPreview(playerRef, 0.0f);
            
        } catch (Exception e) {
            EasyHunger.logInfo("FAILED FEEDING ERROR: " + e.toString());
        }
    }
}
