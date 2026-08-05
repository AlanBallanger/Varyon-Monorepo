package com.varyon.bossarena.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * Writes directly into an NPC role's default target slot to force it onto a player.
 *
 * <p>Mirrors the mechanism used by the Rempart "Provocation" skill in Varyon-RPG: the engine
 * exposes no aggro API, so the target array inside {@code MarkedEntitySupport} is reached by
 * reflection. Duplicated here rather than shared so BossArena stays independent of Varyon-RPG.
 */
public final class MobAggroForcer {
    private static final Logger LOGGER = Logger.getLogger("BossArena");

    private static Field entityTargetsField;
    private static Field defaultTargetSlotField;
    private static Field markedEntitySupportField;

    static {
        try {
            Class<?> supportClass = Class.forName("com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport");
            entityTargetsField = supportClass.getDeclaredField("entityTargets");
            entityTargetsField.setAccessible(true);
            defaultTargetSlotField = supportClass.getDeclaredField("defaultTargetSlot");
            defaultTargetSlotField.setAccessible(true);

            Class<?> roleClass = Class.forName("com.hypixel.hytale.server.npc.role.Role");
            markedEntitySupportField = findSupportField(roleClass, supportClass);
            if (markedEntitySupportField == null && roleClass.getSuperclass() != null) {
                markedEntitySupportField = findSupportField(roleClass.getSuperclass(), supportClass);
            }

            if (markedEntitySupportField == null) {
                LOGGER.warning("[MobAggroForcer] MarkedEntitySupport field not found on Role; forced aggro disabled");
            }
        } catch (Exception e) {
            LOGGER.warning("[MobAggroForcer] reflection setup failed, forced aggro disabled: " + e.getMessage());
        }
    }

    private MobAggroForcer() {}

    @Nullable
    private static Field findSupportField(Class<?> owner, Class<?> supportClass) {
        for (Field f : owner.getDeclaredFields()) {
            if (supportClass.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    /** True when reflection resolved and forcing is possible. */
    public static boolean isAvailable() {
        return entityTargetsField != null && defaultTargetSlotField != null && markedEntitySupportField != null;
    }

    /**
     * Points {@code role} at {@code targetRef} unless it already holds a valid target.
     *
     * <p>Existing targets are preserved so this never steals aggro from a Rempart taunt or from a
     * mob already engaged on a player; it only pulls idle mobs into the fight.
     *
     * @return true if the target slot was written
     */
    public static boolean forceTargetIfIdle(@Nonnull Object role, @Nonnull Ref<EntityStore> targetRef) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Object support = markedEntitySupportField.get(role);
            if (support == null) {
                return false;
            }
            int slot = defaultTargetSlotField.getInt(support);
            if (slot < 0) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Ref<EntityStore>[] targets = (Ref<EntityStore>[]) entityTargetsField.get(support);
            if (targets == null || slot >= targets.length) {
                return false;
            }

            Ref<EntityStore> current = targets[slot];
            if (current != null && current.isValid()) {
                return false;
            }

            targets[slot] = targetRef;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
