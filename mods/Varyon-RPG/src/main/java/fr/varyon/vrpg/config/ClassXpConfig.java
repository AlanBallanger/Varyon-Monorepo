package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
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

    // Zone XP multiplier + soft cap (maxUsefulLevel) per Varyon difficulty zone id (0 = haven/no zone).
    private static final ZoneXpDef[] ZONE_XP = {
        new ZoneXpDef(0.5, 5),   // haven
        new ZoneXpDef(1.0, 8),   // zone_1
        new ZoneXpDef(1.5, 10),  // zone_2
        new ZoneXpDef(2.1, 12),  // zone_3
        new ZoneXpDef(2.8, 15),  // zone_4
        new ZoneXpDef(3.5, 17),  // zone_5
        new ZoneXpDef(4.3, 19),  // zone_6
        new ZoneXpDef(5.5, 21),  // zone_7
        new ZoneXpDef(7.3, 23),  // zone_8
        new ZoneXpDef(9.5, 25),  // zone_9
        new ZoneXpDef(12.0, 27), // zone_10
    };

    // levelFactor by (playerLevel - maxUsefulLevel) gap: throttles XP once a class outlevels its farming zone.
    private static final double[] LEVEL_FACTOR_BY_GAP = { 1.0, 0.75, 0.5, 0.25, 0.10 };

    private ClassXpConfig() {}

    public static int getAntiFarmWindowSeconds() { return antiFarmWindowSeconds; }
    public static int getAntiFarmMaxKillsPerMob() { return antiFarmMaxKillsPerMob; }
    public static double getBaseKillMultiplier() { return baseKillMultiplier; }
    public static double getMinKillXp() { return minKillXp; }

    @Nonnull
    public static ZoneXpDef getZoneXpDef(int varyonZoneId) {
        int idx = (varyonZoneId <= 0) ? 0 : Math.min(varyonZoneId, ZONE_XP.length - 1);
        return ZONE_XP[idx];
    }

    public static double getLevelFactor(int playerLevel, int maxUsefulLevel) {
        int gap = playerLevel - maxUsefulLevel;
        if (gap <= 0) return 1.0;
        if (gap >= LEVEL_FACTOR_BY_GAP.length) return 0.0;
        return LEVEL_FACTOR_BY_GAP[gap];
    }

    public static final class ZoneXpDef {
        public final double multiplier;
        public final int maxUsefulLevel;

        public ZoneXpDef(double multiplier, int maxUsefulLevel) {
            this.multiplier = multiplier;
            this.maxUsefulLevel = maxUsefulLevel;
        }
    }

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
