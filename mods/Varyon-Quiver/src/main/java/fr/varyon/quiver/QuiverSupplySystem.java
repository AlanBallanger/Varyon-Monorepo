package fr.varyon.quiver;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.entity.entities.player.windows.WindowManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemStackItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public final class QuiverSupplySystem extends EntityTickingSystem<EntityStore> {
    private static final int MAGAZINE_SIZE = 10;
    private static final int REFILL_INTERVAL_TICKS = 10;
    private static final String[] QUIVER_IDS = new String[]{"Utility_Leather_Quiver", "Light_Leather_Quiver", "Medium_Leather_Quiver", "Heavy_Leather_Quiver"};

    private final Map<UUID, Integer> tickCounter = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = (Player) archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) {
            return;
        }
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID playerId = playerRef.getUuid();

        int count = this.tickCounter.merge(playerId, 1, Integer::sum);
        if (count < REFILL_INTERVAL_TICKS) {
            return;
        }
        this.tickCounter.put(playerId, 0);

        WindowManager windowManager = player.getWindowManager();
        if (windowManager != null) {
            for (Window window : windowManager.getWindows()) {
                if (window instanceof ItemContainerWindow) {
                    return;
                }
            }
        }
        CombinedItemContainer flatInventory = InventoryComponent.getCombined(commandBuffer, archetypeChunk, index, InventoryComponent.STORAGE_HOTBAR_BACKPACK);
        if (flatInventory == null) {
            return;
        }
        int flatArrows = this.countFlatArrows(flatInventory);
        int remaining = MAGAZINE_SIZE - flatArrows;
        if (remaining <= 0) {
            return;
        }
        CombinedItemContainer searchInventory = InventoryComponent.getCombined(commandBuffer, archetypeChunk, index, InventoryComponent.EVERYTHING);
        if (searchInventory == null) {
            return;
        }
        this.refill(searchInventory, flatInventory, remaining);
    }

    private void refill(CombinedItemContainer searchInventory, CombinedItemContainer flatInventory, int remaining) {
        outer:
        for (int ci = 0; ci < searchInventory.getContainersSize(); ++ci) {
            ItemContainer section = searchInventory.getContainer(ci);
            for (short slot = 0; slot < section.getCapacity(); slot++) {
                ItemStack item = section.getItemStack(slot);
                if (ItemStack.isEmpty(item) || item.getItem().getItemStackContainerConfig() == null || !this.isQuiverItem(item)) {
                    continue;
                }
                ItemStackItemContainer nested = ItemStackItemContainer.getContainer(section, slot);
                if (nested == null || nested.isEmpty()) {
                    continue;
                }
                for (short ns = 0; ns < nested.getCapacity(); ns++) {
                    if (remaining <= 0) {
                        break outer;
                    }
                    ItemStack arrow = nested.getItemStack(ns);
                    if (ItemStack.isEmpty(arrow)) {
                        continue;
                    }
                    int toTransfer = Math.min(arrow.getQuantity(), remaining);
                    ItemStack portion = arrow.withQuantity(toTransfer);
                    ItemStackTransaction remove = nested.removeItemStack(portion, false, false);
                    if (!remove.succeeded()) {
                        continue;
                    }
                    int qty = toTransfer - (remove.getRemainder() != null ? remove.getRemainder().getQuantity() : 0);
                    if (qty <= 0) {
                        continue;
                    }
                    ItemStackTransaction add = flatInventory.addItemStack(arrow.withQuantity(qty), false, false, true);
                    ItemStack overflow = add.getRemainder();
                    if (!ItemStack.isEmpty(overflow)) {
                        nested.addItemStack(overflow, false, false, true);
                        break outer;
                    }
                    remaining -= qty;
                }
            }
        }
    }

    private int countFlatArrows(CombinedItemContainer flatInventory) {
        int total = 0;
        for (int ci = 0; ci < flatInventory.getContainersSize(); ++ci) {
            ItemContainer section = flatInventory.getContainer(ci);
            for (short slot = 0; slot < section.getCapacity(); slot++) {
                ItemStack item = section.getItemStack(slot);
                if (ItemStack.isEmpty(item)) {
                    continue;
                }
                String id = item.getItemId();
                if (id == null || !id.contains("Arrow")) {
                    continue;
                }
                total += item.getQuantity();
            }
        }
        return total;
    }

    public void removePlayer(@Nonnull UUID playerId) {
        this.tickCounter.remove(playerId);
    }

    private boolean isQuiverItem(ItemStack item) {
        String id = item.getItemId();
        if (id == null) {
            return false;
        }
        for (String quiverId : QUIVER_IDS) {
            if (quiverId.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
