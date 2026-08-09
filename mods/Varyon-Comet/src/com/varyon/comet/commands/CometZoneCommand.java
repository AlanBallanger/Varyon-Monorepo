package com.varyon.comet.commands;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.varyon.comet.integration.VaryonZoneResolver;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.varyon.comet.config.model.ShardDropRange;
import com.varyon.comet.config.model.VaryonMineralBonus;
import com.varyon.comet.config.model.ZoneSpawnChances;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Command to check which zone the player is currently in
 */
public class CometZoneCommand extends AbstractWorldCommand {
    
    private static final Logger LOGGER = Logger.getLogger(CometZoneCommand.class.getName());
    
    public CometZoneCommand() {
        super("zone", "Check which zone you are currently in");
        requirePermission(CometPermissions.ZONE);
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
            
            // Get player's current zone
            WorldMapTracker tracker = player.getWorldMapTracker();
            
            // Force update zone info by getting player position and triggering zone check
            // The WorldMapTracker updates zone info periodically, but we can check it directly
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> playerRef =
                player.getReference();
            org.joml.Vector3d playerPos = null;
            if (playerRef != null && playerRef.isValid()) {
                com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform =
                    store.getComponent(playerRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                if (transform != null) {
                    playerPos = transform.getPosition();
                    if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                        LOGGER.info("Player position: " + playerPos + " - checking zone info");
                    }
                }
            }
            
            WorldMapTracker.ZoneDiscoveryInfo zoneInfo = tracker != null ? tracker.getCurrentZone() : null;
            
            if (zoneInfo == null) {
                context.sendMessage(Message.raw("You are not in any zone (or zone not detected yet)."));
                context.sendMessage(Message.raw("Try moving around a bit - zone detection may take a moment."));
                if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                    LOGGER.info("Player " + player.toString() + " has no zone info");
                }
                return;
            }
            
            String zoneName = zoneInfo.zoneName();
            String regionName = zoneInfo.regionName();

            // Same resolution comets use, so this command reports the ring rewards will actually be based on
            int varyonRing = playerPos != null
                    ? VaryonZoneResolver.resolveVaryonRingForComet(
                            playerPos.x, playerPos.z,
                            player.getWorld() != null ? player.getWorld().getName() : null, player)
                    : VaryonZoneResolver.resolveVaryonRing(player);
            int hytaleZone = VaryonZoneResolver.resolveHytaleZone(player);
            int worldMapRaw = VaryonZoneResolver.resolveWorldMapZoneIndexRaw(player);
            CometConfig cfgZones = CometConfig.getInstance();
            ShardDropRange shardRange = cfgZones != null ? cfgZones.getShardDropRangeForVaryonRingOrDefault(varyonRing) : null;
            VaryonMineralBonus mineralBonus = cfgZones != null ? cfgZones.getVaryonMineralBonusForRingOrNull(varyonRing) : null;

            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                LOGGER.info("Zone parsing - zoneName='" + zoneName + "', region='" + regionName + "', hytaleZone=" + hytaleZone
                        + ", worldMapRaw=" + worldMapRaw + ", varyonRing=" + varyonRing
                        + (shardRange != null ? ", shardMinMax=" + shardRange.min + "-" + shardRange.max : "")
                        + (mineralBonus != null ? ", mineral=" + mineralBonus.itemId() + "@" + mineralBonus.chance() : ""));
            }

            String tierInfo = getTierInfoForHytaleZone(hytaleZone);

            context.sendMessage(Message.raw("Current Zone: " + zoneName));
            context.sendMessage(Message.raw("Region: " + regionName));
            context.sendMessage(Message.raw("Hytale zone (1-4, tier roll weights): " + hytaleZone));
            context.sendMessage(Message.raw("World map index (raw, before clamp): " + worldMapRaw));
            if (varyonRing > 0) {
                context.sendMessage(Message.raw("Varyon ring (1-10, delays / wave scaling / shard table): " + varyonRing));
                if (shardRange != null) {
                    context.sendMessage(Message.raw("Shard fragments (min-max) for this ring: " + shardRange.min + "-" + shardRange.max));
                }
                if (mineralBonus != null) {
                    context.sendMessage(Message.raw("Optional mineral (one roll on chest): " + mineralBonus.itemId()
                            + " — " + (Math.round(mineralBonus.chance() * 1000.0) / 10.0) + "%"));
                }
            } else {
                context.sendMessage(Message.raw("Varyon ring: not available (tier rolls use Hytale zone only; shards use ring 1 range)."));
            }
            context.sendMessage(Message.raw("Comet Tier Distribution: " + tierInfo));

            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                LOGGER.info("Player " + player.toString() + " hytaleZone=" + hytaleZone + " varyonRing=" + varyonRing);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error in zone command: " + e.getMessage());
            e.printStackTrace();
            context.sendMessage(Message.raw("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Get tier distribution info for a zone
     */
    private String getTierInfoForHytaleZone(int hytaleZone) {
        CometConfig config = CometConfig.getInstance();
        if (config == null) {
            return "Config unavailable";
        }

        ZoneSpawnChances chances = config.getHytaleZoneSpawnChances(hytaleZone);
        if (chances == null) {
            return "No spawn chances configured for this Hytale zone";
        }

        List<String> parts = new ArrayList<>();
        if (CometConfig.isTier5Enabled()) {
            appendTierPart(parts, "Common", chances.getTier1());
            appendTierPart(parts, "Rare", chances.getTier2());
            appendTierPart(parts, "Epic", chances.getTier3());
            appendTierPart(parts, "Legendary", chances.getTier4());
            appendTierPart(parts, "Mythic", chances.getTier5());
            return parts.isEmpty() ? "No active tiers in this zone" : String.join(", ", parts);
        }

        double t1 = chances.getTier1();
        double t2 = chances.getTier2();
        double t3 = chances.getTier3();
        double t4 = chances.getTier4();
        double totalWithoutMythic = t1 + t2 + t3 + t4;

        if (totalWithoutMythic <= 0.0) {
            return "No active tiers in this zone (Tier 5 disabled)";
        }

        appendTierPart(parts, "Common", t1 / totalWithoutMythic);
        appendTierPart(parts, "Rare", t2 / totalWithoutMythic);
        appendTierPart(parts, "Epic", t3 / totalWithoutMythic);
        appendTierPart(parts, "Legendary", t4 / totalWithoutMythic);

        String info = parts.isEmpty() ? "No active tiers in this zone" : String.join(", ", parts);
        if (chances.getTier5() > 0.0) {
            info += " (Mythic disabled: Endgame&QoL missing)";
        }
        return info;
    }

    private void appendTierPart(List<String> parts, String name, double chance) {
        if (chance <= 0.0) {
            return;
        }
        parts.add(String.format(Locale.ROOT, "%.0f%% %s", chance * 100.0, name));
    }
}
