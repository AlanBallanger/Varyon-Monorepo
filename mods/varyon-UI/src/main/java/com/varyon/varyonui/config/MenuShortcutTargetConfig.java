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

public final class MenuShortcutTargetConfig {

    public enum Target {
        ACCUEIL("accueil"),
        COMMANDES("commands"),
        VARYON("var");

        private final String commandName;

        Target(@Nonnull String commandName) {
            this.commandName = commandName;
        }

        @Nonnull
        public String getCommandName() {
            return commandName;
        }

        @Nonnull
        public static Target fromKey(@Nullable String key) {
            if (key == null) {
                return defaultTarget();
            }
            String k = key.trim().toLowerCase();
            return switch (k) {
                case "accueil", "home" -> ACCUEIL;
                case "commandes", "commands", "command" -> COMMANDES;
                case "varyon" -> VARYON;
                default -> defaultTarget();
            };
        }

        @Nonnull
        public static Target defaultTarget() {
            return ACCUEIL;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MenuShortcutTargetConfig instance;

    private final Path file;
    private final ConcurrentHashMap<UUID, Target> byUuid = new ConcurrentHashMap<>();

    private MenuShortcutTargetConfig() {
        java.io.File df = VaryonUIPlugin.getInstance().getDataFolder();
        if (!df.exists()) {
            df.mkdirs();
        }
        this.file = df.toPath().resolve("menu_shortcut_target.json");
        load();
    }

    public static synchronized MenuShortcutTargetConfig getInstance() {
        if (instance == null) {
            instance = new MenuShortcutTargetConfig();
        }
        return instance;
    }

    @Nonnull
    public Target getTarget(@Nullable UUID uuid) {
        if (uuid == null) {
            return Target.defaultTarget();
        }
        return byUuid.getOrDefault(uuid, Target.defaultTarget());
    }

    @Nonnull
    public String getCommandName(@Nullable UUID uuid) {
        return getTarget(uuid).getCommandName();
    }

    public void setTarget(@Nonnull UUID uuid, @Nonnull Target target) {
        byUuid.put(uuid, target);
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
                    Target t = Target.fromKey(e.getValue());
                    byUuid.put(id, t);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to load menu_shortcut_target.json: " + e.getMessage());
        }
    }

    private void save() {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<UUID, Target> e : byUuid.entrySet()) {
            out.put(e.getKey().toString(), e.getValue().name());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(out, w);
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to save menu_shortcut_target.json: " + e.getMessage());
        }
    }
}
