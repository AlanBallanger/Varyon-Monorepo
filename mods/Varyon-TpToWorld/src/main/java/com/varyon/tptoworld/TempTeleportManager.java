package com.varyon.tptoworld;

import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TempTeleportManager {

    public record SavedLocation(World world, Vector3d position, Rotation3f rotation,
                                @Nullable UUID initiatorUuid, @Nullable String initiatorName) {
    }

    private static final Map<UUID, SavedLocation> SAVED_LOCATIONS = new ConcurrentHashMap<>();

    private TempTeleportManager() {
    }

    public static void save(@Nonnull UUID playerUuid, @Nonnull SavedLocation location) {
        SAVED_LOCATIONS.put(playerUuid, location);
    }

    public static SavedLocation get(@Nonnull UUID playerUuid) {
        return SAVED_LOCATIONS.get(playerUuid);
    }

    public static SavedLocation remove(@Nonnull UUID playerUuid) {
        return SAVED_LOCATIONS.remove(playerUuid);
    }
}
