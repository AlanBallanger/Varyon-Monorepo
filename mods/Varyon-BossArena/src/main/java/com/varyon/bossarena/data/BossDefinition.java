package com.varyon.bossarena.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BossDefinition {
    public String bossName;
    public String npcId;
    public String tier = "common";
    public int amount;
    // 0 = default RPGLeveling behavior, >=1 forces a specific spawn level.
    public int levelOverride = 0;

    /**
     * Basename of an OGG in the BossArena {@code music/} folder (optional). Empty = no fight music.
     */
    public String musicFileName = "";
    /** Sphere radius (blocks) around the encounter center for fight music. Default 30. */
    public double musicRadius = 30.0d;

    /**
     * When true, each boss entity spawns at a random point within {@link #spawnSpreadRadius}.
     * When false, bosses use a spiral around the arena center (spacing from the radius).
     */
    public boolean useRandomBossSpawn = false;
    /** Boss spawn spread radius in blocks (random disk, or spiral spacing base). Default 15. */
    public double spawnSpreadRadius = 15.0d;

    public Modifiers modifiers = new Modifiers();
    public PerPlayerIncrease perPlayerIncrease = new PerPlayerIncrease();
    public ExtraMobs extraMobs = new ExtraMobs();

    public boolean hasFightMusic() {
        return musicFileName != null && !musicFileName.isBlank();
    }

    public double getMusicRadius() {
        if (!Double.isFinite(musicRadius) || musicRadius <= 0.0d) {
            return 30.0d;
        }
        return musicRadius;
    }

    public double getSpawnSpreadRadius() {
        if (!Double.isFinite(spawnSpreadRadius) || spawnSpreadRadius < 0.0d) {
            return 15.0d;
        }
        return spawnSpreadRadius;
    }

    /**
     * One-time migration of the legacy per-boss timed proximity settings onto arenas.
     * <p>
     * For every boss that had proximity enabled with a valid radius and a resolvable arena,
     * the proximity settings are copied onto that arena (unless the arena already has proximity
     * enabled), then cleared from the boss. Must be called after both bosses and arenas are loaded.
     *
     * @return true when at least one boss was migrated (callers should persist bosses and arenas).
     */
    public static boolean migrateProximityToArenas() {
        boolean migratedAny = false;
        for (BossDefinition def : BossRegistry.getAll().values()) {
            if (def == null || def.extraMobs == null) {
                continue;
            }
            ExtraMobs extra = def.extraMobs;
            if (!extra.timedProximityEnabled) {
                continue;
            }
            double radius = extra.getTimedProximityRadius();
            String arenaId = extra.timedProximityArenaId == null ? "" : extra.timedProximityArenaId.trim();
            if (radius > 0.0d && !arenaId.isEmpty()) {
                Arena arena = ArenaRegistry.get(arenaId);
                if (arena != null && !arena.proximityEnabled) {
                    arena.proximityEnabled = true;
                    arena.proximityRadius = radius;
                    arena.proximityCooldownSeconds = extra.getTimedProximityCooldownSeconds(60L);
                }
            }
            extra.timedProximityEnabled = false;
            extra.timedProximityArenaId = "";
            extra.timedProximityRadius = 0.0d;
            migratedAny = true;
        }
        return migratedAny;
    }

    public static class Modifiers {
        public float hp = 1.0f;
        public float damage = 1.0f;
        public float movementSpeed = 1.0f;
        public float size = 1.0f;
        public float attackRate = 1.0f;
        public float abilityCooldown = 1.0f;
        public float knockbackGiven = 1.0f;
        public float knockbackTaken = 1.0f;
        public float turnRate = 1.0f;
        /** Flat HP restored every second (0, 1, 5, 10, 20, 50, 100, 200, 500, 1000). */
        public float regen = 0.0f;
    }

    public static class PerPlayerIncrease {
        /** Multiplier applied as {@code base × value × playerCount}. Default 1.0 = ×1 par joueur. */
        public float hp = 1.0f;
        public float damage = 1.0f;
        public float movementSpeed = 1.0f;
        public float size = 1.0f;
        public float attackRate = 1.0f;
        public float abilityCooldown = 1.0f;
        public float knockbackGiven = 1.0f;
        public float knockbackTaken = 1.0f;
        public float turnRate = 1.0f;
        public float regen = 1.0f;
    }

    public static class ExtraMobs {
        public static final String TRIGGER_BEFORE_BOSS = "before_boss";
        public static final String TRIGGER_ON_SPAWN = "on_spawn";
        public static final String TRIGGER_AFTER_SPAWN_SECONDS = "after_spawn_seconds";
        public static final String TRIGGER_SINCE_LAST_WAVE = "since_last_wave";
        public static final String TRIGGER_BOSS_HP_PERCENT = "boss_hp_percent";

        /** When to spawn the boss when using before_boss waves. */
        public static final String BOSS_SPAWN_AFTER_BEFORE_BOSS = "after_before_boss";
        /** Spawn boss after a fixed delay in seconds (from encounter start). */
        public static final String BOSS_SPAWN_AFTER_SECONDS = "after_seconds";

        public String npcId;
        public long timeLimitMs;
        public int waves;
        public int mobsPerWave = 3;
        /**
         * Legacy global switch. Migrated on sanitize into per-wave {@link ScheduledWave#enabled}.
         * Always forced back to true after migration.
         */
        public boolean wavesEnabled = true;
        public boolean useRandomSpawnLocations = true;
        public double randomSpawnRadius = 15.0d;
        /**
         * Extra wave mob count per player beyond the first, as a multiplier: 1.3 = +30% mobs per
         * additional player. Applied as {@code base × (1 + (mult - 1) × (players - 1))}, rounded to
         * the nearest integer, same formula as {@link PerPlayerIncrease}.
         */
        public float mobsPerPlayerMult = 1.0f;
        // New format: multiple add definitions with per-wave cadence.
        public List<WaveAdd> adds = new ArrayList<>();
        // Trigger-based wave schedule. If empty, legacy fields are auto-migrated.
        public List<ScheduledWave> scheduledWaves = new ArrayList<>();
        /** Boss spawn trigger: after_before_boss (wait for pre-boss adds dead) or after_seconds. */
        public String bossSpawnTrigger = BOSS_SPAWN_AFTER_BEFORE_BOSS;
        /** Seconds for after_seconds trigger; ignored for after_before_boss. */
        public double bossSpawnTriggerValue = 0.0d;

        /** Whether timed boss spawns should wait for proximity before spawning. */
        public boolean timedProximityEnabled = false;
        /**
         * Arena id used as the proximity center for timed boss spawns when {@link #timedProximityEnabled} is true.
         * If empty, the timed spawn rule's arenaId is used.
         */
        public String timedProximityArenaId = "";
        /** Proximity radius in blocks for timed boss spawns; <= 0 disables proximity even when enabled is true. */
        public double timedProximityRadius = 0.0d;
        /** Cooldown between proximity spawns for this boss (seconds). 0 uses the plugin default. */
        public long timedProximityCooldownSeconds = 60L;

        private static double sanitizeWaveRandomSpawnRadius(double radius) {
            if (!Double.isFinite(radius) || radius < 0.0d) {
                return 15.0d;
            }
            return radius;
        }

        public double getTimedProximityRadius() {
            if (!Double.isFinite(timedProximityRadius) || timedProximityRadius <= 0.0d) {
                return 0.0d;
            }
            return timedProximityRadius;
        }

        public long getTimedProximityCooldownSeconds(long defaultSeconds) {
            if (timedProximityCooldownSeconds <= 0L) {
                return defaultSeconds;
            }
            // Clamp to a reasonable range (1 second to 7 days).
            long min = 1L;
            long max = 7L * 24L * 60L * 60L;
            long value = timedProximityCooldownSeconds;
            if (value < min) value = min;
            if (value > max) value = max;
            return value;
        }

        private static WaveAdd sanitizeWaveAdd(WaveAdd add, boolean requireNpcId) {
            if (add == null) {
                return null;
            }
            String id = add.npcId == null ? "" : add.npcId.trim();
            if (requireNpcId && id.isEmpty()) {
                return null;
            }
            WaveAdd out = new WaveAdd();
            out.npcId = id;
            int min = add.mobsPerWaveMin;
            int max = add.mobsPerWaveMax;
            if (min < 1 && max < 1) {
                // Legacy config (pre min/max): fall back to the single mobsPerWave value.
                min = Math.max(1, add.mobsPerWave);
                max = min;
            }
            min = Math.max(1, min);
            max = Math.max(min, max);
            out.mobsPerWaveMin = min;
            out.mobsPerWaveMax = max;
            out.mobsPerWave = min;
            out.everyWave = Math.max(1, add.everyWave);
            out.hp = (!Float.isFinite(add.hp) || add.hp <= 0f) ? 1.0f : add.hp;
            out.damage = (!Float.isFinite(add.damage) || add.damage <= 0f) ? 1.0f : add.damage;
            out.size = (!Float.isFinite(add.size) || add.size <= 0f) ? 1.0f : add.size;
            return out;
        }

        /**
         * Rolls a random spawn count within {@code [mobsPerWaveMin, mobsPerWaveMax]} (inclusive),
         * after scaling both bounds by {@code mobsPerPlayerMult} for the given player count.
         */
        public static int rollMobCount(WaveAdd add, float mobsPerPlayerMult, int playerCount) {
            if (add == null) {
                return 1;
            }
            int min = scalePlayerMobCount(Math.max(1, add.mobsPerWaveMin), mobsPerPlayerMult, playerCount);
            int max = scalePlayerMobCount(Math.max(1, add.mobsPerWaveMax), mobsPerPlayerMult, playerCount);
            max = Math.max(min, max);
            if (min == max) {
                return min;
            }
            return min + (int) Math.floor(Math.random() * (max - min + 1));
        }

        /** {@code round(base × (1 + (mult - 1) × (players - 1)))}, floored at 1. */
        public static int scalePlayerMobCount(int base, float mobsPerPlayerMult, int playerCount) {
            if (!Float.isFinite(mobsPerPlayerMult) || mobsPerPlayerMult <= 1.0f) {
                return base;
            }
            int extraPlayers = Math.max(0, playerCount - 1);
            float scaleFactor = 1.0f + (mobsPerPlayerMult - 1.0f) * extraPlayers;
            return Math.max(1, Math.round(base * scaleFactor));
        }

        private static ScheduledWave sanitizeScheduledWave(ScheduledWave wave) {
            if (wave == null) {
                return null;
            }
            String trigger = normalizeTrigger(wave.trigger);
            if (trigger == null) {
                return null;
            }
            // Fold legacy "on_spawn" into "after_spawn_seconds" at 0s (Avec le boss, immédiat).
            double triggerValue = Double.isFinite(wave.triggerValue) ? wave.triggerValue : 0.0d;
            if (TRIGGER_ON_SPAWN.equals(trigger)) {
                trigger = TRIGGER_AFTER_SPAWN_SECONDS;
                triggerValue = 0.0d;
            }

            ScheduledWave out = new ScheduledWave();
            out.enabled = wave.enabled;
            out.trigger = trigger;
            out.triggerValue = triggerValue;
            out.repeatCount = wave.repeatCount == 0 ? 1 : wave.repeatCount;
            if (out.repeatCount < -1) {
                out.repeatCount = -1;
            }
            out.repeatEverySeconds = Double.isFinite(wave.repeatEverySeconds) ? wave.repeatEverySeconds : 0.0d;
            if (out.repeatEverySeconds < 0d) {
                out.repeatEverySeconds = 0.0d;
            }

            if (TRIGGER_BOSS_HP_PERCENT.equals(out.trigger)) {
                if (out.triggerValue < 0d) {
                    out.triggerValue = 0d;
                } else if (out.triggerValue > 100d) {
                    out.triggerValue = 100d;
                }
            } else {
                if (out.triggerValue < 0d) {
                    out.triggerValue = 0d;
                }
            }

            out.adds = new ArrayList<>();
            List<WaveAdd> srcAdds = wave.adds != null ? wave.adds : List.of();
            for (WaveAdd add : srcAdds) {
                WaveAdd sanitizedAdd = sanitizeWaveAdd(add, true);
                if (sanitizedAdd != null) {
                    sanitizedAdd.everyWave = 1;
                    out.adds.add(sanitizedAdd);
                }
            }
            if (out.adds.isEmpty()) {
                return null;
            }

            if (out.repeatCount == 1) {
                out.repeatEverySeconds = 0.0d;
            } else if (out.repeatEverySeconds <= 0.0d) {
                if (TRIGGER_BOSS_HP_PERCENT.equals(out.trigger)) {
                    out.repeatEverySeconds = 1.0d;
                } else {
                    out.repeatEverySeconds = out.triggerValue > 0.0d ? out.triggerValue : 1.0d;
                }
            }

            return out;
        }

        private static List<ScheduledWave> migrateLegacyWaves(List<WaveAdd> legacyAdds, int legacyWaves, long legacyIntervalMs) {
            if (legacyAdds == null || legacyAdds.isEmpty() || legacyWaves == 0) {
                return new ArrayList<>();
            }

            double intervalSeconds = Math.max(0.001d, legacyIntervalMs / 1000.0d);
            List<ScheduledWave> out = new ArrayList<>();

            for (WaveAdd add : legacyAdds) {
                WaveAdd sanitizedAdd = sanitizeWaveAdd(add, true);
                if (sanitizedAdd == null) {
                    continue;
                }
                int every = Math.max(1, sanitizedAdd.everyWave);
                double firstAtSeconds = intervalSeconds * every;
                int repeatCount;
                if (legacyWaves < 0) {
                    repeatCount = -1;
                } else {
                    repeatCount = legacyWaves / every;
                }
                if (repeatCount == 0) {
                    continue;
                }

                ScheduledWave wave = new ScheduledWave();
                wave.enabled = true;
                wave.trigger = TRIGGER_AFTER_SPAWN_SECONDS;
                wave.triggerValue = firstAtSeconds;
                wave.repeatCount = repeatCount;
                wave.repeatEverySeconds = intervalSeconds * every;
                sanitizedAdd.everyWave = 1;
                wave.adds = new ArrayList<>(List.of(sanitizedAdd));
                out.add(wave);
            }

            return out;
        }

        private static ScheduledWave copyScheduledWave(ScheduledWave wave) {
            ScheduledWave out = new ScheduledWave();
            out.enabled = wave.enabled;
            out.trigger = wave.trigger;
            out.triggerValue = wave.triggerValue;
            out.repeatCount = wave.repeatCount;
            out.repeatEverySeconds = wave.repeatEverySeconds;
            out.adds = new ArrayList<>();
            if (wave.adds != null) {
                for (WaveAdd add : wave.adds) {
                    WaveAdd copy = sanitizeWaveAdd(add, true);
                    if (copy != null) {
                        copy.everyWave = 1;
                        out.adds.add(copy);
                    }
                }
            }
            return out;
        }

        private static String normalizeTrigger(String trigger) {
            if (trigger == null || trigger.isBlank()) {
                return TRIGGER_AFTER_SPAWN_SECONDS;
            }
            String normalized = trigger.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return switch (normalized) {
                case TRIGGER_BEFORE_BOSS, TRIGGER_ON_SPAWN, TRIGGER_AFTER_SPAWN_SECONDS, TRIGGER_SINCE_LAST_WAVE,
                     TRIGGER_BOSS_HP_PERCENT -> normalized;
                default -> null;
            };
        }

        public void sanitize() {
            if (timeLimitMs < 0L) {
                timeLimitMs = 0L;
            }
            if (waves < -1) {
                waves = -1;
            }
            if (mobsPerWave < 1) {
                mobsPerWave = 3;
            }
            if (!Float.isFinite(mobsPerPlayerMult) || mobsPerPlayerMult < 1.0f) {
                mobsPerPlayerMult = 1.0f;
            } else if (mobsPerPlayerMult > 3.0f) {
                mobsPerPlayerMult = 3.0f;
            }
            randomSpawnRadius = sanitizeWaveRandomSpawnRadius(randomSpawnRadius);
            if (!Double.isFinite(timedProximityRadius) || timedProximityRadius < 0.0d) {
                timedProximityRadius = 0.0d;
            }
            if (timedProximityCooldownSeconds < 0L) {
                timedProximityCooldownSeconds = 0L;
            }
            if (timedProximityArenaId == null) {
                timedProximityArenaId = "";
            } else {
                timedProximityArenaId = timedProximityArenaId.trim();
            }

            if (adds == null) {
                adds = new ArrayList<>();
            }
            if (scheduledWaves == null) {
                scheduledWaves = new ArrayList<>();
            }

            List<WaveAdd> cleaned = new ArrayList<>();
            for (WaveAdd add : adds) {
                WaveAdd sanitized = sanitizeWaveAdd(add, true);
                if (sanitized != null) {
                    cleaned.add(sanitized);
                }
            }
            adds = cleaned;

            if (!adds.isEmpty()) {
                // Keep legacy fields in sync with first configured add for UI/backward compatibility.
                WaveAdd first = adds.get(0);
                npcId = first.npcId;
                mobsPerWave = first.mobsPerWave;
            } else {
                String legacyNpcId = npcId == null ? "" : npcId.trim();
                if (!legacyNpcId.isEmpty()) {
                    WaveAdd legacy = new WaveAdd();
                    legacy.npcId = legacyNpcId;
                    legacy.mobsPerWave = Math.max(1, mobsPerWave);
                    legacy.everyWave = 1;
                    adds.add(legacy);
                    npcId = legacy.npcId;
                    mobsPerWave = legacy.mobsPerWave;
                }
            }

            List<ScheduledWave> cleanedWaves = new ArrayList<>();
            for (ScheduledWave wave : scheduledWaves) {
                ScheduledWave sanitized = sanitizeScheduledWave(wave);
                if (sanitized != null) {
                    cleanedWaves.add(sanitized);
                }
            }
            scheduledWaves = cleanedWaves;

            if (scheduledWaves.isEmpty()) {
                scheduledWaves = migrateLegacyWaves(adds, waves, timeLimitMs);
            }

            // Legacy global OFF → disable each wave once, then clear the global flag.
            if (!wavesEnabled) {
                for (ScheduledWave wave : scheduledWaves) {
                    if (wave != null) {
                        wave.enabled = false;
                    }
                }
                wavesEnabled = true;
            }

            String normalizedBossSpawn = normalizeBossSpawnTrigger(bossSpawnTrigger);
            bossSpawnTrigger = normalizedBossSpawn != null ? normalizedBossSpawn : BOSS_SPAWN_AFTER_BEFORE_BOSS;
            bossSpawnTriggerValue = Double.isFinite(bossSpawnTriggerValue) ? Math.max(0.0d, bossSpawnTriggerValue) : 0.0d;
        }

        private static String normalizeBossSpawnTrigger(String trigger) {
            if (trigger == null || trigger.isBlank()) {
                return BOSS_SPAWN_AFTER_BEFORE_BOSS;
            }
            String normalized = trigger.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            if (BOSS_SPAWN_AFTER_BEFORE_BOSS.equals(normalized) || BOSS_SPAWN_AFTER_SECONDS.equals(normalized)) {
                return normalized;
            }
            return null;
        }

        public boolean hasConfiguredAdds() {
            sanitize();
            if (!adds.isEmpty()) {
                return true;
            }
            for (ScheduledWave wave : scheduledWaves) {
                if (wave != null && wave.adds != null && !wave.adds.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        public List<WaveAdd> getConfiguredAdds() {
            sanitize();
            return new ArrayList<>(adds);
        }

        /** All configured waves (including disabled), for the editor / summary. */
        public List<ScheduledWave> getAllScheduledWaves() {
            sanitize();
            List<ScheduledWave> out = new ArrayList<>();
            for (ScheduledWave wave : scheduledWaves) {
                if (wave == null || wave.adds == null || wave.adds.isEmpty()) {
                    continue;
                }
                out.add(copyScheduledWave(wave));
            }
            return out;
        }

        /** Enabled waves only — used at fight start. */
        public List<ScheduledWave> getResolvedScheduledWaves() {
            List<ScheduledWave> out = new ArrayList<>();
            for (ScheduledWave wave : getAllScheduledWaves()) {
                if (wave != null && wave.enabled) {
                    out.add(wave);
                }
            }
            return out;
        }

        /**
         * Planned wave executions for UI (sum of finite {@code repeatCount}).
         * Returns 0 when any wave repeats infinitely ({@code repeatCount < 0}).
         */
        public int countPlannedWaveExecutions() {
            List<ScheduledWave> waves = getResolvedScheduledWaves();
            int total = 0;
            for (ScheduledWave wave : waves) {
                if (wave == null) {
                    continue;
                }
                if (wave.repeatCount < 0) {
                    return 0;
                }
                total += Math.max(1, wave.repeatCount);
            }
            return total;
        }

        /**
         * Total mobs (wave adds) planned across the whole fight.
         * Returns 0 if any enabled wave repeats infinitely (repeatCount &lt; 0) — total unknown.
         */
        public int countPlannedTotalMobs() {
            List<ScheduledWave> waves = getResolvedScheduledWaves();
            int total = 0;
            for (ScheduledWave wave : waves) {
                if (wave == null) {
                    continue;
                }
                if (wave.repeatCount < 0) {
                    return 0;
                }
                int executions = Math.max(1, wave.repeatCount);
                int mobsPerExecution = 0;
                if (wave.adds != null) {
                    for (WaveAdd add : wave.adds) {
                        if (add != null && add.npcId != null && !add.npcId.isBlank()) {
                            mobsPerExecution += Math.max(1, Math.max(add.mobsPerWaveMin, add.mobsPerWaveMax));
                        }
                    }
                }
                total += executions * mobsPerExecution;
            }
            return total;
        }

        public void setPrimaryAdd(String inputNpcId, int inputMobsPerWave) {
            String normalizedNpcId = inputNpcId == null ? "" : inputNpcId.trim();
            npcId = normalizedNpcId;
            mobsPerWave = Math.max(1, inputMobsPerWave);

            if (adds == null) {
                adds = new ArrayList<>();
            }
            if (normalizedNpcId.isEmpty()) {
                if (!adds.isEmpty()) {
                    adds.remove(0);
                }
                sanitize();
                return;
            }

            WaveAdd primary;
            if (adds.isEmpty()) {
                primary = new WaveAdd();
                adds.add(primary);
            } else {
                primary = adds.get(0);
            }

            primary.npcId = normalizedNpcId;
            primary.mobsPerWave = Math.max(1, inputMobsPerWave);
            if (primary.everyWave < 1) {
                primary.everyWave = 1;
            }

            sanitize();
        }

        public double getWaveRandomSpawnRadius() {
            sanitize();
            return randomSpawnRadius;
        }

        public static class WaveAdd {
            public String npcId;
            /** @deprecated superseded by {@link #mobsPerWaveMin}/{@link #mobsPerWaveMax}; kept for legacy config migration. */
            @Deprecated
            public int mobsPerWave = 3;
            /** Random spawn count range (inclusive) rolled fresh each time this add fires. */
            public int mobsPerWaveMin = 3;
            public int mobsPerWaveMax = 3;
            // 1 = every wave, 2 = every 2nd wave, etc.
            public int everyWave = 1;
            public float hp = 1.0f;
            public float damage = 1.0f;
            public float size = 1.0f;
        }

        public static class ScheduledWave {
            /** When false, this wave is kept in config but not run during fights. */
            public boolean enabled = true;
            public String trigger = TRIGGER_AFTER_SPAWN_SECONDS;
            // Seconds for time-based triggers, HP percent for hp trigger.
            public double triggerValue = 0.0d;
            // Number of executions. -1 = infinite.
            public int repeatCount = 1;
            // Repeat period in seconds for repeated schedules.
            public double repeatEverySeconds = 0.0d;
            public List<WaveAdd> adds = new ArrayList<>();
        }
    }
}
