package com.faiizer.craftrestrict.ui;

import com.faiizer.craftrestrict.Main;
import com.faiizer.craftrestrict.config.RestrictionRule;
import com.faiizer.craftrestrict.config.RestrictionRulesManager;
import com.faiizer.craftrestrict.recipes.CraftableItemsIndex;
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
    private static final int MAX_WORLD_ROWS = 10;

    private String availableSearchQuery = "";
    private String restrictedSearchQuery = "";

    @Nullable
    private String editingRuleId;
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
        buildAvailableList(cmd, evt);
        buildRestrictedList(cmd, evt);
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
            case "add_item" -> {
                String itemId = data.getItemId();
                RestrictionRulesManager.addRule(itemId, "", "");
                refreshUI();
            }
            case "remove_rule" -> {
                RestrictionRulesManager.removeRule(data.getRuleId());
                if (data.getRuleId() != null && data.getRuleId().equals(editingRuleId)) {
                    editingRuleId = null;
                }
                refreshUI();
            }
            case "edit_rule" -> {
                openEditPopup(data.getRuleId());
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

    private void openEditPopup(@Nullable String ruleId) {
        if (ruleId == null) {
            return;
        }
        RestrictionRule rule = Main.getConfig().getRestrictionRules().get(ruleId);
        if (rule == null) {
            return;
        }
        editingRuleId = ruleId;
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
        RestrictionRule existing = Main.getConfig().getRestrictionRules().get(editingRuleId);
        if (existing == null) {
            editingRuleId = null;
            return;
        }
        String excluded = RestrictionRule.joinWorlds(List.copyOf(editingExcludedWorlds));
        RestrictionRulesManager.updateRule(editingRuleId, excluded, permission == null ? "" : permission.trim());
        editingRuleId = null;
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AvailableSearchInput",
                new EventData().append("Action", "search_available").append("@SearchQuery", "#AvailableSearchInput.Value"), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RestrictedSearchInput",
                new EventData().append("Action", "search_restricted").append("@SearchQuery", "#RestrictedSearchInput.Value"), false);
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

    private void buildEditPopup(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        boolean open = editingRuleId != null;
        cmd.set("#EditPopup.Visible", open);
        if (!open) {
            return;
        }
        RestrictionRule rule = Main.getConfig().getRestrictionRules().get(editingRuleId);
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
        buildRestrictedList(cmd, evt);
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
