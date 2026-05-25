package com.varyon.util;

import com.hypixel.hytale.server.core.universe.world.World;
import com.varyon.VaryonPlugin;
import com.varyon.config.ZoneConfig;

import javax.annotation.Nullable;

public final class VaryonWorldAccess {

    private VaryonWorldAccess() {}

    public static boolean isVaryonEnabledWorld(@Nullable World world) {
        if (world == null) {
            return false;
        }
        return isVaryonEnabledWorld(world.getName());
    }

    public static boolean isVaryonEnabledWorld(@Nullable String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager() != null
                ? VaryonPlugin.getStaticConfigManager().getZoneConfig() : null;
        return zoneConfig != null && zoneConfig.isWorldEnabled(worldName);
    }
}
