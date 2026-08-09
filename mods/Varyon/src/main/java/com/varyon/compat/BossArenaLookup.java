package com.varyon.compat;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;

/**
 * Optional lookup of Varyon-BossArena arenas at a world position.
 *
 * <p>BossArena depends on Varyon (compileOnly), so Varyon cannot reference its types directly
 * without a circular dependency. Access goes through reflection instead, and every failure
 * degrades to "no boss arena here" so extraction keeps its normal portal behaviour when the
 * BossArena mod is absent.
 */
public final class BossArenaLookup {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String REGISTRY_CLASS = "com.varyon.bossarena.data.ArenaRegistry";
    private static final String ARENA_CLASS = "com.varyon.bossarena.data.Arena";

    private static volatile Boolean available;
    private static Method getAllMethod;
    private static Method getPositionMethod;
    private static Method getLootRadiusMethod;
    private static java.lang.reflect.Field worldNameField;

    private BossArenaLookup() {}

    /**
     * Returns true when the position falls inside a registered boss arena's radius —
     * the same {@code lootRadius} value BossArena's config UI exposes as "arena radius"
     * and mirrors into {@code proximityRadius} for its own spawn gating.
     */
    public static boolean isInBossArena(String worldName, double x, double y, double z) {
        if (worldName == null || worldName.isBlank() || !ensureBridge()) {
            return false;
        }
        try {
            Collection<?> arenas = (Collection<?>) getAllMethod.invoke(null);
            if (arenas == null || arenas.isEmpty()) {
                return false;
            }
            for (Object arena : arenas) {
                if (arena == null) {
                    continue;
                }
                Object arenaWorld = worldNameField.get(arena);
                if (!(arenaWorld instanceof String s) || !worldName.equalsIgnoreCase(s)) {
                    continue;
                }
                double radius = (double) (Double) getLootRadiusMethod.invoke(arena);
                if (!Double.isFinite(radius) || radius <= 0.0d) {
                    radius = 30.0d;
                }
                Object position = getPositionMethod.invoke(arena);
                if (position == null) {
                    continue;
                }
                org.joml.Vector3d center = (org.joml.Vector3d) position;
                double dx = center.x - x;
                double dy = center.y - y;
                double dz = center.z - z;
                if ((dx * dx) + (dy * dy) + (dz * dz) <= radius * radius) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            available = false;
            LOGGER.at(Level.FINE).log("BossArena lookup failed: " + t);
            return false;
        }
    }

    private static boolean ensureBridge() {
        Boolean cached = available;
        if (cached != null) {
            return cached;
        }
        synchronized (BossArenaLookup.class) {
            if (available != null) {
                return available;
            }
            try {
                Class<?> registry = Class.forName(REGISTRY_CLASS);
                Class<?> arena = Class.forName(ARENA_CLASS);
                getAllMethod = registry.getMethod("getAll");
                getPositionMethod = arena.getMethod("getPosition");
                getLootRadiusMethod = arena.getMethod("getLootRadius");
                worldNameField = arena.getField("worldName");
                available = true;
                LOGGER.at(Level.INFO).log("BossArena detected: extraction will be instant inside boss arenas");
            } catch (Throwable t) {
                available = false;
                LOGGER.at(Level.INFO).log("BossArena absent, extraction uses portals everywhere");
            }
            return available;
        }
    }

    /** Test / reload helper. */
    public static void resetCache() {
        synchronized (BossArenaLookup.class) {
            available = null;
        }
    }
}
