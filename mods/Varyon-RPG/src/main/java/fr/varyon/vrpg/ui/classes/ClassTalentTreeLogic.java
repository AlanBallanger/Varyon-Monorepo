package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.PlayerClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassTalentTreeLogic {

    private ClassTalentTreeLogic() {}

    public static void enterEditMode(@Nonnull RpgClassUiState state,
                                     @Nullable ClassAccount acc,
                                     @Nullable PlayerClass activeClass,
                                     @Nonnull ClassTalentTree.Node[] nodes) {
        if (state.classEditMode) return;
        state.classEditMode = true;
        state.savedClassRanks = new int[nodes.length];
        state.pendingClassRanks = new int[nodes.length];
        if (acc != null && activeClass != null) {
            for (int i = 0; i < nodes.length; i++) {
                int rank = acc.getTalentRank(activeClass, String.valueOf(i));
                state.savedClassRanks[i] = rank;
                state.pendingClassRanks[i] = rank;
            }
        }
    }

    public static void exitEditMode(@Nonnull RpgClassUiState state) {
        state.classEditMode = false;
        state.pendingClassRanks = null;
        state.savedClassRanks = null;
    }

    public static boolean tryPendingAdd(@Nonnull RpgClassUiState state,
                                        @Nullable ClassAccount acc,
                                        @Nullable PlayerClass activeClass,
                                        int nodeIdx,
                                        @Nonnull ClassTalentTree.Node[] nodes) {
        if (acc == null || activeClass == null) return false;
        if (!state.classEditMode || state.pendingClassRanks == null) return false;
        if (nodeIdx < 0 || nodeIdx >= nodes.length) return false;
        if (state.pendingClassRanks[nodeIdx] >= nodes[nodeIdx].maxRank()) return false;
        if (pendingRemainingPoints(state, acc, activeClass) <= 0) return false;
        state.pendingClassRanks[nodeIdx]++;
        return true;
    }

    public static boolean tryPendingRemove(@Nonnull RpgClassUiState state, int nodeIdx) {
        if (!state.classEditMode || state.pendingClassRanks == null || state.savedClassRanks == null) return false;
        if (nodeIdx < 0 || nodeIdx >= state.pendingClassRanks.length) return false;
        if (state.pendingClassRanks[nodeIdx] <= state.savedClassRanks[nodeIdx]) return false;
        state.pendingClassRanks[nodeIdx]--;
        return true;
    }

    public static int pendingRemainingPoints(@Nonnull RpgClassUiState state,
                                             @Nonnull ClassAccount acc,
                                             @Nonnull PlayerClass activeClass) {
        int available = acc.availableTalentPoints(activeClass);
        if (!state.classEditMode || state.pendingClassRanks == null || state.savedClassRanks == null) {
            return available;
        }
        int spent = 0;
        for (int i = 0; i < state.pendingClassRanks.length; i++) {
            spent += Math.max(0, state.pendingClassRanks[i] - state.savedClassRanks[i]);
        }
        return available - spent;
    }

    public static void syncDisplayRanks(@Nonnull RpgClassUiState state,
                                        @Nullable ClassAccount acc,
                                        @Nullable PlayerClass activeClass,
                                        int nodeCount) {
        if (state.classEditMode && state.pendingClassRanks != null) return;
        if (state.pendingClassRanks == null || state.pendingClassRanks.length != nodeCount) {
            state.pendingClassRanks = new int[nodeCount];
            if (acc != null && activeClass != null) {
                for (int i = 0; i < nodeCount; i++) {
                    state.pendingClassRanks[i] = acc.getTalentRank(activeClass, String.valueOf(i));
                }
            }
        }
    }
}
