package com.varyon.pickuprange;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge to {@code ItemComponent}'s private {@code float pickupRange} cache field.
 *
 * <p>The server has no public setter for it: {@code getPickupRadius(accessor)} lazily resolves the
 * value from the item's {@code ItemEntityConfig} (or the world default) the first time it is called
 * and then caches it in this field; every later call just returns the cached float. To apply a
 * multiplier we resolve the vanilla value once and overwrite the cache once, per item, at spawn.
 *
 * <p>The {@link MethodHandle}s are bound once at class-load. If the field ever disappears (server
 * update) the handles stay {@code null} and every call here becomes a no-op, so the mod degrades to
 * "does nothing" instead of breaking item pickup.
 */
final class PickupRangeField {

    private static final Logger LOGGER = Logger.getLogger("Varyon-PickupRange");

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        MethodHandle getter = null;
        MethodHandle setter = null;
        try {
            Field field = ItemComponent.class.getDeclaredField("pickupRange");
            field.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            getter = lookup.unreflectGetter(field);
            setter = lookup.unreflectSetter(field);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                    "[Varyon-PickupRange] Could not bind ItemComponent.pickupRange; the pickup multiplier will be inert.", t);
        }
        GETTER = getter;
        SETTER = setter;
    }

    private PickupRangeField() {
    }

    /** @return true if the reflective bridge is usable. */
    static boolean available() {
        return GETTER != null && SETTER != null;
    }

    /**
     * Resolve the vanilla pickup radius for {@code item} (forcing the lazy computation if needed) and
     * overwrite its cached value with {@code vanilla * multiplier}. Idempotent-safe: callers guard so
     * this runs at most once per item.
     *
     * @return the new radius that was written, or a negative value if the bridge is unavailable / failed.
     */
    static float applyMultiplier(ItemComponent item, ComponentAccessor<EntityStore> accessor, float multiplier) {
        if (!available() || item == null) {
            return -1.0f;
        }
        try {
            // Force the lazy resolve so the field holds the real vanilla radius, then read it back.
            SETTER.invoke(item, -1.0f);
            float vanilla = item.getPickupRadius(accessor);
            if (!Float.isFinite(vanilla) || vanilla <= 0.0f) {
                return -1.0f;
            }
            float boosted = vanilla * multiplier;
            SETTER.invoke(item, boosted);
            return boosted;
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "[Varyon-PickupRange] applyMultiplier failed", t);
            return -1.0f;
        }
    }

    /** Read the current cached value (may be {@code -1} if not yet resolved). */
    static float readRaw(ItemComponent item) {
        if (GETTER == null || item == null) {
            return Float.NaN;
        }
        try {
            return (float) GETTER.invoke(item);
        } catch (Throwable t) {
            return Float.NaN;
        }
    }
}
