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

public final class TravelCamShowCommand extends AbstractPlayerCommand {

    public TravelCamShowCommand() {
        super("show", "Bascule l'affichage des waypoints proches de toi, tous travelings confondus");
        addAliases(new String[] {"visualize", "viz"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        boolean enabled = TravelingCameraManager.toggleVisualization(playerRef.getUuid(), world);
        if (enabled) {
            context.sendMessage(Message.raw("Visualisation activee : les waypoints proches de toi s'afficheront en continu.")
                .color("#55FF55"));
        } else {
            context.sendMessage(Message.raw("Visualisation desactivee.").color("#AAAAAA"));
        }
    }
}
