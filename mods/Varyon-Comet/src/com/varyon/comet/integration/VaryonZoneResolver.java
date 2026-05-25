package com.varyon.comet.integration;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;

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
                return 0;
            }
            Object r = c.getMethod("getZoneId", PlayerRef.class).invoke(null, pr);
            if (r instanceof Integer) {
                int v = (Integer) r;
                if (v < VARYON_RING_MIN || v > VARYON_RING_MAX) {
                    return Math.max(VARYON_RING_MIN, Math.min(VARYON_RING_MAX, v));
                }
                return v;
            }
        } catch (Throwable ignored) {
        }
        return 0;
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
