package fr.varyon.travelingcamera;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class TravelCamRecordCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg =
        withRequiredArg("name", "Nom du traveling a enregistrer", ArgTypes.STRING);

    public TravelCamRecordCommand() {
        super("record", "Demarre l'enregistrement d'un nouveau traveling de camera");
        addAliases(new String[] {"rec", "r"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        String name = nameArg.get(context);
        if (TravelingCameraManager.isRecording(playerRef.getUuid())) {
            context.sendMessage(Message.raw("Un enregistrement est deja en cours. Utilise /travelcam stop ou /travelcam cancel d'abord.")
                .color("#FF5555"));
            return;
        }
        TravelingCameraManager.startRecording(playerRef.getUuid(), name);
        context.sendMessage(Message.raw("Enregistrement du traveling '")
            .color("#55FF55")
            .insert(Message.raw(name).color("#FFFFFF"))
            .insert(Message.raw("' demarre. Utilise /travelcam waypoint pour ajouter des points, /travelcam stop pour terminer.")
                .color("#55FF55")));
    }
}
