package com.varyon.craftrestrict.inventory;

import com.varyon.craftrestrict.utils.PermissionsUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PossessionEnforcer {

    private static final int[] SOURCE_SECTIONS = {
            InventoryComponent.HOTBAR_SECTION_ID,
            InventoryComponent.ARMOR_SECTION_ID,
            InventoryComponent.TOOLS_SECTION_ID,
            InventoryComponent.UTILITY_SECTION_ID,
            InventoryComponent.STORAGE_SECTION_ID,
            InventoryComponent.BACKPACK_SECTION_ID,
    };

    private static final int[] RELOCATION_TARGET_SECTIONS = {
            InventoryComponent.STORAGE_SECTION_ID,
            InventoryComponent.BACKPACK_SECTION_ID,
            InventoryComponent.UTILITY_SECTION_ID,
    };

    private PossessionEnforcer() {
    }

    public static void enforceForPlayer(Holder<EntityStore> holder, PlayerRef playerRef) {
        boolean anyHandled = false;
        for (int sectionId : SOURCE_SECTIONS) {
            InventoryComponent inv = getInventoryComponent(holder, sectionId);
            if (inv == null) {
                continue;
            }
            ItemContainer container = inv.getInventory();
            for (short slot = 0; slot < container.getCapacity(); slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String itemId = stack.getItemId();
                if (!PermissionsUtils.shouldRestrict(playerRef, itemId, "possession")) {
                    continue;
                }
                anyHandled = true;
                if ("DELETE".equalsIgnoreCase(PermissionsUtils.resolveRestrictionMode(playerRef, itemId))) {
                    container.removeItemStackFromSlot(slot);
                    continue;
                }
                if (!relocate(holder, container, slot)) {
                    container.removeItemStackFromSlot(slot);
                }
            }
        }
        if (anyHandled) {
            PermissionsUtils.sendRestrictionNotifications(playerRef, "possession");
        }
    }

    private static boolean relocate(Holder<EntityStore> holder, ItemContainer source, short slot) {
        for (int targetSectionId : RELOCATION_TARGET_SECTIONS) {
            InventoryComponent targetInv = getInventoryComponent(holder, targetSectionId);
            if (targetInv == null) {
                continue;
            }
            ItemContainer target = targetInv.getInventory();
            if (target == source) {
                continue;
            }
            if (source.moveItemStackFromSlot(slot, target).succeeded()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static InventoryComponent getInventoryComponent(Holder<EntityStore> holder, int sectionId) {
        ComponentType<EntityStore, ? extends InventoryComponent> componentType =
                InventoryComponent.getComponentTypeById(sectionId);
        return holder.getComponent((ComponentType<EntityStore, InventoryComponent>) componentType);
    }
}
