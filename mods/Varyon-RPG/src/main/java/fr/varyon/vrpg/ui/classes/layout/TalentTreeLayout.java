package fr.varyon.vrpg.ui.classes.layout;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;

public interface TalentTreeLayout {

    int[][] slotPositions();

    default boolean skipCentering() {
        return false;
    }

    default int canvasWidth() {
        return 872;
    }

    int buildEdges(@Nonnull UICommandBuilder ui, int seg, @Nonnull int[][] shifted, @Nonnull String edgePrefix);
}
