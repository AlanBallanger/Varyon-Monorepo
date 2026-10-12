package fr.varyon.death.combat;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;

/**
 * Traduit une source non-entite en libelle francais affichable comme menace a part entiere.
 *
 * <p>Les degats d'environnement n'ont pas d'entite attaquante : ils sont regroupes par type
 * ({@code chute}, {@code lave}...) pour occuper leur propre case dans le classement.
 */
public final class LibellesEnvironnement {

    private static final Map<String, String> BY_KEYWORD = Map.ofEntries(
            Map.entry("fall", "Chute"),
            Map.entry("drown", "Noyade"),
            Map.entry("suffocation", "Suffocation"),
            Map.entry("lava", "Lave"),
            Map.entry("fire", "Feu"),
            Map.entry("burn", "Brulure"),
            Map.entry("poison", "Poison"),
            Map.entry("bleed", "Saignement"),
            Map.entry("ice", "Glace"),
            Map.entry("frost", "Gel"),
            Map.entry("shock", "Foudre"),
            Map.entry("void", "Vide"),
            Map.entry("out_of_world", "Vide"),
            Map.entry("explosion", "Explosion"),
            Map.entry("command", "Commande"),
            Map.entry("environment", "Environnement"));

    private LibellesEnvironnement() {}

    /**
     * Cle de regroupement stable pour un evenement sans entite source. Deux chutes distinctes
     * doivent s'accumuler sur la meme case.
     */
    @Nonnull
    public static String groupingKey(@Nullable Damage damage) {
        return "env:" + rawType(damage);
    }

    @Nonnull
    public static String displayName(@Nullable Damage damage) {
        String raw = rawType(damage);
        for (Map.Entry<String, String> entry : BY_KEYWORD.entrySet()) {
            if (raw.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return prettify(raw);
    }

    @Nonnull
    private static String rawType(@Nullable Damage damage) {
        if (damage == null) {
            return "inconnu";
        }
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EnvironmentSource environmentSource) {
            String type = environmentSource.getType();
            if (type != null && !type.isBlank()) {
                return type.toLowerCase(Locale.ROOT);
            }
        }
        try {
            DamageCause cause = damage.getCause();
            if (cause != null && cause.getId() != null && !cause.getId().isBlank()) {
                return cause.getId().toLowerCase(Locale.ROOT);
            }
        } catch (RuntimeException ignored) {
            // Asset store may be unavailable; fall through to the generic label.
        }
        return "inconnu";
    }

    @Nonnull
    private static String prettify(@Nonnull String raw) {
        String cleaned = raw.replace('_', ' ').replace('-', ' ').trim();
        if (cleaned.isEmpty()) {
            return "Inconnu";
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }
}
