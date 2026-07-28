package com.varyon.bossarena.config;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.npc.NPCPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared UI helpers for BossArena config toggles, tiers, and NPC search lists. */
final class BossArenaConfigUiControls {
    static final String[] TIERS = {"common", "uncommon", "rare", "epic", "legendary"};
    static final int MAX_BOSS_NPC_PICKS = 8;
    static final int MAX_WAVE_NPC_PICKS = 8;
    static final int MAX_ITEM_PICKS = 8;

    static final float[] PER_PLAYER_STEPS = {1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.75f, 2.0f, 3.0f};

    /** Advances a per-player multiplier to the next value in {@link #PER_PLAYER_STEPS}, wrapping to the first after the last. */
    static float nextPerPlayerStep(float current) {
        for (int i = 0; i < PER_PLAYER_STEPS.length; i++) {
            if (current < PER_PLAYER_STEPS[i] - 0.001f) {
                return PER_PLAYER_STEPS[i];
            }
        }
        return PER_PLAYER_STEPS[0];
    }

    private static final String ON_BG = "#1f7d4b";
    private static final String ON_HOVER = "#2b9a5f";
    private static final String OFF_BG = "#a33b3b";
    private static final String OFF_HOVER = "#c44a4a";
    private static final String IDLE_BG = "#2a3d58";
    private static final String IDLE_HOVER = "#38567b";
    private static final String ACTIVE_MODE_BG = "#1f7d4b";
    private static final String ACTIVE_MODE_HOVER = "#2b9a5f";

    private BossArenaConfigUiControls() {}

    static void styleOnOffTextButton(@Nonnull UICommandBuilder cmd, @Nonnull String buttonId, boolean enabled) {
        cmd.set(buttonId + ".Text", enabled ? "ON" : "OFF");
        styleTextButton(cmd, buttonId, enabled ? ON_BG : OFF_BG, enabled ? ON_HOVER : OFF_HOVER);
    }

    static void styleModeTextButton(@Nonnull UICommandBuilder cmd, @Nonnull String buttonId, boolean active) {
        styleTextButton(cmd, buttonId, active ? ACTIVE_MODE_BG : IDLE_BG, active ? ACTIVE_MODE_HOVER : IDLE_HOVER);
    }

    static void styleTierButton(@Nonnull UICommandBuilder cmd, @Nonnull String buttonId, boolean active) {
        styleTextButton(cmd, buttonId, active ? ACTIVE_MODE_BG : IDLE_BG, active ? ACTIVE_MODE_HOVER : IDLE_HOVER);
    }

    private static void styleTextButton(
            @Nonnull UICommandBuilder cmd,
            @Nonnull String buttonId,
            @Nonnull String bg,
            @Nonnull String hover
    ) {
        PatchStyle def = new PatchStyle().setColor(Value.of(bg));
        PatchStyle hov = new PatchStyle().setColor(Value.of(hover));
        String base = buttonId + ".Style";
        cmd.setObject(base + ".Default.Background", def);
        cmd.setObject(base + ".Hovered.Background", hov);
        cmd.setObject(base + ".Pressed.Background", def);
    }

    @Nonnull
    static List<String> filterNpcIds(@Nullable String query, int limit) {
        List<String> all = allNpcIds();
        if (all.isEmpty()) {
            return List.of();
        }

        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String role : all) {
            if (role == null || role.isBlank()) {
                continue;
            }
            if (needle.isEmpty() || role.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(role);
                if (matches.size() >= limit) {
                    break;
                }
            }
        }
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);
        if (matches.size() > limit) {
            return matches.subList(0, limit);
        }
        return matches;
    }

    /** True when query equals an NPC role id exactly (case-insensitive). */
    static boolean isExactNpcId(@Nullable String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String needle = query.trim();
        for (String role : allNpcIds()) {
            if (role != null && role.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> allNpcIds() {
        try {
            NPCPlugin plugin = NPCPlugin.get();
            List<String> all = plugin != null ? plugin.getRoleTemplateNames(true) : null;
            return all == null ? List.of() : all;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    @Nonnull
    static List<String> filterItemIds(@Nullable String query, int limit) {
        List<String> all = allItemIds();
        if (all.isEmpty()) {
            return List.of();
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String itemId : all) {
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            if (needle.isEmpty() || itemId.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(itemId);
                if (matches.size() >= Math.max(1, limit) * 4) {
                    break;
                }
            }
        }
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);
        if (matches.size() > limit) {
            return matches.subList(0, limit);
        }
        return matches;
    }

    static boolean isExactItemId(@Nullable String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String needle = query.trim();
        try {
            Item item = Item.getAssetMap() != null ? Item.getAssetMap().getAsset(needle) : null;
            if (item != null) {
                return true;
            }
        } catch (Throwable ignored) {
            // fall through to scan
        }
        for (String itemId : allItemIds()) {
            if (itemId != null && itemId.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> allItemIds() {
        try {
            var assetMap = Item.getAssetMap();
            if (assetMap == null) {
                return List.of();
            }
            Map<String, Item> map = assetMap.getAssetMap();
            if (map == null || map.isEmpty()) {
                return List.of();
            }
            List<String> ids = new ArrayList<>(map.size());
            for (String id : map.keySet()) {
                if (id != null && !id.isBlank()) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    static float clampFloat(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
