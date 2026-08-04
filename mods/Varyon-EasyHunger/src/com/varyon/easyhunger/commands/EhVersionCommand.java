package com.varyon.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class EhVersionCommand extends AbstractPlayerCommand {

    public EhVersionCommand() {
        super("ehversion", "Show EasyHunger version and HUD mode", false);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        // Creative only
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && player.getGameMode() != GameMode.Creative) {
            context.sendMessage(Message.raw("This command is only available in Creative mode."));
            return;
        }

        String version = "1.2.1";

        context.sendMessage(Message.raw("[EasyHunger] Version: " + version));
    }
}
