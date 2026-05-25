package com.varyon.deposit;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.varyon.VaryonPlugin;
import com.varyon.integration.FactionDepositEcoSync;
import com.varyon.essence.EssenceManager;
import com.varyon.essence.GlobalRewardsManager;
import com.varyon.faction.FactionManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.logging.Level;

public class DepositUIManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final EssenceManager essenceManager;
    private final FactionManager factionManager;

    public DepositUIManager(@Nonnull EssenceManager essenceManager, @Nonnull FactionManager factionManager) {
        this.essenceManager = essenceManager;
        this.factionManager = factionManager;
    }

    public void openDepositUI(@Nonnull Player player) {
        try {
            PlayerRef playerRef = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(player.getUuid());
            if (playerRef == null) {
                LOGGER.at(Level.WARNING).log("PlayerRef is null for player");
                return;
            }

            FactionManager.Faction faction = factionManager.getFaction(playerRef);
            if (faction == null) {
                player.sendMessage(Message.raw("Vous n'appartenez à aucune faction.").color(Color.RED));
                return;
            }

            int amount = essenceManager.getEssenceDisplay(playerRef.getUuid());
            if (amount <= 0) {
                player.sendMessage(Message.raw("Vous n'avez pas de points de faction à déposer.").color(Color.YELLOW));
                return;
            }

            int contribution = amount * faction.getBalanceMultiplier();
            if (!essenceManager.canApplyGuildContribution(contribution)) {
                player.sendMessage(Message.raw("Impossible de déposer : la jauge est verrouillée à cet extrême (contribution de votre faction refusée).").color(Color.RED));
                return;
            }

            essenceManager.addEssence(playerRef.getUuid(), playerRef.getUsername(), -amount);

            GlobalRewardsManager rewardsManager = VaryonPlugin.getStaticGlobalRewardsManager();
            if (rewardsManager != null) {
                rewardsManager.recordDeposit(playerRef.getUuid(), faction, amount);
            }

            essenceManager.addToGlobalBalance(contribution);

            FactionDepositEcoSync.applyEcoFactionTokenForDeposit(playerRef, amount);

            int newBalance = essenceManager.getGlobalBalance();
            int gaugeMax = essenceManager.getGuildGaugeAbsMax();
            player.sendMessage(Message.raw("Déposé " + amount + " points de faction dans " + faction.getDisplayName()).color(Color.GREEN));
            player.sendMessage(Message.raw(FactionManager.factionPointsAfterDepositLine(newBalance, gaugeMax, faction)).color(Color.YELLOW));

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
            player.sendMessage(Message.raw("Erreur lors du dépôt.").color(Color.RED));
        }
    }
}
