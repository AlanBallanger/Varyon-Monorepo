package com.varyon.bossarena.util;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.UUID;

public final class EntityComponents {
    private EntityComponents() {}

    /**
     * Player UUID without touching the entity {@link com.hypixel.hytale.component.Store}.
     * Store access is world-thread-bound; server-wide alerts iterate players from other worlds
     * and must not call {@code store.getComponent}.
     */
    @Nullable
    public static UUID uuid(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        return playerRef.getUuid();
    }
}
