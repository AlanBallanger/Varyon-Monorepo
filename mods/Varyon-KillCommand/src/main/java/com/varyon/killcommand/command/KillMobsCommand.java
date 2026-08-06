package com.varyon.killcommand.command;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class KillMobsCommand extends CommandBase {

    private final RequiredArg<Double> radiusArg =
            this.withRequiredArg("rayon", "Rayon de recherche autour de vous.", ArgTypes.DOUBLE);

    public KillMobsCommand() {
        super("mobs", "Supprime tous les mobs dans le rayon indiqué.");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        double radius = this.radiusArg.get(context);
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Cette commande ne peut être utilisée que par un joueur."));
            return;
        }
        if (radius <= 0.0) {
            context.sendMessage(Message.raw("Le rayon doit être supérieur à 0."));
            return;
        }

        Ref<EntityStore> playerRef = context.senderAsPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(Message.raw("Joueur introuvable dans le monde."));
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            ComponentType<EntityStore, TransformComponent> transformComponentType =
                    EntityModule.get().getTransformComponentType();
            TransformComponent transform = transformComponentType == null
                    ? null
                    : store.getComponent(playerRef, transformComponentType);
            if (transform == null || transform.getPosition() == null) {
                context.sendMessage(Message.raw("Impossible de déterminer votre position."));
                return;
            }

            Vector3d origin = transform.getPosition();
            List<Ref<EntityStore>> targets = TargetUtil.getAllEntitiesInSphere(origin, radius, store);
            int removed = removeLivingNonPlayers(store, targets);
            context.sendMessage(Message.raw(removed + " mob(s) supprimé(s)."));
        });
    }

    static int removeLivingNonPlayers(@Nonnull Store<EntityStore> store,
                                      @Nonnull List<Ref<EntityStore>> targetEntities) {
        EntityModule entityModule = EntityModule.get();
        ComponentType<EntityStore, Player> playerComponentType = entityModule.getPlayerComponentType();
        ComponentType<EntityStore, LivingEntity> livingEntityComponentType =
                entityModule.getComponentType(LivingEntity.class);

        int removed = 0;
        for (Ref<EntityStore> ref : targetEntities) {
            if (ref == null || !ref.isValid()) {
                continue;
            }
            if (playerComponentType != null && store.getComponent(ref, playerComponentType) != null) {
                continue;
            }
            boolean isLiving = livingEntityComponentType == null
                    || store.getComponent(ref, livingEntityComponentType) != null;
            if (!isLiving) {
                continue;
            }
            store.removeEntity(ref, RemoveReason.REMOVE);
            removed++;
        }
        return removed;
    }
}
