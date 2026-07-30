package com.faiizer.craftrestrict.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A single restriction rule: an item/recipe id pattern (exact id, or wildcard using '*'),
 * an optional scope (a comma-separated list of world names, or blank for global) and an
 * optional permission node (reserved for future per-permission/rank restrictions).
 */
public class RestrictionRule {

    public static final BuilderCodec<RestrictionRule> CODEC = BuilderCodec.builder(RestrictionRule.class, RestrictionRule::new)
            .append(new KeyedCodec<>("Pattern", Codec.STRING),
                    (rule, value, extraInfo) -> rule.pattern = value,
                    (rule, extraInfo) -> rule.pattern).add()
            .append(new KeyedCodec<>("WorldScope", Codec.STRING),
                    (rule, value, extraInfo) -> rule.worldScope = value,
                    (rule, extraInfo) -> rule.worldScope).add()
            .append(new KeyedCodec<>("Permission", Codec.STRING),
                    (rule, value, extraInfo) -> rule.permission = value,
                    (rule, extraInfo) -> rule.permission).add()
            .build();

    private String pattern = "";
    /** Comma-separated list of world names this rule applies to, or blank for global (all worlds). */
    private String worldScope = "";
    private String permission = "";

    public RestrictionRule() {
    }

    public RestrictionRule(String pattern, String worldScope, String permission) {
        this.pattern = pattern == null ? "" : pattern;
        this.worldScope = worldScope == null ? "" : worldScope;
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

    public String getWorldScope() {
        return worldScope;
    }

    public List<String> getWorldScopeList() {
        if (isGlobal()) {
            return List.of();
        }
        List<String> worlds = new ArrayList<>();
        for (String world : worldScope.split(",")) {
            String trimmed = world.trim();
            if (!trimmed.isEmpty()) {
                worlds.add(trimmed);
            }
        }
        return worlds;
    }

    public boolean isGlobal() {
        return worldScope == null || worldScope.isBlank();
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
            return false;
        }
        return Arrays.stream(worldScope.split(","))
                .map(String::trim)
                .anyMatch(world -> world.equalsIgnoreCase(worldName));
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
