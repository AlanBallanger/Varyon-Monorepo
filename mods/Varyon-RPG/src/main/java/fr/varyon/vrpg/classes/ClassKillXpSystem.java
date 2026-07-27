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
import fr.varyon.vrpg.classes.ravageur.RavageurPassifs;
import fr.varyon.vrpg.classes.ravageur.RavageurState;
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
    private final RavageurState ravageurState;
    private fr.varyon.vrpg.classes.arcaniste.ArcanistState arcanistState;
    private final Map<UUID, Map<String, KillTracker>> killTrackers = new ConcurrentHashMap<>();
    private Integer healthStatIndex = null;

    public ClassKillXpSystem(@Nonnull ClassManager classManager,
                             @Nonnull MobParticipantsTracker participantsTracker,
                             @Nonnull BerserkerState berserkerState,
                             @Nonnull RavageurState ravageurState) {
        this.classManager = classManager;
        this.participantsTracker = participantsTracker;
        this.berserkerState = berserkerState;
        this.ravageurState = ravageurState;
    }

    public void setArcanistState(@Nonnull fr.varyon.vrpg.classes.arcaniste.ArcanistState arcanistState) {
        this.arcanistState = arcanistState;
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

            onKill(playerRef);
            grantKillXp(playerRef, attackerRef, victimRef, npc, store);
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
                    onKill(playerRef);
                    grantKillXp(playerRef, entry.playerRef(), victimRef, npc, (Store<EntityStore>) store);
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
        grantKillXp(playerRef, attackerRef, null, npc, store);
    }

    private void grantKillXp(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> attackerRef,
                             @Nullable Ref<EntityStore> victimRef,
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
        if (spec == PlayerSpecialization.RAVAGEUR) {
            int moissonneurRank = acc.getTalentRank(activeClass, RavageurPassifs.MOISSONNEUR_NODE);
            if (moissonneurRank > 0) {
                int stacks = ravageurState.getMoissonneurStacks(uuid);
                if (stacks > 0) {
                    double bonus = stacks * RavageurPassifs.moissonneurXpBonusPerStack(moissonneurRank);
                    mult = 1.0 + bonus;
                    multReason = "Moissonneur(" + stacks + "x)=+" + String.format("%.0f", bonus * 100) + "%";
                }
            }
        }
        if (spec == PlayerSpecialization.BAGARREUR) {
            int jusquRank = acc.getTalentRank(activeClass, fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.JUSQUAU_BOUT_NODE);
            if (jusquRank > 0) {
                double hpPct = getPlayerHpPercent(attackerRef, store);
                if (hpPct > 0 && hpPct < fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.JUSQUAU_BOUT_THRESHOLD) {
                    double bonus = fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.jusquAuBoutBonusForRank(jusquRank);
                    mult = 1.0 + bonus;
                    multReason = "JusquAuBout(hp=" + String.format("%.0f", hpPct * 100) + "%)=+" + String.format("%.0f", bonus * 100) + "%";
                }
            }
        }
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
        if (spec == PlayerSpecialization.VAUDOU) {
            int feticheurRank = acc.getTalentRank(activeClass, fr.varyon.vrpg.classes.vaudou.VaudouPassifs.FETICHEUR_NODE);
            if (feticheurRank > 0 && victimRef != null) {
                double dist = getDistanceBetween(attackerRef, victimRef, store);
                if (dist >= 0 && dist <= fr.varyon.vrpg.classes.vaudou.VaudouPassifs.FETICHEUR_RANGE) {
                    double bonus = fr.varyon.vrpg.classes.vaudou.VaudouPassifs.feticheurXpBonusForRank(feticheurRank);
                    mult = 1.0 + bonus;
                    multReason = "Feticheur(dist=" + String.format("%.1f", dist) + ")=+" + String.format("%.0f", bonus * 100) + "%";
                }
            }
        }

        double zoneMultiplier = 1.0;
        double levelFactor = 1.0;
        String zoneReason = null;
        if (fr.varyon.vrpg.integration.VaryonZoneBridge.isPresent()) {
            var pos = getPosition(attackerRef, store);
            String worldName = getWorldName(store);
            if (pos != null && worldName != null) {
                boolean isVaryonWorld = fr.varyon.vrpg.integration.VaryonZoneBridge.isVaryonWorld(worldName);
                boolean isHaven = !isVaryonWorld
                    || fr.varyon.vrpg.integration.VaryonZoneBridge.isInSafeZone(pos.x, pos.z);
                int zoneId = isHaven ? 0 : fr.varyon.vrpg.integration.VaryonZoneBridge.getZoneId(pos.x, pos.z, worldName);

                ClassXpConfig.ZoneXpDef zoneDef = ClassXpConfig.getZoneXpDef(zoneId);
                zoneMultiplier = zoneDef.multiplier;
                levelFactor = ClassXpConfig.getLevelFactor(progress.getLevel(), zoneDef.maxUsefulLevel);
                zoneReason = "zone=" + zoneId + " x" + String.format("%.2f", zoneMultiplier)
                    + " levelFactor=" + String.format("%.2f", levelFactor);
            }
        }

        double finalXp = baseXp * mult * zoneMultiplier * levelFactor;
        LOGGER.atInfo().log("[ClassKillXp] +" + String.format("%.2f", finalXp) + " xp"
            + " (base=" + String.format("%.2f", baseXp) + (multReason != null ? " x" + multReason : "")
            + (zoneReason != null ? " " + zoneReason : "")
            + ") class=" + activeClass + " mob=" + MobKillXpResolver.npcRoleKey(npc));

        classManager.addXp(uuid, activeClass, finalXp, playerRef);
    }

    private void onKill(@Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        ClassAccount acc = classManager.getOrLoad(uuid);
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return;

        PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (activeClass == PlayerClass.BARBARE && spec == PlayerSpecialization.RAVAGEUR) {
            int moissonneurRank = acc.getTalentRank(activeClass, RavageurPassifs.MOISSONNEUR_NODE);
            if (moissonneurRank > 0) {
                ravageurState.onKillMoissonneur(uuid, RavageurPassifs.MOISSONNEUR_MAX_STACKS, 10000L);
            }
            int elanRank = acc.getTalentRank(activeClass, RavageurPassifs.ELAN_DESTRUCTEUR_NODE);
            if (elanRank > 0) {
                ravageurState.armElan(uuid, elanRank);
            }
        }
        if (activeClass == PlayerClass.MAGE && spec == PlayerSpecialization.ARCANISTE && arcanistState != null) {
            int drainRank = acc.getTalentRank(activeClass, fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.DRAIN_MYSTIQUE_NODE);
            if (drainRank > 0) {
                float pct = fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.drainMystiqueBonusForRank(drainRank);
                fr.varyon.vrpg.classes.ability.ClassSkillMana.restoreByPct(playerRef, pct);
            }
        }
        if (activeClass == PlayerClass.BARBARE && spec == PlayerSpecialization.BERSERKER) {
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

    @Nullable
    private org.joml.Vector3d getPosition(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc =
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            return tc != null ? tc.getPosition() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String getWorldName(@Nonnull Store<EntityStore> store) {
        try {
            return store.getExternalData().getWorld().getName();
        } catch (Exception e) {
            return null;
        }
    }

    private double getDistanceBetween(@Nonnull Ref<EntityStore> refA, @Nonnull Ref<EntityStore> refB,
                                       @Nonnull Store<EntityStore> store) {
        try {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tcA =
                store.getComponent(refA, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tcB =
                store.getComponent(refB, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tcA == null || tcB == null) return -1;
            org.joml.Vector3d pa = tcA.getPosition();
            org.joml.Vector3d pb = tcB.getPosition();
            double dx = pa.x - pb.x, dy = pa.y - pb.y, dz = pa.z - pb.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        } catch (Exception e) { return -1; }
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
