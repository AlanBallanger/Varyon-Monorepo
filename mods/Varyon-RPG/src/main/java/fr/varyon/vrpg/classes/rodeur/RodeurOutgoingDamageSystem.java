package fr.varyon.vrpg.classes.rodeur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassPlayerStats;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.ability.ClassSkillCooldowns;
import fr.varyon.vrpg.combat.CombatCritDetection;
import fr.varyon.vrpg.config.VrpgConfig;
import fr.varyon.vrpg.integration.DamageFloatBridge;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public final class RodeurOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final Set<Dependency<EntityStore>> DEPENDENCIES =
        Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class));

    private final ClassManager classManager;
    private final RodeurState rodeurState;
    private final RodeurPoisonSystem poisonSystem;
    private final ClassSkillCooldowns cooldowns;
    private Integer healthIdx = null;

    public RodeurOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                      @Nonnull RodeurState rodeurState,
                                      @Nonnull RodeurPoisonSystem poisonSystem,
                                      @Nonnull ClassSkillCooldowns cooldowns) {
        this.classManager = classManager;
        this.rodeurState = rodeurState;
        this.poisonSystem = poisonSystem;
        this.cooldowns = cooldowns;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
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
            if (acc.getActiveClass() != PlayerClass.TIREUR) return;
            if (acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.RODEUR) return;

            if (damage.getAmount() <= 0f) return;

            float base = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[RodeurDmg] base=%.1f", base)) : null;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            // Flèches rodeur à l'impact (Recul, Entravante, Rafale) — remplace les 1 dégâts symboliques du JSON
            int arrowType = rodeurState.getPendingArrowType(uuid);
            if (arrowType > 0) {
                float arrowDmg = rodeurState.consumePendingArrowDmg(uuid);
                if (arrowType == RodeurState.ARROW_TYPE_MARQUAGE || arrowDmg > 0f) {
                    if (arrowDmg > 0f) {
                        amount = applyCritToArrowDamage(uuid, acc, arrowDmg, damage);
                        damage.setAmount(amount);
                    }
                    if (debug) log.append(String.format(" Arrow(%d)=%.1f", arrowType, arrowDmg));

                    if (arrowType == RodeurState.ARROW_TYPE_RECUL) {
                        double kb = rodeurState.getPendingArrowKb(uuid);
                        LOG.atInfo().log("[RodeurKB] RECUL hit — kb=" + kb + " victimRef=" + (victimRef != null ? victimRef.getIndex() : "null") + " attackerRef=" + (attackerRef != null ? attackerRef.getIndex() : "null"));
                        if (kb > 0) {
                            TransformComponent tcVictim = store.getComponent(victimRef, TransformComponent.getComponentType());
                            TransformComponent tcAttacker = store.getComponent(attackerRef, TransformComponent.getComponentType());
                            LOG.atInfo().log("[RodeurKB] tcVictim=" + tcVictim + " tcAttacker=" + tcAttacker);
                            if (tcVictim != null && tcAttacker != null) {
                                org.joml.Vector3d d = new org.joml.Vector3d(
                                    tcVictim.getPosition().x - tcAttacker.getPosition().x,
                                    0,
                                    tcVictim.getPosition().z - tcAttacker.getPosition().z);
                                double dlen = Math.sqrt(d.x*d.x + d.z*d.z);
                                LOG.atInfo().log("[RodeurKB] dx=" + d.x + " dz=" + d.z + " dlen=" + dlen);
                                if (dlen > 1e-6) { d.x /= dlen; d.z /= dlen; }
                                com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent kbComp =
                                    new com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent();
                                kbComp.setVelocity(new org.joml.Vector3d(d.x * kb, 5.0, d.z * kb));
                                kbComp.setVelocityType(com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                kbComp.setDuration(0.0f);
                                damage.putMetaObject(com.hypixel.hytale.server.core.modules.entity.damage.Damage.KNOCKBACK_COMPONENT, kbComp);
                                LOG.atInfo().log("[RodeurKB] KnockbackComponent applied: vx=" + (d.x * kb) + " vy=5.0 vz=" + (d.z * kb));
                            } else {
                                LOG.atInfo().log("[RodeurKB] SKIP — transform null");
                            }
                        } else {
                            LOG.atInfo().log("[RodeurKB] SKIP — kb=0");
                        }
                        rodeurState.clearPendingArrow(uuid);
                    } else if (arrowType == RodeurState.ARROW_TYPE_MARQUAGE) {
                        damage.setAmount(0f);
                        long pendingDuration = rodeurState.getPendingMarqueDuration(uuid);
                        int pendingConsumes = rodeurState.getPendingMarqueConsumes(uuid);
                        int marqRank = rodeurState.consumePendingMarquageRank(uuid);
                        rodeurState.startMarque(uuid, Math.max(1, marqRank), pendingDuration, victimRef.getIndex(), pendingConsumes);
                        int rootIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Marque_Chasseur");
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect marqueEff =
                            rootIdx >= 0 ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(rootIdx) : null;
                        if (marqueEff != null) {
                            com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                            if (ec != null) ec.addEffect(victimRef, marqueEff, pendingDuration / 1000f,
                                com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                        }
                        return;
                    } else if (arrowType == RodeurState.ARROW_TYPE_ENTRAVANTE) {
                        long rootMs = rodeurState.getPendingArrowRoot(uuid);
                        if (rootMs > 0) {
                            int rootIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Ombre_Root");
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect rootEffect =
                                rootIdx >= 0 ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(rootIdx) : null;
                            if (rootEffect != null) {
                                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                    store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                                if (ec != null) ec.addEffect(victimRef, rootEffect, rootMs / 1000f,
                                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                            }
                        }
                        rodeurState.clearPendingArrow(uuid);
                    } else if (arrowType == RodeurState.ARROW_TYPE_RAFALE) {
                        rodeurState.clearPendingArrow(uuid);
                    }

                    if (arrowType != RodeurState.ARROW_TYPE_MARQUAGE && amount > 0f) {
                        amount = applyMarqueIfMarked(uuid, acc, victimRef, amount, store, debug, log);
                        damage.setAmount(amount);
                    }
                }
                return;
            }

            amount = applyMarqueIfMarked(uuid, acc, victimRef, amount, store, debug, log);

            // Précision mortelle — bonus si cible < 50% HP
            int precisionRank = acc.getTalentRank(PlayerClass.TIREUR, RodeurPassifs.PRECISION_MORTELLE_NODE);
            if (precisionRank > 0) {
                float hpRatio = getHpRatio(victimRef, store);
                if (hpRatio < RodeurPassifs.PRECISION_HP_THRESH) {
                    float bonus = RodeurPassifs.precisionBonusForRank(precisionRank);
                    amount *= (1f + bonus);
                    if (debug) log.append(String.format(" PrecisionMortelle=+%.0f%%", bonus * 100));
                }
            }

            // Traque mobile — bonus si le joueur se déplace
            int traqueMobileRank = acc.getTalentRank(PlayerClass.TIREUR, RodeurPassifs.TRAQUE_MOBILE_NODE);
            if (traqueMobileRank > 0 && isMoving(attackerRef, store)) {
                float bonus = RodeurPassifs.traqueBonusForRank(traqueMobileRank);
                amount *= (1f + bonus);
                if (debug) log.append(String.format(" TraqueMobile=+%.0f%%", bonus * 100));
            }

            if (amount != base) damage.setAmount(amount);
            if (debug) { log.append(String.format(" → final=%.1f", amount)); LOG.atInfo().log(log.toString()); }

            // Flèches toxiques — chance d'empoisonner
            int poisonRank = acc.getTalentRank(PlayerClass.TIREUR, RodeurPassifs.FLECHES_TOXIQUES_NODE);
            if (poisonRank > 0 && Math.random() < RodeurPassifs.poisonChanceForRank(poisonRank)) {
                poisonSystem.applyPoison(victimRef, amount, store);
            }

        } catch (Exception ignored) {}
    }

    private boolean willKill(@Nonnull Ref<EntityStore> ref, float damage, @Nonnull Store<EntityStore> store) {
        if (healthIdx == null) {
            try { healthIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        if (healthIdx < 0) return false;
        com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap stats = store.getComponent(ref, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
        if (stats == null) return false;
        var hp = stats.get(healthIdx);
        if (hp == null) return false;
        return damage >= hp.get();
    }

    private boolean isMoving(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
            if (vel == null) return false;
            org.joml.Vector3d v = vel.getVelocity();
            if (v == null) return false;
            double hSpeed = v.x * v.x + v.z * v.z;
            return hSpeed > 0.01;
        } catch (Exception ignored) {
            return false;
        }
    }

    private float applyCritToArrowDamage(@Nonnull UUID uuid,
                                         @Nonnull ClassAccount acc,
                                         float arrowDmg,
                                         @Nonnull Damage damage) {
        ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
        if (stats == null) {
            stats = ClassStatDefinition.compute(acc.getProgress(PlayerClass.TIREUR).getLevel(),
                PlayerSpecialization.RODEUR);
        }
        boolean isCrit = CombatCritDetection.isCriticalHit(damage);
        if (!isCrit && stats != null && stats.critChancePct() > 0
                && Math.random() < stats.critChancePct() / 100.0) {
            isCrit = true;
        }
        float amount = arrowDmg;
        if (isCrit && stats != null) {
            amount *= (1.0f + stats.critDamagePct() / 100.0f);
            DamageFloatBridge.markCritical(damage);
        } else {
            DamageFloatBridge.clearCritical(damage);
        }
        return amount;
    }

    private float applyMarqueIfMarked(@Nonnull UUID uuid,
                                      @Nonnull ClassAccount acc,
                                      @Nonnull Ref<EntityStore> victimRef,
                                      float amount,
                                      @Nonnull Store<EntityStore> store,
                                      boolean debug,
                                      StringBuilder log) {
        int marqueRank = rodeurState.getMarqueRank(uuid);
        if (marqueRank <= 0) return amount;
        long targetIdx = rodeurState.getMarqueTargetIdx(uuid);
        if (targetIdx < 0 || victimRef.getIndex() != targetIdx) return amount;

        float bonus = MarqueDuChasseurSkill.damageBonusForRank(marqueRank);
        amount *= (1f + bonus);
        rodeurState.consumeMarque(uuid);
        if (debug) log.append(String.format(" Marque=+%.0f%%", bonus * 100));

        int traqueSansFinRank = acc.getTalentRank(PlayerClass.TIREUR, RodeurPassifs.TRAQUE_SANS_FIN_NODE);
        if (traqueSansFinRank > 0 && willKill(victimRef, amount, store)) {
            float reduction = RodeurPassifs.traqueCdReductionForRank(traqueSansFinRank);
            long cdMs = MarqueDuChasseurSkill.cooldownMsForRank(marqueRank);
            long reduceMs = (long) (cdMs * reduction);
            cooldowns.reduceCooldownBy(uuid, MarqueDuChasseurSkill.SKILL_ID, reduceMs);
            if (debug) log.append(String.format(" TraqueSansFin=-%dms", reduceMs));
        }
        return amount;
    }

    private float getHpRatio(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (healthIdx == null) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        if (healthIdx < 0) return 1f;
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return 1f;
        var hp = stats.get(healthIdx);
        if (hp == null || hp.getMax() <= 0) return 1f;
        return hp.get() / hp.getMax();
    }
}
