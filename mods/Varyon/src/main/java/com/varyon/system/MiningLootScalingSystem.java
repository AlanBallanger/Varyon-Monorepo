package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.util.MiningOreBlockIds;
import com.varyon.util.ZoneCalculator;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.List;

public class MiningLootScalingSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    @Nonnull
    private final ConfigManager    configManager;
    @Nonnull
    private final PlacedOreTracker placedOreTracker;

    public MiningLootScalingSystem(@Nonnull ConfigManager configManager,
                                   @Nonnull PlacedOreTracker placedOreTracker) {
        super(BreakBlockEvent.class);
        this.configManager = configManager;
        this.placedOreTracker = placedOreTracker;
    }

    @Override
    public void handle(int index, @Nonnull com.hypixel.hytale.component.ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        BlockType blockType = event.getBlockType();
        if (blockType == null) {
            return;
        }

        String blockTypeId = blockType.getId();
        if (blockTypeId != null && MiningOreBlockIds.isExcludedFromVaryonOreRewards(blockTypeId)) {
            return;
        }

        BlockGathering gathering = blockType.getGathering();
        if (gathering == null) {
            return;
        }

        BlockBreakingDropType breaking = gathering.getBreaking();
        if (breaking == null) {
            return;
        }

        if (!hasBreakingLoot(blockType, breaking)) {
            return;
        }

        String world = resolveWorld(store);
        if (!configManager.getZoneConfig().isWorldEnabled(world)) {
            return;
        }

        Vector3i blockPos = event.getTargetBlock();
        if (blockPos != null && placedOreTracker.isPlayerPlaced(world, blockPos)) {
            return;
        }

        Ref<EntityStore> minerRef = archetypeChunk.getReferenceTo(index);
        DifficultyZone zone;
        if (blockPos != null) {
            zone = ZoneCalculator.getZoneAtPosition(blockPos.x, blockPos.z, world, configManager.getZoneConfig());
        } else {
            zone = ZoneCalculator.getCurrentZone(store, minerRef, world, configManager.getZoneConfig());
        }

        float lootMultiplier = zone != null ? (float) zone.getLootMultiplier() : 1.0f;
        if (lootMultiplier <= 1.0f) {
            return;
        }

        int extraDropSets = (int) Math.floor(lootMultiplier) - 1;
        float fractionalPart = lootMultiplier - (float) Math.floor(lootMultiplier);
        if (fractionalPart > 0 && Math.random() < fractionalPart) {
            extraDropSets++;
        }
        if (extraDropSets <= 0) {
            return;
        }

        org.joml.Vector3d baseDrop;
        if (blockPos != null) {
            baseDrop = new org.joml.Vector3d(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5);
        } else {
            TransformComponent transform =
                store.getComponent(minerRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            baseDrop = new org.joml.Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);
        }

        HeadRotation headRotation = store.getComponent(minerRef, HeadRotation.getComponentType());
        com.hypixel.hytale.math.vector.Rotation3fc rot = headRotation != null ? headRotation.getRotation() : com.hypixel.hytale.math.vector.Rotation3f.ZERO;

        for (int i = 0; i < extraDropSets; i++) {
            List<ItemStack> stacks = dropsForOneBlockBreak(blockType, breaking);
            if (stacks.isEmpty()) {
                continue;
            }
            org.joml.Vector3d offsetPosition = new org.joml.Vector3d(
                baseDrop.x + (Math.random() - 0.5) * 0.5,
                baseDrop.y + 0.1 * i,
                baseDrop.z + (Math.random() - 0.5) * 0.5
            );
            Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(
                store,
                new ObjectArrayList<>(stacks),
                offsetPosition,
                rot
            );
            commandBuffer.addEntities(drops, AddReason.SPAWN);
        }
    }

    private static boolean hasBreakingLoot(@Nonnull BlockType blockType, @Nonnull BlockBreakingDropType breaking) {
        String dropListId = breaking.getDropListId();
        String itemId = breaking.getItemId();
        if (dropListId != null && !dropListId.isBlank()) {
            return true;
        }
        if (itemId != null && !itemId.isBlank()) {
            return true;
        }
        return blockType.getItem() != null;
    }

    @Nonnull
    private static List<ItemStack> dropsForOneBlockBreak(@Nonnull BlockType blockType,
                                                         @Nonnull BlockBreakingDropType breaking) {
        int quantity = breaking.getQuantity();
        if (quantity <= 0) {
            quantity = 1;
        }
        String dropListId = breaking.getDropListId();
        String itemId = breaking.getItemId();
        boolean hasDropList = dropListId != null && !dropListId.isBlank();
        boolean hasItem = itemId != null && !itemId.isBlank();

        if (!hasDropList && !hasItem) {
            Item item = blockType.getItem();
            if (item == null) {
                return List.of();
            }
            return List.of(new ItemStack(item.getId(), quantity));
        }

        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        ItemModule itemModule = ItemModule.get();
        if (hasDropList && itemModule.isEnabled()) {
            for (int i = 0; i < quantity; ++i) {
                out.addAll(itemModule.getRandomItemDrops(dropListId));
            }
        }
        if (hasItem) {
            out.add(new ItemStack(itemId, quantity));
        }
        return out;
    }

    private static String resolveWorld(@Nonnull Store<EntityStore> store) {
        try {
            if (store.getExternalData() != null && ((EntityStore) store.getExternalData()).getWorld() != null) {
                return ((EntityStore) store.getExternalData()).getWorld().getName();
            }
        } catch (Exception ignored) {
        }
        return "world";
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
