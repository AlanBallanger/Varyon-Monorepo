package fr.varyon.vrpg.ui.tabs.profession;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.TalentSoundNodes;
import fr.varyon.vrpg.ui.profession.ProfessionAccounts;
import fr.varyon.vrpg.ui.profession.ProfessionBonusData;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTreeDef;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTreeLogic;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTrees;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.*;
import static fr.varyon.vrpg.ui.tree.TalentTreeTheme.*;

public final class ProfessionSkillsTab {

    private ProfessionSkillsTab() {}

    public static void build(@Nonnull PlayerRef playerRef,
                             @Nonnull RpgProfessionUiState state,
                             @Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        ProfessionSkillTreeLogic.syncTalentTreeSlotIndex(state, acc);
        ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
        if (state.selectedNode >= tree.nodes.length) state.selectedNode = 0;
        ProfessionSkillTreeLogic.loadSkillRanksFromAccount(playerRef, state);
        Profession prof = ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state);
        int remainingPoints = ProfessionSkillTreeLogic.pendingRemainingPoints(playerRef, state, acc, prof);
        uiBuilder.set("#SkillTreePointsValue.TextSpans",
            Message.raw("Points restants : " + remainingPoints + " (" + prof.getDisplayName() + ")"));

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SkillTreeBackButton",
            EventData.of("Action", "tab").append("Tab", "character"),
            false
        );

        int minX = Integer.MAX_VALUE;
        for (int[] lt : tree.slotLt) if (lt[0] < minX) minX = lt[0];
        int xOffset = 58 - minX;
        int[][] shiftedLt = new int[tree.slotLt.length][2];
        for (int i = 0; i < tree.slotLt.length; i++) {
            shiftedLt[i][0] = tree.slotLt[i][0] + xOffset;
            shiftedLt[i][1] = tree.slotLt[i][1];
        }

        for (String legacyId : ProfessionSkillTrees.LEGACY_STATIC_EDGE_IDS) {
            uiBuilder.set(legacyId + ".Visible", false);
        }
        hideEdgeSegmentRange(uiBuilder, 0, SKILL_TREE_EDGE_SEGMENTS);

        int seg = tree.edgeBuilder.build(uiBuilder, 0, shiftedLt);
        hideEdgeSegmentRange(uiBuilder, seg, SKILL_TREE_EDGE_SEGMENTS);

        for (int i = 0; i < tree.nodes.length; i++) {
            String[] node = tree.nodes[i];
            String id = node[0];
            int sl = shiftedLt[i][0];
            int st = shiftedLt[i][1];

            positionSkillSlot(uiBuilder, id, sl, st);
            positionSkillRank(uiBuilder, id, sl, st);

            String rawIcon = node[5];
            String iconPath = rawIcon.isEmpty() ? null : (rawIcon.contains("/") ? rawIcon : ProfessionSkillTrees.ICON_BASE + rawIcon);
            PatchStyle iconStyle = iconPath != null ? new PatchStyle().setTexturePath(Value.of(iconPath)) : new PatchStyle();
            int allocated = ProfessionSkillTreeLogic.currentRanks(state)[i];

            uiBuilder.set("#SkillTreeNode" + id + "Slot.Visible", true);
            uiBuilder.set("#SkillTreeNode" + id + "Unlocked.Visible", true);
            uiBuilder.setObject("#SkillTreeNode" + id + "Unlocked.Background", NODE_FILL_STYLE);
            uiBuilder.set("#SkillTreeNode" + id + "Icon.Visible", iconPath != null);
            if (iconPath != null) uiBuilder.setObject("#SkillTreeNode" + id + "Icon.Background", iconStyle);
            uiBuilder.set("#SkillTreeNode" + id + "RankText.Visible", true);
            uiBuilder.set("#SkillTreeNode" + id + "RankText.TextSpans",
                Message.raw(allocated + "/" + tree.maxRanks[i]));
            uiBuilder.set("#SkillTreeNode" + id + ".Visible", true);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SkillTreeNode" + id,
                EventData.of("Action", "skill").append("Node", id),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.RightClicking,
                "#SkillTreeNode" + id,
                EventData.of("Action", "skillRight").append("Node", id),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseEntered,
                "#SkillTreeNode" + id,
                EventData.of("Action", "skillHover").append("Node", id),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseExited,
                "#SkillTreeNode" + id,
                EventData.of("Action", "skillUnhover"),
                false
            );
        }

        for (String xId : new String[]{"12", "13", "14", "15", "16"}) {
            boolean active = false;
            for (String[] n : tree.nodes) { if (n[0].equals(xId)) { active = true; break; } }
            if (!active) {
                uiBuilder.set("#SkillTreeNode" + xId + "Slot.Visible", false);
                uiBuilder.set("#SkillTreeNode" + xId + "RankText.Visible", false);
                uiBuilder.set("#SkillTreeNode" + xId + ".Visible", false);
            }
        }

        populateBonusTree(uiBuilder, eventBuilder, acc, prof, state);
        applySelectionAndHoverChrome(playerRef, state, uiBuilder);
        uiBuilder.set("#SkillTreeAttribuerButton.Visible", state.isEditMode);
        uiBuilder.set("#SkillTreeAttribuerButton.Disabled", false);
        uiBuilder.set("#SkillTreeResetButton.Visible", true);
        if (state.isEditMode) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SkillTreeAttribuerButton",
                EventData.of("Action", "saveSkills"),
                false
            );
        }
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SkillTreeResetButton",
            EventData.of("Action", "resetSkills"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SkillTreeSoundToggle",
            EventData.of("Action", "toggleTalentSound"),
            false
        );
    }

    public static void applyHoverChrome(@Nonnull PlayerRef playerRef,
                                        @Nonnull RpgProfessionUiState state,
                                        @Nonnull UICommandBuilder cmd) {
        applySelectionAndHoverChrome(playerRef, state, cmd);
    }

    private static void populateBonusTree(@Nonnull UICommandBuilder ui,
                                          @Nonnull UIEventBuilder ev,
                                          @Nullable PlayerAccount acc,
                                          @Nonnull Profession prof,
                                          @Nonnull RpgProfessionUiState state) {
        int panelLeft = 550;
        Anchor bonusAnchor = new Anchor();
        bonusAnchor.setLeft(Value.of(panelLeft));
        bonusAnchor.setTop(Value.of(0));
        bonusAnchor.setBottom(Value.of(0));
        bonusAnchor.setWidth(Value.of(90));
        ui.setObject("#BonusTreePanel.Anchor", bonusAnchor);
        ui.set("#BonusTreePanel.Visible", true);
        boolean hasPoints = acc != null && acc.availableTalentPoints(prof) > 0;
        for (int i = 0; i < 3; i++) {
            int real = acc != null ? acc.getTalentRank(prof, ProfessionBonusData.NODE_IDS[i]) : 0;
            int rank = (state.pendingBonusRanks != null) ? state.pendingBonusRanks[i] : real;
            boolean allocated = rank > 0;

            ui.set("#BonusTreeNode" + i + "Container.Visible", true);
            ui.set("#BonusTreeNode" + i + "BorderAllocated.Visible", allocated);
            ui.set("#BonusTreeNode" + i + "RankText.Visible", true);
            ui.set("#BonusTreeNode" + i + "RankText.TextSpans", Message.raw(rank + "/1"));
            String icon = ProfessionBonusData.icons(prof)[i];
            ui.setObject("#BonusTreeNode" + i + "Icon.Background",
                new PatchStyle().setTexturePath(Value.of(icon)));

            if (i < 2) {
                ui.set("#BonusTreeNode" + i + "Connector.Visible", true);
            }

            ev.addEventBinding(CustomUIEventBindingType.Activating,
                "#BonusTreeNode" + i,
                EventData.of("Action", "bonusClick").append("Index", String.valueOf(i)),
                false);
            ev.addEventBinding(CustomUIEventBindingType.MouseEntered,
                "#BonusTreeNode" + i,
                EventData.of("Action", "bonusHover").append("Index", String.valueOf(i)),
                false);
        }
    }

    private static void applySelectionAndHoverChrome(@Nonnull PlayerRef playerRef,
                                                   @Nonnull RpgProfessionUiState state,
                                                   @Nonnull UICommandBuilder uiBuilder) {
        ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
        int effectiveSelected = (state.selectedNode >= 0 && state.selectedNode < tree.nodes.length) ? state.selectedNode : 0;
        int bonusPanel = state.hoveredBonusNode >= 0 ? state.hoveredBonusNode : state.selectedBonusNode;
        if (bonusPanel >= 0 && bonusPanel < ProfessionBonusData.NODE_IDS.length) {
            PlayerAccount acc = ProfessionAccounts.get(playerRef);
            Profession prof = ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state);
            int realRank = acc != null ? acc.getTalentRank(prof, ProfessionBonusData.NODE_IDS[bonusPanel]) : 0;
            int rank = (state.pendingBonusRanks != null) ? state.pendingBonusRanks[bonusPanel] : realRank;
            uiBuilder.set("#SkillTreeSelectedTitle.TextSpans", Message.raw(ProfessionBonusData.names(prof)[bonusPanel]));
            uiBuilder.set("#SkillTreeSelectedFlavor.TextSpans", Message.raw("« " + ProfessionBonusData.descs(prof)[bonusPanel] + " »"));
            uiBuilder.set("#SkillTreeSelectedEffect.TextSpans", Message.raw(""));
            String bonusIcon = ProfessionBonusData.icons(prof)[bonusPanel];
            uiBuilder.setObject("#SkillSidebarIconImage.Background",
                bonusIcon.isEmpty() ? new PatchStyle() : new PatchStyle().setTexturePath(Value.of(bonusIcon)));
            uiBuilder.set("#SkillTreeCurrentRankValue.TextSpans", Message.raw(rank + "/1"));
            String bonusStat = ProfessionBonusData.stats(prof)[bonusPanel];
            uiBuilder.set("#SkillTreeCurrentBonusRow.Visible", rank > 0);
            if (rank > 0) uiBuilder.set("#SkillTreeCurrentBonusValue.TextSpans", Message.raw(bonusStat));
            uiBuilder.set("#SkillTreeNextRankRow.Visible", rank == 0);
            if (rank == 0) uiBuilder.set("#SkillTreeNextRankValue.TextSpans", Message.raw(bonusStat));
            uiBuilder.set("#SkillTreeTypePassif.Visible", true);
            uiBuilder.set("#SkillTreeTypeActif.Visible", false);
            uiBuilder.set("#SkillTreeTypeObjet.Visible", false);
            uiBuilder.set("#SkillTreeSoundToggle.Visible", false);
            String selectedId = tree.nodes[effectiveSelected][0];
            boolean hoverValid = state.hoveredNode >= 0 && state.hoveredNode < tree.nodes.length;
            String hoverId = hoverValid ? tree.nodes[state.hoveredNode][0] : null;
            int[] display = ProfessionSkillTreeLogic.currentRanks(state);
            for (int i = 0; i < tree.nodes.length; i++) {
                String id = tree.nodes[i][0];
                int allocated = display[i];
                boolean nodeSelected = id.equals(selectedId);
                boolean nodeHovered = hoverId != null && id.equals(hoverId);
                String borderRgb = allocated >= 1 ? NODE_BORDER_ALLOCATED : NODE_BORDER;
                uiBuilder.setObject("#SkillTreeNode" + id + "Slot.Background", new PatchStyle().setColor(Value.of(borderRgb)));
                uiBuilder.set("#SkillTreeNode" + id + "Veil.Visible", !nodeSelected && !nodeHovered && allocated == 0);
                uiBuilder.setObject("#SkillTreeNode" + id + "Veil.Background", NODE_VEIL_STYLE);
            }
            for (int i = 0; i < ProfessionBonusData.NODE_IDS.length; i++) {
                uiBuilder.set("#BonusTreeNode" + i + "BorderHovered.Visible", i == state.hoveredBonusNode);
            }
            return;
        }
        String selectedId = tree.nodes[effectiveSelected][0];
        boolean hoverValid = state.hoveredNode >= 0 && state.hoveredNode < tree.nodes.length;
        String hoverId = hoverValid ? tree.nodes[state.hoveredNode][0] : null;

        int[] display = ProfessionSkillTreeLogic.currentRanks(state);
        for (int i = 0; i < tree.nodes.length; i++) {
            String id = tree.nodes[i][0];
            int allocated = display[i];
            boolean nodeSelected = id.equals(selectedId);
            boolean nodeHovered = hoverId != null && id.equals(hoverId);
            String borderRgb;
            if (nodeHovered) {
                borderRgb = NODE_BORDER_SELECTION;
            } else if (allocated >= 1) {
                borderRgb = NODE_BORDER_ALLOCATED;
            } else {
                borderRgb = NODE_BORDER;
            }
            uiBuilder.setObject("#SkillTreeNode" + id + "Slot.Background", new PatchStyle().setColor(Value.of(borderRgb)));
            uiBuilder.set("#SkillTreeNode" + id + "Veil.Visible",
                !nodeSelected && !nodeHovered && allocated == 0);
            uiBuilder.setObject("#SkillTreeNode" + id + "Veil.Background", NODE_VEIL_STYLE);
        }
        for (int i = 0; i < ProfessionBonusData.NODE_IDS.length; i++) {
            uiBuilder.set("#BonusTreeNode" + i + "BorderHovered.Visible", i == state.hoveredBonusNode);
        }

        int panelNode = hoverValid ? state.hoveredNode : effectiveSelected;
        String[] sel = tree.nodes[panelNode];
        int rank = display[panelNode];
        int maxRank = tree.maxRanks[panelNode];
        String[] stats = (tree.nodeStatValues != null && panelNode < tree.nodeStatValues.length)
            ? tree.nodeStatValues[panelNode] : null;

        uiBuilder.set("#SkillTreeSelectedTitle.TextSpans", Message.raw(sel[1]));
        uiBuilder.set("#SkillTreeSelectedFlavor.TextSpans", Message.raw("« " + sel[3] + " »"));
        uiBuilder.set("#SkillTreeSelectedEffect.TextSpans", Message.raw(sel[4]));

        String sidebarIconPath = sel[5].contains("/") ? sel[5] : ProfessionSkillTrees.ICON_BASE + sel[5];
        uiBuilder.setObject("#SkillSidebarIconImage.Background",
            new PatchStyle().setTexturePath(Value.of(sidebarIconPath)));

        uiBuilder.set("#SkillTreeCurrentRankValue.TextSpans",
            Message.raw(rank + "/" + maxRank));

        boolean hasCurrent = rank > 0 && stats != null;
        uiBuilder.set("#SkillTreeCurrentBonusRow.Visible", hasCurrent);
        if (hasCurrent) {
            uiBuilder.set("#SkillTreeCurrentBonusValue.TextSpans", Message.raw(stats[rank - 1]));
        }

        boolean hasNext = rank < maxRank && stats != null;
        uiBuilder.set("#SkillTreeNextRankRow.Visible", hasNext);
        if (hasNext) {
            uiBuilder.set("#SkillTreeNextRankValue.TextSpans", Message.raw(stats[rank]));
        }

        String type = sel[2];
        uiBuilder.set("#SkillTreeTypePassif.Visible", "Passif".equals(type));
        uiBuilder.set("#SkillTreeTypeActif.Visible",  "Actif".equals(type));
        uiBuilder.set("#SkillTreeTypeObjet.Visible",  "Objet".equals(type));

        applyTalentSoundToggle(playerRef, state, uiBuilder, sel[0]);
    }

    private static void applyTalentSoundToggle(@Nonnull PlayerRef playerRef,
                                               @Nonnull RpgProfessionUiState state,
                                               @Nonnull UICommandBuilder uiBuilder,
                                               @Nonnull String panelNodeId) {
        Profession profession = ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state);
        boolean show = TalentSoundNodes.hasSoundToggle(profession, panelNodeId);
        uiBuilder.set("#SkillTreeSoundToggle.Visible", show);
        if (!show) return;
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        boolean soundOn = acc == null || acc.isTalentSoundEnabled(profession, panelNodeId);
        String icon = soundOn ? ProfessionBonusData.TALENT_SOUND_ICON_ON : ProfessionBonusData.TALENT_SOUND_ICON_OFF;
        uiBuilder.setObject("#SkillTreeSoundToggleIcon.Background",
            new PatchStyle().setTexturePath(Value.of(icon)));
    }
}
