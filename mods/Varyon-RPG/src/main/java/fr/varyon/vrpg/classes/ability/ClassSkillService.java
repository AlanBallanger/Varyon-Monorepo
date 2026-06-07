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
import fr.varyon.vrpg.classes.duelliste.PerceeSkill;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import fr.varyon.vrpg.ui.XpNotifHud;

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
        if (PerceeSkill.SKILL_ID.equals(skillId)) {
            return tryCastPercee(uuid, playerRef, entityRef, store, commandBuffer);
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

    public boolean tryCastPercee(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                 @Nonnull Ref<EntityStore> entityRef,
                                 @Nonnull Store<EntityStore> store,
                                 @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) { deny(playerRef, "Percée — Duelliste requis."); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, PerceeSkill.TALENT_NODE_ID);
        if (rank <= 0) { deny(playerRef, "Percée — talent non débloqué."); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, PerceeSkill.SKILL_ID, PerceeSkill.cooldownMsForRank(rank))) {
            deny(playerRef, String.format("Percée — cooldown %.1fs",
                cooldowns.remainingMs(uuid, PerceeSkill.SKILL_ID, PerceeSkill.cooldownMsForRank(rank)) / 1000f));
            return false;
        }
        if (!bypass) cooldowns.markUsed(uuid, PerceeSkill.SKILL_ID);
        notifySkill(uuid, "Percée");
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
        XpNotifHud hud = XpNotifHud.get(uuid);
        if (hud != null) hud.showBurst(name, "Weapon_Sword_Mithril");
    }

    public void cleanup(@Nonnull UUID uuid) {
        cooldowns.cleanup(uuid);
    }

    private static void deny(@Nonnull PlayerRef playerRef, @Nonnull String msg) {
        playerRef.sendMessage(Message.raw(msg).color(Color.RED));
    }
}
