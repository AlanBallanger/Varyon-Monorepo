package com.varyon.varyonui.config;

import com.moandjiezana.toml.Toml;
import com.varyon.varyonui.VaryonUIPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TutorielConfig {
    private static TutorielConfig instance;
    private String content = "";
    private final Path configPath;

    private TutorielConfig() {
        File dataFolder = VaryonUIPlugin.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configPath = dataFolder.toPath().resolve("tutoriel.toml");
        loadConfig();
    }

    public static TutorielConfig getInstance() {
        if (instance == null) {
            instance = new TutorielConfig();
        }
        return instance;
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                try (InputStream in = getClass().getResourceAsStream("/tutoriel.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            }

            if (Files.exists(configPath)) {
                Toml toml = new Toml().read(configPath.toFile());
                this.content = toml.getString("content", "").trim();
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Error loading tutoriel config:");
            e.printStackTrace();
        }
    }

    public String getContent() {
        return content;
    }
}
