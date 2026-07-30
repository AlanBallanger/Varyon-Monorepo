package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.arena.ArenaManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class ArenaCommand extends AbstractAsyncCommand {
    private final ArenaManager arenaManager;

    public ArenaCommand(ArenaManager arenaManager) {
        super("arena", "Gérer les arènes (zones non-PvP)");
        this.arenaManager = arenaManager;
        this.requirePermission("varyon.admin");
        this.addSubCommand(new AddSubCommand(arenaManager));
        this.addSubCommand(new RemoveSubCommand(arenaManager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        context.sendMessage(Message.raw("Usage: /varyon arena <add|remove> [nom]").color(Color.WHITE));
        return CompletableFuture.completedFuture(null);
    }

    private static PlayerRef requirePlayer(CommandContext context) {
        CommandSender sender = context.sender();
        if (!(sender instanceof PlayerRef playerRef)) {
            context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return null;
        }
        return playerRef;
    }

    public static class AddSubCommand extends AbstractAsyncCommand {
        private final ArenaManager arenaManager;
        private final RequiredArg<String> nameArg =
                this.withRequiredArg("nom", "Nom de l'arène", ArgTypes.STRING);

        public AddSubCommand(ArenaManager arenaManager) {
            super("add", "Créer une arène (zone non-PvP, rayon 50) à votre position");
            this.arenaManager = arenaManager;
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef playerRef = requirePlayer(context);
            if (playerRef == null) {
                return CompletableFuture.completedFuture(null);
            }

            String name = context.get(nameArg).trim();
            if (name.isEmpty()) {
                context.sendMessage(Message.raw("Nom d'arène invalide.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = context.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) {
                context.sendMessage(Message.raw("Joueur non connecté au monde.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            if (world == null) {
                context.sendMessage(Message.raw("Monde indisponible.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            org.joml.Vector3d position = playerRef.getTransform().getPosition();
            boolean added = arenaManager.addArena(name, world.getName(), position.x, position.y, position.z);
            if (!added) {
                context.sendMessage(Message.raw("Une arène nommée « " + name + " » existe déjà.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            context.sendMessage(Message.raw(
                "Arène « " + name + " » créée (rayon " + (int) ArenaManager.DEFAULT_RADIUS + " blocs, zone non-PvP)."
            ).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class RemoveSubCommand extends AbstractAsyncCommand {
        private final ArenaManager arenaManager;

        public RemoveSubCommand(ArenaManager arenaManager) {
            super("remove", "Supprimer une arène (par nom, ou l'arène courante si aucun nom)");
            this.arenaManager = arenaManager;
            this.requirePermission("varyon.admin");
            this.addUsageVariant(new NamedVariant(arenaManager));
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef playerRef = requirePlayer(context);
            if (playerRef == null) {
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = context.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) {
                context.sendMessage(Message.raw("Joueur non connecté au monde.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            if (world == null) {
                context.sendMessage(Message.raw("Monde indisponible.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            org.joml.Vector3d position = playerRef.getTransform().getPosition();
            ArenaManager.Arena arena = arenaManager.findArenaAt(world.getName(), position.x, position.z);
            if (arena == null) {
                context.sendMessage(Message.raw("Vous n'êtes dans aucune arène. Précisez un nom : /varyon arena remove <nom>").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            arenaManager.removeArena(arena.getName());
            context.sendMessage(Message.raw("Arène « " + arena.getName() + " » supprimée.").color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }

        private class NamedVariant extends CommandBase {
            private final ArenaManager arenaManager;
            private final RequiredArg<String> nameArg =
                    this.withRequiredArg("nom", "Nom de l'arène à supprimer", ArgTypes.STRING);

            NamedVariant(ArenaManager arenaManager) {
                super("Supprimer une arène par son nom");
                this.arenaManager = arenaManager;
            }

            @Override
            protected void executeSync(@Nonnull CommandContext context) {
                String name = context.get(nameArg).trim();
                if (name.isEmpty()) {
                    context.sendMessage(Message.raw("Nom d'arène invalide.").color(Color.RED));
                    return;
                }
                if (arenaManager.removeArena(name)) {
                    context.sendMessage(Message.raw("Arène « " + name + " » supprimée.").color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw("Aucune arène nommée « " + name + " ».").color(Color.RED));
                }
            }
        }
    }
}
