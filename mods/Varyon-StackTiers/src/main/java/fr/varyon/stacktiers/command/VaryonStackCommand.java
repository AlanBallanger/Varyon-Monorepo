package fr.varyon.stacktiers.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.stacktiers.PlayerTierStore;
import fr.varyon.stacktiers.StackCategories;
import fr.varyon.stacktiers.research.ResearchManager;
import fr.varyon.stacktiers.research.ResearchTree;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class VaryonStackCommand extends AbstractAsyncCommand {
    private final PlayerTierStore store;

    public VaryonStackCommand(@Nonnull PlayerTierStore store, @Nonnull ResearchManager researchManager) {
        super("varyonstack", "Gérer les paliers de stack par joueur/catégorie");
        this.store = store;
        this.requirePermission("varyon.stacktiers.admin");
        this.addSubCommand(new SetSubCommand(store, researchManager));
        this.addSubCommand(new RemoveSubCommand(store));
        this.addSubCommand(new ClearSubCommand(store));
        this.addSubCommand(new ListSubCommand(store));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        context.sendMessage(Message.raw(
                "Utilisation : /varyonstack <set|remove|clear|list> ...").color(Color.YELLOW));
        return CompletableFuture.completedFuture(null);
    }

    public static class SetSubCommand extends AbstractAsyncCommand {
        private final PlayerTierStore store;
        private final ResearchManager researchManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> categoryArg;
        private final RequiredArg<Integer> tierArg;

        public SetSubCommand(@Nonnull PlayerTierStore store, @Nonnull ResearchManager researchManager) {
            super("set", "Définir le palier de stack d'un joueur pour une catégorie");
            this.store = store;
            this.researchManager = researchManager;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
            this.categoryArg = this.withRequiredArg("category", "Catégorie (wood, rock, ...)", ArgTypes.STRING);
            this.tierArg = this.withRequiredArg("tier", "Palier (1-3)", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            String category = context.get(categoryArg).toLowerCase();
            int tier = context.get(tierArg);

            if (!StackCategories.isValid(category)) {
                context.sendMessage(Message.raw("Catégorie inconnue : " + category
                        + ". Catégories valides : " + String.join(", ", StackCategories.KNOWN))
                        .color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            if (tier < 1 || tier > 3) {
                context.sendMessage(Message.raw("Le palier doit être compris entre 1 et 3.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int previous = store.set(target.getUuid(), category, tier);

            // Synchronise l'arbre de recherche : un don admin marque aussi la chaîne de
            // prérequis correspondante comme complétée (décision actée : les deux systèmes
            // restent cohérents plutôt qu'indépendants).
            List<String> chain = ResearchTree.chainUpTo(category, tier);
            for (String nodeId : chain) {
                researchManager.markCompleted(target.getUuid(), nodeId);
            }

            context.sendMessage(Message.raw(
                    target.getUsername() + " : " + category + " -> palier " + tier
                            + (previous > 0 ? " (était palier " + previous + ")" : "")
                            + (store.isBridgeActive() ? "" : " [ATTENTION: bridge inactif, sans effet ce démarrage]"))
                    .color(store.isBridgeActive() ? Color.GREEN : Color.ORANGE));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class RemoveSubCommand extends AbstractAsyncCommand {
        private final PlayerTierStore store;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> categoryArg;

        public RemoveSubCommand(@Nonnull PlayerTierStore store) {
            super("remove", "Retirer l'override de palier d'un joueur pour une catégorie");
            this.store = store;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
            this.categoryArg = this.withRequiredArg("category", "Catégorie", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            String category = context.get(categoryArg).toLowerCase();

            boolean removed = store.remove(target.getUuid(), category);
            context.sendMessage(removed
                    ? Message.raw("Override retiré pour " + target.getUsername() + " / " + category).color(Color.GREEN)
                    : Message.raw("Aucun override existant pour " + target.getUsername() + " / " + category).color(Color.YELLOW));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ClearSubCommand extends AbstractAsyncCommand {
        private final PlayerTierStore store;
        private final RequiredArg<PlayerRef> playerArg;

        public ClearSubCommand(@Nonnull PlayerTierStore store) {
            super("clear", "Retirer tous les overrides de palier d'un joueur");
            this.store = store;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            Map<String, Integer> existing = store.listForPlayer(target.getUuid());
            for (String category : existing.keySet()) {
                store.remove(target.getUuid(), category);
            }
            context.sendMessage(Message.raw(
                    existing.size() + " override(s) retiré(s) pour " + target.getUsername())
                    .color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ListSubCommand extends AbstractAsyncCommand {
        private final PlayerTierStore store;
        private final RequiredArg<PlayerRef> playerArg;

        public ListSubCommand(@Nonnull PlayerTierStore store) {
            super("list", "Afficher les paliers de stack d'un joueur");
            this.store = store;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            Map<String, Integer> overrides = store.listForPlayer(target.getUuid());

            context.sendMessage(Message.raw("=== Paliers de " + target.getUsername()
                    + (store.isBridgeActive() ? "" : " [bridge inactif]") + " ===").color(Color.ORANGE));
            if (overrides.isEmpty()) {
                context.sendMessage(Message.raw("(aucun override — LuckPerms uniquement)").color(Color.WHITE));
            } else {
                overrides.forEach((category, tier) ->
                        context.sendMessage(Message.raw(category + " : palier " + tier).color(Color.WHITE)));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
