package fr.varyon.vrpg.ui.profession;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.config.VrpgConfig;
import fr.varyon.vrpg.rpg.Profession;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public final class ProfessionUiUtil {

    private static final Logger LOG = Logger.getLogger(ProfessionUiUtil.class.getName());

    public static final Profession[] BOOST_PROFESSION_ORDER = {
        Profession.MINEUR, Profession.FERMIER, Profession.FORESTIER, Profession.CHASSEUR
    };

    private ProfessionUiUtil() {}

    public static void applyGaugeBar(@Nonnull UICommandBuilder ui,
                                     @Nonnull String fillId,
                                     long xpInLevel, long xpToNext) {
        double ratio = xpToNext > 0 ? Math.min(1.0, (double) xpInLevel / xpToNext) : 1.0;
        if (VrpgConfig.isDebugProfessions()) {
            LOG.info("[RPG-Gauge] " + fillId + " xpInLevel=" + xpInLevel
                + " xpToNext=" + xpToNext + " ratio=" + String.format("%.3f", ratio));
        }
        ui.set(fillId + ".Value", ratio);
    }

    public static String capitalize(@Nonnull String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String formatXp(long xp) {
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000.0);
        if (xp >= 1_000) return String.format("%.1fK", xp / 1_000.0);
        return String.valueOf(xp);
    }

    public static String formatBoostMultiplier(double bonus) {
        int pct = (int) Math.round(bonus * 100);
        return "+" + pct + "% XP";
    }

    public static String formatBoostTime(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        long h = m / 60;
        if (h > 0) return h + "h " + (m % 60) + "min";
        if (m > 0) return m + "min " + (s % 60) + "s";
        return s + "s";
    }
}
