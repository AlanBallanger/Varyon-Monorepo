package fr.varyon.travelingcamera;

import com.hypixel.hytale.math.vector.Rotation3fc;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TravelingCameraManager {

    private static final float VISUALIZE_REFRESH_INTERVAL = 1.5f;
    private static final float VISUALIZE_DISPLAY_TIME = VISUALIZE_REFRESH_INTERVAL + 1.0f;
    private static final double VISUALIZE_RADIUS = 40.0;
    private static final float SPEED_BOOST_MULTIPLIER = 3.0f;

    private static CameraPathStore pathStore;

    private static final Map<UUID, RecordingSession> recordingSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, PlaybackState> playbackStates = new ConcurrentHashMap<>();
    private static final Map<UUID, VisualizationSession> visualizationSessions = new ConcurrentHashMap<>();

    private TravelingCameraManager() {}

    public static void bind(@Nonnull CameraPathStore store) {
        pathStore = store;
    }

    public static CameraPathStore store() {
        return pathStore;
    }

    public static boolean isRecording(@Nonnull UUID uuid) {
        return recordingSessions.containsKey(uuid);
    }

    @Nullable
    public static RecordingSession recordingSession(@Nonnull UUID uuid) {
        return recordingSessions.get(uuid);
    }

    public static void startRecording(@Nonnull UUID uuid, @Nonnull String name) {
        recordingSessions.put(uuid, new RecordingSession(name));
    }

    public static void addWaypoint(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                   float holdSeconds, float travelSeconds) {
        RecordingSession session = recordingSessions.get(uuid);
        if (session == null) {
            return;
        }
        var position = playerRef.getTransform().getPosition();
        Rotation3fc rotation = playerRef.getHeadRotation();
        session.path.waypoints.add(new CameraWaypoint(
            position.x(), position.y(), position.z(),
            rotation.yaw(), rotation.pitch(), rotation.roll(),
            holdSeconds / 2f, travelSeconds / 2f
        ));
    }

    @Nullable
    public static CameraPath stopRecording(@Nonnull UUID uuid) {
        RecordingSession session = recordingSessions.remove(uuid);
        if (session == null) {
            return null;
        }
        if (pathStore != null) {
            pathStore.save(session.path);
        }
        return session.path;
    }

    public static void cancelRecording(@Nonnull UUID uuid) {
        recordingSessions.remove(uuid);
    }

    public static boolean isPlaying(@Nonnull UUID uuid) {
        return playbackStates.containsKey(uuid);
    }

    public static boolean play(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef, @Nonnull CameraPath path) {
        if (path.waypoints.isEmpty()) {
            return false;
        }
        playbackStates.put(uuid, new PlaybackState(path));
        applyFrame(playerRef, path.waypoints.get(0), 0.2f, 0.2f);
        runOnWorldThread(playerRef, () -> TravelCamBoostHintHud.show(resolvePlayer(playerRef), playerRef));
        return true;
    }

    public static void stop(@Nonnull UUID uuid, @Nullable PlayerRef playerRef) {
        if (playbackStates.remove(uuid) != null && playerRef != null) {
            resetCamera(playerRef);
            runOnWorldThread(playerRef, () -> TravelCamBoostHintHud.hide(resolvePlayer(playerRef), playerRef));
        }
    }

    @Nullable
    private static Player resolvePlayer(@Nonnull PlayerRef playerRef) {
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        var store = ref.getStore();
        if (store == null) {
            return null;
        }
        return store.getComponent(ref, Player.getComponentType());
    }

    private static void runOnWorldThread(@Nonnull PlayerRef playerRef, @Nonnull Runnable action) {
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        var store = ref.getStore();
        if (store == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }
        world.execute(action);
    }

    public static void reportMovementIntent(@Nonnull UUID uuid, boolean moving) {
        PlaybackState state = playbackStates.get(uuid);
        if (state == null) {
            return;
        }
        if (moving && !state.lastReportedMoving) {
            state.speedBoosted = !state.speedBoosted;
        }
        state.lastReportedMoving = moving;
    }

    public static void tickPlayback(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef, float dt) {
        PlaybackState state = playbackStates.get(uuid);
        if (state == null) {
            return;
        }
        CameraPath path = state.path;
        float effectiveDt = state.speedBoosted ? dt * SPEED_BOOST_MULTIPLIER : dt;
        state.segmentElapsed += effectiveDt;

        CameraWaypoint from = path.waypoints.get(state.segmentIndex);
        int nextIndex = state.segmentIndex + 1;
        boolean wraps = nextIndex >= path.waypoints.size();
        CameraWaypoint to = wraps
            ? (path.loop ? path.waypoints.get(0) : from)
            : path.waypoints.get(nextIndex);

        float segmentDuration = from.travelSeconds + from.holdSeconds;
        if (segmentDuration <= 0f) {
            segmentDuration = 0.0001f;
        }

        if (state.segmentElapsed >= segmentDuration) {
            state.segmentElapsed -= segmentDuration;
            if (wraps) {
                if (path.loop) {
                    state.segmentIndex = 0;
                } else {
                    stop(uuid, playerRef);
                    return;
                }
            } else {
                state.segmentIndex = nextIndex;
            }
            from = path.waypoints.get(state.segmentIndex);
            int afterIndex = state.segmentIndex + 1;
            to = afterIndex >= path.waypoints.size()
                ? (path.loop ? path.waypoints.get(0) : from)
                : path.waypoints.get(afterIndex);
        }

        float travelSeconds = Math.max(from.travelSeconds, 0.0001f);
        float travelT = Math.min(state.segmentElapsed / travelSeconds, 1f);
        double x = lerp(from.x, to.x, travelT);
        double y = lerp(from.y, to.y, travelT);
        double z = lerp(from.z, to.z, travelT);
        float yaw = lerpAngle(from.yaw, to.yaw, travelT);
        float pitch = lerp(from.pitch, to.pitch, travelT);
        float roll = lerp(from.roll, to.roll, travelT);

        sendFrame(playerRef, x, y, z, yaw, pitch, roll, 0.35f, 0.35f);
    }

    private static void applyFrame(PlayerRef playerRef, CameraWaypoint waypoint,
                                   float positionLerp, float rotationLerp) {
        sendFrame(playerRef, waypoint.x, waypoint.y, waypoint.z,
            waypoint.yaw, waypoint.pitch, waypoint.roll, positionLerp, rotationLerp);
    }

    private static void sendFrame(PlayerRef playerRef, double x, double y, double z,
                                  float yaw, float pitch, float roll,
                                  float positionLerp, float rotationLerp) {
        ServerCameraSettings settings = new ServerCameraSettings();
        settings.positionLerpSpeed = positionLerp;
        settings.rotationLerpSpeed = rotationLerp;
        settings.isFirstPerson = false;
        settings.displayCursor = false;
        settings.allowPitchControls = false;
        settings.speedModifier = 0f;
        settings.positionType = PositionType.Custom;
        settings.position = new Position(x, y, z);
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(yaw, pitch, roll);

        playerRef.getPacketHandler().writeNoCache(
            new SetServerCamera(ClientCameraView.Custom, true, settings));
    }

    public static boolean isVisualizing(@Nonnull UUID uuid) {
        return visualizationSessions.containsKey(uuid);
    }

    public static boolean toggleVisualization(@Nonnull UUID uuid, @Nullable World world) {
        VisualizationSession existing = visualizationSessions.remove(uuid);
        if (existing != null) {
            if (world != null) {
                CameraPathVisualizer.clear(world);
            }
            return false;
        }
        visualizationSessions.put(uuid, new VisualizationSession());
        return true;
    }

    public static void stopVisualization(@Nonnull UUID uuid, @Nullable World world) {
        if (visualizationSessions.remove(uuid) != null && world != null) {
            CameraPathVisualizer.clear(world);
        }
    }

    public static void tickVisualization(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                         @Nonnull World world, float dt) {
        VisualizationSession session = visualizationSessions.get(uuid);
        if (session == null) {
            return;
        }
        session.timeSinceRefresh += dt;
        if (session.timeSinceRefresh < VISUALIZE_REFRESH_INTERVAL) {
            return;
        }
        session.timeSinceRefresh = 0f;

        List<CameraPath> paths = new ArrayList<>();
        if (pathStore != null) {
            paths.addAll(pathStore.allPaths());
        }
        RecordingSession recordingSession = recordingSessions.get(uuid);
        if (recordingSession != null) {
            paths.add(recordingSession.path);
        }

        var position = playerRef.getTransform().getPosition();
        CameraPathVisualizer.showNearby(world, paths, new Vector3d(position.x(), position.y(), position.z()),
            VISUALIZE_RADIUS, VISUALIZE_DISPLAY_TIME);
    }

    private static void resetCamera(PlayerRef playerRef) {
        playerRef.getPacketHandler().writeNoCache(
            new SetServerCamera(ClientCameraView.FirstPerson, false, null));
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float lerpAngle(float a, float b, float t) {
        float diff = ((b - a + 540f) % 360f) - 180f;
        return a + diff * t;
    }
}
