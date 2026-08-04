package com.varyon.craftrestrict.inventory;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PossessionInventoryListenerRegistry {

    private static final int[] SECTION_IDS = {
            InventoryComponent.HOTBAR_SECTION_ID,
            InventoryComponent.ARMOR_SECTION_ID,
            InventoryComponent.TOOLS_SECTION_ID,
            InventoryComponent.UTILITY_SECTION_ID,
            InventoryComponent.STORAGE_SECTION_ID,
            InventoryComponent.BACKPACK_SECTION_ID,
    };

    /**
     * PlayerRef#getHolder() returns null once the engine finishes joining the player to the store,
     * so the join-time Holder must be cached here to support the config-reload rescan.
     */
    private static final Map<UUID, Holder<EntityStore>> ONLINE_HOLDERS = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerRef> ONLINE_PLAYERS = new ConcurrentHashMap<>();

    private PossessionInventoryListenerRegistry() {
    }

    @SuppressWarnings("unchecked")
    public static void registerForPlayer(Holder<EntityStore> holder, PlayerRef playerRef) {
        ONLINE_HOLDERS.put(playerRef.getUuid(), holder);
        ONLINE_PLAYERS.put(playerRef.getUuid(), playerRef);
        for (int sectionId : SECTION_IDS) {
            ComponentType<EntityStore, ? extends InventoryComponent> componentType =
                    InventoryComponent.getComponentTypeById(sectionId);
            InventoryComponent inv = holder.getComponent((ComponentType<EntityStore, InventoryComponent>) componentType);
            if (inv == null) {
                continue;
            }
            inv.getInventory().registerChangeEvent(changeEvent ->
                    PossessionEnforcer.enforceForPlayer(holder, playerRef));
        }
    }

    public static void forgetPlayer(UUID uuid) {
        ONLINE_HOLDERS.remove(uuid);
        ONLINE_PLAYERS.remove(uuid);
    }

    public static void enforceForAllOnlinePlayers() {
        for (Map.Entry<UUID, Holder<EntityStore>> entry : ONLINE_HOLDERS.entrySet()) {
            PlayerRef playerRef = ONLINE_PLAYERS.get(entry.getKey());
            if (playerRef == null) {
                continue;
            }
            PossessionEnforcer.enforceForPlayer(entry.getValue(), playerRef);
        }
    }
}
