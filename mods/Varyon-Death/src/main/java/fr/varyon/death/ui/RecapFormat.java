package fr.varyon.death.ui;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import javax.annotation.Nonnull;

import fr.varyon.death.combat.SuiviCombat.ThreatSnapshot;
import fr.varyon.death.combat.TypeDegats;
import fr.varyon.death.compat.VaryonMobLevel;

/** Formatage des valeurs affichees sur le recapitulatif. */
public final class RecapFormat {

    private RecapFormat() {}

    @Nonnull
    public static String number(float value) {
        long rounded = Math.round(value);
        if (rounded < 1_000L) {
            return Long.toString(rounded);
        }
        if (rounded < 1_000_000L) {
            return String.format(Locale.FRANCE, "%.1fk", rounded / 1_000.0);
        }
        return String.format(Locale.FRANCE, "%.2fM", rounded / 1_000_000.0);
    }

    /** Toujours en secondes, quelle que soit la duree : « 18,6s », « 92,4s ». */
    @Nonnull
    public static String duration(long ms) {
        return String.format(Locale.FRANCE, "%.1fs", ms / 1_000.0);
    }

    /**
     * Ligne de niveau d'une carte. Le niveau n'apparait que si Varyon l'a fourni : sur un monde
     * sans Varyon les creatures n'ont pas de niveau et la ligne reste vide.
     */
    @Nonnull
    public static String levelLabel(@Nonnull ThreatSnapshot threat) {
        if (threat.isEnvironment()) {
            return "";
        }
        String prefix = threat.isPlayer() ? "[J] " : "";
        if (threat.level() > VaryonMobLevel.NO_LEVEL) {
            return prefix + "Niv." + threat.level();
        }
        return prefix.isBlank() ? "" : prefix.trim();
    }

    /** Types cumules, du plus au moins dommageable : « Projectile, Melee ». */
    @Nonnull
    public static String kindLabel(@Nonnull ThreatSnapshot threat) {
        List<TypeDegats> kinds = threat.kindsByDamage();
        if (kinds.isEmpty()) {
            return TypeDegats.UNKNOWN.label();
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (TypeDegats kind : kinds) {
            joiner.add(kind.label());
        }
        return joiner.toString();
    }

    /**
     * Couleur du type dominant : rouge melee, orange projectile, vert environnement.
     * Sur une menace mixte, c'est le type ayant inflige le plus de degats qui donne la teinte.
     */
    @Nonnull
    public static String dominantKindColor(@Nonnull ThreatSnapshot threat) {
        List<TypeDegats> kinds = threat.kindsByDamage();
        return kinds.isEmpty() ? TypeDegats.UNKNOWN.color() : kinds.get(0).color();
    }

    /** Sous-titre du panneau de detail : « Creature - Niv.3 Yeti ». */
    @Nonnull
    public static String detailSubtitle(@Nonnull ThreatSnapshot threat) {
        StringBuilder out = new StringBuilder();
        if (threat.isEnvironment()) {
            out.append("Environnement - ");
        } else if (threat.isPlayer()) {
            out.append("Joueur - ");
        } else {
            out.append("Creature - ");
        }
        if (!threat.isEnvironment() && threat.level() > VaryonMobLevel.NO_LEVEL) {
            out.append("Niv.").append(threat.level()).append(' ');
        }
        out.append(threat.displayName());
        return out.toString();
    }

    /** Ligne de repartition par type : « Projectile   54 (74%) - 3 coups ». */
    @Nonnull
    public static String kindBreakdownLine(@Nonnull ThreatSnapshot threat, @Nonnull TypeDegats kind) {
        float damage = threat.damageOfKind(kind);
        int hits = threat.hitsOfKind(kind);
        float total = threat.damageDealt();
        int percent = total > 0f ? Math.round(damage / total * 100f) : 0;
        return String.format(Locale.FRANCE, "%s   %s (%d%%) - %d %s",
                kind.label(), number(damage), percent, hits, hits > 1 ? "coups" : "coup");
    }

    @Nonnull
    public static String averagePerHit(@Nonnull ThreatSnapshot threat) {
        if (threat.hitCount() <= 0) {
            return "-";
        }
        return number(threat.damageDealt() / threat.hitCount());
    }
}
