package fr.varyon.vrpg.ui.profession;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public final class ProfessionSkillTreeLogic {

    private ProfessionSkillTreeLogic() {}

    public static void syncTalentTreeSlotIndex(@Nonnull RpgProfessionUiState state, @Nullable PlayerAccount acc) {
        if (acc == null) {
            state.talentTreeSlotIndex = 0;
            return;
        }
        Profession p0 = acc.getActiveSlot0();
        Profession p1 = acc.getActiveSlot1();
        if (state.talentTreeSlotIndex != 0 && state.talentTreeSlotIndex != 1) {
            state.talentTreeSlotIndex = 0;
        }
        if (state.talentTreeSlotIndex == 0 && p0 == null && p1 != null) {
            state.talentTreeSlotIndex = 1;
        } else if (state.talentTreeSlotIndex == 1 && p1 == null && p0 != null) {
            state.talentTreeSlotIndex = 0;
        }
    }

    @Nonnull
    public static Profession currentTalentProfession(@Nonnull PlayerRef playerRef,
                                                     @Nonnull RpgProfessionUiState state) {
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        syncTalentTreeSlotIndex(state, acc);
        if (acc == null) return Profession.MINEUR;
        Profession p = state.talentTreeSlotIndex == 0 ? acc.getActiveSlot0() : acc.getActiveSlot1();
        if (p != null) return p;
        Profession fallback = acc.getActiveSlot0() != null ? acc.getActiveSlot0() : acc.getActiveSlot1();
        return fallback != null ? fallback : Profession.MINEUR;
    }

    @Nonnull
    public static ProfessionSkillTreeDef currentSkillTree(@Nonnull PlayerRef playerRef,
                                                          @Nonnull RpgProfessionUiState state) {
        return ProfessionSkillTrees.forProfession(currentTalentProfession(playerRef, state));
    }

    public static void loadSkillRanksFromAccount(@Nonnull PlayerRef playerRef,
                                                 @Nonnull RpgProfessionUiState state) {
        Arrays.fill(state.skillRanks, 0);
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        if (acc == null) return;
        Profession prof = currentTalentProfession(playerRef, state);
        ProfessionSkillTreeDef tree = currentSkillTree(playerRef, state);
        for (int i = 0; i < tree.nodes.length; i++) {
            state.skillRanks[i] = acc.getTalentRank(prof, tree.nodes[i][0]);
        }
    }

    @Nonnull
    public static int[] currentRanks(@Nonnull RpgProfessionUiState state) {
        return (state.isEditMode && state.pendingRanks != null) ? state.pendingRanks : state.skillRanks;
    }

    public static int pendingRemainingPoints(@Nonnull PlayerRef playerRef,
                                             @Nonnull RpgProfessionUiState state,
                                             @Nullable PlayerAccount acc,
                                             @Nonnull Profession prof) {
        if (acc == null) return 0;
        int real = acc.availableTalentPoints(prof);
        if (!state.isEditMode || state.pendingRanks == null) return real;
        int spent = 0;
        for (int i = 0; i < state.pendingRanks.length; i++) {
            spent += Math.max(0, state.pendingRanks[i] - state.skillRanks[i]);
        }
        return real - spent;
    }

    public static void enterEditMode(@Nonnull PlayerRef playerRef, @Nonnull RpgProfessionUiState state) {
        if (state.isEditMode) return;
        state.isEditMode = true;
        state.pendingRanks = state.skillRanks.clone();
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        Profession prof = currentTalentProfession(playerRef, state);
        if (acc != null) {
            state.pendingBonusRanks = new int[ProfessionBonusData.NODE_IDS.length];
            for (int i = 0; i < ProfessionBonusData.NODE_IDS.length; i++) {
                state.pendingBonusRanks[i] = acc.getTalentRank(prof, ProfessionBonusData.NODE_IDS[i]);
            }
        }
    }

    public static void exitEditMode(@Nonnull RpgProfessionUiState state) {
        state.isEditMode = false;
        state.pendingRanks = null;
        state.pendingBonusRanks = null;
    }

    public static boolean tryPendingAdd(@Nonnull PlayerRef playerRef,
                                        @Nonnull RpgProfessionUiState state,
                                        int nodeIdx,
                                        @Nonnull ProfessionSkillTreeDef tree) {
        if (state.pendingRanks == null) return false;
        if (state.pendingRanks[nodeIdx] >= tree.maxRanks[nodeIdx]) return false;
        if (!parentsAllow(state.pendingRanks, tree, nodeIdx)) return false;
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        if (acc == null) return false;
        int pending = 0;
        for (int i = 0; i < state.pendingRanks.length; i++) {
            pending += Math.max(0, state.pendingRanks[i] - state.skillRanks[i]);
        }
        if (acc.availableTalentPoints(currentTalentProfession(playerRef, state)) - pending <= 0) return false;
        state.pendingRanks[nodeIdx]++;
        return true;
    }

    public static boolean tryPendingRemove(@Nonnull RpgProfessionUiState state, int nodeIdx) {
        if (state.pendingRanks == null) return false;
        if (state.pendingRanks[nodeIdx] <= state.skillRanks[nodeIdx]) return false;
        state.pendingRanks[nodeIdx]--;
        return true;
    }

    private static boolean parentsAllow(@Nonnull int[] ranks,
                                        @Nonnull ProfessionSkillTreeDef tree,
                                        int nodeIdx) {
        int[] parents = tree.parentGroups[nodeIdx];
        if (parents.length == 0) return true;
        for (int p : parents) {
            if (ranks[p] >= 1) return true;
        }
        return false;
    }
}
