package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class ClassSkillUiSync {

    private ClassSkillUiSync() {}

    public static void hydrate(@Nonnull UUID uuid, @Nonnull RpgClassUiState state) {
        ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
        if (classManager == null) return;
        ClassAccount acc = classManager.getOrLoad(uuid);
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return;
        classManager.pruneInvalidSkillSlots(acc, activeClass);
        state.skillSlotAssignments.clear();
        state.skillSlotAssignments.putAll(acc.copySkillSlots(activeClass));
    }
}
