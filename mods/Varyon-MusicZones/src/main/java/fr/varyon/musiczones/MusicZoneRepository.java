package fr.varyon.musiczones;

import com.hypixel.hytale.logger.HytaleLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MusicZoneRepository {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILE_NAME = "zones.json";
    private static final Pattern ZONE_BLOCK = Pattern.compile(
            "\\{\\s*\"id\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"\\s*,\\s*\"worldName\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"\\s*,"
                    + "\\s*\"minX\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"minY\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"minZ\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"maxX\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"maxY\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"maxZ\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,"
                    + "\\s*\"musicFileName\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"\\s*\\}",
            Pattern.DOTALL);

    private final Path dataDir;
    private final List<MusicZone> zones = new ArrayList<>();

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

    public void addOrReplace(MusicZone zone) {
        synchronized (zones) {
            zones.removeIf(z -> z.getWorldName().equals(zone.getWorldName()) && z.getId().equals(zone.getId()));
            zones.add(zone);
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

    MusicZone find(String worldName, String id) {
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
            synchronized (zones) {
                zones.clear();
                zones.addAll(loaded);
                Collections.sort(zones);
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] load failed");
        }
    }

    private static MusicZone tryParseBlock(Matcher m) {
        try {
            String id = unescape(m.group(1));
            String worldName = unescape(m.group(2));
            double minX = Double.parseDouble(m.group(3));
            double minY = Double.parseDouble(m.group(4));
            double minZ = Double.parseDouble(m.group(5));
            double maxX = Double.parseDouble(m.group(6));
            double maxY = Double.parseDouble(m.group(7));
            double maxZ = Double.parseDouble(m.group(8));
            String musicFileName = unescape(m.group(9));
            return new MusicZone(id, worldName, minX, minY, minZ, maxX, maxY, maxZ, musicFileName);
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
            }
            sb.append("\n  ]\n}\n");
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] save failed");
        }
    }

    private static String toJsonObject(MusicZone z) {
        return "{ \"id\": \""
                + escape(z.getId())
                + "\", \"worldName\": \""
                + escape(z.getWorldName())
                + "\", \"minX\": "
                + z.getMinX()
                + ", \"minY\": "
                + z.getMinY()
                + ", \"minZ\": "
                + z.getMinZ()
                + ", \"maxX\": "
                + z.getMaxX()
                + ", \"maxY\": "
                + z.getMaxY()
                + ", \"maxZ\": "
                + z.getMaxZ()
                + ", \"musicFileName\": \""
                + escape(z.getMusicFileName())
                + "\" }";
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
