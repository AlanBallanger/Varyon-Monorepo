package fr.varyon.vrpg.profession.forestier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseForestierStaminaSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 20;
    private static final String MODIFIER_KEY = "maitrise_forestier_stamina";
    private static final float STAMINA_MULT = 1.15f;

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Set<UUID> applied = ConcurrentHashMap.newKeySet();

    public MaitriseForestierStaminaSystem(ProfessionManager professionManager) {
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
            boolean shouldApply = acc != null && acc.isActive(Profession.FORESTIER)
                && acc.getTalentRank(Profession.FORESTIER, "bonus_2") > 0;

            if (!shouldApply) {
                clearModifier(uuid, chunk, index, store);
                return;
            }

            if (applied.contains(uuid)) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap == null) return;

            statMap.putModifier(
                DefaultEntityStatTypes.getStamina(),
                MODIFIER_KEY,
                new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.MULTIPLICATIVE, STAMINA_MULT)
            );
            applied.add(uuid);
        } catch (Exception ignored) {}
    }

    private void clearModifier(UUID uuid,
                                ArchetypeChunk<EntityStore> chunk, int index,
                                Store<EntityStore> store) {
        if (!applied.remove(uuid)) return;
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) statMap.removeModifier(DefaultEntityStatTypes.getStamina(), MODIFIER_KEY);
        } catch (Exception ignored) {}
    }

    public void removePlayer(UUID uuid) {
        tickCounters.remove(uuid);
        applied.remove(uuid);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
