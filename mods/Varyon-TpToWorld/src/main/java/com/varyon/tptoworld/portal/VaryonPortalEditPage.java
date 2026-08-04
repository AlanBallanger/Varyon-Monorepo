package com.varyon.tptoworld.portal;

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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;

public class VaryonPortalEditPage extends InteractiveCustomUIPage<VaryonPortalEditEventData> {

    private final World world;
    private final int x;
    private final int y;
    private final int z;

    public VaryonPortalEditPage(@Nonnull PlayerRef playerRef, @Nonnull World world, int x, int y, int z) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, VaryonPortalEditEventData.CODEC);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/VaryonPortalEditPage.ui");
        VaryonPortalCommandBlock existing = VaryonPortalCommandBlock.getAt(world, x, y, z);
        cmd.set("#CommandInput.Value", existing != null ? existing.getCommand() : "");
        bindEvents(evt);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull VaryonPortalEditEventData data) {
        String action = data.getAction();
        if (action == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if ("save".equals(action)) {
            String command = data.getCommand() != null ? data.getCommand().trim() : "";
            boolean attached = PortalCommandBlockAttacher.attach(world, x, y, z, command);
            if (playerRef != null) {
                if (attached) {
                    playerRef.sendMessage(Message.raw("Commande du portail enregistrée.").color(Color.GREEN));
                } else {
                    playerRef.sendMessage(Message.raw("Échec de l'enregistrement de la commande.").color(Color.RED));
                }
            }
            closePage(ref, store);
        } else if ("cancel".equals(action)) {
            closePage(ref, store);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    }

    private void bindEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton",
                new EventData().append("Action", "save").append("@Command", "#CommandInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton",
                EventData.of("Action", "cancel"));
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }
}
