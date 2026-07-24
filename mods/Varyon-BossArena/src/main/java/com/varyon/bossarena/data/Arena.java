package com.varyon.bossarena.data;

import com.varyon.bossarena.util.NotificationRadiusConstants;
import org.joml.Vector3d;

public class Arena {
    public String arenaId;
    public String worldName;
    public double x;
    public double y;
    public double z;
    /** Distance (blocks) within which players see boss event title/subtitle for this arena. */
    public double notificationRadius = NotificationRadiusConstants.DEFAULT;
    /**
     * Optional distance (blocks) within which players are eligible for loot when a boss dies
     * at this arena. When &lt;= 0, BossArena falls back to the boss loot table radius.
     */
    /** Loot eligibility radius (blocks). Default 30 for new arenas. Values &lt;= 0 fall back to boss loot table. */
    public double lootRadius = 30.0d;
    /**
     * Legacy: ignored at runtime. Proximity gating is configured per planification rule
     * ({@code requirePlayerInRadius}) using {@link #proximityRadius}.
     */
    public boolean proximityEnabled = false;
    /** Distance (blocks) used when a planification rule has "Attendre joueur" enabled. */
    public double proximityRadius = 0.0d;
    /** Legacy: ignored at runtime (respawn delay lives on planification rules). */
    public long proximityCooldownSeconds = 60L;

    // For GSON
    public Arena() {}

    public Arena(String arenaId, String worldName, Vector3d position) {
        this.arenaId = arenaId;
        this.worldName = worldName;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public Vector3d getPosition() {
        return new Vector3d(x, y, z);
    }

    /** Returns notification radius in blocks, clamped to valid range. Missing or invalid values default to 100. */
    public double getNotificationRadius() {
        return NotificationRadiusConstants.clamp(notificationRadius);
    }

    /** Returns loot radius in blocks. Values &lt;= 0 mean "use boss loot radius instead". */
    public double getLootRadius() {
        if (!Double.isFinite(lootRadius) || lootRadius <= 0.0d) {
            return 0.0d;
        }
        return lootRadius;
    }

    /** Returns proximity radius in blocks. Values &lt;= 0 mean proximity spawning is disabled. */
    public double getProximityRadius() {
        if (!Double.isFinite(proximityRadius) || proximityRadius <= 0.0d) {
            return 0.0d;
        }
        return proximityRadius;
    }

    /** Returns the proximity cooldown in seconds, clamped to a sane range. Non-positive values fall back to the default. */
    public long getProximityCooldownSeconds(long defaultSeconds) {
        long value = proximityCooldownSeconds;
        if (value <= 0L) {
            value = defaultSeconds;
        }
        long min = 1L;
        long max = 7L * 24L * 60L * 60L;
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    /**
     * Returns the radius (blocks) used for the event banner visibility.
     * Uses loot radius when set; otherwise falls back to notification radius for backward compatibility.
     */
    public double getBannerRadius() {
        double loot = getLootRadius();
        if (loot > 0.0d) {
            return loot;
        }
        return getNotificationRadius();
    }

    @Override
    public String toString() {
        return String.format("Arena[%s @ %.1f, %.1f, %.1f in %s]", arenaId, x, y, z, worldName);
    }
}