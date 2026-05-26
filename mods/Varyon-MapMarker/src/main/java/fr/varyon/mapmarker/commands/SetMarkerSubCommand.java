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

public final class SetMarkerSubCommand extends OperatorCommandBase {

    private final RequiredArg<String> imageArg =
            withRequiredArg("image", "Fichier image (extension .png optionnelle, casse ignorée)", (ArgumentType<String>) ArgTypes.STRING);
    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom affiché sur la carte", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public SetMarkerSubCommand() {
        super("set", "Créer un marqueur sur la carte à ta position avec une image PNG");
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
        World _w = Universe.get().getWorld(_pr.getWorldUuid());
        if (_w == null) {
            context.sendMessage(Message.raw("Erreur : tu n'es dans aucun monde actif."));
            return;
        }
        plugin.createSharedMarkerFromPlayer(_pr, _w, imageArg.get(context), nameArg.get(context));
    }
}
