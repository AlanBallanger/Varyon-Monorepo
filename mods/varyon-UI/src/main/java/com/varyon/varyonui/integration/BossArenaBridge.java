package com.varyon.varyonui.integration;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads upcoming boss-spawn timers from Varyon-BossArena for display in varyon-UI.
 * Mirrors {@link CombatProfilBridge}'s reflection-bridge pattern: separate jars, no
 * compile-time dependency between the two mods.
 */
public final class BossArenaBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");
    private static final String PLUGIN_CLASS = "com.varyon.bossarena.BossArenaPlugin";
    private static final String SCHEDULER_CLASS = "com.varyon.bossarena.spawn.BossTimedSpawnScheduler";

    private static final Map<String, java.util.Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, java.util.Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    private BossArenaBridge() {}

    public static final class UpcomingSpawn {
        public final String label;
        public final String bossName;
        public final String arenaId;
        public final int minTier;
        public final long nextSpawnEpochMs;
        public final boolean waitingForBossDeath;
        public final boolean readyOrInGrace;

        UpcomingSpawn(String label, String bossName, String arenaId, int minTier,
                      long nextSpawnEpochMs, boolean waitingForBossDeath, boolean readyOrInGrace) {
            this.label = label;
            this.bossName = bossName;
            this.arenaId = arenaId;
            this.minTier = minTier;
            this.nextSpawnEpochMs = nextSpawnEpochMs;
            this.waitingForBossDeath = waitingForBossDeath;
            this.readyOrInGrace = readyOrInGrace;
        }

        public boolean isReady() {
            if (waitingForBossDeath) return false;
            if (readyOrInGrace) return true;
            return nextSpawnEpochMs >= 0 && nextSpawnEpochMs <= System.currentTimeMillis();
        }
    }

    /** Returns an empty list if BossArena is not installed or its scheduler isn't ready yet. */
    @Nonnull
    public static List<UpcomingSpawn> snapshotUpcomingSpawns() {
        try {
            java.util.Optional<Class<?>> pluginClass = resolveClass(PLUGIN_CLASS);
            if (pluginClass.isEmpty()) return List.of();

            java.util.Optional<Method> getInstance = resolveMethod(pluginClass.get(), "getInstance");
            if (getInstance.isEmpty()) return List.of();
            Object plugin = getInstance.get().invoke(null);
            if (plugin == null) return List.of();

            java.util.Optional<Method> getScheduler = resolveMethod(plugin.getClass(), "getTimedSpawnScheduler");
            if (getScheduler.isEmpty()) return List.of();
            Object scheduler = getScheduler.get().invoke(plugin);
            if (scheduler == null) return List.of();

            java.util.Optional<Method> snapshot = resolveMethod(scheduler.getClass(), "snapshotUpcomingSpawns");
            if (snapshot.isEmpty()) return List.of();
            Object rows = snapshot.get().invoke(scheduler);
            if (!(rows instanceof List<?> rowList)) return List.of();

            List<UpcomingSpawn> out = new ArrayList<>(rowList.size());
            for (Object row : rowList) {
                UpcomingSpawn converted = convertRow(row);
                if (converted != null) {
                    out.add(converted);
                }
            }
            return out;
        } catch (Throwable t) {
            LOG.log(Level.FINE, "BossArenaBridge: snapshot failed", t);
            return List.of();
        }
    }

    private static UpcomingSpawn convertRow(Object row) {
        if (row == null) return null;
        try {
            Class<?> rowClass = row.getClass();
            String label = (String) readField(rowClass, row, "label");
            String bossName = (String) readField(rowClass, row, "bossName");
            String arenaId = (String) readField(rowClass, row, "arenaId");
            int minTier = ((Number) readField(rowClass, row, "minTier")).intValue();
            long nextSpawnEpochMs = ((Number) readField(rowClass, row, "nextSpawnEpochMs")).longValue();
            boolean waiting = (Boolean) readField(rowClass, row, "waitingForBossDeath");
            boolean readyOrInGrace = (Boolean) readField(rowClass, row, "readyOrInGrace");
            return new UpcomingSpawn(label, bossName, arenaId, minTier, nextSpawnEpochMs, waiting, readyOrInGrace);
        } catch (Throwable t) {
            LOG.log(Level.FINE, "BossArenaBridge: row conversion failed", t);
            return null;
        }
    }

    private static Object readField(Class<?> owner, Object target, String name) throws Exception {
        Field f = owner.getField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static java.util.Optional<Class<?>> resolveClass(String name) {
        return CLASS_CACHE.computeIfAbsent(name, n -> {
            try {
                return java.util.Optional.of(Class.forName(n));
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        });
    }

    private static java.util.Optional<Method> resolveMethod(Class<?> owner, String name, Class<?>... paramTypes) {
        StringBuilder key = new StringBuilder(owner.getName()).append('#').append(name);
        for (Class<?> pt : paramTypes) {
            key.append(',').append(pt.getName());
        }
        return METHOD_CACHE.computeIfAbsent(key.toString(), k -> {
            try {
                Method m = owner.getMethod(name, paramTypes);
                m.setAccessible(true);
                return java.util.Optional.of(m);
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        });
    }
}
