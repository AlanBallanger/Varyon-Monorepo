package com.varyon.crashrestarter;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ShutdownReason;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Polls all worlds and failed joins; schedules a clean server shutdown when unhealthy.
 */
public final class CrashRestarterMonitor {

    private static final long POLL_SECONDS = 7L;
    private static final long FAILED_JOIN_WINDOW_MS = TimeUnit.SECONDS.toMillis(90);
    private static final long PENDING_JOIN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);
    private static final int FAILED_JOIN_COUNT = 3;
    private static final int FAILED_JOIN_DISTINCT_PLAYERS = 2;
    private static final long RESTART_DELAY_SECONDS = 15L;
    private static final long ANTI_LOOP_WINDOW_MS = TimeUnit.MINUTES.toMillis(15);
    private static final int ANTI_LOOP_MAX_RESTARTS = 2;
    private static final String RESTART_HISTORY_FILE = "automatic-restarts.log";

    private final Path dataDirectory;
    private final HytaleLogger logger;
    private final AtomicBoolean restartAlreadyScheduled = new AtomicBoolean(false);
    private final AtomicBoolean autoRestartDisabled = new AtomicBoolean(false);
    private final Deque<FailedJoin> failedJoins = new ArrayDeque<>();
    private final Object joinLock = new Object();
    /** Players who started connecting but have not reached AddPlayer/Ready yet. */
    private final Map<UUID, Long> pendingJoins = new ConcurrentHashMap<>();

    @Nullable
    private ScheduledFuture<?> pollTask;
    @Nullable
    private ScheduledFuture<?> restartTask;

    public CrashRestarterMonitor(@Nonnull Path dataDirectory, @Nonnull HytaleLogger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void start() {
        pollTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::tickSafe, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
        if (restartTask != null) {
            restartTask.cancel(false);
            restartTask = null;
        }
        pendingJoins.clear();
    }

    /**
     * Called at end of {@code PlayerConnectEvent}. Counts immediate failures
     * (no world / unhealthy target world), otherwise tracks a pending join.
     */
    public void onConnectAttempt(@Nonnull UUID playerUuid, @Nullable World resolvedWorld) {
        if (autoRestartDisabled.get() || restartAlreadyScheduled.get()) {
            return;
        }

        if (resolvedWorld == null) {
            recordFailedJoin(playerUuid, "no world available / no default world configured");
            return;
        }
        if (resolvedWorld.getFailureException() != null) {
            recordFailedJoin(playerUuid, "target world crashed: " + resolvedWorld.getName());
            return;
        }
        if (!resolvedWorld.isAlive()) {
            recordFailedJoin(playerUuid, "target world not alive: " + resolvedWorld.getName());
            return;
        }

        pendingJoins.put(playerUuid, System.currentTimeMillis());
    }

    public void onJoinSucceeded() {
        pendingJoins.clear();
        synchronized (joinLock) {
            failedJoins.clear();
        }
    }

    public void onJoinSucceeded(@Nonnull UUID playerUuid) {
        pendingJoins.remove(playerUuid);
        synchronized (joinLock) {
            failedJoins.clear();
        }
    }

    public void onDisconnectBeforeReady(@Nonnull UUID playerUuid) {
        Long started = pendingJoins.remove(playerUuid);
        if (started == null) {
            return;
        }
        recordFailedJoin(playerUuid, "disconnected before join completed");
    }

    private void recordFailedJoin(@Nonnull UUID playerUuid, @Nonnull String reason) {
        if (autoRestartDisabled.get() || restartAlreadyScheduled.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        int count;
        int distinct;
        synchronized (joinLock) {
            failedJoins.addLast(new FailedJoin(playerUuid, now, reason));
            pruneFailedJoins(now);
            count = failedJoins.size();
            distinct = distinctPlayers();
        }

        logger.at(Level.WARNING).log(
                "[CrashRestarter] Failed join: reason=%s player=%s window=%d/%d distinct=%d/%d",
                reason, playerUuid, count, FAILED_JOIN_COUNT, distinct, FAILED_JOIN_DISTINCT_PLAYERS);

        if (count >= FAILED_JOIN_COUNT && distinct >= FAILED_JOIN_DISTINCT_PLAYERS) {
            logger.at(Level.SEVERE).log(
                    "[CrashRestarter] %d failed joins detected:%nReason = %s",
                    count, summarizeFailedJoinReasons());
            scheduleRestart("failed join threshold (" + count + " fails / " + distinct + " players)");
        }
    }

    private void tickSafe() {
        try {
            tick();
        } catch (Throwable t) {
            logger.at(Level.SEVERE).withCause(t).log("[CrashRestarter] Poll tick failed");
        }
    }

    private void tick() {
        if (autoRestartDisabled.get() || restartAlreadyScheduled.get()) {
            return;
        }
        if (HytaleServer.get().isShuttingDown()) {
            return;
        }

        expirePendingJoins();

        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        World defaultWorld = universe.getDefaultWorld();
        if (defaultWorld == null) {
            logger.at(Level.SEVERE).log("[CrashRestarter] Default world is null.");
            scheduleRestart("default world null");
            return;
        }

        Map<String, World> worlds = universe.getWorlds();
        if (worlds == null || worlds.isEmpty()) {
            logger.at(Level.SEVERE).log("[CrashRestarter] No worlds loaded.");
            scheduleRestart("no worlds loaded");
            return;
        }

        for (Map.Entry<String, World> entry : worlds.entrySet()) {
            String name = entry.getKey() != null ? entry.getKey() : "?";
            String issue = worldIssue(entry.getValue(), name);
            if (issue != null) {
                logger.at(Level.SEVERE).log("[CrashRestarter] %s", issue);
                scheduleRestart(issue);
                return;
            }
        }
    }

    private void expirePendingJoins() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : pendingJoins.entrySet()) {
            Long started = entry.getValue();
            if (started == null || now - started < PENDING_JOIN_TIMEOUT_MS) {
                continue;
            }
            UUID uuid = entry.getKey();
            if (pendingJoins.remove(uuid, started)) {
                recordFailedJoin(uuid, "join timed out after " + (PENDING_JOIN_TIMEOUT_MS / 1000) + "s");
            }
        }
    }

    /**
     * @return human-readable issue, or null if healthy.
     * After a WorldThread NPE, {@link World#isAlive()} can stay true — also check {@link World#getFailureException()}.
     */
    @Nullable
    private static String worldIssue(@Nullable World world, @Nonnull String label) {
        if (world == null) {
            return label + " world missing";
        }
        if (world.getFailureException() != null) {
            return label + " world thread crashed (" + world.getFailureException().getClass().getSimpleName() + ")";
        }
        if (!world.isAlive()) {
            return label + " world is not alive";
        }
        return null;
    }

    private void scheduleRestart(@Nonnull String reason) {
        if (autoRestartDisabled.get()) {
            return;
        }
        if (!restartAlreadyScheduled.compareAndSet(false, true)) {
            return;
        }

        if (countRecentAutomaticRestarts() >= ANTI_LOOP_MAX_RESTARTS) {
            autoRestartDisabled.set(true);
            restartAlreadyScheduled.set(false);
            logger.at(Level.SEVERE).log(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!%n"
                            + "[CrashRestarter] ANTI-LOOP: %d automatic restarts within 15 minutes.%n"
                            + "[CrashRestarter] Further automatic restarts DISABLED.%n"
                            + "[CrashRestarter] Server left running for manual intervention.%n"
                            + "[CrashRestarter] Last reason would have been: %s%n"
                            + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!",
                    ANTI_LOOP_MAX_RESTARTS, reason);
            return;
        }

        logger.at(Level.SEVERE).log("[CrashRestarter] %s. Scheduling automatic restart.", reason);
        logger.at(Level.SEVERE).log("[CrashRestarter] Restart scheduled in %d seconds.", RESTART_DELAY_SECONDS);
        broadcast(
                "Un problème critique est détecté.\nLe serveur redémarre dans 15 secondes.");

        restartTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            try {
                recordAutomaticRestart();
                logger.at(Level.SEVERE).log(
                        "[CrashRestarter] Performing clean shutdown. Reason: %s", reason);
                HytaleServer.get().shutdownServer(
                        ShutdownReason.SHUTDOWN.withMessage(
                                Message.raw("[CrashRestarter] " + reason)));
            } catch (Throwable t) {
                logger.at(Level.SEVERE).withCause(t).log("[CrashRestarter] Shutdown failed");
                restartAlreadyScheduled.set(false);
            }
        }, RESTART_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void pruneFailedJoins(long now) {
        while (!failedJoins.isEmpty() && now - failedJoins.peekFirst().atMs > FAILED_JOIN_WINDOW_MS) {
            failedJoins.removeFirst();
        }
    }

    private int distinctPlayers() {
        Set<UUID> ids = new HashSet<>();
        for (FailedJoin join : failedJoins) {
            ids.add(join.playerUuid);
        }
        return ids.size();
    }

    @Nonnull
    private String summarizeFailedJoinReasons() {
        synchronized (joinLock) {
            if (failedJoins.isEmpty()) {
                return "unknown";
            }
            Set<String> reasons = new HashSet<>();
            for (FailedJoin join : failedJoins) {
                reasons.add(join.reason);
            }
            return String.join(" | ", reasons);
        }
    }

    private void broadcast(@Nonnull String text) {
        try {
            Message message = Message.raw(text);
            for (PlayerRef player : Universe.get().getPlayers()) {
                try {
                    player.sendMessage(message);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            logger.at(Level.WARNING).withCause(t).log("[CrashRestarter] Broadcast failed");
        }
    }

    private Path historyPath() {
        return dataDirectory.resolve(RESTART_HISTORY_FILE);
    }

    private int countRecentAutomaticRestarts() {
        Path path = historyPath();
        if (!Files.isRegularFile(path)) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - ANTI_LOOP_WINDOW_MS;
        int count = 0;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                try {
                    long ts = Long.parseLong(trimmed);
                    if (ts >= cutoff) {
                        count++;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            logger.at(Level.WARNING).withCause(e).log("[CrashRestarter] Could not read restart history");
        }
        return count;
    }

    private void recordAutomaticRestart() {
        try {
            Files.createDirectories(dataDirectory);
            Path path = historyPath();
            long now = System.currentTimeMillis();
            long cutoff = now - ANTI_LOOP_WINDOW_MS;
            Deque<String> keep = new ArrayDeque<>();
            if (Files.isRegularFile(path)) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    try {
                        if (Long.parseLong(trimmed) >= cutoff) {
                            keep.addLast(trimmed);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            keep.addLast(Long.toString(now));
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = keep.iterator();
            while (it.hasNext()) {
                sb.append(it.next()).append('\n');
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.at(Level.WARNING).withCause(e).log("[CrashRestarter] Could not persist restart history");
        }
    }

    private record FailedJoin(@Nonnull UUID playerUuid, long atMs, @Nonnull String reason) {
    }
}
