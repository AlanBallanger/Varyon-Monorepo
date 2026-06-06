package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;

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

    private ClassXpConfig() {}

    public static int getAntiFarmWindowSeconds() { return antiFarmWindowSeconds; }
    public static int getAntiFarmMaxKillsPerMob() { return antiFarmMaxKillsPerMob; }
    public static double getBaseKillMultiplier() { return baseKillMultiplier; }
    public static double getMinKillXp() { return minKillXp; }

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
            LOGGER.atInfo().log("[VaryonRPG] class_xp — antiFarm=%ds/%d kills, base=%.2f, min=%.2f",
                antiFarmWindowSeconds, antiFarmMaxKillsPerMob, baseKillMultiplier, minKillXp);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de lire class_xp dans config.toml");
        }
    }
}
