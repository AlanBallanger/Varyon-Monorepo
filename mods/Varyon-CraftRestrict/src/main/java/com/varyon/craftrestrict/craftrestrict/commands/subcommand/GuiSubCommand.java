package com.faiizer.craftrestrict.commands.subcommand;

import com.faiizer.craftrestrict.ui.CraftRestrictConfigPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GuiSubCommand extends AbstractPlayerCommand {

    public GuiSubCommand() {
        super("gui", "Ouvre l'interface de gestion des restrictions de craft");
    }

    @Nullable
    @Override
    protected String generatePermissionNode() {
        return "craftrestrict.commands.craftrestrict.gui.use";
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        CraftRestrictConfigPage.open(ref, store, player, world);
    }
}
