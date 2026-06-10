package fr.varyon.vrpg.ui.classes.layout;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout;
import fr.varyon.vrpg.ui.tree.TalentTreeTheme;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class ClassTalentTreeLayouts {

    private static final TalentTreeLayout STANDARD = Standard12NodeLayout.INSTANCE;
    private static final Map<PlayerSpecialization, TalentTreeLayout> BY_SPEC =
        new EnumMap<>(PlayerSpecialization.class);

    static {
        BY_SPEC.put(PlayerSpecialization.DUELLISTE, DuellisteLayout.INSTANCE);
        BY_SPEC.put(PlayerSpecialization.OMBRE,     OmbreLayout.INSTANCE);
    }

    private ClassTalentTreeLayouts() {}

    @Nonnull
    public static TalentTreeLayout forAccount(@Nullable ClassAccount acc) {
        if (acc != null) {
            PlayerClass activeClass = acc.getActiveClass();
            if (activeClass != null) {
                PlayerSpecialization spec = acc.getActiveSpec(activeClass);
                if (spec != null) {
                    TalentTreeLayout layout = BY_SPEC.get(spec);
                    if (layout != null) return layout;
                }
            }
        }
        return STANDARD;
    }

    @Nonnull
    public static int[][] positionsForAccount(@Nullable ClassAccount acc) {
        TalentTreeLayout layout = forAccount(acc);
        int[][] lt = layout.slotPositions();
        return layout.skipCentering() ? lt : TalentTreeEdgeLayout.centerHorizontally(lt, layout.canvasWidth());
    }

    @Nonnull
    public static ClassTalentTree.Node[] talentNodes(@Nullable ClassAccount acc) {
        if (acc == null) return ClassTalentTree.getTree(PlayerClass.GUERRIER);
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return ClassTalentTree.getTree(PlayerClass.GUERRIER);
        PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (spec != null) {
            ClassTalentTree.Node[] specTree = ClassTalentTree.getSpecTree(spec);
            if (specTree != null) return specTree;
        }
        return ClassTalentTree.getTree(activeClass);
    }

    public static int buildEdges(@Nullable ClassAccount acc,
                                 @Nonnull com.hypixel.hytale.server.core.ui.builder.UICommandBuilder ui,
                                 int seg) {
        TalentTreeLayout layout = forAccount(acc);
        int[][] shifted = positionsForAccount(acc);
        TalentTreeEdgeLayout.hideEdgeSegmentRange(ui, TalentTreeTheme.CLASS_EDGE_PREFIX, 0, TalentTreeTheme.CLASS_TREE_EDGE_SEGMENTS);
        seg = layout.buildEdges(ui, seg, shifted, TalentTreeTheme.CLASS_EDGE_PREFIX);
        TalentTreeEdgeLayout.hideEdgeSegmentRange(ui, TalentTreeTheme.CLASS_EDGE_PREFIX, seg, TalentTreeTheme.CLASS_TREE_EDGE_SEGMENTS);
        return seg;
    }
}
