package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the Varyon faction-dominance bonus (HP/damage multipliers) for display in varyon-UI.
 * Mirrors {@link CombatProfilBridge}'s reflection-bridge pattern: Varyon and varyon-UI are
 * separate jars with no compile-time dependency on each other.
 */
public final class FactionBonusBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");
    private static final String PLUGIN_CLASS = "com.varyon.VaryonPlugin";
    private static final String FACTION_MANAGER_CLASS = "com.varyon.faction.FactionManager";
    private static final String FACTION_BONUS_MANAGER_CLASS = "com.varyon.points.FactionBonusManager";

    private static final Map<String, java.util.Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, java.util.Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    private FactionBonusBridge() {}

    public static final class FactionBonusInfo {
        public final String factionDisplayName;
        public final float damageMultiplier;
        public final float hpMultiplier;

        FactionBonusInfo(String factionDisplayName, float damageMultiplier, float hpMultiplier) {
            this.factionDisplayName = factionDisplayName;
            this.damageMultiplier = damageMultiplier;
            this.hpMultiplier = hpMultiplier;
        }

        public boolean hasBonus() {
            return damageMultiplier > 1.0f || hpMultiplier > 1.0f;
        }
    }

    /** Returns null if the Varyon mod, faction data, or bonus manager is unavailable. */
    @Nullable
    public static FactionBonusInfo getBonusForPlayer(@Nonnull PlayerRef playerRef) {
        try {
            java.util.Optional<Class<?>> pluginClass = resolveClass(PLUGIN_CLASS);
            if (pluginClass.isEmpty()) return null;

            Object factionManager = invokeStaticNoArg(pluginClass.get(), "getStaticFactionManager");
            Object bonusManager = invokeStaticNoArg(pluginClass.get(), "getStaticFactionBonusManager");
            if (factionManager == null || bonusManager == null) return null;

            java.util.Optional<Class<?>> factionManagerClass = resolveClass(FACTION_MANAGER_CLASS);
            if (factionManagerClass.isEmpty()) return null;
            java.util.Optional<Method> getFaction = resolveMethod(factionManagerClass.get(), "getFaction", PlayerRef.class);
            if (getFaction.isEmpty()) return null;
            Object faction = getFaction.get().invoke(factionManager, playerRef);
            if (faction == null) return null;

            String displayName = (String) invokeNoArg(faction, "getDisplayName");

            java.util.Optional<Class<?>> bonusManagerClass = resolveClass(FACTION_BONUS_MANAGER_CLASS);
            if (bonusManagerClass.isEmpty()) return null;
            java.util.Optional<Method> getTierForPlayer = resolveMethod(bonusManagerClass.get(), "getTierForPlayer", PlayerRef.class);
            if (getTierForPlayer.isEmpty()) return null;
            Object tier = getTierForPlayer.get().invoke(bonusManager, playerRef);
            if (tier == null) return null;

            float dmgMult = ((Number) invokeNoArg(tier, "damageMultiplier")).floatValue();
            float hpMult = ((Number) invokeNoArg(tier, "hpMultiplier")).floatValue();

            return new FactionBonusInfo(displayName, dmgMult, hpMult);
        } catch (Throwable t) {
            LOG.log(Level.FINE, "FactionBonusBridge: lookup failed", t);
            return null;
        }
    }

    private static java.util.Optional<Class<?>> resolveClass(String name) {
        return CLASS_CACHE.computeIfAbsent(name, n -> {
            try {
                return java.util.Optional.of(Class.forName(n));
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        });
    }

    private static java.util.Optional<Method> resolveMethod(Class<?> owner, String name, Class<?>... paramTypes) {
        StringBuilder key = new StringBuilder(owner.getName()).append('#').append(name);
        for (Class<?> pt : paramTypes) {
            key.append(',').append(pt.getName());
        }
        return METHOD_CACHE.computeIfAbsent(key.toString(), k -> {
            try {
                Method m = owner.getMethod(name, paramTypes);
                m.setAccessible(true);
                return java.util.Optional.of(m);
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        });
    }

    @Nullable
    private static Object invokeStaticNoArg(Class<?> owner, String name) {
        java.util.Optional<Method> m = resolveMethod(owner, name);
        if (m.isEmpty()) return null;
        try {
            return m.get().invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static Object invokeNoArg(Object target, String name) {
        if (target == null) return null;
        java.util.Optional<Method> m = resolveMethod(target.getClass(), name);
        if (m.isEmpty()) return null;
        try {
            return m.get().invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }
}
