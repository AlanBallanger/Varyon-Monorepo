package com.varyon.easyhunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.config.HudPosition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.WeakHashMap;

/**
 * Combined HUD for vanilla Hytale compatibility.
 * Appends both Hunger.ui and Water.ui in a single CustomUIHud,
 * since vanilla only allows one CustomUIHud per player.
 */
public class EasyCombinedHud extends CustomUIHud {
    static public final String hudIdentifier = "com.varyon.easyhunger.hud.combined";
    static private final WeakHashMap<PlayerRef, EasyCombinedHud> hudMap = new WeakHashMap<>();

    private GameMode gameMode;
    private float hungerLevel;
    private float thirstLevel;
    private float previewHungerRestoration = 0.0f;
    private float previewThirstRestoration = 0.0f;
    private boolean visible = true;
    private final boolean thirstEnabled;

    public EasyCombinedHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode,
                           float hungerLevel, float thirstLevel, boolean thirstEnabled) {
        super(playerRef, hudIdentifier);
        this.gameMode = gameMode;
        this.hungerLevel = hungerLevel;
        this.thirstLevel = thirstLevel;
        this.thirstEnabled = thirstEnabled;
        hudMap.put(playerRef, this);
    }

    @Override
    protected void onRemove() {
        hudMap.remove(getPlayerRef());
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        EasyHungerConfig config = EasyHunger.get().getConfig();
        HudPosition hudPosition = config.getHudPosition();

        // Append Hunger UI
        uiCommandBuilder.append("HUD/Hunger/Hunger.ui");
        updateHungerHudPosition(uiCommandBuilder, hudPosition);
        updateHungerGameMode(uiCommandBuilder, this.gameMode);
        updateHungerLevel(uiCommandBuilder, this.hungerLevel);

        // Append Water UI (only if thirst enabled)
        if (this.thirstEnabled) {
            uiCommandBuilder.append("HUD/Hunger/Water.ui");
            updateWaterHudPosition(uiCommandBuilder, hudPosition);
            updateWaterGameMode(uiCommandBuilder, this.gameMode);
            updateThirstLevel(uiCommandBuilder, this.thirstLevel);
        }

        updateVisibility(uiCommandBuilder, this.visible);
    }

    // === Visibility ===

    protected void updateVisibility(UICommandBuilder uiCommandBuilder, boolean visible) {
        this.visible = visible;
        uiCommandBuilder.set("#EasyHungerContainer.Visible", this.visible);
        if (this.thirstEnabled) {
            uiCommandBuilder.set("#EasyWaterContainer.Visible", this.visible);
        }
    }

    // === Hunger Methods ===

    protected void updateHungerHudPosition(UICommandBuilder uiCommandBuilder, HudPosition hudPosition) {
        int DefaultItemSlotSize = 74;
        int DefaultItemSlotsPerRow = 9;
        int DefaultItemGridPadding = 2;
        int HotbarSlotSpacingHud = 4;

        int HotbarHeight = DefaultItemSlotSize + (2 * DefaultItemGridPadding);
        int HotbarWidthHud = (DefaultItemSlotSize * DefaultItemSlotsPerRow) + (HotbarSlotSpacingHud * DefaultItemSlotsPerRow);

        int BottomMargin = 30;
        int ContainerMargin = 6;
        int InventoryClosedContainerMargin = BottomMargin + ContainerMargin;

        Anchor anchor = new Anchor();

        switch (hudPosition) {
            case TOP:
                 anchor.setWidth(Value.of(HotbarWidthHud));
                 anchor.setBottom(Value.of(InventoryClosedContainerMargin + HotbarHeight + 32));
                 break;
            case BOTTOM:
                anchor.setWidth(Value.of(HotbarWidthHud));
                anchor.setBottom(Value.of(4));
                break;
        }

        uiCommandBuilder.setObject("#EasyHungerContainer.Anchor", anchor);
    }

    protected void updateHungerLevel(UICommandBuilder uiCommandBuilder, float hungerLevel) {
        this.hungerLevel = hungerLevel;
        float max = EasyHunger.get().getConfig().getMaxHunger();
        float barValue = hungerLevel / max;
        uiCommandBuilder.set("#EasyHungerHungerBar.Value", barValue);
        uiCommandBuilder.set("#EasyHungerCreativeHungerBar.Value", barValue);
        uiCommandBuilder.set("#EasyHungerProgressBarEffect.Value", barValue);

        if (this.previewHungerRestoration != 0.0f) {
            updateHungerPreview(uiCommandBuilder, this.previewHungerRestoration);
        }
    }

    protected void updateHungerPreview(UICommandBuilder uiCommandBuilder, float hungerRestoration) {
        this.previewHungerRestoration = hungerRestoration;

        if (hungerRestoration == 0.0f) {
            uiCommandBuilder.set("#EasyHungerPreviewBar.Value", 0.0f);
            return;
        }

        float max = EasyHunger.get().getConfig().getMaxHunger();
        float expectedLevel = Math.min(this.hungerLevel + hungerRestoration, max);
        float previewBarValue = expectedLevel / max;

        uiCommandBuilder.set("#EasyHungerPreviewBar.Value", previewBarValue);
    }

    protected void updateHungerGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        this.gameMode = gameMode;
        String iconBackground = gameMode == GameMode.Adventure
            ? "HUD/Hunger/HungerIcon.png"
            : "HUD/Hunger/CreativeHungerIcon.png";
        uiCommandBuilder.set("#EasyHungerIcon.Background", iconBackground);
        uiCommandBuilder.set("#EasyHungerHungerBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#EasyHungerCreativeHungerBar.Visible", gameMode == GameMode.Creative);
    }

    // === Water/Thirst Methods ===

    protected void updateWaterHudPosition(UICommandBuilder uiCommandBuilder, HudPosition hudPosition) {
        int DefaultItemSlotSize = 74;
        int DefaultItemSlotsPerRow = 9;
        int DefaultItemGridPadding = 2;
        int HotbarSlotSpacingHud = 4;

        int HotbarHeight = DefaultItemSlotSize + (2 * DefaultItemGridPadding);
        int HotbarWidthHud = (DefaultItemSlotSize * DefaultItemSlotsPerRow) + (HotbarSlotSpacingHud * DefaultItemSlotsPerRow);

        int BottomMargin = 30;
        int ContainerMargin = 6;
        int InventoryClosedContainerMargin = BottomMargin + ContainerMargin;

        Anchor anchor = new Anchor();

        switch (hudPosition) {
            case TOP:
                 anchor.setWidth(Value.of(HotbarWidthHud));
                 anchor.setBottom(Value.of(InventoryClosedContainerMargin + HotbarHeight + 32));
                 break;
            case BOTTOM:
                anchor.setWidth(Value.of(HotbarWidthHud));
                anchor.setBottom(Value.of(4));
                break;
        }

        uiCommandBuilder.setObject("#EasyWaterContainer.Anchor", anchor);
    }

    protected void updateThirstLevel(UICommandBuilder uiCommandBuilder, float thirstLevel) {
        this.thirstLevel = thirstLevel;
        float max = EasyHunger.get().getConfig().getMaxThirst();
        float barValue = thirstLevel / max;
        uiCommandBuilder.set("#EasyWaterThirstBar.Value", barValue);
        uiCommandBuilder.set("#EasyWaterCreativeThirstBar.Value", barValue);
        uiCommandBuilder.set("#EasyWaterProgressBarEffect.Value", barValue);

        if (this.previewThirstRestoration != 0.0f) {
            updateThirstPreview(uiCommandBuilder, this.previewThirstRestoration);
        }
    }

    protected void updateThirstPreview(UICommandBuilder uiCommandBuilder, float thirstRestoration) {
        this.previewThirstRestoration = thirstRestoration;

        if (thirstRestoration == 0.0f) {
            uiCommandBuilder.set("#EasyWaterPreviewBar.Value", 0.0f);
            return;
        }

        float max = EasyHunger.get().getConfig().getMaxThirst();
        float expectedLevel = Math.min(this.thirstLevel + thirstRestoration, max);
        float previewBarValue = expectedLevel / max;

        uiCommandBuilder.set("#EasyWaterPreviewBar.Value", previewBarValue);
    }

    protected void updateWaterGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        String iconBackground = gameMode == GameMode.Adventure
            ? "HUD/Hunger/WaterIcon.png"
            : "HUD/Hunger/CreativeWaterIcon.png";
        uiCommandBuilder.set("#EasyWaterIcon.Background", iconBackground);
        uiCommandBuilder.set("#EasyWaterThirstBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#EasyWaterCreativeThirstBar.Visible", gameMode == GameMode.Creative);
    }

    // === Static Update Methods (called externally) ===

    static public void updatePlayerHungerLevel(@NonNullDecl PlayerRef playerRef, float hungerLevel) {
        EasyCombinedHud hud = hudMap.get(playerRef);
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerLevel(uiCommandBuilder, hungerLevel);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHungerPreview(@NonNullDecl PlayerRef playerRef, float hungerRestoration) {
        EasyCombinedHud hud = hudMap.get(playerRef);
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerPreview(uiCommandBuilder, hungerRestoration);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerThirstLevel(@NonNullDecl PlayerRef playerRef, float thirstLevel) {
        EasyCombinedHud hud = hudMap.get(playerRef);
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstLevel(uiCommandBuilder, thirstLevel);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerThirstPreview(@NonNullDecl PlayerRef playerRef, float thirstRestoration) {
        EasyCombinedHud hud = hudMap.get(playerRef);
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstPreview(uiCommandBuilder, thirstRestoration);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        EasyCombinedHud hud = hudMap.get(playerRef);
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerGameMode(uiCommandBuilder, gameMode);
        if (hud.thirstEnabled) {
            hud.updateWaterGameMode(uiCommandBuilder, gameMode);
        }
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudVisibility(@NonNullDecl PlayerRef playerRef, boolean visible) {
        EasyCombinedHud hud = hudMap.get(playerRef);
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateVisibility(uiCommandBuilder, visible);
        hud.update(false, uiCommandBuilder);
    }
}
