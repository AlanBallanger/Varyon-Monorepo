package fr.varyon.vrpg.ui.tree;

import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;

public final class TalentTreeTheme {

    public static final String NODE_FILL = "#1A1F29FF";
    public static final String NODE_BORDER = "#4E576DFF";
    public static final String NODE_BORDER_SELECTION = "#B0B8C8FF";
    public static final String NODE_BORDER_ALLOCATED = "#31C677FF";
    public static final String NODE_VEIL = "#14182166";

    public static final PatchStyle NODE_FILL_STYLE =
        new PatchStyle().setColor(Value.of(NODE_FILL));
    public static final PatchStyle NODE_VEIL_STYLE =
        new PatchStyle().setColor(Value.of(NODE_VEIL));

    public static final int RAIL = 4;
    public static final int STEM_DOWN_FROM_PARENT = 12;

    public static final int SKILL_TREE_EDGE_SEGMENTS = 48;
    public static final int CLASS_TREE_EDGE_SEGMENTS = 256;

    public static final int ICON_SIZE = 40;
    public static final int SLOT = Math.round(76 * 0.8f);
    public static final int ICON_INSET = (SLOT - ICON_SIZE) / 2;
    public static final int FILL_INSET = 3;
    public static final int FILL_SIZE = SLOT - 2 * FILL_INSET;

    public static final int RANK_LABEL_W = 44;
    public static final int RANK_LABEL_H = 14;
    public static final int RANK_LABEL_GAP_TOP = -1;
    public static final int RANK_LABEL_SHIFT_RIGHT = (48 * SLOT + 38) / 76;

    public static final String CLASS_EDGE_PREFIX = "#ClassTreeEdgeSeg";
    public static final String SKILL_EDGE_PREFIX = "#SkillTreeEdgeSeg";

    private TalentTreeTheme() {}
}
