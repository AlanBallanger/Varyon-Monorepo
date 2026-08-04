package fr.varyon.vrpg.rpg;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.ui.ProfessionXpHud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public final class ProfessionManager extends AbstractPlayerManager<PlayerAccount, Profession> {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("VaryonRPG");
    public static final long RECONVERT_COOLDOWN_MS = 12L * 60L * 60L * 1000L;

    private final ProfessionStorage storage;

    public ProfessionManager(@Nonnull Path dataDirectory) {
        super(LOGGER, "VaryonRPG-AutoSave");
        this.storage = new SqliteProfessionStorage(dataDirectory);
        this.storage.initialize().join();
        LOGGER.at(Level.INFO).log("ProfessionManager ready (%s, auto-save %ds)",
            storage.getName(), AUTOSAVE_SECONDS);
    }

    @Nonnull
    public ProfessionStorage getStorage() {
        return storage;
    }

    @Override
    protected PlayerAccount loadAccount(@Nonnull UUID uuid) {
        return storage.loadPlayer(uuid).join();
    }

    @Override
    protected String getPlayerName(@Nonnull PlayerAccount account) {
        return account.getPlayerName();
    }

    @Override
    protected void setPlayerName(@Nonnull PlayerAccount account, @Nonnull String name) {
        account.setPlayerName(name);
    }

    @Override
    protected int applyWholeXp(@Nonnull PlayerAccount account, @Nonnull Profession key, long wholeXp) {
        return account.getProgress(key).addXp(wholeXp);
    }

    @Override
    protected void sendXpNotification(@Nonnull PlayerRef playerRef, @Nonnull Profession profession, @Nonnull String xpStr) {
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin != null && plugin.getUiPreferencesManager() != null
            && !plugin.getUiPreferencesManager().get(playerRef.getUuid()).xpNotificationsEnabled) {
            return;
        }
        if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) {
            LOGGER.at(Level.INFO).log("[XpNotif] %s %s +%s XP", playerRef.getUsername(), profession.name(), xpStr);
        }
        try {
            Message msg = Message.raw("+" + xpStr + " XP").color(new Color(0x5BFF7F));
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), msg, null, profession.getIconPath());
        } catch (Exception ignored) {}
    }

    public int addXp(@Nonnull UUID uuid, @Nonnull Profession profession, double amount) {
        return addXpInternal(uuid, profession, amount);
    }

    public int addXp(@Nonnull UUID uuid, @Nonnull Profession profession, double amount, @Nonnull PlayerRef playerRef) {
        if (CreativeGate.isCreative(playerRef)) return 0;
        double multiplier = slotMultiplier(uuid, profession);
        double effective = amount * multiplier;
        int levelsGained = addXpInternal(uuid, profession, effective);
        if (effective > 0) {
            scheduleXpNotif(uuid, playerRef, profession, effective, profession.getDebounceMs());
            ProfessionXpHud.refreshIfPresent(uuid);
        }
        return levelsGained;
    }

    private double slotMultiplier(@Nonnull UUID uuid, @Nonnull Profession profession) {
        PlayerAccount acc = cache.get(uuid);
        if (acc == null) return 1.0;
        if (profession == acc.getActiveSlot0()) return 1.0;
        if (profession == acc.getActiveSlot1()) return 0.7;
        return 1.0;
    }

    public void setLevel(@Nonnull UUID uuid, @Nonnull Profession profession, int level) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            acc.getProgress(profession).setLevel(level, 0L);
            dirty.add(uuid);
            ProfessionXpHud.refreshIfPresent(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void resetProfession(@Nonnull UUID uuid, @Nonnull Profession profession) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            acc.getProgress(profession).setLevel(1, 0L);
            acc.resetTalents(profession);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void resetAccount(@Nonnull UUID uuid) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            for (Profession p : Profession.values()) {
                acc.getProgress(p).setLevel(1, 0L);
                acc.resetTalents(p);
            }
            acc.setActiveSlot0(Profession.MINEUR);
            acc.setActiveSlot1(Profession.FERMIER);
            acc.setLastReconvertAt(0L);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public boolean allocateTalent(@Nonnull UUID uuid, @Nonnull Profession profession, @Nonnull String nodeId, int nodeMax) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            int currentRank = acc.getTalentRank(profession, nodeId);
            if (currentRank >= nodeMax) return false;
            if (acc.availableTalentPoints(profession) <= 0) return false;
            acc.setTalentRank(profession, nodeId, currentRank + 1);
            dirty.add(uuid);
            ProfessionXpHud.refreshIfPresent(uuid);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void resetTalents(@Nonnull UUID uuid, @Nonnull Profession profession) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).resetTalents(profession);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public boolean toggleTalentSound(@Nonnull UUID uuid, @Nonnull Profession profession, @Nonnull String nodeId) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            boolean next = !acc.isTalentSoundEnabled(profession, nodeId);
            acc.setTalentSoundEnabled(profession, nodeId, next);
            dirty.add(uuid);
            return next;
        } finally {
            lock.unlock();
        }
    }

    public enum ReconvertResult { SUCCESS, COOLDOWN_ACTIVE, INVALID_SLOT, NOT_UNLOCKED, SAME_PROFESSION, NO_CHANGE }

    public ReconvertResult setActiveSlot(@Nonnull UUID uuid, int slotIndex, @Nullable Profession newProfession) {
        if (slotIndex < 0 || slotIndex > 1) return ReconvertResult.INVALID_SLOT;
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            Profession current = slotIndex == 0 ? acc.getActiveSlot0() : acc.getActiveSlot1();
            Profession other   = slotIndex == 0 ? acc.getActiveSlot1() : acc.getActiveSlot0();
            if (newProfession != null) {
                if (!acc.isUnlocked(newProfession)) return ReconvertResult.NOT_UNLOCKED;
                if (newProfession == other) return ReconvertResult.SAME_PROFESSION;
            }
            if (newProfession == current) return ReconvertResult.NO_CHANGE;

            long now = System.currentTimeMillis();
            Profession leaving = current;
            if (leaving != null && newProfession != null && leaving != newProfession) {
                acc.resetTalents(leaving);
            }
            if (slotIndex == 0) acc.setActiveSlot0(newProfession);
            else                acc.setActiveSlot1(newProfession);
            acc.setLastReconvertAt(now);
            dirty.add(uuid);
            ProfessionXpHud.refreshIfPresent(uuid);
            return ReconvertResult.SUCCESS;
        } finally {
            lock.unlock();
        }
    }

    public long getReconvertCooldownRemainingMs(@Nonnull UUID uuid) {
        PlayerAccount acc = cache.get(uuid);
        if (acc == null || acc.getLastReconvertAt() == 0L) return 0L;
        long left = RECONVERT_COOLDOWN_MS - (System.currentTimeMillis() - acc.getLastReconvertAt());
        return Math.max(0L, left);
    }

    @Override
    protected void saveAccountOnDisconnect(@Nonnull UUID uuid, @Nonnull PlayerAccount account) {
        storage.savePlayer(uuid, account);
    }

    @Override
    protected void flushDirty() {
        if (dirty.isEmpty()) return;
        Set<UUID> snapshot = new HashSet<>(dirty);
        dirty.clear();
        Map<UUID, PlayerAccount> map = new HashMap<>();
        for (UUID u : snapshot) {
            PlayerAccount acc = cache.get(u);
            if (acc != null) map.put(u, acc);
        }
        storage.saveAll(map).exceptionally(t -> {
            LOGGER.at(Level.WARNING).log("Auto-save failed, will retry next cycle: %s", t.getMessage());
            dirty.addAll(snapshot);
            return null;
        });
    }

    public double getXpBoostMultiplier(@Nonnull UUID uuid, @Nonnull Profession profession) {
        PlayerAccount acc = cache.get(uuid);
        return acc != null ? acc.getBoostMultiplier(profession) : 0.0;
    }

    public void applyXpBoost(@Nonnull UUID uuid, @Nonnull Profession profession,
                              int tier, double bonus, long durationMs) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            PlayerAccount acc = getOrLoad(uuid);
            XpBoost existing = acc.getBoost(profession);
            long now = System.currentTimeMillis();
            if (existing != null) {
                if (tier == existing.getTier()) {
                    existing.setExpiryMs(existing.getExpiryMs() + durationMs);
                } else if (tier > existing.getTier()) {
                    acc.setBoost(profession, new XpBoost(tier, bonus, now + durationMs));
                }
            } else {
                acc.setBoost(profession, new XpBoost(tier, bonus, now + durationMs));
            }
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public java.util.List<LeaderboardEntry> getLeaderboard(@Nonnull Profession profession) {
        if (storage instanceof SqliteProfessionStorage sql) {
            return sql.leaderboardEntriesForProfession(profession);
        }
        return java.util.Collections.emptyList();
    }

    public void forceSave() {
        flushDirty();
    }

    public void shutdown() {
        LOGGER.at(Level.INFO).log("ProfessionManager shutdown starting...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        try {
            storage.saveAllSync(new HashMap<>(cache));
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Final flush failed: %s", e.getMessage());
        }
        try {
            storage.shutdown().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Storage shutdown error: %s", e.getMessage());
        }
        cache.clear();
        locks.clear();
        dirty.clear();
        LOGGER.at(Level.INFO).log("ProfessionManager shutdown complete");
    }
}
