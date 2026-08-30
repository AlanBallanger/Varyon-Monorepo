package fr.varyon.readablebooks.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.readablebooks.BookManager;
import fr.varyon.readablebooks.data.BookEntry;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class BookCommand extends AbstractCommandCollection {

    private static final Color COLOR_OK = new Color(85, 255, 85);
    private static final Color COLOR_WARN = new Color(255, 170, 0);
    private static final Color COLOR_ERROR = new Color(255, 85, 85);
    private static final Color COLOR_INFO = new Color(170, 170, 170);
    private static final Color COLOR_TITLE = new Color(255, 255, 85);

    public static final String PERMISSION = "varyon.readablebooks.admin";

    public BookCommand() {
        super("vbook", "Gérer les livres lisibles");
        requireNoPermission();
        this.addAliases(new String[]{"varyonbook"});
        this.addSubCommand(new EditSubCommand());
        this.addSubCommand(new ListSubCommand());
        this.addSubCommand(new ReloadSubCommand());
    }


    static boolean perm(@Nonnull CommandContext context) {
        if (context.sender() instanceof ConsoleSender) {
            return true;
        }
        return context.sender().hasPermission("*") || context.sender().hasPermission(PERMISSION);
    }

    private static final class EditSubCommand extends AbstractAsyncCommand {
        EditSubCommand() {
            super("edit", "Active/désactive ton mode édition des livres");
            requireNoPermission();
        }

        @NonNullDecl
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!perm(ctx)) {
                ctx.sendMessage(Message.raw("Permission refusée.").color(COLOR_ERROR));
                return CompletableFuture.completedFuture(null);
            }

            if (!(ctx.sender() instanceof PlayerRef playerRef)) {
                ctx.sendMessage(Message.raw("Commande réservée aux joueurs.").color(COLOR_ERROR));
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                ctx.sendMessage(Message.raw("Tu n'es pas dans un monde.").color(COLOR_ERROR));
                return CompletableFuture.completedFuture(null);
            }

            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            return CompletableFuture.runAsync(() -> {
                UUID playerUuid = playerRef.getUuid();
                boolean nowEditing = BookManager.getInstance().toggleEditMode(playerUuid);
                if (nowEditing) {
                    ctx.sendMessage(Message.raw("Mode édition ACTIVÉ — interagis avec un livre pour l'éditer.")
                        .color(COLOR_OK));
                } else {
                    ctx.sendMessage(Message.raw("Mode édition DÉSACTIVÉ — tu liras les livres normalement.")
                        .color(COLOR_WARN));
                }
            }, world);
        }
    }

    private static final class ListSubCommand extends AbstractAsyncCommand {
        ListSubCommand() {
            super("list", "Liste les livres configurés");
            requireNoPermission();
        }

        @NonNullDecl
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!perm(ctx)) {
                ctx.sendMessage(Message.raw("Permission refusée.").color(COLOR_ERROR));
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                List<BookEntry> books = BookManager.getInstance().getAllBooks();
                if (books.isEmpty()) {
                    ctx.sendMessage(Message.raw("Aucun livre configuré.").color(COLOR_WARN));
                    return;
                }

                ctx.sendMessage(Message.raw("Livres configurés (" + books.size() + ") :")
                    .color(COLOR_TITLE));
                books.forEach(entry -> {
                    String label = entry.title().isBlank() ? "(sans titre)" : entry.title();
                    ctx.sendMessage(Message.raw(
                        "  " + label + "  —  " + entry.dimension()
                            + " " + entry.x() + ", " + entry.y() + ", " + entry.z()
                    ).color(COLOR_INFO));
                });
            });
        }
    }

    private static final class ReloadSubCommand extends AbstractAsyncCommand {
        ReloadSubCommand() {
            super("reload", "Recharge les livres depuis le disque");
            requireNoPermission();
        }

        @NonNullDecl
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!perm(ctx)) {
                ctx.sendMessage(Message.raw("Permission refusée.").color(COLOR_ERROR));
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                BookManager.getInstance().reload();
                ctx.sendMessage(Message.raw(
                    "Livres rechargés (" + BookManager.getInstance().count() + ").")
                    .color(COLOR_OK));
            });
        }
    }
}
