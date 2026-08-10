package com.varyon.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Marker component: an entity carrying this must not drop currency, key fragments, or any other
 * reward-system loot on death. Reward systems (coins, key fragments, ...) should check for its
 * presence and skip the entity entirely. Posed by spawners (e.g. BossArena) on entities that
 * already have their own dedicated loot handling.
 */
public class NoLootComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, NoLootComponent> COMPONENT_TYPE;

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new NoLootComponent();
    }

    @Nonnull
    public static ComponentType<EntityStore, NoLootComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, NoLootComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
