package fr.varyon.vrpg.classes.ability;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.duelliste.DuellistePassifs;
import fr.varyon.vrpg.classes.duelliste.PerceeSkill;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassSkillRegistry {

    private ClassSkillRegistry() {}

    @Nullable
    public static String resolveSkillId(@Nullable ClassAccount acc, @Nullable String itemId) {
        return ClassSkillSlots.resolveSkillId(acc, itemId);
    }

    @Nullable
    public static String skillIdForNode(@Nullable ClassAccount acc, int nodeIndex) {
        if (acc == null) return null;
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return null;
        PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (activeClass == PlayerClass.GUERRIER && spec == PlayerSpecialization.DUELLISTE) {
            return switch (nodeIndex) {
                case 0  -> AssautEclairSkill.SKILL_ID;
                case 1  -> ExpertEnDuelSkill.SKILL_ID;
                case 2  -> DuellistePassifs.BLESSURE_NODE;
                case 5  -> DuellistePassifs.CONTRE_NODE;
                case 6  -> PerceeSkill.SKILL_ID;
                case 7  -> DuellistePassifs.ESQUIVE_NODE;
                case 8  -> DuellistePassifs.FRAPPE_NODE;
                case 9  -> DesarmementSkill.SKILL_ID;
                case 10 -> DuellistePassifs.MOMENTUM_NODE;
                case 11 -> AssautBretteurSkill.SKILL_ID;
                default -> null;
            };
        }
        return null;
    }
}
