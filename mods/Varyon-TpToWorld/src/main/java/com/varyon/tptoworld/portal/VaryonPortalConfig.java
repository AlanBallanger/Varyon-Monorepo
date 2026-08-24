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

public final class VaryonPortalConfig implements Component<ChunkStore> {

    public static final String DEFAULT_TYPE = "MagicPortal_ForgottenTemple";
    public static final String NO_BACKGROUND = "";
    public static final float DEFAULT_SCALE = 1.0f;
    public static final float DEFAULT_CENTER_OFFSET_Y = 0.0f;

    public static final String TEMPLATE_ITEM_ID = "Varyon_Portal_Template";
    public static final String TEMPLATE_X2_ITEM_ID = "Varyon_Portal_Template_X2";

    /**
     * Each SystemId bakes its own PositionOffset.Y into its spawners (MagicPortal_ForgottenTemple
     * and MagicPortal_VoidKeyArt use 2.5, Varyon_Portal_Purple uses 1.5), so the base vertical
     * offset needed to line the portal up with the block differs per type. Added on top of the
     * user-facing height field so a height of 0.0 lines up with the block for every type.
     */
    public static float baseHeightOffsetFor(String type) {
        if ("Varyon_Portal_Purple".equals(type)) {
            return 0.0f;
        }
        return 2.0f;
    }

    /**
     * Varyon_Portal_Purple already bakes its own generic city-background spawner
     * (Varyon_CreativeWorld_Purple) into its particlesystem. Stacking one of the
     * Novale/Fraktale/Haven backgrounds on top of it would double up the image, so the
     * user-selected background is ignored for this type instead of being spawned twice.
     */
    public static boolean supportsCustomBackground(String type) {
        return !"Varyon_Portal_Purple".equals(type);
    }

    public static final float DEFAULT_YAW = 0f;

    private static volatile ComponentType<ChunkStore, VaryonPortalConfig> componentType;

    public static final BuilderCodec<VaryonPortalConfig> CODEC = BuilderCodec.builder(
                    VaryonPortalConfig.class, VaryonPortalConfig::new)
            .append(new KeyedCodec<>("Command", Codec.STRING),
                    (b, v) -> b.command = v, b -> b.command).add()
            .append(new KeyedCodec<>("Type", Codec.STRING),
                    (b, v) -> b.type = v, b -> b.type).add()
            .append(new KeyedCodec<>("Background", Codec.STRING),
                    (b, v) -> b.background = v, b -> b.background).add()
            .append(new KeyedCodec<>("Scale", Codec.FLOAT),
                    (b, v) -> b.scale = v, b -> b.scale).add()
            .append(new KeyedCodec<>("CenterOffsetY", Codec.FLOAT),
                    (b, v) -> b.centerOffsetY = v, b -> b.centerOffsetY).add()
            .append(new KeyedCodec<>("Yaw", Codec.FLOAT),
                    (b, v) -> b.yaw = v, b -> b.yaw).add()
            .append(new KeyedCodec<>("AsServer", Codec.BOOLEAN),
                    (b, v) -> b.asServer = v, b -> b.asServer).add()
            .build();

    private String command = "";
    private String type = DEFAULT_TYPE;
    private String background = NO_BACKGROUND;
    private float scale = DEFAULT_SCALE;
    private float centerOffsetY = DEFAULT_CENTER_OFFSET_Y;
    private float yaw = DEFAULT_YAW;
    private boolean asServer = true;

    public static ComponentType<ChunkStore, VaryonPortalConfig> getComponentType() {
        ComponentType<ChunkStore, VaryonPortalConfig> componentTypeLocal = componentType;
        if (componentTypeLocal == null) {
            throw new IllegalStateException("VaryonPortalConfig component type not registered");
        }
        return componentTypeLocal;
    }

    public static void setComponentType(ComponentType<ChunkStore, VaryonPortalConfig> type) {
        componentType = type;
    }

    public static VaryonPortalConfig getAt(World world, int x, int y, int z) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(x, y, z);
        if (holder == null) {
            return null;
        }
        return holder.getComponent(getComponentType());
    }

    public static VaryonPortalConfig getAt(World world, Vector3i pos) {
        return getAt(world, pos.x, pos.y, pos.z);
    }

    public VaryonPortalConfig() {
    }

    public VaryonPortalConfig(String command, String type, String background, float scale, float centerOffsetY,
            float yaw, boolean asServer) {
        this.command = command != null ? command : "";
        this.type = type != null && !type.isBlank() ? type : DEFAULT_TYPE;
        this.background = background != null ? background : NO_BACKGROUND;
        this.scale = scale > 0 ? scale : DEFAULT_SCALE;
        this.centerOffsetY = centerOffsetY;
        this.yaw = yaw;
        this.asServer = asServer;
    }

    public String getCommand() {
        return command;
    }

    public String getType() {
        return type;
    }

    public String getBackground() {
        return background;
    }

    public float getScale() {
        return scale;
    }

    public float getCenterOffsetY() {
        return centerOffsetY;
    }

    public float getEffectiveOffsetY() {
        return centerOffsetY + baseHeightOffsetFor(type);
    }

    public float getYaw() {
        return yaw;
    }

    public boolean isAsServer() {
        return asServer;
    }

    public boolean hasBackground() {
        return background != null && !background.isBlank() && supportsCustomBackground(type);
    }

    @Override
    public Component<ChunkStore> clone() {
        return new VaryonPortalConfig(
                this.command, this.type, this.background, this.scale, this.centerOffsetY, this.yaw, this.asServer);
    }
}
