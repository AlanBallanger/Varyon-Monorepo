package fr.varyon.death.systeme;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;

import fr.varyon.death.etat.GestionnaireATerre;

/**
 * Force l'attitude {@link Attitude#IGNORE} envers tout joueur a terre.
 *
 * <p>Remplace l'ancienne approche de {@code SystemeDesaggro} (purge periodique de
 * {@code TargetMemory} + {@code WorldSupport.overrideAttitude}) : cette derniere ecrivait dans
 * {@code WorldSupport.attitudeOverrideMemory}, un cache que le moteur ne consulte jamais lors de
 * l'evaluation reelle du ciblage ({@code EntityFilterAttitude} / {@code CombatTargetCollector}
 * ne lisent que {@code WorldSupport.getAttitude()}, qui l'ignore). Un {@link IAttitudeProvider}
 * enregistre sur chaque {@code AttitudeView} du {@code Blackboard} NPC est en revanche un point
 * d'extension officiel, interroge par le moteur a chaque evaluation d'attitude — pas de probleme
 * d'ordre entre systemes ECS a gerer. Meme mecanisme que {@code OmbreStealthAttitudeProvider}
 * dans Varyon-RPG, sans l'invisibilite visuelle qui l'accompagne la-bas.
 */
public final class AttitudeProviderATerre implements IAttitudeProvider {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeath-AttitudeATerre");

    private final GestionnaireATerre gestionnaire;

    public AttitudeProviderATerre(@Nonnull GestionnaireATerre gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    @Override
    @Nullable
    public Attitude getAttitude(@Nonnull Ref<EntityStore> npcRef,
                                int roleIndex,
                                @Nonnull Ref<EntityStore> targetRef,
                                @Nonnull ComponentAccessor<EntityStore> accessor) {
        try {
            PlayerRef playerRef = accessor.getComponent(targetRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                return null;
            }
            if (gestionnaire.estATerre(playerRef.getUuid())) {
                return Attitude.IGNORE;
            }
        } catch (RuntimeException e) {
            LOGGER.at(java.util.logging.Level.FINE).log("Evaluation d'attitude echouee: %s", e.getMessage());
        }
        return null;
    }
}
