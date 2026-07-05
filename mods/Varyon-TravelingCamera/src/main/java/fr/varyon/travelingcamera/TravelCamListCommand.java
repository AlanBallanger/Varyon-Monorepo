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
import java.util.List;

public final class TravelCamListCommand extends AbstractPlayerCommand {

    public TravelCamListCommand() {
        super("list", "Liste les travelings de camera enregistres");
        addAliases(new String[] {"ls"});
        requirePermission(TravelCamCommand.ADMIN_PERMISSION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        CameraPathStore pathStore = TravelingCameraManager.store();
        List<String> names = pathStore != null ? pathStore.listNames() : List.of();
        if (names.isEmpty()) {
            context.sendMessage(Message.raw("Aucun traveling enregistre.").color("#AAAAAA"));
            return;
        }
        context.sendMessage(Message.raw("Travelings (" + names.size() + ") : " + String.join(", ", names))
            .color("#AAAAAA"));
    }
}
