package fr.varyon.travelingcamera;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CameraPathStore {

    private static final HytaleLogger LOGGER =
        HytaleLogger.getLogger().getSubLogger("VaryonTravelingCamera-Store");
    private static final String PATHS_DIR = "paths";

    private static final Pattern PATH_BLOCK =
        Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"loop\"\\s*:\\s*(true|false)\\s*,\\s*\"waypoints\"\\s*:\\s*\\[(.*?)]\\s*}",
            Pattern.DOTALL);
    private static final Pattern WAYPOINT_ENTRY = Pattern.compile(
        "\\{\\s*\"x\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*\"y\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*\"z\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*"
            + "\"yaw\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*\"pitch\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*\"roll\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*"
            + "\"holdSeconds\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*,\\s*\"travelSeconds\"\\s*:\\s*(-?[0-9.eE+-]+)\\s*}");

    private final Path dataDirectory;
    private final Path pathsDirectory;
    private final Map<String, CameraPath> cache = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VaryonTravelingCamera-IO");
        t.setDaemon(true);
        return t;
    });

    public CameraPathStore(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.pathsDirectory = dataDirectory.resolve(PATHS_DIR);
    }

    public void initialize() {
        try {
            Files.createDirectories(pathsDirectory);
            try (var stream = Files.list(pathsDirectory)) {
                for (Path file : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                    try {
                        CameraPath path = parse(Files.readString(file, StandardCharsets.UTF_8));
                        if (path != null) {
                            cache.put(path.name.toLowerCase(Locale.ROOT), path);
                        }
                    } catch (IOException e) {
                        LOGGER.at(Level.WARNING).log("Failed to read %s: %s", file, e.getMessage());
                    }
                }
            }
            LOGGER.at(Level.INFO).log("Loaded %d camera path(s)", cache.size());
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to init camera path store: %s", e.getMessage());
        }
    }

    @Nullable
    public CameraPath get(@Nonnull String name) {
        return cache.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(@Nonnull String name) {
        return cache.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public List<String> listNames() {
        return new TreeMap<>(cache).values().stream().map(p -> p.name).toList();
    }

    public List<CameraPath> allPaths() {
        return List.copyOf(cache.values());
    }

    public void save(@Nonnull CameraPath path) {
        cache.put(path.name.toLowerCase(Locale.ROOT), path);
        CompletableFuture.runAsync(() -> writeToDisk(path), ioExecutor);
    }

    public boolean delete(@Nonnull String name) {
        CameraPath removed = cache.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(fileFor(removed.name));
            } catch (IOException e) {
                LOGGER.at(Level.WARNING).log("Failed to delete path %s: %s", removed.name, e.getMessage());
            }
        }, ioExecutor);
        return true;
    }

    private Path fileFor(String name) {
        String safe = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        return pathsDirectory.resolve(safe + ".json");
    }

    private void writeToDisk(CameraPath path) {
        try {
            Files.createDirectories(pathsDirectory);
            Files.writeString(fileFor(path.name), toJson(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to save path %s: %s", path.name, e.getMessage());
        }
    }

    private static String toJson(CameraPath path) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"name\": \"").append(path.name).append("\",\n");
        out.append("  \"loop\": ").append(path.loop).append(",\n");
        out.append("  \"waypoints\": [\n");
        for (int i = 0; i < path.waypoints.size(); i++) {
            CameraWaypoint w = path.waypoints.get(i);
            out.append("    {\"x\": ").append(w.x)
                .append(", \"y\": ").append(w.y)
                .append(", \"z\": ").append(w.z)
                .append(", \"yaw\": ").append(w.yaw)
                .append(", \"pitch\": ").append(w.pitch)
                .append(", \"roll\": ").append(w.roll)
                .append(", \"holdSeconds\": ").append(w.holdSeconds)
                .append(", \"travelSeconds\": ").append(w.travelSeconds)
                .append("}");
            if (i + 1 < path.waypoints.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  ]\n}\n");
        return out.toString();
    }

    @Nullable
    private static CameraPath parse(String json) {
        Matcher blockMatcher = PATH_BLOCK.matcher(json);
        if (!blockMatcher.find()) {
            return null;
        }
        CameraPath path = new CameraPath(blockMatcher.group(1));
        path.loop = Boolean.parseBoolean(blockMatcher.group(2));
        Matcher waypointMatcher = WAYPOINT_ENTRY.matcher(blockMatcher.group(3));
        while (waypointMatcher.find()) {
            path.waypoints.add(new CameraWaypoint(
                Double.parseDouble(waypointMatcher.group(1)),
                Double.parseDouble(waypointMatcher.group(2)),
                Double.parseDouble(waypointMatcher.group(3)),
                Float.parseFloat(waypointMatcher.group(4)),
                Float.parseFloat(waypointMatcher.group(5)),
                Float.parseFloat(waypointMatcher.group(6)),
                Float.parseFloat(waypointMatcher.group(7)),
                Float.parseFloat(waypointMatcher.group(8))
            ));
        }
        return path;
    }
}
