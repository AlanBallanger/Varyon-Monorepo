package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Resolves the Varyon zone tier ("varyon.zone.N") a player has unlocked, for display in varyon-UI.
 *
 * <p>Tiers are cumulative: {@code varyon.zone.4} grants tiers 1-4. Mirrors BossArena's
 * {@code ZoneTier} fallback path (plain {@link PlayerRef#hasPermission}) rather than depending on
 * LuckPerms directly, since varyon-UI has no compile-time LuckPerms dependency. Players with no
 * zone permission at all default to tier 1.
 */
public final class ZoneTierResolver {

    private static final String PERMISSION_PREFIX = "varyon.zone.";
    public static final int MAX_TIER = 10;
    public static final int DEFAULT_TIER = 1;

    private ZoneTierResolver() {}

    public static int unlockedTier(@Nonnull PlayerRef playerRef) {
        for (int tier = MAX_TIER; tier >= 1; tier--) {
            if (playerRef.hasPermission(PERMISSION_PREFIX + tier)) {
                return tier;
            }
        }
        return DEFAULT_TIER;
    }
}
