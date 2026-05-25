package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;

public abstract class OperatorCommandBase extends CommandBase {

    protected OperatorCommandBase(String name, String description) {
        super(name, description);
    }

    public static boolean isMapMarkerOperator(CommandContext context) {
        return context.sender().hasPermission("*") || context.sender().hasPermission("varyon.mapmarker.admin");
    }

    protected boolean requireOperator(CommandContext context) {
        if (isMapMarkerOperator(context)) {
            VaryonMapMarkerPlugin plugin = VaryonMapMarkerPlugin.getInstance();
            if (plugin != null) {
                plugin.debug(
                        "Permission accordée commande=%s expéditeur=%s",
                        context.getCalledCommand().getFullyQualifiedName(),
                        context.sender().getDisplayName());
            }
            return true;
        }
        VaryonMapMarkerPlugin plugin = VaryonMapMarkerPlugin.getInstance();
        if (plugin != null) {
            plugin.debug(
                    "Permission refusée commande=%s expéditeur=%s",
                    context.getCalledCommand().getFullyQualifiedName(),
                    context.sender().getDisplayName());
        }
        context.sendMessage(Message.raw("Tu dois être opérateur ou avoir la permission varyon.mapmarker.admin."));
        return false;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
