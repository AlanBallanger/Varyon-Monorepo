package com.varyon.comet.config;

import com.varyon.comet.CometConfig;
import com.varyon.comet.config.model.ThemeConfig;
import com.varyon.comet.config.parser.ThemeConfigParser;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.varyon.comet.config.parser.ConfigJson;

public final class ThemeMobsLoader {

    private static final Logger LOGGER = Logger.getLogger(ThemeMobsLoader.class.getName());
    private static final String THEME_MOBS_FILE = "theme_mobs.json";

    private ThemeMobsLoader() {
    }

    public static File getThemeMobsFile(File baseConfigFile) {
        File parent = baseConfigFile != null ? baseConfigFile.getParentFile() : null;
        if (parent == null) {
            return new File(THEME_MOBS_FILE);
        }
        return new File(parent, THEME_MOBS_FILE);
    }

    public static void applyThemeMobsOverrides(CometConfig config, File baseConfigFile) {
        if (config == null || baseConfigFile == null) {
            return;
        }
        File f = getThemeMobsFile(baseConfigFile);
        if (!f.exists() || !f.isFile()) {
            return;
        }
        try {
            String json = Files.readString(f.toPath());
            if (json == null || json.isBlank()) {
                return;
            }
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                String inner = ConfigJson.extractJsonObject(trimmed, "cometMobs");
                if (inner == null || inner.isBlank()) {
                    inner = ConfigJson.extractJsonObject(trimmed, "themes");
                }
                if (inner != null && !inner.isBlank()) {
                    applyFromObjectBlock(config, inner);
                } else {
                    applyFromObjectBlock(config, trimmed);
                }
            }
            LOGGER.info("Applied mob overrides from " + f.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.warning("Failed to load " + THEME_MOBS_FILE + ": " + e.getMessage());
        }
    }

    private static void applyFromObjectBlock(CometConfig config, String json) {
        Pattern themePattern = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\\{");
        Matcher matcher = themePattern.matcher(json);
        while (matcher.find()) {
            String themeId = matcher.group(1);
            if (shouldSkipKey(themeId)) {
                continue;
            }
            int startPos = matcher.end() - 1;
            String themeJson = ConfigJson.extractObjectFromPosition(json, startPos);
            if (themeJson == null) {
                continue;
            }
            ThemeConfig parsed = ThemeConfigParser.parseThemeBlock(themeId, themeJson);
            if (parsed == null) {
                continue;
            }
            boolean hasMobs = parsed.getMobs() != null && !parsed.getMobs().isEmpty();
            boolean hasBosses = parsed.getBosses() != null && !parsed.getBosses().isEmpty();
            boolean hasWaves = parsed.hasMultiWave();
            if (!hasMobs && !hasBosses && !hasWaves) {
                continue;
            }
            ThemeConfig existing = config.getTheme(themeId);
            if (existing != null) {
                if (hasMobs) {
                    existing.setMobs(parsed.getMobs());
                }
                if (hasBosses) {
                    existing.setBosses(parsed.getBosses());
                }
                if (hasWaves) {
                    existing.setWaves(parsed.getWaves());
                } else if (hasMobs || hasBosses) {
                    ThemeConfigParser.rebuildWavesFromMobsAndBosses(existing);
                }
                String dn = ConfigJson.extractStringValue(themeJson, "displayName");
                if (dn != null && !dn.isBlank()) {
                    existing.setDisplayName(dn.trim());
                }
                List<Integer> tiers = ConfigJson.extractIntArray(themeJson, "tiers");
                if (tiers != null && !tiers.isEmpty()) {
                    existing.setTiers(tiers);
                }
                Boolean ns = ConfigJson.extractBooleanValue(themeJson, "naturalSpawn");
                if (ns != null) {
                    existing.setNaturalSpawn(ns);
                }
            } else {
                if (parsed.getTiers() == null || parsed.getTiers().isEmpty()) {
                    List<Integer> all = new ArrayList<>();
                    for (int i = 1; i <= 5; i++) {
                        all.add(i);
                    }
                    parsed.setTiers(all);
                }
                if (parsed.getDisplayName() == null || parsed.getDisplayName().isBlank()) {
                    parsed.setDisplayName(themeId);
                }
                if (!hasWaves && (hasMobs || hasBosses)) {
                    ThemeConfigParser.rebuildWavesFromMobsAndBosses(parsed);
                }
                config.addOrUpdateTheme(parsed);
            }
        }
        Map<String, ThemeConfig> themes = config.getThemes();
        if (themes != null) {
            config.setThemeList(new ArrayList<>(themes.values()));
        }
    }

    private static boolean shouldSkipKey(String key) {
        if (key == null) {
            return true;
        }
        String k = key.toLowerCase(Locale.ROOT);
        return "version".equals(k) || "schema".equals(k) || "comment".equals(k);
    }

}
