package com.varyon.varyonui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.varyon.varyonui.VaryonUIPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class AccueilShortcutConfig {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    public enum Mode {
        ALT,
        O,
        DISABLE;

        @Nonnull
        public static Mode fromKey(@Nullable String key) {
            if (key == null) {
                return defaultMode();
            }
            String k = key.trim().toLowerCase();
            return switch (k) {
                case "alt" -> ALT;
                case "o" -> O;
                case "c" -> O;
                case "disable", "desactiver", "désactiver", "aucun" -> DISABLE;
                default -> defaultMode();
            };
        }

        @Nonnull
        public static Mode defaultMode() {
            return O;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AccueilShortcutConfig instance;

    private final Path file;
    private final ConcurrentHashMap<UUID, Mode> byUuid = new ConcurrentHashMap<>();

    private AccueilShortcutConfig() {
        java.io.File df = VaryonUIPlugin.getInstance().getDataFolder();
        if (!df.exists()) {
            df.mkdirs();
        }
        this.file = df.toPath().resolve("accueil_shortcut.json");
        load();
    }

    public static synchronized AccueilShortcutConfig getInstance() {
        if (instance == null) {
            instance = new AccueilShortcutConfig();
        }
        return instance;
    }

    @Nonnull
    public Mode getMode(@Nullable UUID uuid) {
        if (uuid == null) {
            return Mode.defaultMode();
        }
        return byUuid.getOrDefault(uuid, Mode.defaultMode());
    }

    public void setMode(@Nonnull UUID uuid, @Nonnull Mode mode) {
        byUuid.put(uuid, mode);
        LOG.warning("Raccourci menu enregistre: " + uuid + " -> " + mode.name());
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> raw = GSON.fromJson(r, mapType);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, String> e : raw.entrySet()) {
                try {
                    UUID id = UUID.fromString(e.getKey());
                    Mode m = Mode.fromKey(e.getValue());
                    byUuid.put(id, m);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to load accueil_shortcut.json: " + e.getMessage());
        }
    }

    private void save() {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<UUID, Mode> e : byUuid.entrySet()) {
            out.put(e.getKey().toString(), e.getValue().name());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(out, w);
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to save accueil_shortcut.json: " + e.getMessage());
        }
    }
}
