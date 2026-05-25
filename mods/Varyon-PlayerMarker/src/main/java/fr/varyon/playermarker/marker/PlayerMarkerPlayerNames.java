package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

final class PlayerMarkerPlayerNames {

    private PlayerMarkerPlayerNames() {
    }

    static String resolve(PlayerRef playerRef) {
        if (playerRef == null) {
            return "unknown";
        }

        String username = playerRef.getUsername();
        if (username != null && !username.isBlank()) {
            return username;
        }

        UUID playerUuid = playerRef.getUuid();
        return playerUuid != null ? playerUuid.toString().substring(0, 8) : "unknown";
    }

    static String resolveOrLocalizedUnknown(PlayerRef playerRef, PlayerRef viewerRef) {
        if (playerRef == null) {
            return PlayerMarkerUiText.choose(viewerRef, "Unknown player", "Невідомий гравець");
        }

        String username = playerRef.getUsername();
        if (username != null && !username.isBlank()) {
            return username;
        }

        UUID playerUuid = playerRef.getUuid();
        return playerUuid != null
                ? playerUuid.toString().substring(0, 8)
                : PlayerMarkerUiText.choose(viewerRef, "Unknown player", "Невідомий гравець");
    }
}

