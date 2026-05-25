package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

record PlayerMarkerProviderContext(VaryonPlayerMarkerPlugin plugin,
                                   PlayerMarkerConfig config,
                                   PlayerRef viewerRef,
                                   UUID viewerUuid,
                                   PlayerMarkerPlayerSettings viewerSettings,
                                   PlayerMarkerSurface surface) {

    static PlayerMarkerProviderContext resolve(Player viewer) {
        VaryonPlayerMarkerPlugin plugin = VaryonPlayerMarkerPlugin.getInstance();
        PlayerMarkerConfig config = VaryonPlayerMarkerPlugin.getConfig();
        UUID viewerUuid = viewer != null ? ((CommandSender) viewer).getUuid() : null;
        PlayerRef viewerRef = plugin != null ? plugin.getActivePlayerRef(viewerUuid) : null;
        if (plugin != null && plugin.getAvatarService() != null && viewerRef != null) {
            plugin.getAvatarService().advanceViewerDeliveryPhase(viewerRef);
        }

        boolean worldMapVisible = PlayerMarkerWorldMapState.isWorldMapVisible(viewer);
        PlayerMarkerSurface surface = worldMapVisible ? PlayerMarkerSurface.MAP : PlayerMarkerSurface.COMPASS;
        PlayerMarkerPlayerSettings viewerSettings = plugin != null
                ? plugin.resolvePlayerSettings(viewerUuid)
                : new PlayerMarkerPlayerSettings();
        return new PlayerMarkerProviderContext(plugin, config, viewerRef, viewerUuid, viewerSettings, surface);
    }

    boolean isSurfaceEnabled() {
        return viewerSettings.isEnabled(surface);
    }

    boolean isViewer(UUID targetUuid) {
        return viewerUuid != null && viewerUuid.equals(targetUuid);
    }

    boolean isTargetEnabled(UUID targetUuid, PlayerMarkerVisibilityDecision visibility) {
        boolean isViewer = isViewer(targetUuid);
        return surface == PlayerMarkerSurface.MAP && isViewer && visibility != null && visibility.isGhosted()
                ? viewerSettings.isEnabled(surface)
                : viewerSettings.isEnabledFor(surface, viewerUuid, targetUuid);
    }
}

