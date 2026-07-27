package com.varyon.varyonui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.varyonui.config.ActuStateConfig;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ActuNotifCommand extends AbstractAsyncCommand {

    private static final Message MESSAGE_PLAYER_NOT_IN_WORLD = Message.translation("server.commands.errors.playerNotInWorld");

    public ActuNotifCommand() {
        super("actunotif", "Active ou desactive l'ouverture automatique des actualites a la connexion");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Cette commande doit etre executee par un joueur"));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            ctx.sendMessage(MESSAGE_PLAYER_NOT_IN_WORLD);
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = playerRef.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            UUID uuid = playerRefComponent != null ? playerRefComponent.getUuid() : null;
            if (uuid == null) {
                ctx.sendMessage(Message.raw("Impossible de determiner votre identifiant joueur"));
                return;
            }

            ActuStateConfig config = ActuStateConfig.getInstance();
            boolean newValue = !config.wantsPopup(uuid);
            config.setWantsPopup(uuid, newValue);
            ctx.sendMessage(Message.raw(newValue
                    ? "Les actualites s'ouvriront automatiquement a la connexion."
                    : "Les actualites ne s'ouvriront plus automatiquement a la connexion."));
        }, world);
    }
}
