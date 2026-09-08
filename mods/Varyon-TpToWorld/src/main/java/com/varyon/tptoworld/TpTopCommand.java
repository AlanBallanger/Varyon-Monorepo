package com.varyon.tptoworld;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Rotation3fc;
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

public final class TpTopCommand extends CommandBase {

    private static final double HEIGHT_OFFSET = 10.0;

    private final RequiredArg<PlayerRef> playerArg;

    public TpTopCommand() {
        super("tptop", "Téléporte l'exécuteur 10 blocs au-dessus d'un joueur (/tptop <joueur>)");
        this.requirePermission("varyon.admin");
        this.playerArg = this.withRequiredArg("joueur", "Joueur au-dessus duquel se téléporter", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef sourceRef = resolveSender(context);
        if (sourceRef == null || !sourceRef.isValid()) {
            context.sendMessage(Message.raw("Cette commande doit être exécutée par un joueur.").color(Color.RED));
            return;
        }

        PlayerRef targetRef = context.get(playerArg);
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(Message.raw("Ce joueur n'est pas dans le monde.").color(Color.RED));
            return;
        }

        Ref<EntityStore> src = sourceRef.getReference();
        Store<EntityStore> srcStore = src != null && src.isValid() ? src.getStore() : null;
        if (srcStore == null) {
            context.sendMessage(Message.raw("Impossible de te téléporter pour le moment.").color(Color.RED));
            return;
        }

        Ref<EntityStore> dst = targetRef.getReference();
        Store<EntityStore> dstStore = dst != null && dst.isValid() ? dst.getStore() : null;
        World targetWorld = dstStore != null && dstStore.getExternalData() != null ? dstStore.getExternalData().getWorld() : null;
        if (dstStore == null || targetWorld == null) {
            context.sendMessage(Message.raw("Impossible de récupérer la position de ce joueur.").color(Color.RED));
            return;
        }

        String targetName = nameOf(targetRef);

        targetWorld.execute(() -> {
            TransformComponent targetTransform = dstStore.getComponent(dst, TransformComponent.getComponentType());
            if (targetTransform == null) {
                context.sendMessage(Message.raw("Impossible de récupérer la position de ce joueur.").color(Color.RED));
                return;
            }
            Vector3d destination = new Vector3d(targetTransform.getPosition()).add(0.0, HEIGHT_OFFSET, 0.0);
            Rotation3f rotation = copyRotation(targetTransform.getRotation());

            targetWorld.execute(() -> {
                if (!src.isValid()) {
                    return;
                }
                Teleport teleportComponent = Teleport.createForPlayer(targetWorld, destination, rotation);
                srcStore.addComponent(src, Teleport.getComponentType(), teleportComponent);
                sourceRef.sendMessage(Message.raw("Téléporté 10 blocs au-dessus de " + targetName + ".").color(Color.GREEN));
            });
        });
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
