package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.duelliste.DuellisteState;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.classes.duelliste.RiposteParfaiteSkill;
import fr.varyon.vrpg.classes.ombre.OmbreState;
import fr.varyon.vrpg.classes.ombre.PasDesTenebresSkill;
import fr.varyon.vrpg.classes.ombre.EcranDeFumeeSkill;
import fr.varyon.vrpg.classes.ombre.FrappeFataleSkill;
import fr.varyon.vrpg.classes.ombre.DelugeDeGamesSkill;
import fr.varyon.vrpg.classes.ombre.PasDeLOmbreSkill;
import fr.varyon.vrpg.classes.ombre.ChaseOuverteSkill;
import fr.varyon.vrpg.classes.ombre.OmbrePassifs;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import fr.varyon.vrpg.audio.ClassSkillSounds;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClassSkillService {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    @FunctionalInterface
    private interface SkillCaster {
        boolean cast(@Nonnull UUID uuid,
                     @Nonnull PlayerRef playerRef,
                     @Nonnull Ref<EntityStore> entityRef,
                     @Nonnull Store<EntityStore> store,
                     @Nullable CommandBuffer<EntityStore> commandBuffer);
    }

    private final ClassManager classManager;
    private final DuellisteState duellisteState;
    private final OmbreState ombreState;
    private final fr.varyon.vrpg.classes.rempart.RempartState rempartState;
    private final ClassSkillCooldowns cooldowns = new ClassSkillCooldowns();
    private final Map<String, SkillCaster> casters = new HashMap<>();

    public ClassSkillService(@Nonnull ClassManager classManager,
                             @Nonnull DuellisteState duellisteState,
                             @Nonnull OmbreState ombreState,
                             @Nonnull fr.varyon.vrpg.classes.rempart.RempartState rempartState) {
        this.classManager = classManager;
        this.duellisteState = duellisteState;
        this.ombreState = ombreState;
        this.rempartState = rempartState;
        registerCasters();
    }

    private void registerCasters() {
        casters.put(AssautEclairSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAssautEclair(uuid, pr, er, st, cb));
        casters.put(AssautBretteurSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAssautBretteur(uuid, pr));
        casters.put(DesarmementSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDesarmement(uuid, pr, er, st));
        casters.put(CoupEstocSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCoupEstoc(uuid, pr, er, st));
        casters.put(FeintSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFeinte(uuid, pr, er, st));
        casters.put(RiposteParfaiteSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastRiposte(uuid, pr, er, st));
        casters.put(PasDesTenebresSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPasDesTenebres(uuid, pr, er, st, cb));
        casters.put(EcranDeFumeeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastEcranDeFumee(uuid, pr));
        casters.put(FrappeFataleSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFrappeFatale(uuid, pr));
        casters.put(DelugeDeGamesSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDelugeDeGames(uuid, pr, er, st));
        casters.put(PasDeLOmbreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPasDeLOmbre(uuid, pr, er, st));
        casters.put(ChaseOuverteSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastChaseOuverte(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastChargeLourde(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCoupDeBouclier(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastForteresse(uuid, pr));
        casters.put(fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastSecondSouffleRempart(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastGardeRapprochee(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastProvocation(uuid, pr, er, st));
    }

    public boolean tryCast(@Nonnull String skillId,
                           @Nonnull UUID uuid,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull Ref<EntityStore> entityRef,
                           @Nonnull Store<EntityStore> store,
                           @Nullable CommandBuffer<EntityStore> commandBuffer) {
        SkillCaster caster = casters.get(skillId);
        return caster != null && caster.cast(uuid, playerRef, entityRef, store, commandBuffer);
    }

    public boolean tryCastAssautEclair(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass != PlayerClass.GUERRIER) return false;
        if (acc.getActiveSpec(activeClass) != PlayerSpecialization.DUELLISTE) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(activeClass, AssautEclairSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;

        long cooldownMs = AssautEclairSkill.cooldownMsForRank(rank);
        boolean bypassCooldown = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypassCooldown && cooldowns.isOnCooldown(uuid, AssautEclairSkill.SKILL_ID, cooldownMs)) return false;

        float staminaCost = AssautEclairSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        if (!AssautEclairSkill.execute(playerRef, entityRef, store, commandBuffer, rank)) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound(ClassSkillSounds.ASSAUT_ECLAIR_SOUND, playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}
        cooldowns.markUsed(uuid, AssautEclairSkill.SKILL_ID);
        return true;
    }

    public boolean tryCastAssautBretteur(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, AssautBretteurSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, AssautBretteurSkill.SKILL_ID, AssautBretteurSkill.cooldownMsForRank(rank))) return false;
        duellisteState.startAssautBretteur(uuid, AssautBretteurSkill.durationMsForRank(rank));
        if (!bypass) cooldowns.markUsed(uuid, AssautBretteurSkill.SKILL_ID);
        try {
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                        fr.varyon.vrpg.audio.ClassSkillSounds.ASSAUT_BRETTEUR_SOUND,
                        pr, new org.joml.Vector3d(), null);
                    ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", pr, new org.joml.Vector3d(), null);
                    break;
                }
            }
        } catch (Exception ignored) {}
        notifySkill(uuid, "Assaut du Bretteur");
        return true;
    }

    public boolean tryCastDesarmement(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, DesarmementSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, DesarmementSkill.SKILL_ID, DesarmementSkill.cooldownMsForRank(rank))) return false;
        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store);
        if (targeted == null) return false;
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "SwingRight", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound("SFX_Sword_T2_Swing", playerRef, tc.getPosition(), null);
            }
            int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg = weaponDmg > 0 ? (float) weaponDmg : 10f;
            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
        } catch (Exception ignored) {}
        float durationSec = DesarmementSkill.durationMsForRank(rank) / 1000f;
        duellisteState.startDesarmement(uuid, targeted.getIndex(), DesarmementSkill.durationMsForRank(rank));
        try {
            int idx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap()
                .getIndex("Vrpg_Desarmement");
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect effect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(idx);
            if (effect != null) {
                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                    store.getComponent(targeted,
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                if (ec != null) {
                    ec.addEffect(targeted, effect, durationSec,
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                }
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, DesarmementSkill.SKILL_ID);
        notifySkill(uuid, "Désarmement");
        return true;
    }

    public boolean tryCastCoupEstoc(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                    @Nonnull Ref<EntityStore> entityRef,
                                    @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, CoupEstocSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = CoupEstocSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, CoupEstocSkill.SKILL_ID, cd)) return false;
        // AoE au cast
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "Stab", true, store);
            long casterIdx = entityRef.getIndex();
            float dmg = CoupEstocSkill.castDamageForRank(rank, playerRef);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound(ClassSkillSounds.COUP_ESTOC_SOUND, playerRef, tc.getPosition(), null);
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), CoupEstocSkill.castRadius(), targetRef -> {
                        try {
                            if (targetRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                targetRef, store,
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
                        } catch (Exception ignored) {}
                    }, t2 -> t2.getIndex() != casterIdx);
            }
        } catch (Exception ignored) {}
        // Arme le prochain coup
        duellisteState.armCoupEstoc(uuid, CoupEstocSkill.nextHitMultForRank(rank), CoupEstocSkill.armedWindowMs());
        if (!bypass) cooldowns.markUsed(uuid, CoupEstocSkill.SKILL_ID);
        notifySkill(uuid, "Coup d'Estoc");
        return true;
    }

    public boolean tryCastFeinte(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                  @Nonnull Ref<EntityStore> entityRef,
                                  @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, FeintSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = FeintSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, FeintSkill.SKILL_ID, cd)) return false;
        duellisteState.startFeinte(uuid, FeintSkill.windowMs());
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound("SFX_Sword_T2_Signature_Part_1", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, FeintSkill.SKILL_ID);
        notifySkill(uuid, "Feinte");
        return true;
    }

    public boolean tryCastRiposte(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                   @Nonnull Ref<EntityStore> entityRef,
                                   @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, RiposteParfaiteSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = RiposteParfaiteSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, RiposteParfaiteSkill.SKILL_ID, cd)) return false;
        duellisteState.startRiposteWindow(uuid, RiposteParfaiteSkill.windowMsForRank(rank), rank);
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "Guard", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound("SFX_Sword_T1_Block_Local", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, RiposteParfaiteSkill.SKILL_ID);
        notifySkill(uuid, "Riposte Parfaite");
        return true;
    }

    public boolean tryCastPasDesTenebres(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store,
                                          @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        LOG.atInfo().log("[PasDesTenebres] cast attempt uuid=" + uuid + " class=" + acc.getActiveClass() + " spec=" + (acc.getActiveClass() != null ? acc.getActiveSpec(acc.getActiveClass()) : "null"));
        if (!isOmbre(acc)) { LOG.atInfo().log("[PasDesTenebres] BLOCKED not ombre"); return false; }
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); LOG.atInfo().log("[PasDesTenebres] BLOCKED no dagger"); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, PasDesTenebresSkill.TALENT_NODE_ID);
        LOG.atInfo().log("[PasDesTenebres] rank=" + rank + " bypass=" + bypass);
        if (rank <= 0 && !bypass) { LOG.atInfo().log("[PasDesTenebres] BLOCKED rank=0"); return false; }
        if (rank <= 0) rank = 1;
        long cdMs = PasDesTenebresSkill.cooldownMsForRank(rank);
        boolean onCd = !bypass && cooldowns.isOnCooldown(uuid, PasDesTenebresSkill.SKILL_ID, cdMs);
        LOG.atInfo().log("[PasDesTenebres] onCooldown=" + onCd);
        if (onCd) return false;
        float staminaCost = PasDesTenebresSkill.staminaCostForRank(rank);
        boolean hasStamina = ClassSkillStamina.hasEnough(playerRef, staminaCost);
        LOG.atInfo().log("[PasDesTenebres] staminaCost=" + staminaCost + " hasStamina=" + hasStamina);
        if (!hasStamina) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Daggers", "DashBackward", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, tc.getPosition(), commandBuffer);
                try {
                    final org.joml.Vector3d vanishPos = new org.joml.Vector3d(tc.getPosition());
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "ombre-vanish-sound"); t.setDaemon(true); return t;
                    }).schedule(() -> ClassSkillSounds.playSkillSound(
                        "SFX_Vrpg_OmbreVanish", playerRef, vanishPos, null),
                        100, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (Exception ignored2) {}

                double dist = PasDesTenebresSkill.dashDistanceForRank(rank);
                org.joml.Vector3d newPos = new org.joml.Vector3d(
                    tc.getPosition().x - dx * dist,
                    tc.getPosition().y,
                    tc.getPosition().z - dz * dist);
                com.hypixel.hytale.math.vector.Rotation3fc curRot = hr.getRotation();
                com.hypixel.hytale.math.vector.Rotation3f keepRot = new com.hypixel.hytale.math.vector.Rotation3f(
                    curRot.pitch(), curRot.yaw(), curRot.roll());
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(
                        newPos, keepRot);
                tele.withoutVelocityReset();
                (commandBuffer != null ? commandBuffer : store).addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(-dx * 30.0, 0.2, -dz * 30.0);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
            }
        } catch (Exception ignored) {}

        long stealthMs = PasDesTenebresSkill.stealthDurationMs(rank);
        LOG.atInfo().log("[PasDesTenebres] stealth started durationMs=" + stealthMs);
        ombreState.startStealth(uuid, stealthMs);
        fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.resetAggroAround(entityRef, store);
        int embRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.EMBUSCADE_NODE);
        if (embRank > 0) ombreState.armEmbuscade(uuid, stealthMs + 2000, embRank);

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, PasDesTenebresSkill.SKILL_ID);
        notifySkill(uuid, "Pas des Ténèbres");
        return true;
    }

    public boolean tryCastEcranDeFumee(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        LOG.atInfo().log("[EcranDeFumee] cast attempt uuid=" + uuid + " class=" + acc.getActiveClass() + " spec=" + (acc.getActiveClass() != null ? acc.getActiveSpec(acc.getActiveClass()) : "null"));
        if (!isOmbre(acc)) { LOG.atInfo().log("[EcranDeFumee] BLOCKED not ombre"); return false; }
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); LOG.atInfo().log("[EcranDeFumee] BLOCKED no dagger"); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, EcranDeFumeeSkill.TALENT_NODE_ID);
        LOG.atInfo().log("[EcranDeFumee] rank=" + rank + " bypass=" + bypass);
        if (rank <= 0 && !bypass) { LOG.atInfo().log("[EcranDeFumee] BLOCKED rank=0"); return false; }
        if (rank <= 0) rank = 1;
        boolean onCd = !bypass && cooldowns.isOnCooldown(uuid, EcranDeFumeeSkill.SKILL_ID, EcranDeFumeeSkill.cooldownMsForRank(rank));
        LOG.atInfo().log("[EcranDeFumee] onCooldown=" + onCd);
        if (onCd) return false;

        long durationMs = EcranDeFumeeSkill.durationMsForRank(rank);
        LOG.atInfo().log("[EcranDeFumee] stealth started durationMs=" + durationMs);
        ombreState.startEcranFumee(uuid, durationMs);
        try {
            Ref<EntityStore> ecranRef = playerRef.getReference();
            if (ecranRef != null && ecranRef.isValid()) {
                fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.resetAggroAround(
                    ecranRef, ecranRef.getStore());
            }
        } catch (Exception ignored) {}
        int embRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.EMBUSCADE_NODE);
        if (embRank > 0) ombreState.armEmbuscade(uuid, durationMs + 2000, embRank);
        try {
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    ClassSkillSounds.playSkillSound("SFX_Vrpg_OmbreVanish", pr, new org.joml.Vector3d(), null);
                    break;
                }
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, EcranDeFumeeSkill.SKILL_ID);
        notifySkill(uuid, "Écran de Fumée");
        return true;
    }

    public boolean tryCastFrappeFatale(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, FrappeFataleSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, FrappeFataleSkill.SKILL_ID, FrappeFataleSkill.cooldownMsForRank(rank))) return false;

        ombreState.armFrappeFatale(uuid, FrappeFataleSkill.armedWindowMs());
        try {
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    ClassSkillSounds.playSkillSound(ClassSkillSounds.FRAPPE_FATALE_SOUND, pr, new org.joml.Vector3d(), null);
                    ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", pr, new org.joml.Vector3d(), null);
                    break;
                }
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, FrappeFataleSkill.SKILL_ID);
        notifySkill(uuid, "Frappe Fatale");
        return true;
    }

    public boolean tryCastDelugeDeGames(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, DelugeDeGamesSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, DelugeDeGamesSkill.SKILL_ID, DelugeDeGamesSkill.cooldownMsForRank(rank))) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d pos = tc.getPosition();
                org.joml.Vector3d dir = hr.getDirection();
                double searchRadius = 2.5;
                long casterIdx = entityRef.getIndex();
                int strikes = DelugeDeGamesSkill.strikeCountForRank(rank);
                float dmgPct = DelugeDeGamesSkill.damagePctForRank(rank);
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float baseDmg = weaponDmg > 0 ? weaponDmg : 1f;

                org.joml.Vector3d target = new org.joml.Vector3d(
                    pos.x + dir.x * 2.0, pos.y + dir.y * 2.0 + 0.5, pos.z + dir.z * 2.0);

                com.hypixel.hytale.server.core.universe.world.World world = null;
                try {
                    java.util.UUID worldUuid = playerRef.getWorldUuid();
                    if (worldUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
                } catch (Exception ignored) {}
                final com.hypixel.hytale.server.core.universe.world.World delugeWorld = world;
                if (delugeWorld == null) return false;

                java.util.concurrent.ScheduledExecutorService delugeExec =
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "ombre-deluge");
                        t.setDaemon(true);
                        return t;
                    });
                int[] strikesDone = {0};
                delugeExec.scheduleAtFixedRate(() -> {
                    try {
                        if (strikesDone[0] >= strikes) {
                            delugeExec.shutdown();
                            return;
                        }
                        strikesDone[0]++;
                        boolean leftStrike = (strikesDone[0] % 2 == 1);
                        ClassSkillSounds.playSkillSound("SFX_Daggers_T2_Slash_Impact", playerRef, pos, null);
                        delugeWorld.execute(() -> {
                            try {
                                String anim = leftStrike ? "SwingLeft" : "SwingRight";
                                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Daggers", anim, true, store);
                                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                                    .selectNearbyEntities(store, target, searchRadius, targetRef -> {
                                        try {
                                            if (targetRef.getIndex() == casterIdx) return;
                                            float finalDmg = baseDmg * dmgPct;
                                            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                                                store.getComponent(targetRef, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                                            if (sm != null) {
                                                try {
                                                    int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                                                    var hpStat = sm.get(hIdx);
                                                    if (hpStat != null && hpStat.getMax() > 0
                                                            && (hpStat.get() / hpStat.getMax()) < DelugeDeGamesSkill.lowHpThreshold()) {
                                                        finalDmg *= (1f + DelugeDeGamesSkill.lowHpBonus());
                                                    }
                                                } catch (Exception ignored2) {}
                                            }
                                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                                targetRef, store,
                                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, finalDmg));
                                        } catch (Exception ignored) {}
                                    }, ref -> ref.getIndex() != casterIdx);
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                }, 0, DelugeDeGamesSkill.intervalMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {}

        if (!bypass) cooldowns.markUsed(uuid, DelugeDeGamesSkill.SKILL_ID);
        notifySkill(uuid, "Déluge de Lames");
        return true;
    }

    public boolean tryCastPasDeLOmbre(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, PasDeLOmbreSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, PasDeLOmbreSkill.SKILL_ID, PasDeLOmbreSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = PasDeLOmbreSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, PasDeLOmbreSkill.rangeForRank(rank));
        if (targeted == null) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent targetTc = store.getComponent(targeted, TransformComponent.getComponentType());

            if (tc != null && targetTc != null) {
                org.joml.Vector3d targetPos = targetTc.getPosition();

                double toPlayerX = tc.getPosition().x - targetPos.x;
                double toPlayerZ = tc.getPosition().z - targetPos.z;
                double toPlayerLen = Math.sqrt(toPlayerX*toPlayerX + toPlayerZ*toPlayerZ);
                if (toPlayerLen > 1e-6) { toPlayerX /= toPlayerLen; toPlayerZ /= toPlayerLen; }

                double behindX = targetPos.x - toPlayerX * 1.5;
                double behindZ = targetPos.z - toPlayerZ * 1.5;

                org.joml.Vector3d behindPos = new org.joml.Vector3d(behindX, targetPos.y, behindZ);
                double facingDx = targetPos.x - behindX;
                double facingDz = targetPos.z - behindZ;
                float facingYaw = (float) Math.atan2(-facingDx, -facingDz);
                com.hypixel.hytale.math.vector.Rotation3f faceRot = new com.hypixel.hytale.math.vector.Rotation3f(0f, facingYaw, 0f);
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele2 =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(
                        behindPos, faceRot);
                tele2.withoutVelocityReset();
                store.addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele2);

                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, targetPos, null);
                try {
                    com.hypixel.hytale.server.core.universe.world.World stabWorld = null;
                    try {
                        java.util.UUID wUuid = playerRef.getWorldUuid();
                        if (wUuid != null) stabWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                    } catch (Exception ignored3) {}
                    if (stabWorld != null) {
                        final com.hypixel.hytale.server.core.universe.world.World fw = stabWorld;
                        final Ref<EntityStore> stabRef = entityRef;
                        final org.joml.Vector3d soundPos = new org.joml.Vector3d(targetPos);
                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                            Thread t = new Thread(r, "ombre-stab"); t.setDaemon(true); return t;
                        }).schedule(() -> fw.execute(() -> {
                            try { AnimationUtils.playAnimation(stabRef, AnimationSlot.Action, "Daggers", "SwingRight", true, store); } catch (Exception ignored3) {}
                            ClassSkillSounds.playSkillSound("SFX_Vrpg_PasDeLOmbre_Strike", playerRef, soundPos, null);
                        }), 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    }
                } catch (Exception ignored2) {}

                int weaponDmg2 = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float baseDmg2 = weaponDmg2 > 0 ? weaponDmg2 : 1f;
                try {
                    com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                        store.getComponent(targeted, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                    if (sm != null) {
                        int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                        var hpStat = sm.get(hIdx);
                        if (hpStat != null && hpStat.getMax() > 0
                                && (hpStat.get() / hpStat.getMax()) < PasDeLOmbreSkill.lowHpThreshold()) {
                            baseDmg2 *= (1f + PasDeLOmbreSkill.lowHpBonus());
                        }
                    }
                } catch (Exception ignored) {}
                final float skillDmg = baseDmg2 * PasDeLOmbreSkill.damagePctForRank(rank);
                final Ref<EntityStore> skillTarget = targeted;
                final Ref<EntityStore> skillCaster = entityRef;
                com.hypixel.hytale.server.core.universe.world.World dmgWorldRef = null;
                try { java.util.UUID wUuid2 = playerRef.getWorldUuid(); if (wUuid2 != null) dmgWorldRef = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid2); } catch (Exception ignored2) {}
                if (dmgWorldRef != null) {
                    final com.hypixel.hytale.server.core.universe.world.World dmgWorld = dmgWorldRef;
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "ombre-skilldmg"); t.setDaemon(true); return t;
                    }).schedule(() -> dmgWorld.execute(() -> {
                        try {
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                skillTarget, store,
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(skillCaster),
                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, skillDmg));
                        } catch (Exception ignored3) {}
                    }), 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                } else {
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                        targeted, store,
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, skillDmg));
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, PasDeLOmbreSkill.SKILL_ID);
        notifySkill(uuid, "Pas de l'Ombre");
        return true;
    }

    public boolean tryCastChaseOuverte(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, ChaseOuverteSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, ChaseOuverteSkill.SKILL_ID, ChaseOuverteSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = ChaseOuverteSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        long durationMs = ChaseOuverteSkill.durationMsForRank(rank);

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 7.0);
        if (targeted == null) {
            LOG.atInfo().log("[ChaseOuverte] BLOCKED no target in range");
            return false;
        }

        ombreState.startChaseOuverte(uuid, durationMs, rank);

        try {
            TransformComponent casterTc = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent targetTc = store.getComponent(targeted, TransformComponent.getComponentType());
            if (casterTc != null) {
                ClassSkillSounds.playSkillSound("Bow_T2_Shoot_01", playerRef, casterTc.getPosition(), null);

                if (targetTc != null) {
                    try {
                        com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig kunaiConfig =
                            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap()
                                .getAsset("Projectile_Config_Kunai");
                        LOG.atInfo().log("[Kunai] config=" + kunaiConfig);
                        if (kunaiConfig != null) {
                            org.joml.Vector3d casterPos = casterTc.getPosition();
                            org.joml.Vector3d targetPos2 = targetTc.getPosition();
                            double toDx = targetPos2.x - casterPos.x;
                            double toDy = (targetPos2.y + 0.5) - (casterPos.y + 1.5);
                            double toDz = targetPos2.z - casterPos.z;
                            double len = Math.sqrt(toDx*toDx + toDy*toDy + toDz*toDz);
                            if (len > 1e-6) { toDx /= len; toDy /= len; toDz /= len; }
                            org.joml.Vector3d direction = new org.joml.Vector3d(toDx, toDy, toDz);
                            org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                                casterPos.x - toDx * 1.0 - toDz * 0.5,
                                casterPos.y + 1.8,
                                casterPos.z - toDz * 1.0 + toDx * 0.5);
                            if (commandBuffer != null) {
                                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                                    .spawnProjectile(entityRef, commandBuffer, kunaiConfig, spawnPos, direction);
                                LOG.atInfo().log("[Kunai] spawned via commandBuffer!");
                            } else {
                                com.hypixel.hytale.server.core.universe.world.World kunaiWorld = null;
                                try {
                                    java.util.UUID wUuid = playerRef.getWorldUuid();
                                    if (wUuid != null) kunaiWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                                } catch (Exception ignored2) {}
                                if (kunaiWorld != null) {
                                    final com.hypixel.hytale.server.core.universe.world.World fw = kunaiWorld;
                                    final org.joml.Vector3d fSpawnPos = spawnPos;
                                    final org.joml.Vector3d fDir = direction;
                                    final Ref<EntityStore> fEntityRef = entityRef;
                                    final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fConfig = kunaiConfig;
                                    fw.execute(() -> {
                                        try {
                                            Store<EntityStore> ws = fw.getEntityStore().getStore();
                                            java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                                            takeCmd.setAccessible(true);
                                            @SuppressWarnings("unchecked")
                                            CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                                            Exception spawnEx = null;
                                            try {
                                                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                                                    .spawnProjectile(fEntityRef, cb, fConfig, fSpawnPos, fDir);
                                                LOG.atInfo().log("[Kunai] spawnProjectile OK");
                                            } catch (Exception e3) {
                                                spawnEx = e3;
                                                LOG.atWarning().log("[Kunai] spawnProjectile EXCEPTION: " + e3 + " cause=" + e3.getCause());
                                            }
                                            try {
                                                java.lang.reflect.Method consume = cb.getClass().getDeclaredMethod("consume");
                                                consume.setAccessible(true);
                                                consume.invoke(cb);
                                                LOG.atInfo().log("[Kunai] commandBuffer consumed OK");
                                            } catch (Exception e4) {
                                                LOG.atWarning().log("[Kunai] consume EXCEPTION: " + e4 + " cause=" + e4.getCause());
                                            }
                                            if (spawnEx == null) LOG.atInfo().log("[Kunai] fully spawned!");
                                        } catch (Exception e2) {
                                            LOG.atWarning().log("[Kunai] reflection EXCEPTION: " + e2 + " cause=" + e2.getCause());
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception ignoredProj) {
                        LOG.atWarning().log("[Kunai] EXCEPTION: " + ignoredProj.getMessage());
                    }
                }
            }

            float durationSec = durationMs / 1000f;
            int idx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap()
                .getIndex("Vrpg_Chasse_Ouverte");
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect effect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(idx);
            if (effect != null) {
                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                    store.getComponent(targeted,
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                if (ec != null) {
                    ec.addEffect(targeted, effect, durationSec,
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                }
            }
            LOG.atInfo().log("[ChaseOuverte] target marked rank=" + rank + " durationMs=" + durationMs);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, ChaseOuverteSkill.SKILL_ID);
        notifySkill(uuid, "Chasse Ouverte");
        return true;
    }

    private boolean isDuelliste(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.DUELLISTE;
    }

    private boolean isOmbre(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.OMBRE;
    }

    private boolean isHoldingDagger(@Nonnull PlayerRef playerRef) {
        return fr.varyon.vrpg.classes.WeaponCategory.fromItemId(getHeldItemId(playerRef))
            == fr.varyon.vrpg.classes.WeaponCategory.DAGUE;
    }

    private String getHeldItemId(@Nonnull PlayerRef playerRef) {
        try {
            var hotbar = playerRef.getComponent(InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return null;
            ItemStack held = hotbar.getInventory().getItemStack((short) hotbar.getActiveSlot());
            if (held == null || held.isEmpty()) return null;
            return held.getItemId();
        } catch (Exception e) { return null; }
    }

    @Nullable
    private Ref<EntityStore> findTargetedNpcRef(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return findTargetedNpcRef(playerRef, entityRef, store, 5.0);
    }

    @Nullable
    private Ref<EntityStore> findTargetedNpcRef(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            double maxRange) {
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc == null) return null;
            org.joml.Vector3d pos = tc.getPosition();

            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dir = hr != null ? hr.getDirection() : new org.joml.Vector3d(0, 0, 1);

            long casterIdx = entityRef.getIndex();
            double searchRadius = maxRange;
            double[] bestDist = {searchRadius * searchRadius + 1};
            Ref<EntityStore>[] bestRef = new Ref[]{null};

            org.joml.Vector3d searchCenter = new org.joml.Vector3d(
                pos.x + dir.x * 2.5, pos.y + dir.y * 2.5 + 1.0, pos.z + dir.z * 2.5);

            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                store, searchCenter, searchRadius, targetRef -> {
                    try {
                        if (targetRef.getIndex() == casterIdx) return;
                        com.hypixel.hytale.server.npc.entities.NPCEntity npc =
                            store.getComponent(targetRef, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
                        if (npc == null) return;
                        TransformComponent ttc = store.getComponent(targetRef, TransformComponent.getComponentType());
                        if (ttc == null) return;
                        org.joml.Vector3d tp = ttc.getPosition();
                        double dx = tp.x - pos.x, dy = tp.y - pos.y, dz = tp.z - pos.z;
                        double dot = dx * dir.x + dy * dir.y + dz * dir.z;
                        if (dot <= 0) return;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < bestDist[0]) {
                            bestDist[0] = distSq;
                            bestRef[0] = targetRef;
                        }
                    } catch (Exception ignored) {}
                }, t2 -> t2.getIndex() != casterIdx);

            return bestRef[0];
        } catch (Exception e) {
            return null;
        }
    }

    private void notifySkill(@Nonnull UUID uuid, @Nonnull String name) {
        try {
            ClassAccount acc = classManager.getOrLoad(uuid);
            PlayerClass cls = acc.getActiveClass();
            PlayerSpecialization spec = cls != null ? acc.getActiveSpec(cls) : null;
            String icon = spec != null ? spec.getItemId() : "Weapon_Sword_Mithril";
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    NotificationUtil.sendNotification(pr.getPacketHandler(),
                        com.hypixel.hytale.server.core.Message.raw(name), icon, NotificationStyle.Success);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void notifyNoWeapon(@Nonnull PlayerRef playerRef) {
        try {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                com.hypixel.hytale.server.core.Message.raw("Arme requise"),
                (String) null, NotificationStyle.Danger);
        } catch (Exception ignored) {}
    }

    private boolean isHoldingSword(@Nonnull PlayerRef playerRef) {
        try {
            var hotbar = playerRef.getComponent(
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return false;
            byte slot = hotbar.getActiveSlot();
            com.hypixel.hytale.server.core.inventory.ItemStack held = hotbar.getInventory().getItemStack((short) slot);
            if (held == null || held.isEmpty()) return false;
            String id = held.getItemId();
            if (id == null) return false;
            return fr.varyon.vrpg.classes.WeaponCategory.fromItemId(id)
                == fr.varyon.vrpg.classes.WeaponCategory.EPEE;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRempart(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.REMPART;
    }

    private boolean isHoldingOneHanded(@Nonnull PlayerRef playerRef) {
        String id = getHeldItemId(playerRef);
        if (id == null) return false;
        fr.varyon.vrpg.classes.WeaponCategory cat = fr.varyon.vrpg.classes.WeaponCategory.fromItemId(id);
        return cat == fr.varyon.vrpg.classes.WeaponCategory.EPEE
            || cat == fr.varyon.vrpg.classes.WeaponCategory.DAGUE
            || cat == fr.varyon.vrpg.classes.WeaponCategory.HACHE;
    }

    public boolean tryCastChargeLourde(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store,
                                        @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        if (!isHoldingOneHanded(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    vel.getInstructions().clear();
                    vel.addInstruction(
                        new org.joml.Vector3d(dx * 22.0, 0.5, dz * 22.0),
                        null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }

                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float dmg = (weaponDmg > 0 ? weaponDmg : 1f) * fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.damagePctForRank(rank);
                long casterIdx = entityRef.getIndex();
                double dist = fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.dashDistanceForRank(rank);
                org.joml.Vector3d startPos = tc.getPosition();
                java.util.HashSet<Long> hitSet = new java.util.HashSet<>();

                for (double t = 0.5; t <= dist; t += 0.8) {
                    org.joml.Vector3d sample = new org.joml.Vector3d(
                        startPos.x + dx * t,
                        startPos.y + 0.5,
                        startPos.z + dz * t);
                    final double fx = dx, fz = dz;
                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                        store, sample, 1.8, targetRef -> {
                            try {
                                long tidx = targetRef.getIndex();
                                if (tidx == casterIdx || !hitSet.add(tidx)) return;
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targetRef, store,
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
                                com.hypixel.hytale.server.core.modules.physics.component.Velocity targetVel =
                                    store.getComponent(targetRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                                if (targetVel != null) {
                                    targetVel.getInstructions().clear();
                                    targetVel.addInstruction(
                                        new org.joml.Vector3d(fx * 12.0, 6.0, fz * 12.0),
                                        null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                }
                            } catch (Exception ignored) {}
                        }, ref -> ref.getIndex() != casterIdx);
                }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "StabDashCharged", true, commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Sword_T2_Lunge_Local", playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID);
        notifySkill(uuid, "Charge Lourde");
        return true;
    }

    public boolean tryCastCoupDeBouclier(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Status, "Sword", "Guard", true, store);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "SwingRight", true, store);
            if (tc != null) ClassSkillSounds.playSkillSound("Shield_T1_Impact_01", playerRef, tc.getPosition(), null);

            Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store);
            if (targeted != null) {
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float dmg = (weaponDmg > 0 ? weaponDmg : 1f) * fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.damagePctForRank(rank);
                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

                float stunSec = fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.stunMsForRank(rank) / 1000f;
                int stunIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Ombre_Stun");
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect stunEffect =
                    (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(stunIdx);
                if (stunEffect != null) {
                    com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                        store.getComponent(targeted, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                    if (ec != null) ec.addEffect(targeted, stunEffect, stunSec,
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID);
        notifySkill(uuid, "Coup de Bouclier");
        return true;
    }

    public boolean tryCastForteresse(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.ForteresseSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.ForteresseSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.ForteresseSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        rempartState.startForteresse(uuid, fr.varyon.vrpg.classes.rempart.ForteresseSkill.durationMsForRank(rank), fr.varyon.vrpg.classes.rempart.ForteresseSkill.damageReductionForRank(rank));
        try {
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, new org.joml.Vector3d(), null);
        } catch (Exception ignored) {}
        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID);
        notifySkill(uuid, "Forteresse");
        return true;
    }

    public boolean tryCastSecondSouffleRempart(@Nonnull UUID uuid,
                                                @Nonnull PlayerRef playerRef,
                                                @Nonnull Ref<EntityStore> entityRef,
                                                @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            Ref<EntityStore> ref = entityRef;
            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap stats =
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
            if (stats != null) {
                int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                var hp = stats.get(hIdx);
                if (hp != null && hp.getMax() > 0) {
                    float heal = hp.getMax() * fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.healPctForRank(rank);
                    stats.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + heal));
                }
            }
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, new org.joml.Vector3d(), null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID);
        notifySkill(uuid, "Second Souffle");
        return true;
    }

    public boolean tryCastGardeRapprochee(@Nonnull UUID uuid,
                                           @Nonnull PlayerRef playerRef,
                                           @Nonnull Ref<EntityStore> entityRef,
                                           @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        float reduction = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.damageReductionForRank(rank);
        long durationMs = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.durationMsForRank(rank);
        rempartState.startGardeRapprochee(uuid, durationMs, reduction);

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                double radius = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.allyRadius();
                long casterIdx = entityRef.getIndex();
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), radius, nearRef -> {
                        try {
                            if (nearRef.getIndex() == casterIdx) return;
                            PlayerRef nearPlayer = store.getComponent(nearRef, PlayerRef.getComponentType());
                            if (nearPlayer == null) return;
                            rempartState.startGardeRapprochee(nearPlayer.getUuid(), durationMs, reduction);
                        } catch (Exception ignored) {}
                    }, ref -> ref.getIndex() != casterIdx);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID);
        notifySkill(uuid, "Garde Rapprochée");
        return true;
    }

    public boolean tryCastProvocation(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.ProvocationSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.ProvocationSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.ProvocationSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        rempartState.startProvocation(uuid, fr.varyon.vrpg.classes.rempart.ProvocationSkill.durationMsForRank(rank));
        fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.resetAggroAround(entityRef, store);

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                double radius = fr.varyon.vrpg.classes.rempart.ProvocationSkill.tauntRadius();
                long casterIdx = entityRef.getIndex();
                final Ref<EntityStore> casterRef = entityRef;
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), radius, npcRef -> {
                        try {
                            if (npcRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.npc.entities.NPCEntity npc =
                                store.getComponent(npcRef, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
                            if (npc == null) return;
                            com.hypixel.hytale.server.npc.role.Role role = npc.getRole();
                            if (role == null) return;
                            fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.forceTarget(role, casterRef);
                        } catch (Exception ignored) {}
                    }, ref -> ref.getIndex() != casterIdx);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID);
        notifySkill(uuid, "Provocation");
        return true;
    }

    private interface CooldownResolver {
        long resolve(@Nonnull ClassAccount acc, @Nonnull PlayerClass cls);
    }

    private static final Map<String, CooldownResolver> COOLDOWN_RESOLVERS = new HashMap<>();

    static {
        COOLDOWN_RESOLVERS.put(AssautEclairSkill.SKILL_ID,
            (acc, cls) -> AssautEclairSkill.cooldownMsForRank(acc.getTalentRank(cls, AssautEclairSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(AssautBretteurSkill.SKILL_ID,
            (acc, cls) -> AssautBretteurSkill.cooldownMsForRank(acc.getTalentRank(cls, AssautBretteurSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(DesarmementSkill.SKILL_ID,
            (acc, cls) -> DesarmementSkill.cooldownMsForRank(acc.getTalentRank(cls, DesarmementSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(CoupEstocSkill.SKILL_ID,
            (acc, cls) -> CoupEstocSkill.cooldownMsForRank(acc.getTalentRank(cls, CoupEstocSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(FeintSkill.SKILL_ID,
            (acc, cls) -> FeintSkill.cooldownMsForRank(acc.getTalentRank(cls, FeintSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(RiposteParfaiteSkill.SKILL_ID,
            (acc, cls) -> RiposteParfaiteSkill.cooldownMsForRank(acc.getTalentRank(cls, RiposteParfaiteSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(PasDesTenebresSkill.SKILL_ID,
            (acc, cls) -> PasDesTenebresSkill.cooldownMsForRank(acc.getTalentRank(cls, PasDesTenebresSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(EcranDeFumeeSkill.SKILL_ID,
            (acc, cls) -> EcranDeFumeeSkill.cooldownMsForRank(acc.getTalentRank(cls, EcranDeFumeeSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(FrappeFataleSkill.SKILL_ID,
            (acc, cls) -> FrappeFataleSkill.cooldownMsForRank(acc.getTalentRank(cls, FrappeFataleSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(DelugeDeGamesSkill.SKILL_ID,
            (acc, cls) -> DelugeDeGamesSkill.cooldownMsForRank(acc.getTalentRank(cls, DelugeDeGamesSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(PasDeLOmbreSkill.SKILL_ID,
            (acc, cls) -> PasDeLOmbreSkill.cooldownMsForRank(acc.getTalentRank(cls, PasDeLOmbreSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(ChaseOuverteSkill.SKILL_ID,
            (acc, cls) -> ChaseOuverteSkill.cooldownMsForRank(acc.getTalentRank(cls, ChaseOuverteSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.ForteresseSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.ForteresseSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.ProvocationSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.ProvocationSkill.TALENT_NODE_ID)));
    }

    public long getCooldownTotalMs(@Nonnull String skillId, @Nonnull ClassAccount acc, @Nonnull PlayerClass cls) {
        CooldownResolver resolver = COOLDOWN_RESOLVERS.get(skillId);
        return resolver != null ? resolver.resolve(acc, cls) : 0L;
    }

    public long getCooldownRemainingMs(@Nonnull UUID uuid, @Nonnull String skillId,
                                       @Nonnull ClassAccount acc, @Nonnull PlayerClass cls) {
        long total = getCooldownTotalMs(skillId, acc, cls);
        if (total <= 0L) return 0L;
        return cooldowns.remainingMs(uuid, skillId, total);
    }

    public void cleanup(@Nonnull UUID uuid) {
        cooldowns.cleanup(uuid);
    }
}
