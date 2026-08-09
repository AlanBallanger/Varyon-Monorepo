package com.varyon.easyhunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.components.ThirstComponent;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.config.HudPosition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EasyWaterHud extends CustomUIHud {
    static private final ConcurrentHashMap<UUID, EasyWaterHud> hudMap = new ConcurrentHashMap<>();
    static public final String hudIdentifier = "com.varyon.easyhunger.hud.water";
    private GameMode gameMode;
    private float thirstLevel;
    private float previewThirstRestoration = 0.0f;
    private boolean visible = true;

    public EasyWaterHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float thirstLevel) {
        super(playerRef, hudIdentifier);
        this.gameMode = gameMode;
        this.thirstLevel = thirstLevel;
        hudMap.put(playerRef.getUuid(), this);
    }

    @Override
    protected void onRemove() {
        hudMap.remove(getPlayerRef().getUuid());
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        EasyHungerConfig config = EasyHunger.get().getConfig();
        HudPosition hudPosition = config.getHudPosition();
        uiCommandBuilder.append("HUD/Hunger/Water.ui");
        updateHudPosition(uiCommandBuilder, hudPosition);
        updateGameMode(uiCommandBuilder, this.gameMode);
        updateThirstLevel(uiCommandBuilder, this.thirstLevel);
        updateVisibility(uiCommandBuilder, this.visible);
    }

    protected void updateVisibility(UICommandBuilder uiCommandBuilder, boolean visible) {
        this.visible = visible;
        uiCommandBuilder.set("#EasyWaterContainer.Visible", this.visible);
    }

    protected void updateHudPosition(UICommandBuilder uiCommandBuilder, HudPosition hudPosition) {
        int DefaultItemSlotSize = 74;
        int DefaultItemSlotsPerRow = 9;
        int DefaultItemGridPadding = 2;
        int HotbarSlotSpacingHud = 4;
        
        int HotbarHeight = DefaultItemSlotSize + (2 * DefaultItemGridPadding);
        int HotbarWidthHud = (DefaultItemSlotSize * DefaultItemSlotsPerRow) + (HotbarSlotSpacingHud * DefaultItemSlotsPerRow);

        int BottomMargin = 30;
        int ContainerMargin = 6;
        int InventoryClosedContainerMargin = BottomMargin + ContainerMargin;
        
        int calculatedBottomOffset = InventoryClosedContainerMargin + HotbarHeight + 6;

        // Offset for Water Bar to be ABOVE Hunger Bar
        // Assuming Hunger Bar height is 12 + some padding.
        int STACK_OFFSET = 14; 

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
        
        // Also update preview if active
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

    protected void updateGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        this.gameMode = gameMode;
        String iconBackground = gameMode == GameMode.Adventure
            ? "HUD/Hunger/WaterIcon.png"
            : "HUD/Hunger/CreativeWaterIcon.png";
        uiCommandBuilder.set("#EasyWaterIcon.Background", iconBackground);
        uiCommandBuilder.set("#EasyWaterThirstBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#EasyWaterCreativeThirstBar.Visible", gameMode == GameMode.Creative);
    }

    static public void updatePlayerThirstLevel(@NonNullDecl PlayerRef playerRef, float thirstLevel) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) {
            EasyCombinedHud.updatePlayerThirstLevel(playerRef, thirstLevel);
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstLevel(uiCommandBuilder, thirstLevel);
        hud.update(false, uiCommandBuilder);
    }
    
    static public void updatePlayerThirstPreview(@NonNullDecl PlayerRef playerRef, float thirstRestoration) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) {
            EasyCombinedHud.updatePlayerThirstPreview(playerRef, thirstRestoration);
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstPreview(uiCommandBuilder, thirstRestoration);
        hud.update(false, uiCommandBuilder);
    }
    
    static public void updatePlayerGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) {
            // Combined HUD handles both game modes
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateGameMode(uiCommandBuilder, gameMode);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudVisibility(@NonNullDecl PlayerRef playerRef, boolean visible) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) {
            // Combined HUD handles visibility for both
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateVisibility(uiCommandBuilder, visible);
        hud.update(false, uiCommandBuilder);
    }
}
