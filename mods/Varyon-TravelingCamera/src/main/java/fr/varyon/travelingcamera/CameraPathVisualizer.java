package fr.varyon.travelingcamera;

import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;

import org.joml.Vector3d;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;

public final class CameraPathVisualizer {

    private static final Vector3f WAYPOINT_COLOR = DebugUtils.COLOR_YELLOW;
    private static final Vector3f FIRST_WAYPOINT_COLOR = DebugUtils.COLOR_LIME;
    private static final Vector3f PATH_COLOR = DebugUtils.COLOR_CYAN;
    private static final double WAYPOINT_RADIUS = 0.35;
    private static final double LINE_THICKNESS = 0.05;

    private CameraPathVisualizer() {}

    public static void showNearby(@Nonnull World world, @Nonnull List<CameraPath> paths,
                                  @Nonnull Vector3d center, double radius, float durationSeconds) {
        double radiusSquared = radius * radius;
        for (CameraPath path : paths) {
            for (int i = 0; i < path.waypoints.size(); i++) {
                CameraWaypoint waypoint = path.waypoints.get(i);
                Vector3d position = new Vector3d(waypoint.x, waypoint.y, waypoint.z);
                boolean thisNear = position.distanceSquared(center) <= radiusSquared;

                boolean hasNext = i + 1 < path.waypoints.size();
                boolean wrapsToFirst = !hasNext && path.loop && path.waypoints.size() > 1;
                CameraWaypoint next = hasNext ? path.waypoints.get(i + 1)
                    : wrapsToFirst ? path.waypoints.get(0) : null;
                Vector3d nextPosition = next != null ? new Vector3d(next.x, next.y, next.z) : null;
                boolean nextNear = nextPosition != null && nextPosition.distanceSquared(center) <= radiusSquared;

                if (thisNear) {
                    Vector3f color = i == 0 ? FIRST_WAYPOINT_COLOR : WAYPOINT_COLOR;
                    DebugUtils.addSphere(world, position, color, WAYPOINT_RADIUS, durationSeconds);
                }
                if (nextPosition != null && (thisNear || nextNear)) {
                    DebugUtils.addLine(world, position, nextPosition,
                        PATH_COLOR, LINE_THICKNESS, durationSeconds, DebugUtils.FLAG_NONE);
                }
            }
        }
    }

    public static void clear(@Nonnull World world) {
        DebugUtils.clear(world);
    }
}
