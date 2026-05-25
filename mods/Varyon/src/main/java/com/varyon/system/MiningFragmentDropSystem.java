package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.util.MiningOreBlockIds;
import com.varyon.util.ZoneCalculator;
import com.varyon.system.PlacedOreTracker;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;

public class MiningFragmentDropSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ConfigManager         configManager;
    private final PlacedOreTracker      placedOreTracker;

    public MiningFragmentDropSystem(@Nonnull ConfigManager configManager,
                                    @Nonnull PlacedOreTracker placedOreTracker) {
        super(BreakBlockEvent.class);
        this.configManager      = configManager;
        this.placedOreTracker   = placedOreTracker;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        try {
            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            String world = resolveWorld(store);
            if (!configManager.getZoneConfig().isWorldEnabled(world)) return;

            String blockId = event.getBlockType().getId().toLowerCase();
            if (MiningOreBlockIds.isExcludedFromVaryonOreRewards(blockId)) {
                return;
            }
            int fragments = configManager.getMobFragmentsConfig().rollMiningFragmentDrops(blockId);
            if (fragments <= 0) return;

            if (event.getTargetBlock() != null) {
                if (placedOreTracker.isPlayerPlaced(world, event.getTargetBlock())) {
                    return;
                }
            }

            Ref ref = archetypeChunk.getReferenceTo(index);

            Vector3i blockPos = event.getTargetBlock();
            DifficultyZone zone;
            if (blockPos != null) {
                zone = ZoneCalculator.getZoneAtPosition(blockPos.getX(), blockPos.getZ(), world, configManager.getZoneConfig());
            } else {
                zone = ZoneCalculator.getCurrentZone(store, ref, world, configManager.getZoneConfig());
            }
            int zoneId = zone != null ? zone.getZoneId() : 1;

            Player player = (Player) store.getComponent(ref, Player.getComponentType());
            int lootZoneId = zoneId;
            if (player != null) {
                lootZoneId = Math.min(zoneId, configManager.getZonePermissionsConfig().getMaxAccessibleZone(player));
            }

            String itemId = configManager.getZoneLootConfig().getItemForZone(lootZoneId);
            if (itemId == null || itemId.isBlank()) {
                itemId = "Key_Fragment" + lootZoneId;
            }

            // Drop at player's position (same pattern as MobFragmentDropSystem)
            TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return;

            Vector3d pos = transform.getPosition().clone().add(0.0, 1.0, 0.0);
            HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
            Vector3f rot = headRotation != null ? headRotation.getRotation().clone() : new Vector3f(0f, 0f, 0f);

            Holder[] drops = ItemComponent.generateItemDrops(store, List.of(new ItemStack(itemId, fragments)), pos, rot);
            commandBuffer.addEntities(drops, AddReason.SPAWN);

            LOGGER.at(Level.FINE).log("Mining: " + playerRef.getUsername() + " mined " + blockId
                + " → +" + fragments + "x " + itemId + " (loot zone " + lootZoneId + ", pos zone " + zoneId + ")");

        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in MiningFragmentDropSystem: " + e.getMessage());
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
}
