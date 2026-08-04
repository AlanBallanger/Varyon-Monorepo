package com.varyon.craftrestrict.events;

import com.varyon.craftrestrict.utils.PermissionsUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryActiveSlotRequestEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EventInventoryActiveSlotRequest extends EntityEventSystem<EntityStore, InventoryActiveSlotRequestEvent> {

    public EventInventoryActiveSlotRequest() {
        super(InventoryActiveSlotRequestEvent.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                        @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryActiveSlotRequestEvent event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        ComponentType<EntityStore, ? extends InventoryComponent> componentType =
                InventoryComponent.getComponentTypeById(event.getInventorySectionId());
        InventoryComponent inv = store.getComponent(ref, (ComponentType<EntityStore, InventoryComponent>) componentType);
        if (inv == null) {
            return;
        }
        ItemStack stack = inv.getInventory().getItemStack((short) Byte.toUnsignedInt(event.getNewSlot()));
        if (stack == null || stack.isEmpty()) {
            return;
        }
        boolean restricted = PermissionsUtils.shouldRestrict(playerRef, stack.getItemId(), "possession");
        if (restricted) {
            PermissionsUtils.sendRestrictionNotifications(playerRef, "possession");
        }
        event.setCancelled(restricted);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
