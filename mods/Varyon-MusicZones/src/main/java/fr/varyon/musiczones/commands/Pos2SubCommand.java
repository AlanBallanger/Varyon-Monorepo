package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class Pos2SubCommand extends MusicZoneAdminCommandBase {

    public Pos2SubCommand() {
        super("pos2", "Mémoriser la seconde extrémité du parallélépipède (ta position)");
    }

    @Override
    @SuppressWarnings("removal")
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
        TransformComponent t = p.getTransformComponent();
        if (t == null) {
            context.sendMessage(Message.raw("Transform indisponible."));
            return;
        }
        double x = t.getPosition().getX();
        double y = t.getPosition().getY();
        double z = t.getPosition().getZ();
        UUID uuid = p.getUuid();
        if (uuid == null) {
            context.sendMessage(Message.raw("UUID indisponible."));
            return;
        }
        plugin.setPendingCorner2(p.getWorld().getName(), uuid, x, y, z);
        context.sendMessage(Message.raw("pos2 enregistré (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")."));
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}
