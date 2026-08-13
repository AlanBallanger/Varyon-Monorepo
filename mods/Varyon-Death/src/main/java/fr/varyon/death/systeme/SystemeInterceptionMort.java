package fr.varyon.death.systeme;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.BanqueRecaps;
import fr.varyon.death.combat.SuiviCombat;
import fr.varyon.death.config.ConfigDeath;
import fr.varyon.death.config.PreferencesRecap;
import fr.varyon.death.etat.EtatATerre;
import fr.varyon.death.etat.GestionnaireATerre;
import fr.varyon.death.etat.OutilsJoueur;
import fr.varyon.death.hud.GestionnaireHud;
import fr.varyon.death.ui.DeathRecapPage;

/**
 * Remplace la mort d'un joueur par l'etat a terre.
 *
 * <p>Le systeme s'execute APRES le groupe de filtrage des degats et AVANT
 * {@code DamageSystems.ApplyDamage} : on peut ainsi annuler le coup fatal avant qu'il ne
 * retire les derniers points de vie, ce qui evite tout passage par l'ecran de mort.
 *
 * <p>Deux cas ne passent jamais a terre : les joueurs en mode Creatif (ils ne meurent pas
 * normalement) et ceux qui sont deja a terre — pour ces derniers le mod maintient
 * l'invulnerabilite, aucun degat ne doit les atteindre.
 */
public final class SystemeInterceptionMort extends DamageEventSystem {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonRevive-Mort");

    private final GestionnaireATerre gestionnaire;
    private final GestionnaireHud hud;

    public SystemeInterceptionMort(@Nonnull GestionnaireATerre gestionnaire,
                                   @Nonnull GestionnaireHud hud) {
        this.gestionnaire = gestionnaire;
        this.hud = hud;
    }

    /**
     * Le systeme appartient au groupe {@code FilterDamage}, comme le
     * {@code DamageSystems.FilterUnkillable} du serveur : c'est la phase prevue pour annuler
     * un degat avant qu'il ne soit applique.
     *
     * <p>Aucune dependance n'est declaree, volontairement. Exiger en plus de s'executer
     * apres l'integralite de {@code FilterDamage} tout en appartenant a ce groupe formait
     * un cycle, et le serveur refusait alors de charger le mod.
     */
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    /**
     * S'execute apres les reductions d'armure du serveur, pour que {@code getAmount()}
     * reflete les degats reellement encaisses : sans cela on comparerait des degats bruts
     * aux points de vie et on mettrait a terre des joueurs qui survivaient au coup.
     */
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, DamageSystems.ArmorDamageReduction.class));
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> tampon,
                       @Nullable Damage degats) {
        if (degats == null || degats.isCancelled()) {
            return;
        }
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        // Joueur deja a terre : il est invulnerable, on absorbe le degat quel qu'il soit.
        if (gestionnaire.estATerre(uuid)) {
            degats.setAmount(0f);
            degats.setCancelled(true);
            return;
        }
        // Invulnerabilite temporaire accordee a la reussite d'un relevement (SystemeReleve),
        // le temps que le joueur reprenne ses reperes avant de pouvoir etre touche a nouveau.
        if (gestionnaire.estInvulnerableApresReleve(uuid)) {
            degats.setAmount(0f);
            degats.setCancelled(true);
            return;
        }

        float montant = degats.getAmount();
        if (montant <= 0f) {
            return;
        }
        float pointsDeVie = OutilsJoueur.pointsDeVie(ref, tampon);
        if (pointsDeVie < 0f) {
            pointsDeVie = OutilsJoueur.pointsDeVie(ref, store);
        }
        // Coup non fatal : le jeu suit son cours normal.
        if (pointsDeVie < 0f || montant < pointsDeVie) {
            return;
        }

        Player joueur = chunk.getComponent(index, Player.getComponentType());
        if (estExclu(joueur)) {
            return;
        }

        // A partir d'ici le coup aurait tue : on l'annule et on passe le joueur a terre.
        // Le coup est conserve pour etre rejoue si le joueur finit par mourir, afin que le
        // jeu affiche la vraie cause plutot qu'un motif invente par le mod.
        //
        // Avant de l'annuler, on le porte au recapitulatif : une fois annule, l'evenement
        // n'atteint plus le groupe InspectDamage et le coup fatal — le plus important du
        // recap — serait perdu. On gele ensuite la session pour qu'elle survive au
        // saignement, qui dure bien plus longtemps que le delai d'expiration normal.
        SuiviCombat.get().enregistrerDegats(uuid, degats, sourceEntite(degats), store, tampon,
                Math.max(0f, degats.getInitialAmount()), montant);
        SuiviCombat.get().gelerPourMortDifferee(uuid);

        degats.setAmount(0f);
        degats.setCancelled(true);
        mettreATerre(uuid, joueur, playerRef, ref, store, tampon, degats);
    }

    /**
     * Entite a l'origine du coup, ou {@code null} pour une cause environnementale.
     * Pour un projectile, {@code getRef()} designe deja le tireur et non la fleche.
     */
    @Nullable
    private static Ref<EntityStore> sourceEntite(@Nonnull Damage degats) {
        if (!(degats.getSource() instanceof Damage.EntitySource source)) {
            return null;
        }
        Ref<EntityStore> reference = source.getRef();
        return reference != null && reference.isValid() ? reference : null;
    }

    /** Les modes Creatif et Spectateur meurent (ou non) selon les regles du jeu, sans etat a terre. */
    private static boolean estExclu(@Nullable Player joueur) {
        if (joueur == null) {
            return true;
        }
        try {
            GameMode mode = joueur.getGameMode();
            return mode != null && mode != GameMode.Adventure;
        } catch (RuntimeException ignore) {
            return false;
        }
    }

    private void mettreATerre(@Nonnull UUID uuid,
                              @Nullable Player joueur,
                              @Nonnull PlayerRef playerRef,
                              @Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> tampon,
                              @Nonnull Damage coupFatal) {
        ConfigDeath config = gestionnaire.getConfig();
        String nom = nomDe(playerRef);
        EtatATerre etat = gestionnaire.mettreATerre(uuid, nom, coupFatal);

        // Un point de vie symbolique : le joueur n'est pas mort, et l'invulnerabilite
        // geree plus haut garantit qu'il ne peut plus en perdre.
        OutilsJoueur.definirPointsDeVie(ref, tampon, 1f);
        // Le personnage doit etre couche AVANT de basculer la camera : sinon celle-ci se
        // retrouve a l'interieur du modele reste debout.
        OutilsJoueur.jouerAnimationATerre(ref, tampon);
        OutilsJoueur.activerCameraATerre(playerRef, config);

        // AttitudeProviderATerre empeche tout NOUVEAU ciblage tant que le joueur est a terre,
        // mais un NPC ayant deja verrouille sa cible juste avant cet instant la garderait sans
        // ce reset immediat de sa memoire de ciblage.
        ResetAggroATerre.resetAggroAutourDe(ref, store);

        hud.afficherATerre(joueur, playerRef, config.getDureeSaignementSecondes(),
                0f, 0f, 0, "");

        LOGGER.at(Level.INFO).log("%s est a terre pour %d s (etat pose=%s).",
                nom, config.getDureeSaignementSecondes(), gestionnaire.estATerre(uuid));

        ouvrirRecapATerre(uuid, playerRef, tampon);
    }

    /**
     * Ouvre le recapitulatif des lors que le joueur tombe a terre, plutot qu'a la
     * reapparition : a cet instant le joueur est deja stable dans le monde (aucune transition
     * de reapparition en cours) et aucun ecran de mort vanilla ne bloque l'ouverture de la page.
     */
    private void ouvrirRecapATerre(@Nonnull UUID uuid,
                                   @Nonnull PlayerRef playerRef,
                                   @Nonnull CommandBuffer<EntityStore> tampon) {
        if (!PreferencesRecap.estActif(uuid)) {
            return;
        }
        SuiviCombat.Snapshot snapshot = SuiviCombat.get().snapshotSansConsommer(uuid);
        if (snapshot == null || snapshot.topThreats().isEmpty()) {
            return;
        }
        BanqueRecaps.get().put(uuid, snapshot);
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        World world = store != null && store.getExternalData() != null
                ? store.getExternalData().getWorld()
                : null;
        if (world == null) {
            return;
        }
        world.execute(() -> {
            try {
                DeathRecapPage.openFor(playerRef, snapshot);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Ouverture du recap a terre echouee pour %s: %s",
                        uuid, e.getMessage());
            }
        });
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
