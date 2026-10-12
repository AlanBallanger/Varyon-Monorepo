package fr.varyon.vrpg.integration;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Reflective bridge to Varyon's mob scaling subsystem ({@code com.varyon.component.MobScalingComponent}).
 * Lets {@code ClassKillXpSystem} read the victim's level (1-100, assigned at spawn from the difficulty
 * zone) without a compile-time dependency on the Varyon mod.
 */
public final class VaryonMobScalingBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.get("VaryonRPG");
    private static final boolean PRESENT;

    private static Method methodGetComponentType;
    private static Method methodGetMobLevel;

    static {
        boolean found = false;
        try {
            Class<?> componentClass = Class.forName("com.varyon.component.MobScalingComponent");
            methodGetComponentType = componentClass.getMethod("getComponentType");
            methodGetMobLevel = componentClass.getMethod("getMobLevel");
            found = true;
            LOGGER.at(Level.INFO).log("[VaryonMobScalingBridge] mob scaling detected — mob-level XP scaling active.");
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.INFO).log("[VaryonMobScalingBridge] mob scaling not present — mob-level XP scaling disabled.");
        } catch (NoSuchMethodException | LinkageError e) {
            LOGGER.at(Level.WARNING).log("[VaryonMobScalingBridge] API mismatch: " + e.getMessage());
        }
        PRESENT = found;
    }

    private VaryonMobScalingBridge() {}

    public static boolean isPresent() {
        return PRESENT;
    }

    /** @return the victim's mob level (1-100), or 0 if not scaled / unavailable. */
    @SuppressWarnings("unchecked")
    public static int getMobLevel(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef) {
        if (!PRESENT) {
            return 0;
        }
        try {
            Object componentType = methodGetComponentType.invoke(null);
            if (componentType == null) {
                return 0;
            }
            Object component = store.getComponent(victimRef,
                (com.hypixel.hytale.component.ComponentType<EntityStore, ?>) componentType);
            if (component == null) {
                return 0;
            }
            Object level = methodGetMobLevel.invoke(component);
            return level instanceof Integer i ? Math.max(0, i) : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
