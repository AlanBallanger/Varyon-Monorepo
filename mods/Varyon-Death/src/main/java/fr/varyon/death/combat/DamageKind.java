package fr.varyon.death.combat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;

/**
 * Type de degats affiche en toutes lettres sur le recapitulatif.
 *
 * <p>Remplace les badges de tier d'augment de l'implementation d'origine, qui dependaient
 * du systeme d'augments d'EndlessLeveling. Ici tout est derive de {@link DamageCause},
 * donnee vanilla disponible sans aucune dependance externe.
 */
public enum TypeDegats {
    MELEE("Melee", "#ff6a6a"),
    PROJECTILE("Projectile", "#ff9c3a"),
    ENVIRONMENT("Environnement", "#5fd97a"),
    UNKNOWN("Inconnu", "#c0cee5");

    private final String label;
    private final String color;

    TypeDegats(@Nonnull String label, @Nonnull String color) {
        this.label = label;
        this.color = color;
    }

    @Nonnull
    public String label() {
        return label;
    }

    /** Couleur d'affichage du type : rouge melee, orange projectile, vert environnement. */
    @Nonnull
    public String color() {
        return color;
    }

    /**
     * Classe un evenement de degats. La source prime sur la cause : une fleche tiree par un
     * squelette arrive avec une {@link Damage.ProjectileSource}, ce qui doit rester
     * {@link #PROJECTILE} meme si la cause declaree est physique.
     */
    @Nonnull
    public static TypeDegats classify(@Nullable Damage damage) {
        if (damage == null) {
            return UNKNOWN;
        }
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.ProjectileSource) {
            return PROJECTILE;
        }
        if (source instanceof Damage.EnvironmentSource) {
            return ENVIRONMENT;
        }
        TypeDegats fromCause = fromCause(damage);
        if (fromCause != UNKNOWN) {
            return fromCause;
        }
        if (source instanceof Damage.EntitySource) {
            return MELEE;
        }
        return UNKNOWN;
    }

    @Nonnull
    private static TypeDegats fromCause(@Nonnull Damage damage) {
        DamageCause cause;
        try {
            cause = damage.getCause();
        } catch (RuntimeException ignored) {
            return UNKNOWN;
        }
        if (cause == null || cause.getId() == null) {
            return UNKNOWN;
        }
        String id = cause.getId().toLowerCase(java.util.Locale.ROOT);
        if (id.contains("projectile") || id.contains("arrow")) {
            return PROJECTILE;
        }
        if (id.contains("physical") || id.contains("melee")) {
            return MELEE;
        }
        if (id.contains("fall") || id.contains("drown") || id.contains("suffocation")
                || id.contains("environment") || id.contains("out_of_world") || id.contains("fire")
                || id.contains("lava") || id.contains("burn") || id.contains("void")) {
            return ENVIRONMENT;
        }
        return UNKNOWN;
    }
}
