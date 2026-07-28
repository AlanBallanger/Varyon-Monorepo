package com.varyon.rtpv;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.hud.RtpvConfirmHud;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.safezone.SafeZoneQuadrant;
import com.varyon.teleport.FirstSpawnStyleParticleFx;
import com.varyon.teleport.RtpService;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Handles the "keep this RTP location or pay to re-roll" flow: shows the non-blocking
 * confirm HUD after a random teleport, and performs the paid re-teleport when the
 * player presses the confirm key (E) within the confirm window.
 */
public final class RtpvRetryService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long CONFIRM_WINDOW_MS = RtpvConfirmHud.DURATION_MS;
    private static final Random RANDOM = new Random();

    private RtpvRetryService() {
    }

    public static void showConfirmHud(
        @Nonnull PlayerRef playerRef,
        @Nonnull World world,
        int zoneId,
        @Nullable Boolean pvpFilter,
        int chainBase,
        int retryOrdinal
    ) {
        RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
        if (mgr == null) {
            return;
        }

        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager() != null
            ? VaryonPlugin.getStaticConfigManager().getRtpvConfig() : null;
        boolean economyEnabled = rtpvConfig != null && rtpvConfig.isEconomyEnabled();
        int retryPrice = RtpvRetryPricing.retryCost(chainBase, retryOrdinal);

        long expiresAt = System.currentTimeMillis() + CONFIRM_WINDOW_MS;
        mgr.setPendingConfirm(playerRef.getUuid(),
            new RtpvConfirmManager.PendingConfirm(zoneId, pvpFilter, chainBase, retryOrdinal, expiresAt));

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        String subtitle = economyEnabled
            ? "Pas satisfait de ta position ? Appuie sur A pour te re-téléporter (" + retryPrice + " coins)"
            : "Pas satisfait de ta position ? Appuie sur A pour te re-téléporter";

        RtpvConfirmHud hud = RtpvConfirmHud.getOrCreate(player, playerRef);
        hud.show("Téléportation aléatoire sur Varyon", subtitle);
    }

    /**
     * Called when the player presses the confirm key (A) while a confirm window is active.
     */
    public static void handleConfirmKeyPress(@Nonnull PlayerRef playerRef) {
        RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
        if (mgr == null) {
            return;
        }

        RtpvConfirmManager.PendingConfirm pending = mgr.getPendingConfirm(playerRef.getUuid());
        if (pending == null || pending.isExpired()) {
            return;
        }
        mgr.clearPending(playerRef.getUuid());

        RtpvConfirmHud hud = RtpvConfirmHud.get(playerRef.getUuid());
        if (hud != null) {
            hud.hide();
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        World world = ((EntityStore) store.getExternalData()).getWorld();

        retry(ref, store, world, playerRef, pending.zoneId, pending.pvpFilter, pending.chainBase, pending.retryOrdinal);
    }

    private static void retry(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull World world,
        @Nonnull PlayerRef playerRef,
        int zoneId,
        @Nullable Boolean pvpFilter,
        int chainBase,
        int retryOrdinal
    ) {
        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager() != null
            ? VaryonPlugin.getStaticConfigManager().getRtpvConfig() : null;
        int cooldownSec = rtpvConfig != null ? rtpvConfig.getCooldownSeconds() : 0;
        if (!RtpvCooldownStore.isConsecutiveRtpvAllowed(playerRef.getUuid(), cooldownSec)) {
            playerRef.sendMessage(Message.raw(
                "Limite atteinte : 5 téléportations aléatoires d'affilée maximum."
            ).color(Color.RED));
            return;
        }
        boolean economyEnabled = rtpvConfig != null && rtpvConfig.isEconomyEnabled();
        int price = RtpvRetryPricing.retryCost(chainBase, retryOrdinal);

        if (economyEnabled) {
            BigDecimal costBD = BigDecimal.valueOf(price);
            try {
                if (!hasEnoughBalance(playerRef, costBD)) {
                    BigDecimal balance = getBalance(playerRef);
                    playerRef.sendMessage(Message.raw(
                        "Coins insuffisants pour la re-téléportation. Coût : " + price +
                            " | Solde : " + balance.intValue()
                    ).color(Color.RED));
                    return;
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible lors du contrôle économie confirm");
            }
        }

        ZoneConfig config = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        List<DifficultyZone> zones = config.getZones();
        DifficultyZone targetZone = zones.get(zoneId - 1);
        double minDist = targetZone.getRadiusStart();
        double maxDist = (zoneId < zones.size()) ? zones.get(zoneId).getRadiusStart() : minDist + 5000;
        final int finalRetryCost = economyEnabled ? price : 0;

        world.execute(() -> {
            try {
                IWorldGen worldGen = world.getChunkStore().getGenerator();
                if (!(worldGen instanceof ChunkGenerator generator)) {
                    playerRef.sendMessage(Message.raw("World generation non supportée").color(Color.RED));
                    return;
                }

                double[] angleRange = pickAngleRange(pvpFilter);
                RtpService rtpService = new RtpService();
                org.joml.Vector3d safePos = rtpService.findSafePositionInRing(
                    world, generator, minDist, maxDist,
                    angleRange[0], angleRange[1], RtpService.DEFAULT_RTP_MAX_ATTEMPTS);

                if (safePos == null) {
                    playerRef.sendMessage(Message.raw(
                        "Impossible de trouver un emplacement sûr dans " + targetZone.getName()
                    ).color(Color.RED));
                    return;
                }

                Teleport teleport = Teleport.createForPlayer(world, safePos, com.hypixel.hytale.math.vector.Rotation3f.ZERO);
                store.addComponent(ref, Teleport.getComponentType(), teleport);
                FirstSpawnStyleParticleFx.playAt(world, safePos, ref, store,
                    rtpvConfig != null ? rtpvConfig.getJoinDurationSeconds() : 0);

                if (finalRetryCost > 0) {
                    try {
                        withdrawBalance(playerRef, BigDecimal.valueOf(finalRetryCost), finalRetryCost);
                    } catch (NoClassDefFoundError e) {
                        LOGGER.at(Level.WARNING).log("Vault non disponible lors de la déduction confirm");
                    }
                }

                RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
                if (joinMgr != null && rtpvConfig != null) {
                    long expireAt = System.currentTimeMillis() + rtpvConfig.getJoinDurationSeconds() * 1000L;
                    joinMgr.markJoinable(playerRef.getUuid(), zoneId, world.getName(), expireAt,
                        safePos.x, safePos.y, safePos.z, 0f, 0f, 0f);
                }

                String pvpLabel = pvpFilter == null ? "" : (pvpFilter ? " (PvP)" : " (Hors PvP)");
                String costLabel = finalRetryCost > 0 ? " (-" + finalRetryCost + " coins)" : "";
                playerRef.sendMessage(Message.raw(
                    "Re-téléporté vers " + targetZone.getName() + pvpLabel + costLabel
                ).color(Color.GREEN));

                RtpvCooldownStore.incrementConsecutiveRtpv(playerRef.getUuid());

                showConfirmHud(playerRef, world, zoneId, pvpFilter, chainBase, retryOrdinal + 1);

            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur re-téléportation confirm: " + e.getMessage(), e);
                playerRef.sendMessage(Message.raw("Erreur lors de la re-téléportation").color(Color.RED));
            }
        });
    }

    private static double[] pickAngleRange(@Nullable Boolean pvpFilter) {
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

    private static boolean hasEnoughBalance(@Nonnull PlayerRef playerRef, @Nonnull BigDecimal cost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) {
            return true;
        }
        return economy.has("Varyon", playerRef.getUuid(), cost);
    }

    @Nonnull
    private static BigDecimal getBalance(@Nonnull PlayerRef playerRef) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) {
            return BigDecimal.ZERO;
        }
        return economy.getBalance("Varyon", playerRef.getUuid());
    }

    private static void withdrawBalance(@Nonnull PlayerRef playerRef, @Nonnull BigDecimal cost, int displayCost) {
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
}
