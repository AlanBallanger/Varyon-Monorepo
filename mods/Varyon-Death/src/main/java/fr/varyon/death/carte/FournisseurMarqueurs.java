package fr.varyon.death.carte;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerBuilder;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;

import fr.varyon.death.config.ConfigDeath;
import fr.varyon.death.etat.EtatATerre;
import fr.varyon.death.etat.GestionnaireATerre;

/**
 * Place sur la carte un marqueur pour chaque allie a terre du meme monde.
 *
 * <p>Les marqueurs ignorent la distance de vue : c'est tout l'interet, retrouver un allie
 * que l'on ne voit pas. L'affichage est reserve aux joueurs disposant de la permission
 * {@link ConfigDeath#PERMISSION_CARTE}.
 */
public final class FournisseurMarqueurs implements WorldMapManager.MarkerProvider {

    /** Identifiant sous lequel le fournisseur est enregistre aupres de chaque monde. */
    public static final String CLE = "VaryonReviveMarqueurs";

    private static final String ICONE = "MapMarkerPlayer.png";

    private final GestionnaireATerre gestionnaire;

    public FournisseurMarqueurs(@Nonnull GestionnaireATerre gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    @Override
    public void update(@Nullable World monde,
                       @Nullable Player observateur,
                       @Nullable MarkersCollector collecteur) {
        if (monde == null || observateur == null || collecteur == null) {
            return;
        }
        ConfigDeath config = gestionnaire.getConfig();
        if (!config.isCarteActive()) {
            return;
        }
        PlayerRef observateurRef = refDe(observateur, monde);
        if (observateurRef == null || !aLaPermission(observateurRef)) {
            return;
        }
        UUID uuidObservateur = observateurRef.getUuid();

        for (EtatATerre etat : gestionnaire.tousLesEtats()) {
            if (etat.getUuidJoueur().equals(uuidObservateur)) {
                continue;
            }
            PlayerRef cibleRef = joueurEnLigne(etat.getUuidJoueur());
            if (cibleRef == null || !memeMonde(observateurRef, cibleRef)) {
                continue;
            }
            MapMarker marqueur = construire(etat, cibleRef);
            if (marqueur != null) {
                collecteur.addIgnoreViewDistance(marqueur);
            }
        }
    }

    @Nullable
    private static MapMarker construire(@Nonnull EtatATerre etat, @Nonnull PlayerRef cibleRef) {
        try {
            Transform transform = cibleRef.getTransform();
            if (transform == null) {
                return null;
            }
            String identifiant = "VaryonRevive-" + etat.getUuidJoueur();
            return new MapMarkerBuilder(identifiant, ICONE, transform)
                    .withCustomName(etat.getNomJoueur() + " (a terre)")
                    .build();
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    private static boolean aLaPermission(@Nonnull PlayerRef playerRef) {
        try {
            return playerRef.hasPermission(ConfigDeath.PERMISSION_CARTE);
        } catch (RuntimeException ignore) {
            // En cas de doute on n'affiche pas : la permission est la regle.
            return false;
        }
    }

    /**
     * Resout le {@link PlayerRef} de l'observateur en lisant le composant depuis le store
     * du monde. On evite {@code Player#getPlayerRef()} et {@code Entity#getUuid()}, tous
     * deux deprecies et marques pour suppression.
     */
    @Nullable
    private static PlayerRef refDe(@Nonnull Player joueur, @Nonnull World monde) {
        try {
            Ref<EntityStore> reference = joueur.getReference();
            if (reference == null || !reference.isValid()) {
                return null;
            }
            Store<EntityStore> store = monde.getEntityStore().getStore();
            PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());
            return playerRef != null && playerRef.isValid() ? playerRef : null;
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @Nullable
    private static PlayerRef joueurEnLigne(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            PlayerRef playerRef = universe.getPlayer(uuid);
            return playerRef != null && playerRef.isValid() ? playerRef : null;
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    private static boolean memeMonde(@Nonnull PlayerRef a, @Nonnull PlayerRef b) {
        try {
            return a.getWorldUuid() != null && a.getWorldUuid().equals(b.getWorldUuid());
        } catch (RuntimeException ignore) {
            return false;
        }
    }
}
