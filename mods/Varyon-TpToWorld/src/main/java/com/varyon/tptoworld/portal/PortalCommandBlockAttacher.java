package com.varyon.tptoworld.portal;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class PortalCommandBlockAttacher {

    private PortalCommandBlockAttacher() {
    }

    /**
     * World#getBlockComponentHolder returns a copy — mutating it alone does nothing,
     * the write has to go through the live entity or WorldChunk#setState.
     */
    public static boolean attach(World world, int x, int y, int z, String command) {
        if (world == null) {
            return false;
        }
        VaryonPortalCommandBlock custom = new VaryonPortalCommandBlock(command);
        long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) {
            return false;
        }
        int localX = x & 31;
        int localZ = z & 31;

        try {
            Ref<ChunkStore> entityRef = chunk.getBlockComponentEntity(localX, y, localZ);
            if (entityRef != null) {
                Store<ChunkStore> chunkStore = entityRef.getStore();
                if (chunkStore != null) {
                    chunkStore.putComponent(entityRef, VaryonPortalCommandBlock.getComponentType(), custom);
                    if (VaryonPortalCommandBlock.getAt(world, x, y, z) != null) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Holder<ChunkStore> holder = world.getBlockComponentHolder(x, y, z);
            if (holder == null) {
                holder = ChunkStore.REGISTRY.newHolder();
            }
            holder.tryRemoveComponent(VaryonPortalCommandBlock.getComponentType());
            holder.putComponent(VaryonPortalCommandBlock.getComponentType(), custom);
            BlockType blockType = world.getBlockType(x, y, z);
            if (blockType != null) {
                int rotation = chunk.getRotationIndex(localX, y, localZ);
                chunk.setState(localX, y, localZ, blockType, rotation, holder);
            }
            return VaryonPortalCommandBlock.getAt(world, x, y, z) != null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
