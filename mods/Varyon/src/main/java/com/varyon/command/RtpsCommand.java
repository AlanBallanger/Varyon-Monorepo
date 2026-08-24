package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.varyon.VaryonPlugin;
import com.varyon.config.RtpsConfig;
import com.varyon.teleport.RtpService;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class RtpsCommand extends AbstractAsyncCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService = new RtpService();

    public RtpsCommand() {
        super("rtps", "Téléportation aléatoire en zone Hytale zone1, hors du carré central ([rtps])");
        this.requirePermission("varyon.rtps");
        this.addUsageVariant(new TargetVariant());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        CommandSender sender = context.sender();
        if (!(sender instanceof PlayerRef playerRef)) {
            context.sendMessage(Message.raw("Cette commande doit être exécutée par un joueur, ou précisez /rtps <joueur>.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        teleport(context, playerRef);
        return CompletableFuture.completedFuture(null);
    }

    private class TargetVariant extends CommandBase {
        private final RequiredArg<PlayerRef> playerArg;

        TargetVariant() {
            super("Téléportation aléatoire (zone1) d'un joueur cible, hors du carré central");
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("joueur", "Nom du joueur à téléporter", ArgTypes.PLAYER_REF);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            PlayerRef target = context.get(playerArg);
            teleport(context, target);
        }
    }

    private void teleport(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            context.sendMessage(Message.raw("Joueur non connecté au monde.").color(Color.RED));
            return;
        }
        Store<EntityStore> store = ref.getStore();
        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("Monde indisponible.").color(Color.RED));
            return;
        }

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

        context.sendMessage(Message.raw("Téléportation aléatoire (zone1) de " + playerRef.getUsername() + "...").color(Color.GREEN));

        world.execute(() -> {
            try {
                Vector3d safePosition = rtpService.findSafePositionOutsideInnerSquare(
                    world, generator, minBlocks, maxBlocks, RtpService.DEFAULT_RTP_MAX_ATTEMPTS, zone1Zones);

                if (safePosition != null) {
                    Teleport teleport = Teleport.createForPlayer(world, new org.joml.Vector3d(safePosition.x, safePosition.y, safePosition.z), com.hypixel.hytale.math.vector.Rotation3f.ZERO);
                    store.addComponent(ref, Teleport.getComponentType(), teleport);
                    context.sendMessage(Message.raw(playerRef.getUsername() + " téléporté en " +
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
