package com.varyon.bossarena.compat;

import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import com.varyon.config.ZoneConfig;
import com.varyon.util.ZoneCalculator;

/**
 * Hard references to Varyon types. Loaded only when {@link VaryonZoneLookup} confirms Varyon is present.
 */
final class VaryonZoneLookupBridge {

    private VaryonZoneLookupBridge() {}

    /** Zone id at the given world position, or 0 when it cannot be resolved. */
    static int zoneIdAt(double x, double z, String worldName) {
        ConfigManager configManager = VaryonPlugin.getStaticConfigManager();
        if (configManager == null) {
            return 0;
        }
        ZoneConfig zoneConfig = configManager.getZoneConfig();
        if (zoneConfig == null) {
            return 0;
        }
        var zone = ZoneCalculator.getZoneAtPosition(x, z, worldName, zoneConfig);
        return zone != null ? zone.getZoneId() : 0;
    }
}
