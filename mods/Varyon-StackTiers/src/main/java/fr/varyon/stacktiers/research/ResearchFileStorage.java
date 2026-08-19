package fr.varyon.stacktiers.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistance JSON de l'état de recherche de tous les joueurs (un seul fichier pour tout le
 * mod — le volume d'écritures est faible, une recherche démarrée par joueur de temps en temps).
 * Écriture atomique (fichier temporaire + ATOMIC_MOVE), même schéma que
 * BossTimedSpawnScheduler (Varyon-BossArena) : source de vérité en JSON, timestamps epoch
 * absolus, versionné pour permettre une migration future du format.
 */
public final class ResearchFileStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PERSISTENCE_VERSION = 1;

    private final Path file;

    public ResearchFileStorage(Path dataDirectory) {
        this.file = dataDirectory.resolve("research-state.json");
    }

    public synchronized Map<UUID, PlayerResearchState> load() {
        Map<UUID, PlayerResearchState> result = new HashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            PersistedState persisted = GSON.fromJson(json, PersistedState.class);
            if (persisted == null || persisted.players == null) {
                return result;
            }
            for (Map.Entry<String, PersistedPlayerRow> entry : persisted.players.entrySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                PersistedPlayerRow row = entry.getValue();
                PlayerResearchState state = new PlayerResearchState();
                if (row.completed != null) {
                    state.completedNodeIds.addAll(row.completed);
                }
                state.inProgressNodeId = row.inProgressNodeId;
                state.startEpochMs = row.startEpochMs;
                state.completionEpochMs = row.completionEpochMs;
                result.put(uuid, state);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + file, e);
        }
        return result;
    }

    public synchronized void save(Map<UUID, PlayerResearchState> states) throws IOException {
        PersistedState persisted = new PersistedState();
        for (Map.Entry<UUID, PlayerResearchState> entry : states.entrySet()) {
            PlayerResearchState state = entry.getValue();
            PersistedPlayerRow row = new PersistedPlayerRow();
            row.completed = new ArrayList<>(state.completedNodeIds);
            row.inProgressNodeId = state.inProgressNodeId;
            row.startEpochMs = state.startEpochMs;
            row.completionEpochMs = state.completionEpochMs;
            persisted.players.put(entry.getKey().toString(), row);
        }

        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(persisted), StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class PersistedState {
        int version = PERSISTENCE_VERSION;
        Map<String, PersistedPlayerRow> players = new HashMap<>();
    }

    private static final class PersistedPlayerRow {
        List<String> completed = new ArrayList<>();
        String inProgressNodeId;
        long startEpochMs;
        long completionEpochMs;
    }
}
