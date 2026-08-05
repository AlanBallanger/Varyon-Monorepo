package com.varyon.comet.integration;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p><b>Hytale world-map zones</b> ({@value #HYTALE_ZONE_MIN}–{@value #HYTALE_ZONE_MAX}): indices used for
 * {@code hytaleZoneSpawnChances} (tier roll weights), from {@link WorldMapTracker} / region names.</p>
 * <p><b>Varyon placeholder rings</b> ({@value #VARYON_RING_MIN}–{@value #VARYON_RING_MAX}): concentric rings from
 * spawn via {@code VaryonPlaceholders.getZoneId}; used for delays, wave scaling, and per-ring shard counts
 * ({@code spawnSettings.cometVaryonRingRewards}).</p>
 */
public final class VaryonZoneResolver {

    public static final int HYTALE_ZONE_MIN = 1;
    public static final int HYTALE_ZONE_MAX = 4;
    public static final int VARYON_RING_MIN = 1;
    public static final int VARYON_RING_MAX = 10;

    private static final Pattern ZONE_NUM = Pattern.compile("(?i)zone(\\d+)");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private static final Logger LOGGER = Logger.getLogger(VaryonZoneResolver.class.getName());
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private VaryonZoneResolver() {
    }

    /**
     * Varyon ring 1–10 from placeholders, or 0 if unavailable.
     */
    public static int resolveVaryonRing(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            Class<?> c = Class.forName("com.varyon.placeholders.VaryonPlaceholders");
            PlayerRef pr = Universe.get().getPlayer(player.getUuid());
            if (pr == null) {
                warnOnce("playerRef", "Universe.get().getPlayer returned null for " + player.getUuid());
                return 0;
            }
            Object r = c.getMethod("getZoneId", PlayerRef.class).invoke(null, pr);
            if (r instanceof Integer) {
                int v = (Integer) r;
                if (v <= 0) {
                    warnOnce("playerZone", "VaryonPlaceholders.getZoneId returned " + v
                            + " (Varyon zone config not resolved for this player)");
                    return 0;
                }
                return Math.max(VARYON_RING_MIN, Math.min(VARYON_RING_MAX, v));
            }
        } catch (Throwable t) {
            warnOnce("playerReflect", "Varyon ring lookup via VaryonPlaceholders failed: " + t);
        }
        return 0;
    }

    /**
     * Varyon ring 1–10 for a world position, or 0 if the Varyon zone config is unavailable.
     *
     * <p>Unlike {@link #resolveVaryonRing(Player)} this reflects where the comet actually is, not where the
     * player who triggered it stands, so reward rings stay correct however the comet was spawned.</p>
     */
    public static int resolveVaryonRingAtPosition(double x, double z, String worldName) {
        try {
            Class<?> pluginClass = Class.forName("com.varyon.VaryonPlugin");
            Object configManager = pluginClass.getMethod("getStaticConfigManager").invoke(null);
            if (configManager == null) {
                warnOnce("configManager", "VaryonPlugin.getStaticConfigManager() is null (Varyon mod not ready)");
                return 0;
            }
            Object zoneConfig = configManager.getClass().getMethod("getZoneConfig").invoke(configManager);
            if (zoneConfig == null) {
                warnOnce("zoneConfig", "ConfigManager.getZoneConfig() returned null");
                return 0;
            }

            Class<?> zoneConfigClass = Class.forName("com.varyon.config.ZoneConfig");
            Class<?> calculator = Class.forName("com.varyon.util.ZoneCalculator");
            Object id = calculator
                    .getMethod("getZoneIdAtPosition", double.class, double.class, zoneConfigClass)
                    .invoke(null, x, z, zoneConfig);
            if (id instanceof Integer) {
                int v = (Integer) id;
                if (v <= 0) {
                    warnOnce("positionZone", "No Varyon zone matches position (" + x + ", " + z + ")");
                    return 0;
                }
                return Math.max(VARYON_RING_MIN, Math.min(VARYON_RING_MAX, v));
            }
        } catch (Throwable t) {
            warnOnce("positionReflect", "Varyon ring lookup via ZoneCalculator failed: " + t);
        }
        return 0;
    }

    /**
     * Position-based ring, falling back to the triggering player's ring when the position lookup is unavailable.
     */
    public static int resolveVaryonRingForComet(double x, double z, String worldName, Player fallbackPlayer) {
        int ring = resolveVaryonRingAtPosition(x, z, worldName);
        return ring > 0 ? ring : resolveVaryonRing(fallbackPlayer);
    }

    /** Keeps a broken Varyon integration visible in logs without spamming one line per comet. */
    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            LOGGER.warning("[VaryonZoneResolver] " + message
                    + " — falling back to ring " + VARYON_RING_MIN + " for comet rewards.");
        }
    }

    /**
     * Hytale world-map zone index, clamped to {@value #HYTALE_ZONE_MIN}–{@value #HYTALE_ZONE_MAX} for tier chances.
     */
    public static int resolveHytaleZone(Player player) {
        int raw = resolveWorldMapZoneIndexRaw(player);
        return Math.max(HYTALE_ZONE_MIN, Math.min(HYTALE_ZONE_MAX, raw));
    }

    /**
     * Raw parsed index from region/zone names (before clamping to Hytale 1–4).
     */
    public static int resolveWorldMapZoneIndexRaw(Player player) {
        if (player == null) {
            return 1;
        }
        try {
            WorldMapTracker tracker = player.getWorldMapTracker();
            WorldMapTracker.ZoneDiscoveryInfo zoneInfo = tracker != null ? tracker.getCurrentZone() : null;
            String regionName = zoneInfo != null ? zoneInfo.regionName() : null;
            String zoneName = zoneInfo != null ? zoneInfo.zoneName() : null;
            int parsed = parseZoneId(regionName);
            if (parsed < 0) {
                parsed = parseZoneId(zoneName);
            }
            return parsed >= 0 ? parsed : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static int parseZoneId(String name) {
        if (name == null || name.isEmpty()) {
            return -1;
        }
        Matcher matcher = ZONE_NUM.matcher(name);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        Matcher fallback = DIGITS.matcher(name);
        if (fallback.find()) {
            try {
                return Integer.parseInt(fallback.group());
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }
}
