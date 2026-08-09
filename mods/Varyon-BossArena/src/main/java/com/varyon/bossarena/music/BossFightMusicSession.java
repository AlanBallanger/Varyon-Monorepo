package com.varyon.bossarena.music;

final class BossFightMusicSession {

    private final String id;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final String musicFileName;
    /** Increments each start so ForcedMusic can restart from track beginning. */
    private final long generation;

    BossFightMusicSession(
            String id,
            String worldName,
            double x,
            double y,
            double z,
            double radius,
            String musicFileName,
            long generation) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(0.0d, radius);
        this.musicFileName = musicFileName;
        this.generation = generation;
    }

    String getId() {
        return id;
    }

    String getWorldName() {
        return worldName;
    }

    String getMusicFileName() {
        return musicFileName;
    }

    long getGeneration() {
        return generation;
    }

    String ambienceAssetId() {
        return BossFightMusicIds.ambienceAssetId(musicFileName);
    }

    boolean contains(double px, double py, double pz) {
        if (radius <= 0.0d) {
            return false;
        }
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return (dx * dx) + (dy * dy) + (dz * dz) <= radius * radius;
    }

    double distanceSquared(double px, double py, double pz) {
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return (dx * dx) + (dy * dy) + (dz * dz);
    }
}
