package com.varyon.item;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;

import static java.util.Map.entry;

public final class AmbassadeOrbInteraction extends SimpleInstantInteraction {

    public static final String TYPE_NAME = "varyon_ambassade_orb";

    public static final BuilderCodec<AmbassadeOrbInteraction> CODEC =
        BuilderCodec.builder(
            AmbassadeOrbInteraction.class,
            AmbassadeOrbInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ITEM_FRAKTALE = "Varyon_Orbe_Ambassade_Fraktale";
    public static final String ITEM_NOVALE = "Varyon_Orbe_Ambassade_Novale";
    public static final String ITEM_VARYON = "Varyon_Orbe_Ambassade_Varyon";

    private static final String WARP_FRAKTALE = "ambassade_fraktale";
    private static final String WARP_NOVALE = "ambassade_novale";
    private static final String WARP_VARYON = "varyon";

    private static final Map<String, String> ITEM_TO_WARP = Map.ofEntries(
        entry(ITEM_FRAKTALE, WARP_FRAKTALE),
        entry(ITEM_NOVALE, WARP_NOVALE),
        entry(ITEM_VARYON, WARP_VARYON)
    );

    @Override
    protected void simulateFirstRun(
            @Nonnull InteractionType interactionType,
            @Nonnull InteractionContext interactionContext,
            @Nonnull CooldownHandler cooldownHandler) {
        interactionContext.getState().state = InteractionState.Finished;
    }

    @Override
    protected void firstRun(
            @Nonnull InteractionType interactionType,
            @Nonnull InteractionContext interactionContext,
            @Nonnull CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack held = interactionContext.getHeldItem();
        if (held == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        String itemId = held.getItemId();
        String warp = ITEM_TO_WARP.get(itemId);
        if (warp == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        try {
            if (AmbassadeWarpHelper.tryWarp(playerRef, warp)) {
                ItemContainer container = interactionContext.getHeldItemContainer();
                if (container != null) {
                    ItemStack after = held.withQuantity(held.getQuantity() - 1);
                    interactionContext.setHeldItem(after);
                    container.replaceItemStackInSlot(interactionContext.getHeldItemSlot(), held, after);
                }
                interactionContext.getState().state = InteractionState.Finished;
            } else {
                interactionContext.getState().state = InteractionState.Failed;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("[AmbassadeOrb] " + e.getMessage());
            playerRef.sendMessage(Message.raw("[Varyon] Téléportation ambassade impossible."));
            interactionContext.getState().state = InteractionState.Failed;
        }
    }
}
