/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.woxtz.weaponinfo.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.woxtz.weaponinfo.PluginConfig;
import com.woxtz.weaponinfo.ui.WeaponInfoPage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WeaponsCommand
extends AbstractCommand {
    public WeaponsCommand(String name, String description) {
        super(name, description);
    }

    protected boolean canGeneratePermission() {
        return false;
    }

    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!PluginConfig.getInstance().isPublicCommand() && !context.sender().hasPermission("weapons.use")) {
            context.sendMessage(Message.raw((String)"You don't have permission to use this command").color("#FF0000"));
            return CompletableFuture.completedFuture(null);
        }
        if (context.sender() instanceof Player) {
            Player player = (Player)context.sender();
            Ref ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store store = ref.getStore();
                World world = ((EntityStore)store.getExternalData()).getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRefComponent = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRefComponent != null) {
                        player.getPageManager().openCustomPage(ref, store, (CustomUIPage)new WeaponInfoPage(playerRefComponent, CustomPageLifetime.CanDismiss, ""));
                    }
                }, (Executor)world);
            }
            context.sendMessage(Message.raw((String)"Player is not in a world!"));
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(null);
    }
}

