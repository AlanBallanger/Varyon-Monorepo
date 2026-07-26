package com.varyon.bossarena.spawn;

import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.data.BossDefinition;
import com.varyon.bossarena.data.BossRegistry;
import com.varyon.bossarena.system.BossTrackingSystem;
import com.varyon.bossarena.system.BossWaveNotificationService;
import com.varyon.bossarena.util.EntityComponents;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector3d;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class BossTimedSpawnScheduler {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PERSISTENCE_VERSION = 1;
    private static final long SCHEDULER_TICK_SECONDS = 5L;
    private static final long WORLD_LOOKUP_RETRY_MINUTES = 1L;
    private static final long PENDING_SPAWN_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10L);
    /** If no matching boss is alive and pending is older than this, treat as stale and clear (boss died/crate gone). */
    private static final long STALE_PENDING_WHEN_NO_BOSS_MS = TimeUnit.MINUTES.toMillis(2L);
    // When no players are online in the target world, defer timed spawns and retry soon.
    private static final long NO_PLAYER_RETRY_SECONDS = 30L;
    /** When waiting for a player within proximity radius, re-check this often so walking into range triggers quickly. */
    private static final long PROXIMITY_RETRY_SECONDS = 10L;
    /** Sentinel next-spawn epoch while waiting for boss death (AFTER_DEATH mode). */
    private static final long WAIT_FOR_DEATH_EPOCH_MS = Long.MAX_VALUE / 4L;
    /** Chat reminder lead time before scheduled spawn. */
    private static final long REMINDER_LEAD_MS = TimeUnit.MINUTES.toMillis(5L);
    private static final long WAITING_PLAYERS_TITLE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30L);
    private static final long GRACE_TITLE_REFRESH_MS = TimeUnit.SECONDS.toMillis(5L);
    private final Random bossPoolRandom = new Random();
    private final BossSpawnService bossSpawnService;
    private final BossTrackingSystem trackingSystem;
    private final Map<String, PendingSpawnState> pendingSpawnByKey = new ConcurrentHashMap<>();
    private final Set<UUID> spawnedTimedBossUuids = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BossArena-TimedSpawns");
                t.setDaemon(true);
                return t;
            });
    private final Object persistenceLock = new Object();
    private TimedBossMapMarkerService mapMarkerService;
    private volatile Consumer<BossArenaConfig.TimedBossSpawn> oneShotDisableHandler;
    private volatile Supplier<BossArenaConfig> configSupplier;
    private volatile Path persistencePath;
    private volatile Map<String, Long> persistedNextSpawnByLabel = Map.of();
    private volatile List<TimedSpawnState> states = List.of();
    private volatile boolean started;

    public BossTimedSpawnScheduler(BossSpawnService bossSpawnService, BossTrackingSystem trackingSystem) {
        this.bossSpawnService = bossSpawnService;
        this.trackingSystem = trackingSystem;
    }

    private static void removeEntity(World world, UUID entityUuid) {
        if (world == null || entityUuid == null) {
            return;
        }
        if (!world.isInThread()) {
            world.execute(() -> removeEntity(world, entityUuid));
            return;
        }
        try {
            var ref = world.getEntityRef(entityUuid);
            if (ref == null || !ref.isValid()) {
                return;
            }
            world.getEntityStore().getStore().removeEntity(ref, RemoveReason.REMOVE);
        } catch (Exception ignored) {
            // Best-effort cleanup.
        }
    }

    private static boolean matchesRule(BossTrackingSystem.BossData data, BossArenaConfig.TimedBossSpawn rule) {
        if (data == null || rule == null) {
            return false;
        }

        String expectedArenaId = optional(rule.arenaId);
        if (!expectedArenaId.isEmpty()) {
            String trackedArenaId = optional(data.arenaId);
            if (!trackedArenaId.equalsIgnoreCase(expectedArenaId)) {
                return false;
            }
        }

        String trackedBoss = optional(data.bossName);
        if (trackedBoss.isEmpty()) {
            return false;
        }
        for (BossArenaConfig.BossPoolEntry entry : rule.resolveBossPool()) {
            String expectedBossName = resolveExpectedBossName(entry.bossId);
            if (!expectedBossName.isEmpty() && trackedBoss.equalsIgnoreCase(expectedBossName)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveExpectedBossName(String configuredBossId) {
        String normalized = optional(configuredBossId);
        if (normalized.isEmpty()) {
            return "";
        }
        BossDefinition def = BossRegistry.get(normalized);
        if (def != null && def.bossName != null && !def.bossName.isBlank()) {
            return def.bossName.trim();
        }
        return normalized;
    }

    private static String resolveSpawnKey(BossArenaConfig.TimedBossSpawn rule) {
        if (rule == null) {
            return "";
        }
        String arena = optional(rule.arenaId).toLowerCase(Locale.ROOT);
        List<BossArenaConfig.BossPoolEntry> pool = rule.resolveBossPool();
        if (arena.isEmpty() || pool.isEmpty()) {
            return "";
        }
        List<String> bosses = new ArrayList<>();
        for (BossArenaConfig.BossPoolEntry entry : pool) {
            String id = optional(entry.bossId).toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                bosses.add(id);
            }
        }
        if (bosses.isEmpty()) {
            return "";
        }
        bosses.sort(String::compareTo);
        return String.join(",", bosses) + "@" + arena;
    }

    private static World resolveWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        Universe universe = Universe.get();
        if (universe == null) {
            return null;
        }
        World w = universe.getWorld(worldName);
        if (w != null) {
            return w;
        }
        for (World candidate : universe.getWorlds().values()) {
            if (candidate != null && worldName.equalsIgnoreCase(candidate.getName())) {
                return candidate;
            }
        }
        return null;
    }

    private static long resolveArrivalWindowMillis(BossArenaConfig.TimedBossSpawn rule) {
        if (rule == null) {
            return TimeUnit.MINUTES.toMillis(15L);
        }
        long seconds = BossArenaConfig.resolveSeconds(
                rule.arrivalWindowHours,
                rule.arrivalWindowMinutes,
                rule.arrivalWindowSeconds
        );
        return Math.max(0L, seconds * 1000L);
    }

    private static long resolveSpawnIntervalMillis(BossArenaConfig.TimedBossSpawn rule) {
        if (rule == null) {
            return TimeUnit.HOURS.toMillis(1L);
        }
        if (rule.isIntervalMode()) {
            long seconds = BossArenaConfig.resolveIntervalSeconds(
                    rule.intervalHours,
                    rule.intervalDays,
                    rule.intervalSeconds
            );
            if (seconds <= 0L) {
                seconds = BossArenaConfig.resolveIntervalSeconds(rule.intervalEvery, rule.intervalUnit);
            }
            return Math.max(1000L, seconds * 1000L);
        }
        long seconds = BossArenaConfig.resolveSeconds(
                rule.spawnIntervalHours,
                rule.spawnIntervalMinutes,
                rule.spawnIntervalSeconds
        );
        return Math.max(1000L, seconds * 1000L);
    }

    private static long resolveDespawnMinutes(BossArenaConfig.TimedBossSpawn rule) {
        // Only Planifié (interval) uses forced despawn. Temps réapparition has no lifetime limit from this field.
        if (rule == null || rule.isAfterDeathMode()) {
            return 0L;
        }
        // Explicit contract: 0h 0m means infinite lifetime (no forced despawn).
        if (rule.despawnAfterHours <= 0L && rule.despawnAfterMinutes <= 0L) {
            return 0L;
        }
        return Math.max(0L, BossArenaConfig.resolveMinutes(rule.despawnAfterHours, rule.despawnAfterMinutes));
    }

    private static long minutesToMillis(long minutes) {
        return TimeUnit.MINUTES.toMillis(Math.max(0L, minutes));
    }

    private static String resolveRuleLabel(BossArenaConfig.TimedBossSpawn rule, int index) {
        String id = optional(rule.id);
        if (!id.isEmpty()) {
            return id;
        }
        List<BossArenaConfig.BossPoolEntry> pool = rule.resolveBossPool();
        String arena = optional(rule.arenaId);
        if (!pool.isEmpty() || !arena.isEmpty()) {
            String boss = pool.isEmpty() ? optional(rule.bossId) : optional(pool.get(0).bossId);
            if (pool.size() > 1) {
                boss = boss + "+" + (pool.size() - 1);
            }
            return boss + "@" + arena;
        }
        return "rule_" + index;
    }

    private static BossArenaConfig.TimedBossSpawn copyRule(BossArenaConfig.TimedBossSpawn source) {
        BossArenaConfig.TimedBossSpawn out = new BossArenaConfig.TimedBossSpawn();
        out.id = source.id;
        out.enabled = source.enabled;
        out.bossId = source.bossId;
        out.bossPool = new ArrayList<>();
        for (BossArenaConfig.BossPoolEntry entry : source.resolveBossPool()) {
            BossArenaConfig.BossPoolEntry copy = new BossArenaConfig.BossPoolEntry();
            copy.bossId = entry.bossId;
            copy.weight = Math.max(1, entry.weight);
            out.bossPool.add(copy);
        }
        out.arenaId = source.arenaId;
        out.scheduleMode = source.scheduleMode;
        out.spawnIntervalHours = source.spawnIntervalHours;
        out.spawnIntervalMinutes = source.spawnIntervalMinutes;
        out.spawnIntervalSeconds = source.spawnIntervalSeconds;
        out.intervalHours = source.intervalHours;
        out.intervalDays = source.intervalDays;
        out.intervalSeconds = source.intervalSeconds;
        out.intervalEvery = source.intervalEvery;
        out.intervalUnit = source.intervalUnit;
        out.arrivalWindowHours = source.arrivalWindowHours;
        out.arrivalWindowMinutes = source.arrivalWindowMinutes;
        out.arrivalWindowSeconds = source.arrivalWindowSeconds;
        out.fixedTimes = source.fixedTimes == null ? new ArrayList<>() : new ArrayList<>(source.fixedTimes);
        out.oneShot = false;
        out.requirePlayerInRadius = false;
        out.minPlayers = Math.max(0, source.minPlayers);
        out.preventDuplicateWhileAlive = source.preventDuplicateWhileAlive;
        out.despawnAfterHours = source.despawnAfterHours;
        out.despawnAfterMinutes = source.despawnAfterMinutes;
        out.announceWorldWide = source.announceWorldWide;
        out.announceCurrentWorld = source.announceCurrentWorld;
        out.worldAnnouncementText = source.worldAnnouncementText;
        out.reminderAnnouncementText = source.reminderAnnouncementText;
        out.gracePeriodEnabled = source.gracePeriodEnabled;
        out.gracePeriodSeconds = Math.max(0L, source.gracePeriodSeconds);
        out.graceTitleText = source.graceTitleText;
        return out;
    }

    private static long sanitizeNextSpawnEpoch(Long storedEpochMs, long now, long intervalMs) {
        if (storedEpochMs == null || storedEpochMs <= 0L) {
            return now;
        }
        if (storedEpochMs >= WAIT_FOR_DEATH_EPOCH_MS) {
            return WAIT_FOR_DEATH_EPOCH_MS;
        }
        return storedEpochMs;
    }

    private long resolveInitialNextSpawn(BossArenaConfig.TimedBossSpawn rule, String label, long now) {
        Map<String, Long> persistedMap = persistedNextSpawnByLabel;
        Long stored = persistedMap.get(label);
        if (rule.isIntervalMode()) {
            if (stored != null && stored > now && stored < WAIT_FOR_DEATH_EPOCH_MS) {
                return stored;
            }
            // Bootstrap ASAP on enable/reload; then every N after each spawn.
            return now;
        }
        // AFTER_DEATH: bootstrap ASAP unless a future death-delay is already persisted.
        if (hasAliveBossForRule(rule)) {
            return WAIT_FOR_DEATH_EPOCH_MS;
        }
        if (stored != null && stored > now && stored < WAIT_FOR_DEATH_EPOCH_MS) {
            return stored;
        }
        return now;
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    public void setMapMarkerService(TimedBossMapMarkerService mapMarkerService) {
        this.mapMarkerService = mapMarkerService;
    }

    public void setOneShotDisableHandler(Consumer<BossArenaConfig.TimedBossSpawn> oneShotDisableHandler) {
        this.oneShotDisableHandler = oneShotDisableHandler;
    }

    public void setConfigSupplier(Supplier<BossArenaConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public synchronized void initializePersistence(Path stateFilePath) {
        this.persistencePath = stateFilePath;
        if (stateFilePath == null) {
            this.persistedNextSpawnByLabel = Map.of();
            return;
        }

        try {
            Path parent = stateFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to create timed scheduler persistence directory: " + e.getMessage());
        }

        this.persistedNextSpawnByLabel = loadPersistedNextSpawnMap();
    }

    public void flushPersistence() {
        persistState();
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        executor.scheduleAtFixedRate(this::tickSafely, SCHEDULER_TICK_SECONDS, SCHEDULER_TICK_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void shutdown() {
        flushPersistence();
        started = false;
        executor.shutdownNow();
        pendingSpawnByKey.clear();
        spawnedTimedBossUuids.clear();
        states = List.of();
    }

    public void reloadFromConfig(BossArenaConfig config) {
        long now = System.currentTimeMillis();
        Map<String, Long> persistedMap = persistedNextSpawnByLabel;
        List<TimedSpawnState> rebuilt = new ArrayList<>();
        Set<String> validPendingKeys = new HashSet<>();

        if (config != null) {
            List<BossArenaConfig.TimedBossSpawn> configured = config.getTimedBossSpawns();
            int index = 0;
            for (BossArenaConfig.TimedBossSpawn rule : configured) {
                index++;
                if (rule == null || !rule.enabled || rule.isManualMode()) {
                    continue;
                }
                BossArenaConfig.TimedBossSpawn snapshot = copyRule(rule);

                String label = resolveRuleLabel(snapshot, index);
                long firstAt = resolveInitialNextSpawn(snapshot, label, now);
                TimedSpawnState state = new TimedSpawnState(snapshot, firstAt, label);
                if (snapshot.isAfterDeathMode() && hasAliveBossForRule(snapshot)) {
                    state.sawAliveBoss = true;
                }
                rebuilt.add(state);
                String spawnKey = resolveSpawnKey(snapshot);
                if (!spawnKey.isEmpty()) {
                    validPendingKeys.add(spawnKey);
                }
            }
        }

        states = rebuilt;
        pendingSpawnByKey.keySet().retainAll(validPendingKeys);
        rebuildTrackedTimedBossCache(rebuilt);
        persistState();
        LOGGER.info("Timed boss scheduler reloaded: " + rebuilt.size() + " active rule(s).");
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            LOGGER.warning("Timed boss scheduler tick failed: " + e.getMessage());
        }
    }

    private void tick() {
        trackingSystem.retryPendingRestore();

        List<TimedSpawnState> current = states;
        boolean changed = false;
        long now = System.currentTimeMillis();
        pruneExpiredPendingSpawns(now);
        if (!current.isEmpty()) {
            for (TimedSpawnState state : current) {
                if (state == null || state.rule == null) {
                    continue;
                }
                enforceTimedDespawn(state, now);
                if (updateAfterDeathSchedule(state, now)) {
                    changed = true;
                }
                maybeSendTimedReminder(state, now);
                if (now < state.nextSpawnEpochMs) {
                    continue;
                }
                if (evaluateSpawn(state, now)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            persistState();
        }
    }

    /**
     * AFTER_DEATH: while boss is alive, park next spawn; when death is detected, schedule delay from now.
     * @return true if nextSpawnEpochMs changed
     */
    private boolean updateAfterDeathSchedule(TimedSpawnState state, long now) {
        BossArenaConfig.TimedBossSpawn rule = state.rule;
        if (rule == null || !rule.isAfterDeathMode()) {
            return false;
        }
        boolean alive = hasAliveBossForRule(rule)
                || hasAwaitingPrimaryBossForRule(rule)
                || isSpawnPendingForRule(rule, now);
        if (alive) {
            boolean changed = !state.sawAliveBoss || state.nextSpawnEpochMs < WAIT_FOR_DEATH_EPOCH_MS;
            state.sawAliveBoss = true;
            state.nextSpawnEpochMs = WAIT_FOR_DEATH_EPOCH_MS;
            return changed;
        }
        if (state.sawAliveBoss) {
            long delayMs = resolveSpawnIntervalMillis(rule);
            state.sawAliveBoss = false;
            state.nextSpawnEpochMs = now + delayMs;
            state.reminderSent = false;
            LOGGER.info("Timed spawn '" + state.label + "': boss dead, next spawn in "
                    + Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(delayMs)) + " second(s).");
            return true;
        }
        return false;
    }

    /**
     * Send a one-shot chat reminder in the 5-minute window before {@code nextSpawnEpochMs}.
     * Skips Manual, empty text, no announce scope, forced paths, and parked AFTER_DEATH waits.
     */
    private void maybeSendTimedReminder(TimedSpawnState state, long now) {
        if (state == null || state.reminderSent) {
            return;
        }
        BossArenaConfig.TimedBossSpawn rule = state.rule;
        if (rule == null || !rule.enabled || rule.isManualMode()) {
            return;
        }
        if (!rule.isIntervalMode() && !rule.isAfterDeathMode()) {
            return;
        }
        String reminderText = optional(rule.reminderAnnouncementText);
        if (reminderText.isEmpty()) {
            return;
        }
        if (!rule.announceWorldWide && !rule.announceCurrentWorld) {
            return;
        }
        long due = state.nextSpawnEpochMs;
        if (due <= 0L || due >= WAIT_FOR_DEATH_EPOCH_MS) {
            return;
        }
        if (now < due - REMINDER_LEAD_MS || now >= due) {
            return;
        }

        String configuredBossId = ensureRolledBossId(state, rule);
        String configuredArenaId = optional(rule.arenaId);
        if (configuredBossId.isEmpty() || configuredArenaId.isEmpty()) {
            return;
        }
        Arena arena = ArenaRegistry.get(configuredArenaId);
        if (arena == null) {
            return;
        }
        World world = resolveWorld(arena.worldName);
        if (world == null) {
            return;
        }

        state.reminderSent = true;
        BossWaveNotificationService.notifyTimedSpawn(
                resolveExpectedBossName(configuredBossId),
                configuredArenaId,
                world,
                reminderText,
                rule.announceWorldWide,
                rule.announceCurrentWorld
        );
        LOGGER.info("Timed spawn reminder sent for '" + state.label + "' (due in "
                + Math.max(0L, (due - now) / 1000L) + "s).");
    }

    /**
     * Force-spawn a rule by 1-based UI/config row index.
     * Works for Manual rules too (they are not kept in the auto-scheduler state list).
     * Ignores min players, proximity, and alive-boss gates.
     * @return status message for UI
     */
    public synchronized String forceSpawnByRow(int rowIndex1Based) {
        BossArenaConfig.TimedBossSpawn rule = resolveConfiguredRule(rowIndex1Based);
        if (rule == null) {
            return "Règle introuvable.";
        }
        if (rule.resolveBossPool().isEmpty() || optional(rule.arenaId).isEmpty()) {
            return "Renseignez le pool de boss et l'Arène avant Apparition.";
        }

        TimedSpawnState state = findStateForRule(rule, rowIndex1Based);
        if (state == null) {
            // Ephemeral state for Manual (or otherwise non-scheduled) rules.
            state = new TimedSpawnState(copyRule(rule), System.currentTimeMillis(), resolveRuleLabel(rule, rowIndex1Based));
        }

        long now = System.currentTimeMillis();
        boolean ok = evaluateSpawn(state, now, true);
        if (ok) {
            persistState();
        }
        return ok ? "Apparition forcée lancée." : "Apparition forcée impossible (boss/arène/monde).";
    }

    private BossArenaConfig.TimedBossSpawn resolveConfiguredRule(int rowIndex1Based) {
        Supplier<BossArenaConfig> supplier = configSupplier;
        BossArenaConfig config = supplier != null ? supplier.get() : null;
        if (config == null) {
            return null;
        }
        List<BossArenaConfig.TimedBossSpawn> rules = config.getTimedBossSpawns();
        if (rowIndex1Based < 1 || rowIndex1Based > rules.size()) {
            return null;
        }
        return rules.get(rowIndex1Based - 1);
    }

    private TimedSpawnState findStateForRule(BossArenaConfig.TimedBossSpawn rule, int rowIndex1Based) {
        if (rule == null) {
            return null;
        }
        String expectedLabel = resolveRuleLabel(rule, rowIndex1Based).toLowerCase(Locale.ROOT);
        String spawnKey = resolveSpawnKey(rule);
        for (TimedSpawnState state : states) {
            if (state == null || state.rule == null) {
                continue;
            }
            if (expectedLabel.equals(state.label)) {
                return state;
            }
            if (!spawnKey.isEmpty() && spawnKey.equals(resolveSpawnKey(state.rule))) {
                return state;
            }
        }
        return null;
    }

    private boolean evaluateSpawn(TimedSpawnState state, long now) {
        return evaluateSpawn(state, now, false);
    }

    private boolean evaluateSpawn(TimedSpawnState state, long now, boolean force) {
        BossArenaConfig.TimedBossSpawn rule = state.rule;
        long retryMs = TimeUnit.SECONDS.toMillis(PROXIMITY_RETRY_SECONDS);

        if (rule.isIntervalMode()
                && BossArenaConfig.resolveIntervalSeconds(rule.intervalHours, rule.intervalDays, rule.intervalSeconds) <= 0L
                && BossArenaConfig.resolveIntervalSeconds(rule.intervalEvery, rule.intervalUnit) <= 0L) {
            LOGGER.warning("Timed spawn rule '" + state.label + "' has INTERVAL mode but invalid interval.");
            state.nextSpawnEpochMs = now + TimeUnit.MINUTES.toMillis(5L);
            return true;
        }

        if (!force && rule.preventDuplicateWhileAlive) {
            if (hasAliveBossForRule(rule) || hasAwaitingPrimaryBossForRule(rule)) {
                clearPendingSpawnForRule(rule);
                LOGGER.info("Timed spawn skipped for '" + state.label + "' because a matching boss/encounter is already active.");
                if (rule.isAfterDeathMode()) {
                    state.sawAliveBoss = true;
                    state.nextSpawnEpochMs = WAIT_FOR_DEATH_EPOCH_MS;
                } else {
                    state.arrivalDeadlineMs = 0L;
                    state.nextSpawnEpochMs = now + resolveSpawnIntervalMillis(rule);
                }
                return true;
            }
            // No matching boss alive: clear stale pending so we don't block forever after boss/crate gone
            clearStalePendingForRuleIfNoAliveBoss(rule, now);
            if (isSpawnPendingForRule(rule, now)) {
                LOGGER.info("Timed spawn skipped for '" + state.label + "' because a matching spawn is already pending.");
                if (rule.isAfterDeathMode()) {
                    // Pre-boss waves count as an active encounter until the primary boss exists.
                    state.sawAliveBoss = true;
                    state.nextSpawnEpochMs = WAIT_FOR_DEATH_EPOCH_MS;
                } else {
                    state.nextSpawnEpochMs = now + retryMs;
                }
                return true;
            }
        }

        String configuredBossId = ensureRolledBossId(state, rule);
        String configuredArenaId = optional(rule.arenaId);
        if (configuredBossId.isEmpty() || configuredArenaId.isEmpty()) {
            LOGGER.warning("Timed spawn rule '" + state.label + "' is missing boss pool or arenaId.");
            return false;
        }

        BossDefinition def = BossRegistry.get(configuredBossId);
        if (def == null) {
            LOGGER.warning("Timed spawn rule '" + state.label + "' references unknown bossId '" + configuredBossId + "'.");
            state.rolledBossId = "";
            return false;
        }

        Arena arena = ArenaRegistry.get(configuredArenaId);
        if (arena == null) {
            LOGGER.warning("Timed spawn rule '" + state.label + "' references missing arena '" + configuredArenaId + "'.");
            state.nextSpawnEpochMs = now + TimeUnit.MINUTES.toMillis(5L);
            return false;
        }

        World world = resolveWorld(arena.worldName);
        if (world == null) {
            state.nextSpawnEpochMs = now + minutesToMillis(WORLD_LOOKUP_RETRY_MINUTES);
            LOGGER.warning("Timed spawn rule '" + state.label + "' could not resolve world '" + arena.worldName + "'. Retrying soon.");
            return false;
        }

        if (!force) {
            if (!awaitNearbyPlayersAndGrace(state, rule, world, arena, configuredArenaId, configuredBossId, now, retryMs)) {
                return true;
            }
        } else {
            state.arrivalDeadlineMs = 0L;
            clearGraceState(state);
        }

        if (!force && rule.preventDuplicateWhileAlive) {
            markSpawnPending(rule, state.label, now);
        }

        final boolean announce = true;
        world.execute(() -> {
            long timedDespawnMinutes = resolveDespawnMinutes(rule);
            UUID result = bossSpawnService.spawnBossFromJson(
                    null,
                    configuredBossId,
                    world,
                    arena.getPosition(),
                    arena.arenaId,
                    timedDespawnMinutes,
                    uuid -> {
                        spawnedTimedBossUuids.add(uuid);
                        if (mapMarkerService != null) {
                            mapMarkerService.onTimedBossSpawn(world, uuid);
                        }
                        // Clear pending when boss actually spawns (critical for deferred spawns after pre-boss waves)
                        clearPendingSpawnForRule(rule);
                        if (rule.isAfterDeathMode()) {
                            state.sawAliveBoss = true;
                            state.nextSpawnEpochMs = WAIT_FOR_DEATH_EPOCH_MS;
                        }
                    }
            );
            if (result == null) {
                clearPendingSpawnForRule(rule);
                if (rule.isAfterDeathMode()) {
                    state.sawAliveBoss = false;
                }
                LOGGER.warning("Timed spawn failed for rule '" + state.label + "'.");
                return;
            }
            // Immediate spawn: clear now; deferred spawn: cleared in callback when boss spawns
            if (!BossSpawnService.DEFERRED_SPAWN_UUID.equals(result)) {
                clearPendingSpawnForRule(rule);
                if (rule.isAfterDeathMode()) {
                    state.sawAliveBoss = true;
                    state.nextSpawnEpochMs = WAIT_FOR_DEATH_EPOCH_MS;
                }
            }
            if (announce) {
                BossWaveNotificationService.notifyTimedSpawn(
                        resolveExpectedBossName(configuredBossId),
                        configuredArenaId,
                        world,
                        rule.worldAnnouncementText,
                        rule.announceWorldWide,
                        rule.announceCurrentWorld
                );
            }
            if (BossSpawnService.DEFERRED_SPAWN_UUID.equals(result)) {
                LOGGER.info((force ? "Forced" : "Timed") + " spawn sequence started for rule '" + state.label + "'. Boss will spawn after pre-boss waves.");
            } else {
                LOGGER.info((force ? "Forced" : "Timed") + " spawn created boss '" + configuredBossId + "' for rule '" + state.label + "' (uuid=" + result + ").");
            }
        });

        clearGraceState(state);
        state.rolledBossId = "";
        state.lastWaitingPlayersTitleMs = 0L;
        state.prevNearbyPlayerUuids = Set.of();
        if (rule.isAfterDeathMode()) {
            // Keep schedule parked while the encounter starts. For deferred pre-boss waves the primary
            // boss is not alive yet — mark encounter active via pending/awaiting instead of a fake death cycle.
            state.sawAliveBoss = true;
            state.nextSpawnEpochMs = WAIT_FOR_DEATH_EPOCH_MS;
            state.reminderSent = false;
        } else {
            state.arrivalDeadlineMs = 0L;
            state.nextSpawnEpochMs = now + resolveSpawnIntervalMillis(rule);
            if (!force) {
                state.reminderSent = false;
            }
        }
        return true;
    }

    private String ensureRolledBossId(TimedSpawnState state, BossArenaConfig.TimedBossSpawn rule) {
        String existing = optional(state.rolledBossId);
        if (!existing.isEmpty() && rule.poolContainsBoss(existing)) {
            return existing;
        }
        String picked = rule.pickWeightedBossId(bossPoolRandom);
        state.rolledBossId = picked;
        if (!picked.isEmpty()) {
            LOGGER.info("Timed spawn '" + state.label + "': pool rolled boss '" + picked + "'.");
        }
        return picked;
    }

    /**
     * Nearby-player gate + optional grace countdown.
     * @return true when spawn may proceed now
     */
    private boolean awaitNearbyPlayersAndGrace(TimedSpawnState state,
                                               BossArenaConfig.TimedBossSpawn rule,
                                               World world,
                                               Arena arena,
                                               String configuredArenaId,
                                               String configuredBossId,
                                               long now,
                                               long retryMs) {
        int required = Math.max(0, rule.minPlayers);
        boolean graceEnabled = rule.gracePeriodEnabled
                && (rule.isIntervalMode() || rule.isAfterDeathMode());
        long graceSeconds = Math.max(0L, rule.gracePeriodSeconds);
        boolean needsRadius = required > 0 || graceEnabled;

        double proximityRadius = arena.getProximityRadius();
        Vector3d center = arena.getPosition();
        if (needsRadius && proximityRadius <= 0.0d) {
            LOGGER.warning("Timed spawn '" + state.label + "' needs Rayon Décl > 0 on arena '"
                    + configuredArenaId + "' (Joueurs/Grâce).");
            state.nextSpawnEpochMs = now + retryMs;
            return false;
        }

        Set<UUID> nearby = collectNearbyPlayerUuids(world, center, proximityRadius);
        int nearbyCount = nearby.size();
        boolean someoneEntered = false;
        for (UUID uuid : nearby) {
            if (!state.prevNearbyPlayerUuids.contains(uuid)) {
                someoneEntered = true;
                break;
            }
        }
        state.prevNearbyPlayerUuids = nearby;

        // Planifié: open / expire arrival window while waiting.
        if (rule.isIntervalMode()) {
            long windowMs = resolveArrivalWindowMillis(rule);
            if (state.arrivalDeadlineMs <= 0L) {
                state.arrivalDeadlineMs = now + windowMs;
                LOGGER.info("Timed spawn '" + state.label + "': fenêtre d'arrivée ouverte ("
                        + Math.max(0L, windowMs / 1000L) + "s).");
            }
            boolean thresholdMet = required == 0
                    ? (!graceEnabled || nearbyCount >= 1)
                    : nearbyCount >= required;
            if (!thresholdMet
                    && (windowMs <= 0L || now >= state.arrivalDeadlineMs)) {
                state.arrivalDeadlineMs = 0L;
                clearGraceState(state);
                state.nextSpawnEpochMs = now + resolveSpawnIntervalMillis(rule);
                state.reminderSent = false;
                LOGGER.info("Timed spawn '" + state.label
                        + "': delai d'arrivee ecoule - spawn annule jusqu'au prochain intervalle.");
                return false;
            }
        }

        boolean thresholdMet = required == 0
                ? (!graceEnabled || nearbyCount >= 1)
                : nearbyCount >= required;

        if (!thresholdMet) {
            clearGraceState(state);
            if (rule.isIntervalMode() && required > 0 && nearbyCount > 0) {
                maybeNotifyWaitingPlayers(state, world, center, proximityRadius, required, someoneEntered, now);
            }
            state.nextSpawnEpochMs = now + (rule.isIntervalMode() ? retryMs : TimeUnit.SECONDS.toMillis(NO_PLAYER_RETRY_SECONDS));
            if (rule.isIntervalMode() && state.arrivalDeadlineMs > 0L) {
                state.nextSpawnEpochMs = Math.min(state.nextSpawnEpochMs, state.arrivalDeadlineMs);
            }
            LOGGER.info("Timed spawn '" + state.label + "' waiting nearby players: "
                    + nearbyCount + "/" + Math.max(1, required) + " in Rayon Décl.");
            return false;
        }

        if (graceEnabled && graceSeconds > 0L) {
            if (state.graceDeadlineMs <= 0L) {
                state.graceDeadlineMs = now + TimeUnit.SECONDS.toMillis(graceSeconds);
                state.lastGraceTitleMs = 0L;
                LOGGER.info("Timed spawn '" + state.label + "': grâce démarrée (" + graceSeconds + "s).");
            }
            if (now < state.graceDeadlineMs) {
                maybeNotifyGraceTitle(state, rule, world, center, proximityRadius,
                        configuredBossId, configuredArenaId, now);
                state.nextSpawnEpochMs = Math.min(state.graceDeadlineMs, now + GRACE_TITLE_REFRESH_MS);
                return false;
            }
            // Grace elapsed — spawn.
            state.arrivalDeadlineMs = 0L;
            clearGraceState(state);
            return true;
        }

        // Ready, no grace (or grace seconds = 0).
        state.arrivalDeadlineMs = 0L;
        clearGraceState(state);
        return true;
    }

    private static void clearGraceState(TimedSpawnState state) {
        state.graceDeadlineMs = 0L;
        state.lastGraceTitleMs = 0L;
    }

    private void maybeNotifyWaitingPlayers(TimedSpawnState state,
                                           World world,
                                           Vector3d center,
                                           double radius,
                                           int required,
                                           boolean someoneEntered,
                                           long now) {
        if (!someoneEntered && state.lastWaitingPlayersTitleMs > 0L
                && (now - state.lastWaitingPlayersTitleMs) < WAITING_PLAYERS_TITLE_INTERVAL_MS) {
            return;
        }
        state.lastWaitingPlayersTitleMs = now;
        BossWaveNotificationService.notifyTimedWaitingPlayersTitle(world, center, radius, required);
    }

    private void maybeNotifyGraceTitle(TimedSpawnState state,
                                       BossArenaConfig.TimedBossSpawn rule,
                                       World world,
                                       Vector3d center,
                                       double radius,
                                       String configuredBossId,
                                       String configuredArenaId,
                                       long now) {
        if (state.lastGraceTitleMs > 0L && (now - state.lastGraceTitleMs) < GRACE_TITLE_REFRESH_MS) {
            return;
        }
        state.lastGraceTitleMs = now;
        long remaining = Math.max(0L, state.graceDeadlineMs - now);
        BossWaveNotificationService.notifyTimedGraceTitle(
                world,
                center,
                radius,
                resolveExpectedBossName(configuredBossId),
                configuredArenaId,
                rule.graceTitleText,
                remaining
        );
    }

    private static Set<UUID> collectNearbyPlayerUuids(World world, Vector3d center, double radius) {
        Set<UUID> out = new HashSet<>();
        if (world == null || center == null || radius <= 0.0d) {
            return out;
        }
        double radiusSq = radius * radius;
        try {
            for (var playerRef : world.getPlayerRefs()) {
                if (playerRef == null || !playerRef.isValid()) {
                    continue;
                }
                var transform = playerRef.getTransform();
                if (transform == null) {
                    continue;
                }
                org.joml.Vector3d rawPos = transform.getPosition();
                if (rawPos == null) {
                    continue;
                }
                double dx = rawPos.x - center.x;
                double dy = rawPos.y - center.y;
                double dz = rawPos.z - center.z;
                if ((dx * dx) + (dy * dy) + (dz * dz) > radiusSq) {
                    continue;
                }
                UUID uuid = EntityComponents.uuid(playerRef);
                if (uuid != null) {
                    out.add(uuid);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static boolean hasAnyOnlinePlayer(World world) {
        return countOnlinePlayers(world) > 0;
    }

    private static int countOnlinePlayers(World world) {
        if (world == null) {
            return 0;
        }
        int count = 0;
        try {
            for (var playerRef : world.getPlayerRefs()) {
                if (playerRef != null && playerRef.isValid()) {
                    count++;
                }
            }
        } catch (Exception ignored) {
        }
        return count;
    }

    /** Returns distance of closest player to center, or Double.MAX_VALUE if no players. */
    private static double closestPlayerDistance(World world, Vector3d center) {
        if (world == null || center == null) {
            return Double.MAX_VALUE;
        }
        double minDistSq = Double.MAX_VALUE;
        try {
            for (var playerRef : world.getPlayerRefs()) {
                if (playerRef == null || !playerRef.isValid()) {
                    continue;
                }
                var transform = playerRef.getTransform();
                if (transform == null) {
                    continue;
                }
                org.joml.Vector3d rawPos = transform.getPosition();
                if (rawPos == null) {
                    continue;
                }
                double dx = rawPos.x - center.x;
                double dy = rawPos.y - center.y;
                double dz = rawPos.z - center.z;
                double distSq = (dx * dx) + (dy * dy) + (dz * dz);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                }
            }
        } catch (Exception ignored) {
        }
        return minDistSq < Double.MAX_VALUE ? Math.sqrt(minDistSq) : Double.MAX_VALUE;
    }

    private static boolean hasPlayerWithinRadius(World world, Vector3d center, double radius) {
        if (world == null || center == null || radius <= 0.0d) {
            return false;
        }
        double radiusSq = radius * radius;
        try {
            for (var playerRef : world.getPlayerRefs()) {
                if (playerRef == null || !playerRef.isValid()) {
                    continue;
                }
                var transform = playerRef.getTransform();
                if (transform == null) {
                    continue;
                }
                org.joml.Vector3d rawPos = transform.getPosition();
                if (rawPos == null) {
                    continue;
                }
                double dx = rawPos.x - center.x;
                double dy = rawPos.y - center.y;
                double dz = rawPos.z - center.z;
                double distSq = (dx * dx) + (dy * dy) + (dz * dz);
                if (distSq <= radiusSq) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void enforceTimedDespawn(TimedSpawnState state, long now) {
        BossArenaConfig.TimedBossSpawn rule = state.rule;
        long despawnMinutes = resolveDespawnMinutes(rule);
        if (despawnMinutes <= 0L) {
            return;
        }
        long maxAgeMs = minutesToMillis(despawnMinutes);

        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry : trackingSystem.snapshotTrackedBosses().entrySet()) {
            UUID bossUuid = entry.getKey();
            BossTrackingSystem.BossData data = entry.getValue();
            if (!matchesRule(data, rule)) {
                continue;
            }
            long spawnedAt = Math.max(0L, data.spawnedAtEpochMs);
            if (spawnedAt <= 0L || (now - spawnedAt) < maxAgeMs) {
                continue;
            }
            timedDespawnBoss(state.label, bossUuid, data);
        }
    }

    private void timedDespawnBoss(String ruleLabel, UUID bossUuid, BossTrackingSystem.BossData data) {
        if (bossUuid == null || data == null) {
            return;
        }
        if (!trackingSystem.isTracked(bossUuid)) {
            return;
        }

        BossTrackingSystem.EventMembersSnapshot eventSnapshot = trackingSystem.snapshotEventMembersForBoss(bossUuid);
        Set<UUID> bossUuids = new HashSet<>();
        Set<UUID> addUuids = new HashSet<>();
        if (eventSnapshot != null) {
            bossUuids.addAll(eventSnapshot.bossUuids);
            addUuids.addAll(eventSnapshot.activeAddUuids);
        } else {
            bossUuids.add(bossUuid);
            addUuids.addAll(trackingSystem.snapshotAddsForBoss(bossUuid));
        }
        bossUuids.remove(null);
        addUuids.remove(null);
        if (bossUuids.isEmpty()) {
            bossUuids.add(bossUuid);
        }
        spawnedTimedBossUuids.removeAll(bossUuids);

        World world = eventSnapshot != null && eventSnapshot.world != null
                ? eventSnapshot.world
                : data.world;
        Vector3d notifyLocation = eventSnapshot != null && eventSnapshot.eventCenter != null
                ? eventSnapshot.eventCenter
                : data.spawnLocation;
        String notifyBossName = eventSnapshot != null && eventSnapshot.bossName != null && !eventSnapshot.bossName.isBlank()
                ? eventSnapshot.bossName
                : data.bossName;

        for (UUID addUuid : addUuids) {
            trackingSystem.handleTrackedAddDeath(addUuid);
        }
        for (UUID eventBossUuid : bossUuids) {
            trackingSystem.markBossDead(eventBossUuid);
        }

        if (world != null) {
            world.execute(() -> {
                for (UUID eventBossUuid : bossUuids) {
                    if (mapMarkerService != null) {
                        mapMarkerService.onTimedBossDespawn(world, eventBossUuid);
                    }
                    removeEntity(world, eventBossUuid);
                }
                for (UUID addUuid : addUuids) {
                    removeEntity(world, addUuid);
                }
            });
            BossWaveNotificationService.notifyBossAliveStatus(
                    world,
                    notifyLocation,
                    notifyBossName,
                    0,
                    0,
                    "Timed despawn",
                    0L,
                    false,
                    false
            );
        }

        LOGGER.info("Timed despawn removed event '" + notifyBossName + "' via rule '" + ruleLabel
                + "' (bosses=" + bossUuids.size() + ", adds=" + addUuids.size() + ").");
    }

    private boolean hasAliveBossForRule(BossArenaConfig.TimedBossSpawn rule) {
        for (BossTrackingSystem.BossData data : trackingSystem.snapshotTrackedBosses().values()) {
            if (matchesRule(data, rule)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAwaitingPrimaryBossForRule(BossArenaConfig.TimedBossSpawn rule) {
        if (rule == null || trackingSystem == null) {
            return false;
        }
        if (rule.resolveBossPool().isEmpty()) {
            return false;
        }
        String expectedArena = optional(rule.arenaId);
        for (BossTrackingSystem.ActiveEventStatus event : trackingSystem.snapshotActiveEvents()) {
            if (event == null || !event.awaitingPrimaryBossSpawn) {
                continue;
            }
            if (!rule.poolContainsBoss(optional(event.bossName))) {
                continue;
            }
            if (!expectedArena.isEmpty()
                    && event.arenaId != null
                    && !event.arenaId.isBlank()
                    && !expectedArena.equalsIgnoreCase(event.arenaId.trim())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean isSpawnPendingForRule(BossArenaConfig.TimedBossSpawn rule, long now) {
        String key = resolveSpawnKey(rule);
        if (key.isEmpty()) {
            return false;
        }
        PendingSpawnState pending = pendingSpawnByKey.get(key);
        if (pending == null) {
            return false;
        }
        if ((now - pending.startedAtEpochMs) > PENDING_SPAWN_TIMEOUT_MS) {
            if (pendingSpawnByKey.remove(key, pending)) {
                LOGGER.warning("Cleared stale pending timed spawn '" + pending.ruleLabel
                        + "' after timeout (" + PENDING_SPAWN_TIMEOUT_MS + "ms).");
            }
            return false;
        }
        return true;
    }

    private void markSpawnPending(BossArenaConfig.TimedBossSpawn rule, String label, long now) {
        String key = resolveSpawnKey(rule);
        if (key.isEmpty()) {
            return;
        }
        PendingSpawnState pending = new PendingSpawnState();
        pending.ruleLabel = optional(label);
        pending.startedAtEpochMs = now;
        pendingSpawnByKey.put(key, pending);
    }

    private void clearPendingSpawnForRule(BossArenaConfig.TimedBossSpawn rule) {
        String key = resolveSpawnKey(rule);
        if (key.isEmpty()) {
            return;
        }
        pendingSpawnByKey.remove(key);
    }

    /**
     * When no matching boss is alive but pending exists and is older than {@link #STALE_PENDING_WHEN_NO_BOSS_MS},
     * clear it so the next spawn can run (avoids "already pending" forever after boss/crate are gone).
     */
    private void clearStalePendingForRuleIfNoAliveBoss(BossArenaConfig.TimedBossSpawn rule, long now) {
        if (hasAliveBossForRule(rule) || hasAwaitingPrimaryBossForRule(rule)) {
            return;
        }
        String key = resolveSpawnKey(rule);
        if (key.isEmpty()) {
            return;
        }
        PendingSpawnState pending = pendingSpawnByKey.get(key);
        if (pending == null) {
            return;
        }
        if ((now - pending.startedAtEpochMs) < STALE_PENDING_WHEN_NO_BOSS_MS) {
            return;
        }
        if (pendingSpawnByKey.remove(key, pending)) {
            LOGGER.info("Cleared stale pending timed spawn '" + pending.ruleLabel
                    + "' (no matching boss alive for " + (STALE_PENDING_WHEN_NO_BOSS_MS / 60_000L) + " min).");
        }
    }

    private void pruneExpiredPendingSpawns(long now) {
        for (Map.Entry<String, PendingSpawnState> entry : pendingSpawnByKey.entrySet()) {
            PendingSpawnState pending = entry.getValue();
            if (pending == null) {
                pendingSpawnByKey.remove(entry.getKey());
                continue;
            }
            if ((now - pending.startedAtEpochMs) <= PENDING_SPAWN_TIMEOUT_MS) {
                continue;
            }
            if (pendingSpawnByKey.remove(entry.getKey(), pending)) {
                LOGGER.warning("Cleared stale pending timed spawn '" + pending.ruleLabel
                        + "' after timeout (" + PENDING_SPAWN_TIMEOUT_MS + "ms).");
            }
        }
    }

    private void persistState() {
        Path path = persistencePath;
        if (path == null) {
            return;
        }

        synchronized (persistenceLock) {
            PersistedState state = new PersistedState();
            Map<String, Long> nextByLabel = new HashMap<>();
            for (TimedSpawnState timedState : states) {
                if (timedState == null || timedState.label == null || timedState.label.isBlank()) {
                    continue;
                }
                PersistedRuleState row = new PersistedRuleState();
                row.label = timedState.label;
                row.nextSpawnEpochMs = Math.max(0L, timedState.nextSpawnEpochMs);
                state.rules.add(row);
                nextByLabel.put(row.label, row.nextSpawnEpochMs);
            }

            String json = GSON.toJson(state);
            Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try {
                Files.writeString(temp, json, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
                persistedNextSpawnByLabel = nextByLabel;
            } catch (IOException e) {
                LOGGER.warning("Failed to persist timed spawn state: " + e.getMessage());
            }
        }
    }

    private Map<String, Long> loadPersistedNextSpawnMap() {
        Path path = persistencePath;
        if (path == null || Files.notExists(path)) {
            return Map.of();
        }

        synchronized (persistenceLock) {
            try {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                PersistedState state = GSON.fromJson(raw, PersistedState.class);
                if (state == null || state.version != PERSISTENCE_VERSION || state.rules == null) {
                    return Map.of();
                }

                Map<String, Long> out = new HashMap<>();
                for (PersistedRuleState row : state.rules) {
                    if (row == null) {
                        continue;
                    }
                    String label = optional(row.label).toLowerCase(Locale.ROOT);
                    if (label.isEmpty()) {
                        continue;
                    }
                    out.put(label, Math.max(0L, row.nextSpawnEpochMs));
                }
                return out;
            } catch (Exception e) {
                LOGGER.warning("Failed to load timed spawn state: " + e.getMessage());
                return Map.of();
            }
        }
    }

    private void rebuildTrackedTimedBossCache(List<TimedSpawnState> activeStates) {
        if (activeStates == null || activeStates.isEmpty()) {
            spawnedTimedBossUuids.clear();
            return;
        }

        Set<UUID> refreshed = new HashSet<>();
        Map<UUID, BossTrackingSystem.BossData> tracked = trackingSystem.snapshotTrackedBosses();
        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry : tracked.entrySet()) {
            UUID bossUuid = entry.getKey();
            BossTrackingSystem.BossData data = entry.getValue();
            if (bossUuid == null || data == null) {
                continue;
            }
            for (TimedSpawnState state : activeStates) {
                if (state == null || state.rule == null) {
                    continue;
                }
                if (!matchesRule(data, state.rule)) {
                    continue;
                }
                refreshed.add(bossUuid);
                break;
            }
        }
        spawnedTimedBossUuids.clear();
        spawnedTimedBossUuids.addAll(refreshed);
    }

    public Set<UUID> snapshotSpawnedTimedBossUuids() {
        return Set.copyOf(spawnedTimedBossUuids);
    }

    public void forgetSpawnedTimedBoss(UUID bossUuid) {
        if (bossUuid == null) {
            return;
        }
        spawnedTimedBossUuids.remove(bossUuid);
    }

    private static final class PersistedState {
        int version = PERSISTENCE_VERSION;
        List<PersistedRuleState> rules = new ArrayList<>();
    }

    private static final class PersistedRuleState {
        String label;
        long nextSpawnEpochMs;
    }

    private static final class PendingSpawnState {
        String ruleLabel;
        long startedAtEpochMs;
    }

    private static final class TimedSpawnState {
        private final BossArenaConfig.TimedBossSpawn rule;
        private final String label;
        private volatile long nextSpawnEpochMs;
        private volatile boolean sawAliveBoss;
        /** Planifié: deadline epoch for a player to reach the arena after the due time (0 = closed). */
        private volatile long arrivalDeadlineMs;
        /** True after the 5-minute pre-spawn reminder was sent for the current cycle. */
        private volatile boolean reminderSent;
        /** Grace countdown end epoch (0 = not in grace). */
        private volatile long graceDeadlineMs;
        private volatile long lastGraceTitleMs;
        private volatile long lastWaitingPlayersTitleMs;
        private volatile Set<UUID> prevNearbyPlayerUuids = Set.of();
        /** Weighted pool pick for the current spawn cycle (announce/grace/reminder/spawn). */
        private volatile String rolledBossId = "";

        private TimedSpawnState(BossArenaConfig.TimedBossSpawn rule, long nextSpawnEpochMs, String label) {
            this.rule = rule;
            this.label = label == null || label.isBlank() ? "timed_spawn" : label.toLowerCase(Locale.ROOT);
            this.nextSpawnEpochMs = Math.max(0L, nextSpawnEpochMs);
            this.sawAliveBoss = false;
            this.arrivalDeadlineMs = 0L;
            this.reminderSent = false;
            this.graceDeadlineMs = 0L;
            this.lastGraceTitleMs = 0L;
            this.lastWaitingPlayersTitleMs = 0L;
            this.prevNearbyPlayerUuids = Set.of();
            this.rolledBossId = "";
        }
    }
}
