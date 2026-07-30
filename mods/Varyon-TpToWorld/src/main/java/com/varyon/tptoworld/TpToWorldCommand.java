package com.varyon.tptoworld;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.math.vector.Transform;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TpToWorldCommand extends AbstractAsyncCommand {

    private final RequiredArg<PlayerRef> playerArg;
    private final RequiredArg<String> worldArg;

    public TpToWorldCommand() {
        super("tptoworld", "Téléporter un joueur vers un autre monde (au spawn de ce monde)");
        this.requirePermission("varyon.admin");
        this.playerArg = this.withRequiredArg("joueur", "Nom du joueur", ArgTypes.PLAYER_REF);
        this.worldArg = this.withRequiredArg("monde", "Nom du monde de destination", ArgTypes.GREEDY_STRING);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        PlayerRef targetRef = context.get(playerArg);
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(Message.raw("Joueur introuvable ou hors ligne.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        String worldName = context.get(worldArg);
        Universe universe = Universe.get();
        if (universe == null) {
            context.sendMessage(Message.raw("Univers indisponible.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Map<String, World> worlds = universe.getWorlds();
        World targetWorld = worlds != null ? worlds.get(worldName) : null;
        if (targetWorld == null) {
            context.sendMessage(Message.raw("Monde introuvable : " + worldName).color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> targetEntityRef = targetRef.getReference();
        if (targetEntityRef == null || !targetEntityRef.isValid()) {
            context.sendMessage(Message.raw("Le joueur n'est pas dans un monde.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> targetStore = targetEntityRef.getStore();
        Object targetExt = targetStore.getExternalData();
        if (!(targetExt instanceof EntityStore targetEntityStore) || targetEntityStore.getWorld() == null) {
            context.sendMessage(Message.raw("Impossible de localiser le monde actuel du joueur.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        World currentWorld = targetEntityStore.getWorld();

        ISpawnProvider spawnProvider = targetWorld.getWorldConfig().getSpawnProvider();
        if (spawnProvider == null) {
            context.sendMessage(Message.raw("Le monde de destination n'a pas de point de spawn.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Transform spawnPoint = spawnProvider.getSpawnPoint(targetWorld, targetRef.getUuid());
        org.joml.Vector3d spawnPos = spawnPoint.getPosition();

        String targetName = targetRef.getUsername() != null ? targetRef.getUsername() : "";
        currentWorld.execute(() -> {
            Ref<EntityStore> liveRef = targetRef.getReference();
            if (liveRef == null || !liveRef.isValid()) {
                return;
            }
            Store<EntityStore> liveStore = liveRef.getStore();
            Teleport teleport = Teleport.createForPlayer(targetWorld, spawnPos, Rotation3f.ZERO);
            liveStore.addComponent(liveRef, Teleport.getComponentType(), teleport);
        });

        context.sendMessage(Message.raw(targetName + " téléporté vers le monde " + targetWorld.getName() + ".").color(Color.GREEN));
        targetRef.sendMessage(Message.raw("Tu as été téléporté vers le monde " + targetWorld.getName() + ".").color(Color.GREEN));
        return CompletableFuture.completedFuture(null);
    }
}
