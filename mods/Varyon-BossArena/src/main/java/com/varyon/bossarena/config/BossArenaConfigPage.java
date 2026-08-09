package com.varyon.bossarena.config;

/**
 * Config UI for BossArena: bosses, shop locations, and arenas tabs.
 * Consider splitting into {@code BossesTabHandler}, {@code ShopTabHandler}, and {@code ArenasTabHandler}
 * (each owning build/handle logic and tab-specific state) to reduce this class size.
 */
import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.compat.ZoneTier;
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

    /** Arena rows declared in the layout. The list scrolls, so this is only an upper bound. */
    private static final int MAX_ARENA_ROWS = 32;
    private static final int MAX_SHOP_ROWS = 8;
    private static final int MAX_SHOP_CONTRACT_ROWS = 8;
    /** Boss rows declared in the layout. The list scrolls, so this is only an upper bound. */
    private static final int MAX_BOSS_ROWS = 32;
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
    /** Timed rules declared in the layout. The list scrolls, so this is only an upper bound. */
    private static final int MAX_TIMED_SPAWN_ROWS = 32;
    /** Pool rows declared in the layout. The list scrolls, so this is only an upper bound. */
    private static final int MAX_BOSS_POOL_ROWS = 32;
    /** True while the Planification "Textes" modal is open. */
    private boolean timedTextsModalOpen = false;
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

    private static final long CONFIG_AUTOSAVE_DEBOUNCE_MS = 1500L;
    private static final java.util.concurrent.ScheduledExecutorService CONFIG_AUTOSAVE_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BossArena-ConfigAutoSave");
                t.setDaemon(true);
                return t;
            });
    /** Pending debounced auto-save for the currently open boss editor draft, if any. */
    private java.util.concurrent.ScheduledFuture<?> pendingBossEditorAutoSave;
    /** Pending debounced auto-save for the Planification tab's raw text/number fields, if any. */
    private java.util.concurrent.ScheduledFuture<?> pendingTimedAutoSave;
    /** Pending debounced auto-save for the Marchands (shop) editor, if any. */
    private java.util.concurrent.ScheduledFuture<?> pendingShopEditorAutoSave;
    /** Pending debounced auto-saves for Arènes rows, keyed by row index. */
    private final Map<Integer, java.util.concurrent.ScheduledFuture<?>> pendingArenaAutoSaveByRow = new java.util.concurrent.ConcurrentHashMap<>();

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
        copy.mobsPerWaveMin = source.add.mobsPerWaveMin;
        copy.mobsPerWaveMax = source.add.mobsPerWaveMax;
        copy.mobsPerWave = source.add.mobsPerWaveMin;
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
        add.mobsPerWaveMin = 1;
        add.mobsPerWaveMax = 1;
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
                int min = Math.max(1, add.mobsPerWaveMin);
                int max = Math.max(min, add.mobsPerWaveMax);
                addCopy.mobsPerWaveMin = min;
                addCopy.mobsPerWaveMax = max;
                addCopy.mobsPerWave = min;
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
            int min = Math.max(1, row.add.mobsPerWaveMin);
            int max = Math.max(min, row.add.mobsPerWaveMax);
            addCopy.mobsPerWaveMin = min;
            addCopy.mobsPerWaveMax = max;
            addCopy.mobsPerWave = min;
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

        if (data.bossWaveMobMult != null && Float.isFinite(data.bossWaveMobMult)) {
            extra.mobsPerPlayerMult = BossArenaConfigUiControls.clampFloat(data.bossWaveMobMult, 1.0f, 3.0f);
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

    private static String formatFloat1(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
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
        arenas.sort((a, b) -> compareNatural(
                a == null ? "" : a.arenaId,
                b == null ? "" : b.arenaId));
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

    /** Minimum Varyon zone tier (0-{@link ZoneTier#MAX_TIER}) required to see a timed rule's
     * announcements. Tiers are cumulative (a player who unlocked tier 4 sees rules requiring
     * tier 1-4), so 0 means everyone. */
    private static List<DropdownEntryInfo> tierDropdownEntries() {
        List<DropdownEntryInfo> entries = new ArrayList<>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("Tous"), "0"));
        for (int tier = 1; tier <= ZoneTier.MAX_TIER; tier++) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString("Tier " + tier), Integer.toString(tier)));
        }
        return entries;
    }

    @Nonnull
    private static List<DropdownEntryInfo> bossDropdownEntries() {
        List<BossDefinition> bosses = new ArrayList<>(BossRegistry.getAll().values());
        bosses.sort((a, b) -> compareNatural(
                a == null ? "" : a.bossName,
                b == null ? "" : b.bossName));
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
        arenas.sort((a, b) -> compareNatural(
                a == null ? "" : a.arenaId,
                b == null ? "" : b.arenaId));
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
        names.sort(BossArenaConfigPage::compareNatural);
        return names;
    }

    /**
     * Alphabetical, but digit runs compare as numbers so "tier2" sorts before "tier10".
     * Case-insensitive, with a case-sensitive tiebreak for stability.
     */
    static int compareNatural(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int startA = i;
                int startB = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                String numA = a.substring(startA, i).replaceFirst("^0+(?=.)", "");
                String numB = b.substring(startB, j).replaceFirst("^0+(?=.)", "");
                if (numA.length() != numB.length()) {
                    return numA.length() - numB.length();
                }
                int cmp = numA.compareTo(numB);
                if (cmp != 0) {
                    return cmp;
                }
                continue;
            }
            int cmp = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
            if (cmp != 0) {
                return cmp;
            }
            i++;
            j++;
        }
        int remaining = (a.length() - i) - (b.length() - j);
        return remaining != 0 ? remaining : a.compareTo(b);
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
            out.extraMobs.mobsPerPlayerMult = source.extraMobs.mobsPerPlayerMult;
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
                    copy.mobsPerWaveMin = add.mobsPerWaveMin;
                    copy.mobsPerWaveMax = add.mobsPerWaveMax;
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
                            addCopy.mobsPerWaveMin = add.mobsPerWaveMin;
                            addCopy.mobsPerWaveMax = add.mobsPerWaveMax;
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
        int min = Math.max(1, add.mobsPerWaveMin);
        int max = Math.max(min, add.mobsPerWaveMax);
        String summary = safeText(add.npcId)
                + " x" + (min == max ? Integer.toString(min) : min + "-" + max);
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
        try {
            dispatchDataEvent(ref, store, data);
        } catch (Exception ex) {
            // Never let an unexpected exception leave the client stuck on "Loading..." forever:
            // any handler below can throw something other than IllegalArgumentException (the only
            // type most of them catch locally), which would otherwise silently swallow the response.
            plugin.getLogger().atSevere().withCause(ex).log(
                    "Unhandled exception handling BossArena config UI action '" + data.action + "'");
            bossStatusText = "Une erreur inattendue est survenue. Réessayez.";
            try {
                rebuild();
            } catch (Exception rebuildEx) {
                plugin.getLogger().atSevere().withCause(rebuildEx).log(
                        "Failed to rebuild BossArena config UI after handling an earlier exception");
            }
        }
    }

    private void dispatchDataEvent(@Nonnull Ref<EntityStore> ref,
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
            if ("timed_autosave".equals(action)) {
                scheduleTimedAutoSave(data);
            } else if ("timed_texts_open".equals(action)) {
                timedTextsModalOpen = true;
                rebuild();
            } else if ("timed_texts_close".equals(action)) {
                timedTextsModalOpen = false;
                rebuild();
            } else if ("timed_add".equals(action)) {
                handleTimedAdd();
            } else if (action.startsWith("timed_delete_")) {
                handleTimedDelete(action.substring("timed_delete_".length()));
            } else if (action.startsWith("timed_move_up_")) {
                handleTimedMove(action.substring("timed_move_up_".length()), data, -1);
            } else if (action.startsWith("timed_move_down_")) {
                handleTimedMove(action.substring("timed_move_down_".length()), data, 1);
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
            } else if (action.startsWith("timed_pop_boss_only_")) {
                handleTimedPopBossOnly(action.substring("timed_pop_boss_only_".length()));
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

        if ("shop_edit_autosave".equals(action)) {
            scheduleShopEditorAutoSave(data);
            return;
        }

        if ("shop_edit_close".equals(action)) {
            flushPendingShopEditorAutoSave(data);
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
        scheduleShopEditorAutoSave(data);
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

    /**
     * Debounces a background persist of the Marchand editor's current draft (vendor name, currency
     * item, contracts) so admins don't have to click "Enregistrer". Silent — validation errors
     * (e.g. an unknown/partial currency item while typing) are swallowed and retried on the next edit.
     */
    private void scheduleShopEditorAutoSave(ConfigEventData data) {
        if (data == null || shopLocationEditorState == null) {
            return;
        }
        if (pendingShopEditorAutoSave != null) {
            pendingShopEditorAutoSave.cancel(false);
        }
        pendingShopEditorAutoSave = CONFIG_AUTOSAVE_EXECUTOR.schedule(
                () -> autoSaveShopEditor(data),
                CONFIG_AUTOSAVE_DEBOUNCE_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    private void cancelPendingShopEditorAutoSave() {
        if (pendingShopEditorAutoSave != null) {
            pendingShopEditorAutoSave.cancel(false);
            pendingShopEditorAutoSave = null;
        }
    }

    private void flushPendingShopEditorAutoSave(ConfigEventData data) {
        if (pendingShopEditorAutoSave == null) {
            return;
        }
        pendingShopEditorAutoSave.cancel(false);
        pendingShopEditorAutoSave = null;
        autoSaveShopEditor(data);
    }

    private void autoSaveShopEditor(ConfigEventData data) {
        try {
            persistShopEditorDraft(data);
        } catch (Exception ex) {
            plugin.getLogger().atFine().withCause(ex).log("Marchand auto-save skipped (form likely incomplete)");
        }
    }

    /**
     * Validates and writes the shop editor's current draft to {@link BossShopConfig} + disk.
     * Does NOT touch {@code shopLocationEditorState}/{@code rebuild()} — callers decide whether to
     * close the editor (explicit save) or leave it open (auto-save).
     *
     * @return a human-readable summary suitable for {@code shopStatusText}
     * @throws IllegalArgumentException if the draft is currently invalid (e.g. blank/unknown currency item,
     *                                  incomplete or duplicate contract) — callers should treat this as
     *                                  "not ready to save yet" rather than a hard failure.
     */
    private String persistShopEditorDraft(ConfigEventData data) {
        if (shopLocationEditorState == null) {
            throw new IllegalArgumentException("Aucun marchand en cours d'édition.");
        }

        BossShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null) {
            throw new IllegalArgumentException("Config marchands indisponible.");
        }

        ShopLocationRef shopLocation = shopLocationEditorState.shopLocation;
        BossShopConfig.ShopLocation location = shopConfig.getShopLocation(shopLocation.worldName, shopLocation.x, shopLocation.y, shopLocation.z);
        if (location == null) {
            throw new IllegalArgumentException("Le marchand sélectionné n'existe plus dans la config.");
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
            throw new IllegalArgumentException("Indiquez un item monnaie (ex: Ingredient_Bar_Iron).");
        }
        if (!BossArenaConfigUiControls.isExactItemId(currencyItemId)) {
            throw new IllegalArgumentException("Item monnaie inconnu: " + currencyItemId);
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
                throw new IllegalArgumentException("Chaque contrat doit préciser un boss et une arène.");
            }
            String pairKey = bossId.toLowerCase(Locale.ROOT) + "\0" + arenaId.toLowerCase(Locale.ROOT);
            if (!seenPairs.add(pairKey)) {
                throw new IllegalArgumentException("Contrat en double (même boss et même arène).");
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
        return "Marchand enregistré (" + shopLocation.x + ", " + shopLocation.y + ", " + shopLocation.z
                + ") avec " + saved.size() + " contrat(s). Monnaie: " + currencyItemId + ".";
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
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#ShopEditorVendorName",
                buildShopEditSnapshotEvent("shop_edit_autosave"),
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
            for (String fieldId : new String[]{
                    "#ShopEditBoss" + suffix, "#ShopEditArena" + suffix,
                    "#ShopEditBossPrice" + suffix, "#ShopEditSilentPrice" + suffix
            }) {
                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        fieldId,
                        buildShopEditSnapshotEvent("shop_edit_autosave"),
                        false
                );
            }
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ShopEditorAddContract",
                buildShopEditSnapshotEvent("shop_edit_add")
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

        if (action.startsWith("arena_autosave_")) {
            scheduleArenaAutoSave(action.substring("arena_autosave_".length()), data);
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
            for (String fieldId : new String[]{
                    "#ArenaName" + suffix, "#ArenaWorld" + suffix,
                    "#ArenaX" + suffix, "#ArenaY" + suffix, "#ArenaZ" + suffix, "#ArenaRadius" + suffix
            }) {
                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        fieldId,
                        buildArenaRowSnapshotEvent("arena_autosave_" + row, row),
                        false
                );
            }
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
                .append("@ArenaRadius", "#ArenaRadius" + suffix + ".Value");
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
        arena.proximityRadius = 30.0d;
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

    /**
     * Debounces a background persist of one arena row so admins don't have to click a Save button.
     * Silent — validation errors (blank name while clearing the field, unknown world while typing,
     * name collision) are swallowed and simply retried on the next edit.
     */
    private void scheduleArenaAutoSave(String rowToken, ConfigEventData data) {
        int row = parseRow(rowToken);
        if (row < 1 || data == null) {
            return;
        }
        java.util.concurrent.ScheduledFuture<?> previous = pendingArenaAutoSaveByRow.remove(row);
        if (previous != null) {
            previous.cancel(false);
        }
        java.util.concurrent.ScheduledFuture<?> future = CONFIG_AUTOSAVE_EXECUTOR.schedule(
                () -> autoSaveArenaRow(row, data),
                CONFIG_AUTOSAVE_DEBOUNCE_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
        pendingArenaAutoSaveByRow.put(row, future);
    }

    private void autoSaveArenaRow(int row, ConfigEventData data) {
        try {
            persistArenaRow(row, data);
        } catch (Exception ex) {
            plugin.getLogger().atFine().withCause(ex).log("Arena auto-save skipped for row " + row + " (form likely incomplete)");
        }
    }

    /**
     * Validates and writes one arena row's current form fields to {@link ArenaRegistry} + disk.
     *
     * @throws IllegalArgumentException if the row/form is currently invalid — callers should treat
     *                                   this as "not ready to save yet" rather than a hard failure.
     */
    private void persistArenaRow(int row, ConfigEventData data) {
        if (row < 1 || row > arenaRows.size()) {
            throw new IllegalArgumentException("Sélection de ligne arène invalide.");
        }

        Arena arena = ArenaRegistry.get(arenaRows.get(row - 1));
        if (arena == null) {
            throw new IllegalArgumentException("L'arène sélectionnée n'existe plus.");
        }

        String requestedId = normalizeArenaId(data.arenaName);
        if (requestedId.isEmpty()) {
            throw new IllegalArgumentException("Le nom d'arène ne peut pas être vide.");
        }

        if (!ARENA_ID_PATTERN.matcher(requestedId).matches()) {
            throw new IllegalArgumentException("Le nom d'arène ne peut utiliser que lettres, chiffres, '_' ou '-'.");
        }

        Double x = parseCoordinate(data.arenaX);
        Double y = parseCoordinate(data.arenaY);
        Double z = parseCoordinate(data.arenaZ);
        if (x == null || y == null || z == null) {
            throw new IllegalArgumentException("Les coordonnées doivent être des nombres valides.");
        }

        String worldName = optionalText(data.arenaWorld);
        if (looksLikeUiBindingExpression(worldName)) {
            worldName = "";
        }
        if (worldName.isEmpty()) {
            worldName = optionalText(arena.worldName);
        }
        if (worldName.isEmpty()) {
            throw new IllegalArgumentException("Le monde de l'arène ne peut pas être vide.");
        }
        if (Universe.get().getWorld(worldName) == null) {
            throw new IllegalArgumentException("Monde introuvable : '" + worldName + "'.");
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
            throw new IllegalArgumentException("L'arène '" + requestedId + "' existe déjà.");
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
            flushPendingBossEditorAutoSave();
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

        if ("boss_wave_field_changed".equals(action)) {
            handleBossWaveFieldChanged(data);
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

        if (action.startsWith("boss_pp_cycle_")) {
            handleBossPpCycle(action.substring("boss_pp_cycle_".length()), data);
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
        // The list now scrolls natively, so the old offset-based pager is disabled.
        bossListOffset = 0;
        cmd.set("#BossScrollUp.Visible", false);
        cmd.set("#BossScrollDown.Visible", false);
        cmd.set("#BossScrollTrack.Visible", false);
        cmd.set("#BossScrollPageLabel.Visible", false);
        for (int step = 1; step <= BOSS_SCROLL_THUMB_STEPS; step++) {
            cmd.set("#BossScrollThumb" + step + ".Visible", false);
        }
        boolean bossOverflow = totalBosses > MAX_BOSS_ROWS;
        cmd.set("#BossOverflowLabel.Visible", bossOverflow);
        cmd.set("#BossOverflowLabel.Text", bossOverflow
                ? ("+" + (totalBosses - MAX_BOSS_ROWS) + " boss non affichés sur cette page.")
                : "");

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
        float ppRegen = BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.regen, MULT_PERS_MIN, MULT_PERS_MAX);
        boss.perPlayerIncrease.regen = ppRegen;

        cmd.set("#BossEditHp.Value", hpMult);
        cmd.set("#BossEditDamage.Value", dmgMult);
        cmd.set("#BossEditSize.Value", sizeMult);
        cmd.set("#BossEditSpeed.Value", speed);
        cmd.set("#BossEditAttackRate.Value", attackRate);
        cmd.set("#BossEditKnockbackGiven.Value", kbGiven);
        cmd.set("#BossEditKnockbackTaken.Value", kbTaken);
        cmd.set("#BossEditRegen.Value", regen);
        cmd.set("#BossEditHpValue.Text", formatFloat(hpMult));
        cmd.set("#BossEditDamageValue.Text", formatFloat(dmgMult));
        cmd.set("#BossEditSizeValue.Text", formatFloat(sizeMult));
        cmd.set("#BossEditSpeedValue.Text", formatFloat(speed));
        cmd.set("#BossEditAttackRateValue.Text", formatFloat(attackRate));
        cmd.set("#BossEditKnockbackGivenValue.Text", formatFloat(kbGiven));
        cmd.set("#BossEditKnockbackTakenValue.Text", formatFloat(kbTaken));
        cmd.set("#BossEditRegenValue.Text", BossRegen.formatLabel(regen));
        cmd.set("#BossEditPpHp.Text", formatFloat(ppHp));
        cmd.set("#BossEditPpDamage.Text", formatFloat(ppDmg));
        cmd.set("#BossEditPpSize.Text", formatFloat(ppSize));
        cmd.set("#BossEditPpSpeed.Text", formatFloat(ppSpeed));
        cmd.set("#BossEditPpAttackRate.Text", formatFloat(ppAttackRate));
        cmd.set("#BossEditPpKnockbackGiven.Text", formatFloat(ppKbGiven));
        cmd.set("#BossEditPpKnockbackTaken.Text", formatFloat(ppKbTaken));
        cmd.set("#BossEditPpRegen.Text", formatFloat(ppRegen));

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
        float mobMult = BossArenaConfigUiControls.clampFloat(editorExtra.mobsPerPlayerMult, 1.0f, 3.0f);
        cmd.set("#BossWaveMobMult.Value", mobMult);
        cmd.set("#BossWaveMobMultValue.Text", formatFloat(mobMult));

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
                CustomUIEventBindingType.Activating,
                "#BossEditPpHp",
                buildBossEditorSnapshotEvent("boss_pp_cycle_hp"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpDamage",
                buildBossEditorSnapshotEvent("boss_pp_cycle_damage"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpSize",
                buildBossEditorSnapshotEvent("boss_pp_cycle_size"),
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
                "#BossWaveMobMult",
                buildBossEditorSnapshotEvent("boss_slider_changed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpSpeed",
                buildBossEditorSnapshotEvent("boss_pp_cycle_speed"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpAttackRate",
                buildBossEditorSnapshotEvent("boss_pp_cycle_attack_rate"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpKnockbackGiven",
                buildBossEditorSnapshotEvent("boss_pp_cycle_knockback_given"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpKnockbackTaken",
                buildBossEditorSnapshotEvent("boss_pp_cycle_knockback_taken"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BossEditPpRegen",
                buildBossEditorSnapshotEvent("boss_pp_cycle_regen"),
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
                cmd.set("#BossWaveAmountMin" + suffix + ".Value", Integer.toString(Math.max(1, add != null ? add.mobsPerWaveMin : 1)));
                cmd.set("#BossWaveAmountMax" + suffix + ".Value", Integer.toString(Math.max(1, add != null ? Math.max(add.mobsPerWaveMin, add.mobsPerWaveMax) : 1)));
                float hp = add != null && add.hp > 0f ? add.hp : 1.0f;
                float damage = add != null && add.damage > 0f ? add.damage : 1.0f;
                float size = add != null && add.size > 0f ? add.size : 1.0f;
                hp = BossArenaConfigUiControls.clampFloat(hp, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
                damage = BossArenaConfigUiControls.clampFloat(damage, MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
                size = BossArenaConfigUiControls.clampFloat(size, MULT_SIZE_MIN, MULT_SIZE_MAX);
                cmd.set("#BossWaveHp" + suffix + ".Value", formatFloat1(hp));
                cmd.set("#BossWaveDamage" + suffix + ".Value", formatFloat1(damage));
                cmd.set("#BossWaveSize" + suffix + ".Value", formatFloat1(size));

                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#BossWaveNpc" + suffix,
                        buildBossWavesSnapshotEvent("boss_wave_npc_filter_" + row),
                        false
                );
                for (String fieldId : new String[]{
                        "#BossWaveAmountMin" + suffix, "#BossWaveAmountMax" + suffix,
                        "#BossWaveHp" + suffix, "#BossWaveDamage" + suffix, "#BossWaveSize" + suffix
                }) {
                    events.addEventBinding(
                            CustomUIEventBindingType.ValueChanged,
                            fieldId,
                            buildBossWavesSnapshotEvent("boss_wave_field_changed"),
                            false
                    );
                }

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
            for (String fieldId : new String[]{"#BossWaveTriggerValueField", "#BossWaveRepeatCountField"}) {
                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        fieldId,
                        buildBossWavesSnapshotEvent("boss_wave_field_changed"),
                        false
                );
            }
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
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedTextsButton",
                buildBossTimedSnapshotEvent("timed_texts_open"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedTextsClose",
                buildBossTimedSnapshotEvent("timed_texts_close"));
        cmd.set("#TimedTextsOverlay.Visible", timedTextsModalOpen);

        List<DropdownEntryInfo> arenaEntries = arenaDropdownEntries();
        List<DropdownEntryInfo> tierEntries = tierDropdownEntries();

        for (int row = 1; row <= MAX_TIMED_SPAWN_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= rows.size();
            cmd.set("#BossTimedRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            // Reorder arrows: hidden at the ends, where there is nothing to swap with.
            cmd.set("#TimedMoveUp" + suffix + ".Visible", row > 1);
            cmd.set("#TimedMoveDown" + suffix + ".Visible", row < rows.size());

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
            // Despawn lifetime is split across a minutes and a seconds field.
            long despawnTotalSeconds = entry != null
                    ? BossArenaConfig.resolveSeconds(
                            entry.despawnAfterHours, entry.despawnAfterMinutes, entry.despawnAfterSeconds)
                    : 300L;
            if (despawnTotalSeconds <= 0L && interval) {
                despawnTotalSeconds = 300L;
            }
            boolean despawnVisible = !manual;
            cmd.set("#TimedDespawnMinutes" + suffix + ".Value", Long.toString(Math.max(0L, despawnTotalSeconds / 60L)));
            cmd.set("#TimedDespawnSeconds" + suffix + ".Value", Long.toString(Math.max(0L, despawnTotalSeconds % 60L)));
            cmd.set("#TimedDespawnMinutes" + suffix + ".Visible", despawnVisible);
            cmd.set("#TimedDespawnSeconds" + suffix + ".Visible", despawnVisible);
            cmd.set("#TimedDespawnLabel" + suffix + ".Visible", despawnVisible);
            cmd.set("#TimedDespawnMinutesUnit" + suffix + ".Visible", despawnVisible);
            cmd.set("#TimedDespawnSecondsUnit" + suffix + ".Visible", despawnVisible);

            cmd.set("#TimedAnnounceGlobal" + suffix + ".Value", announceGlobal ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedAnnounceGlobalToggle" + suffix, announceGlobal);
            cmd.set("#TimedAnnounceWorld" + suffix + ".Value", announceWorld ? "true" : "false");
            BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedAnnounceWorldToggle" + suffix, announceWorld);
            cmd.set("#TimedAnnounceMinTier" + suffix + ".Entries", tierEntries);
            cmd.set("#TimedAnnounceMinTier" + suffix + ".Value",
                    Integer.toString(entry != null ? Math.max(0, entry.announceMinTier) : 0));

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
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedPopBossOnly" + suffix,
                    EventData.of("Action", "timed_pop_boss_only_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedDelete" + suffix,
                    EventData.of("Action", "timed_delete_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedMoveUp" + suffix,
                    buildBossTimedSnapshotEvent("timed_move_up_" + row));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TimedMoveDown" + suffix,
                    buildBossTimedSnapshotEvent("timed_move_down_" + row));

            for (String fieldId : new String[]{
                    "#TimedBossId" + suffix, "#TimedArenaId" + suffix, "#TimedMinPlayers" + suffix,
                    "#TimedAnnounceMinTier" + suffix,
                    "#TimedGraceSeconds" + suffix,
                    "#TimedEveryHours" + suffix, "#TimedEveryMinutes" + suffix, "#TimedEverySeconds" + suffix,
                    "#TimedIntervalHours" + suffix, "#TimedIntervalDays" + suffix, "#TimedIntervalSeconds" + suffix,
                    "#TimedArrivalHours" + suffix, "#TimedArrivalMinutes" + suffix, "#TimedArrivalSeconds" + suffix,
                    "#TimedDespawnMinutes" + suffix, "#TimedDespawnSeconds" + suffix
            }) {
                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        fieldId,
                        buildBossTimedSnapshotEvent("timed_autosave"),
                        false
                );
            }
        }

        for (String fieldId : new String[]{"#TimedAnnounceText", "#TimedReminderText", "#TimedGraceText"}) {
            events.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    fieldId,
                    buildBossTimedSnapshotEvent("timed_autosave"),
                    false
            );
        }

        cmd.set("#TimedAnnounceText.Value", announceText);
        cmd.set("#TimedReminderText.Value", reminderText);
        cmd.set("#TimedGraceText.Value", graceText);
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
                    .append("@BossWaveDamage" + suffix, "#TimedDespawnSeconds" + suffix + ".Value")
                    .append("@BossWaveSize" + suffix, "#TimedDespawnMinutes" + suffix + ".Value")
                    .append("@TimedAnnounceGlobal" + suffix, "#TimedAnnounceGlobal" + suffix + ".Value")
                    .append("@TimedAnnounceWorld" + suffix, "#TimedAnnounceWorld" + suffix + ".Value")
                    .append("@TimedAnnounceMinTier" + suffix, "#TimedAnnounceMinTier" + suffix + ".Value")
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
        entry.despawnAfterSeconds = 0L;
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

    /**
     * Moves a planification rule one slot up ({@code direction < 0}) or down, and persists the new
     * order. Pending edits are flushed first so reordering never discards what is on screen.
     */
    private void handleTimedMove(String rowToken, ConfigEventData data, int direction) {
        int row;
        try {
            row = Integer.parseInt(rowToken);
        } catch (NumberFormatException ex) {
            return;
        }
        if (data != null) {
            flushPendingTimedAutoSave(data);
        }
        BossArenaConfig cfg = plugin.getConfig();
        if (cfg == null) {
            return;
        }
        List<BossArenaConfig.TimedBossSpawn> rows = cfg.getTimedBossSpawns();
        int index = row - 1;
        int target = index + direction;
        if (index < 0 || index >= rows.size() || target < 0 || target >= rows.size()) {
            return;
        }
        BossArenaConfig.TimedBossSpawn moved = rows.get(index);
        rows.set(index, rows.get(target));
        rows.set(target, moved);
        cfg.timedBossSpawns = rows;
        cfg.save();
        plugin.refreshTimedBossSpawns();
        bossStatusText = "Ordre des règles mis à jour.";
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

    /** "Boss direct": force-spawns the boss for this row immediately, skipping pre-boss waves. */
    private void handleTimedPopBossOnly(String rowToken) {
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
        bossStatusText = scheduler.forceSpawnByRow(row, true);
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
        refreshPoolRow(row, bossId);
    }

    /**
     * Updates just the toggled row instead of rebuilding the page: a full rebuild resets the
     * scroll position, throwing the user back to the top of the list on every click.
     */
    private void refreshPoolRow(int row, String bossId) {
        String suffix = Integer.toString(row);
        Integer weight = bossPoolEditorState.findSelectedWeight(bossId);
        boolean selected = weight != null;
        UICommandBuilder cmd = new UICommandBuilder();
        BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#BossPoolToggle" + suffix, selected);
        cmd.set("#BossPoolWeight" + suffix + ".Text", Integer.toString(selected ? weight : 1));
        cmd.set("#BossPoolPageLabel.Text", "selection: "
                + bossPoolEditorState.selectedWeights.size()
                + " / " + bossPoolEditorState.bossIds.size());
        sendUpdate(cmd, false);
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
        refreshPoolRow(row, bossId);
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
        // The list scrolls natively now, so the Prev/Next pager is hidden.
        bossPoolEditorState.pageOffset = 0;
        cmd.set("#BossPoolPrevButton.Visible", false);
        cmd.set("#BossPoolNextButton.Visible", false);
        cmd.set("#BossPoolPageLabel.Text", "selection: "
                + bossPoolEditorState.selectedWeights.size() + " / " + total);

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
            // "mode" changes which fields are visible on the row, so it needs a full rebuild.
            // The plain on/off toggles only restyle one button — refresh those in place, since a
            // rebuild would reset the scroll position and jump the user back to the top.
            if ("mode".equals(field)) {
                rebuild();
            } else {
                refreshTimedToggle(row, entry, field);
            }
        } catch (IllegalArgumentException ex) {
            bossStatusText = ex.getMessage();
            rebuild();
        }
    }


    /** Restyles a single Planification toggle without rebuilding (and re-scrolling) the page. */
    private void refreshTimedToggle(int row, BossArenaConfig.TimedBossSpawn entry, String field) {
        String suffix = Integer.toString(row);
        UICommandBuilder cmd = new UICommandBuilder();
        switch (field) {
            case "enabled" -> {
                cmd.set("#TimedEnabled" + suffix + ".Value", entry.enabled ? "true" : "false");
                BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedEnabledToggle" + suffix, entry.enabled);
            }
            case "grace" -> {
                cmd.set("#TimedGraceEnabled" + suffix + ".Value", entry.gracePeriodEnabled ? "true" : "false");
                BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedGraceToggle" + suffix, entry.gracePeriodEnabled);
            }
            case "announce_global", "announce_world" -> {
                cmd.set("#TimedAnnounceGlobal" + suffix + ".Value", entry.announceWorldWide ? "true" : "false");
                BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedAnnounceGlobalToggle" + suffix, entry.announceWorldWide);
                cmd.set("#TimedAnnounceWorld" + suffix + ".Value", entry.announceCurrentWorld ? "true" : "false");
                BossArenaConfigUiControls.styleOnOffTextButton(cmd, "#TimedAnnounceWorldToggle" + suffix, entry.announceCurrentWorld);
            }
            default -> {
                rebuild();
                return;
            }
        }
        cmd.set("#BossStatusLabel.Text", bossStatusText == null ? "" : bossStatusText);
        sendUpdate(cmd, false);
    }

    /**
     * Debounces a background persist of the Planification tab's raw form fields (bossId, arenaId,
     * hours/minutes/seconds, min players, announce/reminder/grace text) so admins don't have to
     * click "Enregistrer" to keep typed edits. Toggles already persist immediately elsewhere;
     * this only covers the free-text/number fields. Silent — errors are swallowed since the form
     * may be transiently incomplete while typing.
     */
    private void scheduleTimedAutoSave(ConfigEventData data) {
        if (data == null) {
            return;
        }
        if (pendingTimedAutoSave != null) {
            pendingTimedAutoSave.cancel(false);
        }
        pendingTimedAutoSave = CONFIG_AUTOSAVE_EXECUTOR.schedule(
                () -> autoSaveTimedRows(data),
                CONFIG_AUTOSAVE_DEBOUNCE_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    private void cancelPendingTimedAutoSave() {
        if (pendingTimedAutoSave != null) {
            pendingTimedAutoSave.cancel(false);
            pendingTimedAutoSave = null;
        }
    }

    private void flushPendingTimedAutoSave(ConfigEventData data) {
        if (pendingTimedAutoSave == null) {
            return;
        }
        pendingTimedAutoSave.cancel(false);
        pendingTimedAutoSave = null;
        autoSaveTimedRows(data);
    }

    private void autoSaveTimedRows(ConfigEventData data) {
        try {
            List<BossArenaConfig.TimedBossSpawn> parsed = parseTimedRowsFromEvent(data, false);
            persistTimedRows(parsed, data, false);
        } catch (Exception ex) {
            plugin.getLogger().atFine().withCause(ex).log("Planification auto-save skipped (form likely incomplete)");
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
            String despawnSecondsText = optionalText(data.getBossWaveDamage(row));
            String despawnMinutesText = optionalText(data.getBossWaveSize(row));
            String announceGlobalText = optionalText(data.getTimedAnnounceGlobal(row));
            String announceWorldText = optionalText(data.getTimedAnnounceWorld(row));
            String announceMinTierText = optionalText(data.getTimedAnnounceMinTier(row));
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
                    || looksLikeUiBindingExpression(despawnSecondsText)
                    || looksLikeUiBindingExpression(despawnMinutesText)
                    || looksLikeUiBindingExpression(announceGlobalText)
                    || looksLikeUiBindingExpression(announceWorldText)
                    || looksLikeUiBindingExpression(announceMinTierText)
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
            long despawnSeconds;
            if (!manualMode) {
                despawnMinutes = parseRequiredLong(
                        despawnMinutesText.isEmpty() ? (intervalMode ? "5" : "0") : despawnMinutesText,
                        "Ligne " + row + " : Temps avant disparition (minutes) invalide.",
                        0L,
                        Long.MAX_VALUE
                );
                despawnSeconds = parseRequiredLong(
                        despawnSecondsText.isEmpty() ? "0" : despawnSecondsText,
                        "Ligne " + row + " : Temps avant disparition (secondes) invalide.",
                        0L,
                        59L
                );
            } else if (existingRow != null) {
                despawnMinutes = BossArenaConfig.resolveMinutes(
                        existingRow.despawnAfterHours,
                        existingRow.despawnAfterMinutes
                );
                despawnSeconds = Math.max(0L, existingRow.despawnAfterSeconds);
            } else {
                despawnMinutes = 0L;
                despawnSeconds = 0L;
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
            entry.despawnAfterSeconds = despawnSeconds;
            entry.announceWorldWide = announceGlobal;
            entry.announceCurrentWorld = announceWorld;
            entry.announceMinTier = announceMinTierText.isEmpty()
                    ? (existingRow != null ? Math.max(0, existingRow.announceMinTier) : 0)
                    : parseRequiredInt(announceMinTierText,
                            "Ligne " + row + " : Tier min doit être un entier entre 0 et " + ZoneTier.MAX_TIER + ".",
                            0, ZoneTier.MAX_TIER);
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
                .append("@BossSpawnTrigger", "#BossSpawnTrigger.Value")
                .append("@BossSpawnTriggerValue", "#BossSpawnTriggerValue.Value")
                .append("@BossSpawnSpreadRandom", "#BossSpawnSpreadRandom.Value")
                .append("@BossSpawnSpreadRadius", "#BossSpawnSpreadRadius.Value")
                .append("@BossWaveRandomLocations", "#BossWaveRandomLocations.Value")
                .append("@BossWaveRandomRadius", "#BossWaveRandomRadius.Value")
                .append("@BossWaveMobMult", "#BossWaveMobMult.Value")
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
                    .append("@BossWaveAmountMin" + suffix, "#BossWaveAmountMin" + suffix + ".Value")
                    .append("@BossWaveAmountMax" + suffix, "#BossWaveAmountMax" + suffix + ".Value")
                    .append("@BossWaveHp" + suffix, "#BossWaveHp" + suffix + ".Value")
                    .append("@BossWaveDamage" + suffix, "#BossWaveDamage" + suffix + ".Value")
                    .append("@BossWaveSize" + suffix, "#BossWaveSize" + suffix + ".Value");
        }

        return snapshot;
    }

    private void openNewBossEditor() {
        flushPendingBossEditorAutoSave();
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
        flushPendingBossEditorAutoSave();

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
            cancelPendingBossEditorAutoSave();
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
            boss.modifiers.regen = BossRegen.normalizeHpPerSecond(data.bossEditRegen);
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
            boss.perPlayerIncrease.regen = BossArenaConfigUiControls.clampFloat(data.bossEditPpRegen, MULT_PERS_MIN, MULT_PERS_MAX);
        }
        if (data.bossWaveMobMult != null && Float.isFinite(data.bossWaveMobMult)) {
            if (boss.extraMobs == null) {
                boss.extraMobs = new BossDefinition.ExtraMobs();
            }
            boss.extraMobs.mobsPerPlayerMult = BossArenaConfigUiControls.clampFloat(data.bossWaveMobMult, 1.0f, 3.0f);
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

        scheduleBossEditorAutoSave();
    }

    /**
     * Debounces a background persist of the current boss editor draft so admins no longer have to
     * click "Enregistrer" for every slider/toggle change to survive a crash/restart. Runs
     * {@link #CONFIG_AUTOSAVE_DEBOUNCE_MS} after the last edit; a new edit before that fires
     * cancels and reschedules. Intentionally silent (no rebuild/status message) and skips renames —
     * only the explicit Save button handles renaming/validation errors.
     */
    private void scheduleBossEditorAutoSave() {
        if (bossEditorState == null) {
            return;
        }
        if (pendingBossEditorAutoSave != null) {
            pendingBossEditorAutoSave.cancel(false);
        }
        String originalBossName = bossEditorState.originalBossName;
        pendingBossEditorAutoSave = CONFIG_AUTOSAVE_EXECUTOR.schedule(
                () -> autoSaveBossEditorDraft(originalBossName),
                CONFIG_AUTOSAVE_DEBOUNCE_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    /** Cancels any pending debounced auto-save without flushing it (about to be superseded by an explicit save). */
    private void cancelPendingBossEditorAutoSave() {
        if (pendingBossEditorAutoSave != null) {
            pendingBossEditorAutoSave.cancel(false);
            pendingBossEditorAutoSave = null;
        }
    }

    /** Cancels any pending debounced auto-save and runs it immediately (editor closing — don't lose the last edit). */
    private void flushPendingBossEditorAutoSave() {
        if (pendingBossEditorAutoSave == null) {
            return;
        }
        pendingBossEditorAutoSave.cancel(false);
        pendingBossEditorAutoSave = null;
        if (bossEditorState != null) {
            autoSaveBossEditorDraft(bossEditorState.originalBossName);
        }
    }

    /**
     * Background task: persists the boss editor's current draft as-is. Only touches a boss that
     * already exists in the registry under {@code originalBossName} (i.e. skips brand-new/renamed
     * bosses that haven't been through an explicit Save yet, since only the Save handler resolves
     * name collisions and registers a NEW entry).
     */
    private void autoSaveBossEditorDraft(String originalBossName) {
        try {
            BossEditorState state = bossEditorState;
            if (state == null || state.boss == null) {
                return;
            }
            String currentName = optionalText(state.boss.bossName);
            if (currentName.isEmpty()) {
                // Blank mid-edit: nothing coherent to save yet.
                return;
            }

            boolean isNewBoss = originalBossName == null || originalBossName.isBlank();
            boolean nameChanged = !isNewBoss && !currentName.equalsIgnoreCase(originalBossName);

            if (isNewBoss || nameChanged) {
                if (BossRegistry.exists(currentName)) {
                    // Renamed/created onto a name that's already taken: leave it for the admin
                    // to resolve by changing the BossID again — don't silently overwrite another boss.
                    return;
                }
            } else if (BossRegistry.get(originalBossName) == null) {
                // Boss was deleted mid-edit — nothing to auto-save onto.
                return;
            }

            BossDefinition draftCopy = cloneBoss(state.boss);
            LootTable lootCopy = state.loot != null ? cloneLoot(state.loot, draftCopy.bossName) : null;

            if (nameChanged) {
                BossRegistry.remove(originalBossName);
                LootRegistry.remove(originalBossName);
            }
            BossRegistry.register(draftCopy);
            if (lootCopy != null) {
                LootRegistry.register(lootCopy);
            }
            plugin.saveBossDefinitions();
            plugin.saveLootTables();

            if (isNewBoss || nameChanged) {
                bossEditorState = new BossEditorState(draftCopy.bossName, cloneBoss(draftCopy),
                        cloneLoot(lootCopy, draftCopy.bossName));
            }
        } catch (Exception ex) {
            plugin.getLogger().atWarning().withCause(ex).log(
                    "Boss editor auto-save failed for '" + originalBossName + "'");
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
            scheduleBossEditorAutoSave();
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
                blank.mobsPerWaveMin = 1;
                blank.mobsPerWaveMax = 1;
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

    private void handleBossPpCycle(String statKey, ConfigEventData data) {
        if (bossEditorState == null) {
            return;
        }
        applyBossEditorDraft(data);
        BossDefinition.PerPlayerIncrease pp = bossEditorState.boss.perPlayerIncrease;
        switch (statKey) {
            case "hp" -> pp.hp = BossArenaConfigUiControls.nextPerPlayerStep(pp.hp);
            case "damage" -> pp.damage = BossArenaConfigUiControls.nextPerPlayerStep(pp.damage);
            case "size" -> pp.size = BossArenaConfigUiControls.nextPerPlayerStep(pp.size);
            case "speed" -> pp.movementSpeed = BossArenaConfigUiControls.nextPerPlayerStep(pp.movementSpeed);
            case "attack_rate" -> pp.attackRate = BossArenaConfigUiControls.nextPerPlayerStep(pp.attackRate);
            case "knockback_given" -> pp.knockbackGiven = BossArenaConfigUiControls.nextPerPlayerStep(pp.knockbackGiven);
            case "knockback_taken" -> pp.knockbackTaken = BossArenaConfigUiControls.nextPerPlayerStep(pp.knockbackTaken);
            case "regen" -> pp.regen = BossArenaConfigUiControls.nextPerPlayerStep(pp.regen);
            default -> {
                return;
            }
        }
        softUpdateBossSliderLabels();
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
        cmd.set("#BossEditPpHp.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.hp, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpDamage.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.damage, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpSize.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.size, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpSpeed.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.movementSpeed, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpAttackRate.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.attackRate, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpKnockbackGiven.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.knockbackGiven, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpKnockbackTaken.Text", formatFloat(BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.knockbackTaken, MULT_PERS_MIN, MULT_PERS_MAX)));
        cmd.set("#BossEditPpRegen.Text", formatFloat(
                BossArenaConfigUiControls.clampFloat(boss.perPlayerIncrease.regen, MULT_PERS_MIN, MULT_PERS_MAX)));
        if (boss.extraMobs != null) {
            cmd.set("#BossWaveMobMultValue.Text", formatFloat(
                    BossArenaConfigUiControls.clampFloat(boss.extraMobs.mobsPerPlayerMult, 1.0f, 3.0f)));
        }
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
        wave.trigger = BossDefinition.ExtraMobs.TRIGGER_SINCE_LAST_WAVE;
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
            String minText = optionalText(data.getBossWaveAmountMin(row));
            String maxText = optionalText(data.getBossWaveAmountMax(row));
            String hpText = optionalText(data.getBossWaveHp(row));
            String damageText = optionalText(data.getBossWaveDamage(row));
            String sizeText = optionalText(data.getBossWaveSize(row));
            BossDefinition.ExtraMobs.WaveAdd fb = row <= fallbackAdds.size() ? fallbackAdds.get(row - 1) : null;

            if (looksLikeUiBindingExpression(npcId)
                    || looksLikeUiBindingExpression(minText)
                    || looksLikeUiBindingExpression(maxText)
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

            int min = parseRequiredInt(
                    minText.isEmpty() ? (fb != null ? Integer.toString(Math.max(1, fb.mobsPerWaveMin)) : "1") : minText,
                    "Quantité min vague doit être un entier sur la ligne " + row + ".",
                    1,
                    Integer.MAX_VALUE
            );
            int max = parseRequiredInt(
                    maxText.isEmpty() ? (fb != null ? Integer.toString(Math.max(1, fb.mobsPerWaveMax)) : Integer.toString(min)) : maxText,
                    "Quantité max vague doit être un entier sur la ligne " + row + ".",
                    1,
                    Integer.MAX_VALUE
            );
            if (max < min) {
                max = min;
            }
            String resolvedHp = !hpText.isEmpty() ? hpText : (fb != null ? formatFloat(fb.hp > 0f ? fb.hp : 1.0f) : "1.00");
            String resolvedDamage = !damageText.isEmpty() ? damageText : (fb != null ? formatFloat(fb.damage > 0f ? fb.damage : 1.0f) : "1.00");
            String resolvedSize = !sizeText.isEmpty() ? sizeText : (fb != null ? formatFloat(fb.size > 0f ? fb.size : 1.0f) : "1.00");
            float hp = parseRequiredFloat(resolvedHp, "Mult PV vague doit être entre 0.50 et 50.00 sur la ligne " + row + ".", MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
            float damage = parseRequiredFloat(resolvedDamage, "Mult dégâts vague doit être entre 0.50 et 50.00 sur la ligne " + row + ".", MULT_HP_DMG_MIN, MULT_HP_DMG_MAX);
            float size = parseRequiredFloat(resolvedSize, "Mult taille vague doit être entre 0.10 et 10.00 sur la ligne " + row + ".", MULT_SIZE_MIN, MULT_SIZE_MAX);

            BossDefinition.ExtraMobs.WaveAdd add = new BossDefinition.ExtraMobs.WaveAdd();
            add.npcId = npcId;
            add.mobsPerWaveMin = min;
            add.mobsPerWaveMax = max;
            add.mobsPerWave = min;
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
        int min = Math.max(1, source.mobsPerWaveMin);
        int max = Math.max(min, source.mobsPerWaveMax);
        copy.mobsPerWaveMin = min;
        copy.mobsPerWaveMax = max;
        copy.mobsPerWave = min;
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
        handleBossWaveFieldChanged(data);
    }

    /** Auto-save hook for wave overlay fields with no dedicated live handler (min/max, hp/dmg/size, trigger value, repeat count). */
    private void handleBossWaveFieldChanged(ConfigEventData data) {
        if (bossEditorState == null || !bossWavesOverlayOpen) {
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
            scheduleBossEditorAutoSave();
        } catch (IllegalArgumentException ignored) {
            // Silent: the wave form may be transiently incomplete while typing.
        }
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
            String minText = optionalText(data.getBossWaveAmountMin(row));
            String maxText = optionalText(data.getBossWaveAmountMax(row));
            String hpText = optionalText(data.getBossWaveHp(row));
            String damageText = optionalText(data.getBossWaveDamage(row));
            String sizeText = optionalText(data.getBossWaveSize(row));

            if (looksLikeUiBindingExpression(triggerText)
                    || looksLikeUiBindingExpression(triggerValueText)
                    || looksLikeUiBindingExpression(repeatCountText)
                    || looksLikeUiBindingExpression(repeatSecText)
                    || looksLikeUiBindingExpression(npcId)
                    || looksLikeUiBindingExpression(minText)
                    || looksLikeUiBindingExpression(maxText)
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

            int min = parseRequiredInt(minText, "Quantité min vague doit être un entier sur la ligne " + row + ".", 1, Integer.MAX_VALUE);
            int max = parseRequiredInt(
                    maxText.isEmpty() ? Integer.toString(min) : maxText,
                    "Quantité max vague doit être un entier sur la ligne " + row + ".", 1, Integer.MAX_VALUE);
            if (max < min) {
                max = min;
            }
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
            add.mobsPerWaveMin = min;
            add.mobsPerWaveMax = max;
            add.mobsPerWave = min;
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
        public static final BuilderCodec<ConfigEventData> CODEC = buildCodec();

        /**
         * Built statement-by-statement rather than as one chained expression: the chain grew
         * past what javac can analyse and blew the compiler stack.
         */
        private static BuilderCodec<ConfigEventData> buildCodec() {
            var b = BuilderCodec.builder(ConfigEventData.class, ConfigEventData::new);
            b.append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add();
            b.append(new KeyedCodec<>("@ArenaName", Codec.STRING), (d, v) -> d.arenaName = v, d -> d.arenaName).add();
            b.append(new KeyedCodec<>("@ArenaRadius", Codec.STRING), (d, v) -> d.arenaRadius = v, d -> d.arenaRadius).add();
            b.append(new KeyedCodec<>("@ArenaWorld", Codec.STRING), (d, v) -> d.arenaWorld = v, d -> d.arenaWorld).add();
            b.append(new KeyedCodec<>("@ArenaX", Codec.STRING), (d, v) -> d.arenaX = v, d -> d.arenaX).add();
            b.append(new KeyedCodec<>("@ArenaY", Codec.STRING), (d, v) -> d.arenaY = v, d -> d.arenaY).add();
            b.append(new KeyedCodec<>("@ArenaZ", Codec.STRING), (d, v) -> d.arenaZ = v, d -> d.arenaZ).add();
            b.append(new KeyedCodec<>("@ArenaRadius1", Codec.STRING), (d, v) -> d.arenaRadius1 = v, d -> d.arenaRadius1).add();
            b.append(new KeyedCodec<>("@ArenaRadius2", Codec.STRING), (d, v) -> d.arenaRadius2 = v, d -> d.arenaRadius2).add();
            b.append(new KeyedCodec<>("@ArenaRadius3", Codec.STRING), (d, v) -> d.arenaRadius3 = v, d -> d.arenaRadius3).add();
            b.append(new KeyedCodec<>("@ArenaRadius4", Codec.STRING), (d, v) -> d.arenaRadius4 = v, d -> d.arenaRadius4).add();
            b.append(new KeyedCodec<>("@ArenaRadius5", Codec.STRING), (d, v) -> d.arenaRadius5 = v, d -> d.arenaRadius5).add();
            b.append(new KeyedCodec<>("@ArenaRadius6", Codec.STRING), (d, v) -> d.arenaRadius6 = v, d -> d.arenaRadius6).add();
            b.append(new KeyedCodec<>("@ArenaRadius7", Codec.STRING), (d, v) -> d.arenaRadius7 = v, d -> d.arenaRadius7).add();
            b.append(new KeyedCodec<>("@ArenaRadius8", Codec.STRING), (d, v) -> d.arenaRadius8 = v, d -> d.arenaRadius8).add();
            b.append(new KeyedCodec<>("@ArenaProxEnabled", Codec.STRING), (d, v) -> d.arenaProxEnabled = v, d -> d.arenaProxEnabled).add();
            b.append(new KeyedCodec<>("@ArenaProxCooldown", Codec.STRING), (d, v) -> d.arenaProxCooldown = v, d -> d.arenaProxCooldown).add();
            b.append(new KeyedCodec<>("@ShopEditArenaId", Codec.STRING), (d, v) -> d.shopEditArenaId = v, d -> d.shopEditArenaId).add();
            b.append(new KeyedCodec<>("@ShopEditVendorName", Codec.STRING), (d, v) -> d.shopEditVendorName = v, d -> d.shopEditVendorName).add();
            b.append(new KeyedCodec<>("@ShopEditCurrencyItem", Codec.STRING), (d, v) -> d.shopEditCurrencyItem = v, d -> d.shopEditCurrencyItem).add();
            b.append(new KeyedCodec<>("@ShopEditBoss1", Codec.STRING), (d, v) -> d.shopEditBoss1 = v, d -> d.shopEditBoss1).add();
            b.append(new KeyedCodec<>("@ShopEditBoss2", Codec.STRING), (d, v) -> d.shopEditBoss2 = v, d -> d.shopEditBoss2).add();
            b.append(new KeyedCodec<>("@ShopEditBoss3", Codec.STRING), (d, v) -> d.shopEditBoss3 = v, d -> d.shopEditBoss3).add();
            b.append(new KeyedCodec<>("@ShopEditBoss4", Codec.STRING), (d, v) -> d.shopEditBoss4 = v, d -> d.shopEditBoss4).add();
            b.append(new KeyedCodec<>("@ShopEditBoss5", Codec.STRING), (d, v) -> d.shopEditBoss5 = v, d -> d.shopEditBoss5).add();
            b.append(new KeyedCodec<>("@ShopEditBoss6", Codec.STRING), (d, v) -> d.shopEditBoss6 = v, d -> d.shopEditBoss6).add();
            b.append(new KeyedCodec<>("@ShopEditBoss7", Codec.STRING), (d, v) -> d.shopEditBoss7 = v, d -> d.shopEditBoss7).add();
            b.append(new KeyedCodec<>("@ShopEditBoss8", Codec.STRING), (d, v) -> d.shopEditBoss8 = v, d -> d.shopEditBoss8).add();
            b.append(new KeyedCodec<>("@ShopEditArena1", Codec.STRING), (d, v) -> d.shopEditArena1 = v, d -> d.shopEditArena1).add();
            b.append(new KeyedCodec<>("@ShopEditArena2", Codec.STRING), (d, v) -> d.shopEditArena2 = v, d -> d.shopEditArena2).add();
            b.append(new KeyedCodec<>("@ShopEditArena3", Codec.STRING), (d, v) -> d.shopEditArena3 = v, d -> d.shopEditArena3).add();
            b.append(new KeyedCodec<>("@ShopEditArena4", Codec.STRING), (d, v) -> d.shopEditArena4 = v, d -> d.shopEditArena4).add();
            b.append(new KeyedCodec<>("@ShopEditArena5", Codec.STRING), (d, v) -> d.shopEditArena5 = v, d -> d.shopEditArena5).add();
            b.append(new KeyedCodec<>("@ShopEditArena6", Codec.STRING), (d, v) -> d.shopEditArena6 = v, d -> d.shopEditArena6).add();
            b.append(new KeyedCodec<>("@ShopEditArena7", Codec.STRING), (d, v) -> d.shopEditArena7 = v, d -> d.shopEditArena7).add();
            b.append(new KeyedCodec<>("@ShopEditArena8", Codec.STRING), (d, v) -> d.shopEditArena8 = v, d -> d.shopEditArena8).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice1", Codec.STRING), (d, v) -> d.shopEditBossPrice1 = v, d -> d.shopEditBossPrice1).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice2", Codec.STRING), (d, v) -> d.shopEditBossPrice2 = v, d -> d.shopEditBossPrice2).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice3", Codec.STRING), (d, v) -> d.shopEditBossPrice3 = v, d -> d.shopEditBossPrice3).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice4", Codec.STRING), (d, v) -> d.shopEditBossPrice4 = v, d -> d.shopEditBossPrice4).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice5", Codec.STRING), (d, v) -> d.shopEditBossPrice5 = v, d -> d.shopEditBossPrice5).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice6", Codec.STRING), (d, v) -> d.shopEditBossPrice6 = v, d -> d.shopEditBossPrice6).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice7", Codec.STRING), (d, v) -> d.shopEditBossPrice7 = v, d -> d.shopEditBossPrice7).add();
            b.append(new KeyedCodec<>("@ShopEditBossPrice8", Codec.STRING), (d, v) -> d.shopEditBossPrice8 = v, d -> d.shopEditBossPrice8).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice1", Codec.STRING), (d, v) -> d.shopEditSilentPrice1 = v, d -> d.shopEditSilentPrice1).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice2", Codec.STRING), (d, v) -> d.shopEditSilentPrice2 = v, d -> d.shopEditSilentPrice2).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice3", Codec.STRING), (d, v) -> d.shopEditSilentPrice3 = v, d -> d.shopEditSilentPrice3).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice4", Codec.STRING), (d, v) -> d.shopEditSilentPrice4 = v, d -> d.shopEditSilentPrice4).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice5", Codec.STRING), (d, v) -> d.shopEditSilentPrice5 = v, d -> d.shopEditSilentPrice5).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice6", Codec.STRING), (d, v) -> d.shopEditSilentPrice6 = v, d -> d.shopEditSilentPrice6).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice7", Codec.STRING), (d, v) -> d.shopEditSilentPrice7 = v, d -> d.shopEditSilentPrice7).add();
            b.append(new KeyedCodec<>("@ShopEditSilentPrice8", Codec.STRING), (d, v) -> d.shopEditSilentPrice8 = v, d -> d.shopEditSilentPrice8).add();
            b.append(new KeyedCodec<>("@BossEditName", Codec.STRING), (d, v) -> d.bossEditName = v, d -> d.bossEditName).add();
            b.append(new KeyedCodec<>("@BossEditNpcId", Codec.STRING), (d, v) -> d.bossEditNpcId = v, d -> d.bossEditNpcId).add();
            b.append(new KeyedCodec<>("@BossEditTier", Codec.STRING), (d, v) -> d.bossEditTier = v, d -> d.bossEditTier).add();
            b.append(new KeyedCodec<>("@BossEditAmount", Codec.STRING), (d, v) -> d.bossEditAmount = v, d -> d.bossEditAmount).add();
            b.append(new KeyedCodec<>("@BossEditLevelOverride", Codec.STRING), (d, v) -> d.bossEditLevelOverride = v, d -> d.bossEditLevelOverride).add();
            b.append(new KeyedCodec<>("@BossEditHp", Codec.FLOAT), (d, v) -> d.bossEditHp = v, d -> d.bossEditHp).add();
            b.append(new KeyedCodec<>("@BossEditDamage", Codec.FLOAT), (d, v) -> d.bossEditDamage = v, d -> d.bossEditDamage).add();
            b.append(new KeyedCodec<>("@BossEditSpeed", Codec.FLOAT), (d, v) -> d.bossEditSpeed = v, d -> d.bossEditSpeed).add();
            b.append(new KeyedCodec<>("@BossEditSize", Codec.FLOAT), (d, v) -> d.bossEditSize = v, d -> d.bossEditSize).add();
            b.append(new KeyedCodec<>("@BossEditAttackRate", Codec.FLOAT), (d, v) -> d.bossEditAttackRate = v, d -> d.bossEditAttackRate).add();
            b.append(new KeyedCodec<>("@BossEditAbilityCooldown", Codec.FLOAT), (d, v) -> d.bossEditAbilityCooldown = v, d -> d.bossEditAbilityCooldown).add();
            b.append(new KeyedCodec<>("@BossEditKnockbackGiven", Codec.FLOAT), (d, v) -> d.bossEditKnockbackGiven = v, d -> d.bossEditKnockbackGiven).add();
            b.append(new KeyedCodec<>("@BossEditKnockbackTaken", Codec.FLOAT), (d, v) -> d.bossEditKnockbackTaken = v, d -> d.bossEditKnockbackTaken).add();
            b.append(new KeyedCodec<>("@BossEditTurnRate", Codec.FLOAT), (d, v) -> d.bossEditTurnRate = v, d -> d.bossEditTurnRate).add();
            b.append(new KeyedCodec<>("@BossEditRegen", Codec.FLOAT), (d, v) -> d.bossEditRegen = v, d -> d.bossEditRegen).add();
            b.append(new KeyedCodec<>("@BossEditPpHp", Codec.FLOAT), (d, v) -> d.bossEditPpHp = v, d -> d.bossEditPpHp).add();
            b.append(new KeyedCodec<>("@BossEditPpDamage", Codec.FLOAT), (d, v) -> d.bossEditPpDamage = v, d -> d.bossEditPpDamage).add();
            b.append(new KeyedCodec<>("@BossEditPpSpeed", Codec.FLOAT), (d, v) -> d.bossEditPpSpeed = v, d -> d.bossEditPpSpeed).add();
            b.append(new KeyedCodec<>("@BossEditPpSize", Codec.FLOAT), (d, v) -> d.bossEditPpSize = v, d -> d.bossEditPpSize).add();
            b.append(new KeyedCodec<>("@BossEditPpAttackRate", Codec.FLOAT), (d, v) -> d.bossEditPpAttackRate = v, d -> d.bossEditPpAttackRate).add();
            b.append(new KeyedCodec<>("@BossEditPpAbilityCooldown", Codec.FLOAT), (d, v) -> d.bossEditPpAbilityCooldown = v, d -> d.bossEditPpAbilityCooldown).add();
            b.append(new KeyedCodec<>("@BossEditPpKnockbackGiven", Codec.FLOAT), (d, v) -> d.bossEditPpKnockbackGiven = v, d -> d.bossEditPpKnockbackGiven).add();
            b.append(new KeyedCodec<>("@BossEditPpKnockbackTaken", Codec.FLOAT), (d, v) -> d.bossEditPpKnockbackTaken = v, d -> d.bossEditPpKnockbackTaken).add();
            b.append(new KeyedCodec<>("@BossEditPpTurnRate", Codec.FLOAT), (d, v) -> d.bossEditPpTurnRate = v, d -> d.bossEditPpTurnRate).add();
            b.append(new KeyedCodec<>("@BossEditPpRegen", Codec.FLOAT), (d, v) -> d.bossEditPpRegen = v, d -> d.bossEditPpRegen).add();
            b.append(new KeyedCodec<>("@BossEditWaves", Codec.STRING), (d, v) -> d.bossEditWaves = v, d -> d.bossEditWaves).add();
            b.append(new KeyedCodec<>("@BossEditExtraNpcId", Codec.STRING), (d, v) -> d.bossEditExtraNpcId = v, d -> d.bossEditExtraNpcId).add();
            b.append(new KeyedCodec<>("@BossEditExtraTimeLimit", Codec.STRING), (d, v) -> d.bossEditExtraTimeLimit = v, d -> d.bossEditExtraTimeLimit).add();
            b.append(new KeyedCodec<>("@BossEditExtraWaves", Codec.STRING), (d, v) -> d.bossEditExtraWaves = v, d -> d.bossEditExtraWaves).add();
            b.append(new KeyedCodec<>("@BossEditExtraMobsPerWave", Codec.STRING), (d, v) -> d.bossEditExtraMobsPerWave = v, d -> d.bossEditExtraMobsPerWave).add();
            b.append(new KeyedCodec<>("@BossSpawnTrigger", Codec.STRING), (d, v) -> d.bossSpawnTrigger = v, d -> d.bossSpawnTrigger).add();
            b.append(new KeyedCodec<>("@BossSpawnTriggerValue", Codec.STRING), (d, v) -> d.bossSpawnTriggerValue = v, d -> d.bossSpawnTriggerValue).add();
            b.append(new KeyedCodec<>("@BossSpawnSpreadRandom", Codec.STRING), (d, v) -> d.bossSpawnSpreadRandom = v, d -> d.bossSpawnSpreadRandom).add();
            b.append(new KeyedCodec<>("@BossSpawnSpreadRadius", Codec.STRING), (d, v) -> d.bossSpawnSpreadRadius = v, d -> d.bossSpawnSpreadRadius).add();
            b.append(new KeyedCodec<>("@BossWaveRandomLocations", Codec.STRING), (d, v) -> d.bossWaveRandomLocations = v, d -> d.bossWaveRandomLocations).add();
            b.append(new KeyedCodec<>("@BossWaveRandomRadius", Codec.STRING), (d, v) -> d.bossWaveRandomRadius = v, d -> d.bossWaveRandomRadius).add();
            b.append(new KeyedCodec<>("@BossWaveMobMult", Codec.FLOAT), (d, v) -> d.bossWaveMobMult = v, d -> d.bossWaveMobMult).add();
            b.append(new KeyedCodec<>("@BossWavesEnabled", Codec.STRING), (d, v) -> d.bossWavesEnabled = v, d -> d.bossWavesEnabled).add();
            b.append(new KeyedCodec<>("@BossEditMusic", Codec.STRING), (d, v) -> d.bossEditMusic = v, d -> d.bossEditMusic).add();
            b.append(new KeyedCodec<>("@BossEditMusicRadius", Codec.STRING), (d, v) -> d.bossEditMusicRadius = v, d -> d.bossEditMusicRadius).add();
            b.append(new KeyedCodec<>("@BossWaveTimeSec", Codec.STRING), (d, v) -> d.bossWaveTimeSec = v, d -> d.bossWaveTimeSec).add();
            b.append(new KeyedCodec<>("@BossWaveNpc1", Codec.STRING), (d, v) -> d.bossWaveNpc1 = v, d -> d.bossWaveNpc1).add();
            b.append(new KeyedCodec<>("@BossWaveAmount1", Codec.STRING), (d, v) -> d.bossWaveAmount1 = v, d -> d.bossWaveAmount1).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin1", Codec.STRING), (d, v) -> d.bossWaveAmountMin1 = v, d -> d.bossWaveAmountMin1).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax1", Codec.STRING), (d, v) -> d.bossWaveAmountMax1 = v, d -> d.bossWaveAmountMax1).add();
            b.append(new KeyedCodec<>("@BossWaveEvery1", Codec.STRING), (d, v) -> d.bossWaveEvery1 = v, d -> d.bossWaveEvery1).add();
            b.append(new KeyedCodec<>("@BossWaveHp1", Codec.STRING), (d, v) -> d.bossWaveHp1 = v, d -> d.bossWaveHp1).add();
            b.append(new KeyedCodec<>("@BossWaveDamage1", Codec.STRING), (d, v) -> d.bossWaveDamage1 = v, d -> d.bossWaveDamage1).add();
            b.append(new KeyedCodec<>("@BossWaveSize1", Codec.STRING), (d, v) -> d.bossWaveSize1 = v, d -> d.bossWaveSize1).add();
            b.append(new KeyedCodec<>("@BossWaveNpc2", Codec.STRING), (d, v) -> d.bossWaveNpc2 = v, d -> d.bossWaveNpc2).add();
            b.append(new KeyedCodec<>("@BossWaveAmount2", Codec.STRING), (d, v) -> d.bossWaveAmount2 = v, d -> d.bossWaveAmount2).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin2", Codec.STRING), (d, v) -> d.bossWaveAmountMin2 = v, d -> d.bossWaveAmountMin2).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax2", Codec.STRING), (d, v) -> d.bossWaveAmountMax2 = v, d -> d.bossWaveAmountMax2).add();
            b.append(new KeyedCodec<>("@BossWaveEvery2", Codec.STRING), (d, v) -> d.bossWaveEvery2 = v, d -> d.bossWaveEvery2).add();
            b.append(new KeyedCodec<>("@BossWaveHp2", Codec.STRING), (d, v) -> d.bossWaveHp2 = v, d -> d.bossWaveHp2).add();
            b.append(new KeyedCodec<>("@BossWaveDamage2", Codec.STRING), (d, v) -> d.bossWaveDamage2 = v, d -> d.bossWaveDamage2).add();
            b.append(new KeyedCodec<>("@BossWaveSize2", Codec.STRING), (d, v) -> d.bossWaveSize2 = v, d -> d.bossWaveSize2).add();
            b.append(new KeyedCodec<>("@BossWaveNpc3", Codec.STRING), (d, v) -> d.bossWaveNpc3 = v, d -> d.bossWaveNpc3).add();
            b.append(new KeyedCodec<>("@BossWaveAmount3", Codec.STRING), (d, v) -> d.bossWaveAmount3 = v, d -> d.bossWaveAmount3).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin3", Codec.STRING), (d, v) -> d.bossWaveAmountMin3 = v, d -> d.bossWaveAmountMin3).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax3", Codec.STRING), (d, v) -> d.bossWaveAmountMax3 = v, d -> d.bossWaveAmountMax3).add();
            b.append(new KeyedCodec<>("@BossWaveEvery3", Codec.STRING), (d, v) -> d.bossWaveEvery3 = v, d -> d.bossWaveEvery3).add();
            b.append(new KeyedCodec<>("@BossWaveHp3", Codec.STRING), (d, v) -> d.bossWaveHp3 = v, d -> d.bossWaveHp3).add();
            b.append(new KeyedCodec<>("@BossWaveDamage3", Codec.STRING), (d, v) -> d.bossWaveDamage3 = v, d -> d.bossWaveDamage3).add();
            b.append(new KeyedCodec<>("@BossWaveSize3", Codec.STRING), (d, v) -> d.bossWaveSize3 = v, d -> d.bossWaveSize3).add();
            b.append(new KeyedCodec<>("@BossWaveNpc4", Codec.STRING), (d, v) -> d.bossWaveNpc4 = v, d -> d.bossWaveNpc4).add();
            b.append(new KeyedCodec<>("@BossWaveAmount4", Codec.STRING), (d, v) -> d.bossWaveAmount4 = v, d -> d.bossWaveAmount4).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin4", Codec.STRING), (d, v) -> d.bossWaveAmountMin4 = v, d -> d.bossWaveAmountMin4).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax4", Codec.STRING), (d, v) -> d.bossWaveAmountMax4 = v, d -> d.bossWaveAmountMax4).add();
            b.append(new KeyedCodec<>("@BossWaveEvery4", Codec.STRING), (d, v) -> d.bossWaveEvery4 = v, d -> d.bossWaveEvery4).add();
            b.append(new KeyedCodec<>("@BossWaveHp4", Codec.STRING), (d, v) -> d.bossWaveHp4 = v, d -> d.bossWaveHp4).add();
            b.append(new KeyedCodec<>("@BossWaveDamage4", Codec.STRING), (d, v) -> d.bossWaveDamage4 = v, d -> d.bossWaveDamage4).add();
            b.append(new KeyedCodec<>("@BossWaveSize4", Codec.STRING), (d, v) -> d.bossWaveSize4 = v, d -> d.bossWaveSize4).add();
            b.append(new KeyedCodec<>("@BossWaveNpc5", Codec.STRING), (d, v) -> d.bossWaveNpc5 = v, d -> d.bossWaveNpc5).add();
            b.append(new KeyedCodec<>("@BossWaveAmount5", Codec.STRING), (d, v) -> d.bossWaveAmount5 = v, d -> d.bossWaveAmount5).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin5", Codec.STRING), (d, v) -> d.bossWaveAmountMin5 = v, d -> d.bossWaveAmountMin5).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax5", Codec.STRING), (d, v) -> d.bossWaveAmountMax5 = v, d -> d.bossWaveAmountMax5).add();
            b.append(new KeyedCodec<>("@BossWaveEvery5", Codec.STRING), (d, v) -> d.bossWaveEvery5 = v, d -> d.bossWaveEvery5).add();
            b.append(new KeyedCodec<>("@BossWaveHp5", Codec.STRING), (d, v) -> d.bossWaveHp5 = v, d -> d.bossWaveHp5).add();
            b.append(new KeyedCodec<>("@BossWaveDamage5", Codec.STRING), (d, v) -> d.bossWaveDamage5 = v, d -> d.bossWaveDamage5).add();
            b.append(new KeyedCodec<>("@BossWaveSize5", Codec.STRING), (d, v) -> d.bossWaveSize5 = v, d -> d.bossWaveSize5).add();
            b.append(new KeyedCodec<>("@BossWaveNpc6", Codec.STRING), (d, v) -> d.bossWaveNpc6 = v, d -> d.bossWaveNpc6).add();
            b.append(new KeyedCodec<>("@BossWaveNpc7", Codec.STRING), (d, v) -> d.bossWaveNpc7 = v, d -> d.bossWaveNpc7).add();
            b.append(new KeyedCodec<>("@BossWaveNpc8", Codec.STRING), (d, v) -> d.bossWaveNpc8 = v, d -> d.bossWaveNpc8).add();
            b.append(new KeyedCodec<>("@BossWaveNpc9", Codec.STRING), (d, v) -> d.bossWaveNpc9 = v, d -> d.bossWaveNpc9).add();
            b.append(new KeyedCodec<>("@BossWaveNpc10", Codec.STRING), (d, v) -> d.bossWaveNpc10 = v, d -> d.bossWaveNpc10).add();
            b.append(new KeyedCodec<>("@BossWaveNpc11", Codec.STRING), (d, v) -> d.bossWaveNpc11 = v, d -> d.bossWaveNpc11).add();
            b.append(new KeyedCodec<>("@BossWaveNpc12", Codec.STRING), (d, v) -> d.bossWaveNpc12 = v, d -> d.bossWaveNpc12).add();
            b.append(new KeyedCodec<>("@BossWaveNpc13", Codec.STRING), (d, v) -> d.bossWaveNpc13 = v, d -> d.bossWaveNpc13).add();
            b.append(new KeyedCodec<>("@BossWaveNpc14", Codec.STRING), (d, v) -> d.bossWaveNpc14 = v, d -> d.bossWaveNpc14).add();
            b.append(new KeyedCodec<>("@BossWaveNpc15", Codec.STRING), (d, v) -> d.bossWaveNpc15 = v, d -> d.bossWaveNpc15).add();
            b.append(new KeyedCodec<>("@BossWaveNpc16", Codec.STRING), (d, v) -> d.bossWaveNpc16 = v, d -> d.bossWaveNpc16).add();
            b.append(new KeyedCodec<>("@BossWaveNpc17", Codec.STRING), (d, v) -> d.bossWaveNpc17 = v, d -> d.bossWaveNpc17).add();
            b.append(new KeyedCodec<>("@BossWaveNpc18", Codec.STRING), (d, v) -> d.bossWaveNpc18 = v, d -> d.bossWaveNpc18).add();
            b.append(new KeyedCodec<>("@BossWaveNpc19", Codec.STRING), (d, v) -> d.bossWaveNpc19 = v, d -> d.bossWaveNpc19).add();
            b.append(new KeyedCodec<>("@BossWaveNpc20", Codec.STRING), (d, v) -> d.bossWaveNpc20 = v, d -> d.bossWaveNpc20).add();
            b.append(new KeyedCodec<>("@BossWaveNpc21", Codec.STRING), (d, v) -> d.bossWaveNpc21 = v, d -> d.bossWaveNpc21).add();
            b.append(new KeyedCodec<>("@BossWaveNpc22", Codec.STRING), (d, v) -> d.bossWaveNpc22 = v, d -> d.bossWaveNpc22).add();
            b.append(new KeyedCodec<>("@BossWaveNpc23", Codec.STRING), (d, v) -> d.bossWaveNpc23 = v, d -> d.bossWaveNpc23).add();
            b.append(new KeyedCodec<>("@BossWaveNpc24", Codec.STRING), (d, v) -> d.bossWaveNpc24 = v, d -> d.bossWaveNpc24).add();
            b.append(new KeyedCodec<>("@BossWaveNpc25", Codec.STRING), (d, v) -> d.bossWaveNpc25 = v, d -> d.bossWaveNpc25).add();
            b.append(new KeyedCodec<>("@BossWaveNpc26", Codec.STRING), (d, v) -> d.bossWaveNpc26 = v, d -> d.bossWaveNpc26).add();
            b.append(new KeyedCodec<>("@BossWaveNpc27", Codec.STRING), (d, v) -> d.bossWaveNpc27 = v, d -> d.bossWaveNpc27).add();
            b.append(new KeyedCodec<>("@BossWaveNpc28", Codec.STRING), (d, v) -> d.bossWaveNpc28 = v, d -> d.bossWaveNpc28).add();
            b.append(new KeyedCodec<>("@BossWaveNpc29", Codec.STRING), (d, v) -> d.bossWaveNpc29 = v, d -> d.bossWaveNpc29).add();
            b.append(new KeyedCodec<>("@BossWaveNpc30", Codec.STRING), (d, v) -> d.bossWaveNpc30 = v, d -> d.bossWaveNpc30).add();
            b.append(new KeyedCodec<>("@BossWaveNpc31", Codec.STRING), (d, v) -> d.bossWaveNpc31 = v, d -> d.bossWaveNpc31).add();
            b.append(new KeyedCodec<>("@BossWaveNpc32", Codec.STRING), (d, v) -> d.bossWaveNpc32 = v, d -> d.bossWaveNpc32).add();
            b.append(new KeyedCodec<>("@BossWaveAmount6", Codec.STRING), (d, v) -> d.bossWaveAmount6 = v, d -> d.bossWaveAmount6).add();
            b.append(new KeyedCodec<>("@BossWaveAmount7", Codec.STRING), (d, v) -> d.bossWaveAmount7 = v, d -> d.bossWaveAmount7).add();
            b.append(new KeyedCodec<>("@BossWaveAmount8", Codec.STRING), (d, v) -> d.bossWaveAmount8 = v, d -> d.bossWaveAmount8).add();
            b.append(new KeyedCodec<>("@BossWaveAmount9", Codec.STRING), (d, v) -> d.bossWaveAmount9 = v, d -> d.bossWaveAmount9).add();
            b.append(new KeyedCodec<>("@BossWaveAmount10", Codec.STRING), (d, v) -> d.bossWaveAmount10 = v, d -> d.bossWaveAmount10).add();
            b.append(new KeyedCodec<>("@BossWaveAmount11", Codec.STRING), (d, v) -> d.bossWaveAmount11 = v, d -> d.bossWaveAmount11).add();
            b.append(new KeyedCodec<>("@BossWaveAmount12", Codec.STRING), (d, v) -> d.bossWaveAmount12 = v, d -> d.bossWaveAmount12).add();
            b.append(new KeyedCodec<>("@BossWaveAmount13", Codec.STRING), (d, v) -> d.bossWaveAmount13 = v, d -> d.bossWaveAmount13).add();
            b.append(new KeyedCodec<>("@BossWaveAmount14", Codec.STRING), (d, v) -> d.bossWaveAmount14 = v, d -> d.bossWaveAmount14).add();
            b.append(new KeyedCodec<>("@BossWaveAmount15", Codec.STRING), (d, v) -> d.bossWaveAmount15 = v, d -> d.bossWaveAmount15).add();
            b.append(new KeyedCodec<>("@BossWaveAmount16", Codec.STRING), (d, v) -> d.bossWaveAmount16 = v, d -> d.bossWaveAmount16).add();
            b.append(new KeyedCodec<>("@BossWaveAmount17", Codec.STRING), (d, v) -> d.bossWaveAmount17 = v, d -> d.bossWaveAmount17).add();
            b.append(new KeyedCodec<>("@BossWaveAmount18", Codec.STRING), (d, v) -> d.bossWaveAmount18 = v, d -> d.bossWaveAmount18).add();
            b.append(new KeyedCodec<>("@BossWaveAmount19", Codec.STRING), (d, v) -> d.bossWaveAmount19 = v, d -> d.bossWaveAmount19).add();
            b.append(new KeyedCodec<>("@BossWaveAmount20", Codec.STRING), (d, v) -> d.bossWaveAmount20 = v, d -> d.bossWaveAmount20).add();
            b.append(new KeyedCodec<>("@BossWaveAmount21", Codec.STRING), (d, v) -> d.bossWaveAmount21 = v, d -> d.bossWaveAmount21).add();
            b.append(new KeyedCodec<>("@BossWaveAmount22", Codec.STRING), (d, v) -> d.bossWaveAmount22 = v, d -> d.bossWaveAmount22).add();
            b.append(new KeyedCodec<>("@BossWaveAmount23", Codec.STRING), (d, v) -> d.bossWaveAmount23 = v, d -> d.bossWaveAmount23).add();
            b.append(new KeyedCodec<>("@BossWaveAmount24", Codec.STRING), (d, v) -> d.bossWaveAmount24 = v, d -> d.bossWaveAmount24).add();
            b.append(new KeyedCodec<>("@BossWaveAmount25", Codec.STRING), (d, v) -> d.bossWaveAmount25 = v, d -> d.bossWaveAmount25).add();
            b.append(new KeyedCodec<>("@BossWaveAmount26", Codec.STRING), (d, v) -> d.bossWaveAmount26 = v, d -> d.bossWaveAmount26).add();
            b.append(new KeyedCodec<>("@BossWaveAmount27", Codec.STRING), (d, v) -> d.bossWaveAmount27 = v, d -> d.bossWaveAmount27).add();
            b.append(new KeyedCodec<>("@BossWaveAmount28", Codec.STRING), (d, v) -> d.bossWaveAmount28 = v, d -> d.bossWaveAmount28).add();
            b.append(new KeyedCodec<>("@BossWaveAmount29", Codec.STRING), (d, v) -> d.bossWaveAmount29 = v, d -> d.bossWaveAmount29).add();
            b.append(new KeyedCodec<>("@BossWaveAmount30", Codec.STRING), (d, v) -> d.bossWaveAmount30 = v, d -> d.bossWaveAmount30).add();
            b.append(new KeyedCodec<>("@BossWaveAmount31", Codec.STRING), (d, v) -> d.bossWaveAmount31 = v, d -> d.bossWaveAmount31).add();
            b.append(new KeyedCodec<>("@BossWaveAmount32", Codec.STRING), (d, v) -> d.bossWaveAmount32 = v, d -> d.bossWaveAmount32).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin6", Codec.STRING), (d, v) -> d.bossWaveAmountMin6 = v, d -> d.bossWaveAmountMin6).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin7", Codec.STRING), (d, v) -> d.bossWaveAmountMin7 = v, d -> d.bossWaveAmountMin7).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin8", Codec.STRING), (d, v) -> d.bossWaveAmountMin8 = v, d -> d.bossWaveAmountMin8).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin9", Codec.STRING), (d, v) -> d.bossWaveAmountMin9 = v, d -> d.bossWaveAmountMin9).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin10", Codec.STRING), (d, v) -> d.bossWaveAmountMin10 = v, d -> d.bossWaveAmountMin10).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin11", Codec.STRING), (d, v) -> d.bossWaveAmountMin11 = v, d -> d.bossWaveAmountMin11).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin12", Codec.STRING), (d, v) -> d.bossWaveAmountMin12 = v, d -> d.bossWaveAmountMin12).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin13", Codec.STRING), (d, v) -> d.bossWaveAmountMin13 = v, d -> d.bossWaveAmountMin13).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin14", Codec.STRING), (d, v) -> d.bossWaveAmountMin14 = v, d -> d.bossWaveAmountMin14).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin15", Codec.STRING), (d, v) -> d.bossWaveAmountMin15 = v, d -> d.bossWaveAmountMin15).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin16", Codec.STRING), (d, v) -> d.bossWaveAmountMin16 = v, d -> d.bossWaveAmountMin16).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin17", Codec.STRING), (d, v) -> d.bossWaveAmountMin17 = v, d -> d.bossWaveAmountMin17).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin18", Codec.STRING), (d, v) -> d.bossWaveAmountMin18 = v, d -> d.bossWaveAmountMin18).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin19", Codec.STRING), (d, v) -> d.bossWaveAmountMin19 = v, d -> d.bossWaveAmountMin19).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin20", Codec.STRING), (d, v) -> d.bossWaveAmountMin20 = v, d -> d.bossWaveAmountMin20).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin21", Codec.STRING), (d, v) -> d.bossWaveAmountMin21 = v, d -> d.bossWaveAmountMin21).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin22", Codec.STRING), (d, v) -> d.bossWaveAmountMin22 = v, d -> d.bossWaveAmountMin22).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin23", Codec.STRING), (d, v) -> d.bossWaveAmountMin23 = v, d -> d.bossWaveAmountMin23).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin24", Codec.STRING), (d, v) -> d.bossWaveAmountMin24 = v, d -> d.bossWaveAmountMin24).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin25", Codec.STRING), (d, v) -> d.bossWaveAmountMin25 = v, d -> d.bossWaveAmountMin25).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin26", Codec.STRING), (d, v) -> d.bossWaveAmountMin26 = v, d -> d.bossWaveAmountMin26).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin27", Codec.STRING), (d, v) -> d.bossWaveAmountMin27 = v, d -> d.bossWaveAmountMin27).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin28", Codec.STRING), (d, v) -> d.bossWaveAmountMin28 = v, d -> d.bossWaveAmountMin28).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin29", Codec.STRING), (d, v) -> d.bossWaveAmountMin29 = v, d -> d.bossWaveAmountMin29).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin30", Codec.STRING), (d, v) -> d.bossWaveAmountMin30 = v, d -> d.bossWaveAmountMin30).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin31", Codec.STRING), (d, v) -> d.bossWaveAmountMin31 = v, d -> d.bossWaveAmountMin31).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMin32", Codec.STRING), (d, v) -> d.bossWaveAmountMin32 = v, d -> d.bossWaveAmountMin32).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax6", Codec.STRING), (d, v) -> d.bossWaveAmountMax6 = v, d -> d.bossWaveAmountMax6).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax7", Codec.STRING), (d, v) -> d.bossWaveAmountMax7 = v, d -> d.bossWaveAmountMax7).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax8", Codec.STRING), (d, v) -> d.bossWaveAmountMax8 = v, d -> d.bossWaveAmountMax8).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax9", Codec.STRING), (d, v) -> d.bossWaveAmountMax9 = v, d -> d.bossWaveAmountMax9).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax10", Codec.STRING), (d, v) -> d.bossWaveAmountMax10 = v, d -> d.bossWaveAmountMax10).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax11", Codec.STRING), (d, v) -> d.bossWaveAmountMax11 = v, d -> d.bossWaveAmountMax11).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax12", Codec.STRING), (d, v) -> d.bossWaveAmountMax12 = v, d -> d.bossWaveAmountMax12).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax13", Codec.STRING), (d, v) -> d.bossWaveAmountMax13 = v, d -> d.bossWaveAmountMax13).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax14", Codec.STRING), (d, v) -> d.bossWaveAmountMax14 = v, d -> d.bossWaveAmountMax14).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax15", Codec.STRING), (d, v) -> d.bossWaveAmountMax15 = v, d -> d.bossWaveAmountMax15).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax16", Codec.STRING), (d, v) -> d.bossWaveAmountMax16 = v, d -> d.bossWaveAmountMax16).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax17", Codec.STRING), (d, v) -> d.bossWaveAmountMax17 = v, d -> d.bossWaveAmountMax17).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax18", Codec.STRING), (d, v) -> d.bossWaveAmountMax18 = v, d -> d.bossWaveAmountMax18).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax19", Codec.STRING), (d, v) -> d.bossWaveAmountMax19 = v, d -> d.bossWaveAmountMax19).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax20", Codec.STRING), (d, v) -> d.bossWaveAmountMax20 = v, d -> d.bossWaveAmountMax20).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax21", Codec.STRING), (d, v) -> d.bossWaveAmountMax21 = v, d -> d.bossWaveAmountMax21).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax22", Codec.STRING), (d, v) -> d.bossWaveAmountMax22 = v, d -> d.bossWaveAmountMax22).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax23", Codec.STRING), (d, v) -> d.bossWaveAmountMax23 = v, d -> d.bossWaveAmountMax23).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax24", Codec.STRING), (d, v) -> d.bossWaveAmountMax24 = v, d -> d.bossWaveAmountMax24).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax25", Codec.STRING), (d, v) -> d.bossWaveAmountMax25 = v, d -> d.bossWaveAmountMax25).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax26", Codec.STRING), (d, v) -> d.bossWaveAmountMax26 = v, d -> d.bossWaveAmountMax26).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax27", Codec.STRING), (d, v) -> d.bossWaveAmountMax27 = v, d -> d.bossWaveAmountMax27).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax28", Codec.STRING), (d, v) -> d.bossWaveAmountMax28 = v, d -> d.bossWaveAmountMax28).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax29", Codec.STRING), (d, v) -> d.bossWaveAmountMax29 = v, d -> d.bossWaveAmountMax29).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax30", Codec.STRING), (d, v) -> d.bossWaveAmountMax30 = v, d -> d.bossWaveAmountMax30).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax31", Codec.STRING), (d, v) -> d.bossWaveAmountMax31 = v, d -> d.bossWaveAmountMax31).add();
            b.append(new KeyedCodec<>("@BossWaveAmountMax32", Codec.STRING), (d, v) -> d.bossWaveAmountMax32 = v, d -> d.bossWaveAmountMax32).add();
            b.append(new KeyedCodec<>("@BossWaveEvery6", Codec.STRING), (d, v) -> d.bossWaveEvery6 = v, d -> d.bossWaveEvery6).add();
            b.append(new KeyedCodec<>("@BossWaveEvery7", Codec.STRING), (d, v) -> d.bossWaveEvery7 = v, d -> d.bossWaveEvery7).add();
            b.append(new KeyedCodec<>("@BossWaveEvery8", Codec.STRING), (d, v) -> d.bossWaveEvery8 = v, d -> d.bossWaveEvery8).add();
            b.append(new KeyedCodec<>("@BossWaveEvery9", Codec.STRING), (d, v) -> d.bossWaveEvery9 = v, d -> d.bossWaveEvery9).add();
            b.append(new KeyedCodec<>("@BossWaveEvery10", Codec.STRING), (d, v) -> d.bossWaveEvery10 = v, d -> d.bossWaveEvery10).add();
            b.append(new KeyedCodec<>("@BossWaveEvery11", Codec.STRING), (d, v) -> d.bossWaveEvery11 = v, d -> d.bossWaveEvery11).add();
            b.append(new KeyedCodec<>("@BossWaveEvery12", Codec.STRING), (d, v) -> d.bossWaveEvery12 = v, d -> d.bossWaveEvery12).add();
            b.append(new KeyedCodec<>("@BossWaveEvery13", Codec.STRING), (d, v) -> d.bossWaveEvery13 = v, d -> d.bossWaveEvery13).add();
            b.append(new KeyedCodec<>("@BossWaveEvery14", Codec.STRING), (d, v) -> d.bossWaveEvery14 = v, d -> d.bossWaveEvery14).add();
            b.append(new KeyedCodec<>("@BossWaveEvery15", Codec.STRING), (d, v) -> d.bossWaveEvery15 = v, d -> d.bossWaveEvery15).add();
            b.append(new KeyedCodec<>("@BossWaveEvery16", Codec.STRING), (d, v) -> d.bossWaveEvery16 = v, d -> d.bossWaveEvery16).add();
            b.append(new KeyedCodec<>("@BossWaveEvery17", Codec.STRING), (d, v) -> d.bossWaveEvery17 = v, d -> d.bossWaveEvery17).add();
            b.append(new KeyedCodec<>("@BossWaveEvery18", Codec.STRING), (d, v) -> d.bossWaveEvery18 = v, d -> d.bossWaveEvery18).add();
            b.append(new KeyedCodec<>("@BossWaveEvery19", Codec.STRING), (d, v) -> d.bossWaveEvery19 = v, d -> d.bossWaveEvery19).add();
            b.append(new KeyedCodec<>("@BossWaveEvery20", Codec.STRING), (d, v) -> d.bossWaveEvery20 = v, d -> d.bossWaveEvery20).add();
            b.append(new KeyedCodec<>("@BossWaveEvery21", Codec.STRING), (d, v) -> d.bossWaveEvery21 = v, d -> d.bossWaveEvery21).add();
            b.append(new KeyedCodec<>("@BossWaveEvery22", Codec.STRING), (d, v) -> d.bossWaveEvery22 = v, d -> d.bossWaveEvery22).add();
            b.append(new KeyedCodec<>("@BossWaveEvery23", Codec.STRING), (d, v) -> d.bossWaveEvery23 = v, d -> d.bossWaveEvery23).add();
            b.append(new KeyedCodec<>("@BossWaveEvery24", Codec.STRING), (d, v) -> d.bossWaveEvery24 = v, d -> d.bossWaveEvery24).add();
            b.append(new KeyedCodec<>("@BossWaveEvery25", Codec.STRING), (d, v) -> d.bossWaveEvery25 = v, d -> d.bossWaveEvery25).add();
            b.append(new KeyedCodec<>("@BossWaveEvery26", Codec.STRING), (d, v) -> d.bossWaveEvery26 = v, d -> d.bossWaveEvery26).add();
            b.append(new KeyedCodec<>("@BossWaveEvery27", Codec.STRING), (d, v) -> d.bossWaveEvery27 = v, d -> d.bossWaveEvery27).add();
            b.append(new KeyedCodec<>("@BossWaveEvery28", Codec.STRING), (d, v) -> d.bossWaveEvery28 = v, d -> d.bossWaveEvery28).add();
            b.append(new KeyedCodec<>("@BossWaveEvery29", Codec.STRING), (d, v) -> d.bossWaveEvery29 = v, d -> d.bossWaveEvery29).add();
            b.append(new KeyedCodec<>("@BossWaveEvery30", Codec.STRING), (d, v) -> d.bossWaveEvery30 = v, d -> d.bossWaveEvery30).add();
            b.append(new KeyedCodec<>("@BossWaveEvery31", Codec.STRING), (d, v) -> d.bossWaveEvery31 = v, d -> d.bossWaveEvery31).add();
            b.append(new KeyedCodec<>("@BossWaveEvery32", Codec.STRING), (d, v) -> d.bossWaveEvery32 = v, d -> d.bossWaveEvery32).add();
            b.append(new KeyedCodec<>("@BossWaveHp6", Codec.STRING), (d, v) -> d.bossWaveHp6 = v, d -> d.bossWaveHp6).add();
            b.append(new KeyedCodec<>("@BossWaveHp7", Codec.STRING), (d, v) -> d.bossWaveHp7 = v, d -> d.bossWaveHp7).add();
            b.append(new KeyedCodec<>("@BossWaveHp8", Codec.STRING), (d, v) -> d.bossWaveHp8 = v, d -> d.bossWaveHp8).add();
            b.append(new KeyedCodec<>("@BossWaveHp9", Codec.STRING), (d, v) -> d.bossWaveHp9 = v, d -> d.bossWaveHp9).add();
            b.append(new KeyedCodec<>("@BossWaveHp10", Codec.STRING), (d, v) -> d.bossWaveHp10 = v, d -> d.bossWaveHp10).add();
            b.append(new KeyedCodec<>("@BossWaveHp11", Codec.STRING), (d, v) -> d.bossWaveHp11 = v, d -> d.bossWaveHp11).add();
            b.append(new KeyedCodec<>("@BossWaveHp12", Codec.STRING), (d, v) -> d.bossWaveHp12 = v, d -> d.bossWaveHp12).add();
            b.append(new KeyedCodec<>("@BossWaveHp13", Codec.STRING), (d, v) -> d.bossWaveHp13 = v, d -> d.bossWaveHp13).add();
            b.append(new KeyedCodec<>("@BossWaveHp14", Codec.STRING), (d, v) -> d.bossWaveHp14 = v, d -> d.bossWaveHp14).add();
            b.append(new KeyedCodec<>("@BossWaveHp15", Codec.STRING), (d, v) -> d.bossWaveHp15 = v, d -> d.bossWaveHp15).add();
            b.append(new KeyedCodec<>("@BossWaveHp16", Codec.STRING), (d, v) -> d.bossWaveHp16 = v, d -> d.bossWaveHp16).add();
            b.append(new KeyedCodec<>("@BossWaveHp17", Codec.STRING), (d, v) -> d.bossWaveHp17 = v, d -> d.bossWaveHp17).add();
            b.append(new KeyedCodec<>("@BossWaveHp18", Codec.STRING), (d, v) -> d.bossWaveHp18 = v, d -> d.bossWaveHp18).add();
            b.append(new KeyedCodec<>("@BossWaveHp19", Codec.STRING), (d, v) -> d.bossWaveHp19 = v, d -> d.bossWaveHp19).add();
            b.append(new KeyedCodec<>("@BossWaveHp20", Codec.STRING), (d, v) -> d.bossWaveHp20 = v, d -> d.bossWaveHp20).add();
            b.append(new KeyedCodec<>("@BossWaveHp21", Codec.STRING), (d, v) -> d.bossWaveHp21 = v, d -> d.bossWaveHp21).add();
            b.append(new KeyedCodec<>("@BossWaveHp22", Codec.STRING), (d, v) -> d.bossWaveHp22 = v, d -> d.bossWaveHp22).add();
            b.append(new KeyedCodec<>("@BossWaveHp23", Codec.STRING), (d, v) -> d.bossWaveHp23 = v, d -> d.bossWaveHp23).add();
            b.append(new KeyedCodec<>("@BossWaveHp24", Codec.STRING), (d, v) -> d.bossWaveHp24 = v, d -> d.bossWaveHp24).add();
            b.append(new KeyedCodec<>("@BossWaveHp25", Codec.STRING), (d, v) -> d.bossWaveHp25 = v, d -> d.bossWaveHp25).add();
            b.append(new KeyedCodec<>("@BossWaveHp26", Codec.STRING), (d, v) -> d.bossWaveHp26 = v, d -> d.bossWaveHp26).add();
            b.append(new KeyedCodec<>("@BossWaveHp27", Codec.STRING), (d, v) -> d.bossWaveHp27 = v, d -> d.bossWaveHp27).add();
            b.append(new KeyedCodec<>("@BossWaveHp28", Codec.STRING), (d, v) -> d.bossWaveHp28 = v, d -> d.bossWaveHp28).add();
            b.append(new KeyedCodec<>("@BossWaveHp29", Codec.STRING), (d, v) -> d.bossWaveHp29 = v, d -> d.bossWaveHp29).add();
            b.append(new KeyedCodec<>("@BossWaveHp30", Codec.STRING), (d, v) -> d.bossWaveHp30 = v, d -> d.bossWaveHp30).add();
            b.append(new KeyedCodec<>("@BossWaveHp31", Codec.STRING), (d, v) -> d.bossWaveHp31 = v, d -> d.bossWaveHp31).add();
            b.append(new KeyedCodec<>("@BossWaveHp32", Codec.STRING), (d, v) -> d.bossWaveHp32 = v, d -> d.bossWaveHp32).add();
            b.append(new KeyedCodec<>("@BossWaveDamage6", Codec.STRING), (d, v) -> d.bossWaveDamage6 = v, d -> d.bossWaveDamage6).add();
            b.append(new KeyedCodec<>("@BossWaveDamage7", Codec.STRING), (d, v) -> d.bossWaveDamage7 = v, d -> d.bossWaveDamage7).add();
            b.append(new KeyedCodec<>("@BossWaveDamage8", Codec.STRING), (d, v) -> d.bossWaveDamage8 = v, d -> d.bossWaveDamage8).add();
            b.append(new KeyedCodec<>("@BossWaveDamage9", Codec.STRING), (d, v) -> d.bossWaveDamage9 = v, d -> d.bossWaveDamage9).add();
            b.append(new KeyedCodec<>("@BossWaveDamage10", Codec.STRING), (d, v) -> d.bossWaveDamage10 = v, d -> d.bossWaveDamage10).add();
            b.append(new KeyedCodec<>("@BossWaveDamage11", Codec.STRING), (d, v) -> d.bossWaveDamage11 = v, d -> d.bossWaveDamage11).add();
            b.append(new KeyedCodec<>("@BossWaveDamage12", Codec.STRING), (d, v) -> d.bossWaveDamage12 = v, d -> d.bossWaveDamage12).add();
            b.append(new KeyedCodec<>("@BossWaveDamage13", Codec.STRING), (d, v) -> d.bossWaveDamage13 = v, d -> d.bossWaveDamage13).add();
            b.append(new KeyedCodec<>("@BossWaveDamage14", Codec.STRING), (d, v) -> d.bossWaveDamage14 = v, d -> d.bossWaveDamage14).add();
            b.append(new KeyedCodec<>("@BossWaveDamage15", Codec.STRING), (d, v) -> d.bossWaveDamage15 = v, d -> d.bossWaveDamage15).add();
            b.append(new KeyedCodec<>("@BossWaveDamage16", Codec.STRING), (d, v) -> d.bossWaveDamage16 = v, d -> d.bossWaveDamage16).add();
            b.append(new KeyedCodec<>("@BossWaveDamage17", Codec.STRING), (d, v) -> d.bossWaveDamage17 = v, d -> d.bossWaveDamage17).add();
            b.append(new KeyedCodec<>("@BossWaveDamage18", Codec.STRING), (d, v) -> d.bossWaveDamage18 = v, d -> d.bossWaveDamage18).add();
            b.append(new KeyedCodec<>("@BossWaveDamage19", Codec.STRING), (d, v) -> d.bossWaveDamage19 = v, d -> d.bossWaveDamage19).add();
            b.append(new KeyedCodec<>("@BossWaveDamage20", Codec.STRING), (d, v) -> d.bossWaveDamage20 = v, d -> d.bossWaveDamage20).add();
            b.append(new KeyedCodec<>("@BossWaveDamage21", Codec.STRING), (d, v) -> d.bossWaveDamage21 = v, d -> d.bossWaveDamage21).add();
            b.append(new KeyedCodec<>("@BossWaveDamage22", Codec.STRING), (d, v) -> d.bossWaveDamage22 = v, d -> d.bossWaveDamage22).add();
            b.append(new KeyedCodec<>("@BossWaveDamage23", Codec.STRING), (d, v) -> d.bossWaveDamage23 = v, d -> d.bossWaveDamage23).add();
            b.append(new KeyedCodec<>("@BossWaveDamage24", Codec.STRING), (d, v) -> d.bossWaveDamage24 = v, d -> d.bossWaveDamage24).add();
            b.append(new KeyedCodec<>("@BossWaveDamage25", Codec.STRING), (d, v) -> d.bossWaveDamage25 = v, d -> d.bossWaveDamage25).add();
            b.append(new KeyedCodec<>("@BossWaveDamage26", Codec.STRING), (d, v) -> d.bossWaveDamage26 = v, d -> d.bossWaveDamage26).add();
            b.append(new KeyedCodec<>("@BossWaveDamage27", Codec.STRING), (d, v) -> d.bossWaveDamage27 = v, d -> d.bossWaveDamage27).add();
            b.append(new KeyedCodec<>("@BossWaveDamage28", Codec.STRING), (d, v) -> d.bossWaveDamage28 = v, d -> d.bossWaveDamage28).add();
            b.append(new KeyedCodec<>("@BossWaveDamage29", Codec.STRING), (d, v) -> d.bossWaveDamage29 = v, d -> d.bossWaveDamage29).add();
            b.append(new KeyedCodec<>("@BossWaveDamage30", Codec.STRING), (d, v) -> d.bossWaveDamage30 = v, d -> d.bossWaveDamage30).add();
            b.append(new KeyedCodec<>("@BossWaveDamage31", Codec.STRING), (d, v) -> d.bossWaveDamage31 = v, d -> d.bossWaveDamage31).add();
            b.append(new KeyedCodec<>("@BossWaveDamage32", Codec.STRING), (d, v) -> d.bossWaveDamage32 = v, d -> d.bossWaveDamage32).add();
            b.append(new KeyedCodec<>("@BossWaveSize6", Codec.STRING), (d, v) -> d.bossWaveSize6 = v, d -> d.bossWaveSize6).add();
            b.append(new KeyedCodec<>("@BossWaveSize7", Codec.STRING), (d, v) -> d.bossWaveSize7 = v, d -> d.bossWaveSize7).add();
            b.append(new KeyedCodec<>("@BossWaveSize8", Codec.STRING), (d, v) -> d.bossWaveSize8 = v, d -> d.bossWaveSize8).add();
            b.append(new KeyedCodec<>("@BossWaveSize9", Codec.STRING), (d, v) -> d.bossWaveSize9 = v, d -> d.bossWaveSize9).add();
            b.append(new KeyedCodec<>("@BossWaveSize10", Codec.STRING), (d, v) -> d.bossWaveSize10 = v, d -> d.bossWaveSize10).add();
            b.append(new KeyedCodec<>("@BossWaveSize11", Codec.STRING), (d, v) -> d.bossWaveSize11 = v, d -> d.bossWaveSize11).add();
            b.append(new KeyedCodec<>("@BossWaveSize12", Codec.STRING), (d, v) -> d.bossWaveSize12 = v, d -> d.bossWaveSize12).add();
            b.append(new KeyedCodec<>("@BossWaveSize13", Codec.STRING), (d, v) -> d.bossWaveSize13 = v, d -> d.bossWaveSize13).add();
            b.append(new KeyedCodec<>("@BossWaveSize14", Codec.STRING), (d, v) -> d.bossWaveSize14 = v, d -> d.bossWaveSize14).add();
            b.append(new KeyedCodec<>("@BossWaveSize15", Codec.STRING), (d, v) -> d.bossWaveSize15 = v, d -> d.bossWaveSize15).add();
            b.append(new KeyedCodec<>("@BossWaveSize16", Codec.STRING), (d, v) -> d.bossWaveSize16 = v, d -> d.bossWaveSize16).add();
            b.append(new KeyedCodec<>("@BossWaveSize17", Codec.STRING), (d, v) -> d.bossWaveSize17 = v, d -> d.bossWaveSize17).add();
            b.append(new KeyedCodec<>("@BossWaveSize18", Codec.STRING), (d, v) -> d.bossWaveSize18 = v, d -> d.bossWaveSize18).add();
            b.append(new KeyedCodec<>("@BossWaveSize19", Codec.STRING), (d, v) -> d.bossWaveSize19 = v, d -> d.bossWaveSize19).add();
            b.append(new KeyedCodec<>("@BossWaveSize20", Codec.STRING), (d, v) -> d.bossWaveSize20 = v, d -> d.bossWaveSize20).add();
            b.append(new KeyedCodec<>("@BossWaveSize21", Codec.STRING), (d, v) -> d.bossWaveSize21 = v, d -> d.bossWaveSize21).add();
            b.append(new KeyedCodec<>("@BossWaveSize22", Codec.STRING), (d, v) -> d.bossWaveSize22 = v, d -> d.bossWaveSize22).add();
            b.append(new KeyedCodec<>("@BossWaveSize23", Codec.STRING), (d, v) -> d.bossWaveSize23 = v, d -> d.bossWaveSize23).add();
            b.append(new KeyedCodec<>("@BossWaveSize24", Codec.STRING), (d, v) -> d.bossWaveSize24 = v, d -> d.bossWaveSize24).add();
            b.append(new KeyedCodec<>("@BossWaveSize25", Codec.STRING), (d, v) -> d.bossWaveSize25 = v, d -> d.bossWaveSize25).add();
            b.append(new KeyedCodec<>("@BossWaveSize26", Codec.STRING), (d, v) -> d.bossWaveSize26 = v, d -> d.bossWaveSize26).add();
            b.append(new KeyedCodec<>("@BossWaveSize27", Codec.STRING), (d, v) -> d.bossWaveSize27 = v, d -> d.bossWaveSize27).add();
            b.append(new KeyedCodec<>("@BossWaveSize28", Codec.STRING), (d, v) -> d.bossWaveSize28 = v, d -> d.bossWaveSize28).add();
            b.append(new KeyedCodec<>("@BossWaveSize29", Codec.STRING), (d, v) -> d.bossWaveSize29 = v, d -> d.bossWaveSize29).add();
            b.append(new KeyedCodec<>("@BossWaveSize30", Codec.STRING), (d, v) -> d.bossWaveSize30 = v, d -> d.bossWaveSize30).add();
            b.append(new KeyedCodec<>("@BossWaveSize31", Codec.STRING), (d, v) -> d.bossWaveSize31 = v, d -> d.bossWaveSize31).add();
            b.append(new KeyedCodec<>("@BossWaveSize32", Codec.STRING), (d, v) -> d.bossWaveSize32 = v, d -> d.bossWaveSize32).add();
            b.append(new KeyedCodec<>("@BossWaveValue1", Codec.STRING), (d, v) -> d.bossWaveValue1 = v, d -> d.bossWaveValue1).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount1", Codec.STRING), (d, v) -> d.bossWaveRepeatCount1 = v, d -> d.bossWaveRepeatCount1).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec1", Codec.STRING), (d, v) -> d.bossWaveRepeatSec1 = v, d -> d.bossWaveRepeatSec1).add();
            b.append(new KeyedCodec<>("@BossWaveValue2", Codec.STRING), (d, v) -> d.bossWaveValue2 = v, d -> d.bossWaveValue2).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount2", Codec.STRING), (d, v) -> d.bossWaveRepeatCount2 = v, d -> d.bossWaveRepeatCount2).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec2", Codec.STRING), (d, v) -> d.bossWaveRepeatSec2 = v, d -> d.bossWaveRepeatSec2).add();
            b.append(new KeyedCodec<>("@BossWaveValue3", Codec.STRING), (d, v) -> d.bossWaveValue3 = v, d -> d.bossWaveValue3).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount3", Codec.STRING), (d, v) -> d.bossWaveRepeatCount3 = v, d -> d.bossWaveRepeatCount3).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec3", Codec.STRING), (d, v) -> d.bossWaveRepeatSec3 = v, d -> d.bossWaveRepeatSec3).add();
            b.append(new KeyedCodec<>("@BossWaveValue4", Codec.STRING), (d, v) -> d.bossWaveValue4 = v, d -> d.bossWaveValue4).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount4", Codec.STRING), (d, v) -> d.bossWaveRepeatCount4 = v, d -> d.bossWaveRepeatCount4).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec4", Codec.STRING), (d, v) -> d.bossWaveRepeatSec4 = v, d -> d.bossWaveRepeatSec4).add();
            b.append(new KeyedCodec<>("@BossWaveValue5", Codec.STRING), (d, v) -> d.bossWaveValue5 = v, d -> d.bossWaveValue5).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount5", Codec.STRING), (d, v) -> d.bossWaveRepeatCount5 = v, d -> d.bossWaveRepeatCount5).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec5", Codec.STRING), (d, v) -> d.bossWaveRepeatSec5 = v, d -> d.bossWaveRepeatSec5).add();
            b.append(new KeyedCodec<>("@BossWaveValue6", Codec.STRING), (d, v) -> d.bossWaveValue6 = v, d -> d.bossWaveValue6).add();
            b.append(new KeyedCodec<>("@BossWaveValue7", Codec.STRING), (d, v) -> d.bossWaveValue7 = v, d -> d.bossWaveValue7).add();
            b.append(new KeyedCodec<>("@BossWaveValue8", Codec.STRING), (d, v) -> d.bossWaveValue8 = v, d -> d.bossWaveValue8).add();
            b.append(new KeyedCodec<>("@BossWaveValue9", Codec.STRING), (d, v) -> d.bossWaveValue9 = v, d -> d.bossWaveValue9).add();
            b.append(new KeyedCodec<>("@BossWaveValue10", Codec.STRING), (d, v) -> d.bossWaveValue10 = v, d -> d.bossWaveValue10).add();
            b.append(new KeyedCodec<>("@BossWaveValue11", Codec.STRING), (d, v) -> d.bossWaveValue11 = v, d -> d.bossWaveValue11).add();
            b.append(new KeyedCodec<>("@BossWaveValue12", Codec.STRING), (d, v) -> d.bossWaveValue12 = v, d -> d.bossWaveValue12).add();
            b.append(new KeyedCodec<>("@BossWaveValue13", Codec.STRING), (d, v) -> d.bossWaveValue13 = v, d -> d.bossWaveValue13).add();
            b.append(new KeyedCodec<>("@BossWaveValue14", Codec.STRING), (d, v) -> d.bossWaveValue14 = v, d -> d.bossWaveValue14).add();
            b.append(new KeyedCodec<>("@BossWaveValue15", Codec.STRING), (d, v) -> d.bossWaveValue15 = v, d -> d.bossWaveValue15).add();
            b.append(new KeyedCodec<>("@BossWaveValue16", Codec.STRING), (d, v) -> d.bossWaveValue16 = v, d -> d.bossWaveValue16).add();
            b.append(new KeyedCodec<>("@BossWaveValue17", Codec.STRING), (d, v) -> d.bossWaveValue17 = v, d -> d.bossWaveValue17).add();
            b.append(new KeyedCodec<>("@BossWaveValue18", Codec.STRING), (d, v) -> d.bossWaveValue18 = v, d -> d.bossWaveValue18).add();
            b.append(new KeyedCodec<>("@BossWaveValue19", Codec.STRING), (d, v) -> d.bossWaveValue19 = v, d -> d.bossWaveValue19).add();
            b.append(new KeyedCodec<>("@BossWaveValue20", Codec.STRING), (d, v) -> d.bossWaveValue20 = v, d -> d.bossWaveValue20).add();
            b.append(new KeyedCodec<>("@BossWaveValue21", Codec.STRING), (d, v) -> d.bossWaveValue21 = v, d -> d.bossWaveValue21).add();
            b.append(new KeyedCodec<>("@BossWaveValue22", Codec.STRING), (d, v) -> d.bossWaveValue22 = v, d -> d.bossWaveValue22).add();
            b.append(new KeyedCodec<>("@BossWaveValue23", Codec.STRING), (d, v) -> d.bossWaveValue23 = v, d -> d.bossWaveValue23).add();
            b.append(new KeyedCodec<>("@BossWaveValue24", Codec.STRING), (d, v) -> d.bossWaveValue24 = v, d -> d.bossWaveValue24).add();
            b.append(new KeyedCodec<>("@BossWaveValue25", Codec.STRING), (d, v) -> d.bossWaveValue25 = v, d -> d.bossWaveValue25).add();
            b.append(new KeyedCodec<>("@BossWaveValue26", Codec.STRING), (d, v) -> d.bossWaveValue26 = v, d -> d.bossWaveValue26).add();
            b.append(new KeyedCodec<>("@BossWaveValue27", Codec.STRING), (d, v) -> d.bossWaveValue27 = v, d -> d.bossWaveValue27).add();
            b.append(new KeyedCodec<>("@BossWaveValue28", Codec.STRING), (d, v) -> d.bossWaveValue28 = v, d -> d.bossWaveValue28).add();
            b.append(new KeyedCodec<>("@BossWaveValue29", Codec.STRING), (d, v) -> d.bossWaveValue29 = v, d -> d.bossWaveValue29).add();
            b.append(new KeyedCodec<>("@BossWaveValue30", Codec.STRING), (d, v) -> d.bossWaveValue30 = v, d -> d.bossWaveValue30).add();
            b.append(new KeyedCodec<>("@BossWaveValue31", Codec.STRING), (d, v) -> d.bossWaveValue31 = v, d -> d.bossWaveValue31).add();
            b.append(new KeyedCodec<>("@BossWaveValue32", Codec.STRING), (d, v) -> d.bossWaveValue32 = v, d -> d.bossWaveValue32).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount6", Codec.STRING), (d, v) -> d.bossWaveRepeatCount6 = v, d -> d.bossWaveRepeatCount6).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount7", Codec.STRING), (d, v) -> d.bossWaveRepeatCount7 = v, d -> d.bossWaveRepeatCount7).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount8", Codec.STRING), (d, v) -> d.bossWaveRepeatCount8 = v, d -> d.bossWaveRepeatCount8).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount9", Codec.STRING), (d, v) -> d.bossWaveRepeatCount9 = v, d -> d.bossWaveRepeatCount9).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount10", Codec.STRING), (d, v) -> d.bossWaveRepeatCount10 = v, d -> d.bossWaveRepeatCount10).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount11", Codec.STRING), (d, v) -> d.bossWaveRepeatCount11 = v, d -> d.bossWaveRepeatCount11).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount12", Codec.STRING), (d, v) -> d.bossWaveRepeatCount12 = v, d -> d.bossWaveRepeatCount12).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount13", Codec.STRING), (d, v) -> d.bossWaveRepeatCount13 = v, d -> d.bossWaveRepeatCount13).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount14", Codec.STRING), (d, v) -> d.bossWaveRepeatCount14 = v, d -> d.bossWaveRepeatCount14).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount15", Codec.STRING), (d, v) -> d.bossWaveRepeatCount15 = v, d -> d.bossWaveRepeatCount15).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount16", Codec.STRING), (d, v) -> d.bossWaveRepeatCount16 = v, d -> d.bossWaveRepeatCount16).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount17", Codec.STRING), (d, v) -> d.bossWaveRepeatCount17 = v, d -> d.bossWaveRepeatCount17).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount18", Codec.STRING), (d, v) -> d.bossWaveRepeatCount18 = v, d -> d.bossWaveRepeatCount18).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount19", Codec.STRING), (d, v) -> d.bossWaveRepeatCount19 = v, d -> d.bossWaveRepeatCount19).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount20", Codec.STRING), (d, v) -> d.bossWaveRepeatCount20 = v, d -> d.bossWaveRepeatCount20).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount21", Codec.STRING), (d, v) -> d.bossWaveRepeatCount21 = v, d -> d.bossWaveRepeatCount21).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount22", Codec.STRING), (d, v) -> d.bossWaveRepeatCount22 = v, d -> d.bossWaveRepeatCount22).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount23", Codec.STRING), (d, v) -> d.bossWaveRepeatCount23 = v, d -> d.bossWaveRepeatCount23).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount24", Codec.STRING), (d, v) -> d.bossWaveRepeatCount24 = v, d -> d.bossWaveRepeatCount24).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount25", Codec.STRING), (d, v) -> d.bossWaveRepeatCount25 = v, d -> d.bossWaveRepeatCount25).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount26", Codec.STRING), (d, v) -> d.bossWaveRepeatCount26 = v, d -> d.bossWaveRepeatCount26).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount27", Codec.STRING), (d, v) -> d.bossWaveRepeatCount27 = v, d -> d.bossWaveRepeatCount27).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount28", Codec.STRING), (d, v) -> d.bossWaveRepeatCount28 = v, d -> d.bossWaveRepeatCount28).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount29", Codec.STRING), (d, v) -> d.bossWaveRepeatCount29 = v, d -> d.bossWaveRepeatCount29).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount30", Codec.STRING), (d, v) -> d.bossWaveRepeatCount30 = v, d -> d.bossWaveRepeatCount30).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount31", Codec.STRING), (d, v) -> d.bossWaveRepeatCount31 = v, d -> d.bossWaveRepeatCount31).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatCount32", Codec.STRING), (d, v) -> d.bossWaveRepeatCount32 = v, d -> d.bossWaveRepeatCount32).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec6", Codec.STRING), (d, v) -> d.bossWaveRepeatSec6 = v, d -> d.bossWaveRepeatSec6).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec7", Codec.STRING), (d, v) -> d.bossWaveRepeatSec7 = v, d -> d.bossWaveRepeatSec7).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec8", Codec.STRING), (d, v) -> d.bossWaveRepeatSec8 = v, d -> d.bossWaveRepeatSec8).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec9", Codec.STRING), (d, v) -> d.bossWaveRepeatSec9 = v, d -> d.bossWaveRepeatSec9).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec10", Codec.STRING), (d, v) -> d.bossWaveRepeatSec10 = v, d -> d.bossWaveRepeatSec10).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec11", Codec.STRING), (d, v) -> d.bossWaveRepeatSec11 = v, d -> d.bossWaveRepeatSec11).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec12", Codec.STRING), (d, v) -> d.bossWaveRepeatSec12 = v, d -> d.bossWaveRepeatSec12).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec13", Codec.STRING), (d, v) -> d.bossWaveRepeatSec13 = v, d -> d.bossWaveRepeatSec13).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec14", Codec.STRING), (d, v) -> d.bossWaveRepeatSec14 = v, d -> d.bossWaveRepeatSec14).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec15", Codec.STRING), (d, v) -> d.bossWaveRepeatSec15 = v, d -> d.bossWaveRepeatSec15).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec16", Codec.STRING), (d, v) -> d.bossWaveRepeatSec16 = v, d -> d.bossWaveRepeatSec16).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec17", Codec.STRING), (d, v) -> d.bossWaveRepeatSec17 = v, d -> d.bossWaveRepeatSec17).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec18", Codec.STRING), (d, v) -> d.bossWaveRepeatSec18 = v, d -> d.bossWaveRepeatSec18).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec19", Codec.STRING), (d, v) -> d.bossWaveRepeatSec19 = v, d -> d.bossWaveRepeatSec19).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec20", Codec.STRING), (d, v) -> d.bossWaveRepeatSec20 = v, d -> d.bossWaveRepeatSec20).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec21", Codec.STRING), (d, v) -> d.bossWaveRepeatSec21 = v, d -> d.bossWaveRepeatSec21).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec22", Codec.STRING), (d, v) -> d.bossWaveRepeatSec22 = v, d -> d.bossWaveRepeatSec22).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec23", Codec.STRING), (d, v) -> d.bossWaveRepeatSec23 = v, d -> d.bossWaveRepeatSec23).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec24", Codec.STRING), (d, v) -> d.bossWaveRepeatSec24 = v, d -> d.bossWaveRepeatSec24).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec25", Codec.STRING), (d, v) -> d.bossWaveRepeatSec25 = v, d -> d.bossWaveRepeatSec25).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec26", Codec.STRING), (d, v) -> d.bossWaveRepeatSec26 = v, d -> d.bossWaveRepeatSec26).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec27", Codec.STRING), (d, v) -> d.bossWaveRepeatSec27 = v, d -> d.bossWaveRepeatSec27).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec28", Codec.STRING), (d, v) -> d.bossWaveRepeatSec28 = v, d -> d.bossWaveRepeatSec28).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec29", Codec.STRING), (d, v) -> d.bossWaveRepeatSec29 = v, d -> d.bossWaveRepeatSec29).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec30", Codec.STRING), (d, v) -> d.bossWaveRepeatSec30 = v, d -> d.bossWaveRepeatSec30).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec31", Codec.STRING), (d, v) -> d.bossWaveRepeatSec31 = v, d -> d.bossWaveRepeatSec31).add();
            b.append(new KeyedCodec<>("@BossWaveRepeatSec32", Codec.STRING), (d, v) -> d.bossWaveRepeatSec32 = v, d -> d.bossWaveRepeatSec32).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers1", Codec.STRING), (d, v) -> d.timedMinPlayers1 = v, d -> d.timedMinPlayers1).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers2", Codec.STRING), (d, v) -> d.timedMinPlayers2 = v, d -> d.timedMinPlayers2).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers3", Codec.STRING), (d, v) -> d.timedMinPlayers3 = v, d -> d.timedMinPlayers3).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers4", Codec.STRING), (d, v) -> d.timedMinPlayers4 = v, d -> d.timedMinPlayers4).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers5", Codec.STRING), (d, v) -> d.timedMinPlayers5 = v, d -> d.timedMinPlayers5).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers6", Codec.STRING), (d, v) -> d.timedMinPlayers6 = v, d -> d.timedMinPlayers6).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers7", Codec.STRING), (d, v) -> d.timedMinPlayers7 = v, d -> d.timedMinPlayers7).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers8", Codec.STRING), (d, v) -> d.timedMinPlayers8 = v, d -> d.timedMinPlayers8).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers9", Codec.STRING), (d, v) -> d.timedMinPlayers9 = v, d -> d.timedMinPlayers9).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers10", Codec.STRING), (d, v) -> d.timedMinPlayers10 = v, d -> d.timedMinPlayers10).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers11", Codec.STRING), (d, v) -> d.timedMinPlayers11 = v, d -> d.timedMinPlayers11).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers12", Codec.STRING), (d, v) -> d.timedMinPlayers12 = v, d -> d.timedMinPlayers12).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers13", Codec.STRING), (d, v) -> d.timedMinPlayers13 = v, d -> d.timedMinPlayers13).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers14", Codec.STRING), (d, v) -> d.timedMinPlayers14 = v, d -> d.timedMinPlayers14).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers15", Codec.STRING), (d, v) -> d.timedMinPlayers15 = v, d -> d.timedMinPlayers15).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers16", Codec.STRING), (d, v) -> d.timedMinPlayers16 = v, d -> d.timedMinPlayers16).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers17", Codec.STRING), (d, v) -> d.timedMinPlayers17 = v, d -> d.timedMinPlayers17).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers18", Codec.STRING), (d, v) -> d.timedMinPlayers18 = v, d -> d.timedMinPlayers18).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers19", Codec.STRING), (d, v) -> d.timedMinPlayers19 = v, d -> d.timedMinPlayers19).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers20", Codec.STRING), (d, v) -> d.timedMinPlayers20 = v, d -> d.timedMinPlayers20).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers21", Codec.STRING), (d, v) -> d.timedMinPlayers21 = v, d -> d.timedMinPlayers21).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers22", Codec.STRING), (d, v) -> d.timedMinPlayers22 = v, d -> d.timedMinPlayers22).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers23", Codec.STRING), (d, v) -> d.timedMinPlayers23 = v, d -> d.timedMinPlayers23).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers24", Codec.STRING), (d, v) -> d.timedMinPlayers24 = v, d -> d.timedMinPlayers24).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers25", Codec.STRING), (d, v) -> d.timedMinPlayers25 = v, d -> d.timedMinPlayers25).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers26", Codec.STRING), (d, v) -> d.timedMinPlayers26 = v, d -> d.timedMinPlayers26).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers27", Codec.STRING), (d, v) -> d.timedMinPlayers27 = v, d -> d.timedMinPlayers27).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers28", Codec.STRING), (d, v) -> d.timedMinPlayers28 = v, d -> d.timedMinPlayers28).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers29", Codec.STRING), (d, v) -> d.timedMinPlayers29 = v, d -> d.timedMinPlayers29).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers30", Codec.STRING), (d, v) -> d.timedMinPlayers30 = v, d -> d.timedMinPlayers30).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers31", Codec.STRING), (d, v) -> d.timedMinPlayers31 = v, d -> d.timedMinPlayers31).add();
            b.append(new KeyedCodec<>("@TimedMinPlayers32", Codec.STRING), (d, v) -> d.timedMinPlayers32 = v, d -> d.timedMinPlayers32).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds1", Codec.STRING), (d, v) -> d.timedEverySeconds1 = v, d -> d.timedEverySeconds1).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds2", Codec.STRING), (d, v) -> d.timedEverySeconds2 = v, d -> d.timedEverySeconds2).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds3", Codec.STRING), (d, v) -> d.timedEverySeconds3 = v, d -> d.timedEverySeconds3).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds4", Codec.STRING), (d, v) -> d.timedEverySeconds4 = v, d -> d.timedEverySeconds4).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds5", Codec.STRING), (d, v) -> d.timedEverySeconds5 = v, d -> d.timedEverySeconds5).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds6", Codec.STRING), (d, v) -> d.timedEverySeconds6 = v, d -> d.timedEverySeconds6).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds7", Codec.STRING), (d, v) -> d.timedEverySeconds7 = v, d -> d.timedEverySeconds7).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds8", Codec.STRING), (d, v) -> d.timedEverySeconds8 = v, d -> d.timedEverySeconds8).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds9", Codec.STRING), (d, v) -> d.timedEverySeconds9 = v, d -> d.timedEverySeconds9).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds10", Codec.STRING), (d, v) -> d.timedEverySeconds10 = v, d -> d.timedEverySeconds10).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds11", Codec.STRING), (d, v) -> d.timedEverySeconds11 = v, d -> d.timedEverySeconds11).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds12", Codec.STRING), (d, v) -> d.timedEverySeconds12 = v, d -> d.timedEverySeconds12).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds13", Codec.STRING), (d, v) -> d.timedEverySeconds13 = v, d -> d.timedEverySeconds13).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds14", Codec.STRING), (d, v) -> d.timedEverySeconds14 = v, d -> d.timedEverySeconds14).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds15", Codec.STRING), (d, v) -> d.timedEverySeconds15 = v, d -> d.timedEverySeconds15).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds16", Codec.STRING), (d, v) -> d.timedEverySeconds16 = v, d -> d.timedEverySeconds16).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds17", Codec.STRING), (d, v) -> d.timedEverySeconds17 = v, d -> d.timedEverySeconds17).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds18", Codec.STRING), (d, v) -> d.timedEverySeconds18 = v, d -> d.timedEverySeconds18).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds19", Codec.STRING), (d, v) -> d.timedEverySeconds19 = v, d -> d.timedEverySeconds19).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds20", Codec.STRING), (d, v) -> d.timedEverySeconds20 = v, d -> d.timedEverySeconds20).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds21", Codec.STRING), (d, v) -> d.timedEverySeconds21 = v, d -> d.timedEverySeconds21).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds22", Codec.STRING), (d, v) -> d.timedEverySeconds22 = v, d -> d.timedEverySeconds22).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds23", Codec.STRING), (d, v) -> d.timedEverySeconds23 = v, d -> d.timedEverySeconds23).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds24", Codec.STRING), (d, v) -> d.timedEverySeconds24 = v, d -> d.timedEverySeconds24).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds25", Codec.STRING), (d, v) -> d.timedEverySeconds25 = v, d -> d.timedEverySeconds25).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds26", Codec.STRING), (d, v) -> d.timedEverySeconds26 = v, d -> d.timedEverySeconds26).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds27", Codec.STRING), (d, v) -> d.timedEverySeconds27 = v, d -> d.timedEverySeconds27).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds28", Codec.STRING), (d, v) -> d.timedEverySeconds28 = v, d -> d.timedEverySeconds28).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds29", Codec.STRING), (d, v) -> d.timedEverySeconds29 = v, d -> d.timedEverySeconds29).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds30", Codec.STRING), (d, v) -> d.timedEverySeconds30 = v, d -> d.timedEverySeconds30).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds31", Codec.STRING), (d, v) -> d.timedEverySeconds31 = v, d -> d.timedEverySeconds31).add();
            b.append(new KeyedCodec<>("@TimedEverySeconds32", Codec.STRING), (d, v) -> d.timedEverySeconds32 = v, d -> d.timedEverySeconds32).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours1", Codec.STRING), (d, v) -> d.timedIntervalHours1 = v, d -> d.timedIntervalHours1).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours2", Codec.STRING), (d, v) -> d.timedIntervalHours2 = v, d -> d.timedIntervalHours2).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours3", Codec.STRING), (d, v) -> d.timedIntervalHours3 = v, d -> d.timedIntervalHours3).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours4", Codec.STRING), (d, v) -> d.timedIntervalHours4 = v, d -> d.timedIntervalHours4).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours5", Codec.STRING), (d, v) -> d.timedIntervalHours5 = v, d -> d.timedIntervalHours5).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours6", Codec.STRING), (d, v) -> d.timedIntervalHours6 = v, d -> d.timedIntervalHours6).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours7", Codec.STRING), (d, v) -> d.timedIntervalHours7 = v, d -> d.timedIntervalHours7).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours8", Codec.STRING), (d, v) -> d.timedIntervalHours8 = v, d -> d.timedIntervalHours8).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours9", Codec.STRING), (d, v) -> d.timedIntervalHours9 = v, d -> d.timedIntervalHours9).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours10", Codec.STRING), (d, v) -> d.timedIntervalHours10 = v, d -> d.timedIntervalHours10).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours11", Codec.STRING), (d, v) -> d.timedIntervalHours11 = v, d -> d.timedIntervalHours11).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours12", Codec.STRING), (d, v) -> d.timedIntervalHours12 = v, d -> d.timedIntervalHours12).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours13", Codec.STRING), (d, v) -> d.timedIntervalHours13 = v, d -> d.timedIntervalHours13).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours14", Codec.STRING), (d, v) -> d.timedIntervalHours14 = v, d -> d.timedIntervalHours14).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours15", Codec.STRING), (d, v) -> d.timedIntervalHours15 = v, d -> d.timedIntervalHours15).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours16", Codec.STRING), (d, v) -> d.timedIntervalHours16 = v, d -> d.timedIntervalHours16).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours17", Codec.STRING), (d, v) -> d.timedIntervalHours17 = v, d -> d.timedIntervalHours17).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours18", Codec.STRING), (d, v) -> d.timedIntervalHours18 = v, d -> d.timedIntervalHours18).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours19", Codec.STRING), (d, v) -> d.timedIntervalHours19 = v, d -> d.timedIntervalHours19).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours20", Codec.STRING), (d, v) -> d.timedIntervalHours20 = v, d -> d.timedIntervalHours20).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours21", Codec.STRING), (d, v) -> d.timedIntervalHours21 = v, d -> d.timedIntervalHours21).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours22", Codec.STRING), (d, v) -> d.timedIntervalHours22 = v, d -> d.timedIntervalHours22).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours23", Codec.STRING), (d, v) -> d.timedIntervalHours23 = v, d -> d.timedIntervalHours23).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours24", Codec.STRING), (d, v) -> d.timedIntervalHours24 = v, d -> d.timedIntervalHours24).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours25", Codec.STRING), (d, v) -> d.timedIntervalHours25 = v, d -> d.timedIntervalHours25).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours26", Codec.STRING), (d, v) -> d.timedIntervalHours26 = v, d -> d.timedIntervalHours26).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours27", Codec.STRING), (d, v) -> d.timedIntervalHours27 = v, d -> d.timedIntervalHours27).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours28", Codec.STRING), (d, v) -> d.timedIntervalHours28 = v, d -> d.timedIntervalHours28).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours29", Codec.STRING), (d, v) -> d.timedIntervalHours29 = v, d -> d.timedIntervalHours29).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours30", Codec.STRING), (d, v) -> d.timedIntervalHours30 = v, d -> d.timedIntervalHours30).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours31", Codec.STRING), (d, v) -> d.timedIntervalHours31 = v, d -> d.timedIntervalHours31).add();
            b.append(new KeyedCodec<>("@TimedIntervalHours32", Codec.STRING), (d, v) -> d.timedIntervalHours32 = v, d -> d.timedIntervalHours32).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays1", Codec.STRING), (d, v) -> d.timedIntervalDays1 = v, d -> d.timedIntervalDays1).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays2", Codec.STRING), (d, v) -> d.timedIntervalDays2 = v, d -> d.timedIntervalDays2).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays3", Codec.STRING), (d, v) -> d.timedIntervalDays3 = v, d -> d.timedIntervalDays3).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays4", Codec.STRING), (d, v) -> d.timedIntervalDays4 = v, d -> d.timedIntervalDays4).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays5", Codec.STRING), (d, v) -> d.timedIntervalDays5 = v, d -> d.timedIntervalDays5).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays6", Codec.STRING), (d, v) -> d.timedIntervalDays6 = v, d -> d.timedIntervalDays6).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays7", Codec.STRING), (d, v) -> d.timedIntervalDays7 = v, d -> d.timedIntervalDays7).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays8", Codec.STRING), (d, v) -> d.timedIntervalDays8 = v, d -> d.timedIntervalDays8).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays9", Codec.STRING), (d, v) -> d.timedIntervalDays9 = v, d -> d.timedIntervalDays9).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays10", Codec.STRING), (d, v) -> d.timedIntervalDays10 = v, d -> d.timedIntervalDays10).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays11", Codec.STRING), (d, v) -> d.timedIntervalDays11 = v, d -> d.timedIntervalDays11).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays12", Codec.STRING), (d, v) -> d.timedIntervalDays12 = v, d -> d.timedIntervalDays12).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays13", Codec.STRING), (d, v) -> d.timedIntervalDays13 = v, d -> d.timedIntervalDays13).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays14", Codec.STRING), (d, v) -> d.timedIntervalDays14 = v, d -> d.timedIntervalDays14).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays15", Codec.STRING), (d, v) -> d.timedIntervalDays15 = v, d -> d.timedIntervalDays15).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays16", Codec.STRING), (d, v) -> d.timedIntervalDays16 = v, d -> d.timedIntervalDays16).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays17", Codec.STRING), (d, v) -> d.timedIntervalDays17 = v, d -> d.timedIntervalDays17).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays18", Codec.STRING), (d, v) -> d.timedIntervalDays18 = v, d -> d.timedIntervalDays18).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays19", Codec.STRING), (d, v) -> d.timedIntervalDays19 = v, d -> d.timedIntervalDays19).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays20", Codec.STRING), (d, v) -> d.timedIntervalDays20 = v, d -> d.timedIntervalDays20).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays21", Codec.STRING), (d, v) -> d.timedIntervalDays21 = v, d -> d.timedIntervalDays21).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays22", Codec.STRING), (d, v) -> d.timedIntervalDays22 = v, d -> d.timedIntervalDays22).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays23", Codec.STRING), (d, v) -> d.timedIntervalDays23 = v, d -> d.timedIntervalDays23).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays24", Codec.STRING), (d, v) -> d.timedIntervalDays24 = v, d -> d.timedIntervalDays24).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays25", Codec.STRING), (d, v) -> d.timedIntervalDays25 = v, d -> d.timedIntervalDays25).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays26", Codec.STRING), (d, v) -> d.timedIntervalDays26 = v, d -> d.timedIntervalDays26).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays27", Codec.STRING), (d, v) -> d.timedIntervalDays27 = v, d -> d.timedIntervalDays27).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays28", Codec.STRING), (d, v) -> d.timedIntervalDays28 = v, d -> d.timedIntervalDays28).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays29", Codec.STRING), (d, v) -> d.timedIntervalDays29 = v, d -> d.timedIntervalDays29).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays30", Codec.STRING), (d, v) -> d.timedIntervalDays30 = v, d -> d.timedIntervalDays30).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays31", Codec.STRING), (d, v) -> d.timedIntervalDays31 = v, d -> d.timedIntervalDays31).add();
            b.append(new KeyedCodec<>("@TimedIntervalDays32", Codec.STRING), (d, v) -> d.timedIntervalDays32 = v, d -> d.timedIntervalDays32).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds1", Codec.STRING), (d, v) -> d.timedIntervalSeconds1 = v, d -> d.timedIntervalSeconds1).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds2", Codec.STRING), (d, v) -> d.timedIntervalSeconds2 = v, d -> d.timedIntervalSeconds2).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds3", Codec.STRING), (d, v) -> d.timedIntervalSeconds3 = v, d -> d.timedIntervalSeconds3).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds4", Codec.STRING), (d, v) -> d.timedIntervalSeconds4 = v, d -> d.timedIntervalSeconds4).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds5", Codec.STRING), (d, v) -> d.timedIntervalSeconds5 = v, d -> d.timedIntervalSeconds5).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds6", Codec.STRING), (d, v) -> d.timedIntervalSeconds6 = v, d -> d.timedIntervalSeconds6).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds7", Codec.STRING), (d, v) -> d.timedIntervalSeconds7 = v, d -> d.timedIntervalSeconds7).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds8", Codec.STRING), (d, v) -> d.timedIntervalSeconds8 = v, d -> d.timedIntervalSeconds8).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds9", Codec.STRING), (d, v) -> d.timedIntervalSeconds9 = v, d -> d.timedIntervalSeconds9).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds10", Codec.STRING), (d, v) -> d.timedIntervalSeconds10 = v, d -> d.timedIntervalSeconds10).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds11", Codec.STRING), (d, v) -> d.timedIntervalSeconds11 = v, d -> d.timedIntervalSeconds11).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds12", Codec.STRING), (d, v) -> d.timedIntervalSeconds12 = v, d -> d.timedIntervalSeconds12).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds13", Codec.STRING), (d, v) -> d.timedIntervalSeconds13 = v, d -> d.timedIntervalSeconds13).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds14", Codec.STRING), (d, v) -> d.timedIntervalSeconds14 = v, d -> d.timedIntervalSeconds14).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds15", Codec.STRING), (d, v) -> d.timedIntervalSeconds15 = v, d -> d.timedIntervalSeconds15).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds16", Codec.STRING), (d, v) -> d.timedIntervalSeconds16 = v, d -> d.timedIntervalSeconds16).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds17", Codec.STRING), (d, v) -> d.timedIntervalSeconds17 = v, d -> d.timedIntervalSeconds17).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds18", Codec.STRING), (d, v) -> d.timedIntervalSeconds18 = v, d -> d.timedIntervalSeconds18).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds19", Codec.STRING), (d, v) -> d.timedIntervalSeconds19 = v, d -> d.timedIntervalSeconds19).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds20", Codec.STRING), (d, v) -> d.timedIntervalSeconds20 = v, d -> d.timedIntervalSeconds20).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds21", Codec.STRING), (d, v) -> d.timedIntervalSeconds21 = v, d -> d.timedIntervalSeconds21).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds22", Codec.STRING), (d, v) -> d.timedIntervalSeconds22 = v, d -> d.timedIntervalSeconds22).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds23", Codec.STRING), (d, v) -> d.timedIntervalSeconds23 = v, d -> d.timedIntervalSeconds23).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds24", Codec.STRING), (d, v) -> d.timedIntervalSeconds24 = v, d -> d.timedIntervalSeconds24).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds25", Codec.STRING), (d, v) -> d.timedIntervalSeconds25 = v, d -> d.timedIntervalSeconds25).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds26", Codec.STRING), (d, v) -> d.timedIntervalSeconds26 = v, d -> d.timedIntervalSeconds26).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds27", Codec.STRING), (d, v) -> d.timedIntervalSeconds27 = v, d -> d.timedIntervalSeconds27).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds28", Codec.STRING), (d, v) -> d.timedIntervalSeconds28 = v, d -> d.timedIntervalSeconds28).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds29", Codec.STRING), (d, v) -> d.timedIntervalSeconds29 = v, d -> d.timedIntervalSeconds29).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds30", Codec.STRING), (d, v) -> d.timedIntervalSeconds30 = v, d -> d.timedIntervalSeconds30).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds31", Codec.STRING), (d, v) -> d.timedIntervalSeconds31 = v, d -> d.timedIntervalSeconds31).add();
            b.append(new KeyedCodec<>("@TimedIntervalSeconds32", Codec.STRING), (d, v) -> d.timedIntervalSeconds32 = v, d -> d.timedIntervalSeconds32).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours1", Codec.STRING), (d, v) -> d.timedArrivalHours1 = v, d -> d.timedArrivalHours1).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours2", Codec.STRING), (d, v) -> d.timedArrivalHours2 = v, d -> d.timedArrivalHours2).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours3", Codec.STRING), (d, v) -> d.timedArrivalHours3 = v, d -> d.timedArrivalHours3).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours4", Codec.STRING), (d, v) -> d.timedArrivalHours4 = v, d -> d.timedArrivalHours4).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours5", Codec.STRING), (d, v) -> d.timedArrivalHours5 = v, d -> d.timedArrivalHours5).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours6", Codec.STRING), (d, v) -> d.timedArrivalHours6 = v, d -> d.timedArrivalHours6).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours7", Codec.STRING), (d, v) -> d.timedArrivalHours7 = v, d -> d.timedArrivalHours7).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours8", Codec.STRING), (d, v) -> d.timedArrivalHours8 = v, d -> d.timedArrivalHours8).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours9", Codec.STRING), (d, v) -> d.timedArrivalHours9 = v, d -> d.timedArrivalHours9).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours10", Codec.STRING), (d, v) -> d.timedArrivalHours10 = v, d -> d.timedArrivalHours10).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours11", Codec.STRING), (d, v) -> d.timedArrivalHours11 = v, d -> d.timedArrivalHours11).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours12", Codec.STRING), (d, v) -> d.timedArrivalHours12 = v, d -> d.timedArrivalHours12).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours13", Codec.STRING), (d, v) -> d.timedArrivalHours13 = v, d -> d.timedArrivalHours13).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours14", Codec.STRING), (d, v) -> d.timedArrivalHours14 = v, d -> d.timedArrivalHours14).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours15", Codec.STRING), (d, v) -> d.timedArrivalHours15 = v, d -> d.timedArrivalHours15).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours16", Codec.STRING), (d, v) -> d.timedArrivalHours16 = v, d -> d.timedArrivalHours16).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours17", Codec.STRING), (d, v) -> d.timedArrivalHours17 = v, d -> d.timedArrivalHours17).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours18", Codec.STRING), (d, v) -> d.timedArrivalHours18 = v, d -> d.timedArrivalHours18).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours19", Codec.STRING), (d, v) -> d.timedArrivalHours19 = v, d -> d.timedArrivalHours19).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours20", Codec.STRING), (d, v) -> d.timedArrivalHours20 = v, d -> d.timedArrivalHours20).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours21", Codec.STRING), (d, v) -> d.timedArrivalHours21 = v, d -> d.timedArrivalHours21).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours22", Codec.STRING), (d, v) -> d.timedArrivalHours22 = v, d -> d.timedArrivalHours22).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours23", Codec.STRING), (d, v) -> d.timedArrivalHours23 = v, d -> d.timedArrivalHours23).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours24", Codec.STRING), (d, v) -> d.timedArrivalHours24 = v, d -> d.timedArrivalHours24).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours25", Codec.STRING), (d, v) -> d.timedArrivalHours25 = v, d -> d.timedArrivalHours25).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours26", Codec.STRING), (d, v) -> d.timedArrivalHours26 = v, d -> d.timedArrivalHours26).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours27", Codec.STRING), (d, v) -> d.timedArrivalHours27 = v, d -> d.timedArrivalHours27).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours28", Codec.STRING), (d, v) -> d.timedArrivalHours28 = v, d -> d.timedArrivalHours28).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours29", Codec.STRING), (d, v) -> d.timedArrivalHours29 = v, d -> d.timedArrivalHours29).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours30", Codec.STRING), (d, v) -> d.timedArrivalHours30 = v, d -> d.timedArrivalHours30).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours31", Codec.STRING), (d, v) -> d.timedArrivalHours31 = v, d -> d.timedArrivalHours31).add();
            b.append(new KeyedCodec<>("@TimedArrivalHours32", Codec.STRING), (d, v) -> d.timedArrivalHours32 = v, d -> d.timedArrivalHours32).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes1", Codec.STRING), (d, v) -> d.timedArrivalMinutes1 = v, d -> d.timedArrivalMinutes1).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes2", Codec.STRING), (d, v) -> d.timedArrivalMinutes2 = v, d -> d.timedArrivalMinutes2).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes3", Codec.STRING), (d, v) -> d.timedArrivalMinutes3 = v, d -> d.timedArrivalMinutes3).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes4", Codec.STRING), (d, v) -> d.timedArrivalMinutes4 = v, d -> d.timedArrivalMinutes4).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes5", Codec.STRING), (d, v) -> d.timedArrivalMinutes5 = v, d -> d.timedArrivalMinutes5).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes6", Codec.STRING), (d, v) -> d.timedArrivalMinutes6 = v, d -> d.timedArrivalMinutes6).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes7", Codec.STRING), (d, v) -> d.timedArrivalMinutes7 = v, d -> d.timedArrivalMinutes7).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes8", Codec.STRING), (d, v) -> d.timedArrivalMinutes8 = v, d -> d.timedArrivalMinutes8).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes9", Codec.STRING), (d, v) -> d.timedArrivalMinutes9 = v, d -> d.timedArrivalMinutes9).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes10", Codec.STRING), (d, v) -> d.timedArrivalMinutes10 = v, d -> d.timedArrivalMinutes10).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes11", Codec.STRING), (d, v) -> d.timedArrivalMinutes11 = v, d -> d.timedArrivalMinutes11).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes12", Codec.STRING), (d, v) -> d.timedArrivalMinutes12 = v, d -> d.timedArrivalMinutes12).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes13", Codec.STRING), (d, v) -> d.timedArrivalMinutes13 = v, d -> d.timedArrivalMinutes13).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes14", Codec.STRING), (d, v) -> d.timedArrivalMinutes14 = v, d -> d.timedArrivalMinutes14).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes15", Codec.STRING), (d, v) -> d.timedArrivalMinutes15 = v, d -> d.timedArrivalMinutes15).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes16", Codec.STRING), (d, v) -> d.timedArrivalMinutes16 = v, d -> d.timedArrivalMinutes16).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes17", Codec.STRING), (d, v) -> d.timedArrivalMinutes17 = v, d -> d.timedArrivalMinutes17).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes18", Codec.STRING), (d, v) -> d.timedArrivalMinutes18 = v, d -> d.timedArrivalMinutes18).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes19", Codec.STRING), (d, v) -> d.timedArrivalMinutes19 = v, d -> d.timedArrivalMinutes19).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes20", Codec.STRING), (d, v) -> d.timedArrivalMinutes20 = v, d -> d.timedArrivalMinutes20).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes21", Codec.STRING), (d, v) -> d.timedArrivalMinutes21 = v, d -> d.timedArrivalMinutes21).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes22", Codec.STRING), (d, v) -> d.timedArrivalMinutes22 = v, d -> d.timedArrivalMinutes22).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes23", Codec.STRING), (d, v) -> d.timedArrivalMinutes23 = v, d -> d.timedArrivalMinutes23).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes24", Codec.STRING), (d, v) -> d.timedArrivalMinutes24 = v, d -> d.timedArrivalMinutes24).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes25", Codec.STRING), (d, v) -> d.timedArrivalMinutes25 = v, d -> d.timedArrivalMinutes25).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes26", Codec.STRING), (d, v) -> d.timedArrivalMinutes26 = v, d -> d.timedArrivalMinutes26).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes27", Codec.STRING), (d, v) -> d.timedArrivalMinutes27 = v, d -> d.timedArrivalMinutes27).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes28", Codec.STRING), (d, v) -> d.timedArrivalMinutes28 = v, d -> d.timedArrivalMinutes28).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes29", Codec.STRING), (d, v) -> d.timedArrivalMinutes29 = v, d -> d.timedArrivalMinutes29).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes30", Codec.STRING), (d, v) -> d.timedArrivalMinutes30 = v, d -> d.timedArrivalMinutes30).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes31", Codec.STRING), (d, v) -> d.timedArrivalMinutes31 = v, d -> d.timedArrivalMinutes31).add();
            b.append(new KeyedCodec<>("@TimedArrivalMinutes32", Codec.STRING), (d, v) -> d.timedArrivalMinutes32 = v, d -> d.timedArrivalMinutes32).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds1", Codec.STRING), (d, v) -> d.timedArrivalSeconds1 = v, d -> d.timedArrivalSeconds1).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds2", Codec.STRING), (d, v) -> d.timedArrivalSeconds2 = v, d -> d.timedArrivalSeconds2).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds3", Codec.STRING), (d, v) -> d.timedArrivalSeconds3 = v, d -> d.timedArrivalSeconds3).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds4", Codec.STRING), (d, v) -> d.timedArrivalSeconds4 = v, d -> d.timedArrivalSeconds4).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds5", Codec.STRING), (d, v) -> d.timedArrivalSeconds5 = v, d -> d.timedArrivalSeconds5).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds6", Codec.STRING), (d, v) -> d.timedArrivalSeconds6 = v, d -> d.timedArrivalSeconds6).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds7", Codec.STRING), (d, v) -> d.timedArrivalSeconds7 = v, d -> d.timedArrivalSeconds7).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds8", Codec.STRING), (d, v) -> d.timedArrivalSeconds8 = v, d -> d.timedArrivalSeconds8).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds9", Codec.STRING), (d, v) -> d.timedArrivalSeconds9 = v, d -> d.timedArrivalSeconds9).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds10", Codec.STRING), (d, v) -> d.timedArrivalSeconds10 = v, d -> d.timedArrivalSeconds10).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds11", Codec.STRING), (d, v) -> d.timedArrivalSeconds11 = v, d -> d.timedArrivalSeconds11).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds12", Codec.STRING), (d, v) -> d.timedArrivalSeconds12 = v, d -> d.timedArrivalSeconds12).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds13", Codec.STRING), (d, v) -> d.timedArrivalSeconds13 = v, d -> d.timedArrivalSeconds13).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds14", Codec.STRING), (d, v) -> d.timedArrivalSeconds14 = v, d -> d.timedArrivalSeconds14).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds15", Codec.STRING), (d, v) -> d.timedArrivalSeconds15 = v, d -> d.timedArrivalSeconds15).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds16", Codec.STRING), (d, v) -> d.timedArrivalSeconds16 = v, d -> d.timedArrivalSeconds16).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds17", Codec.STRING), (d, v) -> d.timedArrivalSeconds17 = v, d -> d.timedArrivalSeconds17).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds18", Codec.STRING), (d, v) -> d.timedArrivalSeconds18 = v, d -> d.timedArrivalSeconds18).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds19", Codec.STRING), (d, v) -> d.timedArrivalSeconds19 = v, d -> d.timedArrivalSeconds19).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds20", Codec.STRING), (d, v) -> d.timedArrivalSeconds20 = v, d -> d.timedArrivalSeconds20).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds21", Codec.STRING), (d, v) -> d.timedArrivalSeconds21 = v, d -> d.timedArrivalSeconds21).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds22", Codec.STRING), (d, v) -> d.timedArrivalSeconds22 = v, d -> d.timedArrivalSeconds22).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds23", Codec.STRING), (d, v) -> d.timedArrivalSeconds23 = v, d -> d.timedArrivalSeconds23).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds24", Codec.STRING), (d, v) -> d.timedArrivalSeconds24 = v, d -> d.timedArrivalSeconds24).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds25", Codec.STRING), (d, v) -> d.timedArrivalSeconds25 = v, d -> d.timedArrivalSeconds25).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds26", Codec.STRING), (d, v) -> d.timedArrivalSeconds26 = v, d -> d.timedArrivalSeconds26).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds27", Codec.STRING), (d, v) -> d.timedArrivalSeconds27 = v, d -> d.timedArrivalSeconds27).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds28", Codec.STRING), (d, v) -> d.timedArrivalSeconds28 = v, d -> d.timedArrivalSeconds28).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds29", Codec.STRING), (d, v) -> d.timedArrivalSeconds29 = v, d -> d.timedArrivalSeconds29).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds30", Codec.STRING), (d, v) -> d.timedArrivalSeconds30 = v, d -> d.timedArrivalSeconds30).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds31", Codec.STRING), (d, v) -> d.timedArrivalSeconds31 = v, d -> d.timedArrivalSeconds31).add();
            b.append(new KeyedCodec<>("@TimedArrivalSeconds32", Codec.STRING), (d, v) -> d.timedArrivalSeconds32 = v, d -> d.timedArrivalSeconds32).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer1", Codec.STRING), (d, v) -> d.timedRequirePlayer1 = v, d -> d.timedRequirePlayer1).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer2", Codec.STRING), (d, v) -> d.timedRequirePlayer2 = v, d -> d.timedRequirePlayer2).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer3", Codec.STRING), (d, v) -> d.timedRequirePlayer3 = v, d -> d.timedRequirePlayer3).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer4", Codec.STRING), (d, v) -> d.timedRequirePlayer4 = v, d -> d.timedRequirePlayer4).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer5", Codec.STRING), (d, v) -> d.timedRequirePlayer5 = v, d -> d.timedRequirePlayer5).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer6", Codec.STRING), (d, v) -> d.timedRequirePlayer6 = v, d -> d.timedRequirePlayer6).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer7", Codec.STRING), (d, v) -> d.timedRequirePlayer7 = v, d -> d.timedRequirePlayer7).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer8", Codec.STRING), (d, v) -> d.timedRequirePlayer8 = v, d -> d.timedRequirePlayer8).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer9", Codec.STRING), (d, v) -> d.timedRequirePlayer9 = v, d -> d.timedRequirePlayer9).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer10", Codec.STRING), (d, v) -> d.timedRequirePlayer10 = v, d -> d.timedRequirePlayer10).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer11", Codec.STRING), (d, v) -> d.timedRequirePlayer11 = v, d -> d.timedRequirePlayer11).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer12", Codec.STRING), (d, v) -> d.timedRequirePlayer12 = v, d -> d.timedRequirePlayer12).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer13", Codec.STRING), (d, v) -> d.timedRequirePlayer13 = v, d -> d.timedRequirePlayer13).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer14", Codec.STRING), (d, v) -> d.timedRequirePlayer14 = v, d -> d.timedRequirePlayer14).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer15", Codec.STRING), (d, v) -> d.timedRequirePlayer15 = v, d -> d.timedRequirePlayer15).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer16", Codec.STRING), (d, v) -> d.timedRequirePlayer16 = v, d -> d.timedRequirePlayer16).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer17", Codec.STRING), (d, v) -> d.timedRequirePlayer17 = v, d -> d.timedRequirePlayer17).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer18", Codec.STRING), (d, v) -> d.timedRequirePlayer18 = v, d -> d.timedRequirePlayer18).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer19", Codec.STRING), (d, v) -> d.timedRequirePlayer19 = v, d -> d.timedRequirePlayer19).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer20", Codec.STRING), (d, v) -> d.timedRequirePlayer20 = v, d -> d.timedRequirePlayer20).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer21", Codec.STRING), (d, v) -> d.timedRequirePlayer21 = v, d -> d.timedRequirePlayer21).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer22", Codec.STRING), (d, v) -> d.timedRequirePlayer22 = v, d -> d.timedRequirePlayer22).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer23", Codec.STRING), (d, v) -> d.timedRequirePlayer23 = v, d -> d.timedRequirePlayer23).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer24", Codec.STRING), (d, v) -> d.timedRequirePlayer24 = v, d -> d.timedRequirePlayer24).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer25", Codec.STRING), (d, v) -> d.timedRequirePlayer25 = v, d -> d.timedRequirePlayer25).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer26", Codec.STRING), (d, v) -> d.timedRequirePlayer26 = v, d -> d.timedRequirePlayer26).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer27", Codec.STRING), (d, v) -> d.timedRequirePlayer27 = v, d -> d.timedRequirePlayer27).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer28", Codec.STRING), (d, v) -> d.timedRequirePlayer28 = v, d -> d.timedRequirePlayer28).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer29", Codec.STRING), (d, v) -> d.timedRequirePlayer29 = v, d -> d.timedRequirePlayer29).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer30", Codec.STRING), (d, v) -> d.timedRequirePlayer30 = v, d -> d.timedRequirePlayer30).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer31", Codec.STRING), (d, v) -> d.timedRequirePlayer31 = v, d -> d.timedRequirePlayer31).add();
            b.append(new KeyedCodec<>("@TimedRequirePlayer32", Codec.STRING), (d, v) -> d.timedRequirePlayer32 = v, d -> d.timedRequirePlayer32).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal1", Codec.STRING), (d, v) -> d.timedAnnounceGlobal1 = v, d -> d.timedAnnounceGlobal1).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal2", Codec.STRING), (d, v) -> d.timedAnnounceGlobal2 = v, d -> d.timedAnnounceGlobal2).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal3", Codec.STRING), (d, v) -> d.timedAnnounceGlobal3 = v, d -> d.timedAnnounceGlobal3).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal4", Codec.STRING), (d, v) -> d.timedAnnounceGlobal4 = v, d -> d.timedAnnounceGlobal4).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal5", Codec.STRING), (d, v) -> d.timedAnnounceGlobal5 = v, d -> d.timedAnnounceGlobal5).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal6", Codec.STRING), (d, v) -> d.timedAnnounceGlobal6 = v, d -> d.timedAnnounceGlobal6).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal7", Codec.STRING), (d, v) -> d.timedAnnounceGlobal7 = v, d -> d.timedAnnounceGlobal7).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal8", Codec.STRING), (d, v) -> d.timedAnnounceGlobal8 = v, d -> d.timedAnnounceGlobal8).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal9", Codec.STRING), (d, v) -> d.timedAnnounceGlobal9 = v, d -> d.timedAnnounceGlobal9).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal10", Codec.STRING), (d, v) -> d.timedAnnounceGlobal10 = v, d -> d.timedAnnounceGlobal10).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal11", Codec.STRING), (d, v) -> d.timedAnnounceGlobal11 = v, d -> d.timedAnnounceGlobal11).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal12", Codec.STRING), (d, v) -> d.timedAnnounceGlobal12 = v, d -> d.timedAnnounceGlobal12).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal13", Codec.STRING), (d, v) -> d.timedAnnounceGlobal13 = v, d -> d.timedAnnounceGlobal13).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal14", Codec.STRING), (d, v) -> d.timedAnnounceGlobal14 = v, d -> d.timedAnnounceGlobal14).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal15", Codec.STRING), (d, v) -> d.timedAnnounceGlobal15 = v, d -> d.timedAnnounceGlobal15).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal16", Codec.STRING), (d, v) -> d.timedAnnounceGlobal16 = v, d -> d.timedAnnounceGlobal16).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal17", Codec.STRING), (d, v) -> d.timedAnnounceGlobal17 = v, d -> d.timedAnnounceGlobal17).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal18", Codec.STRING), (d, v) -> d.timedAnnounceGlobal18 = v, d -> d.timedAnnounceGlobal18).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal19", Codec.STRING), (d, v) -> d.timedAnnounceGlobal19 = v, d -> d.timedAnnounceGlobal19).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal20", Codec.STRING), (d, v) -> d.timedAnnounceGlobal20 = v, d -> d.timedAnnounceGlobal20).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal21", Codec.STRING), (d, v) -> d.timedAnnounceGlobal21 = v, d -> d.timedAnnounceGlobal21).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal22", Codec.STRING), (d, v) -> d.timedAnnounceGlobal22 = v, d -> d.timedAnnounceGlobal22).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal23", Codec.STRING), (d, v) -> d.timedAnnounceGlobal23 = v, d -> d.timedAnnounceGlobal23).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal24", Codec.STRING), (d, v) -> d.timedAnnounceGlobal24 = v, d -> d.timedAnnounceGlobal24).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal25", Codec.STRING), (d, v) -> d.timedAnnounceGlobal25 = v, d -> d.timedAnnounceGlobal25).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal26", Codec.STRING), (d, v) -> d.timedAnnounceGlobal26 = v, d -> d.timedAnnounceGlobal26).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal27", Codec.STRING), (d, v) -> d.timedAnnounceGlobal27 = v, d -> d.timedAnnounceGlobal27).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal28", Codec.STRING), (d, v) -> d.timedAnnounceGlobal28 = v, d -> d.timedAnnounceGlobal28).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal29", Codec.STRING), (d, v) -> d.timedAnnounceGlobal29 = v, d -> d.timedAnnounceGlobal29).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal30", Codec.STRING), (d, v) -> d.timedAnnounceGlobal30 = v, d -> d.timedAnnounceGlobal30).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal31", Codec.STRING), (d, v) -> d.timedAnnounceGlobal31 = v, d -> d.timedAnnounceGlobal31).add();
            b.append(new KeyedCodec<>("@TimedAnnounceGlobal32", Codec.STRING), (d, v) -> d.timedAnnounceGlobal32 = v, d -> d.timedAnnounceGlobal32).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld1", Codec.STRING), (d, v) -> d.timedAnnounceWorld1 = v, d -> d.timedAnnounceWorld1).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld2", Codec.STRING), (d, v) -> d.timedAnnounceWorld2 = v, d -> d.timedAnnounceWorld2).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld3", Codec.STRING), (d, v) -> d.timedAnnounceWorld3 = v, d -> d.timedAnnounceWorld3).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld4", Codec.STRING), (d, v) -> d.timedAnnounceWorld4 = v, d -> d.timedAnnounceWorld4).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld5", Codec.STRING), (d, v) -> d.timedAnnounceWorld5 = v, d -> d.timedAnnounceWorld5).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld6", Codec.STRING), (d, v) -> d.timedAnnounceWorld6 = v, d -> d.timedAnnounceWorld6).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld7", Codec.STRING), (d, v) -> d.timedAnnounceWorld7 = v, d -> d.timedAnnounceWorld7).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld8", Codec.STRING), (d, v) -> d.timedAnnounceWorld8 = v, d -> d.timedAnnounceWorld8).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld9", Codec.STRING), (d, v) -> d.timedAnnounceWorld9 = v, d -> d.timedAnnounceWorld9).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld10", Codec.STRING), (d, v) -> d.timedAnnounceWorld10 = v, d -> d.timedAnnounceWorld10).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld11", Codec.STRING), (d, v) -> d.timedAnnounceWorld11 = v, d -> d.timedAnnounceWorld11).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld12", Codec.STRING), (d, v) -> d.timedAnnounceWorld12 = v, d -> d.timedAnnounceWorld12).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld13", Codec.STRING), (d, v) -> d.timedAnnounceWorld13 = v, d -> d.timedAnnounceWorld13).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld14", Codec.STRING), (d, v) -> d.timedAnnounceWorld14 = v, d -> d.timedAnnounceWorld14).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld15", Codec.STRING), (d, v) -> d.timedAnnounceWorld15 = v, d -> d.timedAnnounceWorld15).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld16", Codec.STRING), (d, v) -> d.timedAnnounceWorld16 = v, d -> d.timedAnnounceWorld16).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld17", Codec.STRING), (d, v) -> d.timedAnnounceWorld17 = v, d -> d.timedAnnounceWorld17).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld18", Codec.STRING), (d, v) -> d.timedAnnounceWorld18 = v, d -> d.timedAnnounceWorld18).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld19", Codec.STRING), (d, v) -> d.timedAnnounceWorld19 = v, d -> d.timedAnnounceWorld19).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld20", Codec.STRING), (d, v) -> d.timedAnnounceWorld20 = v, d -> d.timedAnnounceWorld20).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld21", Codec.STRING), (d, v) -> d.timedAnnounceWorld21 = v, d -> d.timedAnnounceWorld21).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld22", Codec.STRING), (d, v) -> d.timedAnnounceWorld22 = v, d -> d.timedAnnounceWorld22).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld23", Codec.STRING), (d, v) -> d.timedAnnounceWorld23 = v, d -> d.timedAnnounceWorld23).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld24", Codec.STRING), (d, v) -> d.timedAnnounceWorld24 = v, d -> d.timedAnnounceWorld24).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld25", Codec.STRING), (d, v) -> d.timedAnnounceWorld25 = v, d -> d.timedAnnounceWorld25).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld26", Codec.STRING), (d, v) -> d.timedAnnounceWorld26 = v, d -> d.timedAnnounceWorld26).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld27", Codec.STRING), (d, v) -> d.timedAnnounceWorld27 = v, d -> d.timedAnnounceWorld27).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld28", Codec.STRING), (d, v) -> d.timedAnnounceWorld28 = v, d -> d.timedAnnounceWorld28).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld29", Codec.STRING), (d, v) -> d.timedAnnounceWorld29 = v, d -> d.timedAnnounceWorld29).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld30", Codec.STRING), (d, v) -> d.timedAnnounceWorld30 = v, d -> d.timedAnnounceWorld30).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld31", Codec.STRING), (d, v) -> d.timedAnnounceWorld31 = v, d -> d.timedAnnounceWorld31).add();
            b.append(new KeyedCodec<>("@TimedAnnounceWorld32", Codec.STRING), (d, v) -> d.timedAnnounceWorld32 = v, d -> d.timedAnnounceWorld32).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier1", Codec.STRING), (d, v) -> d.timedAnnounceMinTier1 = v, d -> d.timedAnnounceMinTier1).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier2", Codec.STRING), (d, v) -> d.timedAnnounceMinTier2 = v, d -> d.timedAnnounceMinTier2).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier3", Codec.STRING), (d, v) -> d.timedAnnounceMinTier3 = v, d -> d.timedAnnounceMinTier3).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier4", Codec.STRING), (d, v) -> d.timedAnnounceMinTier4 = v, d -> d.timedAnnounceMinTier4).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier5", Codec.STRING), (d, v) -> d.timedAnnounceMinTier5 = v, d -> d.timedAnnounceMinTier5).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier6", Codec.STRING), (d, v) -> d.timedAnnounceMinTier6 = v, d -> d.timedAnnounceMinTier6).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier7", Codec.STRING), (d, v) -> d.timedAnnounceMinTier7 = v, d -> d.timedAnnounceMinTier7).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier8", Codec.STRING), (d, v) -> d.timedAnnounceMinTier8 = v, d -> d.timedAnnounceMinTier8).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier9", Codec.STRING), (d, v) -> d.timedAnnounceMinTier9 = v, d -> d.timedAnnounceMinTier9).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier10", Codec.STRING), (d, v) -> d.timedAnnounceMinTier10 = v, d -> d.timedAnnounceMinTier10).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier11", Codec.STRING), (d, v) -> d.timedAnnounceMinTier11 = v, d -> d.timedAnnounceMinTier11).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier12", Codec.STRING), (d, v) -> d.timedAnnounceMinTier12 = v, d -> d.timedAnnounceMinTier12).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier13", Codec.STRING), (d, v) -> d.timedAnnounceMinTier13 = v, d -> d.timedAnnounceMinTier13).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier14", Codec.STRING), (d, v) -> d.timedAnnounceMinTier14 = v, d -> d.timedAnnounceMinTier14).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier15", Codec.STRING), (d, v) -> d.timedAnnounceMinTier15 = v, d -> d.timedAnnounceMinTier15).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier16", Codec.STRING), (d, v) -> d.timedAnnounceMinTier16 = v, d -> d.timedAnnounceMinTier16).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier17", Codec.STRING), (d, v) -> d.timedAnnounceMinTier17 = v, d -> d.timedAnnounceMinTier17).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier18", Codec.STRING), (d, v) -> d.timedAnnounceMinTier18 = v, d -> d.timedAnnounceMinTier18).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier19", Codec.STRING), (d, v) -> d.timedAnnounceMinTier19 = v, d -> d.timedAnnounceMinTier19).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier20", Codec.STRING), (d, v) -> d.timedAnnounceMinTier20 = v, d -> d.timedAnnounceMinTier20).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier21", Codec.STRING), (d, v) -> d.timedAnnounceMinTier21 = v, d -> d.timedAnnounceMinTier21).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier22", Codec.STRING), (d, v) -> d.timedAnnounceMinTier22 = v, d -> d.timedAnnounceMinTier22).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier23", Codec.STRING), (d, v) -> d.timedAnnounceMinTier23 = v, d -> d.timedAnnounceMinTier23).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier24", Codec.STRING), (d, v) -> d.timedAnnounceMinTier24 = v, d -> d.timedAnnounceMinTier24).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier25", Codec.STRING), (d, v) -> d.timedAnnounceMinTier25 = v, d -> d.timedAnnounceMinTier25).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier26", Codec.STRING), (d, v) -> d.timedAnnounceMinTier26 = v, d -> d.timedAnnounceMinTier26).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier27", Codec.STRING), (d, v) -> d.timedAnnounceMinTier27 = v, d -> d.timedAnnounceMinTier27).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier28", Codec.STRING), (d, v) -> d.timedAnnounceMinTier28 = v, d -> d.timedAnnounceMinTier28).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier29", Codec.STRING), (d, v) -> d.timedAnnounceMinTier29 = v, d -> d.timedAnnounceMinTier29).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier30", Codec.STRING), (d, v) -> d.timedAnnounceMinTier30 = v, d -> d.timedAnnounceMinTier30).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier31", Codec.STRING), (d, v) -> d.timedAnnounceMinTier31 = v, d -> d.timedAnnounceMinTier31).add();
            b.append(new KeyedCodec<>("@TimedAnnounceMinTier32", Codec.STRING), (d, v) -> d.timedAnnounceMinTier32 = v, d -> d.timedAnnounceMinTier32).add();
            b.append(new KeyedCodec<>("@TimedAnnounceText", Codec.STRING), (d, v) -> d.timedAnnounceText = v, d -> d.timedAnnounceText).add();
            b.append(new KeyedCodec<>("@TimedReminderText", Codec.STRING), (d, v) -> d.timedReminderText = v, d -> d.timedReminderText).add();
            b.append(new KeyedCodec<>("@TimedGraceText", Codec.STRING), (d, v) -> d.timedGraceText = v, d -> d.timedGraceText).add();
            b.append(new KeyedCodec<>("@BossPoolPick", Codec.STRING), (d, v) -> d.bossPoolPick = v, d -> d.bossPoolPick).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled1", Codec.STRING), (d, v) -> d.timedGraceEnabled1 = v, d -> d.timedGraceEnabled1).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled2", Codec.STRING), (d, v) -> d.timedGraceEnabled2 = v, d -> d.timedGraceEnabled2).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled3", Codec.STRING), (d, v) -> d.timedGraceEnabled3 = v, d -> d.timedGraceEnabled3).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled4", Codec.STRING), (d, v) -> d.timedGraceEnabled4 = v, d -> d.timedGraceEnabled4).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled5", Codec.STRING), (d, v) -> d.timedGraceEnabled5 = v, d -> d.timedGraceEnabled5).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled6", Codec.STRING), (d, v) -> d.timedGraceEnabled6 = v, d -> d.timedGraceEnabled6).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled7", Codec.STRING), (d, v) -> d.timedGraceEnabled7 = v, d -> d.timedGraceEnabled7).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled8", Codec.STRING), (d, v) -> d.timedGraceEnabled8 = v, d -> d.timedGraceEnabled8).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled9", Codec.STRING), (d, v) -> d.timedGraceEnabled9 = v, d -> d.timedGraceEnabled9).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled10", Codec.STRING), (d, v) -> d.timedGraceEnabled10 = v, d -> d.timedGraceEnabled10).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled11", Codec.STRING), (d, v) -> d.timedGraceEnabled11 = v, d -> d.timedGraceEnabled11).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled12", Codec.STRING), (d, v) -> d.timedGraceEnabled12 = v, d -> d.timedGraceEnabled12).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled13", Codec.STRING), (d, v) -> d.timedGraceEnabled13 = v, d -> d.timedGraceEnabled13).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled14", Codec.STRING), (d, v) -> d.timedGraceEnabled14 = v, d -> d.timedGraceEnabled14).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled15", Codec.STRING), (d, v) -> d.timedGraceEnabled15 = v, d -> d.timedGraceEnabled15).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled16", Codec.STRING), (d, v) -> d.timedGraceEnabled16 = v, d -> d.timedGraceEnabled16).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled17", Codec.STRING), (d, v) -> d.timedGraceEnabled17 = v, d -> d.timedGraceEnabled17).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled18", Codec.STRING), (d, v) -> d.timedGraceEnabled18 = v, d -> d.timedGraceEnabled18).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled19", Codec.STRING), (d, v) -> d.timedGraceEnabled19 = v, d -> d.timedGraceEnabled19).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled20", Codec.STRING), (d, v) -> d.timedGraceEnabled20 = v, d -> d.timedGraceEnabled20).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled21", Codec.STRING), (d, v) -> d.timedGraceEnabled21 = v, d -> d.timedGraceEnabled21).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled22", Codec.STRING), (d, v) -> d.timedGraceEnabled22 = v, d -> d.timedGraceEnabled22).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled23", Codec.STRING), (d, v) -> d.timedGraceEnabled23 = v, d -> d.timedGraceEnabled23).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled24", Codec.STRING), (d, v) -> d.timedGraceEnabled24 = v, d -> d.timedGraceEnabled24).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled25", Codec.STRING), (d, v) -> d.timedGraceEnabled25 = v, d -> d.timedGraceEnabled25).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled26", Codec.STRING), (d, v) -> d.timedGraceEnabled26 = v, d -> d.timedGraceEnabled26).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled27", Codec.STRING), (d, v) -> d.timedGraceEnabled27 = v, d -> d.timedGraceEnabled27).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled28", Codec.STRING), (d, v) -> d.timedGraceEnabled28 = v, d -> d.timedGraceEnabled28).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled29", Codec.STRING), (d, v) -> d.timedGraceEnabled29 = v, d -> d.timedGraceEnabled29).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled30", Codec.STRING), (d, v) -> d.timedGraceEnabled30 = v, d -> d.timedGraceEnabled30).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled31", Codec.STRING), (d, v) -> d.timedGraceEnabled31 = v, d -> d.timedGraceEnabled31).add();
            b.append(new KeyedCodec<>("@TimedGraceEnabled32", Codec.STRING), (d, v) -> d.timedGraceEnabled32 = v, d -> d.timedGraceEnabled32).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds1", Codec.STRING), (d, v) -> d.timedGraceSeconds1 = v, d -> d.timedGraceSeconds1).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds2", Codec.STRING), (d, v) -> d.timedGraceSeconds2 = v, d -> d.timedGraceSeconds2).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds3", Codec.STRING), (d, v) -> d.timedGraceSeconds3 = v, d -> d.timedGraceSeconds3).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds4", Codec.STRING), (d, v) -> d.timedGraceSeconds4 = v, d -> d.timedGraceSeconds4).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds5", Codec.STRING), (d, v) -> d.timedGraceSeconds5 = v, d -> d.timedGraceSeconds5).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds6", Codec.STRING), (d, v) -> d.timedGraceSeconds6 = v, d -> d.timedGraceSeconds6).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds7", Codec.STRING), (d, v) -> d.timedGraceSeconds7 = v, d -> d.timedGraceSeconds7).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds8", Codec.STRING), (d, v) -> d.timedGraceSeconds8 = v, d -> d.timedGraceSeconds8).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds9", Codec.STRING), (d, v) -> d.timedGraceSeconds9 = v, d -> d.timedGraceSeconds9).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds10", Codec.STRING), (d, v) -> d.timedGraceSeconds10 = v, d -> d.timedGraceSeconds10).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds11", Codec.STRING), (d, v) -> d.timedGraceSeconds11 = v, d -> d.timedGraceSeconds11).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds12", Codec.STRING), (d, v) -> d.timedGraceSeconds12 = v, d -> d.timedGraceSeconds12).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds13", Codec.STRING), (d, v) -> d.timedGraceSeconds13 = v, d -> d.timedGraceSeconds13).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds14", Codec.STRING), (d, v) -> d.timedGraceSeconds14 = v, d -> d.timedGraceSeconds14).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds15", Codec.STRING), (d, v) -> d.timedGraceSeconds15 = v, d -> d.timedGraceSeconds15).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds16", Codec.STRING), (d, v) -> d.timedGraceSeconds16 = v, d -> d.timedGraceSeconds16).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds17", Codec.STRING), (d, v) -> d.timedGraceSeconds17 = v, d -> d.timedGraceSeconds17).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds18", Codec.STRING), (d, v) -> d.timedGraceSeconds18 = v, d -> d.timedGraceSeconds18).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds19", Codec.STRING), (d, v) -> d.timedGraceSeconds19 = v, d -> d.timedGraceSeconds19).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds20", Codec.STRING), (d, v) -> d.timedGraceSeconds20 = v, d -> d.timedGraceSeconds20).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds21", Codec.STRING), (d, v) -> d.timedGraceSeconds21 = v, d -> d.timedGraceSeconds21).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds22", Codec.STRING), (d, v) -> d.timedGraceSeconds22 = v, d -> d.timedGraceSeconds22).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds23", Codec.STRING), (d, v) -> d.timedGraceSeconds23 = v, d -> d.timedGraceSeconds23).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds24", Codec.STRING), (d, v) -> d.timedGraceSeconds24 = v, d -> d.timedGraceSeconds24).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds25", Codec.STRING), (d, v) -> d.timedGraceSeconds25 = v, d -> d.timedGraceSeconds25).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds26", Codec.STRING), (d, v) -> d.timedGraceSeconds26 = v, d -> d.timedGraceSeconds26).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds27", Codec.STRING), (d, v) -> d.timedGraceSeconds27 = v, d -> d.timedGraceSeconds27).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds28", Codec.STRING), (d, v) -> d.timedGraceSeconds28 = v, d -> d.timedGraceSeconds28).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds29", Codec.STRING), (d, v) -> d.timedGraceSeconds29 = v, d -> d.timedGraceSeconds29).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds30", Codec.STRING), (d, v) -> d.timedGraceSeconds30 = v, d -> d.timedGraceSeconds30).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds31", Codec.STRING), (d, v) -> d.timedGraceSeconds31 = v, d -> d.timedGraceSeconds31).add();
            b.append(new KeyedCodec<>("@TimedGraceSeconds32", Codec.STRING), (d, v) -> d.timedGraceSeconds32 = v, d -> d.timedGraceSeconds32).add();
            b.append(new KeyedCodec<>("@BossLootName1", Codec.STRING), (d, v) -> d.bossLootName1 = v, d -> d.bossLootName1).add();
            b.append(new KeyedCodec<>("@BossLootMin1", Codec.STRING), (d, v) -> d.bossLootMin1 = v, d -> d.bossLootMin1).add();
            b.append(new KeyedCodec<>("@BossLootMax1", Codec.STRING), (d, v) -> d.bossLootMax1 = v, d -> d.bossLootMax1).add();
            b.append(new KeyedCodec<>("@BossLootChance1", Codec.STRING), (d, v) -> d.bossLootChance1 = v, d -> d.bossLootChance1).add();
            b.append(new KeyedCodec<>("@BossLootName2", Codec.STRING), (d, v) -> d.bossLootName2 = v, d -> d.bossLootName2).add();
            b.append(new KeyedCodec<>("@BossLootMin2", Codec.STRING), (d, v) -> d.bossLootMin2 = v, d -> d.bossLootMin2).add();
            b.append(new KeyedCodec<>("@BossLootMax2", Codec.STRING), (d, v) -> d.bossLootMax2 = v, d -> d.bossLootMax2).add();
            b.append(new KeyedCodec<>("@BossLootChance2", Codec.STRING), (d, v) -> d.bossLootChance2 = v, d -> d.bossLootChance2).add();
            b.append(new KeyedCodec<>("@BossLootName3", Codec.STRING), (d, v) -> d.bossLootName3 = v, d -> d.bossLootName3).add();
            b.append(new KeyedCodec<>("@BossLootMin3", Codec.STRING), (d, v) -> d.bossLootMin3 = v, d -> d.bossLootMin3).add();
            b.append(new KeyedCodec<>("@BossLootMax3", Codec.STRING), (d, v) -> d.bossLootMax3 = v, d -> d.bossLootMax3).add();
            b.append(new KeyedCodec<>("@BossLootChance3", Codec.STRING), (d, v) -> d.bossLootChance3 = v, d -> d.bossLootChance3).add();
            b.append(new KeyedCodec<>("@BossLootName4", Codec.STRING), (d, v) -> d.bossLootName4 = v, d -> d.bossLootName4).add();
            b.append(new KeyedCodec<>("@BossLootMin4", Codec.STRING), (d, v) -> d.bossLootMin4 = v, d -> d.bossLootMin4).add();
            b.append(new KeyedCodec<>("@BossLootMax4", Codec.STRING), (d, v) -> d.bossLootMax4 = v, d -> d.bossLootMax4).add();
            b.append(new KeyedCodec<>("@BossLootChance4", Codec.STRING), (d, v) -> d.bossLootChance4 = v, d -> d.bossLootChance4).add();
            b.append(new KeyedCodec<>("@BossLootName5", Codec.STRING), (d, v) -> d.bossLootName5 = v, d -> d.bossLootName5).add();
            b.append(new KeyedCodec<>("@BossLootMin5", Codec.STRING), (d, v) -> d.bossLootMin5 = v, d -> d.bossLootMin5).add();
            b.append(new KeyedCodec<>("@BossLootMax5", Codec.STRING), (d, v) -> d.bossLootMax5 = v, d -> d.bossLootMax5).add();
            b.append(new KeyedCodec<>("@BossLootChance5", Codec.STRING), (d, v) -> d.bossLootChance5 = v, d -> d.bossLootChance5).add();
            b.append(new KeyedCodec<>("@BossLootName6", Codec.STRING), (d, v) -> d.bossLootName6 = v, d -> d.bossLootName6).add();
            b.append(new KeyedCodec<>("@BossLootMin6", Codec.STRING), (d, v) -> d.bossLootMin6 = v, d -> d.bossLootMin6).add();
            b.append(new KeyedCodec<>("@BossLootMax6", Codec.STRING), (d, v) -> d.bossLootMax6 = v, d -> d.bossLootMax6).add();
            b.append(new KeyedCodec<>("@BossLootChance6", Codec.STRING), (d, v) -> d.bossLootChance6 = v, d -> d.bossLootChance6).add();
            b.append(new KeyedCodec<>("@BossLootName7", Codec.STRING), (d, v) -> d.bossLootName7 = v, d -> d.bossLootName7).add();
            b.append(new KeyedCodec<>("@BossLootMin7", Codec.STRING), (d, v) -> d.bossLootMin7 = v, d -> d.bossLootMin7).add();
            b.append(new KeyedCodec<>("@BossLootMax7", Codec.STRING), (d, v) -> d.bossLootMax7 = v, d -> d.bossLootMax7).add();
            b.append(new KeyedCodec<>("@BossLootChance7", Codec.STRING), (d, v) -> d.bossLootChance7 = v, d -> d.bossLootChance7).add();
            b.append(new KeyedCodec<>("@BossLootName8", Codec.STRING), (d, v) -> d.bossLootName8 = v, d -> d.bossLootName8).add();
            b.append(new KeyedCodec<>("@BossLootMin8", Codec.STRING), (d, v) -> d.bossLootMin8 = v, d -> d.bossLootMin8).add();
            b.append(new KeyedCodec<>("@BossLootMax8", Codec.STRING), (d, v) -> d.bossLootMax8 = v, d -> d.bossLootMax8).add();
            b.append(new KeyedCodec<>("@BossLootChance8", Codec.STRING), (d, v) -> d.bossLootChance8 = v, d -> d.bossLootChance8).add();
            return b.build();
        }
        public String action;
        public String arenaName;
        /** Radius of the edited arena row; the row index comes from the action token. */
        public String arenaRadius;
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
        public Float bossWaveMobMult;
        public String bossWavesEnabled;
        public String bossEditMusic;
        public String bossEditMusicRadius;
        public String bossWaveTimeSec;
        public String bossWaveNpc1;
        public String bossWaveAmount1;
        public String bossWaveAmountMin1;
        public String bossWaveAmountMax1;
        public String bossWaveEvery1;
        public String bossWaveHp1;
        public String bossWaveDamage1;
        public String bossWaveSize1;
        public String bossWaveNpc2;
        public String bossWaveAmount2;
        public String bossWaveAmountMin2;
        public String bossWaveAmountMax2;
        public String bossWaveEvery2;
        public String bossWaveHp2;
        public String bossWaveDamage2;
        public String bossWaveSize2;
        public String bossWaveNpc3;
        public String bossWaveAmount3;
        public String bossWaveAmountMin3;
        public String bossWaveAmountMax3;
        public String bossWaveEvery3;
        public String bossWaveHp3;
        public String bossWaveDamage3;
        public String bossWaveSize3;
        public String bossWaveNpc4;
        public String bossWaveAmount4;
        public String bossWaveAmountMin4;
        public String bossWaveAmountMax4;
        public String bossWaveEvery4;
        public String bossWaveHp4;
        public String bossWaveDamage4;
        public String bossWaveSize4;
        public String bossWaveNpc5;
        public String bossWaveAmount5;
        public String bossWaveAmountMin5;
        public String bossWaveAmountMax5;
        public String bossWaveEvery5;
        public String bossWaveHp5;
        public String bossWaveDamage5;
        public String bossWaveSize5;
        public String bossWaveNpc6;
        public String bossWaveNpc7;
        public String bossWaveNpc8;
        public String bossWaveNpc9;
        public String bossWaveNpc10;
        public String bossWaveNpc11;
        public String bossWaveNpc12;
        public String bossWaveNpc13;
        public String bossWaveNpc14;
        public String bossWaveNpc15;
        public String bossWaveNpc16;
        public String bossWaveNpc17;
        public String bossWaveNpc18;
        public String bossWaveNpc19;
        public String bossWaveNpc20;
        public String bossWaveNpc21;
        public String bossWaveNpc22;
        public String bossWaveNpc23;
        public String bossWaveNpc24;
        public String bossWaveNpc25;
        public String bossWaveNpc26;
        public String bossWaveNpc27;
        public String bossWaveNpc28;
        public String bossWaveNpc29;
        public String bossWaveNpc30;
        public String bossWaveNpc31;
        public String bossWaveNpc32;
        public String bossWaveAmount6;
        public String bossWaveAmount7;
        public String bossWaveAmount8;
        public String bossWaveAmount9;
        public String bossWaveAmount10;
        public String bossWaveAmount11;
        public String bossWaveAmount12;
        public String bossWaveAmount13;
        public String bossWaveAmount14;
        public String bossWaveAmount15;
        public String bossWaveAmount16;
        public String bossWaveAmount17;
        public String bossWaveAmount18;
        public String bossWaveAmount19;
        public String bossWaveAmount20;
        public String bossWaveAmount21;
        public String bossWaveAmount22;
        public String bossWaveAmount23;
        public String bossWaveAmount24;
        public String bossWaveAmount25;
        public String bossWaveAmount26;
        public String bossWaveAmount27;
        public String bossWaveAmount28;
        public String bossWaveAmount29;
        public String bossWaveAmount30;
        public String bossWaveAmount31;
        public String bossWaveAmount32;
        public String bossWaveAmountMin6;
        public String bossWaveAmountMin7;
        public String bossWaveAmountMin8;
        public String bossWaveAmountMin9;
        public String bossWaveAmountMin10;
        public String bossWaveAmountMin11;
        public String bossWaveAmountMin12;
        public String bossWaveAmountMin13;
        public String bossWaveAmountMin14;
        public String bossWaveAmountMin15;
        public String bossWaveAmountMin16;
        public String bossWaveAmountMin17;
        public String bossWaveAmountMin18;
        public String bossWaveAmountMin19;
        public String bossWaveAmountMin20;
        public String bossWaveAmountMin21;
        public String bossWaveAmountMin22;
        public String bossWaveAmountMin23;
        public String bossWaveAmountMin24;
        public String bossWaveAmountMin25;
        public String bossWaveAmountMin26;
        public String bossWaveAmountMin27;
        public String bossWaveAmountMin28;
        public String bossWaveAmountMin29;
        public String bossWaveAmountMin30;
        public String bossWaveAmountMin31;
        public String bossWaveAmountMin32;
        public String bossWaveAmountMax6;
        public String bossWaveAmountMax7;
        public String bossWaveAmountMax8;
        public String bossWaveAmountMax9;
        public String bossWaveAmountMax10;
        public String bossWaveAmountMax11;
        public String bossWaveAmountMax12;
        public String bossWaveAmountMax13;
        public String bossWaveAmountMax14;
        public String bossWaveAmountMax15;
        public String bossWaveAmountMax16;
        public String bossWaveAmountMax17;
        public String bossWaveAmountMax18;
        public String bossWaveAmountMax19;
        public String bossWaveAmountMax20;
        public String bossWaveAmountMax21;
        public String bossWaveAmountMax22;
        public String bossWaveAmountMax23;
        public String bossWaveAmountMax24;
        public String bossWaveAmountMax25;
        public String bossWaveAmountMax26;
        public String bossWaveAmountMax27;
        public String bossWaveAmountMax28;
        public String bossWaveAmountMax29;
        public String bossWaveAmountMax30;
        public String bossWaveAmountMax31;
        public String bossWaveAmountMax32;
        public String bossWaveEvery6;
        public String bossWaveEvery7;
        public String bossWaveEvery8;
        public String bossWaveEvery9;
        public String bossWaveEvery10;
        public String bossWaveEvery11;
        public String bossWaveEvery12;
        public String bossWaveEvery13;
        public String bossWaveEvery14;
        public String bossWaveEvery15;
        public String bossWaveEvery16;
        public String bossWaveEvery17;
        public String bossWaveEvery18;
        public String bossWaveEvery19;
        public String bossWaveEvery20;
        public String bossWaveEvery21;
        public String bossWaveEvery22;
        public String bossWaveEvery23;
        public String bossWaveEvery24;
        public String bossWaveEvery25;
        public String bossWaveEvery26;
        public String bossWaveEvery27;
        public String bossWaveEvery28;
        public String bossWaveEvery29;
        public String bossWaveEvery30;
        public String bossWaveEvery31;
        public String bossWaveEvery32;
        public String bossWaveHp6;
        public String bossWaveHp7;
        public String bossWaveHp8;
        public String bossWaveHp9;
        public String bossWaveHp10;
        public String bossWaveHp11;
        public String bossWaveHp12;
        public String bossWaveHp13;
        public String bossWaveHp14;
        public String bossWaveHp15;
        public String bossWaveHp16;
        public String bossWaveHp17;
        public String bossWaveHp18;
        public String bossWaveHp19;
        public String bossWaveHp20;
        public String bossWaveHp21;
        public String bossWaveHp22;
        public String bossWaveHp23;
        public String bossWaveHp24;
        public String bossWaveHp25;
        public String bossWaveHp26;
        public String bossWaveHp27;
        public String bossWaveHp28;
        public String bossWaveHp29;
        public String bossWaveHp30;
        public String bossWaveHp31;
        public String bossWaveHp32;
        public String bossWaveDamage6;
        public String bossWaveDamage7;
        public String bossWaveDamage8;
        public String bossWaveDamage9;
        public String bossWaveDamage10;
        public String bossWaveDamage11;
        public String bossWaveDamage12;
        public String bossWaveDamage13;
        public String bossWaveDamage14;
        public String bossWaveDamage15;
        public String bossWaveDamage16;
        public String bossWaveDamage17;
        public String bossWaveDamage18;
        public String bossWaveDamage19;
        public String bossWaveDamage20;
        public String bossWaveDamage21;
        public String bossWaveDamage22;
        public String bossWaveDamage23;
        public String bossWaveDamage24;
        public String bossWaveDamage25;
        public String bossWaveDamage26;
        public String bossWaveDamage27;
        public String bossWaveDamage28;
        public String bossWaveDamage29;
        public String bossWaveDamage30;
        public String bossWaveDamage31;
        public String bossWaveDamage32;
        public String bossWaveSize6;
        public String bossWaveSize7;
        public String bossWaveSize8;
        public String bossWaveSize9;
        public String bossWaveSize10;
        public String bossWaveSize11;
        public String bossWaveSize12;
        public String bossWaveSize13;
        public String bossWaveSize14;
        public String bossWaveSize15;
        public String bossWaveSize16;
        public String bossWaveSize17;
        public String bossWaveSize18;
        public String bossWaveSize19;
        public String bossWaveSize20;
        public String bossWaveSize21;
        public String bossWaveSize22;
        public String bossWaveSize23;
        public String bossWaveSize24;
        public String bossWaveSize25;
        public String bossWaveSize26;
        public String bossWaveSize27;
        public String bossWaveSize28;
        public String bossWaveSize29;
        public String bossWaveSize30;
        public String bossWaveSize31;
        public String bossWaveSize32;
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
        public String bossWaveValue7;
        public String bossWaveValue8;
        public String bossWaveValue9;
        public String bossWaveValue10;
        public String bossWaveValue11;
        public String bossWaveValue12;
        public String bossWaveValue13;
        public String bossWaveValue14;
        public String bossWaveValue15;
        public String bossWaveValue16;
        public String bossWaveValue17;
        public String bossWaveValue18;
        public String bossWaveValue19;
        public String bossWaveValue20;
        public String bossWaveValue21;
        public String bossWaveValue22;
        public String bossWaveValue23;
        public String bossWaveValue24;
        public String bossWaveValue25;
        public String bossWaveValue26;
        public String bossWaveValue27;
        public String bossWaveValue28;
        public String bossWaveValue29;
        public String bossWaveValue30;
        public String bossWaveValue31;
        public String bossWaveValue32;
        public String bossWaveRepeatCount6;
        public String bossWaveRepeatCount7;
        public String bossWaveRepeatCount8;
        public String bossWaveRepeatCount9;
        public String bossWaveRepeatCount10;
        public String bossWaveRepeatCount11;
        public String bossWaveRepeatCount12;
        public String bossWaveRepeatCount13;
        public String bossWaveRepeatCount14;
        public String bossWaveRepeatCount15;
        public String bossWaveRepeatCount16;
        public String bossWaveRepeatCount17;
        public String bossWaveRepeatCount18;
        public String bossWaveRepeatCount19;
        public String bossWaveRepeatCount20;
        public String bossWaveRepeatCount21;
        public String bossWaveRepeatCount22;
        public String bossWaveRepeatCount23;
        public String bossWaveRepeatCount24;
        public String bossWaveRepeatCount25;
        public String bossWaveRepeatCount26;
        public String bossWaveRepeatCount27;
        public String bossWaveRepeatCount28;
        public String bossWaveRepeatCount29;
        public String bossWaveRepeatCount30;
        public String bossWaveRepeatCount31;
        public String bossWaveRepeatCount32;
        public String bossWaveRepeatSec6;
        public String bossWaveRepeatSec7;
        public String bossWaveRepeatSec8;
        public String bossWaveRepeatSec9;
        public String bossWaveRepeatSec10;
        public String bossWaveRepeatSec11;
        public String bossWaveRepeatSec12;
        public String bossWaveRepeatSec13;
        public String bossWaveRepeatSec14;
        public String bossWaveRepeatSec15;
        public String bossWaveRepeatSec16;
        public String bossWaveRepeatSec17;
        public String bossWaveRepeatSec18;
        public String bossWaveRepeatSec19;
        public String bossWaveRepeatSec20;
        public String bossWaveRepeatSec21;
        public String bossWaveRepeatSec22;
        public String bossWaveRepeatSec23;
        public String bossWaveRepeatSec24;
        public String bossWaveRepeatSec25;
        public String bossWaveRepeatSec26;
        public String bossWaveRepeatSec27;
        public String bossWaveRepeatSec28;
        public String bossWaveRepeatSec29;
        public String bossWaveRepeatSec30;
        public String bossWaveRepeatSec31;
        public String bossWaveRepeatSec32;
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
        public String timedGraceEnabled7;
        public String timedGraceEnabled8;
        public String timedGraceEnabled9;
        public String timedGraceEnabled10;
        public String timedGraceEnabled11;
        public String timedGraceEnabled12;
        public String timedGraceEnabled13;
        public String timedGraceEnabled14;
        public String timedGraceEnabled15;
        public String timedGraceEnabled16;
        public String timedGraceEnabled17;
        public String timedGraceEnabled18;
        public String timedGraceEnabled19;
        public String timedGraceEnabled20;
        public String timedGraceEnabled21;
        public String timedGraceEnabled22;
        public String timedGraceEnabled23;
        public String timedGraceEnabled24;
        public String timedGraceEnabled25;
        public String timedGraceEnabled26;
        public String timedGraceEnabled27;
        public String timedGraceEnabled28;
        public String timedGraceEnabled29;
        public String timedGraceEnabled30;
        public String timedGraceEnabled31;
        public String timedGraceEnabled32;
        public String timedGraceSeconds1;
        public String timedGraceSeconds2;
        public String timedGraceSeconds3;
        public String timedGraceSeconds4;
        public String timedGraceSeconds5;
        public String timedGraceSeconds6;
        public String timedGraceSeconds7;
        public String timedGraceSeconds8;
        public String timedGraceSeconds9;
        public String timedGraceSeconds10;
        public String timedGraceSeconds11;
        public String timedGraceSeconds12;
        public String timedGraceSeconds13;
        public String timedGraceSeconds14;
        public String timedGraceSeconds15;
        public String timedGraceSeconds16;
        public String timedGraceSeconds17;
        public String timedGraceSeconds18;
        public String timedGraceSeconds19;
        public String timedGraceSeconds20;
        public String timedGraceSeconds21;
        public String timedGraceSeconds22;
        public String timedGraceSeconds23;
        public String timedGraceSeconds24;
        public String timedGraceSeconds25;
        public String timedGraceSeconds26;
        public String timedGraceSeconds27;
        public String timedGraceSeconds28;
        public String timedGraceSeconds29;
        public String timedGraceSeconds30;
        public String timedGraceSeconds31;
        public String timedGraceSeconds32;
        public String timedAnnounceGlobal1;
        public String timedAnnounceGlobal2;
        public String timedAnnounceGlobal3;
        public String timedAnnounceGlobal4;
        public String timedAnnounceGlobal5;
        public String timedAnnounceGlobal6;
        public String timedAnnounceGlobal7;
        public String timedAnnounceGlobal8;
        public String timedAnnounceGlobal9;
        public String timedAnnounceGlobal10;
        public String timedAnnounceGlobal11;
        public String timedAnnounceGlobal12;
        public String timedAnnounceGlobal13;
        public String timedAnnounceGlobal14;
        public String timedAnnounceGlobal15;
        public String timedAnnounceGlobal16;
        public String timedAnnounceGlobal17;
        public String timedAnnounceGlobal18;
        public String timedAnnounceGlobal19;
        public String timedAnnounceGlobal20;
        public String timedAnnounceGlobal21;
        public String timedAnnounceGlobal22;
        public String timedAnnounceGlobal23;
        public String timedAnnounceGlobal24;
        public String timedAnnounceGlobal25;
        public String timedAnnounceGlobal26;
        public String timedAnnounceGlobal27;
        public String timedAnnounceGlobal28;
        public String timedAnnounceGlobal29;
        public String timedAnnounceGlobal30;
        public String timedAnnounceGlobal31;
        public String timedAnnounceGlobal32;
        public String timedAnnounceWorld1;
        public String timedAnnounceWorld2;
        public String timedAnnounceWorld3;
        public String timedAnnounceWorld4;
        public String timedAnnounceWorld5;
        public String timedAnnounceWorld6;
        public String timedAnnounceWorld7;
        public String timedAnnounceWorld8;
        public String timedAnnounceWorld9;
        public String timedAnnounceWorld10;
        public String timedAnnounceWorld11;
        public String timedAnnounceWorld12;
        public String timedAnnounceWorld13;
        public String timedAnnounceWorld14;
        public String timedAnnounceWorld15;
        public String timedAnnounceWorld16;
        public String timedAnnounceWorld17;
        public String timedAnnounceWorld18;
        public String timedAnnounceWorld19;
        public String timedAnnounceWorld20;
        public String timedAnnounceWorld21;
        public String timedAnnounceWorld22;
        public String timedAnnounceWorld23;
        public String timedAnnounceWorld24;
        public String timedAnnounceWorld25;
        public String timedAnnounceWorld26;
        public String timedAnnounceWorld27;
        public String timedAnnounceWorld28;
        public String timedAnnounceWorld29;
        public String timedAnnounceWorld30;
        public String timedAnnounceWorld31;
        public String timedAnnounceWorld32;
        public String timedAnnounceMinTier1;
        public String timedAnnounceMinTier2;
        public String timedAnnounceMinTier3;
        public String timedAnnounceMinTier4;
        public String timedAnnounceMinTier5;
        public String timedAnnounceMinTier6;
        public String timedAnnounceMinTier7;
        public String timedAnnounceMinTier8;
        public String timedAnnounceMinTier9;
        public String timedAnnounceMinTier10;
        public String timedAnnounceMinTier11;
        public String timedAnnounceMinTier12;
        public String timedAnnounceMinTier13;
        public String timedAnnounceMinTier14;
        public String timedAnnounceMinTier15;
        public String timedAnnounceMinTier16;
        public String timedAnnounceMinTier17;
        public String timedAnnounceMinTier18;
        public String timedAnnounceMinTier19;
        public String timedAnnounceMinTier20;
        public String timedAnnounceMinTier21;
        public String timedAnnounceMinTier22;
        public String timedAnnounceMinTier23;
        public String timedAnnounceMinTier24;
        public String timedAnnounceMinTier25;
        public String timedAnnounceMinTier26;
        public String timedAnnounceMinTier27;
        public String timedAnnounceMinTier28;
        public String timedAnnounceMinTier29;
        public String timedAnnounceMinTier30;
        public String timedAnnounceMinTier31;
        public String timedAnnounceMinTier32;
        public String timedMinPlayers1;
        public String timedMinPlayers2;
        public String timedMinPlayers3;
        public String timedMinPlayers4;
        public String timedMinPlayers5;
        public String timedMinPlayers6;
        public String timedMinPlayers7;
        public String timedMinPlayers8;
        public String timedMinPlayers9;
        public String timedMinPlayers10;
        public String timedMinPlayers11;
        public String timedMinPlayers12;
        public String timedMinPlayers13;
        public String timedMinPlayers14;
        public String timedMinPlayers15;
        public String timedMinPlayers16;
        public String timedMinPlayers17;
        public String timedMinPlayers18;
        public String timedMinPlayers19;
        public String timedMinPlayers20;
        public String timedMinPlayers21;
        public String timedMinPlayers22;
        public String timedMinPlayers23;
        public String timedMinPlayers24;
        public String timedMinPlayers25;
        public String timedMinPlayers26;
        public String timedMinPlayers27;
        public String timedMinPlayers28;
        public String timedMinPlayers29;
        public String timedMinPlayers30;
        public String timedMinPlayers31;
        public String timedMinPlayers32;
        public String timedEverySeconds1;
        public String timedEverySeconds2;
        public String timedEverySeconds3;
        public String timedEverySeconds4;
        public String timedEverySeconds5;
        public String timedEverySeconds6;
        public String timedEverySeconds7;
        public String timedEverySeconds8;
        public String timedEverySeconds9;
        public String timedEverySeconds10;
        public String timedEverySeconds11;
        public String timedEverySeconds12;
        public String timedEverySeconds13;
        public String timedEverySeconds14;
        public String timedEverySeconds15;
        public String timedEverySeconds16;
        public String timedEverySeconds17;
        public String timedEverySeconds18;
        public String timedEverySeconds19;
        public String timedEverySeconds20;
        public String timedEverySeconds21;
        public String timedEverySeconds22;
        public String timedEverySeconds23;
        public String timedEverySeconds24;
        public String timedEverySeconds25;
        public String timedEverySeconds26;
        public String timedEverySeconds27;
        public String timedEverySeconds28;
        public String timedEverySeconds29;
        public String timedEverySeconds30;
        public String timedEverySeconds31;
        public String timedEverySeconds32;
        public String timedIntervalHours1;
        public String timedIntervalHours2;
        public String timedIntervalHours3;
        public String timedIntervalHours4;
        public String timedIntervalHours5;
        public String timedIntervalHours6;
        public String timedIntervalHours7;
        public String timedIntervalHours8;
        public String timedIntervalHours9;
        public String timedIntervalHours10;
        public String timedIntervalHours11;
        public String timedIntervalHours12;
        public String timedIntervalHours13;
        public String timedIntervalHours14;
        public String timedIntervalHours15;
        public String timedIntervalHours16;
        public String timedIntervalHours17;
        public String timedIntervalHours18;
        public String timedIntervalHours19;
        public String timedIntervalHours20;
        public String timedIntervalHours21;
        public String timedIntervalHours22;
        public String timedIntervalHours23;
        public String timedIntervalHours24;
        public String timedIntervalHours25;
        public String timedIntervalHours26;
        public String timedIntervalHours27;
        public String timedIntervalHours28;
        public String timedIntervalHours29;
        public String timedIntervalHours30;
        public String timedIntervalHours31;
        public String timedIntervalHours32;
        public String timedIntervalDays1;
        public String timedIntervalDays2;
        public String timedIntervalDays3;
        public String timedIntervalDays4;
        public String timedIntervalDays5;
        public String timedIntervalDays6;
        public String timedIntervalDays7;
        public String timedIntervalDays8;
        public String timedIntervalDays9;
        public String timedIntervalDays10;
        public String timedIntervalDays11;
        public String timedIntervalDays12;
        public String timedIntervalDays13;
        public String timedIntervalDays14;
        public String timedIntervalDays15;
        public String timedIntervalDays16;
        public String timedIntervalDays17;
        public String timedIntervalDays18;
        public String timedIntervalDays19;
        public String timedIntervalDays20;
        public String timedIntervalDays21;
        public String timedIntervalDays22;
        public String timedIntervalDays23;
        public String timedIntervalDays24;
        public String timedIntervalDays25;
        public String timedIntervalDays26;
        public String timedIntervalDays27;
        public String timedIntervalDays28;
        public String timedIntervalDays29;
        public String timedIntervalDays30;
        public String timedIntervalDays31;
        public String timedIntervalDays32;
        public String timedIntervalSeconds1;
        public String timedIntervalSeconds2;
        public String timedIntervalSeconds3;
        public String timedIntervalSeconds4;
        public String timedIntervalSeconds5;
        public String timedIntervalSeconds6;
        public String timedIntervalSeconds7;
        public String timedIntervalSeconds8;
        public String timedIntervalSeconds9;
        public String timedIntervalSeconds10;
        public String timedIntervalSeconds11;
        public String timedIntervalSeconds12;
        public String timedIntervalSeconds13;
        public String timedIntervalSeconds14;
        public String timedIntervalSeconds15;
        public String timedIntervalSeconds16;
        public String timedIntervalSeconds17;
        public String timedIntervalSeconds18;
        public String timedIntervalSeconds19;
        public String timedIntervalSeconds20;
        public String timedIntervalSeconds21;
        public String timedIntervalSeconds22;
        public String timedIntervalSeconds23;
        public String timedIntervalSeconds24;
        public String timedIntervalSeconds25;
        public String timedIntervalSeconds26;
        public String timedIntervalSeconds27;
        public String timedIntervalSeconds28;
        public String timedIntervalSeconds29;
        public String timedIntervalSeconds30;
        public String timedIntervalSeconds31;
        public String timedIntervalSeconds32;
        public String timedArrivalHours1;
        public String timedArrivalHours2;
        public String timedArrivalHours3;
        public String timedArrivalHours4;
        public String timedArrivalHours5;
        public String timedArrivalHours6;
        public String timedArrivalHours7;
        public String timedArrivalHours8;
        public String timedArrivalHours9;
        public String timedArrivalHours10;
        public String timedArrivalHours11;
        public String timedArrivalHours12;
        public String timedArrivalHours13;
        public String timedArrivalHours14;
        public String timedArrivalHours15;
        public String timedArrivalHours16;
        public String timedArrivalHours17;
        public String timedArrivalHours18;
        public String timedArrivalHours19;
        public String timedArrivalHours20;
        public String timedArrivalHours21;
        public String timedArrivalHours22;
        public String timedArrivalHours23;
        public String timedArrivalHours24;
        public String timedArrivalHours25;
        public String timedArrivalHours26;
        public String timedArrivalHours27;
        public String timedArrivalHours28;
        public String timedArrivalHours29;
        public String timedArrivalHours30;
        public String timedArrivalHours31;
        public String timedArrivalHours32;
        public String timedArrivalMinutes1;
        public String timedArrivalMinutes2;
        public String timedArrivalMinutes3;
        public String timedArrivalMinutes4;
        public String timedArrivalMinutes5;
        public String timedArrivalMinutes6;
        public String timedArrivalMinutes7;
        public String timedArrivalMinutes8;
        public String timedArrivalMinutes9;
        public String timedArrivalMinutes10;
        public String timedArrivalMinutes11;
        public String timedArrivalMinutes12;
        public String timedArrivalMinutes13;
        public String timedArrivalMinutes14;
        public String timedArrivalMinutes15;
        public String timedArrivalMinutes16;
        public String timedArrivalMinutes17;
        public String timedArrivalMinutes18;
        public String timedArrivalMinutes19;
        public String timedArrivalMinutes20;
        public String timedArrivalMinutes21;
        public String timedArrivalMinutes22;
        public String timedArrivalMinutes23;
        public String timedArrivalMinutes24;
        public String timedArrivalMinutes25;
        public String timedArrivalMinutes26;
        public String timedArrivalMinutes27;
        public String timedArrivalMinutes28;
        public String timedArrivalMinutes29;
        public String timedArrivalMinutes30;
        public String timedArrivalMinutes31;
        public String timedArrivalMinutes32;
        public String timedArrivalSeconds1;
        public String timedArrivalSeconds2;
        public String timedArrivalSeconds3;
        public String timedArrivalSeconds4;
        public String timedArrivalSeconds5;
        public String timedArrivalSeconds6;
        public String timedArrivalSeconds7;
        public String timedArrivalSeconds8;
        public String timedArrivalSeconds9;
        public String timedArrivalSeconds10;
        public String timedArrivalSeconds11;
        public String timedArrivalSeconds12;
        public String timedArrivalSeconds13;
        public String timedArrivalSeconds14;
        public String timedArrivalSeconds15;
        public String timedArrivalSeconds16;
        public String timedArrivalSeconds17;
        public String timedArrivalSeconds18;
        public String timedArrivalSeconds19;
        public String timedArrivalSeconds20;
        public String timedArrivalSeconds21;
        public String timedArrivalSeconds22;
        public String timedArrivalSeconds23;
        public String timedArrivalSeconds24;
        public String timedArrivalSeconds25;
        public String timedArrivalSeconds26;
        public String timedArrivalSeconds27;
        public String timedArrivalSeconds28;
        public String timedArrivalSeconds29;
        public String timedArrivalSeconds30;
        public String timedArrivalSeconds31;
        public String timedArrivalSeconds32;
        public String timedRequirePlayer1;
        public String timedRequirePlayer2;
        public String timedRequirePlayer3;
        public String timedRequirePlayer4;
        public String timedRequirePlayer5;
        public String timedRequirePlayer6;
        public String timedRequirePlayer7;
        public String timedRequirePlayer8;
        public String timedRequirePlayer9;
        public String timedRequirePlayer10;
        public String timedRequirePlayer11;
        public String timedRequirePlayer12;
        public String timedRequirePlayer13;
        public String timedRequirePlayer14;
        public String timedRequirePlayer15;
        public String timedRequirePlayer16;
        public String timedRequirePlayer17;
        public String timedRequirePlayer18;
        public String timedRequirePlayer19;
        public String timedRequirePlayer20;
        public String timedRequirePlayer21;
        public String timedRequirePlayer22;
        public String timedRequirePlayer23;
        public String timedRequirePlayer24;
        public String timedRequirePlayer25;
        public String timedRequirePlayer26;
        public String timedRequirePlayer27;
        public String timedRequirePlayer28;
        public String timedRequirePlayer29;
        public String timedRequirePlayer30;
        public String timedRequirePlayer31;
        public String timedRequirePlayer32;
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
                case 7 -> bossWaveNpc7;
                case 8 -> bossWaveNpc8;
                case 9 -> bossWaveNpc9;
                case 10 -> bossWaveNpc10;
                case 11 -> bossWaveNpc11;
                case 12 -> bossWaveNpc12;
                case 13 -> bossWaveNpc13;
                case 14 -> bossWaveNpc14;
                case 15 -> bossWaveNpc15;
                case 16 -> bossWaveNpc16;
                case 17 -> bossWaveNpc17;
                case 18 -> bossWaveNpc18;
                case 19 -> bossWaveNpc19;
                case 20 -> bossWaveNpc20;
                case 21 -> bossWaveNpc21;
                case 22 -> bossWaveNpc22;
                case 23 -> bossWaveNpc23;
                case 24 -> bossWaveNpc24;
                case 25 -> bossWaveNpc25;
                case 26 -> bossWaveNpc26;
                case 27 -> bossWaveNpc27;
                case 28 -> bossWaveNpc28;
                case 29 -> bossWaveNpc29;
                case 30 -> bossWaveNpc30;
                case 31 -> bossWaveNpc31;
                case 32 -> bossWaveNpc32;
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
                case 7 -> bossWaveAmount7;
                case 8 -> bossWaveAmount8;
                case 9 -> bossWaveAmount9;
                case 10 -> bossWaveAmount10;
                case 11 -> bossWaveAmount11;
                case 12 -> bossWaveAmount12;
                case 13 -> bossWaveAmount13;
                case 14 -> bossWaveAmount14;
                case 15 -> bossWaveAmount15;
                case 16 -> bossWaveAmount16;
                case 17 -> bossWaveAmount17;
                case 18 -> bossWaveAmount18;
                case 19 -> bossWaveAmount19;
                case 20 -> bossWaveAmount20;
                case 21 -> bossWaveAmount21;
                case 22 -> bossWaveAmount22;
                case 23 -> bossWaveAmount23;
                case 24 -> bossWaveAmount24;
                case 25 -> bossWaveAmount25;
                case 26 -> bossWaveAmount26;
                case 27 -> bossWaveAmount27;
                case 28 -> bossWaveAmount28;
                case 29 -> bossWaveAmount29;
                case 30 -> bossWaveAmount30;
                case 31 -> bossWaveAmount31;
                case 32 -> bossWaveAmount32;
                default -> "";
            };
        }

        public String getBossWaveAmountMin(int row) {
            return switch (row) {
                case 1 -> bossWaveAmountMin1;
                case 2 -> bossWaveAmountMin2;
                case 3 -> bossWaveAmountMin3;
                case 4 -> bossWaveAmountMin4;
                case 5 -> bossWaveAmountMin5;
                case 6 -> bossWaveAmountMin6;
                case 7 -> bossWaveAmountMin7;
                case 8 -> bossWaveAmountMin8;
                case 9 -> bossWaveAmountMin9;
                case 10 -> bossWaveAmountMin10;
                case 11 -> bossWaveAmountMin11;
                case 12 -> bossWaveAmountMin12;
                case 13 -> bossWaveAmountMin13;
                case 14 -> bossWaveAmountMin14;
                case 15 -> bossWaveAmountMin15;
                case 16 -> bossWaveAmountMin16;
                case 17 -> bossWaveAmountMin17;
                case 18 -> bossWaveAmountMin18;
                case 19 -> bossWaveAmountMin19;
                case 20 -> bossWaveAmountMin20;
                case 21 -> bossWaveAmountMin21;
                case 22 -> bossWaveAmountMin22;
                case 23 -> bossWaveAmountMin23;
                case 24 -> bossWaveAmountMin24;
                case 25 -> bossWaveAmountMin25;
                case 26 -> bossWaveAmountMin26;
                case 27 -> bossWaveAmountMin27;
                case 28 -> bossWaveAmountMin28;
                case 29 -> bossWaveAmountMin29;
                case 30 -> bossWaveAmountMin30;
                case 31 -> bossWaveAmountMin31;
                case 32 -> bossWaveAmountMin32;
                default -> "";
            };
        }

        public String getBossWaveAmountMax(int row) {
            return switch (row) {
                case 1 -> bossWaveAmountMax1;
                case 2 -> bossWaveAmountMax2;
                case 3 -> bossWaveAmountMax3;
                case 4 -> bossWaveAmountMax4;
                case 5 -> bossWaveAmountMax5;
                case 6 -> bossWaveAmountMax6;
                case 7 -> bossWaveAmountMax7;
                case 8 -> bossWaveAmountMax8;
                case 9 -> bossWaveAmountMax9;
                case 10 -> bossWaveAmountMax10;
                case 11 -> bossWaveAmountMax11;
                case 12 -> bossWaveAmountMax12;
                case 13 -> bossWaveAmountMax13;
                case 14 -> bossWaveAmountMax14;
                case 15 -> bossWaveAmountMax15;
                case 16 -> bossWaveAmountMax16;
                case 17 -> bossWaveAmountMax17;
                case 18 -> bossWaveAmountMax18;
                case 19 -> bossWaveAmountMax19;
                case 20 -> bossWaveAmountMax20;
                case 21 -> bossWaveAmountMax21;
                case 22 -> bossWaveAmountMax22;
                case 23 -> bossWaveAmountMax23;
                case 24 -> bossWaveAmountMax24;
                case 25 -> bossWaveAmountMax25;
                case 26 -> bossWaveAmountMax26;
                case 27 -> bossWaveAmountMax27;
                case 28 -> bossWaveAmountMax28;
                case 29 -> bossWaveAmountMax29;
                case 30 -> bossWaveAmountMax30;
                case 31 -> bossWaveAmountMax31;
                case 32 -> bossWaveAmountMax32;
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
                case 7 -> timedMinPlayers7;
                case 8 -> timedMinPlayers8;
                case 9 -> timedMinPlayers9;
                case 10 -> timedMinPlayers10;
                case 11 -> timedMinPlayers11;
                case 12 -> timedMinPlayers12;
                case 13 -> timedMinPlayers13;
                case 14 -> timedMinPlayers14;
                case 15 -> timedMinPlayers15;
                case 16 -> timedMinPlayers16;
                case 17 -> timedMinPlayers17;
                case 18 -> timedMinPlayers18;
                case 19 -> timedMinPlayers19;
                case 20 -> timedMinPlayers20;
                case 21 -> timedMinPlayers21;
                case 22 -> timedMinPlayers22;
                case 23 -> timedMinPlayers23;
                case 24 -> timedMinPlayers24;
                case 25 -> timedMinPlayers25;
                case 26 -> timedMinPlayers26;
                case 27 -> timedMinPlayers27;
                case 28 -> timedMinPlayers28;
                case 29 -> timedMinPlayers29;
                case 30 -> timedMinPlayers30;
                case 31 -> timedMinPlayers31;
                case 32 -> timedMinPlayers32;
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
                case 7 -> timedEverySeconds7;
                case 8 -> timedEverySeconds8;
                case 9 -> timedEverySeconds9;
                case 10 -> timedEverySeconds10;
                case 11 -> timedEverySeconds11;
                case 12 -> timedEverySeconds12;
                case 13 -> timedEverySeconds13;
                case 14 -> timedEverySeconds14;
                case 15 -> timedEverySeconds15;
                case 16 -> timedEverySeconds16;
                case 17 -> timedEverySeconds17;
                case 18 -> timedEverySeconds18;
                case 19 -> timedEverySeconds19;
                case 20 -> timedEverySeconds20;
                case 21 -> timedEverySeconds21;
                case 22 -> timedEverySeconds22;
                case 23 -> timedEverySeconds23;
                case 24 -> timedEverySeconds24;
                case 25 -> timedEverySeconds25;
                case 26 -> timedEverySeconds26;
                case 27 -> timedEverySeconds27;
                case 28 -> timedEverySeconds28;
                case 29 -> timedEverySeconds29;
                case 30 -> timedEverySeconds30;
                case 31 -> timedEverySeconds31;
                case 32 -> timedEverySeconds32;
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
                case 7 -> timedIntervalHours7;
                case 8 -> timedIntervalHours8;
                case 9 -> timedIntervalHours9;
                case 10 -> timedIntervalHours10;
                case 11 -> timedIntervalHours11;
                case 12 -> timedIntervalHours12;
                case 13 -> timedIntervalHours13;
                case 14 -> timedIntervalHours14;
                case 15 -> timedIntervalHours15;
                case 16 -> timedIntervalHours16;
                case 17 -> timedIntervalHours17;
                case 18 -> timedIntervalHours18;
                case 19 -> timedIntervalHours19;
                case 20 -> timedIntervalHours20;
                case 21 -> timedIntervalHours21;
                case 22 -> timedIntervalHours22;
                case 23 -> timedIntervalHours23;
                case 24 -> timedIntervalHours24;
                case 25 -> timedIntervalHours25;
                case 26 -> timedIntervalHours26;
                case 27 -> timedIntervalHours27;
                case 28 -> timedIntervalHours28;
                case 29 -> timedIntervalHours29;
                case 30 -> timedIntervalHours30;
                case 31 -> timedIntervalHours31;
                case 32 -> timedIntervalHours32;
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
                case 7 -> timedIntervalDays7;
                case 8 -> timedIntervalDays8;
                case 9 -> timedIntervalDays9;
                case 10 -> timedIntervalDays10;
                case 11 -> timedIntervalDays11;
                case 12 -> timedIntervalDays12;
                case 13 -> timedIntervalDays13;
                case 14 -> timedIntervalDays14;
                case 15 -> timedIntervalDays15;
                case 16 -> timedIntervalDays16;
                case 17 -> timedIntervalDays17;
                case 18 -> timedIntervalDays18;
                case 19 -> timedIntervalDays19;
                case 20 -> timedIntervalDays20;
                case 21 -> timedIntervalDays21;
                case 22 -> timedIntervalDays22;
                case 23 -> timedIntervalDays23;
                case 24 -> timedIntervalDays24;
                case 25 -> timedIntervalDays25;
                case 26 -> timedIntervalDays26;
                case 27 -> timedIntervalDays27;
                case 28 -> timedIntervalDays28;
                case 29 -> timedIntervalDays29;
                case 30 -> timedIntervalDays30;
                case 31 -> timedIntervalDays31;
                case 32 -> timedIntervalDays32;
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
                case 7 -> timedIntervalSeconds7;
                case 8 -> timedIntervalSeconds8;
                case 9 -> timedIntervalSeconds9;
                case 10 -> timedIntervalSeconds10;
                case 11 -> timedIntervalSeconds11;
                case 12 -> timedIntervalSeconds12;
                case 13 -> timedIntervalSeconds13;
                case 14 -> timedIntervalSeconds14;
                case 15 -> timedIntervalSeconds15;
                case 16 -> timedIntervalSeconds16;
                case 17 -> timedIntervalSeconds17;
                case 18 -> timedIntervalSeconds18;
                case 19 -> timedIntervalSeconds19;
                case 20 -> timedIntervalSeconds20;
                case 21 -> timedIntervalSeconds21;
                case 22 -> timedIntervalSeconds22;
                case 23 -> timedIntervalSeconds23;
                case 24 -> timedIntervalSeconds24;
                case 25 -> timedIntervalSeconds25;
                case 26 -> timedIntervalSeconds26;
                case 27 -> timedIntervalSeconds27;
                case 28 -> timedIntervalSeconds28;
                case 29 -> timedIntervalSeconds29;
                case 30 -> timedIntervalSeconds30;
                case 31 -> timedIntervalSeconds31;
                case 32 -> timedIntervalSeconds32;
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
                case 7 -> timedArrivalHours7;
                case 8 -> timedArrivalHours8;
                case 9 -> timedArrivalHours9;
                case 10 -> timedArrivalHours10;
                case 11 -> timedArrivalHours11;
                case 12 -> timedArrivalHours12;
                case 13 -> timedArrivalHours13;
                case 14 -> timedArrivalHours14;
                case 15 -> timedArrivalHours15;
                case 16 -> timedArrivalHours16;
                case 17 -> timedArrivalHours17;
                case 18 -> timedArrivalHours18;
                case 19 -> timedArrivalHours19;
                case 20 -> timedArrivalHours20;
                case 21 -> timedArrivalHours21;
                case 22 -> timedArrivalHours22;
                case 23 -> timedArrivalHours23;
                case 24 -> timedArrivalHours24;
                case 25 -> timedArrivalHours25;
                case 26 -> timedArrivalHours26;
                case 27 -> timedArrivalHours27;
                case 28 -> timedArrivalHours28;
                case 29 -> timedArrivalHours29;
                case 30 -> timedArrivalHours30;
                case 31 -> timedArrivalHours31;
                case 32 -> timedArrivalHours32;
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
                case 7 -> timedArrivalMinutes7;
                case 8 -> timedArrivalMinutes8;
                case 9 -> timedArrivalMinutes9;
                case 10 -> timedArrivalMinutes10;
                case 11 -> timedArrivalMinutes11;
                case 12 -> timedArrivalMinutes12;
                case 13 -> timedArrivalMinutes13;
                case 14 -> timedArrivalMinutes14;
                case 15 -> timedArrivalMinutes15;
                case 16 -> timedArrivalMinutes16;
                case 17 -> timedArrivalMinutes17;
                case 18 -> timedArrivalMinutes18;
                case 19 -> timedArrivalMinutes19;
                case 20 -> timedArrivalMinutes20;
                case 21 -> timedArrivalMinutes21;
                case 22 -> timedArrivalMinutes22;
                case 23 -> timedArrivalMinutes23;
                case 24 -> timedArrivalMinutes24;
                case 25 -> timedArrivalMinutes25;
                case 26 -> timedArrivalMinutes26;
                case 27 -> timedArrivalMinutes27;
                case 28 -> timedArrivalMinutes28;
                case 29 -> timedArrivalMinutes29;
                case 30 -> timedArrivalMinutes30;
                case 31 -> timedArrivalMinutes31;
                case 32 -> timedArrivalMinutes32;
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
                case 7 -> timedArrivalSeconds7;
                case 8 -> timedArrivalSeconds8;
                case 9 -> timedArrivalSeconds9;
                case 10 -> timedArrivalSeconds10;
                case 11 -> timedArrivalSeconds11;
                case 12 -> timedArrivalSeconds12;
                case 13 -> timedArrivalSeconds13;
                case 14 -> timedArrivalSeconds14;
                case 15 -> timedArrivalSeconds15;
                case 16 -> timedArrivalSeconds16;
                case 17 -> timedArrivalSeconds17;
                case 18 -> timedArrivalSeconds18;
                case 19 -> timedArrivalSeconds19;
                case 20 -> timedArrivalSeconds20;
                case 21 -> timedArrivalSeconds21;
                case 22 -> timedArrivalSeconds22;
                case 23 -> timedArrivalSeconds23;
                case 24 -> timedArrivalSeconds24;
                case 25 -> timedArrivalSeconds25;
                case 26 -> timedArrivalSeconds26;
                case 27 -> timedArrivalSeconds27;
                case 28 -> timedArrivalSeconds28;
                case 29 -> timedArrivalSeconds29;
                case 30 -> timedArrivalSeconds30;
                case 31 -> timedArrivalSeconds31;
                case 32 -> timedArrivalSeconds32;
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
                case 7 -> timedRequirePlayer7;
                case 8 -> timedRequirePlayer8;
                case 9 -> timedRequirePlayer9;
                case 10 -> timedRequirePlayer10;
                case 11 -> timedRequirePlayer11;
                case 12 -> timedRequirePlayer12;
                case 13 -> timedRequirePlayer13;
                case 14 -> timedRequirePlayer14;
                case 15 -> timedRequirePlayer15;
                case 16 -> timedRequirePlayer16;
                case 17 -> timedRequirePlayer17;
                case 18 -> timedRequirePlayer18;
                case 19 -> timedRequirePlayer19;
                case 20 -> timedRequirePlayer20;
                case 21 -> timedRequirePlayer21;
                case 22 -> timedRequirePlayer22;
                case 23 -> timedRequirePlayer23;
                case 24 -> timedRequirePlayer24;
                case 25 -> timedRequirePlayer25;
                case 26 -> timedRequirePlayer26;
                case 27 -> timedRequirePlayer27;
                case 28 -> timedRequirePlayer28;
                case 29 -> timedRequirePlayer29;
                case 30 -> timedRequirePlayer30;
                case 31 -> timedRequirePlayer31;
                case 32 -> timedRequirePlayer32;
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
                case 7 -> timedGraceEnabled7;
                case 8 -> timedGraceEnabled8;
                case 9 -> timedGraceEnabled9;
                case 10 -> timedGraceEnabled10;
                case 11 -> timedGraceEnabled11;
                case 12 -> timedGraceEnabled12;
                case 13 -> timedGraceEnabled13;
                case 14 -> timedGraceEnabled14;
                case 15 -> timedGraceEnabled15;
                case 16 -> timedGraceEnabled16;
                case 17 -> timedGraceEnabled17;
                case 18 -> timedGraceEnabled18;
                case 19 -> timedGraceEnabled19;
                case 20 -> timedGraceEnabled20;
                case 21 -> timedGraceEnabled21;
                case 22 -> timedGraceEnabled22;
                case 23 -> timedGraceEnabled23;
                case 24 -> timedGraceEnabled24;
                case 25 -> timedGraceEnabled25;
                case 26 -> timedGraceEnabled26;
                case 27 -> timedGraceEnabled27;
                case 28 -> timedGraceEnabled28;
                case 29 -> timedGraceEnabled29;
                case 30 -> timedGraceEnabled30;
                case 31 -> timedGraceEnabled31;
                case 32 -> timedGraceEnabled32;
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
                case 7 -> timedGraceSeconds7;
                case 8 -> timedGraceSeconds8;
                case 9 -> timedGraceSeconds9;
                case 10 -> timedGraceSeconds10;
                case 11 -> timedGraceSeconds11;
                case 12 -> timedGraceSeconds12;
                case 13 -> timedGraceSeconds13;
                case 14 -> timedGraceSeconds14;
                case 15 -> timedGraceSeconds15;
                case 16 -> timedGraceSeconds16;
                case 17 -> timedGraceSeconds17;
                case 18 -> timedGraceSeconds18;
                case 19 -> timedGraceSeconds19;
                case 20 -> timedGraceSeconds20;
                case 21 -> timedGraceSeconds21;
                case 22 -> timedGraceSeconds22;
                case 23 -> timedGraceSeconds23;
                case 24 -> timedGraceSeconds24;
                case 25 -> timedGraceSeconds25;
                case 26 -> timedGraceSeconds26;
                case 27 -> timedGraceSeconds27;
                case 28 -> timedGraceSeconds28;
                case 29 -> timedGraceSeconds29;
                case 30 -> timedGraceSeconds30;
                case 31 -> timedGraceSeconds31;
                case 32 -> timedGraceSeconds32;
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
                case 7 -> timedAnnounceGlobal7;
                case 8 -> timedAnnounceGlobal8;
                case 9 -> timedAnnounceGlobal9;
                case 10 -> timedAnnounceGlobal10;
                case 11 -> timedAnnounceGlobal11;
                case 12 -> timedAnnounceGlobal12;
                case 13 -> timedAnnounceGlobal13;
                case 14 -> timedAnnounceGlobal14;
                case 15 -> timedAnnounceGlobal15;
                case 16 -> timedAnnounceGlobal16;
                case 17 -> timedAnnounceGlobal17;
                case 18 -> timedAnnounceGlobal18;
                case 19 -> timedAnnounceGlobal19;
                case 20 -> timedAnnounceGlobal20;
                case 21 -> timedAnnounceGlobal21;
                case 22 -> timedAnnounceGlobal22;
                case 23 -> timedAnnounceGlobal23;
                case 24 -> timedAnnounceGlobal24;
                case 25 -> timedAnnounceGlobal25;
                case 26 -> timedAnnounceGlobal26;
                case 27 -> timedAnnounceGlobal27;
                case 28 -> timedAnnounceGlobal28;
                case 29 -> timedAnnounceGlobal29;
                case 30 -> timedAnnounceGlobal30;
                case 31 -> timedAnnounceGlobal31;
                case 32 -> timedAnnounceGlobal32;
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
                case 7 -> timedAnnounceWorld7;
                case 8 -> timedAnnounceWorld8;
                case 9 -> timedAnnounceWorld9;
                case 10 -> timedAnnounceWorld10;
                case 11 -> timedAnnounceWorld11;
                case 12 -> timedAnnounceWorld12;
                case 13 -> timedAnnounceWorld13;
                case 14 -> timedAnnounceWorld14;
                case 15 -> timedAnnounceWorld15;
                case 16 -> timedAnnounceWorld16;
                case 17 -> timedAnnounceWorld17;
                case 18 -> timedAnnounceWorld18;
                case 19 -> timedAnnounceWorld19;
                case 20 -> timedAnnounceWorld20;
                case 21 -> timedAnnounceWorld21;
                case 22 -> timedAnnounceWorld22;
                case 23 -> timedAnnounceWorld23;
                case 24 -> timedAnnounceWorld24;
                case 25 -> timedAnnounceWorld25;
                case 26 -> timedAnnounceWorld26;
                case 27 -> timedAnnounceWorld27;
                case 28 -> timedAnnounceWorld28;
                case 29 -> timedAnnounceWorld29;
                case 30 -> timedAnnounceWorld30;
                case 31 -> timedAnnounceWorld31;
                case 32 -> timedAnnounceWorld32;
                default -> null;
            };
        }

        public String getTimedAnnounceMinTier(int row) {
            return switch (row) {
                case 1 -> timedAnnounceMinTier1;
                case 2 -> timedAnnounceMinTier2;
                case 3 -> timedAnnounceMinTier3;
                case 4 -> timedAnnounceMinTier4;
                case 5 -> timedAnnounceMinTier5;
                case 6 -> timedAnnounceMinTier6;
                case 7 -> timedAnnounceMinTier7;
                case 8 -> timedAnnounceMinTier8;
                case 9 -> timedAnnounceMinTier9;
                case 10 -> timedAnnounceMinTier10;
                case 11 -> timedAnnounceMinTier11;
                case 12 -> timedAnnounceMinTier12;
                case 13 -> timedAnnounceMinTier13;
                case 14 -> timedAnnounceMinTier14;
                case 15 -> timedAnnounceMinTier15;
                case 16 -> timedAnnounceMinTier16;
                case 17 -> timedAnnounceMinTier17;
                case 18 -> timedAnnounceMinTier18;
                case 19 -> timedAnnounceMinTier19;
                case 20 -> timedAnnounceMinTier20;
                case 21 -> timedAnnounceMinTier21;
                case 22 -> timedAnnounceMinTier22;
                case 23 -> timedAnnounceMinTier23;
                case 24 -> timedAnnounceMinTier24;
                case 25 -> timedAnnounceMinTier25;
                case 26 -> timedAnnounceMinTier26;
                case 27 -> timedAnnounceMinTier27;
                case 28 -> timedAnnounceMinTier28;
                case 29 -> timedAnnounceMinTier29;
                case 30 -> timedAnnounceMinTier30;
                case 31 -> timedAnnounceMinTier31;
                case 32 -> timedAnnounceMinTier32;
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
                case 7 -> bossWaveEvery7;
                case 8 -> bossWaveEvery8;
                case 9 -> bossWaveEvery9;
                case 10 -> bossWaveEvery10;
                case 11 -> bossWaveEvery11;
                case 12 -> bossWaveEvery12;
                case 13 -> bossWaveEvery13;
                case 14 -> bossWaveEvery14;
                case 15 -> bossWaveEvery15;
                case 16 -> bossWaveEvery16;
                case 17 -> bossWaveEvery17;
                case 18 -> bossWaveEvery18;
                case 19 -> bossWaveEvery19;
                case 20 -> bossWaveEvery20;
                case 21 -> bossWaveEvery21;
                case 22 -> bossWaveEvery22;
                case 23 -> bossWaveEvery23;
                case 24 -> bossWaveEvery24;
                case 25 -> bossWaveEvery25;
                case 26 -> bossWaveEvery26;
                case 27 -> bossWaveEvery27;
                case 28 -> bossWaveEvery28;
                case 29 -> bossWaveEvery29;
                case 30 -> bossWaveEvery30;
                case 31 -> bossWaveEvery31;
                case 32 -> bossWaveEvery32;
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
                case 7 -> bossWaveValue7;
                case 8 -> bossWaveValue8;
                case 9 -> bossWaveValue9;
                case 10 -> bossWaveValue10;
                case 11 -> bossWaveValue11;
                case 12 -> bossWaveValue12;
                case 13 -> bossWaveValue13;
                case 14 -> bossWaveValue14;
                case 15 -> bossWaveValue15;
                case 16 -> bossWaveValue16;
                case 17 -> bossWaveValue17;
                case 18 -> bossWaveValue18;
                case 19 -> bossWaveValue19;
                case 20 -> bossWaveValue20;
                case 21 -> bossWaveValue21;
                case 22 -> bossWaveValue22;
                case 23 -> bossWaveValue23;
                case 24 -> bossWaveValue24;
                case 25 -> bossWaveValue25;
                case 26 -> bossWaveValue26;
                case 27 -> bossWaveValue27;
                case 28 -> bossWaveValue28;
                case 29 -> bossWaveValue29;
                case 30 -> bossWaveValue30;
                case 31 -> bossWaveValue31;
                case 32 -> bossWaveValue32;
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
                case 7 -> bossWaveRepeatCount7;
                case 8 -> bossWaveRepeatCount8;
                case 9 -> bossWaveRepeatCount9;
                case 10 -> bossWaveRepeatCount10;
                case 11 -> bossWaveRepeatCount11;
                case 12 -> bossWaveRepeatCount12;
                case 13 -> bossWaveRepeatCount13;
                case 14 -> bossWaveRepeatCount14;
                case 15 -> bossWaveRepeatCount15;
                case 16 -> bossWaveRepeatCount16;
                case 17 -> bossWaveRepeatCount17;
                case 18 -> bossWaveRepeatCount18;
                case 19 -> bossWaveRepeatCount19;
                case 20 -> bossWaveRepeatCount20;
                case 21 -> bossWaveRepeatCount21;
                case 22 -> bossWaveRepeatCount22;
                case 23 -> bossWaveRepeatCount23;
                case 24 -> bossWaveRepeatCount24;
                case 25 -> bossWaveRepeatCount25;
                case 26 -> bossWaveRepeatCount26;
                case 27 -> bossWaveRepeatCount27;
                case 28 -> bossWaveRepeatCount28;
                case 29 -> bossWaveRepeatCount29;
                case 30 -> bossWaveRepeatCount30;
                case 31 -> bossWaveRepeatCount31;
                case 32 -> bossWaveRepeatCount32;
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
                case 7 -> bossWaveRepeatSec7;
                case 8 -> bossWaveRepeatSec8;
                case 9 -> bossWaveRepeatSec9;
                case 10 -> bossWaveRepeatSec10;
                case 11 -> bossWaveRepeatSec11;
                case 12 -> bossWaveRepeatSec12;
                case 13 -> bossWaveRepeatSec13;
                case 14 -> bossWaveRepeatSec14;
                case 15 -> bossWaveRepeatSec15;
                case 16 -> bossWaveRepeatSec16;
                case 17 -> bossWaveRepeatSec17;
                case 18 -> bossWaveRepeatSec18;
                case 19 -> bossWaveRepeatSec19;
                case 20 -> bossWaveRepeatSec20;
                case 21 -> bossWaveRepeatSec21;
                case 22 -> bossWaveRepeatSec22;
                case 23 -> bossWaveRepeatSec23;
                case 24 -> bossWaveRepeatSec24;
                case 25 -> bossWaveRepeatSec25;
                case 26 -> bossWaveRepeatSec26;
                case 27 -> bossWaveRepeatSec27;
                case 28 -> bossWaveRepeatSec28;
                case 29 -> bossWaveRepeatSec29;
                case 30 -> bossWaveRepeatSec30;
                case 31 -> bossWaveRepeatSec31;
                case 32 -> bossWaveRepeatSec32;
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
                case 7 -> bossWaveHp7;
                case 8 -> bossWaveHp8;
                case 9 -> bossWaveHp9;
                case 10 -> bossWaveHp10;
                case 11 -> bossWaveHp11;
                case 12 -> bossWaveHp12;
                case 13 -> bossWaveHp13;
                case 14 -> bossWaveHp14;
                case 15 -> bossWaveHp15;
                case 16 -> bossWaveHp16;
                case 17 -> bossWaveHp17;
                case 18 -> bossWaveHp18;
                case 19 -> bossWaveHp19;
                case 20 -> bossWaveHp20;
                case 21 -> bossWaveHp21;
                case 22 -> bossWaveHp22;
                case 23 -> bossWaveHp23;
                case 24 -> bossWaveHp24;
                case 25 -> bossWaveHp25;
                case 26 -> bossWaveHp26;
                case 27 -> bossWaveHp27;
                case 28 -> bossWaveHp28;
                case 29 -> bossWaveHp29;
                case 30 -> bossWaveHp30;
                case 31 -> bossWaveHp31;
                case 32 -> bossWaveHp32;
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
                case 7 -> bossWaveDamage7;
                case 8 -> bossWaveDamage8;
                case 9 -> bossWaveDamage9;
                case 10 -> bossWaveDamage10;
                case 11 -> bossWaveDamage11;
                case 12 -> bossWaveDamage12;
                case 13 -> bossWaveDamage13;
                case 14 -> bossWaveDamage14;
                case 15 -> bossWaveDamage15;
                case 16 -> bossWaveDamage16;
                case 17 -> bossWaveDamage17;
                case 18 -> bossWaveDamage18;
                case 19 -> bossWaveDamage19;
                case 20 -> bossWaveDamage20;
                case 21 -> bossWaveDamage21;
                case 22 -> bossWaveDamage22;
                case 23 -> bossWaveDamage23;
                case 24 -> bossWaveDamage24;
                case 25 -> bossWaveDamage25;
                case 26 -> bossWaveDamage26;
                case 27 -> bossWaveDamage27;
                case 28 -> bossWaveDamage28;
                case 29 -> bossWaveDamage29;
                case 30 -> bossWaveDamage30;
                case 31 -> bossWaveDamage31;
                case 32 -> bossWaveDamage32;
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
                case 7 -> bossWaveSize7;
                case 8 -> bossWaveSize8;
                case 9 -> bossWaveSize9;
                case 10 -> bossWaveSize10;
                case 11 -> bossWaveSize11;
                case 12 -> bossWaveSize12;
                case 13 -> bossWaveSize13;
                case 14 -> bossWaveSize14;
                case 15 -> bossWaveSize15;
                case 16 -> bossWaveSize16;
                case 17 -> bossWaveSize17;
                case 18 -> bossWaveSize18;
                case 19 -> bossWaveSize19;
                case 20 -> bossWaveSize20;
                case 21 -> bossWaveSize21;
                case 22 -> bossWaveSize22;
                case 23 -> bossWaveSize23;
                case 24 -> bossWaveSize24;
                case 25 -> bossWaveSize25;
                case 26 -> bossWaveSize26;
                case 27 -> bossWaveSize27;
                case 28 -> bossWaveSize28;
                case 29 -> bossWaveSize29;
                case 30 -> bossWaveSize30;
                case 31 -> bossWaveSize31;
                case 32 -> bossWaveSize32;
                default -> "";
            };
        }

        /** Only one arena row is edited at a time, so the radius travels in a single field. */
        public String getArenaRadius(int row) {
            return arenaRadius != null ? arenaRadius : "";
        }
    }
}
