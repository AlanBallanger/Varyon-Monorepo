package com.varyon.tptoworld.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3d;
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
     * The dynamic template uses "Varyon_Portal_Purple" as its stored/UI type value, but its own
     * generic Varyon city background is baked into that shared particlesystem (also used by the
     * static, non-customizable Varyon_Portal_Purple item). Spawning the template with that id
     * would always show the Varyon background regardless of the user's Background selection, so
     * the template resolves to a bare variant with no baked background instead; the Varyon
     * background is offered as one of the selectable Background options.
     */
    public static String resolveParticleSystemId(String type) {
        if ("Varyon_Portal_Purple".equals(type)) {
            return "Varyon_Portal_Purple_Template";
        }
        return type;
    }

    /**
     * The Background dropdown's Blue_*_Standalone systems are vertically tuned for the oval
     * Blue portal (whose ring already sits +2.0 higher, see baseHeightOffsetFor), so layering
     * them on the round Purple portal (base offset 0.0) puts the background image below where
     * the ring is drawn. Purple has its own vertically-matched Purple_*_Standalone systems for
     * the same backgrounds; this routes to those when the selected type is Purple.
     */
    public static String resolveBackgroundParticleSystemId(String type, String background) {
        if ("Varyon_Portal_Purple".equals(type) && background != null && background.startsWith("Varyon_CreativeWorld_Blue_")) {
            return "Varyon_CreativeWorld_Purple_" + background.substring("Varyon_CreativeWorld_Blue_".length());
        }
        return background;
    }

    /**
     * The tick system spawns the background at gy, the visual centre of the ring (gy already
     * folds in baseHeightOffsetFor). The *_Standalone background systems carry no baked Y offset,
     * so for oval types (ring drawn at gy) they line up as-is. The round Varyon_Portal_Purple
     * ring, however, is drawn 1.5 higher than gy because Varyon_Portal_Purple_Template's spawners
     * bake PositionOffset.Y = 1.5 while baseHeightOffsetFor returns 0.0 for it. This adds that
     * same 1.5 to the background's Y for the round type only, so the image stays centred on the
     * ring; every other type gets 0.
     */
    public static float backgroundHeightOffsetFor(String type) {
        if ("Varyon_Portal_Purple".equals(type)) {
            return 1.5f;
        }
        return 0.0f;
    }

    /**
     * The X2 template's hitbox is 2 blocks wide, but which world direction it actually spans
     * depends on the block's placement rotation (VariantRotation: NESW) — hardcoding a fixed
     * +X offset put the portal a block off from its hitbox whenever it wasn't placed facing
     * the default direction. BlockType#getBlockCenter returns the true block-local center of
     * the rotation-selected hitbox variant, so it's used instead when available.
     */
    public static void computeHorizontalCenter(World world, int x, int y, int z, boolean wide, Vector3d out) {
        try {
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            BlockType blockType = world.getBlockType(x, y, z);
            if (chunk != null && blockType != null) {
                int rotationIndex = chunk.getRotationIndex(x & 31, y, z & 31);
                Vector3d localCenter = new Vector3d();
                blockType.getBlockCenter(rotationIndex, localCenter);
                out.set(x + localCenter.x, out.y, z + localCenter.z);
                return;
            }
        } catch (Exception ignored) {
        }
        out.set(x + (wide ? 1.0 : 0.5), out.y, z + 0.5);
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

    /**
     * Reads the block component from a chunk already in memory. world.getBlockComponentHolder(...)
     * routes through World#getChunk, which since the game update can kick off an async chunk load
     * — illegal from inside a system/interaction tick ("Store is currently processing"). Callers
     * always target a portal in a loaded chunk, so getChunkIfInMemory is enough.
     */
    public static VaryonPortalConfig getAt(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) {
            return null;
        }
        WorldChunk chunk = world.getChunkIfInMemory(
                com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return null;
        }
        Holder<ChunkStore> holder = chunk.getBlockComponentHolder(x, y, z);
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
        return background != null && !background.isBlank();
    }

    @Override
    public Component<ChunkStore> clone() {
        return new VaryonPortalConfig(
                this.command, this.type, this.background, this.scale, this.centerOffsetY, this.yaw, this.asServer);
    }
}
