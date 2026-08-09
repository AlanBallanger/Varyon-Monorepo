package com.varyon.comet.systems;

import com.varyon.comet.*;
import com.varyon.comet.audio.CometWorldSounds;
import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.varyon.comet.util.VecUtil;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.component.Projectile;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.varyon.comet.integration.ClaimProtectionGuard;
import com.varyon.comet.integration.WorldProtectRegionGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class CometFallingSystem {

    private static final Logger LOGGER = Logger.getLogger(CometFallingSystem.class.getName());

    private static double despawnTimeMinutes = 2.0;

    public static double getDespawnTimeMinutes() {
        return despawnTimeMinutes;
    }

    public static void setDespawnTimeMinutes(double minutes) {
        despawnTimeMinutes = minutes;
        if (CometConfig.getInstance().isDebugLoggingEnabled()) {
            LOGGER.info("Despawn time set to: " + minutes + " minutes");
        }
    }

    // Map to track projectile target positions by UUID
    // Key: projectile UUID, Value: target block position
    private final Map<UUID, Vector3i> trackedProjectiles = new HashMap<>();

    private static final long FALLBACK_ERROR_LOG_BACKOFF_MS = 10_000;
    private volatile long lastEntityErrorLoggedAt = 0;
    private volatile long lastFallbackErrorLoggedAt = 0;

    // Map to track spawn Y positions for fallback position checking
    // Key: projectile UUID, Value: spawn Y position
    private final Map<UUID, Double> projectileSpawnY = new HashMap<>();

    // Map to track tier for each projectile
    // Key: projectile UUID, Value: comet tier
    private final Map<UUID, CometTier> projectileTiers = new HashMap<>();

    // Map to track forced theme ID for each projectile (String-based)
    // Key: projectile UUID, Value: theme ID
    private final Map<UUID, String> projectileThemes = new HashMap<>();

    // Map to track owner UUID for each projectile (for marker visibility)
    // Key: projectile UUID, Value: owner player UUID
    private final Map<UUID, UUID> projectileOwners = new HashMap<>();

    // Map to track zone ID for each projectile (for zone-based rewards)
    // Key: projectile UUID, Value: zone ID
    private final Map<UUID, Integer> projectileZones = new HashMap<>();

    // Map to track spawn time for each projectile (for timeout detection)
    // Key: projectile UUID, Value: spawn timestamp in milliseconds
    private final Map<UUID, Long> projectileSpawnTime = new HashMap<>();

    // Timeout for projectile falling (in seconds) - if exceeded, force spawn comet at target
    private static final long PROJECTILE_TIMEOUT_SECONDS = 15;

    private final World world;

    public CometFallingSystem(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    public void trackProjectile(UUID projectileUUID, Vector3i targetBlockPos, double spawnY, CometTier tier,
            String themeId, UUID ownerUUID, int zoneId) {
        trackedProjectiles.put(projectileUUID, targetBlockPos);
        projectileSpawnY.put(projectileUUID, spawnY);
        projectileTiers.put(projectileUUID, tier);
        projectileZones.put(projectileUUID, zoneId);
        projectileSpawnTime.put(projectileUUID, System.currentTimeMillis());
        if (themeId != null) {
            projectileThemes.put(projectileUUID, themeId);
        }
        if (ownerUUID != null) {
            projectileOwners.put(projectileUUID, ownerUUID);
        }
        if (CometConfig.getInstance().isDebugLoggingEnabled()) {
            LOGGER.fine("Tracking projectile " + projectileUUID + " -> " + targetBlockPos);
        }
    }

    public Vector3i getTrackedTarget(UUID projectileUUID) {
        return trackedProjectiles.get(projectileUUID);
    }

    public Vector3i removeTrackedProjectile(UUID projectileUUID) {
        Vector3i targetPos = trackedProjectiles.remove(projectileUUID);
        projectileSpawnY.remove(projectileUUID);
        projectileTiers.remove(projectileUUID);
        projectileThemes.remove(projectileUUID);
        projectileOwners.remove(projectileUUID);
        projectileZones.remove(projectileUUID);
        projectileSpawnTime.remove(projectileUUID);
        return targetPos;
    }

    public CometTier getProjectileTier(UUID projectileUUID) {
        return projectileTiers.getOrDefault(projectileUUID, CometTier.UNCOMMON);
    }

    public String getProjectileThemeId(UUID projectileUUID) {
        return projectileThemes.get(projectileUUID);
    }

    public UUID getProjectileOwner(UUID projectileUUID) {
        return projectileOwners.get(projectileUUID);
    }

    public int getProjectileZone(UUID projectileUUID) {
        return projectileZones.getOrDefault(projectileUUID, 0);
    }

    public void checkProjectilesFallback(World world, Store<EntityStore> store) {
        if (trackedProjectiles.isEmpty()) {
            return;
        }

        try {
            // Resolve each tracked projectile directly by UUID instead of scanning every loaded
            // chunk in the world — tracked-projectile count is tiny (usually 0-1) regardless of
            // world size, so a direct lookup avoids an O(all chunks) scan every second.
            for (UUID entityUUID : new java.util.ArrayList<>(trackedProjectiles.keySet())) {
                try {
                    Vector3i targetPos = trackedProjectiles.get(entityUUID);
                    if (targetPos == null) {
                        continue;
                    }

                    Ref<EntityStore> ref = world.getEntityRef(entityUUID);
                    if (ref == null || !ref.isValid()) {
                        continue; // Not resolvable this cycle; orphan cleanup handles timeouts below.
                    }

                    // Get position
                    TransformComponent transform = store.getComponent(ref,
                            TransformComponent.getComponentType());
                    if (transform == null) {
                        continue;
                    }

                    Vector3d position = VecUtil.toJoml(transform.getPosition());
                    Double spawnY = projectileSpawnY.get(entityUUID);
                    Long spawnTime = projectileSpawnTime.get(entityUUID);

                    // Check for timeout (projectile stuck on entity)
                    boolean timedOut = false;
                    if (spawnTime != null) {
                        long elapsedSeconds = (System.currentTimeMillis() - spawnTime) / 1000;
                        if (elapsedSeconds >= PROJECTILE_TIMEOUT_SECONDS) {
                            timedOut = true;
                            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                                LOGGER.info("Projectile " + entityUUID + " timed out after " + elapsedSeconds + "s, force-spawning comet at target");
                            }
                        }
                    }

                    if (spawnY != null) {
                        // Calculate target Y (ground level - spawnY was 100 blocks above target)
                        double targetY = spawnY - 100.0;

                        // Check if projectile has hit or passed the target Y level OR timed out
                        if (position.y <= targetY + 1.0 || timedOut) {
                            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                                LOGGER.fine("Fallback: Projectile " + entityUUID + " hit ground" + (timedOut ? " (timed out)" : ""));
                            }

                            org.joml.Vector3i actualBlockPos;

                            if (timedOut) {
                                // Use original target position when timed out (projectile stuck)
                                actualBlockPos = targetPos;
                            } else {
                                // Use actual landing position - round to nearest block for X/Z
                                int blockX = (int) Math.round(position.x);
                                int blockZ = (int) Math.round(position.z);
                                int landingBlockY = (int) Math.floor(position.y);

                                // Find the actual solid ground below the landing position
                                // The projectile might land on grass/plants, so we need to find the solid block
                                int solidGroundY = findGroundLevelAtPosition(world, blockX, blockZ, landingBlockY);

                                int blockY;
                                if (solidGroundY != -1) {
                                    // Found solid ground, place comet one block above it
                                    blockY = solidGroundY + 1;
                                } else {
                                    // Fallback: place one block above landing Y
                                    blockY = landingBlockY + 1;
                                }

                                actualBlockPos = new org.joml.Vector3i(
                                        blockX, blockY, blockZ);
                            }

                            CometTier tier = getProjectileTier(entityUUID);
                            String themeId = getProjectileThemeId(entityUUID);
                            UUID ownerUUID = getProjectileOwner(entityUUID);
                            int zoneId = getProjectileZone(entityUUID);
                            spawnCometBlock(world, actualBlockPos, store, tier, themeId, ownerUUID, zoneId);
                            removeTrackedProjectile(entityUUID);

                            // Remove the projectile entity
                            try {
                                com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer =
                                        com.varyon.comet.util.CommandBufferUtil.take(store);
                                if (commandBuffer != null) {
                                    try {
                                        commandBuffer.removeEntity(ref,
                                                com.hypixel.hytale.component.RemoveReason.REMOVE);
                                    } finally {
                                        com.varyon.comet.util.CommandBufferUtil.consume(commandBuffer);
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.warning(
                                        "[CometFallingSystem] Could not remove projectile: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    long now = System.currentTimeMillis();
                    if (now - lastEntityErrorLoggedAt >= FALLBACK_ERROR_LOG_BACKOFF_MS) {
                        lastEntityErrorLoggedAt = now;
                        LOGGER.warning("[CometFallingSystem] Error checking tracked projectile: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            // Cleanup pass: handle orphaned projectiles that timed out but entity is gone
            cleanupOrphanedProjectiles(world, store);

        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastFallbackErrorLoggedAt >= FALLBACK_ERROR_LOG_BACKOFF_MS) {
                lastFallbackErrorLoggedAt = now;
                LOGGER.warning("[CometFallingSystem] Error in fallback projectile check: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void cleanupOrphanedProjectiles(World world, Store<EntityStore> store) {
        if (trackedProjectiles.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        java.util.List<UUID> toRemove = new java.util.ArrayList<>();

        for (Map.Entry<UUID, Long> entry : projectileSpawnTime.entrySet()) {
            UUID projectileUUID = entry.getKey();
            Long spawnTime = entry.getValue();

            if (spawnTime != null) {
                long elapsedSeconds = (currentTime - spawnTime) / 1000;
                // Use a longer timeout for orphan cleanup (30 seconds) to give normal processing time
                if (elapsedSeconds >= PROJECTILE_TIMEOUT_SECONDS * 2) {
                    toRemove.add(projectileUUID);
                }
            }
        }

        for (UUID projectileUUID : toRemove) {
            Vector3i targetPos = trackedProjectiles.get(projectileUUID);
            if (targetPos != null) {
                LOGGER.warning("Cleaning up orphaned projectile " + projectileUUID + " - spawning comet at target " + targetPos);
                CometTier tier = getProjectileTier(projectileUUID);
                String themeId = getProjectileThemeId(projectileUUID);
                UUID ownerUUID = getProjectileOwner(projectileUUID);
                int zoneId = getProjectileZone(projectileUUID);
                spawnCometBlock(world, targetPos, store, tier, themeId, ownerUUID, zoneId);
            }
            removeTrackedProjectile(projectileUUID);
        }
    }

    public int findGroundLevelAtPosition(World world, int x, int z, int startY) {
        // Search downward from startY to find the first solid block
        // Start from startY (the block the projectile is in/above)
        int minY = Math.max(0, startY - 50); // Search down up to 50 blocks

        // Check startY first (projectile might be directly above a solid block)
        for (int y = startY; y >= minY; y--) {
            try {
                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType = world.getBlockType(x,
                        y, z);

                if (blockType != null) {
                    // Check if block is solid using the same method as spawning system
                    int blockId = com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap()
                            .getIndex(blockType.getId());

                    // Check if block is solid (not air, not empty)
                    if (blockId != 0) {
                        // Check material - same as LocalSpawnControllerSystem
                        Object material = blockType.getMaterial();
                        if (material != null) {
                            String materialStr = material.toString();
                            // Solid blocks are valid ground
                            if (materialStr.equals("Solid") || materialStr.equals("Opaque")) {
                                return y;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Continue searching if block type check fails
                continue;
            }
        }

        return -1; // Ground not found
    }

    public void spawnFallingComet(Ref<EntityStore> playerRef, Vector3i targetBlockPos, CometTier tier, String themeId,
            Store<EntityStore> store, World targetWorld, UUID ownerUUID, int zoneId) {
        try {
            CometConfig config = CometConfig.getInstance();
            if (!ClaimProtectionGuard.canSpawnAt(targetWorld, targetBlockPos.x, targetBlockPos.y, targetBlockPos.z,
                    config)) {
                String regionId = WorldProtectRegionGuard.getPrimaryRegionIdAt(targetWorld, targetBlockPos.x,
                        targetBlockPos.y, targetBlockPos.z);
                if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                    LOGGER.info("Comet spawn blocked by claim/protected-zone rules at " + targetBlockPos +
                            (regionId != null ? " (region: " + regionId + ")" : ""));
                }
                return;
            }

            // Get projectile config (tier-specific)
            String projectileConfigName = tier.getFallingProjectileConfig();
            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig projectileConfig = (com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig) com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig
                    .getAssetMap()
                    .getAsset(projectileConfigName);

            if (projectileConfig == null) {
                LOGGER.warning("Projectile config not found: " + projectileConfigName + "; placing spawn block directly.");
                spawnCometBlock(targetWorld, targetBlockPos, store, tier, themeId, ownerUUID, zoneId);
                return;
            }

            // Spawn position: 100 blocks above target
            Vector3d spawnPos = new Vector3d(
                    targetBlockPos.x + 0.5,
                    targetBlockPos.y + 100.0,
                    targetBlockPos.z + 0.5);

            // Direction: straight down
            Vector3d direction = new Vector3d(0, -1, 0);

            // Generate UUID for tracking
            UUID projectileUUID = UUID.randomUUID();

            com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer = com.varyon.comet.util.CommandBufferUtil.take(store);
            if (commandBuffer == null) return;

            Ref<EntityStore> projectileRef = com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                    .spawnProjectile(projectileUUID, playerRef, commandBuffer, projectileConfig, spawnPos, VecUtil.toHytale(direction));

            if (projectileRef != null) {
                trackProjectile(projectileUUID, targetBlockPos, spawnPos.y, tier, themeId, ownerUUID, zoneId);
            } else {
                LOGGER.warning("Failed to spawn projectile, falling back to direct block spawn");
                spawnCometBlock(targetWorld, targetBlockPos, store, tier, themeId, ownerUUID, zoneId);
            }

            com.varyon.comet.util.CommandBufferUtil.consume(commandBuffer);

        } catch (Exception e) {
            LOGGER.warning("Error spawning falling comet: " + e.getMessage());
            e.printStackTrace();
            // Fallback: spawn block directly
            spawnCometBlock(targetWorld, targetBlockPos, store, tier, themeId, ownerUUID, zoneId);
        }
    }

    public void spawnCometBlock(World world, Vector3i blockPos, Store<EntityStore> store, CometTier tier,
            String themeId, UUID ownerUUID, int zoneId) {
        try {
            String blockIdName = CometSpawnUtil.placeAndRegisterCometBlock(world, blockPos, store, tier, themeId, ownerUUID, zoneId);
            if (blockIdName != null) {
                CometWorldSounds.playCometLand(store, blockPos);
                scheduleDespawn(world, blockPos, blockIdName);
            }
        } catch (Exception e) {
            LOGGER.severe("Error in spawnCometBlock: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void scheduleDespawn(World world, Vector3i blockPos, String blockIdName) {
        // Schedule despawn after configured time (convert minutes to seconds)
        // Use long to handle decimal minutes correctly (e.g., 0.1 minutes = 6 seconds)
        long despawnSeconds = Math.round(despawnTimeMinutes * 60.0);
        com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            world.execute(() -> {
                try {
                    despawnCometBlock(world, blockPos, blockIdName);
                } catch (Exception e) {
                    LOGGER.warning("Error despawning comet block at " + blockPos + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }, despawnSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void despawnCometBlock(World world, Vector3i blockPos, String blockIdName) {
        try {
            // Get chunk
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(
                    blockPos.x, blockPos.z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

            if (chunk == null) {
                chunk = world.getChunk(chunkIndex);
            }

            if (chunk == null) {
                LOGGER.warning("Could not get chunk for despawn at " + blockPos);
                return;
            }

            // Always break whatever is at the registered position (simple v1 behavior)
            {
                try {
                    boolean broken = world.breakBlock(blockPos.x, blockPos.y, blockPos.z, 0);
                    if (broken) {
                        chunk.markNeedsSaving();
                        if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                            LOGGER.info("Despawned block at " + blockPos + (blockIdName != null ? " (" + blockIdName + ")" : ""));
                        }
                    } else {
                        LOGGER.warning("Failed to break block at " + blockPos + " for despawn");
                    }
                } catch (Exception e) {
                    try {
                        boolean broken = chunk.breakBlock(blockPos.x, blockPos.y, blockPos.z, 0);
                        if (broken) {
                            chunk.markNeedsSaving();
                            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                                LOGGER.info("Despawned block at " + blockPos + " (using chunk.breakBlock)");
                            }
                        }
                    } catch (Exception e2) {
                        LOGGER.warning("Failed to despawn block at " + blockPos + ": " + e2.getMessage());
                    }
                }
            }

            // Clean up wave manager tracking and remove map marker (world available; store
            // not)
            CometWaveManager waveManager = CometModPlugin.getWaveManager();
            if (waveManager != null) {
                waveManager.handleBlockBreak(world, blockPos);
            }

            // Unregister from despawn tracker
            CometDespawnTracker.getInstance().unregisterComet(blockPos);

        } catch (Exception e) {
            LOGGER.warning("Error despawning comet block: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
