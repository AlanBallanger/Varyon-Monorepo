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
import fr.varyon.vrpg.classes.ability.ClassSkillSlotIds;
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

        uiBuilder.set("#ClassTreeMainTitle.TextSpans", Message.raw("Arbre de talents"));
        int remaining = acc != null && activeClass != null
            ? ClassTalentTreeLogic.pendingRemainingPoints(state, acc, activeClass)
            : 0;
        String specName = acc != null && activeClass != null && acc.getActiveSpec(activeClass) != null
            ? acc.getActiveSpec(activeClass).getDisplayName() : activeClass != null ? activeClass.getDisplayName() : "";
        uiBuilder.set("#ClassTreePointsLabel.TextSpans",
            Message.raw("Points restants : " + remaining + " (" + specName + ")"));

        uiBuilder.set("#ClassTreeTalentsPanel.Visible", true);
        uiBuilder.set("#ClassTreeSkillsPanel.Visible", false);
        uiBuilder.set("#ClassTreeTabTalentsButton.Visible", false);
        uiBuilder.set("#ClassTreeTabSkillsButton.Visible", false);
        uiBuilder.set("#ClassTreeTabTalentsUnderline.Visible", false);
        uiBuilder.set("#ClassTreeTabSkillsUnderline.Visible", false);

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

        applyKeybindPanel(cmd, null, state, acc, activeClass, panelTalent, -1);
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
            boolean isActif = "Actif".equals(node.type());
            java.awt.Color typeColor = isActif
                ? new java.awt.Color(0xFF, 0xD7, 0x00)
                : new java.awt.Color(0x6B, 0xCB, 0x7A);
            uiBuilder.set("#ClassTreeNode" + id + "TypeText.Visible", true);
            uiBuilder.set("#ClassTreeNode" + id + "TypeText.TextSpans",
                Message.raw(isActif ? "A" : "P").color(typeColor));
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
            eventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited,
                "#ClassTreeNode" + id,
                EventData.of("Action", "classtreeUnhover"), false);
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

        applyKeybindPanel(uiBuilder, eventBuilder, state, acc, activeClass, panelTalent, effectiveHov >= 0 ? -1 : effectiveSel);
    }

    private static final String[] SLOT_ORDER = {
        ClassSkillSlotIds.E,
        ClassSkillSlotIds.R,
        ClassSkillSlotIds.A,
        ClassSkillSlotIds.CROUCH_E,
        ClassSkillSlotIds.CROUCH_R,
        ClassSkillSlotIds.CROUCH_A
    };

    private static final java.util.Map<String, String> SLOT_LABELS = java.util.Map.of(
        ClassSkillSlotIds.E, "E",
        ClassSkillSlotIds.R, "R",
        ClassSkillSlotIds.A, "A",
        ClassSkillSlotIds.CROUCH_E, "Crouch+E",
        ClassSkillSlotIds.CROUCH_R, "Crouch+R",
        ClassSkillSlotIds.CROUCH_A, "Crouch+A"
    );

    private static final java.util.Map<String, String> SLOT_SHORT_LABELS = java.util.Map.of(
        ClassSkillSlotIds.E, "E",
        ClassSkillSlotIds.R, "R",
        ClassSkillSlotIds.A, "A",
        ClassSkillSlotIds.CROUCH_E, "E2",
        ClassSkillSlotIds.CROUCH_R, "R2",
        ClassSkillSlotIds.CROUCH_A, "A2"
    );

    private static void applyKeybindPanel(@Nonnull UICommandBuilder ui,
                                          @Nullable UIEventBuilder eventBuilder,
                                          @Nonnull RpgClassUiState state,
                                          @Nullable ClassAccount acc,
                                          @Nullable PlayerClass activeClass,
                                          @Nonnull ClassTalentTree.Node panelTalent,
                                          int selectedActiveNode) {
        boolean isActif = "Actif".equals(panelTalent.type());
        ui.set("#ClassTreeKeybindPanel.Visible", isActif);
        if (!isActif) return;

        String nodeItemId = panelTalent.itemId();

        String currentSlot = null;
        for (String slotId : SLOT_ORDER) {
            if (nodeItemId.equals(state.skillSlotAssignments.get(slotId))) {
                currentSlot = slotId;
                break;
            }
        }

        boolean picking = nodeItemId.equals(state.treeKeybindPickingItemId);

        boolean isBound = currentSlot != null;
        String slotShortText = isBound ? SLOT_SHORT_LABELS.get(currentSlot) : "";
        String slotFullText  = isBound ? SLOT_LABELS.get(currentSlot) : "";
        ui.set("#ClassTreeKbSlotLabel.Visible", isBound);
        ui.set("#ClassTreeKbSlotLabel.TextSpans", isBound ? Message.raw(slotShortText) : Message.raw(""));
        ui.set("#ClassTreeKbSlotBorder.Visible", isBound);
        ui.set("#ClassTreeKbCurrentLabel.TextSpans", isBound
            ? Message.raw("Assigné : " + slotFullText)
            : Message.raw("Non assigné"));

        ui.set("#ClassTreeKbPickerPanel.Visible", picking);
        ui.set("#ClassTreeKbSlotButton.Visible", true);

        if (eventBuilder != null) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ClassTreeKbSlotButton",
                EventData.of("Action", "treeKeybindPick").append("Node", nodeItemId), false);
        }

        // Panneau de sélection des 6 slots
        for (String slotId : SLOT_ORDER) {
            boolean isCurrentSlot = slotId.equals(currentSlot);
            String otherItemId = state.skillSlotAssignments.get(slotId);
            boolean hasOther = otherItemId != null && !otherItemId.equals(nodeItemId);

            String suffix = hasOther ? " (occupé)" : "";
            ui.set("#ClassTreeKb" + slotId + "Label.TextSpans", Message.raw(SLOT_LABELS.get(slotId) + suffix));

            String borderHex = isCurrentSlot ? "#6BCB7A" : "#555555";
            ui.setObject("#ClassTreeKb" + slotId + "Border.Background",
                new PatchStyle().setColor(Value.of(borderHex)));

            if (eventBuilder != null && picking) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ClassTreeKb" + slotId,
                    EventData.of("Action", "treeKeybindAssign")
                        .append("Slot", slotId).append("Node", nodeItemId), false);
            }
        }

        // Bouton Désassigner dans le picker (seulement si un slot est assigné)
        ui.set("#ClassTreeKbClearButton.Visible", picking && isBound);
        if (eventBuilder != null && picking && isBound) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ClassTreeKbClearButton",
                EventData.of("Action", "skillSlotClear").append("Slot", currentSlot), false);
        }
    }

    @Nullable
    private static String resolveIconForItemId(@Nonnull String itemId, @Nonnull ClassAccount acc) {
        ClassTalentTree.Node[] nodes = ClassTalentTreeLayouts.talentNodes(acc);
        for (ClassTalentTree.Node n : nodes) {
            if (itemId.equals(n.itemId())) return n.itemId();
        }
        return null;
    }

    private static void applyTalentDetail(@Nonnull UICommandBuilder ui,
                                          @Nullable ClassAccount acc,
                                          int panelNode,
                                          @Nonnull ClassTalentTree.Node panelTalent,
                                          int panelRank) {
        String skillId = ClassSkillDescriptions.skillIdForNode(acc, panelNode);
        String iconRef = panelTalent.itemId();
        if (iconRef.contains("/")) {
            ui.set("#ClassSidebarItemIcon.Visible", false);
            ui.set("#ClassSidebarCustomIcon.Visible", true);
            ui.setObject("#ClassSidebarCustomIcon.Background",
                new PatchStyle().setTexturePath(Value.of(iconRef)));
        } else {
            ui.set("#ClassSidebarItemIcon.Visible", true);
            ui.set("#ClassSidebarCustomIcon.Visible", false);
            ui.set("#ClassSidebarItemIcon.ItemId", iconRef);
        }
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

}
