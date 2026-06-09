package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.ability.ClassSkillRegistry;
import fr.varyon.vrpg.classes.ability.ExpertEnDuelSkill;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.duelliste.DuellistePassifs;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.classes.duelliste.RiposteParfaiteSkill;

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
        if (rank < 1 || skillId == null) return null;
        return switch (skillId) {
            case AssautEclairSkill.SKILL_ID      -> assautEclairLine(rank);
            case ExpertEnDuelSkill.SKILL_ID      -> ExpertEnDuelSkill.statLineForRank(rank);
            case AssautBretteurSkill.SKILL_ID    -> AssautBretteurSkill.statLineForRank(rank);
            case DesarmementSkill.SKILL_ID       -> DesarmementSkill.statLineForRank(rank);
            case CoupEstocSkill.SKILL_ID         -> CoupEstocSkill.statLineForRank(rank);
            case FeintSkill.SKILL_ID             -> FeintSkill.statLineForRank(rank);
            case RiposteParfaiteSkill.SKILL_ID   -> RiposteParfaiteSkill.statLineForRank(rank);
            case DuellistePassifs.BLESSURE_NODE -> DuellistePassifs.bleedStatLine(rank);
            case DuellistePassifs.FRAPPE_NODE   -> DuellistePassifs.critStatLine(rank);
            case DuellistePassifs.CONTRE_NODE   -> DuellistePassifs.contreStatLine(rank);
            case DuellistePassifs.ESQUIVE_NODE  -> DuellistePassifs.dodgeStatLine(rank);
            case DuellistePassifs.MOMENTUM_NODE -> DuellistePassifs.momentumStatLine(rank);
            default -> null;
        };
    }

    private static String assautEclairLine(int rank) {
        int pct     = Math.round(AssautEclairSkill.damageFactor(rank) * 100);
        String cd   = formatCooldownShort(AssautEclairSkill.cooldownMsForRank(rank));
        int stamina = Math.round(AssautEclairSkill.staminaCostForRank(rank));
        return pct + "% dégâts arme, CD " + cd + " - " + stamina + " endurance";
    }

    private static String formatCooldownShort(long cooldownMs) {
        if (cooldownMs % 1000L == 0L) {
            return (cooldownMs / 1000L) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", cooldownMs / 1000.0);
    }
}
