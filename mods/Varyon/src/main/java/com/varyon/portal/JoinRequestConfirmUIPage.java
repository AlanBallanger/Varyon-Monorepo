package com.varyon.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.command.JoinAcceptService;
import com.varyon.rtpv.RtpvJoinManager;

import javax.annotation.Nonnull;

import java.awt.Color;
import java.util.UUID;

public class JoinRequestConfirmUIPage extends InteractiveCustomUIPage<JoinRequestConfirmUIPage.EventDataClass> {

    private final UUID joinerUuid;
    private final String joinerNameForSubtitle;

    public JoinRequestConfirmUIPage(
        @Nonnull PlayerRef targetRef,
        @Nonnull UUID joinerUuid,
        @Nonnull String joinerNameForSubtitle
    ) {
        super(targetRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.joinerUuid = joinerUuid;
        this.joinerNameForSubtitle = joinerNameForSubtitle;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("JoinRequestConfirmMenu.ui");
        commandBuilder.set("#TitleLabel.Text", "Rejoindre un joueur");
        commandBuilder.set("#Subtitle.Text",
            joinerNameForSubtitle + " demande à te rejoindre");
        commandBuilder.set("#YesButtonLabel.Text", "Accepter");
        commandBuilder.set("#NoButtonLabel.Text", "Refuser");

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#YesButton", EventData.of("Action", "yes"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#NoButton", EventData.of("Action", "no"));
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull EventDataClass data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef accepterRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || accepterRef == null) {
            return;
        }

        if ("no".equals(data.action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
            if (joinMgr != null) {
                joinMgr.removePendingJoin(accepterRef.getUuid(), joinerUuid);
            }
            PlayerRef joinerRef = Universe.get().getPlayer(joinerUuid);
            if (joinerRef != null && joinerRef.isValid()) {
                String hostName = accepterRef.getUsername() != null ? accepterRef.getUsername() : "";
                joinerRef.sendMessage(Message.raw(hostName + " a refusé ta demande de join.").color(Color.RED));
            }
            return;
        }

        if (!"yes".equals(data.action)) {
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);

        RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
        if (joinMgr == null) {
            accepterRef.sendMessage(Message.raw("Système de join indisponible.").color(Color.RED));
            return;
        }

        if (joinMgr.findPendingJoinByJoiner(accepterRef.getUuid(), joinerUuid) == null) {
            accepterRef.sendMessage(Message.raw("Cette demande n'est plus valide.").color(Color.RED));
            return;
        }

        PlayerRef joinerRef = Universe.get().getPlayer(joinerUuid);
        if (joinerRef == null || !joinerRef.isValid()) {
            joinMgr.removePendingJoin(accepterRef.getUuid(), joinerUuid);
            accepterRef.sendMessage(Message.raw("Le joueur n'est plus en ligne.").color(Color.RED));
            return;
        }

        if (joinerRef.getUuid().equals(accepterRef.getUuid())) {
            return;
        }

        JoinAcceptService.accept(accepterRef, joinerRef, accepterRef::sendMessage, joinerRef::sendMessage);
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
