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
import com.hypixel.hytale.server.core.Message;
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
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.teleport.FirstSpawnStyleParticleFx;

import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import org.joml.Vector3d;

import com.hypixel.hytale.server.core.entity.entities.Player;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArenasPortalUIPage extends InteractiveCustomUIPage<ArenasPortalUIPage.EventDataClass> {
    private static final com.hypixel.hytale.logger.HytaleLogger LOGGER = com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final int WINDOW_WIDTH = 852;
    private static final int WINDOW_HEIGHT = 595;

    private static final String TELEPORT_LABEL = "Se téléporter";

    private static final double[][] ARENA_COORDS = {
        {106, 122, -3},
        {5247.5, 202, -14.5},
        {7918.5, 130, 223.5},
        {10035.5, 123, -13.5},
        {12609.5, 136, 82.5},
        {15084.5, 117, -22.5},
        {17981.5, 121, 43.5},
        {20301.5, 118, -428.5},
        {22597.5, 127, -3.5},
        {25207.5, 119, -69.5},
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
        RtpvConfig rtpvConfig = null;
        List<DifficultyZone> zones = null;
        boolean economyEnabled = false;
        double safeMultiplier = 2.0;
        try {
            if (VaryonPlugin.getStaticConfigManager() != null) {
                zonePermissionsConfig = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
                rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
                zones = VaryonPlugin.getStaticConfigManager().getZoneConfig().getZones();
                economyEnabled = rtpvConfig.isEconomyEnabled();
                safeMultiplier = rtpvConfig.getSafeCostMultiplier();
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
                commandBuilder.set("#ZoneMainCoinSlot" + i + ".Visible", false);
                commandBuilder.set("#ZoneMainAmount" + i + ".Text", "");
                commandBuilder.setNull("#ZoneMainCoin" + i + ".ItemId");
                continue;
            }

            commandBuilder.set("#ZoneRadius" + i + ".Visible", true);

            int arenaCost = arenaTeleportCost(zones, i, safeMultiplier);

            if (economyEnabled) {
                commandBuilder.set("#ZoneMainLead" + i + ".Text", TELEPORT_LABEL + " ");
                commandBuilder.set("#ZoneMainAmount" + i + ".Text", String.valueOf(arenaCost));
                commandBuilder.set("#ZoneMainCoinSlot" + i + ".Visible", true);
                EconomyCoinItemHelper.applyCoinItem(commandBuilder, "ZoneMainCoin" + i);
            } else {
                commandBuilder.set("#ZoneMainLead" + i + ".Text", TELEPORT_LABEL);
                commandBuilder.set("#ZoneMainAmount" + i + ".Text", "");
                commandBuilder.set("#ZoneMainCoinSlot" + i + ".Visible", false);
                commandBuilder.setNull("#ZoneMainCoin" + i + ".ItemId");
            }

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

            RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager() != null
                ? VaryonPlugin.getStaticConfigManager().getRtpvConfig() : null;
            List<DifficultyZone> zones = VaryonPlugin.getStaticConfigManager() != null
                ? VaryonPlugin.getStaticConfigManager().getZoneConfig().getZones() : null;

            if (rtpvConfig != null && rtpvConfig.isEconomyEnabled()) {
                int cost = arenaTeleportCost(zones, requestedZone, rtpvConfig.getSafeCostMultiplier());
                BigDecimal costBD = BigDecimal.valueOf(cost);
                try {
                    if (!hasEnoughBalance(playerRefComp, costBD)) {
                        BigDecimal balance = getBalance(playerRefComp);
                        playerRefComp.sendMessage(Message.raw(
                            "Coins insuffisants. Coût : " + cost + " | Solde : " + balance.intValue()
                        ).color(Color.RED));
                        return;
                    }
                } catch (NoClassDefFoundError e) {
                    LOGGER.at(Level.WARNING).log("Vault non disponible, vérification économie ignorée");
                }

                try {
                    withdrawBalance(playerRefComp, costBD, cost);
                } catch (NoClassDefFoundError e) {
                    LOGGER.at(Level.WARNING).log("Vault non disponible, déduction ignorée");
                }
            }

            player.getPageManager().setPage(ref, store, Page.None);

            double[] coords = ARENA_COORDS[requestedZone - 1];
            Vector3d position = new Vector3d(coords[0], coords[1], coords[2]);
            Teleport teleport = Teleport.createForPlayer(world, position, Rotation3f.ZERO);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
            FirstSpawnStyleParticleFx.playAt(world, position, ref, store, 0);
        }
    }

    private static int arenaTeleportCost(@Nullable List<DifficultyZone> zones, int arenaIndexOneBased, double safeMultiplier) {
        if (zones == null || arenaIndexOneBased < 1 || arenaIndexOneBased > zones.size()) {
            return 0;
        }
        int baseCost = zones.get(arenaIndexOneBased - 1).getTeleportCost();
        return (int) Math.ceil(baseCost * safeMultiplier);
    }

    private static boolean hasEnoughBalance(PlayerRef playerRef, BigDecimal cost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return true;
        return economy.has("Varyon", playerRef.getUuid(), cost);
    }

    private static BigDecimal getBalance(PlayerRef playerRef) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return BigDecimal.ZERO;
        return economy.getBalance("Varyon", playerRef.getUuid());
    }

    private static void withdrawBalance(PlayerRef playerRef, BigDecimal cost, int finalCost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return;
        EconomyResponse response = economy.withdraw("Varyon", playerRef.getUuid(), cost);
        if (!response.transactionSuccess()) {
            LOGGER.at(Level.WARNING).log("Failed to deduct " + finalCost + " coins from " + playerRef.getUuid() + ": " + response.errorMessage);
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
