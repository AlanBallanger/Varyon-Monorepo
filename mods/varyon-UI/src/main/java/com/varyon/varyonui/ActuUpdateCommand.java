package com.varyon.varyonui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.varyon.varyonui.config.ActuStateConfig;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ActuUpdateCommand extends AbstractAsyncCommand {

    public ActuUpdateCommand() {
        super("actuupdate", "Marque les actualites comme mises a jour pour tous les joueurs");
        this.requirePermission("varyonui.admin");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
        long version = ActuStateConfig.getInstance().bumpVersion();
        ctx.sendMessage(Message.raw("Actualites mises a jour (version " + version
                + "). Les joueurs verront la page actualites a leur prochaine connexion."));
        return CompletableFuture.completedFuture(null);
    }
}
