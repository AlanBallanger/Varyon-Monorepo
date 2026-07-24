package fr.varyon.vrpg.ui.tabs;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.ui.prefs.HudCorner;
import fr.varyon.vrpg.ui.prefs.HudLayoutHelper;
import fr.varyon.vrpg.ui.prefs.PlayerUiPreferences;

import javax.annotation.Nonnull;

public final class SettingsTab {

    private static final PatchStyle BTN_DEFAULT = new PatchStyle().setColor(Value.of("#1a2838CC"));
    private static final PatchStyle BTN_HOVER = new PatchStyle().setColor(Value.of("#243448CC"));
    private static final PatchStyle BTN_ACTIVE = new PatchStyle().setColor(Value.of("#D4AF3760"));
    private static final PatchStyle BTN_ACTIVE_HOVER = new PatchStyle().setColor(Value.of("#D4AF3788"));
    private static final String CORNER_ACTIVE_TEXT = "#FFFFFF";
    private static final String CORNER_INACTIVE_TEXT = "#8899AA";
    private static final String ON_ACTIVE_BG = "#27AE60";
    private static final String ON_INACTIVE_BG = "#27AE6047";
    private static final String OFF_ACTIVE_BG = "#E74C3C";
    private static final String OFF_INACTIVE_BG = "#E74C3C47";
    private static final String ACTIVE_TEXT = "#FFFFFF";
    private static final String INACTIVE_TEXT = "#FFFFFF55";
    public static final int OFFSET_STEP = 10;

    private SettingsTab() {}

    public static void build(@Nonnull PlayerRef playerRef,
                             @Nonnull UICommandBuilder ui,
                             @Nonnull UIEventBuilder events) {
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        PlayerUiPreferences prefs = plugin != null && plugin.getUiPreferencesManager() != null
            ? plugin.getUiPreferencesManager().get(playerRef.getUuid())
            : new PlayerUiPreferences();

        bindSwitch(events, "#SettingsClassHudOn", "classHud", true);
        bindSwitch(events, "#SettingsClassHudOff", "classHud", false);
        bindSwitch(events, "#SettingsProfHudOn", "profHud", true);
        bindSwitch(events, "#SettingsProfHudOff", "profHud", false);
        bindSwitch(events, "#SettingsXpNotifOn", "xpNotif", true);
        bindSwitch(events, "#SettingsXpNotifOff", "xpNotif", false);
        bindSwitch(events, "#SettingsSkillNotifOn", "skillNotif", true);
        bindSwitch(events, "#SettingsSkillNotifOff", "skillNotif", false);
        bindSwitch(events, "#SettingsProfSoundsOn", "profSounds", true);
        bindSwitch(events, "#SettingsProfSoundsOff", "profSounds", false);

        bindCorner(events, "classHud", "#SettingsClassHudPosTL", HudCorner.TOP_LEFT);
        bindCorner(events, "classHud", "#SettingsClassHudPosTR", HudCorner.TOP_RIGHT);
        bindCorner(events, "classHud", "#SettingsClassHudPosBR", HudCorner.BOTTOM_RIGHT);
        bindCorner(events, "classHud", "#SettingsClassHudPosBL", HudCorner.BOTTOM_LEFT);
        bindCorner(events, "profHud", "#SettingsProfHudPosTL", HudCorner.TOP_LEFT);
        bindCorner(events, "profHud", "#SettingsProfHudPosTR", HudCorner.TOP_RIGHT);
        bindCorner(events, "profHud", "#SettingsProfHudPosBR", HudCorner.BOTTOM_RIGHT);
        bindCorner(events, "profHud", "#SettingsProfHudPosBL", HudCorner.BOTTOM_LEFT);

        bindOffsetStep(events, "#SettingsClassHudOffsetXMinus", "classHudOffsetX", -OFFSET_STEP);
        bindOffsetStep(events, "#SettingsClassHudOffsetXPlus", "classHudOffsetX", OFFSET_STEP);
        bindOffsetStep(events, "#SettingsClassHudOffsetYMinus", "classHudOffsetY", -OFFSET_STEP);
        bindOffsetStep(events, "#SettingsClassHudOffsetYPlus", "classHudOffsetY", OFFSET_STEP);
        bindOffsetStep(events, "#SettingsProfHudOffsetXMinus", "profHudOffsetX", -OFFSET_STEP);
        bindOffsetStep(events, "#SettingsProfHudOffsetXPlus", "profHudOffsetX", OFFSET_STEP);
        bindOffsetStep(events, "#SettingsProfHudOffsetYMinus", "profHudOffsetY", -OFFSET_STEP);
        bindOffsetStep(events, "#SettingsProfHudOffsetYPlus", "profHudOffsetY", OFFSET_STEP);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsClassHudOffsetReset",
            EventData.of("Action", "classHudOffsetReset"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsProfHudOffsetReset",
            EventData.of("Action", "profHudOffsetReset"), false);

        applyDualSwitch(ui, "#SettingsClassHudOn", "#SettingsClassHudOnLabel",
            "#SettingsClassHudOff", "#SettingsClassHudOffLabel", prefs.classHudVisible);
        applyDualSwitch(ui, "#SettingsProfHudOn", "#SettingsProfHudOnLabel",
            "#SettingsProfHudOff", "#SettingsProfHudOffLabel", prefs.professionHudVisible);
        applyDualSwitch(ui, "#SettingsXpNotifOn", "#SettingsXpNotifOnLabel",
            "#SettingsXpNotifOff", "#SettingsXpNotifOffLabel", prefs.xpNotificationsEnabled);
        applyDualSwitch(ui, "#SettingsSkillNotifOn", "#SettingsSkillNotifOnLabel",
            "#SettingsSkillNotifOff", "#SettingsSkillNotifOffLabel", prefs.skillNotificationsEnabled);
        applyDualSwitch(ui, "#SettingsProfSoundsOn", "#SettingsProfSoundsOnLabel",
            "#SettingsProfSoundsOff", "#SettingsProfSoundsOffLabel", prefs.professionSoundsEnabled);

        applyCorner(ui, prefs.classHudCorner,
            "#SettingsClassHudPosTL", "#SettingsClassHudPosTLLabel",
            "#SettingsClassHudPosTR", "#SettingsClassHudPosTRLabel",
            "#SettingsClassHudPosBR", "#SettingsClassHudPosBRLabel",
            "#SettingsClassHudPosBL", "#SettingsClassHudPosBLLabel");
        applyCorner(ui, prefs.professionHudCorner,
            "#SettingsProfHudPosTL", "#SettingsProfHudPosTLLabel",
            "#SettingsProfHudPosTR", "#SettingsProfHudPosTRLabel",
            "#SettingsProfHudPosBR", "#SettingsProfHudPosBRLabel",
            "#SettingsProfHudPosBL", "#SettingsProfHudPosBLLabel");

        applyOffsetDisplays(ui, prefs);
    }

    public static void applyOffsetDisplays(@Nonnull UICommandBuilder ui, @Nonnull PlayerUiPreferences prefs) {
        int classOffsetX = HudLayoutHelper.clampClassOffsetX(prefs.classHudOffsetX, prefs.classHudCorner);
        int classOffsetY = HudLayoutHelper.clampClassOffsetY(prefs.classHudOffsetY, prefs.classHudCorner);
        int profOffsetX = HudLayoutHelper.clampProfessionOffsetX(prefs.professionHudOffsetX, prefs.professionHudCorner);
        int profOffsetY = HudLayoutHelper.clampProfessionOffsetY(prefs.professionHudOffsetY, prefs.professionHudCorner);

        ui.set("#SettingsClassHudOffsetXValue.TextSpans", Message.raw(String.valueOf(classOffsetX)));
        ui.set("#SettingsClassHudOffsetYValue.TextSpans", Message.raw(String.valueOf(classOffsetY)));
        ui.set("#SettingsProfHudOffsetXValue.TextSpans", Message.raw(String.valueOf(profOffsetX)));
        ui.set("#SettingsProfHudOffsetYValue.TextSpans", Message.raw(String.valueOf(profOffsetY)));

        applyCornerDisplays(ui, prefs);
    }

    public static void applyCornerDisplays(@Nonnull UICommandBuilder ui, @Nonnull PlayerUiPreferences prefs) {
        applyCorner(ui, prefs.classHudCorner,
            "#SettingsClassHudPosTL", "#SettingsClassHudPosTLLabel",
            "#SettingsClassHudPosTR", "#SettingsClassHudPosTRLabel",
            "#SettingsClassHudPosBR", "#SettingsClassHudPosBRLabel",
            "#SettingsClassHudPosBL", "#SettingsClassHudPosBLLabel");
        applyCorner(ui, prefs.professionHudCorner,
            "#SettingsProfHudPosTL", "#SettingsProfHudPosTLLabel",
            "#SettingsProfHudPosTR", "#SettingsProfHudPosTRLabel",
            "#SettingsProfHudPosBR", "#SettingsProfHudPosBRLabel",
            "#SettingsProfHudPosBL", "#SettingsProfHudPosBLLabel");
    }

    private static void bindSwitch(@Nonnull UIEventBuilder events, @Nonnull String id,
                                   @Nonnull String setting, boolean enabled) {
        events.addEventBinding(CustomUIEventBindingType.Activating, id,
            EventData.of("Action", enabled ? "settingOn" : "settingOff").append("Setting", setting), false);
    }

    private static void bindCorner(@Nonnull UIEventBuilder events, @Nonnull String setting,
                                   @Nonnull String buttonId, @Nonnull HudCorner corner) {
        events.addEventBinding(CustomUIEventBindingType.Activating, buttonId,
            EventData.of("Action", "settingCorner").append("Setting", setting).append("Corner", corner.id()), false);
    }

    private static void bindOffsetStep(@Nonnull UIEventBuilder events, @Nonnull String buttonId,
                                       @Nonnull String action, int delta) {
        events.addEventBinding(CustomUIEventBindingType.Activating, buttonId,
            EventData.of("Action", action).append("Delta", String.valueOf(delta)), false);
    }

    private static void applyDualSwitch(@Nonnull UICommandBuilder ui,
                                        @Nonnull String onBtn, @Nonnull String onLabel,
                                        @Nonnull String offBtn, @Nonnull String offLabel,
                                        boolean enabled) {
        styleSwitchButton(ui, onBtn, onLabel, enabled, ON_ACTIVE_BG, ON_INACTIVE_BG);
        styleSwitchButton(ui, offBtn, offLabel, !enabled, OFF_ACTIVE_BG, OFF_INACTIVE_BG);
    }

    private static void styleSwitchButton(@Nonnull UICommandBuilder ui,
                                          @Nonnull String btnId, @Nonnull String labelId,
                                          boolean active, @Nonnull String activeBg, @Nonnull String inactiveBg) {
        String bg = active ? activeBg : inactiveBg;
        PatchStyle def = new PatchStyle().setColor(Value.of(bg));
        PatchStyle hov = new PatchStyle().setColor(Value.of(active ? activeBg : inactiveBg));
        String base = btnId + ".Style";
        ui.setObject(base + ".Default.Background", def);
        ui.setObject(base + ".Hovered.Background", hov);
        ui.setObject(base + ".Pressed.Background", def);
        ui.set(labelId + ".Style.TextColor", active ? ACTIVE_TEXT : INACTIVE_TEXT);
    }

    private static void applyCorner(@Nonnull UICommandBuilder ui, @Nonnull HudCorner active,
                                      @Nonnull String tl, @Nonnull String tlLabel,
                                      @Nonnull String tr, @Nonnull String trLabel,
                                      @Nonnull String br, @Nonnull String brLabel,
                                      @Nonnull String bl, @Nonnull String blLabel) {
        styleCornerButton(ui, tl, tlLabel, active == HudCorner.TOP_LEFT);
        styleCornerButton(ui, tr, trLabel, active == HudCorner.TOP_RIGHT);
        styleCornerButton(ui, br, brLabel, active == HudCorner.BOTTOM_RIGHT);
        styleCornerButton(ui, bl, blLabel, active == HudCorner.BOTTOM_LEFT);
    }

    private static void styleCornerButton(@Nonnull UICommandBuilder ui,
                                          @Nonnull String btnId, @Nonnull String labelId,
                                          boolean active) {
        PatchStyle def = active ? BTN_ACTIVE : BTN_DEFAULT;
        PatchStyle hov = active ? BTN_ACTIVE_HOVER : BTN_HOVER;
        String base = btnId + ".Style";
        ui.setObject(base + ".Default.Background", def);
        ui.setObject(base + ".Hovered.Background", hov);
        ui.setObject(base + ".Pressed.Background", def);
        ui.set(labelId + ".Style.TextColor", active ? CORNER_ACTIVE_TEXT : CORNER_INACTIVE_TEXT);
    }
}
