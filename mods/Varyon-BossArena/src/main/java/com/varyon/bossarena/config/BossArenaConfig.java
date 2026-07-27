package com.varyon.bossarena.config;

import com.varyon.bossarena.util.NotificationRadiusConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class BossArenaConfig {
    public static final String DEFAULT_TIMED_ANNOUNCEMENT_TEXT = "[$World] $Boss event started at $Arena";
    public static final String DEFAULT_TIMED_REMINDER_TEXT =
            "&e[$World] &6$Boss &eapparaît dans 5 minutes (&b$Arena&e)!";
    public static final String DEFAULT_TIMED_GRACE_TITLE_TEXT =
            "&eLe combat de boss commence dans &6$time&e !";
    public static final String DEFAULT_TIMED_MAP_MARKER_IMAGE = "map_marker.png";
    public static final String DEFAULT_TIMED_MAP_MARKER_NAME_TEMPLATE = "$Arena";
    public static final String DEFAULT_EVENT_ACTIVE_TITLE_TEMPLATE = "$PhaseTitle";
    public static final String DEFAULT_EVENT_ACTIVE_SUBTITLE_TEMPLATE = "Monstres restants : $MonstersAlive";
    public static final String DEFAULT_EVENT_VICTORY_TITLE_TEMPLATE = "VICTOIRE ! Réclamez votre butin !";
    public static final String DEFAULT_EVENT_VICTORY_SUBTITLE_TEMPLATE = "Dégâts infligés : $DamageDealt";
    private static final String LEGACY_EVENT_ACTIVE_TITLE_TEMPLATE = "The Shadows Stir—$BossUpper Approaches!";
    private static final String LEGACY_EVENT_ACTIVE_TITLE_TEMPLATE_COLON = "The Shadows Stir: $BossUpper Approaches!";
    private static final String LEGACY_EVENT_ACTIVE_TITLE_TEMPLATE_FR =
            "Les ombres s'agitent : $BossUpper approche !";
    private static final String LEGACY_EVENT_ACTIVE_SUBTITLE_TEMPLATE =
            "$ContextLine$CountdownLineBoss alive: $BossAlive | Wave mobs alive: $AddsAlive";
    private static final String LEGACY_EVENT_ACTIVE_SUBTITLE_TEMPLATE_CAPS =
            "$ContextLine$CountdownLineALIVE: $BossAlive | WAVE MOBS ALIVE: $AddsAlive";
    private static final String LEGACY_EVENT_ACTIVE_SUBTITLE_TEMPLATE_FR =
            "$ContextLine$CountdownLineBoss en vie : $BossAlive | Mobs de vague en vie : $AddsAlive";
    private static final String LEGACY_EVENT_VICTORY_TITLE_TEMPLATE = "VICTORY! Claim your spoils!";
    private static final String LEGACY_EVENT_VICTORY_SUBTITLE_TEMPLATE = "Boss alive: 0 | Wave mobs alive: 0";
    private static final String LEGACY_EVENT_VICTORY_SUBTITLE_TEMPLATE_CAPS = "ALIVE: 0 | WAVE MOBS ALIVE: 0";
    private static final String LEGACY_EVENT_VICTORY_SUBTITLE_TEMPLATE_FR =
            "Boss en vie : 0 | Mobs de vague en vie : 0";
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final String DEFAULT_CURRENCY_ITEM_ID = "Coin";
    private static final String DEFAULT_FALLBACK_CURRENCY_ITEM_ID = "Ingredient_Bar_Iron";
    private static final int MIN_COUNTDOWN_MINUTES = 1;
    private static final Path CONFIG_PATH = Path.of("mods", "Varyon-BossArena", "config.json");
    private static final Path LEGACY_CONFIG_PATH = Path.of("mods", "BossArena", "config.json");

    /** Distance (blocks) within which players see boss event title/subtitle. */
    public double notificationRadius = NotificationRadiusConstants.DEFAULT;
    public String currencyItemId = DEFAULT_CURRENCY_ITEM_ID;
    public String fallbackCurrencyItemId = DEFAULT_FALLBACK_CURRENCY_ITEM_ID;
    public Map<String, Integer> bossTierCountdownMinutes = createDefaultBossTierCountdownMinutes();
    public EventBannerTemplates eventBanner = createDefaultEventBannerTemplates();
    public TimedMapMarkerSettings timedMapMarker = createDefaultTimedMapMarkerSettings();
    public List<TimedBossSpawn> timedBossSpawns = new ArrayList<>();
    // JSON does not support real comments. This is a docs-only block.
    public PlaceholderDocs _comment_placeholders = createDefaultPlaceholderDocs();

    public static long resolveMinutes(long hours, long minutes) {
        long safeHours = Math.max(0L, hours);
        long safeMinutes = Math.max(0L, minutes);
        return (safeHours * 60L) + safeMinutes;
    }

    public static long resolveSeconds(long hours, long minutes, long seconds) {
        long safeHours = Math.max(0L, hours);
        long safeMinutes = Math.max(0L, minutes);
        long safeSeconds = Math.max(0L, seconds);
        return (safeHours * 3600L) + (safeMinutes * 60L) + safeSeconds;
    }

    public static long resolveIntervalSeconds(long hours, long days, long seconds) {
        long h = Math.max(0L, hours);
        long d = Math.max(0L, days);
        long s = Math.max(0L, seconds);
        return (d * 86400L) + (h * 3600L) + s;
    }

    /** Legacy N+unit → seconds. */
    public static long resolveIntervalSeconds(long every, String unit) {
        long n = Math.max(0L, every);
        String u = normalizeIntervalUnit(unit);
        return switch (u) {
            case INTERVAL_UNIT_DAY -> n * 86400L;
            case INTERVAL_UNIT_SECOND -> n;
            default -> n * 3600L;
        };
    }

    public static String normalizeIntervalUnit(String raw) {
        String value = optional(raw).toUpperCase(Locale.ROOT);
        if (INTERVAL_UNIT_DAY.equals(value) || "J".equals(value) || "JOUR".equals(value) || "DAYS".equals(value)) {
            return INTERVAL_UNIT_DAY;
        }
        if (INTERVAL_UNIT_SECOND.equals(value) || "S".equals(value) || "SEC".equals(value) || "SECONDS".equals(value)) {
            return INTERVAL_UNIT_SECOND;
        }
        return INTERVAL_UNIT_HOUR;
    }

    public static String nextIntervalUnit(String raw) {
        String current = normalizeIntervalUnit(raw);
        if (INTERVAL_UNIT_HOUR.equals(current)) {
            return INTERVAL_UNIT_DAY;
        }
        if (INTERVAL_UNIT_DAY.equals(current)) {
            return INTERVAL_UNIT_SECOND;
        }
        return INTERVAL_UNIT_HOUR;
    }

    public static String intervalUnitLabel(String raw) {
        return switch (normalizeIntervalUnit(raw)) {
            case INTERVAL_UNIT_DAY -> "j";
            case INTERVAL_UNIT_SECOND -> "s";
            default -> "h";
        };
    }

    private static String sanitizeItemId(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static Map<String, Integer> sanitizeBossTierCountdownMinutes(Map<String, Integer> source) {
        Map<String, Integer> out = createDefaultBossTierCountdownMinutes();
        if (source == null || source.isEmpty()) {
            return out;
        }

        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String tier = normalizeTierKey(entry.getKey());
            if (!out.containsKey(tier)) {
                continue;
            }
            out.put(tier, Math.max(MIN_COUNTDOWN_MINUTES, entry.getValue()));
        }
        return out;
    }

    private static Map<String, Integer> createDefaultBossTierCountdownMinutes() {
        Map<String, Integer> out = new LinkedHashMap<>();
        // Start at 15 minutes for common, then +15 per tier.
        out.put("common", 15);
        out.put("uncommon", 30);
        out.put("rare", 45);
        out.put("epic", 60);
        out.put("legendary", 75);
        return out;
    }

    private static String normalizeTierKey(String tier) {
        if (tier == null || tier.isBlank()) {
            return "common";
        }
        return tier.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasAllCountdownTiers(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(normalizeTierKey(entry.getKey()), entry.getValue());
        }
        Map<String, Integer> defaults = createDefaultBossTierCountdownMinutes();
        for (String tier : defaults.keySet()) {
            Integer value = normalized.get(tier);
            if (value == null || value < MIN_COUNTDOWN_MINUTES) {
                return false;
            }
        }
        return true;
    }

    private static EventBannerTemplates createDefaultEventBannerTemplates() {
        return new EventBannerTemplates();
    }

    private static EventBannerTemplates sanitizeEventBannerTemplates(EventBannerTemplates source) {
        EventBannerTemplates out = createDefaultEventBannerTemplates();
        if (source == null) {
            return out;
        }

        out.activeTitle = migrateEventBannerText(
                optional(source.activeTitle),
                DEFAULT_EVENT_ACTIVE_TITLE_TEMPLATE,
                LEGACY_EVENT_ACTIVE_TITLE_TEMPLATE,
                LEGACY_EVENT_ACTIVE_TITLE_TEMPLATE_COLON,
                LEGACY_EVENT_ACTIVE_TITLE_TEMPLATE_FR
        );
        out.activeSubtitle = migrateEventBannerText(
                optional(source.activeSubtitle),
                DEFAULT_EVENT_ACTIVE_SUBTITLE_TEMPLATE,
                LEGACY_EVENT_ACTIVE_SUBTITLE_TEMPLATE,
                LEGACY_EVENT_ACTIVE_SUBTITLE_TEMPLATE_CAPS,
                LEGACY_EVENT_ACTIVE_SUBTITLE_TEMPLATE_FR
        );
        out.victoryTitle = migrateEventBannerText(
                optional(source.victoryTitle),
                DEFAULT_EVENT_VICTORY_TITLE_TEMPLATE,
                LEGACY_EVENT_VICTORY_TITLE_TEMPLATE
        );
        out.victorySubtitle = migrateEventBannerText(
                optional(source.victorySubtitle),
                DEFAULT_EVENT_VICTORY_SUBTITLE_TEMPLATE,
                LEGACY_EVENT_VICTORY_SUBTITLE_TEMPLATE,
                LEGACY_EVENT_VICTORY_SUBTITLE_TEMPLATE_CAPS,
                LEGACY_EVENT_VICTORY_SUBTITLE_TEMPLATE_FR
        );

        return out;
    }

    private static String migrateEventBannerText(String value, String frenchDefault, String... legacyEnglishDefaults) {
        String cleaned = stripEmDashes(value);
        if (cleaned.isEmpty()) {
            return frenchDefault;
        }
        for (String legacy : legacyEnglishDefaults) {
            if (cleaned.equals(legacy) || cleaned.equals(stripEmDashes(legacy))) {
                return frenchDefault;
            }
        }
        return cleaned;
    }

    private static String stripEmDashes(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace('\u2014', ':').replace('\u2013', '-');
    }

    private static TimedMapMarkerSettings createDefaultTimedMapMarkerSettings() {
        return new TimedMapMarkerSettings();
    }

    private static TimedMapMarkerSettings sanitizeTimedMapMarkerSettings(TimedMapMarkerSettings source) {
        TimedMapMarkerSettings out = createDefaultTimedMapMarkerSettings();
        if (source == null) {
            return out;
        }

        out.enabled = source.enabled;
        out.markerImage = optional(source.markerImage);
        if (out.markerImage.isEmpty()) {
            out.markerImage = DEFAULT_TIMED_MAP_MARKER_IMAGE;
        }
        out.nameTemplate = optional(source.nameTemplate);
        if (out.nameTemplate.isEmpty()) {
            out.nameTemplate = DEFAULT_TIMED_MAP_MARKER_NAME_TEMPLATE;
        }
        return out;
    }

    private static PlaceholderDocs createDefaultPlaceholderDocs() {
        return new PlaceholderDocs();
    }

    private static PlaceholderDocs sanitizePlaceholderDocs(PlaceholderDocs source) {
        PlaceholderDocs out = createDefaultPlaceholderDocs();
        if (source == null) {
            return out;
        }
        out.note = optional(source.note);
        if (out.note.isEmpty()) {
            out.note = "Documentation-only placeholders reference. Safe to edit/remove; BossArena does not read this block.";
        }
        out.eventBanner = sanitizeDocsMap(source.eventBanner, createDefaultEventBannerPlaceholderDocs());
        out.timedAnnouncement = sanitizeDocsMap(source.timedAnnouncement, createDefaultTimedAnnouncementPlaceholderDocs());
        out.timedMapMarker = sanitizeDocsMap(source.timedMapMarker, createDefaultTimedMapMarkerPlaceholderDocs());
        return out;
    }

    private static Map<String, String> sanitizeDocsMap(Map<String, String> source, Map<String, String> fallback) {
        Map<String, String> out = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, String> entry : source.entrySet()) {
                if (entry == null || entry.getKey() == null) {
                    continue;
                }
                String key = entry.getKey().trim();
                if (key.isEmpty()) {
                    continue;
                }
                out.put(key, optional(entry.getValue()));
            }
        }
        if (out.isEmpty()) {
            out.putAll(fallback);
        }
        return out;
    }

    private static Map<String, String> createDefaultEventBannerPlaceholderDocs() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("$PhaseTitle / {PhaseTitle}", "Combat title: 'Vague N' or 'Boss : Nom'.");
        out.put("$MonstersAlive / {MonstersAlive}", "Total remaining monsters (bosses + adds).");
        out.put("$DamageDealt / {DamageDealt}", "Player damage dealt (victory subtitle).");
        out.put("$Boss / {Boss}", "Boss display name.");
        out.put("$BossUpper / {BossUpper}", "Boss display name in uppercase.");
        out.put("$BossAlive / {BossAlive}", "Alive tracked boss count.");
        out.put("$AddsAlive / {AddsAlive}", "Alive tracked wave/add mob count.");
        out.put("$State / {State}", "Event state: active or victory.");
        return out;
    }

    private static Map<String, String> createDefaultTimedAnnouncementPlaceholderDocs() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("$Boss / {Boss}", "Boss display name.");
        out.put("$Arena / {Arena}", "Arena id/name.");
        out.put("$World / {World}", "World name.");
        out.put("&0-&f / §0-§f", "Chat color codes (e.g. &6 gold, &c red, &a green).");
        out.put("&l / &o / &r", "Bold, italic, and reset formatting.");
        return out;
    }

    private static Map<String, String> createDefaultTimedMapMarkerPlaceholderDocs() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("$Boss / {Boss}", "Boss display name.");
        out.put("$Arena / {Arena}", "Arena id/name.");
        out.put("$World / {World}", "World name when available.");
        return out;
    }

    private static List<TimedBossSpawn> sanitizeTimedBossSpawns(List<TimedBossSpawn> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        List<TimedBossSpawn> out = new ArrayList<>();
        for (TimedBossSpawn raw : source) {
            if (raw == null) {
                continue;
            }

            TimedBossSpawn clean = new TimedBossSpawn();
            clean.id = optional(raw.id);
            clean.enabled = raw.enabled;
            clean.bossId = optional(raw.bossId);
            clean.bossPool = sanitizeBossPool(raw.bossPool, clean.bossId);
            if (!clean.bossPool.isEmpty()) {
                clean.bossId = optional(clean.bossPool.get(0).bossId);
            }
            clean.arenaId = optional(raw.arenaId);
            clean.scheduleMode = normalizeScheduleMode(raw.scheduleMode);
            clean.spawnIntervalHours = Math.max(0L, raw.spawnIntervalHours);
            clean.spawnIntervalMinutes = Math.max(0L, raw.spawnIntervalMinutes);
            clean.spawnIntervalSeconds = Math.max(0L, raw.spawnIntervalSeconds);
            clean.intervalHours = Math.max(0L, raw.intervalHours);
            clean.intervalDays = Math.max(0L, raw.intervalDays);
            clean.intervalSeconds = Math.max(0L, raw.intervalSeconds);
            clean.intervalEvery = Math.max(0L, raw.intervalEvery);
            clean.intervalUnit = normalizeIntervalUnit(raw.intervalUnit);
            clean.arrivalWindowHours = Math.max(0L, raw.arrivalWindowHours);
            clean.arrivalWindowMinutes = Math.max(0L, raw.arrivalWindowMinutes);
            clean.arrivalWindowSeconds = Math.max(0L, raw.arrivalWindowSeconds);
            clean.fixedTimes = sanitizeFixedTimes(raw.fixedTimes);
            clean.oneShot = false;
            // Legacy proximity toggle ignored: Joueurs=0 means no player gate.
            clean.requirePlayerInRadius = false;
            clean.minPlayers = Math.max(0, raw.minPlayers);
            clean.preventDuplicateWhileAlive = true;
            clean.despawnAfterHours = Math.max(0L, raw.despawnAfterHours);
            clean.despawnAfterMinutes = Math.max(0L, raw.despawnAfterMinutes);
            clean.announceWorldWide = raw.announceWorldWide;
            clean.announceCurrentWorld = raw.announceCurrentWorld;
            if (clean.announceWorldWide) {
                clean.announceCurrentWorld = false;
            }
            clean.worldAnnouncementText = optional(raw.worldAnnouncementText);
            if (clean.worldAnnouncementText.isEmpty()) {
                clean.worldAnnouncementText = DEFAULT_TIMED_ANNOUNCEMENT_TEXT;
            }
            // Empty reminder = disabled (do not force a default).
            clean.reminderAnnouncementText = optional(raw.reminderAnnouncementText);
            clean.gracePeriodEnabled = raw.gracePeriodEnabled;
            clean.gracePeriodSeconds = Math.max(0L, raw.gracePeriodSeconds);
            clean.graceTitleText = optional(raw.graceTitleText);
            if (clean.graceTitleText.isEmpty()) {
                clean.graceTitleText = DEFAULT_TIMED_GRACE_TITLE_TEXT;
            }

            // Legacy FIXED_TIMES créneaux → 1 jour.
            String rawMode = optional(raw.scheduleMode).toUpperCase(Locale.ROOT);
            boolean legacyFixed = SCHEDULE_FIXED_TIMES.equals(rawMode)
                    || "FIXED".equals(rawMode)
                    || "HEURES".equals(rawMode)
                    || "CLOCK".equals(rawMode);
            if (legacyFixed
                    && clean.intervalHours <= 0L
                    && clean.intervalDays <= 0L
                    && clean.intervalSeconds <= 0L
                    && clean.intervalEvery <= 0L) {
                clean.scheduleMode = SCHEDULE_INTERVAL;
                clean.intervalDays = 1L;
            }

            // Legacy N + unité → h/j/s.
            if (resolveIntervalSeconds(clean.intervalHours, clean.intervalDays, clean.intervalSeconds) <= 0L
                    && clean.intervalEvery > 0L) {
                String u = normalizeIntervalUnit(clean.intervalUnit);
                if (INTERVAL_UNIT_DAY.equals(u)) {
                    clean.intervalDays = clean.intervalEvery;
                } else if (INTERVAL_UNIT_SECOND.equals(u)) {
                    clean.intervalSeconds = clean.intervalEvery;
                } else {
                    clean.intervalHours = clean.intervalEvery;
                }
            }

            if (clean.isManualMode()) {
                clean.scheduleMode = SCHEDULE_MANUAL;
            } else if (clean.isAfterDeathMode()) {
                long totalSeconds = resolveSeconds(
                        clean.spawnIntervalHours,
                        clean.spawnIntervalMinutes,
                        clean.spawnIntervalSeconds
                );
                if (totalSeconds <= 0L) {
                    clean.spawnIntervalHours = 1L;
                    clean.spawnIntervalMinutes = 0L;
                    clean.spawnIntervalSeconds = 0L;
                }
            } else {
                clean.scheduleMode = SCHEDULE_INTERVAL;
                if (resolveIntervalSeconds(clean.intervalHours, clean.intervalDays, clean.intervalSeconds) <= 0L) {
                    clean.intervalHours = 1L;
                    clean.intervalDays = 0L;
                    clean.intervalSeconds = 0L;
                }
            }

            out.add(clean);
        }
        return out;
    }

    private static List<BossPoolEntry> sanitizeBossPool(List<BossPoolEntry> source, String legacyBossId) {
        List<BossPoolEntry> out = new ArrayList<>();
        if (source != null) {
            for (BossPoolEntry raw : source) {
                if (raw == null) {
                    continue;
                }
                String bossId = optional(raw.bossId);
                if (bossId.isEmpty()) {
                    continue;
                }
                boolean duplicate = false;
                for (BossPoolEntry existing : out) {
                    if (bossId.equalsIgnoreCase(optional(existing.bossId))) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) {
                    continue;
                }
                BossPoolEntry clean = new BossPoolEntry();
                clean.bossId = bossId;
                clean.weight = Math.max(1, raw.weight);
                out.add(clean);
            }
        }
        if (out.isEmpty()) {
            String legacy = optional(legacyBossId);
            if (!legacy.isEmpty()) {
                BossPoolEntry single = new BossPoolEntry();
                single.bossId = legacy;
                single.weight = 1;
                out.add(single);
            }
        }
        return out;
    }

    private static String normalizeScheduleMode(String raw) {
        String value = optional(raw).toUpperCase(Locale.ROOT);
        if (SCHEDULE_MANUAL.equals(value) || "MANUEL".equals(value)) {
            return SCHEDULE_MANUAL;
        }
        if (SCHEDULE_INTERVAL.equals(value)
                || SCHEDULE_FIXED_TIMES.equals(value)
                || "FIXED".equals(value)
                || "HEURES".equals(value)
                || "CLOCK".equals(value)
                || "INTERVALLE".equals(value)
                || "PLANIFIE".equals(value)) {
            return SCHEDULE_INTERVAL;
        }
        return SCHEDULE_AFTER_DEATH;
    }

    private static List<String> sanitizeFixedTimes(List<String> source) {
        List<String> out = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        for (String raw : source) {
            String normalized = normalizeFixedTime(raw);
            if (normalized.isEmpty()) {
                continue;
            }
            if (!out.contains(normalized)) {
                out.add(normalized);
            }
        }
        out.sort(String::compareTo);
        return out;
    }

    /** Accepts "15:00", "15h00", "15". Returns "HH:MM" or empty if invalid. */
    public static String normalizeFixedTime(String raw) {
        String value = optional(raw).toLowerCase(Locale.ROOT).replace('h', ':').replace('.', ':');
        if (value.isEmpty()) {
            return "";
        }
        String[] parts = value.split(":");
        try {
            int hour;
            int minute = 0;
            if (parts.length == 1) {
                hour = Integer.parseInt(parts[0].trim());
            } else {
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim());
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return "";
            }
            return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
        } catch (NumberFormatException ex) {
            return "";
        }
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(this);
            Files.writeString(CONFIG_PATH, json);
            LOGGER.info("Successfully saved BossArena config");
        } catch (IOException e) {
            LOGGER.severe("Failed to save BossArena config: " + e.getMessage());
        }
    }

    public void load() {
        try {
            migrateLegacyConfigIfNeeded();
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                BossArenaConfig loaded = new GsonBuilder().create().fromJson(content, BossArenaConfig.class);
                if (loaded != null) {
                    applyLoadedConfig(loaded);
                    LOGGER.info("Successfully loaded BossArena config");
                    if (shouldPersistMergedConfig(content)) {
                        save();
                    }
                } else {
                    LOGGER.warning("Config file was empty or invalid JSON, recreating defaults");
                    applyDefaultConfig();
                    save();
                }
            } else {
                LOGGER.info("No config file found, creating defaults");
                applyDefaultConfig();
                save();
            }
        } catch (IOException e) {
            LOGGER.severe("Failed to load BossArena config: " + e.getMessage());
        }
    }

    private void migrateLegacyConfigIfNeeded() {
        try {
            if (Files.exists(CONFIG_PATH) || !Files.isRegularFile(LEGACY_CONFIG_PATH)) {
                return;
            }
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.copy(LEGACY_CONFIG_PATH, CONFIG_PATH);
            LOGGER.info("Migrated config from " + LEGACY_CONFIG_PATH + " to " + CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.warning("Failed to migrate legacy BossArena config: " + e.getMessage());
        }
    }

    public List<TimedBossSpawn> getTimedBossSpawns() {
        timedBossSpawns = sanitizeTimedBossSpawns(timedBossSpawns);
        return new ArrayList<>(timedBossSpawns);
    }

    public int getBossTierCountdownMinutes(String tier) {
        Map<String, Integer> timers = sanitizeBossTierCountdownMinutes(bossTierCountdownMinutes);
        this.bossTierCountdownMinutes = timers;
        String key = normalizeTierKey(tier);
        Integer configured = timers.get(key);
        if (configured == null) {
            configured = timers.get("common");
        }
        if (configured == null) {
            configured = 15;
        }
        return Math.max(MIN_COUNTDOWN_MINUTES, configured);
    }

    private void applyLoadedConfig(BossArenaConfig loaded) {
        this.notificationRadius = NotificationRadiusConstants.clamp(loaded.notificationRadius);
        this.currencyItemId = sanitizeItemId(loaded.currencyItemId, DEFAULT_CURRENCY_ITEM_ID);
        this.fallbackCurrencyItemId = sanitizeItemId(
                loaded.fallbackCurrencyItemId,
                DEFAULT_FALLBACK_CURRENCY_ITEM_ID
        );
        this.bossTierCountdownMinutes = sanitizeBossTierCountdownMinutes(loaded.bossTierCountdownMinutes);
        this.eventBanner = sanitizeEventBannerTemplates(loaded.eventBanner);
        this.timedMapMarker = sanitizeTimedMapMarkerSettings(loaded.timedMapMarker);
        this.timedBossSpawns = sanitizeTimedBossSpawns(loaded.timedBossSpawns);
        this._comment_placeholders = sanitizePlaceholderDocs(loaded._comment_placeholders);
    }

    /** Returns the configured notification radius (blocks), clamped to valid range. */
    public double getNotificationRadius() {
        return NotificationRadiusConstants.clamp(notificationRadius);
    }

    private void applyDefaultConfig() {
        this.notificationRadius = NotificationRadiusConstants.DEFAULT;
        this.currencyItemId = DEFAULT_CURRENCY_ITEM_ID;
        this.fallbackCurrencyItemId = DEFAULT_FALLBACK_CURRENCY_ITEM_ID;
        this.bossTierCountdownMinutes = createDefaultBossTierCountdownMinutes();
        this.eventBanner = createDefaultEventBannerTemplates();
        this.timedMapMarker = createDefaultTimedMapMarkerSettings();
        this.timedBossSpawns = new ArrayList<>();
        this._comment_placeholders = createDefaultPlaceholderDocs();
    }

    private boolean shouldPersistMergedConfig(String originalContent) {
        if (originalContent == null || originalContent.isBlank()) {
            return true;
        }
        try {
            Gson gson = new GsonBuilder().create();
            JsonElement onDisk = JsonParser.parseString(originalContent);
            JsonElement merged = gson.toJsonTree(this);
            if (!hasAllCountdownTiers(this.bossTierCountdownMinutes)) {
                return true;
            }
            return !merged.equals(onDisk);
        } catch (Exception ignored) {
            return true;
        }
    }

    public static final class EventBannerTemplates {
        public String activeTitle = DEFAULT_EVENT_ACTIVE_TITLE_TEMPLATE;
        public String activeSubtitle = DEFAULT_EVENT_ACTIVE_SUBTITLE_TEMPLATE;
        public String victoryTitle = DEFAULT_EVENT_VICTORY_TITLE_TEMPLATE;
        public String victorySubtitle = DEFAULT_EVENT_VICTORY_SUBTITLE_TEMPLATE;
    }

    public static final class TimedMapMarkerSettings {
        public boolean enabled = true;
        public String markerImage = DEFAULT_TIMED_MAP_MARKER_IMAGE;
        public String nameTemplate = DEFAULT_TIMED_MAP_MARKER_NAME_TEMPLATE;
    }

    public static final String SCHEDULE_AFTER_DEATH = "AFTER_DEATH";
    public static final String SCHEDULE_INTERVAL = "INTERVAL";
    /** Manual pop only — no auto schedule. */
    public static final String SCHEDULE_MANUAL = "MANUAL";
    /** Legacy alias; sanitized to {@link #SCHEDULE_INTERVAL}. */
    public static final String SCHEDULE_FIXED_TIMES = "FIXED_TIMES";
    public static final String INTERVAL_UNIT_HOUR = "HOUR";
    public static final String INTERVAL_UNIT_DAY = "DAY";
    public static final String INTERVAL_UNIT_SECOND = "SECOND";

    public static final class BossPoolEntry {
        public String bossId = "";
        /** Relative weight for weighted random (≥ 1). */
        public int weight = 1;
    }

    public static final class TimedBossSpawn {
        public String id = "";
        public boolean enabled = true;
        /** Legacy single boss id (kept in sync with first {@link #bossPool} entry). */
        public String bossId = "";
        /** Weighted pool of bosses; one is rolled on each spawn. */
        public List<BossPoolEntry> bossPool = new ArrayList<>();
        public String arenaId = "";
        /**
         * {@link BossArenaConfig#SCHEDULE_AFTER_DEATH}, {@link BossArenaConfig#SCHEDULE_INTERVAL},
         * or {@link BossArenaConfig#SCHEDULE_MANUAL}.
         */
        public String scheduleMode = SCHEDULE_AFTER_DEATH;
        /** Delay after death. Used when scheduleMode is AFTER_DEATH. */
        public long spawnIntervalHours = 1L;
        public long spawnIntervalMinutes = 0L;
        public long spawnIntervalSeconds = 0L;
        /** Interval recurrence after each spawn: hours + days + seconds. */
        public long intervalHours = 1L;
        public long intervalDays = 0L;
        public long intervalSeconds = 0L;
        /** Legacy single-value interval (migrated into h/j/s). */
        public long intervalEvery = 0L;
        /** Legacy unit HOUR/DAY/SECOND (migrated). */
        public String intervalUnit = INTERVAL_UNIT_HOUR;
        /**
         * Planifié only: max time after the scheduled spawn for a player to reach the arena (Rayon Décl).
         * If nobody arrives in time, this spawn is skipped until the next interval.
         */
        public long arrivalWindowHours = 0L;
        public long arrivalWindowMinutes = 15L;
        public long arrivalWindowSeconds = 0L;
        /** Legacy FIXED_TIMES slots (ignored). */
        public List<String> fixedTimes = new ArrayList<>();
        /** Legacy one-shot flag (ignored). */
        public boolean oneShot = false;
        /** Legacy; ignored. Use {@link #minPlayers} (0 = spawn without player gate). */
        public boolean requirePlayerInRadius = false;
        /** Minimum players online in the arena world required before spawn (0 = no gate). */
        public int minPlayers = 0;
        public boolean preventDuplicateWhileAlive = true;
        public long despawnAfterHours = 0L;
        public long despawnAfterMinutes = 5L;
        // Legacy key: server-wide announcement across all worlds.
        public boolean announceWorldWide = false;
        // Optional world-only announcement for players in the spawned world.
        public boolean announceCurrentWorld = false;
        // Optional custom message for global announcement.
        public String worldAnnouncementText = DEFAULT_TIMED_ANNOUNCEMENT_TEXT;
        /** Optional chat reminder sent 5 minutes before scheduled spawn. Empty = no reminder. */
        public String reminderAnnouncementText = DEFAULT_TIMED_REMINDER_TEXT;
        /** When true, wait {@link #gracePeriodSeconds} after nearby player threshold before spawn. */
        public boolean gracePeriodEnabled = false;
        public long gracePeriodSeconds = 30L;
        /** Title shown during grace; supports $time (mm:ss), $Boss, $Arena, $World. */
        public String graceTitleText = DEFAULT_TIMED_GRACE_TITLE_TEXT;

        public boolean isIntervalMode() {
            String mode = optional(scheduleMode);
            return SCHEDULE_INTERVAL.equalsIgnoreCase(mode) || SCHEDULE_FIXED_TIMES.equalsIgnoreCase(mode);
        }

        public boolean isManualMode() {
            return SCHEDULE_MANUAL.equalsIgnoreCase(optional(scheduleMode));
        }

        /** @deprecated use {@link #isIntervalMode()} */
        @Deprecated
        public boolean isFixedTimesMode() {
            return isIntervalMode();
        }

        public boolean isAfterDeathMode() {
            return !isIntervalMode() && !isManualMode();
        }

        /** Non-empty sanitized pool (falls back to {@link #bossId}). */
        public List<BossPoolEntry> resolveBossPool() {
            return sanitizeBossPool(bossPool, bossId);
        }

        public boolean poolContainsBoss(String candidateBossId) {
            String needle = optional(candidateBossId);
            if (needle.isEmpty()) {
                return false;
            }
            for (BossPoolEntry entry : resolveBossPool()) {
                if (needle.equalsIgnoreCase(optional(entry.bossId))) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Weighted random pick from the pool. Returns empty string if the pool is empty.
         */
        public String pickWeightedBossId(java.util.Random random) {
            List<BossPoolEntry> pool = resolveBossPool();
            if (pool.isEmpty()) {
                return "";
            }
            if (pool.size() == 1) {
                return optional(pool.get(0).bossId);
            }
            int total = 0;
            for (BossPoolEntry entry : pool) {
                total += Math.max(1, entry.weight);
            }
            int roll = random.nextInt(Math.max(1, total));
            int cursor = 0;
            for (BossPoolEntry entry : pool) {
                cursor += Math.max(1, entry.weight);
                if (roll < cursor) {
                    return optional(entry.bossId);
                }
            }
            return optional(pool.get(pool.size() - 1).bossId);
        }
    }

    public static final class PlaceholderDocs {
        public String note = "Documentation-only placeholders reference. Safe to edit/remove; BossArena does not read this block.";
        public Map<String, String> eventBanner = createDefaultEventBannerPlaceholderDocs();
        public Map<String, String> timedAnnouncement = createDefaultTimedAnnouncementPlaceholderDocs();
        public Map<String, String> timedMapMarker = createDefaultTimedMapMarkerPlaceholderDocs();
    }
}
