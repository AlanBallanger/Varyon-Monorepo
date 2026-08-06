package com.varyon.bossarena.compat;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

/**
 * Hard references to LuckPerms types. Loaded only when {@link ZoneTier} confirms LuckPerms is present.
 */
final class ZoneTierBridge {

    private ZoneTierBridge() {}

    /**
     * Highest {@code varyon.zone.N} granted to the player, or 0 when none resolves.
     *
     * <p>Checks non-contextual and active permission data, because a node granted only in a
     * specific context (world, server) is invisible to the other view.
     */
    static int highestZoneGranted(PlayerRef playerRef, int maxTier) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getPlayerAdapter(PlayerRef.class).getUser(playerRef);
        if (user == null) {
            return 0;
        }
        int best = scan(user.getCachedData().getPermissionData(QueryOptions.nonContextual()), maxTier);
        return Math.max(best, scan(user.getCachedData().getPermissionData(), maxTier));
    }

    private static int scan(CachedPermissionData permissionData, int maxTier) {
        if (permissionData == null) {
            return 0;
        }
        for (int tier = maxTier; tier >= 1; tier--) {
            if (permissionData.checkPermission(ZoneTier.permissionNode(tier)).asBoolean()) {
                return tier;
            }
        }
        return 0;
    }
}
