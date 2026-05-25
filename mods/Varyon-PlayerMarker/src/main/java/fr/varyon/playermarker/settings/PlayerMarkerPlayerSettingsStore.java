package fr.varyon.playermarker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class PlayerMarkerPlayerSettingsStore {

    private final Path playersRoot;
    private final ConcurrentMap<UUID, PlayerMarkerPlayerSettings> cache = new ConcurrentHashMap<>();

    PlayerMarkerPlayerSettingsStore(Path dataRoot) {
        this.playersRoot = dataRoot.resolve("player-settings");
    }

    void preload(UUID playerUuid) {
        if (playerUuid != null) {
            resolve(playerUuid);
        }
    }

    void unload(UUID playerUuid) {
        if (playerUuid != null) {
            cache.remove(playerUuid);
        }
    }

    PlayerMarkerPlayerSettings resolve(UUID playerUuid) {
        if (playerUuid == null) {
            return new PlayerMarkerPlayerSettings();
        }
        return cache.computeIfAbsent(playerUuid, this::load).copy();
    }

    void save(UUID playerUuid, PlayerMarkerPlayerSettings settings) {
        if (playerUuid == null || settings == null) {
            return;
        }

        PlayerMarkerPlayerSettings normalized = settings.copy().normalizeForOwner(playerUuid);
        cache.put(playerUuid, normalized);
        Path path = profilePath(playerUuid);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, encode(normalized), StandardCharsets.UTF_8);
        } catch (IOException e) {
        }
    }

    private PlayerMarkerPlayerSettings load(UUID playerUuid) {
        PlayerMarkerPlayerSettings settings = new PlayerMarkerPlayerSettings();
        Path path = profilePath(playerUuid);
        if (!Files.exists(path)) {
            return settings;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            PlayerMarkerJson parsed = PlayerMarkerJson.parseObject(json);
            settings.schemaVersion = parsed.getInt("schemaVersion", settings.schemaVersion);
            settings.mapEnabled = parsed.getBoolean("mapEnabled", settings.mapEnabled);
            settings.minimapEnabled = parsed.getBoolean("minimapEnabled", settings.minimapEnabled);
            settings.compassEnabled = parsed.getBoolean("compassEnabled", settings.compassEnabled);
            for (Map.Entry<String, String> entry : parsed.values().entrySet()) {
                String key = entry.getKey();
                if (key == null || !key.startsWith("override.")) {
                    continue;
                }

                UUID targetUuid = parseOverrideUuid(key.substring("override.".length()));
                if (targetUuid == null) {
                    continue;
                }

                Integer mask = parseOverrideMask(entry.getValue());
                if (mask != null && mask != 0) {
                    settings.playerOverrides.put(targetUuid, mask);
                }
            }
            PlayerMarkerPlayerSettings normalized = settings.normalizeForOwner(playerUuid);
            String normalizedJson = encode(normalized);
            if (!normalizedJson.equals(json)) {
                Files.writeString(path, normalizedJson, StandardCharsets.UTF_8);
            }
            return normalized;
        } catch (IOException | IllegalArgumentException e) {
            return settings.normalizeForOwner(playerUuid);
        }
    }

    private Path profilePath(UUID playerUuid) {
        return playersRoot.resolve(playerUuid + ".json");
    }

    private static String encode(PlayerMarkerPlayerSettings settings) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("schemaVersion", settings.schemaVersion);
        values.put("mapEnabled", settings.mapEnabled);
        values.put("minimapEnabled", settings.minimapEnabled);
        values.put("compassEnabled", settings.compassEnabled);
        settings.playerOverrides.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put("override." + entry.getKey(), entry.getValue()));
        return PlayerMarkerJson.writeObject(values);
    }

    private static UUID parseOverrideUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Integer parseOverrideMask(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}