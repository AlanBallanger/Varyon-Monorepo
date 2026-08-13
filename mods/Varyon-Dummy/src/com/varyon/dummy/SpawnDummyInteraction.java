package com.varyon.dummy;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;

import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpawnDummyInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<SpawnDummyInteraction> CODEC = BuilderCodec.builder(
            SpawnDummyInteraction.class,
            SpawnDummyInteraction::new,
            SimpleBlockInteraction.CODEC
    ).build();

    private void spawnNPC(CommandBuffer<EntityStore> commandBuffer, InteractionContext interactionContext) {
        Ref<EntityStore> ownerRef = interactionContext.getOwningEntity();
        if (!ownerRef.isValid()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        HeadRotation head = commandBuffer.getComponent(ownerRef, HeadRotation.getComponentType());
        if (head == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Rotation3f rotation = new Rotation3f();
        rotation.setYaw((float) (head.getRotation().yaw() + Math.PI));

        int roleIndex = NPCPlugin.get().getIndex("Drex_Dummy");
        if (roleIndex < 0) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Vector3d targetLocation = TargetUtil.getTargetLocation(ownerRef, 8.0, commandBuffer);
        if (targetLocation == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Vector3i targetBlock = new Vector3i((int) Math.floor(targetLocation.x), (int) Math.floor(targetLocation.y), (int) Math.floor(targetLocation.z));
        PlaceBlockEvent event = new PlaceBlockEvent(new ItemStack("Tinkering_Target_Dummy"), targetBlock, RotationTuple.NONE);
        commandBuffer.invoke(ownerRef, (EcsEvent) event);
        if (event.isCancelled()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Vector3d spawnPosition = new Vector3d(targetBlock.x() + 0.5, targetBlock.y(), targetBlock.z() + 0.5);
        commandBuffer.run(store -> NPCPlugin.get().spawnEntity(
                store,
                roleIndex,
                spawnPosition,
                rotation,
                null,
                (npcEntity, holder, store2) -> holder.ensureComponent(DummyComponent.getComponentType()),
                null
        ));
    }

    @Override
    protected void interactWithBlock(@Nonnull World world,
                                      @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                      @Nonnull InteractionType type,
                                      @Nonnull InteractionContext context,
                                      @Nullable ItemStack itemInHand,
                                      @Nonnull Vector3i targetBlock,
                                      @Nonnull CooldownHandler cooldownHandler) {
        spawnNPC(commandBuffer, context);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type,
                                              @Nonnull InteractionContext context,
                                              @Nullable ItemStack itemInHand,
                                              @Nonnull World world,
                                              @Nonnull Vector3i targetBlock) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;
        spawnNPC(commandBuffer, context);
    }
}
