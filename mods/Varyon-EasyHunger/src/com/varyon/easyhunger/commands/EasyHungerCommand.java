package com.varyon.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class EasyHungerCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.command.base";

    public EasyHungerCommand() {
        super("easyhunger", "EasyHunger Base Command", false);
        this.requirePermission(requiredPermission);
        this.addAliases("eh");
        
        this.addSubCommand(new EasyHungerHideCommand());
        this.addSubCommand(new EasyHungerShowCommand());
        this.addSubCommand(new EhAddAllCommand());
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        // Default execution if no subcommand matches
        Message message = Message.empty()
            .insert("EasyHunger Commands:\n")
            .insert("/eh hide - Hide HUDs\n")
            .insert("/eh show - Show HUDs\n")
            .insert("/ehconfig - Open Configuration");

        // The addall command should only show up if they have its specific permission, which is handled
        // intrinsically by the command engine. We'll simply omit it from the hardcoded help menu.
            
        playerRef.sendMessage(message);
    }
}
