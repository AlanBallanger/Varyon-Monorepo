package fr.varyon.vrpg.classes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public final class ClassManager {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("VaryonRPG-Classes");
    private static final long AUTOSAVE_SECONDS = 30L;
    private static final long XP_NOTIF_DEBOUNCE_MS = 1500L;

    private final SqliteClassStorage storage;
    private final ConcurrentHashMap<UUID, ClassAccount> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Map<PlayerClass, Double>> xpFractionBank = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public ClassManager(@Nonnull Path dataDirectory) {
        this.storage = new SqliteClassStorage(dataDirectory);
        this.storage.initialize().join();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VaryonRPG-Classes-AutoSave");
            t.setDaemon(false);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::flushDirty, AUTOSAVE_SECONDS, AUTOSAVE_SECONDS, TimeUnit.SECONDS);
        LOGGER.at(Level.INFO).log("ClassManager ready (auto-save %ds)", AUTOSAVE_SECONDS);
    }

    private ReentrantLock lockFor(UUID uuid) {
        return locks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }

    public void ensureAccount(@Nonnull UUID uuid, @Nullable String playerName) {
        cache.computeIfAbsent(uuid, k -> storage.loadPlayer(k).join());
        if (playerName != null) {
            ReentrantLock lock = lockFor(uuid);
            lock.lock();
            try {
                ClassAccount acc = cache.get(uuid);
                if (acc != null && !playerName.equals(acc.getPlayerName())) {
                    acc.setPlayerName(playerName);
                    dirty.add(uuid);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Nullable
    public ClassAccount getAccount(@Nonnull UUID uuid) {
        return cache.get(uuid);
    }

    @Nonnull
    public ClassAccount getOrLoad(@Nonnull UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> storage.loadPlayer(k).join());
    }

    public void markDirty(@Nonnull UUID uuid) {
        dirty.add(uuid);
    }

    public int addXp(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass, double amount) {
        if (amount <= 0.0) return 0;
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            Map<PlayerClass, Double> bank = xpFractionBank.computeIfAbsent(uuid, k -> new HashMap<>());
            double banked = bank.getOrDefault(playerClass, 0.0) + amount;
            long wholeXp = (long) banked;
            bank.put(playerClass, banked - wholeXp);
            ClassAccount acc = getOrLoad(uuid);
            int levelsGained = wholeXp > 0 ? acc.getProgress(playerClass).addXp(wholeXp) : 0;
            dirty.add(uuid);
            return levelsGained;
        } finally {
            lock.unlock();
        }
    }

    public int addXp(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass, double amount,
                     @Nonnull PlayerRef playerRef) {
        int levelsGained = addXp(uuid, playerClass, amount);
        if (amount > 0) scheduleXpNotif(uuid, playerRef, playerClass, amount);
        return levelsGained;
    }

    private static final class NotifState {
        double total;
        PlayerRef playerRef;
        ScheduledFuture<?> pending;
    }

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<PlayerClass, NotifState>> xpNotifMap = new ConcurrentHashMap<>();

    private void scheduleXpNotif(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                  @Nonnull PlayerClass playerClass, double amount) {
        ConcurrentHashMap<PlayerClass, NotifState> byClass = xpNotifMap.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        NotifState state = byClass.computeIfAbsent(playerClass, k -> new NotifState());
        synchronized (state) {
            state.total += amount;
            state.playerRef = playerRef;
            if (state.pending != null) state.pending.cancel(false);
            state.pending = scheduler.schedule(() -> flushXpNotif(uuid, playerClass), XP_NOTIF_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void flushXpNotif(@Nonnull UUID uuid, @Nonnull PlayerClass playerClass) {
        ConcurrentHashMap<PlayerClass, NotifState> byClass = xpNotifMap.get(uuid);
        if (byClass == null) return;
        NotifState state = byClass.get(playerClass);
        if (state == null) return;
        double toSend;
        PlayerRef playerRef;
        synchronized (state) {
            toSend = state.total;
            state.total = 0.0;
            state.pending = null;
            playerRef = state.playerRef;
        }
        if (toSend <= 0 || playerRef == null) return;
        String xpStr = (toSend == Math.floor(toSend))
            ? String.valueOf((long) toSend)
            : String.valueOf(Math.round(toSend * 10.0) / 10.0);
        try {
            Message msg = Message.raw("+" + xpStr + " XP").color(new Color(0xFFD700));
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), msg, null, playerClass.getIconPath());
        } catch (Exception ignored) {}
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
            if (acc.availableTalentPoints(playerClass) <= 0) return false;
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

    public void onPlayerDisconnect(@Nonnull UUID uuid) {
        ClassAccount acc = cache.get(uuid);
        if (acc != null) {
            storage.savePlayer(uuid, acc);
            dirty.remove(uuid);
        }
        locks.remove(uuid);
        cache.remove(uuid);
        xpFractionBank.remove(uuid);
        ConcurrentHashMap<PlayerClass, NotifState> notifByClass = xpNotifMap.remove(uuid);
        if (notifByClass != null) {
            for (NotifState s : notifByClass.values()) {
                synchronized (s) { if (s.pending != null) s.pending.cancel(false); }
            }
        }
    }

    private void flushDirty() {
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
        flushDirty();
        scheduler.shutdown();
        storage.shutdown();
    }
}
