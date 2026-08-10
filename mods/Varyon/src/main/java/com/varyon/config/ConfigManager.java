package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;
import com.varyon.announce.ChatAnnouncementScheduler;
import com.varyon.safezone.SafeZoneConfig;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


public class ConfigManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_FILENAME = "config.toml";

    private final Path configPath;
    private final Path pluginDataFolder;
    private ZoneConfig zoneConfig;
    private SafeZoneConfig safeZoneConfig;
    private ExtractionConfig extractionConfig;
    private FactionRewardsConfig factionRewardsConfig;
    private ReturnConfig returnConfig;
    private MessagesConfig messagesConfig;
    private ZoneLootConfig zoneLootConfig;
    private MobFragmentsConfig mobFragmentsConfig;
    private PointsEconomyConfig pointsEconomyConfig;
    private ZonePermissionsConfig zonePermissionsConfig;
    private RtphConfig rtphConfig;
    private RtpvConfig rtpvConfig;
    private RtpsConfig rtpsConfig;
    private DeathConfig deathConfig;
    private ShopConfig shopConfig;
    private ChatAnnouncementsConfig chatAnnouncementsConfig;

    public ConfigManager(@Nonnull Path pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        this.configPath = pluginDataFolder.resolve(CONFIG_FILENAME);
    }

    public void load() {
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            LOGGER.at(Level.INFO).log("Configuration file not found, creating default config at: %s", configPath);
            zoneConfig = ZoneConfig.createDefault();
            safeZoneConfig = new SafeZoneConfig();
            extractionConfig = new ExtractionConfig();
            ExtractionConfig.applyDefaultZoneRangesForNewConfig(extractionConfig);
            factionRewardsConfig = FactionRewardsConfig.createDefault();
            factionRewardsConfig.save(pluginDataFolder);
            returnConfig = ReturnConfig.createDefault();
            messagesConfig = MessagesConfig.createDefault();
            messagesConfig.save(pluginDataFolder);
            zoneLootConfig = ZoneLootConfig.createDefault();
            zoneLootConfig.save(pluginDataFolder);
            mobFragmentsConfig = MobFragmentsConfig.load(pluginDataFolder);
            pointsEconomyConfig = PointsEconomyConfig.createDefault();
            zonePermissionsConfig = ZonePermissionsConfig.createDefault();
            zonePermissionsConfig.save(pluginDataFolder);
            rtphConfig = RtphConfig.createDefault();
            rtpvConfig = RtpvConfig.createDefault();
            rtpsConfig = RtpsConfig.createDefault();
            deathConfig = DeathConfig.createDefault();
            shopConfig = ShopConfig.createDefault();
            shopConfig.save(pluginDataFolder);
            chatAnnouncementsConfig = ChatAnnouncementsConfig.createDefault();
            chatAnnouncementsConfig.save(pluginDataFolder);
            save();
            ChatAnnouncementScheduler.onConfigReloaded();
            return;
        }

        try {
            Toml toml = new Toml().read(configFile);
            zoneConfig = parseZoneConfig(toml);
            safeZoneConfig = parseSafeZoneConfig(toml);
            extractionConfig = parseExtractionConfig(toml);
            factionRewardsConfig = FactionRewardsConfig.load(pluginDataFolder);
            returnConfig = parseReturnConfig(toml);
            messagesConfig = MessagesConfig.load(pluginDataFolder);
            zoneLootConfig = ZoneLootConfig.load(pluginDataFolder);
            mobFragmentsConfig = MobFragmentsConfig.load(pluginDataFolder);
            pointsEconomyConfig = PointsEconomyConfig.parse(toml);
            zonePermissionsConfig = ZonePermissionsConfig.load(pluginDataFolder);
            rtphConfig = parseRtphConfig(toml);
            rtpvConfig = parseRtpvConfig(toml);
            rtpsConfig = parseRtpsConfig(toml);
            deathConfig = parseDeathConfig(toml);
            shopConfig = ShopConfig.load(pluginDataFolder);
            chatAnnouncementsConfig = ChatAnnouncementsConfig.load(pluginDataFolder);
            LOGGER.at(Level.INFO).log("Loaded configuration with %s zones", zoneConfig.getZones().size());
            save();
            ChatAnnouncementScheduler.onConfigReloaded();
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load config, using default configuration", e);
            zoneConfig = ZoneConfig.createDefault();
            safeZoneConfig = new SafeZoneConfig();
            extractionConfig = new ExtractionConfig();
            factionRewardsConfig = FactionRewardsConfig.createDefault();
            returnConfig = ReturnConfig.createDefault();
            messagesConfig = MessagesConfig.createDefault();
            zoneLootConfig = ZoneLootConfig.createDefault();
            mobFragmentsConfig = MobFragmentsConfig.load(pluginDataFolder);
            pointsEconomyConfig = PointsEconomyConfig.createDefault();
            zonePermissionsConfig = ZonePermissionsConfig.createDefault();
            rtphConfig = RtphConfig.createDefault();
            rtpvConfig = RtpvConfig.createDefault();
            rtpsConfig = RtpsConfig.createDefault();
            deathConfig = DeathConfig.createDefault();
            shopConfig = ShopConfig.createDefault();
            chatAnnouncementsConfig = ChatAnnouncementsConfig.createDefault();
            ChatAnnouncementScheduler.onConfigReloaded();
        }
    }

    public void save() {
        try {
            File configFile = configPath.toFile();
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(generateToml());
                LOGGER.at(Level.INFO).log("Configuration saved to: %s", configPath);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save configuration", e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();

        sb.append("enabledWorlds = [");
        List<String> worlds = zoneConfig.getEnabledWorlds();
        for (int i = 0; i < worlds.size(); i++) {
            sb.append("\"").append(worlds.get(i)).append("\"");
            if (i < worlds.size() - 1) sb.append(", ");
        }
        sb.append("]\n\n");

        sb.append("# Instance worlds: pattern (prefix) = zone id. World name must start with pattern.\n");
        sb.append("[instanceZones]\n");
        for (Map.Entry<String, Integer> e : zoneConfig.getInstanceZonePatterns().entrySet()) {
            String key = e.getKey().replace("\"", "").trim();
            sb.append(key).append(" = ").append(e.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("[safezone]\n");
        sb.append("enabled = ").append(safeZoneConfig.isEnabled()).append("\n");
        sb.append("minRotationTimeMinutes = ").append(safeZoneConfig.getMinRotationTimeMinutes()).append("\n");
        sb.append("maxRotationTimeMinutes = ").append(safeZoneConfig.getMaxRotationTimeMinutes()).append("\n");
        sb.append("overlapDurationMinutes = ").append(safeZoneConfig.getOverlapDurationMinutes()).append("\n");
        sb.append("maxRadius = ").append(safeZoneConfig.getMaxRadius()).append("\n");
        sb.append("spawnRadius = ").append(safeZoneConfig.getSpawnRadius()).append("\n\n");

        sb.append("[minimap]\n");
        sb.append("enabled = ").append(zoneConfig.isMinimapEnabled()).append("\n");
        sb.append("opacity = ").append(zoneConfig.getMinimapOpacity()).append("\n");
        sb.append("pattern = \"").append(zoneConfig.getMinimapPattern()).append("\"\n");
        sb.append("patternSize = ").append(zoneConfig.getMinimapPatternSize()).append("\n\n");

        sb.append("[notifications]\n");
        sb.append("zoneEnterEnabled = ").append(zoneConfig.isZoneEnterNotification()).append("\n");
        sb.append("zoneEnterTopText = \"").append(zoneConfig.getZoneEnterTopText()).append("\"\n");
        sb.append("duration = ").append(zoneConfig.getNotificationDuration()).append("\n");
        sb.append("soundEnabled = ").append(zoneConfig.isZoneSoundEnabled()).append("\n");
        sb.append("soundId = \"").append(zoneConfig.getZoneSoundId()).append("\"\n");
        sb.append("soundVolume = ").append(zoneConfig.getZoneSoundVolume()).append("\n");
        sb.append("soundPitch = ").append(zoneConfig.getZoneSoundPitch()).append("\n");
        sb.append("hudEnabled = ").append(zoneConfig.isZoneHudEnabled()).append("\n\n");

        sb.append("# extraction: min/max = fallback if zoneRanges empty or player zone unknown (zone id <= 0).\n");
        sb.append("# zoneRanges = anchors (zoneId ascending); linear blend between consecutive anchors.\n");
        sb.append("[extraction]\n");
        sb.append("enabled = ").append(extractionConfig.isEnabled()).append("\n");
        sb.append("minDistance = ").append(extractionConfig.getMinDistance()).append("\n");
        sb.append("maxDistance = ").append(extractionConfig.getMaxDistance()).append("\n");
        sb.append("portalDurationSeconds = ").append(extractionConfig.getPortalDurationSeconds()).append("\n");
        sb.append("cooldownSeconds = ").append(extractionConfig.getCooldownSeconds()).append("\n");
        for (ExtractionZoneDistance zr : extractionConfig.getZoneRanges()) {
            sb.append("[[extraction.zoneRanges]]\n");
            sb.append("zoneId = ").append(zr.zoneId()).append("\n");
            sb.append("minDistance = ").append(zr.minDistance()).append("\n");
            sb.append("maxDistance = ").append(zr.maxDistance()).append("\n");
        }
        sb.append("\n");

        sb.append("[return]\n");
        sb.append("enabled = ").append(returnConfig != null ? returnConfig.isEnabled() : true).append("\n");
        sb.append("cooldownSeconds = ").append(returnConfig != null ? returnConfig.getCooldownSeconds() : 1800).append("\n");
        sb.append("minDistance = ").append(returnConfig != null ? returnConfig.getMinDistance() : 100).append("\n");
        sb.append("maxDistance = ").append(returnConfig != null ? returnConfig.getMaxDistance() : 200).append("\n");
        sb.append("expirationMinutes = ").append(returnConfig != null ? returnConfig.getExpirationMinutes() : 30).append("\n\n");

        RtphConfig rtph = rtphConfig != null ? rtphConfig : RtphConfig.createDefault();
        sb.append("[rtph]\n");
        sb.append("outerMax = ").append(rtph.getOuterMax()).append("\n");
        sb.append("innerMax = ").append(rtph.getInnerMax()).append("\n\n");

        RtpvConfig rtpv = rtpvConfig != null ? rtpvConfig : RtpvConfig.createDefault();
        sb.append("[rtpv]\n");
        sb.append("economyEnabled = ").append(rtpv.isEconomyEnabled()).append("\n");
        sb.append("safeCostMultiplier = ").append(rtpv.getSafeCostMultiplier()).append("\n");
        sb.append("joinDurationSeconds = ").append(rtpv.getJoinDurationSeconds()).append("\n");
        sb.append("cooldownSeconds = ").append(rtpv.getCooldownSeconds()).append("\n");
        sb.append("\n");

        RtpsConfig rtps = rtpsConfig != null ? rtpsConfig : RtpsConfig.createDefault();
        sb.append("[rtps]\n");
        sb.append("minBlocks = ").append(rtps.getMinBlocks()).append("\n");
        sb.append("maxBlocks = ").append(rtps.getMaxBlocks()).append("\n");
        sb.append("\n");

        DeathConfig death = deathConfig != null ? deathConfig : DeathConfig.createDefault();
        sb.append("[death]\n");
        sb.append("pointsLossPercent = ").append(death.getPointsLossPercent()).append("\n");
        sb.append("\n");

        PointsEconomyConfig ee = pointsEconomyConfig != null ? pointsEconomyConfig : PointsEconomyConfig.createDefault();
        sb.append("[points_economy]\n");
        sb.append("pvpPointsMultiplier = ").append(ee.getPvpPointsMultiplier()).append("\n");
        sb.append("defaultMobReward = ").append(ee.getDefaultMobReward()).append("\n");
        sb.append("defaultOreReward = ").append(ee.getDefaultOreReward()).append("\n\n");

        for (DifficultyZone zone : zoneConfig.getZones()) {
            sb.append("[[zones]]\n");
            sb.append("id = ").append(zone.getZoneId()).append("\n");
            sb.append("name = \"").append(zone.getName()).append("\"\n");
            sb.append("color = \"").append(zone.getColor()).append("\"\n");
            sb.append("healthMultiplier = ").append(zone.getHealthMultiplier()).append("\n");
            sb.append("damageMultiplier = ").append(zone.getDamageMultiplier()).append("\n");
            sb.append("lootMultiplier = ").append(zone.getLootMultiplier()).append("\n");
            sb.append("radiusStart = ").append(zone.getRadiusStart()).append("\n");
            sb.append("teleportCost = ").append(zone.getTeleportCost()).append("\n\n");
        }

        return sb.toString();
    }

    @Nonnull
    private ZoneConfig parseZoneConfig(@Nonnull Toml toml) {
        List<String> enabledWorlds = new ArrayList<>();
        List<?> rawList = toml.getList("enabledWorlds");
        if (rawList != null) {
            for (Object o : rawList) {
                if (o != null) {
                    String s = o.toString().trim();
                    if (!s.isEmpty()) {
                        enabledWorlds.add(s);
                    }
                }
            }
        }

        Toml minimapToml = toml.getTable("minimap");
        boolean minimapEnabled = true;
        int minimapOpacity = 50;
        String minimapPattern = "SOLID";
        int minimapPatternSize = 4;
        if (minimapToml != null) {
            minimapEnabled = minimapToml.getBoolean("enabled", true);
            minimapOpacity = minimapToml.getLong("opacity", 50L).intValue();
            minimapPattern = minimapToml.getString("pattern", "SOLID");
            minimapPatternSize = minimapToml.getLong("patternSize", 4L).intValue();
        }

        Toml notifToml = toml.getTable("notifications");
        boolean zoneEnterNotification = true;
        String zoneEnterTopText = "Zone";
        float notificationDuration = 2.0f;
        boolean zoneSoundEnabled = true;
        String zoneSoundId = "SFX_Axe_Special_Swing";
        float zoneSoundVolume = 1.0f;
        float zoneSoundPitch = 1.0f;
        boolean zoneHudEnabled = true;
        if (notifToml != null) {
            zoneEnterNotification = notifToml.getBoolean("zoneEnterEnabled", true);
            zoneEnterTopText = notifToml.getString("zoneEnterTopText", "Zone");
            notificationDuration = notifToml.getDouble("duration", 2.0).floatValue();
            zoneSoundEnabled = notifToml.getBoolean("soundEnabled", true);
            zoneSoundId = notifToml.getString("soundId", "SFX_Axe_Special_Swing");
            zoneSoundVolume = notifToml.getDouble("soundVolume", 1.0).floatValue();
            zoneSoundPitch = notifToml.getDouble("soundPitch", 1.0).floatValue();
            zoneHudEnabled = notifToml.getBoolean("hudEnabled", true);
        }

        List<DifficultyZone> zones = new ArrayList<>();
        List<Toml> zonesList = toml.getTables("zones");
        if (zonesList != null) {
            for (Toml zoneToml : zonesList) {
                int id = zoneToml.getLong("id", 1L).intValue();
                String color = zoneToml.getString("color", "WHITE");
                double healthMultiplier = zoneToml.getDouble("healthMultiplier", 1.0);
                double damageMultiplier = zoneToml.getDouble("damageMultiplier", 1.0);
                double lootMultiplier = zoneToml.getDouble("lootMultiplier", 1.0);
                double pointsMultiplier = zoneToml.getDouble("pointsMultiplier", zoneToml.getDouble("essenceMultiplier", 1.0));
                int radiusStart = zoneToml.getLong("radiusStart", 0L).intValue();
                String name = zoneToml.getString("name", "Zone " + id);
                int teleportCost = zoneToml.getLong("teleportCost", (long)(id * 50)).intValue();
                zones.add(new DifficultyZone(id, color, healthMultiplier, damageMultiplier,
                        lootMultiplier, pointsMultiplier, radiusStart, name, teleportCost));
            }
        }

        Map<String, Integer> instanceZonePatterns = new LinkedHashMap<>();
        Toml instanceZonesToml = toml.getTable("instanceZones");
        if (instanceZonesToml != null) {
            for (Map.Entry<String, Object> e : instanceZonesToml.toMap().entrySet()) {
                if (e.getValue() instanceof Number n) {
                    instanceZonePatterns.put(e.getKey(), n.intValue());
                }
            }
        }
        if (instanceZonePatterns.isEmpty()) {
            instanceZonePatterns.put("instance-portals_henges", 1);
            instanceZonePatterns.put("instance-portals_oasis", 1);
            instanceZonePatterns.put("instance-portals_hedera", 2);
            instanceZonePatterns.put("instance-portals_jungles", 2);
            instanceZonePatterns.put("instance-portals_taiga", 2);
            instanceZonePatterns.put("instance-endgame_frozen_dungeon", 4);
            instanceZonePatterns.put("instance-endgame_swamp_dungeon", 6);
            instanceZonePatterns.put("instance-endgame_golem_void", 8);
        }

        return new ZoneConfig(enabledWorlds, zones, instanceZonePatterns, minimapEnabled, minimapOpacity,
                minimapPattern, minimapPatternSize, zoneEnterNotification,
                zoneEnterTopText, notificationDuration, zoneSoundEnabled,
                zoneSoundId, zoneSoundVolume, zoneSoundPitch, zoneHudEnabled);
    }

    @Nonnull
    private SafeZoneConfig parseSafeZoneConfig(@Nonnull Toml toml) {
        SafeZoneConfig config = new SafeZoneConfig();
        Toml safeToml = toml.getTable("safezone");
        if (safeToml != null) {
            config.setEnabled(safeToml.getBoolean("enabled", true));
            config.setMinRotationTimeMinutes(safeToml.getLong("minRotationTimeMinutes", 60L).intValue());
            config.setMaxRotationTimeMinutes(safeToml.getLong("maxRotationTimeMinutes", 120L).intValue());
            config.setOverlapDurationMinutes(safeToml.getLong("overlapDurationMinutes", 10L).intValue());
            config.setMaxRadius(safeToml.getLong("maxRadius", -1L).intValue());
            config.setSpawnRadius(safeToml.getLong("spawnRadius", 100L).intValue());
        }
        return config;
    }

    @Nonnull
    private ExtractionConfig parseExtractionConfig(@Nonnull Toml toml) {
        ExtractionConfig config = new ExtractionConfig();
        Toml extractionToml = toml.getTable("extraction");
        if (extractionToml != null) {
            config.setEnabled(extractionToml.getBoolean("enabled", true));
            config.setMinDistance(extractionToml.getLong("minDistance", 100L).intValue());
            config.setMaxDistance(extractionToml.getLong("maxDistance", 200L).intValue());
            config.setPortalDurationSeconds(extractionToml.getLong("portalDurationSeconds", 60L).intValue());
            config.setCooldownSeconds(extractionToml.getLong("cooldownSeconds", 60L).intValue());
            List<Toml> zoneRangeTables = extractionToml.getTables("zoneRanges");
            if (zoneRangeTables != null && !zoneRangeTables.isEmpty()) {
                List<ExtractionZoneDistance> ranges = new ArrayList<>();
                for (Toml z : zoneRangeTables) {
                    int zoneId = z.getLong("zoneId", 0L).intValue();
                    int minD = z.getLong("minDistance", 100L).intValue();
                    int maxD = z.getLong("maxDistance", 200L).intValue();
                    if (zoneId > 0) {
                        if (minD > maxD) {
                            int t = minD;
                            minD = maxD;
                            maxD = t;
                        }
                        ranges.add(new ExtractionZoneDistance(zoneId, minD, maxD));
                    }
                }
                config.setZoneRanges(ranges);
            }
        }
        return config;
    }

    @Nonnull
    private RtphConfig parseRtphConfig(@Nonnull Toml toml) {
        Toml rtphToml = toml.getTable("rtph");
        if (rtphToml == null) return RtphConfig.createDefault();
        return new RtphConfig(
            rtphToml.getLong("outerMax", 15000L).intValue(),
            rtphToml.getLong("innerMax", 10000L).intValue()
        );
    }

    @Nonnull
    private RtpsConfig parseRtpsConfig(@Nonnull Toml toml) {
        Toml rtpsToml = toml.getTable("rtps");
        if (rtpsToml == null) return RtpsConfig.createDefault();
        return new RtpsConfig(
            rtpsToml.getLong("minBlocks", 10000L).intValue(),
            rtpsToml.getLong("maxBlocks", 15000L).intValue()
        );
    }

    @Nonnull
    private RtpvConfig parseRtpvConfig(@Nonnull Toml toml) {
        Toml rtpvToml = toml.getTable("rtpv");
        if (rtpvToml == null) return RtpvConfig.createDefault();
        boolean economyEnabled = rtpvToml.getBoolean("economyEnabled", true);
        double safeCostMultiplier = rtpvToml.getDouble("safeCostMultiplier", 2.0);
        int joinDurationSeconds = rtpvToml.getLong("joinDurationSeconds", 60L).intValue();
        int cooldownSeconds = rtpvToml.getLong("cooldownSeconds", 60L).intValue();
        return new RtpvConfig(safeCostMultiplier, economyEnabled, joinDurationSeconds, cooldownSeconds);
    }

    @Nonnull
    private DeathConfig parseDeathConfig(@Nonnull Toml toml) {
        Toml deathToml = toml.getTable("death");
        if (deathToml == null) return DeathConfig.createDefault();
        return new DeathConfig(deathToml.getDouble("pointsLossPercent", deathToml.getDouble("essenceLossPercent", 80.0)));
    }

    @Nonnull
    private ReturnConfig parseReturnConfig(@Nonnull Toml toml) {
        Toml returnToml = toml.getTable("return");
        if (returnToml == null) return ReturnConfig.createDefault();
        return new ReturnConfig(
            returnToml.getBoolean("enabled", true),
            returnToml.getLong("cooldownSeconds", 1800L).intValue(),
            returnToml.getLong("minDistance", 100L).intValue(),
            returnToml.getLong("maxDistance", 200L).intValue(),
            returnToml.getLong("expirationMinutes", 30L).intValue()
        );
    }

    @Nonnull public ZoneConfig getZoneConfig()                    { return zoneConfig; }
    @Nonnull public SafeZoneConfig getSafeZoneConfig()            { return safeZoneConfig; }
    @Nonnull public ExtractionConfig getExtractionConfig()        { return extractionConfig; }
    @Nonnull public FactionRewardsConfig getFactionRewardsConfig(){ return factionRewardsConfig != null ? factionRewardsConfig : FactionRewardsConfig.createDefault(); }
    @Nonnull public ReturnConfig getReturnConfig()                { return returnConfig != null ? returnConfig : ReturnConfig.createDefault(); }
    @Nonnull public MessagesConfig getMessagesConfig()            { return messagesConfig != null ? messagesConfig : MessagesConfig.createDefault(); }
    @Nonnull public ZoneLootConfig getZoneLootConfig()            { return zoneLootConfig != null ? zoneLootConfig : ZoneLootConfig.createDefault(); }
    @Nonnull public MobFragmentsConfig getMobFragmentsConfig()       { return mobFragmentsConfig != null ? mobFragmentsConfig : MobFragmentsConfig.load(pluginDataFolder); }
    @Nonnull public PointsEconomyConfig getPointsEconomyConfig()    { return pointsEconomyConfig != null ? pointsEconomyConfig : PointsEconomyConfig.createDefault(); }
    @Nonnull public ZonePermissionsConfig getZonePermissionsConfig() { return zonePermissionsConfig != null ? zonePermissionsConfig : ZonePermissionsConfig.createDefault(); }
    @Nonnull public RtphConfig getRtphConfig()                       { return rtphConfig != null ? rtphConfig : RtphConfig.createDefault(); }
    @Nonnull public RtpvConfig getRtpvConfig()                       { return rtpvConfig != null ? rtpvConfig : RtpvConfig.createDefault(); }
    @Nonnull public RtpsConfig getRtpsConfig()                       { return rtpsConfig != null ? rtpsConfig : RtpsConfig.createDefault(); }
    @Nonnull public DeathConfig getDeathConfig()                     { return deathConfig != null ? deathConfig : DeathConfig.createDefault(); }
    @Nonnull public ShopConfig getShopConfig()                       { return shopConfig != null ? shopConfig : ShopConfig.createDefault(); }
    @Nonnull public ChatAnnouncementsConfig getChatAnnouncementsConfig() {
        return chatAnnouncementsConfig != null ? chatAnnouncementsConfig : ChatAnnouncementsConfig.createDefault();
    }

    public void reload() { load(); }
}
