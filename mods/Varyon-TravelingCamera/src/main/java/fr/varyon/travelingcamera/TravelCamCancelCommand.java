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

public final class TravelCamCancelCommand extends AbstractPlayerCommand {

    public TravelCamCancelCommand() {
        super("cancel", "Annule l'enregistrement en cours sans sauvegarder");
        addAliases(new String[] {"c"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        if (!TravelingCameraManager.isRecording(playerRef.getUuid())) {
            context.sendMessage(Message.raw("Aucun enregistrement en cours.").color("#FF5555"));
            return;
        }
        TravelingCameraManager.cancelRecording(playerRef.getUuid());
        context.sendMessage(Message.raw("Enregistrement annule.").color("#AAAAAA"));
    }
}
