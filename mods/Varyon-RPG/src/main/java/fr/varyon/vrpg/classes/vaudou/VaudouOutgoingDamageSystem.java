package fr.varyon.vrpg.classes.vaudou;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.WeaponCategory;
import fr.varyon.vrpg.config.VrpgConfig;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class VaudouOutgoingDamageSystem extends DamageEventSystem {

    public static final float STAFF_BASE_MULT = 5f;

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final VaudouState vaudouState;
    @Nullable private VaudouPoisonSystem vaudouPoisonSystem;

    public VaudouOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                      @Nonnull VaudouState vaudouState) {
        this.classManager = classManager;
        this.vaudouState  = vaudouState;
    }

    public void setVaudouPoisonSystem(@Nullable VaudouPoisonSystem sys) {
        this.vaudouPoisonSystem = sys;
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
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;
            Ref<EntityStore> attackerRef = entitySource.getRef();

            com.hypixel.hytale.server.core.entity.entities.Player playerComp =
                store.getComponent(attackerRef,
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerComp == null) {
                playerComp = commandBuffer.getComponent(attackerRef,
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            }
            if (playerComp == null) return;

            com.hypixel.hytale.server.core.universe.PlayerRef playerRef =
                store.getComponent(attackerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
            if (playerRef == null) playerRef =
                commandBuffer.getComponent(attackerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.MAGE) return;
            if (acc.getActiveSpec(PlayerClass.MAGE) != PlayerSpecialization.VAUDOU) return;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            int level = acc.getProgress(PlayerClass.MAGE).getLevel();
            float levelMult = (float) ClassStatDefinition.atkDisplayMultiplier(level, PlayerSpecialization.VAUDOU);

            fr.varyon.vrpg.classes.ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
            if (stats == null) stats = fr.varyon.vrpg.classes.ClassStatDefinition.compute(level, PlayerSpecialization.VAUDOU);
            boolean isCrit = stats != null && stats.critChancePct() > 0
                && Math.random() < stats.critChancePct() / 100.0;
            float critMult = isCrit ? (1.0f + (stats.critDamagePct() / 100.0f)) : 1.0f;

            float pendingSkill = vaudouState.consumePendingSkillDmg(uuid);
            boolean skillDamagePreset = pendingSkill > 0f;
            boolean isFleauHit = vaudouState.hasPendingFleau(uuid);
            boolean staffHit = false;
            if (isFleauHit) {
                damage.setAmount(0f);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
            } else {
                float base = damage.getAmount();
                if (pendingSkill > 0f) {
                    base = pendingSkill;
                }
                float amount = base;
                staffHit = WeaponCategory.heldCategory(playerRef) == WeaponCategory.MAGIE || skillDamagePreset;
                if (staffHit
                        && damage.getCause() != com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND) {
                    amount *= STAFF_BASE_MULT * levelMult;
                }
                amount *= critMult;
                damage.setAmount(amount);
            }

            // Fléau Toxique — consomme le projectile pending et applique poison + malédiction
            VaudouState.PendingFleau pending = vaudouState.consumePendingFleau(uuid);
            if (pending != null && vaudouPoisonSystem != null) {
                vaudouPoisonSystem.applyPoison(victimRef, pending.dpt(), pending.durationMs(), store);
                vaudouState.curseNpc(victimRef.getIndex(), pending.durationMs());
                vaudouState.armCurse(uuid, pending.durationMs(), pending.rank());
                try {
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect eff = null;
                    try {
                        int ei = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Fleau_Toxique");
                        eff = (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(ei);
                    } catch (Exception ignored2) {}
                    if (eff != null) {
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                            store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                        if (ec != null) ec.addEffect(victimRef, eff, pending.durationMs() / 1000f,
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                    }
                } catch (Exception ignored) {}
            }

            boolean inTotemZone;
            boolean npcCursed;
            try {
                TransformComponent victimTc = store.getComponent(victimRef, TransformComponent.getComponentType());
                inTotemZone = victimTc != null && VaudouTotemHelper.isInAnyTotem(victimTc.getPosition());
                npcCursed = vaudouState.isNpcCursed(victimRef.getIndex());
            } catch (Exception ignored) {
                inTotemZone = false;
                npcCursed = false;
            }

            float mult = 1.0f;

            int rituelRank = acc.getTalentRank(PlayerClass.MAGE, VaudouPassifs.RITUEL_NODE);
            if (rituelRank > 0 && inTotemZone) {
                mult += VaudouPassifs.rituelBonusForRank(rituelRank);
            }

            try {
                TransformComponent ttc = store.getComponent(victimRef, TransformComponent.getComponentType());
                if (ttc != null) {
                    float vulnMag = VaudouTotemHelper.getVulnerabilityMagnitudeAt(ttc.getPosition());
                    if (vulnMag > 0f) mult += vulnMag;
                }
            } catch (Exception ignored) {}

            if (mult != 1.0f && !skillDamagePreset) {
                damage.setAmount(damage.getAmount() * mult);
            }

            if (!isFleauHit && damage.getAmount() > 0f) {
                if (staffHit
                        && damage.getCause() != com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND) {
                    float weaponMult = (float) WeaponCategory.MAGIE.getMultiplierFor(PlayerSpecialization.VAUDOU);
                    if (weaponMult != 1.0f) {
                        damage.setAmount(damage.getAmount() * weaponMult);
                    }
                }
                float finalAmount = damage.getAmount();
                if (VrpgConfig.isDebugCombat()) {
                    String causeId = damage.getCause() != null ? damage.getCause().getId() : "?";
                    LOG.atInfo().log(String.format(
                        "[VaudouDmg] cause=%s Staff=x%.0f Lvl=x%.2f Weapon=x%.2f -> %.1f",
                        causeId, STAFF_BASE_MULT, levelMult,
                        (float) WeaponCategory.MAGIE.getMultiplierFor(PlayerSpecialization.VAUDOU),
                        finalAmount));
                }
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                if (isCrit) {
                    fr.varyon.vrpg.integration.DamageFloatBridge.markCritical(damage);
                }
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(
                    store, victimRef, finalAmount, isCrit ? "SHADOW_CRITICAL" : "SHADOW");
            }

            int parasiteRank = acc.getTalentRank(PlayerClass.MAGE, VaudouPassifs.PARASITE_NODE);
            if (parasiteRank > 0 && npcCursed) {
                final float heal = damage.getAmount() * VaudouPassifs.parasiteHealPctForRank(parasiteRank);
                try {
                    com.hypixel.hytale.server.core.universe.PlayerRef casterPlayerRef =
                        com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(uuid);
                    if (casterPlayerRef != null) {
                        Ref<EntityStore> casterEntityRef = casterPlayerRef.getReference();
                        if (casterEntityRef != null && casterEntityRef.isValid()) {
                            Store<EntityStore> casterStore = casterEntityRef.getStore();
                            if (casterStore != null) {
                                EntityStatMap casterSm = casterStore.getComponent(casterEntityRef, EntityStatMap.getComponentType());
                                if (casterSm != null) {
                                    int hIdx = DefaultEntityStatTypes.getHealth();
                                    var hp = casterSm.get(hIdx);
                                    if (hp != null) casterSm.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + heal));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
