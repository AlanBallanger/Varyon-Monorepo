package com.varyon.comet.systems;

import com.varyon.comet.CometConfig;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.system.EntityEventSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CometBlockEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    
    private final CometWaveManager waveManager;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CometBlockEventSystem.class.getName());
    
    public CometBlockEventSystem(CometWaveManager waveManager) {
        super(UseBlockEvent.Pre.class);
        this.waveManager = waveManager;
    }
    
    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        // Return Query.any() to match all entities (events are dispatched on the triggering entity)
        return Query.any();
    }
    
    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, 
                      @Nonnull Store<EntityStore> store, 
                      @Nonnull CommandBuffer<EntityStore> commandBuffer, 
                      @Nonnull UseBlockEvent.Pre event) {
        com.hypixel.hytale.math.vector.Vector3i targetBlock = event.getTargetBlock();
        if (targetBlock == null) return;

        String idAtTarget = blockTypeIdFromUseEvent(event);
        if (idAtTarget != null
                && (idAtTarget.equals("Comet_Bench") || idAtTarget.endsWith("/Comet_Bench"))) {
            return;
        }

        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] UseBlockEvent at " + targetBlock + " interactionType=" + event.getInteractionType() + " (CometBlockEventSystem, position-based)");
        }

        // Position-based: any block at a registered comet position (or within radius for multi-block assets) activates
        com.hypixel.hytale.math.vector.Vector3i registeredPos = waveManager.getRegisteredBlockPos(targetBlock.x, targetBlock.y, targetBlock.z);
        boolean exactMatch = (registeredPos != null);
        if (registeredPos == null) {
            registeredPos = waveManager.getRegisteredBlockPosNear(targetBlock.x, targetBlock.y, targetBlock.z, CometConfig.COMET_USE_NEAR_RADIUS);
        }
        if (registeredPos == null) {
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: no comet registered at/near " + targetBlock + ", ignoring");
            }
            return;
        }
        com.hypixel.hytale.math.vector.Vector3i blockPos = registeredPos;
        // Cancel immediately so vanilla block Use (e.g. OpenContainer) never runs, even if we later return early
        event.setCancelled(true);
        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] CometBlockEventSystem: comet registered at " + blockPos + " (" + (exactMatch ? "exact" : "near") + "), cancelled vanilla Use");
        }

        // Only handle Use (f key) interactions - same as chests
        if (event.getInteractionType() != com.hypixel.hytale.protocol.InteractionType.Use) {
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: skipping activation, interactionType is not Use");
            }
            return;
        }

        boolean resolvedBlockType = false;
        boolean blockPresent = false;
        if (targetBlock.x == registeredPos.x && targetBlock.y == registeredPos.y && targetBlock.z == registeredPos.z) {
            resolvedBlockType = true;
            try {
                Object bt = event.getClass().getMethod("getBlockType").invoke(event);
                blockPresent = blockTypeNonUnknownReflect(bt);
            } catch (Throwable t) {
                blockPresent = false;
            }
        } else {
            try {
                World world = ((EntityStore) store.getExternalData()).getWorld();
                Object bt = world.getClass()
                        .getMethod("getBlockType", int.class, int.class, int.class)
                        .invoke(world, blockPos.x, blockPos.y, blockPos.z);
                resolvedBlockType = true;
                blockPresent = blockTypeNonUnknownReflect(bt);
            } catch (Throwable t) {
                LOGGER.warning("[CometBlockEventSystem] Could not resolve block at comet position (skipping empty check): " + t);
            }
        }
        if (resolvedBlockType && !blockPresent) {
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: no block at " + blockPos + ", skipping");
            }
            return;
        }
        
        // Get player entity ref from context
        Ref<EntityStore> playerRef = event.getContext().getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.warning("[CometBlockEventSystem] PlayerRef is null or invalid!");
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: skipping, playerRef null or invalid");
            }
            return;
        }

        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] CometBlockEventSystem: activating comet at " + blockPos + " (position-based Use)");
        }
        // Handle comet activation (vanilla Use already cancelled above when we found the comet)
        try {
            waveManager.handleCometActivation(store, playerRef, blockPos);
        } catch (Throwable t) {
            LOGGER.severe("[CometBlockEventSystem] Error in handleCometActivation: " + t);
            t.printStackTrace();
        }
    }

    @Nullable
    private static String blockTypeIdFromUseEvent(UseBlockEvent.Pre event) {
        try {
            Object bt = event.getClass().getMethod("getBlockType").invoke(event);
            if (bt == null || Boolean.TRUE.equals(bt.getClass().getMethod("isUnknown").invoke(bt))) {
                return null;
            }
            Object id = bt.getClass().getMethod("getId").invoke(bt);
            return id instanceof String ? (String) id : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean blockTypeNonUnknownReflect(@Nullable Object blockType) {
        if (blockType == null) {
            return false;
        }
        try {
            return !Boolean.TRUE.equals(blockType.getClass().getMethod("isUnknown").invoke(blockType));
        } catch (Throwable t) {
            return false;
        }
    }
}
