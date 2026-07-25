package com.varyon.bossarena.loot;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Shared open logic for boss loot chests (interaction + UseBlock fallback).
 */
public final class BossLootChestOpener {

    private static final Logger LOGGER = Logger.getLogger("BossArena");

    private BossLootChestOpener() {}

    public static boolean open(
            World world,
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            int x,
            int y,
            int z) {
        if (world == null || playerRef == null || store == null) {
            return false;
        }

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null && commandBuffer != null) {
            player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        }
        if (player == null) {
            LOGGER.warning("Boss chest open: no Player component");
            return false;
        }

        BossLootChestBlock chestState = BossLootHandler.ensureBossLootChestBlock(world, x, y, z);
        if (chestState == null) {
            LOGGER.warning("Boss chest open: no BossLootChestBlock at " + x + "," + y + "," + z);
            return false;
        }

        if (!chestState.isAllowViewing() || !chestState.canOpen(playerRef, store)) {
            LOGGER.info("Boss chest open denied at " + x + "," + y + "," + z);
            return false;
        }

        UUIDComponent uuidComponent = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComponent == null && commandBuffer != null) {
            uuidComponent = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
        }
        if (uuidComponent == null) {
            LOGGER.warning("Boss chest open: no UUIDComponent");
            return false;
        }

        UUID playerUuid = uuidComponent.getUuid();
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            LOGGER.warning("Boss chest open: chunk not loaded");
            return false;
        }

        BlockType blockType = world.getBlockType(x, y, z);
        Vector3i pos = new Vector3i(x, y, z);
        Vector3d chestPosition = new Vector3d(x, y, z);

        ContainerBlockWindow window = new ContainerBlockWindow(
                x, y, z,
                chunk.getRotationIndex(x, y, z),
                blockType,
                chestState.getItemContainer(world, player, playerUuid)
        );

        Map<UUID, ContainerBlockWindow> windows = chestState.getWindows();
        if (windows.putIfAbsent(playerUuid, window) != null) {
            return true;
        }

        BossLootHandler.pauseChestExpiry(world, chestPosition);
        if (!player.getPageManager().setPageWithWindows(playerRef, store, Page.Bench, true, new Window[]{window})) {
            windows.remove(playerUuid, window);
            if (windows.isEmpty()) {
                BossLootHandler.scheduleChestExpiry(world, chestPosition);
            }
            LOGGER.warning("Boss chest open: setPageWithWindows failed");
            return false;
        }

        window.registerCloseEvent((event) -> {
            windows.remove(playerUuid, window);
            BlockType currentBlockType = world.getBlockType(pos);
            BossLootHandler.cleanupChestIfEmpty(world, chestPosition);
            if (windows.isEmpty()) {
                world.setBlockInteractionState(pos, currentBlockType, "CloseWindow");
                BossLootHandler.scheduleChestExpiry(world, chestPosition);
            }
            playSound(world, pos, currentBlockType, "CloseWindow", chunk, blockType, playerRef, commandBuffer, store);
        });

        if (windows.size() == 1) {
            world.setBlockInteractionState(pos, blockType, "OpenWindow");
        }
        playSound(world, pos, blockType, "OpenWindow", chunk, blockType, playerRef, commandBuffer, store);
        chestState.onOpen(playerRef, world, store);
        LOGGER.info("Boss chest opened for " + playerUuid + " at " + x + "," + y + "," + z);
        return true;
    }

    private static void playSound(
            World world,
            Vector3i pos,
            BlockType blockType,
            String stateName,
            WorldChunk chunk,
            BlockType originalBlockType,
            Ref<EntityStore> ref,
            CommandBuffer<EntityStore> commandBuffer,
            Store<EntityStore> store) {
        if (blockType == null) {
            return;
        }
        BlockType interactionState = blockType.getBlockForState(stateName);
        if (interactionState == null) {
            return;
        }
        int soundEventIndex = interactionState.getInteractionSoundEventIndex();
        if (soundEventIndex == 0) {
            return;
        }
        int rotationIndex = chunk.getRotationIndex(pos.x, pos.y, pos.z);
        org.joml.Vector3d soundPos = new org.joml.Vector3d();
        originalBlockType.getBlockCenter(rotationIndex, soundPos);
        soundPos.add(pos.x, pos.y, pos.z);
        if (commandBuffer != null) {
            SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX, soundPos, commandBuffer);
        } else {
            SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX, soundPos, store);
        }
    }
}
