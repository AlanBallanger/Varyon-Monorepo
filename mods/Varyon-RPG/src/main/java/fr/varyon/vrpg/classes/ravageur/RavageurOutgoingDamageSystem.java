package fr.varyon.vrpg.classes.ravageur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class RavageurOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final RavageurState ravageurState;
    private Integer healthIdx = null;

    public RavageurOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                        @Nonnull RavageurState ravageurState) {
        this.classManager  = classManager;
        this.ravageurState = ravageurState;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled()) return;

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef == null || !attackerRef.isValid()) return;

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.BARBARE) return;
            if (acc.getActiveSpec(PlayerClass.BARBARE) != PlayerSpecialization.RAVAGEUR) return;

            if (damage.getAmount() <= 0f) return;

            float base   = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[RavageurDmg] base=%.1f", base)) : null;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            // Déchaînement — bonus dégâts actif
            float dechBonus = ravageurState.getDechainementBonus(uuid);
            if (dechBonus > 0f) {
                amount *= (1f + dechBonus);
                if (log != null) log.append(String.format(" Dechainement=+%.0f%%", dechBonus * 100));
            }

            // Élan destructeur — prochain coup armé après kill
            int elanRank = ravageurState.consumeElan(uuid);
            if (elanRank > 0) {
                float elanBonus = RavageurPassifs.elanBonusForRank(elanRank);
                amount *= (1f + elanBonus);
                if (log != null) log.append(String.format(" Elan=+%.0f%%", elanBonus * 100));
            }

            // Exécuteur — bonus si cible < 30% HP
            int executeurRank = acc.getTalentRank(PlayerClass.BARBARE, RavageurPassifs.EXECUTEUR_NODE);
            if (executeurRank > 0) {
                float targetHpPct = getHpPercent(victimRef, store);
                if (targetHpPct > 0f && targetHpPct < RavageurPassifs.EXECUTEUR_THRESHOLD) {
                    float bonus = RavageurPassifs.executeurBonusForRank(executeurRank);
                    amount *= (1f + bonus);
                    if (log != null) log.append(String.format(" Executeur=+%.0f%%", bonus * 100));
                }
            }

            // Chasseur de géant — bonus si cible a plus de HP que soi
            int chasseurRank = acc.getTalentRank(PlayerClass.BARBARE, RavageurPassifs.CHASSEUR_GEANT_NODE);
            if (chasseurRank > 0) {
                float selfHp   = getHpAbsolute(attackerRef, store);
                float targetHp = getHpAbsolute(victimRef, store);
                if (selfHp > 0f && targetHp > selfHp) {
                    float bonus = RavageurPassifs.chasseurGeantBonusForRank(chasseurRank);
                    amount *= (1f + bonus);
                    if (log != null) log.append(String.format(" ChasseurGeant=+%.0f%%", bonus * 100));
                }
            }

            // Combattant infatigable — bonus selon HP manquants du joueur
            int combattantRank = acc.getTalentRank(PlayerClass.BARBARE, RavageurPassifs.COMBATTANT_INFATIGABLE_NODE);
            if (combattantRank > 0) {
                float selfHpPct = getHpPercent(attackerRef, store);
                if (selfHpPct >= 0f && selfHpPct < 1f) {
                    float missingPct = 1f - selfHpPct;
                    int tenPctChunks = (int) (missingPct * 10);
                    if (tenPctChunks > 0) {
                        float bonus = tenPctChunks * RavageurPassifs.combattantBonusPer10PctForRank(combattantRank);
                        amount *= (1f + bonus);
                        if (log != null) log.append(String.format(" Combattant(%d/10)=+%.0f%%", tenPctChunks, bonus * 100));
                    }
                }
            }

            // Arme lourde — slow sur critique
            int armeLourdeRank = acc.getTalentRank(PlayerClass.BARBARE, RavageurPassifs.ARME_LOURDE_NODE);
            if (armeLourdeRank > 0 && isCriticalHit(damage)) {
                try {
                    int slowIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                        .getAssetMap().getIndex("Vrpg_Arme_Lourde");
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect slowEffect =
                        (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                            .getAssetMap().getAsset(slowIdx);
                    if (slowEffect != null) {
                        float slowSec = RavageurPassifs.armeLourdeDurationMs() / 1000f;
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                            store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                        if (ec == null) ec = commandBuffer.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                        if (ec != null) ec.addEffect(victimRef, slowEffect, slowSec,
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                        if (log != null) log.append(" ArmeLourde(slow)");
                    }
                } catch (Exception ignored2) {}
            }

            if (amount != base) damage.setAmount(amount);
            if (log != null) {
                log.append(String.format(" -> %.1f", amount));
                LOG.atInfo().log(log.toString());
            }

        } catch (Exception ignored) {}
    }

    private static boolean isCriticalHit(@Nonnull Damage damage) {
        try {
            boolean[] found = {false};
            damage.forEachMetaObject(new com.hypixel.hytale.server.core.meta.IMetaStore.MetaEntryConsumer() {
                @Override
                public <T> void accept(int metaId, T value) {
                    if (value == null || found[0]) return;
                    String sl = value.toString().toLowerCase(java.util.Locale.ROOT);
                    if (sl.contains("impact_critical") || (sl.contains("critical")
                            && (sl.contains("particle") || sl.contains("systemid")))) {
                        found[0] = true;
                    }
                }
            });
            return found[0];
        } catch (Exception e) { return false; }
    }

    private float getHpPercent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            int hIdx = healthIndex();
            if (hIdx < 0) return 0f;
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 0f;
            var hp = stats.get(hIdx);
            if (hp == null || hp.getMax() <= 0) return 0f;
            return hp.get() / hp.getMax();
        } catch (Exception e) { return 0f; }
    }

    private float getHpAbsolute(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            int hIdx = healthIndex();
            if (hIdx < 0) return 0f;
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 0f;
            var hp = stats.get(hIdx);
            return hp != null ? hp.get() : 0f;
        } catch (Exception e) { return 0f; }
    }

    private int healthIndex() {
        if (healthIdx == null) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        return healthIdx;
    }
}
