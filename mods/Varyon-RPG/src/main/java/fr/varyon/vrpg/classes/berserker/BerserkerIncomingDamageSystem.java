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
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.ability.ClassSkillCooldowns;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class BerserkerIncomingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final BerserkerState berserkerState;
    private final BerserkerCombatTracker combatTracker;
    private final ClassSkillCooldowns cooldowns;
    private Integer healthIdx = null;

    public BerserkerIncomingDamageSystem(@Nonnull ClassManager classManager,
                                          @Nonnull BerserkerState berserkerState,
                                          @Nonnull BerserkerCombatTracker combatTracker,
                                          @Nonnull ClassSkillCooldowns cooldowns) {
        this.classManager   = classManager;
        this.berserkerState = berserkerState;
        this.combatTracker  = combatTracker;
        this.cooldowns      = cooldowns;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.BARBARE) return;
            if (acc.getActiveSpec(PlayerClass.BARBARE) != PlayerSpecialization.BERSERKER) return;

            combatTracker.markCombat(uuid);

            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            // Dernier Souffle — ignore les coups létaux pendant la durée
            int dernierRank = acc.getTalentRank(PlayerClass.BARBARE, BerserkerPassifs.DERNIER_SOUFFLE_NODE);
            if (dernierRank > 0) {
                if (berserkerState.isDernierSouffleActive(uuid)) {
                    // Actif — on bloque les dégâts mortels
                    float currentHp = getCurrentHp(chunk, index, store);
                    if (currentHp > 0f && damage.getAmount() >= currentHp) {
                        damage.setAmount(currentHp - 1f);
                        if (VrpgConfig.isDebugCombat())
                            LOG.atInfo().log("[BerserkerRecu] DernierSouffle actif — coup mortel réduit");
                    }
                } else {
                    // Pas actif — vérifier si ce coup est mortel et déclencher
                    float currentHp = getCurrentHp(chunk, index, store);
                    if (currentHp > 0f && damage.getAmount() >= currentHp) {
                        boolean onCd = cooldowns.isOnCooldown(uuid,
                            BerserkerPassifs.DERNIER_SOUFFLE_NODE,
                            BerserkerPassifs.dernierSouffleCooldownMsForRank(dernierRank));
                        if (!onCd) {
                            berserkerState.startDernierSouffle(uuid, BerserkerPassifs.DERNIER_SOUFFLE_DURATION_MS);
                            cooldowns.markUsed(uuid, BerserkerPassifs.DERNIER_SOUFFLE_NODE);
                            damage.setAmount(currentHp - 1f);
                            if (VrpgConfig.isDebugCombat())
                                LOG.atInfo().log("[BerserkerRecu] DernierSouffle déclenché (" +
                                    (BerserkerPassifs.DERNIER_SOUFFLE_DURATION_MS / 1000) + "s)");
                        }
                    }
                }
            }

        } catch (Exception ignored) {}
    }

    private float getCurrentHp(@Nonnull ArchetypeChunk<EntityStore> chunk, int index,
                                @Nonnull Store<EntityStore> store) {
        try {
            if (healthIdx == null) {
                try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
            }
            if (healthIdx < 0) return 1f;
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 1f;
            var hp = stats.get(healthIdx);
            return hp != null ? hp.get() : 1f;
        } catch (Exception e) { return 1f; }
    }
}
