package com.varyon.bossarena.loot;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OpenBossChestInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<OpenBossChestInteraction> CODEC = BuilderCodec.builder(
            OpenBossChestInteraction.class,
            OpenBossChestInteraction::new,
            SimpleBlockInteraction.CODEC
    ).build();

    public OpenBossChestInteraction() {
        super("BossArena_OpenChest");
    }

    @Override
    protected void interactWithBlock(@Nonnull World world,
                                     @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type,
                                     @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand,
                                     @Nonnull org.joml.Vector3i pos,
                                     @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        BossLootChestOpener.open(world, ref, store, commandBuffer, pos.x, pos.y, pos.z);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type,
                                             @Nonnull InteractionContext context,
                                             @Nullable ItemStack itemInHand,
                                             @Nonnull World world,
                                             @Nonnull org.joml.Vector3i targetBlock) {
        // Nothing to simulate
    }
}
