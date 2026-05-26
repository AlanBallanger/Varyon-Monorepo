package fr.varyon.mapmarker.commands;

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
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import javax.annotation.Nonnull;

public final class ClearMarkerSubCommand extends OperatorCommandBase {

    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom du marqueur sur la carte (casse ignorée)", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public ClearMarkerSubCommand() {
        super("clear", "Retirer du monde courant le ou les marqueurs avec ce nom");
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
        String markerName = nameArg.get(context);
        int removed = plugin.clearMarkersByName(world, markerName);
        if (removed == 0) {
            context.sendMessage(Message.raw("Aucun marqueur trouvé avec ce nom : " + markerName));
            return;
        }
        if (removed == 1) {
            context.sendMessage(Message.raw("Marqueur retiré : " + markerName));
            return;
        }
        context.sendMessage(Message.raw(removed + " marqueurs retirés (nom : " + markerName + ")"));
    }
}
