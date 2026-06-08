package fr.varyon.vrpg.ui.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.rpg.TalentSoundNodes;
import fr.varyon.vrpg.rpg.XpCurve;
import fr.varyon.vrpg.ui.RpgMainUI;
import fr.varyon.vrpg.ui.profession.ProfessionAccounts;
import fr.varyon.vrpg.ui.profession.ProfessionBonusData;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTreeDef;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTreeLogic;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTrees;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.logging.Logger;

public final class ProfessionUiEvents {

    private static final Logger LOG = Logger.getLogger(ProfessionUiEvents.class.getName());

    private ProfessionUiEvents() {}

    public static UiEventResult handle(@Nonnull PlayerRef playerRef,
                                       @Nonnull RpgProfessionUiState state,
                                       @Nonnull RpgMainUI.Data data) {
        if (data.action == null) return UiEventResult.NONE;

        if ("classementFilter".equals(data.action) && data.professionId != null) {
            Profession p = Profession.fromId(data.professionId);
            if (p != null) {
                state.classementFilter = p;
                return UiEventResult.REBUILD;
            }
            return UiEventResult.NONE;
        }
        if ("classementOrder".equals(data.action)) {
            state.classementAsc = !state.classementAsc;
            return UiEventResult.REBUILD;
        }

        if ("talentSlotPick".equals(data.action) && data.talentSlot != null) {
            state.talentTreeSlotIndex = "1".equals(data.talentSlot) ? 1 : 0;
            state.selectedNode = 0;
            state.hoveredNode = -1;
            ProfessionSkillTreeLogic.exitEditMode(state);
            return UiEventResult.REBUILD;
        }

        if ("skill".equals(data.action) && data.node != null) {
            ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
            for (int i = 0; i < tree.nodes.length; i++) {
                if (tree.nodes[i][0].equals(data.node)) {
                    state.selectedNode = i;
                    state.hoveredNode = -1;
                    state.selectedBonusNode = -1;
                    state.hoveredBonusNode = -1;
                    ProfessionSkillTreeLogic.enterEditMode(playerRef, state);
                    ProfessionSkillTreeLogic.tryPendingAdd(playerRef, state, i, tree);
                    break;
                }
            }
            return UiEventResult.REBUILD;
        }
        if ("skillRight".equals(data.action) && data.node != null) {
            ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
            for (int i = 0; i < tree.nodes.length; i++) {
                if (tree.nodes[i][0].equals(data.node)) {
                    state.selectedNode = i;
                    if (!state.isEditMode) {
                        PlayerAccount acc = ProfessionAccounts.get(playerRef);
                        if (acc != null && acc.availableTalentPoints(ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state)) > 0) {
                            ProfessionSkillTreeLogic.enterEditMode(playerRef, state);
                        }
                    } else {
                        ProfessionSkillTreeLogic.tryPendingRemove(state, i);
                    }
                    break;
                }
            }
            return UiEventResult.REBUILD;
        }
        if ("skillHover".equals(data.action) && data.node != null) {
            ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
            for (int i = 0; i < tree.nodes.length; i++) {
                if (tree.nodes[i][0].equals(data.node)) {
                    state.hoveredNode = i;
                    state.hoveredBonusNode = -1;
                    break;
                }
            }
            return UiEventResult.HOVER_UPDATE;
        }
        if ("skillUnhover".equals(data.action)) {
            state.hoveredNode = -1;
            return UiEventResult.HOVER_UPDATE;
        }

        if ("bonusClick".equals(data.action) && data.index != null) {
            int idx;
            try { idx = Integer.parseInt(data.index); } catch (NumberFormatException e) { return UiEventResult.NONE; }
            if (idx < 0 || idx >= ProfessionBonusData.NODE_IDS.length) return UiEventResult.NONE;
            state.selectedBonusNode = idx;
            state.hoveredBonusNode = -1;
            state.selectedNode = -1;
            state.hoveredNode = -1;
            PlayerAccount acc = ProfessionAccounts.get(playerRef);
            if (acc == null) return UiEventResult.REBUILD;
            Profession prof = ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state);
            ProfessionSkillTreeLogic.enterEditMode(playerRef, state);
            if (state.pendingBonusRanks == null) return UiEventResult.REBUILD;
            if (idx > 0 && state.pendingBonusRanks[idx - 1] == 0) return UiEventResult.REBUILD;
            if (state.pendingBonusRanks[idx] >= 1) return UiEventResult.REBUILD;
            int pendingSpent = 0;
            if (state.pendingRanks != null) {
                for (int i = 0; i < state.pendingRanks.length; i++) {
                    pendingSpent += Math.max(0, state.pendingRanks[i] - state.skillRanks[i]);
                }
            }
            for (int i = 0; i < ProfessionBonusData.NODE_IDS.length; i++) {
                int real = acc.getTalentRank(prof, ProfessionBonusData.NODE_IDS[i]);
                pendingSpent += Math.max(0, state.pendingBonusRanks[i] - real);
            }
            if (acc.availableTalentPoints(prof) - pendingSpent <= 0) return UiEventResult.REBUILD;
            state.pendingBonusRanks[idx] = 1;
            return UiEventResult.REBUILD;
        }
        if ("bonusHover".equals(data.action) && data.index != null) {
            try { state.hoveredBonusNode = Integer.parseInt(data.index); } catch (NumberFormatException e) { return UiEventResult.NONE; }
            return UiEventResult.HOVER_UPDATE;
        }

        if ("toggleTalentSound".equals(data.action)) {
            Profession profession = ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state);
            ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
            int panelNode = state.hoveredNode >= 0 && state.hoveredNode < tree.nodes.length ? state.hoveredNode : state.selectedNode;
            String nodeId = tree.nodes[panelNode][0];
            if (TalentSoundNodes.hasSoundToggle(profession, nodeId)) {
                ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
                if (mgr != null) {
                    mgr.toggleTalentSound(playerRef.getUuid(), profession, nodeId);
                }
            }
            return UiEventResult.HOVER_UPDATE;
        }

        if ("saveSkills".equals(data.action)) {
            if (state.isEditMode) {
                ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
                if (mgr != null) {
                    ProfessionSkillTreeDef tree = ProfessionSkillTreeLogic.currentSkillTree(playerRef, state);
                    Profession prof = ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state);
                    if (state.pendingRanks != null) {
                        for (int i = 0; i < tree.nodes.length; i++) {
                            int delta = state.pendingRanks[i] - state.skillRanks[i];
                            if (delta > 0) {
                                String nodeId = tree.nodes[i][0];
                                for (int d = 0; d < delta; d++) {
                                    mgr.allocateTalent(playerRef.getUuid(), prof, nodeId, tree.maxRanks[i]);
                                }
                            }
                        }
                    }
                    if (state.pendingBonusRanks != null) {
                        PlayerAccount acc = ProfessionAccounts.get(playerRef);
                        for (int i = 0; i < ProfessionBonusData.NODE_IDS.length; i++) {
                            int real = acc != null ? acc.getTalentRank(prof, ProfessionBonusData.NODE_IDS[i]) : 0;
                            if (state.pendingBonusRanks[i] > real) {
                                mgr.allocateTalent(playerRef.getUuid(), prof, ProfessionBonusData.NODE_IDS[i], 1);
                            }
                        }
                    }
                }
            }
            ProfessionSkillTreeLogic.exitEditMode(state);
            return UiEventResult.REBUILD;
        }
        if ("resetSkills".equals(data.action)) {
            ProfessionSkillTreeLogic.exitEditMode(state);
            ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
            if (mgr != null) {
                mgr.resetTalents(playerRef.getUuid(), ProfessionSkillTreeLogic.currentTalentProfession(playerRef, state));
            }
            Arrays.fill(state.skillRanks, 0);
            return UiEventResult.REBUILD;
        }

        if ("professionReconvert".equals(data.action) && data.professionId != null) {
            state.reconvertSourceId = data.professionId.equals(state.reconvertSourceId)
                ? null : data.professionId;
            return UiEventResult.REBUILD;
        }
        if ("professionReconvertSelect".equals(data.action) && data.professionId != null
            && state.reconvertSourceId != null) {
            ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
            if (mgr != null) {
                Profession source = Profession.fromId(state.reconvertSourceId);
                Profession target = Profession.fromId(data.professionId);
                LOG.info("[RPG-Reconvert] select source=" + source + " target=" + target);
                if (source != null && target != null) {
                    PlayerAccount acc = mgr.getAccount(playerRef.getUuid());
                    if (acc != null) {
                        int slot = source == acc.getActiveSlot1() ? 1 : 0;
                        ProfessionManager.ReconvertResult result =
                            mgr.setActiveSlot(playerRef.getUuid(), slot, target);
                        LOG.info("[RPG-Reconvert] setActiveSlot slot=" + slot + " result=" + result
                            + " lastReconvertAt=" + acc.getLastReconvertAt());
                    }
                }
            }
            state.reconvertSourceId = null;
            return UiEventResult.REBUILD;
        }

        return UiEventResult.NONE;
    }
}
