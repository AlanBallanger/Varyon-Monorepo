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

public final class SetMarkerSubCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> imageArg =
            withRequiredArg("image", "Fichier image (extension .png optionnelle, casse ignorée)", (ArgumentType<String>) ArgTypes.STRING);
    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom affiché sur la carte", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public SetMarkerSubCommand() {
        super("set", "Créer un marqueur sur la carte à ta position avec une image PNG");
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
        plugin.createSharedMarkerFromPlayer(playerRef, world, imageArg.get(context), nameArg.get(context));
    }
}
