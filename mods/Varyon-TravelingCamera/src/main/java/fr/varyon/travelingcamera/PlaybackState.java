package fr.varyon.travelingcamera;

public final class PlaybackState {

    public final CameraPath path;
    public int segmentIndex;
    public float segmentElapsed;
    public boolean restoreOnFinish;
    public volatile boolean speedBoosted;
    public volatile boolean lastReportedMoving;

    public PlaybackState(CameraPath path) {
        this.path = path;
        this.segmentIndex = 0;
        this.segmentElapsed = 0f;
        this.restoreOnFinish = true;
        this.speedBoosted = false;
        this.lastReportedMoving = false;
    }

    public boolean isFinished() {
        return segmentIndex >= path.waypoints.size();
    }
}
