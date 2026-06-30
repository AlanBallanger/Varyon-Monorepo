package fr.varyon.damagenumber;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DamageNumberDisplaySettingsManager {

    private static final HytaleLogger LOGGER =
        HytaleLogger.getLogger().getSubLogger("VaryonDamageNumber-Prefs");
    private static final String CONFIG_FILE = "config.json";
    private static final String LEGACY_FILE = "damage-number-display.txt";
    private static final Pattern JSON_PLAYER_ENTRY =
        Pattern.compile("\"([0-9a-fA-F-]{36})\"\\s*:\\s*(true|false)");

    private final Path dataDirectory;
    private final Path configPath;
    private final ConcurrentHashMap<UUID, Boolean> cache = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VaryonDamageNumber-Prefs-IO");
        t.setDaemon(true);
        return t;
    });

    public DamageNumberDisplaySettingsManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve(CONFIG_FILE);
    }

    public void initialize() {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.isRegularFile(configPath)) {
                loadFromJson(Files.readString(configPath, StandardCharsets.UTF_8));
            } else {
                Path legacyPath = dataDirectory.resolve(LEGACY_FILE);
                if (Files.isRegularFile(legacyPath)) {
                    loadFromLegacyTxt(Files.readAllLines(legacyPath, StandardCharsets.UTF_8));
                    scheduleSave();
                    LOGGER.at(Level.INFO).log("Migrated legacy prefs to %s", CONFIG_FILE);
                }
            }
            LOGGER.at(Level.INFO).log("Damage number display prefs loaded (%d entries)", cache.size());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to load damage number display prefs: %s", e.getMessage());
        }
    }

    public void ensureLoaded(@Nonnull UUID uuid) {
        cache.computeIfAbsent(uuid, ignored -> Boolean.TRUE);
    }

    public boolean isEnabled(@Nonnull UUID uuid) {
        return cache.getOrDefault(uuid, Boolean.TRUE);
    }

    public void setEnabled(@Nonnull UUID uuid, boolean enabled) {
        cache.put(uuid, enabled);
        scheduleSave();
    }

    public boolean toggle(@Nonnull UUID uuid) {
        boolean next = !isEnabled(uuid);
        setEnabled(uuid, next);
        return next;
    }

    public void flush() {
        saveNow();
    }

    private void scheduleSave() {
        CompletableFuture.runAsync(this::saveNow, ioExecutor);
    }

    private void loadFromJson(@Nonnull String json) {
        Matcher matcher = JSON_PLAYER_ENTRY.matcher(json);
        while (matcher.find()) {
            try {
                UUID uuid = UUID.fromString(matcher.group(1));
                boolean enabled = Boolean.parseBoolean(matcher.group(2));
                cache.put(uuid, enabled);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void loadFromLegacyTxt(@Nonnull List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int sep = trimmed.indexOf('=');
            if (sep <= 0) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(trimmed.substring(0, sep).trim());
                boolean enabled = Boolean.parseBoolean(trimmed.substring(sep + 1).trim());
                cache.put(uuid, enabled);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveNow() {
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(configPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to save damage number display prefs: %s", e.getMessage());
        }
    }

    private String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"displayEnabledByPlayer\": {\n");
        List<Map.Entry<UUID, Boolean>> entries = cache.entrySet().stream()
            .sorted((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()))
            .toList();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<UUID, Boolean> entry = entries.get(i);
            out.append("    \"")
                .append(entry.getKey())
                .append("\": ")
                .append(entry.getValue());
            if (i + 1 < entries.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  }\n}\n");
        return out.toString();
    }
}
