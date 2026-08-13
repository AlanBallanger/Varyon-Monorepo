package fr.varyon.shop.util;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

/** Reads the current Hytale in-game time for a given world, for shop rotation timing. */
public final class WorldDayUtil {
    /** Shop rotations flip at 07:00 in-game, not at in-game midnight. */
    private static final int ROTATION_HOUR = 7;
    private static final int ROTATION_SECOND_OF_DAY = ROTATION_HOUR * 3600;

    private WorldDayUtil() {
    }

    /**
     * Returns the current in-game "rotation day": the in-game day-of-year, except that before
     * 07:00 local in-game time it still counts as the previous day. This is the value shop
     * rotations key off, so a rotation actually flips at 07:00 rather than at in-game midnight.
     * Returns -1 if unavailable.
     */
    public static int currentDayOfYear(World world) {
        LocalDateTime gameTime = gameDateTime(world);
        if (gameTime == null) {
            return -1;
        }
        int dayOfYear = gameTime.getDayOfYear();
        int secondOfDay = gameTime.get(ChronoField.SECOND_OF_DAY);
        return secondOfDay < ROTATION_SECOND_OF_DAY ? dayOfYear - 1 : dayOfYear;
    }

    /**
     * Returns the real-world seconds remaining until the next 07:00 in-game rotation flip for
     * this world, or -1 if unavailable.
     */
    public static long secondsUntilNextRotation(World world) {
        if (world == null) {
            return -1;
        }
        LocalDateTime gameTime = gameDateTime(world);
        if (gameTime == null) {
            return -1;
        }
        int secondOfDay = gameTime.get(ChronoField.SECOND_OF_DAY);
        int gameSecondsRemaining = (ROTATION_SECOND_OF_DAY - secondOfDay + WorldTimeResource.SECONDS_PER_DAY) % WorldTimeResource.SECONDS_PER_DAY;
        if (gameSecondsRemaining == 0) {
            gameSecondsRemaining = WorldTimeResource.SECONDS_PER_DAY;
        }
        try {
            int realDaySeconds = world.getDaytimeDurationSeconds() + world.getNighttimeDurationSeconds();
            return Math.round(gameSecondsRemaining / (double) WorldTimeResource.SECONDS_PER_DAY * realDaySeconds);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static LocalDateTime gameDateTime(World world) {
        if (world == null) {
            return null;
        }
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
            return timeResource == null ? null : timeResource.getGameDateTime();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
