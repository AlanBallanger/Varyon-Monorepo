package com.varyon.tptoworld.portal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class VaryonPortalSpawnInteraction extends SimpleBlockInteraction {

    @Nonnull
    public static final BuilderCodec<VaryonPortalSpawnInteraction> CODEC;

    static {
        CODEC = BuilderCodec
                .builder(VaryonPortalSpawnInteraction.class, VaryonPortalSpawnInteraction::new,
                        SimpleBlockInteraction.CODEC)
                .build();
    }

    public VaryonPortalSpawnInteraction() {
        super("Varyon_Portal_Spawn");
    }

    @Override
    @Nonnull
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
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
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        if (playerComponent == null || uuidComponent == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        VaryonPortalCommandBlock customCommand = VaryonPortalCommandBlock.getAt(world, targetBlock);
        if (customCommand != null && !customCommand.getCommand().isBlank()) {
            PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                String resolved = resolvePlaceholders(customCommand.getCommand(), playerRef);
                CommandManager.get().handleCommand(ConsoleSender.INSTANCE, resolved);
                return;
            }
        }

        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        if (spawnProvider == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Transform spawnTransform = spawnProvider.getSpawnPoint(world, uuidComponent.getUuid());
        Teleport teleport = Teleport.createForPlayer(world, spawnTransform.getPosition(), Rotation3f.ZERO);
        commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull ItemStack itemInHand,
            @Nonnull World world,
            @Nonnull Vector3i targetBlock) {
        context.getState().state = InteractionState.Failed;
    }

    /**
     * Hytale's command parser has no @p/@s selector support (ArgTypes.PLAYER_REF expects a
     * literal username) — replace the common Minecraft-style selectors and {player} with the
     * traversing player's actual username before dispatching the stored command.
     */
    private static String resolvePlaceholders(@Nonnull String command, @Nonnull PlayerRef playerRef) {
        String username = playerRef.getUsername();
        if (username == null) {
            return command;
        }
        return command
                .replace("@p", username)
                .replace("@s", username)
                .replace("{player}", username)
                .replace("{Player}", username)
                .replace("{Username}", username);
    }
}
