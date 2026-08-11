package fr.varyon.vrpg.profession.forestier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.protocol.ComponentUpdateType;
import com.hypixel.hytale.protocol.DynamicLightUpdate;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yeux de Hibou (nœud 8, Forestier) — vision nocturne passive.
 *
 * Rank → rayon de lumière (byte) :
 *   1 → 5   (Vision faible)
 *   2 → 10  (Vision modérée)
 *   3 → 15  (Vision renforcée)
 *   4 → 20  (Vision avancée)
 *   5 → 25  (Vision parfaite)
 */
public final class YeuxHibouTickSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 20;
    private static final String NODE_ID = "8";
    private static final byte[] RADIUS_PER_RANK = {0, 5, 10, 15, 20, 25};

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final ComponentType<EntityStore, EntityTrackerSystems.EntityViewer> viewerType =
            EntityTrackerSystems.EntityViewer.getComponentType();

    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> appliedRanks = new ConcurrentHashMap<>();

    public YeuxHibouTickSystem(ProfessionManager professionManager) {
        this.professionManager = professionManager;
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

        try {
            EntityTrackerSystems.EntityViewer viewer = chunk.getComponent(index, viewerType);
            if (viewer == null) {
                appliedRanks.remove(uuid);
                return;
            }

            int tc = tickCounters.merge(uuid, 1, Integer::sum);
            boolean refresh = tc % CHECK_INTERVAL == 0;
            Integer lastApplied = appliedRanks.get(uuid);

            if (refresh || lastApplied == null) {
                PlayerAccount acc = professionManager.getAccount(uuid);
                int rank = (acc != null && acc.isActive(Profession.FORESTIER))
                    ? acc.getTalentRank(Profession.FORESTIER, NODE_ID) : 0;

                if (rank == 0) {
                    if (lastApplied != null) clearLight(uuid, chunk, index, viewer);
                    return;
                }
                lastApplied = rank;
                appliedRanks.put(uuid, rank);
            }

            // SendPackets vide viewer.updates a chaque tick : la lumiere doit etre
            // re-queue en permanence, sinon elle ne dure qu'une frame.
            applyLight(lastApplied, chunk, index, viewer);
        } catch (Exception ignored) {}
    }

    private void applyLight(int rank,
                             ArchetypeChunk<EntityStore> chunk, int index,
                             EntityTrackerSystems.EntityViewer viewer) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            byte radius = RADIUS_PER_RANK[rank];
            DynamicLightUpdate update = new DynamicLightUpdate(new ColorLight(radius, (byte) 1, (byte) 1, (byte) 1));
            viewer.queueUpdate(ref, update);
        } catch (Exception ignored) {}
    }

    private void clearLight(UUID uuid,
                             ArchetypeChunk<EntityStore> chunk, int index,
                             EntityTrackerSystems.EntityViewer viewer) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            viewer.queueRemove(ref, ComponentUpdateType.DynamicLight);
            appliedRanks.remove(uuid);
        } catch (Exception ignored) {}
    }

    public void removePlayer(UUID uuid) {
        tickCounters.remove(uuid);
        appliedRanks.remove(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(playerRefType, viewerType);
    }
}
