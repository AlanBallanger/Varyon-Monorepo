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

public final class TravelCamDeleteCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg =
        withRequiredArg("name", "Nom du traveling a supprimer", ArgTypes.STRING);

    public TravelCamDeleteCommand() {
        super("delete", "Supprime un traveling de camera enregistre");
        addAliases(new String[] {"remove", "del", "rm"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        String name = nameArg.get(context);
        CameraPathStore pathStore = TravelingCameraManager.store();
        boolean deleted = pathStore != null && pathStore.delete(name);
        if (!deleted) {
            context.sendMessage(Message.raw("Traveling introuvable : ")
                .color("#FF5555")
                .insert(Message.raw(name).color("#FFFFFF")));
            return;
        }
        context.sendMessage(Message.raw("Traveling '")
            .color("#55FF55")
            .insert(Message.raw(name).color("#FFFFFF"))
            .insert(Message.raw("' supprime.").color("#55FF55")));
    }
}
