package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.duelliste.DuellisteState;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.classes.duelliste.RiposteParfaiteSkill;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
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
            return tryCastFeinte(uuid, playerRef);
        }
        if (RiposteParfaiteSkill.SKILL_ID.equals(skillId)) {
            return tryCastRiposte(uuid, playerRef);
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
        if (activeClass != PlayerClass.GUERRIER) {
            deny(playerRef, "Assaut éclair — Guerrier requis.");
            return false;
        }
        if (acc.getActiveSpec(activeClass) != PlayerSpecialization.DUELLISTE) {
            deny(playerRef, "Assaut éclair — spécialisation Duelliste requise.");
            return false;
        }
        int rank = acc.getTalentRank(activeClass, AssautEclairSkill.TALENT_NODE_ID);
        if (rank <= 0) {
            deny(playerRef, "Assaut éclair — talent non débloqué.");
            return false;
        }

        long cooldownMs = AssautEclairSkill.cooldownMsForRank(rank);
        boolean bypassCooldown = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypassCooldown && cooldowns.isOnCooldown(uuid, AssautEclairSkill.SKILL_ID, cooldownMs)) {
            float sec = cooldowns.remainingMs(uuid, AssautEclairSkill.SKILL_ID, cooldownMs) / 1000f;
            deny(playerRef, String.format("Assaut éclair — cooldown %.1fs", sec));
            return false;
        }

        float staminaCost = AssautEclairSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) {
            deny(playerRef, "Assaut éclair — endurance insuffisante (" + (int) staminaCost + ").");
            return false;
        }

        if (!AssautEclairSkill.execute(playerRef, entityRef, store, commandBuffer, rank)) {
            deny(playerRef, "Assaut éclair — échec.");
            return false;
        }
        ClassSkillStamina.consume(playerRef, staminaCost);

        cooldowns.markUsed(uuid, AssautEclairSkill.SKILL_ID);
        return true;
    }

    public boolean tryCastAssautBretteur(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) { deny(playerRef, "Assaut du Bretteur — Duelliste requis."); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, AssautBretteurSkill.TALENT_NODE_ID);
        if (rank <= 0) { deny(playerRef, "Assaut du Bretteur — talent non débloqué."); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, AssautBretteurSkill.SKILL_ID, AssautBretteurSkill.cooldownMsForRank(rank))) {
            deny(playerRef, String.format("Assaut du Bretteur — cooldown %.1fs",
                cooldowns.remainingMs(uuid, AssautBretteurSkill.SKILL_ID, AssautBretteurSkill.cooldownMsForRank(rank)) / 1000f));
            return false;
        }
        duellisteState.startAssautBretteur(uuid, AssautBretteurSkill.durationMsForRank(rank));
        if (!bypass) cooldowns.markUsed(uuid, AssautBretteurSkill.SKILL_ID);
        notifySkill(uuid, "Assaut du Bretteur");
        return true;
    }

    public boolean tryCastDesarmement(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) { deny(playerRef, "Désarmement — Duelliste requis."); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, DesarmementSkill.TALENT_NODE_ID);
        if (rank <= 0) { deny(playerRef, "Désarmement — talent non débloqué."); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, DesarmementSkill.SKILL_ID, DesarmementSkill.cooldownMsForRank(rank))) {
            deny(playerRef, String.format("Désarmement — cooldown %.1fs",
                cooldowns.remainingMs(uuid, DesarmementSkill.SKILL_ID, DesarmementSkill.cooldownMsForRank(rank)) / 1000f));
            return false;
        }
        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store);
        if (targeted == null) { deny(playerRef, "Désarmement — aucune cible."); return false; }
        duellisteState.startDesarmement(uuid, targeted.getIndex(), DesarmementSkill.durationMsForRank(rank));
        if (!bypass) cooldowns.markUsed(uuid, DesarmementSkill.SKILL_ID);
        notifySkill(uuid, "Désarmement");
        return true;
    }

    public boolean tryCastCoupEstoc(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                    @Nonnull Ref<EntityStore> entityRef,
                                    @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) { deny(playerRef, "Coup d'Estoc — Duelliste requis."); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, CoupEstocSkill.TALENT_NODE_ID);
        if (rank <= 0) { deny(playerRef, "Coup d'Estoc — talent non debloque."); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = CoupEstocSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, CoupEstocSkill.SKILL_ID, cd)) {
            deny(playerRef, String.format("Coup d'Estoc — cooldown %.1fs", cooldowns.remainingMs(uuid, CoupEstocSkill.SKILL_ID, cd) / 1000f));
            return false;
        }
        // AoE au cast
        try {
            long casterIdx = entityRef.getIndex();
            float dmg = CoupEstocSkill.castDamageForRank(rank);
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tc != null) {
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

    public boolean tryCastFeinte(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) { deny(playerRef, "Feinte — Duelliste requis."); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, FeintSkill.TALENT_NODE_ID);
        if (rank <= 0) { deny(playerRef, "Feinte — talent non debloque."); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = FeintSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, FeintSkill.SKILL_ID, cd)) {
            deny(playerRef, String.format("Feinte — cooldown %.1fs", cooldowns.remainingMs(uuid, FeintSkill.SKILL_ID, cd) / 1000f));
            return false;
        }
        duellisteState.startFeinte(uuid, FeintSkill.windowMs());
        if (!bypass) cooldowns.markUsed(uuid, FeintSkill.SKILL_ID);
        notifySkill(uuid, "Feinte");
        return true;
    }

    public boolean tryCastRiposte(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) { deny(playerRef, "Riposte Parfaite — Duelliste requis."); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, RiposteParfaiteSkill.TALENT_NODE_ID);
        if (rank <= 0) { deny(playerRef, "Riposte Parfaite — talent non debloque."); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = RiposteParfaiteSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, RiposteParfaiteSkill.SKILL_ID, cd)) {
            deny(playerRef, String.format("Riposte — cooldown %.1fs", cooldowns.remainingMs(uuid, RiposteParfaiteSkill.SKILL_ID, cd) / 1000f));
            return false;
        }
        duellisteState.startRiposteWindow(uuid, RiposteParfaiteSkill.windowMsForRank(rank), rank);
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
        return null;
    }

    private void notifySkill(@Nonnull UUID uuid, @Nonnull String name) {
        try {
            ClassAccount acc = classManager.getOrLoad(uuid);
            PlayerClass cls = acc.getActiveClass();
            PlayerSpecialization spec = cls != null ? acc.getActiveSpec(cls) : null;
            String icon = spec != null ? spec.getItemId() : "Weapon_Sword_Mithril";
            java.util.List<com.hypixel.hytale.server.core.universe.PlayerRef> players =
                new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers());
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr : players) {
                if (pr.getUuid().equals(uuid)) {
                    NotificationUtil.sendNotification(pr.getPacketHandler(),
                        com.hypixel.hytale.server.core.Message.raw(name), icon);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public void cleanup(@Nonnull UUID uuid) {
        cooldowns.cleanup(uuid);
    }

    private static void deny(@Nonnull PlayerRef playerRef, @Nonnull String msg) {
        playerRef.sendMessage(Message.raw(msg).color(Color.RED));
    }
}
