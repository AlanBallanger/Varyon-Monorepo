package fr.varyon.playermarker;

import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;

import java.util.Collection;
import java.util.UUID;

final class BetterMapCompatProvider implements WorldMapManager.MarkerProvider {

    static final String PROVIDER_KEY = "BetterMapPlayerRadar";

    static boolean isAvailable() {
        return BetterMapBridge.isAvailable();
    }

    @Override
    public void update(World world, Player viewer, MarkersCollector collector) {
        BetterMapBridge.ViewerSettings viewerSettings = BetterMapBridge.resolveViewerSettings(viewer);
        if (!viewerSettings.enabled()) {
            return;
        }

        PlayerMarkerProviderContext context = PlayerMarkerProviderContext.resolve(viewer);
        UUID viewerUuid = viewer.getUuid();
        if (!context.isSurfaceEnabled()) {
            return;
        }

        Collection<PlayerRef> playerRefs = world.getPlayerRefs();
        if (playerRefs == null || playerRefs.isEmpty()) {
            return;
        }

        Vector3d viewerPosition = findViewerPosition(playerRefs, viewerUuid);
        long maxDistanceSquared = context.surface() == PlayerMarkerSurface.MAP
                ? Long.MAX_VALUE
                : maxDistanceSquared(viewerSettings.radarRange());

        PlayerMarkerVisibilityBatch visibilityBatch =
                PlayerMarkerVisibilityBatch.build(playerRefs, context.viewerRef(), context.viewerUuid());

        for (PlayerRef ref : playerRefs) {
            try {
                UUID playerUuid = ref.getUuid();
                if (playerUuid == null) {
                    continue;
                }
                boolean isViewer = context.isViewer(playerUuid);
                boolean filteredByCollector = PlayerMarkerVisibilityService.isHiddenByCollectorFilter(collector, ref);
                boolean filteredByVanishCollector = PlayerMarkerVisibilityService.isHiddenByHyEssentialsXVanishCollector(collector, ref);
                PlayerMarkerVisibilityDecision visibility =
                        PlayerMarkerVisibilityService.resolve(
                                context.viewerRef(),
                                context.viewerUuid(),
                                playerUuid,
                                filteredByCollector,
                                filteredByVanishCollector,
                                visibilityBatch);
                PlayerMarkerVisibilityState visibilityState = visibility.state();
                boolean surfaceEnabled = context.isTargetEnabled(playerUuid, visibility);
                if (!surfaceEnabled) {
                    continue;
                }
                if (!visibility.isVisible()) {
                    continue;
                }

                if (context.surface() == PlayerMarkerSurface.MAP && isViewer && !PlayerMarkerWorldMapState.shouldShowSelfMarker(viewer)) {
                    continue;
                }

                Transform playerTransform = PlayerMarkerLiveTracker.resolveTransform(ref);
                if (playerTransform == null || playerTransform.getPosition() == null) {
                    continue;
                }

                org.joml.Vector3d rawPos = playerTransform.getPosition();
                Vector3d playerJomlPos = new Vector3d(rawPos.x, rawPos.y, rawPos.z);

                if (!isViewer && viewerPosition != null && maxDistanceSquared != Long.MAX_VALUE) {
                    if (squaredDistance(viewerPosition, playerJomlPos) > maxDistanceSquared) {
                        continue;
                    }
                }

                String playerName = PlayerMarkerPlayerNames.resolve(ref);
                int distance = 0;
                if (!isViewer && viewerPosition != null) {
                    distance = (int) Math.sqrt(squaredDistance(viewerPosition, playerJomlPos));
                }

                String markerLabel = null;
                if (context.config() == null || context.config().showNickname) {
                    markerLabel = PlayerMarkerVisuals.decorateLabel(playerName + " (" + distance + "m)", visibilityState, context.viewerRef());
                }

                PlayerMarkerVisuals.AvatarVisual avatarVisual =
                        PlayerMarkerVisuals.resolveAvatarVisual(context.viewerRef(), playerUuid, playerName, visibilityState, null);
                Vector3f markerRotJoml = PlayerMarkerFactory.resolveMarkerRotation(
                        context.config(),
                        PlayerMarkerLiveTracker.resolveRotation(ref));
                com.hypixel.hytale.math.vector.Rotation3f markerRotation = new com.hypixel.hytale.math.vector.Rotation3f(markerRotJoml.x, markerRotJoml.y, markerRotJoml.z);
                Transform markerTransform = new Transform(rawPos, markerRotation);

                MapMarker marker = PlayerMarkerFactory.createPlainPlayerMarker(
                    PlayerMarkerFactory.buildDynamicMarkerId(
                    PlayerMarkerFactory.BETTER_MAP_MARKER_PREFIX,
                        playerUuid,
                        avatarVisual.markerVariant(),
                        markerTransform),
                    playerUuid,
                        markerLabel,
                        PlayerMarkerVisuals.labelColor(visibilityState),
                        avatarVisual.markerImage(),
                        markerTransform);

                BetterMapBridge.injectTeleportContextMenu(marker, viewer);
                if (context.surface() == PlayerMarkerSurface.MAP) {
                    collector.addIgnoreViewDistance(marker);
                } else {
                    collector.add(marker);
                }
            } catch (Exception ignored) {
            }
        }
    }


    private static Vector3d findViewerPosition(Collection<PlayerRef> playerRefs, UUID viewerUuid) {
        if (playerRefs == null || viewerUuid == null) {
            return null;
        }

        for (PlayerRef ref : playerRefs) {
            if (ref == null || !viewerUuid.equals(ref.getUuid())) {
                continue;
            }

            Vector3d position = PlayerMarkerLiveTracker.resolvePosition(ref);
            if (position != null) {
                return position;
            }
            break;
        }

        return null;
    }

    private static long maxDistanceSquared(int radarRange) {
        if (radarRange < 0) {
            return Long.MAX_VALUE;
        }

        long range = radarRange;
        return range * range;
    }

    private static double squaredDistance(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }
}