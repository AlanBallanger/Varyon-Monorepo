package com.varyon.arena;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ArenaManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ARENAS_FILE = "arenas.toml";
    private static final String SECTION = "arenas";
    public static final double DEFAULT_RADIUS = 50.0;

    private final Path dataDirectory;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

    public static class Arena {
        private final String name;
        private final String world;
        private final double x;
        private final double y;
        private final double z;
        private final double radius;

        public Arena(String name, String world, double x, double y, double z, double radius) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }

        public String getName()   { return name; }
        public String getWorld()  { return world; }
        public double getX()      { return x; }
        public double getY()      { return y; }
        public double getZ()      { return z; }
        public double getRadius() { return radius; }

        public boolean contains(String world, double x, double z) {
            if (!this.world.equals(world)) {
                return false;
            }
            double dx = x - this.x;
            double dz = z - this.z;
            return (dx * dx + dz * dz) <= radius * radius;
        }
    }

    public ArenaManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        load();
    }

    public boolean addArena(@Nonnull String name, @Nonnull String world, double x, double y, double z) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) {
            return false;
        }
        arenas.put(key, new Arena(name, world, x, y, z, DEFAULT_RADIUS));
        save();
        LOGGER.at(Level.INFO).log("Added arena '" + name + "' at " + world + ":" + (int) x + "," + (int) y + "," + (int) z);
        return true;
    }

    public boolean removeArena(@Nonnull String name) {
        boolean removed = arenas.remove(name.toLowerCase()) != null;
        if (removed) {
            save();
            LOGGER.at(Level.INFO).log("Removed arena '" + name + "'");
        }
        return removed;
    }

    public boolean removeArenaAt(@Nonnull String world, double x, double z) {
        Arena found = findArenaAt(world, x, z);
        if (found == null) {
            return false;
        }
        return removeArena(found.getName());
    }

    @Nullable
    public Arena findArenaAt(@Nonnull String world, double x, double z) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(world, x, z)) {
                return arena;
            }
        }
        return null;
    }

    @Nullable
    public Arena getArena(@Nonnull String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    private void load() {
        File file = dataDirectory.resolve(ARENAS_FILE).toFile();
        if (!file.exists()) {
            LOGGER.at(Level.INFO).log("No arenas file found, starting fresh");
            return;
        }
        try {
            Toml toml = new Toml().read(file);
            Toml arenasTable = toml.getTable(SECTION);
            if (arenasTable == null) return;

            for (Map.Entry<String, Object> entry : arenasTable.entrySet()) {
                String key = entry.getKey();
                Toml sub = arenasTable.getTable(key);
                if (sub == null) continue;
                String name   = sub.getString("name", key);
                String world  = sub.getString("world", "default");
                double x      = sub.getDouble("x", 0.0);
                double y      = sub.getDouble("y", 64.0);
                double z      = sub.getDouble("z", 0.0);
                double radius = sub.getDouble("radius", DEFAULT_RADIUS);
                arenas.put(key, new Arena(name, world, x, y, z, radius));
            }
            LOGGER.at(Level.INFO).log("Loaded " + arenas.size() + " arena(s)");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load arenas: " + e.getMessage());
        }
    }

    private void save() {
        try {
            dataDirectory.toFile().mkdirs();
            File file = dataDirectory.resolve(ARENAS_FILE).toFile();
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(SECTION).append("]\n\n");
            for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
                Arena a = entry.getValue();
                sb.append("[").append(SECTION).append(".\"").append(entry.getKey()).append("\"]\n");
                sb.append("name   = \"").append(a.getName()).append("\"\n");
                sb.append("world  = \"").append(a.getWorld()).append("\"\n");
                sb.append("x      = ").append(a.getX()).append("\n");
                sb.append("y      = ").append(a.getY()).append("\n");
                sb.append("z      = ").append(a.getZ()).append("\n");
                sb.append("radius = ").append(a.getRadius()).append("\n\n");
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(sb.toString());
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save arenas: " + e.getMessage());
        }
    }
}
