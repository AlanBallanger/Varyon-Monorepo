package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.varyon.VaryonPlugin;
import com.varyon.config.RtpsConfig;
import com.varyon.teleport.RtpService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.logging.Level;

public class RtpsCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService = new RtpService();

    public RtpsCommand() {
        super("rtps", "Téléportation aléatoire en zone Hytale zone1, hors du carré central ([rtps])");
        this.requirePermission("varyon.rtps");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        RtpsConfig config = VaryonPlugin.getStaticConfigManager().getRtpsConfig();
        int minBlocks = config.getMinBlocks();
        int maxBlocks = config.getMaxBlocks();

        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw("World generation not supported in this world").color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        Zone[] zone1Zones = rtpService.resolveAllZonesByPrefix(generator, "zone1");
        if (zone1Zones == null || zone1Zones.length == 0) {
            context.sendMessage(Message.raw("Aucune zone Hytale « zone1 » pour ce monde.").color(Color.RED));
            return;
        }

        context.sendMessage(Message.raw("Téléportation aléatoire (zone1)...").color(Color.GREEN));

        world.execute(() -> {
            try {
                Vector3d safePosition = rtpService.findSafePositionOutsideInnerSquare(
                    world, generator, minBlocks, maxBlocks, RtpService.DEFAULT_RTP_MAX_ATTEMPTS, zone1Zones);

                if (safePosition != null) {
                    Teleport teleport = Teleport.createForPlayer(world, safePosition, new Vector3f(0, 0, 0));
                    store.addComponent(ref, Teleport.getComponentType(), teleport);
                    context.sendMessage(Message.raw("Téléporté en " +
                        (int) safePosition.x + ", " + (int) safePosition.y + ", " + (int) safePosition.z).color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw("Impossible de trouver un emplacement sûr").color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur lors de la téléportation RTPS: " + e.getMessage(), e);
                context.sendMessage(Message.raw("Échec de la téléportation").color(Color.RED));
            }
        });
    }
}
