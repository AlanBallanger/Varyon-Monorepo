package fr.varyon.shop.ui.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.VaryonShopPlugin;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.config.ShopSettings;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Full editor for one shop catalog entry, 3-column layout: type (item/service toggle, item or
 * command picked via an inventory picker, name, category list) / stock (quantity in stock,
 * currency dropdown) / live preview (icon, name, computed price). The item's sale price is not
 * edited here — it's the shop-wide multiplier (set in ShopAdminDetailPage) applied to the item's
 * buyback price (BuybackPriceRepository), so there's a single source of truth for item values.
 * This is an admin shop editor — player-run shops are a separate, not-yet-built feature.
 */
public final class ShopItemEditorPage extends InteractiveCustomUIPage<ShopAdminEventData> {
    private static final String LAYOUT = "VaryonShop/Admin/ShopItemEditorPage.ui";

    private final VaryonShopPlugin plugin;
    private final PlayerRef playerRef;
    private final String shopId;
    private final int index;
    private boolean firstBuild = true;
    /** Set while re-rendering after a text-field ValueChanged (auto-save). Text field .Value
     * bindings are skipped in that case so we don't fight the client's in-progress keystrokes
     * (re-sending .Value on every keystroke, e.g. on space, drops characters client-side). */
    private boolean isFieldRefresh = false;

    /** true = "Commande" (service) mode is currently selected in the UI toggle. */
    private boolean serviceMode;
    private boolean commandAsPlayer;
    private String selectedCategory;
    private String selectedCurrencyItemId;

    public ShopItemEditorPage(@NotNull VaryonShopPlugin plugin, @NotNull PlayerRef playerRef, @NotNull String shopId, int index) {
        super(playerRef, CustomPageLifetime.CanDismiss, ShopAdminEventData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.shopId = shopId;
        this.index = index;
    }

    private ShopCatalog catalog() {
        return plugin.getShopCatalogRepository().get(shopId).orElse(null);
    }

    private ShopCatalog.Entry entry() {
        ShopCatalog catalog = catalog();
        if (catalog == null || index < 0 || index >= catalog.items.size()) {
            return null;
        }
        return catalog.items.get(index);
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        if (firstBuild) {
            cmd.append(LAYOUT);
            ShopCatalog.Entry entry = entry();
            if (entry != null) {
                serviceMode = entry.isService();
                commandAsPlayer = entry.commandAsPlayer;
                selectedCategory = entry.category;
                selectedCurrencyItemId = entry.currencyItemId;
            }
            firstBuild = false;
        }
        renderDynamic(ref, store, cmd, evt);
    }

    private void renderDynamic(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder cmd, UIEventBuilder evt) {
        ShopCatalog.Entry entry = entry();
        if (entry == null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, shopId));
            }
            return;
        }

        cmd.set("#EditorTitle.Text", "OBJET " + (index + 1) + " - " + shopId.toUpperCase());

        cmd.set("#ModeItemButton.TooltipText", serviceMode ? "" : "Actif");
        cmd.set("#ModeCommandButton.TooltipText", serviceMode ? "Actif" : "");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ModeItemButton", EventData.of("Action", "setModeItem"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ModeCommandButton", EventData.of("Action", "setModeCommand"), false);

        cmd.set("#ItemModeGroup.Visible", !serviceMode);
        cmd.set("#CommandModeGroup.Visible", serviceMode);

        cmd.set("#ItemIdLabel.Text", entry.itemId == null ? "(aucun objet sélectionné)" : entry.itemId);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PickItemButton", EventData.of("Action", "pickItem"), false);

        if (!isFieldRefresh) {
            cmd.set("#CommandInput.Value", entry.command == null ? "" : entry.command);
        }
        cmd.set("#IconItemIdLabel.Text", entry.iconItemId == null ? "(aucune icône sélectionnée)" : entry.iconItemId);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PickIconButton", EventData.of("Action", "pickIcon"), false);

        cmd.set("#CommandModeServerButton.TooltipText", commandAsPlayer ? "" : "Actif");
        cmd.set("#CommandModePlayerButton.TooltipText", commandAsPlayer ? "Actif" : "");
        cmd.set("#CommandModeHint.Text", commandAsPlayer
                ? "Exécutée par le joueur qui achète (ses propres permissions)."
                : "Exécutée par le serveur (console, toutes permissions).");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CommandModeServerButton", EventData.of("Action", "setCommandModeServer"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CommandModePlayerButton", EventData.of("Action", "setCommandModePlayer"), false);

        if (!isFieldRefresh) {
            cmd.set("#NameInput.Value", entry.name == null ? "" : entry.name);
            cmd.set("#StockMinInput.Value", String.valueOf(entry.maxPerPlayerMin));
            cmd.set("#StockMaxInput.Value", String.valueOf(entry.maxPerPlayerMax));
        }

        renderCategoryOptions(cmd, evt);
        renderCurrencyDropdown(cmd, evt);
        renderPreview(cmd, entry);

        EventData autoSaveEvent = new EventData().append("Action", "save")
                .append("@Command", "#CommandInput.Value")
                .append("@Name", "#NameInput.Value")
                .append("@MaxPerPlayerMin", "#StockMinInput.Value")
                .append("@MaxPerPlayerMax", "#StockMaxInput.Value")
                .append("@CurrencyItemId", "#CurrencyDropdown.Value")
                .append("@Category", "#CategoryDropdown.Value");
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CommandInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NameInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CategoryDropdown", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#StockMinInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#StockMaxInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CurrencyDropdown", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteItemButton", EventData.of("Action", "delete"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackToShopButton", EventData.of("Action", "back"), false);
    }

    private void renderCategoryOptions(UICommandBuilder cmd, UIEventBuilder evt) {
        Set<String> suggestions = new LinkedHashSet<>();
        ShopCatalog catalog = catalog();
        if (catalog != null) {
            for (ShopCatalog.Entry other : catalog.items) {
                if (other.category != null && !other.category.isBlank()) {
                    suggestions.add(other.category);
                }
            }
        }
        suggestions.addAll(plugin.getBuybackPriceRepository().listCategories());
        suggestions.add("Divers");

        List<DropdownEntryInfo> entries = new ArrayList<>();
        for (String category : suggestions) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(category), category));
        }
        cmd.set("#CategoryDropdown.Entries", entries);
        cmd.set("#CategoryDropdown.Value", selectedCategory == null ? "Divers" : selectedCategory);
    }

    private void renderCurrencyDropdown(UICommandBuilder cmd, UIEventBuilder evt) {
        List<DropdownEntryInfo> entries = new ArrayList<>();
        for (ShopSettings.CurrencyOption option : plugin.getShopSettings().listCurrencies()) {
            String value = option.itemId == null ? "" : option.itemId;
            String label = option.label == null ? "Coins" : option.label;
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(label), value));
        }
        cmd.set("#CurrencyDropdown.Entries", entries);
        cmd.set("#CurrencyDropdown.Value", selectedCurrencyItemId == null ? "" : selectedCurrencyItemId);
    }

    private void renderPreview(UICommandBuilder cmd, ShopCatalog.Entry entry) {
        String iconId = entry.isService() ? entry.iconItemId : entry.itemId;
        cmd.set("#PreviewIconWrap.Visible", iconId != null);
        if (iconId != null) {
            cmd.set("#PreviewIcon.ItemId", iconId);
        }
        String name = (entry.name != null && !entry.name.isBlank()) ? entry.name : formatItemName(entry.itemId);
        cmd.set("#PreviewName.Text", name.isBlank() ? "(sans nom)" : name);

        double basePrice = (entry.itemId != null && !entry.itemId.isBlank()) ? plugin.getBuybackPriceRepository().priceOf(entry.itemId) : 0.0;
        ShopCatalog catalog = catalog();
        double multiplierMid = catalog == null ? 1.0 : (catalog.priceMultiplierMin + catalog.priceMultiplierMax) / 2.0;
        double previewPrice = basePrice * multiplierMid;
        String currencyLabel = currencyLabelOf(selectedCurrencyItemId);
        cmd.set("#PreviewPrice.Text", String.format("%.2f", previewPrice) + " " + currencyLabel);
    }

    private String currencyLabelOf(String currencyItemId) {
        for (ShopSettings.CurrencyOption option : plugin.getShopSettings().listCurrencies()) {
            boolean matches = (option.itemId == null) ? (currencyItemId == null) : option.itemId.equalsIgnoreCase(currencyItemId);
            if (matches) {
                return option.label == null ? "Coins" : option.label;
            }
        }
        return "Coins";
    }

    @Override
    public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store, ShopAdminEventData data) {
        if (data == null || data.getAction() == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        ShopCatalog.Entry entry = entry();
        if (entry == null) {
            player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, shopId));
            return;
        }

        switch (data.getAction()) {
            case "setModeItem" -> serviceMode = false;
            case "setModeCommand" -> serviceMode = true;
            case "setCommandModeServer" -> {
                commandAsPlayer = false;
                entry.commandAsPlayer = false;
                plugin.getShopCatalogRepository().save(shopId);
            }
            case "setCommandModePlayer" -> {
                commandAsPlayer = true;
                entry.commandAsPlayer = true;
                plugin.getShopCatalogRepository().save(shopId);
            }
            case "pickItem" -> {
                player.getPageManager().openCustomPage(ref, store,
                        new ShopItemInventoryPickerPage(plugin, playerRef, shopId, index, ShopItemInventoryPickerPage.Target.ITEM_ID));
                return;
            }
            case "pickIcon" -> {
                player.getPageManager().openCustomPage(ref, store,
                        new ShopItemInventoryPickerPage(plugin, playerRef, shopId, index, ShopItemInventoryPickerPage.Target.ICON_ITEM_ID));
                return;
            }
            case "save" -> {
                persistFormFields(data, entry);
                if (!serviceMode) {
                    entry.command = null;
                } else {
                    entry.itemId = null;
                }
                plugin.getShopCatalogRepository().save(shopId);
                isFieldRefresh = true;
            }
            case "delete" -> {
                ShopCatalog catalog = catalog();
                if (catalog != null && index >= 0 && index < catalog.items.size()) {
                    catalog.items.remove(index);
                    plugin.getShopCatalogRepository().save(shopId);
                }
                player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, shopId));
                return;
            }
            case "back" -> {
                player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, shopId));
                return;
            }
            default -> { }
        }

        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        renderDynamic(ref, store, refreshCmd, refreshEvt);
        isFieldRefresh = false;
        sendUpdate(refreshCmd, refreshEvt, false);
    }

    /** Writes the current form field values into the entry (not itemId/iconItemId, set via the inventory picker). */
    private void persistFormFields(ShopAdminEventData data, ShopCatalog.Entry entry) {
        entry.command = blankToNull(data.getCommand());
        entry.name = blankToNull(data.getName());
        entry.maxPerPlayerMin = Math.max(0, parseInt(data.getMaxPerPlayerMin(), entry.maxPerPlayerMin));
        entry.maxPerPlayerMax = Math.max(0, parseInt(data.getMaxPerPlayerMax(), entry.maxPerPlayerMax));
        selectedCategory = data.getCategoryFree() == null || data.getCategoryFree().isBlank() ? "Divers" : data.getCategoryFree();
        entry.category = selectedCategory;
        selectedCurrencyItemId = blankToNull(data.getCurrencyItemId());
        entry.currencyItemId = selectedCurrencyItemId;
    }

    private String blankToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }

    private String formatItemName(String itemId) {
        if (itemId == null) {
            return "";
        }
        String cleaned = itemId;
        int colon = cleaned.lastIndexOf(':');
        if (colon >= 0) {
            cleaned = cleaned.substring(colon + 1);
        }
        return cleaned.replace('_', ' ');
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
