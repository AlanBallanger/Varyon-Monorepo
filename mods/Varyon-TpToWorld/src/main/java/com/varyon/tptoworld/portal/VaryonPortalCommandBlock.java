package com.varyon.tptoworld.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

public final class VaryonPortalCommandBlock implements Component<ChunkStore> {

    private static volatile ComponentType<ChunkStore, VaryonPortalCommandBlock> componentType;

    public static final BuilderCodec<VaryonPortalCommandBlock> CODEC = BuilderCodec.builder(
                    VaryonPortalCommandBlock.class, VaryonPortalCommandBlock::new)
            .append(new KeyedCodec<>("Command", Codec.STRING),
                    (b, v) -> b.command = v, b -> b.command).add()
            .build();

    private String command = "";

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

    public static VaryonPortalCommandBlock getAt(World world, int x, int y, int z) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(x, y, z);
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
        this.command = command != null ? command : "";
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command != null ? command : "";
    }

    @Override
    public Component<ChunkStore> clone() {
        return new VaryonPortalCommandBlock(this.command);
    }
}
