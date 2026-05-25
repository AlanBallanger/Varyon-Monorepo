package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.portal.RtpvConfirmUIPage;
import com.varyon.rtpv.RtpvConfirmManager;
import com.varyon.rtpv.RtpvCooldownStore;
import com.varyon.rtpv.RtpvJoinManager;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.safezone.SafeZoneQuadrant;
import com.varyon.teleport.FirstSpawnStyleParticleFx;
import com.varyon.teleport.RtpService;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class RtpvCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService;
    private final Random random = new Random();
    private final RequiredArg<Integer> zoneArg;

    public RtpvCommand() {
        super("rtpv", "Random teleport to a mod zone");
        this.requirePermission("varyon.rtp");
        this.rtpService = new RtpService();
        this.zoneArg = this.withRequiredArg("zone", "Zone number (1-10)", ArgTypes.INTEGER);
        this.addUsageVariant(new PvpVariant());
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        int zoneNumber = context.get(zoneArg);
        executeRtp(context, store, ref, playerRef, world, zoneNumber, null);
    }

    private class PvpVariant extends CommandBase {
        private final RequiredArg<Integer> zoneArg;
        private final RequiredArg<String> pvpArg;

        PvpVariant() {
            super("Téléportation vers une zone mod avec filtre PvP");
            this.zoneArg = this.withRequiredArg("zone", "Zone number (1-10)", ArgTypes.INTEGER);
            this.pvpArg = this.withRequiredArg("pvp", "true/false - téléporter en zone PvP ou hors PvP", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.raw("Cette commande doit être exécutée par un joueur.").color(Color.RED));
                return;
            }
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            if (ref == null) return;
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();

            int zoneNumber = context.get(zoneArg);
            String pvpRaw = context.get(pvpArg).toLowerCase().trim();
            Boolean pvpFilter;
            if (pvpRaw.equals("true") || pvpRaw.equals("on") || pvpRaw.equals("pvp")) {
                pvpFilter = true;
            } else if (pvpRaw.equals("false") || pvpRaw.equals("off") || pvpRaw.equals("safe")) {
                pvpFilter = false;
            } else {
                context.sendMessage(Message.raw("Valeur pvp invalide. Utilisez : true, false, on, off").color(Color.RED));
                return;
            }

            final Boolean finalPvpFilter = pvpFilter;
            world.execute(() -> {
                PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;
                executeRtp(context, store, ref, playerRef, world, zoneNumber, finalPvpFilter);
            });
        }
    }

    void executeRtp(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                    @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world,
                    int zoneNumber, @Nullable Boolean pvpFilter) {
        ZoneConfig config = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        List<DifficultyZone> zones = config.getZones();

        if (zoneNumber < 1 || zoneNumber > zones.size()) {
            context.sendMessage(Message.raw("Zone invalide. Zones disponibles : 1-" + zones.size()).color(Color.RED));
            return;
        }

        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            ZonePermissionsConfig zonePerms = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
            if (!zonePerms.canAccessZone(player, zoneNumber)) {
                String required = zonePerms.getPermissionForZone(zoneNumber);
                context.sendMessage(Message.raw(
                    "Vous n'avez pas accès à la zone " + zoneNumber + ". Permission requise : " + required
                ).color(Color.RED));
                return;
            }
        }

        DifficultyZone targetZone = zones.get(zoneNumber - 1);

        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
        int cooldownSec = rtpvConfig.getCooldownSeconds();
        if (cooldownSec > 0) {
            int remain = RtpvCooldownStore.getRemainingCooldownSeconds(playerRef.getUuid(), cooldownSec);
            if (remain > 0) {
                context.sendMessage(Message.raw(
                    "Téléportation aléatoire en cooldown. Réessayez dans " + remain + " s."
                ).color(Color.RED));
                return;
            }
        }

        if (!RtpvCooldownStore.isConsecutiveRtpvAllowed(playerRef.getUuid(), cooldownSec)) {
            context.sendMessage(Message.raw(
                "Limite atteinte : 5 téléportations aléatoires d’affilée maximum."
            ).color(Color.RED));
            return;
        }

        int baseCost = targetZone.getTeleportCost();
        double multipliedCost = (pvpFilter != null && !pvpFilter)
            ? baseCost * rtpvConfig.getSafeCostMultiplier()
            : baseCost;
        final int finalCost = (int) Math.ceil(multipliedCost);
        final BigDecimal costBD = BigDecimal.valueOf(finalCost);

        if (rtpvConfig.isEconomyEnabled()) {
            try {
                if (!hasEnoughBalance(playerRef, costBD)) {
                    BigDecimal balance = getBalance(playerRef);
                    context.sendMessage(Message.raw(
                        "Coins insuffisants. Coût : " + finalCost + " | Solde : " + balance.intValue()
                    ).color(Color.RED));
                    return;
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible, vérification économie ignorée");
            }
        }

        double minDist = targetZone.getRadiusStart();
        double maxDist = (zoneNumber < zones.size()) ? zones.get(zoneNumber).getRadiusStart() : minDist + 5000;

        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw("World generation not supported in this world").color(Color.RED));
            return;
        }
        ChunkGenerator generator = (ChunkGenerator) worldGen;

        String pvpLabel = pvpFilter == null ? "" : (pvpFilter ? " (PvP)" : " (Hors PvP)");
        String costLabel = rtpvConfig.isEconomyEnabled() ? " [" + finalCost + " coins]" : "";
        context.sendMessage(Message.raw("Téléportation vers " + targetZone.getName() + pvpLabel + costLabel + "...").color(Color.GREEN));

        world.execute(() -> {
            try {
                double[] angleRange = pickAngleRange(pvpFilter);

                Vector3d safePosition = rtpService.findSafePositionInRing(world, generator, minDist, maxDist, angleRange[0], angleRange[1], RtpService.DEFAULT_RTP_MAX_ATTEMPTS);

                if (safePosition != null) {
                    teleportPlayer(store, ref, world, safePosition, rtpvConfig.getJoinDurationSeconds());

                    if (rtpvConfig.isEconomyEnabled()) {
                        try {
                            withdrawBalance(playerRef, costBD, finalCost);
                        } catch (NoClassDefFoundError e) {
                            LOGGER.at(Level.WARNING).log("Vault non disponible, déduction ignorée");
                        }
                    }

                    RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
                    if (joinMgr != null) {
                        long expireAt = System.currentTimeMillis() + rtpvConfig.getJoinDurationSeconds() * 1000L;
                        joinMgr.markJoinable(playerRef.getUuid(), zoneNumber, world.getName(), expireAt,
                            safePosition.x, safePosition.y, safePosition.z, 0f, 0f, 0f);
                    }

                    context.sendMessage(Message.raw("Téléporté vers " + targetZone.getName() + pvpLabel +
                        " en " + (int) safePosition.x + ", " + (int) safePosition.y + ", " + (int) safePosition.z +
                        (rtpvConfig.isEconomyEnabled() ? " (-" + finalCost + " coins)" : "")).color(Color.GREEN));

                    scheduleConfirmMenu(playerRef, world, zoneNumber, pvpFilter, finalCost);
                    RtpvCooldownStore.recordSuccessfulRtpv(playerRef.getUuid());
                    RtpvCooldownStore.incrementConsecutiveRtpv(playerRef.getUuid());
                } else {
                    context.sendMessage(Message.raw("Impossible de trouver un emplacement sûr dans " + targetZone.getName()).color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Error during RTP: " + e.getMessage(), e);
                context.sendMessage(Message.raw("Échec de la téléportation").color(Color.RED));
            }
        });
    }

    private boolean hasEnoughBalance(PlayerRef playerRef, BigDecimal cost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return true;
        return economy.has("Varyon", playerRef.getUuid(), cost);
    }

    private BigDecimal getBalance(PlayerRef playerRef) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return BigDecimal.ZERO;
        return economy.getBalance("Varyon", playerRef.getUuid());
    }

    private void withdrawBalance(PlayerRef playerRef, BigDecimal cost, int finalCost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return;
        EconomyResponse response = economy.withdraw("Varyon", playerRef.getUuid(), cost);
        if (!response.transactionSuccess()) {
            LOGGER.at(Level.WARNING).log("Failed to deduct " + finalCost + " coins from " + playerRef.getUuid() + ": " + response.errorMessage);
        }
    }

    private double[] pickAngleRange(@Nullable Boolean pvpFilter) {
        if (pvpFilter == null) {
            return new double[]{0, 2 * Math.PI};
        }

        SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
        if (szm == null) {
            return new double[]{0, 2 * Math.PI};
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
            return new double[]{0, 2 * Math.PI};
        }

        SafeZoneQuadrant chosen = candidates.get(random.nextInt(candidates.size()));
        return new double[]{
            Math.toRadians(chosen.getStartAngle()),
            Math.toRadians(chosen.getEndAngle())
        };
    }

    private void teleportPlayer(Store<EntityStore> store, Ref<EntityStore> ref, World world, Vector3d position,
                                int joinDurationSeconds) {
        Teleport teleport = Teleport.createForPlayer(world, position, new Vector3f(0, 0, 0));
        store.addComponent(ref, Teleport.getComponentType(), teleport);
        FirstSpawnStyleParticleFx.playAt(world, position, ref, store, joinDurationSeconds);
    }

    private void scheduleConfirmMenu(
        @Nonnull PlayerRef playerRef,
        @Nonnull World world,
        int zoneNumber,
        @Nullable Boolean pvpFilter,
        int paidCost
    ) {
        RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
        if (mgr == null) {
            return;
        }
        int firstRetryOrdinal = 1;

        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> world.execute(() -> {
                Ref<EntityStore> liveRef = playerRef.getReference();
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
                    new RtpvConfirmUIPage(playerRef, zoneNumber, pvpFilter, paidCost, firstRetryOrdinal));
            }),
            5_000L,
            TimeUnit.MILLISECONDS);
        mgr.schedulePendingMenu(playerRef.getUuid(), future);
    }
}
