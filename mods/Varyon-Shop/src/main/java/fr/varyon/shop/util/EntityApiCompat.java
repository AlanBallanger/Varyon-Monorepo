package fr.varyon.shop.util;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class EntityApiCompat {
    private EntityApiCompat() {
    }

    public static UUID getUuid(Entity entity) {
        if (entity == null) {
            return null;
        }
        try {
            UUID directUuid = entity.getUuid();
            if (directUuid != null) {
                return directUuid;
            }
        } catch (IllegalStateException ignored) {
        }
        Ref<EntityStore> reference = entity.getReference();
        if (reference != null) {
            Store<EntityStore> store = reference.getStore();
            if (store != null) {
                try {
                    UUIDComponent uuidComponent = (UUIDComponent) store.getComponent(reference, UUIDComponent.getComponentType());
                    if (uuidComponent != null && uuidComponent.getUuid() != null) {
                        return uuidComponent.getUuid();
                    }
                } catch (IllegalStateException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static PlayerRef getPlayerRef(Player player) {
        if (player == null) {
            return null;
        }
        try {
            PlayerRef directRef = player.getPlayerRef();
            if (directRef != null) {
                return directRef;
            }
        } catch (IllegalStateException ignored) {
        }
        Ref<EntityStore> reference = player.getReference();
        if (reference != null) {
            Store<EntityStore> store = reference.getStore();
            if (store != null) {
                try {
                    return (PlayerRef) store.getComponent(reference, PlayerRef.getComponentType());
                } catch (IllegalStateException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static Player getPlayer(PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        Ref<EntityStore> reference = playerRef.getReference();
        if (reference == null) {
            return null;
        }
        Store<EntityStore> store = reference.getStore();
        if (store == null) {
            return null;
        }
        try {
            return (Player) store.getComponent(reference, Player.getComponentType());
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public static ItemContainer getStorageContainer(Player player) {
        InventoryComponent.Storage storage = getComponentOf(player, InventoryComponent.Storage.getComponentType());
        return storage == null ? null : storage.getInventory();
    }

    public static ItemContainer getHotbarContainer(Player player) {
        InventoryComponent.Hotbar hotbar = getComponentOf(player, InventoryComponent.Hotbar.getComponentType());
        return hotbar == null ? null : hotbar.getInventory();
    }

    public static ItemContainer getArmorContainer(Player player) {
        InventoryComponent.Armor armor = getComponentOf(player, InventoryComponent.Armor.getComponentType());
        return armor == null ? null : armor.getInventory();
    }

    public static ItemContainer getUtilityContainer(Player player) {
        InventoryComponent.Utility utility = getComponentOf(player, InventoryComponent.Utility.getComponentType());
        return utility == null ? null : utility.getInventory();
    }

    public static ItemContainer getToolsContainer(Player player) {
        InventoryComponent.Tool tool = getComponentOf(player, InventoryComponent.Tool.getComponentType());
        return tool == null ? null : tool.getInventory();
    }

    /** The item currently held in the player's active hotbar slot, or null if empty/unavailable. */
    public static ItemStack getHeldItem(Player player) {
        InventoryComponent.Hotbar hotbar = getComponentOf(player, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            return null;
        }
        try {
            ItemStack stack = hotbar.getActiveItem();
            return (stack == null || stack.isEmpty()) ? null : stack;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static <T extends Component<EntityStore>> T getComponentOf(Player player, ComponentType<EntityStore, T> type) {
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

    public static boolean hasPermission(Player player, String permission) {
        PlayerRef ref = getPlayerRef(player);
        return ref != null && ref.hasPermission(permission);
    }

    public static String getDisplayName(Player player) {
        PlayerRef ref = getPlayerRef(player);
        return ref == null ? null : ref.getUsername();
    }

    public static PlayerRef senderAsPlayerRef(CommandContext context) {
        if (context == null || !context.isPlayer()) {
            return null;
        }
        try {
            CommandSender sender = context.sender();
            if (sender instanceof PlayerRef playerRef) {
                return playerRef;
            }
        } catch (Throwable ignored) {
        }
        try {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                if (store != null) {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        return playerRef;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
