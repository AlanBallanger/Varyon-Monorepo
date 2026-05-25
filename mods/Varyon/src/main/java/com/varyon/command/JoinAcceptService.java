package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.rtpv.RtpvJoinManager;
import com.varyon.teleport.FirstSpawnStyleParticleFx;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class JoinAcceptService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private JoinAcceptService() {}

    public static void accept(
        @Nonnull PlayerRef accepterRef,
        @Nonnull PlayerRef joinerRef,
        @Nonnull Consumer<Message> toAccepter,
        @Nonnull Consumer<Message> toJoiner
    ) {
        RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
        if (joinMgr == null) {
            toAccepter.accept(Message.raw("Système de join indisponible.").color(Color.RED));
            return;
        }

        if (joinMgr.findPendingJoinByJoiner(accepterRef.getUuid(), joinerRef.getUuid()) == null) {
            toAccepter.accept(Message.raw("Tu n'as aucune demande en attente de ce joueur (ou expirée).").color(Color.RED));
            return;
        }

        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();

        String joinerDisplay = joinerRef.getUsername() != null ? joinerRef.getUsername() : "";
        String accepterDisplay = accepterRef.getUsername() != null ? accepterRef.getUsername() : "";

        try {
            Ref<EntityStore> accepterEntityRef = accepterRef.getReference();
            if (accepterEntityRef == null || !accepterEntityRef.isValid()) {
                toAccepter.accept(Message.raw("Tu n'es pas dans un monde.").color(Color.RED));
                return;
            }

            Store<EntityStore> accepterStore = accepterEntityRef.getStore();
            Object accepterExt = accepterStore.getExternalData();
            if (!(accepterExt instanceof EntityStore accepterEntityStore) || accepterEntityStore.getWorld() == null) {
                toAccepter.accept(Message.raw("Impossible de localiser le monde.").color(Color.RED));
                return;
            }

            World accepterWorldFromStore = accepterEntityStore.getWorld();
            String accepterWorldName = accepterWorldFromStore.getName();

            Ref<EntityStore> joinerEntityRef = joinerRef.getReference();
            if (joinerEntityRef == null || !joinerEntityRef.isValid()) {
                toAccepter.accept(Message.raw("Le demandeur n'est pas dans un monde.").color(Color.RED));
                return;
            }

            Store<EntityStore> joinerStoreProbe = joinerEntityRef.getStore();
            Object joinerExt = joinerStoreProbe.getExternalData();
            if (!(joinerExt instanceof EntityStore joinerEntityStore) || joinerEntityStore.getWorld() == null) {
                toAccepter.accept(Message.raw("Impossible de localiser le demandeur.").color(Color.RED));
                return;
            }

            String joinerWorldName = joinerEntityStore.getWorld().getName();
            if (!accepterWorldName.equals(joinerWorldName)) {
                toAccepter.accept(Message.raw("Le demandeur n'est pas dans ton monde.").color(Color.RED));
                return;
            }

            RtpvJoinManager.JoinableEntry entry = joinMgr.getJoinable(accepterRef.getUuid(), accepterWorldName, zoneConfig);
            if (entry == null) {
                joinMgr.removePendingJoin(accepterRef.getUuid(), joinerRef.getUuid());
                toAccepter.accept(Message.raw(
                    joinerDisplay + " essaie de te rejoindre mais tu n'es plus rejoignable"
                ).color(Color.RED));
                toJoiner.accept(Message.raw(
                    accepterDisplay + " n'est plus rejoignable, (utilise le portail pour te téléporter toi même)."
                ).color(Color.RED));
                return;
            }

            Player joinerPlayerEntity = joinerStoreProbe.getComponent(joinerEntityRef, Player.getComponentType());
            if (joinerPlayerEntity == null) {
                toAccepter.accept(Message.raw("Joueur demandeur introuvable.").color(Color.RED));
                return;
            }

            ZonePermissionsConfig zonePerms = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
            if (!zonePerms.canAccessZone(joinerPlayerEntity, entry.zoneId())) {
                joinMgr.removePendingJoin(accepterRef.getUuid(), joinerRef.getUuid());
                String required = zonePerms.getPermissionForZone(entry.zoneId());
                toAccepter.accept(Message.raw(
                    joinerDisplay + " n'a plus accès à la zone " + entry.zoneId() + " (" + required + "). Demande annulée."
                ).color(Color.RED));
                return;
            }

            DifficultyZone zone = zoneConfig.getZoneById(entry.zoneId());
            if (zone == null) {
                toAccepter.accept(Message.raw("Zone invalide.").color(Color.RED));
                return;
            }

            int baseCost = zone.getTeleportCost();
            int finalCost = (int) Math.ceil(baseCost);
            BigDecimal costBD = BigDecimal.valueOf(finalCost);

            if (rtpvConfig.isEconomyEnabled()) {
                try {
                    Economy economy = VaultUnlockedServicesManager.get().economyObj();
                    if (economy != null && economy.isEnabled()) {
                        if (!economy.has("Varyon", joinerRef.getUuid(), costBD)) {
                            toAccepter.accept(Message.raw(
                                joinerDisplay + " n'a plus assez de coins pour te rejoindre."
                            ).color(Color.RED));
                            return;
                        }
                        EconomyResponse response = economy.withdraw("Varyon", joinerRef.getUuid(), costBD);
                        if (!response.transactionSuccess()) {
                            LOGGER.at(Level.WARNING).log("Join accept: failed to deduct " + finalCost + " from " + joinerRef.getUuid());
                            toAccepter.accept(Message.raw("Erreur économie: " + response.errorMessage).color(Color.RED));
                            return;
                        }
                    }
                } catch (NoClassDefFoundError e) {
                    LOGGER.at(Level.WARNING).log("Vault non disponible");
                }
            }

            Vector3d pos = new Vector3d(entry.rtpX(), entry.rtpY(), entry.rtpZ());
            Vector3f rot = new Vector3f(entry.rotYaw(), entry.rotPitch(), entry.rotRoll());
            Teleport teleport = Teleport.createForPlayer(accepterWorldFromStore, pos, rot);
            joinerStoreProbe.addComponent(joinerEntityRef, Teleport.getComponentType(), teleport);
            FirstSpawnStyleParticleFx.playAt(accepterWorldFromStore, pos, joinerEntityRef, joinerStoreProbe,
                rtpvConfig.getJoinDurationSeconds());

            joinMgr.removePendingJoin(accepterRef.getUuid(), joinerRef.getUuid());

            String costSuffix = rtpvConfig.isEconomyEnabled() ? " (-" + finalCost + " coins)" : "";
            toAccepter.accept(Message.raw("Tu as accepté " + joinerDisplay + "." + costSuffix).color(Color.GREEN));
            toJoiner.accept(Message.raw(
                accepterDisplay + " a accepté ta demande. Téléportation en zone " + entry.zoneId() + costSuffix
            ).color(Color.GREEN));
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Join accept error: " + e.getMessage());
            toAccepter.accept(Message.raw("Échec de la téléportation.").color(Color.RED));
        }
    }
}
