package com.varyon.bossarena.config;

/**
 * Config UI for BossArena: bosses, shop locations, and arenas tabs.
 * Consider splitting into {@code BossesTabHandler}, {@code ShopTabHandler}, and {@code ArenasTabHandler}
 * (each owning build/handle logic and tab-specific state) to reduce this class size.
 */
import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.data.BossDefinition;
import com.varyon.bossarena.data.BossRegistry;
import com.varyon.bossarena.loot.LootItem;
import com.varyon.bossarena.loot.LootRegistry;
import com.varyon.bossarena.loot.LootTable;
import com.varyon.bossarena.shop.BossShopConfig;
import com.varyon.bossarena.shop.ShopEntry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.varyon.bossarena.util.VecUtil;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class BossArenaConfigPage extends InteractiveCustomUIPage<BossArenaConfigPage.ConfigEventData> {
    private static final String LAYOUT = "Pages/BossArenaConfigPage.ui";
    private static final String TAB_BOSSES = "bosses";
    private static final String TAB_SHOP = "shop";
    private static final String TAB_ARENAS = "arenas";

    private static final int MAX_ARENA_ROWS = 8;
    private static final int MAX_SHOP_ROWS = 8;
    private static final int MAX_SHOP_BOSS_ROWS = 12;
    private static final int MAX_SHOP_BOSS_VISIBLE_ROWS = 10;
    private static final int MAX_BOSS_ROWS = 8;
    private static final int MAX_LOOT_ROWS = 8;
    private static final int MAX_LOOT_VISIBLE_ROWS = 5;
    private static final int MAX_WAVE_ADD_ROWS = 6;
    private static final float MULT_HP_DMG_MIN = 0.50f;
    private static final float MULT_HP_DMG_MAX = 50.00f;
    private static final float MULT_SIZE_MIN = 0.10f;
    private static final float MULT_SIZE_MAX = 10.00f;
    private static final float MULT_PERS_MIN = 0.00f;
    private static final float MULT_PERS_MAX = 5.00f;
    private static final int MAX_TIMED_SPAWN_ROWS = 6;
    private static final int BOSS_SCROLL_THUMB_STEPS = 10;
    private static final int SHOP_EDIT_SCROLL_THUMB_STEPS = 10;
    private static final int SHOP_CONTRACT_PRICE_STEP = 25;
    private static final Pattern ARENA_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final BossArenaPlugin plugin;
    private final List<String> arenaRows = new ArrayList<>();
    private final List<ShopLocationRef> shopRows = new ArrayList<>();
    private final List<String> bossRows = new ArrayList<>();
    private String selectedTab;
    private String arenaStatusText = "";
    private String shopStatusText = "";
    private String bossStatusText = "";
    private int bossListOffset = 0;

    private BossEditorState bossEditorState;
    private boolean bossWavesOverlayOpen;
    private boolean bossScalersOverlayOpen;
    private boolean bossTimedOverlayOpen;
    /** NPC ID suggestion list visible only while actively searching (typing). */
    private boolean bossNpcPicksOpen;
    /** Wave schedule NPC suggestions: open while typing on a specific row. */
    private boolean waveNpcPicksOpen;
    private int waveNpcPicksRow;
    private String waveNpcSearchQuery = "";
    private ShopLocationEditorState shopLocationEditorState;

    private BossArenaConfigPage(PlayerRef playerRef, BossArenaPlugin plugin, String tab) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigEventData.CODEC);
        this.plugin = plugin;
        this.selectedTab = sanitizeTab(tab);
    }

    public static void open(Ref<EntityStore> ref,
                            Store<EntityStore> store,
                            Player player,
                            BossArenaPlugin plugin) {
        openTab(ref, store, player, plugin, TAB_BOSSES);
    }

    public static void openTab(Ref<EntityStore> ref,
                               Store<EntityStore> store,
                               Player player,
                               BossArenaPlugin plugin,
                               String tab) {
        if (player == null || plugin == null) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        BossArenaConfigPage page = new BossArenaConfigPage(playerRef, plugin, tab);
        player.getPageManager().openCustomPage(ref, store, page);
    }

    private static int countEnabledBosses(List<String> bossIds) {
        if (bossIds == null || bossIds.isEmpty()) {
            return 0;
        }
        int out = 0;
        for (String bossId : bossIds) {
            if (bossId != null && !bossId.isBlank()) {
                out++;
            }
        }
        return out;
    }

    private static Arena findNearestArenaForShop(String worldName, int x, int y, int z) {
        Arena best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Arena arena : ArenaRegistry.getAll()) {
            if (arena == null || arena.worldName == null) {
                continue;
            }
            if (!arena.worldName.equalsIgnoreCase(worldName)) {
                continue;
            }
            double dx = arena.x - x;
            double dy = arena.y - y;
            double dz = arena.z - z;
            double distanceSq = (dx * dx) + (dy * dy) + (dz * dz);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = arena;
            }
        }
        return best;
    }

    private static int resolveShopEditScrollThumbStep(int offset, int maxOffset) {
        if (maxOffset <= 0) {
            return 1;
        }
        double normalized = Math.max(0.0d, Math.min(1.0d, (double) offset / (double) maxOffset));
        int index = (int) Math.round(normalized * (SHOP_EDIT_SCROLL_THUMB_STEPS - 1));
        return Math.max(1, Math.min(SHOP_EDIT_SCROLL_THUMB_STEPS, index + 1));
    }

    private static String resolveBossNameCaseInsensitive(List<String> bossNames, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (String bossName : bossNames) {
            if (bossName.equalsIgnoreCase(input)) {
                return bossName;
            }
        }
        return null;
    }

    private static int resolveBossScrollThumbStep(int offset, int maxOffset) {
        if (maxOffset <= 0) {
            return 1;
        }
        double normalized = Math.max(0.0d, Math.min(1.0d, (double) offset / (double) maxOffset));
        int index = (int) Math.round(normalized * (BOSS_SCROLL_THUMB_STEPS - 1));
        return Math.max(1, Math.min(BOSS_SCROLL_THUMB_STEPS, index + 1));
    }

    private static String defaultWaveTriggerInput(int row, boolean useExamples) {
        if (!useExamples) {
            return "Après spawn";
        }
        if (row == 1) {
            return "3";
        }
        if (row == 2) {
            return "Après spawn";
        }
        return "Après spawn";
    }

    private static int defaultWaveValueSeconds(int row, boolean useExamples) {
        if (!useExamples) {
            return 30;
        }
        if (row == 1) {
            return 15;
        }
        if (row == 2) {
            return 30;
        }
        return 30;
    }

    private static String defaultWaveNpcId(int row, boolean useExamples) {
        if (!useExamples) {
            return "";
        }
        if (row == 1) {
            return "Bat";
        }
        if (row == 2) {
            return "Spider";
        }
        return "";
    }

    private static String normalizeWaveTriggerInput(String input) {
        String cleaned = optionalText(input)
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace("(", " ")
                .replace(")", " ")
                .replace("%", " percent ")
                .replaceAll("\\s+", " ")
                .trim();
        return switch (cleaned) {
            case "1", "before boss", "before", "pre", "pre boss", "bb",
                 "avant le boss", "avant", "avant boss" -> BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS;
            case "2", "on spawn", "spawn", "at spawn", "on boss spawn", "with boss", "os",
                 "au spawn", "a spawn", "avec boss" ->
                    BossDefinition.ExtraMobs.TRIGGER_ON_SPAWN;
            case "after spawn seconds", "after spawn", "after", "seconds", "time", "timer", "after spawn second",
                 "apres spawn", "après spawn", "apres", "après", "secondes",
                 "apres spawn secondes", "après spawn secondes" ->
                    BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS;
            case "3", "as", "after s" -> BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS;
            case "since last wave", "since last wave second", "since last wave seconds", "since wave", "last wave",
                 "wave delay", "since previous wave", "since last",
                 "depuis derniere vague", "depuis dernière vague", "depuis vague",
                 "derniere vague", "dernière vague", "delai vague", "délai vague",
                 "depuis derniere", "depuis dernière" -> BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE;
            case "4", "slw", "since", "depuis" -> BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE;
            case "boss hp percent", "boss health percent", "hp percent", "health percent", "boss hp", "boss health",
                 "hp", "pv boss", "pv boss percent", "pv percent", "vie boss" -> BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT;
            case "5", "hpp", "health" -> BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT;
            default -> null;
        };
    }

    private static String toWaveTriggerDisplayName(String trigger) {
        String normalized = normalizeWaveTriggerInput(trigger);
        if (BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS.equals(normalized)) {
            return "Avant le boss";
        }
        if (BossDefinition.ExtraMobs.TRIGGER_ON_SPAWN.equals(normalized)) {
            return "Au spawn";
        }
        if (BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE.equals(normalized)) {
            return "Depuis dernière vague";
        }
        if (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(normalized)) {
            return "PV boss %";
        }
        return "Après spawn";
    }

    private static WaveScheduleRow copyWaveScheduleRow(WaveScheduleRow source) {
        if (source == null || source.add == null) {
            return null;
        }
        BossDefinition.ExtraMobs.WaveAdd copy = new BossDefinition.ExtraMobs.WaveAdd();
        copy.npcId = source.add.npcId;
        copy.mobsPerWave = source.add.mobsPerWave;
        copy.everyWave = 1;
        copy.hp = source.add.hp;
        copy.damage = source.add.damage;
        copy.size = source.add.size;
        return new WaveScheduleRow(
                source.trigger,
                source.triggerValue,
                source.repeatCount,
                source.repeatEverySeconds,
                copy
        );
    }

    private static WaveScheduleRow defaultWaveScheduleRow() {
        BossDefinition.ExtraMobs.WaveAdd add = new BossDefinition.ExtraMobs.WaveAdd();
        add.npcId = "";
        add.mobsPerWave = 1;
        add.everyWave = 1;
        add.hp = 1.0f;
        add.damage = 1.0f;
        add.size = 1.0f;
        return new WaveScheduleRow(
                BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS,
                30.0d,
                1,
                0.0d,
                add
        );
    }

    private static List<WaveScheduleRow> flattenScheduleRows(BossDefinition.ExtraMobs extra) {
        if (extra == null) {
            return List.of();
        }
        extra.sanitize();
        List<WaveScheduleRow> out = new ArrayList<>();
        for (BossDefinition.ExtraMobs.ScheduledWave wave : extra.getResolvedScheduledWaves()) {
            if (wave == null || wave.adds == null) {
                continue;
            }
            for (BossDefinition.ExtraMobs.WaveAdd add : wave.adds) {
                if (add == null || add.npcId == null || add.npcId.isBlank()) {
                    continue;
                }
                BossDefinition.ExtraMobs.WaveAdd addCopy = new BossDefinition.ExtraMobs.WaveAdd();
                addCopy.npcId = add.npcId;
                addCopy.mobsPerWave = Math.max(1, add.mobsPerWave);
                addCopy.everyWave = 1;
                addCopy.hp = add.hp > 0f ? add.hp : 1.0f;
                addCopy.damage = add.damage > 0f ? add.damage : 1.0f;
                addCopy.size = add.size > 0f ? add.size : 1.0f;
                out.add(new WaveScheduleRow(
                        optionalText(wave.trigger),
                        wave.triggerValue,
                        wave.repeatCount,
                        wave.repeatEverySeconds,
                        addCopy
                ));
                if (out.size() >= MAX_WAVE_ADD_ROWS) {
                    return out;
                }
            }
        }
        return out;
    }

    private static void applyScheduleRowsToExtra(BossDefinition.ExtraMobs extra, List<WaveScheduleRow> rows) {
        if (extra == null) {
            return;
        }

        extra.npcId = "";
        extra.timeLimitMs = 0L;
        extra.waves = 0;
        extra.mobsPerWave = 1;
        extra.adds = new ArrayList<>();
        extra.scheduledWaves = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (WaveScheduleRow row : rows) {
            if (row == null || row.add == null || row.add.npcId == null || row.add.npcId.isBlank()) {
                continue;
            }

            BossDefinition.ExtraMobs.ScheduledWave target = null;
            for (BossDefinition.ExtraMobs.ScheduledWave existing : extra.scheduledWaves) {
                if (sameScheduleKey(existing, row)) {
                    target = existing;
                    break;
                }
            }
            if (target == null) {
                target = new BossDefinition.ExtraMobs.ScheduledWave();
                target.trigger = row.trigger;
                target.triggerValue = row.triggerValue;
                target.repeatCount = row.repeatCount;
                target.repeatEverySeconds = row.repeatEverySeconds;
                target.adds = new ArrayList<>();
                extra.scheduledWaves.add(target);
            }

            BossDefinition.ExtraMobs.WaveAdd addCopy = new BossDefinition.ExtraMobs.WaveAdd();
            addCopy.npcId = row.add.npcId;
            addCopy.mobsPerWave = Math.max(1, row.add.mobsPerWave);
            addCopy.everyWave = 1;
            addCopy.hp = row.add.hp > 0f ? row.add.hp : 1.0f;
            addCopy.damage = row.add.damage > 0f ? row.add.damage : 1.0f;
            addCopy.size = row.add.size > 0f ? row.add.size : 1.0f;
            target.adds.add(addCopy);
        }
    }

    private static boolean sameScheduleKey(BossDefinition.ExtraMobs.ScheduledWave existing, WaveScheduleRow row) {
        if (existing == null || row == null) {
            return false;
        }
        if (!optionalText(existing.trigger).equals(optionalText(row.trigger))) {
            return false;
        }
        if (Math.abs(existing.triggerValue - row.triggerValue) > 0.0001d) {
            return false;
        }
        if (existing.repeatCount != row.repeatCount) {
            return false;
        }
        return Math.abs(existing.repeatEverySeconds - row.repeatEverySeconds) <= 0.0001d;
    }

    private static void applyWaveSpawnSettingsFromData(BossDefinition.ExtraMobs extra, ConfigEventData data) {
        if (extra == null || data == null) {
            return;
        }

        String resolvedRandomLocations = resolvedOrFallback(
                data.bossWaveRandomLocations,
                extra.useRandomSpawnLocations ? "true" : "false"
        );
        if (!resolvedRandomLocations.isEmpty()) {
            Boolean randomEnabled = parseToggleInput(resolvedRandomLocations);
            if (randomEnabled == null) {
                throw new IllegalArgumentException("Les positions aléatoires doivent être true/false, on/off, yes/no, oui/non ou 1/0.");
            }
            extra.useRandomSpawnLocations = randomEnabled;
        }

        String resolvedRadius = resolvedOrFallback(
                data.bossWaveRandomRadius,
                formatWaveNumber(extra.getWaveRandomSpawnRadius())
        );
        if (!resolvedRadius.isEmpty()) {
            extra.randomSpawnRadius = parseRequiredDouble(
                    resolvedRadius,
                    "Le rayon aléatoire doit être un nombre >= 0.",
                    0.0d,
                    Double.MAX_VALUE
            );
        }

        String resolvedBossSpawnTrigger = resolvedOrFallback(data.bossSpawnTrigger, toBossSpawnTriggerDisplayName(extra.bossSpawnTrigger));
        if (resolvedBossSpawnTrigger != null && !resolvedBossSpawnTrigger.isBlank()) {
            String normalized = normalizeBossSpawnTriggerInput(resolvedBossSpawnTrigger);
            if (normalized != null) {
                extra.bossSpawnTrigger = normalized;
            }
        }
        String resolvedBossSpawnValue = resolvedOrFallback(data.bossSpawnTriggerValue, formatWaveNumber(extra.bossSpawnTriggerValue));
        if (resolvedBossSpawnValue != null && !resolvedBossSpawnValue.isBlank()) {
            extra.bossSpawnTriggerValue = parseRequiredDouble(
                    resolvedBossSpawnValue,
                    "La valeur de déclenchement du boss (secondes) doit être un nombre >= 0.",
                    0.0d,
                    Double.MAX_VALUE
            );
        }
        String proxEnabledRaw = resolvedOrFallback(
                data.bossTimedProximityEnabled,
                extra.timedProximityEnabled ? "true" : "false"
        );
        if (!proxEnabledRaw.isEmpty()) {
            Boolean enabled = parseToggleInput(proxEnabledRaw);
            if (enabled == null) {
                throw new IllegalArgumentException("Proximité activée doit être true/false.");
            }
            extra.timedProximityEnabled = enabled;
        }
        String proxArenaRaw = resolvedOrFallback(
                data.bossTimedProximityArena,
                extra.timedProximityArenaId != null ? extra.timedProximityArenaId : ""
        );
        if (!proxArenaRaw.isEmpty() && !looksLikeUiBindingExpression(proxArenaRaw)) {
            extra.timedProximityArenaId = proxArenaRaw.trim();
        }
        String proxRadiusRaw = resolvedOrFallback(
                data.bossTimedProximityRadius,
                formatWaveNumber(extra.getTimedProximityRadius())
        );
        if (!proxRadiusRaw.isEmpty()) {
            extra.timedProximityRadius = parseRequiredDouble(
                    proxRadiusRaw,
                    "Le rayon de proximité doit être un nombre >= 0.",
                    0.0d,
                    Double.MAX_VALUE
            );
        }
        String proxCooldownRaw = resolvedOrFallback(
                data.bossTimedProximityCooldown,
                Long.toString(extra.getTimedProximityCooldownSeconds(60L))
        );
        if (!proxCooldownRaw.isEmpty()) {
            long cooldown = (long) parseRequiredDouble(
                    proxCooldownRaw,
                    "La recharge de proximité doit être un nombre >= 0.",
                    0.0d,
                    Double.MAX_VALUE
            );
            extra.timedProximityCooldownSeconds = cooldown;
        }
        extra.sanitize();
    }

    private static String toBossSpawnTriggerDisplayName(String trigger) {
        if (BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_BEFORE_BOSS.equals(trigger)) {
            return "Après pré-boss";
        }
        if (BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_SECONDS.equals(trigger)) {
            return "Après délai";
        }
        return trigger != null ? trigger : "Après pré-boss";
    }

    private static String normalizeBossSpawnTriggerInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
        if ((normalized.contains("before") && normalized.contains("boss"))
                || normalized.contains("pré-boss")
                || normalized.contains("pre-boss")
                || normalized.contains("pre boss")
                || normalized.contains("pré boss")
                || normalized.contains("apres pre")
                || normalized.contains("après pré")
                || normalized.contains("apres pre-boss")
                || normalized.contains("après pré-boss")) {
            return BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_BEFORE_BOSS;
        }
        if (normalized.contains("second")
                || normalized.contains("délai")
                || normalized.contains("delai")
                || normalized.contains("après délai")
                || normalized.contains("apres delai")) {
            return BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_SECONDS;
        }
        return null;
    }

    private static Boolean parseToggleInput(String raw) {
        String value = optionalText(raw).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "1", "true", "t", "yes", "y", "on", "enabled", "enable", "oui", "o" -> true;
            case "0", "false", "f", "no", "n", "off", "disabled", "disable", "non" -> false;
            default -> null;
        };
    }

    private static int parseRow(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static Double parseCoordinate(String raw) {
        if (raw == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalizeArenaId(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static String formatCoord(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String formatChance(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Nonnull
    private static List<DropdownEntryInfo> arenaDropdownEntries() {
        List<Arena> arenas = new ArrayList<>(ArenaRegistry.getAll());
        arenas.sort(Comparator.comparing(a -> a.arenaId == null ? "" : a.arenaId, String.CASE_INSENSITIVE_ORDER));
        List<DropdownEntryInfo> entries = new ArrayList<>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("(none)"), ""));
        for (Arena arena : arenas) {
            String id = safeText(arena.arenaId);
            if (id.isEmpty()) {
                continue;
            }
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        }
        return entries;
    }

    private static String formatSecondsFromMillis(long millis) {
        if (millis <= 0L) {
            return "0";
        }
        if (millis % 1000L == 0L) {
            return Long.toString(millis / 1000L);
        }
        return formatDouble(millis / 1000.0d);
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeTier(String input) {
        if (input == null) {
            return "common";
        }
        String tier = input.trim().toLowerCase(Locale.ROOT);
        return switch (tier) {
            case "common", "uncommon", "rare", "epic", "legendary" -> tier;
            default -> "common";
        };
    }

    private static int defaultContractPriceForTier(String tier) {
        return switch (normalizeTier(tier)) {
            case "common" -> 100;
            case "uncommon" -> 250;
            case "rare" -> 500;
            case "epic" -> 900;
            case "legendary" -> 1500;
            default -> 100;
        };
    }

    private static Integer findEntryContractPrice(List<ShopEntry> entries, String bossName, String tier) {
        if (entries == null || entries.isEmpty() || bossName == null || bossName.isBlank()) {
            return null;
        }
        for (ShopEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (!normalizeTier(tier).equalsIgnoreCase(normalizeTier(entry.tier))) {
                continue;
            }
            if (bossName.equalsIgnoreCase(optionalText(entry.bossId))) {
                return Math.max(0, entry.cost);
            }
        }
        return null;
    }

    private static int resolveDefaultContractPrice(BossShopConfig shopConfig, String bossName, String tier) {
        Integer configured = findEntryContractPrice(shopConfig != null ? shopConfig.entries : null, bossName, tier);
        return configured != null ? configured : defaultContractPriceForTier(tier);
    }

    private static String toTierDisplayName(String value) {
        String tier = optionalText(value).toLowerCase(Locale.ROOT);
        return switch (tier) {
            case "common" -> "Commun";
            case "uncommon" -> "Peu commun";
            case "rare" -> "Rare";
            case "epic" -> "Épique";
            case "legendary" -> "Légendaire";
            default -> capitalize(value);
        };
    }

    private static String capitalize(String value) {
        String text = optionalText(value);
        if (text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requireNonBlank(String value, String errorMessage) {
        String out = optionalText(value);
        if (out.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return out;
    }

    private static String firstResolvedValue(String primary, String fallback) {
        String primaryValue = optionalText(primary);
        if (!primaryValue.isEmpty() && !looksLikeUiBindingExpression(primaryValue)) {
            return primaryValue;
        }

        String fallbackValue = optionalText(fallback);
        if (!fallbackValue.isEmpty() && !looksLikeUiBindingExpression(fallbackValue)) {
            return fallbackValue;
        }

        return primaryValue;
    }

    private static String resolvedOrFallback(String input, String fallback) {
        String value = optionalText(input);
        if (!value.isEmpty() && !looksLikeUiBindingExpression(value)) {
            return value;
        }
        String fallbackValue = optionalText(fallback);
        if (!fallbackValue.isEmpty() && !looksLikeUiBindingExpression(fallbackValue)) {
            return fallbackValue;
        }
        return "";
    }

    private static boolean looksLikeUiBindingExpression(String value) {
        String out = optionalText(value);
        return out.startsWith("#") && (out.endsWith(".Value") || out.endsWith(".Text"));
    }

    private static int parseRequiredInt(String raw, String errorMessage, int min, int max) {
        try {
            int out = Integer.parseInt(optionalText(raw));
            if (out < min || out > max) {
                throw new IllegalArgumentException(errorMessage);
            }
            return out;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static int parseBossLevelOverride(String raw, String errorMessage) {
        String value = optionalText(raw);
        if (value.isEmpty()) {
            return 0;
        }
        try {
            int out = Integer.parseInt(value);
            if (out < 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return out;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static long parseRequiredLong(String raw, String errorMessage, long min, long max) {
        try {
            long out = Long.parseLong(optionalText(raw));
            if (out < min || out > max) {
                throw new IllegalArgumentException(errorMessage);
            }
            return out;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static long parseRequiredSecondsToMillis(String raw, String errorMessage) {
        double seconds = parseRequiredDouble(raw, errorMessage, 0.0d, Double.MAX_VALUE);
        if (seconds > (Long.MAX_VALUE / 1000.0d)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return Math.round(seconds * 1000.0d);
    }

    private static float requireSliderFloat(Float value, float fallback, float min, float max, String errorMessage) {
        float out = (value != null && Float.isFinite(value)) ? value : fallback;
        if (!Float.isFinite(out) || out < min || out > max) {
            throw new IllegalArgumentException(errorMessage);
        }
        return out;
    }

    private static float parseRequiredFloat(String raw, String errorMessage, float min, float max) {
        try {
            float out = Float.parseFloat(optionalText(raw));
            if (!Float.isFinite(out) || out < min || out > max) {
                throw new IllegalArgumentException(errorMessage);
            }
            return out;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static double parseRequiredDouble(String raw, String errorMessage, double min, double max) {
        try {
            double out = Double.parseDouble(optionalText(raw));
            if (!Double.isFinite(out) || out < min || out > max) {
                throw new IllegalArgumentException(errorMessage);
            }
            return out;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    private static Integer parseOptionalInt(String raw) {
        try {
            String value = optionalText(raw);
            if (value.isEmpty()) {
                return null;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Float parseOptionalFloat(String raw) {
        try {
            String value = optionalText(raw);
            if (value.isEmpty()) {
                return null;
            }
            float parsed = Float.parseFloat(value);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Double parseOptionalDouble(String raw) {
        try {
            String value = optionalText(raw);
            if (value.isEmpty()) {
                return null;
            }
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<Arena> snapshotArenas() {
        List<Arena> arenas = new ArrayList<>(ArenaRegistry.getAll());
        arenas.sort(Comparator.comparing(a -> a.arenaId == null ? "" : a.arenaId, String.CASE_INSENSITIVE_ORDER));
        return arenas;
    }

    private static List<String> snapshotBossNames() {
        List<BossDefinition> bosses = new ArrayList<>(BossRegistry.getAll().values());

        List<String> names = new ArrayList<>();
        for (BossDefinition boss : bosses) {
            if (boss != null && boss.bossName != null && !boss.bossName.isBlank()) {
                names.add(boss.bossName);
            }
        }
        return names;
    }

    private static String nextArenaId() {
        int index = 1;
        while (ArenaRegistry.exists("arena" + index)) {
            index++;
        }
        return "arena" + index;
    }

    private static BossDefinition cloneBoss(BossDefinition source) {
        BossDefinition out = new BossDefinition();
        out.bossName = source != null ? source.bossName : "";
        out.npcId = source != null ? source.npcId : "";
        out.tier = source != null ? normalizeTier(source.tier) : "common";
        out.amount = source != null ? source.amount : 1;
        out.levelOverride = source != null ? Math.max(0, source.levelOverride) : 0;

        out.modifiers = new BossDefinition.Modifiers();
        if (source != null && source.modifiers != null) {
            out.modifiers.hp = source.modifiers.hp;
            out.modifiers.damage = source.modifiers.damage;
            out.modifiers.movementSpeed = source.modifiers.movementSpeed;
            out.modifiers.size = source.modifiers.size;
            out.modifiers.attackRate = source.modifiers.attackRate;
            out.modifiers.abilityCooldown = source.modifiers.abilityCooldown;
            out.modifiers.knockbackGiven = source.modifiers.knockbackGiven;
            out.modifiers.knockbackTaken = source.modifiers.knockbackTaken;
            out.modifiers.turnRate = source.modifiers.turnRate;
            out.modifiers.regen = source.modifiers.regen;
        }

        out.perPlayerIncrease = new BossDefinition.PerPlayerIncrease();
        if (source != null && source.perPlayerIncrease != null) {
            out.perPlayerIncrease.hp = source.perPlayerIncrease.hp;
            out.perPlayerIncrease.damage = source.perPlayerIncrease.damage;
            out.perPlayerIncrease.movementSpeed = source.perPlayerIncrease.movementSpeed;
            out.perPlayerIncrease.size = source.perPlayerIncrease.size;
            out.perPlayerIncrease.attackRate = source.perPlayerIncrease.attackRate;
            out.perPlayerIncrease.abilityCooldown = source.perPlayerIncrease.abilityCooldown;
            out.perPlayerIncrease.knockbackGiven = source.perPlayerIncrease.knockbackGiven;
            out.perPlayerIncrease.knockbackTaken = source.perPlayerIncrease.knockbackTaken;
            out.perPlayerIncrease.turnRate = source.perPlayerIncrease.turnRate;
            out.perPlayerIncrease.regen = source.perPlayerIncrease.regen;
        }

        out.extraMobs = new BossDefinition.ExtraMobs();
        if (source != null && source.extraMobs != null) {
            out.extraMobs.npcId = source.extraMobs.npcId;
            out.extraMobs.timeLimitMs = source.extraMobs.timeLimitMs;
            out.extraMobs.waves = source.extraMobs.waves;
            out.extraMobs.mobsPerWave = source.extraMobs.mobsPerWave;
            out.extraMobs.useRandomSpawnLocations = source.extraMobs.useRandomSpawnLocations;
            out.extraMobs.randomSpawnRadius = source.extraMobs.randomSpawnRadius;
            out.extraMobs.bossSpawnTrigger = source.extraMobs.bossSpawnTrigger != null ? source.extraMobs.bossSpawnTrigger : BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_BEFORE_BOSS;
            out.extraMobs.bossSpawnTriggerValue = source.extraMobs.bossSpawnTriggerValue;
            out.extraMobs.timedProximityEnabled = source.extraMobs.timedProximityEnabled;
            out.extraMobs.timedProximityArenaId = source.extraMobs.timedProximityArenaId;
            out.extraMobs.timedProximityRadius = source.extraMobs.timedProximityRadius;
            out.extraMobs.adds = new ArrayList<>();
            if (source.extraMobs.adds != null) {
                for (BossDefinition.ExtraMobs.WaveAdd add : source.extraMobs.adds) {
                    if (add == null) {
                        continue;
                    }
                    BossDefinition.ExtraMobs.WaveAdd copy = new BossDefinition.ExtraMobs.WaveAdd();
                    copy.npcId = add.npcId;
                    copy.mobsPerWave = add.mobsPerWave;
                    copy.everyWave = add.everyWave;
                    copy.hp = add.hp;
                    copy.damage = add.damage;
                    copy.size = add.size;
                    out.extraMobs.adds.add(copy);
                }
            }
            out.extraMobs.scheduledWaves = new ArrayList<>();
            if (source.extraMobs.scheduledWaves != null) {
                for (BossDefinition.ExtraMobs.ScheduledWave wave : source.extraMobs.scheduledWaves) {
                    if (wave == null) {
                        continue;
                    }
                    BossDefinition.ExtraMobs.ScheduledWave waveCopy = new BossDefinition.ExtraMobs.ScheduledWave();
                    waveCopy.trigger = wave.trigger;
                    waveCopy.triggerValue = wave.triggerValue;
                    waveCopy.repeatCount = wave.repeatCount;
                    waveCopy.repeatEverySeconds = wave.repeatEverySeconds;
                    waveCopy.adds = new ArrayList<>();
                    if (wave.adds != null) {
                        for (BossDefinition.ExtraMobs.WaveAdd add : wave.adds) {
                            if (add == null) {
                                continue;
                            }
                            BossDefinition.ExtraMobs.WaveAdd addCopy = new BossDefinition.ExtraMobs.WaveAdd();
                            addCopy.npcId = add.npcId;
                            addCopy.mobsPerWave = add.mobsPerWave;
                            addCopy.everyWave = add.everyWave;
                            addCopy.hp = add.hp;
                            addCopy.damage = add.damage;
                            addCopy.size = add.size;
                            waveCopy.adds.add(addCopy);
                        }
                    }
                    out.extraMobs.scheduledWaves.add(waveCopy);
                }
            }
        }
        out.extraMobs.sanitize();

        return out;
    }

    private static BossWavesSummary buildBossWavesSummary(BossDefinition.ExtraMobs extra) {
        if (extra == null) {
            return new BossWavesSummary("Aucun planning de vagues.", "Appuyez sur Éditer le planning pour ajouter des vagues.", "Lignes : 0");
        }
        extra.sanitize();

        List<WaveScheduleRow> rows = flattenScheduleRows(extra);
        String addLine1 = "Aucun planning de vagues.";
        String addLine2 = "";
        if (!rows.isEmpty()) {
            WaveScheduleRow first = rows.get(0);
            addLine1 = "Ligne 1 : " + toWaveTriggerDisplayName(first.trigger) + " @ " + formatWaveNumber(first.triggerValue)
                    + " | " + formatWaveAdd(first.add);
            if (rows.size() >= 2) {
                WaveScheduleRow second = rows.get(1);
                addLine2 = "Ligne 2 : " + toWaveTriggerDisplayName(second.trigger) + " @ " + formatWaveNumber(second.triggerValue)
                        + " | " + formatWaveAdd(second.add);
                if (rows.size() > 2) {
                    addLine2 = addLine2 + " | +" + (rows.size() - 2) + " lignes de plus";
                }
            }
        }

        List<BossDefinition.ExtraMobs.ScheduledWave> schedule = extra.getResolvedScheduledWaves();
        String meta = "Lignes : " + rows.size();
        if (!schedule.isEmpty()) {
            meta = "Déclencheurs : " + schedule.size() + " | Lignes : " + rows.size();
        }
        meta += " | Aléa : " + (extra.useRandomSpawnLocations ? "Oui" : "Non")
                + " (" + formatWaveNumber(extra.getWaveRandomSpawnRadius()) + "m)";

        return new BossWavesSummary(addLine1, addLine2, meta);
    }

    private static String formatWaveAdd(BossDefinition.ExtraMobs.WaveAdd add) {
        if (add == null) {
            return "";
        }
        String summary = safeText(add.npcId)
                + " x" + Math.max(1, add.mobsPerWave);
        float hp = add.hp > 0f ? add.hp : 1.0f;
        float damage = add.damage > 0f ? add.damage : 1.0f;
        float size = add.size > 0f ? add.size : 1.0f;
        if (Math.abs(hp - 1.0f) > 0.0001f || Math.abs(damage - 1.0f) > 0.0001f || Math.abs(size - 1.0f) > 0.0001f) {
            summary += " [" + formatFloat(hp) + "/" + formatFloat(damage) + "/" + formatFloat(size) + "]";
        }
        return summary;
    }

    private static String formatWaveNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) <= 0.0001d) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static List<LootItem> buildDefaultEarlyZoneLootItems() {
        List<LootItem> out = new ArrayList<>();
        out.add(new LootItem("Ingredient_Fibre", 1.0d, 4, 10));
        out.add(new LootItem("Ingredient_Stick", 0.85d, 2, 6));
        out.add(new LootItem("Ore_Copper", 0.70d, 1, 4));
        out.add(new LootItem("Plant_Fruit_Berries_Red", 0.60d, 2, 5));
        return out;
    }

    private static LootTable cloneLoot(LootTable source, String fallbackBossName) {
        LootTable out = new LootTable();
        out.bossName = source != null && source.bossName != null && !source.bossName.isBlank()
                ? source.bossName
                : fallbackBossName;
        out.lootRadius = source != null ? source.lootRadius : 50.0d;
        out.items = new ArrayList<>();
        out.commands = new ArrayList<>();

        if (source != null && source.items != null) {
            for (LootItem item : source.items) {
                if (item == null) {
                    continue;
                }
                out.items.add(new LootItem(item.itemId, item.dropChance, item.minAmount, item.maxAmount));
            }
        }
        if (source != null && source.commands != null) {
            out.commands.addAll(source.commands);
        }

        return out;
    }

    private static String sanitizeTab(String tab) {
        if (TAB_SHOP.equals(tab)) {
            return TAB_SHOP;
        }
        if (TAB_ARENAS.equals(tab)) {
            return TAB_ARENAS;
        }
        return TAB_BOSSES;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        cmd.append(LAYOUT);

        cmd.set("#TitleLabel.Text", "Config BossArena");
        cmd.set("#SubtitleLabel.Text", "Configurer boss, boutique et arènes");

        boolean bossesTab = TAB_BOSSES.equals(selectedTab);
        boolean shopTab = TAB_SHOP.equals(selectedTab);
        boolean arenasTab = TAB_ARENAS.equals(selectedTab);

        cmd.set("#TabIndicatorBosses.Visible", bossesTab);
        cmd.set("#TabIndicatorShop.Visible", shopTab);
        cmd.set("#TabIndicatorArenas.Visible", arenasTab);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossesTab", EventData.of("Action", "tab_" + TAB_BOSSES));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopTab", EventData.of("Action", "tab_" + TAB_SHOP));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenasTab", EventData.of("Action", "tab_" + TAB_ARENAS));

        cmd.set("#BossesPanel.Visible", bossesTab);
        cmd.set("#ShopPanel.Visible", shopTab);
        cmd.set("#ArenasPanel.Visible", arenasTab);

        cmd.set("#StatusLabel.Visible", false);
        cmd.set("#StatusLabel.Text", "");

        if (bossesTab) {
            buildBossesTab(cmd, events);
        }

        if (shopTab) {
            buildShopTab(ref, store, cmd, events);
        }

        if (arenasTab) {
            buildArenasTab(cmd, events);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull ConfigEventData data) {
        String action = data.action;
        if (action == null || action.isBlank()) {
            return;
        }

        if ("close".equals(action)) {
            close();
            return;
        }

        if (action.startsWith("tab_")) {
            selectedTab = sanitizeTab(action.substring("tab_".length()));
            rebuild();
            return;
        }

        if (TAB_BOSSES.equals(selectedTab)) {
            handleBossesAction(action, data, ref, store);
            return;
        }

        if (TAB_SHOP.equals(selectedTab)) {
            handleShopAction(action, data);
            return;
        }

        if (TAB_ARENAS.equals(selectedTab)) {
            handleArenasAction(action, ref, store, data);
            return;
        }
    }

    private void buildShopTab(Ref<EntityStore> ref,
                              Store<EntityStore> store,
                              UICommandBuilder cmd,
                              UIEventBuilder events) {
        cmd.set("#ShopStatusLabel.Text", shopStatusText == null ? "" : shopStatusText);
        List<ShopLocationView> shopLocations = snapshotShopLocations(ref, store);
        shopRows.clear();

        cmd.set("#ShopOverflowLabel.Visible", shopLocations.size() > MAX_SHOP_ROWS);
        if (shopLocations.size() > MAX_SHOP_ROWS) {
            cmd.set("#ShopOverflowLabel.Text", "+" + (shopLocations.size() - MAX_SHOP_ROWS) + " emplacements boutique non affichés sur cette page.");
        } else {
            cmd.set("#ShopOverflowLabel.Text", "");
        }

        cmd.set("#ShopEmptyLabel.Visible", shopLocations.isEmpty());
        if (shopLocations.isEmpty()) {
            cmd.set("#ShopEmptyLabel.Text", "Aucun emplacement boutique trouvé pour ce monde.");
        }

        for (int row = 1; row <= MAX_SHOP_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= shopLocations.size();
            cmd.set("#ShopRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            ShopLocationView shopLocation = shopLocations.get(row - 1);
            shopRows.add(new ShopLocationRef(shopLocation.worldName, shopLocation.x, shopLocation.y, shopLocation.z));
            cmd.set("#ShopArena" + suffix + ".Text", shopLocation.arenaLabel);
            cmd.set("#ShopDistance" + suffix + ".Text", shopLocation.distanceLabel);
            cmd.set("#ShopBosses" + suffix + ".Text", Integer.toString(shopLocation.enabledBosses));
            cmd.set("#ShopEntries" + suffix + ".Text", shopLocation.enabledBosses + "/" + shopLocation.totalBosses);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ShopEdit" + suffix,
                    EventData.of("Action", "shop_edit_open_" + row)
            );
        }

        cmd.set("#ShopEditorOverlay.Visible", shopLocationEditorState != null);
        if (shopLocationEditorState != null) {
            buildShopLocationEditorOverlay(cmd, events);
        }
    }

    private List<ShopLocationView> snapshotShopLocations(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String currentWorld = null;
        org.joml.Vector3d playerPosition = null;

        if (player != null) {
            com.hypixel.hytale.server.core.universe.PlayerRef _pr = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
            if (_pr != null) {
                com.hypixel.hytale.server.core.universe.world.World _w = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(_pr.getWorldUuid());
                currentWorld = _w != null ? _w.getName() : null;
            }
            Object _tc = store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (_tc instanceof com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc) {
                playerPosition = tc.getPosition();
            }
        }

        BossShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null || shopConfig.shops == null || shopConfig.shops.isEmpty()) {
            shopStatusText = "";
            return List.of();
        }

        int totalBosses = snapshotBossNames().size();
        List<ShopLocationView> out = new ArrayList<>();
        for (BossShopConfig.ShopLocation location : shopConfig.shops) {
            if (location == null) {
                continue;
            }
            String locationWorld = optionalText(location.worldName);
            if (locationWorld.isEmpty()) {
                continue;
            }
            if (currentWorld != null && !locationWorld.equalsIgnoreCase(currentWorld)) {
                continue;
            }

            String configuredArenaId = optionalText(location.arenaId);
            Arena nearestArena = configuredArenaId.isEmpty()
                    ? findNearestArenaForShop(locationWorld, location.x, location.y, location.z)
                    : null;
            String resolvedArenaId = configuredArenaId;
            if (resolvedArenaId.isEmpty() && nearestArena != null) {
                resolvedArenaId = optionalText(nearestArena.arenaId);
            }
            int enabledBosses = countEnabledBosses(location.enabledBossIds);

            Double distance = null;
            if (playerPosition != null) {
                double dx = playerPosition.x - location.x;
                double dy = playerPosition.y - location.y;
                double dz = playerPosition.z - location.z;
                distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
            }

            String shopName = optionalText(location.name);
            if (shopName.isEmpty()) {
                String uuid = optionalText(location.uuid);
                if (!uuid.isEmpty()) {
                    shopName = "Boutique " + (uuid.length() > 8 ? uuid.substring(0, 8) : uuid);
                } else {
                    shopName = "Shop";
                }
            }

            String arenaLabel = safeText(shopName) + " | (" + location.x + ", " + location.y + ", " + location.z + ")";
            if (!configuredArenaId.isEmpty()) {
                arenaLabel = arenaLabel + " | " + safeText(configuredArenaId);
            } else if (!resolvedArenaId.isEmpty()) {
                arenaLabel = arenaLabel + " | " + safeText(resolvedArenaId);
            } else {
                arenaLabel = arenaLabel + " | [unset]";
            }
            String distanceLabel = distance != null ? formatDouble(distance) + "m" : "--";
            out.add(new ShopLocationView(
                    arenaLabel,
                    distanceLabel,
                    distance,
                    locationWorld,
                    location.x,
                    location.y,
                    location.z,
                    enabledBosses,
                    totalBosses
            ));
        }

        out.sort(Comparator
                .comparing((ShopLocationView row) -> row.distance == null)
                .thenComparing(row -> row.distance == null ? Double.MAX_VALUE : row.distance)
                .thenComparing(row -> row.arenaLabel.toLowerCase(Locale.ROOT)));

        // Only the shop nearest to the player gets " (nearest)" in the label.
        if (!out.isEmpty() && out.get(0).distance != null) {
            ShopLocationView first = out.get(0);
            out.set(0, new ShopLocationView(
                    first.arenaLabel + " (nearest)",
                    first.distanceLabel,
                    first.distance,
                    first.worldName,
                    first.x,
                    first.y,
                    first.z,
                    first.enabledBosses,
                    first.totalBosses
            ));
        }

        if (currentWorld == null) {
            shopStatusText = "Impossible de résoudre le monde du joueur ; affichage de tous les emplacements boutique.";
        } else {
            shopStatusText = "Emplacements boutique du monde '" + currentWorld + "', les plus proches d'abord.";
        }

        return out;
    }

    private void handleShopAction(String action, ConfigEventData data) {
        if (action == null || action.isBlank()) {
            return;
        }

        if (action.startsWith("shop_edit_open_")) {
            handleShopEditOpen(action.substring("shop_edit_open_".length()));
            return;
        }

        if (action.startsWith("shop_edit_toggle_")) {
            handleShopEditToggle(action.substring("shop_edit_toggle_".length()));
            return;
        }

        if (action.startsWith("shop_edit_price_inc_")) {
            handleShopEditPriceAdjust(action.substring("shop_edit_price_inc_".length()), SHOP_CONTRACT_PRICE_STEP);
            return;
        }

        if (action.startsWith("shop_edit_price_dec_")) {
            handleShopEditPriceAdjust(action.substring("shop_edit_price_dec_".length()), -SHOP_CONTRACT_PRICE_STEP);
            return;
        }

        if ("shop_edit_scroll_up".equals(action)) {
            scrollShopEditList(-1);
            return;
        }

        if ("shop_edit_scroll_down".equals(action)) {
            scrollShopEditList(1);
            return;
        }

        if ("shop_edit_save".equals(action)) {
            handleShopEditSave(data);
            return;
        }

        if ("shop_edit_close".equals(action)) {
            shopLocationEditorState = null;
            rebuild();
        }
    }

    private void handleShopEditOpen(String rowToken) {
        int row = parseRow(rowToken);
        if (row < 1 || row > shopRows.size()) {
            shopStatusText = "Sélection de ligne boutique invalide.";
            rebuild();
            return;
        }

        ShopLocationRef shopLocation = shopRows.get(row - 1);
        BossShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null) {
            shopStatusText = "Config boutique indisponible.";
            rebuild();
            return;
        }

        BossShopConfig.ShopLocation location = shopConfig.getShopLocation(shopLocation.worldName, shopLocation.x, shopLocation.y, shopLocation.z);
        if (location == null) {
            shopStatusText = "L'emplacement boutique sélectionné n'existe plus dans la config.";
            rebuild();
            return;
        }

        List<String> orderedBosses = snapshotBossNames();
        Set<String> enabled = new LinkedHashSet<>();
        Map<String, Integer> contractPrices = new LinkedHashMap<>();
        if (location.enabledBossIds != null) {
            for (String bossId : location.enabledBossIds) {
                if (bossId == null || bossId.isBlank()) {
                    continue;
                }
                String resolved = resolveBossNameCaseInsensitive(orderedBosses, bossId);
                if (resolved != null) {
                    enabled.add(resolved);
                }
            }
        }

        if (location.contractPrices != null) {
            for (BossShopConfig.ContractPrice contractPrice : location.contractPrices) {
                if (contractPrice == null || contractPrice.bossId == null || contractPrice.bossId.isBlank()) {
                    continue;
                }
                String resolvedBossName = resolveBossNameCaseInsensitive(orderedBosses, contractPrice.bossId);
                if (resolvedBossName == null) {
                    continue;
                }
                contractPrices.put(resolvedBossName, Math.max(0, contractPrice.cost));
            }
        }

        for (String bossName : orderedBosses) {
            if (bossName == null || bossName.isBlank() || contractPrices.containsKey(bossName)) {
                continue;
            }
            BossDefinition boss = BossRegistry.get(bossName);
            String tier = boss != null ? normalizeTier(boss.tier) : "common";
            int fallbackCost = resolveDefaultContractPrice(shopConfig, bossName, tier);
            contractPrices.put(bossName, Math.max(0, fallbackCost));
        }

        shopLocationEditorState = new ShopLocationEditorState(
                shopLocation,
                orderedBosses,
                enabled,
                contractPrices,
                optionalText(location.arenaId)
        );
        shopStatusText = "";
        rebuild();
    }

    private void handleShopEditToggle(String rowToken) {
        if (shopLocationEditorState == null) {
            return;
        }

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_SHOP_BOSS_VISIBLE_ROWS) {
            shopStatusText = "Ligne de bascule boss invalide.";
            rebuild();
            return;
        }

        int bossIndex = shopLocationEditorState.bossListOffset + row - 1;
        if (bossIndex < 0 || bossIndex >= shopLocationEditorState.orderedBossNames.size()) {
            shopStatusText = "Ligne de bascule boss invalide.";
            rebuild();
            return;
        }

        String bossName = shopLocationEditorState.orderedBossNames.get(bossIndex);
        if (shopLocationEditorState.enabledBossNames.contains(bossName)) {
            shopLocationEditorState.enabledBossNames.remove(bossName);
        } else {
            shopLocationEditorState.enabledBossNames.add(bossName);
        }
        rebuild();
    }

    private void handleShopEditPriceAdjust(String rowToken, int delta) {
        if (shopLocationEditorState == null || delta == 0) {
            return;
        }

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_SHOP_BOSS_VISIBLE_ROWS) {
            shopStatusText = "Ligne de prix boss invalide.";
            rebuild();
            return;
        }

        int bossIndex = shopLocationEditorState.bossListOffset + row - 1;
        if (bossIndex < 0 || bossIndex >= shopLocationEditorState.orderedBossNames.size()) {
            shopStatusText = "Ligne de prix boss invalide.";
            rebuild();
            return;
        }

        String bossName = shopLocationEditorState.orderedBossNames.get(bossIndex);
        if (bossName == null || bossName.isBlank()) {
            shopStatusText = "Ligne de prix boss invalide.";
            rebuild();
            return;
        }

        int current = Math.max(0, shopLocationEditorState.contractPriceByBossName.getOrDefault(bossName, 0));
        int next = Math.max(0, current + delta);
        shopLocationEditorState.contractPriceByBossName.put(bossName, next);
        rebuild();
    }

    private void scrollShopEditList(int delta) {
        if (delta == 0 || shopLocationEditorState == null) {
            return;
        }
        int totalBosses = shopLocationEditorState.orderedBossNames.size();
        int maxOffset = Math.max(0, totalBosses - MAX_SHOP_BOSS_VISIBLE_ROWS);
        int nextOffset = Math.max(0, Math.min(maxOffset, shopLocationEditorState.bossListOffset + delta));
        if (nextOffset == shopLocationEditorState.bossListOffset) {
            return;
        }
        shopLocationEditorState.bossListOffset = nextOffset;
        rebuild();
    }

    private void handleShopEditSave(ConfigEventData data) {
        if (shopLocationEditorState == null) {
            return;
        }

        BossShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null) {
            shopStatusText = "Config boutique indisponible.";
            rebuild();
            return;
        }

        ShopLocationRef shopLocation = shopLocationEditorState.shopLocation;
        BossShopConfig.ShopLocation location = shopConfig.getShopLocation(shopLocation.worldName, shopLocation.x, shopLocation.y, shopLocation.z);
        if (location == null) {
            shopStatusText = "L'emplacement boutique sélectionné n'existe plus dans la config.";
            rebuild();
            return;
        }

        List<String> savedBossIds = new ArrayList<>();
        for (String bossName : shopLocationEditorState.orderedBossNames) {
            if (shopLocationEditorState.enabledBossNames.contains(bossName)) {
                savedBossIds.add(bossName);
            }
        }
        String configuredArenaId = resolvedOrFallback(
                data != null ? data.shopEditArenaId : null,
                shopLocationEditorState.arenaId
        );
        shopLocationEditorState.arenaId = configuredArenaId;
        location.arenaId = configuredArenaId;
        location.enabledBossIds = savedBossIds;
        List<BossShopConfig.ContractPrice> savedContractPrices = new ArrayList<>();
        for (String bossName : savedBossIds) {
            if (bossName == null || bossName.isBlank()) {
                continue;
            }
            BossShopConfig.ContractPrice contractPrice = new BossShopConfig.ContractPrice();
            contractPrice.bossId = bossName;
            contractPrice.cost = Math.max(0, shopLocationEditorState.contractPriceByBossName.getOrDefault(bossName, 0));
            savedContractPrices.add(contractPrice);
        }
        location.contractPrices = savedContractPrices;

        plugin.saveShopConfig();
        String arenaText = configuredArenaId.isEmpty() ? "nearest arena (auto)" : configuredArenaId;
        shopStatusText = "Emplacement boutique enregistré (" + shopLocation.x + ", " + shopLocation.y + ", " + shopLocation.z + ") avec arène '" + arenaText + "' et " + savedContractPrices.size() + " surcharge(s) de prix.";
        shopLocationEditorState = null;
        rebuild();
    }

    private void buildShopLocationEditorOverlay(UICommandBuilder cmd, UIEventBuilder events) {
        ShopLocationEditorState state = shopLocationEditorState;
        if (state == null) {
            return;
        }

        ShopLocationRef shopLocation = state.shopLocation;
        cmd.set(
                "#ShopEditorTitle.Text",
                "Boutique (" + shopLocation.x + ", " + shopLocation.y + ", " + shopLocation.z + ")  [" + safeText(shopLocation.worldName) + "]"
        );
        cmd.set(
                "#ShopEditorHint.Text",
                "Définissez l'arène, activez les boss, et réglez le prix de chaque contrat."
        );
        cmd.set("#ShopEditorArena.Value", optionalText(state.arenaId));

        cmd.set(
                "#ShopEditorOverflowLabel.Visible",
                state.orderedBossNames.size() > MAX_SHOP_BOSS_VISIBLE_ROWS
        );
        if (state.orderedBossNames.size() > MAX_SHOP_BOSS_VISIBLE_ROWS) {
            cmd.set(
                    "#ShopEditorOverflowLabel.Text",
                    "Utilisez le défilement pour voir tous les boss."
            );
        } else {
            cmd.set("#ShopEditorOverflowLabel.Text", "");
        }

        int totalBosses = state.orderedBossNames.size();
        int maxOffset = Math.max(0, totalBosses - MAX_SHOP_BOSS_VISIBLE_ROWS);
        state.bossListOffset = Math.max(0, Math.min(state.bossListOffset, maxOffset));
        boolean scrollable = totalBosses > MAX_SHOP_BOSS_VISIBLE_ROWS;
        boolean canScrollUp = scrollable && state.bossListOffset > 0;
        boolean canScrollDown = scrollable && state.bossListOffset < maxOffset;

        cmd.set("#ShopEditScrollUp.Visible", canScrollUp);
        cmd.set("#ShopEditScrollDown.Visible", canScrollDown);
        cmd.set("#ShopEditScrollTrack.Visible", scrollable);
        cmd.set("#ShopEditScrollPageLabel.Visible", scrollable);

        int scrollThumbStep = resolveShopEditScrollThumbStep(state.bossListOffset, maxOffset);
        for (int step = 1; step <= SHOP_EDIT_SCROLL_THUMB_STEPS; step++) {
            cmd.set("#ShopEditScrollThumb" + step + ".Visible", scrollable && step == scrollThumbStep);
        }
        if (scrollable) {
            int start = state.bossListOffset + 1;
            int end = Math.min(totalBosses, state.bossListOffset + MAX_SHOP_BOSS_VISIBLE_ROWS);
            cmd.set("#ShopEditScrollPageLabel.Text", start + "-" + end + " / " + totalBosses);
        } else {
            cmd.set("#ShopEditScrollPageLabel.Text", "");
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopEditScrollUp", EventData.of("Action", "shop_edit_scroll_up"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopEditScrollDown", EventData.of("Action", "shop_edit_scroll_down"));

        for (int row = 1; row <= MAX_SHOP_BOSS_ROWS; row++) {
            String suffix = Integer.toString(row);
            int bossIndex = state.bossListOffset + row - 1;
            boolean visible = row <= MAX_SHOP_BOSS_VISIBLE_ROWS && bossIndex < totalBosses;
            cmd.set("#ShopEditBossRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            String bossName = state.orderedBossNames.get(bossIndex);
            BossDefinition boss = BossRegistry.get(bossName);
            String tierText = boss != null ? normalizeTier(boss.tier) : "common";
            boolean enabled = state.enabledBossNames.contains(bossName);
            int contractPrice = Math.max(0, state.contractPriceByBossName.getOrDefault(
                    bossName,
                    resolveDefaultContractPrice(plugin.getShopConfig(), bossName, tierText)
            ));

            cmd.set("#ShopEditBossName" + suffix + ".Text", safeText(bossName));
            cmd.set("#ShopEditBossTier" + suffix + ".Text", toTierDisplayName(tierText));
            cmd.set("#ShopEditBossToggle" + suffix + ".Text", enabled ? "Oui" : "Non");
            cmd.set("#ShopEditBossPrice" + suffix + ".Text", Integer.toString(contractPrice));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ShopEditBossToggle" + suffix,
                    EventData.of("Action", "shop_edit_toggle_" + row)
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ShopEditBossPriceUp" + suffix,
                    EventData.of("Action", "shop_edit_price_inc_" + row)
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ShopEditBossPriceDown" + suffix,
                    EventData.of("Action", "shop_edit_price_dec_" + row)
            );
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ShopEditorSaveButton",
                new EventData()
                        .append("Action", "shop_edit_save")
                        .append("@ShopEditArenaId", "#ShopEditorArena.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopEditorCloseButton", EventData.of("Action", "shop_edit_close"));
    }

    private void handleArenasAction(String action,
                                    Ref<EntityStore> ref,
                                    Store<EntityStore> store,
                                    ConfigEventData data) {
        if ("arena_add_here".equals(action)) {
            addArenaAtPlayerPosition(ref, store);
            return;
        }

        if (action.startsWith("arena_delete_")) {
            handleArenaDelete(action.substring("arena_delete_".length()));
            return;
        }

        if (action.startsWith("arena_save_")) {
            handleArenaSave(action.substring("arena_save_".length()), data);
            return;
        }

    }

    private void buildArenasTab(UICommandBuilder cmd, UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaAddButton", EventData.of("Action", "arena_add_here"));
        cmd.set("#ArenaStatusLabel.Text", arenaStatusText == null ? "" : arenaStatusText);

        List<Arena> arenas = snapshotArenas();
        arenaRows.clear();

        cmd.set("#ArenaOverflowLabel.Visible", arenas.size() > MAX_ARENA_ROWS);
        if (arenas.size() > MAX_ARENA_ROWS) {
            cmd.set("#ArenaOverflowLabel.Text", "+" + (arenas.size() - MAX_ARENA_ROWS) + " arènes non affichées sur cette page.");
        } else {
            cmd.set("#ArenaOverflowLabel.Text", "");
        }

        cmd.set("#ArenaEmptyLabel.Visible", arenas.isEmpty());
        if (arenas.isEmpty()) {
            cmd.set("#ArenaEmptyLabel.Text", "Aucune arène. Appuyez sur + pour en ajouter une à votre position.");
        }

        for (int row = 1; row <= MAX_ARENA_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= arenas.size();
            cmd.set("#ArenaRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            Arena arena = arenas.get(row - 1);
            arenaRows.add(arena.arenaId);

            cmd.set("#ArenaName" + suffix + ".Value", arena.arenaId == null ? "" : arena.arenaId);
            cmd.set("#ArenaX" + suffix + ".Value", formatCoord(arena.x));
            cmd.set("#ArenaY" + suffix + ".Value", formatCoord(arena.y));
            cmd.set("#ArenaZ" + suffix + ".Value", formatCoord(arena.z));
            cmd.set("#ArenaLootRadius" + suffix + ".Value", arena.lootRadius > 0.0d ? formatCoord(arena.lootRadius) : "");

            events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaDelete" + suffix, EventData.of("Action", "arena_delete_" + row));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ArenaSave" + suffix,
                    new EventData()
                            .append("Action", "arena_save_" + row)
                            .append("@ArenaName", "#ArenaName" + suffix + ".Value")
                            .append("@ArenaX", "#ArenaX" + suffix + ".Value")
                            .append("@ArenaY", "#ArenaY" + suffix + ".Value")
                            .append("@ArenaZ", "#ArenaZ" + suffix + ".Value")
                            .append("@ArenaLootRadius" + suffix, "#ArenaLootRadius" + suffix + ".Value")
            );
        }
    }

    private void addArenaAtPlayerPosition(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            arenaStatusText = "Impossible de résoudre le joueur ; arène non ajoutée.";
            rebuild();
            return;
        }

        com.hypixel.hytale.server.core.universe.PlayerRef _aPR = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        World world = _aPR != null ? com.hypixel.hytale.server.core.universe.Universe.get().getWorld(_aPR.getWorldUuid()) : null;
        Object _aTc = store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
        org.joml.Vector3d rawPosition = _aTc instanceof com.hypixel.hytale.server.core.modules.entity.component.TransformComponent _atcr ? _atcr.getPosition() : null;
        if (world == null || rawPosition == null) {
            arenaStatusText = "Impossible de résoudre le monde/position ; arène non ajoutée.";
            rebuild();
            return;
        }
        Vector3d position = VecUtil.toJoml(rawPosition);

        String arenaId = nextArenaId();
        Arena arena = new Arena(arenaId, world.getName(), position);
        ArenaRegistry.register(arena);
        plugin.saveArenas();

        arenaStatusText = "Arène '" + arenaId + "' ajoutée à " + formatCoord(position.x) + ", " + formatCoord(position.y) + ", " + formatCoord(position.z) + ".";
        rebuild();
    }

    private void handleArenaDelete(String rowToken) {
        int row = parseRow(rowToken);
        if (row < 1 || row > arenaRows.size()) {
            arenaStatusText = "Sélection de ligne arène invalide.";
            rebuild();
            return;
        }

        String arenaId = arenaRows.get(row - 1);
        Arena removed = ArenaRegistry.remove(arenaId);
        if (removed == null) {
            arenaStatusText = "L'arène '" + arenaId + "' n'existe plus.";
            rebuild();
            return;
        }

        plugin.saveArenas();
        arenaStatusText = "Arène '" + arenaId + "' supprimée.";
        rebuild();
    }

    private void handleArenaSave(String rowToken, ConfigEventData data) {
        int row = parseRow(rowToken);
        if (row < 1 || row > arenaRows.size()) {
            arenaStatusText = "Sélection de ligne arène invalide.";
            rebuild();
            return;
        }

        Arena arena = ArenaRegistry.get(arenaRows.get(row - 1));
        if (arena == null) {
            arenaStatusText = "L'arène sélectionnée n'existe plus.";
            rebuild();
            return;
        }

        String requestedId = normalizeArenaId(data.arenaName);
        if (requestedId.isEmpty()) {
            arenaStatusText = "Le nom d'arène ne peut pas être vide.";
            rebuild();
            return;
        }

        if (!ARENA_ID_PATTERN.matcher(requestedId).matches()) {
            arenaStatusText = "Le nom d'arène ne peut utiliser que lettres, chiffres, '_' ou '-'.";
            rebuild();
            return;
        }

        Double x = parseCoordinate(data.arenaX);
        Double y = parseCoordinate(data.arenaY);
        Double z = parseCoordinate(data.arenaZ);
        if (x == null || y == null || z == null) {
            arenaStatusText = "Les coordonnées doivent être des nombres valides.";
            rebuild();
            return;
        }

        String lootRadiusRaw = optionalText(data.getArenaLootRadius(row));
        double lootRadius = arena.lootRadius;
        if (!lootRadiusRaw.isEmpty()) {
            Double parsed = parseOptionalDouble(lootRadiusRaw);
            lootRadius = (parsed != null && parsed >= 0.0d) ? parsed : 0.0d;
        }

        String oldArenaId = arena.arenaId;
        boolean nameChanged = oldArenaId == null || !oldArenaId.equalsIgnoreCase(requestedId);
        if (nameChanged && ArenaRegistry.exists(requestedId)) {
            arenaStatusText = "L'arène '" + requestedId + "' existe déjà.";
            rebuild();
            return;
        }

        if (nameChanged && oldArenaId != null && !oldArenaId.isBlank()) {
            ArenaRegistry.remove(oldArenaId);
        }

        arena.arenaId = requestedId;
        arena.x = x;
        arena.y = y;
        arena.z = z;
        arena.lootRadius = lootRadius;
        ArenaRegistry.register(arena);

        plugin.saveArenas();
        arenaStatusText = "Arène '" + arena.arenaId + "' enregistrée.";
        rebuild();
    }

    private void handleBossesAction(String action,
                                    ConfigEventData data,
                                    Ref<EntityStore> ref,
                                    Store<EntityStore> store) {
        if ("boss_add_open".equals(action)) {
            openNewBossEditor();
            rebuild();
            return;
        }

        if ("boss_editor_close".equals(action)) {
            bossEditorState = null;
            bossWavesOverlayOpen = false;
            bossScalersOverlayOpen = false;
            bossNpcPicksOpen = false;
            bossStatusText = "";
            rebuild();
            return;
        }

        if ("boss_timed_open".equals(action)) {
            bossTimedOverlayOpen = true;
            rebuild();
            return;
        }

        if ("boss_timed_close".equals(action)) {
            bossTimedOverlayOpen = false;
            rebuild();
            return;
        }

        if ("boss_timed_save".equals(action)) {
            handleBossTimedSave(data);
            return;
        }

        if ("boss_waves_open".equals(action)) {
            if (bossEditorState != null) {
                bossWavesOverlayOpen = true;
                bossScalersOverlayOpen = false;
                waveNpcPicksOpen = false;
                waveNpcPicksRow = 0;
                waveNpcSearchQuery = "";
                rebuild();
            }
            return;
        }

        if ("boss_waves_close".equals(action)) {
            bossWavesOverlayOpen = false;
            waveNpcPicksOpen = false;
            waveNpcPicksRow = 0;
            waveNpcSearchQuery = "";
            rebuild();
            return;
        }

        if (action.startsWith("boss_wave_add_row_")) {
            handleBossWaveAddRow(action.substring("boss_wave_add_row_".length()), data);
            return;
        }

        if (action.startsWith("boss_wave_delete_")) {
            handleBossWaveDelete(action.substring("boss_wave_delete_".length()), data);
            return;
        }

        if ("boss_waves_save".equals(action)) {
            handleBossWavesSave(data);
            return;
        }

        if ("boss_spawn_after_pre".equals(action) || "boss_spawn_after_delay".equals(action)) {
            handleBossSpawnTriggerMode(action, data);
            return;
        }

        if ("boss_wave_random_toggle".equals(action) || "boss_wave_proximity_toggle".equals(action)) {
            handleBossWaveToggle(action, data);
            return;
        }

        if (action.startsWith("boss_wave_npc_pick_")) {
            handleBossWaveNpcPick(action.substring("boss_wave_npc_pick_".length()), data);
            return;
        }

        if (action.startsWith("boss_wave_npc_filter_")) {
            handleBossWaveNpcFilter(action.substring("boss_wave_npc_filter_".length()), data);
            return;
        }

        if ("boss_wave_npc_filter".equals(action)) {
            // Legacy single action: ignore (per-row filter is used).
            return;
        }

        if ("boss_npc_filter".equals(action)) {
            handleBossNpcFilter(data);
            return;
        }

        if (action.startsWith("boss_npc_pick_")) {
            handleBossNpcPick(action.substring("boss_npc_pick_".length()), data);
            return;
        }

        if ("boss_tier_changed".equals(action)) {
            if (bossEditorState != null) {
                applyBossEditorDraft(data);
            }
            return;
        }

        if ("boss_slider_changed".equals(action)) {
            if (bossEditorState != null) {
                applyBossEditorDraft(data);
                softUpdateBossSliderLabels();
            }
            return;
        }

        if (action.startsWith("boss_tier_")) {
            handleBossTierSelect(action.substring("boss_tier_".length()), data);
            return;
        }

        if ("boss_scalers_open".equals(action)) {
            if (bossEditorState != null) {
                bossScalersOverlayOpen = true;
                bossWavesOverlayOpen = false;
                rebuild();
            }
            return;
        }

        if ("boss_scalers_close".equals(action)) {
            bossScalersOverlayOpen = false;
            rebuild();
            return;
        }

        if ("boss_scalers_save".equals(action)) {
            handleBossScalersSave(data);
            return;
        }

        if (action.startsWith("boss_loot_add_row_")) {
            handleBossLootAddRow(action.substring("boss_loot_add_row_".length()), data);
            return;
        }

        if (action.startsWith("boss_loot_delete_")) {
            handleBossLootDelete(action.substring("boss_loot_delete_".length()), data);
            return;
        }

        if ("boss_scroll_up".equals(action)) {
            scrollBossList(-1);
            return;
        }

        if ("boss_scroll_down".equals(action)) {
            scrollBossList(1);
            return;
        }

        if (action.startsWith("boss_open_")) {
            handleBossOpen(action.substring("boss_open_".length()));
            return;
        }

        if (action.startsWith("boss_delete_")) {
            handleBossDelete(action.substring("boss_delete_".length()));
            return;
        }

        if ("boss_editor_save".equals(action)) {
            handleBossEditorSave(data);
        }
    }

    private void buildBossesTab(UICommandBuilder cmd, UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossAddButton", EventData.of("Action", "boss_add_open"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossTimedButton", EventData.of("Action", "boss_timed_open"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossScrollUp", EventData.of("Action", "boss_scroll_up"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossScrollDown", EventData.of("Action", "boss_scroll_down"));
        cmd.set("#BossStatusLabel.Text", bossStatusText == null ? "" : bossStatusText);

        List<String> bosses = snapshotBossNames();
        bossRows.clear();

        int totalBosses = bosses.size();
        int maxOffset = Math.max(0, totalBosses - MAX_BOSS_ROWS);
        bossListOffset = Math.max(0, Math.min(bossListOffset, maxOffset));
        boolean scrollable = totalBosses > MAX_BOSS_ROWS;
        boolean canScrollUp = scrollable && bossListOffset > 0;
        boolean canScrollDown = scrollable && bossListOffset < maxOffset;

        cmd.set("#BossScrollUp.Visible", canScrollUp);
        cmd.set("#BossScrollDown.Visible", canScrollDown);
        cmd.set("#BossScrollTrack.Visible", scrollable);
        cmd.set("#BossScrollPageLabel.Visible", scrollable);

        int scrollThumbStep = resolveBossScrollThumbStep(bossListOffset, maxOffset);
        for (int step = 1; step <= BOSS_SCROLL_THUMB_STEPS; step++) {
            cmd.set("#BossScrollThumb" + step + ".Visible", scrollable && step == scrollThumbStep);
        }

        if (scrollable) {
            int start = bossListOffset + 1;
            int end = Math.min(totalBosses, bossListOffset + MAX_BOSS_ROWS);
            cmd.set("#BossScrollPageLabel.Text", start + "-" + end + " / " + totalBosses);
            cmd.set("#BossOverflowLabel.Visible", true);
            cmd.set("#BossOverflowLabel.Text", "Utilisez le défilement pour voir tous les boss.");
        } else {
            cmd.set("#BossOverflowLabel.Visible", false);
            cmd.set("#BossOverflowLabel.Text", "");
            cmd.set("#BossScrollPageLabel.Text", "");
        }

        cmd.set("#BossEmptyLabel.Visible", bosses.isEmpty());
        if (bosses.isEmpty()) {
            cmd.set("#BossEmptyLabel.Text", "Aucun boss. Appuyez sur Ajouter un boss pour en créer un.");
        }

        for (int row = 1; row <= MAX_BOSS_ROWS; row++) {
            String suffix = Integer.toString(row);
            int bossIndex = bossListOffset + row - 1;
            boolean visible = bossIndex < totalBosses;
            cmd.set("#BossRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            String bossName = bosses.get(bossIndex);
            BossDefinition boss = BossRegistry.get(bossName);
            bossRows.add(bossName);

            cmd.set("#BossName" + suffix + ".Text", safeText(boss != null ? boss.bossName : bossName));
            cmd.set("#BossNpc" + suffix + ".Text", safeText(boss != null ? boss.npcId : ""));
            cmd.set("#BossAmount" + suffix + ".Text", Integer.toString(boss != null ? boss.amount : 0));

            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossOpen" + suffix, EventData.of("Action", "boss_open_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossDelete" + suffix, EventData.of("Action", "boss_delete_" + row));
        }

        if (bossEditorState == null) {
            cmd.set("#BossEditorOverlay.Visible", false);
        } else {
            cmd.set("#BossEditorOverlay.Visible", true);
            buildBossEditorOverlay(cmd, events);
        }

        cmd.set("#BossTimedOverlay.Visible", bossTimedOverlayOpen);
        if (bossTimedOverlayOpen) {
            buildBossTimedOverlay(cmd, events);
        }
    }

    private void scrollBossList(int delta) {
        if (delta == 0) {
            return;
        }

        int totalBosses = snapshotBossNames().size();
        int maxOffset = Math.max(0, totalBosses - MAX_BOSS_ROWS);
        int nextOffset = Math.max(0, Math.min(maxOffset, bossListOffset + delta));
        bossListOffset = nextOffset;
        rebuild();
    }

    private void buildBossEditorOverlay(UICommandBuilder cmd, UIEventBuilder events) {
        BossDefinition boss = bossEditorState.boss;
        LootTable loot = bossEditorState.loot;

        cmd.set("#BossEditorTitle.Text", "Édition : " + safeText(boss.bossName));

        cmd.set("#BossEditName.Value", safeText(boss.bossName));
        cmd.set("#BossEditNpcId.Value", safeText(boss.npcId));
        String tier = normalizeTier(boss.tier);
        cmd.set("#BossEditTier.Value", tier);
        cmd.set("#BossEditAmount.Value", Integer.toString(Math.max(1, boss.amount)));
        cmd.set("#BossEditLevelOverride.Value", boss.levelOverride >= 1 ? Integer.toString(boss.levelOverride) : "");

        float hpMult = BossArenaConfigUiControls.clampFloat(boss.modifiers.hp, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
        float dmgMult = BossArenaConfigUiControls.clampFloat(boss.modifiers.damage, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
        float sizeMult = BossArenaConfigUiControls.clampFloat(boss.modifiers.size, MULT_SIZE_MIN, MULT_SIZE_MAX);
        float ppHp = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.hp, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppDmg = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.damage, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppSize = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.size, MULT_PERS_MIN, MULT_PERS_MAX);

        cmd.set("#BossEditHp.Value", hpMult);
        cmd.set("#BossEditDamage.Value", dmgMult);
        cmd.set("#BossEditSize.Value", sizeMult);
        cmd.set("#BossEditPpHp.Value", ppHp);
        cmd.set("#BossEditPpDamage.Value", ppDmg);
        cmd.set("#BossEditPpSize.Value", ppSize);
        cmd.set("#BossEditHpValue.Text", formatFloat(hpMult));
        cmd.set("#BossEditDamageValue.Text", formatFloat(dmgMult));
        cmd.set("#BossEditSizeValue.Text", formatFloat(sizeMult));
        cmd.set("#BossEditPpHpValue.Text", formatFloat(ppHp));
        cmd.set("#BossEditPpDamageValue.Text", formatFloat(ppDmg));
        cmd.set("#BossEditPpSizeValue.Text", formatFloat(ppSize));

        cmd.set("#BossEditSpeed.Value", formatFloat(boss.modifiers.movementSpeed));
        cmd.set("#BossEditAttackRate.Value", formatFloat(boss.modifiers.attackRate));
        cmd.set("#BossEditAbilityCooldown.Value", formatFloat(boss.modifiers.abilityCooldown));
        cmd.set("#BossEditKnockbackGiven.Value", formatFloat(boss.modifiers.knockbackGiven));
        cmd.set("#BossEditKnockbackTaken.Value", formatFloat(boss.modifiers.knockbackTaken));
        cmd.set("#BossEditTurnRate.Value", formatFloat(boss.modifiers.turnRate));
        cmd.set("#BossEditRegen.Value", formatFloat(boss.modifiers.regen));
        cmd.set("#BossEditPpSpeed.Value", formatFloat(boss.perPlayerIncrease.movementSpeed));
        cmd.set("#BossEditPpAttackRate.Value", formatFloat(boss.perPlayerIncrease.attackRate));
        cmd.set("#BossEditPpAbilityCooldown.Value", formatFloat(boss.perPlayerIncrease.abilityCooldown));
        cmd.set("#BossEditPpKnockbackGiven.Value", formatFloat(boss.perPlayerIncrease.knockbackGiven));
        cmd.set("#BossEditPpKnockbackTaken.Value", formatFloat(boss.perPlayerIncrease.knockbackTaken));
        cmd.set("#BossEditPpTurnRate.Value", formatFloat(boss.perPlayerIncrease.turnRate));
        cmd.set("#BossEditPpRegen.Value", formatFloat(boss.perPlayerIncrease.regen));

        applyBossNpcPicks(cmd, events, true);
        applyBossEditorScrollForNpcPicks(cmd);

        if (boss.extraMobs != null) {
            boss.extraMobs.sanitize();
        }
        BossWavesSummary summary = buildBossWavesSummary(boss.extraMobs);
        cmd.set("#BossWavesAdd1.Text", summary.addLine1);
        cmd.set("#BossWavesAdd2.Text", summary.addLine2);
        cmd.set("#BossWavesMeta.Text", "");

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossEditorCloseButton", EventData.of("Action", "boss_editor_close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossEditWavesButton", EventData.of("Action", "boss_waves_open"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossEditScalersButton", EventData.of("Action", "boss_scalers_open"));
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditNpcId",
                buildBossEditorSnapshotEvent("boss_npc_filter"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditTier",
                buildBossEditorSnapshotEvent("boss_tier_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditHp",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditDamage",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditSize",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpHp",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpDamage",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpSize",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );

        List<LootItem> items = loot.items != null ? loot.items : List.of();
        int visibleRows = Math.max(1, Math.min(MAX_LOOT_VISIBLE_ROWS, items.size() + 1));

        for (int row = 1; row <= MAX_LOOT_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= visibleRows;
            cmd.set("#BossLootRow" + suffix + ".Visible", visible);

            if (!visible) {
                continue;
            }

            LootItem item = row <= items.size() ? items.get(row - 1) : null;
            if (item == null) {
                cmd.set("#BossLootName" + suffix + ".Value", "");
                cmd.set("#BossLootMin" + suffix + ".Value", "1");
                cmd.set("#BossLootMax" + suffix + ".Value", "1");
                cmd.set("#BossLootChance" + suffix + ".Value", "0.3");
            } else {
                cmd.set("#BossLootName" + suffix + ".Value", safeText(item.itemId));
                cmd.set("#BossLootMin" + suffix + ".Value", Integer.toString(Math.max(1, item.minAmount)));
                cmd.set("#BossLootMax" + suffix + ".Value", Integer.toString(Math.max(Math.max(1, item.minAmount), item.maxAmount)));
                cmd.set("#BossLootChance" + suffix + ".Value", formatChance(clamp(item.dropChance, 0.0d, 1.0d)));
            }

            boolean populated = row <= items.size();
            cmd.set("#BossLootDelete" + suffix + ".Text", populated ? "-" : "+");
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#BossLootDelete" + suffix,
                    buildBossEditorSnapshotEvent(populated
                            ? "boss_loot_delete_" + row
                            : "boss_loot_add_row_" + row));
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditorSaveButton",
                buildBossEditorSnapshotEvent("boss_editor_save")
        );

        cmd.set("#BossWavesOverlay.Visible", bossWavesOverlayOpen);
        if (bossWavesOverlayOpen) {
            BossDefinition.ExtraMobs extra = boss.extraMobs != null ? boss.extraMobs : new BossDefinition.ExtraMobs();
            extra.sanitize();
            boolean afterDelay = BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_SECONDS.equals(extra.bossSpawnTrigger);
            cmd.set("#BossSpawnTrigger.Value", toBossSpawnTriggerDisplayName(extra.bossSpawnTrigger));
            cmd.set("#BossSpawnTriggerValue.Value", formatWaveNumber(extra.bossSpawnTriggerValue));
            cmd.set("#BossSpawnTriggerValue.Visible", afterDelay);
            cmd.set("#BossSpawnTriggerValueLabel.Visible", afterDelay);
            BossArenaConfigUiControls.styleModeTextButton(cmd, "#BossSpawnAfterPreBoss", !afterDelay);
            BossArenaConfigUiControls.styleModeTextButton(cmd, "#BossSpawnAfterDelay", afterDelay);

            cmd.set("#BossWaveRandomLocations.Value", extra.useRandomSpawnLocations ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossWaveRandomToggle", extra.useRandomSpawnLocations);
            cmd.set("#BossWaveRandomRadius.Value", formatWaveNumber(extra.getWaveRandomSpawnRadius()));
            cmd.set("#BossTimedProximityEnabled.Value", extra.timedProximityEnabled ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossTimedProximityToggle", extra.timedProximityEnabled);
            cmd.set("#BossTimedProximityArena.Entries", arenaDropdownEntries());
            cmd.set("#BossTimedProximityArena.Value", safeText(extra.timedProximityArenaId));
            cmd.set("#BossTimedProximityRadius.Value", formatWaveNumber(extra.getTimedProximityRadius()));
            cmd.set("#BossTimedProximityCooldown.Value", Long.toString(extra.getTimedProximityCooldownSeconds(60L)));
            List<WaveScheduleRow> scheduleRows = flattenScheduleRows(extra);
            boolean noWavesConfigured = scheduleRows.isEmpty();
            int visibleWaveRows;
            if (noWavesConfigured) {
                visibleWaveRows = 1;
            } else {
                visibleWaveRows = Math.max(1, Math.min(MAX_WAVE_ADD_ROWS, scheduleRows.size() + 1));
            }

            for (int row = 1; row <= MAX_WAVE_ADD_ROWS; row++) {
                String suffix = Integer.toString(row);
                boolean visible = row <= visibleWaveRows;
                cmd.set("#BossWavesRow" + suffix + ".Visible", visible);

                if (!visible) {
                    continue;
                }

                String npcId;
                float hp;
                float damage;
                float size;
                WaveScheduleRow scheduleRow = row <= scheduleRows.size() ? scheduleRows.get(row - 1) : null;
                if (scheduleRow == null) {
                    if (noWavesConfigured) {
                        cmd.set("#BossWaveEvery" + suffix + ".Value", "Après spawn");
                        cmd.set("#BossWaveValue" + suffix + ".Value", "");
                        cmd.set("#BossWaveRepeatCount" + suffix + ".Value", "1");
                        cmd.set("#BossWaveRepeatSec" + suffix + ".Value", "0");
                        npcId = "";
                        cmd.set("#BossWaveAmount" + suffix + ".Value", "1");
                        hp = 1.0f;
                        damage = 1.0f;
                        size = 1.0f;
                    } else {
                        cmd.set("#BossWaveEvery" + suffix + ".Value", defaultWaveTriggerInput(row, false));
                        cmd.set("#BossWaveValue" + suffix + ".Value", Integer.toString(defaultWaveValueSeconds(row, false)));
                        cmd.set("#BossWaveRepeatCount" + suffix + ".Value", "1");
                        cmd.set("#BossWaveRepeatSec" + suffix + ".Value", "0");
                        npcId = defaultWaveNpcId(row, false);
                        cmd.set("#BossWaveAmount" + suffix + ".Value", "1");
                        hp = 1.0f;
                        damage = 1.0f;
                        size = 1.0f;
                    }
                } else {
                    BossDefinition.ExtraMobs.WaveAdd add = scheduleRow.add;
                    cmd.set("#BossWaveEvery" + suffix + ".Value", toWaveTriggerDisplayName(scheduleRow.trigger));
                    cmd.set("#BossWaveValue" + suffix + ".Value", formatWaveNumber(scheduleRow.triggerValue));
                    cmd.set("#BossWaveRepeatCount" + suffix + ".Value", Integer.toString(scheduleRow.repeatCount));
                    cmd.set("#BossWaveRepeatSec" + suffix + ".Value", formatWaveNumber(scheduleRow.repeatEverySeconds));
                    npcId = safeText(add != null ? add.npcId : "");
                    cmd.set("#BossWaveAmount" + suffix + ".Value", Integer.toString(Math.max(1, add != null ? add.mobsPerWave : 1)));
                    hp = add != null && add.hp > 0f ? add.hp : 1.0f;
                    damage = add != null && add.damage > 0f ? add.damage : 1.0f;
                    size = add != null && add.size > 0f ? add.size : 1.0f;
                }

                cmd.set("#BossWaveNpc" + suffix + ".Value", npcId);
                hp = BossArenaConfigUiControls.clampFloat(hp, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
                damage = BossArenaConfigUiControls.clampFloat(damage, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
                size = BossArenaConfigUiControls.clampFloat(size, MULT_SIZE_MIN, MULT_SIZE_MAX);
                cmd.set("#BossWaveHp" + suffix + ".Value", formatFloat(hp));
                cmd.set("#BossWaveDamage" + suffix + ".Value", formatFloat(damage));
                cmd.set("#BossWaveSize" + suffix + ".Value", formatFloat(size));

                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#BossWaveNpc" + suffix,
                        buildBossWavesSnapshotEvent("boss_wave_npc_filter_" + row),
                        false
                );

                boolean populated = row <= scheduleRows.size();
                cmd.set("#BossWaveAction" + suffix + ".Text", populated ? "-" : "+");
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#BossWaveAction" + suffix,
                        buildBossWavesSnapshotEvent(populated
                                ? "boss_wave_delete_" + row
                                : "boss_wave_add_row_" + row)
                );
            }

            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossWavesCloseButton", EventData.of("Action", "boss_waves_close"));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossWavesSaveButton",
                    buildBossWavesSnapshotEvent("boss_waves_save")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossSpawnAfterPreBoss",
                    buildBossWavesSnapshotEvent("boss_spawn_after_pre")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossSpawnAfterDelay",
                    buildBossWavesSnapshotEvent("boss_spawn_after_delay")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossWaveRandomToggle",
                    buildBossWavesSnapshotEvent("boss_wave_random_toggle")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossTimedProximityToggle",
                    buildBossWavesSnapshotEvent("boss_wave_proximity_toggle")
            );
            applyWaveNpcPicks(cmd, events, true);
        }

        cmd.set("#BossScalersOverlay.Visible", bossScalersOverlayOpen);
        if (bossScalersOverlayOpen) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossScalersCloseButton", EventData.of("Action", "boss_scalers_close"));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossScalersSaveButton",
                    buildBossEditorSnapshotEvent("boss_scalers_save")
            );
        }
    }

    private void buildBossTimedOverlay(UICommandBuilder cmd, UIEventBuilder events) {
        List<BossArenaConfig.TimedBossSpawn> rows = List.of();
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg != null) {
            rows = cfg.getTimedBossSpawns();
        }

        boolean announceServerWide = false;
        boolean announceWorldWide = false;
        String announceText = BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT;
        if (!rows.isEmpty()) {
            BossArenaConfig.TimedBossSpawn first = rows.get(0);
            if (first != null) {
                announceServerWide = first.announceWorldWide;
                announceWorldWide = first.announceCurrentWorld;
                if (announceServerWide) {
                    announceWorldWide = false;
                }
                announceText = resolvedOrFallback(
                        first.worldAnnouncementText,
                        BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT
                );
            }
        }

        cmd.set("#BossTimedOverflowLabel.Visible", rows.size() > MAX_TIMED_SPAWN_ROWS);
        if (rows.size() > MAX_TIMED_SPAWN_ROWS) {
            cmd.set("#BossTimedOverflowLabel.Text", "+" + (rows.size() - MAX_TIMED_SPAWN_ROWS) + " règles chrono non affichées.");
        } else {
            cmd.set("#BossTimedOverflowLabel.Text", "");
        }

        for (int row = 1; row <= MAX_TIMED_SPAWN_ROWS; row++) {
            String suffix = Integer.toString(row);
            BossArenaConfig.TimedBossSpawn entry = row <= rows.size() ? rows.get(row - 1) : null;

            cmd.set("#TimedEnabled" + suffix + ".Value", entry == null ? "true" : (entry.enabled ? "true" : "false"));
            cmd.set("#TimedBossId" + suffix + ".Value", safeText(entry != null ? entry.bossId : ""));
            cmd.set("#TimedArenaId" + suffix + ".Value", safeText(entry != null ? entry.arenaId : ""));
            cmd.set("#TimedEveryHours" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.spawnIntervalHours) : 1L));
            cmd.set("#TimedEveryMinutes" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.spawnIntervalMinutes) : 0L));
            cmd.set("#TimedPreventDup" + suffix + ".Value", entry == null ? "true" : (entry.preventDuplicateWhileAlive ? "true" : "false"));
            cmd.set("#TimedDespawnHours" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.despawnAfterHours) : 0L));
            cmd.set("#TimedDespawnMinutes" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.despawnAfterMinutes) : 0L));
        }

        cmd.set("#TimedAnnounceServerWide.Value", announceServerWide ? "true" : "false");
        cmd.set("#TimedAnnounceWorldWide.Value", announceWorldWide ? "true" : "false");
        cmd.set("#TimedAnnounceText.Value", announceText);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossTimedCloseButton", EventData.of("Action", "boss_timed_close"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossTimedSaveButton",
                buildBossTimedSnapshotEvent("boss_timed_save")
        );
    }

    private EventData buildBossTimedSnapshotEvent(String action) {
        EventData snapshot = new EventData().append("Action", action);
        for (int row = 1; row <= MAX_TIMED_SPAWN_ROWS; row++) {
            String suffix = Integer.toString(row);
            snapshot
                    .append("@BossWaveEvery" + suffix, "#TimedEnabled" + suffix + ".Value")
                    .append("@BossWaveNpc" + suffix, "#TimedBossId" + suffix + ".Value")
                    .append("@BossWaveValue" + suffix, "#TimedArenaId" + suffix + ".Value")
                    .append("@BossWaveRepeatCount" + suffix, "#TimedEveryHours" + suffix + ".Value")
                    .append("@BossWaveRepeatSec" + suffix, "#TimedEveryMinutes" + suffix + ".Value")
                    .append("@BossWaveHp" + suffix, "#TimedPreventDup" + suffix + ".Value")
                    .append("@BossWaveDamage" + suffix, "#TimedDespawnHours" + suffix + ".Value")
                    .append("@BossWaveSize" + suffix, "#TimedDespawnMinutes" + suffix + ".Value");
        }
        snapshot
                .append("@TimedAnnounceServerWide", "#TimedAnnounceServerWide.Value")
                .append("@TimedAnnounceWorldWide", "#TimedAnnounceWorldWide.Value")
                .append("@TimedAnnounceText", "#TimedAnnounceText.Value");
        return snapshot;
    }

    private void handleBossTimedSave(ConfigEventData data) {
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            bossStatusText = "Handle de config indisponible.";
            rebuild();
            return;
        }

        try {
            List<BossArenaConfig.TimedBossSpawn> out = new ArrayList<>();
            List<BossArenaConfig.TimedBossSpawn> existingRows = cfg.getTimedBossSpawns();
            BossArenaConfig.TimedBossSpawn existingFirst = existingRows.isEmpty() ? null : existingRows.get(0);

            String announceServerRaw = optionalText(data.timedAnnounceServerWide);
            String announceWorldRaw = optionalText(data.timedAnnounceWorldWide);
            String announceTextRaw = optionalText(data.timedAnnounceText);
            if (looksLikeUiBindingExpression(announceServerRaw)) {
                announceServerRaw = "";
            }
            if (looksLikeUiBindingExpression(announceWorldRaw)) {
                announceWorldRaw = "";
            }
            if (looksLikeUiBindingExpression(announceTextRaw)) {
                announceTextRaw = "";
            }

            boolean defaultAnnounceServer = existingFirst != null && existingFirst.announceWorldWide;
            boolean defaultAnnounceWorld = existingFirst != null && existingFirst.announceCurrentWorld;
            String defaultAnnounceText = existingFirst != null
                    ? resolvedOrFallback(existingFirst.worldAnnouncementText, BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT)
                    : BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT;

            Boolean parsedAnnounceServer = announceServerRaw.isEmpty() ? defaultAnnounceServer : parseToggleInput(announceServerRaw);
            if (parsedAnnounceServer == null) {
                throw new IllegalArgumentException("La bascule d'annonce serveur doit être true/false.");
            }
            Boolean parsedAnnounceWorld = announceWorldRaw.isEmpty() ? defaultAnnounceWorld : parseToggleInput(announceWorldRaw);
            if (parsedAnnounceWorld == null) {
                throw new IllegalArgumentException("La bascule d'annonce monde doit être true/false.");
            }
            if (parsedAnnounceServer) {
                parsedAnnounceWorld = false;
            }
            String resolvedAnnounceText = announceTextRaw.isEmpty() ? defaultAnnounceText : announceTextRaw;

            for (int row = 1; row <= MAX_TIMED_SPAWN_ROWS; row++) {
                String enabledText = optionalText(data.getBossWaveEvery(row));
                String bossId = optionalText(data.getBossWaveNpc(row));
                String arenaId = optionalText(data.getBossWaveValue(row));
                String everyHoursText = optionalText(data.getBossWaveRepeatCount(row));
                String everyMinutesText = optionalText(data.getBossWaveRepeatSec(row));
                String preventDupText = optionalText(data.getBossWaveHp(row));
                String despawnHoursText = optionalText(data.getBossWaveDamage(row));
                String despawnMinutesText = optionalText(data.getBossWaveSize(row));

                if (looksLikeUiBindingExpression(enabledText)
                        || looksLikeUiBindingExpression(bossId)
                        || looksLikeUiBindingExpression(arenaId)
                        || looksLikeUiBindingExpression(everyHoursText)
                        || looksLikeUiBindingExpression(everyMinutesText)
                        || looksLikeUiBindingExpression(preventDupText)
                        || looksLikeUiBindingExpression(despawnHoursText)
                        || looksLikeUiBindingExpression(despawnMinutesText)) {
                    continue;
                }

                if (bossId.isEmpty() && arenaId.isEmpty()) {
                    continue;
                }
                if (bossId.isEmpty() || arenaId.isEmpty()) {
                    throw new IllegalArgumentException("Ligne chrono " + row + " : bossId et arenaId sont tous deux requis.");
                }

                Boolean enabled = enabledText.isEmpty() ? Boolean.TRUE : parseToggleInput(enabledText);
                if (enabled == null) {
                    throw new IllegalArgumentException("Ligne chrono " + row + " : Activé doit être true/false.");
                }
                Boolean preventDup = preventDupText.isEmpty() ? Boolean.TRUE : parseToggleInput(preventDupText);
                if (preventDup == null) {
                    throw new IllegalArgumentException("Ligne chrono " + row + " : Sans dup doit être true/false.");
                }

                long everyHours = parseRequiredLong(
                        everyHoursText.isEmpty() ? "1" : everyHoursText,
                        "Ligne chrono " + row + " : heures d'intervalle doit être un entier >= 0.",
                        0L,
                        Long.MAX_VALUE
                );
                long everyMinutes = parseRequiredLong(
                        everyMinutesText.isEmpty() ? "0" : everyMinutesText,
                        "Ligne chrono " + row + " : minutes d'intervalle doit être un entier >= 0.",
                        0L,
                        Long.MAX_VALUE
                );
                if (BossArenaConfig.resolveMinutes(everyHours, everyMinutes) <= 0L) {
                    throw new IllegalArgumentException("Ligne chrono " + row + " : l'intervalle doit être > 0.");
                }

                long despawnHours = parseRequiredLong(
                        despawnHoursText.isEmpty() ? "0" : despawnHoursText,
                        "Ligne chrono " + row + " : heures de despawn doit être un entier >= 0.",
                        0L,
                        Long.MAX_VALUE
                );
                long despawnMinutes = parseRequiredLong(
                        despawnMinutesText.isEmpty() ? "0" : despawnMinutesText,
                        "Ligne chrono " + row + " : minutes de despawn doit être un entier >= 0.",
                        0L,
                        Long.MAX_VALUE
                );

                BossArenaConfig.TimedBossSpawn entry = new BossArenaConfig.TimedBossSpawn();
                entry.id = "";
                entry.enabled = enabled;
                entry.bossId = bossId;
                entry.arenaId = arenaId;
                entry.spawnIntervalHours = everyHours;
                entry.spawnIntervalMinutes = everyMinutes;
                entry.preventDuplicateWhileAlive = preventDup;
                entry.despawnAfterHours = despawnHours;
                entry.despawnAfterMinutes = despawnMinutes;
                entry.announceWorldWide = parsedAnnounceServer;
                entry.announceCurrentWorld = parsedAnnounceWorld;
                entry.worldAnnouncementText = resolvedAnnounceText;
                out.add(entry);
            }

            cfg.timedBossSpawns = out;
            cfg.save();
            plugin.refreshTimedBossSpawns();
            bossTimedOverlayOpen = false;
            bossStatusText = "Règles de spawn chrono enregistrées (" + out.size() + ").";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private EventData buildBossEditorSnapshotEvent(String action) {
        EventData snapshot = new EventData()
                .append("Action", action)
                .append("@BossEditName", "#BossEditName.Value")
                .append("@BossEditNpcId", "#BossEditNpcId.Value")
                .append("@BossEditTier", "#BossEditTier.Value")
                .append("@BossEditAmount", "#BossEditAmount.Value")
                .append("@BossEditLevelOverride", "#BossEditLevelOverride.Value")
                .append("@BossEditHp", "#BossEditHp.Value")
                .append("@BossEditDamage", "#BossEditDamage.Value")
                .append("@BossEditSpeed", "#BossEditSpeed.Value")
                .append("@BossEditSize", "#BossEditSize.Value")
                .append("@BossEditAttackRate", "#BossEditAttackRate.Value")
                .append("@BossEditAbilityCooldown", "#BossEditAbilityCooldown.Value")
                .append("@BossEditKnockbackGiven", "#BossEditKnockbackGiven.Value")
                .append("@BossEditKnockbackTaken", "#BossEditKnockbackTaken.Value")
                .append("@BossEditTurnRate", "#BossEditTurnRate.Value")
                .append("@BossEditRegen", "#BossEditRegen.Value")
                .append("@BossEditPpHp", "#BossEditPpHp.Value")
                .append("@BossEditPpDamage", "#BossEditPpDamage.Value")
                .append("@BossEditPpSpeed", "#BossEditPpSpeed.Value")
                .append("@BossEditPpSize", "#BossEditPpSize.Value")
                .append("@BossEditPpAttackRate", "#BossEditPpAttackRate.Value")
                .append("@BossEditPpAbilityCooldown", "#BossEditPpAbilityCooldown.Value")
                .append("@BossEditPpKnockbackGiven", "#BossEditPpKnockbackGiven.Value")
                .append("@BossEditPpKnockbackTaken", "#BossEditPpKnockbackTaken.Value")
                .append("@BossEditPpTurnRate", "#BossEditPpTurnRate.Value")
                .append("@BossEditPpRegen", "#BossEditPpRegen.Value");

        for (int row = 1; row <= MAX_LOOT_ROWS; row++) {
            String suffix = Integer.toString(row);
            snapshot
                    .append("@BossLootName" + suffix, "#BossLootName" + suffix + ".Value")
                    .append("@BossLootMin" + suffix, "#BossLootMin" + suffix + ".Value")
                    .append("@BossLootMax" + suffix, "#BossLootMax" + suffix + ".Value")
                    .append("@BossLootChance" + suffix, "#BossLootChance" + suffix + ".Value");
        }

        return snapshot;
    }

    private EventData buildBossWavesSnapshotEvent(String action) {
        EventData snapshot = new EventData()
                .append("Action", action)
                .append("@BossSpawnTrigger", "#BossSpawnTrigger.Value")
                .append("@BossSpawnTriggerValue", "#BossSpawnTriggerValue.Value")
                .append("@BossWaveRandomLocations", "#BossWaveRandomLocations.Value")
                .append("@BossWaveRandomRadius", "#BossWaveRandomRadius.Value")
                .append("@BossTimedProximityEnabled", "#BossTimedProximityEnabled.Value")
                .append("@BossTimedProximityArena", "#BossTimedProximityArena.Value")
                .append("@BossTimedProximityRadius", "#BossTimedProximityRadius.Value")
                .append("@BossTimedProximityCooldown", "#BossTimedProximityCooldown.Value");

        for (int row = 1; row <= MAX_WAVE_ADD_ROWS; row++) {
            String suffix = Integer.toString(row);
            snapshot
                    .append("@BossWaveValue" + suffix, "#BossWaveValue" + suffix + ".Value")
                    .append("@BossWaveRepeatCount" + suffix, "#BossWaveRepeatCount" + suffix + ".Value")
                    .append("@BossWaveRepeatSec" + suffix, "#BossWaveRepeatSec" + suffix + ".Value")
                    .append("@BossWaveNpc" + suffix, "#BossWaveNpc" + suffix + ".Value")
                    .append("@BossWaveAmount" + suffix, "#BossWaveAmount" + suffix + ".Value")
                    .append("@BossWaveEvery" + suffix, "#BossWaveEvery" + suffix + ".Value")
                    .append("@BossWaveHp" + suffix, "#BossWaveHp" + suffix + ".Value")
                    .append("@BossWaveDamage" + suffix, "#BossWaveDamage" + suffix + ".Value")
                    .append("@BossWaveSize" + suffix, "#BossWaveSize" + suffix + ".Value");
        }

        return snapshot;
    }

    private void openNewBossEditor() {
        BossDefinition boss = new BossDefinition();
        boss.bossName = nextBossName();
        boss.npcId = "Bat";
        boss.tier = "common";
        boss.amount = 1;

        boss.modifiers = new BossDefinition.Modifiers();
        boss.perPlayerIncrease = new BossDefinition.PerPlayerIncrease();
        boss.extraMobs = new BossDefinition.ExtraMobs();

        LootTable loot = new LootTable(boss.bossName, 40.0d);
        loot.items = buildDefaultEarlyZoneLootItems();

        bossEditorState = new BossEditorState(null, boss, loot);
        bossWavesOverlayOpen = false;
        bossScalersOverlayOpen = false;
        bossNpcPicksOpen = false;
        bossStatusText = "Création d'un nouveau boss.";
    }

    private void handleBossOpen(String rowToken) {
        int row = parseRow(rowToken);
        if (row < 1 || row > bossRows.size()) {
            bossStatusText = "Sélection de ligne boss invalide.";
            rebuild();
            return;
        }

        String bossName = bossRows.get(row - 1);
        BossDefinition sourceBoss = BossRegistry.get(bossName);
        if (sourceBoss == null) {
            bossStatusText = "Le boss sélectionné n'existe plus.";
            rebuild();
            return;
        }

        LootTable sourceLoot = LootRegistry.get(bossName);
        bossEditorState = new BossEditorState(
                sourceBoss.bossName,
                cloneBoss(sourceBoss),
                cloneLoot(sourceLoot, sourceBoss.bossName)
        );
        bossWavesOverlayOpen = false;
        bossScalersOverlayOpen = false;
        bossNpcPicksOpen = false;

        bossStatusText = "Édition du boss '" + sourceBoss.bossName + "'.";
        rebuild();
    }

    private void handleBossDelete(String rowToken) {
        int row = parseRow(rowToken);
        if (row < 1 || row > bossRows.size()) {
            bossStatusText = "Sélection de ligne boss invalide.";
            rebuild();
            return;
        }

        String bossName = bossRows.get(row - 1);
        BossDefinition removedBoss = BossRegistry.remove(bossName);
        LootRegistry.remove(bossName);

        if (removedBoss == null) {
            bossStatusText = "Le boss '" + bossName + "' n'existe plus.";
            rebuild();
            return;
        }

        if (bossEditorState != null
                && bossEditorState.originalBossName != null
                && bossEditorState.originalBossName.equalsIgnoreCase(bossName)) {
            bossEditorState = null;
            bossWavesOverlayOpen = false;
            bossScalersOverlayOpen = false;
        }

        plugin.saveBossDefinitions();
        plugin.saveLootTables();

        bossStatusText = "Boss '" + removedBoss.bossName + "' supprimé.";
        rebuild();
    }

    private void handleBossLootAddRow(String rowToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_LOOT_ROWS) {
            bossStatusText = "Ligne de butin invalide.";
            rebuild();
            return;
        }

        try {
            applyBossEditorDraft(data);
            List<LootItem> rows = snapshotLootFromData(data);
            if (rows.size() >= MAX_LOOT_ROWS) {
                bossStatusText = "Nombre maximum de lignes de butin atteint.";
                rebuild();
                return;
            }
            bossEditorState.loot.items = rows;
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossLootDelete(String rowToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_LOOT_ROWS) {
            bossStatusText = "Ligne de butin invalide.";
            rebuild();
            return;
        }

        try {
            applyBossEditorDraft(data);
            List<LootItem> rows = snapshotLootFromData(data);
            if (row > rows.size()) {
                bossStatusText = "Sélection de ligne de butin invalide.";
                rebuild();
                return;
            }
            rows.remove(row - 1);
            bossEditorState.loot.items = rows;
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossEditorSave(ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        try {
            BossDefinition outBoss = cloneBoss(bossEditorState.boss);

            String bossNameText = resolvedOrFallback(data.bossEditName, outBoss.bossName);
            String npcIdText = resolvedOrFallback(data.bossEditNpcId, outBoss.npcId);
            outBoss.bossName = requireNonBlank(bossNameText, "BossID ne peut pas être vide.");
            outBoss.npcId = requireNonBlank(npcIdText, "ID PNJ ne peut pas être vide.");
            outBoss.tier = normalizeTier(resolvedOrFallback(data.bossEditTier, outBoss.tier));

            outBoss.amount = parseRequiredInt(
                    resolvedOrFallback(data.bossEditAmount, Integer.toString(Math.max(1, outBoss.amount))),
                    "La quantité doit être un entier > 0.",
                    1,
                    Integer.MAX_VALUE
            );
            outBoss.levelOverride = parseBossLevelOverride(
                    resolvedOrFallback(data.bossEditLevelOverride, Integer.toString(Math.max(0, outBoss.levelOverride))),
                    "Surcharge niveau doit être vide/0 pour défaut, ou un entier >= 1."
            );
            outBoss.modifiers.hp = requireSliderFloat(
                    data.bossEditHp,
                    outBoss.modifiers.hp,
                    MULT_HP_DMG_MIN,
                    MULT_HP_DMG_MAX,
                    "Mult PV doit être entre 0.50 et 50.00."
            );
            outBoss.modifiers.damage = requireSliderFloat(
                    data.bossEditDamage,
                    outBoss.modifiers.damage,
                    MULT_HP_DMG_MIN,
                    MULT_HP_DMG_MAX,
                    "Mult Dégâts doit être entre 0.50 et 50.00."
            );
            outBoss.modifiers.movementSpeed = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditSpeed, formatFloat(outBoss.modifiers.movementSpeed)),
                    "Mult vitesse déplacement doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.modifiers.size = requireSliderFloat(
                    data.bossEditSize,
                    outBoss.modifiers.size,
                    MULT_SIZE_MIN,
                    MULT_SIZE_MAX,
                    "Mult Taille doit être entre 0.10 et 10.00."
            );
            outBoss.modifiers.attackRate = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditAttackRate, formatFloat(outBoss.modifiers.attackRate)),
                    "Mult cadence d'attaque doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.modifiers.abilityCooldown = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditAbilityCooldown, formatFloat(outBoss.modifiers.abilityCooldown)),
                    "Mult recharge capacité doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.modifiers.knockbackGiven = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditKnockbackGiven, formatFloat(outBoss.modifiers.knockbackGiven)),
                    "Mult knockback donné doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.modifiers.knockbackTaken = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditKnockbackTaken, formatFloat(outBoss.modifiers.knockbackTaken)),
                    "Mult knockback reçu doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.modifiers.turnRate = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditTurnRate, formatFloat(outBoss.modifiers.turnRate)),
                    "Mult vitesse rotation doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.modifiers.regen = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditRegen, formatFloat(outBoss.modifiers.regen)),
                    "Mult régén doit être un nombre > 0.",
                    Float.MIN_NORMAL,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.hp = requireSliderFloat(
                    data.bossEditPpHp,
                    outBoss.perPlayerIncrease.hp,
                    MULT_PERS_MIN,
                    MULT_PERS_MAX,
                    "PV/joueurs doit être entre 0.00 et 5.00."
            );
            outBoss.perPlayerIncrease.damage = requireSliderFloat(
                    data.bossEditPpDamage,
                    outBoss.perPlayerIncrease.damage,
                    MULT_PERS_MIN,
                    MULT_PERS_MAX,
                    "Dégâts/joueurs doit être entre 0.00 et 5.00."
            );
            outBoss.perPlayerIncrease.movementSpeed = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpSpeed, formatFloat(outBoss.perPlayerIncrease.movementSpeed)),
                    "Vitesse déplacement/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.size = requireSliderFloat(
                    data.bossEditPpSize,
                    outBoss.perPlayerIncrease.size,
                    MULT_PERS_MIN,
                    MULT_PERS_MAX,
                    "Taille/joueurs doit être entre 0.00 et 5.00."
            );
            outBoss.perPlayerIncrease.attackRate = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpAttackRate, formatFloat(outBoss.perPlayerIncrease.attackRate)),
                    "Cadence d'attaque/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.abilityCooldown = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpAbilityCooldown, formatFloat(outBoss.perPlayerIncrease.abilityCooldown)),
                    "Recharge capacité/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.knockbackGiven = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpKnockbackGiven, formatFloat(outBoss.perPlayerIncrease.knockbackGiven)),
                    "Knockback donné/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.knockbackTaken = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpKnockbackTaken, formatFloat(outBoss.perPlayerIncrease.knockbackTaken)),
                    "Knockback reçu/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.turnRate = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpTurnRate, formatFloat(outBoss.perPlayerIncrease.turnRate)),
                    "Vitesse rotation/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            outBoss.perPlayerIncrease.regen = parseRequiredFloat(
                    resolvedOrFallback(data.bossEditPpRegen, formatFloat(outBoss.perPlayerIncrease.regen)),
                    "Régén/joueur doit être un nombre fini.",
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            );
            if (outBoss.extraMobs == null) {
                outBoss.extraMobs = new BossDefinition.ExtraMobs();
            }
            outBoss.extraMobs.waves = parseRequiredInt(
                    resolvedOrFallback(data.bossEditWaves, Integer.toString(Math.max(-1, outBoss.extraMobs.waves))),
                    "Vagues doit être -1 (infini) ou un entier >= 0.",
                    -1,
                    Integer.MAX_VALUE
            );
            outBoss.extraMobs.sanitize();

            LootTable outLoot = new LootTable();
            outLoot.bossName = outBoss.bossName;
            outLoot.lootRadius = 50.0d; // Only used for JSON; effective radius is arena Loot Radius or 50 when spawn "here"
            outLoot.items = new ArrayList<>();

            boolean unresolvedLootBindingDetected = false;
            boolean lootPayloadMissing = true;
            for (int row = 1; row <= MAX_LOOT_ROWS; row++) {
                String rawItemId = data.getBossLootName(row);
                String rawMinText = data.getBossLootMin(row);
                String rawMaxText = data.getBossLootMax(row);
                String rawChanceText = data.getBossLootChance(row);

                if (rawItemId != null || rawMinText != null || rawMaxText != null || rawChanceText != null) {
                    lootPayloadMissing = false;
                }

                String itemId = optionalText(rawItemId);
                String minText = optionalText(rawMinText);
                String maxText = optionalText(rawMaxText);
                String chanceText = optionalText(rawChanceText);

                if (looksLikeUiBindingExpression(itemId)
                        || looksLikeUiBindingExpression(minText)
                        || looksLikeUiBindingExpression(maxText)
                        || looksLikeUiBindingExpression(chanceText)) {
                    unresolvedLootBindingDetected = true;
                    break;
                }

                // Ignore unfinished rows with no item name (the trailing blank input row).
                if (itemId.isEmpty()) {
                    continue;
                }

                int minAmount = parseRequiredInt(minText, "Quantité min butin doit être un entier sur la ligne " + row + ".", 1, Integer.MAX_VALUE);
                int maxAmount = parseRequiredInt(maxText, "Quantité max butin doit être un entier sur la ligne " + row + ".", minAmount, Integer.MAX_VALUE);
                double chance = parseRequiredDouble(chanceText, "Chance de butin doit être entre 0.0 et 1.0 sur la ligne " + row + ".", 0.0d, 1.0d);

                outLoot.items.add(new LootItem(itemId, chance, minAmount, maxAmount));
            }
            if (unresolvedLootBindingDetected || lootPayloadMissing) {
                LootTable fallbackLoot = bossEditorState.loot;
                outLoot.items = new ArrayList<>();
                if (fallbackLoot != null && fallbackLoot.items != null) {
                    for (LootItem item : fallbackLoot.items) {
                        if (item == null) {
                            continue;
                        }
                        outLoot.items.add(new LootItem(item.itemId, item.dropChance, item.minAmount, item.maxAmount));
                    }
                }
            }

            String oldName = bossEditorState.originalBossName;
            boolean nameChanged = oldName != null && !oldName.equalsIgnoreCase(outBoss.bossName);

            if ((oldName == null || nameChanged) && BossRegistry.exists(outBoss.bossName)) {
                bossStatusText = "Le boss '" + outBoss.bossName + "' existe déjà.";
                rebuild();
                return;
            }

            if (nameChanged) {
                BossRegistry.remove(oldName);
                LootRegistry.remove(oldName);
            }

            BossRegistry.register(outBoss);
            LootRegistry.register(outLoot);

            plugin.saveBossDefinitions();
            plugin.saveLootTables();

            bossEditorState = new BossEditorState(outBoss.bossName, cloneBoss(outBoss), cloneLoot(outLoot, outBoss.bossName));
            bossWavesOverlayOpen = false;
            bossScalersOverlayOpen = false;
            bossStatusText = "Boss '" + outBoss.bossName + "' enregistré.";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void applyBossEditorDraft(ConfigEventData data) {
        if (data == null || bossEditorState == null) {
            return;
        }

        BossDefinition boss = bossEditorState.boss;
        LootTable loot = bossEditorState.loot;

        String bossName = optionalText(data.bossEditName);
        if (!bossName.isEmpty() && !looksLikeUiBindingExpression(bossName)) {
            boss.bossName = bossName;
            if (loot != null) {
                loot.bossName = bossName;
            }
        } else if (!bossName.isEmpty()) {
        }

        String npcId = optionalText(data.bossEditNpcId);
        if (!npcId.isEmpty() && !looksLikeUiBindingExpression(npcId)) {
            boss.npcId = npcId;
        } else if (!npcId.isEmpty()) {
        }

        String tier = optionalText(data.bossEditTier);
        if (!tier.isEmpty() && !looksLikeUiBindingExpression(tier)) {
            boss.tier = normalizeTier(tier);
        }

        Integer amount = parseOptionalInt(data.bossEditAmount);
        if (amount != null && amount >= 1) {
            boss.amount = amount;
        }
        if (data.bossEditLevelOverride != null) {
            String levelOverrideText = optionalText(data.bossEditLevelOverride);
            if (!looksLikeUiBindingExpression(levelOverrideText)) {
                if (levelOverrideText.isEmpty()) {
                    boss.levelOverride = 0;
                } else {
                    Integer levelOverride = parseOptionalInt(levelOverrideText);
                    if (levelOverride != null && levelOverride >= 0) {
                        boss.levelOverride = levelOverride;
                    }
                }
            }
        }

        if (data.bossEditHp != null && Float.isFinite(data.bossEditHp)) {
            boss.modifiers.hp = BossArenaConfigUiControls.clampFloat(data.bossEditHp, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
        }
        if (data.bossEditDamage != null && Float.isFinite(data.bossEditDamage)) {
            boss.modifiers.damage = BossArenaConfigUiControls.clampFloat(data.bossEditDamage, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
        }
        Float speed = parseOptionalFloat(data.bossEditSpeed);
        if (speed != null && speed > 0f) {
            boss.modifiers.movementSpeed = speed;
        }
        if (data.bossEditSize != null && Float.isFinite(data.bossEditSize)) {
            boss.modifiers.size = BossArenaConfigUiControls.clampFloat(data.bossEditSize, MULT_SIZE_MIN, MULT_SIZE_MAX);
        }
        Float attackRate = parseOptionalFloat(data.bossEditAttackRate);
        if (attackRate != null && attackRate > 0f) {
            boss.modifiers.attackRate = attackRate;
        }
        Float abilityCooldown = parseOptionalFloat(data.bossEditAbilityCooldown);
        if (abilityCooldown != null && abilityCooldown > 0f) {
            boss.modifiers.abilityCooldown = abilityCooldown;
        }
        Float knockbackGiven = parseOptionalFloat(data.bossEditKnockbackGiven);
        if (knockbackGiven != null && knockbackGiven > 0f) {
            boss.modifiers.knockbackGiven = knockbackGiven;
        }
        Float knockbackTaken = parseOptionalFloat(data.bossEditKnockbackTaken);
        if (knockbackTaken != null && knockbackTaken > 0f) {
            boss.modifiers.knockbackTaken = knockbackTaken;
        }
        Float turnRate = parseOptionalFloat(data.bossEditTurnRate);
        if (turnRate != null && turnRate > 0f) {
            boss.modifiers.turnRate = turnRate;
        }
        Float regen = parseOptionalFloat(data.bossEditRegen);
        if (regen != null && regen > 0f) {
            boss.modifiers.regen = regen;
        }

        if (data.bossEditPpHp != null && Float.isFinite(data.bossEditPpHp)) {
            boss.perPlayerIncrease.hp = BossArenaConfigUiControls.clampFloat(data.bossEditPpHp, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpDamage != null && Float.isFinite(data.bossEditPpDamage)) {
            boss.perPlayerIncrease.damage = BossArenaConfigUiControls.clampFloat(data.bossEditPpDamage, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        Float ppSpeed = parseOptionalFloat(data.bossEditPpSpeed);
        if (ppSpeed != null) {
            boss.perPlayerIncrease.movementSpeed = ppSpeed;
        }
        if (data.bossEditPpSize != null && Float.isFinite(data.bossEditPpSize)) {
            boss.perPlayerIncrease.size = BossArenaConfigUiControls.clampFloat(data.bossEditPpSize, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        Float ppAttackRate = parseOptionalFloat(data.bossEditPpAttackRate);
        if (ppAttackRate != null) {
            boss.perPlayerIncrease.attackRate = ppAttackRate;
        }
        Float ppAbilityCooldown = parseOptionalFloat(data.bossEditPpAbilityCooldown);
        if (ppAbilityCooldown != null) {
            boss.perPlayerIncrease.abilityCooldown = ppAbilityCooldown;
        }
        Float ppKnockbackGiven = parseOptionalFloat(data.bossEditPpKnockbackGiven);
        if (ppKnockbackGiven != null) {
            boss.perPlayerIncrease.knockbackGiven = ppKnockbackGiven;
        }
        Float ppKnockbackTaken = parseOptionalFloat(data.bossEditPpKnockbackTaken);
        if (ppKnockbackTaken != null) {
            boss.perPlayerIncrease.knockbackTaken = ppKnockbackTaken;
        }
        Float ppTurnRate = parseOptionalFloat(data.bossEditPpTurnRate);
        if (ppTurnRate != null) {
            boss.perPlayerIncrease.turnRate = ppTurnRate;
        }
        Float ppRegen = parseOptionalFloat(data.bossEditPpRegen);
        if (ppRegen != null) {
            boss.perPlayerIncrease.regen = ppRegen;
        }
        Integer waves = parseOptionalInt(data.bossEditWaves);
        if (waves != null && waves >= -1) {
            if (boss.extraMobs == null) {
                boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            boss.extraMobs.waves = waves;
            boss.extraMobs.sanitize();
        }

    }

    private void handleBossSpawnTriggerMode(String action, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        try {
            applyBossEditorDraft(data);
            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            applyScheduleRowsToExtra(extra, rows);
            extra.bossSpawnTrigger = "boss_spawn_after_delay".equals(action)
                    ? BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_SECONDS
                    : BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_BEFORE_BOSS;
            extra.sanitize();
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveToggle(String action, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        try {
            applyBossEditorDraft(data);
            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            applyScheduleRowsToExtra(extra, rows);
            if ("boss_wave_random_toggle".equals(action)) {
                extra.useRandomSpawnLocations = !extra.useRandomSpawnLocations;
            } else {
                extra.timedProximityEnabled = !extra.timedProximityEnabled;
            }
            extra.sanitize();
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveNpcFilter(String rowToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_WAVE_ADD_ROWS) {
            return;
        }
        try {
            applyBossEditorDraft(data);
            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            applyScheduleRowsToExtra(extra, rows);
            extra.sanitize();

            String query = optionalText(data.getBossWaveNpc(row)).trim();
            waveNpcPicksRow = row;
            waveNpcSearchQuery = query;
            waveNpcPicksOpen = !query.isEmpty() && !BossArenaConfigUiControls.isExactNpcId(query);
            softUpdateWaveNpcPicks(false);
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveNpcPick(String pickToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        int row = waveNpcPicksRow;
        int pick = parseRow(pickToken);
        if (row < 1 || row > MAX_WAVE_ADD_ROWS || pick < 1 || pick > BossArenaConfigUiControls.MAX_WAVE_NPC_PICKS) {
            return;
        }
        try {
            applyBossEditorDraft(data);
            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            while (rows.size() < row) {
                rows.add(defaultWaveScheduleRow());
            }
            WaveScheduleRow current = rows.get(row - 1);
            String filter = current.add != null ? safeText(current.add.npcId) : "";
            if (filter.isEmpty()) {
                filter = optionalText(data.getBossWaveNpc(row));
            }
            List<String> picks = BossArenaConfigUiControls.filterNpcIds(filter, BossArenaConfigUiControls.MAX_WAVE_NPC_PICKS);
            if (pick > picks.size()) {
                waveNpcPicksOpen = false;
                softUpdateWaveNpcPicks(false);
                return;
            }
            if (current.add == null) {
                current = defaultWaveScheduleRow();
            }
            BossDefinition.ExtraMobs.WaveAdd add = current.add != null ? current.add : new BossDefinition.ExtraMobs.WaveAdd();
            add.npcId = picks.get(pick - 1);
            rows.set(row - 1, new WaveScheduleRow(
                    current.trigger,
                    current.triggerValue,
                    current.repeatCount,
                    current.repeatEverySeconds,
                    add
            ));
            applyScheduleRowsToExtra(extra, rows);
            extra.sanitize();
            waveNpcSearchQuery = add.npcId;
            waveNpcPicksOpen = false;
            softUpdateWaveNpcPicks(true);
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void softUpdateWaveNpcPicks(boolean syncFieldValue) {
        if (bossEditorState == null) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        if (syncFieldValue && waveNpcPicksRow >= 1 && waveNpcPicksRow <= MAX_WAVE_ADD_ROWS) {
            cmd.set("#BossWaveNpc" + waveNpcPicksRow + ".Value", safeText(waveNpcSearchQuery));
        }
        applyWaveNpcPicks(cmd, null, false);
        sendUpdate(cmd, false);
    }

    private String currentWaveNpcQuery(int row) {
        if (bossEditorState == null || bossEditorState.boss.extraMobs == null) {
            return "";
        }
        List<WaveScheduleRow> rows = flattenScheduleRows(bossEditorState.boss.extraMobs);
        if (row < 1 || row > rows.size()) {
            return "";
        }
        WaveScheduleRow scheduleRow = rows.get(row - 1);
        if (scheduleRow == null || scheduleRow.add == null) {
            return "";
        }
        return safeText(scheduleRow.add.npcId);
    }

    private static int waveNpcPickTopForRow(int row) {
        int rowTop = 294 + Math.max(0, row - 1) * 34;
        return rowTop + 30;
    }

    private void applyWaveNpcPicks(UICommandBuilder cmd, UIEventBuilder events, boolean bindEvents) {
        List<String> npcPicks = List.of();
        int row = waveNpcPicksRow;
        if (bossWavesOverlayOpen && waveNpcPicksOpen && row >= 1 && row <= MAX_WAVE_ADD_ROWS) {
            String query = safeText(waveNpcSearchQuery).trim();
            if (query.isEmpty()) {
                query = currentWaveNpcQuery(row);
            }
            if (!query.isEmpty() && !BossArenaConfigUiControls.isExactNpcId(query)) {
                npcPicks = BossArenaConfigUiControls.filterNpcIds(
                        query,
                        BossArenaConfigUiControls.MAX_WAVE_NPC_PICKS
                );
            }
        }

        int baseTop = waveNpcPickTopForRow(Math.max(1, row));
        for (int i = 1; i <= BossArenaConfigUiControls.MAX_WAVE_NPC_PICKS; i++) {
            String pickId = "#BossWaveNpcPick" + i;
            boolean visible = i <= npcPicks.size();

            Anchor anchor = new Anchor();
            anchor.setLeft(Value.of(540));
            anchor.setTop(Value.of(baseTop + (i - 1) * 20));
            anchor.setWidth(Value.of(220));
            anchor.setHeight(Value.of(20));
            cmd.setObject(pickId + ".Anchor", anchor);
            cmd.set(pickId + ".Visible", visible);
            if (visible) {
                cmd.set(pickId + ".Text", npcPicks.get(i - 1));
            }
            if (bindEvents && events != null) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        pickId,
                        buildBossWavesSnapshotEvent("boss_wave_npc_pick_" + i),
                        false
                );
            }
        }
    }

    private void handleBossNpcFilter(ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        applyBossEditorDraft(data);
        String query = optionalText(data.bossEditNpcId);
        if (query.isEmpty()) {
            query = safeText(bossEditorState.boss.npcId).trim();
        } else {
            bossEditorState.boss.npcId = query;
        }
        // Show suggestions while typing a non-empty query; hide once an exact id is locked.
        bossNpcPicksOpen = !query.isEmpty() && !BossArenaConfigUiControls.isExactNpcId(query);
        softUpdateBossNpcPicks(false);
    }

    private void handleBossNpcPick(String pickToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        int pick = parseRow(pickToken);
        if (pick < 1 || pick > BossArenaConfigUiControls.MAX_BOSS_NPC_PICKS) {
            return;
        }
        applyBossEditorDraft(data);
        List<String> picks = BossArenaConfigUiControls.filterNpcIds(
                bossEditorState.boss.npcId,
                BossArenaConfigUiControls.MAX_BOSS_NPC_PICKS
        );
        if (pick <= picks.size()) {
            bossEditorState.boss.npcId = picks.get(pick - 1);
        }
        bossNpcPicksOpen = false;
        softUpdateBossNpcPicks(true);
    }

    private void handleBossTierSelect(String tierToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        applyBossEditorDraft(data);
        bossEditorState.boss.tier = normalizeTier(tierToken);
    }

    private void softUpdateBossNpcPicks(boolean syncFieldValue) {
        if (bossEditorState == null) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        if (syncFieldValue) {
            cmd.set("#BossEditNpcId.Value", safeText(bossEditorState.boss.npcId));
        }
        applyBossNpcPicks(cmd, null, false);
        applyBossEditorScrollForNpcPicks(cmd);
        sendUpdate(cmd, false);
    }

    private void softUpdateBossSliderLabels() {
        if (bossEditorState == null) {
            return;
        }
        BossDefinition boss = bossEditorState.boss;
        UICommandBuilder cmd = new UICommandBuilder();
        float hpMult = BossArenaConfigUiControls.clampFloat(boss.modifiers.hp, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
        float dmgMult = BossArenaConfigUiControls.clampFloat(boss.modifiers.damage, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
        float sizeMult = BossArenaConfigUiControls.clampFloat(boss.modifiers.size, MULT_SIZE_MIN, MULT_SIZE_MAX);
        float ppHp = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.hp, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppDmg = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.damage, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppSize = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.size, MULT_PERS_MIN, MULT_PERS_MAX);
        cmd.set("#BossEditHpValue.Text", formatFloat(hpMult));
        cmd.set("#BossEditDamageValue.Text", formatFloat(dmgMult));
        cmd.set("#BossEditSizeValue.Text", formatFloat(sizeMult));
        cmd.set("#BossEditPpHpValue.Text", formatFloat(ppHp));
        cmd.set("#BossEditPpDamageValue.Text", formatFloat(ppDmg));
        cmd.set("#BossEditPpSizeValue.Text", formatFloat(ppSize));
        sendUpdate(cmd, false);
    }

    private void applyBossEditorScrollForNpcPicks(UICommandBuilder cmd) {
        // Keep scroll below the suggestion list so picks remain clickable.
        Anchor scroll = new Anchor();
        scroll.setLeft(Value.of(100));
        scroll.setWidth(Value.of(670));
        if (bossNpcPicksOpen) {
            scroll.setTop(Value.of(340));
            scroll.setHeight(Value.of(226));
        } else {
            scroll.setTop(Value.of(176));
            scroll.setHeight(Value.of(390));
        }
        cmd.setObject("#BossEditorScroll.Anchor", scroll);
    }

    private void applyBossNpcPicks(UICommandBuilder cmd, UIEventBuilder events, boolean bindEvents) {
        if (bossEditorState == null) {
            return;
        }
        List<String> npcPicks = List.of();
        if (bossNpcPicksOpen) {
            String query = safeText(bossEditorState.boss.npcId).trim();
            if (!query.isEmpty() && !BossArenaConfigUiControls.isExactNpcId(query)) {
                npcPicks = BossArenaConfigUiControls.filterNpcIds(
                        query,
                        BossArenaConfigUiControls.MAX_BOSS_NPC_PICKS
                );
            }
        }
        for (int i = 1; i <= BossArenaConfigUiControls.MAX_BOSS_NPC_PICKS; i++) {
            String pickId = "#BossNpcPick" + i;
            boolean visible = i <= npcPicks.size();
            cmd.set(pickId + ".Visible", visible);
            if (visible) {
                cmd.set(pickId + ".Text", npcPicks.get(i - 1));
            }
            // Always bind: picks start hidden, soft-update only toggles Visible.
            if (bindEvents && events != null) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        pickId,
                        buildBossEditorSnapshotEvent("boss_npc_pick_" + i),
                        false
                );
            }
        }
    }

    private void handleBossWavesSave(ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        try {
            applyBossEditorDraft(data);

            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            applyScheduleRowsToExtra(extra, rows);
            extra.sanitize();
            bossWavesOverlayOpen = false;
            bossStatusText = "Vagues boss mises à jour. Cliquez sur Enregistrer pour sauvegarder.";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossScalersSave(ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        try {
            applyBossEditorDraft(data);
            bossScalersOverlayOpen = false;
            bossStatusText = "Scalers boss avancés mis à jour. Cliquez sur Enregistrer pour sauvegarder.";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveAddRow(String rowToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_WAVE_ADD_ROWS) {
            bossStatusText = "Ligne d'ajout de vague invalide.";
            rebuild();
            return;
        }

        try {
            applyBossEditorDraft(data);

            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            if (rows.size() >= MAX_WAVE_ADD_ROWS) {
                bossStatusText = "Nombre maximum de lignes de vagues atteint.";
                rebuild();
                return;
            }
            rows.add(defaultWaveScheduleRow());
            applyScheduleRowsToExtra(extra, rows);
            extra.sanitize();
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveDelete(String rowToken, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_WAVE_ADD_ROWS) {
            bossStatusText = "Ligne d'ajout de vague invalide.";
            rebuild();
            return;
        }

        try {
            applyBossEditorDraft(data);

            if (bossEditorState.boss.extraMobs == null) {
                bossEditorState.boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            BossDefinition.ExtraMobs extra = bossEditorState.boss.extraMobs;
            applyWaveSpawnSettingsFromData(extra, data);
            List<WaveScheduleRow> rows = snapshotWaveScheduleRowsFromData(data);
            if (row > rows.size()) {
                bossStatusText = "Sélection de ligne de vague invalide.";
                rebuild();
                return;
            }
            rows.remove(row - 1);
            applyScheduleRowsToExtra(extra, rows);
            extra.sanitize();
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private List<WaveScheduleRow> snapshotWaveScheduleRowsFromData(ConfigEventData data) {
        List<WaveScheduleRow> rows = new ArrayList<>();
        List<WaveScheduleRow> fallbackRows = List.of();
        if (bossEditorState != null
                && bossEditorState.boss != null
                && bossEditorState.boss.extraMobs != null) {
            fallbackRows = flattenScheduleRows(bossEditorState.boss.extraMobs);
        }

        for (int row = 1; row <= MAX_WAVE_ADD_ROWS; row++) {
            WaveScheduleRow fallback = row <= fallbackRows.size() ? fallbackRows.get(row - 1) : null;
            String triggerText = optionalText(data.getBossWaveEvery(row));
            String triggerValueText = optionalText(data.getBossWaveValue(row));
            String repeatCountText = optionalText(data.getBossWaveRepeatCount(row));
            String repeatSecText = optionalText(data.getBossWaveRepeatSec(row));
            String npcId = optionalText(data.getBossWaveNpc(row));
            String amountText = optionalText(data.getBossWaveAmount(row));
            String hpText = optionalText(data.getBossWaveHp(row));
            String damageText = optionalText(data.getBossWaveDamage(row));
            String sizeText = optionalText(data.getBossWaveSize(row));

            if (looksLikeUiBindingExpression(triggerText)
                    || looksLikeUiBindingExpression(triggerValueText)
                    || looksLikeUiBindingExpression(repeatCountText)
                    || looksLikeUiBindingExpression(repeatSecText)
                    || looksLikeUiBindingExpression(npcId)
                    || looksLikeUiBindingExpression(amountText)
                    || looksLikeUiBindingExpression(hpText)
                    || looksLikeUiBindingExpression(damageText)
                    || looksLikeUiBindingExpression(sizeText)) {
                if (fallback != null && fallback.add != null && fallback.add.npcId != null && !fallback.add.npcId.isBlank()) {
                    rows.add(copyWaveScheduleRow(fallback));
                }
                continue;
            }

            if (npcId.isEmpty()) {
                continue;
            }

            String resolvedTrigger = triggerText.isEmpty()
                    ? (fallback != null ? fallback.trigger : BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS)
                    : triggerText;
            String trigger = normalizeWaveTriggerInput(resolvedTrigger);
            if (trigger == null) {
                throw new IllegalArgumentException(
                        "Déclencheur de vague invalide sur la ligne " + row + ". Utilisez 1-5, Avant le boss, Au spawn, Après spawn, Depuis dernière vague, ou PV boss %."
                );
            }

            String defaultValue = fallback != null
                    ? formatDouble(fallback.triggerValue)
                    : (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger) ? "50" : "0");
            double triggerValue;
            if (BossDefinition.ExtraMobs.TRIGGER_ON_SPAWN.equals(trigger)) {
                triggerValue = 0.0d;
            } else if (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger)) {
                triggerValue = parseRequiredDouble(
                        resolvedOrFallback(triggerValueText, defaultValue),
                        "Valeur déclencheur vague doit être 0-100 sur la ligne " + row + ".",
                        0.0d,
                        100.0d
                );
            } else {
                triggerValue = parseRequiredDouble(
                        resolvedOrFallback(triggerValueText, defaultValue),
                        "Valeur déclencheur vague doit être >= 0 sur la ligne " + row + ".",
                        0.0d,
                        Double.MAX_VALUE
                );
            }

            int repeatCount = parseRequiredInt(
                    resolvedOrFallback(repeatCountText, fallback != null ? Integer.toString(fallback.repeatCount) : "1"),
                    "Nb répétitions vague doit être -1 ou >= 1 sur la ligne " + row + ".",
                    -1,
                    Integer.MAX_VALUE
            );
            double repeatSec = parseRequiredDouble(
                    resolvedOrFallback(repeatSecText, fallback != null ? formatDouble(fallback.repeatEverySeconds) : "0"),
                    "Secondes de répétition vague doit être >= 0 sur la ligne " + row + ".",
                    0.0d,
                    Double.MAX_VALUE
            );

            if (repeatCount == 1) {
                repeatSec = 0.0d;
            } else if (repeatSec <= 0.0d) {
                if (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger)) {
                    repeatSec = 1.0d;
                } else {
                    repeatSec = triggerValue > 0.0d ? triggerValue : 1.0d;
                }
            }

            int amount = parseRequiredInt(amountText, "Quantité vague doit être un entier sur la ligne " + row + ".", 1, Integer.MAX_VALUE);
            String resolvedHp = !hpText.isEmpty() ? hpText : (fallback != null ? formatFloat(fallback.add.hp > 0f ? fallback.add.hp : 1.0f) : "1.00");
            String resolvedDamage = !damageText.isEmpty() ? damageText : (fallback != null ? formatFloat(fallback.add.damage > 0f ? fallback.add.damage : 1.0f) : "1.00");
            String resolvedSize = !sizeText.isEmpty() ? sizeText : (fallback != null ? formatFloat(fallback.add.size > 0f ? fallback.add.size : 1.00f) : "1.00");
            float hp = parseRequiredFloat(
                    resolvedHp,
                    "Mult PV vague doit être entre 0.50 et 50.00 sur la ligne " + row + ".",
                    MULT_HP_DMG_MIN,
                    MULT_HP_DMG_MAX
            );
            float damage = parseRequiredFloat(
                    resolvedDamage,
                    "Mult dégâts vague doit être entre 0.50 et 50.00 sur la ligne " + row + ".",
                    MULT_HP_DMG_MIN,
                    MULT_HP_DMG_MAX
            );
            float size = parseRequiredFloat(
                    resolvedSize,
                    "Mult taille vague doit être entre 0.10 et 10.00 sur la ligne " + row + ".",
                    MULT_SIZE_MIN,
                    MULT_SIZE_MAX
            );

            BossDefinition.ExtraMobs.WaveAdd add = new BossDefinition.ExtraMobs.WaveAdd();
            add.npcId = npcId;
            add.mobsPerWave = amount;
            add.everyWave = 1;
            add.hp = hp;
            add.damage = damage;
            add.size = size;
            rows.add(new WaveScheduleRow(trigger, triggerValue, repeatCount, repeatSec, add));
        }

        return rows;
    }

    private List<LootItem> snapshotLootFromData(ConfigEventData data) {
        List<LootItem> rows = new ArrayList<>();
        List<LootItem> fallbackRows = List.of();
        if (bossEditorState != null && bossEditorState.loot != null && bossEditorState.loot.items != null) {
            fallbackRows = bossEditorState.loot.items;
        }

        for (int row = 1; row <= MAX_LOOT_ROWS; row++) {
            String itemId = optionalText(data.getBossLootName(row));
            String minText = optionalText(data.getBossLootMin(row));
            String maxText = optionalText(data.getBossLootMax(row));
            String chanceText = optionalText(data.getBossLootChance(row));

            if (looksLikeUiBindingExpression(itemId)
                    || looksLikeUiBindingExpression(minText)
                    || looksLikeUiBindingExpression(maxText)
                    || looksLikeUiBindingExpression(chanceText)) {
                if (row <= fallbackRows.size()) {
                    LootItem fallback = fallbackRows.get(row - 1);
                    if (fallback != null && fallback.itemId != null && !fallback.itemId.isBlank()) {
                        rows.add(new LootItem(fallback.itemId, fallback.dropChance, fallback.minAmount, fallback.maxAmount));
                    }
                }
                continue;
            }

            // Ignore unfinished rows with no item name (the trailing blank input row).
            if (itemId.isEmpty()) {
                continue;
            }

            int minAmount = parseRequiredInt(minText, "Quantité min butin doit être un entier sur la ligne " + row + ".", 1, Integer.MAX_VALUE);
            int maxAmount = parseRequiredInt(maxText, "Quantité max butin doit être un entier sur la ligne " + row + ".", minAmount, Integer.MAX_VALUE);
            double chance = parseRequiredDouble(chanceText, "Chance de butin doit être entre 0.0 et 1.0 sur la ligne " + row + ".", 0.0d, 1.0d);

            rows.add(new LootItem(itemId, chance, minAmount, maxAmount));
        }

        return rows;
    }

    private String nextBossName() {
        int index = 1;
        while (BossRegistry.exists("boss" + index)) {
            index++;
        }
        return "boss" + index;
    }

    private static final class ShopLocationRef {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;

        private ShopLocationRef(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class ShopLocationEditorState {
        private final ShopLocationRef shopLocation;
        private final List<String> orderedBossNames;
        private final Set<String> enabledBossNames;
        private final Map<String, Integer> contractPriceByBossName;
        private String arenaId;
        private int bossListOffset;

        private ShopLocationEditorState(ShopLocationRef shopLocation,
                                        List<String> orderedBossNames,
                                        Set<String> enabledBossNames,
                                        Map<String, Integer> contractPriceByBossName,
                                        String arenaId) {
            this.shopLocation = shopLocation;
            this.orderedBossNames = orderedBossNames;
            this.enabledBossNames = enabledBossNames;
            this.contractPriceByBossName = contractPriceByBossName;
            this.arenaId = arenaId;
            this.bossListOffset = 0;
        }
    }

    private static final class ShopLocationView {
        private final String arenaLabel;
        private final String distanceLabel;
        private final Double distance;
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final int enabledBosses;
        private final int totalBosses;

        private ShopLocationView(String arenaLabel,
                                 String distanceLabel,
                                 Double distance,
                                 String worldName,
                                 int x,
                                 int y,
                                 int z,
                                 int enabledBosses,
                                 int totalBosses) {
            this.arenaLabel = arenaLabel;
            this.distanceLabel = distanceLabel;
            this.distance = distance;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.enabledBosses = enabledBosses;
            this.totalBosses = totalBosses;
        }
    }

    private static final class BossEditorState {
        private final String originalBossName;
        private final BossDefinition boss;
        private final LootTable loot;

        private BossEditorState(String originalBossName, BossDefinition boss, LootTable loot) {
            this.originalBossName = originalBossName;
            this.boss = boss;
            this.loot = loot;
        }
    }

    private static final class BossWavesSummary {
        private final String addLine1;
        private final String addLine2;
        private final String metaLine;

        private BossWavesSummary(String addLine1, String addLine2, String metaLine) {
            this.addLine1 = addLine1;
            this.addLine2 = addLine2;
            this.metaLine = metaLine;
        }
    }

    private static final class WaveScheduleRow {
        private final String trigger;
        private final double triggerValue;
        private final int repeatCount;
        private final double repeatEverySeconds;
        private final BossDefinition.ExtraMobs.WaveAdd add;

        private WaveScheduleRow(String trigger,
                                double triggerValue,
                                int repeatCount,
                                double repeatEverySeconds,
                                BossDefinition.ExtraMobs.WaveAdd add) {
            this.trigger = trigger;
            this.triggerValue = triggerValue;
            this.repeatCount = repeatCount;
            this.repeatEverySeconds = repeatEverySeconds;
            this.add = add;
        }
    }

    public static final class ConfigEventData {
        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec.builder(
                        ConfigEventData.class,
                        ConfigEventData::new
                )
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()

                .append(new KeyedCodec<>("@ArenaName", Codec.STRING), (d, v) -> d.arenaName = v, d -> d.arenaName).add()
                .append(new KeyedCodec<>("@ArenaX", Codec.STRING), (d, v) -> d.arenaX = v, d -> d.arenaX).add()
                .append(new KeyedCodec<>("@ArenaY", Codec.STRING), (d, v) -> d.arenaY = v, d -> d.arenaY).add()
                .append(new KeyedCodec<>("@ArenaZ", Codec.STRING), (d, v) -> d.arenaZ = v, d -> d.arenaZ).add()
                .append(new KeyedCodec<>("@ArenaLootRadius1", Codec.STRING), (d, v) -> d.arenaLootRadius1 = v, d -> d.arenaLootRadius1).add()
                .append(new KeyedCodec<>("@ArenaLootRadius2", Codec.STRING), (d, v) -> d.arenaLootRadius2 = v, d -> d.arenaLootRadius2).add()
                .append(new KeyedCodec<>("@ArenaLootRadius3", Codec.STRING), (d, v) -> d.arenaLootRadius3 = v, d -> d.arenaLootRadius3).add()
                .append(new KeyedCodec<>("@ArenaLootRadius4", Codec.STRING), (d, v) -> d.arenaLootRadius4 = v, d -> d.arenaLootRadius4).add()
                .append(new KeyedCodec<>("@ArenaLootRadius5", Codec.STRING), (d, v) -> d.arenaLootRadius5 = v, d -> d.arenaLootRadius5).add()
                .append(new KeyedCodec<>("@ArenaLootRadius6", Codec.STRING), (d, v) -> d.arenaLootRadius6 = v, d -> d.arenaLootRadius6).add()
                .append(new KeyedCodec<>("@ArenaLootRadius7", Codec.STRING), (d, v) -> d.arenaLootRadius7 = v, d -> d.arenaLootRadius7).add()
                .append(new KeyedCodec<>("@ArenaLootRadius8", Codec.STRING), (d, v) -> d.arenaLootRadius8 = v, d -> d.arenaLootRadius8).add()
                .append(new KeyedCodec<>("@ShopEditArenaId", Codec.STRING), (d, v) -> d.shopEditArenaId = v, d -> d.shopEditArenaId).add()

                .append(new KeyedCodec<>("@BossEditName", Codec.STRING), (d, v) -> d.bossEditName = v, d -> d.bossEditName).add()
                .append(new KeyedCodec<>("@BossEditNpcId", Codec.STRING), (d, v) -> d.bossEditNpcId = v, d -> d.bossEditNpcId).add()
                .append(new KeyedCodec<>("@BossEditTier", Codec.STRING), (d, v) -> d.bossEditTier = v, d -> d.bossEditTier).add()
                .append(new KeyedCodec<>("@BossEditAmount", Codec.STRING), (d, v) -> d.bossEditAmount = v, d -> d.bossEditAmount).add()
                .append(new KeyedCodec<>("@BossEditLevelOverride", Codec.STRING), (d, v) -> d.bossEditLevelOverride = v, d -> d.bossEditLevelOverride).add()
                .append(new KeyedCodec<>("@BossEditHp", Codec.FLOAT), (d, v) -> d.bossEditHp = v, d -> d.bossEditHp).add()
                .append(new KeyedCodec<>("@BossEditDamage", Codec.FLOAT), (d, v) -> d.bossEditDamage = v, d -> d.bossEditDamage).add()
                .append(new KeyedCodec<>("@BossEditSpeed", Codec.STRING), (d, v) -> d.bossEditSpeed = v, d -> d.bossEditSpeed).add()
                .append(new KeyedCodec<>("@BossEditSize", Codec.FLOAT), (d, v) -> d.bossEditSize = v, d -> d.bossEditSize).add()
                .append(new KeyedCodec<>("@BossEditAttackRate", Codec.STRING), (d, v) -> d.bossEditAttackRate = v, d -> d.bossEditAttackRate).add()
                .append(new KeyedCodec<>("@BossEditAbilityCooldown", Codec.STRING), (d, v) -> d.bossEditAbilityCooldown = v, d -> d.bossEditAbilityCooldown).add()
                .append(new KeyedCodec<>("@BossEditKnockbackGiven", Codec.STRING), (d, v) -> d.bossEditKnockbackGiven = v, d -> d.bossEditKnockbackGiven).add()
                .append(new KeyedCodec<>("@BossEditKnockbackTaken", Codec.STRING), (d, v) -> d.bossEditKnockbackTaken = v, d -> d.bossEditKnockbackTaken).add()
                .append(new KeyedCodec<>("@BossEditTurnRate", Codec.STRING), (d, v) -> d.bossEditTurnRate = v, d -> d.bossEditTurnRate).add()
                .append(new KeyedCodec<>("@BossEditRegen", Codec.STRING), (d, v) -> d.bossEditRegen = v, d -> d.bossEditRegen).add()
                .append(new KeyedCodec<>("@BossEditPpHp", Codec.FLOAT), (d, v) -> d.bossEditPpHp = v, d -> d.bossEditPpHp).add()
                .append(new KeyedCodec<>("@BossEditPpDamage", Codec.FLOAT), (d, v) -> d.bossEditPpDamage = v, d -> d.bossEditPpDamage).add()
                .append(new KeyedCodec<>("@BossEditPpSpeed", Codec.STRING), (d, v) -> d.bossEditPpSpeed = v, d -> d.bossEditPpSpeed).add()
                .append(new KeyedCodec<>("@BossEditPpSize", Codec.FLOAT), (d, v) -> d.bossEditPpSize = v, d -> d.bossEditPpSize).add()
                .append(new KeyedCodec<>("@BossEditPpAttackRate", Codec.STRING), (d, v) -> d.bossEditPpAttackRate = v, d -> d.bossEditPpAttackRate).add()
                .append(new KeyedCodec<>("@BossEditPpAbilityCooldown", Codec.STRING), (d, v) -> d.bossEditPpAbilityCooldown = v, d -> d.bossEditPpAbilityCooldown).add()
                .append(new KeyedCodec<>("@BossEditPpKnockbackGiven", Codec.STRING), (d, v) -> d.bossEditPpKnockbackGiven = v, d -> d.bossEditPpKnockbackGiven).add()
                .append(new KeyedCodec<>("@BossEditPpKnockbackTaken", Codec.STRING), (d, v) -> d.bossEditPpKnockbackTaken = v, d -> d.bossEditPpKnockbackTaken).add()
                .append(new KeyedCodec<>("@BossEditPpTurnRate", Codec.STRING), (d, v) -> d.bossEditPpTurnRate = v, d -> d.bossEditPpTurnRate).add()
                .append(new KeyedCodec<>("@BossEditPpRegen", Codec.STRING), (d, v) -> d.bossEditPpRegen = v, d -> d.bossEditPpRegen).add()
                .append(new KeyedCodec<>("@BossEditWaves", Codec.STRING), (d, v) -> d.bossEditWaves = v, d -> d.bossEditWaves).add()
                .append(new KeyedCodec<>("@BossEditExtraNpcId", Codec.STRING), (d, v) -> d.bossEditExtraNpcId = v, d -> d.bossEditExtraNpcId).add()
                .append(new KeyedCodec<>("@BossEditExtraTimeLimit", Codec.STRING), (d, v) -> d.bossEditExtraTimeLimit = v, d -> d.bossEditExtraTimeLimit).add()
                .append(new KeyedCodec<>("@BossEditExtraWaves", Codec.STRING), (d, v) -> d.bossEditExtraWaves = v, d -> d.bossEditExtraWaves).add()
                .append(new KeyedCodec<>("@BossEditExtraMobsPerWave", Codec.STRING), (d, v) -> d.bossEditExtraMobsPerWave = v, d -> d.bossEditExtraMobsPerWave).add()

                .append(new KeyedCodec<>("@BossSpawnTrigger", Codec.STRING), (d, v) -> d.bossSpawnTrigger = v, d -> d.bossSpawnTrigger).add()
                .append(new KeyedCodec<>("@BossSpawnTriggerValue", Codec.STRING), (d, v) -> d.bossSpawnTriggerValue = v, d -> d.bossSpawnTriggerValue).add()
                .append(new KeyedCodec<>("@BossWaveRandomLocations", Codec.STRING), (d, v) -> d.bossWaveRandomLocations = v, d -> d.bossWaveRandomLocations).add()
                .append(new KeyedCodec<>("@BossWaveRandomRadius", Codec.STRING), (d, v) -> d.bossWaveRandomRadius = v, d -> d.bossWaveRandomRadius).add()
                .append(new KeyedCodec<>("@BossTimedProximityEnabled", Codec.STRING), (d, v) -> d.bossTimedProximityEnabled = v, d -> d.bossTimedProximityEnabled).add()
                .append(new KeyedCodec<>("@BossTimedProximityArena", Codec.STRING), (d, v) -> d.bossTimedProximityArena = v, d -> d.bossTimedProximityArena).add()
                .append(new KeyedCodec<>("@BossTimedProximityRadius", Codec.STRING), (d, v) -> d.bossTimedProximityRadius = v, d -> d.bossTimedProximityRadius).add()
                .append(new KeyedCodec<>("@BossTimedProximityCooldown", Codec.STRING), (d, v) -> d.bossTimedProximityCooldown = v, d -> d.bossTimedProximityCooldown).add()
                .append(new KeyedCodec<>("@BossWaveTimeSec", Codec.STRING), (d, v) -> d.bossWaveTimeSec = v, d -> d.bossWaveTimeSec).add()
                .append(new KeyedCodec<>("@BossWaveNpc1", Codec.STRING), (d, v) -> d.bossWaveNpc1 = v, d -> d.bossWaveNpc1).add()
                .append(new KeyedCodec<>("@BossWaveAmount1", Codec.STRING), (d, v) -> d.bossWaveAmount1 = v, d -> d.bossWaveAmount1).add()
                .append(new KeyedCodec<>("@BossWaveEvery1", Codec.STRING), (d, v) -> d.bossWaveEvery1 = v, d -> d.bossWaveEvery1).add()
                .append(new KeyedCodec<>("@BossWaveHp1", Codec.STRING), (d, v) -> d.bossWaveHp1 = v, d -> d.bossWaveHp1).add()
                .append(new KeyedCodec<>("@BossWaveDamage1", Codec.STRING), (d, v) -> d.bossWaveDamage1 = v, d -> d.bossWaveDamage1).add()
                .append(new KeyedCodec<>("@BossWaveSize1", Codec.STRING), (d, v) -> d.bossWaveSize1 = v, d -> d.bossWaveSize1).add()
                .append(new KeyedCodec<>("@BossWaveNpc2", Codec.STRING), (d, v) -> d.bossWaveNpc2 = v, d -> d.bossWaveNpc2).add()
                .append(new KeyedCodec<>("@BossWaveAmount2", Codec.STRING), (d, v) -> d.bossWaveAmount2 = v, d -> d.bossWaveAmount2).add()
                .append(new KeyedCodec<>("@BossWaveEvery2", Codec.STRING), (d, v) -> d.bossWaveEvery2 = v, d -> d.bossWaveEvery2).add()
                .append(new KeyedCodec<>("@BossWaveHp2", Codec.STRING), (d, v) -> d.bossWaveHp2 = v, d -> d.bossWaveHp2).add()
                .append(new KeyedCodec<>("@BossWaveDamage2", Codec.STRING), (d, v) -> d.bossWaveDamage2 = v, d -> d.bossWaveDamage2).add()
                .append(new KeyedCodec<>("@BossWaveSize2", Codec.STRING), (d, v) -> d.bossWaveSize2 = v, d -> d.bossWaveSize2).add()
                .append(new KeyedCodec<>("@BossWaveNpc3", Codec.STRING), (d, v) -> d.bossWaveNpc3 = v, d -> d.bossWaveNpc3).add()
                .append(new KeyedCodec<>("@BossWaveAmount3", Codec.STRING), (d, v) -> d.bossWaveAmount3 = v, d -> d.bossWaveAmount3).add()
                .append(new KeyedCodec<>("@BossWaveEvery3", Codec.STRING), (d, v) -> d.bossWaveEvery3 = v, d -> d.bossWaveEvery3).add()
                .append(new KeyedCodec<>("@BossWaveHp3", Codec.STRING), (d, v) -> d.bossWaveHp3 = v, d -> d.bossWaveHp3).add()
                .append(new KeyedCodec<>("@BossWaveDamage3", Codec.STRING), (d, v) -> d.bossWaveDamage3 = v, d -> d.bossWaveDamage3).add()
                .append(new KeyedCodec<>("@BossWaveSize3", Codec.STRING), (d, v) -> d.bossWaveSize3 = v, d -> d.bossWaveSize3).add()
                .append(new KeyedCodec<>("@BossWaveNpc4", Codec.STRING), (d, v) -> d.bossWaveNpc4 = v, d -> d.bossWaveNpc4).add()
                .append(new KeyedCodec<>("@BossWaveAmount4", Codec.STRING), (d, v) -> d.bossWaveAmount4 = v, d -> d.bossWaveAmount4).add()
                .append(new KeyedCodec<>("@BossWaveEvery4", Codec.STRING), (d, v) -> d.bossWaveEvery4 = v, d -> d.bossWaveEvery4).add()
                .append(new KeyedCodec<>("@BossWaveHp4", Codec.STRING), (d, v) -> d.bossWaveHp4 = v, d -> d.bossWaveHp4).add()
                .append(new KeyedCodec<>("@BossWaveDamage4", Codec.STRING), (d, v) -> d.bossWaveDamage4 = v, d -> d.bossWaveDamage4).add()
                .append(new KeyedCodec<>("@BossWaveSize4", Codec.STRING), (d, v) -> d.bossWaveSize4 = v, d -> d.bossWaveSize4).add()
                .append(new KeyedCodec<>("@BossWaveNpc5", Codec.STRING), (d, v) -> d.bossWaveNpc5 = v, d -> d.bossWaveNpc5).add()
                .append(new KeyedCodec<>("@BossWaveAmount5", Codec.STRING), (d, v) -> d.bossWaveAmount5 = v, d -> d.bossWaveAmount5).add()
                .append(new KeyedCodec<>("@BossWaveEvery5", Codec.STRING), (d, v) -> d.bossWaveEvery5 = v, d -> d.bossWaveEvery5).add()
                .append(new KeyedCodec<>("@BossWaveHp5", Codec.STRING), (d, v) -> d.bossWaveHp5 = v, d -> d.bossWaveHp5).add()
                .append(new KeyedCodec<>("@BossWaveDamage5", Codec.STRING), (d, v) -> d.bossWaveDamage5 = v, d -> d.bossWaveDamage5).add()
                .append(new KeyedCodec<>("@BossWaveSize5", Codec.STRING), (d, v) -> d.bossWaveSize5 = v, d -> d.bossWaveSize5).add()
                .append(new KeyedCodec<>("@BossWaveNpc6", Codec.STRING), (d, v) -> d.bossWaveNpc6 = v, d -> d.bossWaveNpc6).add()
                .append(new KeyedCodec<>("@BossWaveAmount6", Codec.STRING), (d, v) -> d.bossWaveAmount6 = v, d -> d.bossWaveAmount6).add()
                .append(new KeyedCodec<>("@BossWaveEvery6", Codec.STRING), (d, v) -> d.bossWaveEvery6 = v, d -> d.bossWaveEvery6).add()
                .append(new KeyedCodec<>("@BossWaveHp6", Codec.STRING), (d, v) -> d.bossWaveHp6 = v, d -> d.bossWaveHp6).add()
                .append(new KeyedCodec<>("@BossWaveDamage6", Codec.STRING), (d, v) -> d.bossWaveDamage6 = v, d -> d.bossWaveDamage6).add()
                .append(new KeyedCodec<>("@BossWaveSize6", Codec.STRING), (d, v) -> d.bossWaveSize6 = v, d -> d.bossWaveSize6).add()
                .append(new KeyedCodec<>("@BossWaveValue1", Codec.STRING), (d, v) -> d.bossWaveValue1 = v, d -> d.bossWaveValue1).add()
                .append(new KeyedCodec<>("@BossWaveRepeatCount1", Codec.STRING), (d, v) -> d.bossWaveRepeatCount1 = v, d -> d.bossWaveRepeatCount1).add()
                .append(new KeyedCodec<>("@BossWaveRepeatSec1", Codec.STRING), (d, v) -> d.bossWaveRepeatSec1 = v, d -> d.bossWaveRepeatSec1).add()
                .append(new KeyedCodec<>("@BossWaveValue2", Codec.STRING), (d, v) -> d.bossWaveValue2 = v, d -> d.bossWaveValue2).add()
                .append(new KeyedCodec<>("@BossWaveRepeatCount2", Codec.STRING), (d, v) -> d.bossWaveRepeatCount2 = v, d -> d.bossWaveRepeatCount2).add()
                .append(new KeyedCodec<>("@BossWaveRepeatSec2", Codec.STRING), (d, v) -> d.bossWaveRepeatSec2 = v, d -> d.bossWaveRepeatSec2).add()
                .append(new KeyedCodec<>("@BossWaveValue3", Codec.STRING), (d, v) -> d.bossWaveValue3 = v, d -> d.bossWaveValue3).add()
                .append(new KeyedCodec<>("@BossWaveRepeatCount3", Codec.STRING), (d, v) -> d.bossWaveRepeatCount3 = v, d -> d.bossWaveRepeatCount3).add()
                .append(new KeyedCodec<>("@BossWaveRepeatSec3", Codec.STRING), (d, v) -> d.bossWaveRepeatSec3 = v, d -> d.bossWaveRepeatSec3).add()
                .append(new KeyedCodec<>("@BossWaveValue4", Codec.STRING), (d, v) -> d.bossWaveValue4 = v, d -> d.bossWaveValue4).add()
                .append(new KeyedCodec<>("@BossWaveRepeatCount4", Codec.STRING), (d, v) -> d.bossWaveRepeatCount4 = v, d -> d.bossWaveRepeatCount4).add()
                .append(new KeyedCodec<>("@BossWaveRepeatSec4", Codec.STRING), (d, v) -> d.bossWaveRepeatSec4 = v, d -> d.bossWaveRepeatSec4).add()
                .append(new KeyedCodec<>("@BossWaveValue5", Codec.STRING), (d, v) -> d.bossWaveValue5 = v, d -> d.bossWaveValue5).add()
                .append(new KeyedCodec<>("@BossWaveRepeatCount5", Codec.STRING), (d, v) -> d.bossWaveRepeatCount5 = v, d -> d.bossWaveRepeatCount5).add()
                .append(new KeyedCodec<>("@BossWaveRepeatSec5", Codec.STRING), (d, v) -> d.bossWaveRepeatSec5 = v, d -> d.bossWaveRepeatSec5).add()
                .append(new KeyedCodec<>("@BossWaveValue6", Codec.STRING), (d, v) -> d.bossWaveValue6 = v, d -> d.bossWaveValue6).add()
                .append(new KeyedCodec<>("@BossWaveRepeatCount6", Codec.STRING), (d, v) -> d.bossWaveRepeatCount6 = v, d -> d.bossWaveRepeatCount6).add()
                .append(new KeyedCodec<>("@BossWaveRepeatSec6", Codec.STRING), (d, v) -> d.bossWaveRepeatSec6 = v, d -> d.bossWaveRepeatSec6).add()
                .append(new KeyedCodec<>("@TimedAnnounceServerWide", Codec.STRING), (d, v) -> d.timedAnnounceServerWide = v, d -> d.timedAnnounceServerWide).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorldWide", Codec.STRING), (d, v) -> d.timedAnnounceWorldWide = v, d -> d.timedAnnounceWorldWide).add()
                .append(new KeyedCodec<>("@TimedAnnounceText", Codec.STRING), (d, v) -> d.timedAnnounceText = v, d -> d.timedAnnounceText).add()

                .append(new KeyedCodec<>("@BossLootName1", Codec.STRING), (d, v) -> d.bossLootName1 = v, d -> d.bossLootName1).add()
                .append(new KeyedCodec<>("@BossLootMin1", Codec.STRING), (d, v) -> d.bossLootMin1 = v, d -> d.bossLootMin1).add()
                .append(new KeyedCodec<>("@BossLootMax1", Codec.STRING), (d, v) -> d.bossLootMax1 = v, d -> d.bossLootMax1).add()
                .append(new KeyedCodec<>("@BossLootChance1", Codec.STRING), (d, v) -> d.bossLootChance1 = v, d -> d.bossLootChance1).add()

                .append(new KeyedCodec<>("@BossLootName2", Codec.STRING), (d, v) -> d.bossLootName2 = v, d -> d.bossLootName2).add()
                .append(new KeyedCodec<>("@BossLootMin2", Codec.STRING), (d, v) -> d.bossLootMin2 = v, d -> d.bossLootMin2).add()
                .append(new KeyedCodec<>("@BossLootMax2", Codec.STRING), (d, v) -> d.bossLootMax2 = v, d -> d.bossLootMax2).add()
                .append(new KeyedCodec<>("@BossLootChance2", Codec.STRING), (d, v) -> d.bossLootChance2 = v, d -> d.bossLootChance2).add()

                .append(new KeyedCodec<>("@BossLootName3", Codec.STRING), (d, v) -> d.bossLootName3 = v, d -> d.bossLootName3).add()
                .append(new KeyedCodec<>("@BossLootMin3", Codec.STRING), (d, v) -> d.bossLootMin3 = v, d -> d.bossLootMin3).add()
                .append(new KeyedCodec<>("@BossLootMax3", Codec.STRING), (d, v) -> d.bossLootMax3 = v, d -> d.bossLootMax3).add()
                .append(new KeyedCodec<>("@BossLootChance3", Codec.STRING), (d, v) -> d.bossLootChance3 = v, d -> d.bossLootChance3).add()

                .append(new KeyedCodec<>("@BossLootName4", Codec.STRING), (d, v) -> d.bossLootName4 = v, d -> d.bossLootName4).add()
                .append(new KeyedCodec<>("@BossLootMin4", Codec.STRING), (d, v) -> d.bossLootMin4 = v, d -> d.bossLootMin4).add()
                .append(new KeyedCodec<>("@BossLootMax4", Codec.STRING), (d, v) -> d.bossLootMax4 = v, d -> d.bossLootMax4).add()
                .append(new KeyedCodec<>("@BossLootChance4", Codec.STRING), (d, v) -> d.bossLootChance4 = v, d -> d.bossLootChance4).add()

                .append(new KeyedCodec<>("@BossLootName5", Codec.STRING), (d, v) -> d.bossLootName5 = v, d -> d.bossLootName5).add()
                .append(new KeyedCodec<>("@BossLootMin5", Codec.STRING), (d, v) -> d.bossLootMin5 = v, d -> d.bossLootMin5).add()
                .append(new KeyedCodec<>("@BossLootMax5", Codec.STRING), (d, v) -> d.bossLootMax5 = v, d -> d.bossLootMax5).add()
                .append(new KeyedCodec<>("@BossLootChance5", Codec.STRING), (d, v) -> d.bossLootChance5 = v, d -> d.bossLootChance5).add()

                .append(new KeyedCodec<>("@BossLootName6", Codec.STRING), (d, v) -> d.bossLootName6 = v, d -> d.bossLootName6).add()
                .append(new KeyedCodec<>("@BossLootMin6", Codec.STRING), (d, v) -> d.bossLootMin6 = v, d -> d.bossLootMin6).add()
                .append(new KeyedCodec<>("@BossLootMax6", Codec.STRING), (d, v) -> d.bossLootMax6 = v, d -> d.bossLootMax6).add()
                .append(new KeyedCodec<>("@BossLootChance6", Codec.STRING), (d, v) -> d.bossLootChance6 = v, d -> d.bossLootChance6).add()

                .append(new KeyedCodec<>("@BossLootName7", Codec.STRING), (d, v) -> d.bossLootName7 = v, d -> d.bossLootName7).add()
                .append(new KeyedCodec<>("@BossLootMin7", Codec.STRING), (d, v) -> d.bossLootMin7 = v, d -> d.bossLootMin7).add()
                .append(new KeyedCodec<>("@BossLootMax7", Codec.STRING), (d, v) -> d.bossLootMax7 = v, d -> d.bossLootMax7).add()
                .append(new KeyedCodec<>("@BossLootChance7", Codec.STRING), (d, v) -> d.bossLootChance7 = v, d -> d.bossLootChance7).add()

                .append(new KeyedCodec<>("@BossLootName8", Codec.STRING), (d, v) -> d.bossLootName8 = v, d -> d.bossLootName8).add()
                .append(new KeyedCodec<>("@BossLootMin8", Codec.STRING), (d, v) -> d.bossLootMin8 = v, d -> d.bossLootMin8).add()
                .append(new KeyedCodec<>("@BossLootMax8", Codec.STRING), (d, v) -> d.bossLootMax8 = v, d -> d.bossLootMax8).add()
                .append(new KeyedCodec<>("@BossLootChance8", Codec.STRING), (d, v) -> d.bossLootChance8 = v, d -> d.bossLootChance8).add()
                .build();
        public String action;
        public String arenaName;
        public String arenaX;
        public String arenaY;
        public String arenaZ;
        public String arenaLootRadius1;
        public String arenaLootRadius2;
        public String arenaLootRadius3;
        public String arenaLootRadius4;
        public String arenaLootRadius5;
        public String arenaLootRadius6;
        public String arenaLootRadius7;
        public String arenaLootRadius8;
        public String shopEditArenaId;
        public String bossEditName;
        public String bossEditNpcId;
        public String bossEditTier;
        public String bossEditAmount;
        public String bossEditLevelOverride;
        public Float bossEditHp;
        public Float bossEditDamage;
        public String bossEditSpeed;
        public Float bossEditSize;
        public String bossEditAttackRate;
        public String bossEditAbilityCooldown;
        public String bossEditKnockbackGiven;
        public String bossEditKnockbackTaken;
        public String bossEditTurnRate;
        public String bossEditRegen;
        public Float bossEditPpHp;
        public Float bossEditPpDamage;
        public String bossEditPpSpeed;
        public Float bossEditPpSize;
        public String bossEditPpAttackRate;
        public String bossEditPpAbilityCooldown;
        public String bossEditPpKnockbackGiven;
        public String bossEditPpKnockbackTaken;
        public String bossEditPpTurnRate;
        public String bossEditPpRegen;
        public String bossEditWaves;
        public String bossEditExtraNpcId;
        public String bossEditExtraTimeLimit;
        public String bossEditExtraWaves;
        public String bossEditExtraMobsPerWave;
        public String bossSpawnTrigger;
        public String bossSpawnTriggerValue;
        public String bossWaveRandomLocations;
        public String bossWaveRandomRadius;
        public String bossTimedProximityEnabled;
        public String bossTimedProximityArena;
        public String bossTimedProximityRadius;
        public String bossTimedProximityCooldown;
        public String bossWaveTimeSec;
        public String bossWaveNpc1;
        public String bossWaveAmount1;
        public String bossWaveEvery1;
        public String bossWaveHp1;
        public String bossWaveDamage1;
        public String bossWaveSize1;
        public String bossWaveNpc2;
        public String bossWaveAmount2;
        public String bossWaveEvery2;
        public String bossWaveHp2;
        public String bossWaveDamage2;
        public String bossWaveSize2;
        public String bossWaveNpc3;
        public String bossWaveAmount3;
        public String bossWaveEvery3;
        public String bossWaveHp3;
        public String bossWaveDamage3;
        public String bossWaveSize3;
        public String bossWaveNpc4;
        public String bossWaveAmount4;
        public String bossWaveEvery4;
        public String bossWaveHp4;
        public String bossWaveDamage4;
        public String bossWaveSize4;
        public String bossWaveNpc5;
        public String bossWaveAmount5;
        public String bossWaveEvery5;
        public String bossWaveHp5;
        public String bossWaveDamage5;
        public String bossWaveSize5;
        public String bossWaveNpc6;
        public String bossWaveAmount6;
        public String bossWaveEvery6;
        public String bossWaveHp6;
        public String bossWaveDamage6;
        public String bossWaveSize6;
        public String bossWaveValue1;
        public String bossWaveRepeatCount1;
        public String bossWaveRepeatSec1;
        public String bossWaveValue2;
        public String bossWaveRepeatCount2;
        public String bossWaveRepeatSec2;
        public String bossWaveValue3;
        public String bossWaveRepeatCount3;
        public String bossWaveRepeatSec3;
        public String bossWaveValue4;
        public String bossWaveRepeatCount4;
        public String bossWaveRepeatSec4;
        public String bossWaveValue5;
        public String bossWaveRepeatCount5;
        public String bossWaveRepeatSec5;
        public String bossWaveValue6;
        public String bossWaveRepeatCount6;
        public String bossWaveRepeatSec6;
        public String timedAnnounceServerWide;
        public String timedAnnounceWorldWide;
        public String timedAnnounceText;
        public String bossLootName1;
        public String bossLootMin1;
        public String bossLootMax1;
        public String bossLootChance1;
        public String bossLootName2;
        public String bossLootMin2;
        public String bossLootMax2;
        public String bossLootChance2;
        public String bossLootName3;
        public String bossLootMin3;
        public String bossLootMax3;
        public String bossLootChance3;
        public String bossLootName4;
        public String bossLootMin4;
        public String bossLootMax4;
        public String bossLootChance4;
        public String bossLootName5;
        public String bossLootMin5;
        public String bossLootMax5;
        public String bossLootChance5;
        public String bossLootName6;
        public String bossLootMin6;
        public String bossLootMax6;
        public String bossLootChance6;
        public String bossLootName7;
        public String bossLootMin7;
        public String bossLootMax7;
        public String bossLootChance7;
        public String bossLootName8;
        public String bossLootMin8;
        public String bossLootMax8;
        public String bossLootChance8;

        public String getBossLootName(int row) {
            return switch (row) {
                case 1 -> bossLootName1;
                case 2 -> bossLootName2;
                case 3 -> bossLootName3;
                case 4 -> bossLootName4;
                case 5 -> bossLootName5;
                case 6 -> bossLootName6;
                case 7 -> bossLootName7;
                case 8 -> bossLootName8;
                default -> "";
            };
        }

        public String getBossLootMin(int row) {
            return switch (row) {
                case 1 -> bossLootMin1;
                case 2 -> bossLootMin2;
                case 3 -> bossLootMin3;
                case 4 -> bossLootMin4;
                case 5 -> bossLootMin5;
                case 6 -> bossLootMin6;
                case 7 -> bossLootMin7;
                case 8 -> bossLootMin8;
                default -> "";
            };
        }

        public String getBossLootMax(int row) {
            return switch (row) {
                case 1 -> bossLootMax1;
                case 2 -> bossLootMax2;
                case 3 -> bossLootMax3;
                case 4 -> bossLootMax4;
                case 5 -> bossLootMax5;
                case 6 -> bossLootMax6;
                case 7 -> bossLootMax7;
                case 8 -> bossLootMax8;
                default -> "";
            };
        }

        public String getBossLootChance(int row) {
            return switch (row) {
                case 1 -> bossLootChance1;
                case 2 -> bossLootChance2;
                case 3 -> bossLootChance3;
                case 4 -> bossLootChance4;
                case 5 -> bossLootChance5;
                case 6 -> bossLootChance6;
                case 7 -> bossLootChance7;
                case 8 -> bossLootChance8;
                default -> "";
            };
        }

        public String getBossWaveNpc(int row) {
            return switch (row) {
                case 1 -> bossWaveNpc1;
                case 2 -> bossWaveNpc2;
                case 3 -> bossWaveNpc3;
                case 4 -> bossWaveNpc4;
                case 5 -> bossWaveNpc5;
                case 6 -> bossWaveNpc6;
                default -> "";
            };
        }

        public String getBossWaveAmount(int row) {
            return switch (row) {
                case 1 -> bossWaveAmount1;
                case 2 -> bossWaveAmount2;
                case 3 -> bossWaveAmount3;
                case 4 -> bossWaveAmount4;
                case 5 -> bossWaveAmount5;
                case 6 -> bossWaveAmount6;
                default -> "";
            };
        }

        public String getBossWaveEvery(int row) {
            return switch (row) {
                case 1 -> bossWaveEvery1;
                case 2 -> bossWaveEvery2;
                case 3 -> bossWaveEvery3;
                case 4 -> bossWaveEvery4;
                case 5 -> bossWaveEvery5;
                case 6 -> bossWaveEvery6;
                default -> "";
            };
        }

        public String getBossWaveValue(int row) {
            return switch (row) {
                case 1 -> bossWaveValue1;
                case 2 -> bossWaveValue2;
                case 3 -> bossWaveValue3;
                case 4 -> bossWaveValue4;
                case 5 -> bossWaveValue5;
                case 6 -> bossWaveValue6;
                default -> "";
            };
        }

        public String getBossWaveRepeatCount(int row) {
            return switch (row) {
                case 1 -> bossWaveRepeatCount1;
                case 2 -> bossWaveRepeatCount2;
                case 3 -> bossWaveRepeatCount3;
                case 4 -> bossWaveRepeatCount4;
                case 5 -> bossWaveRepeatCount5;
                case 6 -> bossWaveRepeatCount6;
                default -> "";
            };
        }

        public String getBossWaveRepeatSec(int row) {
            return switch (row) {
                case 1 -> bossWaveRepeatSec1;
                case 2 -> bossWaveRepeatSec2;
                case 3 -> bossWaveRepeatSec3;
                case 4 -> bossWaveRepeatSec4;
                case 5 -> bossWaveRepeatSec5;
                case 6 -> bossWaveRepeatSec6;
                default -> "";
            };
        }

        public String getBossWaveHp(int row) {
            return switch (row) {
                case 1 -> bossWaveHp1;
                case 2 -> bossWaveHp2;
                case 3 -> bossWaveHp3;
                case 4 -> bossWaveHp4;
                case 5 -> bossWaveHp5;
                case 6 -> bossWaveHp6;
                default -> "";
            };
        }

        public String getBossWaveDamage(int row) {
            return switch (row) {
                case 1 -> bossWaveDamage1;
                case 2 -> bossWaveDamage2;
                case 3 -> bossWaveDamage3;
                case 4 -> bossWaveDamage4;
                case 5 -> bossWaveDamage5;
                case 6 -> bossWaveDamage6;
                default -> "";
            };
        }

        public String getBossWaveSize(int row) {
            return switch (row) {
                case 1 -> bossWaveSize1;
                case 2 -> bossWaveSize2;
                case 3 -> bossWaveSize3;
                case 4 -> bossWaveSize4;
                case 5 -> bossWaveSize5;
                case 6 -> bossWaveSize6;
                default -> "";
            };
        }

        public String getArenaLootRadius(int row) {
            return switch (row) {
                case 1 -> arenaLootRadius1;
                case 2 -> arenaLootRadius2;
                case 3 -> arenaLootRadius3;
                case 4 -> arenaLootRadius4;
                case 5 -> arenaLootRadius5;
                case 6 -> arenaLootRadius6;
                case 7 -> arenaLootRadius7;
                case 8 -> arenaLootRadius8;
                default -> "";
            };
        }
    }
}
