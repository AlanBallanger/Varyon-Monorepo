package com.varyon.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.points.PointsManager;
import com.varyon.points.GlobalRewardsManager;
import com.varyon.points.PlayerPointsData;
import com.varyon.integration.FactionDepositEcoSync;
import com.varyon.faction.FactionManager;
import com.varyon.VaryonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PointsCommand extends AbstractAsyncCommand {
    private final PointsManager pointsManager;
    private final FactionManager factionManager;

    public PointsCommand(@Nonnull PointsManager pointsManager, @Nonnull FactionManager factionManager) {
        super("points", "Afficher vos points de faction et le classement");
        this.pointsManager = pointsManager;
        this.factionManager = factionManager;
        this.addSubCommand(new TopSubCommand(pointsManager));
        this.addSubCommand(new GiveSubCommand(pointsManager));
        this.addSubCommand(new TakeSubCommand(pointsManager));
        this.addSubCommand(new SetMaxSubCommand(pointsManager));
        this.addSubCommand(new DepositSubCommand(pointsManager, factionManager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        CommandSender sender = context.sender();

        if (!(sender instanceof PlayerRef playerRef)) {
            context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        int displayPoints = pointsManager.getPointsDisplay(playerRef.getUuid());
        int rank = pointsManager.getPlayerRank(playerRef.getUuid());

        context.sendMessage(Message.raw("Points de faction : " + displayPoints).color(Color.YELLOW));
        context.sendMessage(Message.raw("Rang : #" + rank).color(Color.YELLOW));

        return CompletableFuture.completedFuture(null);
    }

    public static class TopSubCommand extends AbstractAsyncCommand {
        private final PointsManager pointsManager;

        public TopSubCommand(@Nonnull PointsManager pointsManager) {
            super("top", "Classement des points de faction");
            this.pointsManager = pointsManager;
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            List<PlayerPointsData> topPlayers = pointsManager.getTopPlayers(10);

            context.sendMessage(Message.raw("=== Classement — points de faction ===").color(Color.ORANGE));

            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerPointsData data = topPlayers.get(i);
                String position = "#" + (i + 1);
                context.sendMessage(Message.raw(position + " " + data.name() + " — " + (int) Math.floor(data.points()) + " points").color(Color.WHITE));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    public static class GiveSubCommand extends AbstractAsyncCommand {
        private final PointsManager pointsManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public GiveSubCommand(@Nonnull PointsManager pointsManager) {
            super("give", "Donner des points de faction à un joueur");
            this.pointsManager = pointsManager;
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

            pointsManager.addPoints(target.getUuid(), target.getUsername(), amount);
            context.sendMessage(Message.raw("+" + amount + " points de faction à " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class TakeSubCommand extends AbstractAsyncCommand {
        private final PointsManager pointsManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public TakeSubCommand(@Nonnull PointsManager pointsManager) {
            super("take", "Retirer des points de faction à un joueur");
            this.pointsManager = pointsManager;
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

            pointsManager.addPoints(target.getUuid(), target.getUsername(), -amount);
            context.sendMessage(Message.raw("-" + amount + " points de faction de " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class SetMaxSubCommand extends AbstractAsyncCommand {
        private final PointsManager pointsManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public SetMaxSubCommand(@Nonnull PointsManager pointsManager) {
            super("setmax", "Définir le stock de points de faction d'un joueur (hors plafond 1000 par défaut)");
            this.pointsManager = pointsManager;
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

            pointsManager.setPointsUncapped(target.getUuid(), target.getUsername(), amount);
            context.sendMessage(Message.raw("Points de faction de " + target.getUsername() + " définis à " + amount).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class DepositSubCommand extends AbstractAsyncCommand {
        private final PointsManager pointsManager;
        private final FactionManager factionManager;
        private final RequiredArg<Integer> amountArg;

        public DepositSubCommand(@Nonnull PointsManager pointsManager, @Nonnull FactionManager factionManager) {
            super("deposit", "Déposer des points de faction pour votre faction");
            this.pointsManager = pointsManager;
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

            int currentPoints = pointsManager.getPointsDisplay(playerRef.getUuid());
            if (currentPoints < amount) {
                context.sendMessage(Message.raw("Vous n'avez que " + currentPoints + " points de faction.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int contribution = amount * faction.getBalanceMultiplier();
            if (!pointsManager.canApplyGuildContribution(contribution)) {
                context.sendMessage(Message.raw("Impossible de déposer : la jauge est verrouillée à cet extrême (contribution de votre faction refusée).").color(Color.RED));
                return CompletableFuture.completedFuture(null);
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
