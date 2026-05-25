package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import javax.annotation.Nonnull;

public final class ImportMarkersSubCommand extends OperatorCommandBase {

    public ImportMarkersSubCommand() {
        super("import", "Réimporter tous les marqueurs depuis markers.db");
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
        if (plugin.importSavedMarkers()) {
            context.sendMessage(Message.raw("Les marqueurs sauvegardés ont été rechargés."));
            return;
        }
        context.sendMessage(Message.raw("Échec du rechargement des marqueurs sauvegardés."));
    }
}
