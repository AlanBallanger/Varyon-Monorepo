package fr.varyon.death.hud;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * HUD affiche au soigneur pendant qu'il releve un allie : barre de progression,
 * nom de la cible et rappel des conditions d'annulation.
 */
public final class HudSoigneur extends CustomUIHud {

    public static final String CLE = "varyon-revive:soigneur";

    private static final String CHEMIN_UI = "HUD/Revive/ReviveSoigneurHud.ui";

    private volatile float progression;
    private volatile String nomCible = "";
    private volatile int nombreSoigneurs = 1;

    /**
     * Passe a vrai quand le serveur a effectivement construit le HUD. Toute mise a jour
     * envoyee avant cet instant est perdue, le HUD n'existant pas encore cote client.
     */
    private volatile boolean construit;

    public HudSoigneur(@Nonnull PlayerRef playerRef) {
        super(playerRef, CLE);
    }

    public void rafraichir(float progression, @Nonnull String nomCible, int nombreSoigneurs) {
        this.progression = progression < 0f ? 0f : Math.min(progression, 1f);
        this.nomCible = nomCible;
        this.nombreSoigneurs = Math.max(1, nombreSoigneurs);

        if (!construit) {
            // Les valeurs sont conservees : build() les appliquera des sa construction.
            return;
        }
        UICommandBuilder commandes = new UICommandBuilder();
        appliquerValeurs(commandes);
        update(false, commandes);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandes) {
        commandes.append(CHEMIN_UI);
        appliquerValeurs(commandes);
        construit = true;
    }

    /**
     * Largeur interieure disponible pour la barre : {@code Width} de {@code #ReviveSoigneurBox}
     * (340) moins le padding horizontal applique au conteneur (12 de chaque cote).
     */
    private static final int LARGEUR_BARRE_PX = 340 - 2 * 12;

    private void appliquerValeurs(@Nonnull UICommandBuilder commandes) {
        commandes.set("#ReviveSoigneurTitre.Text", "Relèvement de " + nomCible);
        // Pas de ProgressBar/BarTexturePath (aucune texture de remplissage lineaire fiable
        // dans ce moteur) : la largeur en pixels d'un Group colore est recalculee directement.
        Anchor ancrage = new Anchor();
        ancrage.setLeft(Value.of(0));
        ancrage.setTop(Value.of(0));
        ancrage.setBottom(Value.of(0));
        ancrage.setWidth(Value.of(Math.round(progression * LARGEUR_BARRE_PX)));
        commandes.setObject("#ReviveSoigneurBarreFill.Anchor", ancrage);
        // Affiche « x2 », « x3 »... des qu'un renfort accelere le relevement.
        commandes.set("#ReviveSoigneurMultiplicateur.Text",
                nombreSoigneurs > 1 ? "x" + nombreSoigneurs : "");
        commandes.set("#ReviveSoigneurIndice.Text", "Restez accroupi et immobile");
    }
}
