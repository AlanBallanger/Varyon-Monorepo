package com.varyon.comet.commands;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import org.joml.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Command to destroy all comet blocks in the world.
 * Usage: /comet destroyall
 */
public class CometDestroyAllCommand extends AbstractWorldCommand {
    
    private static final Logger LOGGER = Logger.getLogger(CometDestroyAllCommand.class.getName());
    
    public CometDestroyAllCommand() {
        super("destroyall", "Destroy all comet blocks in the world");
        requirePermission(CometPermissions.DESTROY_ALL);
    }
    
    @Override
    protected void execute(@Nonnull CommandContext context, 
                          @Nonnull World world, 
                          @Nonnull Store<EntityStore> store) {
        
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return;
        }
        
        try {
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> _senderRef = context.senderAsPlayerRef();
            Player player = (_senderRef != null && _senderRef.isValid()) ? _senderRef.getStore().getComponent(_senderRef, Player.getComponentType()) : null;

            CometWaveManager waveManager = CometModPlugin.getWaveManager();
            if (waveManager == null) {
                context.sendMessage(Message.raw("Wave manager not available."));
                return;
            }

            // Discover comets by registered positions from the wave manager
            java.util.Map<Vector3i, ?> cometTiersMap = waveManager.getCometTiers();
            List<Vector3i> cometPositions = new ArrayList<>(cometTiersMap.keySet());

            if (cometPositions.isEmpty()) {
                context.sendMessage(Message.raw("No registered comet positions found in the world."));
                return;
            }

            final int totalComets = cometPositions.size();
            context.sendMessage(Message.raw("Found " + totalComets + " comet(s). Destroying..."));

            CometDespawnTracker tracker = CometDespawnTracker.getInstance();

            // Destroy all comets on the world thread
            world.execute(() -> {
                int destroyed = 0;
                for (Vector3i pos : cometPositions) {
                    try {
                        if (waveManager != null) {
                            waveManager.handleBlockBreak(world, pos);
                        }
                        destroyCometBlock(world, pos);
                        tracker.unregisterComet(pos);
                        destroyed++;
                    } catch (Exception e) {
                        LOGGER.warning("Error destroying comet at " + pos + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                final int finalDestroyed = destroyed;
                com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
                    context.sendMessage(Message.raw("Destroyed " + finalDestroyed + " comet(s)."));
                    if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                        LOGGER.info("Destroyed " + finalDestroyed + " comets via /comet destroyall");
                    }
                });
            });
            
        } catch (Exception e) {
            LOGGER.severe("Error in destroyall command: " + e.getMessage());
            e.printStackTrace();
            context.sendMessage(Message.raw("Error: " + e.getMessage()));
        }
    }

    /**
     * Destroy the block at the given registered comet position.
     */
    private void destroyCometBlock(World world, Vector3i pos) {
        try {
            // Prefer normal break logic so block state and drop behavior remain consistent.
            boolean broken = world.breakBlock(pos.x, pos.y, pos.z, 0);
            if (broken) {
                if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                    LOGGER.info("Destroyed comet block at " + pos + " via world.breakBlock");
                }
                return;
            }

            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            
            if (chunk == null) {
                chunk = world.getChunk(chunkIndex);
            }
            
            if (chunk == null) {
                LOGGER.warning("Could not get chunk for comet block at " + pos);
                return;
            }
            
            // Get local chunk coordinates
            int localX = pos.x & 31;
            int localZ = pos.z & 31;
            
            // Fallback: force set to air with explicit EMPTY block type.
            boolean set = chunk.setBlock(localX, pos.y, localZ, BlockType.EMPTY_ID, BlockType.EMPTY, 0, 0, 157);
            if (!set) {
                LOGGER.warning("Failed to force-remove comet block at " + pos);
                return;
            }
            chunk.markNeedsSaving();

            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                LOGGER.info("Destroyed comet block at " + pos + " via chunk fallback");
            }
        } catch (Exception e) {
            LOGGER.warning("Error destroying comet block at " + pos + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
