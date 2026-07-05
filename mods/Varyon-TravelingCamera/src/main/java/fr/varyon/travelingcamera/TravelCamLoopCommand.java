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

public final class TravelCamLoopCommand extends AbstractPlayerCommand {

    public TravelCamLoopCommand() {
        super("loop", "Bascule le bouclage pour l'enregistrement en cours");
        addAliases(new String[] {"lp", "l"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        RecordingSession session = TravelingCameraManager.recordingSession(playerRef.getUuid());
        if (session == null) {
            context.sendMessage(Message.raw("Aucun enregistrement en cours.").color("#FF5555"));
            return;
        }
        session.path.loop = !session.path.loop;
        context.sendMessage(Message.raw("Bouclage " + (session.path.loop ? "active" : "desactive") + " pour '" + session.name + "'.")
            .color("#AAAAAA"));
    }
}
