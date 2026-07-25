package com.varyon.bossarena.loot;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

/**
 * Opens boss loot chests on F (Use) via UseBlockEvent.Pre — same pattern as Comet.
 * LivingEntityUseBlockEvent alone is unreliable for custom chest blocks.
 */
public final class BossLootChestUseSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final Logger LOGGER = Logger.getLogger("BossArena");

    private static final int[][] NEIGHBORS = {
            {0, 0, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    public BossLootChestUseSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull UseBlockEvent.Pre event) {
        if (event.getInteractionType() != InteractionType.Use) {
            return;
        }
        Vector3i target = event.getTargetBlock();
        if (target == null) {
            return;
        }

        Object external = store.getExternalData();
        if (!(external instanceof EntityStore entityStore)) {
            return;
        }
        World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        if (playerRef == null) {
            return;
        }

        for (int[] d : NEIGHBORS) {
            int x = target.x + d[0];
            int y = target.y + d[1];
            int z = target.z + d[2];
            boolean known = isBossArenaChestBlock(world, x, y, z)
                    || BossLootChestBlock.getAt(world, x, y, z) != null;
            if (!known) {
                continue;
            }
            if (BossLootHandler.ensureBossLootChestBlock(world, x, y, z) == null) {
                continue;
            }
            event.setCancelled(true);
            boolean opened = BossLootChestOpener.open(world, playerRef, store, commandBuffer, x, y, z);
            if (opened) {
                LOGGER.info("Boss chest opened via UseBlockEvent at " + x + "," + y + "," + z);
            }
            return;
        }
    }

    private static boolean isBossArenaChestBlock(World world, int x, int y, int z) {
        BlockType type = world.getBlockType(x, y, z);
        if (type == null || type.getId() == null) {
            return false;
        }
        String id = type.getId();
        return id.contains("Boss_Arena_Chest")
                || id.contains("Furniture_Dungeon_Chest_Legendary");
    }
}
