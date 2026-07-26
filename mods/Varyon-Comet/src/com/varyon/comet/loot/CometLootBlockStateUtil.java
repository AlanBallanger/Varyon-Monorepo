package com.varyon.comet.loot;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class CometLootBlockStateUtil {

    private static final String ITEM_CONTAINER_STATE =
            "com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState";

    private CometLootBlockStateUtil() {
    }

    public static Object getState(World world, int x, int y, int z, boolean load) {
        if (world == null) {
            return null;
        }
        try {
            Method m = world.getClass().getMethod("getState", int.class, int.class, int.class, boolean.class);
            return m.invoke(world, x, y, z, load);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isItemContainerState(Object o) {
        return o != null && ITEM_CONTAINER_STATE.equals(o.getClass().getName());
    }

    public static Object newItemContainerState(ClassLoader serverLoader) {
        try {
            Class<?> c = Class.forName(ITEM_CONTAINER_STATE, true, serverLoader);
            return c.getConstructor().newInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean initializeContainer(Object state, BlockType blockType) {
        if (state == null || blockType == null) {
            return false;
        }
        try {
            Method m = state.getClass().getMethod("initialize", BlockType.class);
            Object r = m.invoke(state, blockType);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setPositionOnChunk(Object state, WorldChunk chunk, Vector3i pos) {
        if (state == null || chunk == null || pos == null) {
            return;
        }
        try {
            Method m = state.getClass().getMethod("setPosition", WorldChunk.class, Vector3i.class);
            m.invoke(state, chunk, pos);
        } catch (Throwable ignored) {
        }
    }

    public static void setChunkState(WorldChunk chunk, int localX, int y, int localZ, Object state, boolean mark) {
        if (chunk == null || state == null) {
            return;
        }
        try {
            for (Method m : chunk.getClass().getMethods()) {
                if (!"setState".equals(m.getName()) || m.getParameterCount() != 5) {
                    continue;
                }
                Class<?>[] p = m.getParameterTypes();
                if (p[0] == int.class && p[1] == int.class && p[2] == int.class && p[4] == boolean.class
                        && p[3].isAssignableFrom(state.getClass())) {
                    m.invoke(chunk, localX, y, localZ, state, mark);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setCustom(Object state, boolean v) {
        invokeVoid(state, "setCustom", boolean.class, v);
    }

    public static void setAllowViewing(Object state, boolean v) {
        invokeVoid(state, "setAllowViewing", boolean.class, v);
    }

    public static void setDroplist(Object state, String droplist) {
        if (state == null) {
            return;
        }
        try {
            Method m = state.getClass().getMethod("setDroplist", String.class);
            m.invoke(state, droplist);
        } catch (Throwable ignored) {
        }
    }

    public static void setItemContainer(Object state, Object container) {
        if (state == null) {
            return;
        }
        try {
            for (Method m : state.getClass().getMethods()) {
                if ("setItemContainer".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.invoke(state, container);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean isAllowViewing(Object state) {
        if (state == null) {
            return false;
        }
        try {
            Method m = state.getClass().getMethod("isAllowViewing");
            Object r = m.invoke(state);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean canOpen(Object state, Ref<EntityStore> playerRef, CommandBuffer<EntityStore> commandBuffer) {
        if (state == null || playerRef == null || commandBuffer == null) {
            return false;
        }
        try {
            for (Method m : state.getClass().getMethods()) {
                if (!"canOpen".equals(m.getName()) || m.getParameterCount() != 2) {
                    continue;
                }
                Object r = m.invoke(state, playerRef, commandBuffer);
                return r instanceof Boolean && (Boolean) r;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Map<UUID, com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow> getWindows(
            Object state) {
        if (state == null) {
            return null;
        }
        try {
            Method m = state.getClass().getMethod("getWindows");
            Object r = m.invoke(state);
            if (r instanceof Map) {
                return (Map<UUID, com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow>) r;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Object getItemContainer(Object state) {
        if (state == null) {
            return null;
        }
        try {
            Method m = state.getClass().getMethod("getItemContainer");
            return m.invoke(state);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getDroplist(Object state) {
        if (state == null) {
            return null;
        }
        try {
            Method m = state.getClass().getMethod("getDroplist");
            Object r = m.invoke(state);
            return r instanceof String ? (String) r : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void onOpen(Object state, Ref<EntityStore> playerRef, World world,
            com.hypixel.hytale.component.Store<EntityStore> store) {
        if (state == null || playerRef == null || world == null || store == null) {
            return;
        }
        try {
            for (Method m : state.getClass().getMethods()) {
                if (!"onOpen".equals(m.getName()) || m.getParameterCount() != 3) {
                    continue;
                }
                m.invoke(state, playerRef, world, store);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    public static void clearWindowsIfContainer(Object state) {
        if (!isItemContainerState(state)) {
            return;
        }
        Map<?, ?> windows = getWindows(state);
        if (windows != null && !windows.isEmpty()) {
            windows.clear();
        }
    }

    public static void clearContainerContents(Object state) {
        if (!isItemContainerState(state)) {
            return;
        }
        try {
            clearWindowsIfContainer(state);
            Object ic = getItemContainer(state);
            if (ic != null) {
                Method clear = ic.getClass().getMethod("clear");
                clear.invoke(ic);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Strip {@code ItemContainerBlock} from the live block entity before break/replace.
     * Leaving it without {@code BlockStateInfo} crashes {@code ItemContainerBlockSpatialSystem}.
     */
    @SuppressWarnings("unchecked")
    public static void stripLiveItemContainerBlock(World world, int x, int y, int z) {
        if (world == null) {
            return;
        }
        try {
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                return;
            }
            int localX = x & 31;
            int localZ = z & 31;
            Ref<ChunkStore> entityRef = chunk.getBlockComponentEntity(localX, y, localZ);
            if (entityRef == null) {
                return;
            }
            Store<ChunkStore> store = entityRef.getStore();
            if (store == null) {
                return;
            }
            Class<?> cls = Class.forName(
                    "com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock");
            ComponentType<ChunkStore, ?> type =
                    (ComponentType<ChunkStore, ?>) cls.getMethod("getComponentType").invoke(null);
            if (type != null) {
                store.tryRemoveComponent(entityRef, type);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Strip live ItemContainerBlock then break the block. */
    public static boolean safeBreakBlock(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        stripLiveItemContainerBlock(world, x, y, z);
        try {
            return world.breakBlock(x, y, z, 0);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void invokeVoid(Object target, String name, Class<?> argType, boolean v) {
        if (target == null) {
            return;
        }
        try {
            Method m = target.getClass().getMethod(name, argType);
            m.invoke(target, v);
        } catch (Throwable ignored) {
        }
    }
}
