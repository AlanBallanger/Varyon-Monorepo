package fr.varyon.stacktiers;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pont réflexif vers le champ statique varyon$playerTierOverrides injecté par
 * Mixin-Varyon-StackSize (early-plugin, ASM) dans la classe serveur Item. Item est
 * chargée par TransformingClassLoader, ce plugin classique par le loader normal des
 * plugins — mais un objet déjà construit (ici une ConcurrentHashMap) peut être
 * référencé et manipulé depuis n'importe quel classloader tant qu'on ne s'appuie que
 * sur des types JDK présents partout (Map, String).
 *
 * Class.forName déclenche le chargement (et donc <clinit>, donc l'initialisation du
 * champ) si Item n'est pas encore chargée — aucune dépendance d'ordre de chargement
 * explicite n'est nécessaire entre les deux mods.
 */
final class OverrideBridge {
    private static final Logger LOGGER = Logger.getLogger("Varyon-StackTiers");
    private static final String ITEM_CLASS = "com.hypixel.hytale.server.core.asset.type.item.config.Item";
    private static final String FIELD_NAME = "varyon$playerTierOverrides";

    private final Map<String, String> liveMap;

    private OverrideBridge(Map<String, String> liveMap) {
        this.liveMap = liveMap;
    }

    static OverrideBridge attach() {
        try {
            Class<?> itemClass = Class.forName(ITEM_CLASS);
            Field field = itemClass.getDeclaredField(FIELD_NAME);
            field.setAccessible(true);
            Object raw = field.get(null);
            if (raw instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) rawMap;
                LOGGER.info("Bridge établi vers Item.varyon$playerTierOverrides ("
                        + map.size() + " entrée(s) déjà présente(s))");
                return new OverrideBridge(map);
            }
            LOGGER.severe(FIELD_NAME + " existe mais n'est pas une Map (type inattendu : "
                    + (raw == null ? "null" : raw.getClass()) + "). Les overrides de palier "
                    + "seront persistés sur disque mais N'AURONT PAS D'EFFET tant que ce "
                    + "problème n'est pas résolu.");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            LOGGER.log(Level.SEVERE, "Champ " + FIELD_NAME + " introuvable sur Item — "
                    + "Mixin-Varyon-StackSize est-il installé et à jour ? Les overrides de "
                    + "palier seront persistés sur disque mais N'AURONT PAS D'EFFET.", e);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Échec inattendu de la connexion au bridge de palier de stack", t);
        }
        return new OverrideBridge(null);
    }

    boolean isActive() {
        return liveMap != null;
    }

    void put(String uuid, String category, int tier) {
        if (liveMap != null) {
            liveMap.put(uuid + "|" + category, Integer.toString(tier));
        }
    }

    void remove(String uuid, String category) {
        if (liveMap != null) {
            liveMap.remove(uuid + "|" + category);
        }
    }

    /** Utilisé uniquement au démarrage pour repeupler la map partagée depuis le disque. */
    void bulkLoad(Map<String, String> entries) {
        if (liveMap != null) {
            liveMap.putAll(entries);
        }
    }
}
