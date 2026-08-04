package com.varyon.craftrestrict.config;

import com.varyon.craftrestrict.Main;
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

    private static Map<String, RestrictionRule> rulesOf(boolean possession) {
        return possession ? Main.getConfig().getPossessionRestrictionRules() : Main.getConfig().getRestrictionRules();
    }

    private static void setRulesOf(boolean possession, Map<String, RestrictionRule> rules) {
        if (possession) {
            Main.getConfig().setPossessionRestrictionRules(rules);
        } else {
            Main.getConfig().setRestrictionRules(rules);
        }
    }

    public static List<RestrictionRule> listRules() {
        return new ArrayList<>(rulesOf(false).values());
    }

    public static List<Map.Entry<String, RestrictionRule>> listRuleEntries() {
        return new ArrayList<>(rulesOf(false).entrySet());
    }

    public static List<Map.Entry<String, RestrictionRule>> listPossessionRuleEntries() {
        return new ArrayList<>(rulesOf(true).entrySet());
    }

    /**
     * Adds a restriction rule, or updates the existing one if a rule with the same
     * pattern and excluded-worlds scope already exists (no duplicates).
     */
    public static String addRule(String pattern, String excludedWorlds, String permission) {
        return addRule(false, pattern, excludedWorlds, permission, "DENY");
    }

    public static String addPossessionRule(String pattern) {
        return addRule(true, pattern, "", "", "DENY");
    }

    public static String addRule(boolean possession, String pattern, String excludedWorlds, String permission, String mode) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        String normalizedPattern = pattern.trim();
        String normalizedExcluded = excludedWorlds == null ? "" : excludedWorlds.trim();
        String normalizedPermission = permission == null ? "" : permission.trim();
        String normalizedMode = mode == null ? "DENY" : mode.trim();

        Map<String, RestrictionRule> rules = new LinkedHashMap<>(rulesOf(possession));

        for (Map.Entry<String, RestrictionRule> entry : rules.entrySet()) {
            RestrictionRule existing = entry.getValue();
            if (existing.getPattern().equalsIgnoreCase(normalizedPattern)
                    && existing.getExcludedWorlds().equalsIgnoreCase(normalizedExcluded)) {
                entry.setValue(new RestrictionRule(normalizedPattern, normalizedExcluded, normalizedPermission, normalizedMode));
                setRulesOf(possession, rules);
                Main.getPluginInstance().saveConfig();
                return entry.getKey();
            }
        }

        String id = UUID.randomUUID().toString();
        rules.put(id, new RestrictionRule(normalizedPattern, normalizedExcluded, normalizedPermission, normalizedMode));
        setRulesOf(possession, rules);
        Main.getPluginInstance().saveConfig();
        return id;
    }

    public static void removeRule(String ruleId) {
        removeRule(false, ruleId);
    }

    public static void removePossessionRule(String ruleId) {
        removeRule(true, ruleId);
    }

    public static void removeRule(boolean possession, String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        Map<String, RestrictionRule> rules = new LinkedHashMap<>(rulesOf(possession));
        if (rules.remove(ruleId) != null) {
            setRulesOf(possession, rules);
            Main.getPluginInstance().saveConfig();
        }
    }

    /**
     * Updates the excluded-worlds scope and permission of an existing rule (its id pattern is kept unchanged).
     */
    public static void updateRule(String ruleId, String excludedWorlds, String permission) {
        updateRule(false, ruleId, excludedWorlds, permission, null);
    }

    public static void updateRule(boolean possession, String ruleId, String excludedWorlds, String permission, String mode) {
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        Map<String, RestrictionRule> rules = new LinkedHashMap<>(rulesOf(possession));
        RestrictionRule existing = rules.get(ruleId);
        if (existing == null) {
            return;
        }
        String normalizedMode = mode == null ? existing.getMode() : mode;
        rules.put(ruleId, new RestrictionRule(existing.getPattern(),
                excludedWorlds == null ? "" : excludedWorlds.trim(),
                permission == null ? "" : permission.trim(),
                normalizedMode));
        setRulesOf(possession, rules);
        Main.getPluginInstance().saveConfig();
    }

    public static void togglePossessionRuleMode(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        Map<String, RestrictionRule> rules = new LinkedHashMap<>(rulesOf(true));
        RestrictionRule existing = rules.get(ruleId);
        if (existing == null) {
            return;
        }
        String newMode = existing.isDeleteMode() ? "DENY" : "DELETE";
        rules.put(ruleId, new RestrictionRule(existing.getPattern(), existing.getExcludedWorlds(), existing.getPermission(), newMode));
        setRulesOf(true, rules);
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
        for (RestrictionRule rule : rulesOf(false).values()) {
            if (rule.matchesId(itemId) && rule.matchesWorld(worldName)) {
                return true;
            }
        }
        return false;
    }
}
