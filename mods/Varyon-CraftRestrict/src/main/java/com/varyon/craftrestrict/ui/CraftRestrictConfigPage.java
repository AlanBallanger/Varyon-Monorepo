package com.varyon.craftrestrict.ui;

import com.varyon.craftrestrict.Main;
import com.varyon.craftrestrict.config.RestrictionRule;
import com.varyon.craftrestrict.config.RestrictionRulesManager;
import com.varyon.craftrestrict.recipes.CraftableItemsIndex;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CraftRestrictConfigPage extends InteractiveCustomUIPage<CraftRestrictEventData> {

    private static final String LAYOUT = "Pages/CraftRestrictConfigPage.ui";
    private static final int MAX_AVAILABLE_ROWS = 14;
    private static final int MAX_RESTRICTED_ROWS = 14;
    private static final int MAX_POSSESSION_ROWS = 14;
    private static final int MAX_WORLD_ROWS = 10;

    private String availableSearchQuery = "";
    private String restrictedSearchQuery = "";
    private String possessionSearchQuery = "";
    private boolean possessionTabActive = false;

    @Nullable
    private String editingRuleId;
    private boolean editingPossession = false;
    /** Worlds toggled OFF (excluded) for the rule currently being edited. */
    private final Set<String> editingExcludedWorlds = new LinkedHashSet<>();

    private CraftRestrictConfigPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, CraftRestrictEventData.CODEC);
    }

    public static void open(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Player player, @Nonnull World world) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        CraftRestrictConfigPage page = new CraftRestrictConfigPage(playerRef);
        player.getPageManager().openCustomPage(ref, store, page);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                       @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append(LAYOUT);
        cmd.set("#AvailableSearchInput.Value", availableSearchQuery);
        cmd.set("#RestrictedSearchInput.Value", restrictedSearchQuery);
        cmd.set("#PossessionSearchInput.Value", possessionSearchQuery);
        buildAvailableList(cmd, evt);
        buildTabs(cmd);
        buildRestrictedList(cmd, evt);
        buildPossessionList(cmd, evt);
        buildEditPopup(cmd, evt);
        bindStaticEvents(evt);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                 @Nonnull CraftRestrictEventData data) {
        String action = data.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case "search_available" -> {
                availableSearchQuery = data.getSearchQuery() == null ? "" : data.getSearchQuery();
                refreshUI();
            }
            case "search_restricted" -> {
                restrictedSearchQuery = data.getSearchQuery() == null ? "" : data.getSearchQuery();
                refreshUI();
            }
            case "search_possession" -> {
                possessionSearchQuery = data.getSearchQuery() == null ? "" : data.getSearchQuery();
                refreshUI();
            }
            case "show_craft_tab" -> {
                possessionTabActive = false;
                refreshUI();
            }
            case "show_possession_tab" -> {
                possessionTabActive = true;
                refreshUI();
            }
            case "add_item" -> {
                String itemId = data.getItemId();
                if (possessionTabActive) {
                    RestrictionRulesManager.addPossessionRule(itemId);
                } else {
                    RestrictionRulesManager.addRule(itemId, "", "");
                }
                refreshUI();
            }
            case "remove_rule" -> {
                RestrictionRulesManager.removeRule(data.getRuleId());
                if (data.getRuleId() != null && data.getRuleId().equals(editingRuleId)) {
                    editingRuleId = null;
                }
                refreshUI();
            }
            case "remove_possession_rule" -> {
                RestrictionRulesManager.removePossessionRule(data.getRuleId());
                if (data.getRuleId() != null && data.getRuleId().equals(editingRuleId)) {
                    editingRuleId = null;
                }
                refreshUI();
            }
            case "toggle_possession_mode" -> {
                RestrictionRulesManager.togglePossessionRuleMode(data.getRuleId());
                refreshUI();
            }
            case "edit_rule" -> {
                openEditPopup(data.getRuleId(), false);
                refreshUI();
            }
            case "edit_possession_rule" -> {
                openEditPopup(data.getRuleId(), true);
                refreshUI();
            }
            case "edit_toggle_world" -> {
                toggleEditingWorld(data.getItemId());
                refreshUI();
            }
            case "edit_save" -> {
                saveEditPopup(data.getEditPermission());
                refreshUI();
            }
            case "edit_close" -> {
                editingRuleId = null;
                refreshUI();
            }
            case "close" -> closePage(ref, store);
            default -> {
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    }

    private void openEditPopup(@Nullable String ruleId, boolean possession) {
        if (ruleId == null) {
            return;
        }
        Map<String, RestrictionRule> rules = possession
                ? Main.getConfig().getPossessionRestrictionRules()
                : Main.getConfig().getRestrictionRules();
        RestrictionRule rule = rules.get(ruleId);
        if (rule == null) {
            return;
        }
        editingRuleId = ruleId;
        editingPossession = possession;
        editingExcludedWorlds.clear();
        editingExcludedWorlds.addAll(rule.getExcludedWorldsList());
    }

    private void toggleEditingWorld(@Nullable String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        if (!editingExcludedWorlds.remove(worldName)) {
            editingExcludedWorlds.add(worldName);
        }
    }

    private void saveEditPopup(@Nullable String permission) {
        if (editingRuleId == null) {
            return;
        }
        Map<String, RestrictionRule> rules = editingPossession
                ? Main.getConfig().getPossessionRestrictionRules()
                : Main.getConfig().getRestrictionRules();
        RestrictionRule existing = rules.get(editingRuleId);
        if (existing == null) {
            editingRuleId = null;
            return;
        }
        String excluded = RestrictionRule.joinWorlds(List.copyOf(editingExcludedWorlds));
        RestrictionRulesManager.updateRule(editingPossession, editingRuleId, excluded,
                permission == null ? "" : permission.trim(), null);
        editingRuleId = null;
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AvailableSearchInput",
                new EventData().append("Action", "search_available").append("@SearchQuery", "#AvailableSearchInput.Value"), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RestrictedSearchInput",
                new EventData().append("Action", "search_restricted").append("@SearchQuery", "#RestrictedSearchInput.Value"), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PossessionSearchInput",
                new EventData().append("Action", "search_possession").append("@SearchQuery", "#PossessionSearchInput.Value"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabCraftButton", EventData.of("Action", "show_craft_tab"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabPossessionButton", EventData.of("Action", "show_possession_tab"));
    }

    private void buildTabs(@Nonnull UICommandBuilder cmd) {
        cmd.set("#CraftTab.Visible", !possessionTabActive);
        cmd.set("#PossessionTab.Visible", possessionTabActive);
    }

    private void buildAvailableList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        List<String> items = CraftableItemsIndex.filterItemIds(availableSearchQuery, MAX_AVAILABLE_ROWS);
        for (int row = 1; row <= MAX_AVAILABLE_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= items.size();
            cmd.set("#AvailableRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }
            String itemId = items.get(row - 1);
            cmd.set("#AvailableLabel" + suffix + ".Text", escape(itemId));
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#AvailableAdd" + suffix,
                    EventData.of("Action", "add_item").append("ItemId", itemId), false);
        }
    }

    private void buildRestrictedList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        List<Map.Entry<String, RestrictionRule>> rules = RestrictionRulesManager.listRuleEntries();
        String needle = restrictedSearchQuery == null ? "" : restrictedSearchQuery.trim().toLowerCase(Locale.ROOT);
        if (!needle.isEmpty()) {
            rules = rules.stream()
                    .filter(entry -> entry.getValue().getPattern().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }
        cmd.set("#RestrictedEmptyLabel.Visible", rules.isEmpty());
        for (int row = 1; row <= MAX_RESTRICTED_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= rules.size();
            cmd.set("#RestrictedRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }
            Map.Entry<String, RestrictionRule> entry = rules.get(row - 1);
            RestrictionRule rule = entry.getValue();
            String scopeLabel = rule.isGlobal() ? "GLOBAL" : "SAUF " + String.join(", ", rule.getExcludedWorldsList());
            String permLabel = rule.hasPermission() ? (" [" + rule.getPermission() + "]") : "";
            cmd.set("#RestrictedLabel" + suffix + ".Text", escape(rule.getPattern() + "  (" + scopeLabel + ")" + permLabel));
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#RestrictedOpen" + suffix,
                    EventData.of("Action", "edit_rule").append("RuleId", entry.getKey()), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#RestrictedRemove" + suffix,
                    EventData.of("Action", "remove_rule").append("RuleId", entry.getKey()), false);
        }
    }

    private void buildPossessionList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        List<Map.Entry<String, RestrictionRule>> rules = RestrictionRulesManager.listPossessionRuleEntries();
        String needle = possessionSearchQuery == null ? "" : possessionSearchQuery.trim().toLowerCase(Locale.ROOT);
        if (!needle.isEmpty()) {
            rules = rules.stream()
                    .filter(entry -> entry.getValue().getPattern().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }
        cmd.set("#PossessionEmptyLabel.Visible", rules.isEmpty());
        for (int row = 1; row <= MAX_POSSESSION_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= rules.size();
            cmd.set("#PossessionRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }
            Map.Entry<String, RestrictionRule> entry = rules.get(row - 1);
            RestrictionRule rule = entry.getValue();
            String scopeLabel = rule.isGlobal() ? "GLOBAL" : "SAUF " + String.join(", ", rule.getExcludedWorldsList());
            String permLabel = rule.hasPermission() ? (" [" + rule.getPermission() + "]") : "";
            String modeLabel = rule.isDeleteMode() ? "DELETE" : "DENY";
            cmd.set("#PossessionLabel" + suffix + ".Text",
                    escape(rule.getPattern() + "  (" + scopeLabel + ")" + permLabel + "  <" + modeLabel + ">"));
            cmd.set("#PossessionMode" + suffix + ".Text", modeLabel);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#PossessionOpen" + suffix,
                    EventData.of("Action", "edit_possession_rule").append("RuleId", entry.getKey()), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#PossessionMode" + suffix,
                    EventData.of("Action", "toggle_possession_mode").append("RuleId", entry.getKey()), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#PossessionRemove" + suffix,
                    EventData.of("Action", "remove_possession_rule").append("RuleId", entry.getKey()), false);
        }
    }

    private void buildEditPopup(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        boolean open = editingRuleId != null;
        cmd.set("#EditPopup.Visible", open);
        if (!open) {
            return;
        }
        Map<String, RestrictionRule> rules = editingPossession
                ? Main.getConfig().getPossessionRestrictionRules()
                : Main.getConfig().getRestrictionRules();
        RestrictionRule rule = rules.get(editingRuleId);
        if (rule == null) {
            cmd.set("#EditPopup.Visible", false);
            return;
        }
        cmd.set("#EditPopupTitle.Text", escape(rule.getPattern()));
        cmd.set("#EditPermissionInput.Value", rule.getPermission());

        List<String> worlds = CraftableItemsIndex.listWorldNames();
        cmd.set("#EditWorldsEmptyLabel.Visible", worlds.isEmpty());
        for (int row = 1; row <= MAX_WORLD_ROWS; row++) {
            String suffix = Integer.toString(row);
            boolean visible = row <= worlds.size();
            cmd.set("#EditWorldRow" + suffix + ".Visible", visible);
            if (!visible) {
                continue;
            }
            String worldName = worlds.get(row - 1);
            boolean excluded = editingExcludedWorlds.contains(worldName);
            cmd.set("#EditWorldBadgeOn" + suffix + ".Visible", !excluded);
            cmd.set("#EditWorldBadgeOff" + suffix + ".Visible", excluded);
            cmd.set("#EditWorldName" + suffix + ".Text", escape(worldName));
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#EditWorldRow" + suffix,
                    EventData.of("Action", "edit_toggle_world").append("ItemId", worldName), false);
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#EditSaveButton",
                new EventData().append("Action", "edit_save").append("@EditPermission", "#EditPermissionInput.Value"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#EditCloseButton",
                EventData.of("Action", "edit_close"), false);
    }

    private void refreshUI() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        buildAvailableList(cmd, evt);
        buildTabs(cmd);
        buildRestrictedList(cmd, evt);
        buildPossessionList(cmd, evt);
        buildEditPopup(cmd, evt);
        bindStaticEvents(evt);
        sendUpdate(cmd, evt, false);
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
