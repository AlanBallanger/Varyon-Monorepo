package fr.varyon.vrpg.classes.bagarreur;

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
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassProgress;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class BagarreurOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final BagarreurState bagarreurState;
    private Integer healthIdx = null;

    public BagarreurOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                          @Nonnull BagarreurState bagarreurState) {
        this.classManager   = classManager;
        this.bagarreurState = bagarreurState;
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
            if (acc.getActiveClass() != PlayerClass.BARBARE) return;
            if (acc.getActiveSpec(PlayerClass.BARBARE) != PlayerSpecialization.BAGARREUR) return;

            if (damage.getAmount() <= 0f) return;

            float base   = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[BagarreurDmg] base=%.1f", base)) : null;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
            long victimIdx = victimRef.getIndex();

            // Montée d'adrénaline — bonus dégâts actif
            float monteeBonus = bagarreurState.getMonteeBonus(uuid);
            if (monteeBonus > 0f) {
                amount *= (1f + monteeBonus);
                if (log != null) log.append(String.format(" Montee=+%.0f%%", monteeBonus * 100));
            }

            // Esprit combatif — bonus si joueur < 50% HP
            int espritRank = acc.getTalentRank(PlayerClass.BARBARE, BagarreurPassifs.ESPRIT_COMBATIF_NODE);
            if (espritRank > 0) {
                float selfHpPct = getSelfHpPercent(attackerRef, store);
                if (selfHpPct > 0f && selfHpPct < BagarreurPassifs.ESPRIT_COMBATIF_THRESHOLD) {
                    float bonus = BagarreurPassifs.espritCombatifBonusForRank(espritRank);
                    amount *= (1f + bonus);
                    if (log != null) log.append(String.format(" EspritCombatif=+%.0f%%", bonus * 100));
                }
            }

            // Acharnement — stacks sur même cible
            int acharnRank = acc.getTalentRank(PlayerClass.BARBARE, BagarreurPassifs.ACHARNEMENT_NODE);
            if (acharnRank > 0) {
                int stacks = bagarreurState.onHitAcharnement(uuid, victimIdx,
                    BagarreurPassifs.ACHARNEMENT_MAX_STACKS, BagarreurPassifs.ACHARNEMENT_WINDOW_MS);
                if (stacks > 0) {
                    float bonus = stacks * BagarreurPassifs.acharnementBonusPerStack(acharnRank);
                    amount *= (1f + bonus);
                    if (log != null) log.append(String.format(" Acharnement(%dx)=+%.0f%%", stacks, bonus * 100));
                }
            }

            // Poings d'acier — tirage crit indépendant → stun
            int poingsRank = acc.getTalentRank(PlayerClass.BARBARE, BagarreurPassifs.POINGS_ACIER_NODE);
            if (poingsRank > 0) {
                fr.varyon.vrpg.classes.ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
                boolean isCrit = stats != null && stats.critChancePct() > 0
                    && Math.random() < stats.critChancePct() / 100.0;
                if (isCrit) {
                    float chance = BagarreurPassifs.poingsAcierChanceForRank(poingsRank);
                    if (Math.random() < chance) {
                        try {
                            int stunIdx2 = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                                .getAssetMap().getIndex("Vrpg_Stun");
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect stunEff =
                                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                                    .getAssetMap().getAsset(stunIdx2);
                            if (stunEff != null) {
                                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                    store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                                if (ec == null) ec = commandBuffer.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                                if (ec != null) ec.addEffect(victimRef, stunEff,
                                    BagarreurPassifs.POINGS_ACIER_STUN_MS / 1000f,
                                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                                if (log != null) log.append(" PoingsAcier(stun)");
                            }
                        } catch (Exception ignored2) {}
                    }
                }
            }

            // Mains nues — multiplicateur bagarreur 100%→5000% selon niveau
            if (isHoldingNothing(playerRef)) {
                ClassProgress prog = acc.getProgress(PlayerClass.BARBARE);
                int level = prog != null ? prog.getLevel() : 1;
                float bareFistMult = (float) ClassStatDefinition.bagarreurBareFistMultiplier(level);
                amount *= bareFistMult;
                if (log != null) log.append(String.format(" BareFist(lvl%d)=x%.1f", level, bareFistMult));
            }

            if (amount != base) damage.setAmount(amount);
            if (log != null) {
                log.append(String.format(" -> %.1f", amount));
                LOG.atInfo().log(log.toString());
            }

        } catch (Exception ignored) {}
    }

    private static boolean isHoldingNothing(@Nonnull PlayerRef playerRef) {
        try {
            InventoryComponent.Hotbar hotbar = playerRef.getComponent(InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return true;
            ItemStack held = hotbar.getInventory().getItemStack((short) hotbar.getActiveSlot());
            return held == null || held.isEmpty();
        } catch (Exception e) { return true; }
    }

    private float getSelfHpPercent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            if (healthIdx == null) {
                try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
            }
            if (healthIdx < 0) return 0f;
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 0f;
            var hp = stats.get(healthIdx);
            if (hp == null || hp.getMax() <= 0) return 0f;
            return hp.get() / hp.getMax();
        } catch (Exception e) { return 0f; }
    }
}
