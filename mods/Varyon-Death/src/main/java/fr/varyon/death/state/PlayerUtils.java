package fr.varyon.death.etat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ApplyLookType;
import com.hypixel.hytale.protocol.AttachedToType;
import com.hypixel.hytale.protocol.CanMoveType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.MouseInputTargetType;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.config.ConfigDeath;

/** Operations bas niveau sur un joueur : points de vie, camera, mise a mort, distances. */
public final class OutilsJoueur {

    private OutilsJoueur() {}

    // --- Points de vie ------------------------------------------------------

    @Nullable
    public static EntityStatMap statistiques(@Nullable Ref<EntityStore> ref,
                                             @Nullable ComponentAccessor<EntityStore> accesseur) {
        if (ref == null || !ref.isValid() || accesseur == null) {
            return null;
        }
        try {
            return accesseur.getComponent(ref, EntityStatMap.getComponentType());
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    /** Points de vie courants, ou -1 si la statistique est introuvable. */
    public static float pointsDeVie(@Nullable Ref<EntityStore> ref,
                                    @Nullable ComponentAccessor<EntityStore> accesseur) {
        EntityStatMap stats = statistiques(ref, accesseur);
        if (stats == null) {
            return -1f;
        }
        EntityStatValue vie = stats.get(DefaultEntityStatTypes.getHealth());
        return vie == null ? -1f : vie.get();
    }

    /** Points de vie maximum, ou -1 si la statistique est introuvable. */
    public static float pointsDeVieMax(@Nullable Ref<EntityStore> ref,
                                       @Nullable ComponentAccessor<EntityStore> accesseur) {
        EntityStatMap stats = statistiques(ref, accesseur);
        if (stats == null) {
            return -1f;
        }
        EntityStatValue vie = stats.get(DefaultEntityStatTypes.getHealth());
        return vie == null ? -1f : vie.getMax();
    }

    /**
     * Fixe les points de vie a la valeur demandee, bornee entre 1 et le maximum du joueur.
     * On ne descend jamais a zero : cela declencherait la mort que le mod cherche a remplacer.
     */
    public static void definirPointsDeVie(@Nullable Ref<EntityStore> ref,
                                          @Nullable ComponentAccessor<EntityStore> accesseur,
                                          float valeur) {
        EntityStatMap stats = statistiques(ref, accesseur);
        if (stats == null) {
            return;
        }
        int idVie = DefaultEntityStatTypes.getHealth();
        EntityStatValue vie = stats.get(idVie);
        if (vie == null) {
            return;
        }
        float borne = Math.max(1f, Math.min(valeur, vie.getMax()));
        stats.setStatValue(EntityStatMap.Predictable.SELF, idVie, borne);
    }

    /**
     * Points de vie a rendre au relevement : un pourcentage de la vie MAXIMUM du joueur
     * releve.
     *
     * <p>Le pourcentage depend du tier de la potion de resurrection consommee (10/30/50/100 %
     * pour mineure/classique/majeure/mythique) : voir {@code TierPotionResurrection.pourcentagePv()}.
     * Pas de valeur de repli implicite ici, l'appelant doit fournir le pourcentage a appliquer.
     */
    public static float pointsDeVieAuRelevement(@Nullable Ref<EntityStore> ref,
                                                @Nullable ComponentAccessor<EntityStore> accesseur,
                                                int pourcentagePv) {
        float max = pointsDeVieMax(ref, accesseur);
        if (max <= 0f) {
            return 1f;
        }
        return Math.max(1f, max * pourcentagePv / 100f);
    }

    // --- Animation ----------------------------------------------------------

    /**
     * Couche le joueur au sol. L'animation « Sleep » est jouee en boucle sur le slot
     * {@link AnimationSlot#Status}, qui n'entre pas en conflit avec les slots de
     * deplacement ou d'action.
     *
     * <p>Sans elle le personnage reste debout : la camera, placee juste au-dessus de lui,
     * se retrouve alors a l'interieur du modele et traverse le corps.
     */
    public static void jouerAnimationATerre(@Nullable Ref<EntityStore> ref,
                                            @Nullable ComponentAccessor<EntityStore> accesseur) {
        if (ref == null || !ref.isValid() || accesseur == null) {
            return;
        }
        try {
            AnimationUtils.playAnimation(ref, AnimationSlot.Status, "Sleep", true, accesseur);
        } catch (RuntimeException ignore) {
            // Le joueur a pu quitter le monde entre-temps.
        }
    }

    /** Remet le joueur debout, au relevement comme a la mort. */
    public static void arreterAnimationATerre(@Nullable Ref<EntityStore> ref,
                                              @Nullable ComponentAccessor<EntityStore> accesseur) {
        if (ref == null || !ref.isValid() || accesseur == null) {
            return;
        }
        try {
            AnimationUtils.stopAnimation(ref, AnimationSlot.Status, true, accesseur);
        } catch (RuntimeException ignore) {
            // Le joueur a pu quitter le monde entre-temps.
        }
    }

    // --- Camera -------------------------------------------------------------

    /**
     * Bascule le joueur en camera exterieure pendant l'etat a terre.
     *
     * <p>La camera est reculee ({@code distance}) et surelevee ({@code positionOffset}) pour
     * que le joueur se voie couche au sol, au lieu d'avoir l'objectif au centre de son
     * propre modele. Le raycast evite qu'elle traverse un mur situe derriere lui.
     *
     * <p>Les entrees de rotation sont neutralisees : un joueur a terre ne doit pas pouvoir
     * pivoter sur lui-meme. Le paquet est en outre envoye verrouille ({@code isLocked}),
     * ce qui empeche le client de reprendre la main sur la vue.
     */
    public static void activerCameraATerre(@Nullable PlayerRef playerRef, @Nonnull ConfigDeath config) {
        PacketHandler handler = gestionnairePaquets(playerRef);
        if (handler == null) {
            return;
        }
        ServerCameraSettings reglages = new ServerCameraSettings();
        reglages.isFirstPerson = false;

        // Recul de la camera, avec raycast pour ne pas passer au travers des murs.
        reglages.distance = (float) config.getCameraDistance();
        reglages.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;
        reglages.positionOffset = new Position(
                config.getCameraDecalageX(),
                config.getCameraDecalageY(),
                config.getCameraDecalageZ());

        // Camera solidaire du joueur.
        reglages.attachedToType = AttachedToType.LocalPlayer;
        reglages.canMoveType = CanMoveType.AttachedToLocalPlayer;

        // Aucune entree souris ne fait tourner le personnage ni la vue.
        reglages.applyLookType = ApplyLookType.Rotation;
        reglages.lookMultiplier = new Vector2f(0f, 0f);
        reglages.allowPitchControls = false;
        reglages.sendMouseMotion = false;
        reglages.mouseInputTargetType = MouseInputTargetType.None;

        // Aucune entree clavier ne deplace le personnage.
        reglages.movementMultiplier = new Vector3f(0f, 0f, 0f);

        reglages.displayCursor = false;
        reglages.displayReticle = false;

        handler.write((ToClientPacket) new SetServerCamera(ClientCameraView.Custom, true, reglages));
    }

    /** Rend au joueur sa camera normale. */
    public static void retablirCamera(@Nullable PlayerRef playerRef) {
        PacketHandler handler = gestionnairePaquets(playerRef);
        if (handler == null) {
            return;
        }
        handler.write((ToClientPacket) new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    @Nullable
    private static PacketHandler gestionnairePaquets(@Nullable PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        try {
            return playerRef.getPacketHandler();
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    // --- Immobilisation -----------------------------------------------------

    /**
     * Immobilise le joueur a terre : vitesse et force de saut ramenees a zero.
     *
     * <p>A appeler a chaque tick. Le serveur reevalue regulierement les reglages de
     * deplacement (changement de monde, effets, monture) : une application unique a la mise
     * a terre finirait par etre ecrasee et rendrait sa mobilite au joueur.
     */
    public static void immobiliser(@Nullable Ref<EntityStore> ref,
                                   @Nullable ComponentAccessor<EntityStore> accesseur,
                                   @Nullable PlayerRef playerRef) {
        MovementManager deplacement = deplacementDe(ref, accesseur);
        if (deplacement == null) {
            return;
        }
        MovementSettings reglages = deplacement.getSettings();
        if (reglages == null) {
            return;
        }
        // Rien a faire si le joueur est deja fige : on evite un paquet reseau par tick.
        if (reglages.baseSpeed == 0f && reglages.jumpForce == 0f) {
            return;
        }
        reglages.baseSpeed = 0f;
        reglages.jumpForce = 0f;
        PacketHandler handler = gestionnairePaquets(playerRef);
        if (handler != null) {
            deplacement.update(handler);
        }
    }

    /**
     * Rend sa mobilite au joueur en restaurant les reglages par defaut du serveur.
     *
     * <p>On ne memorise volontairement pas les anciennes valeurs : les recalculer depuis
     * la source evite de figer une vitesse peripee d'un effet temporaire actif au moment
     * de la mise a terre.
     */
    public static void rendreMobilite(@Nullable Ref<EntityStore> ref,
                                      @Nullable ComponentAccessor<EntityStore> accesseur) {
        MovementManager deplacement = deplacementDe(ref, accesseur);
        if (deplacement == null || ref == null || accesseur == null) {
            return;
        }
        try {
            deplacement.resetDefaultsAndUpdate(ref, accesseur);
        } catch (RuntimeException ignore) {
            // Le joueur a pu quitter le monde entre-temps.
        }
    }

    @Nullable
    private static MovementManager deplacementDe(@Nullable Ref<EntityStore> ref,
                                                 @Nullable ComponentAccessor<EntityStore> accesseur) {
        if (ref == null || !ref.isValid() || accesseur == null) {
            return null;
        }
        try {
            return accesseur.getComponent(ref, MovementManager.getComponentType());
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    // --- Mise a mort --------------------------------------------------------

    /**
     * Tue reellement le joueur, une fois l'etat a terre termine. Le montant enorme garantit
     * que le coup est fatal quels que soient les points de vie et les reductions en place.
     */
    public static void tuer(@Nullable Ref<EntityStore> ref,
                            @Nullable CommandBuffer<EntityStore> tampon,
                            @Nullable Damage coupFatal) {
        if (ref == null || !ref.isValid() || tampon == null) {
            return;
        }
        try {
            if (tampon.getComponent(ref, DeathComponent.getComponentType()) != null) {
                return;
            }
            DeathComponent.tryAddComponent(tampon, ref, mortelle(coupFatal));
        } catch (RuntimeException ignore) {
            // Le joueur a pu quitter le monde entre-temps.
        }
    }

    /**
     * Reconstruit le coup d'origine avec un montant garanti fatal.
     *
     * <p>On conserve sa source et sa cause : c'est ce qui determine le message de mort
     * affiche au joueur. Le mod ne doit pas s'y substituer — un joueur tue par un zombie
     * doit lire qu'un zombie l'a tue, et non « abandon » ou « saignement », qui decrivent
     * seulement la facon dont l'etat a terre s'est termine.
     *
     * <p>Faute de coup d'origine connu, on retombe sur {@code OUT_OF_WORLD}, la cause
     * generique du serveur pour une mort forcee : elle contourne les resistances.
     */
    @Nonnull
    private static Damage mortelle(@Nullable Damage coupFatal) {
        float montant = Float.MAX_VALUE / 2f;
        if (coupFatal == null) {
            return new Damage(
                    new Damage.EnvironmentSource("downed"), DamageCause.OUT_OF_WORLD, montant);
        }
        Damage.Source source = coupFatal.getSource();
        DamageCause cause = coupFatal.getCause();
        if (source == null || cause == null) {
            return new Damage(
                    new Damage.EnvironmentSource("downed"), DamageCause.OUT_OF_WORLD, montant);
        }
        return new Damage(source, cause, montant);
    }

    // --- Distances ----------------------------------------------------------

    /** Distance au carre entre deux joueurs, ou {@link Double#MAX_VALUE} si indisponible. */
    public static double distanceCarree(@Nullable PlayerRef a, @Nullable PlayerRef b) {
        Vector3d posA = position(a);
        Vector3d posB = position(b);
        if (posA == null || posB == null) {
            return Double.MAX_VALUE;
        }
        return posA.distanceSquared(posB);
    }

    /** Distance au carre entre un joueur et un point d'ancrage {x, y, z}. */
    /**
     * Ecart horizontal au carre entre le joueur et son point d'ancrage.
     *
     * <p>L'altitude est volontairement ignoree : s'accroupir, se relever ou la simple
     * correction de hauteur du serveur font varier Y de plusieurs dixiemes de bloc sans que
     * le joueur se soit deplace. La prendre en compte interrompait le relevement en boucle.
     *
     * <p>Une position momentanement illisible renvoie {@code 0} et non une distance infinie :
     * un echec de lecture ne doit pas etre interprete comme un deplacement.
     */
    public static double distanceCarree(@Nullable PlayerRef joueur, @Nullable double[] ancrage) {
        Vector3d pos = position(joueur);
        if (pos == null || ancrage == null || ancrage.length < 3) {
            return 0d;
        }
        double dx = pos.x() - ancrage[0];
        double dz = pos.z() - ancrage[2];
        return dx * dx + dz * dz;
    }

    @Nullable
    public static Vector3d position(@Nullable PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        try {
            Transform transform = playerRef.getTransform();
            return transform == null ? null : transform.getPosition();
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    /** Vrai si les deux joueurs se trouvent dans le meme monde. */
    public static boolean memeMonde(@Nullable PlayerRef a, @Nullable PlayerRef b) {
        if (a == null || b == null || !a.isValid() || !b.isValid()) {
            return false;
        }
        try {
            return a.getWorldUuid() != null && a.getWorldUuid().equals(b.getWorldUuid());
        } catch (RuntimeException ignore) {
            return false;
        }
    }
}
