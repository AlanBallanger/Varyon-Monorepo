package com.varyon.comet.audio;

import com.hypixel.hytale.builtin.ambience.AmbiencePlugin;
import com.hypixel.hytale.builtin.ambience.resources.AmbienceResource;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.comet.CometConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class CometCombatMusic {

    private static final Logger LOGGER = Logger.getLogger(CometCombatMusic.class.getName());

    private static final ConcurrentHashMap<Store<EntityStore>, AtomicInteger> REFS = new ConcurrentHashMap<>();

    private CometCombatMusic() {
    }

    public static void beginEncounter(Store<EntityStore> store) {
        if (store == null) {
            return;
        }
        CometConfig cfg = CometConfig.getInstance();
        if (cfg == null) {
            LOGGER.warning("[CometCombatMusic] beginEncounter skipped: CometConfig instance null");
            return;
        }
        String id = cfg.combatMusicAmbienceId;
        if (id == null || id.isBlank()) {
            LOGGER.warning("[CometCombatMusic] beginEncounter skipped: combatMusicAmbienceId empty in config.json");
            return;
        }
        try {
            AmbiencePlugin plugin = AmbiencePlugin.get();
            if (plugin == null) {
                LOGGER.warning("[CometCombatMusic] beginEncounter skipped: AmbiencePlugin null (is the built-in Ambience plugin enabled?)");
                return;
            }
            AtomicInteger c = REFS.computeIfAbsent(store, s -> new AtomicInteger(0));
            if (c.incrementAndGet() != 1) {
                c.decrementAndGet();
                LOGGER.info("[CometCombatMusic] beginEncounter skipped: encounter already active for this world (ref not reset; another comet?)");
                return;
            }
            try {
                AmbienceResource res = store.getResource(AmbienceResource.getResourceType());
                res.setForcedMusicAmbience(id.trim());
                int idx = res.getForcedMusicIndex();
                if (idx < 0) {
                    LOGGER.warning("[CometCombatMusic] combatMusicAmbienceId '" + id
                            + "' not in AmbienceFX map (index < 0). Check Server/Audio/AmbienceFX.");
                    undoBeginEncounterRef(store);
                    return;
                }
                CometAmbienceSync.pushForcedMusicToAllPlayers(store, id.trim());
            } catch (Throwable t) {
                undoBeginEncounterRef(store);
                throw t;
            }
        } catch (Throwable t) {
            LOGGER.warning("Combat music (AmbienceFX) unavailable: " + t.getMessage());
        }
    }

    public static void endEncounter(Store<EntityStore> store) {
        if (store == null) {
            return;
        }
        CometConfig cfg = CometConfig.getInstance();
        if (cfg == null) {
            return;
        }
        String id = cfg.combatMusicAmbienceId;
        if (id == null || id.isBlank()) {
            return;
        }
        try {
            AtomicInteger c = REFS.get(store);
            if (c == null) {
                return;
            }
            if (c.decrementAndGet() > 0) {
                return;
            }
            REFS.remove(store);
            AmbiencePlugin plugin = AmbiencePlugin.get();
            if (plugin == null) {
                return;
            }
            AmbienceResource res = store.getResource(AmbienceResource.getResourceType());
            res.setForcedMusicAmbience(null);
            CometAmbienceSync.pushForcedMusicToAllPlayers(store, null);
        } catch (Throwable t) {
            LOGGER.warning("Combat music clear failed: " + t.getMessage());
        }
    }

    public static void reset() {
        REFS.clear();
    }

    private static void undoBeginEncounterRef(Store<EntityStore> store) {
        AtomicInteger c = REFS.get(store);
        if (c == null) {
            return;
        }
        if (c.decrementAndGet() <= 0) {
            REFS.remove(store);
        }
    }
}
