package com.varyon.signaturepreservation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.signaturepreservation.config.VaryonSignaturePreservationConfig;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.jspecify.annotations.Nullable;

public final class SignatureEnergyPreservationSystem extends EntityTickingSystem<EntityStore> {

    public static final String META_KEY_SIGNATURE_ENERGY = "SP_SavedSignatureEnergy";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final VaryonSignaturePreservationConfig config;
    private final Map<UUID, Byte> lastActiveSlot = new ConcurrentHashMap<>();
    private final Map<UUID, Float> previousTickEnergy = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> restoreEpoch = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SignatureEnergyPreservationSystem(VaryonSignaturePreservationConfig config) {
        this.config = config;
    }

    void shutdownScheduler() {
        scheduler.shutdown();
    }

    private void debug(String message) {
        if (config.isDebug()) {
            LOGGER.at(Level.INFO).log(message);
        }
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void tick(
            float dt,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer) {
        if (!config.isEnabled()) {
            return;
        }
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        if (!entityRef.isValid()) {
            return;
        }
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        UUID playerUuid = uuidComponent.getUuid();
        byte currentSlot = inventory.getActiveHotbarSlot();
        float currentEnergy = getSignatureEnergy(entityRef, store);
        Byte lastSlotObj = lastActiveSlot.get(playerUuid);
        if (lastSlotObj == null) {
            lastActiveSlot.put(playerUuid, currentSlot);
            previousTickEnergy.put(playerUuid, currentEnergy);
            debug(String.format("Player first tick - initial slot: %d, energy: %.1f", currentSlot, currentEnergy));
            return;
        }
        byte previousSlot = lastSlotObj;
        if (currentSlot == previousSlot) {
            previousTickEnergy.put(playerUuid, currentEnergy);
            return;
        }
        Float energyBeforeReset = previousTickEnergy.get(playerUuid);
        float savedEnergy = energyBeforeReset != null ? energyBeforeReset : 0.0f;
        lastActiveSlot.put(playerUuid, currentSlot);
        previousTickEnergy.put(playerUuid, currentEnergy);
        debug(String.format(
                "Hotbar slot change detected: %d -> %d (energy before reset: %.1f)",
                previousSlot, currentSlot, savedEnergy));
        int epoch = nextRestoreEpochFor(playerUuid);
        handleSlotChange(entityRef, store, inventory, playerUuid, previousSlot, currentSlot, savedEnergy, epoch);
    }

    private static boolean isHotbarSlotIndex(ItemContainer hotbar, int slot) {
        if (slot < 0) {
            return false;
        }
        int capacity = Short.toUnsignedInt(hotbar.getCapacity());
        return slot < capacity;
    }

    private int nextRestoreEpochFor(UUID playerUuid) {
        return restoreEpoch.computeIfAbsent(playerUuid, u -> new AtomicInteger()).incrementAndGet();
    }

    private void handleSlotChange(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            Inventory inventory,
            UUID playerUuid,
            byte previousSlot,
            byte currentSlot,
            float energyBeforeReset,
            int restoreEpochAtSchedule) {
        ItemContainer hotbar = inventory.getHotbar();
        boolean prevInRange = isHotbarSlotIndex(hotbar, previousSlot);
        boolean currInRange = isHotbarSlotIndex(hotbar, currentSlot);
        ItemStack oldItem = prevInRange ? hotbar.getItemStack((short) previousSlot) : null;
        ItemStack newItem = currInRange ? hotbar.getItemStack((short) currentSlot) : null;
        debug(String.format(
                "Hotbar swap: slot %d -> %d | Energy before reset: %.1f",
                previousSlot, currentSlot, energyBeforeReset));
        debug(String.format(
                "Old item: %s | New item: %s",
                oldItem != null ? oldItem.getItem().getId() : "null",
                newItem != null ? newItem.getItem().getId() : "null"));
        boolean oldIsWeapon = oldItem != null && !oldItem.isEmpty() && isWeapon(oldItem);
        debug(String.format("Old item is weapon: %b", oldIsWeapon));
        if (oldIsWeapon) {
            if (energyBeforeReset > 0.0f && prevInRange) {
                ItemStack updatedOldItem = saveSignatureEnergy(oldItem, energyBeforeReset);
                hotbar.setItemStackForSlot((short) previousSlot, updatedOldItem);
                debug(String.format(
                        "SAVED SignatureEnergy %.1f to old weapon in slot %d", energyBeforeReset, previousSlot));
            } else {
                debug("Skipping save - energy before reset is 0");
            }
        }
        boolean newIsWeapon = newItem != null && !newItem.isEmpty() && isWeapon(newItem);
        debug(String.format("New item is weapon: %b", newIsWeapon));
        if (newIsWeapon) {
            Float savedEnergy = getSavedSignatureEnergy(newItem);
            debug(String.format("Saved energy in new weapon metadata: %s", savedEnergy));
            if (savedEnergy != null && savedEnergy > 0.0f && currInRange) {
                ItemStack updatedNewItem = clearSavedSignatureEnergy(newItem);
                hotbar.setItemStackForSlot((short) currentSlot, updatedNewItem);
                debug("Cleared saved energy from new weapon metadata");
                World world = store.getExternalData().getWorld();
                float energyToRestore = savedEnergy;
                long delayMs = config.getRestoreDelayMs();
                debug(String.format("Scheduling restore of %.1f energy in %dms", energyToRestore, delayMs));
                scheduler.schedule(
                        () -> {
                            debug(String.format("Scheduler fired - about to restore %.1f", energyToRestore));
                            world.execute(() -> {
                                AtomicInteger epochCounter = restoreEpoch.get(playerUuid);
                                if (epochCounter == null
                                        || epochCounter.get() != restoreEpochAtSchedule) {
                                    debug("Skipping restore — stale slot-change generation");
                                    return;
                                }
                                if (!entityRef.isValid()) {
                                    debug("Skipping restore — entity ref invalid");
                                    return;
                                }
                                Player p = store.getComponent(entityRef, Player.getComponentType());
                                if (p == null || p.getInventory() == null) {
                                    debug("Skipping restore — player or inventory missing");
                                    return;
                                }
                                if (p.getInventory().getActiveHotbarSlot() != currentSlot) {
                                    debug("Skipping restore — active hotbar slot changed");
                                    return;
                                }
                                debug(String.format("world.execute() running - restoring %.1f", energyToRestore));
                                setSignatureEnergy(entityRef, store, energyToRestore);
                                debug(String.format(
                                        "RESTORED SignatureEnergy %.1f for weapon in slot %d (after %dms delay)",
                                        energyToRestore, currentSlot, delayMs));
                            });
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS);
            } else {
                debug("No saved energy to restore (null or 0)");
            }
        } else {
            debug("New item is not a weapon - no restore needed");
        }
    }

    private boolean isWeapon(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        if (item.getItem().getWeapon() != null) {
            return true;
        }
        String id = item.getItemId();
        return id != null && id.startsWith("Weapon_");
    }

    public void cleanupPlayer(UUID playerUuid) {
        lastActiveSlot.remove(playerUuid);
        previousTickEnergy.remove(playerUuid);
        restoreEpoch.remove(playerUuid);
    }

    private float getSignatureEnergy(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        int signatureEnergyIndex = EntityStatType.getAssetMap().getIndex("SignatureEnergy");
        if (signatureEnergyIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return 0.0f;
        }
        EntityStatValue statValue = statMap.get(signatureEnergyIndex);
        return statValue != null ? statValue.get() : 0.0f;
    }

    private void setSignatureEnergy(Ref<EntityStore> entityRef, Store<EntityStore> store, float value) {
        int signatureEnergyIndex = EntityStatType.getAssetMap().getIndex("SignatureEnergy");
        if (signatureEnergyIndex == Integer.MIN_VALUE) {
            debug("setSignatureEnergy FAILED: SignatureEnergy stat not found!");
            return;
        }
        if (!entityRef.isValid()) {
            debug("setSignatureEnergy FAILED: entityRef is no longer valid!");
            return;
        }
        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            debug("setSignatureEnergy FAILED: statMap is null!");
            return;
        }
        statMap.setStatValue(signatureEnergyIndex, value);
        if (config.isDebug()) {
            EntityStatValue verify = statMap.get(signatureEnergyIndex);
            float verifyValue = verify != null ? verify.get() : -1.0f;
            debug(String.format("setSignatureEnergy: set %.1f, verify read back: %.1f", value, verifyValue));
        }
    }

    private ItemStack saveSignatureEnergy(ItemStack item, float energy) {
        return item.withMetadata(META_KEY_SIGNATURE_ENERGY, Codec.FLOAT, energy);
    }

    private @Nullable Float getSavedSignatureEnergy(ItemStack item) {
        return item.getFromMetadataOrNull(META_KEY_SIGNATURE_ENERGY, Codec.FLOAT);
    }

    private ItemStack clearSavedSignatureEnergy(ItemStack item) {
        return item.withMetadata(META_KEY_SIGNATURE_ENERGY, Codec.FLOAT, 0.0f);
    }
}
