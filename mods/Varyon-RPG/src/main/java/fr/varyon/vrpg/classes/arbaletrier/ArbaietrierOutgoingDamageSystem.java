package fr.varyon.vrpg.classes.arbaletrier;

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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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

public final class ArbaietrierOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final ArbaietrierState arbaState;
    private final ArbaietrierBleedSystem bleedSystem;
    private Integer healthIdx = null;

    public ArbaietrierOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                           @Nonnull ArbaietrierState arbaState,
                                           @Nonnull ArbaietrierBleedSystem bleedSystem) {
        this.classManager = classManager;
        this.arbaState = arbaState;
        this.bleedSystem = bleedSystem;
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
            if (acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.ARBALETRIER) return;

            if (damage.getAmount() <= 0f) return;

            float base = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[ArbaietrierDmg] base=%.1f", base)) : null;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            // Recul tactique — bonus prochain tir
            float reculBonus = arbaState.consumeReculBonus(uuid);
            if (reculBonus > 0f) {
                amount *= (1f + reculBonus);
                if (debug) log.append(String.format(" ReculTactique=+%.0f%%", reculBonus * 100));
            }

            // Carreau lourd — bonus prochain tir (cumulatif avec recul)
            int lourdRank = arbaState.consumeCarreauLourd(uuid);
            if (lourdRank > 0) {
                float bonus = CarreauLourdSkill.damageBonusForRank(lourdRank);
                amount *= (1f + bonus);
                if (debug) log.append(String.format(" CarreauLourd=+%.0f%%", bonus * 100));
            }

            // Mise en joue — crit garanti + gros bonus
            int miseRank = arbaState.consumeMiseEnJou(uuid);
            if (miseRank > 0) {
                float bonus = MiseEnJouSkill.damageBonusForRank(miseRank);
                amount *= (1f + bonus);
                fr.varyon.vrpg.integration.DamageFloatBridge.markCritical(damage);
                if (debug) log.append(String.format(" MiseEnJou=+%.0f%%(crit)", bonus * 100));
            }

            // Tireur embusqué — bonus après immobilité
            int embusqueRank = acc.getTalentRank(PlayerClass.TIREUR, ArbaietrierPassifs.TIREUR_EMBUSQUE_NODE);
            if (embusqueRank > 0 && arbaState.isImmobileFor(uuid, ArbaietrierPassifs.EMBUSQUE_DELAY_MS)) {
                float bonus = ArbaietrierPassifs.embusqueBonusForRank(embusqueRank);
                amount *= (1f + bonus);
                if (debug) log.append(String.format(" TireurEmbusque=+%.0f%%", bonus * 100));
            }

            // Chasseur de colosses — bonus si cible a plus de HP que soi
            int colossesRank = acc.getTalentRank(PlayerClass.TIREUR, ArbaietrierPassifs.CHASSEUR_COLOSSES_NODE);
            if (colossesRank > 0) {
                float attackerHp = getMaxHp(attackerRef, store);
                float victimHp = getMaxHp(victimRef, store);
                if (victimHp > attackerHp) {
                    float bonus = ArbaietrierPassifs.colossesBonusForRank(colossesRank);
                    amount *= (1f + bonus);
                    if (debug) log.append(String.format(" ChasseurColosses=+%.0f%%", bonus * 100));
                }
            }

            // Viseur expérimenté — bonus selon distance (node 8)
            int reflexesRank = acc.getTalentRank(PlayerClass.TIREUR, ArbaietrierPassifs.VISEUR_NODE);
            if (reflexesRank > 0) {
                double dist = getDistanceBetween(attackerRef, victimRef, store);
                if (dist > ArbaietrierPassifs.VISEUR_MIN_DIST) {
                    float bonus = ArbaietrierPassifs.viseurTotalBonusForRank(reflexesRank, dist);
                    if (bonus > 0f) {
                        amount *= (1f + bonus);
                        if (debug) log.append(String.format(" Viseur=+%.0f%%(%.0fm)", bonus * 100, dist));
                    }
                }
            }

            if (amount != base) damage.setAmount(amount);
            if (debug) { log.append(String.format(" → final=%.1f", amount)); LOG.atInfo().log(log.toString()); }

            // Carreaux lacérants — saignement passif
            int bleedRank = acc.getTalentRank(PlayerClass.TIREUR, ArbaietrierPassifs.CARREAUX_LACERANTS_NODE);
            if (bleedRank > 0 && Math.random() < ArbaietrierPassifs.bleedChanceForRank(bleedRank)) {
                bleedSystem.applyBleed(victimRef, amount, store);
            }

        } catch (Exception ignored) {}
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

    private double getDistanceBetween(@Nonnull Ref<EntityStore> a, @Nonnull Ref<EntityStore> b,
                                      @Nonnull Store<EntityStore> store) {
        try {
            TransformComponent ta = store.getComponent(a, TransformComponent.getComponentType());
            TransformComponent tb = store.getComponent(b, TransformComponent.getComponentType());
            if (ta == null || tb == null) return 0;
            return ta.getPosition().distance(tb.getPosition());
        } catch (Exception e) { return 0; }
    }
}
