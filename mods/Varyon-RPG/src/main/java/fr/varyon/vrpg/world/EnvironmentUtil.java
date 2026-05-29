package fr.varyon.vrpg.world;

import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EnvironmentUtil {

    private EnvironmentUtil() {}

    @Nullable
    public static String getEnvironmentId(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull TransformComponent transform,
                                          @Nonnull ComponentAccessor<EntityStore> accessor) {
        int index = resolveEnvironmentIndex(ref, transform, accessor);
        if (index <= 0) return null;
        try {
            Environment env = Environment.getAssetMap().getAsset(index);
            return env != null ? env.getId() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isForest(@Nullable String environmentId) {
        return environmentIdMatches(environmentId, "forest");
    }

    public static boolean isDesert(@Nullable String environmentId) {
        return environmentIdMatches(environmentId, "desert");
    }

    public static boolean isForest(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull TransformComponent transform,
                                   @Nonnull ComponentAccessor<EntityStore> accessor) {
        return isForest(getEnvironmentId(ref, transform, accessor));
    }

    public static boolean isDesert(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull TransformComponent transform,
                                   @Nonnull ComponentAccessor<EntityStore> accessor) {
        return isDesert(getEnvironmentId(ref, transform, accessor));
    }

    public static boolean isZone1(@Nullable String environmentId) {
        if (environmentId == null) return false;
        String id = environmentId.toLowerCase();
        return id.contains("zone1") || id.contains("zone_1")
            || id.contains("forest") || id.contains("meadow") || id.contains("grassland")
            || id.contains("plains") || id.contains("river") || id.contains("swamp");
    }

    public static boolean isZone2(@Nullable String environmentId) {
        if (environmentId == null) return false;
        String id = environmentId.toLowerCase();
        return id.contains("zone2") || id.contains("zone_2")
            || id.contains("highlands") || id.contains("highland")
            || id.contains("tundra") || id.contains("frozen") || id.contains("taiga");
    }

    public static boolean isDay(@Nonnull Store<EntityStore> store) {
        try {
            WorldTimeResource timeRes = store.getResource(WorldTimeResource.getResourceType());
            return timeRes != null && timeRes.getSunlightFactor() > 0.2;
        } catch (Exception ignored) {
            return true;
        }
    }

    public static boolean isNight(@Nonnull Store<EntityStore> store) {
        return !isDay(store);
    }

    public static boolean isKnownNonForest(@Nullable String environmentId) {
        return environmentId != null && !isForest(environmentId);
    }

    public static boolean isKnownNonDesert(@Nullable String environmentId) {
        return environmentId != null && !isDesert(environmentId);
    }

    private static int resolveEnvironmentIndex(@Nonnull Ref<EntityStore> ref,
                                               @Nonnull TransformComponent transform,
                                               @Nonnull ComponentAccessor<EntityStore> accessor) {
        try {
            Ref<ChunkStore> chunkRef = transform.getChunkRef();
            if (chunkRef != null && chunkRef.isValid()) {
                World world = accessor.getExternalData().getWorld();
                if (world != null) {
                    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                    BlockChunk blockChunk = chunkStore.getComponent(chunkRef, BlockChunk.getComponentType());
                    if (blockChunk != null) {
                        var pos = transform.getPosition();
                        int x = MathUtil.floor(pos.x);
                        int y = MathUtil.clamp(MathUtil.floor(pos.y), 0, 319);
                        int z = MathUtil.floor(pos.z);
                        int id = blockChunk.getEnvironment(x, y, z);
                        if (id > 0) return id;
                    }
                }
            }

            WeatherTracker tracker = accessor.getComponent(ref, WeatherTracker.getComponentType());
            if (tracker != null) {
                int cached = tracker.getEnvironmentId();
                if (cached > 0) return cached;
                tracker.updateEnvironment(transform, accessor);
                return tracker.getEnvironmentId();
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static boolean environmentIdMatches(@Nullable String environmentId, @Nonnull String token) {
        return environmentId != null && environmentId.toLowerCase().contains(token.toLowerCase());
    }
}
