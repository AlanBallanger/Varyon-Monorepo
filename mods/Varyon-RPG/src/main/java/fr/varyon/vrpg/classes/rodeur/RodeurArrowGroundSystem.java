package fr.varyon.vrpg.classes.rodeur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.component.Projectile;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RodeurArrowGroundSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
        Projectile.getComponentType(),
        StandardPhysicsProvider.getComponentType(),
        TransformComponent.getComponentType()
    );

    private final RodeurState rodeurState;
    private final ConcurrentHashMap<Ref<EntityStore>, UUID> trackedProjectiles = new ConcurrentHashMap<>();

    public RodeurArrowGroundSystem(@Nonnull RodeurState rodeurState) {
        this.rodeurState = rodeurState;
    }

    public void trackProjectile(@Nonnull Ref<EntityStore> projectileRef, @Nonnull UUID creatorUuid) {
        trackedProjectiles.put(projectileRef, creatorUuid);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public boolean isExplicitQuery() {
        return true;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (trackedProjectiles.isEmpty()) return;

        try {
            Ref<EntityStore> projRef = chunk.getReferenceTo(index);
            UUID creatorUuid = trackedProjectiles.get(projRef);
            if (creatorUuid == null) return;

            StandardPhysicsProvider physics =
                chunk.getComponent(index, StandardPhysicsProvider.getComponentType());
            if (physics == null || physics.getState() == StandardPhysicsProvider.STATE.ACTIVE) return;

            trackedProjectiles.remove(projRef);

            try {
                commandBuffer.removeEntity(projRef, RemoveReason.REMOVE);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }
}
