package com.varyon.comet.config;

import com.varyon.comet.CometConfig;
import com.varyon.comet.config.defaults.DefaultThemes;
import com.varyon.comet.config.model.ThemeConfig;
import com.varyon.comet.config.parser.ThemeConfigParser;
import com.varyon.comet.config.parser.ThemeConfigWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import static com.varyon.comet.config.parser.ConfigJson.extractJsonObject;

public final class ThemeConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ThemeConfigLoader.class.getName());
    private static final String COMET_MOBS_FILE_NAME = "cometMobs.json";
    private static final String LEGACY_THEMES_JSON_FILE = "themes.json";
    private static final String LEGACY_THEMES_AND_GROUPS_FILE = "comet_themes_and_monster_groups.json";

    private ThemeConfigLoader() {}

    public static File getCometMobsFile(File baseConfigFile) {
        File parent = baseConfigFile != null ? baseConfigFile.getParentFile() : null;
        if (parent == null) {
            return new File(COMET_MOBS_FILE_NAME);
        }
        return new File(parent, COMET_MOBS_FILE_NAME);
    }

    public static void loadCometMobs(CometConfig config, File baseConfigFile) {
        if (config == null) {
            return;
        }
        try {
            File cometMobsFile = getCometMobsFile(baseConfigFile);
            File parent = baseConfigFile != null ? baseConfigFile.getParentFile() : null;
            File legacyThemesJson = parent != null ? new File(parent, LEGACY_THEMES_JSON_FILE)
                    : new File(LEGACY_THEMES_JSON_FILE);
            File legacyGroupsFile = parent != null ? new File(parent, LEGACY_THEMES_AND_GROUPS_FILE)
                    : new File(LEGACY_THEMES_AND_GROUPS_FILE);

            if (!cometMobsFile.exists() || !cometMobsFile.isFile()) {
                if (legacyThemesJson.exists() && legacyThemesJson.isFile()) {
                    try {
                        String content = new String(java.nio.file.Files.readAllBytes(legacyThemesJson.toPath()));
                        Map<String, ThemeConfig> fromLegacy = ThemeConfigParser.parseThemes(content);
                        if (!fromLegacy.isEmpty()) {
                            config.setThemes(fromLegacy);
                            config.setThemeList(new ArrayList<>(config.getThemes().values()));
                            config.setThemesLoaded(true);
                        }
                        saveCometMobs(config, cometMobsFile);
                        renameToMigrated(legacyThemesJson);
                        LOGGER.info("Migrated " + LEGACY_THEMES_JSON_FILE + " to " + COMET_MOBS_FILE_NAME);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to migrate " + LEGACY_THEMES_JSON_FILE + ": " + e.getMessage());
                        saveCometMobs(config, cometMobsFile);
                    }
                } else if (legacyGroupsFile.exists() && legacyGroupsFile.isFile()) {
                    try {
                        String themesContent = new String(java.nio.file.Files.readAllBytes(legacyGroupsFile.toPath()));
                        Map<String, ThemeConfig> fromLegacy = ThemeConfigParser.parseThemes(themesContent);
                        if (!fromLegacy.isEmpty()) {
                            config.setThemes(fromLegacy);
                            config.setThemeList(new ArrayList<>(config.getThemes().values()));
                            config.setThemesLoaded(true);
                        }
                        saveCometMobs(config, cometMobsFile);
                        renameToMigrated(legacyGroupsFile);
                        LOGGER.info("Migrated themes from " + LEGACY_THEMES_AND_GROUPS_FILE + " to " + COMET_MOBS_FILE_NAME);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to migrate legacy themes file: " + e.getMessage());
                        saveCometMobs(config, cometMobsFile);
                    }
                } else {
                    saveCometMobs(config, cometMobsFile);
                }
                return;
            }
            try {
                String json = new String(java.nio.file.Files.readAllBytes(cometMobsFile.toPath()));
                String block = extractJsonObject(json, "cometMobs");
                if (block == null || block.isBlank()) {
                    block = extractJsonObject(json, "themes");
                }
                if (block == null || block.isBlank()) {
                    LOGGER.warning("Missing top-level 'cometMobs' object: " + cometMobsFile.getAbsolutePath()
                            + " — loading defaults and repairing.");
                    Map<String, ThemeConfig> defaultThemes = DefaultThemes.generateDefaults();
                    config.setThemes(defaultThemes);
                    config.setThemeList(new ArrayList<>(defaultThemes.values()));
                    config.setThemesLoaded(true);
                    saveCometMobs(config, cometMobsFile);
                    LOGGER.info("Repaired " + COMET_MOBS_FILE_NAME + " with " + defaultThemes.size() + " default entries.");
                    return;
                }
                Map<String, ThemeConfig> externalThemes = ThemeConfigParser.parseThemes(json);
                if (externalThemes.isEmpty()) {
                    LOGGER.warning(COMET_MOBS_FILE_NAME + " has no parsed entries — loading defaults and repairing.");
                    Map<String, ThemeConfig> defaultThemes = DefaultThemes.generateDefaults();
                    config.setThemes(defaultThemes);
                    config.setThemeList(new ArrayList<>(defaultThemes.values()));
                    config.setThemesLoaded(true);
                    saveCometMobs(config, cometMobsFile);
                    LOGGER.info("Repaired " + COMET_MOBS_FILE_NAME + " with " + defaultThemes.size() + " default entries.");
                    return;
                }
                config.setThemes(externalThemes);
                config.setThemeList(new ArrayList<>(externalThemes.values()));
                config.setThemesLoaded(true);
                LOGGER.info("Loaded comet mobs from: " + cometMobsFile.getAbsolutePath() + " (" + externalThemes.size() + " entries)");
            } catch (Exception e) {
                LOGGER.warning("Failed to load '" + cometMobsFile.getAbsolutePath() + "': " + e.getMessage());
            }
        } finally {
            ThemeMobsLoader.applyThemeMobsOverrides(config, baseConfigFile);
            if (config != null && config.repairLegacyNaturalSpawnIfAllDisabled()) {
                File cometMobsFile = getCometMobsFile(baseConfigFile);
                saveCometMobs(config, cometMobsFile);
                LOGGER.info("Saved " + cometMobsFile.getName() + " after naturalSpawn repair.");
            }
            if (config != null) {
                config.logThemePoolEligibilityWarnings();
            }
            if (config != null && (config.getThemes() == null || config.getThemes().isEmpty())) {
                LOGGER.warning("No comet mob entries loaded; waves will not spawn mobs. Check " + COMET_MOBS_FILE_NAME + ".");
            }
        }
    }

    public static void saveCometMobs(CometConfig config, File cometMobsFile) {
        if (config == null || cometMobsFile == null) {
            return;
        }
        try {
            File parentDir = cometMobsFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            String json = ThemeConfigWriter.generateCometMobsConfig(config.getThemes());
            try (FileWriter writer = new FileWriter(cometMobsFile)) {
                writer.write(json);
                writer.flush();
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to save '" + cometMobsFile.getAbsolutePath() + "': " + e.getMessage());
        }
    }

    private static void renameToMigrated(File file) {
        try {
            if (file == null || !file.exists()) {
                return;
            }
            File parent = file.getParentFile();
            File target = parent != null ? new File(parent, file.getName() + ".migrated") : new File(file.getName() + ".migrated");
            if (file.renameTo(target)) {
                LOGGER.info("Renamed legacy file to " + target.getName());
            }
        } catch (Exception e) {
            LOGGER.warning("Could not rename legacy file: " + e.getMessage());
        }
    }
}
