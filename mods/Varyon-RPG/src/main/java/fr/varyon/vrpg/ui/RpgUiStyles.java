package fr.varyon.vrpg.ui;

import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;

public final class RpgUiStyles {

    public static final PatchStyle CARD_BG_ACTIVE_STYLE =
        new PatchStyle(Value.of("Elements/HudPanelActive.png"), Value.of(10));
    public static final PatchStyle CARD_BG_INACTIVE_STYLE =
        new PatchStyle(Value.of("Elements/HudPanel.png"), Value.of(10));
    public static final PatchStyle HUD_XP_PANEL_STYLE =
        new PatchStyle()
            .setTexturePath(Value.of("Elements/HudPanel.png"))
            .setBorder(Value.of(10))
            .setColor(Value.of("#FFFFFFD1"));

    private RpgUiStyles() {}
}
