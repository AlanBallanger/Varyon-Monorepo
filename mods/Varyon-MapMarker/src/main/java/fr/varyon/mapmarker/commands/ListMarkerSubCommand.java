package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.mapmarker.MarkerEntry;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import java.util.List;
import javax.annotation.Nonnull;

public final class ListMarkerSubCommand extends AbstractPlayerCommand {

    public ListMarkerSubCommand() {
        super("list", "Lister les marqueurs du mod dans le monde courant");
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
        List<MarkerEntry> markers = plugin.listMarkersInWorld(world);
        if (markers.isEmpty()) {
            context.sendMessage(Message.raw("Aucun marqueur Varyon-MapMarker dans ce monde (" + world.getName() + ")."));
            return;
        }
        context.sendMessage(Message.raw("Marqueurs (monde « " + world.getName() + " ») : " + markers.size()));
        for (MarkerEntry e : markers) {
            context.sendMessage(Message.raw(
                    "— « "
                            + e.markerName()
                            + " » | X "
                            + String.format("%.1f", e.x())
                            + " Z "
                            + String.format("%.1f", e.z())
                            + " | icône "
                            + e.imageName()
                            + " | par "
                            + e.createdByName()
                            + " | id "
                            + e.id()));
        }
    }
}
