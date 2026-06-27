package fr.varyon.vrpg.classes.berserker;

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
import fr.varyon.vrpg.classes.duelliste.DuellisteBleedSystem;
import fr.varyon.vrpg.config.VrpgConfig;
import fr.varyon.vrpg.integration.DamageFloatBridge;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class BerserkerOutgoingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final BerserkerState berserkerState;
    private final BerserkerCombatTracker combatTracker;
    private final DuellisteBleedSystem bleedSystem;
    private Integer healthIdx = null;

    public BerserkerOutgoingDamageSystem(@Nonnull ClassManager classManager,
                                          @Nonnull BerserkerState berserkerState,
                                          @Nonnull BerserkerCombatTracker combatTracker,
                                          @Nonnull DuellisteBleedSystem bleedSystem) {
        this.classManager   = classManager;
        this.berserkerState = berserkerState;
        this.combatTracker  = combatTracker;
        this.bleedSystem    = bleedSystem;
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
            if (acc.getActiveSpec(PlayerClass.BARBARE) != PlayerSpecialization.BERSERKER) return;

            combatTracker.markCombat(uuid);

            if (damage.getAmount() <= 0f) return;

            float base   = damage.getAmount();
            float amount = base;
            boolean debug = VrpgConfig.isDebugCombat();
            StringBuilder log = debug ? new StringBuilder(String.format("[BerserkerDmg] base=%.1f", base)) : null;

            // Éviscération — prochain coup armé
            int eviscRank = berserkerState.consumeEvisc(uuid);
            float lifestealAmount = 0f;
            if (eviscRank > 0) {
                float mult = DechiquetageSkill.damagePctForRank(eviscRank);
                float lifestealPct = DechiquetageSkill.lifestealPctForRank(eviscRank);
                lifestealAmount = amount * mult * lifestealPct;
                amount *= mult;
                if (log != null) log.append(String.format(" Evisc=x%.2f lifesteal=%.1f", mult, lifestealAmount));
            }

            // Exécution Sauvage — prochain coup armé
            // (armé via ClassSkillService, le multiplicateur est passé via le state)
            // On vérifie directement le state ici si l'attaque est une exécution
            // Note: ExecutionSauvage utilise le même mécanisme qu'ÉviscérationSkill

            // Cri de Ralliement — bonus dégâts actif
            float criBonus = berserkerState.getCriRalliementBonus(uuid);
            if (criBonus > 0f) {
                amount *= (1f + criBonus);
                if (log != null) log.append(String.format(" Cri=+%.0f%%", criBonus * 100));
            }

            // Ferveur Guerrière — stacks dégâts passif
            int ferveurRank = acc.getTalentRank(PlayerClass.BARBARE, BerserkerPassifs.FERVEUR_NODE);
            if (ferveurRank > 0 && combatTracker.isInCombat(uuid)) {
                int stacks = berserkerState.getFerveurStacks(uuid);
                if (stacks > 0) {
                    float fervMult = stacks * BerserkerPassifs.ferveurBonusPerStack(ferveurRank);
                    amount *= (1f + fervMult);
                    if (log != null) log.append(String.format(" Ferveur(%dx)=+%.0f%%", stacks, fervMult * 100));
                }
            }

            // Frénésie — stacks dégâts passif après kill
            int frenesieRank = acc.getTalentRank(PlayerClass.BARBARE, BerserkerPassifs.FRENESIE_NODE);
            if (frenesieRank > 0) {
                int stacks = berserkerState.getFrenesieStacks(uuid);
                if (stacks > 0) {
                    float frenMult = stacks * BerserkerPassifs.frenesieDmgBonusPerStack(frenesieRank);
                    amount *= (1f + frenMult);
                    if (log != null) log.append(String.format(" Frenesie(%dx)=+%.0f%%", stacks, frenMult * 100));
                }
            }

            // Fureur Sanguinaire — vol de vie actif
            int fureurRank = berserkerState.getFureurRank(uuid);
            if (fureurRank > 0) {
                float stealPct = BerserkerPassifs.fureurLifestealPct(fureurRank);
                lifestealAmount += amount * stealPct;
                if (log != null) log.append(String.format(" Fureur=+%.1fHP", amount * stealPct));
            }

            if (amount != base) damage.setAmount(amount);
            if (log != null) {
                log.append(String.format(" -> %.1f", amount));
                LOG.atInfo().log(log.toString());
            }

            // Appliquer le vol de vie
            if (lifestealAmount > 0f) {
                healAttacker(attackerRef, store, lifestealAmount);
            }

            // Blessures Profondes — chance saignement passif
            int blessuresRank = acc.getTalentRank(PlayerClass.BARBARE, BerserkerPassifs.BLESSURES_NODE);
            if (blessuresRank > 0 && Math.random() < BerserkerPassifs.bleedChanceForRank(blessuresRank)) {
                Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float weaponBase = weaponDmg > 0 ? (float) weaponDmg : 1f;
                bleedSystem.applyBleed(victimRef, weaponBase, BerserkerPassifs.BLEED_WEAPON_PCT, store);
                if (debug) LOG.atInfo().log("[BerserkerDmg] BlessuresProfondes saignement dpt=" +
                    String.format("%.1f", weaponBase * BerserkerPassifs.BLEED_WEAPON_PCT) + "/s");
            }

        } catch (Exception ignored) {}
    }

    private void healAttacker(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              float amount) {
        try {
            if (healthIdx == null) {
                try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
            }
            if (healthIdx < 0) return;
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return;
            var hp = stats.get(healthIdx);
            if (hp == null || hp.getMax() <= 0) return;
            stats.setStatValue(healthIdx, Math.min(hp.getMax(), hp.get() + amount));
        } catch (Exception ignored) {}
    }
}
