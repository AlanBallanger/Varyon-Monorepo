package fr.varyon.musiczones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
        Ref<EntityStore> _ref = context.senderAsPlayerRef();
        Store<EntityStore> _store = _ref != null ? _ref.getStore() : null;
        PlayerRef _pr = _store != null ? _store.getComponent(_ref, PlayerRef.getComponentType()) : null;
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return;
        }
        Object _tcObj = _store != null ? _store.getComponent(_ref, TransformComponent.getComponentType()) : null;
        if (!(_tcObj instanceof TransformComponent t)) {
            context.sendMessage(Message.raw("Transform indisponible."));
            return;
        }
        double x = t.getPosition().x;
        double y = t.getPosition().y;
        double z = t.getPosition().z;
        UUID uuid = _pr != null ? _pr.getUuid() : null;
        if (uuid == null) {
            context.sendMessage(Message.raw("UUID indisponible."));
            return;
        }
        World _w = _pr != null ? Universe.get().getWorld(_pr.getWorldUuid()) : null;
        plugin.setPendingCorner2(_w != null ? _w.getName() : "", uuid, x, y, z);
        context.sendMessage(Message.raw("pos2 enregistré (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")."));
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}
