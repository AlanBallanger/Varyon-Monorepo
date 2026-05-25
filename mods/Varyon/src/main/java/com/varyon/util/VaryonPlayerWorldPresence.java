package com.varyon.util;

import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VaryonPlayerWorldPresence {

    private static final ConcurrentHashMap<UUID, Boolean> IN_VARYON_ENABLED_WORLD = new ConcurrentHashMap<>();

    private VaryonPlayerWorldPresence() {}

    public static void update(@Nullable UUID playerUuid, @Nullable World worldJoined) {
        if (playerUuid == null) {
            return;
        }
        IN_VARYON_ENABLED_WORLD.put(playerUuid, VaryonWorldAccess.isVaryonEnabledWorld(worldJoined));
    }

    public static boolean isInVaryonEnabledWorld(@Nullable UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        return Boolean.TRUE.equals(IN_VARYON_ENABLED_WORLD.get(playerUuid));
    }

    public static void clear(@Nullable UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        IN_VARYON_ENABLED_WORLD.remove(playerUuid);
    }
}
