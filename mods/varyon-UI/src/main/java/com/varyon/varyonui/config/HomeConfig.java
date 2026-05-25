package com.varyon.varyonui.config;

import com.moandjiezana.toml.Toml;
import com.varyon.varyonui.VaryonUIPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class HomeConfig {
    private static HomeConfig instance;
    private String imagePath = "";
    private String content = "";
    private final Path configPath;

    private HomeConfig() {
        File dataFolder = VaryonUIPlugin.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configPath = dataFolder.toPath().resolve("home.toml");
        loadConfig();
    }

    public static HomeConfig getInstance() {
        if (instance == null) {
            instance = new HomeConfig();
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
                try (InputStream in = getClass().getResourceAsStream("/home.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            }

            if (Files.exists(configPath)) {
                Toml toml = new Toml().read(configPath.toFile());
                this.imagePath = toml.getString("image", "");
                this.content = toml.getString("content", "").trim();
                System.out.println("[VaryonUI] Home config loaded");
                System.out.println("[VaryonUI] Content preview: " + (content.length() > 100 ? content.substring(0, 100) + "..." : content));
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Error loading home config:");
            e.printStackTrace();
        }
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getContent() {
        return content;
    }
}
