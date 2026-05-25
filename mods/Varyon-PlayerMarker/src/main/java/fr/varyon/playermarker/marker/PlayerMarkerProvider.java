package fr.varyon.playermarker;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.asset.type.gameplay.WorldMapConfig;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;

import java.util.Collection;
import java.util.UUID;

public class PlayerMarkerProvider implements WorldMapManager.MarkerProvider {

    static void removePersistedAvatar(UUID uuid) {
        PlayerMarkerPlayerModelSupport.forget(uuid);
    }

    @Override
    public void update(World world, Player viewer, MarkersCollector collector) {
        Collection<PlayerRef> playerRefs = world.getPlayerRefs();
        if (playerRefs == null || playerRefs.isEmpty()) {
            return;
        }

        WorldMapConfig worldMapConfig = world.getGameplayConfig().getWorldMapConfig();
        if (worldMapConfig != null && !worldMapConfig.isDisplayPlayers()) {
            return;
        }

        PlayerMarkerProviderContext context = PlayerMarkerProviderContext.resolve(viewer);
        if (!context.isSurfaceEnabled()) {
            return;
        }

        PlayerMarkerVisibilityBatch visibilityBatch =
                PlayerMarkerVisibilityBatch.build(playerRefs, context.viewerRef(), context.viewerUuid());

        for (PlayerRef ref : playerRefs) {
            try {
                UUID playerUuid = ref.getUuid();
                if (playerUuid == null) continue;
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
                Transform t = PlayerMarkerLiveTracker.resolveTransform(ref);
                if (t == null) continue;
                Vector3d position = t.getPosition();
                if (position == null) continue;
                String playerName = PlayerMarkerPlayerNames.resolve(ref);
                PlayerMarkerVisuals.AvatarVisual avatarVisual =
                        PlayerMarkerVisuals.resolveAvatarVisual(context.viewerRef(), playerUuid, playerName, visibilityState, null);
                Vector3f headRotation = PlayerMarkerLiveTracker.resolveRotation(ref);
                Vector3f markerRotation = PlayerMarkerFactory.resolveMarkerRotation(context.config(), headRotation);
                Transform transform = new Transform(position, markerRotation);
                if (context.surface() == PlayerMarkerSurface.MAP && isViewer && !PlayerMarkerWorldMapState.shouldShowSelfMarker(viewer)) {
                    continue;
                }

                String markerLabel = context.config() == null || context.config().showNickname
                        ? PlayerMarkerVisuals.decorateLabel(playerName, visibilityState, context.viewerRef())
                        : null;

                MapMarker marker = PlayerMarkerFactory.createNamedPlayerMarker(
                    PlayerMarkerFactory.buildDynamicMarkerId(
                        PlayerMarkerFactory.PLAYER_MARKER_PREFIX,
                        playerUuid,
                        avatarVisual.markerVariant(),
                        transform),
                        playerUuid,
                        markerLabel,
                        PlayerMarkerVisuals.labelColor(visibilityState),
                        avatarVisual.markerImage(),
                        transform);
                if (context.surface() == PlayerMarkerSurface.MAP) {
                    collector.addIgnoreViewDistance(marker);
                } else {
                    collector.add(marker);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
