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
import java.util.concurrent.atomic.AtomicLong;

public final class ActuStateConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ActuStateConfig instance;

    private final Path versionFile;
    private final Path playersFile;
    private final AtomicLong currentVersion = new AtomicLong(0L);
    private final ConcurrentHashMap<UUID, Long> lastSeenVersion = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> wantsPopup = new ConcurrentHashMap<>();

    private ActuStateConfig() {
        java.io.File df = VaryonUIPlugin.getInstance().getDataFolder();
        if (!df.exists()) {
            df.mkdirs();
        }
        this.versionFile = df.toPath().resolve("actu_version.json");
        this.playersFile = df.toPath().resolve("actu_players.json");
        loadVersion();
        loadPlayers();
    }

    public static synchronized ActuStateConfig getInstance() {
        if (instance == null) {
            instance = new ActuStateConfig();
        }
        return instance;
    }

    public long getCurrentVersion() {
        return currentVersion.get();
    }

    public long bumpVersion() {
        long newVersion = System.currentTimeMillis();
        currentVersion.set(newVersion);
        saveVersion();
        return newVersion;
    }

    public boolean wantsPopup(@Nonnull UUID uuid) {
        return wantsPopup.getOrDefault(uuid, true);
    }

    public void setWantsPopup(@Nonnull UUID uuid, boolean value) {
        wantsPopup.put(uuid, value);
        savePlayers();
    }

    public boolean hasUnseenUpdate(@Nonnull UUID uuid) {
        return lastSeenVersion.getOrDefault(uuid, 0L) < currentVersion.get();
    }

    public void markSeen(@Nonnull UUID uuid) {
        lastSeenVersion.put(uuid, currentVersion.get());
        savePlayers();
    }

    private void loadVersion() {
        if (!Files.isRegularFile(versionFile)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(versionFile)) {
            VersionData data = GSON.fromJson(r, VersionData.class);
            if (data != null) {
                currentVersion.set(data.version);
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to load actu_version.json: " + e.getMessage());
        }
    }

    private void saveVersion() {
        VersionData data = new VersionData();
        data.version = currentVersion.get();
        try {
            Files.createDirectories(versionFile.getParent());
            try (Writer w = Files.newBufferedWriter(versionFile)) {
                GSON.toJson(data, w);
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to save actu_version.json: " + e.getMessage());
        }
    }

    private void loadPlayers() {
        if (!Files.isRegularFile(playersFile)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(playersFile)) {
            Type mapType = new TypeToken<Map<String, PlayerData>>() {}.getType();
            Map<String, PlayerData> raw = GSON.fromJson(r, mapType);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, PlayerData> e : raw.entrySet()) {
                try {
                    UUID id = UUID.fromString(e.getKey());
                    PlayerData pd = e.getValue();
                    if (pd == null) {
                        continue;
                    }
                    lastSeenVersion.put(id, pd.lastSeenVersion);
                    wantsPopup.put(id, pd.wantsPopup);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to load actu_players.json: " + e.getMessage());
        }
    }

    private void savePlayers() {
        Map<String, PlayerData> out = new HashMap<>();
        for (UUID uuid : keySetUnion()) {
            PlayerData pd = new PlayerData();
            pd.lastSeenVersion = lastSeenVersion.getOrDefault(uuid, 0L);
            pd.wantsPopup = wantsPopup.getOrDefault(uuid, true);
            out.put(uuid.toString(), pd);
        }
        try {
            Files.createDirectories(playersFile.getParent());
            try (Writer w = Files.newBufferedWriter(playersFile)) {
                GSON.toJson(out, w);
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Failed to save actu_players.json: " + e.getMessage());
        }
    }

    @Nonnull
    private java.util.Set<UUID> keySetUnion() {
        java.util.Set<UUID> keys = new java.util.HashSet<>(lastSeenVersion.keySet());
        keys.addAll(wantsPopup.keySet());
        return keys;
    }

    private static final class VersionData {
        long version;
    }

    private static final class PlayerData {
        long lastSeenVersion;
        boolean wantsPopup = true;
    }
}
