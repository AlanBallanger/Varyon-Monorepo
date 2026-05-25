package com.varyon.comet.config.parser;

import static com.varyon.comet.config.parser.ConfigJson.extractArrayFromPosition;
import static com.varyon.comet.config.parser.ConfigJson.extractArrayObjects;
import static com.varyon.comet.config.parser.ConfigJson.extractBooleanValue;
import static com.varyon.comet.config.parser.ConfigJson.extractDoubleValue;
import static com.varyon.comet.config.parser.ConfigJson.extractIntArray;
import static com.varyon.comet.config.parser.ConfigJson.extractIntValue;
import static com.varyon.comet.config.parser.ConfigJson.extractJsonArray;
import static com.varyon.comet.config.parser.ConfigJson.extractJsonObject;
import static com.varyon.comet.config.parser.ConfigJson.extractObjectFromPosition;
import static com.varyon.comet.config.parser.ConfigJson.extractTopLevelObjectEntries;
import static com.varyon.comet.config.parser.ConfigJson.extractStringArray;
import static com.varyon.comet.config.parser.ConfigJson.extractStringValue;

import com.varyon.comet.config.defaults.DefaultThemes;
import com.varyon.comet.config.model.BossEntry;
import com.varyon.comet.config.model.MobEntry;
import com.varyon.comet.config.model.RewardEntry;
import com.varyon.comet.config.model.ThemeConfig;
import com.varyon.comet.config.model.TierRewards;
import com.varyon.comet.config.model.WaveEntry;
import com.varyon.comet.config.model.ZoneSpawnChances;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON parser for theme configurations.
 * Handles nested objects and arrays for theme definitions.
 * 
 * Note: This is a simple parser without external dependencies.
 * For production use, consider using a JSON library like Gson.
 */
public class ThemeConfigParser {

    private static final Logger LOGGER = Logger.getLogger(ThemeConfigParser.class.getName());

    /**
     * Parse themes from a JSON string
     * 
     * @param json The full config JSON content
     * @return Map of theme ID to ThemeConfig
     */
    public static Map<String, ThemeConfig> parseThemes(String json) {
        Map<String, ThemeConfig> themes = new LinkedHashMap<>();

        try {
            String themesBlock = extractCometMobsObjectBlock(json);
            if (themesBlock == null || themesBlock.isEmpty()) {
                return DefaultThemes.generateDefaults();
            }

            for (String[] entry : extractTopLevelObjectEntries(themesBlock)) {
                String themeId = entry[0];
                String themeJson = entry[1];
                ThemeConfig theme = parseTheme(themeId, themeJson);
                if (theme != null) {
                    themes.put(themeId, theme);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing cometMobs: " + e.getMessage());
            e.printStackTrace();
        }

        if (themes.isEmpty()) {
            LOGGER.warning("No cometMobs parsed, falling back to defaults");
            return DefaultThemes.generateDefaults();
        }

        return themes;
    }

    private static String extractCometMobsObjectBlock(String json) {
        String block = extractJsonObject(json, "cometMobs");
        if (block != null && !block.isEmpty()) {
            return block;
        }
        return extractJsonObject(json, "themes");
    }

    /**
     * Parse a single theme from its JSON block
     */
    private static ThemeConfig parseTheme(String id, String json) {
        try {
            ThemeConfig theme = new ThemeConfig();
            theme.setId(id);

            // Parse displayName
            String displayName = extractStringValue(json, "displayName");
            theme.setDisplayName(displayName != null ? displayName : id);

            // Parse useTierSuffix (default true)
            Boolean useTierSuffix = extractBooleanValue(json, "useTierSuffix");
            theme.setUseTierSuffix(useTierSuffix != null ? useTierSuffix : true);

            // Parse randomBossSelection (default false)
            Boolean randomBossSelection = extractBooleanValue(json, "randomBossSelection");
            theme.setRandomBossSelection(randomBossSelection != null ? randomBossSelection : false);

            // Parse naturalSpawn (default true) - if false, theme won't spawn naturally.
            Boolean naturalSpawn = extractBooleanValue(json, "naturalSpawn");
            theme.setNaturalSpawn(naturalSpawn != null ? naturalSpawn : true);

            // Parse tiers array
            List<Integer> tiers = extractIntArray(json, "tiers");
            theme.setTiers(tiers);

            // Parse mobs array
            List<MobEntry> mobs = parseMobs(json);
            theme.setMobs(mobs);

            // Parse bosses array
            List<BossEntry> bosses = parseBosses(json);
            theme.setBosses(bosses);

            // Parse multi-wave array.
            List<WaveEntry> waves = parseWaves(json);
            if (waves.isEmpty()) {
                waves = synthesizeWavesFromTheme(theme);
            }
            theme.setWaves(waves);
            if (!waves.isEmpty()) {
                syncRootMobsAndBossesFromWaves(theme);
            }

            // Parse rewardOverride if present (per-tier custom loot)
            parseRewardOverride(json, theme);

            return theme;
        } catch (Exception e) {
            LOGGER.warning("Error parsing theme '" + id + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse rewardOverride section from theme JSON.
     * Format:
     * "rewardOverride": {
     *   "2": { "drops": [...], "bonusDrops": [...] },
     *   "3": { "drops": [...], "bonusDrops": [...] }
     * }
     */
    private static void parseRewardOverride(String json, ThemeConfig theme) {
        try {
            String rewardBlock = extractJsonObject(json, "rewardOverride");
            if (rewardBlock == null) {
                return; // No reward override configured
            }

            Map<Integer, TierRewards> rewardOverride = new LinkedHashMap<>();

            // Parse each tier's rewards: "1": {...}, "2": {...}, etc.
            for (int tier = 1; tier <= 5; tier++) {
                String tierKey = String.valueOf(tier);
                String tierJson = extractJsonObject(rewardBlock, tierKey);

                if (tierJson != null) {
                    TierRewards tr = new TierRewards();

                    // Parse drops array
                    List<RewardEntry> drops = parseRewardEntries(tierJson, "drops");
                    tr.setDrops(drops);

                    // Parse bonusDrops array
                    List<RewardEntry> bonusDrops = parseRewardEntries(tierJson, "bonusDrops");
                    tr.setBonusDrops(bonusDrops);

                    rewardOverride.put(tier, tr);
                }
            }

            if (!rewardOverride.isEmpty()) {
                theme.setRewardOverride(rewardOverride);
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing reward override: " + e.getMessage());
        }
    }

    private static Map<Integer, Integer> parseTierIntMap1to5(String jsonObj) {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        if (jsonObj == null) {
            return m;
        }
        for (int tier = 1; tier <= 5; tier++) {
            Integer v = extractIntValue(jsonObj, String.valueOf(tier));
            if (v != null) {
                m.put(tier, v);
            }
        }
        return m;
    }

    private static void normalizeMobEntryRange(MobEntry mob) {
        if (mob.getTierCounts() != null && !mob.getTierCounts().isEmpty()) {
            Map<Integer, Integer> tmin = mob.getTierMinCounts();
            Map<Integer, Integer> tmax = mob.getTierMaxCounts();
            if (tmin != null && tmax != null) {
                for (int t = 1; t <= 5; t++) {
                    if (!tmin.containsKey(t) || !tmax.containsKey(t)) {
                        continue;
                    }
                    if (tmin.get(t) > tmax.get(t)) {
                        int a = tmin.get(t);
                        tmin.put(t, tmax.get(t));
                        tmax.put(t, a);
                    }
                }
            }
            return;
        }
        Integer mn = mob.getExplicitMinCount();
        Integer mx = mob.getExplicitMaxCount();
        if (mn == null && mx == null) {
            return;
        }
        int c = mob.getCount();
        if (mn != null && mx == null) {
            mx = mn + 2;
        } else if (mx != null && mn == null) {
            mn = Math.max(1, mx - 2);
        } else if (mn == null || mx == null) {
            if (mn == null) {
                mn = Math.max(1, c - 1);
            }
            if (mx == null) {
                mx = c + 1;
            }
        }
        if (mn > mx) {
            int tmp = mn;
            mn = mx;
            mx = tmp;
        }
        mob.setExplicitMinCount(mn);
        mob.setExplicitMaxCount(mx);
        mob.setCount((mn + mx + 1) / 2);
    }

    /**
     * Parse mobs array from theme JSON
     */
    private static List<MobEntry> parseMobs(String json) {
        List<MobEntry> mobs = new ArrayList<>();

        try {
            String mobsArray = extractJsonArray(json, "mobs");
            if (mobsArray == null)
                return mobs;

            // Parse each mob object in the array
            List<String> mobObjects = extractArrayObjects(mobsArray);
            for (String mobJson : mobObjects) {
                MobEntry mob = new MobEntry();

                String id = extractStringValue(mobJson, "id");
                if (id != null)
                    mob.setId(id);

                // Try to parse count as integer first
                Integer count = extractIntValue(mobJson, "count");
                if (count != null) {
                    mob.setCount(count);
                } else {
                    // Try to parse as tier-based count object: "count": { "1": 4, "2": 5 }
                    String countObj = extractJsonObject(mobJson, "count");
                    if (countObj != null) {
                        Map<Integer, Integer> tierCounts = new LinkedHashMap<>();
                        for (int tier = 1; tier <= 5; tier++) {
                            Integer tierCount = extractIntValue(countObj, String.valueOf(tier));
                            if (tierCount != null) {
                                tierCounts.put(tier, tierCount);
                            }
                        }
                        if (!tierCounts.isEmpty()) {
                            mob.setTierCounts(tierCounts);
                            // Set simple count to max value as fallback
                            int maxCount = tierCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
                            mob.setCount(maxCount);
                        }
                    }
                }

                String minCountObj = extractJsonObject(mobJson, "minCount");
                if (minCountObj != null && !minCountObj.isBlank()) {
                    Map<Integer, Integer> tierMin = parseTierIntMap1to5(minCountObj);
                    if (!tierMin.isEmpty()) {
                        mob.setTierMinCounts(tierMin);
                    }
                } else {
                    Integer minI = extractIntValue(mobJson, "minCount");
                    if (minI != null) {
                        mob.setExplicitMinCount(minI);
                    }
                }

                String maxCountObj = extractJsonObject(mobJson, "maxCount");
                if (maxCountObj != null && !maxCountObj.isBlank()) {
                    Map<Integer, Integer> tierMax = parseTierIntMap1to5(maxCountObj);
                    if (!tierMax.isEmpty()) {
                        mob.setTierMaxCounts(tierMax);
                    }
                } else {
                    Integer maxI = extractIntValue(mobJson, "maxCount");
                    if (maxI != null) {
                        mob.setExplicitMaxCount(maxI);
                    }
                }

                normalizeMobEntryRange(mob);

                if (id != null && !id.isEmpty()) {
                    mobs.add(mob);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing mobs: " + e.getMessage());
        }

        return mobs;
    }

    /**
     * Parse waves array from theme JSON for multi-wave support.
     * Format:
     * "waves": [
     *   { "type": "normal", "mobs": [...] },
     *   { "type": "normal", "mobs": [...] },
     *   { "type": "boss", "bosses": [...], "randomBossSelection": true }
     * ]
     */
    private static List<WaveEntry> parseWaves(String json) {
        List<WaveEntry> waves = new ArrayList<>();

        try {
            String wavesArray = extractJsonArray(json, "waves");
            if (wavesArray == null) {
                return waves;
            }

            // Parse each wave object in the array
            List<String> waveObjects = extractArrayObjects(wavesArray);
            for (String waveJson : waveObjects) {
                WaveEntry wave = new WaveEntry();

                // Parse type (default to "normal")
                String type = extractStringValue(waveJson, "type");
                if (type != null) {
                    wave.setType(type);
                }

                // Parse mobs array (for normal waves)
                List<MobEntry> waveMobs = parseMobs(waveJson);
                wave.setMobs(waveMobs);

                // Parse bosses array (for boss waves)
                List<BossEntry> waveBosses = parseBosses(waveJson);
                wave.setBosses(waveBosses);

                // Parse randomBossSelection (default false)
                Boolean randomBossSelection = extractBooleanValue(waveJson, "randomBossSelection");
                wave.setRandomBossSelection(randomBossSelection != null ? randomBossSelection : false);

                waves.add(wave);
            }

        } catch (Exception e) {
            LOGGER.warning("Error parsing waves: " + e.getMessage());
        }

        return waves;
    }

    public static ThemeConfig parseThemeBlock(String id, String json) {
        return parseTheme(id, json);
    }

    private static void syncRootMobsAndBossesFromWaves(ThemeConfig theme) {
        List<WaveEntry> waves = theme.getWaves();
        if (waves == null || waves.isEmpty()) {
            return;
        }
        for (WaveEntry w : waves) {
            if (w.isNormalWave() && w.getMobs() != null && !w.getMobs().isEmpty()) {
                theme.setMobs(w.getMobs());
                break;
            }
        }
        for (WaveEntry w : waves) {
            if (w.isBossWave() && w.getBosses() != null && !w.getBosses().isEmpty()) {
                theme.setBosses(w.getBosses());
                break;
            }
        }
    }

    public static void rebuildWavesFromMobsAndBosses(ThemeConfig theme) {
        if (theme == null) {
            return;
        }
        theme.setWaves(synthesizeWavesFromTheme(theme));
        syncRootMobsAndBossesFromWaves(theme);
    }

    private static List<WaveEntry> synthesizeWavesFromTheme(ThemeConfig theme) {
        List<WaveEntry> waves = new ArrayList<>();

        if (theme.getMobs() != null && !theme.getMobs().isEmpty()) {
            WaveEntry normalWave = new WaveEntry(WaveEntry.WaveType.NORMAL);
            normalWave.setMobs(theme.getMobs());
            waves.add(normalWave);
        }

        if (theme.getBosses() != null && !theme.getBosses().isEmpty()) {
            WaveEntry bossWave = new WaveEntry(WaveEntry.WaveType.BOSS);
            bossWave.setBosses(theme.getBosses());
            bossWave.setRandomBossSelection(theme.useRandomBossSelection());
            waves.add(bossWave);
        }

        return waves;
    }

    /**
     * Parse bosses array from theme JSON
     */
    private static List<BossEntry> parseBosses(String json) {
        List<BossEntry> bosses = new ArrayList<>();

        try {
            String bossesArray = extractJsonArray(json, "bosses");
            if (bossesArray == null)
                return bosses;

            // Check if bosses are simple strings or objects
            if (bossesArray.contains("{")) {
                // Object format: { "id": "Boss" }
                List<String> bossObjects = extractArrayObjects(bossesArray);
                for (String bossJson : bossObjects) {
                    BossEntry boss = new BossEntry();

                    String id = extractStringValue(bossJson, "id");
                    if (id != null)
                        boss.setId(id);

                    Integer minI = extractIntValue(bossJson, "minCount");
                    Integer maxI = extractIntValue(bossJson, "maxCount");
                    if (minI != null && maxI != null) {
                        int mn = Math.max(1, minI);
                        int mx = Math.max(mn, maxI);
                        boss.setExplicitMinCount(mn);
                        boss.setExplicitMaxCount(mx);
                    } else if (minI != null) {
                        int mn = Math.max(1, minI);
                        boss.setExplicitMinCount(mn);
                        boss.setExplicitMaxCount(mn);
                    }

                    if (id != null && !id.isEmpty()) {
                        bosses.add(boss);
                    }
                }
            } else {
                // Simple string format: ["Boss1", "Boss2"]
                List<String> bossNames = extractStringArray(bossesArray);
                for (String name : bossNames) {
                    if (name != null && !name.isEmpty()) {
                        bosses.add(new BossEntry(name));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing bosses: " + e.getMessage());
        }

        return bosses;
    }

    /**
     * Parse reward settings from config JSON
     */
    public static Map<Integer, TierRewards> parseRewardSettings(String json) {
        Map<Integer, TierRewards> rewards = new LinkedHashMap<>();

        try {
            String rewardBlock = extractJsonObject(json, "lootByTier");
            if (rewardBlock == null) {
                rewardBlock = extractJsonObject(json, "rewardSettings");
            }
            if (rewardBlock == null) {
                for (int tier = 1; tier <= 5; tier++) {
                    rewards.put(tier, TierRewards.getDefaultForTier(tier));
                }
                return rewards;
            }

            // Parse each tier: "1": { ... }, "2": { ... }
            for (int tier = 1; tier <= 5; tier++) {
                String tierKey = String.valueOf(tier);
                String tierJson = extractJsonObject(rewardBlock, tierKey);

                if (tierJson != null) {
                    TierRewards tr = new TierRewards();

                    // Parse drops array
                    List<RewardEntry> drops = parseRewardEntries(tierJson, "drops");
                    tr.setDrops(drops);

                    // Parse bonusDrops array
                    List<RewardEntry> bonusDrops = parseRewardEntries(tierJson, "bonusDrops");
                    tr.setBonusDrops(bonusDrops);

                    rewards.put(tier, tr);
                } else {
                    // Use defaults for this tier
                    rewards.put(tier, TierRewards.getDefaultForTier(tier));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing reward settings: " + e.getMessage());
            // Return defaults
            for (int tier = 1; tier <= 5; tier++) {
                rewards.put(tier, TierRewards.getDefaultForTier(tier));
            }
        }

        return rewards;
    }

    /**
     * Parse reward entries (drops or bonusDrops) from tier JSON
     */
    private static List<RewardEntry> parseRewardEntries(String json, String key) {
        List<RewardEntry> entries = new ArrayList<>();

        try {
            String entriesArray = extractJsonArray(json, key);
            if (entriesArray == null)
                return entries;

            // Parse each reward object in the array
            List<String> rewardObjects = extractArrayObjects(entriesArray);
            for (String rewardJson : rewardObjects) {
                RewardEntry reward = new RewardEntry();

                String id = extractStringValue(rewardJson, "id");
                if (id != null)
                    reward.setId(id);

                Integer minCount = extractIntValue(rewardJson, "minCount");
                if (minCount != null)
                    reward.setMinCount(minCount);

                Integer maxCount = extractIntValue(rewardJson, "maxCount");
                if (maxCount != null)
                    reward.setMaxCount(maxCount);

                Double chance = extractDoubleValue(rewardJson, "chance");
                if (chance != null)
                    reward.setChance(chance);

                String displayName = extractStringValue(rewardJson, "displayName");
                if (displayName != null)
                    reward.setDisplayName(displayName);

                if (id != null && !id.isEmpty()) {
                    entries.add(reward);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing reward entries for '" + key + "': " + e.getMessage());
        }

        return entries;
    }

    /**
     * Parse Hytale world-map zone spawn chances (keys "1"–"4").
     * Accepts {@code hytaleZoneSpawnChances} or legacy {@code zoneSpawnChances}.
     */
    public static Map<String, ZoneSpawnChances> parseZoneSpawnChances(String json) {
        Map<String, ZoneSpawnChances> zoneChances = new LinkedHashMap<>();

        try {
            String zoneBlock = extractJsonObject(json, "hytaleZoneSpawnChances");
            if (zoneBlock == null) {
                zoneBlock = extractJsonObject(json, "zoneSpawnChances");
            }
            if (zoneBlock == null) {
                return zoneChances;
            }

            // Parse each zone entry: "0": { ... }, "1": { ... }
            Pattern zonePattern = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\\{");
            Matcher matcher = zonePattern.matcher(zoneBlock);

            while (matcher.find()) {
                String zoneKey = matcher.group(1);
                int startPos = matcher.end() - 1;
                String zoneJson = extractObjectFromPosition(zoneBlock, startPos);

                if (zoneJson != null) {
                    ZoneSpawnChances chances = new ZoneSpawnChances();

                    Double tier1 = extractDoubleValue(zoneJson, "tier1");
                    if (tier1 != null) chances.setTier1(tier1);

                    Double tier2 = extractDoubleValue(zoneJson, "tier2");
                    if (tier2 != null) chances.setTier2(tier2);

                    Double tier3 = extractDoubleValue(zoneJson, "tier3");
                    if (tier3 != null) chances.setTier3(tier3);

                    Double tier4 = extractDoubleValue(zoneJson, "tier4");
                    if (tier4 != null) chances.setTier4(tier4);

                    Double tier5 = extractDoubleValue(zoneJson, "tier5");
                    if (tier5 != null) chances.setTier5(tier5);

                    zoneChances.put(zoneKey, chances);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing zone spawn chances: " + e.getMessage());
            e.printStackTrace();
        }

        if (zoneChances.isEmpty()) {
            LOGGER.warning("No zone spawn chances parsed");
        }

        return zoneChances;
    }

}
