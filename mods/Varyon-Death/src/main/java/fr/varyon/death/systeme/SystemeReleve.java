package fr.varyon.death.systeme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.config.ConfigDeath;
import fr.varyon.death.etat.EtatATerre;
import fr.varyon.death.etat.GestionnaireATerre;
import fr.varyon.death.etat.OutilsJoueur;
import fr.varyon.death.etat.PotionsResurrection;
import fr.varyon.death.etat.TierPotionResurrection;
import fr.varyon.death.hud.GestionnaireHud;

/**
 * Gere le relevement, du cote du soigneur.
 *
 * <p>Le relevement demarre quand un joueur valide s'accroupit a portee d'un allie a terre.
 * Il est annule si le soigneur se releve, s'eloigne de son point de depart au-dela de la
 * tolerance configuree, sort de la portee, change de monde, prend des degats ou passe
 * lui-meme a terre.
 *
 * <p>Avec N soigneurs la progression avance de N pas par tick : la duree effective est
 * donc {@code duree / N}, conformement au comportement attendu (10 s seul, 5 s a deux,
 * 3,3 s a trois).
 */
public final class SystemeReleve extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonRevive-Releve");

    /** Delai minimal entre une interruption et la reprise automatique du relevement. */
    private static final long DELAI_AVANT_REPRISE_MS = 1_500L;
    /** Instant de la derniere interruption, par soigneur. */
    private static final Map<UUID, Long> INTERRUPTIONS = new ConcurrentHashMap<>();

    private final GestionnaireATerre gestionnaire;
    private final GestionnaireHud hud;

    public SystemeReleve(@Nonnull GestionnaireATerre gestionnaire, @Nonnull GestionnaireHud hud) {
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
        PlayerRef soigneurRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (soigneurRef == null || !soigneurRef.isValid()) {
            return;
        }
        UUID uuidSoigneur = soigneurRef.getUuid();
        if (uuidSoigneur == null) {
            return;
        }

        Player soigneur = chunk.getComponent(index, Player.getComponentType());
        Ref<EntityStore> refSoigneur = chunk.getReferenceTo(index);

        // Un joueur a terre ne peut pas relever : son propre etat prime.
        if (gestionnaire.estATerre(uuidSoigneur)) {
            if (gestionnaire.estSoigneur(uuidSoigneur)) {
                annuler(uuidSoigneur, soigneur, soigneurRef);
            }
            return;
        }

        if (gestionnaire.estSoigneur(uuidSoigneur)) {
            poursuivre(uuidSoigneur, soigneur, soigneurRef, refSoigneur, tampon);
        } else {
            tenterDeDemarrer(uuidSoigneur, soigneur, soigneurRef, refSoigneur, tampon);
        }
    }

    // --- Demarrage ----------------------------------------------------------

    private void tenterDeDemarrer(@Nonnull UUID uuidSoigneur,
                                  @Nullable Player soigneur,
                                  @Nonnull PlayerRef soigneurRef,
                                  @Nonnull Ref<EntityStore> refSoigneur,
                                  @Nonnull CommandBuffer<EntityStore> tampon) {
        // Le mode de jeu du soigneur n'entre pas en ligne de compte : contrairement a l'etat a
        // terre (qui ne concerne que l'Aventure, un joueur en Creatif ne pouvant pas mourir),
        // rien n'empeche un joueur en Creatif ou Spectateur d'assister un allie a terre.
        if (!SystemeTickATerre.estAccroupi(uuidSoigneur, refSoigneur, tampon)) {
            return;
        }
        // Apres une interruption, on laisse passer un court delai avant de pouvoir relancer.
        // Sans cela, un relevement annule redemarre des le tick suivant et s'interrompt encore :
        // la jauge repart de zero en boucle et le joueur est noye sous les messages.
        Long interrompuA = INTERRUPTIONS.get(uuidSoigneur);
        if (interrompuA != null
                && System.currentTimeMillis() - interrompuA < DELAI_AVANT_REPRISE_MS) {
            return;
        }
        EtatATerre cible = chercherCible(soigneurRef);
        if (cible == null) {
            return;
        }
        // Une potion de resurrection (n'importe quel tier) est obligatoire pour demarrer.
        // Elle n'a pas besoin d'etre en main : n'importe ou dans l'inventaire suffit.
        if (PotionsResurrection.meilleurTierDetenu(uuidSoigneur) == null) {
            envoyer(soigneurRef, "Vous avez besoin d'une potion de résurrection pour relever un allié");
            return;
        }
        Vector3d position = OutilsJoueur.position(soigneurRef);
        if (position == null) {
            return;
        }

        gestionnaire.demarrerReleve(uuidSoigneur, cible, position.x(), position.y(), position.z());
        hud.afficherSoigneur(soigneur, soigneurRef, 0f, cible.getNomJoueur(), cible.getNombreSoigneurs());

        PlayerRef cibleRef = SystemeTickATerre.joueurEnLigne(cible.getUuidJoueur());
        envoyer(cibleRef, nomDe(soigneurRef) + " commence à vous relever");
        LOGGER.at(Level.FINE).log("%s releve %s.", nomDe(soigneurRef), cible.getNomJoueur());
    }

    /** Cherche l'allie a terre le plus proche, a portee et dans le meme monde. */
    @Nullable
    private EtatATerre chercherCible(@Nonnull PlayerRef soigneurRef) {
        EtatATerre meilleure = null;
        double meilleureDistance = Double.MAX_VALUE;
        double porteeMax = gestionnaire.getConfig().getDistanceMaxCarree();

        for (EtatATerre candidat : gestionnaire.tousLesEtats()) {
            PlayerRef candidatRef = SystemeTickATerre.joueurEnLigne(candidat.getUuidJoueur());
            if (candidatRef == null || !OutilsJoueur.memeMonde(soigneurRef, candidatRef)) {
                continue;
            }
            double distance = OutilsJoueur.distanceCarree(soigneurRef, candidatRef);
            if (distance <= porteeMax && distance < meilleureDistance) {
                meilleure = candidat;
                meilleureDistance = distance;
            }
        }
        return meilleure;
    }

    // --- Progression --------------------------------------------------------

    private void poursuivre(@Nonnull UUID uuidSoigneur,
                            @Nullable Player soigneur,
                            @Nonnull PlayerRef soigneurRef,
                            @Nonnull Ref<EntityStore> refSoigneur,
                            @Nonnull CommandBuffer<EntityStore> tampon) {
        EtatATerre cible = gestionnaire.getCibleDuSoigneur(uuidSoigneur);
        if (cible == null) {
            annuler(uuidSoigneur, soigneur, soigneurRef);
            return;
        }
        ConfigDeath config = gestionnaire.getConfig();

        // Le soigneur doit rester accroupi.
        if (!SystemeTickATerre.estAccroupi(uuidSoigneur, refSoigneur, tampon)) {
            annuler(uuidSoigneur, soigneur, soigneurRef);
            envoyer(soigneurRef, "Relèvement interrompu : vous vous êtes relevé");
            return;
        }
        // Il doit rester immobile : le mod ne le bloque pas, il sanctionne le deplacement.
        double[] ancrage = gestionnaire.getAncrage(uuidSoigneur);
        if (OutilsJoueur.distanceCarree(soigneurRef, ancrage) > config.getToleranceMouvementCarree()) {
            annuler(uuidSoigneur, soigneur, soigneurRef);
            envoyer(soigneurRef, "Relèvement interrompu : vous avez bougé");
            return;
        }
        // La cible doit rester a portee, dans le meme monde et connectee.
        PlayerRef cibleRef = SystemeTickATerre.joueurEnLigne(cible.getUuidJoueur());
        if (cibleRef == null
                || !OutilsJoueur.memeMonde(soigneurRef, cibleRef)
                || OutilsJoueur.distanceCarree(soigneurRef, cibleRef) > config.getDistanceMaxCarree()) {
            annuler(uuidSoigneur, soigneur, soigneurRef);
            envoyer(soigneurRef, "Relèvement interrompu : votre allié est hors de portée");
            return;
        }

        // Ce systeme est appele une fois par soigneur et par tick ; chacun avance donc la
        // barre d'un seul pas. La progression totale par tick vaut le nombre de soigneurs,
        // d'ou une duree de « duree / N » (10 s seul, 5 s a deux, 3,3 s a trois).
        int progression = cible.getTicksReleve() + 1;
        cible.setTicksReleve(progression);

        // La duree de base depend de la meilleure potion detenue par n'importe quel soigneur
        // participant, recalculee a chaque tick : un soigneur qui rejoint avec une potion plus
        // forte ameliore aussitot la duree pour tout le monde, sans devoir redemarrer.
        int dureeTicks = dureeEffectiveTicks(cible, config);

        if (progression >= dureeTicks) {
            achever(cible, cibleRef, tampon);
            return;
        }
        hud.afficherSoigneur(soigneur, soigneurRef,
                (float) progression / dureeTicks,
                cible.getNomJoueur(), cible.getNombreSoigneurs());
    }

    /**
     * Duree de base du relevement, determinee par la meilleure potion de resurrection
     * detenue par n'importe quel soigneur participant. La potion elle-meme n'est fournie
     * qu'a l'achevement : ici on ne fait que lire, jamais consommer.
     */
    private static int dureeEffectiveTicks(@Nonnull EtatATerre cible, @Nonnull ConfigDeath config) {
        TierPotionResurrection meilleur = meilleurTierParmiSoigneurs(cible);
        return meilleur == null ? config.getDureeReleveTicks() : meilleur.dureeTicks(config);
    }

    @Nullable
    private static TierPotionResurrection meilleurTierParmiSoigneurs(@Nonnull EtatATerre cible) {
        TierPotionResurrection meilleur = null;
        for (UUID uuidSoigneur : cible.getSoigneurs()) {
            meilleur = TierPotionResurrection.meilleur(
                    meilleur, PotionsResurrection.meilleurTierDetenu(uuidSoigneur));
        }
        return meilleur;
    }

    // --- Fin ----------------------------------------------------------------

    /** Le relevement a abouti : on rend au joueur ses points de vie et sa camera. */
    private void achever(@Nonnull EtatATerre cible,
                         @Nonnull PlayerRef cibleRef,
                         @Nonnull CommandBuffer<EntityStore> tampon) {
        List<UUID> soigneurs = new ArrayList<>(cible.getSoigneurs());
        consommerMeilleurePotion(soigneurs);
        gestionnaire.retirerDeLEtatATerre(cible.getUuidJoueur());
        appliquerReleve(gestionnaire, hud, cible, cibleRef, tampon);

        for (UUID uuidSoigneur : soigneurs) {
            PlayerRef refSoigneur = SystemeTickATerre.joueurEnLigne(uuidSoigneur);
            if (refSoigneur != null) {
                hud.masquerSoigneur(null, refSoigneur);
                envoyer(refSoigneur, "Vous avez relevé " + cible.getNomJoueur());
            }
            hud.oublier(uuidSoigneur);
        }
        LOGGER.at(Level.FINE).log("%s a ete releve par %d allie(s).",
                cible.getNomJoueur(), soigneurs.size());
    }

    /**
     * Releve immediatement un joueur, sans passer par un soigneur ni consommer de potion.
     * Reservee a l'usage administratif ({@code /relever}) : le relevement normal, lui, passe
     * toujours par {@link #achever}, qui gere en plus les soigneurs et la potion consommee.
     *
     * <p>Doit s'executer sur le thread du monde de la cible. Accepte {@code Store} directement
     * (implemente {@link ComponentAccessor}) : une commande n'a pas de {@code CommandBuffer}
     * a sa disposition, seulement le {@code Store} du joueur cible.
     */
    public static void releverImmediatement(@Nonnull GestionnaireATerre gestionnaire,
                                            @Nonnull EtatATerre cible,
                                            @Nonnull PlayerRef cibleRef,
                                            @Nonnull ComponentAccessor<EntityStore> accesseur) {
        GestionnaireHud hud = GestionnaireHud.get();
        gestionnaire.retirerDeLEtatATerre(cible.getUuidJoueur());
        appliquerReleve(gestionnaire, hud, cible, cibleRef, accesseur);
    }

    /** Remet le joueur cible sur pied : points de vie, camera, animation, mobilite, HUD. */
    private static void appliquerReleve(@Nonnull GestionnaireATerre gestionnaire,
                                        @Nullable GestionnaireHud hud,
                                        @Nonnull EtatATerre cible,
                                        @Nonnull PlayerRef cibleRef,
                                        @Nonnull ComponentAccessor<EntityStore> accesseur) {
        Ref<EntityStore> refCible = referenceDe(cibleRef);
        float pointsDeVie = OutilsJoueur.pointsDeVieAuRelevement(refCible, accesseur, gestionnaire.getConfig());
        OutilsJoueur.definirPointsDeVie(refCible, accesseur, pointsDeVie);
        OutilsJoueur.retablirCamera(cibleRef);
        OutilsJoueur.arreterAnimationATerre(refCible, accesseur);
        OutilsJoueur.rendreMobilite(refCible, accesseur);

        if (hud != null) {
            hud.masquerATerre(null, cibleRef);
        }
        envoyer(cibleRef, "Vous avez été relevé");
    }

    /**
     * Consomme une seule potion, du tier le plus fort trouve parmi tous les soigneurs.
     * Si plusieurs d'entre eux (ou un seul avec plusieurs exemplaires) possedent ce tier,
     * une seule potion disparait au total : les autres potions, y compris de tiers
     * inferieurs, restent intactes dans les inventaires.
     */
    private static void consommerMeilleurePotion(@Nonnull List<UUID> soigneurs) {
        TierPotionResurrection meilleur = null;
        for (UUID uuidSoigneur : soigneurs) {
            meilleur = TierPotionResurrection.meilleur(
                    meilleur, PotionsResurrection.meilleurTierDetenu(uuidSoigneur));
        }
        if (meilleur == null) {
            return;
        }
        for (UUID uuidSoigneur : soigneurs) {
            if (PotionsResurrection.consommer(uuidSoigneur, meilleur)) {
                return;
            }
        }
    }

    /** Detache le soigneur de sa cible et retire son HUD. */
    private void annuler(@Nonnull UUID uuidSoigneur,
                         @Nullable Player soigneur,
                         @Nonnull PlayerRef soigneurRef) {
        gestionnaire.arreterReleve(uuidSoigneur);
        hud.masquerSoigneur(soigneur, soigneurRef);
        INTERRUPTIONS.put(uuidSoigneur, System.currentTimeMillis());
    }

    /** Oublie la temporisation d'un joueur, a sa deconnexion. */
    public static void oublier(@Nullable UUID uuidSoigneur) {
        if (uuidSoigneur != null) {
            INTERRUPTIONS.remove(uuidSoigneur);
        }
    }

    /**
     * Annule le relevement en cours d'un soigneur qui vient de prendre des degats.
     * Appele par {@link SystemeDegatsSoigneur}.
     */
    public void annulerPourDegats(@Nonnull UUID uuidSoigneur, @Nullable PlayerRef soigneurRef) {
        if (!gestionnaire.estSoigneur(uuidSoigneur)) {
            return;
        }
        gestionnaire.arreterReleve(uuidSoigneur);
        if (soigneurRef != null) {
            hud.masquerSoigneur(null, soigneurRef);
            envoyer(soigneurRef, "Relèvement interrompu : vous avez été touché");
        }
    }

    // --- Utilitaires --------------------------------------------------------

    @Nullable
    private static Ref<EntityStore> referenceDe(@Nullable PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        try {
            return playerRef.getReference();
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    private static void envoyer(@Nullable PlayerRef destinataire, @Nonnull String texte) {
        if (destinataire == null || !destinataire.isValid()) {
            return;
        }
        try {
            destinataire.sendMessage(Message.raw(texte));
        } catch (RuntimeException ignore) {
            // Deconnexion en cours : le message est simplement perdu.
        }
    }

    @Nonnull
    private static String nomDe(@Nonnull PlayerRef playerRef) {
        try {
            String nom = playerRef.getUsername();
            return nom == null ? "?" : nom;
        } catch (RuntimeException ignore) {
            return "?";
        }
    }
}
