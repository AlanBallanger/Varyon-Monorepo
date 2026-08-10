package com.varyon.safezone;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SafeZoneManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String STATE_FILE = "safezone_state.dat";
    
    private SafeZoneConfig config;
    private com.varyon.config.ZoneConfig zoneConfig;
    private final Path dataDirectory;
    private final Random random = new Random();
    
    private SafeZoneQuadrant currentQuadrant;
    private SafeZoneQuadrant nextQuadrant;
    private long rotationStartTime;
    private long nextRotationTime;
    private boolean isOverlapActive;
    
    private ScheduledFuture<?> rotationTask;
    private ScheduledFuture<?> announcementTask;
    private static final long ROTATION_ERROR_LOG_BACKOFF_MS = 10000;
    private volatile long lastRotationErrorLoggedAt = 0;

    public SafeZoneManager(@Nonnull SafeZoneConfig config, @Nonnull com.varyon.config.ZoneConfig zoneConfig, @Nonnull Path dataDirectory) {
        this.config = config;
        this.zoneConfig = zoneConfig;
        this.dataDirectory = dataDirectory;
        this.currentQuadrant = SafeZoneQuadrant.NORTH_EAST;
        this.nextQuadrant = SafeZoneQuadrant.SOUTH_EAST;
        this.rotationStartTime = System.currentTimeMillis();
        this.nextRotationTime = rotationStartTime + getRandomRotationDuration();
        this.isOverlapActive = false;
        
        loadState();
        
        if (config.isEnabled()) {
            startRotationTask();
            startAnnouncementTask();
            LOGGER.at(Level.INFO).log("SafeZone system initialized - Current safe zone: " + currentQuadrant.getDisplayName());
        }
    }

    public void applyReloadedConfigs(@Nonnull SafeZoneConfig safeZoneConfig,
                                     @Nonnull com.varyon.config.ZoneConfig zoneConfig) {
        this.config = safeZoneConfig;
        this.zoneConfig = zoneConfig;
    }

    private long getRandomRotationDuration() {
        long min = config.getMinRotationTimeMillis();
        long max = config.getMaxRotationTimeMillis();
        return min + (long)(random.nextDouble() * (max - min));
    }

    private void startRotationTask() {
        rotationTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                checkAndRotate();
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                if (now - lastRotationErrorLoggedAt >= ROTATION_ERROR_LOG_BACKOFF_MS) {
                    lastRotationErrorLoggedAt = now;
                    LOGGER.at(Level.WARNING).log("Error in rotation task: " + e.getMessage());
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void checkAndRotate() {
        long currentTime = System.currentTimeMillis();
        long timeUntilRotation = nextRotationTime - currentTime;
        long overlapDuration = config.getOverlapDurationMillis();
        
        if (!isOverlapActive && timeUntilRotation <= overlapDuration && timeUntilRotation > 0) {
            isOverlapActive = true;
            announceOverlapStart();
            LOGGER.at(Level.INFO).log("Overlap started - Both " + currentQuadrant.getDisplayName() + " and " + nextQuadrant.getDisplayName() + " are now safe");
        }
        
        if (currentTime >= nextRotationTime) {
            rotate();
        }
    }

    private void rotate() {
        currentQuadrant = nextQuadrant;
        nextQuadrant = currentQuadrant.next();
        isOverlapActive = false;
        
        rotationStartTime = System.currentTimeMillis();
        nextRotationTime = rotationStartTime + getRandomRotationDuration();
        
        announceRotation();
        saveState();
        
        LOGGER.at(Level.INFO).log("Safe zone rotated to: " + currentQuadrant.getDisplayName() + " (next: " + nextQuadrant.getDisplayName() + ")");
    }

    private void startAnnouncementTask() {
        announcementTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                announceTimeRemaining();
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error in announcement task: " + e.getMessage());
            }
        }, 60000, 300000, TimeUnit.MILLISECONDS);
    }

    private static final java.awt.Color VARYON_TAG_COLOR = java.awt.Color.decode("#AA55FF");

    private void announceRotation() {
        Message message = Message.join(
            Message.raw("[Varyon] ").color(VARYON_TAG_COLOR),
            Message.raw("La zone non-PVP est maintenant au " + currentQuadrant.getDisplayName() + " !").color(java.awt.Color.WHITE)
        );
        broadcastMessage(message);
    }

    private void announceOverlapStart() {
        Message message = Message.join(
            Message.raw("[Varyon] ").color(VARYON_TAG_COLOR),
            Message.raw("Début de la transition ! Les zones " + currentQuadrant.getDisplayName() +
                        " et " + nextQuadrant.getDisplayName() + " sont toutes les deux non-PVP pendant ").color(java.awt.Color.WHITE),
            Message.raw("10 minutes").color(java.awt.Color.GREEN),
            Message.raw(".").color(java.awt.Color.WHITE)
        );
        broadcastMessage(message);
    }

    private void announceTimeRemaining() {
        long currentTime = System.currentTimeMillis();
        long timeRemaining = nextRotationTime - currentTime;

        if (timeRemaining <= 0) {
            return;
        }

        long minutesRemaining = timeRemaining / 60000;

        if (minutesRemaining <= 5) {
            announceRotationCountdown(minutesRemaining);
        }
    }

    private void announceRotationCountdown(long minutesRemaining) {
        String duration = minutesRemaining + " minute" + (minutesRemaining > 1 ? "s" : "");
        Message message = Message.join(
            Message.raw("[Varyon] ").color(VARYON_TAG_COLOR),
            Message.raw("La zone non-PVP va tourner dans ").color(java.awt.Color.WHITE),
            Message.raw(duration).color(java.awt.Color.GREEN),
            Message.raw(" !").color(java.awt.Color.WHITE)
        );
        broadcastMessage(message);
    }

    private void broadcastMessage(Message message) {
        Universe.get().getWorlds().values().forEach(world -> {
            // Vérifier si ce monde est activé dans la config
            if (!zoneConfig.isWorldEnabled(world.getName())) {
                return;
            }

            world.execute(() -> {
                world.getPlayerRefs().forEach(playerRef -> {
                    playerRef.sendMessage(message);
                });
            });
        });
    }

    /**
     * Force une rotation immédiate de la zone PVP, hors cycle normal (ex: commande admin).
     */
    public void forceRotate() {
        rotate();
    }

    public boolean isInSafeZone(double x, double z) {
        return SafeZoneCalculator.isInSafeZone(x, z, currentQuadrant, nextQuadrant, isOverlapActive, config);
    }

    public SafeZoneQuadrant getCurrentQuadrant() {
        return currentQuadrant;
    }

    public SafeZoneQuadrant getNextQuadrant() {
        return nextQuadrant;
    }

    public boolean isOverlapActive() {
        return isOverlapActive;
    }

    public long getTimeUntilRotation() {
        return Math.max(0, nextRotationTime - System.currentTimeMillis());
    }

    private void loadState() {
        File stateFile = dataDirectory.resolve(STATE_FILE).toFile();
        if (!stateFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(stateFile))) {
            currentQuadrant = SafeZoneQuadrant.valueOf(ois.readUTF());
            nextQuadrant = SafeZoneQuadrant.valueOf(ois.readUTF());
            rotationStartTime = ois.readLong();
            nextRotationTime = ois.readLong();
            isOverlapActive = ois.readBoolean();
            
            LOGGER.at(Level.INFO).log("Loaded safe zone state - Current: " + currentQuadrant.getDisplayName());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to load safe zone state, using defaults: " + e.getMessage());
        }
    }

    private void saveState() {
        try {
            dataDirectory.toFile().mkdirs();
            File stateFile = dataDirectory.resolve(STATE_FILE).toFile();
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(stateFile))) {
                oos.writeUTF(currentQuadrant.name());
                oos.writeUTF(nextQuadrant.name());
                oos.writeLong(rotationStartTime);
                oos.writeLong(nextRotationTime);
                oos.writeBoolean(isOverlapActive);
            }
            
            LOGGER.at(Level.INFO).log("Saved safe zone state");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to save safe zone state: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (rotationTask != null) {
            rotationTask.cancel(false);
        }
        if (announcementTask != null) {
            announcementTask.cancel(false);
        }
        saveState();
        LOGGER.at(Level.INFO).log("SafeZone system shutdown");
    }
}
