package fr.varyon.travelingcamera;

import java.util.ArrayList;
import java.util.List;

public final class CameraPath {

    public final String name;
    public final List<CameraWaypoint> waypoints = new ArrayList<>();
    public boolean loop;

    public CameraPath(String name) {
        this.name = name;
    }

    public float totalSeconds() {
        float total = 0f;
        for (CameraWaypoint waypoint : waypoints) {
            total += waypoint.travelSeconds + waypoint.holdSeconds;
        }
        return total;
    }
}
