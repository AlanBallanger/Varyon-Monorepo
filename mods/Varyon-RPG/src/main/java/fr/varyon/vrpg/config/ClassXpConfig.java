package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;
import fr.varyon.vrpg.classes.ClassXpCurve;
import fr.varyon.vrpg.combat.MobXpCurve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ClassXpConfig {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static int antiFarmWindowSeconds = 30;
    private static int antiFarmMaxKillsPerMob = 10;
    private static double baseKillMultiplier = 1.0;
    private static double minKillXp = 1.0;

    /** Cap kill XP at the mob level of the player's highest unlocked zone (+ margin). */
    private static boolean mobLevelCapByTier = true;
    /** Extra mob levels allowed above the unlocked-zone cap before XP is clamped. */
    private static int mobLevelCapMargin = 0;

    private ClassXpConfig() {}

    public static int getAntiFarmWindowSeconds() { return antiFarmWindowSeconds; }
    public static int getAntiFarmMaxKillsPerMob() { return antiFarmMaxKillsPerMob; }
    public static double getBaseKillMultiplier() { return baseKillMultiplier; }
    public static double getMinKillXp() { return minKillXp; }
    public static boolean isMobLevelCapByTier() { return mobLevelCapByTier; }
    public static int getMobLevelCapMargin() { return mobLevelCapMargin; }

    public static void load(Path dataDir) {
        Path file = dataDir.resolve("config.toml");
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file);
            Map<String, Map<String, String>> sections = XpTableConfig.parseSections(lines);
            Map<String, String> xp = sections.getOrDefault("class_xp", Map.of());
            antiFarmWindowSeconds = (int) XpTableConfig.parseLong(
                xp.getOrDefault("anti_farm_window", "30"), 30L);
            antiFarmMaxKillsPerMob = (int) XpTableConfig.parseLong(
                xp.getOrDefault("anti_farm_max_kills", "10"), 10L);
            baseKillMultiplier = XpTableConfig.parseDouble(
                xp.getOrDefault("base_kill_multiplier", "1.0"), 1.0);
            minKillXp = XpTableConfig.parseDouble(
                xp.getOrDefault("min_kill_xp", "1.0"), 1.0);
            mobLevelCapByTier = !"false".equalsIgnoreCase(
                xp.getOrDefault("mob_level_cap_by_tier", "true").trim());
            mobLevelCapMargin = (int) XpTableConfig.parseLong(
                xp.getOrDefault("mob_level_cap_margin", "0"), 0L);

            MobXpCurve.configure(
                XpTableConfig.parseDouble(xp.getOrDefault("mob_xp_base", "14.0"), 14.0),
                XpTableConfig.parseDouble(xp.getOrDefault("mob_xp_linear", "1.3"), 1.3),
                XpTableConfig.parseDouble(xp.getOrDefault("mob_xp_power_coef", "0.0016"), 0.0016),
                XpTableConfig.parseDouble(xp.getOrDefault("mob_xp_power_exp", "2.75"), 2.75));
            ClassXpCurve.configure(
                XpTableConfig.parseDouble(xp.getOrDefault("level_ramp_mid", "9.0"), 9.0),
                XpTableConfig.parseDouble(xp.getOrDefault("level_ramp_exp", "3.4"), 3.4));

            LOGGER.atInfo().log("[VaryonRPG] class_xp - antiFarm=%ds/%d kills, base=%.2f, min=%.2f, "
                    + "mobLevelCapByTier=%b (margin %d)",
                antiFarmWindowSeconds, antiFarmMaxKillsPerMob, baseKillMultiplier, minKillXp,
                mobLevelCapByTier, mobLevelCapMargin);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de lire class_xp dans config.toml");
        }
    }
}
