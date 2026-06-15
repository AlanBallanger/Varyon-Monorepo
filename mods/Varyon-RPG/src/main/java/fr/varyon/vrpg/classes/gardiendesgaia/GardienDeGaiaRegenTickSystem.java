package fr.varyon.vrpg.classes.gardiendesgaia;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.ability.ClassSkillMana;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GardienDeGaiaRegenTickSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL    = 2;
    private static final int HEAL_INTERVAL     = 20; // 1 seconde

    private final ClassManager classManager;
    private final GardienDeGaiaState state;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();

    public GardienDeGaiaRegenTickSystem(@Nonnull ClassManager classManager,
                                         @Nonnull GardienDeGaiaState state) {
        this.classManager = classManager;
        this.state        = state;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
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

        ClassAccount acc = classManager.getAccount(uuid);
        if (acc == null) return;
        if (acc.getActiveClass() != PlayerClass.MAGE) return;
        if (acc.getActiveSpec(PlayerClass.MAGE) != PlayerSpecialization.GARDIEN_DE_GAIA) return;

        // --- Souffle de la Nature : regen HP + endurance aux alliés proches ---
        int souffleRank = acc.getTalentRank(PlayerClass.MAGE, GardienDeGaiaPassifs.SOUFFLE_NODE);
        if (souffleRank > 0) {
            int graceRankSouffle = acc.getTalentRank(PlayerClass.MAGE, GardienDeGaiaPassifs.GRACE_NODE);
            float hpRegen  = GardienDeGaiaPassifs.souffleHpRegenForRank(souffleRank);
            float staRegen = GardienDeGaiaPassifs.souffleStaRegenForRank(souffleRank);
            applyRegenToNearbyAllies(uuid, playerRef, chunk, index, store, hpRegen, staRegen, graceRankSouffle);
        }

        // --- Écorce Protectrice : soin par tick sur la cible (1x/seconde) ---
        float ecorceHeal = state.getEcorceHealPerSec(uuid);
        if (ecorceHeal > 0f && tc % HEAL_INTERVAL == 0) {
            int graceRank = acc.getTalentRank(PlayerClass.MAGE, GardienDeGaiaPassifs.GRACE_NODE);
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> targetRef =
                state.getEcorceTargetRef(uuid);
            UUID ecorceTarget = state.getEcorceTarget(uuid);
            if (targetRef != null && targetRef.isValid()) {
                applyHealToEntityRef(targetRef, ecorceHeal, graceRank);
            } else if (ecorceTarget == null || ecorceTarget.equals(uuid)) {
                applyHealToSelf(chunk, index, store, ecorceHeal, graceRank);
            } else {
                applyHealToTarget(ecorceTarget, ecorceHeal, graceRank);
            }
        }

        // --- Cycle de Vie : mana restauré après un soin ---
        if (state.consumeCycleHeal(uuid)) {
            int cycleRank = acc.getTalentRank(PlayerClass.MAGE, GardienDeGaiaPassifs.CYCLE_NODE);
            if (cycleRank > 0) {
                try {
                    ClassSkillMana.restore(playerRef, GardienDeGaiaPassifs.cycleManaForRank(cycleRank));
                } catch (Exception ignored) {}
            }
        }
    }

    private void applyRegenToNearbyAllies(UUID uuid, PlayerRef casterRef,
                                          ArchetypeChunk<EntityStore> chunk, int index,
                                          Store<EntityStore> store,
                                          float hpRegen, float staRegen, int graceRank) {
        try {
            int hIdx;
            int sIdx;
            try { hIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { return; }
            try { sIdx = DefaultEntityStatTypes.getStamina(); } catch (Exception e) { sIdx = -1; }
            final int staminaIdx = sIdx;

            // Soin sur le caster lui-même
            applyHealToSelf(chunk, index, store, hpRegen, graceRank);
            if (staminaIdx >= 0) {
                try {
                    EntityStatMap selfSm = store.getComponent(chunk.getReferenceTo(index), EntityStatMap.getComponentType());
                    if (selfSm != null) {
                        var sta = selfSm.get(staminaIdx);
                        if (sta != null) selfSm.setStatValue(staminaIdx, Math.min(sta.getMax(), sta.get() + staRegen));
                    }
                } catch (Exception ignored) {}
            }

            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent casterTc =
                store.getComponent(chunk.getReferenceTo(index),
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (casterTc == null) return;
            org.joml.Vector3d pos = casterTc.getPosition();

            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                .selectNearbyEntities(store, pos, GardienDeGaiaPassifs.SOUFFLE_RADIUS, ref -> {
                    try {
                        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                        if (pRef == null || pRef.getUuid().equals(uuid)) return;
                        EntityStatMap sm = store.getComponent(ref, EntityStatMap.getComponentType());
                        if (sm == null) return;
                        var hp = sm.get(hIdx);
                        if (hp != null) {
                            float regen = withGrace(hpRegen, (float) hp.get(), (float) hp.getMax(), graceRank);
                            sm.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + regen));
                        }
                        if (staminaIdx >= 0) {
                            var sta = sm.get(staminaIdx);
                            if (sta != null) sm.setStatValue(staminaIdx, Math.min(sta.getMax(), sta.get() + staRegen));
                        }
                    } catch (Exception ignored) {}
                }, ref -> true);
        } catch (Exception ignored) {}
    }

    private static float withGrace(float amount, float currentHp, float maxHp, int graceRank) {
        if (graceRank <= 0 || maxHp <= 0f) return amount;
        if (currentHp / maxHp < GardienDeGaiaPassifs.GRACE_HP_SEUIL) {
            amount *= 1f + GardienDeGaiaPassifs.graceBonusForRank(graceRank);
        }
        return amount;
    }

    private void applyHealToSelf(ArchetypeChunk<EntityStore> chunk, int index,
                                  Store<EntityStore> store, float amount, int graceRank) {
        try {
            int hIdx = DefaultEntityStatTypes.getHealth();
            EntityStatMap sm = store.getComponent(chunk.getReferenceTo(index), EntityStatMap.getComponentType());
            if (sm != null) {
                var hp = sm.get(hIdx);
                if (hp != null) {
                    float final_ = withGrace(amount, (float) hp.get(), (float) hp.getMax(), graceRank);
                    sm.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + final_));
                }
            }
        } catch (Exception ignored) {}
    }

    private void applyHealToTarget(@Nonnull UUID targetUuid, float amount, int graceRank) {
        try {
            int hIdx = DefaultEntityStatTypes.getHealth();
            com.hypixel.hytale.server.core.universe.PlayerRef targetRef =
                com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(targetUuid);
            if (targetRef != null) {
                EntityStatMap sm = targetRef.getComponent(EntityStatMap.getComponentType());
                if (sm != null) {
                    var hp = sm.get(hIdx);
                    if (hp != null) {
                        float final_ = withGrace(amount, (float) hp.get(), (float) hp.getMax(), graceRank);
                        sm.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + final_));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void applyHealToEntityRef(
            @Nonnull com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> targetRef,
            float amount, int graceRank) {
        try {
            int hIdx = DefaultEntityStatTypes.getHealth();
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> refStore = targetRef.getStore();
            if (refStore == null) return;
            EntityStatMap statMap = refStore.getComponent(targetRef, EntityStatMap.getComponentType());
            if (statMap != null) {
                var hp = statMap.get(hIdx);
                if (hp != null) {
                    float final_ = withGrace(amount, (float) hp.get(), (float) hp.getMax(), graceRank);
                    statMap.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + final_));
                }
            }
        } catch (Exception ignored) {}
    }

    public void removePlayer(@Nonnull UUID uuid) {
        tickCounters.remove(uuid);
    }
}
