package com.varyon.tptoworld;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Rotation3fc;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TpTempCommand extends CommandBase {

    private static final int COUNTDOWN_SECONDS = 3;

    public TpTempCommand() {
        super("tptemp", "Reviens à ta position d'origine après une téléportation temporaire (/tptemp)");
        this.addUsageVariant(new ReleaseVariant());
        this.addUsageVariant(new TeleportVariant());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef playerRef = resolveSender(context);
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(Message.raw("Seul un joueur en jeu peut utiliser cette commande.").color(Color.RED));
            return;
        }
        selfReturn(context, playerRef);
    }

    private static final class ReleaseVariant extends CommandBase {
        private final RequiredArg<PlayerRef> playerArg;

        ReleaseVariant() {
            super("Renvoie un joueur téléporté temporairement (/tptemp <joueur>) vers sa position d'origine");
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("joueur", "Joueur à renvoyer", ArgTypes.PLAYER_REF);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            PlayerRef playerRef = context.get(playerArg);
            if (playerRef == null || !playerRef.isValid()) {
                context.sendMessage(Message.raw("Ce joueur n'est pas dans le monde.").color(Color.RED));
                return;
            }
            release(context, playerRef);
        }
    }

    private static final class TeleportVariant extends CommandBase {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<PlayerRef> destinationArg;

        TeleportVariant() {
            super("Téléporte temporairement un joueur vers un autre (position exacte)");
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("joueur", "Joueur à téléporter", ArgTypes.PLAYER_REF);
            this.destinationArg = this.withRequiredArg("destination", "Joueur vers qui téléporter", ArgTypes.PLAYER_REF);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            PlayerRef playerRef = context.get(playerArg);
            PlayerRef destinationRef = context.get(destinationArg);
            if (playerRef == null || !playerRef.isValid()) {
                context.sendMessage(Message.raw("Ce joueur n'est pas dans le monde.").color(Color.RED));
                return;
            }
            if (destinationRef == null || !destinationRef.isValid()) {
                context.sendMessage(Message.raw("Le joueur de destination n'est pas dans le monde.").color(Color.RED));
                return;
            }
            teleport(context, playerRef, destinationRef);
        }
    }

    private static void teleport(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef, @Nonnull PlayerRef destinationRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        World originWorld = store != null && store.getExternalData() != null ? store.getExternalData().getWorld() : null;
        if (store == null || originWorld == null) {
            context.sendMessage(Message.raw("Impossible de récupérer la position actuelle de ce joueur.").color(Color.RED));
            return;
        }

        Ref<EntityStore> destRef = destinationRef.getReference();
        Store<EntityStore> destStore = destRef != null && destRef.isValid() ? destRef.getStore() : null;
        World destWorld = destStore != null && destStore.getExternalData() != null ? destStore.getExternalData().getWorld() : null;
        if (destStore == null || destWorld == null) {
            context.sendMessage(Message.raw("Impossible de récupérer la position du joueur de destination.").color(Color.RED));
            return;
        }

        String playerName = nameOf(playerRef);
        String destinationName = nameOf(destinationRef);
        PlayerRef initiator = resolveSender(context);
        UUID initiatorUuid = initiator != null ? initiator.getUuid() : null;
        String initiatorName = initiator != null ? nameOf(initiator) : null;

        destWorld.execute(() -> {
            TransformComponent destTransform = destStore.getComponent(destRef, TransformComponent.getComponentType());
            if (destTransform == null) {
                context.sendMessage(Message.raw("Impossible de récupérer la position du joueur de destination.").color(Color.RED));
                return;
            }
            Vector3d destPosition = new Vector3d(destTransform.getPosition());
            Rotation3f destRotation = copyRotation(destTransform.getRotation());

            originWorld.execute(() -> {
                if (!ref.isValid()) {
                    return;
                }
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) {
                    context.sendMessage(Message.raw("Impossible de récupérer la position actuelle de ce joueur.").color(Color.RED));
                    return;
                }
                Vector3d originPosition = new Vector3d(transform.getPosition());
                Rotation3f originRotation = copyRotation(transform.getRotation());
                TempTeleportManager.save(playerRef.getUuid(),
                        new TempTeleportManager.SavedLocation(originWorld, originPosition, originRotation,
                                initiatorUuid, initiatorName));

                context.sendMessage(Message.raw(playerName + " téléporté temporairement vers " + destinationName + ".").color(Color.GREEN));

                Runnable doTeleport = () -> originWorld.execute(() -> {
                    if (!ref.isValid()) {
                        return;
                    }
                    Teleport teleportComponent = Teleport.createForPlayer(destWorld, destPosition, destRotation);
                    store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
                    playerRef.sendMessage(Message.raw("Tu as été téléporté temporairement vers " + destinationName + ".").color(Color.GREEN));
                    playerRef.sendMessage(Message.raw("Fais /tptemp à tout moment pour revenir à ta position d'origine.").color(Color.YELLOW));
                });

                runWithCountdown(playerRef, "Téléportation vers " + destinationName, doTeleport);
            });
        });
    }

    private static void selfReturn(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef) {
        TempTeleportManager.SavedLocation saved = TempTeleportManager.remove(playerRef.getUuid());
        if (saved == null) {
            context.sendMessage(Message.raw("Tu n'as pas de téléportation temporaire en cours.").color(Color.RED));
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        World currentWorld = store != null && store.getExternalData() != null ? store.getExternalData().getWorld() : null;
        if (store == null || currentWorld == null) {
            context.sendMessage(Message.raw("Impossible de te renvoyer à ta position d'origine pour le moment.").color(Color.RED));
            TempTeleportManager.save(playerRef.getUuid(), saved);
            return;
        }

        String playerName = nameOf(playerRef);
        performReturn(playerRef, ref, store, currentWorld, saved);
        notifyInitiator(saved, playerName + " est reparti à sa position d'origine.");
    }

    private static void release(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef) {
        TempTeleportManager.SavedLocation saved = TempTeleportManager.remove(playerRef.getUuid());
        if (saved == null) {
            context.sendMessage(Message.raw(nameOf(playerRef) + " n'a pas de téléportation temporaire en cours.").color(Color.RED));
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        World currentWorld = store != null && store.getExternalData() != null ? store.getExternalData().getWorld() : null;
        if (store == null || currentWorld == null) {
            context.sendMessage(Message.raw("Impossible de renvoyer ce joueur pour le moment.").color(Color.RED));
            TempTeleportManager.save(playerRef.getUuid(), saved);
            return;
        }

        String playerName = nameOf(playerRef);
        context.sendMessage(Message.raw(playerName + " a été renvoyé à sa position d'origine.").color(Color.GREEN));
        performReturn(playerRef, ref, store, currentWorld, saved);
    }

    private static void performReturn(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> ref,
                                      @Nonnull Store<EntityStore> store, @Nonnull World currentWorld,
                                      @Nonnull TempTeleportManager.SavedLocation saved) {
        Runnable doTeleport = () -> currentWorld.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            Teleport teleportComponent = Teleport.createForPlayer(saved.world(), saved.position(), saved.rotation());
            store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
            playerRef.sendMessage(Message.raw("Tu as été renvoyé à ta position d'origine.").color(Color.GREEN));
        });

        runWithCountdown(playerRef, "Retour à ta position d'origine", doTeleport);
    }

    private static void notifyInitiator(@Nonnull TempTeleportManager.SavedLocation saved, @Nonnull String message) {
        UUID initiatorUuid = saved.initiatorUuid();
        if (initiatorUuid == null) {
            return;
        }
        Universe universe = Universe.get();
        PlayerRef initiator = universe != null ? universe.getPlayer(initiatorUuid) : null;
        if (initiator != null && initiator.isValid()) {
            initiator.sendMessage(Message.raw(message).color(Color.YELLOW));
        }
    }

    private static void runWithCountdown(@Nonnull PlayerRef playerRef, @Nonnull String label, @Nonnull Runnable teleport) {
        if (isOp(playerRef)) {
            teleport.run();
            return;
        }

        playerRef.sendMessage(Message.raw(label + " dans " + COUNTDOWN_SECONDS + " secondes...").color(Color.YELLOW));
        for (int i = COUNTDOWN_SECONDS; i >= 1; i--) {
            final int remaining = i;
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                if (playerRef.isValid()) {
                    playerRef.sendMessage(Message.raw(String.valueOf(remaining) + "...").color(Color.YELLOW));
                }
            }, (long) (COUNTDOWN_SECONDS - remaining), TimeUnit.SECONDS);
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(teleport, COUNTDOWN_SECONDS, TimeUnit.SECONDS);
    }

    private static boolean isOp(@Nonnull PlayerRef playerRef) {
        return playerRef.hasPermission("*") || playerRef.hasPermission("varyon.admin");
    }

    @Nullable
    private static PlayerRef resolveSender(@Nonnull CommandContext context) {
        if (!context.isPlayer() || context.sender() == null) {
            return null;
        }
        UUID uuid = context.sender().getUuid();
        if (uuid == null) {
            return null;
        }
        Universe universe = Universe.get();
        return universe != null ? universe.getPlayer(uuid) : null;
    }

    @Nonnull
    private static Rotation3f copyRotation(@Nullable Rotation3fc rotation) {
        if (rotation == null) {
            return new Rotation3f(0f, 0f, 0f);
        }
        return new Rotation3f(rotation.pitch(), rotation.yaw(), rotation.roll());
    }

    @Nonnull
    private static String nameOf(@Nonnull PlayerRef playerRef) {
        String name = playerRef.getUsername();
        return name != null ? name : "?";
    }
}
