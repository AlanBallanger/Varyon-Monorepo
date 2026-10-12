package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VrpgConfig {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private record Entry(String key, String defaultValue, String comment) {}

    private static final List<Entry> TOP_LEVEL_ENTRIES = List.of(
        new Entry("debug_talents", "false",
            "Active les logs de debug pour chaque declenchement de talent Mineur\n"
            + "Utile pour diagnostiquer les talents qui ne fonctionnent pas"),
        new Entry("debug_professions", "false",
            "Active les logs de debug pour les jauges XP des metiers (ProfessionActiveCard / ProfessionCatalogCard)\n"
            + "Utile pour diagnostiquer l'affichage des barres de progression"),
        new Entry("debug_combat", "false",
            "Active les logs de dégats infligés/reçus et des DoT (saignement etc.)"),
        new Entry("debug_class_xp", "false",
            "Active les logs d'XP de classe gagnée à chaque kill (niveau du mob, base, bonus talents/élite, total)")
    );

    private static final String CLASS_XP_SECTION =
        "[class_xp]\n"
        + "anti_farm_window = 30\n"
        + "anti_farm_max_kills = 10\n"
        + "base_kill_multiplier = 1.0\n"
        + "min_kill_xp = 1.0\n";

    private static boolean debugTalents = false;
    private static boolean debugProfessions = false;
    private static boolean debugCombat = false;
    private static boolean debugClassXp = false;

    private VrpgConfig() {}

    public static boolean isDebugTalents()     { return debugTalents; }
    public static boolean isDebugProfessions() { return debugProfessions; }
    public static boolean isDebugCombat()      { return debugCombat; }
    public static boolean isDebugClassXp()     { return debugClassXp; }

    public static void load(Path dataDir) {
        Path configFile = dataDir.resolve("config.toml");
        if (!Files.exists(configFile)) {
            writeDefaults(configFile);
        }
        try {
            List<String> lines = Files.readAllLines(configFile);
            Map<String, String> values = parseToml(lines);
            appendMissingEntries(configFile, lines, values);

            debugTalents = parseBoolean(values.getOrDefault("debug_talents", "false"));
            debugProfessions = parseBoolean(values.getOrDefault("debug_professions", "false"));
            debugCombat = parseBoolean(values.getOrDefault("debug_combat", "false"));
            debugClassXp = parseBoolean(values.getOrDefault("debug_class_xp", "false"));
            LOGGER.atInfo().log("[VaryonRPG] Config chargée — debug_talents=" + debugTalents
                + " debug_professions=" + debugProfessions
                + " debug_combat=" + debugCombat
                + " debug_class_xp=" + debugClassXp);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de lire config.toml, valeurs par défaut utilisées");
        }
    }

    private static void appendMissingEntries(Path path, List<String> existingLines,
                                             Map<String, String> values) {
        List<Entry> missing = new ArrayList<>();
        for (Entry entry : TOP_LEVEL_ENTRIES) {
            if (!values.containsKey(entry.key())) {
                missing.add(entry);
            }
        }
        if (missing.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        int lastNonBlank = existingLines.size();
        while (lastNonBlank > 0 && existingLines.get(lastNonBlank - 1).isBlank()) {
            lastNonBlank--;
        }
        for (int i = 0; i < lastNonBlank; i++) {
            sb.append(existingLines.get(i)).append('\n');
        }
        for (Entry entry : missing) {
            sb.append('\n');
            for (String commentLine : entry.comment().split("\n")) {
                sb.append("# ").append(commentLine).append('\n');
            }
            sb.append(entry.key()).append(" = ").append(entry.defaultValue()).append('\n');
            values.put(entry.key(), entry.defaultValue());
        }

        try {
            Files.writeString(path, sb.toString());
            LOGGER.atInfo().log("[VaryonRPG] config.toml — " + missing.size()
                + " option(s) ajoutée(s) : " + missing.stream().map(Entry::key).toList());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de compléter config.toml");
        }
    }

    private static void writeDefaults(Path path) {
        StringBuilder sb = new StringBuilder("# Varyon RPG — Configuration\n");
        for (Entry entry : TOP_LEVEL_ENTRIES) {
            sb.append('\n');
            for (String commentLine : entry.comment().split("\n")) {
                sb.append("# ").append(commentLine).append('\n');
            }
            sb.append(entry.key()).append(" = ").append(entry.defaultValue()).append('\n');
        }
        sb.append('\n').append(CLASS_XP_SECTION);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, sb.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible d'écrire config.toml");
        }
    }

    private static Map<String, String> parseToml(List<String> lines) {
        Map<String, String> result = new HashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            int commentIdx = val.indexOf(" #");
            if (commentIdx >= 0) val = val.substring(0, commentIdx).trim();
            if (!key.isEmpty()) result.put(key, val);
        }
        return result;
    }

    private static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }
}
