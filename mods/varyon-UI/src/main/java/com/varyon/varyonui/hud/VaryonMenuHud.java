package com.varyon.varyonui.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.varyonui.config.AccueilShortcutConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VaryonMenuHud extends CustomUIHud {

    public static final String HUD_KEY = "varyon_menu_hud";

    private static final Logger LOGGER = Logger.getLogger("VaryonMenuHud");

    private static final int PNG_W = 189;
    private static final int PNG_H = 322;
    private static final int ANCHOR_RIGHT = 12;
    private static final int ANCHOR_BOTTOM = 20;
    private static final int MENU_LIFT_PX = 70;
    private static final int TITLE_SHIFT_RIGHT_PX = 7;
    private static final int TITLE_NUDGE_DOWN_PX = 15;
    private static final int HUD_SHIFT_DOWN_PX = 50;
    private static final int ANCHOR_WIDTH = 128;

    private static final int IMG_W = 118;
    private static final int IMG_H = (int) Math.round((double) IMG_W * (double) PNG_H / (double) PNG_W);
    private static final int KEY_H = 32;
    private static final int KEY_SRC_X = 109;
    private static final int KEY_SRC_Y = 224;
    private static final int KEY_LABEL_W_O = 32;
    private static final int KEY_LABEL_W_ALT = 38;
    private static final int KEY_NUDGE_DOWN_PX = 1;

    private static final int MENU_UP_PX = 56;

    private static final int ANCHOR_HEIGHT = 24 + 28 + IMG_H + MENU_LIFT_PX;

    public static void attach(@Nullable Player player, @Nonnull PlayerRef playerRef) {
        if (player == null) {
            return;
        }
        HudManager hudManager = player.getHudManager();
        if (hudManager == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid != null
                && AccueilShortcutConfig.getInstance().getMode(uuid) == AccueilShortcutConfig.Mode.DISABLE) {
            hudManager.removeCustomHud(playerRef, HUD_KEY);
            return;
        }
        hudManager.addCustomHud(playerRef, new VaryonMenuHud(playerRef));
    }

    private final PlayerRef hudPlayer;

    public VaryonMenuHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
        this.hudPlayer = playerRef;
    }

    @Override
    public void onRemove() {}

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("HUD/VaryonMenuHud.ui");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to build Varyon menu HUD", e);
            return;
        }
        int menuOffsetY = -MENU_LIFT_PX + HUD_SHIFT_DOWN_PX;

        Anchor root = new Anchor();
        root.setRight(Value.of(ANCHOR_RIGHT));
        root.setBottom(Value.of(ANCHOR_BOTTOM));
        root.setWidth(Value.of(ANCHOR_WIDTH));
        root.setHeight(Value.of(ANCHOR_HEIGHT));
        builder.setObject("#VaryonMenuHud.Anchor", root);
        builder.set("#VaryonMenuTitle.Text", "Menu Varyon");
        UUID uuid = hudPlayer.getUuid();
        AccueilShortcutConfig.Mode mode = AccueilShortcutConfig.getInstance().getMode(uuid);
        String keyText = "O";
        int keyLabelW = KEY_LABEL_W_O;
        switch (mode) {
            case ALT -> {
                keyText = "Alt";
                keyLabelW = KEY_LABEL_W_ALT;
            }
            case O -> {
                keyText = "O";
                keyLabelW = KEY_LABEL_W_O;
            }
            case DISABLE -> {
                keyText = "O";
                keyLabelW = KEY_LABEL_W_O;
            }
        }
        builder.set("#VaryonMenuKey.Text", keyText);

        Anchor title = new Anchor();
        title.setLeft(Value.of(TITLE_SHIFT_RIGHT_PX));
        title.setTop(Value.of(menuOffsetY + TITLE_NUDGE_DOWN_PX));
        title.setWidth(Value.of(ANCHOR_WIDTH - TITLE_SHIFT_RIGHT_PX));
        title.setHeight(Value.of(24));
        builder.setObject("#VaryonMenuTitle.Anchor", title);

        int stackLeft = (ANCHOR_WIDTH - IMG_W) / 2;
        int stackTop = 28 + menuOffsetY - MENU_UP_PX;

        Anchor stack = new Anchor();
        stack.setLeft(Value.of(stackLeft));
        stack.setTop(Value.of(stackTop));
        stack.setWidth(Value.of(IMG_W));
        stack.setHeight(Value.of(IMG_H));
        builder.setObject("#VaryonMenuStack.Anchor", stack);

        Anchor base = new Anchor();
        base.setLeft(Value.of(0));
        base.setTop(Value.of(0));
        base.setWidth(Value.of(IMG_W));
        base.setHeight(Value.of(IMG_H));
        builder.setObject("#VaryonButtonBase.Anchor", base);

        Anchor book = new Anchor();
        book.setLeft(Value.of(0));
        book.setTop(Value.of(0));
        book.setWidth(Value.of(IMG_W));
        book.setHeight(Value.of(IMG_H));
        builder.setObject("#VaryonMenuBook.Anchor", book);

        int keyCx = (int) Math.round((double) KEY_SRC_X * (double) IMG_W / (double) PNG_W);
        int keyCy = (int) Math.round((double) KEY_SRC_Y * (double) IMG_H / (double) PNG_H) + KEY_NUDGE_DOWN_PX;
        int keyLeft = keyCx - keyLabelW / 2;
        int keyTop = keyCy - KEY_H / 2;
        if (keyLeft < 0) {
            keyLeft = 0;
        } else if (keyLeft > IMG_W - keyLabelW) {
            keyLeft = IMG_W - keyLabelW;
        }
        if (keyTop < 0) {
            keyTop = 0;
        } else if (keyTop > IMG_H - KEY_H) {
            keyTop = IMG_H - KEY_H;
        }
        Anchor key = new Anchor();
        key.setLeft(Value.of(keyLeft));
        key.setTop(Value.of(keyTop));
        key.setWidth(Value.of(keyLabelW));
        key.setHeight(Value.of(KEY_H));
        builder.setObject("#VaryonMenuKey.Anchor", key);
    }
}
