package fr.varyon.musiczones;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MusicZoneRepository {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILE_NAME = "zones.json";
    private static final Pattern ZONE_BLOCK = Pattern.compile(
            "\\{\\s*\"id\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"\\s*,\\s*\"worldName\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"\\s*,"
                    + "(?:\\s*\"group\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"\\s*,)?"
                    + "\\s*\"minX\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"minY\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"minZ\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"maxX\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"maxY\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"maxZ\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"musicFileName\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\""
                    + "(?:\\s*,\\s*\"volumeDb\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?))?"
                    + "\\s*\\}",
            Pattern.DOTALL);
    private static final Pattern GROUP_BLOCK = Pattern.compile("\"((?:\\\\.|[^\"])*)\"");

    private final Path dataDir;
    private final List<MusicZone> zones = new ArrayList<>();
    // Dossiers cosmétiques sans zone (créés dans l'UI avant d'y ranger une zone).
    private final Set<String> emptyGroups = new LinkedHashSet<>();

    MusicZoneRepository(Path dataDir) {
        this.dataDir = dataDir;
    }

    public List<MusicZone> getZonesReadOnly() {
        synchronized (zones) {
            return List.copyOf(zones);
        }
    }

    public List<MusicZone> zonesForWorld(String worldName) {
        if (worldName == null) {
            return List.of();
        }
        synchronized (zones) {
            return zones.stream()
                    .filter(z -> worldName.equals(z.getWorldName()))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    // Tous les chemins de groupe connus (portés par une zone ou créés vides), triés,
    // insensible à la casse. Utilisé pour la navigation dossier de l'UI.
    public List<String> getSortedGroupPaths() {
        TreeSet<String> paths = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        synchronized (zones) {
            for (MusicZone z : zones) {
                if (z.getGroup() != null) {
                    paths.add(z.getGroup());
                }
            }
            paths.addAll(emptyGroups);
        }
        return new ArrayList<>(paths);
    }

    public void addEmptyGroup(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        synchronized (zones) {
            emptyGroups.add(path);
        }
    }

    public void addOrReplace(MusicZone zone) {
        synchronized (zones) {
            zones.removeIf(z -> z.getWorldName().equals(zone.getWorldName()) && z.getId().equals(zone.getId()));
            zones.add(zone);
            if (zone.getGroup() != null) {
                emptyGroups.remove(zone.getGroup());
            }
            Collections.sort(zones);
        }
    }

    public boolean remove(String worldName, String id) {
        synchronized (zones) {
            boolean removed = zones.removeIf(z -> worldName.equals(z.getWorldName()) && id.equals(z.getId()));
            if (removed) {
                Collections.sort(zones);
            }
            return removed;
        }
    }

    public boolean rename(String worldName, String oldId, String newId) {
        synchronized (zones) {
            if (oldId.equals(newId)) {
                return true;
            }
            boolean clash = zones.stream()
                    .anyMatch(z -> worldName.equals(z.getWorldName()) && newId.equals(z.getId()));
            if (clash) {
                return false;
            }
            MusicZone existing = find(worldName, oldId);
            if (existing == null) {
                return false;
            }
            zones.remove(existing);
            zones.add(existing.withId(newId));
            Collections.sort(zones);
            return true;
        }
    }

    @Nullable
    public MusicZone find(String worldName, String id) {
        synchronized (zones) {
            for (MusicZone z : zones) {
                if (worldName.equals(z.getWorldName()) && id.equals(z.getId())) {
                    return z;
                }
            }
            return null;
        }
    }

    public void load() {
        Path path = dataDir.resolve(FILE_NAME);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            ArrayList<MusicZone> loaded = new ArrayList<>();
            Matcher m = ZONE_BLOCK.matcher(json);
            while (m.find()) {
                MusicZone z = tryParseBlock(m);
                if (z != null) {
                    loaded.add(z);
                }
            }
            LinkedHashSet<String> loadedGroups = new LinkedHashSet<>();
            int groupsIdx = json.indexOf("\"groups\"");
            if (groupsIdx >= 0) {
                int open = json.indexOf('[', groupsIdx);
                int close = open >= 0 ? json.indexOf(']', open) : -1;
                if (open >= 0 && close > open) {
                    Matcher gm = GROUP_BLOCK.matcher(json.substring(open + 1, close));
                    while (gm.find()) {
                        String g = unescape(gm.group(1));
                        if (!g.isBlank()) {
                            loadedGroups.add(g);
                        }
                    }
                }
            }
            synchronized (zones) {
                zones.clear();
                zones.addAll(loaded);
                Collections.sort(zones);
                emptyGroups.clear();
                emptyGroups.addAll(loadedGroups);
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] load failed");
        }
    }

    @Nullable
    private static MusicZone tryParseBlock(Matcher m) {
        try {
            String id = unescape(m.group(1));
            String worldName = unescape(m.group(2));
            String group = m.group(3) != null ? unescape(m.group(3)) : null;
            double minX = Double.parseDouble(m.group(4));
            double minY = Double.parseDouble(m.group(5));
            double minZ = Double.parseDouble(m.group(6));
            double maxX = Double.parseDouble(m.group(7));
            double maxY = Double.parseDouble(m.group(8));
            double maxZ = Double.parseDouble(m.group(9));
            String musicFileName = unescape(m.group(10));
            double volumeDb = m.group(11) != null ? Double.parseDouble(m.group(11)) : 0.0;
            return new MusicZone(id, worldName, group, minX, minY, minZ, maxX, maxY, maxZ, musicFileName, volumeDb);
        } catch (Exception e) {
            return null;
        }
    }

    public void save() {
        Path path = dataDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(dataDir);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"zones\": [\n");
            synchronized (zones) {
                for (int i = 0; i < zones.size(); i++) {
                    if (i > 0) {
                        sb.append(",\n");
                    }
                    sb.append("    ");
                    sb.append(toJsonObject(zones.get(i)));
                }
                sb.append("\n  ],\n  \"groups\": [");
                int gi = 0;
                for (String g : emptyGroups) {
                    if (gi++ > 0) {
                        sb.append(", ");
                    }
                    sb.append('"').append(escape(g)).append('"');
                }
                sb.append("]\n}\n");
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] save failed");
        }
    }

    private static String toJsonObject(MusicZone z) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"id\": \"").append(escape(z.getId())).append('"');
        sb.append(", \"worldName\": \"").append(escape(z.getWorldName())).append('"');
        if (z.getGroup() != null) {
            sb.append(", \"group\": \"").append(escape(z.getGroup())).append('"');
        }
        sb.append(", \"minX\": ").append(z.getMinX());
        sb.append(", \"minY\": ").append(z.getMinY());
        sb.append(", \"minZ\": ").append(z.getMinZ());
        sb.append(", \"maxX\": ").append(z.getMaxX());
        sb.append(", \"maxY\": ").append(z.getMaxY());
        sb.append(", \"maxZ\": ").append(z.getMaxZ());
        sb.append(", \"musicFileName\": \"").append(escape(z.getMusicFileName())).append('"');
        sb.append(", \"volumeDb\": ").append(z.getVolumeDb());
        sb.append(" }");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String unescape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    public Path getMusicDirectory() {
        return dataDir.resolve("music");
    }
}
