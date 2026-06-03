package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.mapmarker.MarkerEntry;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import fr.varyon.mapmarker.gui.MarkerEditorPage;
import java.util.List;
import javax.annotation.Nonnull;

public final class EditMarkerSubCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom du marqueur à éditer (casse ignorée)", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public EditMarkerSubCommand() {
        super("edit", "Ouvrir l'éditeur graphique pour un marqueur existant");
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
        String name = nameArg.get(context);
        List<MarkerEntry> matches = plugin.findMarkersInWorldByName(world, name);
        if (matches.isEmpty()) {
            context.sendMessage(Message.raw("Aucun marqueur trouvé avec ce nom : " + name));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Erreur : impossible d'ouvrir l'interface."));
            return;
        }
        if (matches.size() > 1) {
            context.sendMessage(Message.raw(
                matches.size() + " marqueurs portent ce nom — ouverture du premier (id : " + matches.get(0).id() + ")."));
        }
        player.getPageManager().openCustomPage(ref, store,
            new MarkerEditorPage(playerRef, plugin, world, matches.get(0).id()));
    }
}
