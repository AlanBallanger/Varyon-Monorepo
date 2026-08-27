package fr.varyon.playerinteraction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

import java.util.logging.Level;

public class PlayerInteractionMenuInteraction extends SimpleInstantInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Varyon_PlayerInteractionMenu";
    public static final BuilderCodec<PlayerInteractionMenuInteraction> CODEC =
        BuilderCodec.builder(PlayerInteractionMenuInteraction.class, PlayerInteractionMenuInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Opens the Varyon player interaction menu (whisper/trade/party/profile) on the targeted player.")
            .build();
    public static final RootInteraction DEFAULT_ROOT = new RootInteraction(ID, ID);

    public PlayerInteractionMenuInteraction(String id) {
        super(id);
    }

    protected PlayerInteractionMenuInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        PlayerRef targetPlayerRef = commandBuffer.getComponent(targetRef, PlayerRef.getComponentType());
        if (targetPlayerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        if (targetPlayerRef.getUuid().equals(playerRef.getUuid())) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        LOGGER.at(Level.INFO).log("[PlayerInteraction] Opening menu for %s targeting %s", playerRef.getUsername(), targetPlayerRef.getUsername());

        player.getPageManager().openCustomPage(
            ref, ref.getStore(),
            new PlayerInteractionMenuUIPage(playerRef, targetPlayerRef.getUuid(), targetPlayerRef.getUsername())
        );
    }

    @Override
    @Nonnull
    public String toString() {
        return "PlayerInteractionMenuInteraction{} " + super.toString();
    }
}
