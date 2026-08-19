package fr.varyon.stacktiers.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Petit miroir de fr.varyon.shop.util.EntityApiCompat (deux méthodes seulement) pour éviter
 * une dépendance de module sur Varyon-Shop juste pour accéder aux conteneurs d'inventaire.
 */
public final class InventoryCompat {

    public static ItemContainer getStorageContainer(Player player) {
        InventoryComponent.Storage storage = getComponentOf(player, InventoryComponent.Storage.getComponentType());
        return storage == null ? null : storage.getInventory();
    }

    public static ItemContainer getHotbarContainer(Player player) {
        InventoryComponent.Hotbar hotbar = getComponentOf(player, InventoryComponent.Hotbar.getComponentType());
        return hotbar == null ? null : hotbar.getInventory();
    }

    private static <T extends com.hypixel.hytale.component.Component<EntityStore>> T getComponentOf(
            Player player, com.hypixel.hytale.component.ComponentType<EntityStore, T> type) {
        if (player == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }
        try {
            return store.getComponent(ref, type);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private InventoryCompat() {
    }
}
