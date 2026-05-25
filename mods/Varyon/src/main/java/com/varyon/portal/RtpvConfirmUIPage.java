package com.varyon.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.rtpv.RtpvConfirmManager;
import com.varyon.rtpv.RtpvJoinManager;
import com.varyon.rtpv.RtpvCooldownStore;
import com.varyon.rtpv.RtpvRetryPricing;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.safezone.SafeZoneQuadrant;
import com.varyon.teleport.FirstSpawnStyleParticleFx;
import com.varyon.teleport.RtpService;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RtpvConfirmUIPage extends InteractiveCustomUIPage<RtpvConfirmUIPage.EventDataClass> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long POST_RETRY_CONFIRM_DELAY_MS = 5_000L;
    private static final long SNOOZE_MENU_DELAY_MS = 15_000L;
    private static final Random RANDOM = new Random();

    private final int zoneId;
    @Nullable private final Boolean pvpFilter;
    private final int chainBase;
    private final int retryOrdinal;

    public RtpvConfirmUIPage(
        @Nonnull PlayerRef playerRef,
        int zoneId,
        @Nullable Boolean pvpFilter,
        int chainBase,
        int retryOrdinal
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.zoneId = zoneId;
        this.pvpFilter = pvpFilter;
        this.chainBase = chainBase;
        this.retryOrdinal = retryOrdinal;
    }

    private int currentRetryPrice() {
        return RtpvRetryPricing.retryCost(chainBase, retryOrdinal);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("RtpvConfirmMenu.ui");
        commandBuilder.set("#TitleLabel.Text", "Téléportation aléatoire Varyon");

        RtpvConfig rtpvConfig = null;
        boolean economyEnabled = false;
        try {
            if (VaryonPlugin.getStaticConfigManager() != null) {
                rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
                economyEnabled = rtpvConfig != null && rtpvConfig.isEconomyEnabled();
            }
        } catch (Exception ignored) {
        }

        String noLabel = economyEnabled
            ? "Non, me re-téléporter pour " + currentRetryPrice() + " Coins"
            : "Non, me re-téléporter";
        commandBuilder.set("#NoButtonLabel.Text", noLabel);
        commandBuilder.set("#MaybeButtonLabel.Text", "Peut-être, laisse moi 15 secondes");

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#YesButton", EventData.of("Action", "yes"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#MaybeButton", EventData.of("Action", "maybe"));
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
        PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());

        if ("yes".equals(data.action)) {
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            if (playerRefComp != null) {
                RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
                if (mgr != null) {
                    mgr.cancelPendingMenu(playerRefComp.getUuid());
                }
                RtpvCooldownStore.resetConsecutiveRtpv(playerRefComp.getUuid());
            }
            return;
        }

        if ("maybe".equals(data.action) && player != null && playerRefComp != null) {
            player.getPageManager().setPage(ref, store, Page.None);
            RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
            if (mgr != null) {
                mgr.cancelPendingMenu(playerRefComp.getUuid());
            }
            World world = ((EntityStore) store.getExternalData()).getWorld();
            scheduleSnoozedMenu(playerRefComp, world);
            return;
        }

        if ("no".equals(data.action) && player != null && playerRefComp != null) {
            player.getPageManager().setPage(ref, store, Page.None);
            handleRetry(ref, store, playerRefComp);
        }
    }

    private void handleRetry(
        @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRefComp) {
        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager() != null
            ? VaryonPlugin.getStaticConfigManager().getRtpvConfig() : null;
        int cooldownSec = rtpvConfig != null ? rtpvConfig.getCooldownSeconds() : 0;
        if (!RtpvCooldownStore.isConsecutiveRtpvAllowed(playerRefComp.getUuid(), cooldownSec)) {
            playerRefComp.sendMessage(Message.raw(
                "Limite atteinte : 5 téléportations aléatoires d’affilée maximum."
            ).color(Color.RED));
            return;
        }
        boolean economyEnabled = rtpvConfig != null && rtpvConfig.isEconomyEnabled();

        if (economyEnabled) {
            int price = currentRetryPrice();
            BigDecimal costBD = BigDecimal.valueOf(price);
            try {
                if (!hasEnoughBalance(playerRefComp, costBD)) {
                    BigDecimal balance = getBalance(playerRefComp);
                    playerRefComp.sendMessage(Message.raw(
                        "Coins insuffisants pour la re-téléportation. Coût : " + price +
                            " | Solde : " + balance.intValue()
                    ).color(Color.RED));
                    return;
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible lors du contrôle économie confirm");
            }
        }

        World world = ((EntityStore) store.getExternalData()).getWorld();
        ZoneConfig config = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        List<DifficultyZone> zones = config.getZones();
        DifficultyZone targetZone = zones.get(zoneId - 1);
        double minDist = targetZone.getRadiusStart();
        double maxDist = (zoneId < zones.size()) ? zones.get(zoneId).getRadiusStart() : minDist + 5000;
        final int finalRetryCost = economyEnabled ? currentRetryPrice() : 0;

        world.execute(() -> {
            try {
                IWorldGen worldGen = world.getChunkStore().getGenerator();
                if (!(worldGen instanceof ChunkGenerator generator)) {
                    playerRefComp.sendMessage(Message.raw("World generation non supportée").color(Color.RED));
                    return;
                }

                double[] angleRange = pickAngleRange(pvpFilter);
                RtpService rtpService = new RtpService();
                Vector3d safePos = rtpService.findSafePositionInRing(
                    world, generator, minDist, maxDist,
                    angleRange[0], angleRange[1], RtpService.DEFAULT_RTP_MAX_ATTEMPTS);

                if (safePos == null) {
                    playerRefComp.sendMessage(Message.raw(
                        "Impossible de trouver un emplacement sûr dans " + targetZone.getName()
                    ).color(Color.RED));
                    return;
                }

                Teleport teleport = Teleport.createForPlayer(world, safePos, new Vector3f(0, 0, 0));
                store.addComponent(ref, Teleport.getComponentType(), teleport);
                FirstSpawnStyleParticleFx.playAt(world, safePos, ref, store,
                    rtpvConfig != null ? rtpvConfig.getJoinDurationSeconds() : 0);

                if (finalRetryCost > 0) {
                    try {
                        withdrawBalance(playerRefComp, BigDecimal.valueOf(finalRetryCost), finalRetryCost);
                    } catch (NoClassDefFoundError e) {
                        LOGGER.at(Level.WARNING).log("Vault non disponible lors de la déduction confirm");
                    }
                }

                RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
                if (joinMgr != null && rtpvConfig != null) {
                    long expireAt = System.currentTimeMillis() + rtpvConfig.getJoinDurationSeconds() * 1000L;
                    joinMgr.markJoinable(playerRefComp.getUuid(), zoneId, world.getName(), expireAt,
                        safePos.x, safePos.y, safePos.z, 0f, 0f, 0f);
                }

                String pvpLabel = pvpFilter == null ? "" : (pvpFilter ? " (PvP)" : " (Hors PvP)");
                String costLabel = finalRetryCost > 0 ? " (-" + finalRetryCost + " coins)" : "";
                playerRefComp.sendMessage(Message.raw(
                    "Re-téléporté vers " + targetZone.getName() + pvpLabel + costLabel
                ).color(Color.GREEN));

                RtpvCooldownStore.incrementConsecutiveRtpv(playerRefComp.getUuid());

                scheduleNextConfirm(playerRefComp, world);

            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur re-téléportation confirm: " + e.getMessage(), e);
                playerRefComp.sendMessage(Message.raw("Erreur lors de la re-téléportation").color(Color.RED));
            }
        });
    }

    private void scheduleNextConfirm(@Nonnull PlayerRef playerRefComp, @Nonnull World world) {
        RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
        if (mgr == null) {
            return;
        }

        int nextOrdinal = retryOrdinal + 1;
        int capturedZoneId = zoneId;
        Boolean capturedPvpFilter = pvpFilter;
        int capturedChainBase = chainBase;

        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> world.execute(() -> {
                Ref<EntityStore> liveRef = playerRefComp.getReference();
                if (liveRef == null || !liveRef.isValid()) {
                    return;
                }
                Store<EntityStore> liveStore = liveRef.getStore();
                Player livePlayer = liveStore.getComponent(liveRef, Player.getComponentType());
                if (livePlayer == null) {
                    return;
                }
                livePlayer.getPageManager().openCustomPage(
                    liveRef, liveStore,
                    new RtpvConfirmUIPage(playerRefComp, capturedZoneId, capturedPvpFilter, capturedChainBase, nextOrdinal)
                );
            }),
            POST_RETRY_CONFIRM_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
        mgr.schedulePendingMenu(playerRefComp.getUuid(), future);
    }

    private void scheduleSnoozedMenu(@Nonnull PlayerRef playerRefComp, @Nonnull World world) {
        RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
        if (mgr == null) {
            return;
        }
        int capturedZoneId = zoneId;
        Boolean capturedPvpFilter = pvpFilter;
        int capturedOrdinal = retryOrdinal;
        int capturedChainBase = chainBase;

        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> world.execute(() -> {
                Ref<EntityStore> liveRef = playerRefComp.getReference();
                if (liveRef == null || !liveRef.isValid()) {
                    return;
                }
                Store<EntityStore> liveStore = liveRef.getStore();
                Player livePlayer = liveStore.getComponent(liveRef, Player.getComponentType());
                if (livePlayer == null) {
                    return;
                }
                livePlayer.getPageManager().openCustomPage(
                    liveRef, liveStore,
                    new RtpvConfirmUIPage(playerRefComp, capturedZoneId, capturedPvpFilter, capturedChainBase, capturedOrdinal)
                );
            }),
            SNOOZE_MENU_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
        mgr.schedulePendingMenu(playerRefComp.getUuid(), future);
    }

    private double[] pickAngleRange(@Nullable Boolean pvpFilter) {
        if (pvpFilter == null) {
            return new double[] {0, 2 * Math.PI};
        }
        SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
        if (szm == null) {
            return new double[] {0, 2 * Math.PI};
        }
        SafeZoneQuadrant current = szm.getCurrentQuadrant();
        SafeZoneQuadrant next = szm.getNextQuadrant();
        boolean overlap = szm.isOverlapActive();

        List<SafeZoneQuadrant> candidates = new ArrayList<>();
        for (SafeZoneQuadrant q : SafeZoneQuadrant.values()) {
            boolean isSafe = q == current || (overlap && q == next);
            if (pvpFilter ? !isSafe : isSafe) {
                candidates.add(q);
            }
        }
        if (candidates.isEmpty()) {
            return new double[] {0, 2 * Math.PI};
        }
        SafeZoneQuadrant chosen = candidates.get(RANDOM.nextInt(candidates.size()));
        return new double[] {
            Math.toRadians(chosen.getStartAngle()),
            Math.toRadians(chosen.getEndAngle())
        };
    }

    private boolean hasEnoughBalance(@Nonnull PlayerRef playerRef, @Nonnull BigDecimal cost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) {
            return true;
        }
        return economy.has("Varyon", playerRef.getUuid(), cost);
    }

    @Nonnull
    private BigDecimal getBalance(@Nonnull PlayerRef playerRef) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) {
            return BigDecimal.ZERO;
        }
        return economy.getBalance("Varyon", playerRef.getUuid());
    }

    private void withdrawBalance(@Nonnull PlayerRef playerRef, @Nonnull BigDecimal cost, int displayCost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) {
            return;
        }
        EconomyResponse response = economy.withdraw("Varyon", playerRef.getUuid(), cost);
        if (!response.transactionSuccess()) {
            LOGGER.at(Level.WARNING).log(
                "Failed to deduct " + displayCost + " coins from " + playerRef.getUuid() + ": " + response.errorMessage
            );
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
