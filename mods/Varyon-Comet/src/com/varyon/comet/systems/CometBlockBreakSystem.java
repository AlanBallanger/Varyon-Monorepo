package com.varyon.comet.systems;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.loot.CometLootBlockStateUtil;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.system.EntityEventSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CometBlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final CometWaveManager waveManager;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CometBlockBreakSystem.class.getName());

    public CometBlockBreakSystem(CometWaveManager waveManager) {
        super(BreakBlockEvent.class);
        this.waveManager = waveManager;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {

        com.hypixel.hytale.math.vector.Vector3i blockPos = event.getTargetBlock();
        if (blockPos == null) return;

        // Position-based: only treat as comet if this position is registered
        if (waveManager.getRegisteredBlockPos(blockPos.x, blockPos.y, blockPos.z) == null) {
            return;
        }

        // Only the owner can break the crystal
        java.util.UUID owner = waveManager.getCometOwner(blockPos);
        java.util.UUID breakerUuid = null;

        try {
            Ref<EntityStore> breakerRef = archetypeChunk.getReferenceTo(index);
            if (breakerRef != null && breakerRef.isValid()) {
                UUIDComponent uc = store.getComponent(breakerRef, UUIDComponent.getComponentType());
                if (uc != null) {
                    breakerUuid = uc.getUuid();
                }

                if (breakerUuid == null) {
                    com.hypixel.hytale.server.core.universe.PlayerRef pr = store.getComponent(breakerRef,
                            com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                    if (pr != null) {
                        breakerUuid = pr.getUuid();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error identifying breaker: " + e.getMessage());
        }

        // Check if globalComets is enabled - if so, any player can trigger any comet
        CometConfig config = CometConfig.getInstance();
        boolean globalComets = (config != null && config.globalComets);

        if (!globalComets && owner != null && (breakerUuid == null || !breakerUuid.equals(owner))) {
            try {
                event.setCancelled(true);
            } catch (Exception e) {
                LOGGER.warning("Failed to cancel event: " + e.getMessage());
            }
            return;
        }

        try {
            com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                    .getExternalData()).getWorld();

            Object blockState = CometLootBlockStateUtil.getState(world, blockPos.x, blockPos.y, blockPos.z, true);
            CometLootBlockStateUtil.clearWindowsIfContainer(blockState);

            waveManager.handleBlockBreak(store, blockPos);
            CometDespawnTracker.getInstance().unregisterComet(blockPos);
        } catch (Throwable t) {
            LOGGER.severe("Error handling block break: " + t);
            t.printStackTrace();
        }
    }
}
