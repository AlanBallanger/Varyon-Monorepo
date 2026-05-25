package com.varyon.essence;

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

public class EssenceManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int GAUGE_HOLD_AFTER_MAX_MINUTES = 10;

    private final EssenceDatabase database;
    private final Map<UUID, Double> essenceCache = new ConcurrentHashMap<>();
    private final GuildGaugePlayerWindow guildGaugePlayerWindow = new GuildGaugePlayerWindow();
    private final Object globalBalanceLock = new Object();
    private volatile ScheduledFuture<?> guildGaugeSampler;
    private volatile ScheduledFuture<?> gaugeHoldResetTask;
    private boolean gaugeHoldActive;
    private Integer gaugeHoldPinnedBalance;
    private GlobalRewardsManager rewardsManager;

    public EssenceManager(@Nonnull File pluginFolder) {
        this.database = new EssenceDatabase(pluginFolder);
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

    public double getEssence(UUID playerUuid) {
        return essenceCache.computeIfAbsent(playerUuid, database::getEssence);
    }

    public int getEssenceDisplay(UUID playerUuid) {
        return (int) Math.floor(getEssence(playerUuid));
    }

    public void addEssence(UUID playerUuid, String playerName, double amount) {
        if (amount == 0) return;
        double current = getEssence(playerUuid);
        double newAmount = Math.max(0, current + amount);
        essenceCache.put(playerUuid, newAmount);
        database.setEssenceUncapped(playerUuid, playerName, newAmount);
        LOGGER.at(Level.FINE).log("Player " + playerName + " " + (amount > 0 ? "+" : "") +
            String.format("%.2f", amount) + " points de faction (total: " + String.format("%.1f", newAmount) + ")");
    }

    /**
     * Adds essence but only up to {@code cap}.
     * If the player is already at or above the cap, nothing is added.
     * Returns the amount actually added.
     */
    public double addEssenceCapped(UUID playerUuid, String playerName, double amount, int cap) {
        if (amount <= 0) return 0;
        double current = getEssence(playerUuid);
        if (current >= cap) return 0;
        double actual = Math.min(amount, cap - current);
        addEssence(playerUuid, playerName, actual);
        return actual;
    }

    public void setEssence(UUID playerUuid, String playerName, double amount) {
        essenceCache.put(playerUuid, amount);
        database.setEssence(playerUuid, playerName, amount);
    }

    public int clearCarriedFactionPoints(@Nonnull UUID playerUuid, @Nonnull String playerName) {
        double current = getEssence(playerUuid);
        if (current <= 0.0) {
            return 0;
        }
        int lostDisplay = (int) Math.floor(current);
        setEssence(playerUuid, playerName, 0);
        LOGGER.at(Level.INFO).log("Forfeited carried faction points for " + playerName + ": " + lostDisplay + " (left Varyon world)");
        return lostDisplay;
    }
    
    public void setEssenceUncapped(UUID playerUuid, String playerName, double amount) {
        essenceCache.put(playerUuid, amount);
        database.setEssenceUncapped(playerUuid, playerName, amount);
    }

    public List<PlayerEssenceData> getTopPlayers(int limit) {
        return database.getTopPlayers(limit);
    }

    public int getPlayerRank(UUID playerUuid) {
        return database.getPlayerRank(playerUuid);
    }

    public void loadPlayer(UUID playerUuid) {
        double essence = database.getEssence(playerUuid);
        essenceCache.put(playerUuid, essence);
    }

    public void savePlayer(UUID playerUuid) {
        essenceCache.remove(playerUuid);
    }

    public void saveAll() {
        essenceCache.clear();
    }

    public void shutdown() {
        ScheduledFuture<?> sampler = this.guildGaugeSampler;
        if (sampler != null) {
            sampler.cancel(false);
        }
        synchronized (globalBalanceLock) {
            cancelGaugeHoldTaskLocked();
            gaugeHoldActive = false;
            gaugeHoldPinnedBalance = null;
        }
        guildGaugePlayerWindow.clear();
        saveAll();
        database.close();
    }

    public int getGlobalBalance() {
        synchronized (globalBalanceLock) {
            if (gaugeHoldActive && gaugeHoldPinnedBalance != null) {
                return gaugeHoldPinnedBalance;
            }
        }
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
        if (signedContribution == 0) {
            return true;
        }
        synchronized (globalBalanceLock) {
            if (!gaugeHoldActive || gaugeHoldPinnedBalance == null) {
                return true;
            }
            int p = gaugeHoldPinnedBalance;
            return !((p > 0 && signedContribution > 0) || (p < 0 && signedContribution < 0));
        }
    }

    public void addToGlobalBalance(int amount) {
        if (amount == 0) {
            return;
        }
        synchronized (globalBalanceLock) {
            if (gaugeHoldActive && gaugeHoldPinnedBalance != null) {
                int p = gaugeHoldPinnedBalance;
                if ((p > 0 && amount > 0) || (p < 0 && amount < 0)) {
                    return;
                }
                cancelGaugeHoldTaskLocked();
                gaugeHoldActive = false;
                gaugeHoldPinnedBalance = null;
            }
            int max = getGuildGaugeAbsMax();
            int current = GuildGaugeScale.clamp(database.getGlobalBalance(), max);
            int written = GuildGaugeScale.clamp(current + amount, max);
            database.setGlobalBalance(written);
            if (rewardsManager != null) {
                rewardsManager.checkAndDistributeRewards();
            }
            if (written == max || written == -max) {
                startGaugeHoldLocked(written);
            }
        }
    }

    public void setGlobalBalance(int amount) {
        synchronized (globalBalanceLock) {
            cancelGaugeHoldTaskLocked();
            gaugeHoldActive = false;
            gaugeHoldPinnedBalance = null;
            int max = getGuildGaugeAbsMax();
            int written = GuildGaugeScale.clamp(amount, max);
            database.setGlobalBalance(written);
            if (rewardsManager != null) {
                rewardsManager.checkAndDistributeRewards();
            }
            if (written == max || written == -max) {
                startGaugeHoldLocked(written);
            }
        }
    }

    private void cancelGaugeHoldTaskLocked() {
        ScheduledFuture<?> t = gaugeHoldResetTask;
        if (t != null) {
            t.cancel(false);
            gaugeHoldResetTask = null;
        }
    }

    private void startGaugeHoldLocked(int pinnedBalance) {
        if (gaugeHoldActive) {
            return;
        }
        gaugeHoldActive = true;
        gaugeHoldPinnedBalance = pinnedBalance;
        cancelGaugeHoldTaskLocked();
        gaugeHoldResetTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            synchronized (globalBalanceLock) {
                gaugeHoldActive = false;
                gaugeHoldPinnedBalance = null;
                gaugeHoldResetTask = null;
                database.setGlobalBalance(0);
            }
            try {
                VaryonPlugin plugin = VaryonPlugin.getInstance();
                if (plugin != null && plugin.getHudManager() != null) {
                    plugin.getHudManager().broadcastBalanceUpdate();
                }
            } catch (Exception ignored) {
            }
        }, GAUGE_HOLD_AFTER_MAX_MINUTES, TimeUnit.MINUTES);
    }
}
