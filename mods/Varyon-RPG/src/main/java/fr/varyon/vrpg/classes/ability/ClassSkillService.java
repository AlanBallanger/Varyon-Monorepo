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
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import fr.varyon.vrpg.audio.ClassSkillSounds;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class ClassSkillService {

    private final ClassManager classManager;
    private final DuellisteState duellisteState;
    private final ClassSkillCooldowns cooldowns = new ClassSkillCooldowns();

    public ClassSkillService(@Nonnull ClassManager classManager, @Nonnull DuellisteState duellisteState) {
        this.classManager = classManager;
        this.duellisteState = duellisteState;
    }

    public boolean tryCast(@Nonnull String skillId,
                           @Nonnull UUID uuid,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull Ref<EntityStore> entityRef,
                           @Nonnull Store<EntityStore> store,
                           @Nullable CommandBuffer<EntityStore> commandBuffer) {
        if (AssautEclairSkill.SKILL_ID.equals(skillId)) {
            return tryCastAssautEclair(uuid, playerRef, entityRef, store, commandBuffer);
        }
        if (AssautBretteurSkill.SKILL_ID.equals(skillId)) {
            return tryCastAssautBretteur(uuid, playerRef);
        }
        if (DesarmementSkill.SKILL_ID.equals(skillId)) {
            return tryCastDesarmement(uuid, playerRef, entityRef, store);
        }
        if (CoupEstocSkill.SKILL_ID.equals(skillId)) {
            return tryCastCoupEstoc(uuid, playerRef, entityRef, store);
        }
        if (FeintSkill.SKILL_ID.equals(skillId)) {
            return tryCastFeinte(uuid, playerRef, entityRef, store);
        }
        if (RiposteParfaiteSkill.SKILL_ID.equals(skillId)) {
            return tryCastRiposte(uuid, playerRef, entityRef, store);
        }
        return false;
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

    private boolean isDuelliste(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.DUELLISTE;
    }

    @Nullable
    private Ref<EntityStore> findTargetedNpcRef(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc == null) return null;
            org.joml.Vector3d pos = tc.getPosition();

            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dir = hr != null ? hr.getDirection() : new org.joml.Vector3d(0, 0, 1);

            long casterIdx = entityRef.getIndex();
            double searchRadius = 5.0;
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

    public long getCooldownTotalMs(@Nonnull String skillId, @Nonnull ClassAccount acc, @Nonnull PlayerClass cls) {
        if (AssautEclairSkill.SKILL_ID.equals(skillId))
            return AssautEclairSkill.cooldownMsForRank(acc.getTalentRank(cls, AssautEclairSkill.TALENT_NODE_ID));
        if (AssautBretteurSkill.SKILL_ID.equals(skillId))
            return AssautBretteurSkill.cooldownMsForRank(acc.getTalentRank(cls, AssautBretteurSkill.TALENT_NODE_ID));
        if (DesarmementSkill.SKILL_ID.equals(skillId))
            return DesarmementSkill.cooldownMsForRank(acc.getTalentRank(cls, DesarmementSkill.TALENT_NODE_ID));
        if (CoupEstocSkill.SKILL_ID.equals(skillId))
            return CoupEstocSkill.cooldownMsForRank(acc.getTalentRank(cls, CoupEstocSkill.TALENT_NODE_ID));
        if (FeintSkill.SKILL_ID.equals(skillId))
            return FeintSkill.cooldownMsForRank(acc.getTalentRank(cls, FeintSkill.TALENT_NODE_ID));
        if (RiposteParfaiteSkill.SKILL_ID.equals(skillId))
            return RiposteParfaiteSkill.cooldownMsForRank(acc.getTalentRank(cls, RiposteParfaiteSkill.TALENT_NODE_ID));
        return 0L;
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
