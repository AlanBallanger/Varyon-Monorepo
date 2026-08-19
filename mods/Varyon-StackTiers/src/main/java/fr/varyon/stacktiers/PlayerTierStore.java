package fr.varyon.stacktiers;

import fr.varyon.stacktiers.storage.TierFileStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager réutilisable : source unique de vérité pour les paliers de stack par joueur.
 * Combine le cache en mémoire, la persistance disque et le pont vers le bytecode
 * injecté. C'est cette classe (pas la commande) qu'une future UI (varyon-UI) devra
 * appeler directement pour lire/écrire les paliers d'un joueur.
 */
public final class PlayerTierStore {
    private static final Logger LOGGER = Logger.getLogger("Varyon-StackTiers");

    private final TierFileStorage storage;
    private final OverrideBridge bridge;
    private final Map<String, String> cache = new HashMap<>();

    public PlayerTierStore(Path dataDirectory) {
        this.storage = new TierFileStorage(dataDirectory);
        this.bridge = OverrideBridge.attach();
    }

    public void loadFromDisk() {
        try {
            Map<String, String> loaded = storage.load();
            cache.putAll(loaded);
            bridge.bulkLoad(loaded);
            LOGGER.info("Chargé " + loaded.size() + " override(s) de palier depuis le disque"
                    + (bridge.isActive() ? "" : " (bridge inactif : sans effet ce démarrage)"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Échec de chargement de tier-overrides.txt", e);
        }
    }

    public boolean isBridgeActive() {
        return bridge.isActive();
    }

    /** @return le palier précédent pour cette catégorie (0 si aucun). */
    public synchronized int set(UUID playerUuid, String category, int tier) {
        String key = playerUuid + "|" + category;
        String previous = cache.put(key, Integer.toString(tier));
        bridge.put(playerUuid.toString(), category, tier);
        persist();
        return previous == null ? 0 : Integer.parseInt(previous);
    }

    public synchronized boolean remove(UUID playerUuid, String category) {
        String key = playerUuid + "|" + category;
        String previous = cache.remove(key);
        bridge.remove(playerUuid.toString(), category);
        persist();
        return previous != null;
    }

    public synchronized int get(UUID playerUuid, String category) {
        String v = cache.get(playerUuid + "|" + category);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public synchronized Map<String, Integer> listForPlayer(UUID playerUuid) {
        String prefix = playerUuid + "|";
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                String category = e.getKey().substring(prefix.length());
                result.put(category, Integer.parseInt(e.getValue()));
            }
        }
        return result;
    }

    private void persist() {
        try {
            storage.save(cache);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Échec de sauvegarde de tier-overrides.txt", e);
        }
    }
}
