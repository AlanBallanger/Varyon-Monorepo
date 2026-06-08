package fr.varyon.vrpg.ui.tabs.classes;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.ui.classes.ClassSkillDescriptions;
import fr.varyon.vrpg.ui.classes.ClassTalentTreeLogic;
import fr.varyon.vrpg.ui.classes.ClassUnlockedActiveSkills;
import fr.varyon.vrpg.ui.classes.RpgClassUiState;
import fr.varyon.vrpg.ui.classes.layout.ClassTalentTreeLayouts;
import fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout;
import fr.varyon.vrpg.ui.tree.TalentTreeTheme;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static fr.varyon.vrpg.ui.tree.TalentTreeTheme.*;

public final class ClassTalentsTab {

    public static final String TAB_CLASSES = "classes";

    private ClassTalentsTab() {}

    public static void build(@Nonnull PlayerRef playerRef,
                             @Nonnull RpgClassUiState state,
                             @Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
        ClassAccount acc = classManager != null ? classManager.getAccount(playerRef.getUuid()) : null;
        PlayerClass activeClass = acc != null ? acc.getActiveClass() : null;

        boolean isTalentsSubTab = "talents".equals(state.classTreeSubTab);
        uiBuilder.set("#ClassTreeMainTitle.TextSpans", Message.raw("Arbre de talents"));

        uiBuilder.set("#ClassTreeTalentsPanel.Visible", isTalentsSubTab);
        uiBuilder.set("#ClassTreeSkillsPanel.Visible", !isTalentsSubTab);
        uiBuilder.set("#ClassTreeTabTalentsUnderline.Visible", isTalentsSubTab);
        uiBuilder.set("#ClassTreeTabSkillsUnderline.Visible", !isTalentsSubTab);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SkillSlotsResetButton",
            EventData.of("Action", "skillSlotsReset"), false);

        String[] filters = {"all", "attaque", "defense", "soutien", "mobilite"};
        String[] filterIds = {"All", "Attaque", "Defense", "Soutien", "Mobilite"};
        for (int fi = 0; fi < filters.length; fi++) {
            boolean active = filters[fi].equals(state.skillFilter);
            uiBuilder.set("#SkillFilter" + filterIds[fi] + "Underline.Visible", active);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#SkillFilter" + filterIds[fi],
                EventData.of("Action", "skillFilter").append("Filter", filters[fi]), false);
        }
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ClassTreeTabTalentsButton",
            EventData.of("Action", "classtreeSubTab").append("Sub", "talents"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ClassTreeTabSkillsButton",
            EventData.of("Action", "classtreeSubTab").append("Sub", "skills"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ClassTreeBackButton",
            EventData.of("Action", "tab").append("Tab", TAB_CLASSES), false);

        ClassTalentTree.Node[] talentNodes = ClassTalentTreeLayouts.talentNodes(acc);
        int[][] shifted = ClassTalentTreeLayouts.positionsForAccount(acc);
        ClassTalentTreeLayouts.buildEdges(acc, uiBuilder, 0);

        ClassTalentTreeLogic.syncDisplayRanks(state, acc, activeClass, talentNodes.length);

        int effectiveSel = (state.selectedClassNode >= 0 && state.selectedClassNode < talentNodes.length)
            ? state.selectedClassNode : 0;
        int effectiveHov = (state.hoveredClassNode >= 0 && state.hoveredClassNode < talentNodes.length)
            ? state.hoveredClassNode : -1;

        renderNodes(uiBuilder, eventBuilder, talentNodes, shifted, state, effectiveSel, effectiveHov);
        renderDetailPanel(uiBuilder, eventBuilder, talentNodes, state, acc, activeClass, effectiveSel, effectiveHov);
        renderSkillSlots(uiBuilder, eventBuilder, talentNodes, state);
        renderSkillPicker(uiBuilder, eventBuilder, acc, state);
    }

    public static void applyHoverChrome(@Nonnull PlayerRef playerRef,
                                        @Nonnull RpgClassUiState state,
                                        @Nonnull UICommandBuilder cmd) {
        ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
        ClassAccount acc = classManager != null ? classManager.getAccount(playerRef.getUuid()) : null;
        ClassTalentTree.Node[] talentNodes = ClassTalentTreeLayouts.talentNodes(acc);
        PlayerClass activeClass = acc != null ? acc.getActiveClass() : null;

        int effectiveSel = (state.selectedClassNode >= 0 && state.selectedClassNode < talentNodes.length)
            ? state.selectedClassNode : 0;
        int effectiveHov = (state.hoveredClassNode >= 0 && state.hoveredClassNode < talentNodes.length)
            ? state.hoveredClassNode : -1;

        for (int i = 0; i < Math.min(12, talentNodes.length); i++) {
            applyNodeChrome(cmd, state, i, i == effectiveSel, i == effectiveHov);
        }

        int panelNode = effectiveHov >= 0 ? effectiveHov : effectiveSel;
        ClassTalentTree.Node panelTalent = talentNodes[panelNode];
        int panelRank = state.pendingClassRanks != null && panelNode < state.pendingClassRanks.length
            ? state.pendingClassRanks[panelNode] : 0;

        applyTalentDetail(cmd, acc, panelNode, panelTalent, panelRank);
        String type = panelTalent.type();
        cmd.set("#ClassTreeTypePassif.Visible", "Passif".equals(type));
        cmd.set("#ClassTreeTypeActif.Visible", "Actif".equals(type));
        cmd.set("#ClassTreeTypeObjet.Visible", "Objet".equals(type));

        cmd.set("#ClassTreeAttribuerButton.Visible", state.classEditMode);

    }

    private static void renderNodes(@Nonnull UICommandBuilder uiBuilder,
                                    @Nonnull UIEventBuilder eventBuilder,
                                    @Nonnull ClassTalentTree.Node[] talentNodes,
                                    @Nonnull int[][] shifted,
                                    @Nonnull RpgClassUiState state,
                                    int effectiveSel,
                                    int effectiveHov) {
        for (int i = 0; i < 12; i++) {
            String id = String.valueOf(i);
            int sl = shifted[i][0];
            int st = shifted[i][1];
            ClassTalentTree.Node node = talentNodes[i];
            int rank = state.pendingClassRanks != null && i < state.pendingClassRanks.length
                ? state.pendingClassRanks[i] : 0;

            TalentTreeEdgeLayout.positionClassSlot(uiBuilder, id, sl, st);
            TalentTreeEdgeLayout.positionClassRank(uiBuilder, id, sl, st);
            uiBuilder.set("#ClassTreeNode" + id + "Slot.Visible", true);
            uiBuilder.set("#ClassTreeNode" + id + "Unlocked.Visible", true);
            uiBuilder.setObject("#ClassTreeNode" + id + "Unlocked.Background", NODE_FILL_STYLE);
            String iconRef = node.itemId();
            if (iconRef.contains("/")) {
                uiBuilder.set("#ClassTreeNode" + id + "Icon.Visible", false);
                uiBuilder.set("#ClassTreeNode" + id + "CustomIcon.Visible", true);
                uiBuilder.setObject("#ClassTreeNode" + id + "CustomIcon.Background",
                    new PatchStyle().setTexturePath(Value.of(iconRef)));
            } else {
                uiBuilder.set("#ClassTreeNode" + id + "Icon.Visible", true);
                uiBuilder.set("#ClassTreeNode" + id + "CustomIcon.Visible", false);
                uiBuilder.set("#ClassTreeNode" + id + "Icon.ItemId", iconRef);
            }
            uiBuilder.set("#ClassTreeNode" + id + "RankText.Visible", true);
            uiBuilder.set("#ClassTreeNode" + id + "RankText.TextSpans", Message.raw(rank + "/" + node.maxRank()));
            uiBuilder.set("#ClassTreeNode" + id + ".Visible", true);

            applyNodeChrome(uiBuilder, state, i, i == effectiveSel, i == effectiveHov);

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ClassTreeNode" + id,
                EventData.of("Action", "classtreeSkill").append("Node", id), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.RightClicking,
                "#ClassTreeNode" + id,
                EventData.of("Action", "classtreeSkillRight").append("Node", id), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.MouseEntered,
                "#ClassTreeNode" + id,
                EventData.of("Action", "classtreeHover").append("Node", id), false);
        }
    }

    private static void applyNodeChrome(@Nonnull UICommandBuilder uiBuilder,
                                        @Nonnull RpgClassUiState state,
                                        int i,
                                        boolean nodeSelected,
                                        boolean nodeHovered) {
        String id = String.valueOf(i);
        int rank = state.pendingClassRanks != null && i < state.pendingClassRanks.length
            ? state.pendingClassRanks[i] : 0;
        String borderRgb;
        if (nodeHovered) {
            borderRgb = NODE_BORDER_SELECTION;
        } else if (rank >= 1) {
            borderRgb = NODE_BORDER_ALLOCATED;
        } else {
            borderRgb = NODE_BORDER;
        }
        uiBuilder.setObject("#ClassTreeNode" + id + "Slot.Background",
            new PatchStyle().setColor(Value.of(borderRgb)));
        uiBuilder.set("#ClassTreeNode" + id + "Veil.Visible", !nodeSelected && !nodeHovered && rank == 0);
        uiBuilder.setObject("#ClassTreeNode" + id + "Veil.Background", NODE_VEIL_STYLE);
    }

    private static void renderDetailPanel(@Nonnull UICommandBuilder uiBuilder,
                                          @Nonnull UIEventBuilder eventBuilder,
                                          @Nonnull ClassTalentTree.Node[] talentNodes,
                                          @Nonnull RpgClassUiState state,
                                          ClassAccount acc,
                                          PlayerClass activeClass,
                                          int effectiveSel,
                                          int effectiveHov) {
        int panelNode = effectiveHov >= 0 ? effectiveHov : effectiveSel;
        ClassTalentTree.Node panelTalent = talentNodes[panelNode];
        int panelRank = state.pendingClassRanks != null && panelNode < state.pendingClassRanks.length
            ? state.pendingClassRanks[panelNode] : 0;

        applyTalentDetail(uiBuilder, acc, panelNode, panelTalent, panelRank);

        String type = panelTalent.type();
        uiBuilder.set("#ClassTreeTypePassif.Visible", "Passif".equals(type));
        uiBuilder.set("#ClassTreeTypeActif.Visible", "Actif".equals(type));
        uiBuilder.set("#ClassTreeTypeObjet.Visible", "Objet".equals(type));

        uiBuilder.set("#ClassTreeAttribuerButton.Visible", state.classEditMode);
        uiBuilder.set("#ClassTreeResetButton.Visible", acc != null && activeClass != null);

        if (state.classEditMode) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ClassTreeAttribuerButton",
                EventData.of("Action", "classtreeAttribuer"),
                false);
        }
        if (acc != null && activeClass != null) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ClassTreeResetButton",
                EventData.of("Action", "classtreeReset"), false);
        }
    }

    private static void renderSkillSlots(@Nonnull UICommandBuilder uiBuilder,
                                         @Nonnull UIEventBuilder eventBuilder,
                                         @Nonnull ClassTalentTree.Node[] talentNodes,
                                         @Nonnull RpgClassUiState state) {
        String[] slotIds = {"E", "R", "CrouchA", "CrouchE", "CrouchR", "A"};
        for (String slotId : slotIds) {
            String assigned = state.skillSlotAssignments.get(slotId);
            if (assigned != null) {
                for (ClassTalentTree.Node n : talentNodes) {
                    if (n.itemId().equals(assigned)) {
                        uiBuilder.set("#SkillSlot" + slotId + "Icon.ItemId", n.itemId());
                        uiBuilder.set("#SkillSlot" + slotId + "Icon.Visible", true);
                        uiBuilder.set("#SkillSlot" + slotId + "Name.TextSpans", Message.raw(n.name()));
                        break;
                    }
                }
            } else {
                uiBuilder.set("#SkillSlot" + slotId + "Icon.Visible", false);
                uiBuilder.set("#SkillSlot" + slotId + "Name.TextSpans", Message.raw(""));
            }
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#SkillSlot" + slotId,
                EventData.of("Action", "skillSlotClick").append("Slot", slotId), false);
        }
    }

    private static void applyTalentDetail(@Nonnull UICommandBuilder ui,
                                          @Nullable ClassAccount acc,
                                          int panelNode,
                                          @Nonnull ClassTalentTree.Node panelTalent,
                                          int panelRank) {
        String skillId = ClassSkillDescriptions.skillIdForNode(acc, panelNode);
        ui.set("#ClassTreeSelectedTitle.TextSpans", Message.raw(panelTalent.name()));
        ui.set("#ClassTreeSelectedFlavor.TextSpans", Message.raw("« " + panelTalent.flavor() + " »"));
        ui.set("#ClassTreeSelectedEffect.TextSpans", Message.raw(
            ClassSkillDescriptions.effectText(skillId, panelTalent.description())));
        ui.set("#ClassTreeCurrentRankValue.TextSpans",
            Message.raw(panelRank + "/" + panelTalent.maxRank()));

        boolean hasCurrent = panelRank > 0 && skillId != null;
        ui.set("#ClassTreeCurrentBonusRow.Visible", hasCurrent);
        if (hasCurrent) {
            String current = ClassSkillDescriptions.statLineForRank(skillId, panelRank);
            if (current != null) {
                ui.set("#ClassTreeCurrentBonusValue.TextSpans", Message.raw(current));
            }
        }

        boolean hasNext = panelRank < panelTalent.maxRank() && skillId != null;
        ui.set("#ClassTreeNextRankRow.Visible", hasNext);
        if (hasNext) {
            String next = ClassSkillDescriptions.statLineForRank(skillId, panelRank + 1);
            if (next != null) {
                ui.set("#ClassTreeNextRankValue.TextSpans", Message.raw(next));
            }
        }
    }

    private static void renderSkillPicker(@Nonnull UICommandBuilder uiBuilder,
                                          @Nonnull UIEventBuilder eventBuilder,
                                          ClassAccount acc,
                                          @Nonnull RpgClassUiState state) {
        boolean pickerOpen = state.selectedSkillSlot != null;
        uiBuilder.set("#SkillPickerPanel.Visible", pickerOpen);
        if (!pickerOpen) return;

        String[] slotLabels = {"Sort 1 (E)", "Sort 2 (R)", "Sort 3 (Crouch+A)", "Sort 4 (Crouch+E)", "Sort 5 (Crouch+R)", "Sort 6 (A)"};
        String[] slotIdsLabels = {"E", "R", "CrouchA", "CrouchE", "CrouchR", "A"};
        String slotLabel = state.selectedSkillSlot;
        for (int i = 0; i < slotIdsLabels.length; i++) {
            if (slotIdsLabels[i].equals(state.selectedSkillSlot)) {
                slotLabel = slotLabels[i];
                break;
            }
        }
        uiBuilder.set("#SkillPickerSlotLabel.TextSpans", Message.raw("Assigner à : " + slotLabel));
        uiBuilder.clear("#SkillPickerList");

        var unlockedActives = ClassUnlockedActiveSkills.list(acc);
        for (int i = 0; i < unlockedActives.size(); i++) {
            ClassTalentTree.Node n = unlockedActives.get(i).node();
            uiBuilder.append("#SkillPickerList", "CharacterTabClassTalents_SkillEntry.ui");
            ClassesTab.applySkillEntryIcon(uiBuilder, "#SkillPickerList[" + i + "]", n.itemId());
            uiBuilder.set("#SkillPickerList[" + i + "] #SkillEntryName.TextSpans", Message.raw(n.name()));
            uiBuilder.set("#SkillPickerList[" + i + "] #SkillEntryAssign.Visible", true);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#SkillPickerList[" + i + "] #SkillEntryAssign",
                EventData.of("Action", "skillSlotAssign")
                    .append("Slot", state.selectedSkillSlot)
                    .append("Node", n.itemId()), false);
        }
    }
}
