package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the Varyon zone tier ("varyon.zone.N") a player has unlocked, for display in varyon-UI.
 *
 * <p>Tiers are cumulative: {@code varyon.zone.4} grants tiers 1-4. Queries LuckPerms directly
 * first (checking both non-contextual and active permission data, since a node granted only in a
 * specific world/server context is invisible to the other view — see {@code ZoneTierBridge} in
 * Varyon-BossArena for the same fix). Falls back to plain {@link PlayerRef#hasPermission} when
 * LuckPerms is unavailable. Players with no zone permission at all default to tier 1.
 */
public final class ZoneTierResolver {

    private static final String PERMISSION_PREFIX = "varyon.zone.";
    public static final int MAX_TIER = 10;
    public static final int DEFAULT_TIER = 1;

    private static final Logger LOGGER = Logger.getLogger("varyon-UI");
    private static volatile Boolean bridgeAvailable;

    private ZoneTierResolver() {}

    public static int unlockedTier(@Nonnull PlayerRef playerRef) {
        if (ensureBridge()) {
            try {
                int tier = ZoneTierLuckPermsBridge.highestZoneGranted(playerRef, PERMISSION_PREFIX, MAX_TIER);
                if (tier > 0) {
                    return tier;
                }
            } catch (NoClassDefFoundError | Exception e) {
                bridgeAvailable = false;
                LOGGER.log(Level.FINE, "LuckPerms zone tier lookup failed, falling back", e);
            }
        }
        for (int tier = MAX_TIER; tier >= 1; tier--) {
            if (playerRef.hasPermission(PERMISSION_PREFIX + tier)) {
                return tier;
            }
        }
        return DEFAULT_TIER;
    }

    private static boolean ensureBridge() {
        Boolean available = bridgeAvailable;
        if (available != null) {
            return available;
        }
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            Class.forName("com.varyon.varyonui.integration.ZoneTierLuckPermsBridge");
            bridgeAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            bridgeAvailable = false;
            LOGGER.info("LuckPerms not present; zone tier falls back to PlayerRef permissions.");
        }
        return bridgeAvailable;
    }
}
