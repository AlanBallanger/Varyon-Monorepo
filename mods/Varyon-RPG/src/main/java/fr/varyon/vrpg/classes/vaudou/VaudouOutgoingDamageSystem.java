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
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class VaudouOutgoingDamageSystem extends DamageEventSystem {

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
            if (damage.isCancelled()) return;

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;
            Ref<EntityStore> attackerRef = entitySource.getRef();

            com.hypixel.hytale.server.core.entity.entities.Player playerComp =
                store.getComponent(attackerRef,
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerComp == null) return;

            com.hypixel.hytale.server.core.universe.PlayerRef playerRef =
                store.getComponent(attackerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
            if (playerRef == null) playerRef =
                commandBuffer.getComponent(attackerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());

            UUID uuid = playerComp.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.MAGE) return;
            if (acc.getActiveSpec(PlayerClass.MAGE) != PlayerSpecialization.VAUDOU) return;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            int level = acc.getProgress(PlayerClass.MAGE).getLevel();
            float vaudouMageMult = (float) (ClassStatDefinition.atkDisplayMultiplier(level, PlayerSpecialization.VAUDOU)
                * PlayerSpecialization.VAUDOU.getWeaponMagieMult());

            fr.varyon.vrpg.classes.ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
            if (stats == null) stats = fr.varyon.vrpg.classes.ClassStatDefinition.compute(level, PlayerSpecialization.VAUDOU);
            boolean isCrit = stats != null && stats.critChancePct() > 0
                && Math.random() < stats.critChancePct() / 100.0;
            float critMult = isCrit ? (1.0f + (stats.critDamagePct() / 100.0f)) : 1.0f;

            float pendingSkill = vaudouState.consumePendingSkillDmg(uuid);
            boolean isFleauHit = vaudouState.hasPendingFleau(uuid);
            if (isFleauHit) {
                damage.setAmount(0f);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
            } else if (pendingSkill > 0f) {
                float finalSkill = pendingSkill * critMult;
                damage.setAmount(finalSkill);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, victimRef, finalSkill * vaudouMageMult,
                    isCrit ? "SHADOW_CRITICAL" : "SHADOW");
            } else if (playerRef != null
                    && WeaponCategory.heldCategory(playerRef) == WeaponCategory.MAGIE
                    && damage.getCause() != com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND) {
                float base50 = 50f * critMult;
                damage.setAmount(base50);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, victimRef, base50 * vaudouMageMult,
                    isCrit ? "SHADOW_CRITICAL" : "SHADOW");
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

            boolean isCursed;
            try {
                TransformComponent victimTc = store.getComponent(victimRef, TransformComponent.getComponentType());
                isCursed = victimTc != null && VaudouTotemHelper.isInAnyTotem(victimTc.getPosition());
            } catch (Exception ignored) {
                isCursed = false;
            }

            float mult = 1.0f;

            // Rituel Interdit — bonus sur cibles maudites
            int rituelRank = acc.getTalentRank(PlayerClass.MAGE, VaudouPassifs.RITUEL_NODE);
            if (rituelRank > 0 && isCursed) {
                mult += VaudouPassifs.rituelBonusForRank(rituelRank);
            }

            // Totem de Vulnérabilité — bonus si la cible est dans la zone
            try {
                TransformComponent ttc = store.getComponent(victimRef, TransformComponent.getComponentType());
                if (ttc != null) {
                    float vulnMag = VaudouTotemHelper.getVulnerabilityMagnitudeAt(ttc.getPosition());
                    if (vulnMag > 0f) mult += vulnMag;
                }
            } catch (Exception ignored) {}

            if (mult != 1.0f) {
                damage.setAmount(damage.getAmount() * mult);
            }

            // Parasite Spirituel — soin au caster si cible maudite
            int parasiteRank = acc.getTalentRank(PlayerClass.MAGE, VaudouPassifs.PARASITE_NODE);
            if (parasiteRank > 0 && isCursed) {
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
