package fr.varyon.death.combat;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Conserve, par joueur, les {@value #MAX_ENTRIES} dernieres morts (date, monde et
 * recapitulatif complet), persistees sur disque pour survivre a un redemarrage du serveur.
 *
 * <p>Suit le meme pattern que {@code GestionnairePreferences} : cache memoire, ecritures
 * differees sur un thread d'E/S dedie. La serialisation utilise Gson (deja une dependance du
 * mod via {@code ConfigDeath}) plutot qu'un parsing manuel, la structure etant imbriquee.
 */
public final class HistoriqueMorts {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeathRecap-Historique");
    private static final String FICHIER = "deaths.json";
    public static final int MAX_ENTRIES = 10;

    private static final Type MAP_TYPE =
            new TypeToken<Map<String, List<DeathEntry>>>() {}.getType();

    private static volatile HistoriqueMorts instance;

    private final Path dataDirectory;
    private final Path cheminFichier;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final ConcurrentHashMap<UUID, Deque<DeathEntry>> cache = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "VaryonDeathRecap-Historique-IO");
        thread.setDaemon(true);
        return thread;
    });

    public HistoriqueMorts(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.cheminFichier = dataDirectory.resolve(FICHIER);
    }

    /** Lie l'instance active, accessible depuis les systemes ECS sans reference au plugin. */
    public static void lier(@Nonnull HistoriqueMorts historique) {
        instance = historique;
    }

    @Nullable
    public static HistoriqueMorts get() {
        return instance;
    }

    public void initialize() {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.isRegularFile(cheminFichier)) {
                chargerDepuisJson(Files.readString(cheminFichier, StandardCharsets.UTF_8));
            }
            LOGGER.at(Level.INFO).log("Historique des morts charge (%d joueurs)", cache.size());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Echec chargement de l'historique des morts: %s", e.getMessage());
        }
    }

    /** Ajoute une mort en tete d'historique, en bornant a {@value #MAX_ENTRIES} entrees. */
    public void enregistrer(@Nonnull UUID playerUuid, @Nonnull DeathEntry entree) {
        cache.compute(playerUuid, (uuid, existant) -> {
            Deque<DeathEntry> deque = existant != null ? existant : new ArrayDeque<>(MAX_ENTRIES + 1);
            deque.addFirst(entree);
            while (deque.size() > MAX_ENTRIES) {
                deque.removeLast();
            }
            return deque;
        });
        scheduleSave();
    }

    /** Les morts du joueur, de la plus recente a la plus ancienne. */
    @Nonnull
    public List<DeathEntry> pour(@Nonnull UUID playerUuid) {
        Deque<DeathEntry> deque = cache.get(playerUuid);
        return deque == null ? List.of() : List.copyOf(deque);
    }

    public void dropAll(@Nonnull UUID playerUuid) {
        if (cache.remove(playerUuid) != null) {
            scheduleSave();
        }
    }

    public void flush() {
        saveNow();
    }

    public int clearAll() {
        int size = cache.size();
        cache.clear();
        return size;
    }

    private void scheduleSave() {
        CompletableFuture.runAsync(this::saveNow, ioExecutor);
    }

    private void chargerDepuisJson(@Nonnull String json) {
        if (json.isBlank()) {
            return;
        }
        try {
            Map<String, List<DeathEntry>> brut = gson.fromJson(json, MAP_TYPE);
            if (brut == null) {
                return;
            }
            brut.forEach((uuidStr, entries) -> {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Deque<DeathEntry> deque = new ArrayDeque<>(entries.size());
                    entries.stream().limit(MAX_ENTRIES).forEach(deque::addLast);
                    cache.put(uuid, deque);
                } catch (IllegalArgumentException ignored) {
                    // UUID corrompu : entree ignoree.
                }
            });
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING).log("Fichier d'historique illisible: %s", e.getMessage());
        }
    }

    private void saveNow() {
        try {
            Files.createDirectories(dataDirectory);
            Map<String, List<DeathEntry>> versJson = new java.util.TreeMap<>();
            cache.forEach((uuid, deque) -> versJson.put(uuid.toString(), new ArrayList<>(deque)));
            Files.writeString(cheminFichier, gson.toJson(versJson, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Echec sauvegarde de l'historique des morts: %s", e.getMessage());
        }
    }

    /** Une mort passee : date, monde et recapitulatif complet. */
    public record DeathEntry(long timestampMs,
                             @Nonnull String worldName,
                             @Nonnull SuiviCombat.Snapshot snapshot) {

        @Nullable
        public static DeathEntry of(long timestampMs, @Nullable String worldName,
                                    @Nonnull SuiviCombat.Snapshot snapshot) {
            return new DeathEntry(timestampMs, worldName == null || worldName.isBlank() ? "?" : worldName, snapshot);
        }
    }
}
