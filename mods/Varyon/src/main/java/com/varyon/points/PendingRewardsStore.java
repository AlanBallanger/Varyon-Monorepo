package com.varyon.points;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persists fragment rewards for players who were offline when a faction tier was triggered.
 * Format (pending_rewards.toml):
 *   ["uuid"] = fragmentCount
 */
public class PendingRewardsStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "pending_rewards.toml";

    private final Path dataFolder;
    private final Map<UUID, Integer> pending = new ConcurrentHashMap<>();

    public PendingRewardsStore(@Nonnull Path dataFolder) {
        this.dataFolder = dataFolder;
        load();
    }

    public void add(@Nonnull UUID uuid, int fragments) {
        pending.merge(uuid, fragments, Integer::sum);
        save();
    }

    public int get(@Nonnull UUID uuid) {
        return pending.getOrDefault(uuid, 0);
    }

    public boolean has(@Nonnull UUID uuid) {
        return pending.containsKey(uuid) && pending.get(uuid) > 0;
    }

    public void clear(@Nonnull UUID uuid) {
        pending.remove(uuid);
        save();
    }

    private void load() {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) return;
        try {
            Toml toml = new Toml().read(file);
            for (Map.Entry<String, Object> entry : toml.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Object val = entry.getValue();
                    if (val instanceof Long)   pending.put(uuid, ((Long) val).intValue());
                    if (val instanceof Double) pending.put(uuid, ((Double) val).intValue());
                } catch (IllegalArgumentException ignored) {}
            }
            LOGGER.at(Level.INFO).log("Loaded " + pending.size() + " pending reward(s)");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load pending_rewards.toml: " + e.getMessage());
        }
    }

    private void save() {
        try {
            dataFolder.toFile().mkdirs();
            File file = dataFolder.resolve(FILENAME).toFile();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<UUID, Integer> entry : pending.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\" = ").append(entry.getValue()).append("\n");
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(sb.toString());
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save pending_rewards.toml: " + e.getMessage());
        }
    }
}
