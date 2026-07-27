package fr.varyon.vrpg.integration;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.logging.Level;

public final class VaryonZoneBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.get("VaryonRPG");
    private static final boolean VARYON_PRESENT;

    private static Method methodGetStaticConfigManager;
    private static Method methodGetZoneConfig;
    private static Method methodIsWorldEnabled;
    private static Method methodGetZoneAtPosition;
    private static Method methodGetZoneId;
    private static Method methodGetEssenceMultiplier;
    private static Method methodGetStaticSafeZoneManager;
    private static Method methodIsInSafeZone;

    static {
        boolean found = false;
        try {
            Class<?> pluginClass = Class.forName("com.varyon.VaryonPlugin");
            Class<?> configManagerClass = Class.forName("com.varyon.config.ConfigManager");
            Class<?> zoneConfigClass = Class.forName("com.varyon.config.ZoneConfig");
            Class<?> difficultyZoneClass = Class.forName("com.varyon.config.DifficultyZone");
            Class<?> zoneCalculatorClass = Class.forName("com.varyon.util.ZoneCalculator");
            Class<?> safeZoneManagerClass = Class.forName("com.varyon.safezone.SafeZoneManager");

            methodGetStaticConfigManager = pluginClass.getMethod("getStaticConfigManager");
            methodGetZoneConfig = configManagerClass.getMethod("getZoneConfig");
            methodIsWorldEnabled = zoneConfigClass.getMethod("isWorldEnabled", String.class);
            methodGetZoneAtPosition = zoneCalculatorClass.getMethod(
                "getZoneAtPosition", double.class, double.class, String.class, zoneConfigClass);
            methodGetZoneId = difficultyZoneClass.getMethod("getZoneId");
            methodGetEssenceMultiplier = difficultyZoneClass.getMethod("getEssenceMultiplier");
            methodGetStaticSafeZoneManager = pluginClass.getMethod("getStaticSafeZoneManager");
            methodIsInSafeZone = safeZoneManagerClass.getMethod("isInSafeZone", double.class, double.class);

            found = true;
            LOGGER.at(Level.INFO).log("[VaryonZoneBridge] Varyon mod detected — zone-based XP scaling active.");
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.INFO).log("[VaryonZoneBridge] Varyon mod not loaded — zone XP scaling disabled.");
        } catch (NoSuchMethodException | LinkageError e) {
            LOGGER.at(Level.WARNING).log("[VaryonZoneBridge] Varyon API mismatch: " + e.getMessage());
        }
        VARYON_PRESENT = found;
    }

    public static boolean isPresent() { return VARYON_PRESENT; }

    public static boolean isVaryonWorld(@Nonnull String worldName) {
        if (!VARYON_PRESENT) return false;
        try {
            Object configManager = methodGetStaticConfigManager.invoke(null);
            if (configManager == null) return false;
            Object zoneConfig = methodGetZoneConfig.invoke(configManager);
            if (zoneConfig == null) return false;
            return (boolean) methodIsWorldEnabled.invoke(zoneConfig, worldName);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns the Varyon difficulty zone id (1-10) at a position, or 0 if unavailable/out of zone. */
    public static int getZoneId(double x, double z, @Nonnull String worldName) {
        if (!VARYON_PRESENT) return 0;
        try {
            Object configManager = methodGetStaticConfigManager.invoke(null);
            if (configManager == null) return 0;
            Object zoneConfig = methodGetZoneConfig.invoke(configManager);
            if (zoneConfig == null) return 0;
            Object zone = methodGetZoneAtPosition.invoke(null, x, z, worldName, zoneConfig);
            if (zone == null) return 0;
            return (int) methodGetZoneId.invoke(zone);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static double getEssenceMultiplier(double x, double z, @Nonnull String worldName) {
        if (!VARYON_PRESENT) return 1.0;
        try {
            Object configManager = methodGetStaticConfigManager.invoke(null);
            if (configManager == null) return 1.0;
            Object zoneConfig = methodGetZoneConfig.invoke(configManager);
            if (zoneConfig == null) return 1.0;
            Object zone = methodGetZoneAtPosition.invoke(null, x, z, worldName, zoneConfig);
            if (zone == null) return 1.0;
            return (double) methodGetEssenceMultiplier.invoke(zone);
        } catch (Exception ignored) {
            return 1.0;
        }
    }

    public static boolean isInSafeZone(double x, double z) {
        if (!VARYON_PRESENT) return true;
        try {
            Object safeZoneManager = methodGetStaticSafeZoneManager.invoke(null);
            if (safeZoneManager == null) return true;
            return (boolean) methodIsInSafeZone.invoke(safeZoneManager, x, z);
        } catch (Exception ignored) {
            return true;
        }
    }

    private VaryonZoneBridge() {}
}
