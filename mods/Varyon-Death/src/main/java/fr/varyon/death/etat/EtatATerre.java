package fr.varyon.death.etat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

/**
 * Etat d'un joueur a terre, detenu en memoire par {@link GestionnaireATerre}.
 *
 * <p>Rien n'est persiste : une deconnexion en etat a terre est une mort definitive,
 * l'etat disparait donc avec la session. Les compteurs sont en ticks serveur.
 */
public final class EtatATerre {

    private final UUID uuidJoueur;
    private final String nomJoueur;

    /** Ticks restants avant la mort par saignement. */
    private int ticksRestants;

    /** Progression du relevement, de 0 a {@code dureeReleveTicks}. */
    private int ticksReleve;

    /** Ticks de maintien de la touche d'abandon ; -1 quand la touche n'est pas maintenue. */
    private int ticksAbandon = -1;

    /** Soigneurs qui relevent actuellement ce joueur. */
    private final Set<UUID> soigneurs = ConcurrentHashMap.newKeySet();

    /**
     * Coup qui a mis le joueur a terre. Il est rejoue tel quel a la mort, pour que le jeu
     * affiche la vraie cause (le mob, le joueur, la chute...) et non « abandon » ou
     * « saignement », qui ne sont que la facon dont l'etat a terre s'est termine.
     */
    private final Damage coupFatal;

    public EtatATerre(@Nonnull UUID uuidJoueur,
                      @Nonnull String nomJoueur,
                      int ticksRestants,
                      @Nullable Damage coupFatal) {
        this.uuidJoueur = uuidJoueur;
        this.nomJoueur = nomJoueur;
        this.ticksRestants = ticksRestants;
        this.coupFatal = coupFatal;
    }

    /** Le coup qui a mis a terre, a rejouer a la mort. {@code null} si l'origine est inconnue. */
    @Nullable
    public Damage getCoupFatal() {
        return coupFatal;
    }

    @Nonnull
    public UUID getUuidJoueur() {
        return uuidJoueur;
    }

    @Nonnull
    public String getNomJoueur() {
        return nomJoueur;
    }

    public int getTicksRestants() {
        return ticksRestants;
    }

    public void setTicksRestants(int ticksRestants) {
        this.ticksRestants = ticksRestants;
    }

    public int getTicksReleve() {
        return ticksReleve;
    }

    public void setTicksReleve(int ticksReleve) {
        this.ticksReleve = Math.max(0, ticksReleve);
    }

    public int getTicksAbandon() {
        return ticksAbandon;
    }

    public void setTicksAbandon(int ticksAbandon) {
        this.ticksAbandon = ticksAbandon;
    }

    public boolean isAbandonEnCours() {
        return ticksAbandon >= 0;
    }

    // --- Soigneurs ----------------------------------------------------------

    public void ajouterSoigneur(@Nonnull UUID uuidSoigneur) {
        soigneurs.add(uuidSoigneur);
    }

    public void retirerSoigneur(@Nonnull UUID uuidSoigneur) {
        soigneurs.remove(uuidSoigneur);
        // Sans soigneur la progression repart de zero : un relevement interrompu
        // ne doit pas laisser de credit au suivant.
        if (soigneurs.isEmpty()) {
            ticksReleve = 0;
        }
    }

    @Nonnull
    public Set<UUID> getSoigneurs() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(soigneurs));
    }

    public int getNombreSoigneurs() {
        return soigneurs.size();
    }

    public boolean isEnCoursDeReleve() {
        return !soigneurs.isEmpty();
    }
}
