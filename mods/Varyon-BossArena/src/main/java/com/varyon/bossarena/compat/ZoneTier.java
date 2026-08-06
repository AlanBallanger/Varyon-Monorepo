package com.varyon.bossarena.compat;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the Varyon zone tier a player has unlocked, read straight from LuckPerms.
 *
 * <p>Tiers are cumulative: {@code varyon.zone.4} grants tiers 1-4. LuckPerms owns the player's
 * tier, so BossArena queries it directly instead of going through Varyon.
 */
public final class ZoneTier {

    /** Permission prefix; tier N is {@code varyon.zone.N}. */
    private static final String PERMISSION_PREFIX = "varyon.zone.";
    /** Highest tier probed. Matches Varyon's default 1-10 zone range. */
    public static final int MAX_TIER = 10;

    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static volatile Boolean bridgeAvailable;

    private ZoneTier() {}

    static String permissionNode(int tier) {
        return PERMISSION_PREFIX + tier;
    }

    /**
     * Highest tier unlocked by the player, or 0 when none is granted (or the player is unknown).
     * Falls back to {@code PlayerRef.hasPermission} when LuckPerms is unavailable.
     */
    public static int unlockedTier(PlayerRef playerRef) {
        if (playerRef == null) {
            return 0;
        }
        if (ensureBridge()) {
            try {
                int tier = ZoneTierBridge.highestZoneGranted(playerRef, MAX_TIER);
                if (tier > 0) {
                    return tier;
                }
            } catch (NoClassDefFoundError | Exception e) {
                bridgeAvailable = false;
                LOGGER.log(Level.FINE, "LuckPerms zone tier lookup failed, falling back", e);
            }
        }
        for (int tier = MAX_TIER; tier >= 1; tier--) {
            if (playerRef.hasPermission(permissionNode(tier))) {
                return tier;
            }
        }
        return 0;
    }

    private static boolean ensureBridge() {
        Boolean available = bridgeAvailable;
        if (available != null) {
            return available;
        }
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            Class.forName("com.varyon.bossarena.compat.ZoneTierBridge");
            bridgeAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            bridgeAvailable = false;
            LOGGER.info("LuckPerms not present; zone tier falls back to PlayerRef permissions.");
        }
        return bridgeAvailable;
    }

    /** Test / reload helper. */
    public static void resetCache() {
        bridgeAvailable = null;
    }
}
