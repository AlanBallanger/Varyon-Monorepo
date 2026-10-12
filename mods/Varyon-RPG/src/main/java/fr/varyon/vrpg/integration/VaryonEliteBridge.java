package fr.varyon.vrpg.integration;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Reflective bridge to Varyon's elite subsystem ({@code com.varyon.elite}). Lets
 * {@code ClassKillXpSystem} scale kill XP by the victim's elite tier without a compile-time
 * dependency on the Varyon mod. See {@code Varyon/docs/ELITE_MOBS_DESIGN.md} §6.
 */
public final class VaryonEliteBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.get("VaryonRPG");
    private static final boolean PRESENT;

    private static Method methodRegistryGet;
    private static Method methodGetEliteTier;
    private static Method methodPluginStaticConfigManager;
    private static Method methodGetEliteConfig;
    private static Method methodGetXpMultiplier;

    static {
        boolean found = false;
        try {
            Class<?> registryClass = Class.forName("com.varyon.elite.EliteRegistry");
            Class<?> pluginClass = Class.forName("com.varyon.VaryonPlugin");
            Class<?> configManagerClass = Class.forName("com.varyon.config.ConfigManager");
            Class<?> eliteConfigClass = Class.forName("com.varyon.elite.EliteConfig");

            methodRegistryGet = registryClass.getMethod("get");
            methodGetEliteTier = registryClass.getMethod("getEliteTier", UUID.class);
            methodPluginStaticConfigManager = pluginClass.getMethod("getStaticConfigManager");
            methodGetEliteConfig = configManagerClass.getMethod("getEliteConfig");
            methodGetXpMultiplier = eliteConfigClass.getMethod("getXpMultiplier", int.class);

            found = true;
            LOGGER.at(Level.INFO).log("[VaryonEliteBridge] elite subsystem detected - elite XP scaling active.");
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.INFO).log("[VaryonEliteBridge] elite subsystem not present - elite XP scaling disabled.");
        } catch (NoSuchMethodException | LinkageError e) {
            LOGGER.at(Level.WARNING).log("[VaryonEliteBridge] API mismatch: " + e.getMessage());
        }
        PRESENT = found;
    }

    private VaryonEliteBridge() {}

    public static boolean isPresent() {
        return PRESENT;
    }

    /** @return the victim's elite tier (1..5), or 0 if not an elite / unavailable. */
    public static int getEliteTier(@Nonnull UUID victimId) {
        if (!PRESENT) {
            return 0;
        }
        try {
            Object registry = methodRegistryGet.invoke(null);
            if (registry == null) {
                return 0;
            }
            Object tier = methodGetEliteTier.invoke(registry, victimId);
            return tier instanceof Integer i ? i : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** @return the configured XP multiplier for an elite tier, or 1.0 when unavailable. */
    public static double getXpMultiplier(int tier) {
        if (!PRESENT || tier <= 0) {
            return 1.0;
        }
        try {
            Object configManager = methodPluginStaticConfigManager.invoke(null);
            if (configManager == null) {
                return 1.0;
            }
            Object eliteConfig = methodGetEliteConfig.invoke(configManager);
            if (eliteConfig == null) {
                return 1.0;
            }
            Object mult = methodGetXpMultiplier.invoke(eliteConfig, tier);
            return mult instanceof Double d ? d : 1.0;
        } catch (Exception ignored) {
            return 1.0;
        }
    }
}
