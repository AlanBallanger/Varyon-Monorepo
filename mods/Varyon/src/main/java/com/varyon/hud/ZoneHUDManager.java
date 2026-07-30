package com.varyon.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.MessagesConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.essence.EssenceManager;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ZoneHUDManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final int PAGE_SWITCH_TICKS = 5;

    private static final long ERROR_LOG_BACKOFF_MS = 5000;

    private final Map<UUID, ZoneHUD> playerHuds = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPlayerErrorLoggedAt = new ConcurrentHashMap<>();
    private ZoneConfig zoneConfig;
    private MessagesConfig messagesConfig;
    private ZonePermissionsConfig zonePermsConfig;
    private ScheduledFuture<?> updateTask;
    private int tickCounter = 0;
    private volatile long lastTaskErrorLoggedAt = 0;

    public ZoneHUDManager(@Nonnull ZoneConfig zoneConfig, @Nonnull MessagesConfig messagesConfig,
                          @Nonnull ZonePermissionsConfig zonePermsConfig) {
        this.zoneConfig = zoneConfig;
        this.messagesConfig = messagesConfig;
        this.zonePermsConfig = zonePermsConfig;
        LOGGER.at(Level.INFO).log("ZoneHUDManager initialized");
        startUpdateTask();
    }

    public void applyReloadedConfigs(@Nonnull ConfigManager configManager) {
        this.zoneConfig = configManager.getZoneConfig();
        this.messagesConfig = configManager.getMessagesConfig();
        this.zonePermsConfig = configManager.getZonePermissionsConfig();
    }

    private void startUpdateTask() {
        updateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                tickCounter++;
                boolean switchPage = (tickCounter % PAGE_SWITCH_TICKS == 0);
                updateAllHuds(switchPage);
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                if (now - lastTaskErrorLoggedAt >= ERROR_LOG_BACKOFF_MS) {
                    lastTaskErrorLoggedAt = now;
                    LOGGER.at(Level.WARNING).log("Error updating HUDs: " + e.getMessage());
                }
            }
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void updateAllHuds(boolean switchPage) {
        SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
        boolean safeZoneAvailable = szm != null;
        String quadrantName = "";
        long timeRemaining = 0;

        if (safeZoneAvailable) {
            quadrantName = szm.getCurrentQuadrant() != null ? szm.getCurrentQuadrant().name() : "?";
            timeRemaining = szm.getTimeUntilRotation();
        }

        for (Map.Entry<UUID, ZoneHUD> entry : playerHuds.entrySet()) {
            UUID playerId = entry.getKey();
            ZoneHUD hud = entry.getValue();

            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null || playerRef.getReference() == null) {
                continue;
            }

            try {
                Player player = playerCache.get(playerId);
                String worldName = (player != null && player.getWorld() != null)
                        ? player.getWorld().getName() : "";
                Transform transform = playerRef.getTransform();
                double x = transform.getPosition().x;
                double z = transform.getPosition().z;
                double distance = ZoneCalculator.calculate2DDistance(x, z);
                DifficultyZone zone = ZoneCalculator.getZoneAtPosition(x, z, worldName, zoneConfig);

                boolean inSafe = safeZoneAvailable && szm.isInSafeZone(x, z);

                boolean lootActive = false;
                int maxEssenceCap = 1000;
                if (zone != null) lootActive = zonePermsConfig.canAccessZone(playerRef, zone.getZoneId());
                EssenceManager em = VaryonPlugin.getStaticEssenceManager();
                double current = em != null ? em.getEssence(playerId) : 0.0;
                maxEssenceCap = zonePermsConfig.getEffectiveCap(playerRef, current);

                if (switchPage) hud.nextPage();
                hud.updateZoneInfo(zone, distance, inSafe, quadrantName, timeRemaining, switchPage, lootActive, maxEssenceCap);
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                Long lastLogged = lastPlayerErrorLoggedAt.get(playerId);
                if (lastLogged == null || now - lastLogged >= ERROR_LOG_BACKOFF_MS) {
                    lastPlayerErrorLoggedAt.put(playerId, now);
                    LOGGER.at(Level.WARNING).log("Error updating HUD for player " + playerId + ": " + e.getMessage());
                }
            }
        }
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
    }

    public boolean isAvailable() {
        return true;
    }

    public void registerPlayer(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        String worldName = world.getName();
        UUID playerId = playerRef.getUuid();

        playerHuds.remove(playerId);
        playerCache.remove(playerId);
        lastPlayerErrorLoggedAt.remove(playerId);
        removeHud(player);

        if (!zoneConfig.isWorldEnabled(worldName)) {
            return;
        }

        ZoneHUD hud = new ZoneHUD(playerRef, messagesConfig);
        playerHuds.put(playerId, hud);
        playerCache.put(playerId, player);

        try {
            player.getHudManager().addCustomHud(playerRef, hud);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to register HUD: " + e.getMessage());
        }
    }

    public void removePlayer(@Nonnull UUID playerId) {
        playerHuds.remove(playerId);
        Player player = playerCache.remove(playerId);
        lastPlayerErrorLoggedAt.remove(playerId);
        removeHud(player);
    }

    private void removeHud(@Nullable Player player) {
        if (player == null) return;
        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef != null) {
                player.getHudManager().removeCustomHud(playerRef, ZoneHUD.HUD_KEY);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to remove HUD: " + e.getMessage());
        }
    }

    public void broadcastBalanceUpdate() {
        for (ZoneHUD hud : playerHuds.values()) {
            try {
                hud.updateGlobalBalance();
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to update global balance: " + e.getMessage());
            }
        }
    }
}
