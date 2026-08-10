package com.varyon.deposit;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.varyon.VaryonPlugin;
import com.varyon.integration.FactionDepositEcoSync;
import com.varyon.points.PointsManager;
import com.varyon.points.GlobalRewardsManager;
import com.varyon.faction.FactionManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.logging.Level;

public class DepositUIManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final PointsManager pointsManager;
    private final FactionManager factionManager;

    public DepositUIManager(@Nonnull PointsManager pointsManager, @Nonnull FactionManager factionManager) {
        this.pointsManager = pointsManager;
        this.factionManager = factionManager;
    }

    public void openDepositUI(@Nonnull Player player) {
        PlayerRef playerRef = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(player.getUuid());
        if (playerRef == null) {
            LOGGER.at(Level.WARNING).log("PlayerRef is null for player");
            return;
        }
        try {
            FactionManager.Faction faction = factionManager.getFaction(playerRef);
            if (faction == null) {
                playerRef.sendMessage(Message.raw("Vous n'appartenez à aucune faction.").color(Color.RED));
                return;
            }

            int amount = pointsManager.getPointsDisplay(playerRef.getUuid());
            if (amount <= 0) {
                playerRef.sendMessage(Message.raw("Vous n'avez pas de points de faction à déposer.").color(Color.YELLOW));
                return;
            }

            int contribution = amount * faction.getBalanceMultiplier();
            if (!pointsManager.canApplyGuildContribution(contribution)) {
                playerRef.sendMessage(Message.raw("Impossible de déposer : la jauge est verrouillée à cet extrême (contribution de votre faction refusée).").color(Color.RED));
                return;
            }

            pointsManager.addPoints(playerRef.getUuid(), playerRef.getUsername(), -amount);

            GlobalRewardsManager rewardsManager = VaryonPlugin.getStaticGlobalRewardsManager();
            if (rewardsManager != null) {
                rewardsManager.recordDeposit(playerRef.getUuid(), faction, amount);
            }

            pointsManager.addToGlobalBalance(contribution);

            FactionDepositEcoSync.applyEcoFactionTokenForDeposit(playerRef, amount);

            int newBalance = pointsManager.getGlobalBalance();
            int gaugeMax = pointsManager.getGuildGaugeAbsMax();
            playerRef.sendMessage(Message.raw("Déposé " + amount + " points de faction dans " + faction.getDisplayName()).color(Color.GREEN));
            playerRef.sendMessage(Message.raw(FactionManager.factionPointsAfterDepositLine(newBalance, gaugeMax, faction)).color(Color.YELLOW));

            try {
                EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        Message.raw("Vous avez déposé " + amount + " points de faction"),
                        Message.raw(faction.getDisplayName()),
                        true);
            } catch (Exception ignored) {
            }

            VaryonPlugin plugin = VaryonPlugin.getInstance();
            if (plugin != null && plugin.getHudManager() != null) {
                plugin.getHudManager().broadcastBalanceUpdate();
            }

        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to execute deposit: " + e.getMessage());
            playerRef.sendMessage(Message.raw("Erreur lors du dépôt.").color(Color.RED));
        }
    }
}
