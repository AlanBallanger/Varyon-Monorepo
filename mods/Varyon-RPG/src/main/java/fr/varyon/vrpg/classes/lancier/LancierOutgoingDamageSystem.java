package fr.varyon.vrpg.classes.lancier;

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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.combat.CombatCritDetection;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public final class LancierOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final Set<Dependency<EntityStore>> DEPENDENCIES =
        Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class));

    private final ClassManager classManager;
    private final LancierState lancierState;
    private final LancierBleedSystem bleedSystem;
    private Integer healthIdx = null;

    public LancierOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                       @Nonnull LancierState lancierState,
                                       @Nonnull LancierBleedSystem bleedSystem) {
        this.classManager = classManager;
        this.lancierState = lancierState;
        this.bleedSystem  = bleedSystem;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() { return DEPENDENCIES; }

    @Override
    public SystemGroup<EntityStore> getGroup() { return DamageModule.get().getFilterDamageGroup(); }

    @Override
    public Query<EntityStore> getQuery() { return NPCEntity.getComponentType(); }

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
            if (acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.LANCIER) return;
            if (damage.getAmount() <= 0f) return;

            float base   = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[LancierDmg] base=%.1f", base)) : null;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            // --- Garde du Lancier — multiplicateur prochain coup ---
            int gardeDmgRank = lancierState.consumeGardeDamage(uuid);
            if (gardeDmgRank > 0) {
                float mult = GardeDuLancierSkill.nextHitMultForRank(gardeDmgRank);
                amount *= mult;
                if (debug) log.append(String.format(" GardeDmg(r%d)=x%.2f", gardeDmgRank, mult));
            }

            // --- Posture dominante (passif) — réduction dégâts reçus (géré dans IncomingDamage) ---
            // Armement de la posture après un coup de mêlée
            int postureRank = acc.getTalentRank(PlayerClass.TIREUR, LancierPassifs.POSTURE_NODE);
            if (postureRank > 0) {
                lancierState.armPosture(uuid, postureRank, LancierPassifs.POSTURE_DURATION_MS);
                if (debug) log.append(" PostureArmed");
            }

            // --- Chasseur de géants — bonus si cible a plus de HP max ---
            int geantsRank = acc.getTalentRank(PlayerClass.TIREUR, LancierPassifs.CHASSEUR_GEANTS_NODE);
            if (geantsRank > 0) {
                float attackerHp = getMaxHp(attackerRef, store);
                float victimHp   = getMaxHp(victimRef, store);
                if (victimHp > attackerHp) {
                    float bonus = LancierPassifs.geantsBonusForRank(geantsRank);
                    amount *= (1f + bonus);
                    if (debug) log.append(String.format(" ChasseurGeants=+%.0f%%", bonus * 100));
                }
            }

            // --- Contrôle de l'espace (passif) — bonus après CC ---
            int controleRank = acc.getTalentRank(PlayerClass.TIREUR, LancierPassifs.CONTROLE_NODE);
            if (controleRank > 0 && lancierState.isCcDamageActive(uuid)) {
                float bonus = LancierPassifs.controleBonusForRank(controleRank);
                amount *= (1f + bonus);
                if (debug) log.append(String.format(" ControleEspace=+%.0f%%", bonus * 100));
            }

            // --- Portée maîtrisée (passif) — bonus selon distance ---
            int porteeRank = acc.getTalentRank(PlayerClass.TIREUR, LancierPassifs.PORTEE_NODE);
            if (porteeRank > 0) {
                double dist = getDistanceBetween(attackerRef, victimRef, store);
                if (dist < LancierPassifs.PORTEE_CLOSE_MAX) {
                    float bonus = LancierPassifs.porteeBonusCloseForRank(porteeRank);
                    amount *= (1f + bonus);
                    if (debug) log.append(String.format(" PorteeClose=+%.0f%%", bonus * 100));
                } else if (dist > LancierPassifs.PORTEE_FAR_MIN) {
                    float bonus = LancierPassifs.porteeBonusFarForRank(porteeRank);
                    amount *= (1f + bonus);
                    if (debug) log.append(String.format(" PorteeFar=+%.0f%%", bonus * 100));
                }
            }

            if (amount != base) damage.setAmount(amount);
            if (debug) { log.append(String.format(" → final=%.1f", amount)); LOG.atInfo().log(log.toString()); }

            // --- Perce-cœur (passif) — saignement sur coup critique ---
            int perceRank = acc.getTalentRank(PlayerClass.TIREUR, LancierPassifs.PERCE_COEUR_NODE);
            if (perceRank > 0 && CombatCritDetection.isCriticalHit(damage)) {
                int ticks = (int)(LancierPassifs.PERCE_BLEED_DURATION_MS / 1000L);
                float dpt = amount * LancierPassifs.perceBleedPctForRank(perceRank);
                bleedSystem.applyBleed(victimRef, dpt, ticks, store);
                if (debug) LOG.atInfo().log("[LancierDmg] PerceCoeur bleed dpt=" + dpt);
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
