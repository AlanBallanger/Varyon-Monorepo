package fr.varyon.vrpg.classes.ombre;

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
import fr.varyon.vrpg.classes.WeaponDamageReader;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class OmbreOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final OmbreState ombreState;
    private final OmbrePoisonSystem poisonSystem;
    private Integer healthIdx = null;

    public OmbreOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                     @Nonnull OmbreState ombreState,
                                     @Nonnull OmbrePoisonSystem poisonSystem) {
        this.classManager = classManager;
        this.ombreState = ombreState;
        this.poisonSystem = poisonSystem;
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
            if (acc.getActiveClass() != PlayerClass.GUERRIER) return;
            if (acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.OMBRE) return;

            if (damage.getAmount() <= 0f) return;

            float base = damage.getAmount();
            float amount = base;
            StringBuilder log = new StringBuilder();
            log.append(String.format("[OmbreDmg] base=%.1f", base));

            // Sortie de stealth — casse l'invisibilité au premier coup
            if (ombreState.isInStealth(uuid)) {
                ombreState.breakStealth(uuid);
                log.append(" [stealth brisé]");
            }

            // Frappe Fatale — prochain coup boosté + traité comme crit pour Danse des Lames
            int frappeFataleRank = acc.getTalentRank(PlayerClass.GUERRIER, FrappeFataleSkill.TALENT_NODE_ID);
            boolean frappeFataleConsumed = frappeFataleRank > 0 && ombreState.consumeFrappeFatale(uuid);
            if (frappeFataleConsumed) {
                float bonus = FrappeFataleSkill.damageBonusForRank(frappeFataleRank);
                amount *= (1f + bonus);
                log.append(String.format(" FrappeFatale=+%.0f%%", bonus * 100));
                int danseRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.DANSE_LAMES_NODE);
                if (danseRank > 0) {
                    float speedBonus = OmbrePassifs.danseSpeedBonusForRank(danseRank);
                    ombreState.startDanseLames(uuid, speedBonus, OmbrePassifs.danseDurationMs());
                    log.append(String.format(" DanseLames(FF)=+%.0f%%spd", speedBonus * 100));
                }
            }

            // Chasse Ouverte — coup critique garanti sur cible marquée
            int chasseRank = ombreState.getChaseOuverteRank(uuid);
            if (chasseRank > 0) {
                fr.varyon.vrpg.classes.ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
                float critMult = 1.5f;
                if (stats != null && stats.critDamagePct() > 0) {
                    critMult = 1.0f + stats.critDamagePct() / 100.0f;
                }
                amount *= critMult;
                fr.varyon.vrpg.integration.DamageFloatBridge.markCritical(damage);
                log.append(String.format(" ChaseOuverte=crit(x%.2f)", critMult));
                int danseRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.DANSE_LAMES_NODE);
                if (danseRank > 0) {
                    float speedBonus = OmbrePassifs.danseSpeedBonusForRank(danseRank);
                    ombreState.startDanseLames(uuid, speedBonus, OmbrePassifs.danseDurationMs());
                }
            }

            // Frappe Précise (crit) — Danse des Lames
            boolean isCrit = isCriticalHit(damage);
            if (isCrit) {
                int danseRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.DANSE_LAMES_NODE);
                if (danseRank > 0) {
                    float speedBonus = OmbrePassifs.danseSpeedBonusForRank(danseRank);
                    ombreState.startDanseLames(uuid, speedBonus, OmbrePassifs.danseDurationMs());
                    log.append(String.format(" DanseLames=+%.0f%%spd", speedBonus * 100));
                }
            }

            // Cible < 30% HP — bonus Déluge de Lames et Pas de l'Ombre (appliqué lors du cast)

            if (amount != base) damage.setAmount(amount);
            log.append(String.format(" → final=%.1f", amount));
            if (VrpgConfig.isDebugCombat()) LOG.atInfo().log(log.toString());

            // Lames Empoisonnées — chance d'appliquer poison
            int poisonRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.LAMES_EMPOISONNEES_NODE);
            if (poisonRank > 0 && Math.random() < OmbrePassifs.poisonChanceForRank(poisonRank)) {
                Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
                poisonSystem.applyPoison(victimRef, amount, store);
                if (VrpgConfig.isDebugCombat())
                    LOG.atInfo().log("[OmbreDmg] Poison dpt=" + String.format("%.1f", amount * OmbrePassifs.POISON_WEAPON_PCT) + "/s");
            }

            // Embuscade — première frappe après invisibilité immobilise
            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
            int embuscadeRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.EMBUSCADE_NODE);
            int embuscadeConsumed = ombreState.consumeEmbuscade(uuid);
            if (embuscadeRank > 0 && embuscadeConsumed > 0) {
                applyRoot(victimRef, store, OmbrePassifs.embuscadeDurationMs(embuscadeConsumed));
            }

        } catch (Exception ignored) {}
    }

    private void applyRoot(@Nonnull Ref<EntityStore> victimRef,
                           @Nonnull Store<EntityStore> store, long durationMs) {
        try {
            float durationSec = durationMs / 1000f;
            int idx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap()
                .getIndex("Vrpg_Ombre_Stun");
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect effect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(idx);
            if (effect == null) return;
            com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                store.getComponent(victimRef,
                    com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
            if (ec != null) {
                ec.addEffect(victimRef, effect, durationSec,
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
            }
        } catch (Exception ignored) {}
    }

    private static boolean isCriticalHit(@Nonnull Damage damage) {
        try {
            boolean[] found = {false};
            damage.forEachMetaObject(new com.hypixel.hytale.server.core.meta.IMetaStore.MetaEntryConsumer() {
                @Override
                public <T> void accept(int metaId, T value) {
                    if (found[0] || value == null) return;
                    String s = value.toString().toLowerCase(java.util.Locale.ROOT);
                    if (s.contains("impact_critical") || (s.contains("critical")
                            && (s.contains("particle") || s.contains("systemid")))) {
                        found[0] = true;
                    }
                }
            });
            return found[0];
        } catch (Exception ignored) {}
        return false;
    }

    private float getMaxHp(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (healthIdx == null) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        if (healthIdx < 0) return 0f;
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return 0f;
        var hp = stats.get(healthIdx);
        return hp != null ? (float) hp.getMax() : 0f;
    }

    public float getTargetHpRatio(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
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
