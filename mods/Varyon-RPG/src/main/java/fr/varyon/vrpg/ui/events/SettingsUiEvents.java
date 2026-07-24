package fr.varyon.vrpg.ui.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.ui.RpgMainUI;
import fr.varyon.vrpg.ui.prefs.HudCorner;
import fr.varyon.vrpg.ui.prefs.HudLayoutHelper;
import fr.varyon.vrpg.ui.prefs.PlayerUiPreferences;
import fr.varyon.vrpg.ui.prefs.PlayerUiPreferencesManager;

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
                int delta = parseDelta(data.delta);
                if (delta == 0) return UiEventResult.NONE;
                prefs.classHudOffsetX = HudLayoutHelper.clampClassOffsetX(
                    prefs.classHudOffsetX + delta, prefs.classHudCorner);
                offsetChange = true;
            }
            case "classHudOffsetY" -> {
                int delta = parseDelta(data.delta);
                if (delta == 0) return UiEventResult.NONE;
                prefs.classHudOffsetY = HudLayoutHelper.clampClassOffsetY(
                    prefs.classHudOffsetY + delta, prefs.classHudCorner);
                offsetChange = true;
            }
            case "profHudOffsetX" -> {
                int delta = parseDelta(data.delta);
                if (delta == 0) return UiEventResult.NONE;
                prefs.professionHudOffsetX = HudLayoutHelper.clampProfessionOffsetX(
                    prefs.professionHudOffsetX + delta, prefs.professionHudCorner);
                offsetChange = true;
            }
            case "profHudOffsetY" -> {
                int delta = parseDelta(data.delta);
                if (delta == 0) return UiEventResult.NONE;
                prefs.professionHudOffsetY = HudLayoutHelper.clampProfessionOffsetY(
                    prefs.professionHudOffsetY + delta, prefs.professionHudCorner);
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

    private static int parseDelta(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
