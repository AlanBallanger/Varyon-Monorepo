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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RodeurArrowGroundSystem extends EntityTickingSystem<EntityStore> {

    public interface OnImpact {
        void onImpact(@Nonnull Ref<EntityStore> attackerRef,
                     @Nonnull org.joml.Vector3d impactPos,
                     @Nonnull Store<EntityStore> store);
    }

    private static final Query<EntityStore> QUERY = Query.and(
        Projectile.getComponentType(),
        StandardPhysicsProvider.getComponentType(),
        TransformComponent.getComponentType()
    );

    private final RodeurState rodeurState;
    private final ConcurrentHashMap<Ref<EntityStore>, UUID> trackedProjectiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Ref<EntityStore>, OnImpact> impactCallbacks = new ConcurrentHashMap<>();

    public RodeurArrowGroundSystem(@Nonnull RodeurState rodeurState) {
        this.rodeurState = rodeurState;
    }

    public void trackProjectile(@Nonnull Ref<EntityStore> projectileRef, @Nonnull UUID creatorUuid) {
        trackedProjectiles.put(projectileRef, creatorUuid);
    }

    public void trackProjectile(@Nonnull Ref<EntityStore> projectileRef, @Nonnull UUID creatorUuid,
                                @Nullable OnImpact onImpact) {
        trackedProjectiles.put(projectileRef, creatorUuid);
        if (onImpact != null) impactCallbacks.put(projectileRef, onImpact);
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
            OnImpact onImpact = impactCallbacks.remove(projRef);

            if (onImpact != null) {
                try {
                    TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
                    if (tc != null) {
                        final org.joml.Vector3d fImpactPos = new org.joml.Vector3d(tc.getPosition());
                        final UUID fCreatorUuid = creatorUuid;
                        com.hypixel.hytale.server.core.universe.world.World world =
                            commandBuffer.getExternalData().getWorld();
                        if (world != null) {
                            world.execute(() -> {
                                try {
                                    PlayerRef pr = Universe.get().getPlayer(fCreatorUuid);
                                    if (pr == null) return;
                                    Ref<EntityStore> liveAttackerRef = pr.getReference();
                                    if (liveAttackerRef == null || !liveAttackerRef.isValid()) return;
                                    Store<EntityStore> liveStore = liveAttackerRef.getStore();
                                    if (liveStore == null) return;
                                    onImpact.onImpact(liveAttackerRef, fImpactPos, liveStore);
                                } catch (Exception ignored2) {}
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }

            try {
                commandBuffer.removeEntity(projRef, RemoveReason.REMOVE);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }
}
