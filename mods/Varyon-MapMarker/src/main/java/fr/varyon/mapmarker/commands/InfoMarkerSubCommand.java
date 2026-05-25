package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import fr.varyon.mapmarker.MarkerEntry;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import java.util.List;
import javax.annotation.Nonnull;

public final class InfoMarkerSubCommand extends OperatorCommandBase {

    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom du marqueur sur la carte (casse ignorée)", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public InfoMarkerSubCommand() {
        super("info", "Afficher les marqueurs enregistrés avec ce nom dans le monde courant");
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
        Player sender = context.senderAs(Player.class);
        if (sender == null) {
            context.sendMessage(Message.raw("Erreur : impossible de résoudre le joueur."));
            return;
        }
        World world = sender.getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("Erreur : tu n'es dans aucun monde actif."));
            return;
        }
        String markerName = nameArg.get(context);
        List<MarkerEntry> matches = plugin.findMarkersInWorldByName(world, markerName);
        if (matches.isEmpty()) {
            context.sendMessage(Message.raw("Aucun marqueur trouvé avec ce nom dans ce monde : " + markerName));
            return;
        }
        context.sendMessage(Message.raw("Marqueurs trouvés : " + matches.size()));
        for (MarkerEntry e : matches) {
            context.sendMessage(Message.raw(
                    "— « "
                            + e.markerName()
                            + " » | monde "
                            + e.worldName()
                            + " | X "
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
