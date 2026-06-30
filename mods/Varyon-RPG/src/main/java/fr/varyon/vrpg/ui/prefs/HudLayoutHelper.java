package fr.varyon.vrpg.ui.prefs;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;

public final class HudLayoutHelper {

    private static final int CLASS_WIDTH = 240;
    private static final int CLASS_HEIGHT = 68;
    private static final int PROFESSION_WIDTH = 240;
    private static final int PROFESSION_HEIGHT = 127;
    private static final int BASE_TOP_CLASS = 57;
    private static final int BASE_TOP_PROFESSION = 130;
    private static final int BASE_EDGE = 12;
    private static final int BASE_BOTTOM = 20;

    private HudLayoutHelper() {}

    public static void applyClassPanelAnchor(@Nonnull UICommandBuilder builder, @Nonnull PlayerUiPreferences prefs) {
        int offsetX = clampClassOffsetX(prefs.classHudOffsetX, prefs.classHudCorner);
        int offsetY = clampClassOffsetY(prefs.classHudOffsetY, prefs.classHudCorner);
        builder.setObject("#ClassXPPanel.Anchor", panelAnchor(
            prefs.classHudCorner, offsetX, offsetY, CLASS_WIDTH, CLASS_HEIGHT, BASE_TOP_CLASS));
    }

    public static void applyProfessionPanelAnchor(@Nonnull UICommandBuilder builder, @Nonnull PlayerUiPreferences prefs) {
        int offsetX = clampProfessionOffsetX(prefs.professionHudOffsetX, prefs.professionHudCorner);
        int offsetY = clampProfessionOffsetY(prefs.professionHudOffsetY, prefs.professionHudCorner);
        builder.setObject("#XPPanel.Anchor", panelAnchor(
            prefs.professionHudCorner, offsetX, offsetY, PROFESSION_WIDTH, PROFESSION_HEIGHT, BASE_TOP_PROFESSION));
    }

    public static int clampClassOffsetX(int offsetX, @Nonnull HudCorner corner) {
        offsetX = PlayerUiPreferencesManager.clampOffsetX(offsetX);
        return switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> Math.max(-BASE_EDGE, offsetX);
            default -> offsetX;
        };
    }

    public static int clampClassOffsetY(int offsetY, @Nonnull HudCorner corner) {
        offsetY = PlayerUiPreferencesManager.clampOffsetY(offsetY);
        return switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> Math.max(-BASE_TOP_CLASS, offsetY);
            case BOTTOM_LEFT, BOTTOM_RIGHT -> Math.min(BASE_BOTTOM, offsetY);
        };
    }

    public static int clampProfessionOffsetX(int offsetX, @Nonnull HudCorner corner) {
        offsetX = PlayerUiPreferencesManager.clampOffsetX(offsetX);
        return switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> Math.max(-BASE_EDGE, offsetX);
            default -> offsetX;
        };
    }

    public static int clampProfessionOffsetY(int offsetY, @Nonnull HudCorner corner) {
        offsetY = PlayerUiPreferencesManager.clampOffsetY(offsetY);
        return switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> Math.max(-BASE_TOP_PROFESSION, offsetY);
            case BOTTOM_LEFT, BOTTOM_RIGHT -> Math.min(BASE_BOTTOM, offsetY);
        };
    }

    @Nonnull
    private static Anchor panelAnchor(@Nonnull HudCorner corner, int offsetX, int offsetY,
                                      int width, int height, int baseTop) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        switch (corner) {
            case TOP_LEFT -> {
                anchor.setTop(Value.of(Math.max(0, baseTop + offsetY)));
                anchor.setLeft(Value.of(Math.max(0, BASE_EDGE + offsetX)));
            }
            case TOP_RIGHT -> {
                anchor.setTop(Value.of(Math.max(0, baseTop + offsetY)));
                anchor.setRight(Value.of(Math.max(0, BASE_EDGE - offsetX)));
            }
            case BOTTOM_LEFT -> {
                anchor.setBottom(Value.of(Math.max(0, BASE_BOTTOM - offsetY)));
                anchor.setLeft(Value.of(Math.max(0, BASE_EDGE + offsetX)));
            }
            case BOTTOM_RIGHT -> {
                anchor.setBottom(Value.of(Math.max(0, BASE_BOTTOM - offsetY)));
                anchor.setRight(Value.of(Math.max(0, BASE_EDGE - offsetX)));
            }
        }
        return anchor;
    }
}
