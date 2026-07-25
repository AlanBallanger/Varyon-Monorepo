package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.varyon.VaryonPlugin;
import com.varyon.config.RtphConfig;
import com.varyon.teleport.RtpService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Random;
import java.util.logging.Level;

public class RtphCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService;
    private final Random random = new Random();

    public RtphCommand() {
        super("rtph", "Random teleport to the hinterlands (outside inner zone)");
        this.requirePermission("varyon.rtp");
        this.rtpService = new RtpService();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        RtphConfig config = VaryonPlugin.getStaticConfigManager().getRtphConfig();
        int outerMax = config.getOuterMax();
        int innerMax = config.getInnerMax();

        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw("World generation not supported in this world").color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        context.sendMessage(Message.raw("Téléportation vers les confins...").color(Color.GREEN));

        world.execute(() -> {
            try {
                double targetX = 0, targetZ = 0;
                boolean candidateFound = false;

                for (int i = 0; i < 100; i++) {
                    double x = random.nextDouble() * outerMax * 2 - outerMax;
                    double z = random.nextDouble() * outerMax * 2 - outerMax;
                    if (Math.abs(x) > innerMax || Math.abs(z) > innerMax) {
                        targetX = x;
                        targetZ = z;
                        candidateFound = true;
                        break;
                    }
                }

                if (!candidateFound) {
                    context.sendMessage(Message.raw("Impossible de générer une position valide dans les confins").color(Color.RED));
                    return;
                }

                Vector3d safePosition = rtpService.findSafePosition(world, generator, null, 50, targetX, targetZ);

                if (safePosition != null) {
                    teleportPlayer(store, ref, world, safePosition);
                    context.sendMessage(Message.raw("Téléporté vers les confins en " +
                        (int) safePosition.x + ", " + (int) safePosition.y + ", " + (int) safePosition.z).color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw("Impossible de trouver un emplacement sûr dans les confins").color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur lors de la téléportation RTPH: " + e.getMessage(), e);
                context.sendMessage(Message.raw("Échec de la téléportation").color(Color.RED));
            }
        });
    }

    private void teleportPlayer(Store<EntityStore> store, Ref<EntityStore> ref, World world, Vector3d position) {
        Teleport teleport = Teleport.createForPlayer(
            world,
            new org.joml.Vector3d(position.x, position.y, position.z),
            com.hypixel.hytale.math.vector.Rotation3f.ZERO
        );
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }
}
