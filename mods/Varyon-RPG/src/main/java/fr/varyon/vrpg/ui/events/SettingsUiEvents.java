package fr.varyon.vrpg.ui.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.ui.RpgMainUI;
import fr.varyon.vrpg.ui.prefs.HudCorner;
import fr.varyon.vrpg.ui.prefs.HudLayoutHelper;
import fr.varyon.vrpg.ui.prefs.PlayerUiPreferences;
import fr.varyon.vrpg.ui.prefs.PlayerUiPreferencesManager;
import fr.varyon.vrpg.ui.tabs.SettingsTab;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class SettingsUiEvents {

    private SettingsUiEvents() {}

    public static UiEventResult handle(@Nonnull PlayerRef playerRef, @Nonnull RpgMainUI.Data data) {
        if (data.action == null) return UiEventResult.NONE;
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null) return UiEventResult.NONE;
        PlayerUiPreferencesManager manager = plugin.getUiPreferencesManager();
        if (manager == null) return UiEventResult.NONE;

        UUID uuid = playerRef.getUuid();
        PlayerUiPreferences prefs = manager.get(uuid).copy();
        boolean offsetChange = false;

        switch (data.action) {
            case "settingOn" -> {
                if (data.setting == null) return UiEventResult.NONE;
                applySwitch(prefs, data.setting, true);
            }
            case "settingOff" -> {
                if (data.setting == null) return UiEventResult.NONE;
                applySwitch(prefs, data.setting, false);
            }
            case "settingCorner" -> {
                if (data.setting == null || data.corner == null) return UiEventResult.NONE;
                HudCorner corner = HudCorner.fromId(data.corner);
                switch (data.setting) {
                    case "classHud" -> {
                        prefs.classHudCorner = corner;
                        prefs.classHudOffsetX = HudLayoutHelper.clampClassOffsetX(prefs.classHudOffsetX, corner);
                        prefs.classHudOffsetY = HudLayoutHelper.clampClassOffsetY(prefs.classHudOffsetY, corner);
                        offsetChange = true;
                    }
                    case "profHud" -> {
                        prefs.professionHudCorner = corner;
                        prefs.professionHudOffsetX = HudLayoutHelper.clampProfessionOffsetX(prefs.professionHudOffsetX, corner);
                        prefs.professionHudOffsetY = HudLayoutHelper.clampProfessionOffsetY(prefs.professionHudOffsetY, corner);
                        offsetChange = true;
                    }
                    default -> { return UiEventResult.NONE; }
                }
            }
            case "classHudOffsetX" -> {
                if (data.sliderValue == null) return UiEventResult.NONE;
                prefs.classHudOffsetX = HudLayoutHelper.clampClassOffsetX(
                    SettingsTab.sliderToOffsetX(data.sliderValue), prefs.classHudCorner);
                offsetChange = true;
            }
            case "classHudOffsetY" -> {
                if (data.sliderValue == null) return UiEventResult.NONE;
                prefs.classHudOffsetY = HudLayoutHelper.clampClassOffsetY(
                    SettingsTab.sliderToOffsetY(data.sliderValue), prefs.classHudCorner);
                offsetChange = true;
            }
            case "profHudOffsetX" -> {
                if (data.sliderValue == null) return UiEventResult.NONE;
                prefs.professionHudOffsetX = HudLayoutHelper.clampProfessionOffsetX(
                    SettingsTab.sliderToOffsetX(data.sliderValue), prefs.professionHudCorner);
                offsetChange = true;
            }
            case "profHudOffsetY" -> {
                if (data.sliderValue == null) return UiEventResult.NONE;
                prefs.professionHudOffsetY = HudLayoutHelper.clampProfessionOffsetY(
                    SettingsTab.sliderToOffsetY(data.sliderValue), prefs.professionHudCorner);
                offsetChange = true;
            }
            case "classHudOffsetReset" -> {
                prefs.classHudOffsetX = 0;
                prefs.classHudOffsetY = 0;
                offsetChange = true;
            }
            case "profHudOffsetReset" -> {
                prefs.professionHudOffsetX = 0;
                prefs.professionHudOffsetY = 0;
                offsetChange = true;
            }
            default -> { return UiEventResult.NONE; }
        }

        manager.update(uuid, prefs);
        manager.applyHudLayout(uuid);
        return offsetChange ? UiEventResult.SETTINGS_UPDATE : UiEventResult.REBUILD;
    }

    private static void applySwitch(@Nonnull PlayerUiPreferences prefs, @Nonnull String setting, boolean enabled) {
        switch (setting) {
            case "classHud" -> prefs.classHudVisible = enabled;
            case "profHud" -> prefs.professionHudVisible = enabled;
            case "xpNotif" -> prefs.xpNotificationsEnabled = enabled;
            case "skillNotif" -> prefs.skillNotificationsEnabled = enabled;
            case "profSounds" -> prefs.professionSoundsEnabled = enabled;
            default -> {}
        }
    }
}
