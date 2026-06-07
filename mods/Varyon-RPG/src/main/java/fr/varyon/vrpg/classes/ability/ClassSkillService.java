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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.UUID;

public final class ClassSkillService {

    private final ClassManager classManager;
    private final ClassSkillCooldowns cooldowns = new ClassSkillCooldowns();

    public ClassSkillService(@Nonnull ClassManager classManager) {
        this.classManager = classManager;
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
        if (cooldowns.isOnCooldown(uuid, AssautEclairSkill.SKILL_ID, cooldownMs)) {
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

    public void cleanup(@Nonnull UUID uuid) {
        cooldowns.cleanup(uuid);
    }

    private static void deny(@Nonnull PlayerRef playerRef, @Nonnull String msg) {
        playerRef.sendMessage(Message.raw(msg).color(Color.RED));
    }
}
