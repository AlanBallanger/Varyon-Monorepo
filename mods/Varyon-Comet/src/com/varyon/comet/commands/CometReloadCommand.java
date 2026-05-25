package com.varyon.comet.commands;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.integration.ClaimProtectionGuard;
import com.varyon.comet.spawn.CometSpawnTask;
import com.varyon.comet.services.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Logger;

/**
 * Command to reload the Comet Mod configuration from file.
 * Usage: /comet reload
 */
public class CometReloadCommand extends AbstractWorldCommand {

    private static final Logger LOGGER = Logger.getLogger(CometReloadCommand.class.getName());

    public CometReloadCommand() {
        super("reload", "Reloads the Comet Mod configuration from file");
        requirePermission(CometPermissions.RELOAD);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store) {

        try {
            context.sendMessage(Message.raw("Reloading Comet Mod configuration..."));

            // Reload config
            CometConfig config = CometConfig.reload();

            // Apply spawn settings to spawn task
            CometSpawnTask spawnTask = CometModPlugin.getSpawnTask();
            if (spawnTask != null) {
                config.applyToSpawnTask(spawnTask);
            }

            // Apply despawn time
            CometFallingSystem.setDespawnTimeMinutes(config.despawnTimeMinutes);

            // Report results
            StringBuilder sb = new StringBuilder();
            sb.append("Configuration reloaded!\n");
            sb.append("Spawn Settings:\n");
            sb.append("  - Delay base (zone ").append(config.zoneDelayReferenceZone).append("): ")
                    .append(config.minDelaySeconds).append("-").append(config.maxDelaySeconds).append("s\n");
            if (config.varyonZoneDelayScalingEnabled) {
                sb.append("  - Delay eff. Varyon z1: ")
                        .append(config.getEffectiveNaturalSpawnMinDelaySeconds(1)).append("-")
                        .append(config.getEffectiveNaturalSpawnMaxDelaySeconds(1)).append("s\n");
                sb.append("  - Delay eff. Varyon z").append(config.zoneDelayFastestZone).append(": ")
                        .append(config.getEffectiveNaturalSpawnMinDelaySeconds(config.zoneDelayFastestZone)).append("-")
                        .append(config.getEffectiveNaturalSpawnMaxDelaySeconds(config.zoneDelayFastestZone))
                        .append("s (").append(config.zoneDelaySpeedMultiplierAtFastest).append("x faster vs z")
                        .append(config.zoneDelayReferenceZone).append(")\n");
            }
            sb.append("  - Chance: ").append((int) (config.spawnChance * 100)).append("%\n");
            sb.append("  - Distance: ").append(config.minSpawnDistance).append("-").append(config.maxSpawnDistance)
                    .append(" blocks\n");
            sb.append("  - Despawn: ").append(config.despawnTimeMinutes).append(" min\n");
            List<String> activeClaimProviders = ClaimProtectionGuard.getResolvedProviderKeys(config);
            sb.append("Claim Protect: enabled=").append(config.isClaimProtectEnabled())
                    .append(", autoDetect=").append(config.isClaimProtectAutoDetectProviders())
                    .append(", activeProviders=")
                    .append(activeClaimProviders.isEmpty() ? "none" : String.join(", ", activeClaimProviders))
                    .append("\n");
            sb.append("Comet mobs: ").append(config.getThemeCount()).append(" loaded\n");

            if (!config.hasThemes()) {
                sb.append("\nWARNING: No comet mob entries defined! Waves will not work!");
            }

            context.sendMessage(Message.raw(sb.toString()));
            LOGGER.info("Configuration reloaded via command");

        } catch (Exception e) {
            context.sendMessage(Message.raw("Error reloading config: " + e.getMessage()));
            LOGGER.severe("Error reloading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
