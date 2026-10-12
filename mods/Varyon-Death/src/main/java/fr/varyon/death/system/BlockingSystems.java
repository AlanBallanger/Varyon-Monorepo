package fr.varyon.death.systeme;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.etat.GestionnaireATerre;

/**
 * Empeche un joueur a terre d'agir sur le monde.
 *
 * <p>Le joueur a terre ne peut rien faire : casser, poser, utiliser un bloc, fabriquer,
 * jeter ou ramasser un objet. Chaque evenement concerne est simplement annule.
 * Le chat reste libre, volontairement : un joueur a terre doit pouvoir appeler a l'aide.
 */
public final class SystemesBlocage {

    private SystemesBlocage() {}

    /**
     * Base commune : annule l'evenement si le joueur qui le declenche est a terre.
     *
     * @param <E> type d'evenement ECS annulable
     */
    private abstract static class BlocageBase<E extends com.hypixel.hytale.component.system.CancellableEcsEvent>
            extends EntityEventSystem<EntityStore, E> {

        private final GestionnaireATerre gestionnaire;

        protected BlocageBase(@Nonnull Class<E> classeEvenement,
                              @Nonnull GestionnaireATerre gestionnaire) {
            super(classeEvenement);
            this.gestionnaire = gestionnaire;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> tampon,
                           @Nullable E evenement) {
            if (evenement == null || evenement.isCancelled()) {
                return;
            }
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || !playerRef.isValid()) {
                return;
            }
            UUID uuid = playerRef.getUuid();
            if (uuid != null && gestionnaire.estATerre(uuid)) {
                evenement.setCancelled(true);
            }
        }
    }

    public static final class BlocageCasseBloc extends BlocageBase<BreakBlockEvent> {
        public BlocageCasseBloc(@Nonnull GestionnaireATerre gestionnaire) {
            super(BreakBlockEvent.class, gestionnaire);
        }
    }

    public static final class BlocageDegatsBloc extends BlocageBase<DamageBlockEvent> {
        public BlocageDegatsBloc(@Nonnull GestionnaireATerre gestionnaire) {
            super(DamageBlockEvent.class, gestionnaire);
        }
    }

    public static final class BlocagePoseBloc extends BlocageBase<PlaceBlockEvent> {
        public BlocagePoseBloc(@Nonnull GestionnaireATerre gestionnaire) {
            super(PlaceBlockEvent.class, gestionnaire);
        }
    }

    public static final class BlocageJetObjet extends BlocageBase<DropItemEvent> {
        public BlocageJetObjet(@Nonnull GestionnaireATerre gestionnaire) {
            super(DropItemEvent.class, gestionnaire);
        }
    }

    public static final class BlocageRamassage extends BlocageBase<InteractivelyPickupItemEvent> {
        public BlocageRamassage(@Nonnull GestionnaireATerre gestionnaire) {
            super(InteractivelyPickupItemEvent.class, gestionnaire);
        }
    }
}
