package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.varyon.VaryonPlugin;
import com.varyon.config.MessagesConfig;
import com.varyon.teleport.RtpService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class RtpzCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService;
    private final Random random = new Random();
    private final RequiredArg<String> zoneArg;

    public RtpzCommand() {
        super("rtpz", "Random teleport to a vanilla zone");
        this.requirePermission("varyon.rtp");
        this.rtpService = new RtpService();
        this.zoneArg = this.withRequiredArg(
            "zone",
            "Zone to teleport to (zone1, zone2, zone3, zone4)",
            ArgTypes.STRING
        );
        this.addUsageVariant(new NoArgVariant());
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String targetZonePrefix = context.get(zoneArg);
        teleportToRandomZone(context, store, ref, playerRef, world, targetZonePrefix);
    }

    private class NoArgVariant extends CommandBase {
        public NoArgVariant() {
            super("Téléportation aléatoire vers une zone random");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.raw("Cette commande doit être exécutée par un joueur.").color(Color.RED));
                return;
            }
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            if (ref == null) return;
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            teleportToRandomZone(context, store, ref, null, world, null);
        }
    }

    void teleportToRandomZone(CommandContext context, Store<EntityStore> store, 
                              Ref<EntityStore> ref, PlayerRef playerRef, World world, String targetZonePrefix) {
        MessagesConfig.RtpMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getRtp();
        
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw(msg.worldNotSupported).color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        Zone[] zones = generator.getZonePatternProvider().getZones();

        Zone zoneForTeleport = null;

        if (targetZonePrefix != null && !targetZonePrefix.isEmpty()) {
            String prefix = targetZonePrefix.toLowerCase();

            List<Zone> matchingZones = new ArrayList<>();
            for (Zone zone : zones) {
                String zoneName = zone.name().toLowerCase();
                if (zoneName.startsWith(prefix) && !zoneName.contains("ocean")) {
                    matchingZones.add(zone);
                }
            }

            if (matchingZones.isEmpty()) {
                for (Zone zone : zones) {
                    if (zone.name().toLowerCase().startsWith(prefix)) {
                        matchingZones.add(zone);
                    }
                }
            }

            if (matchingZones.isEmpty()) {
                StringBuilder availableZones = new StringBuilder(msg.availableZones + ": ");
                for (int i = 0; i < zones.length; i++) {
                    availableZones.append(zones[i].name());
                    if (i < zones.length - 1) availableZones.append(", ");
                }
                context.sendMessage(Message.raw(msg.zoneNotFound
                    .replace("{zone}", prefix) + " " + availableZones.toString()).color(Color.RED));
                return;
            }

            zoneForTeleport = matchingZones.get(random.nextInt(matchingZones.size()));
            String zoneDisplay = formatHytaleZoneLabel(zoneForTeleport.name());
            LOGGER.at(Level.INFO).log("RTPZ vanilla: " + zoneForTeleport.name() + " (" + zoneDisplay + ") — "
                + matchingZones.size() + " match(es) for prefix: " + prefix);
        } else {
            List<Zone> z1to4 = listHytaleZonesWithIndexInRange(zones, 1, 4);
            if (z1to4.isEmpty()) {
                context.sendMessage(Message.raw(
                    "Aucune zone Hytale (zone1 à zone4) détectée pour ce monde."
                ).color(Color.RED));
                return;
            }
            zoneForTeleport = z1to4.get(random.nextInt(z1to4.size()));
            LOGGER.at(Level.INFO).log("RTPZ vanilla (sans arg): " + zoneForTeleport.name());
        }

        final Integer extractedZoneNumber = parseZoneIndexFromHytaleName(zoneForTeleport.name());

        // Vérifier la permission (numéro dérivé du nom de zone Hytale, pas des zones Varyon)
        if (extractedZoneNumber != null) {
            com.hypixel.hytale.server.core.entity.entities.Player player = 
                (com.hypixel.hytale.server.core.entity.entities.Player) store.getComponent(ref, 
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            
            if (player != null && !player.hasPermission("varyon.rtp")) {
                // Pas de permission globale, vérifier les permissions granulaires
                boolean hasAccess = false;
                for (int i = extractedZoneNumber; i <= 10; i++) {
                    if (player.hasPermission("varyon.rtp." + i)) {
                        hasAccess = true;
                        break;
                    }
                }
                
                if (!hasAccess) {
                    context.sendMessage(Message.raw("Vous n'avez pas la permission pour cette zone. Permission requise: varyon.rtp." + extractedZoneNumber).color(Color.RED));
                    return;
                }
            }
        }

        final Zone finalZoneForTeleport = zoneForTeleport;
        context.sendMessage(Message.raw(msg.teleporting).color(Color.GREEN));

        world.execute(() -> {
            try {
                Vector3d safePosition = rtpService.findSafePosition(
                    world, generator, finalZoneForTeleport, RtpService.DEFAULT_RTP_MAX_ATTEMPTS);

                if (safePosition != null) {
                    teleportPlayer(store, ref, world, safePosition);
                    String zoneName = formatHytaleZoneLabel(finalZoneForTeleport.name());
                    context.sendMessage(Message.raw(msg.success
                        .replace("{zone}", zoneName)
                        .replace("{x}", String.valueOf((int)safePosition.x))
                        .replace("{y}", String.valueOf((int)safePosition.y))
                        .replace("{z}", String.valueOf((int)safePosition.z))).color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw(msg.noSafeLocation
                        .replace("{attempts}", String.valueOf(RtpService.DEFAULT_RTP_MAX_ATTEMPTS))).color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur lors de la téléportation RTP: " + e.getMessage(), e);
                context.sendMessage(Message.raw(msg.error).color(Color.RED));
            }
        });
    }

    private static List<Zone> listHytaleZonesWithIndexInRange(Zone[] zones, int minIncl, int maxIncl) {
        List<Zone> out = new ArrayList<>();
        for (Zone z : zones) {
            Integer n = parseZoneIndexFromHytaleName(z.name());
            if (n != null && n >= minIncl && n <= maxIncl) {
                out.add(z);
            }
        }
        return out;
    }

    @Nullable
    private static Integer parseZoneIndexFromHytaleName(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.toLowerCase().trim();
        int idx = s.indexOf("zone");
        if (idx < 0) {
            return null;
        }
        int i = idx + 4;
        while (i < s.length() && (s.charAt(i) == '_' || s.charAt(i) == ' ' || s.charAt(i) == '-')) {
            i++;
        }
        if (i >= s.length() || !Character.isDigit(s.charAt(i))) {
            return null;
        }
        int end = i;
        while (end < s.length() && Character.isDigit(s.charAt(end))) {
            end++;
        }
        try {
            return Integer.parseInt(s.substring(i, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nonnull
    private static String formatHytaleZoneLabel(String zoneName) {
        Integer n = parseZoneIndexFromHytaleName(zoneName);
        if (n != null) {
            return "Zone " + n;
        }
        return zoneName;
    }

    private void teleportPlayer(Store<EntityStore> store, Ref<EntityStore> ref, World world, Vector3d position) {
        Teleport teleport = Teleport.createForPlayer(
            world,
            position,
            new Vector3f(0, 0, 0)
        );
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }
}
