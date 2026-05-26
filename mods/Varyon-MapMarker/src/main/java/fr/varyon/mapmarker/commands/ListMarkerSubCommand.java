package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.mapmarker.MarkerEntry;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import java.util.List;
import javax.annotation.Nonnull;

public final class ListMarkerSubCommand extends OperatorCommandBase {

    public ListMarkerSubCommand() {
        super("list", "Lister les marqueurs du mod dans le monde courant");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        VaryonMapMarkerPlugin plugin = VaryonMapMarkerPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Erreur : le plugin n'est pas chargé."));
            return;
        }
        if (!requireOperator(context)) {
            return;
        }
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Erreur : tu dois être un joueur pour exécuter cette commande."));
            return;
        }
        Ref<EntityStore> _ref = context.senderAsPlayerRef();
        Store<EntityStore> _store = _ref != null ? _ref.getStore() : null;
        PlayerRef _pr = _store != null ? _store.getComponent(_ref, PlayerRef.getComponentType()) : null;
        if (_pr == null) {
            context.sendMessage(Message.raw("Erreur : impossible de résoudre le joueur."));
            return;
        }
        World world = Universe.get().getWorld(_pr.getWorldUuid());
        if (world == null) {
            context.sendMessage(Message.raw("Erreur : tu n'es dans aucun monde actif."));
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
