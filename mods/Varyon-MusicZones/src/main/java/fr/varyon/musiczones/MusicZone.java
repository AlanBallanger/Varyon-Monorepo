package fr.varyon.musiczones;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Locale;

public final class MusicZone implements Comparable<MusicZone> {

    private final String id;
    private final String worldName;
    private final String group;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;
    private final String musicFileName;
    private final double volumeDb;

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
        this(id, worldName, null, minX, minY, minZ, maxX, maxY, maxZ, musicFileName, 0.0);
    }

    public MusicZone(
            String id,
            String worldName,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            String musicFileName,
            double volumeDb) {
        this(id, worldName, null, minX, minY, minZ, maxX, maxY, maxZ, musicFileName, volumeDb);
    }

    public MusicZone(
            String id,
            String worldName,
            @Nullable String group,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            String musicFileName,
            double volumeDb) {
        this.id = id;
        this.worldName = worldName;
        this.group = group == null || group.isBlank() ? null : group;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.musicFileName = musicFileName;
        this.volumeDb = volumeDb;
    }

    // Gain en décibels appliqué au MusicContainer généré : 0.0 = intensité nominale (défaut
    // vanilla), négatif = plus doux, <= -100 = muet. Le codec MusicContainer.Volume lit/écrit
    // en dB (AudioUtil.decibelsToLinearGain), pas en gain linéaire.
    public double getVolumeDb() {
        return volumeDb;
    }

    public MusicZone withVolumeDb(double newVolumeDb) {
        return new MusicZone(id, worldName, group, minX, minY, minZ, maxX, maxY, maxZ, musicFileName, newVolumeDb);
    }

    public MusicZone withGroup(@Nullable String newGroup) {
        return new MusicZone(id, worldName, newGroup, minX, minY, minZ, maxX, maxY, maxZ, musicFileName, volumeDb);
    }

    public MusicZone withId(String newId) {
        return new MusicZone(newId, worldName, group, minX, minY, minZ, maxX, maxY, maxZ, musicFileName, volumeDb);
    }

    public MusicZone withMusicFileName(String newMusicFileName) {
        return new MusicZone(id, worldName, group, minX, minY, minZ, maxX, maxY, maxZ, newMusicFileName, volumeDb);
    }

    public MusicZone withBox(double nMinX, double nMinY, double nMinZ, double nMaxX, double nMaxY, double nMaxZ) {
        double x0 = Math.min(nMinX, nMaxX);
        double x1 = Math.max(nMinX, nMaxX);
        double y0 = Math.min(nMinY, nMaxY);
        double y1 = Math.max(nMinY, nMaxY);
        double z0 = Math.min(nMinZ, nMaxZ);
        double z1 = Math.max(nMinZ, nMaxZ);
        return new MusicZone(id, worldName, group, x0, y0, z0, x1, y1, z1, musicFileName, volumeDb);
    }

    // Échelle opérateur 0–100 % -> dB perçus (courbe -20*log10). 100 -> 0 dB, 50 -> ~-6 dB,
    // 0 -> muet.
    public static double percentToDb(double percent) {
        double p = Math.max(0.0, Math.min(100.0, percent));
        if (p <= 0.0) {
            return -100.0;
        }
        return 20.0 * Math.log10(p / 100.0);
    }

    public static double dbToPercent(double db) {
        if (db <= -100.0) {
            return 0.0;
        }
        double p = 100.0 * Math.pow(10.0, db / 20.0);
        return Math.max(0.0, Math.min(100.0, p));
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    @Nullable
    public String getGroup() {
        return group;
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

    // --- Identité des assets générés -------------------------------------------------------
    // Le .ogg, le MusicContainer et l'AmbienceFX sont désormais indexés par le FICHIER SOURCE
    // (+ le volume en dB pour le MC/AmbienceFX), jamais par l'id de zone. Raisons :
    //  - le client met en cache les Common Assets (.ogg) par clé et considère qu'une clé connue
    //    n'a "pas changé" : réécrire VaryonMZ_<Zone>.ogg avec d'autres octets quand la zone
    //    change de musique laissait l'ancienne piste en cache -> zone A jouait la musique de B ;
    //  - AmbienceFX.musicContainerIndex est résolu une seule fois puis figé : garder le même id
    //    d'AmbienceFX en changeant sa cible gardait l'ancien index.
    // Avec des clés dérivées du contenu, les octets d'une clé ne changent jamais ; changer la
    // musique d'une zone ne fait que repointer vers un autre asset déjà correct.

    private String musicStemToken() {
        return MusicZone.sanitizeToken(stripOgg(musicFileName));
    }

    // Volume en centièmes de dB, encodé sans signe : "N120" = -12.0 dB, "P0" = 0 dB.
    private String volumeToken() {
        long centi = Math.round(volumeDb * 10.0);
        return (centi < 0 ? "N" : "P") + Math.abs(centi);
    }

    public String ambienceAssetId() {
        return "VaryonMZ_" + musicStemToken() + "_" + volumeToken() + "_Amb";
    }

    public String musicContainerId() {
        return "VaryonMZ_" + musicStemToken() + "_" + volumeToken() + "_MC";
    }

    public String musicOggFileName() {
        return "VaryonMZ_" + musicStemToken() + ".ogg";
    }

    public String musicCommonTrackPath() {
        return "Music/VaryonMZ/" + musicOggFileName();
    }

    private static String stripOgg(String name) {
        if (name == null) {
            return "";
        }
        String s = name.trim().replace('\\', '/');
        if (s.contains("/")) {
            s = s.substring(s.lastIndexOf('/') + 1);
        }
        return s.toLowerCase(Locale.ROOT).endsWith(".ogg") ? s.substring(0, s.length() - 4) : s;
    }

    public static String sanitizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Zone";
        }
        StringBuilder b = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                b.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            } else if (c == '_' || c == '-' || c == '.') {
                b.append('_');
                capitalizeNext = true;
            }
        }
        String s = b.toString().replaceAll("_+", "_");
        if (s.isEmpty() || s.charAt(0) == '_') {
            return "Zone_" + Integer.toHexString(raw.hashCode());
        }
        if (s.length() > 48) {
            return s.substring(0, 48);
        }
        return s;
    }

    public boolean contains(double x, double y, double z) {
        return contains(x, y, z, 0.0);
    }

    // margin > 0 élargit la zone (hystérésis de sortie : rester "dedans" tant qu'on n'a pas
    // dépassé la bordure de margin blocs, pour éviter le flicker si la position oscille pile
    // sur la limite exacte de la zone).
    public boolean contains(double x, double y, double z, double margin) {
        return x >= minX - margin
                && x <= maxX + margin
                && y >= minY - margin
                && y <= maxY + margin
                && z >= minZ - margin
                && z <= maxZ + margin;
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
