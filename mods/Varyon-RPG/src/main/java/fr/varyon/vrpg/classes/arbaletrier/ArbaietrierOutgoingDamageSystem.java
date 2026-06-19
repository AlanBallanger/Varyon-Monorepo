package fr.varyon.vrpg.classes.arbaletrier;

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
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public final class ArbaietrierOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final Set<Dependency<EntityStore>> DEPENDENCIES =
        Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class));

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
            if (acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.ARBALETRIER) return;

            if (damage.getAmount() <= 0f) return;

            float base = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[ArbaietrierDmg] base=%.1f", base)) : null;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

            // Carreaux custom en vol (Lourd, Explosif, Transpercant) — remplace le 1 dégât symbolique du JSON
            int carreauType = arbaState.getPendingCarreauType(uuid);
            if (carreauType > 0) {
                int carreauRankSaved = arbaState.getPendingCarreauRank(uuid);
                boolean miseEnJouSaved = arbaState.isPendingMiseEnJouActive(uuid);
                float carreauDmg = arbaState.consumePendingCarreauDmg(uuid);
                if (carreauDmg > 0f) {
                    amount = carreauDmg;
                    damage.setAmount(amount);
                    if (debug) log.append(String.format(" Carreau(%d)=%.1f", carreauType, carreauDmg));
                }
                if (miseEnJouSaved) {
                    fr.varyon.vrpg.integration.DamageFloatBridge.markCritical(damage);
                    if (debug) log.append(" MiseEnJou(crit)");
                }
                if (carreauType == ArbaietrierState.CARREAU_TYPE_TRANSPERCANT) {
                    arbaState.clearPendingCarreau(uuid);
                    int pierceLeft = CarreauTranspercantSkill.pierceCountForRank(carreauRankSaved) - 1;
                    if (pierceLeft > 0) {
                        TransformComponent tcAttacker = store.getComponent(attackerRef, TransformComponent.getComponentType());
                        TransformComponent tcVictim   = store.getComponent(victimRef,   TransformComponent.getComponentType());
                        if (tcAttacker != null && tcVictim != null) {
                            org.joml.Vector3d shotDir = new org.joml.Vector3d(tcVictim.getPosition()).sub(tcAttacker.getPosition());
                            double shotLen = shotDir.length();
                            if (shotLen > 1e-6) {
                                shotDir.mul(1.0 / shotLen);
                                final org.joml.Vector3d fDir = shotDir;
                                final org.joml.Vector3d fOrigin = tcVictim.getPosition();
                                final float fAmount = amount;
                                final Ref<EntityStore> fAttackerRef = attackerRef;
                                final int fVictimIdx = victimRef.getIndex();
                                final double coneCos = Math.cos(Math.toRadians(15.0));
                                final double maxDist = 20.0;
                                java.util.List<double[]> pd = new java.util.ArrayList<>();
                                java.util.List<com.hypixel.hytale.component.Ref<EntityStore>> pr = new java.util.ArrayList<>();
                                try {
                                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                                        .selectNearbyEntities(store, fOrigin, (float) maxDist, t -> {
                                            try {
                                                if (t.getIndex() == fVictimIdx || t.getIndex() == fAttackerRef.getIndex()) return;
                                                if (store.getComponent(t, NPCEntity.getComponentType()) == null) return;
                                                TransformComponent ttc = store.getComponent(t, TransformComponent.getComponentType());
                                                if (ttc == null) return;
                                                org.joml.Vector3d toT = new org.joml.Vector3d(ttc.getPosition()).sub(fOrigin);
                                                double d = toT.length();
                                                if (d < 0.1) return;
                                                double dot = toT.dot(fDir) / d;
                                                if (dot < coneCos) return;
                                                pd.add(new double[]{d});
                                                pr.add(t);
                                            } catch (Exception ignored2) {}
                                        }, t -> true);
                                } catch (Exception ignored2) {}
                                java.util.List<Integer> order = new java.util.ArrayList<>();
                                for (int i = 0; i < pr.size(); i++) order.add(i);
                                order.sort((a, b) -> Double.compare(pd.get(a)[0], pd.get(b)[0]));
                                int hit = 0;
                                for (int i : order) {
                                    if (hit >= pierceLeft) break;
                                    try {
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                            pr.get(i), store,
                                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(fAttackerRef),
                                                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, fAmount));
                                        hit++;
                                    } catch (Exception ignored2) {}
                                }
                            }
                        }
                    }
                } else {
                    arbaState.clearPendingCarreau(uuid);
                }

                if (carreauType == ArbaietrierState.CARREAU_TYPE_EXPLOSIF) {
                    double radius = CarreauExplosifSkill.radius();
                    TransformComponent tcVictim = store.getComponent(victimRef, TransformComponent.getComponentType());
                    LOG.atInfo().log("[CarreauExplosif] hit NPC victimRef=" + victimRef.getIndex() + " tcVictim=" + (tcVictim != null) + " amount=" + amount + " radius=" + radius);
                    if (tcVictim != null) {
                        final org.joml.Vector3d center = tcVictim.getPosition();
                        final float aoeAmount = amount;
                        final Ref<EntityStore> fAttackerRef = attackerRef;
                        try {
                            int sndIdx = com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.getAssetMap().getIndex("SFX_Vrpg_Explosion");
                            LOG.atInfo().log("[CarreauExplosif] sndIdx=" + sndIdx);
                            if (sndIdx > 0) com.hypixel.hytale.server.core.universe.world.SoundUtil.playSoundEvent3d(
                                sndIdx, com.hypixel.hytale.protocol.SoundCategory.SFX, center.x, center.y, center.z, commandBuffer);
                        } catch (Exception e2) { LOG.atWarning().log("[CarreauExplosif] sound error: " + e2.getMessage()); }
                        try {
                            java.util.HashSet<Integer> hit = new java.util.HashSet<>();
                            hit.add(victimRef.getIndex());
                            final java.util.concurrent.atomic.AtomicInteger aoeHit = new java.util.concurrent.atomic.AtomicInteger(0);
                            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                                .selectNearbyEntities(store, center, (float) radius, t -> {
                                    try {
                                        if (!hit.add(t.getIndex())) return;
                                        aoeHit.incrementAndGet();
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(t, store,
                                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(fAttackerRef),
                                                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, aoeAmount));
                                    } catch (Exception e2) { LOG.atWarning().log("[CarreauExplosif] aoe dmg error: " + e2.getMessage()); }
                                }, t -> t.getIndex() != fAttackerRef.getIndex());
                            LOG.atInfo().log("[CarreauExplosif] aoe hit " + aoeHit.get() + " extra targets");
                        } catch (Exception e2) { LOG.atWarning().log("[CarreauExplosif] aoe error: " + e2.getMessage()); }
                    }
                }

                // passifs communs (saignement, embusqué, etc.) s'appliquent après
            } else {
                // Tir natif de l'arbalète — consomme Carreau Lourd et Mise en Joue si armés
                int carreauLourdRank = arbaState.consumeCarreauLourd(uuid);
                if (carreauLourdRank > 0) {
                    amount *= (1f + CarreauLourdSkill.damageBonusForRank(carreauLourdRank));
                    damage.setAmount(amount);
                    if (debug) log.append(String.format(" CarreauLourd(r%d)=+%.0f%%", carreauLourdRank,
                        CarreauLourdSkill.damageBonusForRank(carreauLourdRank) * 100));
                }
                int miseEnJouRank = arbaState.consumeMiseEnJou(uuid);
                if (miseEnJouRank > 0) {
                    amount *= (1f + MiseEnJouSkill.damageBonusForRank(miseEnJouRank));
                    damage.setAmount(amount);
                    fr.varyon.vrpg.integration.DamageFloatBridge.markCritical(damage);
                    if (debug) log.append(String.format(" MiseEnJou(r%d,crit)", miseEnJouRank));
                }
            }

            // Recul tactique — bonus prochain tir natif
            float reculBonus = arbaState.consumeReculBonus(uuid);
            if (reculBonus > 0f) {
                amount *= (1f + reculBonus);
                if (debug) log.append(String.format(" ReculTactique=+%.0f%%", reculBonus * 100));
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

            // Coup de Botte — knockback horizontal via commandBuffer
            double[] kbPending = arbaState.consumePendingCoupDeBotteKb(uuid);
            if (kbPending != null) {
                com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent kbComp =
                    new com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent();
                kbComp.setVelocity(new org.joml.Vector3d(kbPending[0], 0.0, kbPending[1]));
                kbComp.setVelocityType(com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                kbComp.setDuration(0.0f);
                commandBuffer.putComponent(victimRef, com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent.getComponentType(), kbComp);
                LOG.atInfo().log("[CoupDeBotte] commandBuffer.putComponent KB vx=" + kbPending[0] + " vz=" + kbPending[1]);
            }

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
