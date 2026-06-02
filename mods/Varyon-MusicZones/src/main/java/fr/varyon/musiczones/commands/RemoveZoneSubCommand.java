package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public final class RemoveZoneSubCommand extends MusicZoneAdminCommandBase {

    private final RequiredArg<String> idArg =
            withRequiredArg("id", "Identifiant de la zone", (ArgumentType<String>) ArgTypes.STRING);

    public RemoveZoneSubCommand() {
        super("remove", "Supprimer une zone dans le monde courant");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return CompletableFuture.completedFuture(null);
        }
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Joueur uniquement."));
            return CompletableFuture.completedFuture(null);
        }
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return CompletableFuture.completedFuture(null);
        }
        String zoneId = idArg.get(context).trim();
        return onWorld(context, () -> {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            PlayerRef pr = store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
            World w = pr != null ? Universe.get().getWorld(pr.getWorldUuid()) : null;
            String worldName = w != null ? w.getName() : "";
            if (plugin.getRepository().remove(worldName, zoneId)) {
                plugin.getRepository().save();
                try {
                    plugin.rebuildAssetPack();
                } catch (Exception e) {
                    context.sendMessage(Message.raw("Supprimé mais pack audio : " + e.getMessage()));
                    return;
                }
                context.sendMessage(Message.raw("Zone « " + zoneId + " » supprimée."));
            } else {
                context.sendMessage(Message.raw("Aucune zone « " + zoneId + " » dans " + worldName + "."));
            }
        });
    }
}
