/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 */
package com.woxtz.weaponinfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

public class PluginConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PluginConfig instance;
    private boolean publicCommand = true;
    private boolean showWelcomeMessage = true;

    private PluginConfig() {
    }

    public static PluginConfig getInstance() {
        if (instance == null) {
            instance = new PluginConfig();
        }
        return instance;
    }

    public void load(JavaPlugin plugin) {
        Path pluginPath = plugin.getFile();
        File configDir = pluginPath.getParent().resolve("WeaponStatsViewer").toFile();
        File configFile = new File(configDir, "WeaponStatsViewer.json");
        if (!configFile.exists()) {
            this.save(plugin);
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            PluginConfig loaded = (PluginConfig)GSON.fromJson((Reader)reader, PluginConfig.class);
            if (loaded != null) {
                this.publicCommand = loaded.publicCommand;
                this.showWelcomeMessage = loaded.showWelcomeMessage;
            }
        }
        catch (IOException e) {
            System.err.println("Failed to load WeaponStatsViewer config, using defaults: " + e.getMessage());
        }
    }

    public void save(JavaPlugin plugin) {
        Path pluginPath = plugin.getFile();
        File configDir = pluginPath.getParent().resolve("WeaponStatsViewer").toFile();
        File configFile = new File(configDir, "WeaponStatsViewer.json");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson((Object)this, (Appendable)writer);
        }
        catch (IOException e) {
            System.err.println("Failed to save WeaponStatsViewer config: " + e.getMessage());
        }
    }

    public boolean isPublicCommand() {
        return this.publicCommand;
    }

    public void setPublicCommand(boolean publicCommand) {
        this.publicCommand = publicCommand;
    }

    public boolean isShowWelcomeMessage() {
        return this.showWelcomeMessage;
    }

    public void setShowWelcomeMessage(boolean showWelcomeMessage) {
        this.showWelcomeMessage = showWelcomeMessage;
    }
}

