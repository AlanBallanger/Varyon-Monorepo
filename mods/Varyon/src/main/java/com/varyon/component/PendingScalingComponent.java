package com.varyon.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Marker dropped on a freshly-spawned NPC by {@code MobScalingRefSystem} instead of resolving its
 * zone and applying scaling immediately. A burst of chunk loads (server boot, a player crossing
 * into an unexplored area) can add thousands of entities within the same tick, and doing the zone
 * lookup + stat modifier synchronously for all of them at once was the CPU/allocation spike seen
 * at world load. {@code MobScalingQueueSystem} drains a bounded number of these markers per tick
 * instead, spreading the work over several frames. Not persisted: a mob that reloads mid-queue
 * just gets re-marked by the normal spawn path on the next boot.
 */
public final class PendingScalingComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PendingScalingComponent> COMPONENT_TYPE;

    public static final PendingScalingComponent INSTANCE = new PendingScalingComponent();

    private PendingScalingComponent() {}

    @Nonnull
    public static ComponentType<EntityStore, PendingScalingComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, PendingScalingComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }

    @Override
    public Component<EntityStore> clone() {
        return this;
    }
}
