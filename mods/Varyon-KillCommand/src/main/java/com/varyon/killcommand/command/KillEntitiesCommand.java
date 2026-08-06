package com.varyon.killcommand.command;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class KillEntitiesCommand extends CommandBase {

    private final RequiredArg<Double> radiusArg =
            this.withRequiredArg("rayon", "Rayon de recherche autour de vous.", ArgTypes.DOUBLE);

    public KillEntitiesCommand() {
        super("entities", "Supprime les objets et projectiles au sol dans le rayon indiqué.");
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
            List<Ref<EntityStore>> targets = new ArrayList<>(
                    TargetUtil.getAllEntitiesInSphere(origin, radius, store));
            collectItemEntitiesInSphere(store, origin, radius, targets);
            int removed = removeGroundClutter(store, targets);

            String radiusText = radius % 1.0 == 0.0 ? Integer.toString((int) radius) : Double.toString(radius);
            context.sendMessage(Message.raw(removed + " entité(s) au sol supprimée(s) dans "
                    + radiusText + " blocs."));
        });
    }

    /** Les objets au sol ont leur propre structure spatiale, que {@link TargetUtil} n'interroge pas. */
    private static void collectItemEntitiesInSphere(@Nonnull Store<EntityStore> store, @Nonnull Vector3d origin,
                                                    double radius, @Nonnull List<Ref<EntityStore>> out) {
        ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> itemSpatialResourceType =
                EntityModule.get().getItemSpatialResourceType();
        if (itemSpatialResourceType == null) {
            return;
        }
        SpatialResource<Ref<EntityStore>, EntityStore> spatialResource = store.getResource(itemSpatialResourceType);
        if (spatialResource == null || spatialResource.getSpatialStructure() == null) {
            return;
        }

        List<Ref<EntityStore>> collected = new ArrayList<>();
        spatialResource.getSpatialStructure().collect(origin, radius, collected);

        ComponentType<EntityStore, TransformComponent> transformComponentType =
                EntityModule.get().getTransformComponentType();
        double radiusSquared = radius * radius;
        for (Ref<EntityStore> ref : collected) {
            if (ref == null || !ref.isValid() || out.contains(ref)) {
                continue;
            }
            if (transformComponentType != null) {
                TransformComponent itemTransform = store.getComponent(ref, transformComponentType);
                if (itemTransform == null || itemTransform.getPosition() == null
                        || itemTransform.getPosition().distanceSquared(origin) > radiusSquared) {
                    continue;
                }
            }
            out.add(ref);
        }
    }

    static int removeGroundClutter(@Nonnull Store<EntityStore> store,
                                  @Nonnull List<Ref<EntityStore>> targetEntities) {
        EntityModule entityModule = EntityModule.get();
        ComponentType<EntityStore, ItemComponent> itemComponentType = entityModule.getItemComponentType();
        ComponentType<EntityStore, ProjectileComponent> projectileComponentType =
                ProjectileComponent.getComponentType();
        if (itemComponentType == null && projectileComponentType == null) {
            return 0;
        }
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
            if (livingEntityComponentType != null && store.getComponent(ref, livingEntityComponentType) != null) {
                continue;
            }
            if (!isGroundClutter(store, ref, itemComponentType, projectileComponentType)) {
                continue;
            }
            store.removeEntity(ref, RemoveReason.REMOVE);
            removed++;
        }
        return removed;
    }

    private static boolean isGroundClutter(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                           @Nullable ComponentType<EntityStore, ItemComponent> itemComponentType,
                                           @Nullable ComponentType<EntityStore, ProjectileComponent> projectileComponentType) {
        if (itemComponentType != null && store.getComponent(ref, itemComponentType) != null) {
            return true;
        }
        if (projectileComponentType != null) {
            ProjectileComponent projectile = store.getComponent(ref, projectileComponentType);
            if (projectile != null) {
                return projectile.isOnGround();
            }
        }
        return false;
    }
}
