package fr.varyon.holograms.hologram;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.regex.Pattern;

public final class HologramGroups {

    private static final Pattern SEGMENT = Pattern.compile("^[a-z0-9_]{2,24}$");
    private static final Pattern PATH = Pattern.compile("^[a-z0-9_]{2,24}(/[a-z0-9_]{2,24})*$");

    private HologramGroups() {}

    @Nonnull
    public static String sanitize(@Nonnull String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT)
            .replace(' ', '_')
            .replaceAll("[^a-z0-9_/]", "")
            .replaceAll("_+", "_")
            .replaceAll("/+", "/")
            .replaceAll("^/|/$", "");
        if (s.length() > 64) s = s.substring(0, 64);
        return s;
    }

    @Nullable
    public static String normalize(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        String path = sanitize(raw);
        if (path.isEmpty()) return null;
        requireValid(path);
        return path;
    }

    public static void requireValid(@Nonnull String path) {
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException(
                "Groupe invalide. Ex: spawn, spawn/shop (2-24 caracteres par niveau, a-z 0-9 _)");
        }
        for (String segment : path.split("/")) {
            if (!SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException(
                    "Segment de groupe invalide: '" + segment + "'");
            }
        }
    }

    @Nonnull
    public static String display(@Nullable String group) {
        return group == null || group.isBlank() ? "Racine" : group;
    }
}
