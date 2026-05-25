package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ChatAnnouncementsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String FILENAME = "chat_announcements.toml";

    private boolean enabled;
    private int intervalMinutes;
    private final List<String> messages;

    public ChatAnnouncementsConfig(boolean enabled, int intervalMinutes, @Nonnull List<String> messages) {
        this.enabled = enabled;
        this.intervalMinutes = intervalMinutes;
        this.messages = new ArrayList<>(messages);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    @Nonnull
    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void setMessages(@Nonnull List<String> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
    }

    @Nonnull
    public static ChatAnnouncementsConfig createDefault() {
        List<String> lines = new ArrayList<>();
        lines.add("Bienvenue sur Varyon — restez prudents hors de la zone sûre.");
        lines.add("Déposez vos points de faction dans les coffres de faction pour contribuer à la balance globale.");
        return new ChatAnnouncementsConfig(true, 5, lines);
    }

    @Nonnull
    public static ChatAnnouncementsConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            ChatAnnouncementsConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);
            boolean en = toml.getBoolean("enabled", true);
            int interval = toml.getLong("intervalMinutes", 5L).intValue();
            if (interval < 1) {
                interval = 1;
            }
            List<String> msgs = new ArrayList<>();
            List<Toml> tables = toml.getTables("messages");
            if (tables != null) {
                for (Toml t : tables) {
                    String text = t.getString("text", "");
                    if (text != null && !text.isBlank()) {
                        msgs.add(text);
                    }
                }
            }
            LOGGER.at(Level.INFO).log("Loaded {0}: enabled={1}, intervalMinutes={2}, messages={3}",
                    FILENAME, en, interval, msgs.size());
            return new ChatAnnouncementsConfig(en, interval, msgs);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load " + FILENAME + ", using defaults", e);
            return createDefault();
        }
    }

    public void save(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateToml());
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save " + FILENAME, e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("enabled = ").append(enabled).append("\n");
        sb.append("intervalMinutes = ").append(intervalMinutes).append("\n\n");
        for (String line : messages) {
            sb.append("[[messages]]\n");
            sb.append("text = \"").append(escape(line)).append("\"\n\n");
        }
        return sb.toString();
    }

    private static String escape(@Nonnull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
