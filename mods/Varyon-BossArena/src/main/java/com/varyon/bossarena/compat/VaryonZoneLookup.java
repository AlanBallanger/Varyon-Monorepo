package com.varyon.bossarena.compat;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional lookup of the Varyon difficulty zone at a world position.
 *
 * <p>Unlike the player's unlocked tier (owned by LuckPerms, see {@link ZoneTier}), the zone a
 * position falls into is defined by Varyon's zone config, so it can only come from Varyon.
 * Returns 0 when Varyon is absent, which callers treat as "no zone gating".
 */
public final class VaryonZoneLookup {

    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static volatile Boolean bridgeAvailable;

    private VaryonZoneLookup() {}

    /** Zone id at the position, or 0 when unknown / Varyon absent. */
    public static int zoneIdAt(double x, double z, String worldName) {
        if (!VaryonMobScale.isVaryonLoaded() || !ensureBridge()) {
            return 0;
        }
        try {
            return VaryonZoneLookupBridge.zoneIdAt(x, z, worldName);
        } catch (NoClassDefFoundError | Exception e) {
            bridgeAvailable = false;
            LOGGER.log(Level.FINE, "Varyon zone lookup failed", e);
            return 0;
        }
    }

    private static boolean ensureBridge() {
        Boolean available = bridgeAvailable;
        if (available != null) {
            return available;
        }
        try {
            Class.forName("com.varyon.bossarena.compat.VaryonZoneLookupBridge");
            Class.forName("com.varyon.util.ZoneCalculator");
            bridgeAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            bridgeAvailable = false;
            LOGGER.info("Varyon zone lookup unavailable (Varyon classes not present).");
        }
        return bridgeAvailable;
    }

    /** Test / reload helper. */
    public static void resetCache() {
        bridgeAvailable = null;
    }
}
