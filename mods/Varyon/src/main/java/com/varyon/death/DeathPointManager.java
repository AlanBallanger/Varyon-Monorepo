package com.varyon.death;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DeathPointManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DEATH_POINTS_FILE = "death_points.toml";
    private static final String SECTION = "players";

    private final Path dataDirectory;
    private final Object returnPenaltyLock = new Object();
    private final Map<UUID, DeathPoint> deathPoints = new ConcurrentHashMap<>();
    private final Map<UUID, Long> returnBlockingUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> returnMultiplierExpiresAt = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> returnCostMultipliers = new ConcurrentHashMap<>();

    public static class DeathPoint {
        private final String world;
        private final double x;
        private final double y;
        private final double z;
        private final long timestamp;
        private boolean used;

        public DeathPoint(String world, double x, double y, double z, long timestamp, boolean used) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
            this.used = used;
        }

        public String getWorld()  { return world; }
        public double getX()      { return x; }
        public double getY()      { return y; }
        public double getZ()      { return z; }
        public long getTimestamp(){ return timestamp; }
        public boolean isUsed()   { return used; }
        public void markUsed()    { this.used = true; }

        public boolean isExpired(int expirationMinutes) {
            return (System.currentTimeMillis() - timestamp) > expirationMinutes * 60 * 1000L;
        }
    }

    public DeathPointManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        load();
    }

    public void recordDeathPoint(@Nonnull UUID playerId, @Nonnull String world, double x, double y, double z) {
        deathPoints.put(playerId, new DeathPoint(world, x, y, z, System.currentTimeMillis(), false));
        save();
        LOGGER.at(Level.INFO).log("Recorded death point for " + playerId + " at " + world + ":" + (int)x + "," + (int)y + "," + (int)z);
    }

    @Nullable
    public DeathPoint getDeathPoint(@Nonnull UUID playerId) {
        return deathPoints.get(playerId);
    }

    public void removeDeathPoint(@Nonnull UUID playerId) {
        deathPoints.remove(playerId);
        save();
        LOGGER.at(Level.INFO).log("Removed death point for " + playerId);
    }

    public void markDeathPointUsed(@Nonnull UUID playerId) {
        DeathPoint point = deathPoints.get(playerId);
        if (point != null) {
            point.markUsed();
            save();
            LOGGER.at(Level.INFO).log("Marked death point as used for " + playerId);
        }
    }

    public boolean isOnCooldown(@Nonnull UUID playerId) {
        synchronized (returnPenaltyLock) {
            sweepExpiredReturnPenalty(playerId);
            Long blockingEnd = returnBlockingUntil.get(playerId);
            return blockingEnd != null && System.currentTimeMillis() < blockingEnd;
        }
    }

    public long getCooldownRemainingSeconds(@Nonnull UUID playerId) {
        synchronized (returnPenaltyLock) {
            sweepExpiredReturnPenalty(playerId);
            Long blockingEnd = returnBlockingUntil.get(playerId);
            if (blockingEnd == null) {
                return 0;
            }
            return Math.max(0, (blockingEnd - System.currentTimeMillis()) / 1000);
        }
    }

    public int getReturnCostMultiplier(@Nonnull UUID playerId) {
        synchronized (returnPenaltyLock) {
            sweepExpiredReturnPenalty(playerId);
            if (!returnCostMultipliers.containsKey(playerId)) {
                return 1;
            }
            return returnCostMultipliers.getOrDefault(playerId, 1);
        }
    }

    public void recordReturnUse(@Nonnull UUID playerId, int cooldownSeconds) {
        synchronized (returnPenaltyLock) {
            sweepExpiredReturnPenalty(playerId);
            long now = System.currentTimeMillis();
            boolean blockingActive = returnBlockingUntil.containsKey(playerId)
                && now < returnBlockingUntil.get(playerId);
            int current = blockingActive
                ? returnCostMultipliers.getOrDefault(playerId, 1)
                : 1;
            returnCostMultipliers.put(playerId, current * 2);
            returnBlockingUntil.put(playerId, now + cooldownSeconds * 1000L);
            returnMultiplierExpiresAt.put(playerId, now + (cooldownSeconds + 1L) * 1000L);
        }
    }

    private void sweepExpiredReturnPenalty(@Nonnull UUID playerId) {
        long now = System.currentTimeMillis();
        Long multExp = returnMultiplierExpiresAt.get(playerId);
        if (multExp != null && now >= multExp) {
            returnCostMultipliers.remove(playerId);
            returnMultiplierExpiresAt.remove(playerId);
        }
        Long blockingEnd = returnBlockingUntil.get(playerId);
        if (blockingEnd != null && now >= blockingEnd) {
            returnBlockingUntil.remove(playerId);
        }
        if (!returnBlockingUntil.containsKey(playerId) && !returnMultiplierExpiresAt.containsKey(playerId)) {
            returnCostMultipliers.remove(playerId);
        }
    }

    private void load() {
        File file = dataDirectory.resolve(DEATH_POINTS_FILE).toFile();
        if (!file.exists()) {
            LOGGER.at(Level.INFO).log("No death points file found, starting fresh");
            return;
        }
        try {
            Toml toml = new Toml().read(file);
            Toml playersTable = toml.getTable(SECTION);
            if (playersTable == null) return;

            for (Map.Entry<String, Object> entry : playersTable.entrySet()) {
                String key = entry.getKey();
                Toml sub = playersTable.getTable(key);
                if (sub == null) continue;
                try {
                    UUID uuid = UUID.fromString(key);
                    String world     = sub.getString("world", "default");
                    double x         = sub.getDouble("x", 0.0);
                    double y         = sub.getDouble("y", 64.0);
                    double z         = sub.getDouble("z", 0.0);
                    long   timestamp = sub.getLong("timestamp", System.currentTimeMillis());
                    boolean used     = sub.getBoolean("used", false);
                    deathPoints.put(uuid, new DeathPoint(world, x, y, z, timestamp, used));
                } catch (IllegalArgumentException ignored) {
                    LOGGER.at(Level.WARNING).log("Invalid UUID in death_points.toml: " + key);
                }
            }
            LOGGER.at(Level.INFO).log("Loaded " + deathPoints.size() + " death point(s)");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load death points: " + e.getMessage());
        }
    }

    private void save() {
        try {
            dataDirectory.toFile().mkdirs();
            File file = dataDirectory.resolve(DEATH_POINTS_FILE).toFile();
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(SECTION).append("]\n\n");
            for (Map.Entry<UUID, DeathPoint> entry : deathPoints.entrySet()) {
                DeathPoint p = entry.getValue();
                sb.append("[").append(SECTION).append(".\"").append(entry.getKey()).append("\"]\n");
                sb.append("world     = \"").append(p.getWorld()).append("\"\n");
                sb.append("x         = ").append(p.getX()).append("\n");
                sb.append("y         = ").append(p.getY()).append("\n");
                sb.append("z         = ").append(p.getZ()).append("\n");
                sb.append("timestamp = ").append(p.getTimestamp()).append("\n");
                sb.append("used      = ").append(p.isUsed()).append("\n\n");
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(sb.toString());
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save death points: " + e.getMessage());
        }
    }

    public void shutdown() {
        save();
    }
}
