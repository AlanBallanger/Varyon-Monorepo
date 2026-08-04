package com.varyon.easyhunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.components.HungerComponent;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.config.HudPosition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.WeakHashMap;


public class EasyHungerHud extends CustomUIHud {
    static private final WeakHashMap<PlayerRef, EasyHungerHud> hudMap = new WeakHashMap<>();
    static public final String hudIdentifier = "com.varyon.easyhunger.hud.hunger";
    private GameMode gameMode;
    private float hungerLevel;
    private float previewHungerRestoration = 0.0f;
    private boolean visible = true;

    public EasyHungerHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float hungerLevel) {
        super(playerRef, hudIdentifier);
        this.gameMode = gameMode;
        this.hungerLevel = hungerLevel;
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
        uiCommandBuilder.append("HUD/Hunger/Hunger.ui");
        updateHudPosition(uiCommandBuilder, hudPosition);
        updateGameMode(uiCommandBuilder, this.gameMode);
        updateHungerLevel(uiCommandBuilder, this.hungerLevel);
        updateVisibility(uiCommandBuilder, this.visible);
    }

    protected void updateVisibility(UICommandBuilder uiCommandBuilder, boolean visible) {
        this.visible = visible;
        uiCommandBuilder.set("#EasyHungerContainer.Visible", this.visible);
    }

    protected void updateHudPosition(UICommandBuilder uiCommandBuilder, HudPosition hudPosition) {
        // Calculation constants from old mod .ui file
        int DefaultItemSlotSize = 74;
        int DefaultItemSlotsPerRow = 9;
        int DefaultItemGridPadding = 2;
        int HotbarSlotSpacingHud = 4;
        
        int HotbarHeight = DefaultItemSlotSize + (2 * DefaultItemGridPadding);
        // Correct width calculation: 9 slots have 8 spaces between them
        int HotbarWidthHud = (DefaultItemSlotSize * DefaultItemSlotsPerRow) + (HotbarSlotSpacingHud * DefaultItemSlotsPerRow);

        int BottomMargin = 30;
        int ContainerMargin = 6;
        int InventoryClosedContainerMargin = BottomMargin + ContainerMargin;
        
        int calculatedBottomOffset = InventoryClosedContainerMargin + HotbarHeight + 6;

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
        
        // Also update preview if active
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

    protected void updateGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        this.gameMode = gameMode;
        String iconBackground = gameMode == GameMode.Adventure
            ? "HUD/Hunger/HungerIcon.png"
            : "HUD/Hunger/CreativeHungerIcon.png";
        uiCommandBuilder.set("#EasyHungerIcon.Background", iconBackground);
        uiCommandBuilder.set("#EasyHungerHungerBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#EasyHungerCreativeHungerBar.Visible", gameMode == GameMode.Creative);
    }

    static public void updatePlayerHungerLevel(@NonNullDecl PlayerRef playerRef, float hungerLevel) {
        EasyHungerHud hud = hudMap.get(playerRef);
        if (hud == null) {
            EasyCombinedHud.updatePlayerHungerLevel(playerRef, hungerLevel);
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerLevel(uiCommandBuilder, hungerLevel);
        hud.update(false, uiCommandBuilder);
    }
    
    static public void updatePlayerHungerPreview(@NonNullDecl PlayerRef playerRef, float hungerRestoration) {
        EasyHungerHud hud = hudMap.get(playerRef);
        if (hud == null) {
            EasyCombinedHud.updatePlayerHungerPreview(playerRef, hungerRestoration);
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerPreview(uiCommandBuilder, hungerRestoration);
        hud.update(false, uiCommandBuilder);
    }
    
    static public void updatePlayerGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        EasyHungerHud hud = hudMap.get(playerRef);
        if (hud == null) {
            EasyCombinedHud.updatePlayerGameMode(playerRef, gameMode);
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateGameMode(uiCommandBuilder, gameMode);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudVisibility(@NonNullDecl PlayerRef playerRef, boolean visible) {
        EasyHungerHud hud = hudMap.get(playerRef);
        if (hud == null) {
            EasyCombinedHud.updatePlayerHudVisibility(playerRef, visible);
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateVisibility(uiCommandBuilder, visible);
        hud.update(false, uiCommandBuilder);
    }
}
