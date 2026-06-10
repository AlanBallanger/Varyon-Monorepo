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
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass;

public final class OmbreIncomingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG = forEnclosingClass();

    private final ClassManager classManager;
    private final OmbreState ombreState;
    private int healthIdx = Integer.MIN_VALUE;

    public OmbreIncomingDamageSystem(@Nonnull ClassManager classManager,
                                     @Nonnull OmbreState ombreState) {
        this.classManager = classManager;
        this.ombreState = ombreState;
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
            if (damage.isCancelled()) return;

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.GUERRIER) return;
            if (acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.OMBRE) return;

            float incoming = damage.getAmount();
            boolean debug = VrpgConfig.isDebugCombat();

            // Écran de fumée / Pas des Ténèbres — invisibilité bloque les dégâts
            if (ombreState.isInStealth(uuid)) {
                damage.setAmount(0f);
                damage.putMetaObject(Damage.BLOCKED, Boolean.TRUE);
                if (debug) LOG.atInfo().log(String.format("[OmbreRecu] %.1f BLOQUÉ (stealth actif)", incoming));
                return;
            }

            if (incoming <= 0f) return;

            // Ombre Insaisissable — esquive passive
            int insaisissableRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.OMBRE_INSAISISSABLE_NODE);
            float dodgeChance = insaisissableRank > 0 ? OmbrePassifs.dodgeBonusForRank(insaisissableRank) : 0f;

            // Instinct de Survie — esquive bonus sous 30% HP
            int survieRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.INSTINCT_SURVIE_NODE);
            if (survieRank > 0) {
                float hpRatio = getHpRatio(playerRef, store, commandBuffer, chunk, index);
                if (hpRatio < OmbrePassifs.survieHpThreshold()) {
                    dodgeChance += OmbrePassifs.survieDodgeBonusForRank(survieRank);
                    if (debug) LOG.atInfo().log(String.format("[OmbreRecu] InstinctSurvie +%.0f%% esquive (HP %.0f%%)",
                        OmbrePassifs.survieDodgeBonusForRank(survieRank) * 100, hpRatio * 100));
                }
            }

            if (dodgeChance > 0f && Math.random() < dodgeChance) {
                damage.setAmount(0f);
                try {
                    fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                        fr.varyon.vrpg.audio.ClassSkillSounds.ESQUIVE_BRETTEUR_SOUND,
                        playerRef, new org.joml.Vector3d(), commandBuffer);
                } catch (Exception ignored2) {}
                if (debug) LOG.atInfo().log(String.format("[OmbreRecu] %.1f ESQUIVÉ (%.0f%% chance)", incoming, dodgeChance * 100));
            }

        } catch (Exception ignored) {}
    }

    private float getHpRatio(@Nonnull PlayerRef playerRef,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer,
                              @Nonnull ArchetypeChunk<EntityStore> chunk, int index) {
        if (healthIdx == Integer.MIN_VALUE) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        if (healthIdx < 0) return 1f;
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 1f;
            var hp = stats.get(healthIdx);
            if (hp == null || hp.getMax() <= 0) return 1f;
            return hp.get() / hp.getMax();
        } catch (Exception e) {
            return 1f;
        }
    }
}
