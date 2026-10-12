package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.component.PendingScalingComponent;

import javax.annotation.Nonnull;

/**
 * Drains {@link PendingScalingComponent} markers left by {@code MobScalingRefSystem.onEntityAdded},
 * running the actual zone lookup + scaling for a time-boxed slice of mobs each tick instead of all
 * of them at once. Spreads a chunk-load burst (server boot, exploring a new area) over several
 * frames instead of one CPU/allocation spike.
 *
 * <p>There is no "start of tick" hook on {@link EntityTickingSystem} to reset a per-tick counter
 * against, and consecutive ticks share the same {@code dt} at a fixed tick rate so that can't be
 * used to detect a new tick either. Instead this tracks the wall-clock time of the last processed
 * entry: once {@link #GAP_NANOS} has passed without one (i.e. the query went empty, or the engine
 * moved on to other systems for at least that long), the budget window resets.
 */
public class MobScalingQueueSystem extends EntityTickingSystem<EntityStore> {

    /** Wall-clock budget per window. Leaves most of a ~30ms tick for everything else, while still
     *  draining a boot-sized burst within a couple of seconds. */
    private static final long BUDGET_NANOS_PER_WINDOW = 3_000_000L; // 3ms
    /** Idle gap after which the next processed entry starts a fresh budget window. */
    private static final long GAP_NANOS = 5_000_000L; // 5ms

    private final MobScalingRefSystem scalingRefSystem;
    private long windowStartNanos;
    private long budgetUsedNanosThisWindow;
    private long lastProcessedNanos;

    public MobScalingQueueSystem(@Nonnull MobScalingRefSystem scalingRefSystem) {
        this.scalingRefSystem = scalingRefSystem;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PendingScalingComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        long now = System.nanoTime();
        if (now - lastProcessedNanos > GAP_NANOS) {
            windowStartNanos = now;
            budgetUsedNanosThisWindow = 0;
        }
        if (budgetUsedNanosThisWindow >= BUDGET_NANOS_PER_WINDOW) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        commandBuffer.removeComponent(ref, PendingScalingComponent.getComponentType());
        try {
            scalingRefSystem.resolveAndApplyScaling(ref, store, commandBuffer);
        } catch (Exception ignored) {
        }

        lastProcessedNanos = System.nanoTime();
        budgetUsedNanosThisWindow += lastProcessedNanos - now;
    }
}
