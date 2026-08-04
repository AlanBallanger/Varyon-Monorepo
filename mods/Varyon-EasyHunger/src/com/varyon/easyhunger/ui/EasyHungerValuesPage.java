package com.varyon.easyhunger.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.varyon.easyhunger.EasyHunger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EasyHungerValuesPage - Interactive UI for viewing and editing food/drink values.
 * Features tabs, search, category filters, and display names.
 */
public class EasyHungerValuesPage extends InteractiveCustomUIPage<EasyHungerValuesPage.ConfigEventData> {

    private String activeTab = "foods";
    private String searchQuery = "";
    private String category = "all"; // all, vanilla, nocube, ale, cheff, other
    private final List<String> itemIds = new ArrayList<>();

    public static class ConfigEventData {
        public String action;
        public String index;
        public String value;
        public String newItemId;
        public String newValue;
        public String searchQuery;
        public String configKey;  // For config tab

        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec
                .builder(ConfigEventData.class, ConfigEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING), (o, v) -> o.index = v, o -> o.index).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING), (o, v) -> o.value = v, o -> o.value).add()
                .append(new KeyedCodec<>("@NewItemId", Codec.STRING), (o, v) -> o.newItemId = v, o -> o.newItemId).add()
                .append(new KeyedCodec<>("@NewValue", Codec.STRING), (o, v) -> o.newValue = v, o -> o.newValue).add()
                .append(new KeyedCodec<>("@SearchQuery", Codec.STRING), (o, v) -> o.searchQuery = v, o -> o.searchQuery).add()
                .append(new KeyedCodec<>("ConfigKey", Codec.STRING), (o, v) -> o.configKey = v, o -> o.configKey).add()
                .build();
    }

    public EasyHungerValuesPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigEventData.CODEC);
    }

    public EasyHungerValuesPage(@Nonnull PlayerRef playerRef, String tab, String search, String cat) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigEventData.CODEC);
        this.activeTab = tab;
        this.searchQuery = search != null ? search : "";
        this.category = cat != null ? cat : "all";
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        
        cmd.append("Pages/EasyHungerValuesUI.ui");

        String language = playerRef.getLanguage();

        // Show/hide sections based on active tab
        boolean isConfigTab = activeTab.equals("config");
        cmd.set("#CategoryRow.Visible", !isConfigTab);
        cmd.set("#SearchRow.Visible", !isConfigTab);
        cmd.set("#TotalLabel.Visible", !isConfigTab);
        cmd.set("#ColumnHeaders.Visible", !isConfigTab);
        cmd.set("#ItemList.Visible", !isConfigTab);
        cmd.set("#AddSection.Visible", !isConfigTab);
        cmd.set("#AddRow.Visible", !isConfigTab);
        cmd.set("#InvPanel.Visible", !isConfigTab);
        cmd.set("#ConfigList.Visible", isConfigTab);

        if (isConfigTab) {
            buildConfigSection(cmd, evt);
            // Only bind tabs for config, rest not needed
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabFoods", EventData.of("Action", "tab:foods"), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabDrinks", EventData.of("Action", "tab:drinks"), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabConfig", EventData.of("Action", "tab:config"), false);
            return;
        }

        // Render player inventory
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            renderInventory(cmd, evt, player);
        }

        Map<String, Float> allItems = activeTab.equals("foods") 
            ? EasyHunger.get().getFoodsConfig().getFoodValues()
            : EasyHunger.get().getDrinksConfig().getDrinkValues();

        // Show/hide category buttons based on installed mods
        cmd.set("#CatVanilla.Visible", hasCategoryItems("vanilla", allItems));
        cmd.set("#CatNoCube.Visible", hasCategoryItems("nocube", allItems));
        cmd.set("#CatSnip3.Visible", hasCategoryItems("snip3", allItems));
        cmd.set("#CatHiddens.Visible", hasCategoryItems("hiddens", allItems));
        cmd.set("#CatGalore.Visible", hasCategoryItems("galore", allItems));
        cmd.set("#CatConnor.Visible", hasCategoryItems("connor", allItems));
        cmd.set("#CatCaba.Visible", hasCategoryItems("caba", allItems));
        cmd.set("#CatZcGaia.Visible", hasCategoryItems("zcgaia", allItems));
        cmd.set("#CatHatsuya.Visible", hasCategoryItems("hatsuya", allItems));
        cmd.set("#CatEndgame.Visible", hasCategoryItems("endgame", allItems));
        cmd.set("#CatSpice.Visible", hasCategoryItems("spice", allItems));
        cmd.set("#CatAle.Visible", hasCategoryItems("ale", allItems));
        cmd.set("#CatCheff.Visible", hasCategoryItems("cheff", allItems));
        cmd.set("#CatOther.Visible", hasCategoryItems("other", allItems));

        final String query = searchQuery.toLowerCase().trim();
        
        // Filter by:
        // 1. Item exists in game (installed mods only) OR is from EasyHunger mod
        // 2. Search query (searches both ID and display name)
        // 3. Category filter
        Map<String, Float> items = allItems.entrySet().stream()
            .filter(e -> {
                String itemId = e.getKey();
                // Always show EasyHunger mod items (our own mod)
                if (itemId.startsWith("EasyHunger_")) return true;
                // Always show items that would be categorized as OTHER (custom entries)
                if (ItemCategoryFilter.getCategory(itemId).equals("other")) return true;
                // Check if item exists in game for other mods
                Item item = (Item) Item.getAssetMap().getAsset(itemId);
                if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
                return item != null;
            })
            .filter(e -> {
                if (query.isEmpty()) return true;
                String id = e.getKey().toLowerCase();
                String displayName = getDisplayName(e.getKey(), language).toLowerCase();
                return id.contains(query) || displayName.contains(query);
            })
            .filter(e -> matchesCategory(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        itemIds.clear();

        int index = 0;
        for (Map.Entry<String, Float> entry : items.entrySet()) {
            String itemId = entry.getKey();
            float value = entry.getValue();
            
            itemIds.add(itemId);
            
            String selector = "#ItemList[" + index + "]";
            cmd.append("#ItemList", "Pages/ItemEntry.ui");
            
            // Set item icon - map EasyHunger config IDs to real item IDs
            String iconId = getIconItemId(itemId);
            cmd.set(selector + " #ItemIcon.ItemId", iconId);
            
            // Show display name with item ID in tooltip
            String displayName = getDisplayName(itemId, language);
            cmd.set(selector + " #ItemName.Text", displayName);
            cmd.set(selector + " #ItemName.TooltipTextSpans", Message.raw(itemId));
            cmd.set(selector + " #ValueInput.Value", formatValue(value));
            
            evt.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                selector + " #ValueInput",
                new EventData()
                    .append("Action", "setValue")
                    .append("Index", String.valueOf(index))
                    .append("@Value", selector + " #ValueInput.Value"),
                false
            );
            
            index++;
        }

        // Total label
        String itemType = activeTab.equals("foods") ? "foods" : "drinks";
        if (query.isEmpty() && category.equals("all")) {
            cmd.set("#TotalLabel.Text", "Total: " + index + " " + itemType);
        } else {
            cmd.set("#TotalLabel.Text", "Showing: " + index + " of " + allItems.size() + " " + itemType);
        }

        // Bind tabs
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabFoods", EventData.of("Action", "tab:foods"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabDrinks", EventData.of("Action", "tab:drinks"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabConfig", EventData.of("Action", "tab:config"), false);

        // Bind category filters
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatAll", EventData.of("Action", "cat:all"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatVanilla", EventData.of("Action", "cat:vanilla"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatNoCube", EventData.of("Action", "cat:nocube"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatSnip3", EventData.of("Action", "cat:snip3"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatHiddens", EventData.of("Action", "cat:hiddens"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatGalore", EventData.of("Action", "cat:galore"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatConnor", EventData.of("Action", "cat:connor"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatCaba", EventData.of("Action", "cat:caba"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatZcGaia", EventData.of("Action", "cat:zcgaia"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatHatsuya", EventData.of("Action", "cat:hatsuya"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatEndgame", EventData.of("Action", "cat:endgame"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatSpice", EventData.of("Action", "cat:spice"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatAle", EventData.of("Action", "cat:ale"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatCheff", EventData.of("Action", "cat:cheff"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatOther", EventData.of("Action", "cat:other"), false);

        // Bind search (button click)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SearchButton",
            new EventData().append("Action", "search").append("@SearchQuery", "#SearchInput.Value"));
        
        // Bind search (live search when typing)
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
            new EventData().append("Action", "search").append("@SearchQuery", "#SearchInput.Value"), false);

        // Bind add
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton",
            new EventData().append("Action", "add").append("@NewItemId", "#NewItemId.Value").append("@NewValue", "#NewValue.Value"));
    }

    /**
     * Gets the display name for an item, using translation if available, 
     * otherwise formatting the item ID.
     */
    private String getDisplayName(String itemId, String language) {
        if (itemId == null || itemId.isEmpty()) {
            return "Unknown";
        }

        // Try to get the Item object and its translation
        try {
            Item item = (Item) Item.getAssetMap().getAsset(itemId);
            if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
            if (item != null) {
                String key = item.getTranslationKey();
                if (key != null) {
                    String translated = I18nModule.get().getMessage(language, key);
                    if (translated != null && !translated.isEmpty()) {
                        return translated;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to formatting the ID
        }

        // Fallback: format the item ID as a readable name
        return formatItemId(itemId);
    }

    /**
     * Formats an item ID into a readable display name.
     * Example: "Food_Beef_Raw" -> "Beef Raw"
     */
    private String formatItemId(String itemId) {
        if (itemId == null) return "Unknown";
        
        // Special display name mappings
        if (itemId.equals("EasyHunger_Odre")) return "EasyHunger WaterSkin";
        if (itemId.equals("EasyHunger_WaterBowl")) return "EasyHunger Water Bowl";
        
        String id = itemId;
        
        // Remove namespace prefix if present (e.g., "hytale:Food_Beef")
        if (id.contains(":")) {
            id = id.substring(id.indexOf(":") + 1);
        }
        
        // Remove common prefixes
        String[] prefixesToRemove = {
            "Food_", "Plant_", "Ingredient_", "Halloween_", "Christmas_",
            "NoCube_", "Bowl_", "Brewery_", "AndiemgCheff_", "Drink_"
        };
        
        for (String prefix : prefixesToRemove) {
            if (id.startsWith(prefix)) {
                id = id.substring(prefix.length());
                break;
            }
        }
        
        // Replace underscores with spaces
        return id.replace("_", " ");
    }

    /**
     * Maps config IDs to the actual item IDs for icon display.
     * EasyHunger items use prefixes in config but full IDs for assets.
     */
    private String getIconItemId(String configId) {
        // EasyHunger items: config uses short form, actual item has _Empty suffix
        if (configId.equals("EasyHunger_Odre")) {
            return "EasyHunger_Odre_Empty";
        }
        if (configId.equals("EasyHunger_WaterBowl")) {
            return "EasyHunger_WaterBowl_Empty";
        }
        Item itemWithStar = (Item) Item.getAssetMap().getAsset("*" + configId);
        if (itemWithStar != null) {
            return "*" + configId;
        }
        // Default: use as-is
        return configId;
    }

    private boolean matchesCategory(String itemId) {
        return ItemCategoryFilter.matchesCategory(itemId, category);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull ConfigEventData data) {
        
        if (data == null || data.action == null) return;
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (data.action.startsWith("tab:")) {
            activeTab = data.action.substring(4);
            searchQuery = "";
            category = "all";
            rebuild(ref, store);
        } else if (data.action.startsWith("cat:")) {
            category = data.action.substring(4);
            updateItemList(ref, store);
        } else if (data.action.equals("search")) {
            searchQuery = data.searchQuery != null ? data.searchQuery : "";
            updateItemList(ref, store);
        } else if (data.action.equals("add")) {
            handleAdd(data);
            rebuild(ref, store);
         } else if (data.action.startsWith("invSlot:")) {
            int slot = Integer.parseInt(data.action.substring(8));
            ItemStack stack;
            if (slot < 36) {
                stack = player.getInventory().getStorage().getItemStack((short) slot);
            } else {
                stack = player.getInventory().getHotbar().getItemStack((short)(slot - 36));
            }
            if (stack != null && !stack.isEmpty()) {
                UICommandBuilder cmd = new UICommandBuilder();
                String itemId = stack.getItemId();
                if (itemId != null && itemId.startsWith("*")) {
                    itemId = itemId.substring(1);
                }
                cmd.set("#NewItemId.Value", itemId);
                cmd.set("#InventoryFeedback.Text", "Selected: " + itemId);
                sendUpdate(cmd, new UIEventBuilder(), false);
            }
        } else if (data.action.equals("setValue")) {
            handleSetValue(data);
        } else if (data.action.equals("setConfig")) {
            handleSetConfig(data);
        }
    }
    
    /**
     * Updates only the item list without rebuilding the entire UI.
     * This preserves the search input field value.
     */
    private void updateItemList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        
        String language = playerRef.getLanguage();
        String query = searchQuery.toLowerCase();
        
        Map<String, Float> allItems = activeTab.equals("foods") 
            ? EasyHunger.get().getFoodsConfig().getFoodValues()
            : EasyHunger.get().getDrinksConfig().getDrinkValues();
        
        // Filter by:
        // 1. Item exists in game (installed mods only)
        // 2. Search query (searches both ID and display name)
        // 3. Category filter
        Map<String, Float> items = allItems.entrySet().stream()
            .filter(e -> {
                String itemId = e.getKey();
                // Always show EasyHunger mod items (our own mod)
                if (itemId.startsWith("EasyHunger_")) return true;
                // Always show items that would be categorized as OTHER (custom entries)
                if (ItemCategoryFilter.getCategory(itemId).equals("other")) return true;
                // Check if item exists in game for other mods
                Item item = (Item) Item.getAssetMap().getAsset(itemId);
                if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
                return item != null;
            })
            .filter(e -> {
                if (query.isEmpty()) return true;
                String id = e.getKey().toLowerCase();
                String displayName = getDisplayName(e.getKey(), language).toLowerCase();
                return id.contains(query) || displayName.contains(query);
            })
            .filter(e -> matchesCategory(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // Clear existing list container
        cmd.clear("#ItemList");
        
        itemIds.clear();
        
        int index = 0;
        for (Map.Entry<String, Float> entry : items.entrySet()) {
            String itemId = entry.getKey();
            float value = entry.getValue();
            
            itemIds.add(itemId);
            
            String selector = "#ItemList[" + index + "]";
            cmd.append("#ItemList", "Pages/ItemEntry.ui");
            
            // Set item icon - map EasyHunger config IDs to real item IDs
            String iconId = getIconItemId(itemId);
            cmd.set(selector + " #ItemIcon.ItemId", iconId);
            
            String displayName = getDisplayName(itemId, language);
            cmd.set(selector + " #ItemName.Text", displayName);
            cmd.set(selector + " #ItemName.TooltipTextSpans", Message.raw(itemId));
            cmd.set(selector + " #ValueInput.Value", formatValue(value));
            
            evt.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                selector + " #ValueInput",
                new EventData()
                    .append("Action", "setValue")
                    .append("Index", String.valueOf(index))
                    .append("@Value", selector + " #ValueInput.Value"),
                false
            );
            
            index++;
        }
        
        // Update total label
        String itemType = activeTab.equals("foods") ? "foods" : "drinks";
        if (query.isEmpty() && category.equals("all")) {
            cmd.set("#TotalLabel.Text", "Total: " + index + " " + itemType);
        } else {
            cmd.set("#TotalLabel.Text", "Showing: " + index + " of " + allItems.size() + " " + itemType);
        }
        
        sendUpdate(cmd, evt, false);
    }

    /**
     * Builds the config section with all editable settings
     */
    private void buildConfigSection(UICommandBuilder cmd, UIEventBuilder evt) {
        var config = EasyHunger.get().getConfig();
        
        // Define all config entries: key, label, value, tooltip (only numeric configs)
        Object[][] configs = {
            // Hunger configs
            {"MaxHunger", "Max Hunger", String.valueOf(config.getMaxHunger()), "Maximum hunger level the player can have."},
            {"StarvationTickRate", "Starvation Tick Rate", String.valueOf(config.getStarvationTickRate()), "How often (in seconds) hunger decreases."},
            {"StarvationPerTick", "Starvation Per Tick", String.valueOf(config.getStarvationPerTick()), "Amount of hunger lost per tick."},
            {"StarvationDamage", "Starvation Damage", String.valueOf(config.getStarvationDamage()), "Damage taken when hunger reaches zero."},
            {"HungryThreshold", "Hungry Threshold", String.valueOf(config.getHungryThreshold()), "Hunger level where warnings start appearing."},
            {"BlockBreakHungerCost", "Block Break Hunger Cost", String.valueOf(config.getBlockBreakHungerCost()), "Hunger consumed when breaking blocks."},
            {"JumpHungerCost", "Jump Hunger Cost", String.valueOf(config.getJumpHungerCost()), "Hunger consumed when jumping."},
            // Thirst configs
            {"MaxThirst", "Max Thirst", String.valueOf(config.getMaxThirst()), "Maximum thirst level the player can have."},
            {"ThirstDecayRate", "Thirst Decay Rate", String.valueOf(config.getThirstDecayRate()), "Amount of thirst lost per tick."},
            {"SprintThirstMultiplier", "Sprint Thirst Multiplier", String.valueOf(config.getSprintThirstMultiplier()), "Multiplier applied to thirst decay when sprinting."},
            {"ThirstDamage", "Thirst Damage", String.valueOf(config.getThirstDamage()), "Damage taken when thirst reaches zero."},
            {"ThirstyThreshold", "Thirsty Threshold", String.valueOf(config.getThirstyThreshold()), "Thirst level where warnings start appearing."},
            {"StarvationStaminaModifier", "Stamina Modifier", String.valueOf(config.getStarvationStaminaModifier()), "How much stamina depletion affects hunger decay."},
            
            // Idle Pause configs
            {"IdlePauseEnabled", "Idle Pause Enabled", String.valueOf(config.isIdlePauseEnabled()), "Pause hunger/thirst when standing still."},
            {"IdlePauseSeconds", "Idle Pause Time (s)", String.valueOf(config.getIdlePauseSeconds()), "Time in seconds to wait before pausing decay."}
        };
        
        for (int i = 0; i < configs.length; i++) {
            String key = (String) configs[i][0];
            String label = (String) configs[i][1];
            String value = (String) configs[i][2];
            String tooltip = (String) configs[i][3];
            
            String selector = "#ConfigList[" + i + "]";
            cmd.append("#ConfigList", "Pages/ConfigEntry.ui");
            
            cmd.set(selector + " #ConfigLabel.Text", label);
            cmd.set(selector + " #ConfigValue.Value", value);
            cmd.set(selector + ".TooltipText", tooltip);
            
            evt.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                selector + " #ConfigValue",
                new EventData()
                    .append("Action", "setConfig")
                    .append("ConfigKey", key)
                    .append("@Value", selector + " #ConfigValue.Value"),
                false
            );
        }
    }

    private void handleAdd(ConfigEventData data) {
        if (data.newItemId == null || data.newItemId.trim().isEmpty()) {
            playerRef.sendMessage(Message.empty().insert("Error: Item ID cannot be empty!"));
            return;
        }

        String itemId = data.newItemId.trim();
        float value = 5.0f;

        if (data.newValue != null && !data.newValue.trim().isEmpty()) {
            try {
                value = Float.parseFloat(data.newValue.trim());
            } catch (NumberFormatException e) {
                playerRef.sendMessage(Message.empty().insert("Error: Invalid value!"));
                return;
            }
        }

        if (activeTab.equals("foods")) {
            EasyHunger.get().getFoodsConfig().setFoodValue(itemId, value);
            EasyHunger.get().saveFoodsConfig();
        } else {
            EasyHunger.get().getDrinksConfig().setDrinkValue(itemId, value);
            EasyHunger.get().saveDrinksConfig();
        }
        playerRef.sendMessage(Message.empty().insert("Added: " + itemId + " = " + formatValue(value)));
    }

    private void handleSetValue(ConfigEventData data) {
        if (data.index == null || data.value == null) return;
        
        try {
            int idx = Integer.parseInt(data.index);
            if (idx < 0 || idx >= itemIds.size()) return;
            
            String itemId = itemIds.get(idx);
            float newValue;
            
            try {
                newValue = Float.parseFloat(data.value.trim());
                newValue = Math.max(0, newValue);
            } catch (NumberFormatException e) {
                playerRef.sendMessage(Message.empty().insert("Invalid value for " + itemId));
                return;
            }
            
            if (activeTab.equals("foods")) {
                EasyHunger.get().getFoodsConfig().setFoodValue(itemId, newValue);
                EasyHunger.get().saveFoodsConfig();
            } else {
                EasyHunger.get().getDrinksConfig().setDrinkValue(itemId, newValue);
                EasyHunger.get().saveDrinksConfig();
            }
            playerRef.sendMessage(Message.empty().insert("Updated: " + itemId + " = " + formatValue(newValue)));
            
        } catch (NumberFormatException e) {
            // Ignore invalid index
        }
    }

    private void handleSetConfig(ConfigEventData data) {
        if (data.configKey == null || data.value == null) return;
        
        var config = EasyHunger.get().getConfig();
        String key = data.configKey;
        String valueStr = data.value.trim();
        
        try {
            switch (key) {
                // Integer configs
                case "MaxHunger":
                    config.setMaxHunger(Integer.parseInt(valueStr));
                    break;
                case "MaxThirst":
                    config.setMaxThirst(Integer.parseInt(valueStr));
                    break;
                    
                // Float configs
                case "StarvationTickRate":
                    config.setStarvationTickRate(Float.parseFloat(valueStr));
                    break;
                case "StarvationPerTick":
                    config.setStarvationPerTick(Float.parseFloat(valueStr));
                    break;
                case "StarvationDamage":
                    config.setStarvationDamage(Float.parseFloat(valueStr));
                    break;
                case "HungryThreshold":
                    config.setHungryThreshold(Float.parseFloat(valueStr));
                    break;
                case "BlockBreakHungerCost":
                    config.setBlockBreakHungerCost(Float.parseFloat(valueStr));
                    break;
                case "JumpHungerCost":
                    config.setJumpHungerCost(Float.parseFloat(valueStr));
                    break;
                case "ThirstDecayRate":
                    config.setThirstDecayRate(Float.parseFloat(valueStr));
                    break;
                case "SprintThirstMultiplier":
                    config.setSprintThirstMultiplier(Float.parseFloat(valueStr));
                    break;
                case "ThirstDamage":
                    config.setThirstDamage(Float.parseFloat(valueStr));
                    break;
                case "ThirstyThreshold":
                    config.setThirstyThreshold(Float.parseFloat(valueStr));
                    break;
                case "StarvationStaminaModifier":
                    config.setStarvationStaminaModifier(Float.parseFloat(valueStr));
                    break;
                case "WellFedThreshold":
                    config.setWellFedThreshold(Float.parseFloat(valueStr));
                    break;
                    
                // Idle Pause
                case "IdlePauseEnabled":
                    config.setIdlePauseEnabled(Boolean.parseBoolean(valueStr));
                    break;
                case "IdlePauseSeconds":
                    config.setIdlePauseSeconds(Float.parseFloat(valueStr));
                    break;
                    
                default:
                    playerRef.sendMessage(Message.empty().insert("Unknown config key: " + key));
                    return;
            }
            
            // Save config
            EasyHunger.get().saveConfig();
            playerRef.sendMessage(Message.empty().insert("Config updated: " + key + " = " + valueStr));
            
        } catch (NumberFormatException e) {
            playerRef.sendMessage(Message.empty().insert("Invalid value for " + key + ": " + valueStr));
        }
    }

    /**
     * Renders the player's inventory grid (4 rows of 9 slots + hotbar).
     */
    private void renderInventory(UICommandBuilder cmd, UIEventBuilder evt, Player player) {
        // Main inventory: 4 rows of 9 slots (slots 0-35)
        for (int row = 0; row < 4; row++) {
            String rowId = "#InvRow" + row;
            cmd.clear(rowId);
            for (int i = 0; i < 9; i++) {
                int slot = row * 9 + i;
                cmd.append(rowId, "Pages/EasyHungerInventorySlot.ui");
                String sel = rowId + "[" + i + "]";
                ItemStack stack = player.getInventory().getStorage().getItemStack((short) slot);
                if (stack != null && !stack.isEmpty()) {
                    cmd.set(sel + " #ItemIcon.ItemId", stack.getItemId());
                    if (stack.getQuantity() > 1) {
                        cmd.set(sel + " #Quantity.Text", String.valueOf(stack.getQuantity()));
                        cmd.set(sel + " #Quantity.Visible", true);
                    }
                }
                evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                    EventData.of("Action", "invSlot:" + slot), false);
            }
        }
        // Hotbar: 9 slots (slots 36-44)
        cmd.clear("#HotbarRow");
        for (int i = 0; i < 9; i++) {
            cmd.append("#HotbarRow", "Pages/EasyHungerInventorySlot.ui");
            String sel = "#HotbarRow[" + i + "]";
            ItemStack stack = player.getInventory().getHotbar().getItemStack((short) i);
            if (stack != null && !stack.isEmpty()) {
                cmd.set(sel + " #ItemIcon.ItemId", stack.getItemId());
                if (stack.getQuantity() > 1) {
                    cmd.set(sel + " #Quantity.Text", String.valueOf(stack.getQuantity()));
                    cmd.set(sel + " #Quantity.Visible", true);
                }
            }
            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                EventData.of("Action", "invSlot:" + (36 + i)), false);
        }
    }

    private void rebuild(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new EasyHungerValuesPage(playerRef, activeTab, searchQuery, category));
        }
    }

    private String formatValue(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }

    /**
     * Checks if there are any installed items for a specific category.
     * Used to hide category buttons when no items from that mod are installed.
     */
    private boolean hasCategoryItems(String categoryName, Map<String, Float> allItems) {
        return allItems.keySet().stream().anyMatch(itemId -> {
            // Check if item exists in game (or is from EasyHunger)
            if (!itemId.startsWith("EasyHunger_")) {
                Item item = (Item) Item.getAssetMap().getAsset(itemId);
                if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
                if (item == null) return false;
            }
            // Check category using the filter
            return ItemCategoryFilter.matchesCategory(itemId, categoryName);
        });
    }
}
