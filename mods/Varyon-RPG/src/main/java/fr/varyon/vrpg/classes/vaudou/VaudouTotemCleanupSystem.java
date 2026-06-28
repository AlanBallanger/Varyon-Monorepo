package fr.varyon.vrpg.classes.vaudou;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class VaudouTotemCleanupSystem extends EntityTickingSystem<EntityStore> {

    private static final int TICK_INTERVAL = 20;
    private int tickCounter = 0;

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void tick(float deltaTime,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;
        try {
            var world = store.getExternalData().getWorld();
            if (world != null) {
                VaudouTotemHelper.cleanupExpired(world);
            }
        } catch (Exception ignored) {}
    }
}
