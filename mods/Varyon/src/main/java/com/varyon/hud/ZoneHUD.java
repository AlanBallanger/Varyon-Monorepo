package com.varyon.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.MessagesConfig;
import com.varyon.essence.EssenceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class ZoneHUD extends CustomUIHud {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PAGE_COUNT = 2;
    private static final int PAGE_ZONE = 0;
    private static final int PAGE_PVP = 1;
    private static final int ESSENCE_FILL_TRACK_HALF = 199;
    private static final int ESSENCE_BAR_TRACK_LEFT = 36;
    private static final int ESSENCE_BAR_CENTER_X = ESSENCE_BAR_TRACK_LEFT + ESSENCE_FILL_TRACK_HALF;
    private static final int ESSENCE_BAR_LABEL_AREA_WIDTH = 470;
    private static final String LOOT_CHEST_ITEM_ID = "Furniture_Dungeon_Chest_Epic";
    private static final String LOOT_KEY_ITEM_ID = "Key_Fragment1";

    @Nonnull
    private final MessagesConfig messagesConfig;

    @Nullable
    private DifficultyZone currentZone;
    private double distanceFromSpawn;
    private int globalBalance;
    private double playerEssence;
    private boolean built;
    private long builtAt = 0;
    private static final long BUILD_GRACE_MS = 2000;
    private int currentPage = PAGE_ZONE;
    private boolean inSafeZone;
    private String safeQuadrantName = "";
    private long safeTimeRemaining;
    private boolean lootSpecialActive = false;
    private int maxEssenceCap = 1000;

    public ZoneHUD(@Nonnull PlayerRef playerRef, @Nonnull MessagesConfig messagesConfig) {
        super(playerRef);
        this.messagesConfig = messagesConfig;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("HUD/ZoneHUD.ui");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to build zone HUD: " + e.getMessage());
            return;
        }
        built = true;
        builtAt = System.currentTimeMillis();
    }

    public void nextPage() {
        currentPage = (currentPage + 1) % PAGE_COUNT;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void updateZoneInfo(@Nullable DifficultyZone zone, double distance, boolean inSafe, @Nonnull String quadrantName, long timeRemaining, boolean forceUpdate, boolean lootSpecialActive, int maxEssenceCap) {
        if (this.maxEssenceCap != maxEssenceCap) {
            this.maxEssenceCap = maxEssenceCap;
            forceUpdate = true;
        }
        if (this.lootSpecialActive != lootSpecialActive) {
            this.lootSpecialActive = lootSpecialActive;
            forceUpdate = true;
        }
        if (!built || System.currentTimeMillis() - builtAt < BUILD_GRACE_MS) {
            return;
        }

        boolean changed = false;

        if (this.currentZone != zone) {
            this.currentZone = zone;
            changed = true;
        }
        if (Math.abs(this.distanceFromSpawn - distance) > 1.0) {
            this.distanceFromSpawn = distance;
            changed = true;
        }
        if (this.inSafeZone != inSafe) {
            this.inSafeZone = inSafe;
            changed = true;
        }
        if (!this.safeQuadrantName.equals(quadrantName)) {
            this.safeQuadrantName = quadrantName;
            changed = true;
        }
        long timeDiff = Math.abs(this.safeTimeRemaining - timeRemaining);
        if (timeDiff > 1000) {
            this.safeTimeRemaining = timeRemaining;
            changed = true;
        }

        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            double currentPlayerEssence = essenceManager.getEssence(getPlayerRef().getUuid());
            int currentGlobalBalance = essenceManager.getGlobalBalance();
            if (Math.abs(this.playerEssence - currentPlayerEssence) > 0.01 || this.globalBalance != currentGlobalBalance) {
                this.playerEssence = currentPlayerEssence;
                this.globalBalance = currentGlobalBalance;
                changed = true;
            }
        }

        if (changed || forceUpdate) {
            sendPageUpdate();
        }
    }

    private void sendPageUpdate() {
        UICommandBuilder builder = new UICommandBuilder();
        if (currentPage == PAGE_ZONE) {
            applyZonePage(builder);
        } else {
            applyPvpPage(builder);
        }
        update(false, builder);
    }

    private void applyZonePage(@Nonnull UICommandBuilder builder) {
        String zoneName;
        if (currentZone != null) {
            String name = currentZone.getName();
            zoneName = (name.toLowerCase().startsWith("zone") ? name : "Zone " + name) + " [" + currentZone.getZoneId() + "]";
        } else {
            zoneName = "Spawn";
        }
        int dist = (int) Math.round(distanceFromSpawn);

        builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
        builder.set("#ZoneName.Style.TextColor", "#FFFFFF");

        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            playerEssence = essenceManager.getEssence(getPlayerRef().getUuid());
            globalBalance = essenceManager.getGlobalBalance();
        }
        
        int currentEssence = (int) Math.floor(playerEssence);
        int displayMax = Math.max(maxEssenceCap, currentEssence);
        builder.set("#Essence.Text", "Points : " + currentEssence + "/" + displayMax);
        builder.set("#Essence.Style.TextColor", "#FFFF55");

        builder.set("#HPIconPng.Visible", true);
        builder.set("#HPIconPngPvp.Visible", false);
        builder.set("#HPIconItem.Visible", false);
        builder.set("#DMGIconPng.Visible", true);
        builder.set("#DMGIconItem.Visible", false);
        if (currentZone != null) {
            builder.set("#HPMult.Text", "x" + String.format("%.1f", currentZone.getHealthMultiplier()));
            builder.set("#DMGMult.Text", "x" + String.format("%.1f", currentZone.getDamageMultiplier()));
            builder.set("#LootMult.Text", "x" + String.format("%.1f", currentZone.getLootMultiplier()));
        } else {
            builder.set("#HPMult.Text", "");
            builder.set("#DMGMult.Text", "");
            builder.set("#LootMult.Text", "");
        }

        builder.set("#HPMult.Style.TextColor", "#FFFFFF");
        builder.set("#DMGMult.Style.TextColor", "#FFAA55");
        builder.set("#LootMult.Style.TextColor", "#55FF55");

        builder.set("#Separator4.Text", "|");
        builder.set("#Separator4.Visible", true);
        builder.set("#LootColumn.Visible", true);
        builder.set("#LootCellContent.Visible", true);
        builder.set("#LootCellSpacer.Visible", false);
        builder.set("#LootIconPng.Visible", false);
        builder.set("#LootItemIcon.Visible", true);
        builder.set("#LootItemIcon.ItemId", LOOT_CHEST_ITEM_ID);

        updateEssenceBar(builder);
    }

    private void applyPvpPage(@Nonnull UICommandBuilder builder) {
        String zoneName;
        if (currentZone != null) {
            String name = currentZone.getName();
            zoneName = (name.toLowerCase().startsWith("zone") ? name : "Zone " + name) + " [" + currentZone.getZoneId() + "]";
        } else {
            zoneName = "Spawn";
        }
        int dist = (int) Math.round(distanceFromSpawn);
        builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
        builder.set("#ZoneName.Style.TextColor", "#FFFFFF");

        int currentEssence = (int) Math.floor(playerEssence);
        int displayMax = Math.max(maxEssenceCap, currentEssence);
        builder.set("#Essence.Text", "Points : " + currentEssence + "/" + displayMax);
        builder.set("#Essence.Style.TextColor", "#FFFF55");

        builder.set("#HPIconPng.Visible", false);
        builder.set("#HPIconPngPvp.Visible", true);
        builder.set("#HPIconItem.Visible", false);
        if (inSafeZone) {
            builder.set("#HPMult.Text", "OFF");
            builder.set("#HPMult.Style.TextColor", "#55FF55");
        } else {
            builder.set("#HPMult.Text", "ON");
            builder.set("#HPMult.Style.TextColor", "#FF5555");
        }

        builder.set("#DMGIconPng.Visible", false);
        builder.set("#DMGIconItem.Visible", true);
        builder.set("#DMGIconItem.ItemId", LOOT_KEY_ITEM_ID);

        if (lootSpecialActive) {
            builder.set("#DMGMult.Text", "Actif");
            builder.set("#DMGMult.Style.TextColor", "#55FF55");
        } else {
            builder.set("#DMGMult.Text", "Inactif");
            builder.set("#DMGMult.Style.TextColor", "#FF5555");
        }

        builder.set("#Separator4.Text", "|");
        builder.set("#Separator4.Visible", true);
        builder.set("#LootColumn.Visible", true);
        builder.set("#LootCellContent.Visible", false);
        builder.set("#LootCellSpacer.Visible", true);
        builder.set("#LootMult.Text", "");

        updateEssenceBar(builder);
    }

    @Nullable
    public DifficultyZone getCurrentZone() {
        return currentZone;
    }

    private void updateEssenceBar(@Nonnull UICommandBuilder builder) {
        int halfFill = ESSENCE_FILL_TRACK_HALF;
        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        int absMax = essenceManager != null ? Math.max(1, essenceManager.getGuildGaugeAbsMax()) : 10000;
        int clamped = Math.max(-absMax, Math.min(absMax, globalBalance));
        
        if (clamped >= 0) {
            int width = (int) Math.round(clamped / (double) absMax * halfFill);
            
            Anchor fractureAnchor = new Anchor();
            fractureAnchor.setLeft(Value.of(halfFill));
            fractureAnchor.setWidth(Value.of(width));
            fractureAnchor.setHeight(Value.of(11));
            builder.setObject("#EssenceBarFracture.Anchor", fractureAnchor);
            
            Anchor noyauAnchor = new Anchor();
            noyauAnchor.setLeft(Value.of(halfFill));
            noyauAnchor.setWidth(Value.of(0));
            noyauAnchor.setHeight(Value.of(11));
            builder.setObject("#EssenceBarNoyau.Anchor", noyauAnchor);
        } else {
            int width = (int) Math.round(Math.abs(clamped) / (double) absMax * halfFill);
            int left = halfFill - width;
            
            Anchor noyauAnchor = new Anchor();
            noyauAnchor.setLeft(Value.of(left));
            noyauAnchor.setWidth(Value.of(width));
            noyauAnchor.setHeight(Value.of(11));
            builder.setObject("#EssenceBarNoyau.Anchor", noyauAnchor);
            
            Anchor fractureAnchor = new Anchor();
            fractureAnchor.setLeft(Value.of(halfFill));
            fractureAnchor.setWidth(Value.of(0));
            fractureAnchor.setHeight(Value.of(11));
            builder.setObject("#EssenceBarFracture.Anchor", fractureAnchor);
        }

        int labelWidth = 56;
        int labelLeft;
        
        if (clamped >= 0) {
            int width = (int) Math.round(clamped / (double) absMax * halfFill);
            int barEnd = ESSENCE_BAR_CENTER_X + width;
            labelLeft = barEnd - (labelWidth / 2);
        } else {
            labelLeft = ESSENCE_BAR_CENTER_X - (labelWidth / 2);
        }
        
        if (labelLeft < 0) {
            labelLeft = 0;
        } else if (labelLeft > ESSENCE_BAR_LABEL_AREA_WIDTH - labelWidth) {
            labelLeft = ESSENCE_BAR_LABEL_AREA_WIDTH - labelWidth;
        }

        Anchor labelAnchor = new Anchor();
        labelAnchor.setLeft(Value.of(labelLeft));
        labelAnchor.setWidth(Value.of(labelWidth));
        labelAnchor.setHeight(Value.of(12));
        builder.setObject("#EssenceValue.Anchor", labelAnchor);
        builder.set("#EssenceValue.Text", String.valueOf(Math.abs(clamped)));
    }

    public void updateGlobalBalance() {
        if (!built) {
            return;
        }

        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            this.globalBalance = essenceManager.getGlobalBalance();

            UICommandBuilder builder = new UICommandBuilder();
            updateEssenceBar(builder);
            update(false, builder);
        }
    }
}
