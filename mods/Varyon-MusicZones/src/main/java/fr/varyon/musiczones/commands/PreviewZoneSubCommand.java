package fr.varyon.musiczones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.musiczones.MusicZoneVisualizer;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PreviewZoneSubCommand extends MusicZoneAdminCommandBase {

    public PreviewZoneSubCommand() {
        super("preview", "Activer/désactiver l'affichage visuel de la zone musicale courante");
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
        MusicZoneVisualizer visualizer = plugin.getVisualizer();
        return onWorld(context, () -> {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            PlayerRef pr = store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
            if (pr == null) {
                context.sendMessage(Message.raw("Joueur introuvable."));
                return;
            }
            UUID uuid = pr.getUuid();
            boolean enabled = visualizer.togglePreview(uuid);
            if (enabled) {
                context.sendMessage(Message.raw("Preview activé : la zone musicale à ta position s'affiche toutes les 0.5s."));
            } else {
                visualizer.removeVisualForPlayer(pr, uuid);
                context.sendMessage(Message.raw("Preview désactivé."));
            }
        });
    }
}
