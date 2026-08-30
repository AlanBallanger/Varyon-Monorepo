package fr.varyon.musiczones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public abstract class MusicZoneAdminCommandBase extends AbstractAsyncCommand {

    protected MusicZoneAdminCommandBase(String name, String description) {
        super(name, description);
        requireNoPermission();
    }

    protected static boolean isMusicZoneAdmin(CommandContext context) {
        return context.sender().hasPermission("*") || context.sender().hasPermission("varyon.musiczones.admin");
    }

    protected boolean requireMusicZoneAdmin(CommandContext context) {
        if (isMusicZoneAdmin(context)) {
            return true;
        }
        context.sendMessage(Message.raw("Op ou permission varyon.musiczones.admin requise."));
        return false;
    }

    protected final CompletableFuture<Void> onWorld(CommandContext context, Runnable action) {
        Ref<EntityStore> ref = context.senderAsPlayerRef();
        Store<EntityStore> store = ref != null ? ref.getStore() : null;
        EntityStore entityStore = store != null ? store.getExternalData() : null;
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (world == null) {
            context.sendMessage(Message.raw("Monde indisponible."));
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(action, world);
    }
}
