package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MenuRpgBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    private static final String RPG_PLUGIN_CLASS = "fr.varyon.rpg.VaryonRPG";
    private static final String RPG_ACCESS_CLASS = "fr.varyon.rpg.VaryonRpgAccess";
    private static final String RPG_API_INTERFACE_CLASS = "fr.varyon.rpg.api.VaryonRpgApi";
    private static final String PLAYER_SNAPSHOT_CLASS = "fr.varyon.rpg.api.PlayerRpgSnapshot";
    private static final String JOB_SNAPSHOT_CLASS = "fr.varyon.rpg.api.JobRpgSnapshot";

    private static final PatchStyle GEN_LEVEL_BG = new PatchStyle().setColor(Value.of("#2a1a00e0"));
    private static final PatchStyle XP_TRACK_BG =
            new PatchStyle().setTexturePath(Value.of("Common/ProgressBarEmpty.png")).setBorder(Value.of(4));
    private static final PatchStyle JOB_LEVEL_BG = new PatchStyle().setColor(Value.of("#1a150ae0"));

    private static final PatchStyle CLEAR_ICON =
            new PatchStyle().setColor(Value.of("#00000000"));

    private MenuRpgBridge() {}

    public static void applyMenuXp(@Nonnull UUID playerId, @Nonnull UICommandBuilder ui) {
        final Class<?> rpgClass;
        try {
            rpgClass = Class.forName(RPG_PLUGIN_CLASS);
        } catch (ClassNotFoundException e) {
            applyMenuXpFallback(ui, false);
            return;
        }
        ClassLoader pluginLoader = rpgClass.getClassLoader();
        try {
            Class<?> accessClass = Class.forName(RPG_ACCESS_CLASS, true, pluginLoader);
            Method optionalMethod = accessClass.getMethod("optional");
            Object rawOpt = optionalMethod.invoke(null);
            if (!(rawOpt instanceof Optional<?> apiOpt) || apiOpt.isEmpty()) {
                applyMenuXpFallback(ui, false);
                return;
            }
            Object api = apiOpt.get();
            Class<?> apiIface = Class.forName(RPG_API_INTERFACE_CLASS, true, pluginLoader);
            Method readyMethod = accessible(apiIface.getMethod("isPluginReady"));
            if (!(Boolean) readyMethod.invoke(api)) {
                applyMenuXpFallback(ui, false);
                return;
            }
            Method snapMethod = accessible(apiIface.getMethod("getPlayerSnapshot", UUID.class));
            Object rawSnapOpt = snapMethod.invoke(api, playerId);
            if (!(rawSnapOpt instanceof Optional<?> snapOpt) || snapOpt.isEmpty()) {
                applyMenuXpFallback(ui, false);
                return;
            }
            applySnapshot(ui, snapOpt.get(), pluginLoader);
        } catch (Throwable e) {
            Throwable root = unwrap(e);
            LOG.log(Level.WARNING, "[MenuRpgBridge] snapshot impossible", root);
            applyMenuXpFallback(ui, true);
        }
    }

    private static Throwable unwrap(Throwable e) {
        Throwable c = e;
        while (c instanceof InvocationTargetException wrap && wrap.getCause() != null) {
            c = wrap.getCause();
        }
        return c;
    }

    private static Method accessible(Method m) {
        m.setAccessible(true);
        return m;
    }

    private static void applySnapshot(UICommandBuilder ui, Object snap, ClassLoader pluginLoader) throws ReflectiveOperationException {
        Class<?> playerSnapClass = Class.forName(PLAYER_SNAPSHOT_CLASS, true, pluginLoader);
        Class<?> jobSnapClass = Class.forName(JOB_SNAPSHOT_CLASS, true, pluginLoader);

        int generalLevel = number(invokeZeroArg(snap, playerSnapClass, "generalLevel")).intValue();
        double genProg = number(invokeZeroArg(snap, playerSnapClass, "generalXpProgress")).doubleValue();
        String genText = stringOrEmpty(invokeZeroArg(snap, playerSnapClass, "generalXpText"));
        boolean genMax = booleanVal(invokeZeroArg(snap, playerSnapClass, "generalAtMaxLevel"));
        String className = stringOrEmpty(invokeZeroArg(snap, playerSnapClass, "displayClassName"));
        int talentPts = number(invokeZeroArg(snap, playerSnapClass, "talentPoints")).intValue();

        String genLevelTxt = "Nv." + generalLevel + (talentPts > 0 ? " *" : "");

        ui.setObject("#MenuGenXPPanel.Background", CLEAR_ICON);
        ui.setObject("#MenuGenLevelBg.Background", GEN_LEVEL_BG);
        ui.setObject("#MenuGenXPTrack.Background", XP_TRACK_BG);
        ui.set("#MenuGenLevel.TextSpans", Message.raw(genLevelTxt));
        ui.set("#MenuGenLevelLabel.TextSpans", Message.raw(className));
        ui.set("#MenuGenXPText.TextSpans", Message.raw(genText));
        ui.set("#MenuGenXP.Value", genMax ? 1.0 : clamp01(genProg));

        @SuppressWarnings("unchecked")
        List<Object> jobs = (List<Object>) invokeZeroArg(snap, playerSnapClass, "professions");
        for (int slot = 1; slot <= 2; slot++) {
            int idx = slot - 1;
            if (idx < jobs.size()) {
                Object job = jobs.get(idx);
                applyJobSlot(ui, slot, job, jobSnapClass);
            } else {
                emptyJobSlot(ui, slot);
            }
        }
    }

    private static void applyJobSlot(UICommandBuilder ui, int slot, Object job, Class<?> jobSnapClass) throws ReflectiveOperationException {
        String displayName = stringOrEmpty(invokeZeroArg(job, jobSnapClass, "displayName"));
        int level = number(invokeZeroArg(job, jobSnapClass, "level")).intValue();
        long xpIn = number(invokeZeroArg(job, jobSnapClass, "xpInLevel")).longValue();
        long xpReq = number(invokeZeroArg(job, jobSnapClass, "xpRequiredForNext")).longValue();
        boolean max = booleanVal(invokeZeroArg(job, jobSnapClass, "atMaxLevel"));

        String levelTxt = "Nv." + level;
        String xpLine = max ? "MAX" : (xpIn + "/" + xpReq);
        double prog = max ? 1.0 : (xpReq > 0 ? (double) xpIn / (double) xpReq : 0.0);

        String p = "#MenuJob" + slot;
        ui.setObject(p + "Panel.Background", CLEAR_ICON);
        ui.setObject(p + "XPTrack.Background", XP_TRACK_BG);
        ui.setObject(p + "LevelBg.Background", JOB_LEVEL_BG);
        ui.set(p + "Name.TextSpans", Message.raw(displayName));
        ui.set(p + "Level.TextSpans", Message.raw(levelTxt));
        ui.set(p + "XPText.TextSpans", Message.raw(xpLine));
        ui.set(p + "XP.Value", clamp01(prog));
    }

    private static void emptyJobSlot(UICommandBuilder ui, int slot) {
        String p = "#MenuJob" + slot;
        ui.setObject(p + "Panel.Background", CLEAR_ICON);
        ui.setObject(p + "XPTrack.Background", XP_TRACK_BG);
        ui.setObject(p + "LevelBg.Background", JOB_LEVEL_BG);
        ui.set(p + "Name.TextSpans", Message.raw(" "));
        ui.set(p + "Level.TextSpans", Message.raw(" "));
        ui.set(p + "XPText.TextSpans", Message.raw(" "));
        ui.set(p + "XP.Value", 0.0);
    }

    private static void applyMenuXpFallback(UICommandBuilder ui, boolean errorState) {
        ui.setObject("#MenuGenXPPanel.Background", CLEAR_ICON);
        ui.setObject("#MenuGenLevelBg.Background", GEN_LEVEL_BG);
        ui.setObject("#MenuGenXPTrack.Background", XP_TRACK_BG);
        String hint = errorState ? "Erreur RPG" : "—";
        ui.set("#MenuGenLevel.TextSpans", Message.raw(hint));
        ui.set("#MenuGenLevelLabel.TextSpans", Message.raw(errorState ? " " : "VaryonRPG"));
        ui.set("#MenuGenXPText.TextSpans", Message.raw(errorState ? " " : " "));
        ui.set("#MenuGenXP.Value", 0.0);
        emptyJobSlot(ui, 1);
        emptyJobSlot(ui, 2);
    }

    private static Object invokeZeroArg(Object target, Class<?> declarator, String name) throws ReflectiveOperationException {
        Method m = accessible(declarator.getMethod(name));
        return m.invoke(target);
    }

    private static Number number(Object o) {
        return (Number) o;
    }

    private static boolean booleanVal(Object o) {
        return (Boolean) o;
    }

    private static String stringOrEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double clamp01(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }
}
