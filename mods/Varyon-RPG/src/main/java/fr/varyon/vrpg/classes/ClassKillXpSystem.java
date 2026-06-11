package fr.varyon.vrpg.classes;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ability.ExpertEnDuelSkill;
import fr.varyon.vrpg.classes.berserker.BerserkerPassifs;
import fr.varyon.vrpg.classes.berserker.BerserkerState;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.combat.MobKillXpResolver;
import fr.varyon.vrpg.combat.MobParticipantsTracker;
import fr.varyon.vrpg.config.ClassXpConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassKillXpSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final MobParticipantsTracker participantsTracker;
    private final BerserkerState berserkerState;
    private final Map<UUID, Map<String, KillTracker>> killTrackers = new ConcurrentHashMap<>();
    private Integer healthStatIndex = null;

    public ClassKillXpSystem(@Nonnull ClassManager classManager,
                             @Nonnull MobParticipantsTracker participantsTracker,
                             @Nonnull BerserkerState berserkerState) {
        this.classManager = classManager;
        this.participantsTracker = participantsTracker;
        this.berserkerState = berserkerState;
    }

    public void cleanup(@Nonnull UUID playerId) {
        killTrackers.remove(playerId);
    }

    public MobParticipantsTracker getParticipantsTracker() {
        return participantsTracker;
    }

    public final class KillPredictor extends DamageEventSystem {

        @Override
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Damage damage) {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;
            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;

            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef == null || !attackerRef.isValid()) return;

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
            if (!wouldKill(store, victimRef, damage.getAmount())) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            NPCEntity npc = store.getComponent(victimRef, NPCEntity.getComponentType());
            if (npc == null) return;

            grantKillXp(playerRef, attackerRef, npc, store);
            participantsTracker.markHandledByPrediction(victimRef, playerRef.getUuid());
        }
    }

    public final class ParticipantDeathXp extends DeathSystems.OnDeathSystem {

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent death,
                                     @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
            Ref<EntityStore> victimRef = (Ref<EntityStore>) ref;
            UUID alreadyHandled = participantsTracker.removeHandledByPrediction(victimRef);
            Map<UUID, MobParticipantsTracker.Entry> allParticipants =
                participantsTracker.removeParticipants(victimRef);
            if (allParticipants == null || allParticipants.isEmpty()) return;

            NPCEntity npc = (NPCEntity) store.getComponent(victimRef, NPCEntity.getComponentType());
            if (npc == null) return;

            for (MobParticipantsTracker.Entry entry : allParticipants.values()) {
                if (entry.playerUuid().equals(alreadyHandled)) continue;
                if (!entry.playerRef().isValid()) continue;

                PlayerRef playerRef = (PlayerRef) store.getComponent(entry.playerRef(), PlayerRef.getComponentType());
                if (playerRef == null) playerRef = (PlayerRef) commandBuffer.getComponent(
                    entry.playerRef(), PlayerRef.getComponentType());
                if (playerRef == null) continue;

                try {
                    grantKillXp(playerRef, entry.playerRef(), npc, (Store<EntityStore>) store);
                } catch (Exception e) {
                    LOGGER.atWarning().log("[ClassKillXp] XP error for %s: %s",
                        entry.playerUuid(), e.getMessage());
                }
            }
        }
    }

    // XP bonus factors sourced from ExpertEnDuelSkill

    private void grantKillXp(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> attackerRef,
                             @Nonnull NPCEntity npc, @Nonnull Store<EntityStore> store) {
        UUID uuid = playerRef.getUuid();
        ClassAccount acc = classManager.getOrLoad(uuid);
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return;

        ClassProgress progress = acc.getProgress(activeClass);
        if (progress.isMaxLevel()) return;

        double baseXp = MobKillXpResolver.resolveBaseXp(npc);
        if (baseXp <= 0.0) return;

        String farmKey = MobKillXpResolver.antiFarmKey(npc);
        if (farmKey == null || !checkAntiFarm(uuid, farmKey)) return;

        double mult = 1.0;
        String multReason = null;

        PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (spec == PlayerSpecialization.DUELLISTE) {
            int rank = acc.getTalentRank(activeClass, ExpertEnDuelSkill.TALENT_NODE_ID);
            if (rank > 0) {
                double hpPct = getPlayerHpPercent(attackerRef, store);
                if (hpPct > 0.70) {
                    double bonus = ExpertEnDuelSkill.xpBonusForRank(rank);
                    mult = 1.0 + bonus;
                    multReason = "ExpertEnDuel(rank=" + rank + ", hp=" + String.format("%.0f", hpPct * 100) + "%)=+" + String.format("%.0f", bonus * 100) + "%";
                }
            }
        }

        double finalXp = baseXp * mult;
        LOGGER.atInfo().log("[ClassKillXp] +" + String.format("%.2f", finalXp) + " xp"
            + " (base=" + String.format("%.2f", baseXp) + (multReason != null ? " x" + multReason : "")
            + ") class=" + activeClass + " mob=" + MobKillXpResolver.npcRoleKey(npc));

        classManager.addXp(uuid, activeClass, finalXp, playerRef);

        if (activeClass == PlayerClass.BARBARE
                && acc.getActiveSpec(activeClass) == PlayerSpecialization.BERSERKER) {
            int frenesieRank = acc.getTalentRank(activeClass, BerserkerPassifs.FRENESIE_NODE);
            if (frenesieRank > 0) {
                berserkerState.onKillFrenesie(uuid,
                    BerserkerPassifs.FRENESIE_MAX_STACKS,
                    BerserkerPassifs.frenesieDurationMs(),
                    5000L);
            }
            int fureurRank = acc.getTalentRank(activeClass, BerserkerPassifs.FUREUR_NODE);
            if (fureurRank > 0) {
                berserkerState.triggerFureur(uuid, BerserkerPassifs.fureurDurationMs(), fureurRank);
            }
            int carnageRank = acc.getTalentRank(activeClass, BerserkerPassifs.CARNAGE_NODE);
            if (carnageRank > 0) {
                berserkerState.onKillCarnage(uuid, BerserkerPassifs.CARNAGE_MAX_STACKS, 10000L);
            }
        }
    }

    private double getPlayerHpPercent(@Nonnull Ref<EntityStore> attackerRef,
                                      @Nonnull Store<EntityStore> store) {
        int hIdx = healthStatIndex();
        if (hIdx < 0) return 0.0;
        EntityStatMap stats = store.getComponent(attackerRef, EntityStatMap.getComponentType());
        if (stats == null) return 0.0;
        var hp = stats.get(hIdx);
        if (hp == null) return 0.0;
        float current = hp.get();
        float max = hp.getMax();
        if (max <= 0f) return 0.0;
        return current / max;
    }

    private boolean checkAntiFarm(@Nonnull UUID playerId, @Nonnull String mobKey) {
        int window = ClassXpConfig.getAntiFarmWindowSeconds();
        int maxKills = ClassXpConfig.getAntiFarmMaxKillsPerMob();

        Map<String, KillTracker> playerTrackers = killTrackers.computeIfAbsent(
            playerId, k -> new ConcurrentHashMap<>());
        KillTracker tracker = playerTrackers.computeIfAbsent(mobKey, k -> new KillTracker());

        long now = System.currentTimeMillis();
        if (now - tracker.windowStart > window * 1000L) {
            tracker.windowStart = now;
            tracker.count = 0;
        }
        tracker.count++;
        return tracker.count <= maxKills;
    }

    private boolean wouldKill(@Nonnull Store<EntityStore> store,
                                @Nonnull Ref<EntityStore> victimRef,
                                float incomingDmg) {
        int hIdx = healthStatIndex();
        if (hIdx < 0) return false;
        EntityStatMap targetStats = store.getComponent(victimRef, EntityStatMap.getComponentType());
        if (targetStats == null) return false;
        var healthStat = targetStats.get(hIdx);
        if (healthStat == null) return false;
        return healthStat.get() - incomingDmg <= 0f;
    }

    private int healthStatIndex() {
        if (healthStatIndex == null) {
            try {
                healthStatIndex = DefaultEntityStatTypes.getHealth();
            } catch (Exception e) {
                healthStatIndex = -1;
            }
        }
        return healthStatIndex;
    }

    private static final class KillTracker {
        long windowStart = System.currentTimeMillis();
        int count;
    }
}
