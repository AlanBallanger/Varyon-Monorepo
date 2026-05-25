package com.varyon.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlacedOreTracker {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILE_NAME = "placed_ores.json";

    private final Set<String> placedPositions = ConcurrentHashMap.newKeySet();
    private final Path dataDirectory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PlacedOreTracker(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        load();
    }

    public void add(@Nonnull String world, @Nonnull Vector3i pos) {
        String k = key(world, pos);
        boolean added = placedPositions.add(k);
        LOGGER.at(Level.INFO).log("[Tracker.add] key=" + k + " added=" + added + " setSize=" + placedPositions.size());
        if (added) {
            save();
        }
    }

    public void remove(@Nonnull String world, @Nonnull Vector3i pos) {
        String k = key(world, pos);
        boolean removed = placedPositions.remove(k);
        LOGGER.at(Level.INFO).log("[Tracker.remove] key=" + k + " removed=" + removed + " setSize=" + placedPositions.size());
        if (removed) {
            save();
        }
    }

    public boolean isPlayerPlaced(@Nonnull String world, @Nonnull Vector3i pos) {
        String k = key(world, pos);
        boolean found = placedPositions.contains(k);
        LOGGER.at(Level.INFO).log("[Tracker.isPlayerPlaced] key=" + k + " found=" + found + " setSize=" + placedPositions.size());
        return found;
    }

    private String key(@Nonnull String world, @Nonnull Vector3i pos) {
        return world + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    private void load() {
        File file = dataDirectory.resolve(FILE_NAME).toFile();
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Set<String> loaded = gson.fromJson(reader, new TypeToken<Set<String>>() {}.getType());
            if (loaded != null) {
                placedPositions.addAll(loaded);
                LOGGER.at(Level.INFO).log("Loaded " + placedPositions.size() + " placed ore position(s)");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load placed ores: " + e.getMessage());
        }
    }

    private void save() {
        try {
            dataDirectory.toFile().mkdirs();
            File file = dataDirectory.resolve(FILE_NAME).toFile();
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(new HashSet<>(placedPositions), writer);
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to save placed ores: " + e.getMessage());
        }
    }
}
