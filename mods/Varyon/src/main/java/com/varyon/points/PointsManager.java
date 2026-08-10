package com.varyon.points;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.varyon.VaryonPlugin;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PointsManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PointsDatabase database;
    private final Map<UUID, Double> pointsCache = new ConcurrentHashMap<>();
    private final GuildGaugePlayerWindow guildGaugePlayerWindow = new GuildGaugePlayerWindow();
    private final Object globalBalanceLock = new Object();
    private volatile ScheduledFuture<?> guildGaugeSampler;
    private GlobalRewardsManager rewardsManager;

    public PointsManager(@Nonnull File pluginFolder) {
        this.database = new PointsDatabase(pluginFolder);
        this.database.initialize();
        this.guildGaugeSampler = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> guildGaugePlayerWindow.recordSample(countOnlinePlayersRaw()),
                0,
                GuildGaugePlayerWindow.SAMPLE_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    public void setRewardsManager(GlobalRewardsManager rewardsManager) {
        this.rewardsManager = rewardsManager;
    }

    public double getPoints(UUID playerUuid) {
        return pointsCache.computeIfAbsent(playerUuid, database::getPoints);
    }

    public int getPointsDisplay(UUID playerUuid) {
        return (int) Math.floor(getPoints(playerUuid));
    }

    public void addPoints(UUID playerUuid, String playerName, double amount) {
        if (amount == 0) return;
        double current = getPoints(playerUuid);
        double newAmount = Math.max(0, current + amount);
        pointsCache.put(playerUuid, newAmount);
        database.setPointsUncapped(playerUuid, playerName, newAmount);
        LOGGER.at(Level.FINE).log("Player " + playerName + " " + (amount > 0 ? "+" : "") +
            String.format("%.2f", amount) + " points de faction (total: " + String.format("%.1f", newAmount) + ")");
    }

    /**
     * Adds points but only up to {@code cap}.
     * If the player is already at or above the cap, nothing is added.
     * Returns the amount actually added.
     */
    public double addPointsCapped(UUID playerUuid, String playerName, double amount, int cap) {
        if (amount <= 0) return 0;
        double current = getPoints(playerUuid);
        if (current >= cap) return 0;
        double actual = Math.min(amount, cap - current);
        addPoints(playerUuid, playerName, actual);
        return actual;
    }

    public void setPoints(UUID playerUuid, String playerName, double amount) {
        pointsCache.put(playerUuid, amount);
        database.setPoints(playerUuid, playerName, amount);
    }

    public int clearCarriedFactionPoints(@Nonnull UUID playerUuid, @Nonnull String playerName) {
        double current = getPoints(playerUuid);
        if (current <= 0.0) {
            return 0;
        }
        int lostDisplay = (int) Math.floor(current);
        setPoints(playerUuid, playerName, 0);
        LOGGER.at(Level.INFO).log("Forfeited carried faction points for " + playerName + ": " + lostDisplay + " (left Varyon world)");
        return lostDisplay;
    }

    public void setPointsUncapped(UUID playerUuid, String playerName, double amount) {
        pointsCache.put(playerUuid, amount);
        database.setPointsUncapped(playerUuid, playerName, amount);
    }

    public List<PlayerPointsData> getTopPlayers(int limit) {
        return database.getTopPlayers(limit);
    }

    public int getPlayerRank(UUID playerUuid) {
        return database.getPlayerRank(playerUuid);
    }

    public void loadPlayer(UUID playerUuid) {
        double points = database.getPoints(playerUuid);
        pointsCache.put(playerUuid, points);
    }

    public void savePlayer(UUID playerUuid) {
        pointsCache.remove(playerUuid);
    }

    public void saveAll() {
        pointsCache.clear();
    }

    public void shutdown() {
        ScheduledFuture<?> sampler = this.guildGaugeSampler;
        if (sampler != null) {
            sampler.cancel(false);
        }
        guildGaugePlayerWindow.clear();
        saveAll();
        database.close();
    }

    public int getGlobalBalance() {
        int raw = database.getGlobalBalance();
        return GuildGaugeScale.clamp(raw, getGuildGaugeAbsMax());
    }

    public int getGuildGaugeAbsMax() {
        int n = guildGaugePlayerWindow.effectivePlayerCount(this::countOnlinePlayersRaw);
        return GuildGaugeScale.maxAbsForOnlineCount(n);
    }

    private int countOnlinePlayersRaw() {
        try {
            int n = 0;
            for (PlayerRef ignored : Universe.get().getPlayers()) {
                n++;
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }


    public boolean canApplyGuildContribution(int signedContribution) {
        return true;
    }

    public void addToGlobalBalance(int amount) {
        if (amount == 0) {
            return;
        }
        synchronized (globalBalanceLock) {
            int max = getGuildGaugeAbsMax();
            int current = GuildGaugeScale.clamp(database.getGlobalBalance(), max);
            int written = GuildGaugeScale.clamp(current + amount, max);
            if (written == max || written == -max) {
                written = 0;
            }
            database.setGlobalBalance(written);
            if (rewardsManager != null) {
                rewardsManager.checkAndDistributeRewards();
            }
        }
        broadcastBalanceUpdate();
    }

    public void setGlobalBalance(int amount) {
        synchronized (globalBalanceLock) {
            int max = getGuildGaugeAbsMax();
            int written = GuildGaugeScale.clamp(amount, max);
            if (written == max || written == -max) {
                written = 0;
            }
            database.setGlobalBalance(written);
            if (rewardsManager != null) {
                rewardsManager.checkAndDistributeRewards();
            }
        }
        broadcastBalanceUpdate();
    }

    private void broadcastBalanceUpdate() {
        try {
            VaryonPlugin plugin = VaryonPlugin.getInstance();
            if (plugin != null && plugin.getHudManager() != null) {
                plugin.getHudManager().broadcastBalanceUpdate();
            }
        } catch (Exception ignored) {
        }
    }
}
