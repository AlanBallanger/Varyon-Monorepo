package com.faiizer.craftrestrict.config;

import com.faiizer.craftrestrict.Main;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the wildcard/scoped restriction rules configured via the CraftRestrict UI
 * (in addition to the plain permission-node based restriction system in PermissionsUtils).
 */
public final class RestrictionRulesManager {

    private RestrictionRulesManager() {
    }

    public static List<RestrictionRule> listRules() {
        return new ArrayList<>(Main.getConfig().getRestrictionRules().values());
    }

    public static List<Map.Entry<String, RestrictionRule>> listRuleEntries() {
        return new ArrayList<>(Main.getConfig().getRestrictionRules().entrySet());
    }

    /**
     * Adds a restriction rule, or updates the existing one if a rule with the same
     * pattern and scope already exists (no duplicates).
     */
    public static String addRule(String pattern, String worldScope, String permission) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        String normalizedPattern = pattern.trim();
        String normalizedScope = worldScope == null ? "" : worldScope.trim();
        String normalizedPermission = permission == null ? "" : permission.trim();

        CraftRestrictConfig config = Main.getConfig();
        Map<String, RestrictionRule> rules = new LinkedHashMap<>(config.getRestrictionRules());

        for (Map.Entry<String, RestrictionRule> entry : rules.entrySet()) {
            RestrictionRule existing = entry.getValue();
            if (existing.getPattern().equalsIgnoreCase(normalizedPattern)
                    && existing.getWorldScope().equalsIgnoreCase(normalizedScope)) {
                entry.setValue(new RestrictionRule(normalizedPattern, normalizedScope, normalizedPermission));
                config.setRestrictionRules(rules);
                Main.getPluginInstance().saveConfig();
                return entry.getKey();
            }
        }

        String id = UUID.randomUUID().toString();
        rules.put(id, new RestrictionRule(normalizedPattern, normalizedScope, normalizedPermission));
        config.setRestrictionRules(rules);
        Main.getPluginInstance().saveConfig();
        return id;
    }

    public static void removeRule(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        CraftRestrictConfig config = Main.getConfig();
        Map<String, RestrictionRule> rules = new LinkedHashMap<>(config.getRestrictionRules());
        if (rules.remove(ruleId) != null) {
            config.setRestrictionRules(rules);
            Main.getPluginInstance().saveConfig();
        }
    }

    /**
     * Updates the scope and permission of an existing rule (its id pattern is kept unchanged).
     */
    public static void updateRule(String ruleId, String worldScope, String permission) {
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        CraftRestrictConfig config = Main.getConfig();
        Map<String, RestrictionRule> rules = new LinkedHashMap<>(config.getRestrictionRules());
        RestrictionRule existing = rules.get(ruleId);
        if (existing == null) {
            return;
        }
        rules.put(ruleId, new RestrictionRule(existing.getPattern(), worldScope == null ? "" : worldScope.trim(), permission == null ? "" : permission.trim()));
        config.setRestrictionRules(rules);
        Main.getPluginInstance().saveConfig();
    }

    /**
     * Whether the given item/recipe output id is restricted for the given world (or globally).
     * A rule applies if its id pattern matches AND its scope is global or matches the world name.
     */
    public static boolean isRestrictedByRule(String itemId, String worldName) {
        if (itemId == null) {
            return false;
        }
        for (RestrictionRule rule : Main.getConfig().getRestrictionRules().values()) {
            if (rule.matchesId(itemId) && rule.matchesWorld(worldName)) {
                return true;
            }
        }
        return false;
    }
}
