package fr.varyon.stacktiers.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persistance disque en texte plat pour tout le mod (un seul fichier, pas un par
 * joueur — le volume d'écritures attendu est faible, une commande admin occasionnelle,
 * pas un flux fréquent comme une économie). Format : une ligne par override,
 * "uuid|categorie|tier". Écriture atomique via fichier temporaire + ATOMIC_MOVE.
 */
public final class TierFileStorage {
    private final Path file;

    public TierFileStorage(Path dataDirectory) {
        this.file = dataDirectory.resolve("tier-overrides.txt");
    }

    /** Clé retournée = "uuid|categorie" (même format que OverrideBridge), valeur = tier. */
    public synchronized Map<String, String> load() throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 3);
            if (parts.length != 3) {
                continue;
            }
            result.put(parts[0] + "|" + parts[1], parts[2]);
        }
        return result;
    }

    public synchronized void save(Map<String, String> entries) throws IOException {
        List<String> lines = entries.entrySet().stream()
                .map(e -> e.getKey() + "|" + e.getValue())
                .sorted()
                .collect(Collectors.toList());

        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
