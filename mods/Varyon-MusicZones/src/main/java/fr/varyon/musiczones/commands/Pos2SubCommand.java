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
import java.util.concurrent.CompletableFuture;

public final class Pos2SubCommand extends MusicZoneAdminCommandBase {

    public Pos2SubCommand() {
        super("pos2", "Mémoriser la seconde extrémité du parallélépipède (ta position)");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return CompletableFuture.completedFuture(null);
        }
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Joueur uniquement."));
            return CompletableFuture.completedFuture(null);
        }
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return CompletableFuture.completedFuture(null);
        }
        return onWorld(context, () -> {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            PlayerRef pr = store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
            Object tcObj = store != null ? store.getComponent(ref, TransformComponent.getComponentType()) : null;
            if (!(tcObj instanceof TransformComponent t)) {
                context.sendMessage(Message.raw("Transform indisponible."));
                return;
            }
            UUID uuid = pr != null ? pr.getUuid() : null;
            if (uuid == null) {
                context.sendMessage(Message.raw("UUID indisponible."));
                return;
            }
            double x = t.getPosition().x;
            double y = t.getPosition().y;
            double z = t.getPosition().z;
            World w = pr != null ? Universe.get().getWorld(pr.getWorldUuid()) : null;
            plugin.setPendingCorner2(w != null ? w.getName() : "", uuid, x, y, z);
            context.sendMessage(Message.raw("pos2 enregistré (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")."));
        });
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}
