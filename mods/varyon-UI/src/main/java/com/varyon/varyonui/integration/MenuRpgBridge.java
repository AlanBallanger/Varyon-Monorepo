package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MenuRpgBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    private static final String PLUGIN_CLASS      = "fr.varyon.vrpg.VaryonRpgPlugin";
    private static final String CLASS_MANAGER     = "fr.varyon.vrpg.classes.ClassManager";
    private static final String CLASS_ACCOUNT     = "fr.varyon.vrpg.classes.ClassAccount";
    private static final String CLASS_PROGRESS    = "fr.varyon.vrpg.classes.ClassProgress";
    private static final String PLAYER_CLASS      = "fr.varyon.vrpg.classes.PlayerClass";
    private static final String PLAYER_SPEC       = "fr.varyon.vrpg.classes.PlayerSpecialization";
    private static final String PROF_MANAGER      = "fr.varyon.vrpg.rpg.ProfessionManager";
    private static final String PLAYER_ACCOUNT    = "fr.varyon.vrpg.rpg.PlayerAccount";
    private static final String PROFESSION        = "fr.varyon.vrpg.rpg.Profession";
    private static final String PROF_PROGRESS     = "fr.varyon.vrpg.rpg.ProfessionProgress";

    private static final PatchStyle LEVEL_BG_CLASS =
            new PatchStyle().setColor(Value.of("#2a1a00e0"));
    private static final PatchStyle LEVEL_BG_JOB =
            new PatchStyle().setColor(Value.of("#1a150ae0"));
    private static final PatchStyle XP_TRACK_BG =
            new PatchStyle().setTexturePath(Value.of("Common/Gauge.png")).setBorder(Value.of(0));
    private static final PatchStyle CLEAR =
            new PatchStyle().setColor(Value.of("#00000000"));

    private MenuRpgBridge() {}

    public static void applyMenuXp(@Nonnull UUID playerId, @Nonnull UICommandBuilder ui) {
        final Class<?> pluginClass;
        try {
            pluginClass = Class.forName(PLUGIN_CLASS);
        } catch (ClassNotFoundException e) {
            applyFallback(ui);
            return;
        }
        ClassLoader ldr = pluginClass.getClassLoader();
        try {
            Object plugin = access(pluginClass.getMethod("getInstance")).invoke(null);
            if (plugin == null) { applyFallback(ui); return; }

            applyClassSection(ui, plugin, pluginClass, ldr, playerId);
            applyJobSection(ui, plugin, pluginClass, ldr, playerId);
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "[MenuRpgBridge] erreur XP menu", unwrap(e));
            applyFallback(ui);
        }
    }

    private static void applyClassSection(UICommandBuilder ui, Object plugin, Class<?> pluginClass,
                                          ClassLoader ldr, UUID uuid) throws ReflectiveOperationException {
        Object classManager = access(pluginClass.getMethod("getClassManager")).invoke(plugin);
        if (classManager == null) { emptyClassSlot(ui); return; }

        Class<?> cmClass = Class.forName(CLASS_MANAGER, true, ldr);
        Class<?> accClass = Class.forName(CLASS_ACCOUNT, true, ldr);
        Class<?> progClass = Class.forName(CLASS_PROGRESS, true, ldr);

        Object acc = access(cmClass.getMethod("getOrLoad", UUID.class)).invoke(classManager, uuid);
        if (acc == null) { emptyClassSlot(ui); return; }

        Object activeClass = access(accClass.getMethod("getActiveClass")).invoke(acc);
        if (activeClass == null) { emptyClassSlot(ui); return; }

        Class<?> pcClass = Class.forName(PLAYER_CLASS, true, ldr);
        Class<?> psClass = Class.forName(PLAYER_SPEC, true, ldr);

        Object prog = access(accClass.getMethod("getProgress", pcClass)).invoke(acc, activeClass);
        int level = ((Number) access(progClass.getMethod("getLevel")).invoke(prog)).intValue();
        long xpIn = ((Number) access(progClass.getMethod("getXpInLevel")).invoke(prog)).longValue();
        long xpTo = ((Number) access(progClass.getMethod("getXpToNextLevel")).invoke(prog)).longValue();
        boolean maxLvl = (boolean) access(progClass.getMethod("isMaxLevel")).invoke(prog);
        Object activeSpec = access(progClass.getMethod("getActiveSpec")).invoke(prog);

        String displayName = activeSpec != null
                ? (String) access(psClass.getMethod("getDisplayName")).invoke(activeSpec)
                : (String) access(pcClass.getMethod("getDisplayName")).invoke(activeClass);

        int talentPts = ((Number) access(accClass.getMethod("availableTalentPoints", pcClass))
                .invoke(acc, activeClass)).intValue();
        String levelTxt = "Nv." + level + (talentPts > 0 ? " *" : "");
        String xpTxt = maxLvl ? "MAX" : (xpIn + "/" + xpTo);
        double prog01 = maxLvl ? 1.0 : (xpTo > 0 ? clamp01((double) xpIn / (double) xpTo) : 0.0);

        ui.setObject("#MenuSpePanel.Background", CLEAR);
        ui.setObject("#MenuSpeLevelBg.Background", LEVEL_BG_CLASS);
        ui.setObject("#MenuSpeXPTrack.Background", XP_TRACK_BG);
        ui.set("#MenuSpeLevel.TextSpans", Message.raw(levelTxt));
        ui.set("#MenuSpeName.TextSpans", Message.raw(displayName));
        ui.set("#MenuSpeXPText.TextSpans", Message.raw(xpTxt));
        ui.set("#MenuSpeXP.Value", prog01);
    }

    private static void applyJobSection(UICommandBuilder ui, Object plugin, Class<?> pluginClass,
                                        ClassLoader ldr, UUID uuid) throws ReflectiveOperationException {
        Object profManager = access(pluginClass.getMethod("getProfessionManager")).invoke(plugin);
        if (profManager == null) { emptyJobSlot(ui, 1); emptyJobSlot(ui, 2); return; }

        Class<?> pmClass = Class.forName(PROF_MANAGER, true, ldr);
        Class<?> paClass = Class.forName(PLAYER_ACCOUNT, true, ldr);
        Class<?> profClass = Class.forName(PROFESSION, true, ldr);
        Class<?> ppClass = Class.forName(PROF_PROGRESS, true, ldr);

        Object acc = access(pmClass.getMethod("getOrLoad", UUID.class)).invoke(profManager, uuid);
        if (acc == null) { emptyJobSlot(ui, 1); emptyJobSlot(ui, 2); return; }

        Object slot0 = access(paClass.getMethod("getActiveSlot0")).invoke(acc);
        Object slot1 = access(paClass.getMethod("getActiveSlot1")).invoke(acc);

        applyJobSlot(ui, 1, slot0, acc, paClass, profClass, ppClass);
        applyJobSlot(ui, 2, slot1, acc, paClass, profClass, ppClass);
    }

    private static void applyJobSlot(UICommandBuilder ui, int slot, Object prof, Object acc,
                                     Class<?> paClass, Class<?> profClass, Class<?> ppClass)
            throws ReflectiveOperationException {
        if (prof == null) { emptyJobSlot(ui, slot); return; }

        Object prog = access(paClass.getMethod("getProgress", profClass)).invoke(acc, prof);
        String displayName = (String) access(profClass.getMethod("getDisplayName")).invoke(prof);
        int level = ((Number) access(ppClass.getMethod("getLevel")).invoke(prog)).intValue();
        long xpIn = ((Number) access(ppClass.getMethod("getXpInLevel")).invoke(prog)).longValue();
        long xpTo = ((Number) access(ppClass.getMethod("getXpToNextLevel")).invoke(prog)).longValue();
        boolean maxLvl = (boolean) access(ppClass.getMethod("isMaxLevel")).invoke(prog);

        String levelTxt = "Nv." + level;
        String xpTxt = maxLvl ? "MAX" : (xpIn + "/" + xpTo);
        double prog01 = maxLvl ? 1.0 : (xpTo > 0 ? clamp01((double) xpIn / (double) xpTo) : 0.0);

        String p = "#MenuJob" + slot;
        ui.setObject(p + "Panel.Background", CLEAR);
        ui.setObject(p + "LevelBg.Background", LEVEL_BG_JOB);
        ui.setObject(p + "XPTrack.Background", XP_TRACK_BG);
        ui.set(p + "Name.TextSpans", Message.raw(displayName));
        ui.set(p + "Level.TextSpans", Message.raw(levelTxt));
        ui.set(p + "XPText.TextSpans", Message.raw(xpTxt));
        ui.set(p + "XP.Value", prog01);
    }

    private static void emptyClassSlot(UICommandBuilder ui) {
        ui.setObject("#MenuSpePanel.Background", CLEAR);
        ui.setObject("#MenuSpeLevelBg.Background", LEVEL_BG_CLASS);
        ui.setObject("#MenuSpeXPTrack.Background", XP_TRACK_BG);
        ui.set("#MenuSpeLevel.TextSpans", Message.raw(" "));
        ui.set("#MenuSpeName.TextSpans", Message.raw(" "));
        ui.set("#MenuSpeXPText.TextSpans", Message.raw(" "));
        ui.set("#MenuSpeXP.Value", 0.0);
    }

    private static void emptyJobSlot(UICommandBuilder ui, int slot) {
        String p = "#MenuJob" + slot;
        ui.setObject(p + "Panel.Background", CLEAR);
        ui.setObject(p + "LevelBg.Background", LEVEL_BG_JOB);
        ui.setObject(p + "XPTrack.Background", XP_TRACK_BG);
        ui.set(p + "Name.TextSpans", Message.raw(" "));
        ui.set(p + "Level.TextSpans", Message.raw(" "));
        ui.set(p + "XPText.TextSpans", Message.raw(" "));
        ui.set(p + "XP.Value", 0.0);
    }

    private static void applyFallback(UICommandBuilder ui) {
        emptyClassSlot(ui);
        emptyJobSlot(ui, 1);
        emptyJobSlot(ui, 2);
    }

    private static Method access(Method m) {
        m.setAccessible(true);
        return m;
    }

    private static Throwable unwrap(Throwable e) {
        Throwable c = e;
        while (c instanceof InvocationTargetException w && w.getCause() != null) c = w.getCause();
        return c;
    }

    private static double clamp01(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }
}
