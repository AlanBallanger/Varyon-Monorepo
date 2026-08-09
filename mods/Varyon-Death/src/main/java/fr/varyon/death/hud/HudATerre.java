package fr.varyon.death.hud;

import java.util.StringJoiner;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * HUD affiche au joueur a terre : compte a rebours du saignement, etat du relevement
 * et progression de l'abandon.
 *
 * <p>Le HUD n'est pas cliquable ({@link CustomUIHud} n'expose aucun evenement) : l'abandon
 * se declenche en maintenant la touche Accroupi, libre puisque le joueur a terre ne peut
 * rien faire d'autre.
 */
public final class HudATerre extends CustomUIHud {

    public static final String CLE = "varyon-revive:a-terre";

    private static final String CHEMIN_UI = "HUD/Revive/ReviveATerreHud.ui";

    private volatile int secondesRestantes;
    private volatile float progressionReleve;
    private volatile float progressionAbandon;
    private volatile int nombreSoigneurs;
    private volatile String nomsSoigneurs = "";

    /**
     * Passe a vrai quand le serveur a effectivement construit le HUD.
     *
     * <p>Toute mise a jour envoyee avant cet instant est perdue : le HUD n'existe pas encore
     * cote client, et les commandes ne s'appliquent a rien. C'est ce qui laissait les barres
     * de progression desesperement vides.
     */
    private volatile boolean construit;

    public HudATerre(@Nonnull PlayerRef playerRef) {
        super(playerRef, CLE);
    }

    /** Met a jour les valeurs affichees puis pousse la mise a jour au client. */
    public void rafraichir(int secondesRestantes,
                           float progressionReleve,
                           float progressionAbandon,
                           int nombreSoigneurs,
                           @Nonnull String nomsSoigneurs) {
        this.secondesRestantes = Math.max(0, secondesRestantes);
        this.progressionReleve = borner(progressionReleve);
        this.progressionAbandon = borner(progressionAbandon);
        this.nombreSoigneurs = Math.max(0, nombreSoigneurs);
        this.nomsSoigneurs = nomsSoigneurs;

        if (!construit) {
            // Les valeurs sont conservees : build() les appliquera des sa construction.
            return;
        }
        UICommandBuilder commandes = new UICommandBuilder();
        appliquerValeurs(commandes);
        update(false, commandes);
    }

    /**
     * Construction initiale : charge le fichier d'interface puis applique les valeurs.
     * Appele une seule fois par le serveur, a l'ajout du HUD.
     */
    @Override
    protected void build(@Nonnull UICommandBuilder commandes) {
        commandes.append(CHEMIN_UI);
        appliquerValeurs(commandes);
        construit = true;
    }

    /** Renseigne les elements du HUD. Utilise aussi bien a la construction qu'aux mises a jour. */
    private void appliquerValeurs(@Nonnull UICommandBuilder commandes) {
        commandes.set("#ReviveATerreTitre.Text", "VOUS ÊTES À TERRE");
        commandes.set("#ReviveATerreChrono.Text", formaterChrono(secondesRestantes));

        boolean enCoursDeReleve = nombreSoigneurs > 0;
        commandes.set("#ReviveATerreMessage.Text", enCoursDeReleve
                ? messageReleve(nombreSoigneurs, nomsSoigneurs)
                : "Aucun allié ne vous relève");
        commandes.set("#ReviveATerreBarreReleve.Visible", enCoursDeReleve);
        commandes.set("#ReviveATerreBarreReleveFill.Value", progressionReleve);

        boolean abandonEnCours = progressionAbandon > 0f;
        commandes.set("#ReviveATerreAbandonTexte.Text", abandonEnCours
                ? "Abandon en cours"
                : "Maintenez [Accroupi] pour abandonner");
        commandes.set("#ReviveATerreBarreAbandon.Visible", abandonEnCours);
        commandes.set("#ReviveATerreBarreAbandonFill.Value", progressionAbandon);
    }

    @Nonnull
    private static String messageReleve(int nombreSoigneurs, @Nonnull String noms) {
        if (noms.isBlank()) {
            return nombreSoigneurs > 1
                    ? nombreSoigneurs + " alliés vous relèvent"
                    : "Un allié vous relève";
        }
        return nombreSoigneurs > 1
                ? noms + " vous relèvent"
                : noms + " vous relève";
    }

    @Nonnull
    public static String formaterChrono(int secondes) {
        int borne = Math.max(0, secondes);
        return String.format("%d:%02d", borne / 60, borne % 60);
    }

    @Nonnull
    public static String joindreNoms(@Nonnull Iterable<String> noms) {
        StringJoiner joiner = new StringJoiner(", ");
        noms.forEach(joiner::add);
        return joiner.toString();
    }

    private static float borner(float valeur) {
        if (valeur < 0f) {
            return 0f;
        }
        return Math.min(valeur, 1f);
    }
}
