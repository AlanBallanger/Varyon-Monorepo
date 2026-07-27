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
import com.varyon.bossarena.music.BossFightMusicService;
import com.varyon.bossarena.shop.BossShopConfig;
import com.varyon.bossarena.shop.ShopEntry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.varyon.bossarena.util.BossRegen;
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
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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
    private static final String TAB_ARENAS = "arenas";
    private static final String TAB_PLANIFICATION = "planification";
    private static final String TAB_SHOP = "shop";

    private static final int MAX_ARENA_ROWS = 8;
    private static final int MAX_SHOP_ROWS = 8;
    private static final int MAX_SHOP_CONTRACT_ROWS = 8;
    private static final int MAX_BOSS_ROWS = 8;
    private static final int MAX_LOOT_ROWS = 8;
    private static final int MAX_LOOT_VISIBLE_ROWS = 8;
    private static final int MAX_WAVE_ADD_ROWS = 6;
    private static final float MULT_HP_DMG_MIN = 0.50f;
    private static final float MULT_HP_DMG_MAX = 50.00f;
    private static final float MULT_SIZE_MIN = 0.10f;
    private static final float MULT_SIZE_MAX = 10.00f;
    private static final float MULT_PERS_MIN = 0.50f;
    private static final float MULT_PERS_MAX = 4.00f;
    private static final float MULT_PERS_STEP = 0.10f;
    private static final float MULT_SCALE_MIN = 0.10f;
    private static final float MULT_SCALE_MAX = 10.00f;
    private static final float REGEN_MIN = BossRegen.MIN_HP_PER_SECOND;
    private static final float REGEN_MAX = BossRegen.MAX_HP_PER_SECOND;
    private static final int MAX_TIMED_SPAWN_ROWS = 6;
    private static final int MAX_BOSS_POOL_ROWS = 8;
    private static final int BOSS_SCROLL_THUMB_STEPS = 10;
    private static final Pattern ARENA_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final int MAX_SHOP_CURRENCY_PICKS = BossArenaConfigUiControls.MAX_ITEM_PICKS;

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
    /** Index (0-based) of the ScheduledWave currently being edited in the waves overlay. */
    private int bossWavePageIndex;
    /** Offset into the unbounded loot list for the visible loot window. */
    private int lootListOffset;
    /** NPC ID suggestion list visible only while actively searching (typing). */
    private boolean bossNpcPicksOpen;
    /** Wave schedule NPC suggestions: open while typing on a specific row. */
    private boolean waveNpcPicksOpen;
    private int waveNpcPicksRow;
    private String waveNpcSearchQuery = "";
    private ShopLocationEditorState shopLocationEditorState;
    /** Currency item suggestions while typing in the shop editor. */
    private boolean shopCurrencyPicksOpen;
    /** Weighted boss-pool editor for a planification rule row. */
    private BossPoolEditorState bossPoolEditorState;

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
            return "Avant le boss";
        }
        if (row == 1) {
            return "1";
        }
        if (row == 2) {
            return "Avant le boss";
        }
        return "Avant le boss";
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
        String raw = optionalText(input).trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS.equals(raw)
                || BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS.equals(raw)
                || BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE.equals(raw)
                || BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(raw)) {
            return raw;
        }
        if (BossDefinition.ExtraMobs.TRIGGER_ON_SPAWN.equals(raw)) {
            return BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS;
        }
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
            // "Avec le boss" merges old Au spawn + Après spawn: delay in Sec (0 = immédiat).
            case "2", "on spawn", "spawn", "at spawn", "on boss spawn", "with boss", "os",
                 "au spawn", "a spawn", "avec boss", "avec le boss",
                 "after spawn seconds", "after spawn", "after", "seconds", "time", "timer", "after spawn second",
                 "apres spawn", "après spawn", "apres", "après", "secondes",
                 "apres spawn secondes", "après spawn secondes",
                 "3", "as", "after s" ->
                    BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS;
            case "since last wave", "since last wave second", "since last wave seconds", "since wave", "last wave",
                 "wave delay", "since previous wave", "since last",
                 "depuis derniere vague", "depuis dernière vague", "depuis vague",
                 "derniere vague", "dernière vague", "delai vague", "délai vague",
                 "depuis derniere", "depuis dernière",
                 "apres vague precedente", "après vague précédente", "apres vague précédente",
                 "après vague precedente", "vague precedente", "vague précédente",
                 "4", "slw", "since", "depuis" -> BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE;
            case "boss hp percent", "boss health percent", "hp percent", "health percent", "boss hp", "boss health",
                 "hp", "pv boss", "pv boss percent", "pv percent", "vie boss",
                 "5", "hpp", "health" -> BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT;
            default -> null;
        };
    }

    /** Dropdown / storage id shown in the wave editor (on_spawn folded into after_spawn_seconds). */
    private static String toWaveTriggerDropdownValue(String trigger) {
        String normalized = normalizeWaveTriggerInput(trigger);
        if (normalized == null || normalized.isBlank()) {
            return BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS;
        }
        return normalized;
    }

    private static String toWaveTriggerDisplayName(String trigger) {
        String normalized = normalizeWaveTriggerInput(trigger);
        if (BossDefinition.ExtraMobs.TRIGGER_AFTER_SPAWN_SECONDS.equals(normalized)) {
            return "Avec le boss";
        }
        if (BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE.equals(normalized)) {
            return "Après vague précédente";
        }
        if (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(normalized)) {
            return "PV boss %";
        }
        return "Avant le boss";
    }

    private static String waveTriggerValueUnitLabel(String trigger) {
        return BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(normalizeWaveTriggerInput(trigger))
                ? "%"
                : "Sec";
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
                BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS,
                0.0d,
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

    private static void applyBossSpawnSpreadFromData(BossDefinition boss, ConfigEventData data) {
        if (boss == null || data == null) {
            return;
        }

        String resolvedRandom = resolvedOrFallback(
                data.bossSpawnSpreadRandom,
                boss.useRandomBossSpawn ? "true" : "false"
        );
        if (!resolvedRandom.isEmpty()) {
            Boolean randomEnabled = parseToggleInput(resolvedRandom);
            if (randomEnabled == null) {
                throw new IllegalArgumentException("Le spread boss aléatoire doit être true/false, on/off, yes/no, oui/non ou 1/0.");
            }
            boss.useRandomBossSpawn = randomEnabled;
        }

        String resolvedRadius = resolvedOrFallback(
                data.bossSpawnSpreadRadius,
                formatWaveNumber(boss.getSpawnSpreadRadius())
        );
        if (!resolvedRadius.isEmpty()) {
            boss.spawnSpreadRadius = parseRequiredDouble(
                    resolvedRadius,
                    "Le rayon de spread boss doit être un nombre >= 0.",
                    0.0d,
                    Double.MAX_VALUE
            );
        }
    }

    private void handleBossSpawnSpreadToggle(ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        try {
            applyBossSpawnSpreadFromData(bossEditorState.boss, data);
            applyWaveSpawnSettingsFromData(
                    bossEditorState.boss.extraMobs != null ? bossEditorState.boss.extraMobs : new BossDefinition.ExtraMobs(),
                    data
            );
            bossEditorState.boss.useRandomBossSpawn = !bossEditorState.boss.useRandomBossSpawn;
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
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
        extra.sanitize();
    }

    private static String toBossSpawnTriggerDisplayName(String trigger) {
        if (BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_BEFORE_BOSS.equals(trigger)) {
            return "Fin des vagues";
        }
        if (BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_SECONDS.equals(trigger)) {
            return "Après délai";
        }
        return trigger != null ? trigger : "Fin des vagues";
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
                || normalized.contains("après pré-boss")
                || normalized.contains("fin des vagues")
                || normalized.contains("fin vagues")
                || normalized.equals("fin des vagues")
                || normalized.contains("after before boss")) {
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
    private static List<DropdownEntryInfo> musicDropdownEntries(List<String> musicFiles) {
        List<DropdownEntryInfo> entries = new ArrayList<>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("(aucune)"), ""));
        if (musicFiles != null) {
            for (String file : musicFiles) {
                if (file == null || file.isBlank()) {
                    continue;
                }
                entries.add(new DropdownEntryInfo(LocalizableString.fromString(file), file));
            }
        }
        return entries;
    }

    private static List<DropdownEntryInfo> arenaDropdownEntries() {
        List<Arena> arenas = new ArrayList<>(ArenaRegistry.getAll());
        arenas.sort(Comparator.comparing(a -> a.arenaId == null ? "" : a.arenaId, String.CASE_INSENSITIVE_ORDER));
        List<DropdownEntryInfo> entries = new ArrayList<>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("(arène)"), ""));
        for (Arena arena : arenas) {
            String id = safeText(arena.arenaId);
            if (id.isEmpty()) {
                continue;
            }
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        }
        return entries;
    }

    @Nonnull
    private static List<DropdownEntryInfo> bossDropdownEntries() {
        List<BossDefinition> bosses = new ArrayList<>(BossRegistry.getAll().values());
        bosses.sort(Comparator.comparing(
                b -> b == null || b.bossName == null ? "" : b.bossName,
                String.CASE_INSENSITIVE_ORDER
        ));
        List<DropdownEntryInfo> entries = new ArrayList<>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("(boss)"), ""));
        for (BossDefinition boss : bosses) {
            if (boss == null) {
                continue;
            }
            String id = safeText(boss.bossName);
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

    private static float requirePerPlayerSlider(Float value, float fallback, String label) {
        float raw = (value != null && Float.isFinite(value)) ? value : fallback;
        if (!Float.isFinite(raw)) {
            throw new IllegalArgumentException(label + " doit être entre 0.00 et 4.00 (pas de 0.10).");
        }
        // Clamp legacy values above the new max (was 5.00) instead of failing save.
        float clamped = BossArenaConfigUiControls.clampFloat(raw, MULT_PERS_MIN, MULT_PERS_MAX);
        return Math.round(clamped / MULT_PERS_STEP) * MULT_PERS_STEP;
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
        out.musicFileName = source != null && source.musicFileName != null ? source.musicFileName : "";
        out.musicRadius = source != null ? source.getMusicRadius() : 30.0d;
        out.useRandomBossSpawn = source != null && source.useRandomBossSpawn;
        out.spawnSpreadRadius = source != null ? source.getSpawnSpreadRadius() : 15.0d;

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
            out.extraMobs.wavesEnabled = source.extraMobs.wavesEnabled;
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
                    waveCopy.enabled = wave.enabled;
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

    private static final int MAX_WAVE_SUMMARY_LINES = 8;

    private static BossWavesSummary buildBossWavesSummary(BossDefinition.ExtraMobs extra) {
        String[] lines = new String[MAX_WAVE_SUMMARY_LINES];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = "";
        }
        if (extra == null) {
            lines[0] = "Aucun planning de vagues.";
            lines[1] = "Appuyez sur Éditer pour ajouter des vagues.";
            return new BossWavesSummary(lines);
        }
        extra.sanitize();

        List<BossDefinition.ExtraMobs.ScheduledWave> schedule = extra.getAllScheduledWaves();
        if (schedule.isEmpty()) {
            lines[0] = "Aucun planning de vagues.";
            lines[1] = "Appuyez sur Éditer pour ajouter des vagues.";
            return new BossWavesSummary(lines);
        }

        int visible = Math.min(MAX_WAVE_SUMMARY_LINES, schedule.size());
        for (int i = 0; i < visible; i++) {
            lines[i] = formatWaveSummaryLine(i + 1, schedule.get(i));
        }
        if (schedule.size() > MAX_WAVE_SUMMARY_LINES) {
            int last = MAX_WAVE_SUMMARY_LINES - 1;
            lines[last] = lines[last] + " | +" + (schedule.size() - MAX_WAVE_SUMMARY_LINES);
        }
        return new BossWavesSummary(lines);
    }

    private static String formatWaveSummaryLine(int index, BossDefinition.ExtraMobs.ScheduledWave wave) {
        String trigger = toWaveTriggerDisplayName(wave != null ? wave.trigger : null);
        String addsSummary = formatWaveAddsSummary(wave);
        String line;
        if (addsSummary.isEmpty()) {
            line = index + " : " + trigger;
        } else {
            line = index + " : " + trigger + " | " + addsSummary;
        }
        if (wave != null && !wave.enabled) {
            return "[OFF] " + line;
        }
        return line;
    }

    private static String formatWaveAddsSummary(BossDefinition.ExtraMobs.ScheduledWave wave) {
        if (wave == null || wave.adds == null || wave.adds.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (BossDefinition.ExtraMobs.WaveAdd add : wave.adds) {
            String part = formatWaveAdd(add);
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return parts.get(0) + " +" + (parts.size() - 1);
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
        if (TAB_PLANIFICATION.equals(tab)) {
            return TAB_PLANIFICATION;
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
        cmd.set("#SubtitleLabel.Text", "Configurer boss, marchands et arènes");

        boolean bossesTab = TAB_BOSSES.equals(selectedTab);
        boolean arenasTab = TAB_ARENAS.equals(selectedTab);
        boolean planificationTab = TAB_PLANIFICATION.equals(selectedTab);
        boolean shopTab = TAB_SHOP.equals(selectedTab);

        cmd.set("#TabIndicatorBosses.Visible", bossesTab);
        cmd.set("#TabIndicatorArenas.Visible", arenasTab);
        cmd.set("#TabIndicatorPlanification.Visible", planificationTab);
        cmd.set("#TabIndicatorShop.Visible", shopTab);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossesTab", EventData.of("Action", "tab_" + TAB_BOSSES));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenasTab", EventData.of("Action", "tab_" + TAB_ARENAS));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PlanificationTab", EventData.of("Action", "tab_" + TAB_PLANIFICATION));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopTab", EventData.of("Action", "tab_" + TAB_SHOP));

        cmd.set("#BossesPanel.Visible", bossesTab);
        cmd.set("#ArenasPanel.Visible", arenasTab);
        cmd.set("#PlanificationPanel.Visible", planificationTab);
        cmd.set("#ShopPanel.Visible", shopTab);

        cmd.set("#StatusLabel.Visible", false);
        cmd.set("#StatusLabel.Text", "");

        if (bossesTab) {
            buildBossesTab(cmd, events);
        }

        if (arenasTab) {
            buildArenasTab(cmd, events);
        }

        if (planificationTab) {
            buildBossTimedOverlay(cmd, events);
        }

        if (shopTab) {
            buildShopTab(ref, store, cmd, events);
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

        if (TAB_PLANIFICATION.equals(selectedTab)) {
            if ("boss_timed_save".equals(action)) {
                handleBossTimedSave(data);
            } else if ("timed_add".equals(action)) {
                handleTimedAdd();
            } else if (action.startsWith("timed_delete_")) {
                handleTimedDelete(action.substring("timed_delete_".length()));
            } else if (action.startsWith("timed_toggle_enabled_")) {
                handleTimedFieldToggle(action.substring("timed_toggle_enabled_".length()), data, "enabled");
            } else if (action.startsWith("timed_toggle_mode_")) {
                handleTimedFieldToggle(action.substring("timed_toggle_mode_".length()), data, "mode");
            } else if (action.startsWith("timed_toggle_require_")) {
                handleTimedFieldToggle(action.substring("timed_toggle_require_".length()), data, "require");
            } else if (action.startsWith("timed_toggle_grace_")) {
                handleTimedFieldToggle(action.substring("timed_toggle_grace_".length()), data, "grace");
            } else if (action.startsWith("timed_toggle_announce_global_")) {
                handleTimedFieldToggle(action.substring("timed_toggle_announce_global_".length()), data, "announce_global");
            } else if (action.startsWith("timed_toggle_announce_world_")) {
                handleTimedFieldToggle(action.substring("timed_toggle_announce_world_".length()), data, "announce_world");
            } else if (action.startsWith("timed_pop_")) {
                handleTimedPop(action.substring("timed_pop_".length()));
            } else if (action.startsWith("timed_pool_open_")) {
                handleTimedPoolOpen(action.substring("timed_pool_open_".length()), data);
            } else if ("timed_pool_close".equals(action)) {
                bossPoolEditorState = null;
                rebuild();
            } else if ("timed_pool_apply".equals(action)) {
                handleTimedPoolApply();
            } else if ("timed_pool_add".equals(action)) {
                handleTimedPoolAdd(data);
            } else if ("timed_pool_prev".equals(action)) {
                handleTimedPoolPage(-1);
            } else if ("timed_pool_next".equals(action)) {
                handleTimedPoolPage(1);
            } else if (action.startsWith("timed_pool_toggle_")) {
                handleTimedPoolToggle(action.substring("timed_pool_toggle_".length()));
            } else if (action.startsWith("timed_pool_wdec_")) {
                handleTimedPoolWeight(action.substring("timed_pool_wdec_".length()), -1);
            } else if (action.startsWith("timed_pool_winc_")) {
                handleTimedPoolWeight(action.substring("timed_pool_winc_".length()), 1);
            }
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
            cmd.set("#ShopOverflowLabel.Text", "+" + (shopLocations.size() - MAX_SHOP_ROWS) + " marchands non affichés sur cette page.");
        } else {
            cmd.set("#ShopOverflowLabel.Text", "");
        }

        cmd.set("#ShopEmptyLabel.Visible", shopLocations.isEmpty());
        if (shopLocations.isEmpty()) {
            cmd.set("#ShopEmptyLabel.Text", "Aucun marchand configuré.");
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
            cmd.set("#ShopWorld" + suffix + ".Text", safeText(shopLocation.worldName));
            cmd.set("#ShopArena" + suffix + ".Text", shopLocation.arenaLabel);
            cmd.set("#ShopDistance" + suffix + ".Text", shopLocation.distanceLabel);
            cmd.set("#ShopBosses" + suffix + ".Text", Integer.toString(shopLocation.contractCount));
            cmd.set("#ShopEntries" + suffix + ".Text", Integer.toString(shopLocation.contractCount));
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

        List<ShopLocationView> out = new ArrayList<>();
        for (BossShopConfig.ShopLocation location : shopConfig.shops) {
            if (location == null) {
                continue;
            }
            String locationWorld = optionalText(location.worldName);
            if (locationWorld.isEmpty()) {
                continue;
            }

            int contractCount = location.contracts == null ? 0 : location.contracts.size();

            Double distance = null;
            boolean sameWorld = currentWorld != null && locationWorld.equalsIgnoreCase(currentWorld);
            if (sameWorld && playerPosition != null) {
                double dx = playerPosition.x - location.x;
                double dy = playerPosition.y - location.y;
                double dz = playerPosition.z - location.z;
                distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
            }

            String shopName = optionalText(location.name);
            if (shopName.isEmpty()) {
                String uuid = optionalText(location.uuid);
                if (!uuid.isEmpty()) {
                    shopName = "Marchand " + (uuid.length() > 8 ? uuid.substring(0, 8) : uuid);
                } else {
                    shopName = "Marchand";
                }
            }

            String arenaLabel = safeText(shopName) + " | (" + location.x + ", " + location.y + ", " + location.z + ")";
            String firstArenaId = firstShopContractArenaId(location);
            if (!firstArenaId.isEmpty()) {
                arenaLabel = arenaLabel + " | " + safeText(firstArenaId);
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
                    contractCount
            ));
        }

        final String playerWorld = currentWorld;
        out.sort(Comparator
                .comparing((ShopLocationView row) -> playerWorld == null
                        || !optionalText(row.worldName).equalsIgnoreCase(playerWorld))
                .thenComparing(row -> row.distance == null)
                .thenComparing(row -> row.distance == null ? Double.MAX_VALUE : row.distance)
                .thenComparing(row -> optionalText(row.worldName).toLowerCase(Locale.ROOT))
                .thenComparing(row -> row.arenaLabel.toLowerCase(Locale.ROOT)));

        // Nearest merchant in the player's current world.
        if (playerWorld != null) {
            for (int i = 0; i < out.size(); i++) {
                ShopLocationView row = out.get(i);
                if (row.distance == null || !playerWorld.equalsIgnoreCase(row.worldName)) {
                    continue;
                }
                out.set(i, new ShopLocationView(
                        row.arenaLabel + " (plus proche)",
                        row.distanceLabel,
                        row.distance,
                        row.worldName,
                        row.x,
                        row.y,
                        row.z,
                        row.contractCount
                ));
                break;
            }
        }

        shopStatusText = "Tous les marchands (tous les mondes). Monde actuel en premier.";
        return out;
    }

    private static String firstShopContractArenaId(BossShopConfig.ShopLocation location) {
        if (location == null || location.contracts == null) {
            return "";
        }
        for (BossShopConfig.ShopContract contract : location.contracts) {
            if (contract == null) {
                continue;
            }
            String arenaId = optionalText(contract.arenaId);
            if (!arenaId.isEmpty()) {
                return arenaId;
            }
        }
        return "";
    }

    private void handleShopAction(String action, ConfigEventData data) {
        if (action == null || action.isBlank()) {
            return;
        }

        if (action.startsWith("shop_edit_open_")) {
            handleShopEditOpen(action.substring("shop_edit_open_".length()));
            return;
        }

        if ("shop_edit_add".equals(action)) {
            handleShopEditAdd(data);
            return;
        }

        if (action.startsWith("shop_edit_remove_")) {
            handleShopEditRemove(action.substring("shop_edit_remove_".length()), data);
            return;
        }

        if ("shop_currency_filter".equals(action)) {
            handleShopCurrencyFilter(data);
            return;
        }

        if (action.startsWith("shop_currency_pick_")) {
            handleShopCurrencyPick(action.substring("shop_currency_pick_".length()), data);
            return;
        }

        if ("shop_edit_save".equals(action)) {
            handleShopEditSave(data);
            return;
        }

        if ("shop_edit_close".equals(action)) {
            shopLocationEditorState = null;
            shopCurrencyPicksOpen = false;
            rebuild();
        }
    }

    private void handleShopEditOpen(String rowToken) {
        int row = parseRow(rowToken);
        if (row < 1 || row > shopRows.size()) {
            shopStatusText = "Sélection de ligne marchand invalide.";
            rebuild();
            return;
        }

        ShopLocationRef shopLocation = shopRows.get(row - 1);
        BossShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null) {
            shopStatusText = "Config marchands indisponible.";
            rebuild();
            return;
        }

        BossShopConfig.ShopLocation location = shopConfig.getShopLocation(shopLocation.worldName, shopLocation.x, shopLocation.y, shopLocation.z);
        if (location == null) {
            shopStatusText = "Le marchand sélectionné n'existe plus dans la config.";
            rebuild();
            return;
        }

        List<ShopContractDraft> drafts = new ArrayList<>();
        if (location.contracts != null) {
            for (BossShopConfig.ShopContract contract : location.contracts) {
                if (contract == null) {
                    continue;
                }
                ShopContractDraft draft = new ShopContractDraft();
                draft.bossId = optionalText(contract.bossId);
                draft.arenaId = optionalText(contract.arenaId);
                draft.cost = Math.max(0, contract.cost);
                draft.silentCost = Math.max(0, contract.silentCost);
                drafts.add(draft);
            }
        }

        String currencyItemId = "";
        if (shopConfig.currencyItemId != null && !shopConfig.currencyItemId.isBlank()) {
            currencyItemId = shopConfig.currencyItemId.trim();
        } else if (plugin.getConfig() != null && plugin.getConfig().fallbackCurrencyItemId != null) {
            currencyItemId = plugin.getConfig().fallbackCurrencyItemId.trim();
        }
        if (currencyItemId.isEmpty()) {
            currencyItemId = "Ingredient_Bar_Iron";
        }

        shopLocationEditorState = new ShopLocationEditorState(
                shopLocation,
                drafts,
                optionalText(location.name),
                currencyItemId
        );
        shopCurrencyPicksOpen = false;
        shopStatusText = "";
        rebuild();
    }

    private void handleShopEditAdd(ConfigEventData data) {
        if (shopLocationEditorState == null) {
            return;
        }
        syncShopContractDraftsFromData(data);
        if (shopLocationEditorState.contracts.size() >= MAX_SHOP_CONTRACT_ROWS) {
            shopStatusText = "Maximum de " + MAX_SHOP_CONTRACT_ROWS + " contrats par marchand.";
            rebuild();
            return;
        }
        shopLocationEditorState.contracts.add(new ShopContractDraft());
        shopStatusText = "";
        rebuild();
    }

    private void handleShopEditRemove(String rowToken, ConfigEventData data) {
        if (shopLocationEditorState == null) {
            return;
        }
        syncShopContractDraftsFromData(data);

        int row = parseRow(rowToken);
        if (row < 1 || row > MAX_SHOP_CONTRACT_ROWS) {
            shopStatusText = "Ligne de contrat invalide.";
            rebuild();
            return;
        }

        int index = shopLocationEditorState.listOffset + row - 1;
        if (index < 0 || index >= shopLocationEditorState.contracts.size()) {
            shopStatusText = "Ligne de contrat invalide.";
            rebuild();
            return;
        }

        shopLocationEditorState.contracts.remove(index);
        shopStatusText = "";
        rebuild();
    }

    private void handleShopCurrencyFilter(ConfigEventData data) {
        if (shopLocationEditorState == null) {
            return;
        }
        syncShopContractDraftsFromData(data);
        String query = optionalText(data != null ? data.shopEditCurrencyItem : null);
        shopLocationEditorState.currencyItemId = query;
        // Soft-update only (like boss NPC search) so the text field keeps focus while typing.
        shopCurrencyPicksOpen = !query.isEmpty() && !BossArenaConfigUiControls.isExactItemId(query);
        shopStatusText = "";
        softUpdateShopCurrencyPicks(false);
    }

    private void handleShopCurrencyPick(String pickToken, ConfigEventData data) {
        if (shopLocationEditorState == null) {
            return;
        }
        syncShopContractDraftsFromData(data);
        int pick = parseRow(pickToken);
        if (pick < 1 || pick > MAX_SHOP_CURRENCY_PICKS) {
            return;
        }
        String filter = optionalText(shopLocationEditorState.currencyItemId);
        List<String> picks = BossArenaConfigUiControls.filterItemIds(filter, MAX_SHOP_CURRENCY_PICKS);
        if (pick > picks.size()) {
            return;
        }
        shopLocationEditorState.currencyItemId = picks.get(pick - 1);
        shopCurrencyPicksOpen = false;
        shopStatusText = "";
        softUpdateShopCurrencyPicks(true);
    }

    private void softUpdateShopCurrencyPicks(boolean syncFieldValue) {
        if (shopLocationEditorState == null) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        if (syncFieldValue) {
            cmd.set("#ShopEditorCurrencyItem.Value", optionalText(shopLocationEditorState.currencyItemId));
        }
        applyShopCurrencyPicks(cmd, null, false);
        applyShopContractLayoutForCurrencyPicks(cmd);
        sendUpdate(cmd, false);
    }

    private void applyShopCurrencyPicks(UICommandBuilder cmd, UIEventBuilder events, boolean bindEvents) {
        List<String> currencyPicks = List.of();
        if (shopLocationEditorState != null && shopCurrencyPicksOpen) {
            String query = optionalText(shopLocationEditorState.currencyItemId).trim();
            if (!query.isEmpty() && !BossArenaConfigUiControls.isExactItemId(query)) {
                currencyPicks = BossArenaConfigUiControls.filterItemIds(query, MAX_SHOP_CURRENCY_PICKS);
            }
        }

        int pickTop = 206;
        for (int i = 1; i <= MAX_SHOP_CURRENCY_PICKS; i++) {
            String pickId = "#ShopCurrencyPick" + i;
            boolean visible = i <= currencyPicks.size();

            Anchor anchor = new Anchor();
            anchor.setLeft(Value.of(520));
            anchor.setTop(Value.of(pickTop + (i - 1) * 20));
            anchor.setWidth(Value.of(400));
            anchor.setHeight(Value.of(20));
            cmd.setObject(pickId + ".Anchor", anchor);
            cmd.set(pickId + ".Visible", visible);
            if (visible) {
                cmd.set(pickId + ".Text", currencyPicks.get(i - 1));
            }
            // Always bind: picks start hidden, soft-update only toggles Visible/Text.
            if (bindEvents && events != null) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        pickId,
                        buildShopEditSnapshotEvent("shop_currency_pick_" + i),
                        false
                );
            }
        }
    }

    /** Push contract headers/rows below the currency suggestion list when it is open. */
    private void applyShopContractLayoutForCurrencyPicks(UICommandBuilder cmd) {
        int extra = shopCurrencyPicksOpen ? (MAX_SHOP_CURRENCY_PICKS * 20 + 8) : 0;
        int headerTop = 214 + extra;
        int row1Top = 240 + extra;
        int rowStride = 34;

        setShopOverlayAnchor(cmd, "#ShopEditHeaderBoss", 98, headerTop, 280, 22);
        setShopOverlayAnchor(cmd, "#ShopEditHeaderArena", 390, headerTop, 220, 22);
        setShopOverlayAnchor(cmd, "#ShopEditHeaderPrice", 626, headerTop, 90, 22);
        setShopOverlayAnchor(cmd, "#ShopEditHeaderSilentPrice", 722, headerTop, 100, 22);

        for (int row = 1; row <= MAX_SHOP_CONTRACT_ROWS; row++) {
            setShopOverlayAnchor(
                    cmd,
                    "#ShopEditContractRow" + row,
                    98,
                    row1Top + (row - 1) * rowStride,
                    980,
                    30
            );
        }
        setShopOverlayAnchor(cmd, "#ShopEditorOverflowLabel", 98, row1Top + MAX_SHOP_CONTRACT_ROWS * rowStride + 4, 700, 22);
    }

    private static void setShopOverlayAnchor(
            UICommandBuilder cmd,
            String selector,
            int left,
            int top,
            int width,
            int height
    ) {
        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(left));
        anchor.setTop(Value.of(top));
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        cmd.setObject(selector + ".Anchor", anchor);
    }

    private void syncShopContractDraftsFromData(ConfigEventData data) {
        if (shopLocationEditorState == null || data == null) {
            return;
        }
        if (data.shopEditVendorName != null) {
            shopLocationEditorState.vendorName = optionalText(data.shopEditVendorName);
        }
        if (data.shopEditCurrencyItem != null) {
            shopLocationEditorState.currencyItemId = optionalText(data.shopEditCurrencyItem);
        }
        List<ShopContractDraft> contracts = shopLocationEditorState.contracts;
        int offset = shopLocationEditorState.listOffset;
        for (int row = 1; row <= MAX_SHOP_CONTRACT_ROWS; row++) {
            int index = offset + row - 1;
            if (index < 0 || index >= contracts.size()) {
                continue;
            }
            ShopContractDraft draft = contracts.get(index);
            draft.bossId = optionalText(data.getShopEditBoss(row));
            draft.arenaId = optionalText(data.getShopEditArena(row));
            Integer parsedCost = parseOptionalInt(data.getShopEditPrice(row));
            if (parsedCost != null) {
                draft.cost = Math.max(0, parsedCost);
            }
            Integer parsedSilentCost = parseOptionalInt(data.getShopEditSilentPrice(row));
            if (parsedSilentCost != null) {
                draft.silentCost = Math.max(0, parsedSilentCost);
            }
        }
    }

    private EventData buildShopEditSnapshotEvent(String action) {
        EventData event = new EventData().append("Action", action);
        event.append("@ShopEditVendorName", "#ShopEditorVendorName.Value");
        event.append("@ShopEditCurrencyItem", "#ShopEditorCurrencyItem.Value");
        for (int row = 1; row <= MAX_SHOP_CONTRACT_ROWS; row++) {
            String suffix = Integer.toString(row);
            event.append("@ShopEditBoss" + suffix, "#ShopEditBoss" + suffix + ".Value");
            event.append("@ShopEditArena" + suffix, "#ShopEditArena" + suffix + ".Value");
            event.append("@ShopEditBossPrice" + suffix, "#ShopEditBossPrice" + suffix + ".Value");
            event.append("@ShopEditSilentPrice" + suffix, "#ShopEditSilentPrice" + suffix + ".Value");
        }
        return event;
    }

    private void handleShopEditSave(ConfigEventData data) {
        if (shopLocationEditorState == null) {
            return;
        }

        BossShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null) {
            shopStatusText = "Config marchands indisponible.";
            rebuild();
            return;
        }

        ShopLocationRef shopLocation = shopLocationEditorState.shopLocation;
        BossShopConfig.ShopLocation location = shopConfig.getShopLocation(shopLocation.worldName, shopLocation.x, shopLocation.y, shopLocation.z);
        if (location == null) {
            shopStatusText = "Le marchand sélectionné n'existe plus dans la config.";
            rebuild();
            return;
        }

        syncShopContractDraftsFromData(data);
        String configuredVendorName = resolvedOrFallback(
                data != null ? data.shopEditVendorName : null,
                shopLocationEditorState.vendorName
        );
        shopLocationEditorState.vendorName = configuredVendorName;
        String currencyItemId = resolvedOrFallback(
                data != null ? data.shopEditCurrencyItem : null,
                shopLocationEditorState.currencyItemId
        ).trim();
        if (currencyItemId.isEmpty()) {
            shopStatusText = "Indiquez un item monnaie (ex: Ingredient_Bar_Iron).";
            rebuild();
            return;
        }
        if (!BossArenaConfigUiControls.isExactItemId(currencyItemId)) {
            shopStatusText = "Item monnaie inconnu: " + currencyItemId;
            rebuild();
            return;
        }
        shopLocationEditorState.currencyItemId = currencyItemId;

        List<BossShopConfig.ShopContract> saved = new ArrayList<>();
        Set<String> seenPairs = new LinkedHashSet<>();
        for (ShopContractDraft draft : shopLocationEditorState.contracts) {
            if (draft == null) {
                continue;
            }
            String bossId = optionalText(draft.bossId);
            String arenaId = optionalText(draft.arenaId);
            if (bossId.isEmpty() || arenaId.isEmpty()) {
                shopStatusText = "Chaque contrat doit préciser un boss et une arène.";
                rebuild();
                return;
            }
            String pairKey = bossId.toLowerCase(Locale.ROOT) + "\0" + arenaId.toLowerCase(Locale.ROOT);
            if (!seenPairs.add(pairKey)) {
                shopStatusText = "Contrat en double (même boss et même arène).";
                rebuild();
                return;
            }
            BossShopConfig.ShopContract contract = new BossShopConfig.ShopContract();
            contract.bossId = bossId;
            contract.arenaId = arenaId;
            contract.cost = Math.max(0, draft.cost);
            contract.silentCost = Math.max(0, draft.silentCost);
            saved.add(contract);
        }

        location.name = configuredVendorName.isEmpty()
                ? ("Marchand " + shopLocation.x + "," + shopLocation.y + "," + shopLocation.z)
                : configuredVendorName;
        location.contracts = saved;
        location.arenaId = "";
        location.enabledBossIds = new ArrayList<>();
        location.contractPrices = new ArrayList<>();

        shopConfig.currencyProvider = "item";
        shopConfig.currencyItemId = currencyItemId;

        plugin.saveShopConfig();
        plugin.refreshShopNpcInteractionHint(location);
        shopStatusText = "Marchand enregistré (" + shopLocation.x + ", " + shopLocation.y + ", " + shopLocation.z
                + ") avec " + saved.size() + " contrat(s). Monnaie: " + currencyItemId + ".";
        shopLocationEditorState = null;
        shopCurrencyPicksOpen = false;
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
                "Marchand (" + shopLocation.x + ", " + shopLocation.y + ", " + shopLocation.z + ")  [" + safeText(shopLocation.worldName) + "]"
        );
        cmd.set(
                "#ShopEditorHint.Text",
                "Contrats: boss + arène + prix classique + prix silencieux. Tapez l'item monnaie pour chercher."
            );
        cmd.set("#ShopEditorVendorName.Value", optionalText(state.vendorName));
        cmd.set("#ShopEditorCurrencyItem.Value", optionalText(state.currencyItemId));

        applyShopCurrencyPicks(cmd, events, true);
        applyShopContractLayoutForCurrencyPicks(cmd);
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#ShopEditorCurrencyItem",
                buildShopEditSnapshotEvent("shop_currency_filter"),
                false
        );

        int totalContracts = state.contracts.size();
        int maxOffset = Math.max(0, totalContracts - MAX_SHOP_CONTRACT_ROWS);
        state.listOffset = Math.max(0, Math.min(state.listOffset, maxOffset));

        cmd.set("#ShopEditorOverflowLabel.Visible", totalContracts > MAX_SHOP_CONTRACT_ROWS);
        if (totalContracts > MAX_SHOP_CONTRACT_ROWS) {
            cmd.set("#ShopEditorOverflowLabel.Text", "+" + (totalContracts - MAX_SHOP_CONTRACT_ROWS) + " contrat(s) non affichés.");
        } else {
            cmd.set("#ShopEditorOverflowLabel.Text", "");
        }

        List<DropdownEntryInfo> bossEntries = bossDropdownEntries();
        List<DropdownEntryInfo> arenaEntries = arenaDropdownEntries();

        for (int row = 1; row <= MAX_SHOP_CONTRACT_ROWS; row++) {
            String suffix = Integer.toString(row);
            int contractIndex = state.listOffset + row - 1;
            boolean visible = contractIndex < totalContracts;
            cmd.set("#ShopEditContractRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            ShopContractDraft draft = state.contracts.get(contractIndex);
            String bossId = optionalText(draft.bossId);
            String arenaId = optionalText(draft.arenaId);

            cmd.set("#ShopEditBoss" + suffix + ".Entries", withExtraDropdownValue(bossEntries, bossId, BossRegistry.get(bossId) == null));
            cmd.set("#ShopEditBoss" + suffix + ".Value", bossId);
            cmd.set("#ShopEditArena" + suffix + ".Entries", withExtraDropdownValue(arenaEntries, arenaId, ArenaRegistry.get(arenaId) == null));
            cmd.set("#ShopEditArena" + suffix + ".Value", arenaId);
            cmd.set("#ShopEditBossPrice" + suffix + ".Value", Integer.toString(Math.max(0, draft.cost)));
            cmd.set("#ShopEditSilentPrice" + suffix + ".Value", Integer.toString(Math.max(0, draft.silentCost)));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ShopEditRemove" + suffix,
                    buildShopEditSnapshotEvent("shop_edit_remove_" + row)
            );
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ShopEditorAddContract",
                buildShopEditSnapshotEvent("shop_edit_add")
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ShopEditorSaveButton",
                buildShopEditSnapshotEvent("shop_edit_save")
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

        if (action.startsWith("arena_here_")) {
            handleArenaMoveHere(action.substring("arena_here_".length()), ref, store);
            return;
        }

        if (action.startsWith("arena_tp_")) {
            handleArenaTeleport(action.substring("arena_tp_".length()), ref, store);
            return;
        }

        if (action.startsWith("arena_save_")) {
            handleArenaSave(action.substring("arena_save_".length()), data, null);
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
            cmd.set("#ArenaWorld" + suffix + ".Value", arena.worldName == null ? "" : arena.worldName);
            cmd.set("#ArenaX" + suffix + ".Value", formatCoord(arena.x));
            cmd.set("#ArenaY" + suffix + ".Value", formatCoord(arena.y));
            cmd.set("#ArenaZ" + suffix + ".Value", formatCoord(arena.z));
            cmd.set("#ArenaRadius" + suffix + ".Value", arena.lootRadius > 0.0d ? formatCoord(arena.lootRadius) : "30");

            events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaDelete" + suffix, EventData.of("Action", "arena_delete_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaHere" + suffix, EventData.of("Action", "arena_here_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaTp" + suffix, EventData.of("Action", "arena_tp_" + row));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ArenaSave" + suffix,
                    buildArenaRowSnapshotEvent("arena_save_" + row, row)
            );
        }
    }

    private EventData buildArenaRowSnapshotEvent(String action, int row) {
        String suffix = Integer.toString(row);
        return new EventData()
                .append("Action", action)
                .append("@ArenaName", "#ArenaName" + suffix + ".Value")
                .append("@ArenaWorld", "#ArenaWorld" + suffix + ".Value")
                .append("@ArenaX", "#ArenaX" + suffix + ".Value")
                .append("@ArenaY", "#ArenaY" + suffix + ".Value")
                .append("@ArenaZ", "#ArenaZ" + suffix + ".Value")
                .append("@ArenaRadius" + suffix, "#ArenaRadius" + suffix + ".Value");
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
        arena.lootRadius = 30.0d;
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

    private void handleArenaMoveHere(String rowToken, Ref<EntityStore> ref, Store<EntityStore> store) {
        int row = parseRow(rowToken);
        if (row < 1 || row > arenaRows.size()) {
            arenaStatusText = "Sélection de ligne arène invalide.";
            rebuild();
            return;
        }

        String arenaId = arenaRows.get(row - 1);
        Arena arena = ArenaRegistry.get(arenaId);
        if (arena == null) {
            arenaStatusText = "L'arène '" + arenaId + "' n'existe plus.";
            rebuild();
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        com.hypixel.hytale.server.core.universe.PlayerRef playerRef =
                store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        World world = playerRef != null
                ? com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid())
                : null;
        Object transformObj = store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
        org.joml.Vector3d rawPosition = transformObj instanceof com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc
                ? tc.getPosition()
                : null;
        if (player == null || world == null || rawPosition == null) {
            arenaStatusText = "Impossible de résoudre votre position.";
            rebuild();
            return;
        }

        arena.worldName = world.getName();
        arena.x = rawPosition.x;
        arena.y = rawPosition.y;
        arena.z = rawPosition.z;
        plugin.saveArenas();
        arenaStatusText = "Arène '" + arenaId + "' déplacée ici : "
                + formatCoord(arena.x) + ", " + formatCoord(arena.y) + ", " + formatCoord(arena.z)
                + " (" + arena.worldName + ").";
        rebuild();
    }

    private void handleArenaTeleport(String rowToken, Ref<EntityStore> ref, Store<EntityStore> store) {
        int row = parseRow(rowToken);
        if (row < 1 || row > arenaRows.size()) {
            arenaStatusText = "Sélection de ligne arène invalide.";
            rebuild();
            return;
        }

        String arenaId = arenaRows.get(row - 1);
        Arena arena = ArenaRegistry.get(arenaId);
        if (arena == null) {
            arenaStatusText = "L'arène '" + arenaId + "' n'existe plus.";
            rebuild();
            return;
        }

        String worldName = optionalText(arena.worldName);
        if (worldName.isEmpty()) {
            arenaStatusText = "L'arène '" + arenaId + "' n'a pas de monde configuré.";
            rebuild();
            return;
        }

        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            arenaStatusText = "Monde introuvable : '" + worldName + "'.";
            rebuild();
            return;
        }

        org.joml.Vector3d dest = new org.joml.Vector3d(arena.x, arena.y, arena.z);
        Teleport teleport = Teleport.createForPlayer(
                targetWorld,
                dest,
                com.hypixel.hytale.math.vector.Rotation3f.ZERO
        );
        if (store.getComponent(ref, Teleport.getComponentType()) != null) {
            store.putComponent(ref, Teleport.getComponentType(), teleport);
        } else {
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        }
        arenaStatusText = "Téléportation vers '" + arenaId + "' (" + worldName + " : "
                + formatCoord(arena.x) + ", " + formatCoord(arena.y) + ", " + formatCoord(arena.z) + ").";
        rebuild();
    }

    private void handleArenaSave(String rowToken, ConfigEventData data, Boolean proximityEnabledOverride) {
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

        String worldName = optionalText(data.arenaWorld);
        if (looksLikeUiBindingExpression(worldName)) {
            worldName = "";
        }
        if (worldName.isEmpty()) {
            worldName = optionalText(arena.worldName);
        }
        if (worldName.isEmpty()) {
            arenaStatusText = "Le monde de l'arène ne peut pas être vide.";
            rebuild();
            return;
        }
        if (Universe.get().getWorld(worldName) == null) {
            arenaStatusText = "Monde introuvable : '" + worldName + "'.";
            rebuild();
            return;
        }

        String radiusRaw = optionalText(data.getArenaRadius(row));
        double radius = arena.lootRadius > 0.0d ? arena.lootRadius : 30.0d;
        if (!radiusRaw.isEmpty()) {
            Double parsed = parseOptionalDouble(radiusRaw);
            radius = (parsed != null && parsed >= 0.0d) ? parsed : 30.0d;
        }
        double lootRadius = radius;
        double proximityRadius = radius;

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
        arena.worldName = worldName;
        arena.x = x;
        arena.y = y;
        arena.z = z;
        arena.lootRadius = lootRadius;
        arena.proximityEnabled = false;
        arena.proximityRadius = proximityRadius;
        ArenaRegistry.register(arena);

        plugin.saveArenas();
        arenaStatusText = "Arène '" + arena.arenaId + "' enregistrée (" + arena.worldName + ").";
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
            bossNpcPicksOpen = false;
            bossStatusText = "";
            rebuild();
            return;
        }

        if ("boss_waves_open".equals(action)) {
            if (bossEditorState != null) {
                bossWavesOverlayOpen = true;
                    bossWavePageIndex = 0;
                waveNpcPicksOpen = false;
                waveNpcPicksRow = 0;
                waveNpcSearchQuery = "";
                rebuild();
            }
            return;
        }

        if ("boss_wave_page_prev".equals(action)) {
            handleBossWavePageChange(-1, data);
            return;
        }

        if ("boss_wave_page_next".equals(action)) {
            handleBossWavePageChange(1, data);
            return;
        }

        if ("boss_wave_trigger_changed".equals(action)) {
            handleBossWaveTriggerChanged(data);
            return;
        }

        if ("boss_wave_add_page".equals(action)) {
            handleBossWaveAddPage(data);
            return;
        }

        if ("boss_wave_delete_page".equals(action)) {
            handleBossWaveDeletePage(data);
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

        if ("boss_spawn_spread_toggle".equals(action)) {
            handleBossSpawnSpreadToggle(data);
            return;
        }

        if ("boss_wave_random_toggle".equals(action) || "boss_waves_enabled_toggle".equals(action)) {
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


        if (action.startsWith("boss_loot_add_row_")) {
            handleBossLootAddRow(action.substring("boss_loot_add_row_".length()), data);
            return;
        }

        if (action.startsWith("boss_loot_delete_")) {
            handleBossLootDelete(action.substring("boss_loot_delete_".length()), data);
            return;
        }

        if ("boss_loot_scroll_up".equals(action)) {
            handleBossLootScroll(-1, data);
            return;
        }

        if ("boss_loot_scroll_down".equals(action)) {
            handleBossLootScroll(1, data);
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

        cmd.set("#BossEditorTitle.Text", "Édition de boss : " + safeText(boss.bossName));

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

        float speed = BossArenaConfigUiControls.clampFloat(boss.modifiers.movementSpeed, MULT_SCALE_MIN, MULT_SCALE_MAX);
        float attackRate = BossArenaConfigUiControls.clampFloat(boss.modifiers.attackRate, MULT_SCALE_MIN, MULT_SCALE_MAX);
        float kbGiven = BossArenaConfigUiControls.clampFloat(boss.modifiers.knockbackGiven, MULT_SCALE_MIN, MULT_SCALE_MAX);
        float kbTaken = BossArenaConfigUiControls.clampFloat(boss.modifiers.knockbackTaken, MULT_SCALE_MIN, MULT_SCALE_MAX);
        float regen = BossArenaConfigUiControls.clampFloat(
                BossRegen.normalizeHpPerSecond(boss.modifiers.regen), REGEN_MIN, REGEN_MAX);
        boss.modifiers.regen = regen;
        float ppSpeed = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.movementSpeed, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppAttackRate = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.attackRate, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppKbGiven = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.knockbackGiven, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppKbTaken = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.knockbackTaken, MULT_PERS_MIN, MULT_PERS_MAX);
        float ppRegen = BossArenaConfigUiControls.clampFloat(
                BossRegen.normalizeHpPerSecond(boss.perPlayerIncrease.regen), REGEN_MIN, REGEN_MAX);
        boss.perPlayerIncrease.regen = ppRegen;

        cmd.set("#BossEditHp.Value", hpMult);
        cmd.set("#BossEditDamage.Value", dmgMult);
        cmd.set("#BossEditSize.Value", sizeMult);
        cmd.set("#BossEditSpeed.Value", speed);
        cmd.set("#BossEditAttackRate.Value", attackRate);
        cmd.set("#BossEditKnockbackGiven.Value", kbGiven);
        cmd.set("#BossEditKnockbackTaken.Value", kbTaken);
        cmd.set("#BossEditRegen.Value", regen);
        cmd.set("#BossEditPpHp.Value", ppHp);
        cmd.set("#BossEditPpDamage.Value", ppDmg);
        cmd.set("#BossEditPpSize.Value", ppSize);
        cmd.set("#BossEditPpSpeed.Value", ppSpeed);
        cmd.set("#BossEditPpAttackRate.Value", ppAttackRate);
        cmd.set("#BossEditPpKnockbackGiven.Value", ppKbGiven);
        cmd.set("#BossEditPpKnockbackTaken.Value", ppKbTaken);
        cmd.set("#BossEditPpRegen.Value", ppRegen);
        cmd.set("#BossEditHpValue.Text", formatFloat(hpMult));
        cmd.set("#BossEditDamageValue.Text", formatFloat(dmgMult));
        cmd.set("#BossEditSizeValue.Text", formatFloat(sizeMult));
        cmd.set("#BossEditSpeedValue.Text", formatFloat(speed));
        cmd.set("#BossEditAttackRateValue.Text", formatFloat(attackRate));
        cmd.set("#BossEditKnockbackGivenValue.Text", formatFloat(kbGiven));
        cmd.set("#BossEditKnockbackTakenValue.Text", formatFloat(kbTaken));
        cmd.set("#BossEditRegenValue.Text", BossRegen.formatLabel(regen));
        cmd.set("#BossEditPpHpValue.Text", formatFloat(ppHp));
        cmd.set("#BossEditPpDamageValue.Text", formatFloat(ppDmg));
        cmd.set("#BossEditPpSizeValue.Text", formatFloat(ppSize));
        cmd.set("#BossEditPpSpeedValue.Text", formatFloat(ppSpeed));
        cmd.set("#BossEditPpAttackRateValue.Text", formatFloat(ppAttackRate));
        cmd.set("#BossEditPpKnockbackGivenValue.Text", formatFloat(ppKbGiven));
        cmd.set("#BossEditPpKnockbackTakenValue.Text", formatFloat(ppKbTaken));
        cmd.set("#BossEditPpRegenValue.Text", BossRegen.formatLabel(ppRegen));

        applyBossNpcPicks(cmd, events, true);
        applyBossEditorScrollForNpcPicks(cmd);

        if (boss.extraMobs != null) {
            boss.extraMobs.sanitize();
        }
        BossWavesSummary summary = buildBossWavesSummary(boss.extraMobs);
        cmd.set("#BossWavesLabel.Text", "Vagues");
        for (int i = 0; i < MAX_WAVE_SUMMARY_LINES; i++) {
            String line = summary.lineAt(i);
            cmd.set("#BossWavesAdd" + (i + 1) + ".Text", line);
            cmd.set("#BossWavesAdd" + (i + 1) + ".Visible", line != null && !line.isBlank());
        }
        cmd.set("#BossWavesMeta.Text", "");

        BossDefinition.ExtraMobs editorExtra = boss.extraMobs != null ? boss.extraMobs : new BossDefinition.ExtraMobs();
        editorExtra.sanitize();
        boolean afterDelay = BossDefinition.ExtraMobs.BOSS_SPAWN_AFTER_SECONDS.equals(editorExtra.bossSpawnTrigger);
        cmd.set("#BossSpawnTrigger.Value", toBossSpawnTriggerDisplayName(editorExtra.bossSpawnTrigger));
        cmd.set("#BossSpawnTriggerValue.Value", formatWaveNumber(editorExtra.bossSpawnTriggerValue));
        cmd.set("#BossSpawnTriggerValue.Visible", afterDelay);
        cmd.set("#BossSpawnTriggerValueLabel.Visible", afterDelay);
        BossArenaConfigUiControls.styleModeTextButton(cmd, "#BossSpawnAfterPreBoss", !afterDelay);
        BossArenaConfigUiControls.styleModeTextButton(cmd, "#BossSpawnAfterDelay", afterDelay);
        cmd.set("#BossSpawnSpreadRandom.Value", boss.useRandomBossSpawn ? "true" : "false");
        BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossSpawnSpreadToggle", boss.useRandomBossSpawn);
        cmd.set("#BossSpawnSpreadRadius.Value", formatWaveNumber(boss.getSpawnSpreadRadius()));
        cmd.set("#BossWaveRandomLocations.Value", editorExtra.useRandomSpawnLocations ? "true" : "false");
        BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossWaveRandomToggle", editorExtra.useRandomSpawnLocations);
        cmd.set("#BossWaveRandomRadius.Value", formatWaveNumber(editorExtra.getWaveRandomSpawnRadius()));

        String musicFile = boss.musicFileName != null ? boss.musicFileName.trim() : "";
        List<String> musicFiles = BossFightMusicService.listMusicFileNames();
        boolean musicKnown = musicFile.isEmpty() || musicFiles.stream().anyMatch(f -> f.equalsIgnoreCase(musicFile));
        List<DropdownEntryInfo> musicEntries = musicDropdownEntries(musicFiles);
        cmd.set("#BossEditMusic.Entries", withExtraDropdownValue(musicEntries, musicFile, !musicFile.isEmpty() && !musicKnown));
        cmd.set("#BossEditMusic.Value", musicFile);
        cmd.set("#BossEditMusicRadius.Value", formatWaveNumber(boss.getMusicRadius()));
        if (BossFightMusicService.listMusicFileNames().isEmpty()) {
            cmd.set("#BossEditMusicHint.Text", "Dépose des .ogg dans Varyon-BossArena/music/");
        } else {
            cmd.set("#BossEditMusicHint.Text", "Fichiers dans Varyon-BossArena/music/*.ogg");
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossEditorCloseButton", EventData.of("Action", "boss_editor_close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossEditWavesButton", EventData.of("Action", "boss_waves_open"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossSpawnAfterPreBoss",
                buildBossEditorSnapshotEvent("boss_spawn_after_pre")
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossSpawnAfterDelay",
                buildBossEditorSnapshotEvent("boss_spawn_after_delay")
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossSpawnSpreadToggle",
                buildBossEditorSnapshotEvent("boss_spawn_spread_toggle")
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossWaveRandomToggle",
                buildBossEditorSnapshotEvent("boss_wave_random_toggle")
        );
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
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditSpeed",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditAttackRate",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditKnockbackGiven",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditKnockbackTaken",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditRegen",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpSpeed",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpAttackRate",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpKnockbackGiven",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpKnockbackTaken",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BossEditPpRegen",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );

        List<LootItem> items = loot.items != null ? loot.items : List.of();
        int itemCount = items.size();
        int offset = clampLootOffset(itemCount);
        lootListOffset = offset;

        for (int row = 1; row <= MAX_LOOT_ROWS; row++) {
            String suffix = Integer.toString(row);
            int itemIndex = offset + row - 1;
            boolean visible = itemIndex <= itemCount;
            cmd.set("#BossLootRow" + suffix + ".Visible", visible);

            if (!visible) {
                continue;
            }

            LootItem item = itemIndex < itemCount ? items.get(itemIndex) : null;
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

            boolean populated = itemIndex < itemCount;
            cmd.set("#BossLootDelete" + suffix + ".Text", populated ? "-" : "+");
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#BossLootDelete" + suffix,
                    buildBossEditorSnapshotEvent(populated
                            ? "boss_loot_delete_" + row
                            : "boss_loot_add_row_" + row));
        }

        boolean canScrollUp = offset > 0;
        boolean canScrollDown = offset + MAX_LOOT_ROWS <= itemCount;
        cmd.set("#BossLootScrollUp.Visible", canScrollUp);
        cmd.set("#BossLootScrollDown.Visible", canScrollDown);
        if (canScrollUp) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#BossLootScrollUp",
                    buildBossEditorSnapshotEvent("boss_loot_scroll_up"));
        }
        if (canScrollDown) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#BossLootScrollDown",
                    buildBossEditorSnapshotEvent("boss_loot_scroll_down"));
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

            List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(extra);
            int pageCount = Math.max(1, waves.size());
            boolean onNewPage = bossWavePageIndex >= waves.size();
            if (onNewPage) {
                pageCount = waves.size() + 1;
            }
            if (bossWavePageIndex < 0) {
                bossWavePageIndex = 0;
            }
            if (bossWavePageIndex > pageCount - 1) {
                bossWavePageIndex = pageCount - 1;
            }
            BossDefinition.ExtraMobs.ScheduledWave current = bossWavePageIndex < waves.size()
                    ? waves.get(bossWavePageIndex)
                    : newEmptyScheduledWave();

            cmd.set("#BossWavePageLabel.Text", "Vague " + (bossWavePageIndex + 1) + " / " + pageCount);
            boolean waveEnabled = current == null || current.enabled;
            cmd.set("#BossWavesEnabled.Value", waveEnabled ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossWavesEnabledToggle", waveEnabled);
            String rawTrigger = optionalText(current.trigger).trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            String triggerId = toWaveTriggerDropdownValue(current.trigger);
            double triggerValue = BossDefinition.ExtraMobs.TRIGGER_ON_SPAWN.equals(rawTrigger)
                    ? 0.0d
                    : current.triggerValue;
            cmd.set("#BossWaveTriggerField.Value", triggerId);
            cmd.set("#BossWaveTriggerValueFieldLabel.Text", waveTriggerValueUnitLabel(triggerId));
            cmd.set("#BossWaveTriggerValueField.Value", formatWaveNumber(triggerValue));
            cmd.set("#BossWaveRepeatCountField.Value", Integer.toString(current.repeatCount));
            cmd.set("#BossWaveRepeatSecField.Value", "0");

            List<BossDefinition.ExtraMobs.WaveAdd> adds = current.adds != null ? current.adds : new ArrayList<>();
            int visibleWaveRows = Math.max(1, Math.min(MAX_WAVE_ADD_ROWS, adds.size() + 1));

            for (int row = 1; row <= MAX_WAVE_ADD_ROWS; row++) {
                String suffix = Integer.toString(row);
                boolean visible = row <= visibleWaveRows;
                cmd.set("#BossWavesRow" + suffix + ".Visible", visible);

                if (!visible) {
                    continue;
                }

                BossDefinition.ExtraMobs.WaveAdd add = row <= adds.size() ? adds.get(row - 1) : null;
                String npcId = safeText(add != null ? add.npcId : "");
                cmd.set("#BossWaveNpc" + suffix + ".Value", npcId);
                cmd.set("#BossWaveAmount" + suffix + ".Value", Integer.toString(Math.max(1, add != null ? add.mobsPerWave : 1)));
                float hp = add != null && add.hp > 0f ? add.hp : 1.0f;
                float damage = add != null && add.damage > 0f ? add.damage : 1.0f;
                float size = add != null && add.size > 0f ? add.size : 1.0f;
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

                boolean populated = row <= adds.size();
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
                    "#BossWavesEnabledToggle",
                    buildBossWavesSnapshotEvent("boss_waves_enabled_toggle")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossWavesSaveButton",
                    buildBossWavesSnapshotEvent("boss_waves_save")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossWavePrev",
                    buildBossWavesSnapshotEvent("boss_wave_page_prev")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossWaveNext",
                    buildBossWavesSnapshotEvent("boss_wave_page_next")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BossWaveDeletePageButton",
                    buildBossWavesSnapshotEvent("boss_wave_delete_page")
            );
            events.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    "#BossWaveTriggerField",
                    buildBossWavesSnapshotEvent("boss_wave_trigger_changed"),
                    false
            );
            applyWaveNpcPicks(cmd, events, true);
        }

    }

    private void buildBossTimedOverlay(UICommandBuilder cmd, UIEventBuilder events) {
        List<BossArenaConfig.TimedBossSpawn> rows = List.of();
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg != null) {
            rows = cfg.getTimedBossSpawns();
        }

        String announceText = BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT;
        String reminderText = BossArenaConfig.DEFAULT_TIMED_REMINDER_TEXT;
        String graceText = BossArenaConfig.DEFAULT_TIMED_GRACE_TITLE_TEXT;
        if (!rows.isEmpty() && rows.get(0) != null) {
            announceText = resolvedOrFallback(
                    rows.get(0).worldAnnouncementText,
                    BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT
            );
            // Empty reminder is intentional (disabled); keep blank in UI.
            if (rows.get(0).reminderAnnouncementText != null) {
                reminderText = rows.get(0).reminderAnnouncementText;
            }
            graceText = resolvedOrFallback(
                    rows.get(0).graceTitleText,
                    BossArenaConfig.DEFAULT_TIMED_GRACE_TITLE_TEXT
            );
        }

        cmd.set("#BossTimedStatusLabel.Text", bossStatusText == null ? "" : bossStatusText);
        cmd.set("#BossTimedOverflowLabel.Visible", rows.size() > MAX_TIMED_SPAWN_ROWS);
        if (rows.size() > MAX_TIMED_SPAWN_ROWS) {
            cmd.set("#BossTimedOverflowLabel.Text", "+" + (rows.size() - MAX_TIMED_SPAWN_ROWS) + " règles non affichées.");
        } else {
            cmd.set("#BossTimedOverflowLabel.Text", "");
        }

        cmd.set("#TimedEmptyLabel.Visible", rows.isEmpty());
        if (rows.isEmpty()) {
            cmd.set("#TimedEmptyLabel.Text", "Aucune règle. Appuyez sur + pour en ajouter une.");
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedAddButton", EventData.of("Action", "timed_add"));

        List<DropdownEntryInfo> arenaEntries = arenaDropdownEntries();

        for (int row = 1; row <= MAX_TIMED_SPAWN_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= rows.size();
            cmd.set("#BossTimedRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            BossArenaConfig.TimedBossSpawn entry = rows.get(row - 1);
            boolean interval = entry != null && entry.isIntervalMode();
            boolean manual = entry != null && entry.isManualMode();
            boolean afterDeath = entry == null || entry.isAfterDeathMode();
            boolean enabled = entry == null || entry.enabled;
            boolean announceGlobal = entry != null && entry.announceWorldWide;
            boolean announceWorld = entry != null && entry.announceCurrentWorld && !announceGlobal;
            boolean graceEnabled = entry != null && entry.gracePeriodEnabled;
            long graceSeconds = entry != null ? Math.max(0L, entry.gracePeriodSeconds) : 30L;
            cmd.set("#TimedEnabled" + suffix + ".Value", enabled ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedEnabledToggle" + suffix, enabled);
            String modeValue = manual
                    ? BossArenaConfig.SCHEDULE_MANUAL
                    : (interval ? BossArenaConfig.SCHEDULE_INTERVAL : BossArenaConfig.SCHEDULE_AFTER_DEATH);
            cmd.set("#TimedMode" + suffix + ".Value", modeValue);
            cmd.set("#TimedModeToggle" + suffix + ".Text",
                    manual ? "Manuel" : (interval ? "Planifié" : "Temps réapparition"));

            List<BossArenaConfig.BossPoolEntry> pool = entry != null ? entry.resolveBossPool() : List.of();
            String bossId = pool.isEmpty() ? "" : safeText(pool.get(0).bossId);
            String arenaId = safeText(entry != null ? entry.arenaId : "");
            cmd.set("#TimedBossId" + suffix + ".Value", bossId);
            cmd.set("#TimedBossPool" + suffix + ".Text", formatBossPoolButtonLabel(pool));
            cmd.set("#TimedArenaId" + suffix + ".Entries", withExtraDropdownValue(arenaEntries, arenaId, ArenaRegistry.get(arenaId) == null));
            cmd.set("#TimedArenaId" + suffix + ".Value", arenaId);

            cmd.set("#TimedMinPlayers" + suffix + ".Value", Integer.toString(entry != null ? Math.max(0, entry.minPlayers) : 0));
            cmd.set("#TimedGraceEnabled" + suffix + ".Value", graceEnabled ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedGraceToggle" + suffix, graceEnabled);
            cmd.set("#TimedGraceSeconds" + suffix + ".Value", Long.toString(graceSeconds));
            cmd.set("#TimedRequirePlayer" + suffix + ".Value", "false");
            cmd.set("#TimedRequirePlayerToggle" + suffix + ".Visible", false);
            cmd.set("#TimedPreventDup" + suffix + ".Value", "true");
            cmd.set("#TimedDespawnHours" + suffix + ".Value", "0");
            long despawnMinutesTotal = entry != null
                    ? BossArenaConfig.resolveMinutes(entry.despawnAfterHours, entry.despawnAfterMinutes)
                    : 5L;
            if (despawnMinutesTotal <= 0L && interval) {
                despawnMinutesTotal = 5L;
            }
            cmd.set("#TimedDespawnMinutes" + suffix + ".Value", Long.toString(Math.max(0L, despawnMinutesTotal)));
            cmd.set("#TimedDespawnMinutes" + suffix + ".Visible", interval);
            cmd.set("#TimedDespawnLabel" + suffix + ".Visible", interval);
            cmd.set("#TimedDespawnMinutesUnit" + suffix + ".Visible", interval);

            cmd.set("#TimedAnnounceGlobal" + suffix + ".Value", announceGlobal ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedAnnounceGlobalToggle" + suffix, announceGlobal);
            cmd.set("#TimedAnnounceWorld" + suffix + ".Value", announceWorld ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedAnnounceWorldToggle" + suffix, announceWorld);

            cmd.set("#TimedAfterDeathLabel" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEveryHours" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEveryHoursUnit" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEveryMinutes" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEveryMinutesUnit" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEverySeconds" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEverySecondsUnit" + suffix + ".Visible", afterDeath);
            cmd.set("#TimedEveryHours" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.spawnIntervalHours) : 1L));
            cmd.set("#TimedEveryMinutes" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.spawnIntervalMinutes) : 0L));
            cmd.set("#TimedEverySeconds" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.spawnIntervalSeconds) : 0L));

            cmd.set("#TimedIntervalLabel" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalHours" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalHoursUnit" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalDays" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalDaysUnit" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalSeconds" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalSecondsUnit" + suffix + ".Visible", interval);
            cmd.set("#TimedIntervalHours" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.intervalHours) : 1L));
            cmd.set("#TimedIntervalDays" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.intervalDays) : 0L));
            cmd.set("#TimedIntervalSeconds" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.intervalSeconds) : 0L));

            cmd.set("#TimedArrivalLabel" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalHours" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalHoursUnit" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalMinutes" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalMinutesUnit" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalSeconds" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalSecondsUnit" + suffix + ".Visible", interval);
            cmd.set("#TimedArrivalHours" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.arrivalWindowHours) : 0L));
            cmd.set("#TimedArrivalMinutes" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.arrivalWindowMinutes) : 15L));
            cmd.set("#TimedArrivalSeconds" + suffix + ".Value", Long.toString(entry != null ? Math.max(0L, entry.arrivalWindowSeconds) : 0L));

            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedEnabledToggle" + suffix,
                    buildBossTimedSnapshotEvent("timed_toggle_enabled_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedModeToggle" + suffix,
                    buildBossTimedSnapshotEvent("timed_toggle_mode_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedGraceToggle" + suffix,
                    buildBossTimedSnapshotEvent("timed_toggle_grace_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedAnnounceGlobalToggle" + suffix,
                    buildBossTimedSnapshotEvent("timed_toggle_announce_global_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedAnnounceWorldToggle" + suffix,
                    buildBossTimedSnapshotEvent("timed_toggle_announce_world_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedBossPool" + suffix,
                    buildBossTimedSnapshotEvent("timed_pool_open_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedPop" + suffix,
                    EventData.of("Action", "timed_pop_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedDelete" + suffix,
                    EventData.of("Action", "timed_delete_" + row));
        }

        cmd.set("#TimedAnnounceText.Value", announceText);
        cmd.set("#TimedReminderText.Value", reminderText);
        cmd.set("#TimedGraceText.Value", graceText);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossTimedSaveButton",
                buildBossTimedSnapshotEvent("boss_timed_save"));
        buildBossPoolOverlay(cmd, events);
    }

    @Nonnull
    private static List<DropdownEntryInfo> withExtraDropdownValue(
            @Nonnull List<DropdownEntryInfo> base,
            @Nonnull String value,
            boolean orphaned
    ) {
        if (value.isEmpty() || !orphaned) {
            return base;
        }
        List<DropdownEntryInfo> out = new ArrayList<>(base.size() + 1);
        out.addAll(base);
        out.add(new DropdownEntryInfo(LocalizableString.fromString(value + " (actuel)"), value));
        return out;
    }

    private static String formatFixedTimes(List<String> times) {
        if (times == null || times.isEmpty()) {
            return "";
        }
        return String.join(",", times);
    }

    private EventData buildBossTimedSnapshotEvent(String action) {
        EventData snapshot = new EventData().append("Action", action);
        for (int row = 1; row <= MAX_TIMED_SPAWN_ROWS; row++) {
            String suffix = Integer.toString(row);
            snapshot
                    .append("@BossWaveEvery" + suffix, "#TimedEnabled" + suffix + ".Value")
                    .append("@BossWaveAmount" + suffix, "#TimedMode" + suffix + ".Value")
                    .append("@BossWaveNpc" + suffix, "#TimedBossId" + suffix + ".Value")
                    .append("@BossWaveValue" + suffix, "#TimedArenaId" + suffix + ".Value")
                    .append("@TimedMinPlayers" + suffix, "#TimedMinPlayers" + suffix + ".Value")
                    .append("@BossWaveRepeatCount" + suffix, "#TimedEveryHours" + suffix + ".Value")
                    .append("@BossWaveRepeatSec" + suffix, "#TimedEveryMinutes" + suffix + ".Value")
                    .append("@TimedEverySeconds" + suffix, "#TimedEverySeconds" + suffix + ".Value")
                    .append("@TimedIntervalHours" + suffix, "#TimedIntervalHours" + suffix + ".Value")
                    .append("@TimedIntervalDays" + suffix, "#TimedIntervalDays" + suffix + ".Value")
                    .append("@TimedIntervalSeconds" + suffix, "#TimedIntervalSeconds" + suffix + ".Value")
                    .append("@TimedArrivalHours" + suffix, "#TimedArrivalHours" + suffix + ".Value")
                    .append("@TimedArrivalMinutes" + suffix, "#TimedArrivalMinutes" + suffix + ".Value")
                    .append("@TimedArrivalSeconds" + suffix, "#TimedArrivalSeconds" + suffix + ".Value")
                    .append("@TimedRequirePlayer" + suffix, "#TimedRequirePlayer" + suffix + ".Value")
                    .append("@BossWaveHp" + suffix, "#TimedPreventDup" + suffix + ".Value")
                    .append("@BossWaveDamage" + suffix, "#TimedDespawnHours" + suffix + ".Value")
                    .append("@BossWaveSize" + suffix, "#TimedDespawnMinutes" + suffix + ".Value")
                    .append("@TimedAnnounceGlobal" + suffix, "#TimedAnnounceGlobal" + suffix + ".Value")
                    .append("@TimedAnnounceWorld" + suffix, "#TimedAnnounceWorld" + suffix + ".Value")
                    .append("@TimedGraceEnabled" + suffix, "#TimedGraceEnabled" + suffix + ".Value")
                    .append("@TimedGraceSeconds" + suffix, "#TimedGraceSeconds" + suffix + ".Value");
        }
        snapshot.append("@TimedAnnounceText", "#TimedAnnounceText.Value");
        snapshot.append("@TimedReminderText", "#TimedReminderText.Value");
        snapshot.append("@TimedGraceText", "#TimedGraceText.Value");
        return snapshot;
    }

    private void handleTimedAdd() {
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            bossStatusText = "Handle de config indisponible.";
            rebuild();
            return;
        }
        List<BossArenaConfig.TimedBossSpawn> rows = cfg.getTimedBossSpawns();
        if (rows.size() >= MAX_TIMED_SPAWN_ROWS) {
            bossStatusText = "Maximum de " + MAX_TIMED_SPAWN_ROWS + " règles affiché. Enregistrez / nettoyez d'abord.";
            rebuild();
            return;
        }
        BossArenaConfig.TimedBossSpawn entry = new BossArenaConfig.TimedBossSpawn();
        entry.enabled = true;
        entry.scheduleMode = BossArenaConfig.SCHEDULE_AFTER_DEATH;
        entry.spawnIntervalHours = 1L;
        entry.spawnIntervalMinutes = 0L;
        entry.spawnIntervalSeconds = 0L;
        entry.intervalHours = 1L;
        entry.intervalDays = 0L;
        entry.intervalSeconds = 0L;
        entry.arrivalWindowHours = 0L;
        entry.arrivalWindowMinutes = 15L;
        entry.arrivalWindowSeconds = 0L;
        entry.minPlayers = 0;
        entry.requirePlayerInRadius = false;
        entry.gracePeriodEnabled = false;
        entry.gracePeriodSeconds = 30L;
        entry.preventDuplicateWhileAlive = true;
        entry.despawnAfterHours = 0L;
        entry.despawnAfterMinutes = 5L;
        entry.oneShot = false;
        entry.announceWorldWide = false;
        entry.announceCurrentWorld = false;
        if (!rows.isEmpty() && rows.get(0) != null) {
            entry.worldAnnouncementText = rows.get(0).worldAnnouncementText;
            entry.reminderAnnouncementText = rows.get(0).reminderAnnouncementText;
            entry.graceTitleText = rows.get(0).graceTitleText;
        }
        rows.add(entry);
        cfg.timedBossSpawns = rows;
        cfg.save();
        plugin.refreshTimedBossSpawns();
        bossStatusText = "Règle ajoutée. Renseignez le pool de boss / Arène puis Enregistrer.";
        rebuild();
    }

    private void handleTimedDelete(String rowToken) {
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            return;
        }
        List<BossArenaConfig.TimedBossSpawn> rows = cfg.getTimedBossSpawns();
        if (row < 1 || row > rows.size()) {
            return;
        }
        rows.remove(row - 1);
        cfg.timedBossSpawns = rows;
        cfg.save();
        plugin.refreshTimedBossSpawns();
        bossStatusText = "Règle supprimée.";
        rebuild();
    }

    private void handleTimedPop(String rowToken) {
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        var scheduler = plugin.getTimedSpawnScheduler();
        if (scheduler == null) {
            bossStatusText = "Scheduler indisponible.";
            rebuild();
            return;
        }
        bossStatusText = scheduler.forceSpawnByRow(row);
        rebuild();
    }

    private void handleTimedPoolOpen(String rowToken, ConfigEventData data) {
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        try {
            List<BossArenaConfig.TimedBossSpawn> parsed = parseTimedRowsFromEvent(data, false);
            persistTimedRows(parsed, data, false);
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
            return;
        }
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            return;
        }
        List<BossArenaConfig.TimedBossSpawn> rows = cfg.getTimedBossSpawns();
        if (row < 1 || row > rows.size()) {
            bossStatusText = "Règle introuvable pour le pool.";
            rebuild();
            return;
        }
        BossArenaConfig.TimedBossSpawn entry = rows.get(row - 1);
        LinkedHashMap<String, Integer> selected = new LinkedHashMap<>();
        for (BossArenaConfig.BossPoolEntry poolEntry : entry.resolveBossPool()) {
            String id = optionalText(poolEntry.bossId);
            if (!id.isEmpty()) {
                selected.put(id, Math.max(1, poolEntry.weight));
            }
        }
        bossPoolEditorState = new BossPoolEditorState(row, listRegisteredBossIds(), selected);
        if (bossPoolEditorState.bossIds.isEmpty()) {
            bossPoolEditorState.statusText = "Aucun boss enregistre. Creez-en dans l'onglet Bosses.";
        }
        rebuild();
    }

    private void handleTimedPoolAdd(ConfigEventData data) {
        if (bossPoolEditorState == null) {
            return;
        }
        String pick = optionalText(data.bossPoolPick);
        if (looksLikeUiBindingExpression(pick)) {
            pick = "";
        }
        if (pick.isEmpty()) {
            bossPoolEditorState.statusText = "Choisissez un boss dans la liste puis Ajouter.";
            rebuild();
            return;
        }
        String canonical = resolveRegisteredBossName(pick);
        if (canonical.isEmpty()) {
            bossPoolEditorState.statusText = "Boss inconnu: " + pick;
            rebuild();
            return;
        }
        if (bossPoolEditorState.findSelectedWeight(canonical) != null) {
            bossPoolEditorState.statusText = canonical + " est deja dans le pool (passez-le ON).";
            rebuild();
            return;
        }
        bossPoolEditorState.putSelected(canonical, 1);
        bossPoolEditorState.ensureBossListed(canonical);
        bossPoolEditorState.statusText = "Ajoute: " + canonical;
        rebuild();
    }

    private void handleTimedPoolApply() {
        if (bossPoolEditorState == null) {
            return;
        }
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            return;
        }
        List<BossArenaConfig.TimedBossSpawn> rows = cfg.getTimedBossSpawns();
        int row = bossPoolEditorState.ruleRow;
        if (row < 1 || row > rows.size()) {
            bossStatusText = "Règle introuvable.";
            bossPoolEditorState = null;
            rebuild();
            return;
        }
        List<BossArenaConfig.BossPoolEntry> pool = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : bossPoolEditorState.selectedWeights.entrySet()) {
            String id = optionalText(entry.getKey());
            if (id.isEmpty()) {
                continue;
            }
            BossArenaConfig.BossPoolEntry poolEntry = new BossArenaConfig.BossPoolEntry();
            poolEntry.bossId = id;
            poolEntry.weight = Math.max(1, entry.getValue() == null ? 1 : entry.getValue());
            pool.add(poolEntry);
        }
        if (pool.isEmpty()) {
            bossPoolEditorState.statusText = "Sélectionnez au moins un boss.";
            rebuild();
            return;
        }
        BossArenaConfig.TimedBossSpawn rule = rows.get(row - 1);
        rule.bossPool = pool;
        rule.bossId = optionalText(pool.get(0).bossId);
        cfg.timedBossSpawns = rows;
        cfg.save();
        plugin.refreshTimedBossSpawns();
        bossPoolEditorState = null;
        bossStatusText = "Pool enregistré (" + pool.size() + " boss).";
        rebuild();
    }

    private void handleTimedPoolPage(int delta) {
        if (bossPoolEditorState == null) {
            return;
        }
        int maxOffset = Math.max(0, bossPoolEditorState.bossIds.size() - MAX_BOSS_POOL_ROWS);
        bossPoolEditorState.pageOffset = Math.max(0, Math.min(maxOffset, bossPoolEditorState.pageOffset + (delta * MAX_BOSS_POOL_ROWS)));
        rebuild();
    }

    private void handleTimedPoolToggle(String rowToken) {
        if (bossPoolEditorState == null) {
            return;
        }
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        int index = bossPoolEditorState.pageOffset + row - 1;
        if (index < 0 || index >= bossPoolEditorState.bossIds.size()) {
            return;
        }
        String bossId = bossPoolEditorState.bossIds.get(index);
        if (bossPoolEditorState.findSelectedWeight(bossId) != null) {
            bossPoolEditorState.removeSelected(bossId);
        } else {
            bossPoolEditorState.putSelected(bossId, 1);
        }
        rebuild();
    }

    private void handleTimedPoolWeight(String rowToken, int delta) {
        if (bossPoolEditorState == null) {
            return;
        }
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        int index = bossPoolEditorState.pageOffset + row - 1;
        if (index < 0 || index >= bossPoolEditorState.bossIds.size()) {
            return;
        }
        String bossId = bossPoolEditorState.bossIds.get(index);
        Integer current = bossPoolEditorState.findSelectedWeight(bossId);
        if (current == null) {
            return;
        }
        bossPoolEditorState.putSelected(bossId, Math.max(1, current + delta));
        rebuild();
    }

    private void buildBossPoolOverlay(UICommandBuilder cmd, UIEventBuilder events) {
        boolean open = bossPoolEditorState != null;
        cmd.set("#BossPoolOverlay.Visible", open);
        if (!open) {
            return;
        }
        bossPoolEditorState.mergeRegisteredBosses(listRegisteredBossIds());
        cmd.set("#BossPoolTitle.Text", "Pool de boss - regle " + bossPoolEditorState.ruleRow);
        cmd.set("#BossPoolStatusLabel.Text",
                bossPoolEditorState.statusText == null ? "" : bossPoolEditorState.statusText);
        int total = bossPoolEditorState.bossIds.size();
        int page = total == 0 ? 1 : (bossPoolEditorState.pageOffset / MAX_BOSS_POOL_ROWS) + 1;
        int pages = Math.max(1, (total + MAX_BOSS_POOL_ROWS - 1) / MAX_BOSS_POOL_ROWS);
        cmd.set("#BossPoolPageLabel.Text", "Page " + page + "/" + pages
                + " | selection: " + bossPoolEditorState.selectedWeights.size()
                + " / " + total);

        List<DropdownEntryInfo> pickEntries = new ArrayList<>();
        pickEntries.add(new DropdownEntryInfo(LocalizableString.fromString("(choisir un boss)"), ""));
        for (String id : bossPoolEditorState.bossIds) {
            if (bossPoolEditorState.findSelectedWeight(id) == null) {
                pickEntries.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
            }
        }
        cmd.set("#BossPoolPick.Entries", pickEntries);
        cmd.set("#BossPoolPick.Value", "");

        for (int row = 1; row <= MAX_BOSS_POOL_ROWS; row++) {
            String suffix = Integer.toString(row);
            int index = bossPoolEditorState.pageOffset + row - 1;
            boolean visible = index < bossPoolEditorState.bossIds.size();
            cmd.set("#BossPoolRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }
            String bossId = bossPoolEditorState.bossIds.get(index);
            Integer selectedWeight = bossPoolEditorState.findSelectedWeight(bossId);
            boolean selected = selectedWeight != null;
            int weight = selected ? Math.max(1, selectedWeight) : 1;
            cmd.set("#BossPoolName" + suffix + ".Text", bossId);
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossPoolToggle" + suffix, selected);
            cmd.set("#BossPoolWeight" + suffix + ".Text", Integer.toString(weight));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolToggle" + suffix,
                    EventData.of("Action", "timed_pool_toggle_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolWeightDec" + suffix,
                    EventData.of("Action", "timed_pool_wdec_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolWeightInc" + suffix,
                    EventData.of("Action", "timed_pool_winc_" + row));
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolCloseButton",
                EventData.of("Action", "timed_pool_close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolApplyButton",
                EventData.of("Action", "timed_pool_apply"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossPoolAddButton",
                new EventData().append("Action", "timed_pool_add").append("@BossPoolPick", "#BossPoolPick.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolPrevButton",
                EventData.of("Action", "timed_pool_prev"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BossPoolNextButton",
                EventData.of("Action", "timed_pool_next"));
    }

    @Nonnull
    private static List<String> listRegisteredBossIds() {
        List<BossDefinition> bosses = new ArrayList<>(BossRegistry.getAll().values());
        bosses.sort(Comparator.comparing(
                b -> b == null || b.bossName == null ? "" : b.bossName,
                String.CASE_INSENSITIVE_ORDER
        ));
        List<String> ids = new ArrayList<>();
        for (BossDefinition boss : bosses) {
            if (boss == null) {
                continue;
            }
            String id = safeText(boss.bossName);
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    @Nonnull
    private static String resolveRegisteredBossName(String raw) {
        String needle = optionalText(raw);
        if (needle.isEmpty()) {
            return "";
        }
        BossDefinition exact = BossRegistry.get(needle);
        if (exact != null && exact.bossName != null && !exact.bossName.isBlank()) {
            return exact.bossName.trim();
        }
        for (String id : listRegisteredBossIds()) {
            if (needle.equalsIgnoreCase(id)) {
                return id;
            }
        }
        return "";
    }

    @Nonnull
    private static List<BossArenaConfig.BossPoolEntry> copyBossPool(BossArenaConfig.TimedBossSpawn source) {
        List<BossArenaConfig.BossPoolEntry> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        for (BossArenaConfig.BossPoolEntry entry : source.resolveBossPool()) {
            BossArenaConfig.BossPoolEntry copy = new BossArenaConfig.BossPoolEntry();
            copy.bossId = entry.bossId;
            copy.weight = Math.max(1, entry.weight);
            out.add(copy);
        }
        return out;
    }

    @Nonnull
    private static String formatBossPoolButtonLabel(List<BossArenaConfig.BossPoolEntry> pool) {
        if (pool == null || pool.isEmpty()) {
            return "Pool (0)";
        }
        if (pool.size() == 1) {
            String id = safeText(pool.get(0).bossId);
            return id.isEmpty() ? "Pool (1)" : id;
        }
        return "Pool (" + pool.size() + ")";
    }

    private void handleTimedFieldToggle(String rowToken, ConfigEventData data, String field) {
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        try {
            List<BossArenaConfig.TimedBossSpawn> parsed = parseTimedRowsFromEvent(data, false);
            if (row < 1 || row > parsed.size()) {
                // Fall back to config index if form incomplete
                BossArenaConfig cfg = plugin.getConfig();
                if (cfg == null) {
                    return;
                }
                parsed = cfg.getTimedBossSpawns();
                if (row < 1 || row > parsed.size()) {
                    return;
                }
            }
            BossArenaConfig.TimedBossSpawn entry = parsed.get(row - 1);
            switch (field) {
                case "enabled" -> entry.enabled = !entry.enabled;
                case "mode" -> {
                    if (entry.isAfterDeathMode()) {
                        entry.scheduleMode = BossArenaConfig.SCHEDULE_INTERVAL;
                    } else if (entry.isIntervalMode()) {
                        entry.scheduleMode = BossArenaConfig.SCHEDULE_MANUAL;
                    } else {
                        entry.scheduleMode = BossArenaConfig.SCHEDULE_AFTER_DEATH;
                    }
                }
                case "grace" -> entry.gracePeriodEnabled = !entry.gracePeriodEnabled;
                case "announce_global" -> {
                    entry.announceWorldWide = !entry.announceWorldWide;
                    if (entry.announceWorldWide) {
                        entry.announceCurrentWorld = false;
                    }
                }
                case "announce_world" -> {
                    entry.announceCurrentWorld = !entry.announceCurrentWorld;
                    if (entry.announceCurrentWorld) {
                        entry.announceWorldWide = false;
                    }
                }
                default -> {
                }
            }
            persistTimedRows(parsed, data, false);
            bossStatusText = "Règle mise à jour.";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossTimedSave(ConfigEventData data) {
        try {
            List<BossArenaConfig.TimedBossSpawn> out = parseTimedRowsFromEvent(data, true);
            persistTimedRows(out, data, true);
            bossStatusText = "Règles de planification enregistrées (" + out.size() + ").";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void persistTimedRows(List<BossArenaConfig.TimedBossSpawn> rows, ConfigEventData data, boolean requireComplete) {
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            throw new IllegalArgumentException("Handle de config indisponible.");
        }
        String announceTextRaw = optionalText(data.timedAnnounceText);
        if (looksLikeUiBindingExpression(announceTextRaw)) {
            announceTextRaw = "";
        }
        String announceText = announceTextRaw.isEmpty()
                ? BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT
                : announceTextRaw;
        String reminderTextRaw = optionalText(data.timedReminderText);
        if (looksLikeUiBindingExpression(reminderTextRaw)) {
            reminderTextRaw = "";
        }
        String graceTextRaw = optionalText(data.timedGraceText);
        if (looksLikeUiBindingExpression(graceTextRaw)) {
            graceTextRaw = "";
        }
        String graceText = graceTextRaw.isEmpty()
                ? BossArenaConfig.DEFAULT_TIMED_GRACE_TITLE_TEXT
                : graceTextRaw;
        for (BossArenaConfig.TimedBossSpawn entry : rows) {
            if (entry == null) {
                continue;
            }
            entry.worldAnnouncementText = announceText;
            entry.reminderAnnouncementText = reminderTextRaw;
            entry.graceTitleText = graceText;
        }
        cfg.timedBossSpawns = rows;
        cfg.save();
        plugin.refreshTimedBossSpawns();
    }

    private List<BossArenaConfig.TimedBossSpawn> parseTimedRowsFromEvent(ConfigEventData data, boolean requireIds) {
        BossArenaConfig cfg = plugin.getConfig();
        List<BossArenaConfig.TimedBossSpawn> existing = cfg != null ? cfg.getTimedBossSpawns() : List.of();
        BossArenaConfig.TimedBossSpawn existingFirst = existing.isEmpty() ? null : existing.get(0);

        String announceTextRaw = optionalText(data.timedAnnounceText);
        if (looksLikeUiBindingExpression(announceTextRaw)) {
            announceTextRaw = "";
        }
        String resolvedAnnounceText = announceTextRaw.isEmpty()
                ? (existingFirst != null
                ? resolvedOrFallback(existingFirst.worldAnnouncementText, BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT)
                : BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT)
                : announceTextRaw;

        String reminderTextRaw = optionalText(data.timedReminderText);
        if (looksLikeUiBindingExpression(reminderTextRaw)) {
            reminderTextRaw = existingFirst != null
                    ? optionalText(existingFirst.reminderAnnouncementText)
                    : BossArenaConfig.DEFAULT_TIMED_REMINDER_TEXT;
        }
        final String resolvedReminderText = reminderTextRaw;

        String graceTextRaw = optionalText(data.timedGraceText);
        if (looksLikeUiBindingExpression(graceTextRaw)) {
            graceTextRaw = existingFirst != null
                    ? optionalText(existingFirst.graceTitleText)
                    : BossArenaConfig.DEFAULT_TIMED_GRACE_TITLE_TEXT;
        }
        final String resolvedGraceText = graceTextRaw.isEmpty()
                ? BossArenaConfig.DEFAULT_TIMED_GRACE_TITLE_TEXT
                : graceTextRaw;

        int visibleCount = Math.min(MAX_TIMED_SPAWN_ROWS, existing.size());
        List<BossArenaConfig.TimedBossSpawn> out = new ArrayList<>();
        for (int row = 1; row <= visibleCount; row++) {
            BossArenaConfig.TimedBossSpawn existingRow = row <= existing.size() ? existing.get(row - 1) : null;
            String enabledText = optionalText(data.getBossWaveEvery(row));
            String modeText = optionalText(data.getBossWaveAmount(row));
            String bossId = optionalText(data.getBossWaveNpc(row));
            String arenaId = optionalText(data.getBossWaveValue(row));
            String minPlayersText = optionalText(data.getTimedMinPlayers(row));
            String everyHoursText = optionalText(data.getBossWaveRepeatCount(row));
            String everyMinutesText = optionalText(data.getBossWaveRepeatSec(row));
            String everySecondsText = optionalText(data.getTimedEverySeconds(row));
            String intervalHoursText = optionalText(data.getTimedIntervalHours(row));
            String intervalDaysText = optionalText(data.getTimedIntervalDays(row));
            String intervalSecondsText = optionalText(data.getTimedIntervalSeconds(row));
            String arrivalHoursText = optionalText(data.getTimedArrivalHours(row));
            String arrivalMinutesText = optionalText(data.getTimedArrivalMinutes(row));
            String arrivalSecondsText = optionalText(data.getTimedArrivalSeconds(row));
            String requireText = optionalText(data.getTimedRequirePlayer(row));
            String despawnHoursText = optionalText(data.getBossWaveDamage(row));
            String despawnMinutesText = optionalText(data.getBossWaveSize(row));
            String announceGlobalText = optionalText(data.getTimedAnnounceGlobal(row));
            String announceWorldText = optionalText(data.getTimedAnnounceWorld(row));
            String graceEnabledText = optionalText(data.getTimedGraceEnabled(row));
            String graceSecondsText = optionalText(data.getTimedGraceSeconds(row));

            if (looksLikeUiBindingExpression(enabledText)
                    || looksLikeUiBindingExpression(modeText)
                    || looksLikeUiBindingExpression(bossId)
                    || looksLikeUiBindingExpression(arenaId)
                    || looksLikeUiBindingExpression(minPlayersText)
                    || looksLikeUiBindingExpression(everyHoursText)
                    || looksLikeUiBindingExpression(everyMinutesText)
                    || looksLikeUiBindingExpression(everySecondsText)
                    || looksLikeUiBindingExpression(intervalHoursText)
                    || looksLikeUiBindingExpression(intervalDaysText)
                    || looksLikeUiBindingExpression(intervalSecondsText)
                    || looksLikeUiBindingExpression(arrivalHoursText)
                    || looksLikeUiBindingExpression(arrivalMinutesText)
                    || looksLikeUiBindingExpression(arrivalSecondsText)
                    || looksLikeUiBindingExpression(requireText)
                    || looksLikeUiBindingExpression(despawnHoursText)
                    || looksLikeUiBindingExpression(despawnMinutesText)
                    || looksLikeUiBindingExpression(announceGlobalText)
                    || looksLikeUiBindingExpression(announceWorldText)
                    || looksLikeUiBindingExpression(graceEnabledText)
                    || looksLikeUiBindingExpression(graceSecondsText)) {
                // Keep existing row if UI bindings failed
                if (existingRow != null) {
                    out.add(existingRow);
                }
                continue;
            }

            List<BossArenaConfig.BossPoolEntry> pool = copyBossPool(existingRow);
            if (pool.isEmpty() && !bossId.isEmpty()) {
                BossArenaConfig.BossPoolEntry single = new BossArenaConfig.BossPoolEntry();
                single.bossId = bossId;
                single.weight = 1;
                pool.add(single);
            }
            if (requireIds) {
                if (pool.isEmpty() && arenaId.isEmpty()) {
                    continue;
                }
                if (pool.isEmpty() || arenaId.isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + row + " : Pool de boss et Arène sont requis.");
                }
            }

            Boolean enabled = enabledText.isEmpty() ? Boolean.TRUE : parseToggleInput(enabledText);
            if (enabled == null) {
                throw new IllegalArgumentException("Ligne " + row + " : Actif doit être Oui/Non.");
            }
            String scheduleMode;
            if (modeText.isEmpty() && existingRow != null) {
                scheduleMode = existingRow.isManualMode()
                        ? BossArenaConfig.SCHEDULE_MANUAL
                        : (existingRow.isIntervalMode()
                        ? BossArenaConfig.SCHEDULE_INTERVAL
                        : BossArenaConfig.SCHEDULE_AFTER_DEATH);
            } else if (BossArenaConfig.SCHEDULE_MANUAL.equalsIgnoreCase(modeText)
                    || "manuel".equalsIgnoreCase(modeText)) {
                scheduleMode = BossArenaConfig.SCHEDULE_MANUAL;
            } else if (BossArenaConfig.SCHEDULE_INTERVAL.equalsIgnoreCase(modeText)
                    || BossArenaConfig.SCHEDULE_FIXED_TIMES.equalsIgnoreCase(modeText)
                    || "intervalle".equalsIgnoreCase(modeText)
                    || "planifié".equalsIgnoreCase(modeText)
                    || "planifie".equalsIgnoreCase(modeText)
                    || "heures".equalsIgnoreCase(modeText)
                    || "fixed".equalsIgnoreCase(modeText)) {
                scheduleMode = BossArenaConfig.SCHEDULE_INTERVAL;
            } else {
                scheduleMode = BossArenaConfig.SCHEDULE_AFTER_DEATH;
            }
            boolean intervalMode = BossArenaConfig.SCHEDULE_INTERVAL.equals(scheduleMode);
            boolean manualMode = BossArenaConfig.SCHEDULE_MANUAL.equals(scheduleMode);
            Boolean announceGlobal = announceGlobalText.isEmpty()
                    ? (existingRow != null && existingRow.announceWorldWide)
                    : parseToggleInput(announceGlobalText);
            Boolean announceWorld = announceWorldText.isEmpty()
                    ? (existingRow != null && existingRow.announceCurrentWorld)
                    : parseToggleInput(announceWorldText);
            Boolean graceEnabled = graceEnabledText.isEmpty()
                    ? (existingRow != null && existingRow.gracePeriodEnabled)
                    : parseToggleInput(graceEnabledText);
            if (announceGlobal == null || announceWorld == null || graceEnabled == null) {
                throw new IllegalArgumentException("Ligne " + row + " : bascule invalide.");
            }
            if (announceGlobal) {
                announceWorld = false;
            }

            long everyHours = parseRequiredLong(
                    everyHoursText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.spawnIntervalHours) : 1L)
                            : everyHoursText,
                    "Ligne " + row + " : délai (h) doit être un entier >= 0.",
                    0L,
                    Long.MAX_VALUE
            );
            long everyMinutes = parseRequiredLong(
                    everyMinutesText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.spawnIntervalMinutes) : 0L)
                            : everyMinutesText,
                    "Ligne " + row + " : délai (min) doit être un entier >= 0.",
                    0L,
                    Long.MAX_VALUE
            );
            long everySeconds = parseRequiredLong(
                    everySecondsText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.spawnIntervalSeconds) : 0L)
                            : everySecondsText,
                    "Ligne " + row + " : délai (s) doit être un entier >= 0.",
                    0L,
                    Long.MAX_VALUE
            );
            long intervalHours = parseRequiredLong(
                    intervalHoursText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.intervalHours) : 1L)
                            : intervalHoursText,
                    "Ligne " + row + " : intervalle (h) doit être un entier >= 0.",
                    0L,
                    Long.MAX_VALUE
            );
            long intervalDays = parseRequiredLong(
                    intervalDaysText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.intervalDays) : 0L)
                            : intervalDaysText,
                    "Ligne " + row + " : intervalle (j) doit être un entier >= 0.",
                    0L,
                    Long.MAX_VALUE
            );
            long intervalSecondsVal = parseRequiredLong(
                    intervalSecondsText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.intervalSeconds) : 0L)
                            : intervalSecondsText,
                    "Ligne " + row + " : intervalle (s) doit être un entier >= 0.",
                    0L,
                    Long.MAX_VALUE
            );
            if (intervalMode && !manualMode
                    && BossArenaConfig.resolveIntervalSeconds(intervalHours, intervalDays, intervalSecondsVal) <= 0L) {
                throw new IllegalArgumentException("Ligne " + row + " : l'intervalle (h/j/s) doit être > 0.");
            }
            long arrivalHours = parseRequiredLong(
                    arrivalHoursText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.arrivalWindowHours) : 0L)
                            : arrivalHoursText,
                    "Ligne " + row + " : Arrivée (h) invalide.",
                    0L,
                    Long.MAX_VALUE
            );
            long arrivalMinutes = parseRequiredLong(
                    arrivalMinutesText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.arrivalWindowMinutes) : 15L)
                            : arrivalMinutesText,
                    "Ligne " + row + " : Arrivée (min) invalide.",
                    0L,
                    Long.MAX_VALUE
            );
            long arrivalSeconds = parseRequiredLong(
                    arrivalSecondsText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.arrivalWindowSeconds) : 0L)
                            : arrivalSecondsText,
                    "Ligne " + row + " : Arrivée (s) invalide.",
                    0L,
                    Long.MAX_VALUE
            );
            int minPlayers = (int) parseRequiredLong(
                    minPlayersText.isEmpty() ? "0" : minPlayersText,
                    "Ligne " + row + " : Joueurs doit être un entier >= 0 (0 = spawn sans contrainte).",
                    0L,
                    64L
            );
            long graceSeconds = parseRequiredLong(
                    graceSecondsText.isEmpty()
                            ? Long.toString(existingRow != null ? Math.max(0L, existingRow.gracePeriodSeconds) : 30L)
                            : graceSecondsText,
                    "Ligne " + row + " : Grâce (sec) invalide.",
                    0L,
                    3600L
            );
            if (!intervalMode && !manualMode
                    && BossArenaConfig.resolveSeconds(everyHours, everyMinutes, everySeconds) <= 0L) {
                throw new IllegalArgumentException("Ligne " + row + " : le délai de respawn doit être > 0.");
            }

            long despawnMinutes;
            if (intervalMode) {
                despawnMinutes = parseRequiredLong(
                        despawnMinutesText.isEmpty() ? "5" : despawnMinutesText,
                        "Ligne " + row + " : Temps avant disparition (minutes) invalide.",
                        0L,
                        Long.MAX_VALUE
                );
            } else if (existingRow != null) {
                despawnMinutes = BossArenaConfig.resolveMinutes(
                        existingRow.despawnAfterHours,
                        existingRow.despawnAfterMinutes
                );
            } else {
                despawnMinutes = 0L;
            }

            BossArenaConfig.TimedBossSpawn entry = new BossArenaConfig.TimedBossSpawn();
            entry.enabled = enabled;
            entry.bossPool = pool;
            entry.bossId = pool.isEmpty() ? "" : optionalText(pool.get(0).bossId);
            entry.arenaId = arenaId;
            entry.scheduleMode = scheduleMode;
            entry.spawnIntervalHours = everyHours;
            entry.spawnIntervalMinutes = everyMinutes;
            entry.spawnIntervalSeconds = everySeconds;
            entry.intervalHours = intervalHours;
            entry.intervalDays = intervalDays;
            entry.intervalSeconds = intervalSecondsVal;
            entry.intervalEvery = 0L;
            entry.arrivalWindowHours = arrivalHours;
            entry.arrivalWindowMinutes = arrivalMinutes;
            entry.arrivalWindowSeconds = arrivalSeconds;
            entry.fixedTimes = new ArrayList<>();
            entry.oneShot = false;
            entry.requirePlayerInRadius = false;
            entry.minPlayers = minPlayers;
            entry.preventDuplicateWhileAlive = true;
            entry.despawnAfterHours = 0L;
            entry.despawnAfterMinutes = despawnMinutes;
            entry.announceWorldWide = announceGlobal;
            entry.announceCurrentWorld = announceWorld;
            entry.worldAnnouncementText = resolvedAnnounceText;
            entry.reminderAnnouncementText = resolvedReminderText;
            entry.gracePeriodEnabled = graceEnabled;
            entry.gracePeriodSeconds = graceSeconds;
            entry.graceTitleText = resolvedGraceText;
            out.add(entry);
        }

        // Keep overflow rows beyond UI
        if (existing.size() > MAX_TIMED_SPAWN_ROWS) {
            out.addAll(existing.subList(MAX_TIMED_SPAWN_ROWS, existing.size()));
        }
        return out;
    }

    private static List<String> parseFixedTimesInput(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("[,;|/\\s]+")) {
            String normalized = BossArenaConfig.normalizeFixedTime(part);
            if (!normalized.isEmpty() && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out;
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
                .append("@BossEditKnockbackGiven", "#BossEditKnockbackGiven.Value")
                .append("@BossEditKnockbackTaken", "#BossEditKnockbackTaken.Value")
                .append("@BossEditRegen", "#BossEditRegen.Value")
                .append("@BossEditPpHp", "#BossEditPpHp.Value")
                .append("@BossEditPpDamage", "#BossEditPpDamage.Value")
                .append("@BossEditPpSpeed", "#BossEditPpSpeed.Value")
                .append("@BossEditPpSize", "#BossEditPpSize.Value")
                .append("@BossEditPpAttackRate", "#BossEditPpAttackRate.Value")
                .append("@BossEditPpKnockbackGiven", "#BossEditPpKnockbackGiven.Value")
                .append("@BossEditPpKnockbackTaken", "#BossEditPpKnockbackTaken.Value")
                .append("@BossEditPpRegen", "#BossEditPpRegen.Value")
                .append("@BossSpawnTrigger", "#BossSpawnTrigger.Value")
                .append("@BossSpawnTriggerValue", "#BossSpawnTriggerValue.Value")
                .append("@BossSpawnSpreadRandom", "#BossSpawnSpreadRandom.Value")
                .append("@BossSpawnSpreadRadius", "#BossSpawnSpreadRadius.Value")
                .append("@BossWaveRandomLocations", "#BossWaveRandomLocations.Value")
                .append("@BossWaveRandomRadius", "#BossWaveRandomRadius.Value")
                .append("@BossEditMusic", "#BossEditMusic.Value")
                .append("@BossEditMusicRadius", "#BossEditMusicRadius.Value");

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
        // Header fields apply to the current ScheduledWave (page). They are transported through the
        // row-1 codec slots (@BossWaveEvery1/@BossWaveValue1/@BossWaveRepeatCount1/@BossWaveRepeatSec1).
        EventData snapshot = new EventData()
                .append("Action", action)
                .append("@BossWavesEnabled", "#BossWavesEnabled.Value")
                .append("@BossWaveEvery1", "#BossWaveTriggerField.Value")
                .append("@BossWaveValue1", "#BossWaveTriggerValueField.Value")
                .append("@BossWaveRepeatCount1", "#BossWaveRepeatCountField.Value")
                .append("@BossWaveRepeatSec1", "#BossWaveRepeatSecField.Value");

        for (int row = 1; row <= MAX_WAVE_ADD_ROWS; row++) {
            String suffix = Integer.toString(row);
            snapshot
                    .append("@BossWaveNpc" + suffix, "#BossWaveNpc" + suffix + ".Value")
                    .append("@BossWaveAmount" + suffix, "#BossWaveAmount" + suffix + ".Value")
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
        bossNpcPicksOpen = false;
        lootListOffset = 0;
        bossWavePageIndex = 0;
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
        bossNpcPicksOpen = false;
        lootListOffset = 0;
        bossWavePageIndex = 0;

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
            List<LootItem> merged = mergeLootWindow(data);
            bossEditorState.loot.items = merged;
            lootListOffset = Math.max(0, merged.size() - (MAX_LOOT_ROWS - 1));
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
            int itemCount = bossEditorState.loot != null && bossEditorState.loot.items != null
                    ? bossEditorState.loot.items.size()
                    : 0;
            int offset = clampLootOffset(itemCount);
            int itemIndex = offset + row - 1;
            List<LootItem> merged = mergeLootWindow(data);
            if (itemIndex >= 0 && itemIndex < merged.size()) {
                merged.remove(itemIndex);
            }
            bossEditorState.loot.items = merged;
            lootListOffset = clampLootOffset(merged.size());
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossLootScroll(int delta, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        try {
            applyBossEditorDraft(data);
            List<LootItem> merged = mergeLootWindow(data);
            bossEditorState.loot.items = merged;
            int itemCount = merged.size();
            int maxOffset = Math.max(0, itemCount - (MAX_LOOT_ROWS - 1));
            int target = clampLootOffset(itemCount) + delta;
            if (target < 0) {
                target = 0;
            }
            if (target > maxOffset) {
                target = maxOffset;
            }
            lootListOffset = target;
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private int clampLootOffset(int itemCount) {
        int maxOffset = Math.max(0, itemCount - (MAX_LOOT_ROWS - 1));
        int offset = lootListOffset;
        if (offset < 0) {
            offset = 0;
        }
        if (offset > maxOffset) {
            offset = maxOffset;
        }
        return offset;
    }

    /**
     * Merges the currently visible loot window (physical rows 1..MAX_LOOT_ROWS) back into the boss's
     * full, unbounded loot list, preserving items outside the visible window. Rows with unresolved UI
     * binding expressions fall back to the existing item at that index.
     */
    private List<LootItem> mergeLootWindow(ConfigEventData data) {
        List<LootItem> existing = bossEditorState != null && bossEditorState.loot != null && bossEditorState.loot.items != null
                ? bossEditorState.loot.items
                : new ArrayList<>();
        int itemCount = existing.size();
        int offset = clampLootOffset(itemCount);

        List<LootItem> windowItems = new ArrayList<>();
        for (int row = 1; row <= MAX_LOOT_ROWS; row++) {
            int itemIndex = offset + row - 1;
            if (itemIndex > itemCount) {
                break;
            }
            LootItem fallback = itemIndex < itemCount ? existing.get(itemIndex) : null;

            String rawItemId = data.getBossLootName(row);
            String rawMinText = data.getBossLootMin(row);
            String rawMaxText = data.getBossLootMax(row);
            String rawChanceText = data.getBossLootChance(row);

            String itemId = optionalText(rawItemId);
            String minText = optionalText(rawMinText);
            String maxText = optionalText(rawMaxText);
            String chanceText = optionalText(rawChanceText);

            if (looksLikeUiBindingExpression(itemId)
                    || looksLikeUiBindingExpression(minText)
                    || looksLikeUiBindingExpression(maxText)
                    || looksLikeUiBindingExpression(chanceText)) {
                if (fallback != null) {
                    windowItems.add(new LootItem(fallback.itemId, fallback.dropChance, fallback.minAmount, fallback.maxAmount));
                }
                continue;
            }

            if (itemId.isEmpty()) {
                continue;
            }

            String resolvedMin = !minText.isEmpty() ? minText : (fallback != null ? Integer.toString(Math.max(1, fallback.minAmount)) : "1");
            String resolvedMax = !maxText.isEmpty() ? maxText : (fallback != null ? Integer.toString(Math.max(1, fallback.maxAmount)) : "1");
            String resolvedChance = !chanceText.isEmpty() ? chanceText : (fallback != null ? formatChance(clamp(fallback.dropChance, 0.0d, 1.0d)) : "0.3");

            int minAmount = parseRequiredInt(resolvedMin, "Quantité min butin doit être un entier sur la ligne " + row + ".", 1, Integer.MAX_VALUE);
            int maxAmount = parseRequiredInt(resolvedMax, "Quantité max butin doit être un entier sur la ligne " + row + ".", minAmount, Integer.MAX_VALUE);
            double chance = parseRequiredDouble(resolvedChance, "Chance de butin doit être entre 0.0 et 1.0 sur la ligne " + row + ".", 0.0d, 1.0d);
            windowItems.add(new LootItem(itemId, chance, minAmount, maxAmount));
        }

        List<LootItem> result = new ArrayList<>();
        for (int i = 0; i < Math.min(offset, itemCount); i++) {
            LootItem item = existing.get(i);
            if (item != null) {
                result.add(new LootItem(item.itemId, item.dropChance, item.minAmount, item.maxAmount));
            }
        }
        result.addAll(windowItems);
        for (int i = offset + MAX_LOOT_ROWS; i < itemCount; i++) {
            LootItem item = existing.get(i);
            if (item != null) {
                result.add(new LootItem(item.itemId, item.dropChance, item.minAmount, item.maxAmount));
            }
        }
        return result;
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
            outBoss.modifiers.movementSpeed = requireSliderFloat(
                    data.bossEditSpeed,
                    outBoss.modifiers.movementSpeed,
                    MULT_SCALE_MIN,
                    MULT_SCALE_MAX,
                    "Mult vitesse déplacement doit être entre 0.10 et 10.00."
            );
            outBoss.modifiers.size = requireSliderFloat(
                    data.bossEditSize,
                    outBoss.modifiers.size,
                    MULT_SIZE_MIN,
                    MULT_SIZE_MAX,
                    "Mult Taille doit être entre 0.10 et 10.00."
            );
            outBoss.modifiers.attackRate = requireSliderFloat(
                    data.bossEditAttackRate,
                    outBoss.modifiers.attackRate,
                    MULT_SCALE_MIN,
                    MULT_SCALE_MAX,
                    "Mult cadence d'attaque doit être entre 0.10 et 10.00."
            );
            // Recharge / Rotation UI removed — keep existing stored values.
            outBoss.modifiers.knockbackGiven = requireSliderFloat(
                    data.bossEditKnockbackGiven,
                    outBoss.modifiers.knockbackGiven,
                    MULT_SCALE_MIN,
                    MULT_SCALE_MAX,
                    "Mult knockback donné doit être entre 0.10 et 10.00."
            );
            outBoss.modifiers.knockbackTaken = requireSliderFloat(
                    data.bossEditKnockbackTaken,
                    outBoss.modifiers.knockbackTaken,
                    MULT_SCALE_MIN,
                    MULT_SCALE_MAX,
                    "Mult knockback reçu doit être entre 0.10 et 10.00."
            );
            outBoss.modifiers.regen = BossRegen.normalizeHpPerSecond(requireSliderFloat(
                    data.bossEditRegen,
                    outBoss.modifiers.regen,
                    REGEN_MIN,
                    REGEN_MAX,
                    "Régén PV/s doit être entre 0 et 1000."
            ));
            outBoss.perPlayerIncrease.hp = requirePerPlayerSlider(
                    data.bossEditPpHp, outBoss.perPlayerIncrease.hp, "PV/joueurs");
            outBoss.perPlayerIncrease.damage = requirePerPlayerSlider(
                    data.bossEditPpDamage, outBoss.perPlayerIncrease.damage, "Dégâts/joueurs");
            outBoss.perPlayerIncrease.movementSpeed = requirePerPlayerSlider(
                    data.bossEditPpSpeed, outBoss.perPlayerIncrease.movementSpeed, "Vitesse/joueurs");
            outBoss.perPlayerIncrease.size = requirePerPlayerSlider(
                    data.bossEditPpSize, outBoss.perPlayerIncrease.size, "Taille/joueurs");
            outBoss.perPlayerIncrease.attackRate = requirePerPlayerSlider(
                    data.bossEditPpAttackRate, outBoss.perPlayerIncrease.attackRate, "Attaque/joueurs");
            outBoss.perPlayerIncrease.knockbackGiven = requirePerPlayerSlider(
                    data.bossEditPpKnockbackGiven, outBoss.perPlayerIncrease.knockbackGiven, "Recul+/joueurs");
            outBoss.perPlayerIncrease.knockbackTaken = requirePerPlayerSlider(
                    data.bossEditPpKnockbackTaken, outBoss.perPlayerIncrease.knockbackTaken, "Recul-/joueurs");
            outBoss.perPlayerIncrease.regen = BossRegen.normalizeHpPerSecond(requireSliderFloat(
                    data.bossEditPpRegen,
                    outBoss.perPlayerIncrease.regen,
                    REGEN_MIN,
                    REGEN_MAX,
                    "Régén/j PV/s doit être entre 0 et 1000."
            ));
            if (outBoss.extraMobs == null) {
                outBoss.extraMobs = new BossDefinition.ExtraMobs();
            }
            applyBossSpawnSpreadFromData(outBoss, data);
            applyWaveSpawnSettingsFromData(outBoss.extraMobs, data);
            outBoss.extraMobs.waves = parseRequiredInt(
                    resolvedOrFallback(data.bossEditWaves, Integer.toString(Math.max(-1, outBoss.extraMobs.waves))),
                    "Vagues doit être -1 (infini) ou un entier >= 0.",
                    -1,
                    Integer.MAX_VALUE
            );
            outBoss.extraMobs.sanitize();

            String musicText = optionalText(data.bossEditMusic);
            if (!looksLikeUiBindingExpression(musicText)) {
                outBoss.musicFileName = musicText;
            }
            String musicRadiusText = resolvedOrFallback(
                    data.bossEditMusicRadius,
                    formatWaveNumber(outBoss.getMusicRadius())
            );
            if (!musicRadiusText.isEmpty()) {
                outBoss.musicRadius = parseRequiredDouble(
                        musicRadiusText,
                        "Le rayon musique doit être un nombre > 0.",
                        0.1d,
                        Double.MAX_VALUE
                );
            }

            LootTable outLoot = new LootTable();
            outLoot.bossName = outBoss.bossName;
            outLoot.lootRadius = 50.0d; // Only used for JSON; effective radius is arena Loot Radius or 50 when spawn "here"
            outLoot.items = new ArrayList<>();

            List<LootItem> mergedLoot = mergeLootWindow(data);
            bossEditorState.loot.items = mergedLoot;
            for (LootItem item : mergedLoot) {
                if (item == null) {
                    continue;
                }
                outLoot.items.add(new LootItem(item.itemId, item.dropChance, item.minAmount, item.maxAmount));
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
        if (data.bossEditSize != null && Float.isFinite(data.bossEditSize)) {
            boss.modifiers.size = BossArenaConfigUiControls.clampFloat(data.bossEditSize, MULT_SIZE_MIN, MULT_SIZE_MAX);
        }
        if (data.bossEditSpeed != null && Float.isFinite(data.bossEditSpeed)) {
            boss.modifiers.movementSpeed = BossArenaConfigUiControls.clampFloat(data.bossEditSpeed, MULT_SCALE_MIN, MULT_SCALE_MAX);
        }
        if (data.bossEditAttackRate != null && Float.isFinite(data.bossEditAttackRate)) {
            boss.modifiers.attackRate = BossArenaConfigUiControls.clampFloat(data.bossEditAttackRate, MULT_SCALE_MIN, MULT_SCALE_MAX);
        }
        if (data.bossEditKnockbackGiven != null && Float.isFinite(data.bossEditKnockbackGiven)) {
            boss.modifiers.knockbackGiven = BossArenaConfigUiControls.clampFloat(data.bossEditKnockbackGiven, MULT_SCALE_MIN, MULT_SCALE_MAX);
        }
        if (data.bossEditKnockbackTaken != null && Float.isFinite(data.bossEditKnockbackTaken)) {
            boss.modifiers.knockbackTaken = BossArenaConfigUiControls.clampFloat(data.bossEditKnockbackTaken, MULT_SCALE_MIN, MULT_SCALE_MAX);
        }
        if (data.bossEditRegen != null && Float.isFinite(data.bossEditRegen)) {
            boss.modifiers.regen = BossArenaConfigUiControls.clampFloat(data.bossEditRegen, REGEN_MIN, REGEN_MAX);
        }
        if (data.bossEditPpHp != null && Float.isFinite(data.bossEditPpHp)) {
            boss.perPlayerIncrease.hp = BossArenaConfigUiControls.clampFloat(data.bossEditPpHp, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpDamage != null && Float.isFinite(data.bossEditPpDamage)) {
            boss.perPlayerIncrease.damage = BossArenaConfigUiControls.clampFloat(data.bossEditPpDamage, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpSize != null && Float.isFinite(data.bossEditPpSize)) {
            boss.perPlayerIncrease.size = BossArenaConfigUiControls.clampFloat(data.bossEditPpSize, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpSpeed != null && Float.isFinite(data.bossEditPpSpeed)) {
            boss.perPlayerIncrease.movementSpeed = BossArenaConfigUiControls.clampFloat(data.bossEditPpSpeed, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpAttackRate != null && Float.isFinite(data.bossEditPpAttackRate)) {
            boss.perPlayerIncrease.attackRate = BossArenaConfigUiControls.clampFloat(data.bossEditPpAttackRate, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpKnockbackGiven != null && Float.isFinite(data.bossEditPpKnockbackGiven)) {
            boss.perPlayerIncrease.knockbackGiven = BossArenaConfigUiControls.clampFloat(data.bossEditPpKnockbackGiven, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpKnockbackTaken != null && Float.isFinite(data.bossEditPpKnockbackTaken)) {
            boss.perPlayerIncrease.knockbackTaken = BossArenaConfigUiControls.clampFloat(data.bossEditPpKnockbackTaken, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossEditPpRegen != null && Float.isFinite(data.bossEditPpRegen)) {
            boss.perPlayerIncrease.regen = BossArenaConfigUiControls.clampFloat(data.bossEditPpRegen, REGEN_MIN, REGEN_MAX);
        }
        Integer waves = parseOptionalInt(data.bossEditWaves);
        if (waves != null && waves >= -1) {
            if (boss.extraMobs == null) {
                boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            boss.extraMobs.waves = waves;
            boss.extraMobs.sanitize();
        }

        if (data.bossEditMusic != null) {
            String musicText = optionalText(data.bossEditMusic);
            if (!looksLikeUiBindingExpression(musicText)) {
                boss.musicFileName = musicText;
            }
        }
        Double musicRadius = parseOptionalDouble(data.bossEditMusicRadius);
        if (musicRadius != null && musicRadius > 0.0d) {
            boss.musicRadius = musicRadius;
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
            if (bossWavesOverlayOpen) {
                applyCurrentWavePage(extra, data);
            }
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
            if (bossWavesOverlayOpen) {
                applyCurrentWavePage(extra, data);
            }
            if ("boss_wave_random_toggle".equals(action)) {
                extra.useRandomSpawnLocations = !extra.useRandomSpawnLocations;
            } else if ("boss_waves_enabled_toggle".equals(action)) {
                List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(extra);
                BossDefinition.ExtraMobs.ScheduledWave current;
                if (bossWavePageIndex >= 0 && bossWavePageIndex < waves.size()) {
                    current = waves.get(bossWavePageIndex);
                } else {
                    current = newEmptyScheduledWave();
                    waves.add(current);
                    bossWavePageIndex = waves.size() - 1;
                }
                current.enabled = !current.enabled;
                extra.scheduledWaves = waves;
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
            applyCurrentWavePage(extra, data);
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
            applyCurrentWavePage(extra, data);

            String filter = optionalText(data.getBossWaveNpc(row));
            List<String> picks = BossArenaConfigUiControls.filterNpcIds(filter, BossArenaConfigUiControls.MAX_WAVE_NPC_PICKS);
            if (pick > picks.size()) {
                waveNpcPicksOpen = false;
                softUpdateWaveNpcPicks(false);
                return;
            }
            String chosen = picks.get(pick - 1);

            List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(extra);
            BossDefinition.ExtraMobs.ScheduledWave current;
            if (bossWavePageIndex >= 0 && bossWavePageIndex < waves.size()) {
                current = waves.get(bossWavePageIndex);
            } else {
                current = newEmptyScheduledWave();
                waves.add(current);
                bossWavePageIndex = waves.size() - 1;
            }
            if (current.adds == null) {
                current.adds = new ArrayList<>();
            }
            while (current.adds.size() < row) {
                BossDefinition.ExtraMobs.WaveAdd blank = new BossDefinition.ExtraMobs.WaveAdd();
                blank.npcId = "";
                blank.mobsPerWave = 1;
                blank.everyWave = 1;
                blank.hp = 1.0f;
                blank.damage = 1.0f;
                blank.size = 1.0f;
                current.adds.add(blank);
            }
            BossDefinition.ExtraMobs.WaveAdd add = current.adds.get(row - 1);
            add.npcId = chosen;
            extra.scheduledWaves = waves;
            extra.sanitize();
            waveNpcSearchQuery = chosen;
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
        List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(bossEditorState.boss.extraMobs);
        if (bossWavePageIndex < 0 || bossWavePageIndex >= waves.size()) {
            return "";
        }
        BossDefinition.ExtraMobs.ScheduledWave wave = waves.get(bossWavePageIndex);
        if (wave.adds == null || row < 1 || row > wave.adds.size()) {
            return "";
        }
        BossDefinition.ExtraMobs.WaveAdd add = wave.adds.get(row - 1);
        return safeText(add != null ? add.npcId : "");
    }

    private static int waveNpcPickTopForRow(int row) {
        int rowTop = 326 + Math.max(0, row - 1) * 32;
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
            anchor.setLeft(Value.of(146));
            anchor.setTop(Value.of(baseTop + (i - 1) * 20));
            anchor.setWidth(Value.of(340));
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
        cmd.set("#BossEditHpValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.hp, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX)));
        cmd.set("#BossEditDamageValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.damage, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX)));
        cmd.set("#BossEditSizeValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.size, MULT_SIZE_MIN, MULT_SIZE_MAX)));
        cmd.set("#BossEditSpeedValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.movementSpeed, MULT_SCALE_MIN, MULT_SCALE_MAX)));
        cmd.set("#BossEditAttackRateValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.attackRate, MULT_SCALE_MIN, MULT_SCALE_MAX)));
        cmd.set("#BossEditKnockbackGivenValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.knockbackGiven, MULT_SCALE_MIN, MULT_SCALE_MAX)));
        cmd.set("#BossEditKnockbackTakenValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.modifiers.knockbackTaken, MULT_SCALE_MIN, MULT_SCALE_MAX)));
        cmd.set("#BossEditRegenValue.Text", BossRegen.formatLabel(
                BossArenaConfigUiControls.clampFloat(boss.modifiers.regen, REGEN_MIN, REGEN_MAX)));
        cmd.set("#BossEditPpHpValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.hp, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpDamageValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.damage, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpSizeValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.size, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpSpeedValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.movementSpeed, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpAttackRateValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.attackRate, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpKnockbackGivenValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.knockbackGiven, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpKnockbackTakenValue.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.knockbackTaken, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpRegenValue.Text", BossRegen.formatLabel(
                BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.regen, REGEN_MIN, REGEN_MAX)));
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

    private static BossDefinition.ExtraMobs.ScheduledWave newEmptyScheduledWave() {
        BossDefinition.ExtraMobs.ScheduledWave wave = new BossDefinition.ExtraMobs.ScheduledWave();
        wave.enabled = true;
        wave.trigger = BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS;
        wave.triggerValue = 0.0d;
        wave.repeatCount = 1;
        wave.repeatEverySeconds = 0.0d;
        wave.adds = new ArrayList<>();
        return wave;
    }

    /** Returns a mutable, deep-copied list of the boss's scheduled waves (including disabled). */
    private static List<BossDefinition.ExtraMobs.ScheduledWave> readEditableWaves(BossDefinition.ExtraMobs extra) {
        if (extra == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(extra.getAllScheduledWaves());
    }

    /** Builds a ScheduledWave from the current page header (row-1 transport slots) and per-row adds. */
    private BossDefinition.ExtraMobs.ScheduledWave readCurrentWaveFromData(ConfigEventData data,
                                                                          BossDefinition.ExtraMobs.ScheduledWave fallback) {
        String triggerText = optionalText(data.getBossWaveEvery(1));
        String triggerValueText = optionalText(data.getBossWaveValue(1));
        String repeatCountText = optionalText(data.getBossWaveRepeatCount(1));
        String repeatSecText = optionalText(data.getBossWaveRepeatSec(1));
        if (looksLikeUiBindingExpression(triggerText)) {
            triggerText = "";
        }
        if (looksLikeUiBindingExpression(triggerValueText)) {
            triggerValueText = "";
        }
        if (looksLikeUiBindingExpression(repeatCountText)) {
            repeatCountText = "";
        }
        if (looksLikeUiBindingExpression(repeatSecText)) {
            repeatSecText = "";
        }

        String fallbackTrigger = fallback != null ? fallback.trigger : BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS;
        String resolvedTrigger = triggerText.isEmpty() ? fallbackTrigger : triggerText;
        String trigger = normalizeWaveTriggerInput(resolvedTrigger);
        if (trigger == null) {
            throw new IllegalArgumentException(
                    "Déclencheur de vague invalide. Utilisez : Avant le boss, Avec le boss, Après vague précédente, ou PV boss %."
            );
        }

        String defaultValue = fallback != null
                ? formatDouble(fallback.triggerValue)
                : (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger) ? "50" : "0");
        double triggerValue;
        if (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger)) {
            triggerValue = parseRequiredDouble(
                    resolvedOrFallback(triggerValueText, defaultValue),
                    "Valeur déclencheur vague doit être 0-100.",
                    0.0d,
                    100.0d
            );
        } else {
            triggerValue = parseRequiredDouble(
                    resolvedOrFallback(triggerValueText, defaultValue),
                    "Valeur déclencheur vague doit être >= 0.",
                    0.0d,
                    Double.MAX_VALUE
            );
        }

        int repeatCount = parseRequiredInt(
                resolvedOrFallback(repeatCountText, fallback != null ? Integer.toString(fallback.repeatCount) : "1"),
                "Nombre de répétitions vague doit être -1 ou >= 1.",
                -1,
                Integer.MAX_VALUE
        );
        // "Toutes(s)" removed from UI: waves no longer use a repeat interval.
        double repeatSec = 0.0d;

        BossDefinition.ExtraMobs.ScheduledWave wave = new BossDefinition.ExtraMobs.ScheduledWave();
        wave.enabled = fallback == null || fallback.enabled;
        String enabledText = optionalText(data.bossWavesEnabled);
        if (!enabledText.isEmpty() && !looksLikeUiBindingExpression(enabledText)) {
            Boolean parsedEnabled = parseToggleInput(enabledText);
            if (parsedEnabled != null) {
                wave.enabled = parsedEnabled;
            }
        }
        wave.trigger = trigger;
        wave.triggerValue = triggerValue;
        wave.repeatCount = repeatCount;
        wave.repeatEverySeconds = repeatSec;
        wave.adds = new ArrayList<>();

        List<BossDefinition.ExtraMobs.WaveAdd> fallbackAdds = fallback != null && fallback.adds != null
                ? fallback.adds
                : List.of();

        for (int row = 1; row <= MAX_WAVE_ADD_ROWS; row++) {
            String npcId = optionalText(data.getBossWaveNpc(row));
            String amountText = optionalText(data.getBossWaveAmount(row));
            String hpText = optionalText(data.getBossWaveHp(row));
            String damageText = optionalText(data.getBossWaveDamage(row));
            String sizeText = optionalText(data.getBossWaveSize(row));
            BossDefinition.ExtraMobs.WaveAdd fb = row <= fallbackAdds.size() ? fallbackAdds.get(row - 1) : null;

            if (looksLikeUiBindingExpression(npcId)
                    || looksLikeUiBindingExpression(amountText)
                    || looksLikeUiBindingExpression(hpText)
                    || looksLikeUiBindingExpression(damageText)
                    || looksLikeUiBindingExpression(sizeText)) {
                if (fb != null && fb.npcId != null && !fb.npcId.isBlank()) {
                    wave.adds.add(copyWaveAdd(fb));
                }
                continue;
            }

            if (npcId.isEmpty()) {
                continue;
            }

            int amount = parseRequiredInt(
                    amountText.isEmpty() ? (fb != null ? Integer.toString(Math.max(1, fb.mobsPerWave)) : "1") : amountText,
                    "Quantité vague doit être un entier sur la ligne " + row + ".",
                    1,
                    Integer.MAX_VALUE
            );
            String resolvedHp = !hpText.isEmpty() ? hpText : (fb != null ? formatFloat(fb.hp > 0f ? fb.hp : 1.0f) : "1.00");
            String resolvedDamage = !damageText.isEmpty() ? damageText : (fb != null ? formatFloat(fb.damage > 0f ? fb.damage : 1.0f) : "1.00");
            String resolvedSize = !sizeText.isEmpty() ? sizeText : (fb != null ? formatFloat(fb.size > 0f ? fb.size : 1.0f) : "1.00");
            float hp = parseRequiredFloat(resolvedHp, "Mult PV vague doit être entre 0.50 et 50.00 sur la ligne " + row + ".", MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
            float damage = parseRequiredFloat(resolvedDamage, "Mult dégâts vague doit être entre 0.50 et 50.00 sur la ligne " + row + ".", MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
            float size = parseRequiredFloat(resolvedSize, "Mult taille vague doit être entre 0.10 et 10.00 sur la ligne " + row + ".", MULT_SIZE_MIN, MULT_SIZE_MAX);

            BossDefinition.ExtraMobs.WaveAdd add = new BossDefinition.ExtraMobs.WaveAdd();
            add.npcId = npcId;
            add.mobsPerWave = amount;
            add.everyWave = 1;
            add.hp = hp;
            add.damage = damage;
            add.size = size;
            wave.adds.add(add);
        }

        return wave;
    }

    private static BossDefinition.ExtraMobs.WaveAdd copyWaveAdd(BossDefinition.ExtraMobs.WaveAdd source) {
        BossDefinition.ExtraMobs.WaveAdd copy = new BossDefinition.ExtraMobs.WaveAdd();
        copy.npcId = source.npcId;
        copy.mobsPerWave = Math.max(1, source.mobsPerWave);
        copy.everyWave = 1;
        copy.hp = source.hp > 0f ? source.hp : 1.0f;
        copy.damage = source.damage > 0f ? source.damage : 1.0f;
        copy.size = source.size > 0f ? source.size : 1.0f;
        return copy;
    }

    /** Reads the current page's edits from {@code data} and writes them into the boss's scheduled waves. */
    private void applyCurrentWavePage(BossDefinition.ExtraMobs extra, ConfigEventData data) {
        List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(extra);
        BossDefinition.ExtraMobs.ScheduledWave fallback =
                (bossWavePageIndex >= 0 && bossWavePageIndex < waves.size()) ? waves.get(bossWavePageIndex) : null;
        BossDefinition.ExtraMobs.ScheduledWave edited = readCurrentWaveFromData(data, fallback);
        if (bossWavePageIndex >= 0 && bossWavePageIndex < waves.size()) {
            if (edited.adds.isEmpty()) {
                waves.remove(bossWavePageIndex);
            } else {
                waves.set(bossWavePageIndex, edited);
            }
        } else if (!edited.adds.isEmpty()) {
            waves.add(edited);
            bossWavePageIndex = waves.size() - 1;
        }
        extra.scheduledWaves = waves;
        extra.sanitize();
    }

    private void handleBossWavePageChange(int delta, ConfigEventData data) {
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
            applyCurrentWavePage(extra, data);
            int waveCount = extra.getAllScheduledWaves().size();
            int target = bossWavePageIndex + delta;
            if (target < 0) {
                target = 0;
            }
            if (target > waveCount) {
                target = waveCount;
            }
            bossWavePageIndex = target;
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveTriggerChanged(ConfigEventData data) {
        if (!bossWavesOverlayOpen) {
            return;
        }
        String trigger = normalizeWaveTriggerInput(optionalText(data.getBossWaveEvery(1)));
        if (trigger == null) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#BossWaveTriggerValueFieldLabel.Text", waveTriggerValueUnitLabel(trigger));
        sendUpdate(cmd, false);
    }

    private void handleBossWaveAddPage(ConfigEventData data) {
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
            applyCurrentWavePage(extra, data);
            bossWavePageIndex = extra.getAllScheduledWaves().size();
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    private void handleBossWaveDeletePage(ConfigEventData data) {
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
            List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(extra);
            if (bossWavePageIndex >= 0 && bossWavePageIndex < waves.size()) {
                waves.remove(bossWavePageIndex);
            }
            extra.scheduledWaves = waves;
            extra.sanitize();
            if (bossWavePageIndex > 0 && bossWavePageIndex >= waves.size()) {
                bossWavePageIndex = Math.max(0, waves.size() - 1);
            }
            bossStatusText = "";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
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
            applyCurrentWavePage(extra, data);
            extra.sanitize();
            persistBossEditorDraftToDisk();
            bossStatusText = "Boss '" + bossEditorState.boss.bossName + "' enregistré (vagues).";
            rebuild();
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }

    /** Writes the in-memory boss editor draft (definition + loot) to registries and disk. */
    private void persistBossEditorDraftToDisk() {
        if (bossEditorState == null || bossEditorState.boss == null) {
            throw new IllegalArgumentException("Aucun boss en cours d'édition.");
        }

        BossDefinition outBoss = cloneBoss(bossEditorState.boss);
        if (outBoss.bossName == null || outBoss.bossName.isBlank()) {
            throw new IllegalArgumentException("BossID ne peut pas être vide.");
        }
        if (outBoss.npcId == null || outBoss.npcId.isBlank()) {
            throw new IllegalArgumentException("ID PNJ ne peut pas être vide.");
        }
        if (outBoss.extraMobs != null) {
            outBoss.extraMobs.sanitize();
        }

        LootTable outLoot = cloneLoot(bossEditorState.loot, outBoss.bossName);
        if (outLoot.items == null) {
            outLoot.items = new ArrayList<>();
        }

        String oldName = bossEditorState.originalBossName;
        boolean nameChanged = oldName != null && !oldName.equalsIgnoreCase(outBoss.bossName);
        if ((oldName == null || nameChanged) && BossRegistry.exists(outBoss.bossName)) {
            throw new IllegalArgumentException("Le boss '" + outBoss.bossName + "' existe déjà.");
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
            applyCurrentWavePage(extra, data);
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
            applyCurrentWavePage(extra, data);
            List<BossDefinition.ExtraMobs.ScheduledWave> waves = readEditableWaves(extra);
            if (bossWavePageIndex >= 0 && bossWavePageIndex < waves.size()) {
                BossDefinition.ExtraMobs.ScheduledWave current = waves.get(bossWavePageIndex);
                if (current.adds != null && row - 1 < current.adds.size()) {
                    current.adds.remove(row - 1);
                }
                if (current.adds == null || current.adds.isEmpty()) {
                    waves.remove(bossWavePageIndex);
                }
            }
            extra.scheduledWaves = waves;
            extra.sanitize();
            if (bossWavePageIndex > 0 && bossWavePageIndex >= waves.size()) {
                bossWavePageIndex = Math.max(0, waves.size() - 1);
            }
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
                    ? (fallback != null ? fallback.trigger : BossDefinition.ExtraMobs.TRIGGER_BEFORE_BOSS)
                    : triggerText;
            String trigger = normalizeWaveTriggerInput(resolvedTrigger);
            if (trigger == null) {
                throw new IllegalArgumentException(
                        "Déclencheur de vague invalide sur la ligne " + row
                                + ". Utilisez : Avant le boss, Avec le boss, Après vague précédente, ou PV boss %."
                );
            }

            String defaultValue = fallback != null
                    ? formatDouble(fallback.triggerValue)
                    : (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger) ? "50" : "0");
            double triggerValue;
            if (BossDefinition.ExtraMobs.TRIGGER_BOSS_HP_PERCENT.equals(trigger)) {
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
                    "Nombre de répétitions vague doit être -1 ou >= 1 sur la ligne " + row + ".",
                    -1,
                    Integer.MAX_VALUE
            );
            double repeatSec = 0.0d;

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

    private static final class ShopContractDraft {
        private String bossId = "";
        private String arenaId = "";
        private int cost = 0;
        private int silentCost = 0;
    }

    private static final class ShopLocationEditorState {
        private final ShopLocationRef shopLocation;
        private final List<ShopContractDraft> contracts;
        private String vendorName;
        private String currencyItemId;
        private int listOffset;

        private ShopLocationEditorState(ShopLocationRef shopLocation,
                                        List<ShopContractDraft> contracts,
                                        String vendorName,
                                        String currencyItemId) {
            this.shopLocation = shopLocation;
            this.contracts = contracts != null ? contracts : new ArrayList<>();
            this.vendorName = vendorName;
            this.currencyItemId = currencyItemId != null ? currencyItemId : "";
            this.listOffset = 0;
        }
    }

    private static final class BossPoolEditorState {
        private final int ruleRow;
        private final List<String> bossIds;
        private final LinkedHashMap<String, Integer> selectedWeights;
        private int pageOffset;
        private String statusText = "";

        private BossPoolEditorState(int ruleRow,
                                    List<String> bossIds,
                                    LinkedHashMap<String, Integer> selectedWeights) {
            this.ruleRow = ruleRow;
            this.bossIds = new ArrayList<>();
            this.selectedWeights = new LinkedHashMap<>();
            this.pageOffset = 0;
            if (selectedWeights != null) {
                for (Map.Entry<String, Integer> entry : selectedWeights.entrySet()) {
                    String id = optionalText(entry.getKey());
                    if (!id.isEmpty()) {
                        this.selectedWeights.put(id, Math.max(1, entry.getValue() == null ? 1 : entry.getValue()));
                    }
                }
            }
            mergeRegisteredBosses(bossIds);
        }

        private void mergeRegisteredBosses(List<String> registered) {
            LinkedHashSet<String> merged = new LinkedHashSet<>();
            if (registered != null) {
                for (String id : registered) {
                    String clean = optionalText(id);
                    if (!clean.isEmpty()) {
                        merged.add(clean);
                    }
                }
            }
            for (String id : selectedWeights.keySet()) {
                if (!containsIgnoreCase(merged, id)) {
                    merged.add(id);
                }
            }
            bossIds.clear();
            bossIds.addAll(merged);
            bossIds.sort(String.CASE_INSENSITIVE_ORDER);
            int maxOffset = Math.max(0, bossIds.size() - MAX_BOSS_POOL_ROWS);
            pageOffset = Math.max(0, Math.min(maxOffset, pageOffset));
        }

        private void ensureBossListed(String bossId) {
            String id = optionalText(bossId);
            if (id.isEmpty() || containsIgnoreCase(bossIds, id)) {
                return;
            }
            bossIds.add(id);
            bossIds.sort(String.CASE_INSENSITIVE_ORDER);
        }

        private Integer findSelectedWeight(String bossId) {
            String needle = optionalText(bossId);
            if (needle.isEmpty()) {
                return null;
            }
            for (Map.Entry<String, Integer> entry : selectedWeights.entrySet()) {
                if (needle.equalsIgnoreCase(optionalText(entry.getKey()))) {
                    return entry.getValue();
                }
            }
            return null;
        }

        private void putSelected(String bossId, int weight) {
            String id = optionalText(bossId);
            if (id.isEmpty()) {
                return;
            }
            removeSelected(id);
            selectedWeights.put(id, Math.max(1, weight));
        }

        private void removeSelected(String bossId) {
            String needle = optionalText(bossId);
            if (needle.isEmpty()) {
                return;
            }
            selectedWeights.entrySet().removeIf(entry ->
                    needle.equalsIgnoreCase(optionalText(entry.getKey())));
        }

        private static boolean containsIgnoreCase(Iterable<String> values, String needle) {
            String target = optionalText(needle);
            if (target.isEmpty()) {
                return false;
            }
            for (String value : values) {
                if (target.equalsIgnoreCase(optionalText(value))) {
                    return true;
                }
            }
            return false;
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
        private final int contractCount;

        private ShopLocationView(String arenaLabel,
                                 String distanceLabel,
                                 Double distance,
                                 String worldName,
                                 int x,
                                 int y,
                                 int z,
                                 int contractCount) {
            this.arenaLabel = arenaLabel;
            this.distanceLabel = distanceLabel;
            this.distance = distance;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.contractCount = contractCount;
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
        private final String[] lines;

        private BossWavesSummary(String[] lines) {
            this.lines = lines != null ? lines : new String[0];
        }

        private String lineAt(int index) {
            if (index < 0 || index >= lines.length || lines[index] == null) {
                return "";
            }
            return lines[index];
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
                .append(new KeyedCodec<>("@ArenaWorld", Codec.STRING), (d, v) -> d.arenaWorld = v, d -> d.arenaWorld).add()
                .append(new KeyedCodec<>("@ArenaX", Codec.STRING), (d, v) -> d.arenaX = v, d -> d.arenaX).add()
                .append(new KeyedCodec<>("@ArenaY", Codec.STRING), (d, v) -> d.arenaY = v, d -> d.arenaY).add()
                .append(new KeyedCodec<>("@ArenaZ", Codec.STRING), (d, v) -> d.arenaZ = v, d -> d.arenaZ).add()
                .append(new KeyedCodec<>("@ArenaRadius1", Codec.STRING), (d, v) -> d.arenaRadius1 = v, d -> d.arenaRadius1).add()
                .append(new KeyedCodec<>("@ArenaRadius2", Codec.STRING), (d, v) -> d.arenaRadius2 = v, d -> d.arenaRadius2).add()
                .append(new KeyedCodec<>("@ArenaRadius3", Codec.STRING), (d, v) -> d.arenaRadius3 = v, d -> d.arenaRadius3).add()
                .append(new KeyedCodec<>("@ArenaRadius4", Codec.STRING), (d, v) -> d.arenaRadius4 = v, d -> d.arenaRadius4).add()
                .append(new KeyedCodec<>("@ArenaRadius5", Codec.STRING), (d, v) -> d.arenaRadius5 = v, d -> d.arenaRadius5).add()
                .append(new KeyedCodec<>("@ArenaRadius6", Codec.STRING), (d, v) -> d.arenaRadius6 = v, d -> d.arenaRadius6).add()
                .append(new KeyedCodec<>("@ArenaRadius7", Codec.STRING), (d, v) -> d.arenaRadius7 = v, d -> d.arenaRadius7).add()
                .append(new KeyedCodec<>("@ArenaRadius8", Codec.STRING), (d, v) -> d.arenaRadius8 = v, d -> d.arenaRadius8).add()
                .append(new KeyedCodec<>("@ArenaProxEnabled", Codec.STRING), (d, v) -> d.arenaProxEnabled = v, d -> d.arenaProxEnabled).add()
                .append(new KeyedCodec<>("@ArenaProxCooldown", Codec.STRING), (d, v) -> d.arenaProxCooldown = v, d -> d.arenaProxCooldown).add()
                .append(new KeyedCodec<>("@ShopEditArenaId", Codec.STRING), (d, v) -> d.shopEditArenaId = v, d -> d.shopEditArenaId).add()
                .append(new KeyedCodec<>("@ShopEditVendorName", Codec.STRING), (d, v) -> d.shopEditVendorName = v, d -> d.shopEditVendorName).add()
                .append(new KeyedCodec<>("@ShopEditCurrencyItem", Codec.STRING), (d, v) -> d.shopEditCurrencyItem = v, d -> d.shopEditCurrencyItem).add()
                .append(new KeyedCodec<>("@ShopEditBoss1", Codec.STRING), (d, v) -> d.shopEditBoss1 = v, d -> d.shopEditBoss1).add()
                .append(new KeyedCodec<>("@ShopEditBoss2", Codec.STRING), (d, v) -> d.shopEditBoss2 = v, d -> d.shopEditBoss2).add()
                .append(new KeyedCodec<>("@ShopEditBoss3", Codec.STRING), (d, v) -> d.shopEditBoss3 = v, d -> d.shopEditBoss3).add()
                .append(new KeyedCodec<>("@ShopEditBoss4", Codec.STRING), (d, v) -> d.shopEditBoss4 = v, d -> d.shopEditBoss4).add()
                .append(new KeyedCodec<>("@ShopEditBoss5", Codec.STRING), (d, v) -> d.shopEditBoss5 = v, d -> d.shopEditBoss5).add()
                .append(new KeyedCodec<>("@ShopEditBoss6", Codec.STRING), (d, v) -> d.shopEditBoss6 = v, d -> d.shopEditBoss6).add()
                .append(new KeyedCodec<>("@ShopEditBoss7", Codec.STRING), (d, v) -> d.shopEditBoss7 = v, d -> d.shopEditBoss7).add()
                .append(new KeyedCodec<>("@ShopEditBoss8", Codec.STRING), (d, v) -> d.shopEditBoss8 = v, d -> d.shopEditBoss8).add()
                .append(new KeyedCodec<>("@ShopEditArena1", Codec.STRING), (d, v) -> d.shopEditArena1 = v, d -> d.shopEditArena1).add()
                .append(new KeyedCodec<>("@ShopEditArena2", Codec.STRING), (d, v) -> d.shopEditArena2 = v, d -> d.shopEditArena2).add()
                .append(new KeyedCodec<>("@ShopEditArena3", Codec.STRING), (d, v) -> d.shopEditArena3 = v, d -> d.shopEditArena3).add()
                .append(new KeyedCodec<>("@ShopEditArena4", Codec.STRING), (d, v) -> d.shopEditArena4 = v, d -> d.shopEditArena4).add()
                .append(new KeyedCodec<>("@ShopEditArena5", Codec.STRING), (d, v) -> d.shopEditArena5 = v, d -> d.shopEditArena5).add()
                .append(new KeyedCodec<>("@ShopEditArena6", Codec.STRING), (d, v) -> d.shopEditArena6 = v, d -> d.shopEditArena6).add()
                .append(new KeyedCodec<>("@ShopEditArena7", Codec.STRING), (d, v) -> d.shopEditArena7 = v, d -> d.shopEditArena7).add()
                .append(new KeyedCodec<>("@ShopEditArena8", Codec.STRING), (d, v) -> d.shopEditArena8 = v, d -> d.shopEditArena8).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice1", Codec.STRING), (d, v) -> d.shopEditBossPrice1 = v, d -> d.shopEditBossPrice1).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice2", Codec.STRING), (d, v) -> d.shopEditBossPrice2 = v, d -> d.shopEditBossPrice2).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice3", Codec.STRING), (d, v) -> d.shopEditBossPrice3 = v, d -> d.shopEditBossPrice3).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice4", Codec.STRING), (d, v) -> d.shopEditBossPrice4 = v, d -> d.shopEditBossPrice4).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice5", Codec.STRING), (d, v) -> d.shopEditBossPrice5 = v, d -> d.shopEditBossPrice5).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice6", Codec.STRING), (d, v) -> d.shopEditBossPrice6 = v, d -> d.shopEditBossPrice6).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice7", Codec.STRING), (d, v) -> d.shopEditBossPrice7 = v, d -> d.shopEditBossPrice7).add()
                .append(new KeyedCodec<>("@ShopEditBossPrice8", Codec.STRING), (d, v) -> d.shopEditBossPrice8 = v, d -> d.shopEditBossPrice8).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice1", Codec.STRING), (d, v) -> d.shopEditSilentPrice1 = v, d -> d.shopEditSilentPrice1).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice2", Codec.STRING), (d, v) -> d.shopEditSilentPrice2 = v, d -> d.shopEditSilentPrice2).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice3", Codec.STRING), (d, v) -> d.shopEditSilentPrice3 = v, d -> d.shopEditSilentPrice3).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice4", Codec.STRING), (d, v) -> d.shopEditSilentPrice4 = v, d -> d.shopEditSilentPrice4).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice5", Codec.STRING), (d, v) -> d.shopEditSilentPrice5 = v, d -> d.shopEditSilentPrice5).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice6", Codec.STRING), (d, v) -> d.shopEditSilentPrice6 = v, d -> d.shopEditSilentPrice6).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice7", Codec.STRING), (d, v) -> d.shopEditSilentPrice7 = v, d -> d.shopEditSilentPrice7).add()
                .append(new KeyedCodec<>("@ShopEditSilentPrice8", Codec.STRING), (d, v) -> d.shopEditSilentPrice8 = v, d -> d.shopEditSilentPrice8).add()

                .append(new KeyedCodec<>("@BossEditName", Codec.STRING), (d, v) -> d.bossEditName = v, d -> d.bossEditName).add()
                .append(new KeyedCodec<>("@BossEditNpcId", Codec.STRING), (d, v) -> d.bossEditNpcId = v, d -> d.bossEditNpcId).add()
                .append(new KeyedCodec<>("@BossEditTier", Codec.STRING), (d, v) -> d.bossEditTier = v, d -> d.bossEditTier).add()
                .append(new KeyedCodec<>("@BossEditAmount", Codec.STRING), (d, v) -> d.bossEditAmount = v, d -> d.bossEditAmount).add()
                .append(new KeyedCodec<>("@BossEditLevelOverride", Codec.STRING), (d, v) -> d.bossEditLevelOverride = v, d -> d.bossEditLevelOverride).add()
                .append(new KeyedCodec<>("@BossEditHp", Codec.FLOAT), (d, v) -> d.bossEditHp = v, d -> d.bossEditHp).add()
                .append(new KeyedCodec<>("@BossEditDamage", Codec.FLOAT), (d, v) -> d.bossEditDamage = v, d -> d.bossEditDamage).add()
                .append(new KeyedCodec<>("@BossEditSpeed", Codec.FLOAT), (d, v) -> d.bossEditSpeed = v, d -> d.bossEditSpeed).add()
                .append(new KeyedCodec<>("@BossEditSize", Codec.FLOAT), (d, v) -> d.bossEditSize = v, d -> d.bossEditSize).add()
                .append(new KeyedCodec<>("@BossEditAttackRate", Codec.FLOAT), (d, v) -> d.bossEditAttackRate = v, d -> d.bossEditAttackRate).add()
                .append(new KeyedCodec<>("@BossEditAbilityCooldown", Codec.FLOAT), (d, v) -> d.bossEditAbilityCooldown = v, d -> d.bossEditAbilityCooldown).add()
                .append(new KeyedCodec<>("@BossEditKnockbackGiven", Codec.FLOAT), (d, v) -> d.bossEditKnockbackGiven = v, d -> d.bossEditKnockbackGiven).add()
                .append(new KeyedCodec<>("@BossEditKnockbackTaken", Codec.FLOAT), (d, v) -> d.bossEditKnockbackTaken = v, d -> d.bossEditKnockbackTaken).add()
                .append(new KeyedCodec<>("@BossEditTurnRate", Codec.FLOAT), (d, v) -> d.bossEditTurnRate = v, d -> d.bossEditTurnRate).add()
                .append(new KeyedCodec<>("@BossEditRegen", Codec.FLOAT), (d, v) -> d.bossEditRegen = v, d -> d.bossEditRegen).add()
                .append(new KeyedCodec<>("@BossEditPpHp", Codec.FLOAT), (d, v) -> d.bossEditPpHp = v, d -> d.bossEditPpHp).add()
                .append(new KeyedCodec<>("@BossEditPpDamage", Codec.FLOAT), (d, v) -> d.bossEditPpDamage = v, d -> d.bossEditPpDamage).add()
                .append(new KeyedCodec<>("@BossEditPpSpeed", Codec.FLOAT), (d, v) -> d.bossEditPpSpeed = v, d -> d.bossEditPpSpeed).add()
                .append(new KeyedCodec<>("@BossEditPpSize", Codec.FLOAT), (d, v) -> d.bossEditPpSize = v, d -> d.bossEditPpSize).add()
                .append(new KeyedCodec<>("@BossEditPpAttackRate", Codec.FLOAT), (d, v) -> d.bossEditPpAttackRate = v, d -> d.bossEditPpAttackRate).add()
                .append(new KeyedCodec<>("@BossEditPpAbilityCooldown", Codec.FLOAT), (d, v) -> d.bossEditPpAbilityCooldown = v, d -> d.bossEditPpAbilityCooldown).add()
                .append(new KeyedCodec<>("@BossEditPpKnockbackGiven", Codec.FLOAT), (d, v) -> d.bossEditPpKnockbackGiven = v, d -> d.bossEditPpKnockbackGiven).add()
                .append(new KeyedCodec<>("@BossEditPpKnockbackTaken", Codec.FLOAT), (d, v) -> d.bossEditPpKnockbackTaken = v, d -> d.bossEditPpKnockbackTaken).add()
                .append(new KeyedCodec<>("@BossEditPpTurnRate", Codec.FLOAT), (d, v) -> d.bossEditPpTurnRate = v, d -> d.bossEditPpTurnRate).add()
                .append(new KeyedCodec<>("@BossEditPpRegen", Codec.FLOAT), (d, v) -> d.bossEditPpRegen = v, d -> d.bossEditPpRegen).add()
                .append(new KeyedCodec<>("@BossEditWaves", Codec.STRING), (d, v) -> d.bossEditWaves = v, d -> d.bossEditWaves).add()
                .append(new KeyedCodec<>("@BossEditExtraNpcId", Codec.STRING), (d, v) -> d.bossEditExtraNpcId = v, d -> d.bossEditExtraNpcId).add()
                .append(new KeyedCodec<>("@BossEditExtraTimeLimit", Codec.STRING), (d, v) -> d.bossEditExtraTimeLimit = v, d -> d.bossEditExtraTimeLimit).add()
                .append(new KeyedCodec<>("@BossEditExtraWaves", Codec.STRING), (d, v) -> d.bossEditExtraWaves = v, d -> d.bossEditExtraWaves).add()
                .append(new KeyedCodec<>("@BossEditExtraMobsPerWave", Codec.STRING), (d, v) -> d.bossEditExtraMobsPerWave = v, d -> d.bossEditExtraMobsPerWave).add()

                .append(new KeyedCodec<>("@BossSpawnTrigger", Codec.STRING), (d, v) -> d.bossSpawnTrigger = v, d -> d.bossSpawnTrigger).add()
                .append(new KeyedCodec<>("@BossSpawnTriggerValue", Codec.STRING), (d, v) -> d.bossSpawnTriggerValue = v, d -> d.bossSpawnTriggerValue).add()
                .append(new KeyedCodec<>("@BossSpawnSpreadRandom", Codec.STRING), (d, v) -> d.bossSpawnSpreadRandom = v, d -> d.bossSpawnSpreadRandom).add()
                .append(new KeyedCodec<>("@BossSpawnSpreadRadius", Codec.STRING), (d, v) -> d.bossSpawnSpreadRadius = v, d -> d.bossSpawnSpreadRadius).add()
                .append(new KeyedCodec<>("@BossWaveRandomLocations", Codec.STRING), (d, v) -> d.bossWaveRandomLocations = v, d -> d.bossWaveRandomLocations).add()
                .append(new KeyedCodec<>("@BossWaveRandomRadius", Codec.STRING), (d, v) -> d.bossWaveRandomRadius = v, d -> d.bossWaveRandomRadius).add()
                .append(new KeyedCodec<>("@BossWavesEnabled", Codec.STRING), (d, v) -> d.bossWavesEnabled = v, d -> d.bossWavesEnabled).add()
                .append(new KeyedCodec<>("@BossEditMusic", Codec.STRING), (d, v) -> d.bossEditMusic = v, d -> d.bossEditMusic).add()
                .append(new KeyedCodec<>("@BossEditMusicRadius", Codec.STRING), (d, v) -> d.bossEditMusicRadius = v, d -> d.bossEditMusicRadius).add()
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
                .append(new KeyedCodec<>("@TimedMinPlayers1", Codec.STRING), (d, v) -> d.timedMinPlayers1 = v, d -> d.timedMinPlayers1).add()
                .append(new KeyedCodec<>("@TimedMinPlayers2", Codec.STRING), (d, v) -> d.timedMinPlayers2 = v, d -> d.timedMinPlayers2).add()
                .append(new KeyedCodec<>("@TimedMinPlayers3", Codec.STRING), (d, v) -> d.timedMinPlayers3 = v, d -> d.timedMinPlayers3).add()
                .append(new KeyedCodec<>("@TimedMinPlayers4", Codec.STRING), (d, v) -> d.timedMinPlayers4 = v, d -> d.timedMinPlayers4).add()
                .append(new KeyedCodec<>("@TimedMinPlayers5", Codec.STRING), (d, v) -> d.timedMinPlayers5 = v, d -> d.timedMinPlayers5).add()
                .append(new KeyedCodec<>("@TimedMinPlayers6", Codec.STRING), (d, v) -> d.timedMinPlayers6 = v, d -> d.timedMinPlayers6).add()
                .append(new KeyedCodec<>("@TimedEverySeconds1", Codec.STRING), (d, v) -> d.timedEverySeconds1 = v, d -> d.timedEverySeconds1).add()
                .append(new KeyedCodec<>("@TimedEverySeconds2", Codec.STRING), (d, v) -> d.timedEverySeconds2 = v, d -> d.timedEverySeconds2).add()
                .append(new KeyedCodec<>("@TimedEverySeconds3", Codec.STRING), (d, v) -> d.timedEverySeconds3 = v, d -> d.timedEverySeconds3).add()
                .append(new KeyedCodec<>("@TimedEverySeconds4", Codec.STRING), (d, v) -> d.timedEverySeconds4 = v, d -> d.timedEverySeconds4).add()
                .append(new KeyedCodec<>("@TimedEverySeconds5", Codec.STRING), (d, v) -> d.timedEverySeconds5 = v, d -> d.timedEverySeconds5).add()
                .append(new KeyedCodec<>("@TimedEverySeconds6", Codec.STRING), (d, v) -> d.timedEverySeconds6 = v, d -> d.timedEverySeconds6).add()
                .append(new KeyedCodec<>("@TimedIntervalHours1", Codec.STRING), (d, v) -> d.timedIntervalHours1 = v, d -> d.timedIntervalHours1).add()
                .append(new KeyedCodec<>("@TimedIntervalHours2", Codec.STRING), (d, v) -> d.timedIntervalHours2 = v, d -> d.timedIntervalHours2).add()
                .append(new KeyedCodec<>("@TimedIntervalHours3", Codec.STRING), (d, v) -> d.timedIntervalHours3 = v, d -> d.timedIntervalHours3).add()
                .append(new KeyedCodec<>("@TimedIntervalHours4", Codec.STRING), (d, v) -> d.timedIntervalHours4 = v, d -> d.timedIntervalHours4).add()
                .append(new KeyedCodec<>("@TimedIntervalHours5", Codec.STRING), (d, v) -> d.timedIntervalHours5 = v, d -> d.timedIntervalHours5).add()
                .append(new KeyedCodec<>("@TimedIntervalHours6", Codec.STRING), (d, v) -> d.timedIntervalHours6 = v, d -> d.timedIntervalHours6).add()
                .append(new KeyedCodec<>("@TimedIntervalDays1", Codec.STRING), (d, v) -> d.timedIntervalDays1 = v, d -> d.timedIntervalDays1).add()
                .append(new KeyedCodec<>("@TimedIntervalDays2", Codec.STRING), (d, v) -> d.timedIntervalDays2 = v, d -> d.timedIntervalDays2).add()
                .append(new KeyedCodec<>("@TimedIntervalDays3", Codec.STRING), (d, v) -> d.timedIntervalDays3 = v, d -> d.timedIntervalDays3).add()
                .append(new KeyedCodec<>("@TimedIntervalDays4", Codec.STRING), (d, v) -> d.timedIntervalDays4 = v, d -> d.timedIntervalDays4).add()
                .append(new KeyedCodec<>("@TimedIntervalDays5", Codec.STRING), (d, v) -> d.timedIntervalDays5 = v, d -> d.timedIntervalDays5).add()
                .append(new KeyedCodec<>("@TimedIntervalDays6", Codec.STRING), (d, v) -> d.timedIntervalDays6 = v, d -> d.timedIntervalDays6).add()
                .append(new KeyedCodec<>("@TimedIntervalSeconds1", Codec.STRING), (d, v) -> d.timedIntervalSeconds1 = v, d -> d.timedIntervalSeconds1).add()
                .append(new KeyedCodec<>("@TimedIntervalSeconds2", Codec.STRING), (d, v) -> d.timedIntervalSeconds2 = v, d -> d.timedIntervalSeconds2).add()
                .append(new KeyedCodec<>("@TimedIntervalSeconds3", Codec.STRING), (d, v) -> d.timedIntervalSeconds3 = v, d -> d.timedIntervalSeconds3).add()
                .append(new KeyedCodec<>("@TimedIntervalSeconds4", Codec.STRING), (d, v) -> d.timedIntervalSeconds4 = v, d -> d.timedIntervalSeconds4).add()
                .append(new KeyedCodec<>("@TimedIntervalSeconds5", Codec.STRING), (d, v) -> d.timedIntervalSeconds5 = v, d -> d.timedIntervalSeconds5).add()
                .append(new KeyedCodec<>("@TimedIntervalSeconds6", Codec.STRING), (d, v) -> d.timedIntervalSeconds6 = v, d -> d.timedIntervalSeconds6).add()
                .append(new KeyedCodec<>("@TimedArrivalHours1", Codec.STRING), (d, v) -> d.timedArrivalHours1 = v, d -> d.timedArrivalHours1).add()
                .append(new KeyedCodec<>("@TimedArrivalHours2", Codec.STRING), (d, v) -> d.timedArrivalHours2 = v, d -> d.timedArrivalHours2).add()
                .append(new KeyedCodec<>("@TimedArrivalHours3", Codec.STRING), (d, v) -> d.timedArrivalHours3 = v, d -> d.timedArrivalHours3).add()
                .append(new KeyedCodec<>("@TimedArrivalHours4", Codec.STRING), (d, v) -> d.timedArrivalHours4 = v, d -> d.timedArrivalHours4).add()
                .append(new KeyedCodec<>("@TimedArrivalHours5", Codec.STRING), (d, v) -> d.timedArrivalHours5 = v, d -> d.timedArrivalHours5).add()
                .append(new KeyedCodec<>("@TimedArrivalHours6", Codec.STRING), (d, v) -> d.timedArrivalHours6 = v, d -> d.timedArrivalHours6).add()
                .append(new KeyedCodec<>("@TimedArrivalMinutes1", Codec.STRING), (d, v) -> d.timedArrivalMinutes1 = v, d -> d.timedArrivalMinutes1).add()
                .append(new KeyedCodec<>("@TimedArrivalMinutes2", Codec.STRING), (d, v) -> d.timedArrivalMinutes2 = v, d -> d.timedArrivalMinutes2).add()
                .append(new KeyedCodec<>("@TimedArrivalMinutes3", Codec.STRING), (d, v) -> d.timedArrivalMinutes3 = v, d -> d.timedArrivalMinutes3).add()
                .append(new KeyedCodec<>("@TimedArrivalMinutes4", Codec.STRING), (d, v) -> d.timedArrivalMinutes4 = v, d -> d.timedArrivalMinutes4).add()
                .append(new KeyedCodec<>("@TimedArrivalMinutes5", Codec.STRING), (d, v) -> d.timedArrivalMinutes5 = v, d -> d.timedArrivalMinutes5).add()
                .append(new KeyedCodec<>("@TimedArrivalMinutes6", Codec.STRING), (d, v) -> d.timedArrivalMinutes6 = v, d -> d.timedArrivalMinutes6).add()
                .append(new KeyedCodec<>("@TimedArrivalSeconds1", Codec.STRING), (d, v) -> d.timedArrivalSeconds1 = v, d -> d.timedArrivalSeconds1).add()
                .append(new KeyedCodec<>("@TimedArrivalSeconds2", Codec.STRING), (d, v) -> d.timedArrivalSeconds2 = v, d -> d.timedArrivalSeconds2).add()
                .append(new KeyedCodec<>("@TimedArrivalSeconds3", Codec.STRING), (d, v) -> d.timedArrivalSeconds3 = v, d -> d.timedArrivalSeconds3).add()
                .append(new KeyedCodec<>("@TimedArrivalSeconds4", Codec.STRING), (d, v) -> d.timedArrivalSeconds4 = v, d -> d.timedArrivalSeconds4).add()
                .append(new KeyedCodec<>("@TimedArrivalSeconds5", Codec.STRING), (d, v) -> d.timedArrivalSeconds5 = v, d -> d.timedArrivalSeconds5).add()
                .append(new KeyedCodec<>("@TimedArrivalSeconds6", Codec.STRING), (d, v) -> d.timedArrivalSeconds6 = v, d -> d.timedArrivalSeconds6).add()
                .append(new KeyedCodec<>("@TimedRequirePlayer1", Codec.STRING), (d, v) -> d.timedRequirePlayer1 = v, d -> d.timedRequirePlayer1).add()
                .append(new KeyedCodec<>("@TimedRequirePlayer2", Codec.STRING), (d, v) -> d.timedRequirePlayer2 = v, d -> d.timedRequirePlayer2).add()
                .append(new KeyedCodec<>("@TimedRequirePlayer3", Codec.STRING), (d, v) -> d.timedRequirePlayer3 = v, d -> d.timedRequirePlayer3).add()
                .append(new KeyedCodec<>("@TimedRequirePlayer4", Codec.STRING), (d, v) -> d.timedRequirePlayer4 = v, d -> d.timedRequirePlayer4).add()
                .append(new KeyedCodec<>("@TimedRequirePlayer5", Codec.STRING), (d, v) -> d.timedRequirePlayer5 = v, d -> d.timedRequirePlayer5).add()
                .append(new KeyedCodec<>("@TimedRequirePlayer6", Codec.STRING), (d, v) -> d.timedRequirePlayer6 = v, d -> d.timedRequirePlayer6).add()
                .append(new KeyedCodec<>("@TimedAnnounceGlobal1", Codec.STRING), (d, v) -> d.timedAnnounceGlobal1 = v, d -> d.timedAnnounceGlobal1).add()
                .append(new KeyedCodec<>("@TimedAnnounceGlobal2", Codec.STRING), (d, v) -> d.timedAnnounceGlobal2 = v, d -> d.timedAnnounceGlobal2).add()
                .append(new KeyedCodec<>("@TimedAnnounceGlobal3", Codec.STRING), (d, v) -> d.timedAnnounceGlobal3 = v, d -> d.timedAnnounceGlobal3).add()
                .append(new KeyedCodec<>("@TimedAnnounceGlobal4", Codec.STRING), (d, v) -> d.timedAnnounceGlobal4 = v, d -> d.timedAnnounceGlobal4).add()
                .append(new KeyedCodec<>("@TimedAnnounceGlobal5", Codec.STRING), (d, v) -> d.timedAnnounceGlobal5 = v, d -> d.timedAnnounceGlobal5).add()
                .append(new KeyedCodec<>("@TimedAnnounceGlobal6", Codec.STRING), (d, v) -> d.timedAnnounceGlobal6 = v, d -> d.timedAnnounceGlobal6).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorld1", Codec.STRING), (d, v) -> d.timedAnnounceWorld1 = v, d -> d.timedAnnounceWorld1).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorld2", Codec.STRING), (d, v) -> d.timedAnnounceWorld2 = v, d -> d.timedAnnounceWorld2).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorld3", Codec.STRING), (d, v) -> d.timedAnnounceWorld3 = v, d -> d.timedAnnounceWorld3).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorld4", Codec.STRING), (d, v) -> d.timedAnnounceWorld4 = v, d -> d.timedAnnounceWorld4).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorld5", Codec.STRING), (d, v) -> d.timedAnnounceWorld5 = v, d -> d.timedAnnounceWorld5).add()
                .append(new KeyedCodec<>("@TimedAnnounceWorld6", Codec.STRING), (d, v) -> d.timedAnnounceWorld6 = v, d -> d.timedAnnounceWorld6).add()
                .append(new KeyedCodec<>("@TimedAnnounceText", Codec.STRING), (d, v) -> d.timedAnnounceText = v, d -> d.timedAnnounceText).add()
                .append(new KeyedCodec<>("@TimedReminderText", Codec.STRING), (d, v) -> d.timedReminderText = v, d -> d.timedReminderText).add()
                .append(new KeyedCodec<>("@TimedGraceText", Codec.STRING), (d, v) -> d.timedGraceText = v, d -> d.timedGraceText).add()
                .append(new KeyedCodec<>("@BossPoolPick", Codec.STRING), (d, v) -> d.bossPoolPick = v, d -> d.bossPoolPick).add()
                .append(new KeyedCodec<>("@TimedGraceEnabled1", Codec.STRING), (d, v) -> d.timedGraceEnabled1 = v, d -> d.timedGraceEnabled1).add()
                .append(new KeyedCodec<>("@TimedGraceEnabled2", Codec.STRING), (d, v) -> d.timedGraceEnabled2 = v, d -> d.timedGraceEnabled2).add()
                .append(new KeyedCodec<>("@TimedGraceEnabled3", Codec.STRING), (d, v) -> d.timedGraceEnabled3 = v, d -> d.timedGraceEnabled3).add()
                .append(new KeyedCodec<>("@TimedGraceEnabled4", Codec.STRING), (d, v) -> d.timedGraceEnabled4 = v, d -> d.timedGraceEnabled4).add()
                .append(new KeyedCodec<>("@TimedGraceEnabled5", Codec.STRING), (d, v) -> d.timedGraceEnabled5 = v, d -> d.timedGraceEnabled5).add()
                .append(new KeyedCodec<>("@TimedGraceEnabled6", Codec.STRING), (d, v) -> d.timedGraceEnabled6 = v, d -> d.timedGraceEnabled6).add()
                .append(new KeyedCodec<>("@TimedGraceSeconds1", Codec.STRING), (d, v) -> d.timedGraceSeconds1 = v, d -> d.timedGraceSeconds1).add()
                .append(new KeyedCodec<>("@TimedGraceSeconds2", Codec.STRING), (d, v) -> d.timedGraceSeconds2 = v, d -> d.timedGraceSeconds2).add()
                .append(new KeyedCodec<>("@TimedGraceSeconds3", Codec.STRING), (d, v) -> d.timedGraceSeconds3 = v, d -> d.timedGraceSeconds3).add()
                .append(new KeyedCodec<>("@TimedGraceSeconds4", Codec.STRING), (d, v) -> d.timedGraceSeconds4 = v, d -> d.timedGraceSeconds4).add()
                .append(new KeyedCodec<>("@TimedGraceSeconds5", Codec.STRING), (d, v) -> d.timedGraceSeconds5 = v, d -> d.timedGraceSeconds5).add()
                .append(new KeyedCodec<>("@TimedGraceSeconds6", Codec.STRING), (d, v) -> d.timedGraceSeconds6 = v, d -> d.timedGraceSeconds6).add()

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
        public String arenaWorld;
        public String arenaX;
        public String arenaY;
        public String arenaZ;
        public String arenaRadius1;
        public String arenaRadius2;
        public String arenaRadius3;
        public String arenaRadius4;
        public String arenaRadius5;
        public String arenaRadius6;
        public String arenaRadius7;
        public String arenaRadius8;
        public String arenaProxEnabled;
        public String arenaProxCooldown;
        public String shopEditArenaId;
        public String shopEditVendorName;
        public String shopEditCurrencyItem;
        public String shopEditBoss1;
        public String shopEditBoss2;
        public String shopEditBoss3;
        public String shopEditBoss4;
        public String shopEditBoss5;
        public String shopEditBoss6;
        public String shopEditBoss7;
        public String shopEditBoss8;
        public String shopEditArena1;
        public String shopEditArena2;
        public String shopEditArena3;
        public String shopEditArena4;
        public String shopEditArena5;
        public String shopEditArena6;
        public String shopEditArena7;
        public String shopEditArena8;
        public String shopEditBossPrice1;
        public String shopEditBossPrice2;
        public String shopEditBossPrice3;
        public String shopEditBossPrice4;
        public String shopEditBossPrice5;
        public String shopEditBossPrice6;
        public String shopEditBossPrice7;
        public String shopEditBossPrice8;
        public String shopEditSilentPrice1;
        public String shopEditSilentPrice2;
        public String shopEditSilentPrice3;
        public String shopEditSilentPrice4;
        public String shopEditSilentPrice5;
        public String shopEditSilentPrice6;
        public String shopEditSilentPrice7;
        public String shopEditSilentPrice8;
        public String bossEditName;
        public String bossEditNpcId;
        public String bossEditTier;
        public String bossEditAmount;
        public String bossEditLevelOverride;
        public Float bossEditHp;
        public Float bossEditDamage;
        public Float bossEditSpeed;
        public Float bossEditSize;
        public Float bossEditAttackRate;
        public Float bossEditAbilityCooldown;
        public Float bossEditKnockbackGiven;
        public Float bossEditKnockbackTaken;
        public Float bossEditTurnRate;
        public Float bossEditRegen;
        public Float bossEditPpHp;
        public Float bossEditPpDamage;
        public Float bossEditPpSpeed;
        public Float bossEditPpSize;
        public Float bossEditPpAttackRate;
        public Float bossEditPpAbilityCooldown;
        public Float bossEditPpKnockbackGiven;
        public Float bossEditPpKnockbackTaken;
        public Float bossEditPpTurnRate;
        public Float bossEditPpRegen;
        public String bossEditWaves;
        public String bossEditExtraNpcId;
        public String bossEditExtraTimeLimit;
        public String bossEditExtraWaves;
        public String bossEditExtraMobsPerWave;
        public String bossSpawnTrigger;
        public String bossSpawnSpreadRandom;
        public String bossSpawnSpreadRadius;
        public String bossSpawnTriggerValue;
        public String bossWaveRandomLocations;
        public String bossWaveRandomRadius;
        public String bossWavesEnabled;
        public String bossEditMusic;
        public String bossEditMusicRadius;
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
        public String timedAnnounceText;
        public String timedReminderText;
        public String timedGraceText;
        public String bossPoolPick;
        public String timedGraceEnabled1;
        public String timedGraceEnabled2;
        public String timedGraceEnabled3;
        public String timedGraceEnabled4;
        public String timedGraceEnabled5;
        public String timedGraceEnabled6;
        public String timedGraceSeconds1;
        public String timedGraceSeconds2;
        public String timedGraceSeconds3;
        public String timedGraceSeconds4;
        public String timedGraceSeconds5;
        public String timedGraceSeconds6;
        public String timedAnnounceGlobal1;
        public String timedAnnounceGlobal2;
        public String timedAnnounceGlobal3;
        public String timedAnnounceGlobal4;
        public String timedAnnounceGlobal5;
        public String timedAnnounceGlobal6;
        public String timedAnnounceWorld1;
        public String timedAnnounceWorld2;
        public String timedAnnounceWorld3;
        public String timedAnnounceWorld4;
        public String timedAnnounceWorld5;
        public String timedAnnounceWorld6;
        public String timedMinPlayers1;
        public String timedMinPlayers2;
        public String timedMinPlayers3;
        public String timedMinPlayers4;
        public String timedMinPlayers5;
        public String timedMinPlayers6;
        public String timedEverySeconds1;
        public String timedEverySeconds2;
        public String timedEverySeconds3;
        public String timedEverySeconds4;
        public String timedEverySeconds5;
        public String timedEverySeconds6;
        public String timedIntervalHours1;
        public String timedIntervalHours2;
        public String timedIntervalHours3;
        public String timedIntervalHours4;
        public String timedIntervalHours5;
        public String timedIntervalHours6;
        public String timedIntervalDays1;
        public String timedIntervalDays2;
        public String timedIntervalDays3;
        public String timedIntervalDays4;
        public String timedIntervalDays5;
        public String timedIntervalDays6;
        public String timedIntervalSeconds1;
        public String timedIntervalSeconds2;
        public String timedIntervalSeconds3;
        public String timedIntervalSeconds4;
        public String timedIntervalSeconds5;
        public String timedIntervalSeconds6;
        public String timedArrivalHours1;
        public String timedArrivalHours2;
        public String timedArrivalHours3;
        public String timedArrivalHours4;
        public String timedArrivalHours5;
        public String timedArrivalHours6;
        public String timedArrivalMinutes1;
        public String timedArrivalMinutes2;
        public String timedArrivalMinutes3;
        public String timedArrivalMinutes4;
        public String timedArrivalMinutes5;
        public String timedArrivalMinutes6;
        public String timedArrivalSeconds1;
        public String timedArrivalSeconds2;
        public String timedArrivalSeconds3;
        public String timedArrivalSeconds4;
        public String timedArrivalSeconds5;
        public String timedArrivalSeconds6;
        public String timedRequirePlayer1;
        public String timedRequirePlayer2;
        public String timedRequirePlayer3;
        public String timedRequirePlayer4;
        public String timedRequirePlayer5;
        public String timedRequirePlayer6;
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

        public String getShopEditBoss(int row) {
            return switch (row) {
                case 1 -> shopEditBoss1;
                case 2 -> shopEditBoss2;
                case 3 -> shopEditBoss3;
                case 4 -> shopEditBoss4;
                case 5 -> shopEditBoss5;
                case 6 -> shopEditBoss6;
                case 7 -> shopEditBoss7;
                case 8 -> shopEditBoss8;
                default -> null;
            };
        }

        public String getShopEditArena(int row) {
            return switch (row) {
                case 1 -> shopEditArena1;
                case 2 -> shopEditArena2;
                case 3 -> shopEditArena3;
                case 4 -> shopEditArena4;
                case 5 -> shopEditArena5;
                case 6 -> shopEditArena6;
                case 7 -> shopEditArena7;
                case 8 -> shopEditArena8;
                default -> null;
            };
        }

        public String getShopEditSilentPrice(int row) {
            return switch (row) {
                case 1 -> shopEditSilentPrice1;
                case 2 -> shopEditSilentPrice2;
                case 3 -> shopEditSilentPrice3;
                case 4 -> shopEditSilentPrice4;
                case 5 -> shopEditSilentPrice5;
                case 6 -> shopEditSilentPrice6;
                case 7 -> shopEditSilentPrice7;
                case 8 -> shopEditSilentPrice8;
                default -> null;
            };
        }

        public String getShopEditPrice(int row) {
            return switch (row) {
                case 1 -> shopEditBossPrice1;
                case 2 -> shopEditBossPrice2;
                case 3 -> shopEditBossPrice3;
                case 4 -> shopEditBossPrice4;
                case 5 -> shopEditBossPrice5;
                case 6 -> shopEditBossPrice6;
                case 7 -> shopEditBossPrice7;
                case 8 -> shopEditBossPrice8;
                default -> null;
            };
        }

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


        public String getTimedMinPlayers(int row) {
            return switch (row) {
                case 1 -> timedMinPlayers1;
                case 2 -> timedMinPlayers2;
                case 3 -> timedMinPlayers3;
                case 4 -> timedMinPlayers4;
                case 5 -> timedMinPlayers5;
                case 6 -> timedMinPlayers6;
                default -> null;
            };
        }

        public String getTimedEverySeconds(int row) {
            return switch (row) {
                case 1 -> timedEverySeconds1;
                case 2 -> timedEverySeconds2;
                case 3 -> timedEverySeconds3;
                case 4 -> timedEverySeconds4;
                case 5 -> timedEverySeconds5;
                case 6 -> timedEverySeconds6;
                default -> null;
            };
        }

        public String getTimedIntervalHours(int row) {
            return switch (row) {
                case 1 -> timedIntervalHours1;
                case 2 -> timedIntervalHours2;
                case 3 -> timedIntervalHours3;
                case 4 -> timedIntervalHours4;
                case 5 -> timedIntervalHours5;
                case 6 -> timedIntervalHours6;
                default -> null;
            };
        }

        public String getTimedIntervalDays(int row) {
            return switch (row) {
                case 1 -> timedIntervalDays1;
                case 2 -> timedIntervalDays2;
                case 3 -> timedIntervalDays3;
                case 4 -> timedIntervalDays4;
                case 5 -> timedIntervalDays5;
                case 6 -> timedIntervalDays6;
                default -> null;
            };
        }

        public String getTimedIntervalSeconds(int row) {
            return switch (row) {
                case 1 -> timedIntervalSeconds1;
                case 2 -> timedIntervalSeconds2;
                case 3 -> timedIntervalSeconds3;
                case 4 -> timedIntervalSeconds4;
                case 5 -> timedIntervalSeconds5;
                case 6 -> timedIntervalSeconds6;
                default -> null;
            };
        }

        public String getTimedArrivalHours(int row) {
            return switch (row) {
                case 1 -> timedArrivalHours1;
                case 2 -> timedArrivalHours2;
                case 3 -> timedArrivalHours3;
                case 4 -> timedArrivalHours4;
                case 5 -> timedArrivalHours5;
                case 6 -> timedArrivalHours6;
                default -> null;
            };
        }

        public String getTimedArrivalMinutes(int row) {
            return switch (row) {
                case 1 -> timedArrivalMinutes1;
                case 2 -> timedArrivalMinutes2;
                case 3 -> timedArrivalMinutes3;
                case 4 -> timedArrivalMinutes4;
                case 5 -> timedArrivalMinutes5;
                case 6 -> timedArrivalMinutes6;
                default -> null;
            };
        }

        public String getTimedArrivalSeconds(int row) {
            return switch (row) {
                case 1 -> timedArrivalSeconds1;
                case 2 -> timedArrivalSeconds2;
                case 3 -> timedArrivalSeconds3;
                case 4 -> timedArrivalSeconds4;
                case 5 -> timedArrivalSeconds5;
                case 6 -> timedArrivalSeconds6;
                default -> null;
            };
        }

        public String getTimedRequirePlayer(int row) {
            return switch (row) {
                case 1 -> timedRequirePlayer1;
                case 2 -> timedRequirePlayer2;
                case 3 -> timedRequirePlayer3;
                case 4 -> timedRequirePlayer4;
                case 5 -> timedRequirePlayer5;
                case 6 -> timedRequirePlayer6;
                default -> null;
            };
        }

        public String getTimedGraceEnabled(int row) {
            return switch (row) {
                case 1 -> timedGraceEnabled1;
                case 2 -> timedGraceEnabled2;
                case 3 -> timedGraceEnabled3;
                case 4 -> timedGraceEnabled4;
                case 5 -> timedGraceEnabled5;
                case 6 -> timedGraceEnabled6;
                default -> null;
            };
        }

        public String getTimedGraceSeconds(int row) {
            return switch (row) {
                case 1 -> timedGraceSeconds1;
                case 2 -> timedGraceSeconds2;
                case 3 -> timedGraceSeconds3;
                case 4 -> timedGraceSeconds4;
                case 5 -> timedGraceSeconds5;
                case 6 -> timedGraceSeconds6;
                default -> null;
            };
        }

        public String getTimedAnnounceGlobal(int row) {
            return switch (row) {
                case 1 -> timedAnnounceGlobal1;
                case 2 -> timedAnnounceGlobal2;
                case 3 -> timedAnnounceGlobal3;
                case 4 -> timedAnnounceGlobal4;
                case 5 -> timedAnnounceGlobal5;
                case 6 -> timedAnnounceGlobal6;
                default -> null;
            };
        }

        public String getTimedAnnounceWorld(int row) {
            return switch (row) {
                case 1 -> timedAnnounceWorld1;
                case 2 -> timedAnnounceWorld2;
                case 3 -> timedAnnounceWorld3;
                case 4 -> timedAnnounceWorld4;
                case 5 -> timedAnnounceWorld5;
                case 6 -> timedAnnounceWorld6;
                default -> null;
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

        public String getArenaRadius(int row) {
            return switch (row) {
                case 1 -> arenaRadius1;
                case 2 -> arenaRadius2;
                case 3 -> arenaRadius3;
                case 4 -> arenaRadius4;
                case 5 -> arenaRadius5;
                case 6 -> arenaRadius6;
                case 7 -> arenaRadius7;
                case 8 -> arenaRadius8;
                default -> "";
            };
        }
    }
}
