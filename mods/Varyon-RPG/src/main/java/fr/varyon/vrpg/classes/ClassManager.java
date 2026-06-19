package fr.varyon.vrpg.classes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import fr.varyon.vrpg.rpg.AbstractPlayerManager;
import fr.varyon.vrpg.ui.classes.ClassUnlockedActiveSkills;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public final class ClassManager extends AbstractPlayerManager<ClassAccount, PlayerClass> {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("VaryonRPG-Classes");
    private static final long XP_NOTIF_DEBOUNCE_MS = 1500L;

    private final SqliteClassStorage storage;
    private final ClassStatEngine statEngine = new ClassStatEngine();

    public ClassManager(@Nonnull Path dataDirectory) {
        super(LOGGER, "VaryonRPG-Classes-AutoSave");
        this.storage = new SqliteClassStorage(dataDirectory);
        this.storage.initialize().join();
        LOGGER.at(Level.INFO).log("ClassManager ready (auto-save %ds)", AUTOSAVE_SECONDS);
    }

    @Override
    protected ClassAccount loadAccount(@Nonnull UUID uuid) {
        return storage.loadPlayer(uuid).join();
    }

    @Override
    protected String getPlayerName(@Nonnull ClassAccount account) {
        return account.getPlayerName();
    }

    @Override
    protected void setPlayerName(@Nonnull ClassAccount account, @Nonnull String name) {
        account.setPlayerName(name);
    }

    @Override
    protected int applyWholeXp(@Nonnull ClassAccount account, @Nonnull PlayerClass key, long wholeXp) {
        return account.getProgress(key).addXp(wholeXp);
    }

    @Override
    protected void sendXpNotification(@Nonnull PlayerRef playerRef, @Nonnull PlayerClass playerClass, @Nonnull String xpStr) {
        try {
            Message msg = Message.raw("+" + xpStr + " XP").color(new Color(0xFFD700));
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), msg, null, (String) null);
        } catch (Exception ignored) {}
    }

    public int addXp(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass, double amount) {
        return addXpInternal(uuid, playerClass, amount);
    }

    public int addXp(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass, double amount,
                     @Nonnull PlayerRef playerRef) {
        int levelsGained = addXpInternal(uuid, playerClass, amount);
        if (amount > 0) scheduleXpNotif(uuid, playerRef, playerClass, amount, XP_NOTIF_DEBOUNCE_MS);
        return levelsGained;
    }

    public void setLevel(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass, int level) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).getProgress(playerClass).setLevel(level, 0L);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void setActiveClass(@Nonnull UUID uuid, @Nullable PlayerClass playerClass) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).setActiveClass(playerClass);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void setActiveSpec(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass,
                               @Nullable PlayerSpecialization spec) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).setActiveSpec(playerClass, spec);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public boolean allocateTalent(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass,
                                   @Nonnull String nodeId, int nodeMax) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            ClassAccount acc = getOrLoad(uuid);
            int currentRank = acc.getTalentRank(playerClass, nodeId);
            if (currentRank >= nodeMax) return false;
            if (acc.availableTalentPoints(playerClass, acc.getActiveSpec(playerClass)) <= 0) return false;
            acc.setTalentRank(playerClass, nodeId, currentRank + 1);
            dirty.add(uuid);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void resetTalents(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).resetTalents(playerClass);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void setSkillSlot(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass,
                             @Nonnull String slotId, @Nonnull String itemId) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).setSkillSlot(playerClass, slotId, itemId);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void clearSkillSlot(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass, @Nonnull String slotId) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).clearSkillSlot(playerClass, slotId);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void clearSkillSlots(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).clearSkillSlots(playerClass);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void pruneInvalidSkillSlots(@Nonnull ClassAccount acc, @Nonnull PlayerClass playerClass) {
        acc.getSkillSlots(playerClass).entrySet().removeIf(e ->
            !ClassUnlockedActiveSkills.isUnlockedActive(acc, e.getValue()));
    }

    public void resetAccount(@Nonnull UUID uuid) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            ClassAccount acc = getOrLoad(uuid);
            for (PlayerClass c : PlayerClass.values()) {
                acc.getProgress(c).setLevel(1, 0L);
                acc.getProgress(c).setActiveSpec(null);
                acc.resetTalents(c);
                acc.getSkillSlots(c).clear();
            }
            acc.setActiveClass(null);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void resetProfile(@Nonnull UUID uuid, int profileIndex) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            ClassAccount acc = getOrLoad(uuid);
            ClassProfile profile = acc.getProfiles()[profileIndex];
            profile.setActiveClass(null);
            for (PlayerClass c : PlayerClass.values()) {
                profile.setSpec(c, null);
                profile.resetTalents(c);
                profile.clearSkillSlots(c);
            }
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
    }

    public void switchProfile(@Nonnull UUID uuid, int profileIndex, @Nonnull PlayerRef playerRef) {
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            getOrLoad(uuid).switchProfile(profileIndex);
            dirty.add(uuid);
        } finally {
            lock.unlock();
        }
        applyStats(uuid, playerRef);
    }

    public ClassStatEngine getStatEngine() {
        return statEngine;
    }

    public ClassPlayerStats applyStats(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = cache.get(uuid);
        return statEngine.computeAndApply(uuid, playerRef, acc);
    }

    @Override
    protected void saveAccountOnDisconnect(@Nonnull UUID uuid, @Nonnull ClassAccount account) {
        storage.savePlayer(uuid, account).join();
    }

    @Override
    public void onPlayerDisconnect(@Nonnull UUID uuid) {
        super.onPlayerDisconnect(uuid);
        statEngine.cleanup(uuid);
    }

    @Override
    protected void flushDirty() {
        if (dirty.isEmpty()) return;
        Set<UUID> snapshot = new HashSet<>(dirty);
        dirty.clear();
        for (UUID u : snapshot) {
            ClassAccount acc = cache.get(u);
            if (acc != null) {
                storage.savePlayer(u, acc).exceptionally(t -> {
                    LOGGER.at(Level.WARNING).log("Auto-save failed for %s: %s", u, t.getMessage());
                    dirty.add(u);
                    return null;
                });
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        // Sauvegarde synchrone de tous les comptes en cache pour ne rien perdre au restart
        for (java.util.Map.Entry<java.util.UUID, ClassAccount> entry : cache.entrySet()) {
            try {
                storage.savePlayer(entry.getKey(), entry.getValue()).join();
            } catch (Exception e) {
                LOGGER.at(java.util.logging.Level.WARNING).log("shutdown save failed for %s: %s", entry.getKey(), e.getMessage());
            }
        }
        storage.shutdown();
    }
}
