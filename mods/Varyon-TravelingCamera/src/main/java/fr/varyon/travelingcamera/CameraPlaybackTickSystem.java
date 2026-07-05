package fr.varyon.travelingcamera;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

public final class CameraPlaybackTickSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void tick(float dt,
                     int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (TravelingCameraManager.isPlaying(uuid)) {
            TravelingCameraManager.tickPlayback(uuid, playerRef, dt);
        }
        if (TravelingCameraManager.isVisualizing(uuid)) {
            World world = store.getExternalData().getWorld();
            TravelingCameraManager.tickVisualization(uuid, playerRef, world, dt);
        }
    }
}
