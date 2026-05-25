package fr.varyon.musiczones;

import java.util.Comparator;

public final class MusicZone implements Comparable<MusicZone> {

    private final String id;
    private final String worldName;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;
    private final String musicFileName;

    public MusicZone(
            String id,
            String worldName,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            String musicFileName) {
        this.id = id;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.musicFileName = musicFileName;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public String getMusicFileName() {
        return musicFileName;
    }

    public String sanitizedId() {
        return MusicZone.sanitizeToken(id);
    }

    public String ambienceAssetId() {
        return "VaryonMZ_" + sanitizedId() + "_Amb";
    }

    public String musicOggFileName() {
        return "VaryonMZ_" + sanitizedId() + ".ogg";
    }

    public String musicCommonTrackPath() {
        return "Music/VaryonMZ/" + musicOggFileName();
    }

    public static String sanitizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "zone";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                b.append(Character.toLowerCase(c));
            } else if (c == '_' || c == '-' || c == '.') {
                b.append('_');
            }
        }
        String s = b.toString().replaceAll("_+", "_");
        if (s.isEmpty() || s.charAt(0) == '_') {
            return "zone_" + Integer.toHexString(raw.hashCode());
        }
        if (s.length() > 48) {
            return s.substring(0, 48);
        }
        return s;
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX
                && x <= maxX
                && y >= minY
                && y <= maxY
                && z >= minZ
                && z <= maxZ;
    }

    public double volume() {
        return Math.abs((maxX - minX) * (maxY - minY) * (maxZ - minZ));
    }

    @Override
    public int compareTo(MusicZone o) {
        return Comparator.comparing(MusicZone::getWorldName)
                .thenComparing(MusicZone::getId)
                .compare(this, o);
    }
}
