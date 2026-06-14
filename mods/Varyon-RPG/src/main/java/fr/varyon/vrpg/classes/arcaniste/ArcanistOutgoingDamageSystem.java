package fr.varyon.vrpg.classes.arcaniste;

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
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class ArcanistOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final ArcanistState arcanistState;

    public ArcanistOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                         @Nonnull ArcanistState arcanistState) {
        this.classManager  = classManager;
        this.arcanistState = arcanistState;
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
            if (attackerRef == null || !attackerRef.isValid()) return;

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.MAGE) return;
            if (acc.getActiveSpec(PlayerClass.MAGE) != PlayerSpecialization.ARCANISTE) return;

            float base   = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[ArcanisteDmg] base=%.1f", base)) : null;

            // Staff : x8 appliqué ici sauf pour COMMAND (Nova de Givre l'a déjà intégré dans le cast)
            if (fr.varyon.vrpg.classes.WeaponCategory.heldCategory(playerRef)
                    == fr.varyon.vrpg.classes.WeaponCategory.MAGIE
                    && damage.getCause() != com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.COMMAND) {
                amount *= 8f;
                if (log != null) log.append(" Staff=x8");
            }

            // Surcharge — bonus dégâts sorts actif
            float surchargeBonus = arcanistState.getSurchargeBonus(uuid);
            if (surchargeBonus > 0f) {
                amount *= (1f + surchargeBonus);
                if (log != null) log.append(String.format(" Surcharge=+%.0f%%", surchargeBonus * 100));
            }

            // Pouvoir Grandissant — bonus si pas pris de dégâts
            int pouvoirRank = acc.getTalentRank(PlayerClass.MAGE, ArcanistPassifs.POUVOIR_GRANDISSANT_NODE);
            if (pouvoirRank > 0 && arcanistState.isPouvoirGrandissantActive(uuid)) {
                float bonus = ArcanistPassifs.pouvoirGrandissantBonusForRank(pouvoirRank);
                amount *= (1f + bonus);
                if (log != null) log.append(String.format(" PouvoirGrandissant=+%.0f%%", bonus * 100));
            }

            // Détermine le type d'élément selon la cause
            // Fire : Météore (cause Fire) + BouleDeFeu (cause PROJECTILE = feu)
            // Ice  : Nova (cause Ice) + Salve (cause PROJECTILE = glace) -> on distingue via l'état actif
            com.hypixel.hytale.server.core.modules.entity.damage.DamageCause fireCause =
                (com.hypixel.hytale.server.core.modules.entity.damage.DamageCause)
                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.getAssetMap().getAsset("Fire");
            com.hypixel.hytale.server.core.modules.entity.damage.DamageCause iceCause =
                (com.hypixel.hytale.server.core.modules.entity.damage.DamageCause)
                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.getAssetMap().getAsset("Ice");
            boolean isFire = (fireCause != null && damage.getCause() == fireCause)
                || (damage.getCause() == com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PROJECTILE
                    && arcanistState.isLastCastFire(uuid))
                || (damage.getCause() == com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.ENVIRONMENT
                    && arcanistState.isLastCastFire(uuid));
            String kindNormal = isFire ? "BURN" : "ICE";
            String kindCrit   = isFire ? "BURN_CRITICAL" : "ICE_CRITICAL";

            fr.varyon.vrpg.classes.ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
            if (stats != null && stats.critChancePct() > 0
                    && Math.random() < stats.critChancePct() / 100.0) {
                float critMult = 1.0f + stats.critDamagePct() / 100.0f;
                float finalAmt = amount * critMult;
                damage.setAmount(finalAmt);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, chunk.getReferenceTo(index), finalAmt, kindCrit);
            } else {
                if (amount != base) damage.setAmount(amount);
                fr.varyon.vrpg.integration.DamageFloatBridge.markSkipCombatText(damage);
                fr.varyon.vrpg.integration.DamageFloatBridge.emit(store, chunk.getReferenceTo(index), amount, kindNormal);
            }

            if (log != null) {
                log.append(String.format(" -> %.1f", amount));
                LOG.atInfo().log(log.toString());
            }
        } catch (Exception ignored) {}
    }
}
