package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.ExtractionConfig;
import com.varyon.config.MessagesConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.extraction.ExtractionPortalManager;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class ExtractCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PERM_USE = "varyon.extract";
    private static final String PERM_BYPASS = "varyon.extract.bypass";
    private static final int START_Y = 320;
    private static final int MIN_Y = 0;
    private static final Random random = new Random();

    public ExtractCommand(String name) {
        super(name, "Spawn an extraction portal nearby");
        this.setPermissionGroups("adventure");
        this.requirePermission(PERM_USE);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        executeExtract(context, store, ref, playerRef, world, player);
    }

    @SuppressWarnings("unchecked")
    public static void executeExtract(@Nonnull CommandContext context, @Nonnull Store store,
                                      @Nonnull Ref ref, @Nonnull PlayerRef playerRef,
                                      @Nonnull World world, @Nullable Player player) {
        UUID playerId = playerRef.getUuid();
        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();

        if (manager == null) {
            context.sendMessage(Message.raw("Système d'extraction indisponible.").color(Color.RED));
            return;
        }

        if (!isVaryonWorldForExtract(world)) {
            context.sendMessage(Message.raw("Cette commande n'est disponible que sur les mondes Varyon.").color(Color.RED));
            return;
        }

        ExtractionConfig config = manager.getConfig();
        MessagesConfig.ExtractionMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getExtraction();

        if (!config.isEnabled()) {
            context.sendMessage(Message.raw("L'extraction est désactivée.").color(Color.RED));
            return;
        }

        boolean bypass = player != null && player.hasPermission(PERM_BYPASS);

        if (manager.hasActivePortal(playerId)) {
            manager.removePlayerPortal(playerId);
        }

        if (!bypass && manager.isOnCooldown(playerId)) {
            long remaining = manager.getCooldownRemainingSeconds(playerId);
            String cooldownMsg = msg.cooldown.replace("{remaining}", String.valueOf(remaining));
            context.sendMessage(Message.raw(cooldownMsg).color(Color.RED));
            return;
        }

        Transform playerTransform = playerRef.getTransform();
        double playerX = playerTransform.getPosition().x;
        double playerZ = playerTransform.getPosition().z;

        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        DifficultyZone zone = ZoneCalculator.getZoneAtPosition(playerX, playerZ, world.getName(), zoneConfig);
        int zoneId = zone != null ? zone.getZoneId() : 0;
        int minDist = config.getEffectiveMinDistance(zoneId);
        int maxDist = config.getEffectiveMaxDistance(zoneId);
        if (minDist > maxDist) {
            int t = minDist;
            minDist = maxDist;
            maxDist = t;
        }
        final int portalMinDist = minDist;
        final int portalMaxDist = maxDist;

        context.sendMessage(Message.raw("Recherche d'un emplacement pour le portail...").color(Color.YELLOW));

        world.execute(() -> {
            try {
                Vector3d portalPos = findPortalPosition(world, playerX, playerZ, portalMinDist, portalMaxDist, 30);

                if (portalPos == null) {
                    context.sendMessage(Message.raw(msg.noSafeLocation).color(Color.RED));
                    return;
                }

                int px = (int) portalPos.x;
                int py = (int) portalPos.y;
                int pz = (int) portalPos.z;
                double distance = Math.sqrt(Math.pow(px - playerX, 2) + Math.pow(pz - playerZ, 2));

                manager.placePortal(playerId, world, px, py, pz);

                int durationSec = config.getPortalDurationSeconds();
                String spawnMsg = msg.portalSpawned
                    .replace("{distance}", String.valueOf((int) distance))
                    .replace("{x}", String.valueOf(px))
                    .replace("{y}", String.valueOf(py))
                    .replace("{z}", String.valueOf(pz))
                    .replace("{duration}", String.valueOf(durationSec));
                context.sendMessage(Message.raw(spawnMsg).color(Color.GREEN));

                LOGGER.at(Level.INFO).log("Portal spawned for " + playerId + " at " + px + "," + py + "," + pz + " dist=" + (int) distance);

            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Error spawning extraction portal: " + e.getMessage(), e);
                context.sendMessage(Message.raw(msg.error).color(Color.RED));
            }
        });
    }

    private static boolean isVaryonWorldForExtract(@Nullable World world) {
        if (world == null) {
            return false;
        }
        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager() != null
            ? VaryonPlugin.getStaticConfigManager().getZoneConfig() : null;
        return zoneConfig != null && zoneConfig.isWorldEnabled(world.getName());
    }

    @Nullable
    private static Vector3d findPortalPosition(@Nonnull World world, double playerX, double playerZ,
                                        int minDist, int maxDist, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDist + random.nextDouble() * (maxDist - minDist);

            int targetX = (int) (playerX + Math.cos(angle) * distance);
            int targetZ = (int) (playerZ + Math.sin(angle) * distance);

            Double safeY = findSafeY(world, targetX, targetZ);
            if (safeY != null) {
                return new Vector3d(targetX, safeY, targetZ);
            }
        }
        return null;
    }

    @Nullable
    private static Double findSafeY(@Nonnull World world, int x, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            return null;
        }

        for (int checkY = START_Y; checkY >= MIN_Y; checkY--) {
            try {
                if (hasFluid(chunk, x, checkY, z)) {
                    return null;
                }

                if (isSolidBlock(chunk, x, checkY, z)) {
                    if (isTreeOrFoliageFooting(chunk, x, checkY, z)) {
                        continue;
                    }
                    int spawnY = checkY + 1;

                    if (hasFluid(chunk, x, spawnY, z) || hasFluid(chunk, x, spawnY + 1, z)) {
                        return null;
                    }

                    boolean hasSpace = true;
                    for (int dy = 0; dy < 4; dy++) {
                        if (isSolidBlock(chunk, x, spawnY + dy, z)) {
                            hasSpace = false;
                            break;
                        }
                    }

                    if (hasSpace) {
                        return (double) spawnY;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    private static boolean isSolidBlock(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType == null) {
                return false;
            }
            return blockType.getMaterial() == BlockMaterial.Solid;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isTreeOrFoliageFooting(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType == null || blockType.getMaterial() != BlockMaterial.Solid) {
                return false;
            }
            String id = blockType.getId();
            if (id == null) {
                return false;
            }
            String s = id.toLowerCase(Locale.ROOT);
            return s.contains("leaf")
                    || s.contains("leaves")
                    || s.contains("vine")
                    || s.contains("sapling")
                    || s.contains("flower")
                    || s.contains("mushroom")
                    || s.contains("bamboo")
                    || s.contains("log")
                    || s.contains("bark")
                    || s.contains("branch");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasFluid(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            return chunk.getFluidId(x, y, z) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
