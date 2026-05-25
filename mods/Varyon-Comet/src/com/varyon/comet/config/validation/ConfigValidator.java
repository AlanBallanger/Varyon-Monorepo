package com.varyon.comet.config.validation;

import static com.varyon.comet.config.parser.ConfigJson.extractArrayFromPosition;
import static com.varyon.comet.config.parser.ConfigJson.extractDoubleValue;
import static com.varyon.comet.config.parser.ConfigJson.extractIntValue;
import static com.varyon.comet.config.parser.ConfigJson.extractJsonArray;
import static com.varyon.comet.config.parser.ConfigJson.extractJsonObject;
import static com.varyon.comet.config.parser.ConfigJson.extractObjectFromPosition;
import static com.varyon.comet.config.parser.ConfigJson.extractStringArray;

import java.util.List;

/**
 * Lightweight schema validator for mod JSON configs.
 */
public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static ConfigValidationReport validateCometConfig(String json) {
        ConfigValidationReport report = new ConfigValidationReport();
        if (json == null || json.trim().isEmpty()) {
            report.error("config.json is empty. Restore a valid config file.");
            return report;
        }

        validateTopLevelBlocks(json, report);
        validateSpawnSettings(json, report);
        validateClaimProtect(json, report);
        validateCometMobsSchema(json, report);
        validateLootByTierBlocks(json, report);

        return report;
    }

    private static void validateTopLevelBlocks(String json, ConfigValidationReport report) {
        requireObject(json, "spawnSettings", report);
        requireObject(json, "messages", report);
        if (extractJsonObject(json, "hytaleZoneSpawnChances") == null && extractJsonObject(json, "zoneSpawnChances") == null) {
            report.error("Missing required top-level object: hytaleZoneSpawnChances (ou zoneSpawnChances pour compatibilité).");
        }
        requireLootSection(json, report);
    }

    private static void requireLootSection(String json, ConfigValidationReport report) {
        if (extractJsonObject(json, "lootByTier") == null && extractJsonObject(json, "rewardSettings") == null) {
            report.error("Missing required top-level object: lootByTier (ou rewardSettings pour compatibilité).");
        }
    }

    private static void validateSpawnSettings(String json, ConfigValidationReport report) {
        String spawnSettings = extractJsonObject(json, "spawnSettings");
        if (spawnSettings == null) {
            return;
        }

        Integer minDelay = extractIntValue(spawnSettings, "minDelaySeconds");
        Integer maxDelay = extractIntValue(spawnSettings, "maxDelaySeconds");
        Double chance = extractDoubleValue(spawnSettings, "spawnChance");
        Integer minDistance = extractIntValue(spawnSettings, "minSpawnDistance");
        Integer maxDistance = extractIntValue(spawnSettings, "maxSpawnDistance");
        Double despawn = extractDoubleValue(spawnSettings, "despawnTimeMinutes");

        if (minDelay != null && minDelay <= 0) {
            report.warn("spawnSettings.minDelaySeconds should be > 0.");
        }
        if (maxDelay != null && maxDelay <= 0) {
            report.warn("spawnSettings.maxDelaySeconds should be > 0.");
        }
        if (minDelay != null && maxDelay != null && maxDelay < minDelay) {
            report.error("spawnSettings.maxDelaySeconds must be >= minDelaySeconds.");
        }
        if (chance != null && (chance < 0.0 || chance > 1.0)) {
            report.error("spawnSettings.spawnChance must be between 0.0 and 1.0.");
        }
        if (minDistance != null && minDistance < 0) {
            report.warn("spawnSettings.minSpawnDistance should be >= 0.");
        }
        if (maxDistance != null && maxDistance < 0) {
            report.warn("spawnSettings.maxSpawnDistance should be >= 0.");
        }
        if (minDistance != null && maxDistance != null && maxDistance < minDistance) {
            report.error("spawnSettings.maxSpawnDistance must be >= minSpawnDistance.");
        }
        if (despawn != null && despawn <= 0) {
            report.warn("spawnSettings.despawnTimeMinutes should be > 0.");
        }

        Integer waveTimeout = extractIntValue(spawnSettings, "waveTimeoutSeconds");
        if (waveTimeout != null && waveTimeout <= 0) {
            report.warn("spawnSettings.waveTimeoutSeconds should be > 0.");
        }
        Double waveMinR = extractDoubleValue(spawnSettings, "waveSpawnMinRadius");
        Double waveMaxR = extractDoubleValue(spawnSettings, "waveSpawnMaxRadius");
        if (waveMinR != null && (waveMinR < 2.0 || waveMinR > 8.0)) {
            report.warn("spawnSettings.waveSpawnMinRadius devrait être entre 2 et 8.");
        }
        if (waveMaxR != null && (waveMaxR < 2.0 || waveMaxR > 8.0)) {
            report.warn("spawnSettings.waveSpawnMaxRadius devrait être entre 2 et 8.");
        }
        if (waveMinR != null && waveMaxR != null && waveMaxR < waveMinR) {
            report.error("spawnSettings.waveSpawnMaxRadius doit être >= waveSpawnMinRadius.");
        }

        Double speedMaxP = extractDoubleValue(spawnSettings, "waveSpeedRewardMaxPercent");
        Double speedMinP = extractDoubleValue(spawnSettings, "waveSpeedRewardMinPercent");
        if (speedMaxP != null && (speedMaxP < 0.0 || speedMaxP > 100.0)) {
            report.warn("spawnSettings.waveSpeedRewardMaxPercent devrait être entre 0 et 100.");
        }
        if (speedMinP != null && (speedMinP < 0.0 || speedMinP > 100.0)) {
            report.warn("spawnSettings.waveSpeedRewardMinPercent devrait être entre 0 et 100.");
        }
        if (speedMaxP != null && speedMinP != null && speedMinP > speedMaxP) {
            report.warn("spawnSettings.waveSpeedRewardMinPercent ne devrait pas dépasser waveSpeedRewardMaxPercent.");
        }

        Integer zoneDelayReferenceZone = extractIntValue(spawnSettings, "zoneDelayReferenceZone");
        Integer zoneDelayFastestZone = extractIntValue(spawnSettings, "zoneDelayFastestZone");
        Double zoneDelaySpeedMultiplierAtFastest = extractDoubleValue(spawnSettings, "zoneDelaySpeedMultiplierAtFastest");
        if (zoneDelayReferenceZone != null && zoneDelayFastestZone != null && zoneDelayFastestZone <= zoneDelayReferenceZone) {
            report.warn("spawnSettings.zoneDelayFastestZone should be > zoneDelayReferenceZone for linear scaling.");
        }
        if (zoneDelaySpeedMultiplierAtFastest != null && zoneDelaySpeedMultiplierAtFastest < 1.0) {
            report.warn("spawnSettings.zoneDelaySpeedMultiplierAtFastest should be >= 1.0.");
        }

        String varyonRingRewards = extractJsonObject(spawnSettings, "cometVaryonRingRewards");
        if (varyonRingRewards != null) {
            for (int i = 1; i <= 10; i++) {
                String obj = extractJsonObject(varyonRingRewards, String.valueOf(i));
                if (obj == null) {
                    continue;
                }
                Integer smin = extractIntValue(obj, "min");
                Integer smax = extractIntValue(obj, "max");
                if (smin != null && smax != null && smax < smin) {
                    report.warn("spawnSettings.cometVaryonRingRewards." + i + ": max < min.");
                }
                if (smin != null && smin < 1) {
                    report.warn("spawnSettings.cometVaryonRingRewards." + i + ": min should be >= 1.");
                }
                String mineralObj = extractJsonObject(obj, "mineralBonus");
                if (mineralObj != null) {
                    Double ch = extractDoubleValue(mineralObj, "chance");
                    if (ch != null && (ch < 0.0 || ch > 100.0)) {
                        report.warn("spawnSettings.cometVaryonRingRewards." + i + ".mineralBonus: chance should be 0..1 or 0..100.");
                    }
                }
            }
        }

        String enabledWorlds = extractJsonArray(spawnSettings, "enabledWorlds");
        if (spawnSettings.contains("\"enabledWorlds\"") && enabledWorlds == null) {
            report.error("spawnSettings.enabledWorlds doit être un tableau de noms de mondes.");
        } else if (enabledWorlds != null) {
            List<String> worldNames = extractStringArray(enabledWorlds);
            for (int i = 0; i < worldNames.size(); i++) {
                String worldName = worldNames.get(i);
                if (worldName == null || worldName.trim().isEmpty()) {
                    report.warn("spawnSettings.enabledWorlds[" + i + "] est vide et sera ignoré.");
                }
            }
        }
    }

    private static void validateClaimProtect(String json, ConfigValidationReport report) {
        String claimProtect = extractJsonObject(json, "claimProtect");
        if (claimProtect == null) {
            return;
        }

        String providers = extractJsonArray(claimProtect, "providers");
        if (claimProtect.contains("\"providers\"") && providers == null) {
            report.error("claimProtect.providers must be an array of provider-name strings.");
            return;
        }

        if (providers != null) {
            List<String> providerNames = extractStringArray(providers);
            for (int i = 0; i < providerNames.size(); i++) {
                String providerName = providerNames.get(i);
                if (providerName == null || providerName.trim().isEmpty()) {
                    report.warn("claimProtect.providers[" + i + "] is blank and will be ignored.");
                }
            }
        }
    }

    private static void validateCometMobsSchema(String json, ConfigValidationReport report) {
        String cometMobs = extractJsonObject(json, "cometMobs");
        if (cometMobs == null || cometMobs.length() < 2) {
            cometMobs = extractJsonObject(json, "themes");
        }
        if (cometMobs == null || cometMobs.length() < 2) {
            return;
        }

        int i = 1;
        int end = cometMobs.length() - 1;

        while (i < end) {
            i = skipWhitespaceAndCommas(cometMobs, i, end);
            if (i >= end) {
                break;
            }

            if (cometMobs.charAt(i) != '"') {
                i++;
                continue;
            }

            int keyEnd = findStringEnd(cometMobs, i + 1);
            if (keyEnd < 0) {
                break;
            }

            String key = cometMobs.substring(i + 1, keyEnd);
            i = keyEnd + 1;

            while (i < end && Character.isWhitespace(cometMobs.charAt(i))) {
                i++;
            }
            if (i >= end || cometMobs.charAt(i) != ':') {
                continue;
            }
            i++;

            while (i < end && Character.isWhitespace(cometMobs.charAt(i))) {
                i++;
            }
            if (i >= end) {
                break;
            }

            char valueStart = cometMobs.charAt(i);
            if (key.startsWith("_")) {
                if (valueStart == '"') {
                    report.info("cometMobs." + key + " recognized as pseudo-comment key.");
                } else {
                    report.warn("cometMobs." + key + " starts with '_' but is not a string comment.");
                }
                continue;
            }

            if (valueStart != '{') {
                report.error("cometMobs." + key + " must be an object. Non-object entries should be prefixed with '_' comments.");
            }

            int nextValuePos = skipJsonValue(cometMobs, i);
            i = nextValuePos > i ? nextValuePos : i + 1;
        }
    }

    private static int skipWhitespaceAndCommas(String text, int start, int end) {
        int i = start;
        while (i < end) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            }
            break;
        }
        return i;
    }

    private static int findStringEnd(String text, int start) {
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static int skipJsonValue(String json, int valueStart) {
        if (valueStart < 0 || valueStart >= json.length()) {
            return valueStart;
        }

        char valueType = json.charAt(valueStart);
        if (valueType == '{') {
            String object = extractObjectFromPosition(json, valueStart);
            return object != null ? valueStart + object.length() : valueStart + 1;
        }
        if (valueType == '[') {
            String array = extractArrayFromPosition(json, valueStart);
            return array != null ? valueStart + array.length() : valueStart + 1;
        }
        if (valueType == '"') {
            int stringEnd = findStringEnd(json, valueStart + 1);
            return stringEnd >= 0 ? stringEnd + 1 : valueStart + 1;
        }

        int i = valueStart;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') {
                break;
            }
            i++;
        }
        return i;
    }

    private static void validateLootByTierBlocks(String json, ConfigValidationReport report) {
        String lootBlock = extractJsonObject(json, "lootByTier");
        if (lootBlock == null) {
            lootBlock = extractJsonObject(json, "rewardSettings");
        }
        if (lootBlock == null) {
            return;
        }

        for (int tier = 1; tier <= 5; tier++) {
            if (extractJsonObject(lootBlock, String.valueOf(tier)) == null) {
                report.warn("loot par palier : entrée \"" + tier + "\" manquante ; défauts utilisés.");
            }
        }
    }

    private static void requireObject(String json, String key, ConfigValidationReport report) {
        if (extractJsonObject(json, key) == null) {
            report.error("Missing required top-level object: " + key);
        }
    }
}
