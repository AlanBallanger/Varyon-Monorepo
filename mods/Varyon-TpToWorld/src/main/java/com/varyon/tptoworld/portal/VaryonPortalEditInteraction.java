package com.varyon.tptoworld.portal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class VaryonPortalEditInteraction extends SimpleBlockInteraction {

    @Nonnull
    public static final BuilderCodec<VaryonPortalEditInteraction> CODEC;

    static {
        CODEC = BuilderCodec
                .builder(VaryonPortalEditInteraction.class, VaryonPortalEditInteraction::new,
                        SimpleBlockInteraction.CODEC)
                .build();
    }

    public VaryonPortalEditInteraction() {
        super("Varyon_Portal_Edit");
    }

    @Override
    protected void interactWithBlock(@Nonnull World world,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull ItemStack itemInHand,
            @Nonnull Vector3i targetBlock,
            @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !PortalEditModeManager.isInEditMode(playerRef.getUuid())) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = ref.getStore();
        player.getPageManager().openCustomPage(ref, store,
                new VaryonPortalEditPage(playerRef, world, targetBlock.x, targetBlock.y, targetBlock.z));
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull ItemStack itemInHand,
            @Nonnull World world,
            @Nonnull Vector3i targetBlock) {
        context.getState().state = InteractionState.Failed;
    }
}
