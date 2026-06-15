package fr.varyon.vrpg.classes.gardiendesgaia;

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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class GardienDeGaiaOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final GardienDeGaiaState state;

    public GardienDeGaiaOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                              @Nonnull GardienDeGaiaState state) {
        this.classManager = classManager;
        this.state        = state;
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

            // Dégâts du tréant : multiplier par le dmgFactor calculé au spawn
            float treantDmg = state.getTreantDmgFactorForRef(attackerRef);
            if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) {
                LOG.atInfo().log(String.format("[TreantLookup] attackerIdx=%d treantDmg=%.2f", attackerRef.getIndex(), treantDmg));
            }
            if (treantDmg != 1f && damage.getAmount() > 0f) {
                float finalDmg = damage.getAmount() * treantDmg;
                damage.setAmount(finalDmg);
                if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
                    LOG.atInfo().log(String.format("[TreantHit] base=%.1f factor=%.2f final=%.1f", damage.getAmount() / treantDmg, treantDmg, finalDmg));
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, chunk.getReferenceTo(index), finalDmg, "NATURE");
                return;
            }

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.MAGE) return;
            if (acc.getActiveSpec(PlayerClass.MAGE) != PlayerSpecialization.GARDIEN_DE_GAIA) return;

            // Étreinte de Gaïa : projectile à impact — appliquer AoE + root autour de la cible touchée
            int etreinteRank = state.consumeEtreinte(uuid);
            if (etreinteRank > 0) {
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float baseDmg = weaponDmg > 0 ? (float) weaponDmg : 1f;
                float dmg = baseDmg * 50f * fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.damageFactor(etreinteRank);
                float rootRadius = (float) fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.rootRadius();
                float rootSec    = fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.rootDurationMs(etreinteRank) / 1000f;

                int rootEffIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                    .getAssetMap().getIndex("Vrpg_Etreinte_Root");
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect rootEff = rootEffIdx >= 0
                    ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                      com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(rootEffIdx)
                    : null;

                com.hypixel.hytale.server.core.modules.entity.component.TransformComponent victimTc =
                    store.getComponent(chunk.getReferenceTo(index),
                        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                if (victimTc != null) {
                    org.joml.Vector3d impactPos = victimTc.getPosition();
                    long casterIdx = attackerRef.getIndex();
                    final float fDmg = dmg;
                    final com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect fRootEff = rootEff;
                    final float fRootSec = rootSec;

                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                        .selectNearbyEntities(store, impactPos, rootRadius, targetRef -> {
                            try {
                                if (targetRef.getIndex() == casterIdx) return;
                                // Dégâts AoE
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                    targetRef, store,
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(attackerRef),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND, fDmg));
                                // Root
                                if (fRootEff != null) {
                                    com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                        store.getComponent(targetRef,
                                            com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                                    if (ec != null)
                                        ec.addEffect(targetRef, fRootEff, fRootSec,
                                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                                }
                                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, targetRef, fDmg, "NATURE");
                            } catch (Exception ignored2) {}
                        }, t -> t.getIndex() != casterIdx);
                }
                // Annuler le dégât direct du projectile — l'AoE gère tout
                damage.setAmount(0f);
                return;
            }

            float base   = damage.getAmount();
            float amount = base;
            boolean debug = fr.varyon.vrpg.config.VrpgConfig.isDebugCombat();
            String causeId = damage.getCause() != null ? damage.getCause().getId() : "?";
            StringBuilder log = debug ? new StringBuilder(String.format("[GardienDmg] cause=%s base=%.1f", causeId, base)) : null;

            if (fr.varyon.vrpg.classes.WeaponCategory.heldCategory(playerRef)
                    == fr.varyon.vrpg.classes.WeaponCategory.MAGIE
                    && damage.getCause() != com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND) {
                amount *= 50f;
                if (log != null) log.append(" Staff=x50");
            }

            fr.varyon.vrpg.classes.ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
            if (stats != null && stats.critChancePct() > 0
                    && Math.random() < stats.critChancePct() / 100.0) {
                float critMult = 1.0f + stats.critDamagePct() / 100.0f;
                float finalAmt = amount * critMult;
                damage.setAmount(finalAmt);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, chunk.getReferenceTo(index), finalAmt, "NATURE_CRITICAL");
                if (log != null) LOG.atInfo().log(log.append(String.format(" CRIT -> %.1f", finalAmt)).toString());
            } else {
                if (amount != base) damage.setAmount(amount);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, chunk.getReferenceTo(index), amount, "NATURE");
                if (log != null) LOG.atInfo().log(log.append(String.format(" -> %.1f", amount)).toString());
            }

        } catch (Exception ignored) {}
    }
}
