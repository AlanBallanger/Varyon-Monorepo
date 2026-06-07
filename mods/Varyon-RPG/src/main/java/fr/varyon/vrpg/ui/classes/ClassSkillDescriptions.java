package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.ability.ClassSkillRegistry;
import fr.varyon.vrpg.classes.ability.ExpertEnDuelSkill;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public final class ClassSkillDescriptions {

    private ClassSkillDescriptions() {}

    @Nullable
    public static String skillIdForNode(@Nullable ClassAccount acc, int nodeIndex) {
        return ClassSkillRegistry.skillIdForNode(acc, nodeIndex);
    }

    @Nonnull
    public static String effectText(@Nullable String skillId, @Nonnull String baseDescription) {
        return baseDescription.trim();
    }

    @Nullable
    public static String statLineForRank(@Nullable String skillId, int rank) {
        if (rank < 1) return null;
        if (AssautEclairSkill.SKILL_ID.equals(skillId)) return assautEclairLine(rank);
        if (ExpertEnDuelSkill.SKILL_ID.equals(skillId)) return ExpertEnDuelSkill.statLineForRank(rank);
        return null;
    }

    private static String assautEclairLine(int rank) {
        int blocks = AssautEclairSkill.blocksForRank(rank);
        String cd = formatCooldownShort(AssautEclairSkill.cooldownMsForRank(rank));
        int stamina = Math.round(AssautEclairSkill.staminaCostForRank(rank));
        return blocks + " blocs - " + cd + " - " + stamina + " endurance";
    }

    private static String formatCooldownShort(long cooldownMs) {
        if (cooldownMs % 1000L == 0L) {
            return (cooldownMs / 1000L) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", cooldownMs / 1000.0);
    }
}
