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

public final class TravelCamCommand extends AbstractPlayerCommand {

    public static final String ADMIN_PERMISSION = "travelingcamera.admin";

    public TravelCamCommand() {
        super("travelcam", "Gere l'edition des travelings de camera cinematiques (reserve aux editeurs)");
        addAliases(new String[] {"tcam"});
        addSubCommand(new TravelCamRecordCommand());
        addSubCommand(new TravelCamWaypointCommand());
        addSubCommand(new TravelCamLoopCommand());
        addSubCommand(new TravelCamStopCommand());
        addSubCommand(new TravelCamCancelCommand());
        addSubCommand(new TravelCamListCommand());
        addSubCommand(new TravelCamDeleteCommand());
        addSubCommand(new TravelCamShowCommand());
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        context.sendMessage(Message.raw(
            "Usage: /travelcam (ou /tcam) <record|waypoint|loop|stop|cancel|list|delete|show> ... "
                + "Pour lancer un traveling sur un joueur : /travelcamplay [joueur] <nom>")
            .color("#AAAAAA"));
    }
}
