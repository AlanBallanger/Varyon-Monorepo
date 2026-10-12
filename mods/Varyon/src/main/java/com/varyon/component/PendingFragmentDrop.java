package com.varyon.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Carries the key-fragment drop a dead NPC still owes as a physical world item.
 *
 * {@code MobFragmentDropSystem.DropOnDeath} resolves the contributing player, zone and amount at
 * the moment of death (that is when the last-attacker tracking is valid), but does not spawn the
 * item then. It attaches this component instead, and {@code DeferredFragmentDropSystem} spawns the
 * fragments once the corpse is ready to disappear — matching vanilla loot timing.
 */
public class PendingFragmentDrop implements Component<EntityStore> {

    private static ComponentType<EntityStore, PendingFragmentDrop> COMPONENT_TYPE;

    private String itemId;
    private int quantity;

    public PendingFragmentDrop() {
    }

    public PendingFragmentDrop(@Nonnull String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    @Nullable
    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new PendingFragmentDrop(itemId, quantity);
    }

    @Nonnull
    public static ComponentType<EntityStore, PendingFragmentDrop> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, PendingFragmentDrop> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
