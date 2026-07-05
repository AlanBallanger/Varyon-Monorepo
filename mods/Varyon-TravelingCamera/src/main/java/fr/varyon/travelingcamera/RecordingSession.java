package fr.varyon.travelingcamera;

public final class RecordingSession {

    public final String name;
    public final CameraPath path;

    public RecordingSession(String name) {
        this.name = name;
        this.path = new CameraPath(name);
    }
}
