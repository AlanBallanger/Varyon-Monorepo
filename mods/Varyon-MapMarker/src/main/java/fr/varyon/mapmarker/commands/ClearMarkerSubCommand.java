package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import javax.annotation.Nonnull;

public final class ClearMarkerSubCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom du marqueur sur la carte (casse ignorée)", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public ClearMarkerSubCommand() {
        super("clear", "Retirer du monde courant le ou les marqueurs avec ce nom");
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
