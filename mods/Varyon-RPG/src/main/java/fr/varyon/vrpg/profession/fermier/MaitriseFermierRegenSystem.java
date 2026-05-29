package fr.varyon.vrpg.profession.fermier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseFermierRegenSystem extends EntityTickingSystem<EntityStore> {

    private static final int REGEN_INTERVAL = 100;
    private static final float REGEN_AMOUNT = 1.0f;

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatMap.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();

    public MaitriseFermierRegenSystem(ProfessionManager professionManager) {
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
        if (tc % REGEN_INTERVAL != 0) return;

        try {
            PlayerAccount acc = professionManager.getAccount(uuid);
            if (acc == null || !acc.isActive(Profession.FERMIER)) return;
            if (acc.getTalentRank(Profession.FERMIER, "bonus_2") <= 0) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap statMap = store.getComponent(ref, statMapType);
            if (statMap == null) return;

            int healthIdx = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthStat = statMap.get(healthIdx);
            if (healthStat == null) return;

            if (healthStat.get() < healthStat.getMax()) {
                statMap.addStatValue(healthIdx, REGEN_AMOUNT);
            }
        } catch (Exception ignored) {}
    }

    public void removePlayer(UUID uuid) {
        tickCounters.remove(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(playerRefType, statMapType);
    }
}
