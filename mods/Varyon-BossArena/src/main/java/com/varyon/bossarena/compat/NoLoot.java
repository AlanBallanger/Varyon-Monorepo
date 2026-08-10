package com.varyon.bossarena.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional bridge to Varyon's {@code NoLootComponent} marker. Posing it on an entity tells other
 * reward systems (Ecotale coins, Varyon key fragments, Varyon loot multiplier) not to award
 * anything on its death, without BossArena needing to know about those systems individually.
 * No-op when Varyon is absent.
 */
public final class NoLoot {
    private static final Logger LOGGER = Logger.getLogger("BossArena");

    private static volatile Boolean bridgeAvailable;

    private NoLoot() {}

    /** Marks the entity so external reward systems (coins, key fragments, ...) skip its death. */
    public static void mark(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (store == null || entityRef == null || !entityRef.isValid() || !VaryonMobScale.isVaryonLoaded()) {
            return;
        }
        if (!ensureBridge()) {
            return;
        }
        try {
            NoLootBridge.mark(store, entityRef);
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.log(Level.FINE, "NoLoot marker skipped", e);
        }
    }

    private static boolean ensureBridge() {
        Boolean available = bridgeAvailable;
        if (available != null) {
            return available;
        }
        try {
            Class.forName("com.varyon.bossarena.compat.NoLootBridge");
            Class.forName("com.varyon.component.NoLootComponent");
            bridgeAvailable = true;
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            bridgeAvailable = false;
            LOGGER.info("Varyon NoLoot bridge unavailable (Varyon classes not present).");
            return false;
        }
    }

    /** Test / reload helper. */
    public static void resetCache() {
        bridgeAvailable = null;
    }
}
