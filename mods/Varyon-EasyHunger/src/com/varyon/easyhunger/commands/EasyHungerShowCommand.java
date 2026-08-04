package com.varyon.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.ui.EasyHungerHud;
import com.varyon.easyhunger.ui.EasyWaterHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class EasyHungerShowCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.hud.show";

    public EasyHungerShowCommand() {
        super("show", "Show Hunger and Thirst HUDs", false);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        EasyHungerHud.updatePlayerHudVisibility(playerRef, true);
        EasyWaterHud.updatePlayerHudVisibility(playerRef, true);
    }
}
