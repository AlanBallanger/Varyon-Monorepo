package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;

public final class RemoveZoneSubCommand extends MusicZoneAdminCommandBase {

    private final RequiredArg<String> idArg =
            withRequiredArg("id", "Identifiant de la zone", (ArgumentType<String>) ArgTypes.STRING);

    public RemoveZoneSubCommand() {
        super("remove", "Supprimer une zone dans le monde courant");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return;
        }
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Joueur uniquement."));
            return;
        }
        Player p = context.senderAs(Player.class);
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return;
        }
        String zoneId = idArg.get(context).trim();
        String worldName = p.getWorld().getName();
        if (plugin.getRepository().remove(worldName, zoneId)) {
            plugin.getRepository().save();
            try {
                plugin.rebuildAssetPack();
            } catch (Exception e) {
                context.sendMessage(Message.raw("Supprimé mais pack audio : " + e.getMessage()));
                return;
            }
            context.sendMessage(Message.raw("Zone « " + zoneId + " » supprimée."));
        } else {
            context.sendMessage(Message.raw("Aucune zone « " + zoneId + " » dans " + worldName + "."));
        }
    }
}
