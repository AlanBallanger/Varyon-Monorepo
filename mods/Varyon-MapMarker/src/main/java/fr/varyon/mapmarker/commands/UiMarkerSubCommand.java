package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import fr.varyon.mapmarker.gui.MarkerListPage;
import javax.annotation.Nonnull;

public final class UiMarkerSubCommand extends AbstractPlayerCommand {

    public UiMarkerSubCommand() {
        super("list", "Ouvrir l'interface graphique de gestion des marqueurs");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        if (!OperatorCommandBase.isMapMarkerOperator(context)) {
            context.sendMessage(Message.raw("Tu dois être opérateur ou avoir la permission varyon.mapmarker.admin."));
            return;
        }
        VaryonMapMarkerPlugin plugin = VaryonMapMarkerPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Erreur : le plugin n'est pas chargé."));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Erreur : impossible d'ouvrir l'interface."));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new MarkerListPage(playerRef, plugin, world));
    }
}
