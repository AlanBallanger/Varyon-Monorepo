package com.varyon.comet.commands;

import com.varyon.comet.CometConfig;
import com.varyon.comet.CometModPlugin;
import com.varyon.comet.integration.ClaimProtectionGuard;
import com.varyon.comet.integration.VaryonZoneResolver;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.varyon.comet.util.VecUtil;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import javax.annotation.Nonnull;
import java.util.Random;
import java.util.logging.Logger;

public class CometSpawnCommand extends AbstractWorldCommand {

    private static final Logger LOGGER = Logger.getLogger(CometSpawnCommand.class.getName());
    private static final Random RANDOM = new Random();

    // Optional tier argument
    private final OptionalArg<String> tierArg;
    // Optional theme argument
    private final OptionalArg<String> themeArg;
    // Optional flag to spawn comet directly above player
    private final OptionalArg<String> onMeArg;
    // Optional target player to spawn the comet near/on instead of the sender
    private final OptionalArg<PlayerRef> targetArg;

    public CometSpawnCommand() {
        super("spawn", "Spawns a comet near the player. Use --theme <name> to choose the wave theme (see /comet themes).");
        requirePermission(CometPermissions.SPAWN);
        this.tierArg = withOptionalArg("tier", "Tier of the comet (Common, Rare, Epic, Legendary, Mythic)", ArgTypes.STRING);
        this.themeArg = withOptionalArg("theme", "Theme of the wave — e.g. Skeleton, Void, Lava. Use /comet themes to list all.", ArgTypes.STRING);
        this.onMeArg = withOptionalArg("onme", "Use --onme true to spawn comet directly above the player", ArgTypes.STRING);
        this.targetArg = withOptionalArg("target", "Player to target instead of yourself", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store) {

        // Check if sender is a player
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return;
        }

        CometConfig config = CometConfig.getInstance();
        if (config != null && !config.isRaidEnabledInWorld(world)) {
            String worldName = world.getName();
            if (worldName == null || worldName.isBlank()) {
                worldName = "unknown";
            }
            context.sendMessage(Message.raw("Comet raids are disabled in this world (" + worldName + ")."));
            return;
        }

        CometTier tier = CometTier.UNCOMMON;

        if (tierArg.provided(context)) {
            String tierString = tierArg.get(context);
            if (tierString != null && !tierString.isEmpty()) {
                tier = CometTier.fromString(tierString);

                if (tier == CometTier.UNCOMMON && !isTier1Alias(tierString)) {
                    String validTiers = CometConfig.isTier5Enabled()
                            ? "Common, Rare, Epic, Legendary, Mythic"
                            : "Common, Rare, Epic, Legendary";
                    context.sendMessage(Message.raw("Invalid tier! Valid tiers: " + validTiers));
                    return;
                }
            }
        }

        if (tier == CometTier.MYTHIC && !CometConfig.isTier5Enabled()) {
            context.sendMessage(Message.raw("Mythic/Tier 5 is disabled because Endgame&QoL is not installed."));
            return;
        }

        // Get theme from argument (optional) - now uses string-based theme IDs
        String themeId = null;
        String themeNameStr = null;

        if (themeArg.provided(context)) {
            String themeArgStr = themeArg.get(context);
            if (themeArgStr != null && !themeArgStr.isEmpty()) {
                // Replace underscores with spaces to support multi-word themes (e.g.
                // "Legendary_Earth")
                // consistent with command line usage where spaces split arguments
                themeArgStr = themeArgStr.replace('_', ' ');

                CometWaveManager waveManager = CometModPlugin.getWaveManager();
                if (waveManager != null) {
                    themeId = waveManager.getThemeIdByName(themeArgStr);
                    if (themeId == null) {
                        context.sendMessage(Message.raw("Invalid theme! Valid themes: " +
                                String.join(", ", waveManager.getThemeNames())));
                        return;
                    }
                    themeNameStr = WaveThemeProvider.getThemeName(themeId);
                }
            }
        }

        try {
            // Resolve target player: explicit --target argument, falling back to the sender
            Ref<EntityStore> playerRef;
            if (targetArg.provided(context)) {
                PlayerRef targetPlayerRef = targetArg.get(context);
                playerRef = targetPlayerRef != null ? targetPlayerRef.getReference() : null;
                if (playerRef == null || !playerRef.isValid()) {
                    context.sendMessage(Message.raw("Error: Target player is not online or could not be found!"));
                    return;
                }
            } else {
                com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> _senderRef = context.senderAsPlayerRef();
                Player player = (_senderRef != null && _senderRef.isValid()) ? _senderRef.getStore().getComponent(_senderRef, Player.getComponentType()) : null;
                playerRef = player.getReference();

                if (playerRef == null || !playerRef.isValid()) {
                    context.sendMessage(Message.raw("Error: Could not get player reference!"));
                    return;
                }
            }

            // Get player position from TransformComponent
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = store.getComponent(
                    playerRef,
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (transform == null) {
                context.sendMessage(Message.raw("Error: Could not get player position!"));
                return;
            }
            Vector3d playerPos = VecUtil.toJoml(transform.getPosition());
            Player targetPlayer = store.getComponent(playerRef, Player.getComponentType());

            // Check if --onme flag is provided (spawn directly above player)
            boolean spawnOnPlayer = onMeArg.provided(context);

            int spawnX = 0, spawnY = -1, spawnZ = 0;
            boolean foundValidLocation = false;

            if (spawnOnPlayer) {
                // Spawn directly above the player's current position
                spawnX = (int) playerPos.x;
                spawnZ = (int) playerPos.z;
                spawnY = (int) playerPos.y;
                int targetY = spawnY + 1;
                if (!ClaimProtectionGuard.canSpawnAt(world, spawnX, targetY, spawnZ, config)) {
                    context.sendMessage(Message.raw(
                            "Comet blocked by claim/protected-zone rules at this location."));
                    return;
                }
                foundValidLocation = true;
            } else {
                // Override for manual spawn command to keep it close (5-8 blocks)
                int minDist = 5;
                int maxDist = 8;

                // Try up to 16 times to find a valid position (ground, not water)
                for (int attempt = 0; attempt < 16; attempt++) {
                    double angle = RANDOM.nextDouble() * 2 * Math.PI;
                    double distance = minDist + RANDOM.nextDouble() * (maxDist - minDist);
                    int x = (int) (playerPos.x + Math.cos(angle) * distance);
                    int z = (int) (playerPos.z + Math.sin(angle) * distance);
                    int y = findGroundLevel(world, x, z, (int) playerPos.y);
                    if (y == -1)
                        continue;
                    if (isInWater(world, x, y, z) || isInWater(world, x, y + 1, z))
                        continue;
                    if (!ClaimProtectionGuard.canSpawnAt(world, x, y + 1, z, config))
                        continue;
                    spawnX = x;
                    spawnY = y;
                    spawnZ = z;
                    foundValidLocation = true;
                    break;
                }
            }

            if (!foundValidLocation) {
                context.sendMessage(
                        Message.raw("Error: Could not find valid spawn location (ground, water, or protected-zone restrictions) after 16 attempts!"));
                return;
            }

            // Target block position (1 block above ground)
            final Vector3i targetBlockPos = new Vector3i(spawnX, spawnY + 1, spawnZ);
            PlayerRef playerRefComp = store.getComponent(playerRef, PlayerRef.getComponentType());
            final java.util.UUID ownerUUID = playerRefComp != null ? playerRefComp.getUuid() : null;

            int landingRing = VaryonZoneResolver.resolveVaryonRingForComet(
                    targetBlockPos.x, targetBlockPos.z, world.getName(), targetPlayer);
            final int ringForWave = landingRing > 0 ? landingRing : 1;

            if (themeId != null) {
                CometWaveManager waveManager = CometModPlugin.getWaveManager();
                if (waveManager != null) {
                    waveManager.registerCometZone(targetBlockPos, ringForWave);
                    waveManager.forceTheme(targetBlockPos, themeId);
                }
            } else {
                CometWaveManager waveManager = CometModPlugin.getWaveManager();
                if (waveManager != null) {
                    waveManager.registerCometZone(targetBlockPos, ringForWave);
                }
            }

            // Get projectile config (tier-specific)
            String projectileConfigName = tier.getFallingProjectileConfig();
            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig projectileConfig = (com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig) com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig
                    .getAssetMap()
                    .getAsset(projectileConfigName);

            if (projectileConfig == null) {
                context.sendMessage(Message.raw(projectileConfigName + " projectile config not found; placing spawn block directly."));
                spawnCometBlockDirectly(world, targetBlockPos, store, tier, themeId, ownerUUID, ringForWave);
                return;
            }

            // Spawn position: 100 blocks above target (higher for better visibility)
            Vector3d spawnPos = new Vector3d(
                    targetBlockPos.x + 0.5,
                    targetBlockPos.y + 100.0,
                    targetBlockPos.z + 0.5);

            // Direction: straight down
            Vector3d direction = new Vector3d(0, -1, 0);

            // Initialize falling system if not already initialized
            CometFallingSystem fallingSystem = CometModPlugin.getFallingSystem();
            if (fallingSystem == null) {
                fallingSystem = new CometFallingSystem(world);
                CometModPlugin.setFallingSystem(fallingSystem);
            }

            // Store tier and owner UUID with the projectile tracking
            // We'll pass them to spawnCometBlock when projectile lands
            final CometTier finalTier = tier;

            // Generate UUID for tracking this projectile
            java.util.UUID projectileUUID = java.util.UUID.randomUUID();

            com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer = com.varyon.comet.util.CommandBufferUtil.take(store);
            if (commandBuffer == null) {
                spawnCometBlockDirectly(world, targetBlockPos, store, tier, themeId, ownerUUID, ringForWave);
                return;
            }

            Object externalData = commandBuffer.getExternalData();
            if (!(externalData instanceof com.hypixel.hytale.server.core.universe.world.storage.EntityStore)) {
                spawnCometBlockDirectly(world, targetBlockPos, store, tier, themeId, ownerUUID, ringForWave);
                return;
            }

            try {
                com.hypixel.hytale.component.Ref<EntityStore> projectileRef = com.hypixel.hytale.server.core.modules.projectile.ProjectileModule
                        .get()
                        .spawnProjectile(projectileUUID, playerRef, commandBuffer, projectileConfig, spawnPos,
                                VecUtil.toHytale(direction));

                if (projectileRef != null) {
                    fallingSystem.trackProjectile(projectileUUID, targetBlockPos, spawnPos.y, finalTier, themeId,
                            ownerUUID, ringForWave);
                } else {
                    spawnCometBlockDirectly(world, targetBlockPos, store, finalTier, themeId, ownerUUID, ringForWave);
                }
            } catch (Exception e) {
                LOGGER.severe("Error spawning projectile: " + e.getMessage());
                spawnCometBlockDirectly(world, targetBlockPos, store, finalTier, themeId, ownerUUID, ringForWave);
            } finally {
                com.varyon.comet.util.CommandBufferUtil.consume(commandBuffer);
            }

            CometConfig cfgMsg = CometConfig.getInstance();
            String chatTpl = (cfgMsg != null ? cfgMsg.msgCometFallingChatCoords
                    : "[Comète] Une comète %tier% est tombée en %x% ; %y% ; %z%");
            boolean targetingOther = targetArg.provided(context);
            String targetName = targetingOther ? targetArg.get(context).getUsername() : null;
            String chatText = tier.applyTierPlaceholders(chatTpl)
                    .replace("%x%", Integer.toString(targetBlockPos.x))
                    .replace("%y%", Integer.toString(targetBlockPos.y))
                    .replace("%z%", Integer.toString(targetBlockPos.z))
                    + (spawnOnPlayer
                            ? (targetingOther ? " (directement au-dessus de " + targetName + " !)" : " (directement au-dessus de toi !)")
                            : "")
                    + (targetingOther ? " (Cible : " + targetName + ")" : "")
                    + (themeId != null ? " (Thème : " + themeNameStr + ")" : "");
            context.sendMessage(Message.raw(chatText));

            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent != null) {
                String titleT = (cfgMsg != null ? cfgMsg.msgCometFallingTitle : "Comète %tier% en chute !");
                String subT = (cfgMsg != null ? cfgMsg.msgCometFallingSubtitle : "Regarde le ciel !");
                Message primaryTitle = Message.raw(tier.applyTierPlaceholders(titleT));
                Message secondaryTitle = Message.raw(tier.applyTierPlaceholders(subT));

                EventTitleUtil.showEventTitleToPlayer(
                        playerRefComponent,
                        primaryTitle,
                        secondaryTitle,
                        true, // isMajor
                        null,
                        3.0F, // Show for 3 seconds
                        0.1F, // Fade in
                        0.5F // Fade out
                );

                com.varyon.comet.audio.CometWorldSounds.playCometFallingNotify(playerRefComponent);

                // Auto-hide after 3 seconds
                com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    world.execute(() -> {
                        EventTitleUtil.hideEventTitleFromPlayer(playerRefComponent, 0.0F);
                    });
                }, 3L, java.util.concurrent.TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            LOGGER.severe("Error in comet spawn command: " + e.getMessage());
            e.printStackTrace();
            context.sendMessage(Message.raw("Error: " + e.getMessage()));
        }
    }

    private boolean isInWater(World world, int x, int y, int z) {
        try {
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
            com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

            if (chunk == null) {
                chunk = world.getChunk(chunkIndex);
            }

            if (chunk == null) {
                return false;
            }

            return chunk.getFluidId(x, y, z) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int findGroundLevel(World world, int x, int z, int startY) {
        int searchStartY = 255;
        int minY = Math.max(0, startY - 150);

        long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
        com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

        for (int y = searchStartY; y >= minY; y--) {
            try {
                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType = null;

                if (chunk != null) {
                    blockType = chunk.getBlockType(x, y, z);
                } else {
                    blockType = world.getBlockType(x, y, z);
                }

                if (blockType != null) {
                    int blockId = com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap()
                            .getIndex(blockType.getId());

                    if (blockId != 0) {
                        Object material = blockType.getMaterial();
                        if (material != null) {
                            String materialStr = material.toString();
                            if (materialStr.equals("Solid") || materialStr.equals("Opaque")) {
                                return y;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Continue searching
            }
        }

        return -1;
    }

    private void spawnCometBlockDirectly(World world, Vector3i blockPos, Store<EntityStore> store, CometTier tier,
            String themeId, java.util.UUID ownerUUID, int zoneId) {
        world.execute(() -> {
            try {
                CometSpawnUtil.placeAndRegisterCometBlock(world, blockPos, store, tier, themeId, ownerUUID, zoneId);
            } catch (Exception e) {
                LOGGER.severe("Error spawning comet block: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static boolean isTier1Alias(String raw) {
        if (raw == null) {
            return false;
        }
        String n = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return n.equals("common")
                || n.equals("uncommon")
                || n.equals("commun")
                || n.equals("peu commun")
                || n.equals("peu-commun");
    }

}
