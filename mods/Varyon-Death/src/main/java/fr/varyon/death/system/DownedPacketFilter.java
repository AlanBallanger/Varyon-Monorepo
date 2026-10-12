package fr.varyon.death.systeme;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import fr.varyon.death.etat.GestionnaireATerre;

/**
 * Filtre les paquets entrants pour tout ce qui touche a l'etat a terre.
 *
 * <p>Il remplit trois roles :
 * <ul>
 *   <li><b>Lire la touche Accroupi.</b> Le composant serveur {@code MovementStatesComponent}
 *       n'est pas fiable pour un joueur a terre : celui-ci est immobilise et couche par une
 *       animation, si bien que le serveur cesse de rafraichir son etat de deplacement. La
 *       touche paraissait alors relachee en permanence et la jauge d'abandon restait vide.
 *       Le paquet {@link ClientMovement}, lui, continue d'arriver a chaque tick avec l'etat
 *       brut des touches.</li>
 *   <li><b>Figer l'orientation du corps.</b> Les reglages de camera masquent la rotation
 *       sans l'empecher : le personnage pivotait toujours cote serveur. On neutralise donc
 *       l'orientation transmise par le client.</li>
 *   <li><b>Bloquer les attaques et capacites.</b> Un joueur a terre ne doit pas pouvoir
 *       frapper devant lui.</li>
 * </ul>
 *
 * <p><b>Convention de retour</b> : {@code test} renvoie {@code true} pour <em>intercepter</em>
 * le paquet et {@code false} pour le laisser suivre son cours. Renvoyer {@code true} par
 * defaut bloque donc l'integralite du trafic entrant et fige le joueur sur
 * « awaiting for chunks » a la connexion.
 */
public final class FiltrePaquetsATerre implements PlayerPacketFilter {

    private static final Map<UUID, Boolean> ACCROUPI_PAR_JOUEUR = new ConcurrentHashMap<>();

    /** Vrai si le client de ce joueur signale actuellement la touche Accroupi maintenue. */
    public static boolean estAccroupi(@Nullable UUID uuid) {
        return uuid != null && Boolean.TRUE.equals(ACCROUPI_PAR_JOUEUR.get(uuid));
    }

    /** Oublie l'etat d'un joueur, a sa deconnexion. */
    public static void oublier(@Nullable UUID uuid) {
        if (uuid != null) {
            ACCROUPI_PAR_JOUEUR.remove(uuid);
        }
    }

    @Override
    public boolean test(@Nullable PlayerRef playerRef, @Nullable Packet paquet) {
        if (playerRef == null || paquet == null) {
            return false;
        }
        UUID uuid;
        try {
            uuid = playerRef.getUuid();
        } catch (RuntimeException ignore) {
            return false;
        }
        if (uuid == null) {
            return false;
        }

        if (paquet instanceof ClientMovement mouvement) {
            return traiterMouvement(uuid, mouvement);
        }
        if (paquet instanceof SyncInteractionChains interactions) {
            return traiterInteractions(uuid, interactions);
        }
        return false;
    }

    /**
     * Releve l'etat de la touche Accroupi, puis fige l'orientation du corps si le joueur
     * est a terre. Le paquet est toujours laisse passer : le bloquer couperait aussi la
     * synchronisation de position, et le client se desynchroniserait.
     */
    private boolean traiterMouvement(@Nonnull UUID uuid, @Nonnull ClientMovement mouvement) {
        try {
            ACCROUPI_PAR_JOUEUR.put(uuid, lireAccroupi(mouvement));

            GestionnaireATerre gestionnaire = GestionnaireATerre.get();
            if (gestionnaire != null && gestionnaire.estATerre(uuid)) {
                figerOrientation(mouvement);
            }
        } catch (RuntimeException ignore) {
            // Une lecture ratee ne doit jamais perturber le reseau.
        }
        return false;
    }

    /**
     * Neutralise la rotation transmise par le client. On aligne l'orientation du regard sur
     * celle du corps plutot que de les mettre a zero : remettre a zero ferait brusquement
     * pivoter le personnage vers le nord a la mise a terre.
     */
    private static void figerOrientation(@Nonnull ClientMovement mouvement) {
        Direction corps = mouvement.bodyOrientation;
        if (corps != null) {
            mouvement.lookOrientation = corps;
        }
        // Aucune intention de deplacement ne doit subsister.
        mouvement.wishMovement = null;
    }

    /**
     * Bloque les interactions offensives d'un joueur a terre. Les autres types passent :
     * le paquet transporte aussi des evenements purement informatifs (collisions, equipement)
     * dont la suppression desynchroniserait le client.
     */
    private boolean traiterInteractions(@Nonnull UUID uuid,
                                        @Nonnull SyncInteractionChains interactions) {
        GestionnaireATerre gestionnaire = GestionnaireATerre.get();
        if (gestionnaire == null || !gestionnaire.estATerre(uuid)) {
            return false;
        }
        SyncInteractionChain[] chaines = interactions.updates;
        if (chaines == null) {
            return false;
        }
        for (SyncInteractionChain chaine : chaines) {
            if (chaine != null && estOffensive(chaine.interactionType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean estOffensive(@Nullable InteractionType type) {
        if (type == null) {
            return false;
        }
        return type == InteractionType.Primary
                || type == InteractionType.Secondary
                || type == InteractionType.Ability1
                || type == InteractionType.Ability2
                || type == InteractionType.Ability3
                || type == InteractionType.Use
                || type == InteractionType.Dodge
                || type == InteractionType.ProjectileSpawn;
    }

    private static boolean lireAccroupi(@Nonnull ClientMovement mouvement) {
        MovementStates etats = mouvement.movementStates;
        if (etats != null && (etats.crouching || etats.forcedCrouching)) {
            return true;
        }
        // Un joueur sur une monture transmet son etat dans un champ distinct.
        MovementStates monte = mouvement.riderMovementStates;
        return monte != null && (monte.crouching || monte.forcedCrouching);
    }
}
