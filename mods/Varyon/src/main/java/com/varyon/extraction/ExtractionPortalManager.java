package com.varyon.extraction;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.varyon.VaryonPlugin;
import com.varyon.config.ExtractionConfig;
import com.varyon.config.MessagesConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ExtractionPortalManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PORTAL_BLOCK_TYPE = "ExtractionPortal";

    private static ExtractionPortalManager instance;

    private final Map<UUID, PortalData> playerPortals = new ConcurrentHashMap<>();
    private final Map<String, UUID> positionToOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private ExtractionConfig config;
    private int portalBlockIndex = Integer.MIN_VALUE;
    private BlockType portalBlockTypeRef;

    public ExtractionPortalManager(@Nonnull ExtractionConfig config) {
        this.config = config;
        instance = this;
        LOGGER.at(Level.INFO).log("ExtractionPortalManager initialized");
    }

    public void resolveBlockType() {
        portalBlockIndex = BlockType.getAssetMap().getIndex(PORTAL_BLOCK_TYPE);
        if (portalBlockIndex == Integer.MIN_VALUE) {
            LOGGER.at(Level.SEVERE).log("Block type '" + PORTAL_BLOCK_TYPE + "' not found in asset map!");
            return;
        }
        portalBlockTypeRef = BlockType.getAssetMap().getAsset(portalBlockIndex);
        LOGGER.at(Level.INFO).log("ExtractionPortal block resolved: index=" + portalBlockIndex);
    }

    @Nullable
    public static ExtractionPortalManager getInstance() {
        return instance;
    }

    public void setConfig(@Nonnull ExtractionConfig config) {
        this.config = config;
    }

    @Nonnull
    public ExtractionConfig getConfig() {
        return config;
    }

    @Nonnull
    public Map<UUID, PortalData> getActivePortals() {
        return playerPortals;
    }

    public boolean hasActivePortal(@Nonnull UUID playerId) {
        return playerPortals.containsKey(playerId);
    }

    public boolean isOnCooldown(@Nonnull UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(playerId);
            return false;
        }
        return true;
    }

    public long getCooldownRemainingSeconds(@Nonnull UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    @Nullable
    public UUID getPortalOwner(int x, int y, int z) {
        return positionToOwner.get(positionKey(x, y, z));
    }

    public void placePortal(@Nonnull UUID ownerId, @Nonnull World world, int x, int y, int z) {
        if (portalBlockIndex == Integer.MIN_VALUE) {
            resolveBlockType();
            if (portalBlockIndex == Integer.MIN_VALUE) {
                LOGGER.at(Level.SEVERE).log("Cannot place portal: block type not resolved");
                return;
            }
        }

        String posKey = positionKey(x, y, z);

        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                LOGGER.at(Level.WARNING).log("Chunk not loaded for portal at " + x + ", " + z + ", loading async");
                world.getChunkAsync(chunkIndex).thenAccept(asyncChunk -> {
                    placeBlockInChunk(asyncChunk, x, y, z);
                }).exceptionally(ex -> {
                    LOGGER.at(Level.SEVERE).log("Failed to load chunk for portal: " + ex.getMessage());
                    return null;
                });
            } else {
                placeBlockInChunk(chunk, x, y, z);
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Exception placing portal: " + e.getClass().getName() + " - " + e.getMessage());
        }

        ScheduledFuture<?> expiryTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> expirePortal(ownerId),
            config.getPortalDurationSeconds(),
            TimeUnit.SECONDS
        );

        PortalData portalData = new PortalData(ownerId, world, x, y, z, System.currentTimeMillis(), expiryTask);
        playerPortals.put(ownerId, portalData);
        positionToOwner.put(posKey, ownerId);

        LOGGER.at(Level.INFO).log("Portal placed for " + ownerId + " at " + x + ", " + y + ", " + z + " (expires in " + config.getPortalDurationSeconds() + "s)");
    }

    private void placeBlockInChunk(@Nonnull WorldChunk chunk, int x, int y, int z) {
        for (int dy = 0; dy < 4; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    chunk.setBlock(x + dx, y + dy, z + dz, BlockType.EMPTY);
                }
            }
        }
        chunk.setBlock(x, y, z, portalBlockIndex, portalBlockTypeRef, 0, 0, 0);
    }

    public void consumePortal(@Nonnull UUID ownerId) {
        PortalData data = playerPortals.remove(ownerId);
        if (data == null) {
            return;
        }

        data.expiryTask().cancel(false);
        positionToOwner.remove(positionKey(data.x(), data.y(), data.z()));
        removePortalBlock(data);
        cooldowns.put(ownerId, System.currentTimeMillis() + (config.getCooldownSeconds() * 1000L));

        LOGGER.at(Level.INFO).log("Portal consumed by " + ownerId + " at " + data.x() + ", " + data.y() + ", " + data.z());
    }

    private void expirePortal(@Nonnull UUID ownerId) {
        PortalData data = playerPortals.remove(ownerId);
        if (data == null) {
            return;
        }

        positionToOwner.remove(positionKey(data.x(), data.y(), data.z()));
        removePortalBlock(data);

        LOGGER.at(Level.INFO).log("Portal expired for " + ownerId);

        try {
            PlayerRef playerRef = Universe.get().getPlayer(ownerId);
            if (playerRef != null && playerRef.getReference() != null && playerRef.getReference().isValid()) {
                MessagesConfig.ExtractionMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getExtraction();
                playerRef.sendMessage(Message.raw(msg.portalExpired).color(Color.YELLOW));
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to notify player of portal expiry: " + e.getMessage());
        }
    }

    private void removePortalBlock(@Nonnull PortalData data) {
        World world = data.world();
        int x = data.x();
        int y = data.y();
        int z = data.z();

        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk != null) {
                chunk.setBlock(x, y, z, BlockType.EMPTY);
            } else {
                world.getChunkAsync(chunkIndex).thenAcceptAsync(asyncChunk -> {
                    asyncChunk.setBlock(x, y, z, BlockType.EMPTY);
                }, (Executor) world);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to remove portal block at " + x + ", " + y + ", " + z + ": " + e.getMessage());
        }
    }

    public void removePlayerPortal(@Nonnull UUID playerId) {
        PortalData data = playerPortals.remove(playerId);
        if (data == null) {
            return;
        }
        data.expiryTask().cancel(false);
        positionToOwner.remove(positionKey(data.x(), data.y(), data.z()));
        removePortalBlock(data);
        LOGGER.at(Level.INFO).log("Portal removed for disconnected player " + playerId);
    }

    public void shutdown() {
        for (Map.Entry<UUID, PortalData> entry : playerPortals.entrySet()) {
            PortalData data = entry.getValue();
            data.expiryTask().cancel(false);
            removePortalBlock(data);
        }
        playerPortals.clear();
        positionToOwner.clear();
        cooldowns.clear();
        LOGGER.at(Level.INFO).log("ExtractionPortalManager shut down");
    }

    private static String positionKey(int x, int y, int z) {
        return x + "_" + y + "_" + z;
    }

    public record PortalData(
        UUID ownerId,
        World world,
        int x, int y, int z,
        long creationTime,
        ScheduledFuture<?> expiryTask
    ) {}
}
