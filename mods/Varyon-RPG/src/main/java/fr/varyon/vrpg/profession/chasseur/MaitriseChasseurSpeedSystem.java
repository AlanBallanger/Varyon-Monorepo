package fr.varyon.vrpg.profession.chasseur;

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
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseChasseurSpeedSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 20;
    private static final float SPEED_BOOST = 0.30f;
    private static final long BOOST_DURATION_MS = 10_000L;

    private final ProfessionManager professionManager;
    private final MaitriseChasseurTracker maitriseTracker;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Long> boostExpiry = new ConcurrentHashMap<>();

    public MaitriseChasseurSpeedSystem(@Nonnull ProfessionManager professionManager,
                                       @Nonnull MaitriseChasseurTracker maitriseTracker) {
        this.professionManager = professionManager;
        this.maitriseTracker = maitriseTracker;
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = chunk.getComponent(index, playerRefType);
        if (playerRef == null) return;
        if (fr.varyon.vrpg.rpg.CreativeGate.isCreative(playerRef)) return;

        UUID uuid = playerRef.getUuid();

        int tc = tickCounters.merge(uuid, 1, Integer::sum);
        if (tc % CHECK_INTERVAL != 0) return;

        try {
            PlayerAccount acc = professionManager.getAccount(uuid);
            if (acc == null || !acc.isActive(Profession.CHASSEUR)
                    || acc.getTalentRank(Profession.CHASSEUR, "bonus_1") <= 0) {
                clearBoost(uuid, chunk, index, store);
                return;
            }

            long msSinceKill = maitriseTracker.millisSinceLastKill(uuid);
            long now = System.currentTimeMillis();

            if (msSinceKill < BOOST_DURATION_MS) {
                long newExpiry = now - msSinceKill + BOOST_DURATION_MS;
                long current = boostExpiry.getOrDefault(uuid, 0L);
                if (newExpiry > current) {
                    boostExpiry.put(uuid, newExpiry);
                    applyBoost(uuid, chunk, index, store, playerRef);
                }
            } else {
                clearBoost(uuid, chunk, index, store);
            }
        } catch (Exception ignored) {}
    }

    private void applyBoost(UUID uuid,
                             ArchetypeChunk<EntityStore> chunk, int index,
                             Store<EntityStore> store, PlayerRef playerRef) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            if (mm == null) return;
            float boosted = mm.getDefaultSettings().baseSpeed * (1.0f + SPEED_BOOST);
            if (Math.abs(mm.getSettings().baseSpeed - boosted) > 0.01f) {
                mm.getSettings().baseSpeed = boosted;
                mm.update(playerRef.getPacketHandler());
            }
        } catch (Exception ignored) {}
    }

    private void clearBoost(UUID uuid,
                             ArchetypeChunk<EntityStore> chunk, int index,
                             Store<EntityStore> store) {
        if (boostExpiry.remove(uuid) == null) return;
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            if (mm != null) mm.resetDefaultsAndUpdate(ref, store);
        } catch (Exception ignored) {}
    }

    public void removePlayer(UUID uuid) {
        tickCounters.remove(uuid);
        boostExpiry.remove(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
