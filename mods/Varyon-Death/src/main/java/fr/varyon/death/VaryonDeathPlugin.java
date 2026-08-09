package fr.varyon.death;

import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;

import fr.varyon.death.carte.FournisseurMarqueurs;
import fr.varyon.death.combat.BanqueRecaps;
import fr.varyon.death.combat.SuiviCombat;
import fr.varyon.death.command.DeathRecapCommand;
import fr.varyon.death.command.ReleverCommand;
import fr.varyon.death.config.ConfigDeath;
import fr.varyon.death.config.Diagnostic;
import fr.varyon.death.config.GestionnairePreferences;
import fr.varyon.death.config.PreferencesRecap;
import fr.varyon.death.etat.GestionnaireATerre;
import fr.varyon.death.etat.OutilsJoueur;
import fr.varyon.death.hud.GestionnaireHud;
import fr.varyon.death.systeme.FiltrePaquetsATerre;
import fr.varyon.death.systeme.SystemeDegatsRecus;
import fr.varyon.death.systeme.SystemeDegatsSoigneur;
import fr.varyon.death.systeme.SystemeDesaggro;
import fr.varyon.death.systeme.SystemeInterceptionMort;
import fr.varyon.death.systeme.SystemeRecapMort;
import fr.varyon.death.systeme.SystemeReleve;
import fr.varyon.death.systeme.SystemeReleveValeurs;
import fr.varyon.death.systeme.SystemeTickATerre;
import fr.varyon.death.systeme.SystemesBlocage;

/**
 * Point d'entree du mod Varyon-Death : etat « a terre » et recapitulatif de mort.
 *
 * <p>Les deux fonctionnalites etaient a l'origine deux mods distincts. Leur reunion supprime
 * une classe entiere de problemes : l'etat a terre annule le coup fatal et differe la mort de
 * plusieurs minutes, ce que le recapitulatif doit connaitre pour ne pas perdre le coup mortel
 * ni laisser expirer sa session de combat. Cette coordination passait auparavant par des
 * dependances entre plugins, fragiles et parfois rejetees par le graphe de systemes.
 *
 * <p>Le cycle complet est donc :
 * <ol>
 *   <li>chaque degat subi alimente le suivi de combat ;</li>
 *   <li>le coup fatal est intercepte : il est verse au recapitulatif, la session est gelee,
 *       puis le joueur passe a terre ;</li>
 *   <li>le joueur est releve par un allie, ou meurt a l'expiration du saignement ;</li>
 *   <li>a la mort reelle, le recapitulatif est fige puis affiche a la reapparition.</li>
 * </ol>
 */
public final class VaryonDeathPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeath");

    /** Duree de vie d'une session de combat en l'absence de nouveau coup. */
    private static final long EXPIRATION_SESSION_MS = 10_000L;
    /** Duree maximale couverte par un recapitulatif. */
    private static final long DUREE_MAX_COMBAT_MS = 60_000L;
    /** Peremption d'un recapitulatif en attente d'affichage. */
    private static final long PEREMPTION_RECAP_MS = 300_000L;
    private static final int MENACES_SUIVIES = 20;
    private static final int MENACES_AFFICHEES = 10;

    private GestionnaireATerre gestionnaire;
    private GestionnaireHud hud;
    private GestionnairePreferences preferences;

    public VaryonDeathPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        ConfigDeath config = ConfigDeath.charger(getDataDirectory());
        Diagnostic.definir(config.isLogsDiagnostic());
        this.gestionnaire = new GestionnaireATerre(config);
        this.hud = new GestionnaireHud();
        GestionnaireATerre.lier(gestionnaire);

        this.preferences = new GestionnairePreferences(getDataDirectory());
        this.preferences.initialize();
        PreferencesRecap.bind(preferences);

        configurerRecapitulatif(config);
        enregistrerSystemes();
        enregistrerEvenements();

        // Lit la touche Accroupi dans les paquets bruts du client, fige l'orientation du
        // corps et bloque les attaques d'un joueur a terre.
        PacketAdapters.registerInbound((PlayerPacketFilter) new FiltrePaquetsATerre());

        LOGGER.at(Level.INFO).log(
                "Varyon-Death pret : saignement=%d s, relevement=%d s, portee=%.1f blocs, PV rendus=%d %%.",
                config.getDureeSaignementSecondes(),
                config.getDureeReleveSecondes(),
                config.getDistanceMaxBlocs(),
                config.getPvRendusPourcent());
        if (!config.isLogsDiagnostic()) {
            LOGGER.at(Level.INFO).log(
                    "Journalisation detaillee desactivee. Pour diagnostiquer, passez "
                    + "\"logs_diagnostic\": true dans config.json puis redemarrez.");
        }
    }

    /**
     * La session de combat doit survivre a tout l'etat a terre : la mort reelle survient apres
     * le saignement, souvent plusieurs minutes apres le dernier coup recu. Sans cette marge,
     * elle aurait expire au deces et le recapitulatif serait vide.
     */
    private void configurerRecapitulatif(@Nonnull ConfigDeath config) {
        long margeATerre = (config.getDureeSaignementSecondes() + 60L) * 1_000L;
        SuiviCombat.applyConfig(true,
                Math.max(EXPIRATION_SESSION_MS, margeATerre),
                Math.max(DUREE_MAX_COMBAT_MS, margeATerre),
                MENACES_SUIVIES,
                MENACES_AFFICHEES);
        BanqueRecaps.applyConfig(Math.max(PEREMPTION_RECAP_MS, margeATerre));
    }

    private void enregistrerSystemes() {
        SystemeReleve systemeReleve = new SystemeReleve(gestionnaire, hud);

        // Le releve des valeurs doit preceder la lecture des degats : il capture le montant
        // reel avant que le coup fatal ne soit annule ou remis a zero par un autre mod.
        registre(new SystemeReleveValeurs());
        registre(new SystemeDegatsRecus());

        registre(new SystemeInterceptionMort(gestionnaire, hud));
        registre(new SystemeRecapMort());
        registre(new SystemeTickATerre(gestionnaire, hud));
        registre(systemeReleve);
        registre(new SystemeDegatsSoigneur(gestionnaire, systemeReleve));
        registre(new SystemeDesaggro(gestionnaire));

        registre(new SystemesBlocage.BlocageCasseBloc(gestionnaire));
        registre(new SystemesBlocage.BlocageDegatsBloc(gestionnaire));
        registre(new SystemesBlocage.BlocagePoseBloc(gestionnaire));
        registre(new SystemesBlocage.BlocageJetObjet(gestionnaire));
        registre(new SystemesBlocage.BlocageRamassage(gestionnaire));
    }

    private void registre(@Nonnull ISystem systeme) {
        getEntityStoreRegistry().registerSystem(systeme);
    }

    @Override
    protected void start() {
        try {
            getCommandRegistry().registerCommand(new DeathRecapCommand());
        } catch (Exception erreur) {
            LOGGER.at(Level.WARNING).log(
                    "Echec enregistrement de la commande /mort : %s", erreur.getMessage());
        }
        try {
            getCommandRegistry().registerCommand(new ReleverCommand());
        } catch (Exception erreur) {
            LOGGER.at(Level.WARNING).log(
                    "Echec enregistrement de la commande /relever : %s", erreur.getMessage());
        }
    }

    private void enregistrerEvenements() {
        // Les marqueurs de carte sont attaches a chaque monde au moment ou il est ajoute.
        getEventRegistry().registerGlobal(AddWorldEvent.class, this::onMondeAjoute);
        // Une deconnexion en etat a terre est une mort definitive : on nettoie tout.
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onDeconnexion);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, evenement -> {
            PlayerRef playerRef = evenement.getHolder().getComponent(PlayerRef.getComponentType());
            if (playerRef != null) {
                PreferencesRecap.ensureLoaded(playerRef.getUuid());
            }
        });
    }

    private void onMondeAjoute(@Nullable AddWorldEvent evenement) {
        if (evenement == null || gestionnaire == null) {
            return;
        }
        try {
            WorldMapManager carte = evenement.getWorld().getWorldMapManager();
            if (carte != null) {
                carte.addMarkerProvider(FournisseurMarqueurs.CLE, new FournisseurMarqueurs(gestionnaire));
            }
        } catch (RuntimeException erreur) {
            LOGGER.at(Level.WARNING).log(
                    "Marqueurs de carte non enregistres : %s", erreur.getMessage());
        }
    }

    /**
     * Nettoie l'etat d'un joueur qui se deconnecte. Une deconnexion en etat a terre est
     * une mort definitive : le joueur ne revient pas a terre. S'il relevait un allie, le
     * relevement est interrompu.
     *
     * <p>La mobilite est restauree explicitement : l'immobilisation modifie les reglages
     * de deplacement, qui sont sauvegardes avec le joueur. Sans cette remise a zero, il
     * reviendrait fige a sa prochaine connexion.
     */
    private void onDeconnexion(@Nullable PlayerDisconnectEvent evenement) {
        if (evenement == null || gestionnaire == null) {
            return;
        }
        PlayerRef playerRef = evenement.getPlayerRef();
        UUID uuid = playerRef == null ? null : playerRef.getUuid();
        if (uuid == null) {
            return;
        }
        boolean etaitATerre = gestionnaire.estATerre(uuid);
        gestionnaire.retirerDeLEtatATerre(uuid);
        gestionnaire.arreterReleve(uuid);
        hud.oublier(uuid);
        FiltrePaquetsATerre.oublier(uuid);
        SystemeReleve.oublier(uuid);
        SuiviCombat.get().clear(uuid);

        if (etaitATerre) {
            rendreMobiliteAvantDeconnexion(playerRef);
        }
    }

    private void rendreMobiliteAvantDeconnexion(@Nonnull PlayerRef playerRef) {
        try {
            Ref<EntityStore> reference = playerRef.getReference();
            if (reference == null || !reference.isValid()) {
                return;
            }
            OutilsJoueur.rendreMobilite(reference, reference.getStore());
        } catch (RuntimeException erreur) {
            LOGGER.at(Level.FINE).log(
                    "Mobilite non restauree a la deconnexion : %s", erreur.getMessage());
        }
    }

    @Override
    protected void shutdown() {
        if (preferences != null) {
            preferences.flush();
        }
        if (gestionnaire != null) {
            gestionnaire.toutEffacer();
        }
        SuiviCombat.get().clearAll();
        BanqueRecaps.get().clearAll();
        LOGGER.at(Level.INFO).log("Varyon-Death arrete.");
    }
}
