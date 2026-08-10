package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.config.PointsRewardsConfig;
import com.varyon.config.MobFragmentsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class PlaceOreListener extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PlacedOreTracker tracker;
    private final ConfigManager configManager;
    private final PointsRewardsConfig rewardsConfig;

    public PlaceOreListener(@Nonnull PlacedOreTracker tracker,
                            @Nonnull ConfigManager configManager,
                            @Nonnull PointsRewardsConfig rewardsConfig) {
        super(PlaceBlockEvent.class);
        this.tracker = tracker;
        this.configManager = configManager;
        this.rewardsConfig = rewardsConfig;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {
        try {
            ItemStack item = event.getItemInHand();
            if (item == null) return;

            String itemId = item.getItemId().toLowerCase();
            MobFragmentsConfig fragmentsConfig = configManager.getMobFragmentsConfig();
            if (fragmentsConfig.getMiningFragmentWeight(itemId) <= 0 && rewardsConfig.getOreReward(itemId) <= 0) {
                return;
            }

            Vector3i pos = event.getTargetBlock();
            if (pos == null) return;

            String world = resolveWorld(store);

            tracker.add(world, pos);
            LOGGER.at(Level.INFO).log("Tracked placed ore " + itemId + " at " + world + ":" + pos.x + "," + pos.y + "," + pos.z);

        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in PlaceOreListener: " + e.getMessage());
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

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
