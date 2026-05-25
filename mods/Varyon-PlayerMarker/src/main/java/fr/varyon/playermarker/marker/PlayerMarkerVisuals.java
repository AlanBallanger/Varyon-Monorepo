package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

final class PlayerMarkerVisuals {

    private static final String GHOSTED_LABEL_COLOR = "#FFFFFF80";

    private PlayerMarkerVisuals() {
    }

    static String decorateLabel(String baseLabel, PlayerMarkerVisibilityState visibilityState, PlayerRef viewerRef) {
        if (baseLabel == null || baseLabel.isBlank() || visibilityState == null || !visibilityState.isGhosted()) {
            return baseLabel;
        }

        return baseLabel + " · " + PlayerMarkerUiText.choose(viewerRef, "hidden", "прихований");
    }

    static String labelColor(PlayerMarkerVisibilityState visibilityState) {
        return visibilityState != null && visibilityState.isGhosted() ? GHOSTED_LABEL_COLOR : null;
    }

    static AvatarVisual resolveAvatarVisual(PlayerRef viewerRef,
                                            UUID playerUuid,
                                            String playerName,
                                            PlayerMarkerVisibilityState visibilityState,
                                            Runnable onReady) {
        VaryonPlayerMarkerPlugin plugin = VaryonPlayerMarkerPlugin.getInstance();
        PlayerMarkerAvatarService avatarService = plugin != null ? plugin.getAvatarService() : null;
        if (avatarService == null) {
            return new AvatarVisual(":fallback", null);
        }

        boolean ghosted = visibilityState != null && visibilityState.isGhosted();
        String markerImage = avatarService.ensureAvatarForViewer(viewerRef, playerUuid, playerName, ghosted, onReady);
        String variantPrefix = ghosted ? ":ghosted:" : ":avatar:";
        String markerVariant = PlayerMarkerAvatarService.isFallbackMarkerImage(markerImage)
                ? ":fallback"
                : variantPrefix + Integer.toUnsignedString(markerImage.hashCode(), 16);
        return new AvatarVisual(markerVariant, markerImage);
    }

    static String toUiAssetPath(String markerImage) {
        return PlayerMarkerAvatarService.toUiAssetPath(markerImage);
    }

    record AvatarVisual(String markerVariant, String markerImage) {
    }
}

