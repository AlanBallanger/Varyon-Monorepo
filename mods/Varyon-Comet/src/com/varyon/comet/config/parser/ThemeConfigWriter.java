package com.varyon.comet.config.parser;

import com.varyon.comet.config.model.BossEntry;
import com.varyon.comet.config.model.MobEntry;
import com.varyon.comet.config.model.RewardEntry;
import com.varyon.comet.config.model.ThemeConfig;
import com.varyon.comet.config.model.WaveEntry;
import com.varyon.comet.config.model.TierRewards;
import com.varyon.comet.config.model.ShardDropRange;
import com.varyon.comet.config.model.VaryonMineralBonus;
import com.varyon.comet.config.model.ZoneSpawnChances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON writer for generating config files.
 * Creates properly formatted JSON without external dependencies.
 */
public class ThemeConfigWriter {

    private static final String INDENT = "  ";

    public static String generateCometMobsConfig(Map<String, ThemeConfig> themes) {
        if (themes == null) {
            themes = java.util.Collections.emptyMap();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(INDENT).append("\"cometMobs\": {\n");
        int index = 0;
        for (Map.Entry<String, ThemeConfig> entry : themes.entrySet()) {
            index++;
            writeThemeMonsterGroupsOnly(sb, entry.getKey(), entry.getValue(), index < themes.size());
        }
        sb.append(INDENT).append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String generateFullConfig(
            int minDelaySeconds, int maxDelaySeconds, double spawnChance,
            double despawnTimeMinutes, int minSpawnDistance, int maxSpawnDistance,
            boolean naturalSpawnsEnabled, boolean globalComets, boolean injectUseForCleanSlateBlocks,
            boolean disableWaveMobLoot,
            boolean verboseWaveLogging,
            boolean debugLogging,
            List<String> enabledWorlds,
            int waveTimeoutSeconds, double waveSpawnMinRadius, double waveSpawnMaxRadius, String combatMusicAmbienceId,
            String cometLandSoundEventId, String cometDestroySoundEventId, String cometFallingNotifySoundEventId,
            boolean waveSpeedRewardEnabled, double waveSpeedRewardMaxPercent, double waveSpeedRewardMinPercent,
            boolean varyonZoneDelayScalingEnabled, int zoneDelayReferenceZone, int zoneDelayFastestZone,
            double zoneDelaySpeedMultiplierAtFastest,
            Map<String, ShardDropRange> varyonRingShardDrops,
            Map<String, VaryonMineralBonus> varyonRingMineralBonuses,
            Map<String, ZoneSpawnChances> hytaleZoneSpawnChances,
            Map<Integer, TierRewards> lootByTier,
            boolean protectedZoneRulesEnabled, boolean defaultInProtectedRegion,
            Map<String, Boolean> regionOverrides,
            boolean claimProtectEnabled, boolean claimProtectAutoDetectProviders, List<String> claimProtectProviders,
            String msgCometFallingTitle, String msgCometFallingSubtitle, String msgCometFallingChatCoords,
            String msgWaveBossTitle, String msgWaveBossTitleNoCount, String msgWaveBossSubtitle,
            String msgWaveTitle, String msgWaveTitleNoCount, String msgWaveSubtitle,
            String msgWaveFailedTitle, String msgWaveFailedSubtitle, String msgWaveFailedPlayerDeathSubtitle,
            String msgWaveCompleteTitle, String msgWaveCompleteSubtitle,
            String msgWaveCompleteChatHeaderPrefix, String msgWaveCompleteChatHeader, String msgWaveCompleteChatItemPrefix,
            Map<Integer, Map<String, Integer>> themePoolByTier) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append(INDENT).append("\"spawnSettings\": {\n");
        sb.append(INDENT).append(INDENT).append("\"naturalSpawnsEnabled\": ").append(naturalSpawnsEnabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"minDelaySeconds\": ").append(minDelaySeconds).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"maxDelaySeconds\": ").append(maxDelaySeconds).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"spawnChance\": ").append(spawnChance).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"despawnTimeMinutes\": ").append(despawnTimeMinutes).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"minSpawnDistance\": ").append(minSpawnDistance).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"maxSpawnDistance\": ").append(maxSpawnDistance).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"enabledWorlds\": ");
        appendStringArrayInline(sb, sanitizeWorldNames(enabledWorlds));
        sb.append(",\n");
        sb.append(INDENT).append(INDENT).append("\"globalComets\": ").append(globalComets).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"injectUseForCleanSlateBlocks\": ").append(injectUseForCleanSlateBlocks).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"disableWaveMobLoot\": ").append(disableWaveMobLoot).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"verboseWaveLogging\": ").append(verboseWaveLogging).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"debugLogging\": ").append(debugLogging).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"waveTimeoutSeconds\": ").append(waveTimeoutSeconds).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"waveSpawnMinRadius\": ").append(waveSpawnMinRadius).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"waveSpawnMaxRadius\": ").append(waveSpawnMaxRadius).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"combatMusicAmbienceId\": \"")
                .append(escapeString(combatMusicAmbienceId != null ? combatMusicAmbienceId : "")).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"cometLandSoundEventId\": \"")
                .append(escapeString(cometLandSoundEventId != null ? cometLandSoundEventId : "")).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"cometDestroySoundEventId\": \"")
                .append(escapeString(cometDestroySoundEventId != null ? cometDestroySoundEventId : "")).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"cometFallingNotifySoundEventId\": \"")
                .append(escapeString(cometFallingNotifySoundEventId != null ? cometFallingNotifySoundEventId : ""))
                .append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"waveSpeedRewardEnabled\": ").append(waveSpeedRewardEnabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"waveSpeedRewardMaxPercent\": ").append(waveSpeedRewardMaxPercent).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"waveSpeedRewardMinPercent\": ").append(waveSpeedRewardMinPercent).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"varyonZoneDelayScalingEnabled\": ").append(varyonZoneDelayScalingEnabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"zoneDelayReferenceZone\": ").append(zoneDelayReferenceZone).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"zoneDelayFastestZone\": ").append(zoneDelayFastestZone).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"zoneDelaySpeedMultiplierAtFastest\": ").append(zoneDelaySpeedMultiplierAtFastest).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"cometVaryonRingRewards\": ");
        appendVaryonRingRewardsMap(sb, varyonRingShardDrops, varyonRingMineralBonuses);
        sb.append("\n");
        sb.append(INDENT).append("},\n\n");

        writeProtectedZoneSpawnRules(sb, protectedZoneRulesEnabled, defaultInProtectedRegion, regionOverrides);

        writeClaimProtectSettings(sb, claimProtectEnabled, claimProtectAutoDetectProviders, claimProtectProviders);

        sb.append(INDENT).append("\"messages\": {\n");
        sb.append(INDENT).append(INDENT).append("\"msgCometFallingTitle\": \"")
                .append(escapeString(msgCometFallingTitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgCometFallingSubtitle\": \"")
                .append(escapeString(msgCometFallingSubtitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgCometFallingChatCoords\": \"")
                .append(escapeString(msgCometFallingChatCoords)).append("\",\n");

        sb.append(INDENT).append(INDENT).append("\"msgWaveBossTitle\": \"")
                .append(escapeString(msgWaveBossTitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveBossTitleNoCount\": \"")
                .append(escapeString(msgWaveBossTitleNoCount)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveBossSubtitle\": \"")
                .append(escapeString(msgWaveBossSubtitle)).append("\",\n");

        sb.append(INDENT).append(INDENT).append("\"msgWaveTitle\": \"")
                .append(escapeString(msgWaveTitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveTitleNoCount\": \"")
                .append(escapeString(msgWaveTitleNoCount)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveSubtitle\": \"")
                .append(escapeString(msgWaveSubtitle)).append("\",\n");

        sb.append(INDENT).append(INDENT).append("\"msgWaveFailedTitle\": \"")
                .append(escapeString(msgWaveFailedTitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveFailedSubtitle\": \"")
                .append(escapeString(msgWaveFailedSubtitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveFailedPlayerDeathSubtitle\": \"")
                .append(escapeString(msgWaveFailedPlayerDeathSubtitle != null ? msgWaveFailedPlayerDeathSubtitle : "")).append("\",\n");

        sb.append(INDENT).append(INDENT).append("\"msgWaveCompleteTitle\": \"")
                .append(escapeString(msgWaveCompleteTitle)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveCompleteSubtitle\": \"")
                .append(escapeString(msgWaveCompleteSubtitle)).append("\",\n");

        sb.append(INDENT).append(INDENT).append("\"msgWaveCompleteChatHeaderPrefix\": \"")
                .append(escapeString(msgWaveCompleteChatHeaderPrefix)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveCompleteChatHeader\": \"")
                .append(escapeString(msgWaveCompleteChatHeader)).append("\",\n");
        sb.append(INDENT).append(INDENT).append("\"msgWaveCompleteChatItemPrefix\": \"")
                .append(escapeString(msgWaveCompleteChatItemPrefix)).append("\"\n");
        sb.append(INDENT).append("},\n\n");

        writeZoneSpawnChances(sb, hytaleZoneSpawnChances);

        sb.append(INDENT).append("\"lootByTier\": {\n");
        int rewardCount = (lootByTier != null) ? lootByTier.size() : 0;
        int rewardIndex = 0;
        if (lootByTier != null) {
            for (Map.Entry<Integer, TierRewards> entry : lootByTier.entrySet()) {
                rewardIndex++;
                TierRewards tr = entry.getValue();
                sb.append(INDENT).append(INDENT).append("\"").append(entry.getKey()).append("\": {\n");

                sb.append(INDENT).append(INDENT).append(INDENT).append("\"drops\": [\n");
                List<RewardEntry> drops = tr.getDrops();
                for (int i = 0; i < drops.size(); i++) {
                    RewardEntry drop = drops.get(i);
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("{ \"id\": \"")
                            .append(drop.getId()).append("\", \"minCount\": ").append(drop.getMinCount())
                            .append(", \"maxCount\": ").append(drop.getMaxCount()).append(", \"chance\": ")
                            .append(drop.getChance()).append(", \"displayName\": \"")
                            .append(escapeString(drop.getDisplayName())).append("\" }");
                    if (i < drops.size() - 1)
                        sb.append(",");
                    sb.append("\n");
                }
                sb.append(INDENT).append(INDENT).append(INDENT).append("],\n");

                sb.append(INDENT).append(INDENT).append(INDENT).append("\"bonusDrops\": [\n");
                List<RewardEntry> bonusDrops = tr.getBonusDrops();
                for (int i = 0; i < bonusDrops.size(); i++) {
                    RewardEntry bonus = bonusDrops.get(i);
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("{ \"id\": \"")
                            .append(bonus.getId()).append("\", \"minCount\": ").append(bonus.getMinCount())
                            .append(", \"maxCount\": ").append(bonus.getMaxCount()).append(", \"chance\": ")
                            .append(bonus.getChance()).append(", \"displayName\": \"")
                            .append(escapeString(bonus.getDisplayName())).append("\" }");
                    if (i < bonusDrops.size() - 1)
                        sb.append(",");
                    sb.append("\n");
                }
                sb.append(INDENT).append(INDENT).append(INDENT).append("]\n");

                sb.append(INDENT).append(INDENT).append("}");
                if (rewardIndex < rewardCount) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        sb.append(INDENT).append("}\n");

        if (hasAnyThemePool(themePoolByTier)) {
            sb.append(",\n");
            appendThemePoolSection(sb, themePoolByTier);
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static boolean hasAnyThemePool(Map<Integer, Map<String, Integer>> themePoolByTier) {
        if (themePoolByTier == null || themePoolByTier.isEmpty()) {
            return false;
        }
        for (int t = 1; t <= 5; t++) {
            Map<String, Integer> m = themePoolByTier.get(t);
            if (m != null && !m.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void appendThemePoolSection(StringBuilder sb, Map<Integer, Map<String, Integer>> themePoolByTier) {
        List<Integer> tiers = new ArrayList<>();
        for (int t = 1; t <= 5; t++) {
            Map<String, Integer> m = themePoolByTier.get(t);
            if (m != null && !m.isEmpty()) {
                tiers.add(t);
            }
        }
        sb.append(INDENT).append("\"themePool\": {\n");
        for (int i = 0; i < tiers.size(); i++) {
            int t = tiers.get(i);
            Map<String, Integer> m = themePoolByTier.get(t);
            sb.append(INDENT).append(INDENT).append("\"").append(t).append("\": {\n");
            int ki = 0;
            int ks = m.size();
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                ki++;
                sb.append(INDENT).append(INDENT).append(INDENT).append("\"")
                        .append(escapeString(e.getKey())).append("\": ").append(e.getValue());
                if (ki < ks) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(INDENT).append(INDENT).append("}");
            if (i < tiers.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("}\n");
    }

    /**
     * Write a single comet mob entry for cometMobs.json (optional rewardOverride, waves).
     */
    private static void writeThemeMonsterGroupsOnly(StringBuilder sb, String id, ThemeConfig theme, boolean hasMore) {
        String i2 = INDENT + INDENT;
        String i3 = INDENT + INDENT + INDENT;
        String i4 = INDENT + INDENT + INDENT + INDENT;

        sb.append(i2).append("\"").append(id).append("\": {\n");
        sb.append(i3).append("\"displayName\": \"").append(escapeString(theme.getDisplayName())).append("\",\n");
        sb.append(i3).append("\"tiers\": [");
        List<Integer> tiers = theme.getTiers();
        for (int i = 0; i < tiers.size(); i++) {
            sb.append(tiers.get(i));
            if (i < tiers.size() - 1)
                sb.append(", ");
        }
        sb.append("],\n");
        sb.append(i3).append("\"naturalSpawn\": ").append(theme.isNaturalSpawn()).append(",\n");

        String i5 = i4 + INDENT;
        if (theme.hasMultiWave()) {
            appendWavesArray(sb, theme, i3, i4, i5);
        } else {
            sb.append(i3).append("\"mobs\": [\n");
            List<MobEntry> mobs = theme.getMobs();
            for (int i = 0; i < mobs.size(); i++) {
                MobEntry mob = mobs.get(i);
                sb.append(i4);
                appendMobEntryJson(sb, mob);
                if (i < mobs.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(i3).append("],\n");
            sb.append(i3).append("\"bosses\": [\n");
            List<BossEntry> bosses = theme.getBosses();
            for (int i = 0; i < bosses.size(); i++) {
                BossEntry boss = bosses.get(i);
                sb.append(i4);
                appendBossEntryJson(sb, boss);
                if (i < bosses.size() - 1)
                    sb.append(",");
                sb.append("\n");
            }
            sb.append(i3).append("]");
        }

        if (theme.hasRewardOverride()) {
            sb.append(",\n");
            sb.append(i3).append("\"rewardOverride\": {\n");
            Map<Integer, TierRewards> overrides = theme.getRewardOverride();
            int count = 0;
            for (Map.Entry<Integer, TierRewards> e : overrides.entrySet()) {
                count++;
                sb.append(i4).append("\"").append(e.getKey()).append("\": {\n");
                writeTierRewardsBody(sb, e.getValue(), 5);
                sb.append("\n").append(i4).append("}");
                if (count < overrides.size())
                    sb.append(",");
                sb.append("\n");
            }
            sb.append(i3).append("}");
        }

        sb.append("\n").append(i2).append("}");
        if (hasMore)
            sb.append(",");
        sb.append("\n");
    }

    private static void appendWavesArray(StringBuilder sb, ThemeConfig theme, String i3, String i4, String i5) {
        List<WaveEntry> waves = theme.getWaves();
        if (waves == null || waves.isEmpty()) {
            return;
        }
        String i6 = i5 + INDENT;
        sb.append(i3).append("\"waves\": [\n");
        for (int w = 0; w < waves.size(); w++) {
            WaveEntry we = waves.get(w);
            sb.append(i4).append("{\n");
            sb.append(i5).append("\"type\": \"").append(we.isBossWave() ? "boss" : "normal").append("\"");
            if (we.isNormalWave() && we.getMobs() != null && !we.getMobs().isEmpty()) {
                sb.append(",\n");
                sb.append(i5).append("\"mobs\": [\n");
                List<MobEntry> wm = we.getMobs();
                for (int i = 0; i < wm.size(); i++) {
                    sb.append(i6);
                    appendMobEntryJson(sb, wm.get(i));
                    if (i < wm.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append(i5).append("]");
            }
            if (we.isBossWave() && we.getBosses() != null && !we.getBosses().isEmpty()) {
                sb.append(",\n");
                sb.append(i5).append("\"bosses\": [\n");
                List<BossEntry> wb = we.getBosses();
                for (int i = 0; i < wb.size(); i++) {
                    sb.append(i6);
                    appendBossEntryJson(sb, wb.get(i));
                    if (i < wb.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append(i5).append("]");
                if (we.useRandomBossSelection()) {
                    sb.append(",\n").append(i5).append("\"randomBossSelection\": true");
                }
            }
            sb.append("\n").append(i4).append("}");
            if (w < waves.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(i3).append("]");
    }

    /**
     * Write zone spawn chances section to JSON
     */
    private static void writeZoneSpawnChances(StringBuilder sb, Map<String, ZoneSpawnChances> hytaleZoneSpawnChances) {
        if (hytaleZoneSpawnChances == null || hytaleZoneSpawnChances.isEmpty()) {
            hytaleZoneSpawnChances = ZoneSpawnChances.generateDefaults();
        }

        sb.append(INDENT).append("\"hytaleZoneSpawnChances\": {\n");
        int zoneCount = 0;
        for (Map.Entry<String, ZoneSpawnChances> entry : hytaleZoneSpawnChances.entrySet()) {
            zoneCount++;
            String zoneKey = entry.getKey();
            ZoneSpawnChances chances = entry.getValue();

            sb.append(INDENT).append(INDENT).append("\"").append(zoneKey).append("\": { ");
            sb.append("\"tier1\": ").append(chances.getTier1()).append(", ");
            sb.append("\"tier2\": ").append(chances.getTier2()).append(", ");
            sb.append("\"tier3\": ").append(chances.getTier3()).append(", ");
            sb.append("\"tier4\": ").append(chances.getTier4()).append(", ");
            sb.append("\"tier5\": ").append(chances.getTier5());
            sb.append(" }");

            if (zoneCount < hytaleZoneSpawnChances.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("},\n\n");
    }

    private static void writeTierRewardsBody(StringBuilder sb, TierRewards rewards, int indentLevel) {
        String ind = INDENT.repeat(indentLevel);
        String ind2 = INDENT.repeat(indentLevel + 1);
        List<RewardEntry> drops = (rewards != null) ? rewards.getDrops() : java.util.Collections.emptyList();
        List<RewardEntry> bonusDrops = (rewards != null) ? rewards.getBonusDrops() : java.util.Collections.emptyList();

        sb.append(ind).append("\"drops\": [\n");
        for (int i = 0; i < drops.size(); i++) {
            RewardEntry drop = drops.get(i);
            sb.append(ind2).append("{ \"id\": \"")
                    .append(drop.getId()).append("\", \"minCount\": ").append(drop.getMinCount())
                    .append(", \"maxCount\": ").append(drop.getMaxCount()).append(", \"chance\": ")
                    .append(drop.getChance()).append(", \"displayName\": \"")
                    .append(escapeString(drop.getDisplayName())).append("\" }");
            if (i < drops.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(ind).append("],\n");

        sb.append(ind).append("\"bonusDrops\": [\n");
        for (int i = 0; i < bonusDrops.size(); i++) {
            RewardEntry bonus = bonusDrops.get(i);
            sb.append(ind2).append("{ \"id\": \"")
                    .append(bonus.getId()).append("\", \"minCount\": ").append(bonus.getMinCount())
                    .append(", \"maxCount\": ").append(bonus.getMaxCount()).append(", \"chance\": ")
                    .append(bonus.getChance()).append(", \"displayName\": \"")
                    .append(escapeString(bonus.getDisplayName())).append("\" }");
            if (i < bonusDrops.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(ind).append("]");
    }

    /**
     * Write optional WorldProtect rules for comet spawning inside protected regions.
     */
    private static void writeProtectedZoneSpawnRules(StringBuilder sb, boolean enabled, boolean defaultInProtectedRegion,
            Map<String, Boolean> regionOverrides) {
        sb.append(INDENT).append("\"worldProtectSpawnRules\": {\n");
        sb.append(INDENT).append(INDENT).append("\"enabled\": ").append(enabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"defaultInWorldProtectRegion\": ")
                .append(defaultInProtectedRegion).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"regionOverrides\": {\n");

        int count = 0;
        int size = (regionOverrides == null) ? 0 : regionOverrides.size();
        if (regionOverrides != null) {
            for (Map.Entry<String, Boolean> entry : regionOverrides.entrySet()) {
                count++;
                sb.append(INDENT).append(INDENT).append(INDENT)
                        .append("\"").append(escapeString(entry.getKey())).append("\": ")
                        .append(entry.getValue());
                if (count < size) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }

        sb.append(INDENT).append(INDENT).append("}\n");
        sb.append(INDENT).append("},\n\n");
    }

    private static void writeClaimProtectSettings(StringBuilder sb, boolean enabled, boolean autoDetectProviders,
            List<String> providers) {
        sb.append(INDENT).append("\"claimProtect\": {\n");
        sb.append(INDENT).append(INDENT).append("\"enabled\": ").append(enabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"autoDetectProviders\": ").append(autoDetectProviders).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"providers\": ");
        appendStringArrayInline(sb, sanitizeClaimProviderNames(providers));
        sb.append("\n");
        sb.append(INDENT).append("},\n\n");
    }

    private static List<String> sanitizeWorldNames(List<String> worldNames) {
        if (worldNames == null || worldNames.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> sanitized = new java.util.ArrayList<>();
        for (String worldName : worldNames) {
            if (worldName == null) {
                continue;
            }
            String trimmed = worldName.trim();
            if (trimmed.isEmpty() || sanitized.contains(trimmed)) {
                continue;
            }
            sanitized.add(trimmed);
        }
        return sanitized;
    }

    private static List<String> sanitizeClaimProviderNames(List<String> providerNames) {
        if (providerNames == null || providerNames.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> sanitized = new java.util.ArrayList<>();
        for (String providerName : providerNames) {
            if (providerName == null) {
                continue;
            }
            String trimmed = providerName.trim();
            if (trimmed.isEmpty() || sanitized.contains(trimmed)) {
                continue;
            }
            sanitized.add(trimmed);
        }
        return sanitized;
    }

    private static void appendStringArrayInline(StringBuilder sb, List<String> values) {
        sb.append("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append("\"").append(escapeString(values.get(i))).append("\"");
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
    }

    private static void appendVaryonRingRewardsMap(
            StringBuilder sb,
            Map<String, ShardDropRange> rings,
            Map<String, VaryonMineralBonus> minerals) {
        sb.append("{\n");
        boolean first = true;
        for (int i = 1; i <= 10; i++) {
            String key = String.valueOf(i);
            ShardDropRange r = rings != null ? rings.get(key) : null;
            VaryonMineralBonus b = minerals != null ? minerals.get(key) : null;
            if (r == null) {
                continue;
            }
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append(INDENT).append(INDENT).append(INDENT).append("\"").append(key).append("\": { ");
            sb.append("\"min\": ").append(r.min).append(", \"max\": ").append(r.max);
            if (b != null) {
                sb.append(", \"mineralBonus\": { \"itemId\": \"").append(escapeString(b.itemId()))
                        .append("\", \"chance\": ").append(b.chance()).append(" }");
            }
            sb.append(" }");
        }
        sb.append("\n").append(INDENT).append(INDENT).append("}");
    }

    private static void appendBossEntryJson(StringBuilder sb, BossEntry boss) {
        sb.append("{ \"id\": \"").append(escapeString(boss.getId())).append("\"");
        Integer bmin = boss.getExplicitMinCount();
        Integer bmax = boss.getExplicitMaxCount();
        if (bmin != null && bmax != null) {
            sb.append(", \"minCount\": ").append(bmin).append(", \"maxCount\": ").append(bmax);
        }
        sb.append(" }");
    }

    private static void appendMobEntryJson(StringBuilder sb, MobEntry mob) {
        sb.append("{ \"id\": \"").append(escapeString(mob.getId())).append("\"");
        if (mob.getTierCounts() != null && !mob.getTierCounts().isEmpty()) {
            sb.append(", \"count\": ");
            writeTierIntMapInline(sb, mob.getTierCounts());
            sb.append(", \"minCount\": ");
            if (mob.getTierMinCounts() != null && !mob.getTierMinCounts().isEmpty()
                    && mob.getTierMaxCounts() != null && !mob.getTierMaxCounts().isEmpty()) {
                writeTierIntMapInline(sb, mob.getTierMinCounts());
                sb.append(", \"maxCount\": ");
                writeTierIntMapInline(sb, mob.getTierMaxCounts());
            } else {
                writeDerivedTierMinFromCounts(sb, mob.getTierCounts());
                sb.append(", \"maxCount\": ");
                writeDerivedTierMaxFromCounts(sb, mob.getTierCounts());
            }
        } else {
            sb.append(", \"minCount\": ").append(mob.getMinCountForTier(1));
            sb.append(", \"maxCount\": ").append(mob.getMaxCountForTier(1));
        }
        sb.append(" }");
    }

    private static void writeTierIntMapInline(StringBuilder sb, Map<Integer, Integer> map) {
        sb.append("{ ");
        if (map == null || map.isEmpty()) {
            sb.append("}");
            return;
        }
        int i = 0;
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append(" }");
    }

    private static void writeDerivedTierMinFromCounts(StringBuilder sb, Map<Integer, Integer> tierCounts) {
        sb.append("{ ");
        int i = 0;
        for (Map.Entry<Integer, Integer> e : tierCounts.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            int c = e.getValue();
            sb.append("\"").append(e.getKey()).append("\": ").append(Math.max(1, c - 1));
        }
        sb.append(" }");
    }

    private static void writeDerivedTierMaxFromCounts(StringBuilder sb, Map<Integer, Integer> tierCounts) {
        sb.append("{ ");
        int i = 0;
        for (Map.Entry<Integer, Integer> e : tierCounts.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            int c = e.getValue();
            sb.append("\"").append(e.getKey()).append("\": ").append(c + 1);
        }
        sb.append(" }");
    }

    /**
     * Escape special characters in JSON strings
     */
    private static String escapeString(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
