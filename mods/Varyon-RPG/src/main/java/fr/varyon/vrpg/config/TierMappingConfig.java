package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TierMappingConfig {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String FILE_NAME = "tier_mapping.toml";

    public static Map<String, Double> TIER_WEIGHTS = Collections.emptyMap();

    private TierMappingConfig() {}

    public static void load(Path dataDir) {
        Path file = ReferenceTomlInstaller.ensureInstalledAtRoot(dataDir, FILE_NAME);
        try {
            TIER_WEIGHTS = parseTierWeights(Files.readAllLines(file));
            LOGGER.atInfo().log("[VaryonRPG] " + FILE_NAME + " chargé — " + TIER_WEIGHTS.size() + " tiers");
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de lire " + FILE_NAME);
            loadClasspathFallback();
        }
    }

    static Map<String, Double> parseTierWeights(List<String> lines) {
        Map<String, Double> weights = new HashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim().toLowerCase();
            String val = line.substring(eq + 1).trim();
            int commentIdx = val.indexOf(" #");
            if (commentIdx >= 0) val = val.substring(0, commentIdx).trim();
            if (!key.isEmpty()) {
                weights.put(key, XpTableConfig.parseDouble(val, 0.0));
            }
        }
        return weights;
    }

    private static void loadClasspathFallback() {
        try (InputStream in = ReferenceTomlInstaller.openClasspathResource(FILE_NAME)) {
            if (in == null) return;
            TIER_WEIGHTS = parseTierWeights(List.of(new String(in.readAllBytes()).split("\n")));
        } catch (IOException ignored) {}
    }
}
