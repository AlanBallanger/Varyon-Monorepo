package com.varyon.varyonui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.varyonui.ui.SimpleUIPage;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class UICommand extends AbstractAsyncCommand {

    private static final Message MESSAGE_PLAYER_NOT_IN_WORLD = Message.translation("server.commands.errors.playerNotInWorld");
    private final String targetTab;

    public UICommand(String name, String description, String targetTab, String... aliases) {
        super(name, description);
        this.targetTab = targetTab;
        if (aliases.length > 0) {
            this.addAliases(aliases);
        }
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

        final String tab = this.targetTab;
        final boolean isAdmin = ctx.sender().hasPermission("varyonui.admin");
        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent == null) return;

            Player playerComponent = store.getComponent(playerRef, Player.getComponentType());
            if (playerComponent == null) return;

            SimpleUIPage uiPage = new SimpleUIPage(playerRefComponent, tab, isAdmin);
            playerComponent.getPageManager().openCustomPage(playerRef, store, uiPage);
            playerComponent.getHudManager().getCustomHuds().values().forEach(
                com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud::show
            );
        }, world);
    }
}
