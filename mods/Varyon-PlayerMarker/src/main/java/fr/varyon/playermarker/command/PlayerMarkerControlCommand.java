package fr.varyon.playermarker;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class PlayerMarkerControlCommand extends AbstractPlayerCommand {

    private final VaryonPlayerMarkerPlugin plugin;

    PlayerMarkerControlCommand(VaryonPlayerMarkerPlugin plugin) {
        super("playermarker", "Marqueurs joueurs sur carte, minicarte et boussole");
        this.plugin = plugin;
        setAllowsExtraArguments(true);
        addAliases("pm");
    }

    @Override
    protected void execute(CommandContext context,
                           Store<EntityStore> store,
                           Ref<EntityStore> entityRef,
                           PlayerRef playerRef,
                           World world) {
        if (!PlayerMarkerPermissions.canOpenUi(playerRef)) {
            PlayerMarkerPermissions.sendUseDenied(playerRef);
            return;
        }

        String input = context.getInputString();
        String[] args = input == null || input.isBlank() ? new String[0] : input.trim().split("\\s+");
        int start = 0;
        if (args.length > 0 && ("playermarker".equalsIgnoreCase(args[0])
                || "pm".equalsIgnoreCase(args[0]))) {
            start = 1;
        }

        if (args.length > start) {
            playerRef.sendMessage(Message.raw(PlayerMarkerUiText.choose(
                    playerRef,
                    "Usage: /playermarker or /pm",
                    "Використання: /playermarker або /pm")));
            return;
        }

        plugin.openControlPage(store, entityRef, playerRef);
    }
}