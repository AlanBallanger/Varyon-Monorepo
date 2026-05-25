package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.MessagesConfig;
import com.varyon.config.ReturnConfig;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.death.DeathPointManager;
import com.varyon.portal.ReturnConfirmUIPage;
import com.varyon.util.VaryonWorldAccess;
import com.varyon.util.ZoneCalculator;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.Random;
import java.util.logging.Level;

public class ReturnCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PERM_USE = "varyon.return";
    private static final int START_Y = 200;
    private static final int MIN_Y = 0;
    private static final Random random = new Random();

    public ReturnCommand() {
        super("return", "Ouvre la confirmation de retour près de votre point de mort");
        this.setPermissionGroups("adventure");
        this.requirePermission(PERM_USE);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        DeathPointManager deathManager = VaryonPlugin.getInstance().getDeathPointManager();
        if (deathManager == null) {
            context.sendMessage(Message.raw("Système de retour indisponible.").color(Color.RED));
            return;
        }

        if (!VaryonWorldAccess.isVaryonEnabledWorld(world)) {
            context.sendMessage(Message.raw("Cette commande n'est disponible que sur les mondes Varyon.").color(Color.RED));
            return;
        }

        ReturnConfig config = VaryonPlugin.getStaticConfigManager().getReturnConfig();
        MessagesConfig.ReturnMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getReturn();

        if (!config.isEnabled()) {
            context.sendMessage(Message.raw("Le système de retour est désactivé.").color(Color.RED));
            return;
        }

        DeathPointManager.DeathPoint deathPoint = deathManager.getDeathPoint(playerRef.getUuid());
        if (deathPoint == null) {
            context.sendMessage(Message.raw(msg.noDeathPoint).color(Color.RED));
            return;
        }

        if (deathPoint.isExpired(config.getExpirationMinutes())) {
            deathManager.removeDeathPoint(playerRef.getUuid());
            context.sendMessage(Message.raw(msg.expired).color(Color.RED));
            return;
        }

        if (deathPoint.isUsed()) {
            context.sendMessage(Message.raw(msg.alreadyUsed).color(Color.YELLOW));
            return;
        }

        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        DifficultyZone zone = ZoneCalculator.getZoneAtPosition(deathPoint.getX(), deathPoint.getZ(), deathPoint.getWorld(), zoneConfig);
        int baseCost = zone != null ? zone.getTeleportCost() : 0;
        int multiplier = deathManager.getReturnCostMultiplier(playerRef.getUuid());
        int totalCost = baseCost * multiplier;
        BigDecimal costBD = BigDecimal.valueOf(totalCost);

        if (rtpvConfig.isEconomyEnabled()) {
            try {
                if (!hasEnoughBalance(playerRef, costBD)) {
                    BigDecimal balance = getBalance(playerRef);
                    context.sendMessage(Message.raw(
                        "Coins insuffisants. Coût : " + totalCost + " | Solde : " + balance.intValue()
                    ).color(Color.RED));
                    return;
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible, vérification économie ignorée");
            }
        }

        String deathPointMsg = msg.deathPointInfo
            .replace("{world}", deathPoint.getWorld())
            .replace("{x}", String.valueOf((int) deathPoint.getX()))
            .replace("{y}", String.valueOf((int) deathPoint.getY()))
            .replace("{z}", String.valueOf((int) deathPoint.getZ()));
        context.sendMessage(Message.raw(deathPointMsg).color(Color.GREEN));

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
            new ReturnConfirmUIPage(playerRef, totalCost, multiplier, rtpvConfig.isEconomyEnabled()));
    }

    public void confirmReturnFromUi(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        DeathPointManager deathManager = VaryonPlugin.getInstance().getDeathPointManager();
        if (deathManager == null) {
            return;
        }

        if (!VaryonWorldAccess.isVaryonEnabledWorld(world)) {
            return;
        }

        ReturnConfig config = VaryonPlugin.getStaticConfigManager().getReturnConfig();
        MessagesConfig.ReturnMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getReturn();
        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager().getZoneConfig();

        if (!config.isEnabled()) {
            return;
        }

        DeathPointManager.DeathPoint deathPoint = deathManager.getDeathPoint(playerRef.getUuid());
        if (deathPoint == null) {
            playerRef.sendMessage(Message.raw(msg.noDeathPoint).color(Color.RED));
            return;
        }
        if (deathPoint.isExpired(config.getExpirationMinutes())) {
            deathManager.removeDeathPoint(playerRef.getUuid());
            playerRef.sendMessage(Message.raw(msg.expired).color(Color.RED));
            return;
        }
        if (deathPoint.isUsed()) {
            playerRef.sendMessage(Message.raw(msg.alreadyUsed).color(Color.YELLOW));
            return;
        }

        DifficultyZone zone = ZoneCalculator.getZoneAtPosition(deathPoint.getX(), deathPoint.getZ(), deathPoint.getWorld(), zoneConfig);
        int baseCost = zone != null ? zone.getTeleportCost() : 0;
        int multiplier = deathManager.getReturnCostMultiplier(playerRef.getUuid());
        int totalCost = baseCost * multiplier;
        BigDecimal costBD = BigDecimal.valueOf(totalCost);

        if (rtpvConfig.isEconomyEnabled()) {
            try {
                if (!hasEnoughBalance(playerRef, costBD)) {
                    BigDecimal balance = getBalance(playerRef);
                    playerRef.sendMessage(Message.raw(
                        "Coins insuffisants. Coût : " + totalCost + " | Solde : " + balance.intValue()
                    ).color(Color.RED));
                    return;
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible, vérification économie ignorée");
            }
        }

        deathManager.markDeathPointUsed(playerRef.getUuid());
        deathManager.recordReturnUse(playerRef.getUuid(), config.getCooldownSeconds());

        if (rtpvConfig.isEconomyEnabled()) {
            try {
                withdrawBalance(playerRef, costBD, totalCost);
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible, déduction ignorée");
            }
        }

        LOGGER.at(Level.INFO).log("Return confirmed from UI for " + playerRef.getUuid() +
            " zone=" + (zone != null ? zone.getName() : "none") +
            " cost=" + totalCost + " multiplier=x" + multiplier);

        world.execute(() -> {
            try {
                Vector3d targetPos = findSafePositionNearDeath(world, deathPoint,
                    config.getMinDistance(), config.getMaxDistance(), 30);

                if (targetPos == null) {
                    playerRef.sendMessage(Message.raw(msg.noSafeLocation
                        .replace("{attempts}", "30")).color(Color.RED));
                    return;
                }

                Teleport teleport = Teleport.createForPlayer(world, targetPos, new Vector3f(0, 0, 0));
                store.addComponent(ref, Teleport.getComponentType(), teleport);

                double distance = Math.sqrt(
                    Math.pow(targetPos.x - deathPoint.getX(), 2) +
                    Math.pow(targetPos.z - deathPoint.getZ(), 2));
                String successMsg = msg.success
                    .replace("{distance}", String.valueOf((int) distance))
                    .replace("{x}", String.valueOf((int) targetPos.x))
                    .replace("{y}", String.valueOf((int) targetPos.y))
                    .replace("{z}", String.valueOf((int) targetPos.z));
                String costSuffix = rtpvConfig.isEconomyEnabled()
                    ? " (-" + totalCost + " coins)" : "";
                playerRef.sendMessage(Message.raw(successMsg + costSuffix).color(Color.GREEN));

                LOGGER.at(Level.INFO).log("Player " + playerRef.getUuid() + " teleported to " +
                    (int) targetPos.x + "," + (int) targetPos.y + "," + (int) targetPos.z);

            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Error during return teleport: " + e.getMessage(), e);
                playerRef.sendMessage(Message.raw(msg.error).color(Color.RED));
            }
        });
    }

    private boolean hasEnoughBalance(PlayerRef playerRef, BigDecimal cost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return true;
        return economy.has("Varyon", playerRef.getUuid(), cost);
    }

    private BigDecimal getBalance(PlayerRef playerRef) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return BigDecimal.ZERO;
        return economy.getBalance("Varyon", playerRef.getUuid());
    }

    private void withdrawBalance(PlayerRef playerRef, BigDecimal cost, int finalCost) {
        Economy economy = VaultUnlockedServicesManager.get().economyObj();
        if (economy == null || !economy.isEnabled()) return;
        EconomyResponse response = economy.withdraw("Varyon", playerRef.getUuid(), cost);
        if (!response.transactionSuccess()) {
            LOGGER.at(Level.WARNING).log("Failed to deduct " + finalCost + " coins from " + playerRef.getUuid() + ": " + response.errorMessage);
        }
    }

    @Nullable
    private Vector3d findSafePositionNearDeath(@Nonnull World world, @Nonnull DeathPointManager.DeathPoint deathPoint,
                                                int minDist, int maxDist, int maxAttempts) {
        double deathX = deathPoint.getX();
        double deathZ = deathPoint.getZ();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDist + random.nextDouble() * (maxDist - minDist);

            int targetX = (int) (deathX + Math.cos(angle) * distance);
            int targetZ = (int) (deathZ + Math.sin(angle) * distance);

            Double safeY = findSafeY(world, targetX, targetZ);
            if (safeY != null) {
                return new Vector3d(targetX, safeY, targetZ);
            }
        }
        return null;
    }

    @Nullable
    private Double findSafeY(@Nonnull World world, int x, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            return null;
        }

        for (int checkY = START_Y; checkY >= MIN_Y; checkY--) {
            try {
                if (hasFluid(chunk, x, checkY, z)) {
                    continue;
                }

                if (isSolidBlock(chunk, x, checkY, z)) {
                    int spawnY = checkY + 1;

                    if (hasFluid(chunk, x, spawnY, z) || hasFluid(chunk, x, spawnY + 1, z)) {
                        continue;
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

    private boolean isSolidBlock(@Nonnull WorldChunk chunk, int x, int y, int z) {
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

    private boolean hasFluid(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            return chunk.getFluidId(x, y, z) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
