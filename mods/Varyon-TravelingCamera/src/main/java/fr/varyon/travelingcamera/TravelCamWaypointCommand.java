package fr.varyon.travelingcamera;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class TravelCamWaypointCommand extends AbstractPlayerCommand {

    private final DefaultArg<Float> travelSecondsArg = withDefaultArg(
        "travelSeconds", "Duree du trajet depuis le point precedent (secondes)", ArgTypes.FLOAT, 3.0f, "3.0");
    private final DefaultArg<Float> holdSecondsArg = withDefaultArg(
        "holdSeconds", "Duree de pause sur ce point (secondes)", ArgTypes.FLOAT, 0.0f, "0.0");

    public TravelCamWaypointCommand() {
        super("waypoint", "Capture la position/rotation actuelle de la camera comme point du traveling");
        addAliases(new String[] {"wp"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        if (!TravelingCameraManager.isRecording(playerRef.getUuid())) {
            context.sendMessage(Message.raw("Aucun enregistrement en cours. Utilise /travelcam record <nom> d'abord.")
                .color("#FF5555"));
            return;
        }
        float travelSeconds = travelSecondsArg.get(context);
        float holdSeconds = holdSecondsArg.get(context);
        TravelingCameraManager.addWaypoint(playerRef.getUuid(), playerRef, holdSeconds, travelSeconds);

        RecordingSession session = TravelingCameraManager.recordingSession(playerRef.getUuid());
        int count = session != null ? session.path.waypoints.size() : 0;
        context.sendMessage(Message.raw("Point #")
            .color("#AAAAAA")
            .insert(Message.raw(String.valueOf(count)).color("#FFFFFF"))
            .insert(Message.raw(" ajoute.").color("#AAAAAA")));
    }
}
