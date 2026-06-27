package fr.varyon.vrpg.integration;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.logging.Level;

public final class DamageFloatBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.get("VaryonRPG");
    private static final boolean DFF_PRESENT;

    private static Method methodMarkKind;
    private static Method methodMarkCritical;
    private static Method methodClearCritical;
    private static Method methodResetForNewEvent;
    private static Method methodIsCritical;
    private static Method methodMarkSkip;
    private static Method methodEmit;
    private static Method methodEmitWithCB;

    static {
        boolean found = false;
        try {
            Class<?> numbers = Class.forName("irai.mod.DynamicFloatingDamageFormatter.DamageNumbers");
            Class<?> dmgClass = Damage.class;
            methodMarkKind     = numbers.getMethod("markKind", dmgClass, String.class);
            methodMarkCritical = numbers.getMethod("markCritical", dmgClass);
            try {
                methodClearCritical = numbers.getMethod("clearCritical", dmgClass);
            } catch (NoSuchMethodException ignored) {
                methodClearCritical = null;
            }
            try {
                methodResetForNewEvent = numbers.getMethod("resetForNewEvent", dmgClass);
            } catch (NoSuchMethodException ignored) {
                methodResetForNewEvent = null;
            }
            methodIsCritical   = numbers.getMethod("isCritical", dmgClass);
            methodMarkSkip     = numbers.getMethod("markSkipCombatText", dmgClass);
            methodEmit         = numbers.getMethod("emit", Store.class, Ref.class, float.class, String.class);
            methodEmitWithCB   = numbers.getMethod("emit", Store.class, CommandBuffer.class, Ref.class, float.class, String.class);
            found = true;
            LOGGER.at(Level.INFO).log("[DamageFloatBridge] DFF detected — styled damage numbers active.");
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.INFO).log("[DamageFloatBridge] DFF not loaded — vanilla numbers will be used.");
        } catch (NoSuchMethodException | LinkageError e) {
            LOGGER.at(Level.WARNING).log("[DamageFloatBridge] DFF API mismatch: " + e.getMessage());
        }
        DFF_PRESENT = found;
    }

    public static boolean isPresent() { return DFF_PRESENT; }

    public static void markKind(@Nonnull Damage damage, @Nonnull String kindId) {
        if (!DFF_PRESENT) return;
        try { methodMarkKind.invoke(null, damage, kindId); } catch (Exception ignored) {}
    }

    public static void markCritical(@Nonnull Damage damage) {
        if (!DFF_PRESENT) return;
        try { methodMarkCritical.invoke(null, damage); } catch (Exception ignored) {}
    }

    public static void clearCritical(@Nonnull Damage damage) {
        if (!DFF_PRESENT || methodClearCritical == null) return;
        try { methodClearCritical.invoke(null, damage); } catch (Exception ignored) {}
    }

    public static void resetForNewEvent(@Nonnull Damage damage) {
        if (!DFF_PRESENT) return;
        if (methodResetForNewEvent != null) {
            try { methodResetForNewEvent.invoke(null, damage); return; } catch (Exception ignored) {}
        }
        clearCritical(damage);
    }

    public static boolean isCritical(@Nonnull Damage damage) {
        if (!DFF_PRESENT) return false;
        try {
            Object result = methodIsCritical.invoke(null, damage);
            return result instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void markSkipCombatText(@Nonnull Damage damage) {
        if (!DFF_PRESENT) return;
        try { methodMarkSkip.invoke(null, damage); } catch (Exception ignored) {}
    }

    public static void emit(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> targetRef,
                            float amount, @Nonnull String kindId) {
        if (!DFF_PRESENT || amount <= 0f) return;
        try { methodEmit.invoke(null, store, targetRef, amount, kindId); } catch (Exception ignored) {}
    }

    public static void emit(@Nonnull Store<EntityStore> store,
                            @Nonnull CommandBuffer<EntityStore> commandBuffer,
                            @Nonnull Ref<EntityStore> targetRef, float amount, @Nonnull String kindId) {
        if (!DFF_PRESENT || amount <= 0f) return;
        try { methodEmitWithCB.invoke(null, store, commandBuffer, targetRef, amount, kindId); } catch (Exception ignored) {}
    }

    private DamageFloatBridge() {}
}
