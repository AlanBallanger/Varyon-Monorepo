package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MenuRpgBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    /** Cached Class/Method reflective lookups — this bridge runs on every menu build/tab switch,
     * so re-resolving Class.forName/getMethod fresh on every call was real avoidable per-interaction
     * CPU cost (a dozen+ classes and methods resolved from scratch on every rebuild). */
    private static final Map<String, java.util.Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, java.util.Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    private static Class<?> classOf(String name, ClassLoader ldr) throws ClassNotFoundException {
        java.util.Optional<Class<?>> cached = CLASS_CACHE.computeIfAbsent(name, n -> {
            try {
                return java.util.Optional.of(Class.forName(n, true, ldr));
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        });
        if (cached.isEmpty()) {
            throw new ClassNotFoundException(name);
        }
        return cached.get();
    }

    private static Method methodOf(Class<?> owner, String name, Class<?>... paramTypes) throws NoSuchMethodException {
        StringBuilder key = new StringBuilder(owner.getName()).append('#').append(name);
        for (Class<?> pt : paramTypes) {
            key.append(',').append(pt.getName());
        }
        java.util.Optional<Method> cached = METHOD_CACHE.computeIfAbsent(key.toString(), k -> {
            try {
                Method m = owner.getMethod(name, paramTypes);
                m.setAccessible(true);
                return java.util.Optional.of(m);
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        });
        if (cached.isEmpty()) {
            throw new NoSuchMethodException(owner.getName() + "#" + name);
        }
        return cached.get();
    }

    private static final String PLUGIN_CLASS      = "fr.varyon.vrpg.VaryonRpgPlugin";
    private static final String CLASS_MANAGER     = "fr.varyon.vrpg.classes.ClassManager";
    private static final String CLASS_ACCOUNT     = "fr.varyon.vrpg.classes.ClassAccount";
    private static final String CLASS_PROGRESS    = "fr.varyon.vrpg.classes.ClassProgress";
    private static final String PLAYER_CLASS      = "fr.varyon.vrpg.classes.PlayerClass";
    private static final String PLAYER_SPEC       = "fr.varyon.vrpg.classes.PlayerSpecialization";
    private static final String CLASS_STAT_ENGINE = "fr.varyon.vrpg.classes.ClassStatEngine";
    private static final String CLASS_PLAYER_STATS = "fr.varyon.vrpg.classes.ClassPlayerStats";
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

    private static final PatchStyle ICON_HP          = new PatchStyle().setTexturePath(Value.of("Icons/health.png"));
    private static final PatchStyle ICON_ARM         = new PatchStyle().setTexturePath(Value.of("Icons/defense.png"));
    private static final PatchStyle ICON_STA         = new PatchStyle().setTexturePath(Value.of("Icons/stamina.png"));
    private static final PatchStyle ICON_ATK         = new PatchStyle().setTexturePath(Value.of("Icons/attack.png"));
    private static final PatchStyle ICON_CRIT_CHANCE = new PatchStyle().setTexturePath(Value.of("Icons/taux_crit.png"));
    private static final PatchStyle ICON_CRIT_DAMAGE = new PatchStyle().setTexturePath(Value.of("Icons/degat_crit.png"));

    private MenuRpgBridge() {}

    public static void applyMenuXp(@Nonnull UUID playerId, @Nonnull UICommandBuilder ui) {
        final Class<?> pluginClass;
        try {
            pluginClass = classOf(PLUGIN_CLASS, MenuRpgBridge.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            applyFallback(ui);
            return;
        }
        ClassLoader ldr = pluginClass.getClassLoader();
        try {
            Object plugin = methodOf(pluginClass, "getInstance").invoke(null);
            if (plugin == null) { applyFallback(ui); return; }

            applyClassSection(ui, plugin, pluginClass, ldr, playerId);
            applyJobSection(ui, plugin, pluginClass, ldr, playerId);
            applyStatsSection(ui, plugin, pluginClass, ldr, playerId);
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "[MenuRpgBridge] erreur XP menu", unwrap(e));
            applyFallback(ui);
        }
    }

    private static void applyClassSection(UICommandBuilder ui, Object plugin, Class<?> pluginClass,
                                          ClassLoader ldr, UUID uuid) throws ReflectiveOperationException {
        Object classManager = methodOf(pluginClass, "getClassManager").invoke(plugin);
        if (classManager == null) { emptyClassSlot(ui); return; }

        Class<?> cmClass = classOf(CLASS_MANAGER, ldr);
        Class<?> accClass = classOf(CLASS_ACCOUNT, ldr);
        Class<?> progClass = classOf(CLASS_PROGRESS, ldr);

        Object acc = methodOf(cmClass, "getOrLoad", UUID.class).invoke(classManager, uuid);
        if (acc == null) { emptyClassSlot(ui); return; }

        Object activeClass = methodOf(accClass, "getActiveClass").invoke(acc);
        if (activeClass == null) { emptyClassSlot(ui); return; }

        Class<?> pcClass = classOf(PLAYER_CLASS, ldr);
        Class<?> psClass = classOf(PLAYER_SPEC, ldr);

        Object prog = methodOf(accClass, "getProgress", pcClass).invoke(acc, activeClass);
        int level = ((Number) methodOf(progClass, "getLevel").invoke(prog)).intValue();
        long xpIn = ((Number) methodOf(progClass, "getXpInLevel").invoke(prog)).longValue();
        long xpTo = ((Number) methodOf(progClass, "getXpToNextLevel").invoke(prog)).longValue();
        boolean maxLvl = (boolean) methodOf(progClass, "isMaxLevel").invoke(prog);
        Object activeSpec = methodOf(progClass, "getActiveSpec").invoke(prog);

        String displayName = activeSpec != null
                ? (String) methodOf(psClass, "getDisplayName").invoke(activeSpec)
                : (String) methodOf(pcClass, "getDisplayName").invoke(activeClass);

        int talentPts = ((Number) methodOf(accClass, "availableTalentPoints", pcClass)
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
        Object profManager = methodOf(pluginClass, "getProfessionManager").invoke(plugin);
        if (profManager == null) { emptyJobSlot(ui, 1); emptyJobSlot(ui, 2); return; }

        Class<?> pmClass = classOf(PROF_MANAGER, ldr);
        Class<?> paClass = classOf(PLAYER_ACCOUNT, ldr);
        Class<?> profClass = classOf(PROFESSION, ldr);
        Class<?> ppClass = classOf(PROF_PROGRESS, ldr);

        Object acc = methodOf(pmClass, "getOrLoad", UUID.class).invoke(profManager, uuid);
        if (acc == null) { emptyJobSlot(ui, 1); emptyJobSlot(ui, 2); return; }

        Object slot0 = methodOf(paClass, "getActiveSlot0").invoke(acc);
        Object slot1 = methodOf(paClass, "getActiveSlot1").invoke(acc);

        applyJobSlot(ui, 1, slot0, acc, paClass, profClass, ppClass);
        applyJobSlot(ui, 2, slot1, acc, paClass, profClass, ppClass);
    }

    private static void applyJobSlot(UICommandBuilder ui, int slot, Object prof, Object acc,
                                     Class<?> paClass, Class<?> profClass, Class<?> ppClass)
            throws ReflectiveOperationException {
        if (prof == null) { emptyJobSlot(ui, slot); return; }

        Object prog = methodOf(paClass, "getProgress", profClass).invoke(acc, prof);
        String displayName = (String) methodOf(profClass, "getDisplayName").invoke(prof);
        int level = ((Number) methodOf(ppClass, "getLevel").invoke(prog)).intValue();
        long xpIn = ((Number) methodOf(ppClass, "getXpInLevel").invoke(prog)).longValue();
        long xpTo = ((Number) methodOf(ppClass, "getXpToNextLevel").invoke(prog)).longValue();
        boolean maxLvl = (boolean) methodOf(ppClass, "isMaxLevel").invoke(prog);

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

    private static void applyStatsSection(UICommandBuilder ui, Object plugin, Class<?> pluginClass,
                                           ClassLoader ldr, UUID uuid) throws ReflectiveOperationException {
        Object classManager = methodOf(pluginClass, "getClassManager").invoke(plugin);
        if (classManager == null) { emptyStats(ui); return; }

        Class<?> cmClass = classOf(CLASS_MANAGER, ldr);
        Class<?> statEngineClass = classOf(CLASS_STAT_ENGINE, ldr);
        Class<?> statsClass = classOf(CLASS_PLAYER_STATS, ldr);

        Object statEngine = methodOf(cmClass, "getStatEngine").invoke(classManager);
        if (statEngine == null) { emptyStats(ui); return; }

        Object stats = methodOf(statEngineClass, "getStats", UUID.class).invoke(statEngine, uuid);
        if (stats == null) { emptyStats(ui); return; }

        int maxHp        = ((Number) methodOf(statsClass, "maxHp").invoke(stats)).intValue();
        int atk          = ((Number) methodOf(statsClass, "atk").invoke(stats)).intValue();
        int armorPct     = ((Number) methodOf(statsClass, "armorPct").invoke(stats)).intValue();
        int maxStamina   = ((Number) methodOf(statsClass, "maxStamina").invoke(stats)).intValue();
        int critChancePct = ((Number) methodOf(statsClass, "critChancePct").invoke(stats)).intValue();
        int critDamagePct = ((Number) methodOf(statsClass, "critDamagePct").invoke(stats)).intValue();

        ui.setObject("#SidebarStatHPIcon.Background",         ICON_HP);
        ui.setObject("#SidebarStatArmorIcon.Background",      ICON_ARM);
        ui.setObject("#SidebarStatStaminaIcon.Background",    ICON_STA);
        ui.setObject("#SidebarStatATKIcon.Background",        ICON_ATK);
        ui.setObject("#SidebarStatCritChanceIcon.Background", ICON_CRIT_CHANCE);
        ui.setObject("#SidebarStatCritDamageIcon.Background", ICON_CRIT_DAMAGE);
        ui.set("#SidebarStatHPValueMain.TextSpans",         Message.raw(String.valueOf(maxHp)));
        ui.set("#SidebarStatArmorValueMain.TextSpans",      Message.raw(armorPct + "%"));
        ui.set("#SidebarStatStaminaValueMain.TextSpans",    Message.raw(String.valueOf(maxStamina)));
        ui.set("#SidebarStatATKValueMain.TextSpans",        Message.raw(String.valueOf(atk)));
        ui.set("#SidebarStatCritChanceValueMain.TextSpans", Message.raw(critChancePct + "%"));
        ui.set("#SidebarStatCritDamageValueMain.TextSpans", Message.raw("+" + critDamagePct + "%"));
    }

    private static void emptyStats(UICommandBuilder ui) {
        ui.setObject("#SidebarStatHPIcon.Background",         ICON_HP);
        ui.setObject("#SidebarStatArmorIcon.Background",      ICON_ARM);
        ui.setObject("#SidebarStatStaminaIcon.Background",    ICON_STA);
        ui.setObject("#SidebarStatATKIcon.Background",        ICON_ATK);
        ui.setObject("#SidebarStatCritChanceIcon.Background", ICON_CRIT_CHANCE);
        ui.setObject("#SidebarStatCritDamageIcon.Background", ICON_CRIT_DAMAGE);
        ui.set("#SidebarStatHPValueMain.TextSpans",         Message.raw("—"));
        ui.set("#SidebarStatArmorValueMain.TextSpans",      Message.raw("—"));
        ui.set("#SidebarStatStaminaValueMain.TextSpans",    Message.raw("—"));
        ui.set("#SidebarStatATKValueMain.TextSpans",        Message.raw("—"));
        ui.set("#SidebarStatCritChanceValueMain.TextSpans", Message.raw("—"));
        ui.set("#SidebarStatCritDamageValueMain.TextSpans", Message.raw("—"));
    }

    private static void applyFallback(UICommandBuilder ui) {
        emptyClassSlot(ui);
        emptyJobSlot(ui, 1);
        emptyJobSlot(ui, 2);
        emptyStats(ui);
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
