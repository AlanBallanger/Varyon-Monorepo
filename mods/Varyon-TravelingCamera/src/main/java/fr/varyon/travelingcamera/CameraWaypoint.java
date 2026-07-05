package fr.varyon.travelingcamera;

public final class CameraWaypoint {

    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final float roll;
    public final float holdSeconds;
    public final float travelSeconds;

    public CameraWaypoint(double x, double y, double z,
                          float yaw, float pitch, float roll,
                          float holdSeconds, float travelSeconds) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.holdSeconds = holdSeconds;
        this.travelSeconds = travelSeconds;
    }
}
