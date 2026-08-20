package fr.varyon.stacktiers.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.stacktiers.StackCategories;
import fr.varyon.stacktiers.research.ResearchManager;
import fr.varyon.stacktiers.ui.ResearchTreeUI;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * research-state.json (via ResearchManager) est la seule source de vérité pour les paliers de
 * stack — cette commande ne fait qu'appeler ResearchManager.setTier/removeCategory/listTiers,
 * il n'y a plus de stockage séparé pour les overrides admin.
 */
public class VaryonStackCommand extends AbstractAsyncCommand {
    private final ResearchManager researchManager;

    public VaryonStackCommand(@Nonnull ResearchManager researchManager) {
        super("varyonstack", "Gérer les paliers de stack par joueur/catégorie");
        this.researchManager = researchManager;
        this.requirePermission("varyon.stacktiers.admin");
        this.addSubCommand(new SetSubCommand(researchManager));
        this.addSubCommand(new RemoveSubCommand(researchManager));
        this.addSubCommand(new ClearSubCommand(researchManager));
        this.addSubCommand(new ListSubCommand(researchManager));
        this.addSubCommand(new ViewSubCommand(researchManager));
    }

    /** Sans sous-commande, ouvre l'arbre de recherche — même comportement que /varyonresearch. */
    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        PlayerRef playerRef = resolveSender(context);
        if (playerRef != null) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                ((EntityStore) store.getExternalData()).getWorld().execute(() -> {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        player.getPageManager().openCustomPage(ref, store, new ResearchTreeUI(playerRef, researchManager));
                    }
                });
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private static PlayerRef resolveSender(CommandContext context) {
        if (!context.isPlayer() || context.sender() == null) {
            return null;
        }
        UUID uuid = context.sender().getUuid();
        if (uuid == null) {
            return null;
        }
        Universe universe = Universe.get();
        return universe != null ? universe.getPlayer(uuid) : null;
    }

    public static class SetSubCommand extends AbstractAsyncCommand {
        private final ResearchManager researchManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> categoryArg;
        private final RequiredArg<Integer> tierArg;

        public SetSubCommand(@Nonnull ResearchManager researchManager) {
            super("set", "Définir le palier de stack d'un joueur pour une catégorie");
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

            int previous = researchManager.setTier(target.getUuid(), category, tier);

            context.sendMessage(Message.raw(
                    target.getUsername() + " : " + category + " -> palier " + tier
                            + (previous > 0 ? " (était palier " + previous + ")" : "")
                            + (researchManager.isBridgeActive() ? "" : " [ATTENTION: bridge inactif, sans effet ce démarrage]"))
                    .color(researchManager.isBridgeActive() ? Color.GREEN : Color.ORANGE));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class RemoveSubCommand extends AbstractAsyncCommand {
        private final ResearchManager researchManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> categoryArg;

        public RemoveSubCommand(@Nonnull ResearchManager researchManager) {
            super("remove", "Retirer le palier d'un joueur pour une catégorie");
            this.researchManager = researchManager;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
            this.categoryArg = this.withRequiredArg("category", "Catégorie", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            String category = context.get(categoryArg).toLowerCase();

            boolean removed = researchManager.removeCategory(target.getUuid(), category);
            context.sendMessage(removed
                    ? Message.raw("Palier retiré pour " + target.getUsername() + " / " + category).color(Color.GREEN)
                    : Message.raw("Aucun palier existant pour " + target.getUsername() + " / " + category).color(Color.YELLOW));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ClearSubCommand extends AbstractAsyncCommand {
        private final ResearchManager researchManager;
        private final RequiredArg<PlayerRef> playerArg;

        public ClearSubCommand(@Nonnull ResearchManager researchManager) {
            super("clear", "Retirer tous les paliers de stack d'un joueur");
            this.researchManager = researchManager;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            Map<String, Integer> existing = researchManager.listTiers(target.getUuid());
            int count = 0;
            for (String category : existing.keySet()) {
                if (researchManager.removeCategory(target.getUuid(), category)) {
                    count++;
                }
            }
            context.sendMessage(Message.raw(
                    count + " palier(s) retiré(s) pour " + target.getUsername())
                    .color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Ouvre l'arbre de recherche d'un autre joueur pour l'admin qui exécute la commande.
     * ResearchTreeUI détecte automatiquement la permission admin de l'exécutant (pas de la
     * cible) et bascule en mode "un clic = complété instantanément" (voir ResearchTreeUI).
     */
    public static class ViewSubCommand extends AbstractAsyncCommand {
        private final ResearchManager researchManager;
        private final RequiredArg<PlayerRef> playerArg;

        public ViewSubCommand(@Nonnull ResearchManager researchManager) {
            super("view", "Voir (et éditer en un clic) l'arbre de recherche d'un joueur");
            this.researchManager = researchManager;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            PlayerRef viewer = resolveSender(context);
            if (viewer == null) {
                context.sendMessage(Message.raw("Cette commande doit être exécutée par un joueur.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            Ref<EntityStore> ref = viewer.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                ((EntityStore) store.getExternalData()).getWorld().execute(() -> {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        player.getPageManager().openCustomPage(ref, store,
                                new ResearchTreeUI(viewer, target.getUuid(), researchManager));
                    }
                });
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ListSubCommand extends AbstractAsyncCommand {
        private final ResearchManager researchManager;
        private final RequiredArg<PlayerRef> playerArg;

        public ListSubCommand(@Nonnull ResearchManager researchManager) {
            super("list", "Afficher les paliers de stack d'un joueur");
            this.researchManager = researchManager;
            this.requirePermission("varyon.stacktiers.admin");
            this.playerArg = this.withRequiredArg("player", "Joueur", ArgTypes.PLAYER_REF);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            Map<String, Integer> tiers = researchManager.listTiers(target.getUuid());

            context.sendMessage(Message.raw("=== Paliers de " + target.getUsername()
                    + (researchManager.isBridgeActive() ? "" : " [bridge inactif]") + " ===").color(Color.ORANGE));
            if (tiers.isEmpty()) {
                context.sendMessage(Message.raw("(aucun palier)").color(Color.WHITE));
            } else {
                tiers.forEach((category, tier) ->
                        context.sendMessage(Message.raw(category + " : palier " + tier).color(Color.WHITE)));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
