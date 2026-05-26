package com.varyon.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;

import java.util.List;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VoidPortalUIPage extends InteractiveCustomUIPage<VoidPortalUIPage.EventDataClass> {

    private static final int WINDOW_WIDTH = 852;
    private static final int WINDOW_HEIGHT = 595;

    private static final String ECON_PRICE_PREFIX = "\u00A4 ";
    private static final String RANDOM_LABEL = "Al\u00e9atoire";

    private final PlayerRef ownerRef;

    public VoidPortalUIPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.ownerRef = playerRef;
    }

    @Nonnull
    private static String radiusSubtitleLine(@Nullable List<DifficultyZone> zones, int indexOneBased) {
        if (zones == null || zones.isEmpty() || indexOneBased < 1 || indexOneBased > zones.size()) {
            return "";
        }
        if (indexOneBased == 1) {
            if (zones.size() < 2) {
                return "";
            }
            return "Moins de " + zones.get(1).getRadiusStart() + " blocs";
        }
        DifficultyZone cur = zones.get(indexOneBased - 1);
        if (indexOneBased == zones.size()) {
            return cur.getRadiusStart() + " - infini blocs";
        }
        DifficultyZone next = zones.get(indexOneBased);
        return cur.getRadiusStart() + " - " + next.getRadiusStart() + " blocs";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("VoidPortalMenu.ui");
        Anchor windowAnchor = new Anchor();
        windowAnchor.setWidth(Value.of(WINDOW_WIDTH));
        windowAnchor.setHeight(Value.of(WINDOW_HEIGHT));
        commandBuilder.setObject("#VoidPortalRoot.Anchor", windowAnchor);
        commandBuilder.set("#MenuTitle.Text", "T\u00e9l\u00e9portation zones Varyon");

        RtpvConfig rtpvConfig = null;
        List<DifficultyZone> zones = null;
        boolean economyEnabled = false;
        double safeMultiplier = 2.0;
        ZonePermissionsConfig zonePermissionsConfig = null;
        try {
            if (VaryonPlugin.getStaticConfigManager() != null) {
                rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
                zones = VaryonPlugin.getStaticConfigManager().getZoneConfig().getZones();
                economyEnabled = rtpvConfig.isEconomyEnabled();
                safeMultiplier = rtpvConfig.getSafeCostMultiplier();
                zonePermissionsConfig = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
            }
        } catch (Exception ignored) {}

        int maxAccessibleZone = 1;
        if (zonePermissionsConfig != null) {
            maxAccessibleZone = zonePermissionsConfig.getMaxAccessibleZone(ownerRef);
        } else {
            maxAccessibleZone = zones != null ? zones.size() : 10;
        }

        int totalZones = zones != null ? zones.size() : 10;
        for (int i = 1; i <= totalZones; i++) {
            boolean accessible = i <= maxAccessibleZone;

            if (!accessible) {
                commandBuilder.set("#ZoneImg" + i + ".Background", "varyon_zones_no.png");
                commandBuilder.set("#ZoneMain" + i + ".Disabled", true);
                commandBuilder.set("#ZonePvP" + i + ".Disabled", true);
                commandBuilder.set("#ZoneSafe" + i + ".Disabled", true);
                commandBuilder.set("#ZoneRadius" + i + ".Visible", false);
                commandBuilder.set("#ZoneMainLead" + i + ".Text", "");
                commandBuilder.set("#ZoneMainAmount" + i + ".Text", "");
                commandBuilder.set("#ZoneMainCoinSlot" + i + ".Visible", false);
                commandBuilder.setNull("#ZoneMainCoin" + i + ".ItemId");
                commandBuilder.set("#ZonePvPPriceRow" + i + ".Visible", false);
                commandBuilder.set("#ZoneSafePriceRow" + i + ".Visible", false);
                continue;
            }

            commandBuilder.set("#ZoneRadius" + i + ".Visible", true);
            commandBuilder.set("#ZoneRadiusValue" + i + ".Text", radiusSubtitleLine(zones, i));

            DifficultyZone zone = zones != null ? zones.get(i - 1) : null;
            int baseCost = zone != null ? zone.getTeleportCost() : 0;
            int safeCost = (int) Math.ceil(baseCost * safeMultiplier);

            if (economyEnabled) {
                commandBuilder.set("#ZoneMainLead" + i + ".Text", RANDOM_LABEL + " ");
                commandBuilder.set("#ZoneMainAmount" + i + ".Text", String.valueOf(baseCost));
                commandBuilder.set("#ZoneMainCoinSlot" + i + ".Visible", true);
                EconomyCoinItemHelper.applyCoinItem(commandBuilder, "ZoneMainCoin" + i);
                commandBuilder.set("#ZonePvPPriceRow" + i + ".Visible", true);
                commandBuilder.set("#ZoneSafePriceRow" + i + ".Visible", true);
                commandBuilder.set("#ZonePvPPrice" + i + ".Text", ECON_PRICE_PREFIX + baseCost);
                commandBuilder.set("#ZoneSafePrice" + i + ".Text", ECON_PRICE_PREFIX + safeCost);
            } else {
                commandBuilder.set("#ZoneMainLead" + i + ".Text", RANDOM_LABEL);
                commandBuilder.set("#ZoneMainAmount" + i + ".Text", "");
                commandBuilder.set("#ZoneMainCoinSlot" + i + ".Visible", false);
                commandBuilder.setNull("#ZoneMainCoin" + i + ".ItemId");
                commandBuilder.set("#ZonePvPPriceRow" + i + ".Visible", false);
                commandBuilder.set("#ZoneSafePriceRow" + i + ".Visible", false);
            }

            int zoneId = i;
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZoneMain" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)).append("Pvp", "none"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZonePvP" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)).append("Pvp", "true"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZoneSafe" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)).append("Pvp", "false"));
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
            if (player != null && playerRefComp != null) {
                try {
                    int requestedZone = Integer.parseInt(data.zoneId);
                    ZonePermissionsConfig zonePerms = VaryonPlugin.getStaticConfigManager() != null
                        ? VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig() : null;
                    if (zonePerms != null && !zonePerms.canAccessZone(playerRefComp, requestedZone)) {
                        return;
                    }
                } catch (NumberFormatException ignored) {}

                player.getPageManager().setPage(ref, store, Page.None);
                String command;
                if ("true".equals(data.pvp)) {
                    command = "rtpv " + data.zoneId + " true";
                } else if ("false".equals(data.pvp)) {
                    command = "rtpv " + data.zoneId + " false";
                } else {
                    command = "rtpv " + data.zoneId;
                }
                CommandManager.get().handleCommand(playerRefComp, command);
            }
        }
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING),
                    (entry, s) -> entry.action = s, entry -> entry.action)
                .addField(new KeyedCodec<>("ZoneId", Codec.STRING),
                    (entry, s) -> entry.zoneId = s, entry -> entry.zoneId)
                .addField(new KeyedCodec<>("Pvp", Codec.STRING),
                    (entry, s) -> entry.pvp = s, entry -> entry.pvp)
                .build();

        public String action;
        public String zoneId;
        @Nullable public String pvp;
    }
}