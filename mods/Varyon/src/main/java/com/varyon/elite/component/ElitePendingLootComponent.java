package com.varyon.elite.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Currency an elite still owes as world items. Resolved at death by {@code EliteLootSystem};
 * spawned by {@code EliteDeferredLootSystem} once the corpse is ready to vanish, matching
 * vanilla loot timing (same pattern as {@code PendingFragmentDrop}).
 */
public class ElitePendingLootComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, ElitePendingLootComponent> COMPONENT_TYPE;

    private int voidCrystals;
    private int corruptionCores;

    public ElitePendingLootComponent() {
    }

    public ElitePendingLootComponent(int voidCrystals, int corruptionCores) {
        this.voidCrystals = voidCrystals;
        this.corruptionCores = corruptionCores;
    }

    public int getVoidCrystals() {
        return voidCrystals;
    }

    public int getCorruptionCores() {
        return corruptionCores;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new ElitePendingLootComponent(voidCrystals, corruptionCores);
    }

    @Nonnull
    public static ComponentType<EntityStore, ElitePendingLootComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, ElitePendingLootComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
