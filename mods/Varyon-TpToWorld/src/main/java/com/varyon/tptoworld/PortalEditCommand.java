package com.varyon.tptoworld;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.tptoworld.portal.PortalEditModeManager;

import javax.annotation.Nonnull;
import java.awt.Color;

public final class PortalEditCommand extends AbstractPlayerCommand {

    public PortalEditCommand() {
        super("portaledit", "Basculer le mode édition des portails Varyon");
        this.requirePermission("varyon.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        boolean enabled = PortalEditModeManager.toggle(playerRef.getUuid());
        if (enabled) {
            context.sendMessage(Message.raw("Mode édition des portails activé. Utilise l'objet en main sur un portail pour le configurer.").color(Color.GREEN));
        } else {
            context.sendMessage(Message.raw("Mode édition des portails désactivé.").color(Color.RED));
        }
    }
}
