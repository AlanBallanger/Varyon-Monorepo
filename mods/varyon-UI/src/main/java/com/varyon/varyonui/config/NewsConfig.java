package com.varyon.varyonui.config;

import com.moandjiezana.toml.Toml;
import com.varyon.varyonui.VaryonUIPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NewsConfig {
    private static NewsConfig instance;
    private List<NewsEntry> entries = new ArrayList<>();
    private final Path configPath;

    private NewsConfig() {
        File dataFolder = VaryonUIPlugin.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configPath = dataFolder.toPath().resolve("news.toml");
        loadConfig();
    }

    public static NewsConfig getInstance() {
        if (instance == null) {
            instance = new NewsConfig();
        }
        return instance;
    }

    public void reload() {
        entries.clear();
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                try (InputStream in = getClass().getResourceAsStream("/news.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            }

            if (Files.exists(configPath)) {
                Toml toml = new Toml().read(configPath.toFile());
                List<Toml> newsList = toml.getTables("news");

                if (newsList != null) {
                    for (Toml newsToml : newsList) {
                        String date = newsToml.getString("date", "");
                        String title = newsToml.getString("title", "");
                        String content = newsToml.getString("content", "");
                        entries.add(new NewsEntry(date, title, content.trim()));
                    }
                }
                System.out.println("[VaryonUI] Loaded " + entries.size() + " news entries");
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Error loading news config:");
            e.printStackTrace();
        }
    }

    public List<NewsEntry> getEntries() {
        return entries;
    }

    public static class NewsEntry {
        private final String date;
        private final String title;
        private final String content;

        public NewsEntry(String date, String title, String content) {
            this.date = date;
            this.title = title;
            this.content = content;
        }

        public String getDate() {
            return date;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}
