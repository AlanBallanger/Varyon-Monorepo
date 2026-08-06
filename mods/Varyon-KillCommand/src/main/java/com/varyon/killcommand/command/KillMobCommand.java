package com.varyon.killcommand.command;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class KillMobCommand extends CommandBase {

    private static final double DEFAULT_RADIUS = 20.0;

    private final RequiredArg<String> mobArg = this.withRequiredArg(
            "mob", "Filtre sur l'identifiant du modèle du mob (exemple : fox, wolf, spider).", ArgTypes.STRING);

    public KillMobCommand() {
        super("mob", "Supprime les mobs correspondants dans un rayon de 20 blocs.");
        this.addUsageVariant(new KillMobLimitedVariant());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        executeKill(context, this.mobArg.get(context), null, DEFAULT_RADIUS);
    }

    private static void executeKill(@Nonnull CommandContext context, @Nullable String mobFilter,
                                    @Nullable Integer maxAmount, double radius) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Cette commande ne peut être utilisée que par un joueur."));
            return;
        }
        if (mobFilter == null || mobFilter.isBlank()) {
            context.sendMessage(Message.raw("Veuillez indiquer un nom de mob."));
            return;
        }
        if (radius <= 0.0) {
            context.sendMessage(Message.raw("Le rayon doit être supérieur à 0."));
            return;
        }
        if (maxAmount != null && maxAmount <= 0) {
            context.sendMessage(Message.raw("La quantité doit être supérieure à 0."));
            return;
        }

        Ref<EntityStore> playerRef = context.senderAsPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(Message.raw("Joueur introuvable dans le monde."));
            return;
        }

        String filter = mobFilter.toLowerCase(Locale.ROOT);
        Store<EntityStore> store = playerRef.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            EntityModule entityModule = EntityModule.get();
            ComponentType<EntityStore, TransformComponent> transformComponentType =
                    entityModule.getTransformComponentType();
            ComponentType<EntityStore, Player> playerComponentType = entityModule.getPlayerComponentType();
            ComponentType<EntityStore, LivingEntity> livingEntityComponentType =
                    entityModule.getComponentType(LivingEntity.class);
            ComponentType<EntityStore, ModelComponent> modelComponentType = entityModule.getModelComponentType();
            ComponentType<EntityStore, PersistentModel> persistentModelComponentType =
                    entityModule.getPersistentModelComponentType();

            TransformComponent transform = transformComponentType == null
                    ? null
                    : store.getComponent(playerRef, transformComponentType);
            if (transform == null || transform.getPosition() == null) {
                context.sendMessage(Message.raw("Impossible de déterminer votre position."));
                return;
            }

            Vector3d origin = transform.getPosition();
            List<Ref<EntityStore>> targets = TargetUtil.getAllEntitiesInSphere(origin, radius, store);
            int removed = 0;
            for (Ref<EntityStore> ref : targets) {
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
                if (!matchesMobFilter(store, ref, modelComponentType, persistentModelComponentType, filter)) {
                    continue;
                }
                store.removeEntity(ref, RemoveReason.REMOVE);
                removed++;
                if (maxAmount != null && removed >= maxAmount) {
                    break;
                }
            }

            String radiusText = radius % 1.0 == 0.0 ? Integer.toString((int) radius) : Double.toString(radius);
            if (maxAmount == null) {
                context.sendMessage(Message.raw(removed + " mob(s) « " + filter + " » supprimé(s) dans "
                        + radiusText + " blocs."));
            } else {
                context.sendMessage(Message.raw(removed + " mob(s) « " + filter + " » supprimé(s) dans "
                        + radiusText + " blocs (max " + maxAmount + ")."));
            }
        });
    }

    private static boolean matchesMobFilter(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                            @Nullable ComponentType<EntityStore, ModelComponent> modelComponentType,
                                            @Nullable ComponentType<EntityStore, PersistentModel> persistentModelComponentType,
                                            @Nonnull String mobFilter) {
        if (modelComponentType != null) {
            ModelComponent modelComponent = store.getComponent(ref, modelComponentType);
            if (modelComponent != null) {
                Model model = modelComponent.getModel();
                if (model != null && containsIgnoreCase(model.getModelAssetId(), mobFilter)) {
                    return true;
                }
            }
        }
        if (persistentModelComponentType != null) {
            PersistentModel persistentModel = store.getComponent(ref, persistentModelComponentType);
            if (persistentModel != null && persistentModel.getModelReference() != null) {
                return containsIgnoreCase(persistentModel.getModelReference().getModelAssetId(), mobFilter);
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(@Nullable String value, @Nonnull String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    private static final class KillMobLimitedVariant extends CommandBase {

        private final RequiredArg<String> mobArg = this.withRequiredArg(
                "mob", "Filtre sur l'identifiant du modèle du mob (exemple : fox, wolf, spider).", ArgTypes.STRING);
        private final RequiredArg<Integer> amountArg = this.withRequiredArg(
                "quantité", "Nombre maximum de mobs correspondants à supprimer.", ArgTypes.INTEGER);
        private final RequiredArg<Double> radiusArg = this.withRequiredArg(
                "rayon", "Rayon de recherche autour de vous.", ArgTypes.DOUBLE);

        private KillMobLimitedVariant() {
            super("Supprime un nombre limité de mobs correspondants dans le rayon indiqué.");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            executeKill(context, this.mobArg.get(context), this.amountArg.get(context), this.radiusArg.get(context));
        }
    }
}
