package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.ui.classes.layout.ClassTalentTreeLayouts;

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
                String key = nodeKey(acc, activeClass, i);
                int rank = acc.getTalentRank(activeClass, key);
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
        if (!parentsAllow(state, acc, nodeIdx)) return false;
        if (pendingRemainingPoints(state, acc, activeClass) <= 0) return false;
        state.pendingClassRanks[nodeIdx]++;
        return true;
    }

    private static boolean parentsAllow(@Nonnull RpgClassUiState state,
                                        @Nonnull ClassAccount acc,
                                        int nodeIdx) {
        int[][] parentGroups = ClassTalentTreeLayouts.parentGroupsForAccount(acc);
        if (nodeIdx >= parentGroups.length) return true;
        int[] parents = parentGroups[nodeIdx];
        if (parents.length == 0) return true;
        for (int p : parents) {
            if (p < state.pendingClassRanks.length && state.pendingClassRanks[p] >= 1) return true;
        }
        return false;
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
        int available = acc.availableTalentPoints(activeClass, acc.getActiveSpec(activeClass));
        if (!state.classEditMode || state.pendingClassRanks == null || state.savedClassRanks == null) {
            return available;
        }
        int spent = 0;
        for (int i = 0; i < state.pendingClassRanks.length; i++) {
            spent += Math.max(0, state.pendingClassRanks[i] - state.savedClassRanks[i]);
        }
        return available - spent;
    }

    @Nonnull
    public static String nodeKey(@Nonnull ClassAccount acc, @Nonnull PlayerClass activeClass, int nodeIndex) {
        fr.varyon.vrpg.classes.PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (spec != null) {
            return spec.getId() + "_" + nodeIndex;
        }
        return String.valueOf(nodeIndex);
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
                    String key = nodeKey(acc, activeClass, i);
                    state.pendingClassRanks[i] = acc.getTalentRank(activeClass, key);
                }
            }
        }
    }
}
