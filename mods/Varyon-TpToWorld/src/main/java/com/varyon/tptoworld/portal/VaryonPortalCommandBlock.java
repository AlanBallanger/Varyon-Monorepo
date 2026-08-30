package com.varyon.tptoworld.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

public final class VaryonPortalCommandBlock implements Component<ChunkStore> {

    private static volatile ComponentType<ChunkStore, VaryonPortalCommandBlock> componentType;

    public static final BuilderCodec<VaryonPortalCommandBlock> CODEC = BuilderCodec.builder(
                    VaryonPortalCommandBlock.class, VaryonPortalCommandBlock::new)
            .append(new KeyedCodec<>("Command", Codec.STRING),
                    (b, v) -> b.command = v, b -> b.command).add()
            .append(new KeyedCodec<>("AsServer", Codec.BOOLEAN),
                    (b, v) -> b.asServer = v, b -> b.asServer).add()
            .build();

    private String command = "";
    private boolean asServer = true;

    public static ComponentType<ChunkStore, VaryonPortalCommandBlock> getComponentType() {
        ComponentType<ChunkStore, VaryonPortalCommandBlock> type = componentType;
        if (type == null) {
            throw new IllegalStateException("VaryonPortalCommandBlock component type not registered");
        }
        return type;
    }

    public static void setComponentType(ComponentType<ChunkStore, VaryonPortalCommandBlock> type) {
        componentType = type;
    }

    /**
     * Reads the block component via a chunk that is already in memory. Going through
     * world.getBlockComponentHolder(...) instead would call World#getChunk, which since the game
     * update can trigger an async chunk load (loadChunkIfInMemory -> startsTicking -> addEntities)
     * — illegal from inside a system/interaction tick ("Store is currently processing"). Any
     * portal a player is interacting with is in a loaded chunk, so getChunkIfInMemory suffices.
     */
    public static VaryonPortalCommandBlock getAt(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) {
            return null;
        }
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return null;
        }
        Holder<ChunkStore> holder = chunk.getBlockComponentHolder(x, y, z);
        if (holder == null) {
            return null;
        }
        return holder.getComponent(getComponentType());
    }

    public static VaryonPortalCommandBlock getAt(World world, Vector3i pos) {
        return getAt(world, pos.x, pos.y, pos.z);
    }

    public VaryonPortalCommandBlock() {
    }

    public VaryonPortalCommandBlock(String command) {
        this(command, true);
    }

    public VaryonPortalCommandBlock(String command, boolean asServer) {
        this.command = command != null ? command : "";
        this.asServer = asServer;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command != null ? command : "";
    }

    public boolean isAsServer() {
        return asServer;
    }

    public void setAsServer(boolean asServer) {
        this.asServer = asServer;
    }

    @Override
    public Component<ChunkStore> clone() {
        return new VaryonPortalCommandBlock(this.command, this.asServer);
    }
}
