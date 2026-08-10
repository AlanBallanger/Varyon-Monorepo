package fr.varyon.death.systeme;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.config.ConfigDeath;
import fr.varyon.death.etat.EtatATerre;
import fr.varyon.death.etat.GestionnaireATerre;
import fr.varyon.death.etat.OutilsJoueur;
import fr.varyon.death.etat.TamponsStore;
import fr.varyon.death.hud.GestionnaireHud;
import fr.varyon.death.hud.HudATerre;

/**
 * Tick principal du mod, execute pour chaque joueur.
 *
 * <p>Il traite trois choses par joueur a terre : le compte a rebours du saignement,
 * la progression de l'abandon (touche Accroupi maintenue) et le rafraichissement du HUD.
 * La progression du relevement, elle, est geree par {@link SystemeReleve}.
 *
 * <p>Ce systeme s'execute dans le tick monde de Hytale : aucune tache planifiee sur un
 * thread externe, pour eviter tout acces concurrent au store d'entites.
 */
public final class SystemeTickATerre extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonRevive-Tick");

    private final GestionnaireATerre gestionnaire;
    private final GestionnaireHud hud;

    public SystemeTickATerre(@Nonnull GestionnaireATerre gestionnaire, @Nonnull GestionnaireHud hud) {
        this.gestionnaire = gestionnaire;
        this.hud = hud;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void tick(float delta,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> tampon) {
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }
        EtatATerre etat = gestionnaire.getEtat(uuid);
        if (etat == null) {
            return;
        }

        Player joueur = chunk.getComponent(index, Player.getComponentType());
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        ConfigDeath config = gestionnaire.getConfig();

        // L'invulnerabilite passe surtout par l'annulation des degats, mais on remet le
        // point de vie a 1 a chaque tick : un mod tiers pourrait l'avoir modifie autrement.
        OutilsJoueur.definirPointsDeVie(ref, tampon, 1f);
        // Reapplique a chaque tick : le serveur reevalue les reglages de deplacement
        // (monde, effets, monture) et ecraserait une immobilisation appliquee une seule fois.
        OutilsJoueur.immobiliser(ref, tampon, playerRef);

        if (traiterAbandon(etat, joueur, playerRef, ref, tampon, config)) {
            return;
        }
        if (traiterSaignement(etat, joueur, playerRef, ref, tampon)) {
            return;
        }
        rafraichirHud(etat, joueur, playerRef, config);
    }

    /**
     * Fait progresser l'abandon tant que la touche Accroupi est maintenue.
     *
     * @return vrai si le joueur est mort d'avoir abandonne, auquel cas le tick s'arrete la
     */
    private boolean traiterAbandon(@Nonnull EtatATerre etat,
                                   @Nullable Player joueur,
                                   @Nonnull PlayerRef playerRef,
                                   @Nonnull Ref<EntityStore> ref,
                                   @Nonnull CommandBuffer<EntityStore> tampon,
                                   @Nonnull ConfigDeath config) {
        boolean accroupi = estAccroupi(etat.getUuidJoueur(), ref, tampon);
        if (config.isLogsDiagnostic() && etat.getTicksRestants() % 20 == 0) {
            // Le compte a rebours doit perdre exactement 20 ticks par seconde. Une chute plus
            // rapide signale que plusieurs entites decrementent le meme etat.
            LOGGER.at(Level.INFO).log(
                    "a terre: restants=%d/%d abandon=%s/%s accroupi=%s soigneurs=%d",
                    etat.getTicksRestants(), config.getDureeSaignementTicks(),
                    etat.getTicksAbandon(), config.getDureeAbandonTicks(),
                    accroupi, etat.getNombreSoigneurs());
        }
        if (!accroupi) {
            // Touche relachee : la progression est perdue, pas mise en pause.
            etat.setTicksAbandon(-1);
            return false;
        }
        int ticks = etat.isAbandonEnCours() ? etat.getTicksAbandon() + 1 : 1;
        etat.setTicksAbandon(ticks);
        if (ticks < config.getDureeAbandonTicks()) {
            return false;
        }
        LOGGER.at(Level.INFO).log("%s a abandonne (touche maintenue).", etat.getNomJoueur());
        terminer(etat, joueur, playerRef, ref, tampon);
        return true;
    }

    /**
     * Decremente le compte a rebours du saignement.
     *
     * @return vrai si le joueur est mort d'avoir saigne
     */
    private boolean traiterSaignement(@Nonnull EtatATerre etat,
                                      @Nullable Player joueur,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> ref,
                                      @Nonnull CommandBuffer<EntityStore> tampon) {
        int restants = etat.getTicksRestants() - 1;
        etat.setTicksRestants(restants);
        if (restants > 0) {
            return false;
        }
        LOGGER.at(Level.INFO).log("%s a succombe a ses blessures (saignement termine).",
                etat.getNomJoueur());
        terminer(etat, joueur, playerRef, ref, tampon);
        return true;
    }

    /**
     * Met fin a l'etat a terre par la mort : nettoyage complet, puis on rejoue le coup
     * qui avait mis a terre pour que le jeu affiche la vraie cause de la mort.
     */
    private void terminer(@Nonnull EtatATerre etat,
                          @Nullable Player joueur,
                          @Nonnull PlayerRef playerRef,
                          @Nonnull Ref<EntityStore> ref,
                          @Nonnull CommandBuffer<EntityStore> tampon) {
        terminerEtat(gestionnaire, hud, etat, joueur, playerRef, ref, tampon);
    }

    /**
     * Meme logique que {@link #terminer}, en statique : utilisee par le bouton ABANDONNER de
     * {@code DeathRecapPage}, qui n'a pas d'instance de ce systeme. La touche Accroupi reste
     * la voie normale d'abandon ; ce chemin existe pour le cas ou le clavier ne repond plus
     * (page custom ouverte, qui capture les entrees du client).
     *
     * @return vrai si l'abandon a bien ete declenche
     */
    public static boolean abandonnerImmediatement(@Nonnull UUID uuidJoueur) {
        GestionnaireATerre gestionnaire = GestionnaireATerre.get();
        GestionnaireHud hud = GestionnaireHud.get();
        if (gestionnaire == null || hud == null) {
            return false;
        }
        EtatATerre etat = gestionnaire.getEtat(uuidJoueur);
        if (etat == null) {
            return false;
        }
        PlayerRef playerRef = joueurEnLigne(uuidJoueur);
        if (playerRef == null) {
            return false;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return false;
        }
        // Hors tick ECS (clic sur une page UI) : aucun CommandBuffer n'est fourni par le
        // framework, seul le Store est accessible. TamponsStore en obtient un par reflexion.
        return TamponsStore.executer(store, tampon -> {
            Player joueur = store.getComponent(ref, Player.getComponentType());
            terminerEtat(gestionnaire, hud, etat, joueur, playerRef, ref, tampon);
            return true;
        });
    }

    private static void terminerEtat(@Nonnull GestionnaireATerre gestionnaire,
                                     @Nonnull GestionnaireHud hud,
                                     @Nonnull EtatATerre etat,
                                     @Nullable Player joueur,
                                     @Nonnull PlayerRef playerRef,
                                     @Nonnull Ref<EntityStore> ref,
                                     @Nonnull CommandBuffer<EntityStore> tampon) {
        // Retire l'etat AVANT d'infliger les degats, sinon le systeme d'interception
        // les absorberait au titre de l'invulnerabilite et le joueur ne mourrait jamais.
        List<UUID> soigneurs = new ArrayList<>(etat.getSoigneurs());
        gestionnaire.retirerDeLEtatATerre(etat.getUuidJoueur());

        hud.masquerATerre(joueur, playerRef);
        masquerHudDesSoigneurs(hud, soigneurs);
        OutilsJoueur.retablirCamera(playerRef);
        OutilsJoueur.arreterAnimationATerre(ref, tampon);
        // La mobilite doit etre rendue avant la mort : sinon le joueur reapparaitrait fige.
        OutilsJoueur.rendreMobilite(ref, tampon);
        OutilsJoueur.tuer(ref, tampon, etat.getCoupFatal());
    }

    /** Retire le HUD de relevement des allies qui soignaient ce joueur. */
    private static void masquerHudDesSoigneurs(@Nonnull GestionnaireHud hud, @Nonnull List<UUID> soigneurs) {
        for (UUID uuidSoigneur : soigneurs) {
            PlayerRef refSoigneur = joueurEnLigne(uuidSoigneur);
            if (refSoigneur != null) {
                hud.masquerSoigneur(null, refSoigneur);
            }
            hud.oublier(uuidSoigneur);
        }
    }

    private void rafraichirHud(@Nonnull EtatATerre etat,
                               @Nullable Player joueur,
                               @Nonnull PlayerRef playerRef,
                               @Nonnull ConfigDeath config) {
        int secondesRestantes = (etat.getTicksRestants() + 19) / 20;
        float progressionReleve = (float) etat.getTicksReleve() / config.getDureeReleveTicks();
        float progressionAbandon = etat.isAbandonEnCours()
                ? (float) etat.getTicksAbandon() / config.getDureeAbandonTicks()
                : 0f;

        hud.afficherATerre(joueur, playerRef, secondesRestantes, progressionReleve,
                progressionAbandon, etat.getNombreSoigneurs(), nomsDesSoigneurs(etat));
    }

    @Nonnull
    private static String nomsDesSoigneurs(@Nonnull EtatATerre etat) {
        List<String> noms = new ArrayList<>();
        for (UUID uuidSoigneur : etat.getSoigneurs()) {
            PlayerRef refSoigneur = joueurEnLigne(uuidSoigneur);
            if (refSoigneur == null) {
                continue;
            }
            try {
                String nom = refSoigneur.getUsername();
                if (nom != null) {
                    noms.add(nom);
                }
            } catch (RuntimeException ignore) {
                // Joueur en cours de deconnexion : on l'omet simplement de la liste.
            }
        }
        return HudATerre.joindreNoms(noms);
    }

    /**
     * Indique si le joueur maintient actuellement la touche Accroupi.
     *
     * <p>La source de verite est {@link FiltrePaquetsATerre}, qui lit les paquets bruts du
     * client. Le composant serveur ne suffit pas pour un joueur a terre : immobilise et
     * couche par une animation, son etat de deplacement cesse d'etre rafraichi, la touche
     * paraissait donc relachee en permanence.
     *
     * <p>Le composant sert de repli pour les joueurs valides (les soigneurs), pour lesquels
     * il reste fiable.
     */
    static boolean estAccroupi(@Nullable UUID uuid,
                               @Nullable Ref<EntityStore> ref,
                               @Nullable CommandBuffer<EntityStore> tampon) {
        if (FiltrePaquetsATerre.estAccroupi(uuid)) {
            return true;
        }
        if (ref == null || !ref.isValid() || tampon == null) {
            return false;
        }
        try {
            MovementStatesComponent composant =
                    tampon.getComponent(ref, MovementStatesComponent.getComponentType());
            if (composant == null) {
                return false;
            }
            MovementStates etats = composant.getMovementStates();
            return etats != null && (etats.crouching || etats.forcedCrouching);
        } catch (RuntimeException ignore) {
            return false;
        }
    }

    @Nullable
    static PlayerRef joueurEnLigne(@Nullable UUID uuid) {
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
}
