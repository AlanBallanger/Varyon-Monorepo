package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.config.MobFragmentsConfig;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class BreakOreCleanupListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PlacedOreTracker tracker;
    private final ConfigManager configManager;
    private final EssenceRewardsConfig rewardsConfig;

    public BreakOreCleanupListener(@Nonnull PlacedOreTracker tracker,
                                   @Nonnull ConfigManager configManager,
                                   @Nonnull EssenceRewardsConfig rewardsConfig) {
        super(BreakBlockEvent.class);
        this.tracker = tracker;
        this.configManager = configManager;
        this.rewardsConfig = rewardsConfig;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        try {
            if (event.getBlockType() == null) return;
            String blockId = event.getBlockType().getId().toLowerCase();

            MobFragmentsConfig fragmentsConfig = configManager.getMobFragmentsConfig();
            if (fragmentsConfig.getMiningFragmentWeight(blockId) <= 0 && rewardsConfig.getOreReward(blockId) <= 0) {
                return;
            }

            Vector3i pos = event.getTargetBlock();
            if (pos == null) return;

            String world = resolveWorld(store);
            boolean wasTracked = tracker.isPlayerPlaced(world, pos);
            tracker.remove(world, pos);
            if (wasTracked) {
                LOGGER.at(Level.INFO).log("AntiExploit cleanup: removed " + world + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " block=" + blockId);
            }

        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in BreakOreCleanupListener: " + e.getMessage());
        }
    }

    private String resolveWorld(@Nonnull Store<EntityStore> store) {
        try {
            if (store.getExternalData() != null && ((EntityStore) store.getExternalData()).getWorld() != null) {
                return ((EntityStore) store.getExternalData()).getWorld().getName();
            }
        } catch (Exception ignored) {}
        return "world";
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
