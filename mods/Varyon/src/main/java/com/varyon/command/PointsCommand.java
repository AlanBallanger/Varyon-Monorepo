package com.varyon.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.essence.EssenceManager;
import com.varyon.essence.GlobalRewardsManager;
import com.varyon.essence.PlayerEssenceData;
import com.varyon.integration.FactionDepositEcoSync;
import com.varyon.faction.FactionManager;
import com.varyon.VaryonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PointsCommand extends AbstractAsyncCommand {
    private final EssenceManager essenceManager;
    private final FactionManager factionManager;

    public PointsCommand(@Nonnull EssenceManager essenceManager, @Nonnull FactionManager factionManager) {
        super("points", "Afficher vos points de faction et le classement");
        this.essenceManager = essenceManager;
        this.factionManager = factionManager;
        this.addSubCommand(new TopSubCommand(essenceManager));
        this.addSubCommand(new GiveSubCommand(essenceManager));
        this.addSubCommand(new TakeSubCommand(essenceManager));
        this.addSubCommand(new SetMaxSubCommand(essenceManager));
        this.addSubCommand(new DepositSubCommand(essenceManager, factionManager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        CommandSender sender = context.sender();

        if (!(sender instanceof PlayerRef playerRef)) {
            context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        int displayPoints = essenceManager.getEssenceDisplay(playerRef.getUuid());
        int rank = essenceManager.getPlayerRank(playerRef.getUuid());

        context.sendMessage(Message.raw("Points de faction : " + displayPoints).color(Color.YELLOW));
        context.sendMessage(Message.raw("Rang : #" + rank).color(Color.YELLOW));

        return CompletableFuture.completedFuture(null);
    }

    public static class TopSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;

        public TopSubCommand(@Nonnull EssenceManager essenceManager) {
            super("top", "Classement des points de faction");
            this.essenceManager = essenceManager;
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            List<PlayerEssenceData> topPlayers = essenceManager.getTopPlayers(10);

            context.sendMessage(Message.raw("=== Classement — points de faction ===").color(Color.ORANGE));

            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerEssenceData data = topPlayers.get(i);
                String position = "#" + (i + 1);
                context.sendMessage(Message.raw(position + " " + data.name() + " — " + (int) Math.floor(data.essence()) + " points").color(Color.WHITE));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    public static class GiveSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public GiveSubCommand(@Nonnull EssenceManager essenceManager) {
            super("give", "Donner des points de faction à un joueur");
            this.essenceManager = essenceManager;
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("player", "Nom du joueur", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Montant", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Le montant doit être > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(target.getUuid(), target.getUsername(), amount);
            context.sendMessage(Message.raw("+" + amount + " points de faction à " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class TakeSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public TakeSubCommand(@Nonnull EssenceManager essenceManager) {
            super("take", "Retirer des points de faction à un joueur");
            this.essenceManager = essenceManager;
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("player", "Nom du joueur", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Montant", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Le montant doit être > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(target.getUuid(), target.getUsername(), -amount);
            context.sendMessage(Message.raw("-" + amount + " points de faction de " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class SetMaxSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public SetMaxSubCommand(@Nonnull EssenceManager essenceManager) {
            super("setmax", "Définir le stock de points de faction d'un joueur (hors plafond 1000 par défaut)");
            this.essenceManager = essenceManager;
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("player", "Nom du joueur", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Montant", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);

            if (amount < 0) {
                context.sendMessage(Message.raw("Le montant doit être >= 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.setEssenceUncapped(target.getUuid(), target.getUsername(), amount);
            context.sendMessage(Message.raw("Points de faction de " + target.getUsername() + " définis à " + amount).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class DepositSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final FactionManager factionManager;
        private final RequiredArg<Integer> amountArg;

        public DepositSubCommand(@Nonnull EssenceManager essenceManager, @Nonnull FactionManager factionManager) {
            super("deposit", "Déposer des points de faction pour votre faction");
            this.essenceManager = essenceManager;
            this.factionManager = factionManager;
            this.requirePermission("varyon.deposit");
            this.amountArg = this.withRequiredArg("amount", "Montant", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            if (!(sender instanceof PlayerRef playerRef)) {
                context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            FactionManager.Faction faction = factionManager.getFaction(playerRef);
            if (faction == null) {
                context.sendMessage(Message.raw("Vous n'appartenez à aucune faction (group.fracture ou group.noyau requis).").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Le montant doit être > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int currentPoints = essenceManager.getEssenceDisplay(playerRef.getUuid());
            if (currentPoints < amount) {
                context.sendMessage(Message.raw("Vous n'avez que " + currentPoints + " points de faction.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int contribution = amount * faction.getBalanceMultiplier();
            if (!essenceManager.canApplyGuildContribution(contribution)) {
                context.sendMessage(Message.raw("Impossible de déposer : la jauge est verrouillée à cet extrême (contribution de votre faction refusée).").color(Color.RED));
                return CompletableFuture.completedFuture(null);
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
            context.sendMessage(Message.raw("Déposé " + amount + " points de faction dans " + faction.getDisplayName()).color(Color.GREEN));
            context.sendMessage(Message.raw(FactionManager.factionPointsAfterDepositLine(newBalance, gaugeMax, faction)).color(Color.YELLOW));

            VaryonPlugin plugin = VaryonPlugin.getInstance();
            if (plugin != null && plugin.getHudManager() != null) {
                plugin.getHudManager().broadcastBalanceUpdate();
            }

            return CompletableFuture.completedFuture(null);
        }
    }
}
