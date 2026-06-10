package fr.varyon.vrpg.ui.classes.layout;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout;

import javax.annotation.Nonnull;

import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.bot;
import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.cx;
import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.top;

public final class OmbreLayout implements TalentTreeLayout {

    private static final int[][] SLOT_LT = {
        {240,  32},
        {430,  32},
        {170, 151},
        {330, 151},
        {490, 151},
        {170, 270},
        {330, 270},
        {490, 270},
        {240, 389},
        {430, 389},
        {240, 508},
        {430, 508},
    };

    public static final OmbreLayout INSTANCE = new OmbreLayout();

    private OmbreLayout() {}

    @Override
    public int[][] slotPositions() {
        return SLOT_LT;
    }

    @Override
    public boolean skipCentering() {
        return true;
    }

    @Override
    public int buildEdges(@Nonnull UICommandBuilder ui, int seg, @Nonnull int[][] lt, @Nonnull String ep) {
        seg = TalentTreeEdgeLayout.layoutFanTwoToThree(ui, ep, seg,
            cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]),
            cx(lt[2]), cx(lt[3]), cx(lt[4]), top(lt[2]));
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[2]), bot(lt[2]), top(lt[5]) + 4);
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[3]), bot(lt[3]), top(lt[6]));
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[4]), bot(lt[4]), top(lt[7]));
        seg = TalentTreeEdgeLayout.layoutMergeTwoToOne(ui, ep, seg,
            cx(lt[5]), bot(lt[5]), cx(lt[6]), bot(lt[6]), cx(lt[8]), top(lt[8]));
        seg = TalentTreeEdgeLayout.layoutMergeTwoToOne(ui, ep, seg,
            cx(lt[6]), bot(lt[6]), cx(lt[7]), bot(lt[7]), cx(lt[9]), top(lt[9]));
        seg = TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[8]), bot(lt[8]), top(lt[10]));
        return TalentTreeEdgeLayout.layoutVerticalConnector(ui, ep, seg, cx(lt[9]), bot(lt[9]), top(lt[11]));
    }
}
