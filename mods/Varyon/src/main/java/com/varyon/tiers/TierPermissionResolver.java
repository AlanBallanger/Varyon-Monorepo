package com.varyon.tiers;

import com.hypixel.hytale.logger.HytaleLogger;
import com.varyon.config.ZonePermissionsConfig;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Resolves the highest zone/tier permission a player (online or offline) holds,
 * via LuckPerms. Uses the same permission mapping as ZonePermissionsConfig so the
 * leaderboard reflects the permissions actually granted to players.
 */
public final class TierPermissionResolver {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private TierPermissionResolver() {}

    public static int highestTier(@Nonnull UUID playerUuid, @Nonnull ZonePermissionsConfig zonePermissionsConfig, int maxTier) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().loadUser(playerUuid).join();
            if (user == null) {
                return 1;
            }
            int best = scan(user.getCachedData().getPermissionData(QueryOptions.nonContextual()), zonePermissionsConfig, maxTier);
            best = Math.max(best, scan(user.getCachedData().getPermissionData(), zonePermissionsConfig, maxTier));
            return Math.max(best, 1);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to resolve tier for " + playerUuid + ": " + e.getMessage());
            return 1;
        }
    }

    private static int scan(CachedPermissionData permissionData, ZonePermissionsConfig zonePermissionsConfig, int maxTier) {
        if (permissionData == null) {
            return 1;
        }
        for (int tier = maxTier; tier >= 1; tier--) {
            if (permissionData.checkPermission(zonePermissionsConfig.getPermissionForZone(tier)).asBoolean()) {
                return tier;
            }
        }
        return 1;
    }
}
