package com.varyon.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.teleport.FirstSpawnStyleParticleFx;

import org.joml.Vector3d;

import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArenasPortalUIPage extends InteractiveCustomUIPage<ArenasPortalUIPage.EventDataClass> {

    private static final int WINDOW_WIDTH = 852;
    private static final int WINDOW_HEIGHT = 595;

    private static final String TELEPORT_LABEL = "Se téléporter";

    private static final double[][] ARENA_COORDS = {
        {106, 122, -3},
        {5100, 125, 0},
        {7600, 125, 0},
        {10100, 125, 0},
        {12600, 125, 0},
        {15100, 125, 0},
        {17600, 125, 0},
        {20100, 125, 0},
        {22600, 125, 0},
        {25100, 125, 0},
    };

    private final PlayerRef ownerRef;

    public ArenasPortalUIPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.ownerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("ArenasPortalMenu.ui");
        Anchor windowAnchor = new Anchor();
        windowAnchor.setWidth(Value.of(WINDOW_WIDTH));
        windowAnchor.setHeight(Value.of(WINDOW_HEIGHT));
        commandBuilder.setObject("#ArenasPortalRoot.Anchor", windowAnchor);
        commandBuilder.set("#MenuTitle.Text", "Téléportation arènes Varyon");

        ZonePermissionsConfig zonePermissionsConfig = null;
        try {
            if (VaryonPlugin.getStaticConfigManager() != null) {
                zonePermissionsConfig = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
            }
        } catch (Exception ignored) {}

        int maxAccessibleZone = zonePermissionsConfig != null
            ? zonePermissionsConfig.getMaxAccessibleZone(ownerRef)
            : ARENA_COORDS.length;

        for (int i = 1; i <= ARENA_COORDS.length; i++) {
            boolean accessible = i <= maxAccessibleZone;

            if (!accessible) {
                commandBuilder.set("#ZoneImg" + i + ".Background", "varyon_arenas_no.png");
                commandBuilder.set("#ZoneMain" + i + ".Disabled", true);
                commandBuilder.set("#ZoneRadius" + i + ".Visible", true);
                commandBuilder.set("#ZoneMainLead" + i + ".Text", "");
                continue;
            }

            commandBuilder.set("#ZoneRadius" + i + ".Visible", true);
            commandBuilder.set("#ZoneMainLead" + i + ".Text", TELEPORT_LABEL);

            int zoneId = i;
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZoneMain" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventDataClass data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if ("close".equals(data.action)) {
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            return;
        }

        if ("zone".equals(data.action) && data.zoneId != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null || playerRefComp == null) {
                return;
            }

            int requestedZone;
            try {
                requestedZone = Integer.parseInt(data.zoneId);
            } catch (NumberFormatException e) {
                return;
            }
            if (requestedZone < 1 || requestedZone > ARENA_COORDS.length) {
                return;
            }

            ZonePermissionsConfig zonePerms = VaryonPlugin.getStaticConfigManager() != null
                ? VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig() : null;
            if (zonePerms != null && !zonePerms.canAccessZone(playerRefComp, requestedZone)) {
                return;
            }

            World world;
            try {
                world = ((EntityStore) store.getExternalData()).getWorld();
            } catch (Exception e) {
                return;
            }

            player.getPageManager().setPage(ref, store, Page.None);

            double[] coords = ARENA_COORDS[requestedZone - 1];
            Vector3d position = new Vector3d(coords[0], coords[1], coords[2]);
            Teleport teleport = Teleport.createForPlayer(world, position, Rotation3f.ZERO);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
            FirstSpawnStyleParticleFx.playAt(world, position, ref, store, 0);
        }
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING),
                    (entry, s) -> entry.action = s, entry -> entry.action)
                .addField(new KeyedCodec<>("ZoneId", Codec.STRING),
                    (entry, s) -> entry.zoneId = s, entry -> entry.zoneId)
                .build();

        public String action;
        public String zoneId;
    }
}
