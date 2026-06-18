package fr.varyon.vrpg.classes.arbaletrier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArbaietrierImmobilityTickSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 3;
    private static final double MOVE_THRESHOLD_SQ = 0.04;

    private final ClassManager classManager;
    private final ArbaietrierState arbaState;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();

    public ArbaietrierImmobilityTickSystem(@Nonnull ClassManager classManager,
                                           @Nonnull ArbaietrierState arbaState) {
        this.classManager = classManager;
        this.arbaState = arbaState;
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = chunk.getComponent(index, playerRefType);
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();

        int tc = tickCounters.merge(uuid, 1, Integer::sum);
        if (tc % CHECK_INTERVAL != 0) return;

        try {
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.TIREUR
                    || acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.ARBALETRIER) {
                arbaState.markMoving(uuid);
                return;
            }

            boolean hasEmbusque = acc.getTalentRank(PlayerClass.TIREUR, ArbaietrierPassifs.TIREUR_EMBUSQUE_NODE) > 0;
            if (!hasEmbusque) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());

            if (vel != null) {
                org.joml.Vector3d v = vel.getVelocity();
                double hSpeedSq = v != null ? (v.x * v.x + v.z * v.z) : 0;
                if (hSpeedSq > MOVE_THRESHOLD_SQ) {
                    arbaState.markMoving(uuid);
                } else {
                    arbaState.ensureImmobileTimer(uuid);
                }
            }
        } catch (Exception ignored) {}
    }

    public void removePlayer(@Nonnull UUID uuid) {
        tickCounters.remove(uuid);
        arbaState.markMoving(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
