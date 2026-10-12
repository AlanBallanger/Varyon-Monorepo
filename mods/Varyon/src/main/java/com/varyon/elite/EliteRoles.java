package com.varyon.elite;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * NPC role normalisation, mirroring the canonicalisation used by Varyon-RPG's
 * {@code MobKillXpResolver.canonicalRole()} so whitelist entries match consistently.
 */
public final class EliteRoles {

    private EliteRoles() {}

    @Nonnull
    public static String canonical(@Nonnull String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash + 1);
        int colon = s.lastIndexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        return s;
    }
}
