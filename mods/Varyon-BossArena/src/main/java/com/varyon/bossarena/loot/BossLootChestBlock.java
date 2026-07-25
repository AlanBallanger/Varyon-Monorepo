package com.varyon.bossarena.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class BossLootChestBlock implements Component<ChunkStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static volatile ComponentType<ChunkStore, BossLootChestBlock> componentType;

    private static final ThreadLocal<UUID> LAST_OPEN_UUID = new ThreadLocal<>();

    public static final BuilderCodec<BossLootChestBlock> CODEC = BuilderCodec.builder(
                    BossLootChestBlock.class,
                    BossLootChestBlock::new)
            .append(new KeyedCodec<>("LootOx", Codec.DOUBLE),
                    (BossLootChestBlock b, Double v) -> b.lootOx = v,
                    b -> b.lootOx).add()
            .append(new KeyedCodec<>("LootOy", Codec.DOUBLE),
                    (BossLootChestBlock b, Double v) -> b.lootOy = v,
                    b -> b.lootOy).add()
            .append(new KeyedCodec<>("LootOz", Codec.DOUBLE),
                    (BossLootChestBlock b, Double v) -> b.lootOz = v,
                    b -> b.lootOz).add()
            .build();

    private final transient Map<UUID, ContainerBlockWindow> windows = new ConcurrentHashMap<>();
    private final transient Map<UUID, ItemContainer> playerContainers = new ConcurrentHashMap<>();
    private boolean allowViewing = true;
    private double lootOx;
    private double lootOy;
    private double lootOz;

    public static ComponentType<ChunkStore, BossLootChestBlock> getComponentType() {
        ComponentType<ChunkStore, BossLootChestBlock> t = componentType;
        if (t == null) {
            throw new IllegalStateException("BossLootChestBlock component type not registered");
        }
        return t;
    }

    public static void setComponentType(ComponentType<ChunkStore, BossLootChestBlock> type) {
        componentType = type;
    }

    public static BossLootChestBlock getAt(World world, int x, int y, int z) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(x, y, z);
        if (holder == null) {
            return null;
        }
        return holder.getComponent(getComponentType());
    }

    public static BossLootChestBlock getAt(World world, Vector3i pos) {
        return getAt(world, pos.x, pos.y, pos.z);
    }

    private BossLootChestBlock() {
    }

    public BossLootChestBlock(Vector3d lootOrigin) {
        if (lootOrigin != null) {
            this.lootOx = lootOrigin.x;
            this.lootOy = lootOrigin.y;
            this.lootOz = lootOrigin.z;
        }
    }

    private Vector3d lootLookupLocation() {
        return new Vector3d(lootOx, lootOy, lootOz);
    }

    public ItemContainer getItemContainer(Player playerComponent, UUID playerUuid) {
        return getOrCreateContainer(null, playerUuid);
    }

    public ItemContainer getItemContainer(World world, Player playerComponent, UUID playerUuid) {
        return getOrCreateContainer(world, playerUuid);
    }

    public ItemContainer getItemContainer() {
        UUID playerUuid = LAST_OPEN_UUID.get();
        if (playerUuid == null) {
            return new SimpleItemContainer((short) 27);
        }
        LAST_OPEN_UUID.remove();
        return getOrCreateContainer(null, playerUuid);
    }

    public boolean canOpen(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        UUIDComponent uuidComponent = (UUIDComponent) accessor.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            LAST_OPEN_UUID.set(uuidComponent.getUuid());
        } else {
            LAST_OPEN_UUID.remove();
        }
        return true;
    }

    public void onOpen(Ref<EntityStore> ref, World world, com.hypixel.hytale.component.Store<EntityStore> store) {
    }

    public boolean isAllowViewing() {
        return allowViewing;
    }

    public Map<UUID, ContainerBlockWindow> getWindows() {
        return windows;
    }

    private ItemContainer getOrCreateContainer(World world, UUID playerUuid) {
        ItemContainer cached = playerContainers.get(playerUuid);
        if (cached != null) {
            return cached;
        }

        ItemContainer container = new SimpleItemContainer((short) 27);

        Vector3d lookupLocation = lootLookupLocation();
        List<GeneratedLoot> loot = BossLootHandler.claimLoot(
                world,
                lookupLocation,
                playerUuid
        );

        if (loot != null && !loot.isEmpty()) {
            int slot = 0;
            for (GeneratedLoot item : loot) {
                if (slot >= 27) {
                    break;
                }
                try {
                    ItemStack stack = new ItemStack(item.itemId, item.amount);
                    container.setItemStackForSlot((short) slot, stack);
                    slot++;
                } catch (Exception e) {
                    LOGGER.warning("Failed to create ItemStack for " + item.itemId + ": " + e.getMessage());
                }
            }
        }

        playerContainers.put(playerUuid, container);
        return container;
    }

    @Override
    public Component<ChunkStore> clone() {
        BossLootChestBlock c = new BossLootChestBlock(lootLookupLocation());
        c.allowViewing = this.allowViewing;
        return c;
    }
}
