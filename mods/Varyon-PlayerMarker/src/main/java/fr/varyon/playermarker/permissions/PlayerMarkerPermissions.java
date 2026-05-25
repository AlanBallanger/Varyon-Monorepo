package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

final class PlayerMarkerPermissions {

    static final String ADMIN = "varyon.playermarker.admin";
    static final String USE = "varyon.playermarker.use";
    static final String SETTINGS_MAP = "varyon.playermarker.settings.map";
    static final String SETTINGS_MINIMAP = "varyon.playermarker.settings.minimap";
    static final String SETTINGS_COMPASS = "varyon.playermarker.settings.compass";

    private PlayerMarkerPermissions() {
    }

    static boolean canOpenUi(PlayerRef playerRef) {
        return has(playerRef, USE);
    }

    static boolean canEditSurface(PlayerRef playerRef, PlayerMarkerSurface surface) {
        return has(playerRef, switch (surface) {
            case MAP -> SETTINGS_MAP;
            case MINIMAP -> SETTINGS_MINIMAP;
            case COMPASS -> SETTINGS_COMPASS;
        });
    }

    static void sendUseDenied(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        playerRef.sendMessage(Message.raw(PlayerMarkerUiText.choose(
                playerRef,
                "You do not have permission to use /playermarker.",
                "У вас немає дозволу на використання /playermarker.")));
    }

    static void sendSurfaceDenied(PlayerRef playerRef, PlayerMarkerSurface surface) {
        if (playerRef == null || surface == null) {
            return;
        }

        String surfaceLabel = switch (surface) {
            case MAP -> PlayerMarkerUiText.choose(playerRef, "map", "мапи");
            case MINIMAP -> PlayerMarkerUiText.choose(playerRef, "minimap", "мінімапи");
            case COMPASS -> PlayerMarkerUiText.choose(playerRef, "compass", "компаса");
        };
        playerRef.sendMessage(Message.raw(PlayerMarkerUiText.format(
                playerRef,
                "You do not have permission to change %s avatar visibility.",
                "У вас немає дозволу змінювати видимість аватарок для %s.",
                surfaceLabel)));
    }

    private static boolean has(PlayerRef playerRef, String permission) {
        if (playerRef == null || permission == null || permission.isBlank()) {
            return false;
        }

        PermissionsModule permissionsModule = PermissionsModule.get();
        if (permissionsModule == null) {
            return false;
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return false;
        }

        return permissionsModule.hasPermission(playerUuid, ADMIN)
                || permissionsModule.hasPermission(playerUuid, permission);
    }
}