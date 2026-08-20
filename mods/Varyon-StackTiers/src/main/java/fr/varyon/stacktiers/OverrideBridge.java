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
public final class OverrideBridge {
    private static final Logger LOGGER = Logger.getLogger("Varyon-StackTiers");
    private static final String ITEM_CLASS = "com.hypixel.hytale.server.core.asset.type.item.config.Item";
    private static final String FIELD_NAME = "varyon$playerTierOverrides";

    // Volatile : put()/remove() peuvent s'exécuter avant qu'un retry en arrière-plan (tick
    // serveur, prochain appel) ait réussi à attacher liveMap depuis un autre thread.
    private volatile Map<String, String> liveMap;
    private volatile Map<String, String> pendingBacklog;
    // Throttle : tryAttach() est rappelé à chaque put()/remove() (potentiellement plusieurs
    // fois par action, une par catégorie via applyBridge) ; sans limite, un échec persistant
    // spamme un warning par appel. Une seule tentative par fenêtre de 30s suffit à détecter un
    // rattachement tardif sans redémarrer le serveur.
    private volatile long lastAttachAttemptMs;
    private static final long RETRY_INTERVAL_MS = 30_000L;

    private OverrideBridge(Map<String, String> liveMap) {
        this.liveMap = liveMap;
    }

    public static OverrideBridge attach() {
        OverrideBridge bridge = new OverrideBridge(null);
        bridge.tryAttach();
        return bridge;
    }

    /**
     * Tente (ré)essaie de résoudre le champ injecté. Le premier essai a lieu au démarrage du
     * plugin, qui peut courir contre l'injection ASM du early-plugin Mixin-Varyon-StackSize
     * (ordre de chargement non garanti entre les deux mods) — d'où l'échec observé en jeu alors
     * que /varyonstack semblait fonctionner (en réalité via LuckPerms, chemin indépendant qui
     * ne passe jamais par ce pont). Rappeler tryAttach() plus tard permet de se rattacher sans
     * redémarrer le serveur. Sans effet si déjà attaché.
     */
    public synchronized void tryAttach() {
        if (liveMap != null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAttachAttemptMs < RETRY_INTERVAL_MS) {
            return;
        }
        lastAttachAttemptMs = now;
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
                liveMap = map;
                if (pendingBacklog != null) {
                    map.putAll(pendingBacklog);
                    pendingBacklog = null;
                }
                return;
            }
            LOGGER.severe(FIELD_NAME + " existe mais n'est pas une Map (type inattendu : "
                    + (raw == null ? "null" : raw.getClass()) + "). Les overrides de palier "
                    + "seront persistés sur disque mais N'AURONT PAS D'EFFET tant que ce "
                    + "problème n'est pas résolu.");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            LOGGER.warning("Champ " + FIELD_NAME + " introuvable sur Item pour l'instant — "
                    + "Mixin-Varyon-StackSize n'a peut-être pas encore fini de charger. "
                    + "Nouvelle tentative au prochain appel : " + e);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Échec inattendu de la connexion au bridge de palier de stack", t);
        }
    }

    public boolean isActive() {
        return liveMap != null;
    }

    public void put(String uuid, String category, int tier) {
        tryAttach();
        Map<String, String> map = liveMap;
        if (map != null) {
            map.put(uuid + "|" + category, Integer.toString(tier));
        } else {
            backlog().put(uuid + "|" + category, Integer.toString(tier));
        }
    }

    public void remove(String uuid, String category) {
        tryAttach();
        Map<String, String> map = liveMap;
        if (map != null) {
            map.remove(uuid + "|" + category);
        } else if (pendingBacklog != null) {
            pendingBacklog.remove(uuid + "|" + category);
        }
    }

    /** Utilisé uniquement au démarrage pour repeupler la map partagée depuis le disque. */
    public void bulkLoad(Map<String, String> entries) {
        tryAttach();
        Map<String, String> map = liveMap;
        if (map != null) {
            map.putAll(entries);
        } else {
            backlog().putAll(entries);
        }
    }

    /** Entrées écrites pendant que le bridge était inactif — rejouées dès qu'il s'attache. */
    private synchronized Map<String, String> backlog() {
        if (pendingBacklog == null) {
            pendingBacklog = new java.util.concurrent.ConcurrentHashMap<>();
        }
        return pendingBacklog;
    }
}
