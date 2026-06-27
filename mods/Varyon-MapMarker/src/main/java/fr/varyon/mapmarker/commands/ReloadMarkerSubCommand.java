package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import javax.annotation.Nonnull;

public final class ReloadMarkerSubCommand extends OperatorCommandBase {

    public ReloadMarkerSubCommand() {
        super("reload", "Repousser les PNG de images/ aux clients et réimporter les marqueurs");
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
        if (plugin.reloadMarkerAssets()) {
            context.sendMessage(Message.raw("Marqueurs rechargés avec succès."));
            return;
        }
        context.sendMessage(
                Message.raw("Rechargement échoué ou aucun marqueur à importer."));
    }
}
