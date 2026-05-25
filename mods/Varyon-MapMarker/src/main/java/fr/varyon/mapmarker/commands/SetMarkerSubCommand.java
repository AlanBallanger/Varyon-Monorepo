package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
        Player sender = context.senderAs(Player.class);
        if (sender == null) {
            context.sendMessage(Message.raw("Erreur : impossible de résoudre le joueur."));
            return;
        }
        plugin.createSharedMarkerFromPlayer(sender, imageArg.get(context), nameArg.get(context));
    }
}
