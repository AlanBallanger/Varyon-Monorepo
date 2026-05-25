package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import fr.varyon.musiczones.MusicZone;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.util.List;

public final class ListZonesSubCommand extends MusicZoneAdminCommandBase {

    public ListZonesSubCommand() {
        super("list", "Lister les zones musicales du monde courant");
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
        String worldName = p.getWorld().getName();
        List<MusicZone> list = plugin.getRepository().zonesForWorld(worldName);
        if (list.isEmpty()) {
            context.sendMessage(Message.raw("Aucune zone dans " + worldName + "."));
            return;
        }
        for (MusicZone z : list) {
            context.sendMessage(Message.raw(
                    "• "
                            + z.getId()
                            + " → "
                            + z.getMusicFileName()
                            + " | "
                            + z.ambienceAssetId()
                            + " | ["
                            + fmt(z.getMinX())
                            + ","
                            + fmt(z.getMinY())
                            + ","
                            + fmt(z.getMinZ())
                            + "]–["
                            + fmt(z.getMaxX())
                            + ","
                            + fmt(z.getMaxY())
                            + ","
                            + fmt(z.getMaxZ())
                            + "]"));
        }
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
