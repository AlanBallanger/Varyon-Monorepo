package fr.varyon.death.etat;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

import fr.varyon.death.config.ConfigDeath;

/**
 * Registre central des joueurs a terre et des relevements en cours.
 *
 * <p>Les collections sont concurrentes car les marqueurs de carte sont lus depuis un autre
 * contexte que le tick monde. Toutes les mutations, elles, proviennent des systemes ECS.
 */
public final class GestionnaireATerre {

    private static volatile GestionnaireATerre instance;

    private final ConfigDeath config;

    /** Joueurs actuellement a terre, par UUID. */
    private final Map<UUID, EtatATerre> aTerreParJoueur = new ConcurrentHashMap<>();

    /** Cible en cours de relevement, par soigneur. Un soigneur ne releve qu'un joueur a la fois. */
    private final Map<UUID, UUID> cibleParSoigneur = new ConcurrentHashMap<>();

    /** Position du soigneur au demarrage du relevement, pour detecter qu'il a bouge. */
    private final Map<UUID, double[]> ancrageParSoigneur = new ConcurrentHashMap<>();

    /** Instant (epoch ms) jusqu'auquel un joueur fraichement releve reste invulnerable. */
    private final Map<UUID, Long> invulnerabiliteApresReleveJusqua = new ConcurrentHashMap<>();

    public GestionnaireATerre(@Nonnull ConfigDeath config) {
        this.config = config;
    }

    public static void lier(@Nonnull GestionnaireATerre gestionnaire) {
        instance = gestionnaire;
    }

    @Nullable
    public static GestionnaireATerre get() {
        return instance;
    }

    @Nonnull
    public ConfigDeath getConfig() {
        return config;
    }

    // --- Etat a terre -------------------------------------------------------

    /**
     * @param coupFatal le coup qui a mis a terre, conserve pour etre rejoue a la mort et
     *                  preserver la vraie cause affichee par le jeu
     */
    @Nonnull
    public EtatATerre mettreATerre(@Nonnull UUID uuidJoueur,
                                   @Nonnull String nomJoueur,
                                   @Nullable Damage coupFatal) {
        EtatATerre etat =
                new EtatATerre(uuidJoueur, nomJoueur, config.getDureeSaignementTicks(), coupFatal);
        aTerreParJoueur.put(uuidJoueur, etat);
        return etat;
    }

    public boolean estATerre(@Nullable UUID uuidJoueur) {
        return uuidJoueur != null && aTerreParJoueur.containsKey(uuidJoueur);
    }

    @Nullable
    public EtatATerre getEtat(@Nullable UUID uuidJoueur) {
        return uuidJoueur == null ? null : aTerreParJoueur.get(uuidJoueur);
    }

    @Nonnull
    public Collection<EtatATerre> tousLesEtats() {
        return Collections.unmodifiableCollection(aTerreParJoueur.values());
    }

    /**
     * Retire un joueur de l'etat a terre et annule proprement tous les relevements
     * dont il etait la cible. A appeler aussi bien pour un relevement reussi que pour
     * une mort par saignement, un abandon ou une deconnexion.
     */
    @Nullable
    public EtatATerre retirerDeLEtatATerre(@Nullable UUID uuidJoueur) {
        if (uuidJoueur == null) {
            return null;
        }
        EtatATerre etat = aTerreParJoueur.remove(uuidJoueur);
        if (etat != null) {
            for (UUID uuidSoigneur : etat.getSoigneurs()) {
                cibleParSoigneur.remove(uuidSoigneur);
                ancrageParSoigneur.remove(uuidSoigneur);
            }
        }
        // Si ce joueur etait lui-meme en train de relever quelqu'un, on coupe aussi ce lien.
        arreterReleve(uuidJoueur);
        return etat;
    }

    // --- Relevement ---------------------------------------------------------

    /**
     * Enregistre le debut d'un relevement. La position fournie sert d'ancrage : si le
     * soigneur s'en eloigne de plus que la tolerance configuree, le relevement est annule.
     */
    public void demarrerReleve(@Nonnull UUID uuidSoigneur,
                               @Nonnull EtatATerre cible,
                               double x, double y, double z) {
        cibleParSoigneur.put(uuidSoigneur, cible.getUuidJoueur());
        ancrageParSoigneur.put(uuidSoigneur, new double[] {x, y, z});
        cible.ajouterSoigneur(uuidSoigneur);
    }

    /** Detache un soigneur de sa cible, sans toucher a l'etat a terre de celle-ci. */
    public void arreterReleve(@Nullable UUID uuidSoigneur) {
        if (uuidSoigneur == null) {
            return;
        }
        UUID uuidCible = cibleParSoigneur.remove(uuidSoigneur);
        ancrageParSoigneur.remove(uuidSoigneur);
        if (uuidCible != null) {
            EtatATerre cible = aTerreParJoueur.get(uuidCible);
            if (cible != null) {
                cible.retirerSoigneur(uuidSoigneur);
            }
        }
    }

    public boolean estSoigneur(@Nullable UUID uuidSoigneur) {
        return uuidSoigneur != null && cibleParSoigneur.containsKey(uuidSoigneur);
    }

    @Nullable
    public EtatATerre getCibleDuSoigneur(@Nullable UUID uuidSoigneur) {
        if (uuidSoigneur == null) {
            return null;
        }
        UUID uuidCible = cibleParSoigneur.get(uuidSoigneur);
        return uuidCible == null ? null : aTerreParJoueur.get(uuidCible);
    }

    @Nullable
    public double[] getAncrage(@Nullable UUID uuidSoigneur) {
        return uuidSoigneur == null ? null : ancrageParSoigneur.get(uuidSoigneur);
    }

    // --- Invulnerabilite post-relevement -------------------------------------

    /** Rend {@code uuidJoueur} invulnerable pour {@code dureeMs} a partir de maintenant. */
    public void accorderInvulnerabiliteTemporaire(@Nonnull UUID uuidJoueur, long dureeMs) {
        invulnerabiliteApresReleveJusqua.put(uuidJoueur, System.currentTimeMillis() + dureeMs);
    }

    /**
     * Vrai si {@code uuidJoueur} beneficie encore de l'invulnerabilite accordee a son dernier
     * relevement. Purge automatiquement l'entree une fois expiree, pour ne pas laisser grossir
     * la map indefiniment.
     */
    public boolean estInvulnerableApresReleve(@Nullable UUID uuidJoueur) {
        if (uuidJoueur == null) {
            return false;
        }
        Long expiration = invulnerabiliteApresReleveJusqua.get(uuidJoueur);
        if (expiration == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiration) {
            invulnerabiliteApresReleveJusqua.remove(uuidJoueur, expiration);
            return false;
        }
        return true;
    }

    /** Vide tout le registre, a l'arret du serveur. */
    public void toutEffacer() {
        aTerreParJoueur.clear();
        cibleParSoigneur.clear();
        ancrageParSoigneur.clear();
        invulnerabiliteApresReleveJusqua.clear();
    }
}
