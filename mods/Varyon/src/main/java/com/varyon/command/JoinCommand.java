package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.portal.JoinRequestConfirmUIPage;
import com.varyon.rtpv.RtpvJoinManager;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class JoinCommand extends AbstractAsyncCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RequiredArg<PlayerRef> playerArg;

    public JoinCommand() {
        super("join", "Demander à rejoindre un joueur après son /rtpv");
        this.requirePermission("varyon.rtp");
        this.playerArg = this.withRequiredArg("joueur", "Nom du joueur (préfixe autorisé)", ArgTypes.PLAYER_REF);
        this.addSubCommand(new AcceptSubCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Tu dois être un joueur pour cette commande.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Player joinerPlayer = (Player) context.sender();
        if (joinerPlayer == null) {
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef joinerRef = Universe.get().getPlayer(joinerPlayer.getUuid());
        if (joinerRef == null) {
            context.sendMessage(Message.raw("Tu dois être connecté.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef targetRef = context.get(playerArg);
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(Message.raw("Joueur introuvable ou hors ligne.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        String targetName = targetRef.getUsername() != null ? targetRef.getUsername() : "";

        if (targetRef.getUuid().equals(joinerRef.getUuid())) {
            context.sendMessage(Message.raw("Tu ne peux pas te rejoindre toi-même.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> targetEntityRef = targetRef.getReference();
        if (targetEntityRef == null || !targetEntityRef.isValid()) {
            context.sendMessage(Message.raw("Le joueur n'est pas dans un monde.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> targetStore = targetEntityRef.getStore();
        Object ext = targetStore.getExternalData();
        if (!(ext instanceof EntityStore targetEntityStore) || targetEntityStore.getWorld() == null) {
            context.sendMessage(Message.raw("Impossible de localiser le joueur.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        World targetWorld = targetEntityStore.getWorld();
        String targetWorldName = targetWorld.getName();

        String joinerWorldName = joinerPlayer.getWorld() != null ? joinerPlayer.getWorld().getName() : "";
        if (!targetWorldName.equals(joinerWorldName)) {
            context.sendMessage(Message.raw("Tu dois être dans le même monde (" + targetWorldName + ").").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
        if (joinMgr == null) {
            context.sendMessage(Message.raw("Système de join indisponible.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        RtpvJoinManager.JoinableEntry entry = joinMgr.getJoinable(targetRef.getUuid(), targetWorldName, zoneConfig);
        if (entry == null) {
            context.sendMessage(Message.raw(
                targetName + " n'est plus rejoignable, (utilise le portail pour te téléporter toi même)."
            ).color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        ZonePermissionsConfig zonePerms = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
        if (!zonePerms.canAccessZone(joinerRef, entry.zoneId())) {
            String required = zonePerms.getPermissionForZone(entry.zoneId());
            context.sendMessage(Message.raw("Tu n'as pas accès à la zone " + entry.zoneId() + ". Permission : " + required).color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        DifficultyZone zone = zoneConfig.getZoneById(entry.zoneId());
        if (zone == null) {
            context.sendMessage(Message.raw("Zone invalide.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
        int baseCost = zone.getTeleportCost();
        int finalCost = (int) Math.ceil(baseCost);
        BigDecimal costBD = BigDecimal.valueOf(finalCost);

        if (rtpvConfig.isEconomyEnabled()) {
            try {
                Economy economy = VaultUnlockedServicesManager.get().economyObj();
                if (economy != null && economy.isEnabled()) {
                    if (!economy.has("Varyon", joinerRef.getUuid(), costBD)) {
                        BigDecimal balance = economy.getBalance("Varyon", joinerRef.getUuid());
                        context.sendMessage(Message.raw(
                            "Coins insuffisants. Coût : " + finalCost + " | Solde : " + balance.intValue()
                        ).color(Color.RED));
                        return CompletableFuture.completedFuture(null);
                    }
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible");
            }
        }

        if (!joinMgr.tryRegisterJoinRequestForCurrentHostRtpv(targetRef.getUuid(), joinerRef.getUuid())) {
            context.sendMessage(Message.raw(
                "Tu as déjà envoyé une demande pour le téléport actuel de ce joueur."
            ).color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        String joinerLabel = joinerRef.getUsername() != null ? joinerRef.getUsername() : "";
        long pendingExp = System.currentTimeMillis() + rtpvConfig.getJoinDurationSeconds() * 1000L;
        joinMgr.addPendingJoin(targetRef.getUuid(), joinerRef.getUuid(), joinerLabel, pendingExp);

        targetWorld.execute(() -> {
            Ref<EntityStore> liveRef = targetRef.getReference();
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
                new JoinRequestConfirmUIPage(targetRef, joinerRef.getUuid(), joinerLabel)
            );
        });

        context.sendMessage(Message.raw("Demande envoyée à " + targetName + " (en attente d'acceptation).").color(Color.GREEN));
        return CompletableFuture.completedFuture(null);
    }

    private static final class AcceptSubCommand extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> joinerArg;

        AcceptSubCommand() {
            super("accept", "Accepter une demande de join");
            this.requirePermission("varyon.rtp");
            this.joinerArg = this.withRequiredArg("demandeur", "Joueur qui demande (préfixe autorisé)", ArgTypes.PLAYER_REF);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.raw("Tu dois être un joueur pour cette commande.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Player accepterPlayer = (Player) context.sender();
            if (accepterPlayer == null) {
                return CompletableFuture.completedFuture(null);
            }

            World accepterWorld = accepterPlayer.getWorld();
            if (accepterWorld == null) {
                context.sendMessage(Message.raw("Tu n'es pas dans un monde.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            PlayerRef accepterRef = Universe.get().getPlayer(accepterPlayer.getUuid());
            if (accepterRef == null) {
                context.sendMessage(Message.raw("Erreur joueur.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            PlayerRef joinerRef = context.get(joinerArg);
            if (joinerRef == null || !joinerRef.isValid()) {
                context.sendMessage(Message.raw("Joueur introuvable ou hors ligne.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
            if (joinMgr == null) {
                context.sendMessage(Message.raw("Système de join indisponible.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            if (joinMgr.findPendingJoinByJoiner(accepterRef.getUuid(), joinerRef.getUuid()) == null) {
                context.sendMessage(Message.raw("Tu n'as aucune demande en attente de ce joueur (ou expirée).").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            if (joinerRef.getUuid().equals(accepterRef.getUuid())) {
                context.sendMessage(Message.raw("Requête invalide.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            accepterWorld.execute(() -> JoinAcceptService.accept(
                accepterRef, joinerRef, context::sendMessage, joinerRef::sendMessage));

            return CompletableFuture.completedFuture(null);
        }
    }
}
