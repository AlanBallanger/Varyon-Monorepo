package com.varyon.item;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class AmbassadeWarpHelper {

    private AmbassadeWarpHelper() {
    }

    public static boolean tryWarp(@Nonnull PlayerRef playerRef, @Nonnull String warpName) {
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        String name = warpName.trim();
        if (name.isEmpty()) {
            return false;
        }
        CommandManager cm = CommandManager.get();
        if (cm == null) {
            return false;
        }
        cm.handleCommand(playerRef, "warp " + name);
        return true;
    }
}
