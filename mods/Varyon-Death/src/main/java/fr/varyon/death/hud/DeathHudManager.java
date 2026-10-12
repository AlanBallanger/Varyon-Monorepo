package fr.varyon.death.hud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Cree, met a jour et retire les HUD du mod.
 *
 * <p>Les instances sont conservees par joueur : {@link com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud}
 * pousse une mise a jour differentielle, il ne faut donc pas en recreer une a chaque tick.
 */
public final class GestionnaireHud {

    private static volatile GestionnaireHud instance;

    private final Map<UUID, HudATerre> hudATerre = new ConcurrentHashMap<>();
    private final Map<UUID, HudSoigneur> hudSoigneur = new ConcurrentHashMap<>();

    public GestionnaireHud() {
        instance = this;
    }

    /** Accessible sans reference explicite, pour les commandes qui n'en recoivent pas une. */
    @Nullable
    public static GestionnaireHud get() {
        return instance;
    }

    // --- Joueur a terre -----------------------------------------------------

    public void afficherATerre(@Nullable Player joueur,
                               @Nullable PlayerRef playerRef,
                               int secondesRestantes,
                               float progressionReleve,
                               float progressionAbandon,
                               int nombreSoigneurs,
                               @Nonnull String nomsSoigneurs) {
        UUID uuid = uuidDe(playerRef);
        HudManager gestionnaire = gestionnaireDe(joueur, playerRef);
        if (uuid == null || gestionnaire == null) {
            return;
        }
        HudATerre hud = hudATerre.computeIfAbsent(uuid, ignore -> {
            HudATerre nouveau = new HudATerre(playerRef);
            gestionnaire.addCustomHud(playerRef, nouveau);
            return nouveau;
        });
        try {
            hud.rafraichir(secondesRestantes, progressionReleve, progressionAbandon,
                    nombreSoigneurs, nomsSoigneurs);
        } catch (RuntimeException ignore) {
            // Le joueur peut s'etre deconnecte entre la lecture de l'etat et l'envoi.
        }
    }

    /**
     * Affiche ou masque le HUD "a terre" sans le detruire : utilise quand {@code DeathRecapPage}
     * est depliee, pour eviter que les deux se superposent a l'ecran.
     */
    public void definirATerreMasqueParRecap(@Nullable UUID uuid, boolean masque) {
        if (uuid == null) {
            return;
        }
        HudATerre hud = hudATerre.get(uuid);
        if (hud == null) {
            return;
        }
        try {
            hud.definirMasqueParRecap(masque);
        } catch (RuntimeException ignore) {
            // Le joueur peut s'etre deconnecte entre-temps.
        }
    }

    public void masquerATerre(@Nullable Player joueur, @Nullable PlayerRef playerRef) {
        UUID uuid = uuidDe(playerRef);
        if (uuid == null) {
            return;
        }
        hudATerre.remove(uuid);
        retirer(joueur, playerRef, HudATerre.CLE);
    }

    // --- Soigneur -----------------------------------------------------------

    public void afficherSoigneur(@Nullable Player joueur,
                                 @Nullable PlayerRef playerRef,
                                 float progression,
                                 @Nonnull String nomCible,
                                 int nombreSoigneurs) {
        UUID uuid = uuidDe(playerRef);
        HudManager gestionnaire = gestionnaireDe(joueur, playerRef);
        if (uuid == null || gestionnaire == null) {
            return;
        }
        HudSoigneur hud = hudSoigneur.computeIfAbsent(uuid, ignore -> {
            HudSoigneur nouveau = new HudSoigneur(playerRef);
            gestionnaire.addCustomHud(playerRef, nouveau);
            return nouveau;
        });
        try {
            hud.rafraichir(progression, nomCible, nombreSoigneurs);
        } catch (RuntimeException ignore) {
            // Idem : deconnexion possible en cours de relevement.
        }
    }

    public void masquerSoigneur(@Nullable Player joueur, @Nullable PlayerRef playerRef) {
        UUID uuid = uuidDe(playerRef);
        if (uuid == null) {
            return;
        }
        hudSoigneur.remove(uuid);
        retirer(joueur, playerRef, HudSoigneur.CLE);
    }

    /** Retire les deux HUD d'un joueur, a la deconnexion ou a l'arret du mod. */
    public void toutMasquer(@Nullable Player joueur, @Nullable PlayerRef playerRef) {
        masquerATerre(joueur, playerRef);
        masquerSoigneur(joueur, playerRef);
    }

    public void oublier(@Nullable UUID uuid) {
        if (uuid != null) {
            hudATerre.remove(uuid);
            hudSoigneur.remove(uuid);
        }
    }

    // --- Utilitaires --------------------------------------------------------

    private static void retirer(@Nullable Player joueur, @Nullable PlayerRef playerRef, @Nonnull String cle) {
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        HudManager gestionnaire = gestionnaireDe(joueur, playerRef);
        if (gestionnaire == null) {
            return;
        }
        try {
            gestionnaire.removeCustomHud(playerRef, cle);
        } catch (RuntimeException ignore) {
            // Le HUD peut deja avoir ete detruit avec la session du joueur.
        }
    }

    /**
     * Resout le gestionnaire de HUD, en retombant sur le store d'entites lorsque le
     * {@link Player} n'est pas fourni.
     *
     * <p>C'est le cas a la fin d'un relevement : on ne dispose alors que du {@link PlayerRef}
     * des soigneurs. Sans ce repli, leur barre de progression restait affichee indefiniment.
     */
    @Nullable
    private static HudManager gestionnaireDe(@Nullable Player joueur, @Nullable PlayerRef playerRef) {
        HudManager direct = gestionnaireDe(joueur);
        if (direct != null) {
            return direct;
        }
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        try {
            Ref<EntityStore> reference = playerRef.getReference();
            if (reference == null || !reference.isValid()) {
                return null;
            }
            Store<EntityStore> store = reference.getStore();
            if (store == null) {
                return null;
            }
            return gestionnaireDe(store.getComponent(reference, Player.getComponentType()));
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @Nullable
    private static HudManager gestionnaireDe(@Nullable Player joueur) {
        if (joueur == null) {
            return null;
        }
        try {
            return joueur.getHudManager();
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @Nullable
    private static UUID uuidDe(@Nullable PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        try {
            return playerRef.getUuid();
        } catch (RuntimeException ignore) {
            return null;
        }
    }
}
