package com.varyon.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.ui.EasyHungerValuesPage;

import javax.annotation.Nonnull;

/**
 * Command to open the EasyHunger Values configuration UI.
 * Usage: /ehconfig
 */
public class EasyHungerValuesCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.config";

    public EasyHungerValuesCommand() {
        this("ehconfig");
    }

    public EasyHungerValuesCommand(String commandName) {
        super(commandName, "Opens the EasyHunger configuration UI", false);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Open the configuration UI
        EasyHungerValuesPage page = new EasyHungerValuesPage(playerRef);
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
