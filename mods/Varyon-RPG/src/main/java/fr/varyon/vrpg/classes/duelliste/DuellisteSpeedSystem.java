package fr.varyon.vrpg.classes.duelliste;

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

public final class DuellisteSpeedSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 10;

    private final ClassManager classManager;
    private final DuellisteState state;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Float> appliedBoost = new ConcurrentHashMap<>();

    public DuellisteSpeedSystem(@Nonnull ClassManager classManager, @Nonnull DuellisteState state) {
        this.classManager = classManager;
        this.state = state;
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
            if (acc.getActiveClass() != PlayerClass.GUERRIER
                    || acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.DUELLISTE) {
                clearBoost(uuid, chunk, index, store);
                return;
            }

            int rank = acc.getTalentRank(PlayerClass.GUERRIER, AssautBretteurSkill.TALENT_NODE_ID);
            boolean active = rank > 0 && state.isAssautBretteurActive(uuid);

            if (active) {
                float boost = AssautBretteurSkill.speedBonusForRank(rank);
                Float current = appliedBoost.get(uuid);
                if (current == null || Math.abs(current - boost) > 0.001f) {
                    appliedBoost.put(uuid, boost);
                    applyBoost(uuid, boost, chunk, index, store, playerRef);
                }
            } else {
                if (state.consumeAssautBretteurIfExpired(uuid) || appliedBoost.containsKey(uuid)) {
                    clearBoost(uuid, chunk, index, store);
                }
            }
        } catch (Exception ignored) {}
    }

    private void applyBoost(UUID uuid, float boost,
                            ArchetypeChunk<EntityStore> chunk, int index,
                            Store<EntityStore> store, PlayerRef playerRef) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            if (mm == null) return;
            float target = mm.getDefaultSettings().baseSpeed * (1.0f + boost);
            if (Math.abs(mm.getSettings().baseSpeed - target) > 0.01f) {
                mm.getSettings().baseSpeed = target;
                mm.update(playerRef.getPacketHandler());
            }
        } catch (Exception ignored) {}
    }

    private void clearBoost(UUID uuid,
                            ArchetypeChunk<EntityStore> chunk, int index,
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
