package fr.varyon.vrpg.ui.classes.layout;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout;

import javax.annotation.Nonnull;

import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.bot;
import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.cx;
import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.top;

public final class Standard12NodeLayout implements TalentTreeLayout {

    private static final int[][] SLOT_LT = {
        {118,  32},
        {298,  32},
        {208, 125},
        {58,  218},
        {208, 218},
        {358, 218},
        {58,  309},
        {208, 309},
        {358, 309},
        {208, 402},
        {118, 495},
        {298, 495},
    };

    public static final Standard12NodeLayout INSTANCE = new Standard12NodeLayout();

    private Standard12NodeLayout() {}

    @Override
    public int[][] slotPositions() {
        return SLOT_LT;
    }

    @Override
    public int buildEdges(@Nonnull UICommandBuilder ui, int seg, @Nonnull int[][] lt, @Nonnull String ep) {
        seg = TalentTreeEdgeLayout.layoutMergeTwoToOne(ui, ep, seg,
            cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]), cx(lt[2]), top(lt[2]));
        seg = TalentTreeEdgeLayout.layoutSplitOneToThree(ui, ep, seg,
            cx(lt[2]), bot(lt[2]), cx(lt[3]), cx(lt[4]), cx(lt[5]), top(lt[3]));
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[3]), bot(lt[3]), top(lt[6]));
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[4]), bot(lt[4]), top(lt[7]));
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[5]), bot(lt[5]), top(lt[8]));
        seg = TalentTreeEdgeLayout.layoutMergeThreeToOne(ui, ep, seg,
            cx(lt[6]), bot(lt[6]), cx(lt[7]), bot(lt[7]), cx(lt[8]), bot(lt[8]), cx(lt[9]), top(lt[9]));
        return TalentTreeEdgeLayout.layoutSplitOneToTwo(ui, ep, seg,
            cx(lt[9]), bot(lt[9]), cx(lt[10]), cx(lt[11]), top(lt[10]));
    }
}
