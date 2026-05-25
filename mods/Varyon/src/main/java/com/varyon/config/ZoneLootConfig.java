package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ZoneLootConfig {
    private static final HytaleLogger LOGGER  = HytaleLogger.forEnclosingClass();
    private static final String       FILENAME = "zone_loot.toml";
    private static final String       SECTION  = "zone_loot";

    private final Map<Integer, String> itemByZone;

    public ZoneLootConfig(@Nonnull Map<Integer, String> itemByZone) {
        this.itemByZone = new HashMap<>(itemByZone);
    }

    @Nullable
    public String getItemForZone(int zoneId) {
        return itemByZone.get(zoneId);
    }

    @Nonnull
    public static ZoneLootConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            ZoneLootConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);
            Map<Integer, String> zones = new HashMap<>();
            Toml section = toml.getTable(SECTION);
            if (section != null) {
                for (Map.Entry<String, Object> e : section.entrySet()) {
                    try {
                        int zoneId = Integer.parseInt(e.getKey());
                        if (e.getValue() instanceof String s && !s.isBlank()) zones.put(zoneId, s);
                    } catch (NumberFormatException ignored) {}
                }
            }
            LOGGER.at(Level.INFO).log("Loaded zone_loot.toml: {0} zones", zones.size());
            return new ZoneLootConfig(zones);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load " + FILENAME + ", using defaults", e);
            return createDefault();
        }
    }

    public void save(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateToml());
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save " + FILENAME, e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(SECTION).append("]\n");
        for (int z = 1; z <= 10; z++) {
            String item = itemByZone.getOrDefault(z, "Key_Fragment" + z);
            sb.append(z).append(" = \"").append(item).append("\"\n");
        }
        return sb.toString();
    }

    @Nonnull
    public static ZoneLootConfig createDefault() {
        Map<Integer, String> zones = new HashMap<>();
        for (int z = 1; z <= 10; z++) zones.put(z, "Key_Fragment" + z);
        return new ZoneLootConfig(zones);
    }
}
