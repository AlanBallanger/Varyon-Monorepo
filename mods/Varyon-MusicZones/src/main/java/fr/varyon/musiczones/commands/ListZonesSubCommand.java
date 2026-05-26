package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
        Ref<EntityStore> _ref = context.senderAsPlayerRef();
        Store<EntityStore> _store = _ref != null ? _ref.getStore() : null;
        PlayerRef _pr = _store != null ? _store.getComponent(_ref, PlayerRef.getComponentType()) : null;
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return;
        }
        World _w = _pr != null ? Universe.get().getWorld(_pr.getWorldUuid()) : null;
        String worldName = _w != null ? _w.getName() : "";
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
