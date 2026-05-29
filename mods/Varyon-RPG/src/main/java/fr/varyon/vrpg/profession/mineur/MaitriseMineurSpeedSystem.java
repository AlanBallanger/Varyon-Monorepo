package fr.varyon.vrpg.profession.mineur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseMineurSpeedSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 20;
    private static final float SPEED_BOOST = 0.30f;
    private static final long IDLE_THRESHOLD_MS = 3000L;

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Map<UUID, double[]> lastPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> idleSince = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> boosted = new ConcurrentHashMap<>();

    public MaitriseMineurSpeedSystem(ProfessionManager professionManager) {
        this.professionManager = professionManager;
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
            PlayerAccount acc = professionManager.getAccount(uuid);
            if (acc == null || !acc.isActive(Profession.MINEUR)
                    || acc.getTalentRank(Profession.MINEUR, "bonus_2") <= 0) {
                clearBoost(uuid, chunk, index, store);
                return;
            }

            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return;

            org.joml.Vector3d pos = transform.getPosition();
            double[] last = lastPositions.get(uuid);
            long now = System.currentTimeMillis();

            if (last != null) {
                double dx = pos.x - last[0];
                double dy = pos.y - last[1];
                double dz = pos.z - last[2];
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 0.0025) {
                    idleSince.remove(uuid);
                    clearBoost(uuid, chunk, index, store);
                    lastPositions.put(uuid, new double[]{pos.x, pos.y, pos.z});
                    return;
                }
            }

            lastPositions.put(uuid, new double[]{pos.x, pos.y, pos.z});
            idleSince.putIfAbsent(uuid, now);
            long since = idleSince.get(uuid);

            if (now - since >= IDLE_THRESHOLD_MS) {
                applyBoost(uuid, ref, store, playerRef);
            }
        } catch (Exception ignored) {}
    }

    private void applyBoost(UUID uuid, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        if (Boolean.TRUE.equals(boosted.get(uuid))) return;
        try {
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            if (mm == null) return;
            mm.getSettings().baseSpeed = mm.getDefaultSettings().baseSpeed * (1.0f + SPEED_BOOST);
            mm.update(playerRef.getPacketHandler());
            boosted.put(uuid, true);
        } catch (Exception ignored) {}
    }

    private void clearBoost(UUID uuid,
                             ArchetypeChunk<EntityStore> chunk, int index,
                             Store<EntityStore> store) {
        if (!Boolean.TRUE.equals(boosted.remove(uuid))) return;
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
            if (mm != null) mm.resetDefaultsAndUpdate(ref, store);
        } catch (Exception ignored) {}
    }

    public void removePlayer(UUID uuid) {
        tickCounters.remove(uuid);
        lastPositions.remove(uuid);
        idleSince.remove(uuid);
        boosted.remove(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
