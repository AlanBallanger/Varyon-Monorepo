package fr.varyon.vrpg.classes.duelliste;

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
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.integration.DamageFloatBridge;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class DuellisteOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final DuellisteState state;
    private final DuellisteBleedSystem bleedSystem;
    private int healthIdx = Integer.MIN_VALUE;

    public DuellisteOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                         @Nonnull DuellisteState state,
                                         @Nonnull DuellisteBleedSystem bleedSystem) {
        this.classManager = classManager;
        this.state = state;
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
            if (acc.getActiveClass() != PlayerClass.GUERRIER) return;
            if (acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.DUELLISTE) return;

            // Feinte — prochain coup imparable (bypass BLOCKED), agit même sur les coups bloqués (amount==0)
            int feinteRank = acc.getTalentRank(PlayerClass.GUERRIER, FeintSkill.TALENT_NODE_ID);
            if (feinteRank > 0 && state.consumeFeinte(uuid)) {
                damage.putMetaObject(Damage.BLOCKED, Boolean.FALSE);
                if (damage.getAmount() <= 0f) damage.setAmount(damage.getInitialAmount());
            }

            if (damage.getAmount() <= 0f) return;

            float base = damage.getAmount();
            float amount = base;
            StringBuilder log = new StringBuilder();
            log.append(String.format("[Dmg] base=%.1f", base));

            // Coup d'Estoc — prochain coup armé
            float coupEstocMult = state.consumeCoupEstoc(uuid);
            if (coupEstocMult > 0f) {
                amount *= coupEstocMult;
                log.append(String.format(" CoupEstoc=x%.2f", coupEstocMult));
            }

            // Assaut du Bretteur — outgoing damage bonus
            if (state.isAssautBretteurActive(uuid)) {
                int rank = acc.getTalentRank(PlayerClass.GUERRIER, AssautBretteurSkill.TALENT_NODE_ID);
                if (rank > 0) {
                    float mult = AssautBretteurSkill.damageBonusForRank(rank);
                    amount *= (1.0f + mult);
                    log.append(String.format(" AssautBretteur=+%.0f%%", mult * 100));
                }
            }

            // Frappe Précise — bonus si crit détecté via particules d'impact
            boolean isCrit = isCriticalHit(damage);
            if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
                LOG.atInfo().log("[FrappePrecise] isCrit=" + isCrit + " base=" + base + " initial=" + damage.getInitialAmount());
            if (isCrit) {
                int rank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.FRAPPE_NODE);
                if (rank > 0) {
                    float mult = DuellistePassifs.critBonusForRank(rank);
                    amount *= (1.0f + mult);
                    log.append(String.format(" FrappePrecise=+%.0f%%", mult * 100));
                } else {
                    log.append(" CRIT");
                }
            }

            // Contre-Attaque — bonus after parry
            if (state.hasContreAttaqueBuff(uuid)) {
                int rank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.CONTRE_NODE);
                if (rank > 0) {
                    float mult = DuellistePassifs.contreBonusForRank(rank);
                    amount *= (1.0f + mult);
                    log.append(String.format(" ContreAttaque=+%.0f%%", mult * 100));
                    state.consumeContreAttaque(uuid);
                    try {
                        com.hypixel.hytale.server.core.entity.AnimationUtils.playAnimation(
                            attackerRef, com.hypixel.hytale.protocol.AnimationSlot.Action,
                            "Sword", "SwingLeft", true, store);
                        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent ctc =
                            store.getComponent(attackerRef,
                                com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                        if (ctc != null) {
                            fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                                "SFX_Sword_T2_Swing", playerRef, ctc.getPosition(), commandBuffer);
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Momentum — stacking damage bonus
            int momentumRank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.MOMENTUM_NODE);
            if (momentumRank > 0) {
                int stacks = state.incrementMomentum(uuid, DuellistePassifs.MOMENTUM_MAX_STACKS);
                if (stacks > 0) {
                    float mult = stacks * DuellistePassifs.momentumBonusPerStack(momentumRank);
                    amount *= (1.0f + mult);
                    log.append(String.format(" Momentum(%dx)=+%.0f%%", stacks, mult * 100));
                }
            }

            if (amount != base) damage.setAmount(amount);
            log.append(String.format(" → final=%.1f", amount));
            if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) LOG.atInfo().log(log.toString());

            // Blessure Ouverte — chance to apply bleed
            int bleedRank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.BLESSURE_NODE);
            if (bleedRank > 0 && Math.random() < DuellistePassifs.bleedChanceForRank(bleedRank)) {
                Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float weaponBase = weaponDmg > 0 ? (float) weaponDmg : 1f;
                bleedSystem.applyBleed(victimRef, weaponBase, store);
                if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
                    LOG.atInfo().log("[Dmg] BlessureOuverte dpt=" +
                        String.format("%.1f", weaponBase * DuellistePassifs.BLEED_WEAPON_PCT) + "/s");
            }

        } catch (Exception ignored) {}
    }

    private static boolean isCriticalHit(@Nonnull Damage damage) {
        try {
            boolean[] found = {false};
            java.util.List<String> debugValues = fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()
                ? new java.util.ArrayList<>() : null;
            damage.forEachMetaObject(new com.hypixel.hytale.server.core.meta.IMetaStore.MetaEntryConsumer() {
                @Override
                public <T> void accept(int metaId, T value) {
                    if (value == null) return;
                    String s = value.toString();
                    if (debugValues != null) debugValues.add(metaId + "=" + s.substring(0, Math.min(60, s.length())));
                    String sl = s.toLowerCase(java.util.Locale.ROOT);
                    if (!found[0] && (sl.contains("impact_critical") || (sl.contains("critical")
                            && (sl.contains("particle") || sl.contains("systemid"))))) {
                        found[0] = true;
                    }
                }
            });
            if (debugValues != null)
                LOG.atInfo().log("[FrappePrecise] meta entries: " + debugValues);
            return found[0];
        } catch (Exception ignored) {}
        return false;
    }

    private float getMaxHp(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (healthIdx == Integer.MIN_VALUE) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        if (healthIdx < 0) return 0f;
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return 0f;
        var hp = stats.get(healthIdx);
        return hp != null ? hp.getMax() : 0f;
    }
}
