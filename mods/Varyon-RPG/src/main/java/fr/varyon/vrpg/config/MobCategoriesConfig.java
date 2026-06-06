package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;
import fr.varyon.vrpg.profession.chasseur.ChasseurXpTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobCategoriesConfig {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String FILE_NAME = "mob_categories.toml";

    private MobCategoriesConfig() {}

    public static void load(Path dataDir) {
        Path file = ReferenceTomlInstaller.ensureInstalled(dataDir, FILE_NAME);
        try {
            Map<String, String> mobTiers = parseMobCategories(Files.readAllLines(file));
            ChasseurXpTable.MOB_TIERS = mobTiers;
            LOGGER.atInfo().log("[VaryonRPG] " + FILE_NAME + " chargé — " + mobTiers.size() + " mobs");
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de lire " + FILE_NAME);
            loadClasspathFallback();
        }
    }

    static Map<String, String> parseMobCategories(List<String> lines) {
        Map<String, String> mobTiers = new HashMap<>();
        Map<String, Map<String, String>> sections = XpTableConfig.parseSections(lines);
        Map<String, String> legacy = sections.get("mobs_categories");
        if (legacy != null && !legacy.isEmpty()) {
            for (Map.Entry<String, String> e : legacy.entrySet()) {
                mobTiers.put(e.getKey().toLowerCase(), stripQuotes(e.getValue()).toLowerCase());
            }
            return mobTiers;
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim().toLowerCase();
            String val = stripQuotes(line.substring(eq + 1).trim());
            int commentIdx = val.indexOf(" #");
            if (commentIdx >= 0) val = val.substring(0, commentIdx).trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                mobTiers.put(key, val.toLowerCase());
            }
        }
        return mobTiers;
    }

    private static String stripQuotes(String val) {
        if (val.startsWith("\"") && val.endsWith("\"")) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private static void loadClasspathFallback() {
        try (InputStream in = ReferenceTomlInstaller.openClasspathResource(FILE_NAME)) {
            if (in == null) return;
            Map<String, String> mobTiers = parseMobCategories(List.of(new String(in.readAllBytes()).split("\n")));
            ChasseurXpTable.MOB_TIERS = mobTiers;
        } catch (IOException ignored) {}
    }
}
