package com.varyon.debugging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;

/**
 * In-memory counters of entity spawns/removals since server start, keyed by world and by
 * "world|type|reason". Read by {@code /spawnreport} to compare worlds without needing to
 * re-profile with spark each time.
 */
public final class SpawnRegistry {

    private static final SpawnRegistry INSTANCE = new SpawnRegistry();

    private final Map<String, LongAdder> spawnsByWorld = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> spawnsByWorldAndType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> removalsByWorld = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> liveCountByWorldAndType = new ConcurrentHashMap<>();

    private SpawnRegistry() {}

    @Nonnull
    public static SpawnRegistry get() {
        return INSTANCE;
    }

    public void recordSpawn(@Nonnull String world, @Nonnull String type) {
        spawnsByWorld.computeIfAbsent(world, w -> new LongAdder()).increment();
        spawnsByWorldAndType.computeIfAbsent(world + "|" + type, k -> new LongAdder()).increment();
        liveCountByWorldAndType.computeIfAbsent(world + "|" + type, k -> new LongAdder()).increment();
    }

    public void recordRemoval(@Nonnull String world, @Nonnull String type) {
        removalsByWorld.computeIfAbsent(world, w -> new LongAdder()).increment();
        LongAdder live = liveCountByWorldAndType.get(world + "|" + type);
        if (live != null) {
            live.decrement();
        }
    }

    @Nonnull
    public Map<String, LongAdder> getSpawnsByWorld() {
        return spawnsByWorld;
    }

    @Nonnull
    public Map<String, LongAdder> getSpawnsByWorldAndType() {
        return spawnsByWorldAndType;
    }

    @Nonnull
    public Map<String, LongAdder> getRemovalsByWorld() {
        return removalsByWorld;
    }

    @Nonnull
    public Map<String, LongAdder> getLiveCountByWorldAndType() {
        return liveCountByWorldAndType;
    }

    public void reset() {
        spawnsByWorld.clear();
        spawnsByWorldAndType.clear();
        removalsByWorld.clear();
        liveCountByWorldAndType.clear();
    }
}
