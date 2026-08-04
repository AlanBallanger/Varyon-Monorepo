package com.faiizer.craftrestrict.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A single restriction rule: an item/recipe id pattern (exact id, or wildcard using '*'),
 * an optional scope (a comma-separated list of world names EXCLUDED from the rule, or
 * blank if the rule applies to every world) and an optional permission node (reserved
 * for future per-permission/rank restrictions).
 *
 * <p>Every world is ON (restricted) by default; toggling a world OFF in the UI excludes it.
 */
public class RestrictionRule {

    public static final BuilderCodec<RestrictionRule> CODEC = BuilderCodec.builder(RestrictionRule.class, RestrictionRule::new)
            .append(new KeyedCodec<>("Pattern", Codec.STRING),
                    (rule, value, extraInfo) -> rule.pattern = value,
                    (rule, extraInfo) -> rule.pattern).add()
            .append(new KeyedCodec<>("WorldScope", Codec.STRING),
                    (rule, value, extraInfo) -> rule.excludedWorlds = value,
                    (rule, extraInfo) -> rule.excludedWorlds).add()
            .append(new KeyedCodec<>("Permission", Codec.STRING),
                    (rule, value, extraInfo) -> rule.permission = value,
                    (rule, extraInfo) -> rule.permission).add()
            .build();

    private String pattern = "";
    /** Comma-separated list of world names EXCLUDED from this rule, or blank if it applies everywhere. */
    private String excludedWorlds = "";
    private String permission = "";

    public RestrictionRule() {
    }

    public RestrictionRule(String pattern, String excludedWorlds, String permission) {
        this.pattern = pattern == null ? "" : pattern;
        this.excludedWorlds = excludedWorlds == null ? "" : excludedWorlds;
        this.permission = permission == null ? "" : permission;
    }

    public static String joinWorlds(List<String> worlds) {
        if (worlds == null || worlds.isEmpty()) {
            return "";
        }
        return String.join(",", worlds);
    }

    public String getPattern() {
        return pattern;
    }

    public String getExcludedWorlds() {
        return excludedWorlds;
    }

    public List<String> getExcludedWorldsList() {
        if (excludedWorlds == null || excludedWorlds.isBlank()) {
            return List.of();
        }
        List<String> worlds = new ArrayList<>();
        for (String world : excludedWorlds.split(",")) {
            String trimmed = world.trim();
            if (!trimmed.isEmpty()) {
                worlds.add(trimmed);
            }
        }
        return worlds;
    }

    public boolean isGlobal() {
        return excludedWorlds == null || excludedWorlds.isBlank();
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    public boolean matchesWorld(String worldName) {
        if (isGlobal()) {
            return true;
        }
        if (worldName == null) {
            return true;
        }
        return Arrays.stream(excludedWorlds.split(","))
                .map(String::trim)
                .noneMatch(world -> world.equalsIgnoreCase(worldName));
    }

    public boolean matchesId(String itemId) {
        if (itemId == null || pattern == null || pattern.isBlank()) {
            return false;
        }
        if (!pattern.contains("*")) {
            return pattern.equalsIgnoreCase(itemId);
        }
        String regex = "(?i)" + java.util.regex.Pattern.quote(pattern).replace("*", "\\E.*\\Q");
        return itemId.matches(regex);
    }
}
