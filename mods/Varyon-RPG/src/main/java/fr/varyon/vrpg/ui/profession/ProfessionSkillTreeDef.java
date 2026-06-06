package fr.varyon.vrpg.ui.profession;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProfessionSkillTreeDef {

    @FunctionalInterface
    public interface EdgeBuilderFn {
        int build(@Nonnull UICommandBuilder ui, int seg, int[][] slotLt);
    }

    public final String[][] nodes;
    public final int[][] slotLt;
    public final int[][] parentGroups;
    public final int[] maxRanks;
    public final EdgeBuilderFn edgeBuilder;
    @Nullable public final String[][] nodeStatValues;

    public ProfessionSkillTreeDef(String[][] nodes, int[][] slotLt, int[][] parentGroups,
                                  int[] maxRanks, EdgeBuilderFn edgeBuilder,
                                  @Nullable String[][] nodeStatValues) {
        this.nodes = nodes;
        this.slotLt = slotLt;
        this.parentGroups = parentGroups;
        this.maxRanks = maxRanks;
        this.edgeBuilder = edgeBuilder;
        this.nodeStatValues = nodeStatValues;
    }
}
