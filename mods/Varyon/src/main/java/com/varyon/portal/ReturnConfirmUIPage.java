package com.varyon.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.command.ReturnCommand;

import javax.annotation.Nonnull;

public class ReturnConfirmUIPage extends InteractiveCustomUIPage<ReturnConfirmUIPage.EventDataClass> {

    private final int totalCost;
    private final int costMultiplier;
    private final boolean economyEnabled;

    public ReturnConfirmUIPage(
        @Nonnull PlayerRef playerRef,
        int totalCost,
        int costMultiplier,
        boolean economyEnabled
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.totalCost = totalCost;
        this.costMultiplier = costMultiplier;
        this.economyEnabled = economyEnabled;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("ReturnConfirmMenu.ui");

        String yesText;
        if (economyEnabled) {
            if (costMultiplier > 1) {
                yesText = "Oui, pour " + totalCost + " coins (x" + costMultiplier + ")";
            } else {
                yesText = "Oui, pour " + totalCost + " coins";
            }
        } else {
            yesText = "Oui";
        }

        commandBuilder.set("#YesButtonLabel.Text", yesText);

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#YesButton", EventData.of("Action", "yes"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#NoButton", EventData.of("Action", "no"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull EventDataClass data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRefComp == null) {
            return;
        }

        if ("no".equals(data.action) || "close".equals(data.action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (!"yes".equals(data.action)) {
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);

        VaryonPlugin plugin = VaryonPlugin.getInstance();
        if (plugin == null) {
            return;
        }
        ReturnCommand returnCommand = plugin.getReturnCommand();
        if (returnCommand == null) {
            return;
        }

        World world = ((EntityStore) store.getExternalData()).getWorld();
        returnCommand.confirmReturnFromUi(ref, store, playerRefComp, world);
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
