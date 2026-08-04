package com.varyon.craftrestrict.inventory;

import com.varyon.craftrestrict.utils.PermissionsUtils;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ContainerPurgeUtil {

    private ContainerPurgeUtil() {
    }

    public static void purgeDeletedItems(World world, int x, int y, int z) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(x, y, z);
        if (holder == null) {
            return;
        }
        ItemContainerBlock block = holder.getComponent(ItemContainerBlock.getComponentType());
        if (block == null) {
            return;
        }
        SimpleItemContainer container = block.getItemContainer();
        if (container == null) {
            return;
        }
        String worldName = world.getName();
        for (short slot = 0; slot < container.getCapacity(); slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if ("DELETE".equalsIgnoreCase(PermissionsUtils.resolvePossessionModeById(stack.getItemId(), worldName))) {
                container.removeItemStackFromSlot(slot);
            }
        }
    }
}
