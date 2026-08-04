package fr.varyon.vrpg.classes.rodeur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
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

public final class RodeurSpeedSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 2;

    private final ClassManager classManager;
    private final RodeurState rodeurState;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Float>   appliedBoost = new ConcurrentHashMap<>();

    public RodeurSpeedSystem(@Nonnull ClassManager classManager, @Nonnull RodeurState rodeurState) {
        this.classManager = classManager;
        this.rodeurState = rodeurState;
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
                    || acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.RODEUR) {
                clearBoost(uuid, chunk, index, store);
                return;
            }

            float boost = rodeurState.getSpeedBonus(uuid);
            if (boost > 0f) {
                Float current = appliedBoost.get(uuid);
                if (current == null || Math.abs(current - boost) > 0.001f) {
                    appliedBoost.put(uuid, boost);
                    applyBoost(boost, chunk, index, store, playerRef);
                }
            } else {
                clearBoost(uuid, chunk, index, store);
            }
        } catch (Exception ignored) {}
    }

    private void applyBoost(float boost, ArchetypeChunk<EntityStore> chunk, int index,
                            Store<EntityStore> store, PlayerRef playerRef) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            boolean debug = fr.varyon.vrpg.config.VrpgConfig.isDebugCombat();
            if (mm == null) {
                if (debug) com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass()
                    .atInfo().log("[RodeurSpeed] MovementManager null, skip");
                return;
            }
            float target = mm.getDefaultSettings().baseSpeed * (1f + boost);
            if (Math.abs(mm.getSettings().baseSpeed - target) > 0.01f) {
                mm.getSettings().baseSpeed = target;
                mm.update(playerRef.getPacketHandler());
                if (debug) com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass()
                    .atInfo().log(String.format("[RodeurSpeed] applied boost=%.2f base=%.2f target=%.2f",
                        boost, mm.getDefaultSettings().baseSpeed, target));
            }
        } catch (Exception ignored) {}
    }

    private void clearBoost(UUID uuid, ArchetypeChunk<EntityStore> chunk, int index,
                            Store<EntityStore> store) {
        if (appliedBoost.remove(uuid) == null) return;
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            if (mm != null) mm.resetDefaultsAndUpdate(ref, store);
        } catch (Exception ignored) {}
    }

    public void removePlayer(@Nonnull UUID uuid) {
        tickCounters.remove(uuid);
        appliedBoost.remove(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
