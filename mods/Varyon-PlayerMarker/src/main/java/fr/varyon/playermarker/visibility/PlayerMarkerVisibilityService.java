package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

final class PlayerMarkerVisibilityService {

    private static final String HYESSENTIALSX_VANISH_FILTER_CLASS =
            "xyz.thelegacyvoyage.hyessentialsx.util.MapVisibilityUtil$VanishMapFilter";

    private PlayerMarkerVisibilityService() {
    }

    static PlayerMarkerVisibilityDecision resolve(PlayerRef viewerRef, UUID viewerUuid, UUID targetUuid) {
        return resolve(viewerRef, viewerUuid, targetUuid, false, false, null);
    }

    static PlayerMarkerVisibilityDecision resolve(PlayerRef viewerRef,
                                                  UUID viewerUuid,
                                                  UUID targetUuid,
                                                  boolean hiddenByCollector,
                                                  boolean hiddenByVanishCollector) {
        return resolve(viewerRef, viewerUuid, targetUuid, hiddenByCollector, hiddenByVanishCollector, null);
    }

    static PlayerMarkerVisibilityDecision resolve(PlayerRef viewerRef,
                                                  UUID viewerUuid,
                                                  UUID targetUuid,
                                                  boolean hiddenByCollector,
                                                  boolean hiddenByVanishCollector,
                                                  PlayerMarkerVisibilityBatch batch) {
        if (targetUuid == null) {
            return new PlayerMarkerVisibilityDecision(
                    PlayerMarkerVisibilityState.HIDDEN,
                    false,
                    false,
                    false,
                    false,
                    hiddenByCollector,
                    hiddenByVanishCollector);
        }

        boolean self = viewerUuid != null && viewerUuid.equals(targetUuid);
        boolean viewerVanished;
        boolean targetVanished;
        boolean hiddenByViewerManager;
        if (batch != null) {
            viewerVanished = batch.effectiveVanished(viewerUuid);
            targetVanished = batch.effectiveVanished(targetUuid);
            hiddenByViewerManager = batch.isHiddenByViewer(targetUuid);
        } else {
            viewerVanished = isVanished(viewerUuid);
            targetVanished = isVanished(targetUuid);
            hiddenByViewerManager = isHiddenByViewer(viewerRef, targetUuid);
        }

        return PlayerMarkerVisibilityResolver.resolve(new PlayerMarkerVisibilityInputs(
                self,
                viewerVanished,
                targetVanished,
                hiddenByViewerManager,
                hiddenByCollector,
                hiddenByVanishCollector));
    }


    static boolean isHiddenByCollectorFilter(MarkersCollector collector, PlayerRef targetRef) {
        if (collector == null || targetRef == null) {
            return false;
        }

        Predicate<PlayerRef> playerMapFilter = collector.getPlayerMapFilter();
        return playerMapFilter != null && playerMapFilter.test(targetRef);
    }

    static boolean isHiddenByHyEssentialsXVanishCollector(MarkersCollector collector, PlayerRef targetRef) {
        if (collector == null || targetRef == null) {
            return false;
        }

        Predicate<PlayerRef> playerMapFilter = collector.getPlayerMapFilter();
        return playerMapFilter != null
                && isHyEssentialsXVanishFilter(playerMapFilter)
                && playerMapFilter.test(targetRef);
    }

    private static boolean isHiddenByViewer(PlayerRef viewerRef, UUID targetUuid) {
        if (viewerRef == null || targetUuid == null) {
            return false;
        }

        return Boolean.TRUE.equals(PlayerMarkerWorldThreadBridge.call(
                viewerRef,
                () -> {
                    HiddenPlayersManager hiddenPlayersManager = viewerRef.getHiddenPlayersManager();
                    return hiddenPlayersManager != null && hiddenPlayersManager.isPlayerHidden(targetUuid);
                },
                Boolean.FALSE,
                "resolve hidden player state"));
    }

    private static boolean isVanished(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }

        if (PlayerMarkerVanishProviders.isVanished(playerUuid)) {
            return true;
        }

        return isHiddenByOtherPlayers(playerUuid);
    }

    private static boolean isHiddenByOtherPlayers(UUID targetUuid) {
        if (targetUuid == null) {
            return false;
        }

        VaryonPlayerMarkerPlugin plugin = VaryonPlayerMarkerPlugin.getInstance();
        Collection<PlayerRef> players = plugin != null ? plugin.getActivePlayers() : null;
        if (players == null || players.isEmpty()) {
            return false;
        }

        for (PlayerRef observerRef : players) {
            if (observerRef == null || targetUuid.equals(observerRef.getUuid())) {
                continue;
            }

            boolean hidden = Boolean.TRUE.equals(PlayerMarkerWorldThreadBridge.call(
                    observerRef,
                    () -> {
                        HiddenPlayersManager hiddenPlayersManager = observerRef.getHiddenPlayersManager();
                        return hiddenPlayersManager != null && hiddenPlayersManager.isPlayerHidden(targetUuid);
                    },
                    Boolean.FALSE,
                    "infer vanish visibility state"));
            if (hidden) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHyEssentialsXVanishFilter(Predicate<PlayerRef> playerMapFilter) {
        if (playerMapFilter == null) {
            return false;
        }

        for (Class<?> type = playerMapFilter.getClass(); type != null; type = type.getSuperclass()) {
            if (HYESSENTIALSX_VANISH_FILTER_CLASS.equals(type.getName())) {
                return true;
            }
        }

        String className = playerMapFilter.getClass().getName();
        return className != null
                && className.startsWith(HYESSENTIALSX_VANISH_FILTER_CLASS + "$");
    }
}

