package fr.varyon.vrpg.ui.tree;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;

import static fr.varyon.vrpg.ui.tree.TalentTreeTheme.*;

public final class TalentTreeEdgeLayout {

    private TalentTreeEdgeLayout() {}

    public static int cx(int[] lt) {
        return lt[0] + SLOT / 2;
    }

    public static int top(int[] lt) {
        return lt[1];
    }

    public static int bot(int[] lt) {
        return lt[1] + SLOT;
    }

    public static int vx(int cxCenter) {
        return cxCenter - RAIL / 2;
    }

    public static void setAnchor(@Nonnull UICommandBuilder ui,
                                 @Nonnull String elementId,
                                 int left,
                                 int top,
                                 int width,
                                 int height) {
        Anchor a = new Anchor();
        a.setLeft(Value.of(left));
        a.setTop(Value.of(top));
        a.setWidth(Value.of(width));
        a.setHeight(Value.of(height));
        ui.setObject(elementId + ".Anchor", a);
    }

    public static void hideEdgeSegmentRange(@Nonnull UICommandBuilder ui,
                                            int fromInclusive,
                                            int toExclusive) {
        hideEdgeSegmentRange(ui, SKILL_EDGE_PREFIX, fromInclusive, toExclusive);
    }

    public static void hideEdgeSegmentRange(@Nonnull UICommandBuilder ui,
                                            @Nonnull String edgePrefix,
                                            int fromInclusive,
                                            int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            ui.set(edgePrefix + i + ".Visible", false);
        }
    }

    public static void positionSkillSlot(@Nonnull UICommandBuilder ui,
                                         @Nonnull String id,
                                         int slotLeft,
                                         int slotTop) {
        setAnchor(ui, "#SkillTreeNode" + id + "Slot", slotLeft, slotTop, SLOT, SLOT);
        setAnchor(ui, "#SkillTreeNode" + id + "Unlocked",
            slotLeft + FILL_INSET, slotTop + FILL_INSET, FILL_SIZE, FILL_SIZE);
        setAnchor(ui, "#SkillTreeNode" + id + "Icon",
            slotLeft + 8, slotTop + 8, SLOT - 16, SLOT - 16);
        setAnchor(ui, "#SkillTreeNode" + id + "Veil",
            slotLeft + FILL_INSET, slotTop + FILL_INSET, FILL_SIZE, FILL_SIZE);
        setAnchor(ui, "#SkillTreeNode" + id, slotLeft, slotTop, SLOT, SLOT);
    }

    public static void positionSkillRank(@Nonnull UICommandBuilder ui,
                                         @Nonnull String id,
                                         int slotLeft,
                                         int slotTop) {
        setAnchor(ui, "#SkillTreeNode" + id + "RankText",
            slotLeft + RANK_LABEL_SHIFT_RIGHT,
            slotTop + SLOT + RANK_LABEL_GAP_TOP,
            RANK_LABEL_W, RANK_LABEL_H);
    }

    public static void positionClassSlot(@Nonnull UICommandBuilder ui,
                                         @Nonnull String id,
                                         int slotLeft,
                                         int slotTop) {
        setAnchor(ui, "#ClassTreeNode" + id + "Slot", slotLeft, slotTop, SLOT, SLOT);
        setAnchor(ui, "#ClassTreeNode" + id + "Unlocked",
            slotLeft + FILL_INSET, slotTop + FILL_INSET, FILL_SIZE, FILL_SIZE);
        setAnchor(ui, "#ClassTreeNode" + id + "Icon",
            slotLeft + 8, slotTop + 8, SLOT - 16, SLOT - 16);
        setAnchor(ui, "#ClassTreeNode" + id + "Veil",
            slotLeft + FILL_INSET, slotTop + FILL_INSET, FILL_SIZE, FILL_SIZE);
        setAnchor(ui, "#ClassTreeNode" + id, slotLeft, slotTop, SLOT, SLOT);
    }

    public static void positionClassRank(@Nonnull UICommandBuilder ui,
                                         @Nonnull String id,
                                         int slotLeft,
                                         int slotTop) {
        setAnchor(ui, "#ClassTreeNode" + id + "RankText",
            slotLeft + RANK_LABEL_SHIFT_RIGHT,
            slotTop + SLOT + RANK_LABEL_GAP_TOP,
            RANK_LABEL_W, RANK_LABEL_H);
    }

    public static int layoutMergeTwoToOne(@Nonnull UICommandBuilder ui,
                                          int seg,
                                          int cxA, int yBotA,
                                          int cxB, int yBotB,
                                          int cxMid, int yTopMid) {
        return layoutMergeTwoToOne(ui, SKILL_EDGE_PREFIX, seg, cxA, yBotA, cxB, yBotB, cxMid, yTopMid);
    }

    public static int layoutSplitOneToThree(@Nonnull UICommandBuilder ui,
                                            int seg,
                                            int cxP, int yBotP,
                                            int cxL, int cxM, int cxR,
                                            int yTopChildRow) {
        return layoutSplitOneToThree(ui, SKILL_EDGE_PREFIX, seg, cxP, yBotP, cxL, cxM, cxR, yTopChildRow);
    }

    public static int layoutVerticalConnector(@Nonnull UICommandBuilder ui,
                                              int seg,
                                              int cx, int yFrom, int yTo) {
        return layoutVerticalConnector(ui, SKILL_EDGE_PREFIX, seg, cx, yFrom, yTo);
    }

    public static int layoutMergeThreeToOne(@Nonnull UICommandBuilder ui,
                                            int seg,
                                            int cxA, int yBotA,
                                            int cxB, int yBotB,
                                            int cxC, int yBotC,
                                            int cxMid, int yTopMid) {
        return layoutMergeThreeToOne(ui, SKILL_EDGE_PREFIX, seg, cxA, yBotA, cxB, yBotB, cxC, yBotC, cxMid, yTopMid);
    }

    public static int layoutSplitOneToTwo(@Nonnull UICommandBuilder ui,
                                          int seg,
                                          int cxP, int yBotP,
                                          int cxL, int cxR,
                                          int yTopChildRow) {
        return layoutSplitOneToTwo(ui, SKILL_EDGE_PREFIX, seg, cxP, yBotP, cxL, cxR, yTopChildRow);
    }

    public static int layoutFanTwoToThree(@Nonnull UICommandBuilder ui, int seg,
                                          int cxA, int yBotA, int cxB, int yBotB,
                                          int cxL, int cxM, int cxR, int yTopChildRow) {
        return layoutFanTwoToThree(ui, SKILL_EDGE_PREFIX, seg, cxA, yBotA, cxB, yBotB, cxL, cxM, cxR, yTopChildRow);
    }

    public static int layoutFanThreeToTwo(@Nonnull UICommandBuilder ui, int seg,
                                          int cxA, int yBotA, int cxB, int yBotB, int cxC, int yBotC,
                                          int cxL, int cxR, int yTopChildRow) {
        int barTop = Math.min(yBotA, Math.min(yBotB, yBotC)) + STEM_DOWN_FROM_PARENT;
        int barBot = barTop + RAIL;
        showEdge(ui, seg++, vx(cxA), yBotA, RAIL, STEM_DOWN_FROM_PARENT);
        showEdge(ui, seg++, vx(cxB), yBotB, RAIL, STEM_DOWN_FROM_PARENT);
        showEdge(ui, seg++, vx(cxC), yBotC, RAIL, STEM_DOWN_FROM_PARENT);
        int barL = vx(cxA);
        showEdge(ui, seg++, barL, barTop, vx(cxC) - barL + RAIL, RAIL);
        int stemH = yTopChildRow - barBot;
        if (stemH > 0) {
            showEdge(ui, seg++, vx(cxL), barBot, RAIL, stemH);
            showEdge(ui, seg++, vx(cxR), barBot, RAIL, stemH);
        }
        return seg;
    }

    public static int layoutHorizontalSiblings(@Nonnull UICommandBuilder ui,
                                               int seg,
                                               int cxCenter, int cyMid,
                                               int cxLeft, int cxRight) {
        int slotHalfL = SLOT / 2;
        int slotHalfR = SLOT - slotHalfL;
        int barY = cyMid - RAIL / 2;
        int leftBarX = cxLeft + slotHalfR;
        int leftBarW = (cxCenter - slotHalfL) - leftBarX;
        int rightBarX = cxCenter + slotHalfR;
        int rightBarW = (cxRight - slotHalfL) - rightBarX;
        showEdge(ui, seg++, leftBarX, barY, leftBarW, RAIL);
        showEdge(ui, seg++, rightBarX, barY, rightBarW, RAIL);
        return seg;
    }

    public static int layoutMergeTwoToOne(@Nonnull UICommandBuilder ui, @Nonnull String ep,
                                          int seg, int cxA, int yBotA, int cxB, int yBotB,
                                          int cxMid, int yTopMid) {
        int yBotMin = Math.min(yBotA, yBotB);
        int barTop = yBotMin + STEM_DOWN_FROM_PARENT;
        int barBot = barTop + RAIL;
        showEdge(ui, ep, seg++, vx(cxA), yBotA, RAIL, STEM_DOWN_FROM_PARENT);
        showEdge(ui, ep, seg++, vx(cxB), yBotB, RAIL, STEM_DOWN_FROM_PARENT);
        int barL = vx(Math.min(cxA, cxB));
        showEdge(ui, ep, seg++, barL, barTop, vx(Math.max(cxA, cxB)) - barL + RAIL, RAIL);
        int stemToChild = yTopMid - barBot;
        if (stemToChild > 0) showEdge(ui, ep, seg++, vx(cxMid), barBot, RAIL, stemToChild);
        return seg;
    }

    public static int layoutSplitOneToThree(@Nonnull UICommandBuilder ui, @Nonnull String ep,
                                            int seg, int cxP, int yBotP,
                                            int cxL, int cxM, int cxR, int yTopChildRow) {
        int barTop = yBotP + STEM_DOWN_FROM_PARENT;
        int barBot = barTop + RAIL;
        showEdge(ui, ep, seg++, vx(cxP), yBotP, RAIL, STEM_DOWN_FROM_PARENT);
        int barL = vx(Math.min(cxL, Math.min(cxM, cxR)));
        showEdge(ui, ep, seg++, barL, barTop, vx(Math.max(cxL, Math.max(cxM, cxR))) - barL + RAIL, RAIL);
        int stemToChild = yTopChildRow - barBot;
        if (stemToChild > 0) {
            showEdge(ui, ep, seg++, vx(cxL), barBot, RAIL, stemToChild);
            showEdge(ui, ep, seg++, vx(cxM), barBot, RAIL, stemToChild);
            showEdge(ui, ep, seg++, vx(cxR), barBot, RAIL, stemToChild);
        }
        return seg;
    }

    public static int layoutVerticalConnector(@Nonnull UICommandBuilder ui, @Nonnull String ep,
                                              int seg, int cx, int yFrom, int yTo) {
        int h = yTo - yFrom;
        if (h > 0) showEdge(ui, ep, seg++, vx(cx), yFrom, RAIL, h);
        return seg;
    }

    public static int layoutMergeThreeToOne(@Nonnull UICommandBuilder ui, @Nonnull String ep,
                                            int seg, int cxA, int yBotA, int cxB, int yBotB,
                                            int cxC, int yBotC, int cxMid, int yTopMid) {
        int yBotMin = Math.min(yBotA, Math.min(yBotB, yBotC));
        int barTop = yBotMin + STEM_DOWN_FROM_PARENT;
        int barBot = barTop + RAIL;
        showEdge(ui, ep, seg++, vx(cxA), yBotA, RAIL, STEM_DOWN_FROM_PARENT);
        showEdge(ui, ep, seg++, vx(cxB), yBotB, RAIL, STEM_DOWN_FROM_PARENT);
        showEdge(ui, ep, seg++, vx(cxC), yBotC, RAIL, STEM_DOWN_FROM_PARENT);
        int barL = vx(Math.min(cxA, Math.min(cxB, cxC)));
        showEdge(ui, ep, seg++, barL, barTop, vx(Math.max(cxA, Math.max(cxB, cxC))) - barL + RAIL, RAIL);
        int stemToChild = yTopMid - barBot;
        if (stemToChild > 0) showEdge(ui, ep, seg++, vx(cxMid), barBot, RAIL, stemToChild);
        return seg;
    }

    public static int layoutSplitOneToTwo(@Nonnull UICommandBuilder ui, @Nonnull String ep,
                                          int seg, int cxP, int yBotP,
                                          int cxL, int cxR, int yTopChildRow) {
        int barTop = yBotP + STEM_DOWN_FROM_PARENT;
        int barBot = barTop + RAIL;
        showEdge(ui, ep, seg++, vx(cxP), yBotP, RAIL, STEM_DOWN_FROM_PARENT);
        int barL = vx(Math.min(cxL, cxR));
        showEdge(ui, ep, seg++, barL, barTop, vx(Math.max(cxL, cxR)) - barL + RAIL, RAIL);
        int stemToChild = yTopChildRow - barBot;
        if (stemToChild > 0) {
            showEdge(ui, ep, seg++, vx(cxL), barBot, RAIL, stemToChild);
            showEdge(ui, ep, seg++, vx(cxR), barBot, RAIL, stemToChild);
        }
        return seg;
    }

    public static int layoutFanTwoToThree(@Nonnull UICommandBuilder ui, @Nonnull String ep, int seg,
                                          int cxA, int yBotA, int cxB, int yBotB,
                                          int cxL, int cxM, int cxR, int yTopChildRow) {
        int barTop = Math.min(yBotA, yBotB) + STEM_DOWN_FROM_PARENT;
        int barBot = barTop + RAIL;
        showEdge(ui, ep, seg++, vx(cxA), yBotA, RAIL, STEM_DOWN_FROM_PARENT);
        showEdge(ui, ep, seg++, vx(cxB), yBotB, RAIL, STEM_DOWN_FROM_PARENT);
        int barL = vx(cxL);
        showEdge(ui, ep, seg++, barL, barTop, vx(cxR) - barL + RAIL, RAIL);
        int stemH = yTopChildRow - barBot;
        if (stemH > 0) {
            showEdge(ui, ep, seg++, vx(cxL), barBot, RAIL, stemH);
            showEdge(ui, ep, seg++, vx(cxM), barBot, RAIL, stemH);
            showEdge(ui, ep, seg++, vx(cxR), barBot, RAIL, stemH);
        }
        return seg;
    }

    public static void showEdge(@Nonnull UICommandBuilder ui,
                                int index,
                                int left, int top, int width, int height) {
        showEdge(ui, SKILL_EDGE_PREFIX, index, left, top, width, height);
    }

    public static void showEdge(@Nonnull UICommandBuilder ui,
                                @Nonnull String edgePrefix,
                                int index,
                                int left, int top, int width, int height) {
        if (width <= 0 || height <= 0) return;
        setAnchor(ui, edgePrefix + index, left, top, width, height);
        ui.set(edgePrefix + index + ".Visible", true);
    }

    public static int[][] centerHorizontally(int[][] slotLt, int canvasWidth) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int[] row : slotLt) {
            if (row[0] < minX) minX = row[0];
            if (row[0] > maxX) maxX = row[0];
        }
        int treeWidth = maxX - minX + SLOT;
        int xOffset = (canvasWidth - treeWidth) / 2 - minX;
        int[][] shifted = new int[slotLt.length][2];
        for (int i = 0; i < slotLt.length; i++) {
            shifted[i][0] = slotLt[i][0] + xOffset;
            shifted[i][1] = slotLt[i][1];
        }
        return shifted;
    }
}
