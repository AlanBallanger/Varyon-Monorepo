package fr.varyon.death.config;

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

import javax.annotation.Nonnull;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Preference persistante « afficher le recapitulatif de mort », par joueur.
 * Les ecritures sont differees sur un thread d'E/S dedie pour ne pas bloquer le tick monde.
 */
public final class GestionnairePreferences {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeathRecap-Prefs");
    private static final String CONFIG_FILE = "config.json";
    private static final Pattern JSON_PLAYER_ENTRY =
            Pattern.compile("\"([0-9a-fA-F-]{36})\"\\s*:\\s*(true|false)");

    private final Path dataDirectory;
    private final Path configPath;
    private final ConcurrentHashMap<UUID, Boolean> cache = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "VaryonDeathRecap-Prefs-IO");
        thread.setDaemon(true);
        return thread;
    });

    public GestionnairePreferences(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve(CONFIG_FILE);
    }

    public void initialize() {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.isRegularFile(configPath)) {
                loadFromJson(Files.readString(configPath, StandardCharsets.UTF_8));
            }
            LOGGER.at(Level.INFO).log("Preferences de recapitulatif chargees (%d entrees)", cache.size());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Echec chargement des preferences: %s", e.getMessage());
        }
    }

    public void ensureLoaded(@Nonnull UUID uuid) {
        cache.computeIfAbsent(uuid, ignored -> Boolean.TRUE);
    }

    public boolean estActif(@Nonnull UUID uuid) {
        return cache.getOrDefault(uuid, Boolean.TRUE);
    }

    public void setEnabled(@Nonnull UUID uuid, boolean enabled) {
        cache.put(uuid, enabled);
        scheduleSave();
    }

    public boolean toggle(@Nonnull UUID uuid) {
        boolean next = !estActif(uuid);
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
                cache.put(UUID.fromString(matcher.group(1)), Boolean.parseBoolean(matcher.group(2)));
            } catch (IllegalArgumentException ignored) {
                // Entree corrompue : ignoree, la valeur par defaut s'applique.
            }
        }
    }

    private void saveNow() {
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(configPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Echec sauvegarde des preferences: %s", e.getMessage());
        }
    }

    @Nonnull
    private String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"recapEnabledByPlayer\": {\n");
        List<Map.Entry<UUID, Boolean>> entries = cache.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<UUID, Boolean> entry = entries.get(i);
            out.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            if (i + 1 < entries.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  }\n}\n");
        return out.toString();
    }
}
