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

    private static final int ANCHOR_RIGHT = 392;
    private static final int ANCHOR_TOP = 907;
    private static final int ANCHOR_WIDTH = 128;

    private static final int DIVIDER_W = 4;
    private static final int DIVIDER_H = 72;
    private static final int DIVIDER_RIGHT = ANCHOR_RIGHT + ANCHOR_WIDTH - 20;
    private static final int DIVIDER_TOP = 969;

private static final int IMG_W = 118;
    private static final int IMG_H = (int) Math.round((double) IMG_W * (double) PNG_H / (double) PNG_W);

    private static final int KEY_H = 32;
    private static final int KEY_SRC_X = 109;
    private static final int KEY_SRC_Y = 224;
    private static final int KEY_LABEL_W_O = 32;
    private static final int KEY_LABEL_W_ALT = 38;

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
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("HUD/VaryonMenuHud.ui");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to build Varyon menu HUD", e);
            return;
        }

        Anchor root = new Anchor();
        root.setRight(Value.of(ANCHOR_RIGHT));
        root.setTop(Value.of(ANCHOR_TOP));
        root.setWidth(Value.of(ANCHOR_WIDTH));
        root.setHeight(Value.of(IMG_H));
        builder.setObject("#VaryonMenuHud.Anchor", root);

        Anchor divider = new Anchor();
        divider.setRight(Value.of(DIVIDER_RIGHT));
        divider.setTop(Value.of(DIVIDER_TOP));
        divider.setWidth(Value.of(DIVIDER_W));
        divider.setHeight(Value.of(DIVIDER_H));
        builder.setObject("#VaryonDividerGroup.Anchor", divider);

        Anchor dividerImg = new Anchor();
        dividerImg.setLeft(Value.of(0));
        dividerImg.setTop(Value.of(0));
        dividerImg.setWidth(Value.of(DIVIDER_W));
        dividerImg.setHeight(Value.of(DIVIDER_H));
        builder.setObject("#VaryonHotbarDivider.Anchor", dividerImg);

        int stackLeft = (ANCHOR_WIDTH - IMG_W) / 2;

        Anchor stack = new Anchor();
        stack.setLeft(Value.of(stackLeft));
        stack.setTop(Value.of(0));
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

        UUID uuid = hudPlayer.getUuid();
        AccueilShortcutConfig.Mode mode = AccueilShortcutConfig.getInstance().getMode(uuid);
        String keyText;
        int keyLabelW;
        if (mode == AccueilShortcutConfig.Mode.ALT) {
            keyText = "Alt";
            keyLabelW = KEY_LABEL_W_ALT;
        } else {
            keyText = "O";
            keyLabelW = KEY_LABEL_W_O;
        }
        builder.set("#VaryonMenuKey.Text", keyText);

        int keyCx = (int) Math.round((double) KEY_SRC_X * (double) IMG_W / (double) PNG_W);
        int keyCy = (int) Math.round((double) KEY_SRC_Y * (double) IMG_H / (double) PNG_H);
        int keyLeft = Math.max(0, Math.min(keyCx - keyLabelW / 2, IMG_W - keyLabelW));
        int keyTop = Math.max(0, Math.min(keyCy - KEY_H / 2, IMG_H - KEY_H));

        Anchor key = new Anchor();
        key.setLeft(Value.of(keyLeft));
        key.setTop(Value.of(keyTop));
        key.setWidth(Value.of(keyLabelW));
        key.setHeight(Value.of(KEY_H));
        builder.setObject("#VaryonMenuKey.Anchor", key);
    }
}
