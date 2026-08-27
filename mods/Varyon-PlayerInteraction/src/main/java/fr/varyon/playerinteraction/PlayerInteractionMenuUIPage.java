package fr.varyon.playerinteraction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.OpenChatWithCommand;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

import java.util.UUID;

public class PlayerInteractionMenuUIPage extends InteractiveCustomUIPage<PlayerInteractionMenuUIPage.EventDataClass> {

    private final UUID targetUuid;
    private final String targetName;

    public PlayerInteractionMenuUIPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull UUID targetUuid,
        @Nonnull String targetName
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("PlayerInteractionMenu.ui");
        commandBuilder.set("#TargetNameLabel.Text", targetName);

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#MurmurerButton", EventData.of("Action", "murmurer"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#EchangerButton", EventData.of("Action", "echanger"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#InviterButton", EventData.of("Action", "inviter"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#ProfilButton", EventData.of("Action", "profil"));
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull EventDataClass data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null || data.action == null) {
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);

        switch (data.action) {
            case "murmurer" -> playerRef.getPacketHandler()
                .write(new OpenChatWithCommand("/msg " + targetName + " "));
            case "echanger" -> CommandManager.get().handleCommand(playerRef, "trade " + targetName);
            case "inviter" -> CommandManager.get().handleCommand(playerRef, "party invite " + targetName);
            case "profil" -> playerRef.sendMessage(Message.raw("Cette fonctionnalité sera disponible bientot"));
            default -> { }
        }
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (entry, s) -> entry.action = s, entry -> entry.action)
                .build();
        public String action;
    }
}
