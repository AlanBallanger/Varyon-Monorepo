package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.arguments.types.Coord;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.exceptions.GeneralCommandException;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.mapmarker.MarkerEntry;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import java.util.List;
import javax.annotation.Nonnull;

public final class TeleportMarkerSubCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg =
            withRequiredArg("name", "Nom du marqueur (casse ignorée)", (ArgumentType<String>) ArgTypes.GREEDY_STRING);

    public TeleportMarkerSubCommand() {
        super("tp", "Te téléporter au X/Z d’un marqueur du monde courant (hauteur adaptée au terrain)");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        if (!OperatorCommandBase.isMapMarkerOperator(context)) {
            context.sendMessage(Message.raw("Tu dois être opérateur ou avoir la permission varyon.mapmarker.admin."));
            return;
        }
        VaryonMapMarkerPlugin plugin = VaryonMapMarkerPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Erreur : le plugin n'est pas chargé."));
            return;
        }
        String markerName = nameArg.get(context);
        List<MarkerEntry> matches = plugin.findMarkersInWorldByName(world, markerName);
        if (matches.isEmpty()) {
            context.sendMessage(Message.raw("Aucun marqueur trouvé avec ce nom dans ce monde : " + markerName));
            return;
        }
        MarkerEntry target = matches.get(0);
        if (matches.size() > 1) {
            context.sendMessage(Message.raw(
                    "Plusieurs marqueurs portent ce nom ("
                            + matches.size()
                            + ") ; téléportation au premier (id : "
                            + shortId(target.id())
                            + ")."));
        }
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            context.sendMessage(Message.raw("Erreur : impossible de lire ta position."));
            return;
        }
        Vector3d prev = tc.getPosition();
        double tx = target.x() + 0.5;
        double tz = target.z() + 0.5;
        double fx;
        double fz;
        double fy;
        try {
            fx = Coord.parse(Double.toString(tx)).resolveXZ(prev.getX());
            fz = Coord.parse(Double.toString(tz)).resolveXZ(prev.getZ());
            fy = Coord.parse("~").resolveYAtWorldCoords(prev.getY(), world, fx, fz);
        } catch (GeneralCommandException e) {
            fx = tx;
            fz = tz;
            fy = prev.getY();
        }
        Teleport teleport = Teleport.createForPlayer(world, new Vector3d(fx, fy, fz), new Vector3f(0, 0, 0));
        store.addComponent(ref, Teleport.getComponentType(), teleport);
        context.sendMessage(Message.raw(
                "Téléportation vers « "
                        + target.markerName()
                        + " » (~ "
                        + String.format("%.1f", fx)
                        + ", "
                        + String.format("%.1f", fy)
                        + ", "
                        + String.format("%.1f", fz)
                        + ")."));
    }

    private static String shortId(String id) {
        if (id == null || id.length() <= 16) {
            return id;
        }
        return id.substring(0, 16) + "…";
    }
}
