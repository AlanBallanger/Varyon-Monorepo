package fr.varyon.travelingcamera;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class TravelCamStopCommand extends AbstractPlayerCommand {

    public TravelCamStopCommand() {
        super("stop", "Termine l'enregistrement en cours et sauvegarde le traveling");
        addAliases(new String[] {"s"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        CameraPath path = TravelingCameraManager.stopRecording(playerRef.getUuid());
        if (path == null) {
            context.sendMessage(Message.raw("Aucun enregistrement en cours.").color("#FF5555"));
            return;
        }
        context.sendMessage(Message.raw("Traveling '")
            .color("#55FF55")
            .insert(Message.raw(path.name).color("#FFFFFF"))
            .insert(Message.raw("' sauvegarde avec " + path.waypoints.size() + " point(s).")
                .color("#55FF55")));
    }
}
