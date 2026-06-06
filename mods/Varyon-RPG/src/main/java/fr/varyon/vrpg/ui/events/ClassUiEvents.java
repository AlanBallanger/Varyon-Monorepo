package fr.varyon.vrpg.ui.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.RpgMainUI;
import fr.varyon.vrpg.ui.classes.RpgClassUiState;
import fr.varyon.vrpg.ui.classes.layout.ClassTalentTreeLayouts;
import fr.varyon.vrpg.ui.tabs.classes.ClassTalentsTab;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class ClassUiEvents {

    private static final Logger LOG = Logger.getLogger(ClassUiEvents.class.getName());

    private ClassUiEvents() {}

    public static UiEventResult handle(@Nonnull PlayerRef playerRef,
                                       @Nonnull RpgClassUiState state,
                                       @Nonnull Consumer<String> setActiveTab,
                                       @Nonnull RpgMainUI.Data data) {
        if (data.action == null) return UiEventResult.NONE;

        if ("selectClass".equals(data.action) && data.classId != null) {
            PlayerClass c = PlayerClass.fromId(data.classId);
            ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
            if (c != null && classManager != null) {
                classManager.setActiveClass(playerRef.getUuid(), c);
                classManager.applyStats(playerRef.getUuid(), playerRef);
            }
            state.pendingClassRanks = null;
            state.selectedClassNode = 0;
            setActiveTab.accept(ClassTalentsTab.TAB_CLASSES);
            return UiEventResult.REBUILD;
        }

        if ("pendingSpec".equals(data.action) && data.specId != null) {
            state.pendingSpecId = data.specId.equals(state.pendingSpecId) ? null : data.specId;
            return UiEventResult.REBUILD;
        }

        if ("selectSpec".equals(data.action) && data.specId != null) {
            state.pendingSpecId = null;
            state.pendingClassRanks = null;
            state.selectedClassNode = 0;
            ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
            if (classManager != null) {
                ClassAccount acc = classManager.getAccount(playerRef.getUuid());
                PlayerClass activeClass = acc != null ? acc.getActiveClass() : null;
                if (activeClass != null) {
                    PlayerSpecialization spec = PlayerSpecialization.fromId(data.specId);
                    if (spec != null && spec.getParentClass() == activeClass) {
                        classManager.setActiveSpec(playerRef.getUuid(), activeClass, spec);
                        classManager.applyStats(playerRef.getUuid(), playerRef);
                    }
                }
            }
            return UiEventResult.REBUILD;
        }

        if ("switchProfile".equals(data.action) && data.index != null) {
            int idx;
            try { idx = Integer.parseInt(data.index); } catch (NumberFormatException e) { return UiEventResult.NONE; }
            ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
            if (classManager != null) classManager.switchProfile(playerRef.getUuid(), idx, playerRef);
            return UiEventResult.REBUILD;
        }

        if ("classTab".equals(data.action) && data.classId != null) {
            PlayerClass c = PlayerClass.fromId(data.classId);
            if (c != null) {
                ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
                if (classManager != null) {
                    ClassAccount acc = classManager.getAccount(playerRef.getUuid());
                    if (acc != null) acc.setActiveClass(c);
                }
            }
            return UiEventResult.REBUILD;
        }

        if ("classtreeSubTab".equals(data.action) && data.sub != null) {
            LOG.info("[RPG-SubTab] classtreeSubTab received, sub=" + data.sub);
            state.classTreeSubTab = data.sub;
            state.selectedSkillSlot = null;
            return UiEventResult.REBUILD;
        }
        if ("classtreeSkill".equals(data.action) && data.node != null) {
            try {
                int idx = Integer.parseInt(data.node);
                state.selectedClassNode = idx;
                state.hoveredClassNode = -1;
            } catch (NumberFormatException ignored) {}
            return UiEventResult.REBUILD;
        }
        if ("classtreeHover".equals(data.action) && data.node != null) {
            try {
                state.hoveredClassNode = Integer.parseInt(data.node);
            } catch (NumberFormatException ignored) {}
            return UiEventResult.HOVER_UPDATE;
        }
        if ("classtreeAttribuer".equals(data.action) && data.node != null) {
            ClassManager classManager2 = VaryonRpgPlugin.getInstance().getClassManager();
            if (classManager2 != null) {
                ClassAccount acc2 = classManager2.getAccount(playerRef.getUuid());
                PlayerClass activeClass2 = acc2 != null ? acc2.getActiveClass() : null;
                if (activeClass2 != null) {
                    try {
                        int idx = Integer.parseInt(data.node);
                        ClassTalentTree.Node[] nodes = ClassTalentTreeLayouts.talentNodes(acc2);
                        if (idx >= 0 && idx < nodes.length) {
                            boolean allocated = classManager2.allocateTalent(
                                playerRef.getUuid(), activeClass2, String.valueOf(idx), nodes[idx].maxRank());
                            if (allocated) {
                                state.pendingClassRanks = null;
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            return UiEventResult.REBUILD;
        }
        if ("classtreeReset".equals(data.action)) {
            ClassManager classManager3 = VaryonRpgPlugin.getInstance().getClassManager();
            if (classManager3 != null) {
                ClassAccount acc3 = classManager3.getAccount(playerRef.getUuid());
                PlayerClass activeClass3 = acc3 != null ? acc3.getActiveClass() : null;
                if (activeClass3 != null) {
                    classManager3.resetTalents(playerRef.getUuid(), activeClass3);
                }
            }
            state.pendingClassRanks = null;
            state.selectedClassNode = 0;
            return UiEventResult.REBUILD;
        }

        if ("skillSlotClick".equals(data.action) && data.slot != null) {
            state.selectedSkillSlot = data.slot.equals(state.selectedSkillSlot) ? null : data.slot;
            return UiEventResult.REBUILD;
        }
        if ("skillFilter".equals(data.action) && data.filter != null) {
            state.skillFilter = data.filter;
            return UiEventResult.REBUILD;
        }
        if ("skillSlotsReset".equals(data.action)) {
            state.skillSlotAssignments.clear();
            state.selectedSkillSlot = null;
            return UiEventResult.REBUILD;
        }
        if ("skillSlotAssign".equals(data.action) && data.slot != null && data.node != null) {
            state.skillSlotAssignments.put(data.slot, data.node);
            state.selectedSkillSlot = null;
            return UiEventResult.REBUILD;
        }
        if ("skillSlotClear".equals(data.action) && data.slot != null) {
            state.skillSlotAssignments.remove(data.slot);
            return UiEventResult.REBUILD;
        }

        return UiEventResult.NONE;
    }
}
