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
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class GardienDeGaiaOutgoingDamageSystem extends DamageEventSystem {

    private static final float STAFF_BASE_MULT = 10f;

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

            // Dégâts du tréant — seulement si l'attaquant est un tréant enregistré
            if (state.isKnownTreantRef(attackerRef)) {
                float treantDmg = state.getTreantDmgFactorForRef(attackerRef);
                if (damage.getAmount() > 0f) {
                    float baseDmg = damage.getAmount();
                    float finalDmg = baseDmg * treantDmg;
                    damage.setAmount(finalDmg);
                    if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
                        LOG.atInfo().log(String.format("[TreantHit] base=%.1f factor=%.2f final=%.1f", baseDmg, treantDmg, finalDmg));
                    fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                    fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, chunk.getReferenceTo(index), finalDmg, "NATURE");

                    UUID ownerUuid = state.getOwnerForTreantRef(attackerRef);
                    if (ownerUuid != null) {
                        try {
                            ClassAccount ownerAcc = classManager.getOrLoad(ownerUuid);
                            int lienRank = ownerAcc.getTalentRank(PlayerClass.MAGE, GardienDeGaiaPassifs.LIEN_NODE);
                            if (lienRank > 0) {
                                float healAmt = finalDmg * GardienDeGaiaPassifs.lienRatioForRank(lienRank);
                                com.hypixel.hytale.server.core.universe.PlayerRef ownerRef =
                                    com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(ownerUuid);
                                if (ownerRef != null) {
                                    int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                                    com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                                        ownerRef.getComponent(com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                                    if (sm != null) {
                                        var hp = sm.get(hIdx);
                                        if (hp != null) sm.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + healAmt));
                                    }
                                }
                            }
                        } catch (Exception ignored2) {}
                    }
                }
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
            float base   = damage.getAmount();
            float amount = base;
            boolean debug = fr.varyon.vrpg.config.VrpgConfig.isDebugCombat();
            String causeId = damage.getCause() != null ? damage.getCause().getId() : "?";
            StringBuilder log = debug ? new StringBuilder(String.format("[GardienDmg] cause=%s base=%.1f", causeId, base)) : null;

            if (fr.varyon.vrpg.classes.WeaponCategory.heldCategory(playerRef)
                    == fr.varyon.vrpg.classes.WeaponCategory.MAGIE
                    && damage.getCause() != com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND) {
                int level = acc.getProgress(PlayerClass.MAGE).getLevel();
                float levelMult = (float) ClassStatDefinition.atkDisplayMultiplier(level, PlayerSpecialization.GARDIEN_DE_GAIA);
                amount *= STAFF_BASE_MULT * levelMult;
                if (log != null) log.append(String.format(" Staff=x%.0f Lvl=x%.2f", STAFF_BASE_MULT, levelMult));
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
