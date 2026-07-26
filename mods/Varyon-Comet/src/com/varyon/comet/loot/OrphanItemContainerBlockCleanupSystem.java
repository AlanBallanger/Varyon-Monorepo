package com.varyon.comet.loot;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.system.ItemContainerBlockSpatialSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Removes {@code ItemContainerBlock} entities that lost their {@link BlockModule.BlockStateInfo}.
 * Must run before {@link ItemContainerBlockSpatialSystem}, which NPEs on null blockInfo.
 */
public final class OrphanItemContainerBlockCleanupSystem extends EntityTickingSystem<ChunkStore> {

    private static final Logger LOGGER = Logger.getLogger("Varyon-Comet");
    private static final AtomicInteger REMOVED_LOGGED = new AtomicInteger();

    @Nullable
    private final ComponentType<ChunkStore, ?> itemContainerType;
    @Nonnull
    private final ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoType;
    @Nullable
    private final Query<ChunkStore> query;

    public OrphanItemContainerBlockCleanupSystem() {
        this.blockStateInfoType = BlockModule.BlockStateInfo.getComponentType();
        this.itemContainerType = resolveItemContainerBlockType();
        this.query = itemContainerType != null ? Query.and(itemContainerType) : null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static ComponentType<ChunkStore, ?> resolveItemContainerBlockType() {
        try {
            Class<?> cls = Class.forName(
                    "com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock");
            return (ComponentType<ChunkStore, ?>) cls.getMethod("getComponentType").invoke(null);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "ItemContainerBlock type unavailable; orphan cleanup disabled", t);
            return null;
        }
    }

    @Override
    @Nullable
    public Query<ChunkStore> getQuery() {
        return query;
    }

    @Override
    @Nonnull
    public Set<Dependency<ChunkStore>> getDependencies() {
        Set<Dependency<ChunkStore>> deps = new HashSet<>();
        deps.add(new SystemDependency<>(
                Order.BEFORE,
                ItemContainerBlockSpatialSystem.class,
                OrderPriority.FURTHEST));
        return deps;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store,
            @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        if (itemContainerType == null) {
            return;
        }
        if (archetypeChunk.getComponent(index, blockStateInfoType) != null) {
            return;
        }
        commandBuffer.tryRemoveComponent(archetypeChunk.getReferenceTo(index), itemContainerType);
        int n = REMOVED_LOGGED.incrementAndGet();
        if (n <= 20 || n % 50 == 0) {
            LOGGER.warning("Removed orphan ItemContainerBlock (no BlockStateInfo), count=" + n);
        }
    }
}
