package fr.varyon.musiczones;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MusicZoneNames {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9_]{2,32}$");

    private MusicZoneNames() {}

    @Nonnull
    public static String sanitize(@Nonnull String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_]", "");
        if (s.length() > 32) {
            s = s.substring(0, 32);
        }
        return s;
    }

    public static boolean isValid(@Nonnull String name) {
        return VALID.matcher(name).matches();
    }

    @Nonnull
    public static String requireValid(@Nonnull String raw) {
        String name = sanitize(raw);
        if (!isValid(name)) {
            throw new IllegalArgumentException(
                    "Nom invalide. 2-32 caracteres: lettres, chiffres, _ (ex: spawn, foret_nord)");
        }
        return name;
    }
}
