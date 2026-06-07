package fr.varyon.vrpg.classes.ability;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
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
            if (nodeIndex == 0) return AssautEclairSkill.SKILL_ID;
            if (nodeIndex == 1) return ExpertEnDuelSkill.SKILL_ID;
        }
        return null;
    }
}
