package com.varyon.comet;

import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import static com.varyon.comet.config.parser.ConfigJson.extractBooleanValue;
import static com.varyon.comet.config.parser.ConfigJson.extractDoubleValue;
import static com.varyon.comet.config.parser.ConfigJson.extractIntValue;
import static com.varyon.comet.config.parser.ConfigJson.extractJsonArray;
import static com.varyon.comet.config.parser.ConfigJson.extractJsonObject;
import static com.varyon.comet.config.parser.ConfigJson.extractStringArray;
import static com.varyon.comet.config.parser.ConfigJson.extractStringIntMapFromObject;
import static com.varyon.comet.config.parser.ConfigJson.extractStringValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.varyon.comet.config.model.BossEntry;
import com.varyon.comet.config.defaults.DefaultThemes;
import com.varyon.comet.config.model.MobEntry;
import com.varyon.comet.config.validation.ConfigValidationReport;
import com.varyon.comet.config.validation.ConfigValidator;
import com.varyon.comet.config.model.ThemeConfig;
import com.varyon.comet.config.parser.ThemeConfigParser;
import com.varyon.comet.config.parser.ThemeConfigWriter;
import com.varyon.comet.config.ThemeConfigLoader;
import com.varyon.comet.config.model.TierRewards;
import com.varyon.comet.config.model.RewardEntry;
import com.varyon.comet.config.model.ShardDropRange;
import com.varyon.comet.config.model.VaryonMineralBonus;
import com.varyon.comet.config.model.ZoneSpawnChances;
import com.varyon.comet.integration.VaryonZoneResolver;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.plugin.PluginManager;

/**
 * Configuration manager for Comet Mod settings.
 * Handles spawn settings, comet mob definitions, and tier configurations.
 * Config file is the single source of truth - no fallback to hardcoded after
 * first run.
 */
public class CometConfig {

    private static final Logger LOGGER = Logger.getLogger(CometConfig.class.getName());
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String COMET_MOBS_FILE_NAME = "cometMobs.json";
    private static final String MOD_DATA_FOLDER_NAME = "Varyon_Comet";
    /** Legacy file names; merged into new files on first run if new files are missing. */
    private static final String LEGACY_CONFIG_FILE_NAME = "comet_config.json";
    private static final String LEGACY_THEMES_FILE_NAME = "comet_themes_and_monster_groups.json";
    private static final String ENDGAME_QOL_GROUP = "Config";
    private static final String ENDGAME_QOL_NAME = "Endgame&QoL";
    private static final String ENDGAME_QOL_PLUGIN_CLASS = "endgame.plugin.EndgameQoL";

    /** Block radius for "use comet" detection (distance from registered comet block to allow starting wave). */
    public static final int COMET_USE_NEAR_RADIUS = 4;

    /** Half-size of the "asset box": when we register a comet we also register every block in a (2*radius+1)^3 box so any part of a multi-block asset (e.g. chest, coffin) triggers the comet. */
    public static final int COMET_ASSET_BOX_RADIUS = 2;

    /** Set to true to log [CometDebug] messages to console (activation, waves, placement). Remember to set false before release. */
    public static final boolean DEBUG = false;

    // Singleton instance for global access
    private static CometConfig instance;
    private static volatile boolean tier5Enabled = detectTier5Availability();

    // Spawn settings (existing)
    public int minDelaySeconds = 270;
    public int maxDelaySeconds = 330;

    public boolean varyonZoneDelayScalingEnabled = true;
    public int zoneDelayReferenceZone = 1;
    public int zoneDelayFastestZone = 10;
    public double zoneDelaySpeedMultiplierAtFastest = 3.0;

    /** Keys {@code "1"}–{@code "10"}: min/max fragments per Varyon ring (placeholder). */
    private final Map<String, ShardDropRange> varyonRingShardDrops = new LinkedHashMap<>();

    /** Keys {@code "1"}–{@code "10"}: optional extra bar/mineral (one roll, {@code chance} 0–1) per ring. */
    private final Map<String, VaryonMineralBonus> varyonRingMineralBonuses = new LinkedHashMap<>();
    public double spawnChance = 0.4;
    public double despawnTimeMinutes = 2.0;
    public int minSpawnDistance = 30;
    public int maxSpawnDistance = 50;

    // Natural spawns toggle - if false, comets only spawn from fixed spawn points
    public boolean naturalSpawnsEnabled = true;

    // Global comets setting - if true, any player can trigger any comet (not just the owner)
    public boolean globalComets = false;

    /** If true, inject Use interaction into clean-slate spawn blocks (no Use in asset) so F activates comets. If false, skip injection (avoids loadAssets; use hit-to-activate for those blocks). */
    public boolean injectUseForCleanSlateBlocks = false;

    /** If true, mobs spawned in comet waves do not drop loot from their loot tables (rewards come only from the comet chest). If false, wave mobs use their normal droplist on death. */
    public boolean disableWaveMobLoot = true;

    /** If true, log per-tick wave progress (mob counts, title updates) at INFO level. If false (default), these are logged at FINE and hidden from normal console output. */
    public boolean verboseWaveLogging = false;

    /** If true, emit verbose diagnostic logs (spawn, despawn, wave lifecycle, ambience) at INFO level. If false (default), these are suppressed; warnings/errors always log. */
    public boolean debugLogging = false;

    public boolean isDebugLoggingEnabled() {
        return debugLogging;
    }

    public int waveTimeoutSeconds = 120;
    public double waveSpawnMinRadius = 2.0;
    public double waveSpawnMaxRadius = 8.0;

    public String combatMusicAmbienceId = "Mus_Zone1_Dungeon_Boss";

    public String cometLandSoundEventId = "SFX_Bomb_Fire_Goblin_Death";

    public String cometDestroySoundEventId = "sfx_crystal_break";

    /** Played in UI for the player when the falling comet title/chat is shown. Empty disables. */
    public String cometFallingNotifySoundEventId = "SFX_Player_Pickup_Item";

    /**
     * If true, chest rewards scale by how fast all waves were cleared vs total time budget (per-wave timeout × wave count).
     * At or under 50% of budget: {@code waveSpeedRewardMaxPercent}% of rolls; at 100% of budget: {@code waveSpeedRewardMinPercent}%.
     * Between 50% and 100% of budget: linear interpolation.
     */
    public boolean waveSpeedRewardEnabled = true;
    /** Percent of configured rewards at fast clear (≤50% of time budget). Typically 100. */
    public double waveSpeedRewardMaxPercent = 100.0;
    /** Percent of configured rewards if the last mob dies when the time budget is fully used. Typically 50. */
    public double waveSpeedRewardMinPercent = 50.0;

    public String msgCometFallingTitle = "Comète %tier% en chute !";
    public String msgCometFallingSubtitle = "Regarde le ciel !";
    public String msgCometFallingChatCoords =
            "[Comète] Une comète %tier% est tombée en %x% ; %y% ; %z%";

    public String msgWaveBossTitle = "Vague de boss %currentWave%/%totalWaves%";
    public String msgWaveBossTitleNoCount = "Vague de boss !";
    public String msgWaveBossSubtitle =
            "Boss : %bossStatus% | Temps : %time%";

    public String msgWaveTitle =
            "Vague %currentWave%/%totalWaves% - %theme%";
    public String msgWaveTitleNoCount = "%theme% en approche !";
    public String msgWaveSubtitle =
            "Mobs : %killed%/%total% | Temps : %time%";

    public String msgWaveFailedTitle = "Vague échouée !";
    public String msgWaveFailedSubtitle = "Temps écoulé !";
    /** Subtitle for event title when the activating player dies during the encounter (timeout keeps msgWaveFailedSubtitle). */
    public String msgWaveFailedPlayerDeathSubtitle = "Vous êtes mort !";

    public String msgWaveCompleteTitle = "Vagues vaincues !";
    public String msgWaveCompleteSubtitle = "Butin obtenu !";

    public String msgWaveCompleteChatHeaderPrefix = "[Comète] ";
    public String msgWaveCompleteChatHeader = "Vagues vaincues ! Récompenses :";
    public String msgWaveCompleteChatItemPrefix = " - ";

    private final List<String> enabledWorlds = new ArrayList<>();
    private final Set<String> enabledWorldLookup = new LinkedHashSet<>();

    // Theme configurations (new)
    private Map<String, ThemeConfig> themes = new LinkedHashMap<>();
    private List<ThemeConfig> themeList = new ArrayList<>(); // Ordered list for random selection

    private Map<Integer, TierRewards> rewardSettings = new LinkedHashMap<>();

    /** Keys {@code "1"}–{@code "4"}: tier weights for Hytale world-map zones (not Varyon rings). */
    private Map<String, ZoneSpawnChances> hytaleZoneSpawnChances = new LinkedHashMap<>();

    private final Map<Integer, Map<String, Integer>> themePoolByTier = new LinkedHashMap<>();

    private static final Map<Integer, Map<String, Integer>> DEFAULT_THEME_POOL = defaultThemePools();

    private static Map<Integer, Map<String, Integer>> defaultThemePools() {
        Map<Integer, Map<String, Integer>> m = new LinkedHashMap<>();
        m.put(1, w("skeleton", "goblin", "spider", "void", "undead_rare"));
        m.put(2, w("skeleton", "goblin", "spider", "trork", "skeleton_sand", "sabertooth", "void", "undead_rare"));
        m.put(3, w("trork", "outlander", "skeleton_sand", "frostbound_pack", "void", "void_reavers"));
        m.put(4, w("outlander", "ice", "lava", "earth", "zombie", "skeleton_burnt", "ashen_vanguard"));
        m.put(5, w("outlander", "ice", "lava", "earth", "undead_legendary", "wraithborn_legion"));
        return m;
    }

    private static Map<String, Integer> w(String... ids) {
        LinkedHashMap<String, Integer> x = new LinkedHashMap<>();
        for (String id : ids) {
            x.put(id, 1);
        }
        return x;
    }

    // Optional WorldProtect integration: control comet spawning inside protected regions
    private boolean protectedZoneSpawnRulesEnabled = false;
    private boolean protectedZoneDefaultInProtectedRegion = true;
    private Map<String, Boolean> protectedZoneRegionOverrides = new LinkedHashMap<>();

    // Generic claim protection integration
    private boolean claimProtectEnabled = false;
    private boolean claimProtectAutoDetectProviders = false;
    private final List<String> claimProtectProviders = new ArrayList<>();
    private final Set<String> claimProtectProviderLookup = new LinkedHashSet<>();

    // Bench recipes (new)

    // Track if config was loaded successfully
    private boolean themesLoaded = false;

    /**
     * Get the singleton instance (loaded config)
     */
    public static CometConfig getInstance() {
        return instance;
    }

    /**
     * Directory where config.json, cometMobs.json, theme_mobs.json live (e.g. .../Mods/Varyon_Comet/).
     * Use this so all config files are in the same place.
     */
    public static File getConfigDirectory() {
        File cf = getConfigFile();
        return (cf != null && cf.getParentFile() != null) ? cf.getParentFile() : null;
    }

    /**
     * Get config file location
     */
    private static File getConfigFile() {
        CometModPlugin plugin = CometModPlugin.getInstance();
        if (plugin != null) {
            try {
                java.nio.file.Path pluginFile = plugin.getFile();
                if (pluginFile != null) {
                    java.nio.file.Path pluginDir = pluginFile.getParent();
                    if (pluginDir != null) {
                        String dirName = pluginDir.getFileName().toString();
                        java.nio.file.Path modFolder;

                        if ("Mods".equals(dirName) || "mods".equals(dirName)) {
                            modFolder = resolveModDataDirectory(pluginDir);
                        } else {
                            modFolder = pluginDir;
                        }

                        File modFolderFile = modFolder.toFile();
                        if (!modFolderFile.exists()) {
                            modFolderFile.mkdirs();
                        }

                        File configFile = modFolder.resolve(CONFIG_FILE_NAME).toFile();
                        LOGGER.info("Using plugin directory for config: " + configFile.getAbsolutePath());
                        return configFile;
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Could not get plugin directory, using fallback: " + e.getMessage());
            }
        }

        // Fallback paths
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File modFolder = new File(appData + File.separator + "Hytale" + File.separator +
                    "UserData" + File.separator + "Mods" + File.separator + MOD_DATA_FOLDER_NAME);
            renameLegacyCometFolderIfPresent(modFolder);
            if (!modFolder.exists()) {
                modFolder.mkdirs();
            }
            return new File(modFolder, CONFIG_FILE_NAME);
        }

        File currentDir = new File(System.getProperty("user.dir"));
        File modFolder1 = new File(currentDir, "Mods" + File.separator + MOD_DATA_FOLDER_NAME);
        renameLegacyCometFolderIfPresent(modFolder1);
        if (modFolder1.exists() || modFolder1.getParentFile().exists()) {
            modFolder1.mkdirs();
            return new File(modFolder1, CONFIG_FILE_NAME);
        }

        File fallbackModFolder = new File(currentDir, MOD_DATA_FOLDER_NAME);
        renameLegacyCometFolderIfPresent(fallbackModFolder);
        fallbackModFolder.mkdirs();
        LOGGER.warning("Could not find mod directory, saving config to: " + fallbackModFolder.getAbsolutePath());
        return new File(fallbackModFolder, CONFIG_FILE_NAME);
    }

    private static java.nio.file.Path resolveModDataDirectory(java.nio.file.Path pluginDir) {
        java.nio.file.Path v = pluginDir.resolve(MOD_DATA_FOLDER_NAME);
        java.nio.file.Path legacy = pluginDir.resolve("Comet");
        try {
            if (!java.nio.file.Files.exists(v) && java.nio.file.Files.exists(legacy)
                    && java.nio.file.Files.isDirectory(legacy)) {
                java.nio.file.Files.move(legacy, v);
                LOGGER.info("Renamed data folder Comet -> " + MOD_DATA_FOLDER_NAME);
            }
        } catch (Exception e) {
            LOGGER.warning("Could not migrate folder Comet to " + MOD_DATA_FOLDER_NAME + ": " + e.getMessage());
        }
        return v;
    }

    private static void renameLegacyCometFolderIfPresent(File targetFolder) {
        if (targetFolder == null) {
            return;
        }
        if (targetFolder.exists()) {
            return;
        }
        File parent = targetFolder.getParentFile();
        if (parent == null) {
            return;
        }
        File legacy = new File(parent, "Comet");
        if (legacy.exists() && legacy.isDirectory()) {
            if (!legacy.renameTo(targetFolder)) {
                LOGGER.warning("Could not rename " + legacy.getAbsolutePath() + " to " + targetFolder.getAbsolutePath());
            } else {
                LOGGER.info("Renamed config folder Comet -> " + MOD_DATA_FOLDER_NAME);
            }
        }
    }

    private static File getLegacyConfigFile(File configFile) {
        File parent = configFile != null ? configFile.getParentFile() : null;
        if (parent == null) {
            return new File(LEGACY_CONFIG_FILE_NAME);
        }
        return new File(parent, LEGACY_CONFIG_FILE_NAME);
    }

    private static File getLegacyThemesFile(File configFile) {
        File parent = configFile != null ? configFile.getParentFile() : null;
        if (parent == null) {
            return new File(LEGACY_THEMES_FILE_NAME);
        }
        return new File(parent, LEGACY_THEMES_FILE_NAME);
    }

    /**
     * Load configuration from file, or create with defaults if file doesn't exist.
     * If new config files are missing but legacy files exist, merges legacy into new files and renames legacy to .migrated.
     */
    public static CometConfig load() {
        refreshTier5Availability();
        File configFile = getConfigFile();
        File legacyConfig = getLegacyConfigFile(configFile);
        File legacyThemes = getLegacyThemesFile(configFile);

        // Migrate legacy config into new files so users keep their settings
        if (!configFile.exists() || !configFile.isFile()) {
            if (legacyConfig.exists() && legacyConfig.isFile()) {
                CometConfig config = migrateFromLegacyFiles(configFile, legacyConfig, legacyThemes);
                if (config != null) {
                    instance = config;
                    return config;
                }
            }
        }

        CometConfig config = new CometConfig();

        if (configFile.exists() && configFile.isFile()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
                ConfigValidationReport configValidation = ConfigValidator.validateCometConfig(content);
                logValidationReport(CONFIG_FILE_NAME, configValidation);
                config = parseJson(content, false);
                ThemeConfigLoader.loadCometMobs(config, configFile);
                config.migrateLegacyEnglishMessagesToFrench();
                syncConfigFilesOnBoot(config, configFile);
                if (!configValidation.isClean()) {
                    forceWriteConfigAndThemes(config, configFile);
                }
                LOGGER.info("Loaded Comet Mod configuration from: " + configFile.getAbsolutePath());

                // Warn if no themes
                if (config.themes.isEmpty()) {
                    LOGGER.warning("  WARNING: No themes defined in config! Waves will not spawn mobs!");
                }

            } catch (Exception e) {
                LOGGER.warning("Failed to load config file '" + configFile.getAbsolutePath() + "', using defaults: " + e.getMessage());
                e.printStackTrace();
                config = createDefaultConfig();
                ThemeConfigLoader.saveCometMobs(config, ThemeConfigLoader.getCometMobsFile(configFile));
                config.save();
            }
        } else {
            LOGGER.info("Config file not found, creating default config at: " + configFile.getAbsolutePath());
            config = createDefaultConfig();
            ThemeConfigLoader.saveCometMobs(config, ThemeConfigLoader.getCometMobsFile(configFile));
            config.save();
        }

        instance = config;
        return config;
    }

    public static boolean isTier5Enabled() {
        return tier5Enabled;
    }

    public static synchronized void refreshTier5Availability() {
        boolean previous = tier5Enabled;
        tier5Enabled = detectTier5Availability();

        if (tier5Enabled && !previous) {
            LOGGER.info("Tier 5/Mythic enabled (Endgame&QoL detected).");
        } else if (!tier5Enabled && previous) {
            LOGGER.warning("Tier 5/Mythic disabled (Endgame&QoL not detected).");
        }
    }

    public static CometTier clampUnavailableTier(CometTier requestedTier) {
        if (requestedTier == null) {
            return CometTier.UNCOMMON;
        }
        if (requestedTier == CometTier.MYTHIC && !tier5Enabled) {
            return CometTier.LEGENDARY;
        }
        return requestedTier;
    }

    private static boolean detectTier5Availability() {
        try {
            PluginManager pluginManager = PluginManager.get();
            if (pluginManager != null) {
                PluginIdentifier id = new PluginIdentifier(ENDGAME_QOL_GROUP, ENDGAME_QOL_NAME);
                if (pluginManager.hasPlugin(id, SemverRange.WILDCARD)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Fallback below.
        }

        try {
            Class.forName(ENDGAME_QOL_PLUGIN_CLASS, false, CometConfig.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void logValidationReport(String fileName, ConfigValidationReport report) {
        if (report == null || report.isClean()) {
            return;
        }

        for (String warning : report.getWarnings()) {
            LOGGER.warning("[ConfigValidation][" + fileName + "] " + warning);
        }
        for (String error : report.getErrors()) {
            LOGGER.warning("[ConfigValidation][" + fileName + "] ERROR: " + error);
        }
    }

    /**
     * Create a config with default values
     */
    private static CometConfig createDefaultConfig() {
        CometConfig config = new CometConfig();
        config.themes = DefaultThemes.generateDefaults();
        config.themeList = new ArrayList<>(config.themes.values());
        config.rewardSettings = getDefaultRewardSettings();
        config.hytaleZoneSpawnChances = ZoneSpawnChances.generateDefaults();
        config.themesLoaded = true;
        return config;
    }

    /**
     * Merge legacy config files into the new layout and save. Renames legacy files to .migrated.
     * Call when config.json is missing but comet_config.json (and optionally comet_themes_and_monster_groups.json) exist.
     */
    private static CometConfig migrateFromLegacyFiles(File newConfigFile, File legacyConfigFile, File legacyThemesFile) {
        try {
            String legacyContent = new String(java.nio.file.Files.readAllBytes(legacyConfigFile.toPath()));
            logValidationReport(LEGACY_CONFIG_FILE_NAME, ConfigValidator.validateCometConfig(legacyContent));
            CometConfig config = parseJson(legacyContent, true);

            // If legacy themes file exists, it overrides themes from comet_config.json
            if (legacyThemesFile.exists() && legacyThemesFile.isFile()) {
                String themesContent = new String(java.nio.file.Files.readAllBytes(legacyThemesFile.toPath()));
                String themesBlock = extractJsonObject(themesContent, "cometMobs");
                if (themesBlock == null || themesBlock.isBlank()) {
                    themesBlock = extractJsonObject(themesContent, "themes");
                }
                if (themesBlock != null && !themesBlock.isBlank()) {
                    Map<String, ThemeConfig> fromThemesFile = ThemeConfigParser.parseThemes(themesContent);
                    if (!fromThemesFile.isEmpty()) {
                        config.themes = fromThemesFile;
                        config.themeList = new ArrayList<>(config.themes.values());
                    }
                }
            }

            config.ensureDefaultsForPersistence();
            config.migrateLegacyEnglishMessagesToFrench();

            File parent = newConfigFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter w = new FileWriter(newConfigFile)) {
                w.write(config.buildConfigJsonForPersistence());
                w.flush();
            }
            ThemeConfigLoader.saveCometMobs(config, ThemeConfigLoader.getCometMobsFile(newConfigFile));

            renameToMigrated(legacyConfigFile);
            if (legacyThemesFile.exists()) {
                renameToMigrated(legacyThemesFile);
            }

            LOGGER.info("Migrated legacy config to " + CONFIG_FILE_NAME + ", " + COMET_MOBS_FILE_NAME + ". Old files renamed to .migrated");
            return config;
        } catch (Exception e) {
            LOGGER.warning("Failed to migrate legacy config: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void renameToMigrated(File file) {
        try {
            File target = new File(file.getParentFile(), file.getName() + ".migrated");
            if (file.renameTo(target)) {
                LOGGER.info("Renamed " + file.getName() + " to " + target.getName());
            }
        } catch (Exception e) {
            LOGGER.warning("Could not rename " + file.getName() + " to .migrated: " + e.getMessage());
        }
    }

    private static Map<String, ShardDropRange> mergeRingsWithDefaults(Map<String, ShardDropRange> parsed) {
        Map<String, ShardDropRange> out = new LinkedHashMap<>(createDefaultShardDropsMap());
        if (parsed != null) {
            out.putAll(parsed);
        }
        return out;
    }

    public ShardDropRange getShardDropRangeForVaryonRingOrDefault(int varyonRing) {
        int ring = varyonRing <= 0 ? 1 : varyonRing;
        ring = Math.max(VaryonZoneResolver.VARYON_RING_MIN, Math.min(VaryonZoneResolver.VARYON_RING_MAX, ring));
        ShardDropRange r = varyonRingShardDrops.get(String.valueOf(ring));
        if (r == null) {
            r = getDefaultShardDrops().get(String.valueOf(ring));
        }
        return r != null ? r : new ShardDropRange(1, 1);
    }

    private static VaryonMineralBonus parseMineralBonusFromJsonObject(String obj) {
        if (obj == null) {
            return null;
        }
        String itemId = extractStringValue(obj, "itemId");
        Double ch = extractDoubleValue(obj, "chance");
        if (itemId == null || itemId.isBlank() || ch == null) {
            return null;
        }
        double c = ch;
        if (c > 1.0 && c <= 100.0) {
            c = c / 100.0;
        }
        c = Math.max(0.0, Math.min(1.0, c));
        return new VaryonMineralBonus(itemId.trim(), c);
    }

    private static void parseVaryonRingRewards(CometConfig config, String parseFrom) {
        String block = extractJsonObject(parseFrom, "cometVaryonRingRewards");
        Map<String, ShardDropRange> rings = new LinkedHashMap<>();
        if (block != null) {
            for (int zi = 1; zi <= VaryonZoneResolver.VARYON_RING_MAX; zi++) {
                String key = String.valueOf(zi);
                String obj = extractJsonObject(block, key);
                if (obj == null) {
                    continue;
                }
                Integer smin = extractIntValue(obj, "min");
                Integer smax = extractIntValue(obj, "max");
                if (smin != null && smax != null) {
                    rings.put(key, new ShardDropRange(smin, smax));
                }
                String mineralObj = extractJsonObject(obj, "mineralBonus");
                if (mineralObj != null) {
                    VaryonMineralBonus b = parseMineralBonusFromJsonObject(mineralObj);
                    if (b != null) {
                        config.varyonRingMineralBonuses.put(key, b);
                    }
                }
            }
        }
        config.varyonRingShardDrops.putAll(mergeRingsWithDefaults(rings));
    }

    private static void parseThemePool(CometConfig config, String json) {
        if (config == null || json == null) {
            return;
        }
        config.themePoolByTier.clear();
        String block = extractJsonObject(json, "themePool");
        if (block == null || block.isEmpty()) {
            return;
        }
        for (int t = 1; t <= 5; t++) {
            String tierObj = extractJsonObject(block, String.valueOf(t));
            if (tierObj == null) {
                continue;
            }
            Map<String, Integer> map = extractStringIntMapFromObject(tierObj);
            if (!map.isEmpty()) {
                config.themePoolByTier.put(t, map);
            }
        }
    }

    /**
     * Reload configuration from file
     */
    public static CometConfig reload() {
        LOGGER.info("Reloading configuration from file...");
        return load();
    }

    /**
     * Parse JSON configuration.
     * @param includeThemesFromJson when true, parse cometMobs from json (used when migrating from legacy comet_config.json)
     */
    private static CometConfig parseJson(String json, boolean includeThemesFromJson) {
        CometConfig config = new CometConfig();

        try {
            String spawnBlock = extractJsonObject(json, "spawnSettings");
            String parseFrom = (spawnBlock != null) ? spawnBlock : json;

            // Parse spawn settings
            Integer minDelaySeconds = extractIntValue(parseFrom, "minDelaySeconds");
            if (minDelaySeconds != null) config.minDelaySeconds = minDelaySeconds;

            Integer maxDelaySeconds = extractIntValue(parseFrom, "maxDelaySeconds");
            if (maxDelaySeconds != null) config.maxDelaySeconds = maxDelaySeconds;

            Double spawnChance = extractDoubleValue(parseFrom, "spawnChance");
            if (spawnChance != null) config.spawnChance = spawnChance;

            Double despawnMinutes = extractDoubleValue(parseFrom, "despawnTimeMinutes");
            if (despawnMinutes != null) config.despawnTimeMinutes = despawnMinutes;

            Integer minSpawnDistance = extractIntValue(parseFrom, "minSpawnDistance");
            if (minSpawnDistance != null) config.minSpawnDistance = minSpawnDistance;

            Integer maxSpawnDistance = extractIntValue(parseFrom, "maxSpawnDistance");
            if (maxSpawnDistance != null) config.maxSpawnDistance = maxSpawnDistance;

            Boolean varyonZoneDelayScalingEnabled = extractBooleanValue(parseFrom, "varyonZoneDelayScalingEnabled");
            if (varyonZoneDelayScalingEnabled != null) {
                config.varyonZoneDelayScalingEnabled = varyonZoneDelayScalingEnabled;
            }
            Integer zoneDelayReferenceZone = extractIntValue(parseFrom, "zoneDelayReferenceZone");
            if (zoneDelayReferenceZone != null) {
                config.zoneDelayReferenceZone = zoneDelayReferenceZone;
            }
            Integer zoneDelayFastestZone = extractIntValue(parseFrom, "zoneDelayFastestZone");
            if (zoneDelayFastestZone != null) {
                config.zoneDelayFastestZone = zoneDelayFastestZone;
            }
            Double zoneDelaySpeedMultiplierAtFastest = extractDoubleValue(parseFrom, "zoneDelaySpeedMultiplierAtFastest");
            if (zoneDelaySpeedMultiplierAtFastest != null) {
                config.zoneDelaySpeedMultiplierAtFastest = zoneDelaySpeedMultiplierAtFastest;
            }

            parseVaryonRingRewards(config, parseFrom);

            Boolean globalComets = extractBooleanValue(parseFrom, "globalComets");
            if (globalComets != null) config.globalComets = globalComets;

            Boolean naturalSpawnsEnabled = extractBooleanValue(parseFrom, "naturalSpawnsEnabled");
            if (naturalSpawnsEnabled != null) config.naturalSpawnsEnabled = naturalSpawnsEnabled;

            Boolean injectUseForCleanSlateBlocks = extractBooleanValue(parseFrom, "injectUseForCleanSlateBlocks");
            if (injectUseForCleanSlateBlocks != null) config.injectUseForCleanSlateBlocks = injectUseForCleanSlateBlocks;

            Boolean disableWaveMobLoot = extractBooleanValue(parseFrom, "disableWaveMobLoot");
            if (disableWaveMobLoot != null) config.disableWaveMobLoot = disableWaveMobLoot;

            Boolean verboseWaveLogging = extractBooleanValue(parseFrom, "verboseWaveLogging");
            if (verboseWaveLogging != null) config.verboseWaveLogging = verboseWaveLogging;

            Boolean debugLogging = extractBooleanValue(parseFrom, "debugLogging");
            if (debugLogging != null) config.debugLogging = debugLogging;

            Integer waveTimeoutSeconds = extractIntValue(parseFrom, "waveTimeoutSeconds");
            if (waveTimeoutSeconds != null) config.waveTimeoutSeconds = waveTimeoutSeconds;

            Double waveSpawnMinRadius = extractDoubleValue(parseFrom, "waveSpawnMinRadius");
            if (waveSpawnMinRadius != null) config.waveSpawnMinRadius = waveSpawnMinRadius;

            Double waveSpawnMaxRadius = extractDoubleValue(parseFrom, "waveSpawnMaxRadius");
            if (waveSpawnMaxRadius != null) config.waveSpawnMaxRadius = waveSpawnMaxRadius;

            String combatMusicAmbienceId = com.varyon.comet.config.parser.ConfigJson.extractStringValue(parseFrom,
                    "combatMusicAmbienceId");
            if (combatMusicAmbienceId != null) {
                config.combatMusicAmbienceId = combatMusicAmbienceId.trim();
            }

            String cometLandSoundEventId = com.varyon.comet.config.parser.ConfigJson.extractStringValue(parseFrom,
                    "cometLandSoundEventId");
            if (cometLandSoundEventId != null) {
                config.cometLandSoundEventId = cometLandSoundEventId.trim();
            }
            String cometDestroySoundEventId = com.varyon.comet.config.parser.ConfigJson.extractStringValue(parseFrom,
                    "cometDestroySoundEventId");
            if (cometDestroySoundEventId != null) {
                config.cometDestroySoundEventId = cometDestroySoundEventId.trim();
            }
            String cometFallingNotifySoundEventId = com.varyon.comet.config.parser.ConfigJson.extractStringValue(
                    parseFrom, "cometFallingNotifySoundEventId");
            if (cometFallingNotifySoundEventId != null) {
                config.cometFallingNotifySoundEventId = cometFallingNotifySoundEventId.trim();
            }

            Boolean waveSpeedRewardEnabled = extractBooleanValue(parseFrom, "waveSpeedRewardEnabled");
            if (waveSpeedRewardEnabled != null) config.waveSpeedRewardEnabled = waveSpeedRewardEnabled;
            Double waveSpeedRewardMaxPercent = extractDoubleValue(parseFrom, "waveSpeedRewardMaxPercent");
            if (waveSpeedRewardMaxPercent != null) config.waveSpeedRewardMaxPercent = waveSpeedRewardMaxPercent;
            Double waveSpeedRewardMinPercent = extractDoubleValue(parseFrom, "waveSpeedRewardMinPercent");
            if (waveSpeedRewardMinPercent != null) config.waveSpeedRewardMinPercent = waveSpeedRewardMinPercent;

            String enabledWorldsArray = extractJsonArray(parseFrom, "enabledWorlds");
            if (enabledWorldsArray != null) {
                config.setEnabledWorlds(extractStringArray(enabledWorldsArray));
            }
            if (parseFrom.contains("\"disabledWorlds\"")) {
                LOGGER.warning(
                        "spawnSettings.disabledWorlds est obsolète — utilisez enabledWorlds (liste blanche ; vide = aucun monde).");
            }

            // Parse optional message templates (under top-level "messages" object if present)
            String messagesBlock = extractJsonObject(json, "messages");
            String messageSource = messagesBlock != null ? messagesBlock : json;

            String v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgCometFallingTitle");
            if (v != null && !v.isEmpty()) config.msgCometFallingTitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgCometFallingSubtitle");
            if (v != null && !v.isEmpty()) config.msgCometFallingSubtitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgCometFallingChatCoords");
            if (v != null && !v.isEmpty()) config.msgCometFallingChatCoords = v;

            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveBossTitle");
            if (v != null && !v.isEmpty()) config.msgWaveBossTitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveBossTitleNoCount");
            if (v != null && !v.isEmpty()) config.msgWaveBossTitleNoCount = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveBossSubtitle");
            if (v != null && !v.isEmpty()) config.msgWaveBossSubtitle = v;

            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveTitle");
            if (v != null && !v.isEmpty()) config.msgWaveTitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveTitleNoCount");
            if (v != null && !v.isEmpty()) config.msgWaveTitleNoCount = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveSubtitle");
            if (v != null && !v.isEmpty()) config.msgWaveSubtitle = v;

            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveFailedTitle");
            if (v != null && !v.isEmpty()) config.msgWaveFailedTitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveFailedSubtitle");
            if (v != null && !v.isEmpty()) config.msgWaveFailedSubtitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveFailedPlayerDeathSubtitle");
            if (v != null && !v.isEmpty()) config.msgWaveFailedPlayerDeathSubtitle = v;

            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveCompleteTitle");
            if (v != null && !v.isEmpty()) config.msgWaveCompleteTitle = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveCompleteSubtitle");
            if (v != null && !v.isEmpty()) config.msgWaveCompleteSubtitle = v;

            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveCompleteChatHeaderPrefix");
            if (v != null && !v.isEmpty()) config.msgWaveCompleteChatHeaderPrefix = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveCompleteChatHeader");
            if (v != null && !v.isEmpty()) config.msgWaveCompleteChatHeader = v;
            v = com.varyon.comet.config.parser.ConfigJson.extractStringValue(messageSource, "msgWaveCompleteChatItemPrefix");
            if (v != null && !v.isEmpty()) config.msgWaveCompleteChatItemPrefix = v;

            // Comet mob defs: from this json when migrating from legacy; otherwise loaded from cometMobs.json
            if (includeThemesFromJson) {
                config.themes = ThemeConfigParser.parseThemes(json);
                config.themeList = new ArrayList<>(config.themes.values());
                config.themesLoaded = !config.themes.isEmpty();
            } else {
                config.themes = new LinkedHashMap<>();
                config.themeList = new ArrayList<>();
            }

            config.rewardSettings = ThemeConfigParser.parseRewardSettings(json);

            config.hytaleZoneSpawnChances = ThemeConfigParser.parseZoneSpawnChances(json);

            parseThemePool(config, json);

            // Parse optional WorldProtect spawn rules
            String worldProtectRules = extractJsonObject(json, "worldProtectSpawnRules");
            if (worldProtectRules != null) {
                Boolean enabled = extractBooleanValue(worldProtectRules, "enabled");
                if (enabled != null) {
                    config.protectedZoneSpawnRulesEnabled = enabled;
                }

                Boolean defaultInProtectedRegion = extractBooleanValue(worldProtectRules, "defaultInWorldProtectRegion");
                if (defaultInProtectedRegion != null) {
                    config.protectedZoneDefaultInProtectedRegion = defaultInProtectedRegion;
                }

                String regionOverrides = extractJsonObject(worldProtectRules, "regionOverrides");
                if (regionOverrides != null) {
                    config.protectedZoneRegionOverrides = parseProtectedZoneRegionOverrides(regionOverrides);
                }
            }

            // Parse generic claim protection rules
            String claimProtect = extractJsonObject(json, "claimProtect");
            if (claimProtect != null) {
                Boolean enabled = extractBooleanValue(claimProtect, "enabled");
                if (enabled != null) {
                    config.claimProtectEnabled = enabled;
                }

                Boolean autoDetectProviders = extractBooleanValue(claimProtect, "autoDetectProviders");
                if (autoDetectProviders != null) {
                    config.claimProtectAutoDetectProviders = autoDetectProviders;
                }

                String providersArray = extractJsonArray(claimProtect, "providers");
                if (providersArray != null) {
                    config.setClaimProtectProviders(extractStringArray(providersArray));
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return config;
    }

    /**
     * Save configuration to file
     */
    public void save() {
        File configFile = getConfigFile();

        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        ensureDefaultsForPersistence();

        try (FileWriter writer = new FileWriter(configFile)) {
            String json = buildConfigJsonForPersistence();
            writer.write(json);
            writer.flush();
            LOGGER.info("Saved Comet Mod configuration to: " + configFile.getAbsolutePath());
            ThemeConfigLoader.saveCometMobs(this, ThemeConfigLoader.getCometMobsFile(configFile));
        } catch (IOException e) {
            LOGGER.severe("Failed to save config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<Integer, TierRewards> getDefaultRewardSettings() {
        Map<Integer, TierRewards> defaults = new LinkedHashMap<>();
        for (int tier = 1; tier <= 5; tier++) {
            defaults.put(tier, TierRewards.getDefaultForTier(tier));
        }
        return defaults;
    }

    private static Map<String, Boolean> parseProtectedZoneRegionOverrides(String jsonObject) {
        Map<String, Boolean> overrides = new LinkedHashMap<>();
        Pattern pairPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pairPattern.matcher(jsonObject);
        while (matcher.find()) {
            String regionId = matcher.group(1);
            String boolText = matcher.group(2);
            if (regionId == null || regionId.isBlank()) {
                continue;
            }
            overrides.put(regionId.toLowerCase(Locale.ROOT), Boolean.parseBoolean(boolText));
        }
        return overrides;
    }

    private void migrateZoneSpawnChancesKeysFromZeroToOneIfNeeded() {
        if (!hytaleZoneSpawnChances.containsKey("0")) {
            return;
        }
        ZoneSpawnChances[] slots = new ZoneSpawnChances[4];
        for (int i = 0; i <= 3; i++) {
            slots[i] = hytaleZoneSpawnChances.remove(String.valueOf(i));
        }
        for (int i = 0; i <= 3; i++) {
            if (slots[i] != null) {
                hytaleZoneSpawnChances.put(String.valueOf(i + 1), slots[i]);
            }
        }
    }

    private void ensureDefaultsForPersistence() {
        Map<String, ThemeConfig> defaultThemes = DefaultThemes.generateDefaults();
        for (Map.Entry<String, ThemeConfig> e : defaultThemes.entrySet()) {
            themes.putIfAbsent(e.getKey(), e.getValue());
        }
        themeList = new ArrayList<>(themes.values());
        for (Map.Entry<Integer, TierRewards> e : getDefaultRewardSettings().entrySet()) {
            rewardSettings.putIfAbsent(e.getKey(), e.getValue());
        }
        migrateZoneSpawnChancesKeysFromZeroToOneIfNeeded();
        Map<String, ZoneSpawnChances> defaultZoneChances = ZoneSpawnChances.generateDefaults();
        for (Map.Entry<String, ZoneSpawnChances> e : defaultZoneChances.entrySet()) {
            hytaleZoneSpawnChances.putIfAbsent(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, ShardDropRange> e : getDefaultShardDrops().entrySet()) {
            varyonRingShardDrops.putIfAbsent(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, VaryonMineralBonus> e : getDefaultMineralBonuses().entrySet()) {
            varyonRingMineralBonuses.putIfAbsent(e.getKey(), e.getValue());
        }
        if (themePoolByTier.isEmpty()) {
            for (Map.Entry<Integer, Map<String, Integer>> e : DEFAULT_THEME_POOL.entrySet()) {
                themePoolByTier.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
            }
        }
        if (waveTimeoutSeconds < 10) {
            waveTimeoutSeconds = 120;
        }
        if (waveSpawnMinRadius < 1.0) {
            waveSpawnMinRadius = 2.0;
        }
        if (waveSpawnMaxRadius < waveSpawnMinRadius + 1.0) {
            waveSpawnMaxRadius = 8.0;
        }
        if (waveSpeedRewardMaxPercent < 0.0) {
            waveSpeedRewardMaxPercent = 0.0;
        }
        if (waveSpeedRewardMaxPercent > 100.0) {
            waveSpeedRewardMaxPercent = 100.0;
        }
        if (waveSpeedRewardMinPercent < 0.0) {
            waveSpeedRewardMinPercent = 0.0;
        }
        if (waveSpeedRewardMinPercent > 100.0) {
            waveSpeedRewardMinPercent = 100.0;
        }
        if (waveSpeedRewardMinPercent > waveSpeedRewardMaxPercent) {
            double t = waveSpeedRewardMinPercent;
            waveSpeedRewardMinPercent = waveSpeedRewardMaxPercent;
            waveSpeedRewardMaxPercent = t;
        }

        if (msgCometFallingTitle == null) {
            msgCometFallingTitle = "Comète %tier% en chute !";
        }
        if (msgCometFallingSubtitle == null) {
            msgCometFallingSubtitle = "Regarde le ciel !";
        }
        if (msgCometFallingChatCoords == null) {
            msgCometFallingChatCoords =
                    "[Comète] Une comète %tier% est tombée en %x% ; %y% ; %z%";
        }
        if (msgWaveBossTitle == null) {
            msgWaveBossTitle = "Vague de boss %currentWave%/%totalWaves%";
        }
        if (msgWaveBossTitleNoCount == null) {
            msgWaveBossTitleNoCount = "Vague de boss !";
        }
        if (msgWaveBossSubtitle == null) {
            msgWaveBossSubtitle = "Boss : %bossStatus% | Temps : %time%";
        }
        if (msgWaveTitle == null) {
            msgWaveTitle = "Vague %currentWave%/%totalWaves% - %theme%";
        }
        if (msgWaveTitleNoCount == null) {
            msgWaveTitleNoCount = "%theme% en approche !";
        }
        if (msgWaveSubtitle == null) {
            msgWaveSubtitle = "Mobs : %killed%/%total% | Temps : %time%";
        }
        if (msgWaveFailedTitle == null) {
            msgWaveFailedTitle = "Vague échouée !";
        }
        if (msgWaveFailedSubtitle == null) {
            msgWaveFailedSubtitle = "Temps écoulé !";
        }
        if (msgWaveFailedPlayerDeathSubtitle == null) {
            msgWaveFailedPlayerDeathSubtitle = "Vous êtes mort !";
        }
        if (msgWaveCompleteTitle == null) {
            msgWaveCompleteTitle = "Vagues vaincues !";
        }
        if (msgWaveCompleteSubtitle == null) {
            msgWaveCompleteSubtitle = "Butin obtenu !";
        }
        if (msgWaveCompleteChatHeaderPrefix == null) {
            msgWaveCompleteChatHeaderPrefix = "[Comète] ";
        }
        if (msgWaveCompleteChatHeader == null) {
            msgWaveCompleteChatHeader = "Vagues vaincues ! Récompenses :";
        }
        if (msgWaveCompleteChatItemPrefix == null) {
            msgWaveCompleteChatItemPrefix = " - ";
        }
    }

    private String buildConfigJsonForPersistence() {
        return ThemeConfigWriter.generateFullConfig(
                minDelaySeconds, maxDelaySeconds, spawnChance,
                despawnTimeMinutes, minSpawnDistance, maxSpawnDistance,
                naturalSpawnsEnabled, globalComets, injectUseForCleanSlateBlocks, disableWaveMobLoot, verboseWaveLogging, debugLogging, getEnabledWorlds(),
                waveTimeoutSeconds, waveSpawnMinRadius, waveSpawnMaxRadius, combatMusicAmbienceId,
                cometLandSoundEventId, cometDestroySoundEventId, cometFallingNotifySoundEventId,
                waveSpeedRewardEnabled, waveSpeedRewardMaxPercent, waveSpeedRewardMinPercent,
                varyonZoneDelayScalingEnabled, zoneDelayReferenceZone, zoneDelayFastestZone, zoneDelaySpeedMultiplierAtFastest,
                varyonRingShardDrops,
                varyonRingMineralBonuses,
                hytaleZoneSpawnChances, rewardSettings,
                protectedZoneSpawnRulesEnabled, protectedZoneDefaultInProtectedRegion,
                protectedZoneRegionOverrides,
                claimProtectEnabled, claimProtectAutoDetectProviders, getClaimProtectProviders(),
                msgCometFallingTitle, msgCometFallingSubtitle, msgCometFallingChatCoords,
                msgWaveBossTitle, msgWaveBossTitleNoCount, msgWaveBossSubtitle,
                msgWaveTitle, msgWaveTitleNoCount, msgWaveSubtitle,
                msgWaveFailedTitle, msgWaveFailedSubtitle, msgWaveFailedPlayerDeathSubtitle,
                msgWaveCompleteTitle, msgWaveCompleteSubtitle,
                msgWaveCompleteChatHeaderPrefix, msgWaveCompleteChatHeader, msgWaveCompleteChatItemPrefix,
                themePoolByTier);
    }

    private static void syncConfigFilesOnBoot(CometConfig config, File configFile) {
        if (config == null || configFile == null) {
            return;
        }

        try {
            config.ensureDefaultsForPersistence();

            String mainConfigJson = config.buildConfigJsonForPersistence();
            upsertJsonIfChanged(configFile, mainConfigJson, CONFIG_FILE_NAME);

            File cometMobsFile = ThemeConfigLoader.getCometMobsFile(configFile);
            String cometMobsJson = ThemeConfigWriter.generateCometMobsConfig(config.themes);
            upsertJsonIfChanged(cometMobsFile, cometMobsJson, COMET_MOBS_FILE_NAME);
        } catch (Exception e) {
            LOGGER.warning("Failed to merge/create config JSON files on boot: " + e.getMessage());
        }
    }

    /**
     * Unconditionally write config.json and cometMobs.json (used when validation failed so repair persists).
     */
    private static void forceWriteConfigAndThemes(CometConfig config, File configFile) {
        try {
            String mainJson = config.buildConfigJsonForPersistence();
            java.nio.file.Files.writeString(configFile.toPath(), mainJson != null ? mainJson : "{}");
            LOGGER.info("Wrote config.json (repair after validation errors).");
            File cometMobsFile = ThemeConfigLoader.getCometMobsFile(configFile);
            String cometMobsJson = ThemeConfigWriter.generateCometMobsConfig(config.themes);
            if (cometMobsJson != null) {
                java.nio.file.Files.writeString(cometMobsFile.toPath(), cometMobsJson);
                LOGGER.info("Wrote " + COMET_MOBS_FILE_NAME + " (repair after validation errors).");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to force-write config/cometMobs: " + e.getMessage());
        }
    }

    private static void upsertJsonIfChanged(File file, String desiredContent, String logicalName) throws IOException {
        if (file == null || desiredContent == null) {
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean existed = file.exists() && file.isFile();
        String currentContent = existed ? java.nio.file.Files.readString(file.toPath()) : "";

        boolean forceRepair = CONFIG_FILE_NAME.equals(logicalName)
                && desiredContent != null
                && desiredContent.contains("\"spawnSettings\"")
                && (currentContent == null || currentContent.trim().isEmpty() || !currentContent.contains("spawnSettings"));

        boolean obsoleteLayout = CONFIG_FILE_NAME.equals(logicalName) && configJsonNeedsCanonicalRewrite(currentContent);

        if (!forceRepair && !obsoleteLayout
                && normalizeForCompare(currentContent).equals(normalizeForCompare(desiredContent))) {
            return;
        }

        if (obsoleteLayout && existed) {
            LOGGER.info(logicalName
                    + " : format obsolète détecté (migration loot/mondes / anciennes clés). Réécriture.");
        }

        java.nio.file.Files.writeString(file.toPath(), desiredContent);
        if (existed) {
            LOGGER.info("Merged and synchronized " + logicalName + " on boot.");
        } else {
            LOGGER.info("Created " + logicalName + " on boot.");
        }
    }

    private static String normalizeForCompare(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\r\n", "\n").trim();
    }

    private static boolean configJsonNeedsCanonicalRewrite(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        return json.contains("\"zoneBaseLootPools\"")
                || json.contains("\"tierInheritanceWeights\"")
                || json.contains("\"tierSettings\"")
                || json.contains("\"rewardSettings\"")
                || json.contains("\"disabledWorlds\"");
    }

    private void migrateLegacyEnglishMessagesToFrench() {
        msgCometFallingTitle = replaceIfEnglish(msgCometFallingTitle, "%tier% Comet Falling!", "Comète %tier% en chute !");
        msgCometFallingSubtitle = replaceIfEnglish(msgCometFallingSubtitle, "Watch the sky!", "Regarde le ciel !");
        msgCometFallingChatCoords = replaceIfEnglish(msgCometFallingChatCoords,
                "%tier% Comet falling! Target: X=%x%, Y=%y%, Z=%z%",
                "[Comète] Une comète %tier% est tombée en %x% ; %y% ; %z%");
        msgCometFallingChatCoords = migrateLegacyCometFallingChatCoordsFrench(msgCometFallingChatCoords);
        msgWaveBossTitle = replaceIfEnglish(msgWaveBossTitle, "Boss Wave %currentWave%/%totalWaves%",
                "Vague de boss %currentWave%/%totalWaves%");
        msgWaveBossTitleNoCount = replaceIfEnglish(msgWaveBossTitleNoCount, "Boss Wave!", "Vague de boss !");
        msgWaveBossSubtitle = replaceIfEnglish(msgWaveBossSubtitle, "Boss: %bossStatus% | Time: %time%",
                "Boss : %bossStatus% | Temps : %time%");
        msgWaveTitle = replaceIfEnglish(msgWaveTitle, "Wave %currentWave%/%totalWaves% - %theme%",
                "Vague %currentWave%/%totalWaves% - %theme%");
        msgWaveTitleNoCount = replaceIfEnglish(msgWaveTitleNoCount, "%theme% Incoming!", "%theme% en approche !");
        msgWaveSubtitle = replaceIfEnglish(msgWaveSubtitle, "Mobs: %killed%/%total% | Time: %time%",
                "Mobs : %killed%/%total% | Temps : %time%");
        msgWaveFailedTitle = replaceIfEnglish(msgWaveFailedTitle, "Wave Failed!", "Vague échouée !");
        msgWaveFailedSubtitle = replaceIfEnglish(msgWaveFailedSubtitle, "Time's Up!", "Temps écoulé !");
        msgWaveFailedPlayerDeathSubtitle = replaceIfEnglish(msgWaveFailedPlayerDeathSubtitle, "You died!",
                "Vous êtes mort !");
        msgWaveCompleteTitle = replaceIfEnglish(msgWaveCompleteTitle, "Wave Complete!", "Vagues vaincues !");
        msgWaveCompleteTitle = replaceIfEnglish(msgWaveCompleteTitle, "Vague terminée !", "Vagues vaincues !");
        msgWaveCompleteSubtitle = replaceIfEnglish(msgWaveCompleteSubtitle, "Loot Dropped!", "Butin obtenu !");
        msgWaveCompleteChatHeaderPrefix = replaceIfEnglish(msgWaveCompleteChatHeaderPrefix, "[Comet] ", "[Comète] ");
        msgWaveCompleteChatHeader = replaceIfEnglish(msgWaveCompleteChatHeader, "Wave Complete! Your rewards:",
                "Vagues vaincues ! Récompenses :");
        msgWaveCompleteChatHeader = replaceIfEnglish(msgWaveCompleteChatHeader, "Vague terminée ! Récompenses :",
                "Vagues vaincues ! Récompenses :");
    }

    private static String migrateLegacyCometFallingChatCoordsFrench(String current) {
        if (current == null || current.isEmpty()) {
            return current;
        }
        if ("Comète %tier% ! Cible : X=%x%, Y=%y%, Z=%z%".equals(current)) {
            return "[Comète] Une comète %tier% est tombée en %x% ; %y% ; %z%";
        }
        return current;
    }

    private static String replaceIfEnglish(String current, String english, String french) {
        if (current == null) {
            return french;
        }
        return english.equals(current.trim()) ? french : current;
    }

    /**
     * Apply spawn settings to the spawn task
     */
    public void applyToSpawnTask(CometSpawnTask spawnTask) {
        if (spawnTask != null) {
            spawnTask.setMinDelaySeconds(minDelaySeconds);
            spawnTask.setMaxDelaySeconds(maxDelaySeconds);
            spawnTask.setSpawnChance(spawnChance);
            spawnTask.setMinSpawnDistance(minSpawnDistance);
            spawnTask.setMaxSpawnDistance(maxSpawnDistance);
        }
    }

    // ========== Theme Access Methods ==========

    /**
     * Get all theme configurations
     */
    public Map<String, ThemeConfig> getThemes() {
        return themes;
    }

    public void setThemes(Map<String, ThemeConfig> themes) {
        this.themes = themes != null ? themes : new LinkedHashMap<>();
    }

    /**
     * Get themes as an ordered list (for random selection)
     */
    public List<ThemeConfig> getThemeList() {
        return themeList;
    }

    public void setThemeList(List<ThemeConfig> themeList) {
        this.themeList = themeList != null ? themeList : new ArrayList<>();
    }

    public void setThemesLoaded(boolean themesLoaded) {
        this.themesLoaded = themesLoaded;
    }

    /**
     * Get a theme by ID
     */
    public ThemeConfig getTheme(String id) {
        return themes.get(id);
    }

    /**
     * Get all themes available for a specific tier (excludes themes with naturalSpawn: false)
     *
     * @param tier The comet tier (1-4)
     * @return List of themes that can spawn naturally at this tier
     */
    public List<ThemeConfig> getThemesForTier(int tier) {
        List<ThemeConfig> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : getResolvedThemePool(tier).entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) {
                continue;
            }
            ThemeConfig theme = themes.get(e.getKey());
            if (theme != null && theme.isNaturalSpawn()) {
                result.add(theme);
            }
        }
        return result;
    }

    public Map<String, Integer> getResolvedThemePool(int tier) {
        Map<String, Integer> fromFile = themePoolByTier.get(tier);
        if (fromFile != null && !fromFile.isEmpty()) {
            return fromFile;
        }
        return new LinkedHashMap<>(DEFAULT_THEME_POOL.getOrDefault(tier, Map.of()));
    }

    public String pickThemeByWeight(int tier, Random r) {
        String id = pickFromThemePool(getResolvedThemePool(tier), r);
        if (id != null) {
            return id;
        }
        Map<String, Integer> def = DEFAULT_THEME_POOL.get(tier);
        if (def != null && !def.isEmpty()) {
            id = pickFromThemePool(new LinkedHashMap<>(def), r);
            if (id != null) {
                return id;
            }
        }
        return pickAnyNaturalThemeId(r);
    }

    private String pickFromThemePool(Map<String, Integer> pool, Random r) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        int total = 0;
        for (Map.Entry<String, Integer> e : pool.entrySet()) {
            int weight = e.getValue() != null ? e.getValue() : 0;
            ThemeConfig t = themes.get(e.getKey());
            if (weight <= 0 || t == null || !t.isNaturalSpawn()) {
                continue;
            }
            total += weight;
        }
        if (total <= 0) {
            return null;
        }
        int x = r.nextInt(total);
        for (Map.Entry<String, Integer> e : pool.entrySet()) {
            int weight = e.getValue() != null ? e.getValue() : 0;
            ThemeConfig t = themes.get(e.getKey());
            if (weight <= 0 || t == null || !t.isNaturalSpawn()) {
                continue;
            }
            if (x < weight) {
                return e.getKey();
            }
            x -= weight;
        }
        return null;
    }

    private String pickAnyNaturalThemeId(Random r) {
        int n = 0;
        for (ThemeConfig t : themeList) {
            if (t != null && t.isNaturalSpawn()) {
                n++;
            }
        }
        if (n <= 0) {
            return null;
        }
        int pick = r.nextInt(n);
        for (ThemeConfig t : themeList) {
            if (t != null && t.isNaturalSpawn()) {
                if (pick == 0) {
                    return t.getId();
                }
                pick--;
            }
        }
        return null;
    }

    public boolean repairLegacyNaturalSpawnIfAllDisabled() {
        if (themes == null || themes.isEmpty()) {
            return false;
        }
        int natural = 0;
        for (ThemeConfig t : themes.values()) {
            if (t != null && t.isNaturalSpawn()) {
                natural++;
            }
        }
        if (natural > 0) {
            return false;
        }
        LOGGER.warning("All themes had naturalSpawn=false (legacy export). Setting naturalSpawn=true for every theme.");
        for (ThemeConfig t : themes.values()) {
            if (t != null) {
                t.setNaturalSpawn(true);
            }
        }
        themeList = new ArrayList<>(themes.values());
        return true;
    }

    public void logThemePoolEligibilityWarnings() {
        if (themes == null || themes.isEmpty()) {
            return;
        }
        for (int tier = 1; tier <= 5; tier++) {
            Map<String, Integer> pool = getResolvedThemePool(tier);
            if (pool == null || pool.size() <= 1) {
                continue;
            }
            int eligible = 0;
            String lastEligibleId = null;
            for (Map.Entry<String, Integer> e : pool.entrySet()) {
                int w = e.getValue() != null ? e.getValue() : 0;
                if (w <= 0) {
                    continue;
                }
                ThemeConfig th = themes.get(e.getKey());
                if (th != null && th.isNaturalSpawn()) {
                    eligible++;
                    lastEligibleId = e.getKey();
                }
            }
            if (eligible == 1) {
                LOGGER.warning("Tier " + tier + " themePool lists " + pool.size()
                        + " themes but only one is eligible for random waves ('" + lastEligibleId
                        + "'). Check cometMobs.json: naturalSpawn and theme ids must match the pool.");
            }
        }
    }

    /**
     * Get all theme display names
     */
    public String[] getThemeNames() {
        String[] names = new String[themeList.size()];
        for (int i = 0; i < themeList.size(); i++) {
            names[i] = themeList.get(i).getDisplayName();
        }
        return names;
    }

    /**
     * Get theme count
     */
    public int getThemeCount() {
        return themeList.size();
    }

    /**
     * Check if themes were loaded successfully
     */
    public boolean hasThemes() {
        return themesLoaded && !themes.isEmpty();
    }

    public long getWaveTimeoutMillis() {
        int sec = Math.max(10, waveTimeoutSeconds);
        return sec * 1000L;
    }

    public double[] getWaveSpawnRadiusRange() {
        double min = Math.max(1.0, waveSpawnMinRadius);
        double max = Math.max(min + 1.0, waveSpawnMaxRadius);
        return new double[] { min, max };
    }

    /**
     * Multiplier (0–1) applied to reward stack sizes from chest loot, shards, and mineral bonus.
     * Uses total time budget = per-wave timeout × wave count vs elapsed since encounter start.
     * u = elapsed / budget: u ≤ 0.5 → max%; u ≥ 1 → min%; linear between 0.5 and 1.
     */
    public double getWaveSpeedRewardMultiplier(long encounterStartMs, int totalWaveCount, CometTier tier) {
        if (!waveSpeedRewardEnabled) {
            return 1.0;
        }
        long waveMs = com.varyon.comet.wave.CometWaveRunner.getTierTimeoutMs(tier);
        int waves = Math.max(1, totalWaveCount);
        long tMax = waveMs * (long) waves;
        if (tMax <= 0L) {
            return 1.0;
        }
        long elapsed = System.currentTimeMillis() - encounterStartMs;
        double u = elapsed / (double) tMax;
        if (u < 0.0) {
            u = 0.0;
        }
        if (u > 1.0) {
            u = 1.0;
        }
        double maxMult = waveSpeedRewardMaxPercent / 100.0;
        double minMult = waveSpeedRewardMinPercent / 100.0;
        if (u <= 0.5) {
            return maxMult;
        }
        if (u >= 1.0) {
            return minMult;
        }
        return maxMult + (minMult - maxMult) * (u - 0.5) / 0.5;
    }

    /**
     * Get reward settings for a specific tier
     * 
     * @param tier The tier number (1-4)
     */
    public TierRewards getTierRewards(int tier) {
        return rewardSettings.getOrDefault(tier, TierRewards.getDefaultForTier(tier));
    }

    /**
     * Get all reward settings
     */
    public Map<Integer, TierRewards> getAllRewardSettings() {
        return rewardSettings;
    }

    private static final Map<String, ShardDropRange> DEFAULT_SHARD_DROPS = createDefaultShardDropsMap();

    private static Map<String, ShardDropRange> createDefaultShardDropsMap() {
        Map<String, ShardDropRange> m = new LinkedHashMap<>();
        m.put("1", new ShardDropRange(1, 1));
        m.put("2", new ShardDropRange(2, 3));
        m.put("3", new ShardDropRange(4, 5));
        m.put("4", new ShardDropRange(6, 7));
        m.put("5", new ShardDropRange(8, 9));
        m.put("6", new ShardDropRange(10, 11));
        m.put("7", new ShardDropRange(12, 13));
        m.put("8", new ShardDropRange(14, 15));
        m.put("9", new ShardDropRange(16, 17));
        m.put("10", new ShardDropRange(18, 19));
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, ShardDropRange> getDefaultShardDrops() {
        return DEFAULT_SHARD_DROPS;
    }

    private static final Map<String, VaryonMineralBonus> DEFAULT_MINERAL_BONUSES = createDefaultMineralBonusesMap();

    private static Map<String, VaryonMineralBonus> createDefaultMineralBonusesMap() {
        Map<String, VaryonMineralBonus> m = new LinkedHashMap<>();
        m.put("4", new VaryonMineralBonus("Ingredient_Bar_Mithril", 0.15));
        m.put("5", new VaryonMineralBonus("Ingredient_Bar_Mithril", 0.20));
        m.put("6", new VaryonMineralBonus("Ingredient_Bar_Mithril", 0.25));
        m.put("7", new VaryonMineralBonus("Ingredient_Bar_Onyxium", 0.15));
        m.put("8", new VaryonMineralBonus("Ingredient_Bar_Onyxium", 0.20));
        m.put("9", new VaryonMineralBonus("Ingredient_Bar_Onyxium", 0.25));
        m.put("10", new VaryonMineralBonus("Ingredient_Bar_Prisma", 0.10));
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, VaryonMineralBonus> getDefaultMineralBonuses() {
        return DEFAULT_MINERAL_BONUSES;
    }

    public VaryonMineralBonus getVaryonMineralBonusForRingOrNull(int varyonRing) {
        int ring = varyonRing <= 0 ? 1 : varyonRing;
        ring = Math.max(VaryonZoneResolver.VARYON_RING_MIN, Math.min(VaryonZoneResolver.VARYON_RING_MAX, ring));
        VaryonMineralBonus b = varyonRingMineralBonuses.get(String.valueOf(ring));
        if (b == null) {
            b = getDefaultMineralBonuses().get(String.valueOf(ring));
        }
        return b;
    }

    public String rollOptionalMineralBonusItem(int varyonRing, Random random) {
        VaryonMineralBonus b = getVaryonMineralBonusForRingOrNull(varyonRing);
        if (b == null || b.itemId() == null || b.itemId().isBlank()) {
            return null;
        }
        if (random.nextDouble() < b.chance()) {
            return b.itemId();
        }
        return null;
    }

    public double getNaturalSpawnDelayScaleForVaryonZone(int varyonZone) {
        if (!varyonZoneDelayScalingEnabled) {
            return 1.0;
        }
        int z0 = zoneDelayReferenceZone;
        int z1 = zoneDelayFastestZone;
        double speedMult = zoneDelaySpeedMultiplierAtFastest;
        if (speedMult < 1.0) {
            speedMult = 1.0;
        }
        if (z1 <= z0) {
            return 1.0;
        }
        int z = varyonZone > 0 ? varyonZone : z0;
        int clamped = Math.max(z0, Math.min(z1, z));
        double invAtEnd = 1.0 / speedMult;
        double t = (double) (clamped - z0) / (double) (z1 - z0);
        return 1.0 + (invAtEnd - 1.0) * t;
    }

    public int getEffectiveNaturalSpawnMinDelaySeconds(int varyonZone) {
        double scale = getNaturalSpawnDelayScaleForVaryonZone(varyonZone);
        return Math.max(1, (int) Math.round(minDelaySeconds * scale));
    }

    public int getEffectiveNaturalSpawnMaxDelaySeconds(int varyonZone) {
        double scale = getNaturalSpawnDelayScaleForVaryonZone(varyonZone);
        return Math.max(1, (int) Math.round(maxDelaySeconds * scale));
    }

    public int rollShardCountForVaryonRing(int varyonRing, Random random) {
        int ring = varyonRing;
        if (ring <= 0) {
            ring = VaryonZoneResolver.VARYON_RING_MIN;
        }
        ring = Math.max(VaryonZoneResolver.VARYON_RING_MIN, Math.min(VaryonZoneResolver.VARYON_RING_MAX, ring));
        ShardDropRange r = varyonRingShardDrops.get(String.valueOf(ring));
        if (r == null) {
            r = getDefaultShardDrops().get(String.valueOf(ring));
        }
        if (r == null) {
            return 1;
        }
        int lo = Math.min(r.min, r.max);
        int hi = Math.max(r.min, r.max);
        return lo + random.nextInt(hi - lo + 1);
    }

    // ========== Hytale zone spawn chances (tier weights, keys "1"–"4") ==========

    /**
     * @param hytaleZone Hytale world-map zone index, {@value com.varyon.comet.integration.VaryonZoneResolver#HYTALE_ZONE_MIN}–{@value com.varyon.comet.integration.VaryonZoneResolver#HYTALE_ZONE_MAX}.
     */
    public ZoneSpawnChances getHytaleZoneSpawnChances(int hytaleZone) {
        if (hytaleZone < 0) {
            hytaleZone = 0;
        }
        int capped = Math.max(VaryonZoneResolver.HYTALE_ZONE_MIN,
                Math.min(VaryonZoneResolver.HYTALE_ZONE_MAX, hytaleZone));
        String zoneKey = String.valueOf(capped);
        ZoneSpawnChances chances = hytaleZoneSpawnChances.get(zoneKey);
        if (chances != null) {
            return chances;
        }
        if (hytaleZone == 0) {
            ZoneSpawnChances first = hytaleZoneSpawnChances.get("1");
            if (first != null) {
                return first;
            }
        }
        for (int z = capped - 1; z >= VaryonZoneResolver.HYTALE_ZONE_MIN; z--) {
            chances = hytaleZoneSpawnChances.get(String.valueOf(z));
            if (chances != null) {
                return chances;
            }
        }
        return null;
    }

    public Map<String, ZoneSpawnChances> getAllHytaleZoneSpawnChances() {
        return hytaleZoneSpawnChances;
    }

    public void setHytaleZoneSpawnChances(String zoneKey, ZoneSpawnChances chances) {
        hytaleZoneSpawnChances.put(zoneKey, chances);
    }

    // ========== Protected Zone Rules (WorldProtect integration) ==========

    public boolean isProtectedZoneSpawnRulesEnabled() {
        return protectedZoneSpawnRulesEnabled;
    }

    public boolean getProtectedZoneDefaultInProtectedRegion() {
        return protectedZoneDefaultInProtectedRegion;
    }

    public Map<String, Boolean> getProtectedZoneRegionOverrides() {
        return Collections.unmodifiableMap(protectedZoneRegionOverrides);
    }

    public synchronized boolean isProtectedRegionSpawnAllowed(String regionId) {
        if (!protectedZoneSpawnRulesEnabled) {
            return true;
        }
        if (regionId == null || regionId.isBlank()) {
            return true;
        }

        Boolean override = protectedZoneRegionOverrides.get(regionId.toLowerCase(Locale.ROOT));
        return (override != null) ? override : protectedZoneDefaultInProtectedRegion;
    }

    public boolean isClaimProtectEnabled() {
        return claimProtectEnabled;
    }

    public boolean isClaimProtectAutoDetectProviders() {
        return claimProtectAutoDetectProviders;
    }

    public synchronized List<String> getClaimProtectProviders() {
        return new ArrayList<>(claimProtectProviders);
    }

    public synchronized void setClaimProtectProviders(List<String> providers) {
        claimProtectProviders.clear();
        claimProtectProviderLookup.clear();

        if (providers == null) {
            return;
        }

        for (String provider : providers) {
            if (provider == null) {
                continue;
            }
            String trimmed = provider.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String normalized = normalizeClaimProviderName(trimmed);
            if (normalized == null || claimProtectProviderLookup.contains(normalized)) {
                continue;
            }

            claimProtectProviderLookup.add(normalized);
            claimProtectProviders.add(trimmed);
        }
    }

    /**
     * Synchronize per-region overrides against currently existing WorldProtect regions.
     * New regions are auto-added using defaultInProtectedRegion. Deleted regions are removed.
     *
     * @param activeRegionIds Region ids currently present in WorldProtect.
     * @return true if config changed and was saved.
     */
    public synchronized boolean syncProtectedRegionOverrides(Iterable<String> activeRegionIds) {
        if (activeRegionIds == null) {
            return false;
        }

        Set<String> normalizedActive = new LinkedHashSet<>();
        for (String regionId : activeRegionIds) {
            if (regionId == null || regionId.isBlank()) {
                continue;
            }
            String normalized = regionId.toLowerCase(Locale.ROOT);
            if ("__global__".equals(normalized) || "global".equals(normalized)) {
                continue;
            }
            normalizedActive.add(normalized);
        }

        boolean changed = false;

        for (String regionId : normalizedActive) {
            if (!protectedZoneRegionOverrides.containsKey(regionId)) {
                protectedZoneRegionOverrides.put(regionId, protectedZoneDefaultInProtectedRegion);
                changed = true;
            }
        }

        Set<String> keysToRemove = new LinkedHashSet<>();
        for (String existing : protectedZoneRegionOverrides.keySet()) {
            if (!normalizedActive.contains(existing)) {
                keysToRemove.add(existing);
            }
        }
        if (!keysToRemove.isEmpty()) {
            for (String removeKey : keysToRemove) {
                protectedZoneRegionOverrides.remove(removeKey);
            }
            changed = true;
        }

        if (changed) {
            save();
        }

        return changed;
    }

    public synchronized List<String> getEnabledWorlds() {
        return new ArrayList<>(enabledWorlds);
    }

    public synchronized void setEnabledWorlds(List<String> worldNames) {
        enabledWorlds.clear();
        enabledWorldLookup.clear();

        if (worldNames == null) {
            return;
        }

        for (String worldName : worldNames) {
            String normalized = normalizeWorldName(worldName);
            if (normalized == null || enabledWorldLookup.contains(normalized)) {
                continue;
            }

            enabledWorldLookup.add(normalized);
            enabledWorlds.add(worldName.trim());
        }
    }

    public boolean isRaidEnabledInWorld(com.hypixel.hytale.server.core.universe.world.World world) {
        if (world == null) {
            return true;
        }
        return isRaidEnabledInWorld(world.getName());
    }

    public synchronized boolean isRaidEnabledInWorld(String worldName) {
        String normalized = normalizeWorldName(worldName);
        if (normalized == null) {
            return true;
        }
        if (enabledWorldLookup.isEmpty()) {
            return false;
        }
        return enabledWorldLookup.contains(normalized);
    }

    private static String normalizeWorldName(String worldName) {
        if (worldName == null) {
            return null;
        }

        String trimmed = worldName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String normalizeClaimProviderName(String providerName) {
        if (providerName == null) {
            return null;
        }

        String normalized = providerName.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");

        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Add or update a theme in the config
     * Adds theme to all tiers (1-5) by default if no tiers specified
     */
    public void addOrUpdateTheme(ThemeConfig theme) {
        if (theme == null || theme.getId() == null) {
            LOGGER.warning("Cannot add null theme or theme with null ID");
            return;
        }

        // If theme has no tiers specified, add all tiers by default
        if (theme.getTiers() == null || theme.getTiers().isEmpty()) {
            List<Integer> allTiers = new ArrayList<>();
            for (int tier = 1; tier <= 5; tier++) {
                allTiers.add(tier);
            }
            theme.setTiers(allTiers);
        }

        // Add or update in themes map
        themes.put(theme.getId(), theme);

        // Update theme list
        themeList.removeIf(t -> t.getId().equals(theme.getId()));
        themeList.add(theme);

        themesLoaded = true;
        LOGGER.info("Added/updated theme: " + theme.getId());
    }

    /**
     * Remove a theme by ID
     */
    public void removeTheme(String themeId) {
        if (themeId == null) {
            return;
        }

        themes.remove(themeId);
        themeList.removeIf(t -> themeId.equals(t.getId()));

        LOGGER.info("Removed theme: " + themeId);
    }

    /**
     * Reload configuration from disk (renamed to avoid conflict)
     */
    public static void reloadConfig() {
        instance = null;
        load();
    }

}
