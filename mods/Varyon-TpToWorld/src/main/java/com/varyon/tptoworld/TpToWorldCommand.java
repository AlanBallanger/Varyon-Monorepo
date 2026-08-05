package com.varyon.tptoworld;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import java.awt.Color;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TpToWorldCommand extends AbstractTargetPlayerCommand {

    private final RequiredArg<String> worldArg;
    private final OptionalArg<RelativeDoublePosition> positionArg;

    public TpToWorldCommand() {
        super("tptoworld", "Téléporter un joueur vers un autre monde (au spawn de ce monde, ou à une position donnée)");
        this.requirePermission("varyon.admin");
        this.worldArg = this.withRequiredArg("monde", "Nom du monde de destination", ArgTypes.STRING);
        this.positionArg = this.withOptionalArg("position", "Position X Y Z de destination (optionnel)", ArgTypes.RELATIVE_POSITION);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nullable Ref<EntityStore> sourceRef,
                           @Nonnull Ref<EntityStore> targetRef, @Nonnull PlayerRef playerRef,
                           @Nonnull World world, @Nonnull Store<EntityStore> store) {
        String worldName = context.get(worldArg);
        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            context.sendMessage(Message.raw("Monde introuvable : " + worldName).color(Color.RED));
            return;
        }

        RelativeDoublePosition position = context.get(positionArg);

        Teleport teleportComponent;
        if (position != null) {
            Vector3d destination = position.getRelativePosition(new Vector3d(0, 0, 0), targetWorld);
            teleportComponent = Teleport.createForPlayer(targetWorld, destination, Rotation3f.ZERO);
        } else {
            Transform spawnPoint = targetWorld.getWorldConfig().getSpawnProvider().getSpawnPoint(targetRef, store);
            if (spawnPoint == null) {
                context.sendMessage(Message.raw("Le monde de destination n'a pas de point de spawn.").color(Color.RED));
                return;
            }
            teleportComponent = Teleport.createForPlayer(targetWorld, spawnPoint);
        }

        store.addComponent(targetRef, Teleport.getComponentType(), teleportComponent);

        String targetName = playerRef.getUsername() != null ? playerRef.getUsername() : "";
        context.sendMessage(Message.raw(targetName + " téléporté vers le monde " + targetWorld.getName() + ".").color(Color.GREEN));
        playerRef.sendMessage(Message.raw("Tu as été téléporté vers le monde " + targetWorld.getName() + ".").color(Color.GREEN));
    }
}
