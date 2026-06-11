package fr.varyon.vrpg.rpg;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
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

/**
 * Noyau commun à ProfessionManager et ClassManager :
 * cache par UUID, verrouillage, dirty tracking, autosave, fractional XP banking,
 * et notifications XP debouncées.
 *
 * @param <A>  type du compte joueur (PlayerAccount / ClassAccount)
 * @param <K>  clé de progression (Profession / PlayerClass)
 */
public abstract class AbstractPlayerManager<A, K> {

    protected static final long AUTOSAVE_SECONDS = 30L;

    protected final HytaleLogger logger;
    protected final ConcurrentHashMap<UUID, A> cache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();
    protected final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    protected final ConcurrentHashMap<UUID, Map<K, Double>> xpFractionBank = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService scheduler;

    protected AbstractPlayerManager(@Nonnull HytaleLogger logger, @Nonnull String threadName) {
        this.logger = logger;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(false);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::flushDirty, AUTOSAVE_SECONDS, AUTOSAVE_SECONDS, TimeUnit.SECONDS);
    }

    protected ReentrantLock lockFor(@Nonnull UUID uuid) {
        return locks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }

    protected abstract A loadAccount(@Nonnull UUID uuid);

    @Nullable
    public A getAccount(@Nonnull UUID uuid) {
        return cache.get(uuid);
    }

    @Nonnull
    public A getOrLoad(@Nonnull UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> loadAccount(k));
    }

    public void ensureAccount(@Nonnull UUID uuid, @Nullable String playerName) {
        cache.computeIfAbsent(uuid, k -> loadAccount(k));
        if (playerName != null) {
            ReentrantLock lock = lockFor(uuid);
            lock.lock();
            try {
                A acc = cache.get(uuid);
                if (acc != null && !playerName.equals(getPlayerName(acc))) {
                    setPlayerName(acc, playerName);
                    dirty.add(uuid);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    protected abstract String getPlayerName(@Nonnull A account);

    protected abstract void setPlayerName(@Nonnull A account, @Nonnull String name);

    public void markDirty(@Nonnull UUID uuid) {
        dirty.add(uuid);
    }

    protected int addXpInternal(@Nonnull UUID uuid, @Nonnull K key, double amount) {
        if (amount <= 0.0) return 0;
        ReentrantLock lock = lockFor(uuid);
        lock.lock();
        try {
            Map<K, Double> bank = xpFractionBank.computeIfAbsent(uuid, k -> new HashMap<>());
            double banked = bank.getOrDefault(key, 0.0) + amount;
            long wholeXp = (long) banked;
            bank.put(key, banked - wholeXp);
            A acc = getOrLoad(uuid);
            int levelsGained = wholeXp > 0 ? applyWholeXp(acc, key, wholeXp) : 0;
            dirty.add(uuid);
            return levelsGained;
        } finally {
            lock.unlock();
        }
    }

    protected abstract int applyWholeXp(@Nonnull A account, @Nonnull K key, long wholeXp);

    protected abstract void flushDirty();

    protected abstract void saveAccountOnDisconnect(@Nonnull UUID uuid, @Nonnull A account);

    public void onPlayerDisconnect(@Nonnull UUID uuid) {
        A acc = cache.get(uuid);
        if (acc != null) {
            saveAccountOnDisconnect(uuid, acc);
            dirty.remove(uuid);
        }
        cancelPendingNotifs(uuid);
        locks.remove(uuid);
        cache.remove(uuid);
        xpFractionBank.remove(uuid);
    }

    // ---- Notifications XP debouncées ----

    protected static final class NotifState {
        double total;
        PlayerRef playerRef;
        ScheduledFuture<?> pending;
    }

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<K, NotifState>> xpNotifMap = new ConcurrentHashMap<>();

    protected void scheduleXpNotif(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                    @Nonnull K key, double amount, long debounceMs) {
        ConcurrentHashMap<K, NotifState> byKey = xpNotifMap.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        NotifState state = byKey.computeIfAbsent(key, k -> new NotifState());
        synchronized (state) {
            state.total += amount;
            state.playerRef = playerRef;
            if (state.pending != null) state.pending.cancel(false);
            state.pending = scheduler.schedule(() -> flushXpNotif(uuid, key), debounceMs, TimeUnit.MILLISECONDS);
        }
    }

    private void flushXpNotif(@Nonnull UUID uuid, @Nonnull K key) {
        ConcurrentHashMap<K, NotifState> byKey = xpNotifMap.get(uuid);
        if (byKey == null) return;
        NotifState state = byKey.get(key);
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
        sendXpNotification(playerRef, key, xpStr);
    }

    protected abstract void sendXpNotification(@Nonnull PlayerRef playerRef, @Nonnull K key, @Nonnull String xpStr);

    protected void cancelPendingNotifs(@Nonnull UUID uuid) {
        ConcurrentHashMap<K, NotifState> notifByKey = xpNotifMap.remove(uuid);
        if (notifByKey != null) {
            for (NotifState s : notifByKey.values()) {
                synchronized (s) { if (s.pending != null) s.pending.cancel(false); }
            }
        }
    }

    protected static String formatXpStr(double amount) {
        return (amount == Math.floor(amount))
            ? String.valueOf((long) amount)
            : String.valueOf(Math.round(amount * 10.0) / 10.0);
    }
}
