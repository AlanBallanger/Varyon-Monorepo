package com.varyon.tptoworld.portal;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PortalEditModeManager {

    private static final Set<UUID> EDIT_MODE_PLAYERS = ConcurrentHashMap.newKeySet();

    private PortalEditModeManager() {
    }

    public static boolean toggle(UUID playerUuid) {
        if (EDIT_MODE_PLAYERS.contains(playerUuid)) {
            EDIT_MODE_PLAYERS.remove(playerUuid);
            return false;
        }
        EDIT_MODE_PLAYERS.add(playerUuid);
        return true;
    }

    public static boolean isInEditMode(UUID playerUuid) {
        return EDIT_MODE_PLAYERS.contains(playerUuid);
    }
}
