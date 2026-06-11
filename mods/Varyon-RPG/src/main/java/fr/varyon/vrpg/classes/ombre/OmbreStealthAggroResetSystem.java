package fr.varyon.vrpg.classes.ombre;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public final class OmbreStealthAggroResetSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

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
            for (java.lang.reflect.Field f : roleClass.getDeclaredFields()) {
                if (supportClass.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    markedEntitySupportField = f;
                    break;
                }
            }
            if (markedEntitySupportField == null) {
                for (java.lang.reflect.Field f : roleClass.getSuperclass().getDeclaredFields()) {
                    if (supportClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        markedEntitySupportField = f;
                        break;
                    }
                }
            }
            LOG.atInfo().log("[StealthAggro] reflection OK, markedEntitySupportField=" + (markedEntitySupportField != null ? markedEntitySupportField.getName() : "NOT FOUND"));
        } catch (Exception e) {
            LOG.atWarning().log("[StealthAggro] reflection FAILED: " + e.getMessage());
        }
    }

    private OmbreStealthAggroResetSystem() {}

    public static void resetAggroAround(@Nonnull Ref<EntityStore> playerRef,
                                        @Nonnull Store<EntityStore> store) {
        if (entityTargetsField == null || defaultTargetSlotField == null || markedEntitySupportField == null) return;
        try {
            TransformComponent tc = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (tc == null) return;
            final long playerIdx = playerRef.getIndex();
            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                .selectNearbyEntities(store, tc.getPosition(), 30.0, npcRef -> {
                    try {
                        if (npcRef.getIndex() == playerIdx) return;
                        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
                        if (npc == null) return;
                        com.hypixel.hytale.server.npc.role.Role role = npc.getRole();
                        if (role == null) return;
                        Object support = markedEntitySupportField.get(role);
                        if (support == null) return;
                        int slot = defaultTargetSlotField.getInt(support);
                        if (slot < 0) return;
                        Ref<EntityStore>[] targets = (Ref<EntityStore>[]) entityTargetsField.get(support);
                        if (targets == null || slot >= targets.length) return;
                        Ref<EntityStore> target = targets[slot];
                        if (target == null || !target.isValid()) return;
                        if (target.getIndex() != playerIdx) return;
                        targets[slot] = null;
                    } catch (Exception ignored) {}
                }, ref -> ref.getIndex() != playerIdx);
        } catch (Exception ignored) {}
    }

    public static void forceTarget(@Nonnull com.hypixel.hytale.server.npc.role.Role role,
                                   @Nonnull Ref<EntityStore> targetRef) {
        if (entityTargetsField == null || defaultTargetSlotField == null || markedEntitySupportField == null) return;
        try {
            Object support = markedEntitySupportField.get(role);
            if (support == null) return;
            int slot = defaultTargetSlotField.getInt(support);
            if (slot < 0) return;
            Ref<EntityStore>[] targets = (Ref<EntityStore>[]) entityTargetsField.get(support);
            if (targets == null || slot >= targets.length) return;
            targets[slot] = targetRef;
        } catch (Exception ignored) {}
    }
}
