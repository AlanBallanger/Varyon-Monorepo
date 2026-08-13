package fr.varyon.shop.ui.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.VaryonShopPlugin;
import fr.varyon.shop.config.MerchantRegistry;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.util.ItemRarityUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Detail/settings screen for a single shop catalog: display name, rotation size, price
 * multiplier range (auto-saved on change), a visual grid of its items/services (icon + price,
 * click to edit, trailing "+" tile to add a new one), and the list of merchant NPCs bound to
 * this shop (with unbind).
 */
public final class ShopAdminDetailPage extends InteractiveCustomUIPage<ShopAdminEventData> {
    private static final String LAYOUT = "VaryonShop/Admin/ShopAdminDetailPage.ui";
    private static final int ITEMS_PER_ROW = 7;

    private final VaryonShopPlugin plugin;
    private final PlayerRef playerRef;
    private final String shopId;
    private boolean firstBuild = true;
    private int builtItemRows = -1;
    /** Set while re-rendering after a text-field ValueChanged (auto-save), to avoid resending
     * .Value and fighting the client's in-progress keystrokes (drops chars, e.g. spaces). */
    private boolean isFieldRefresh = false;
    /** Debounces the shopId rename triggered by a display-name edit: renaming (and reopening the
     * page) on every keystroke would drop the client's typing focus, so we wait for a pause. */
    @Nullable
    private ScheduledFuture<?> renameDebounceTask;
    private static final long RENAME_DEBOUNCE_MS = 900;

    public ShopAdminDetailPage(@NotNull VaryonShopPlugin plugin, @NotNull PlayerRef playerRef, @NotNull String shopId) {
        super(playerRef, CustomPageLifetime.CanDismiss, ShopAdminEventData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.shopId = shopId;
    }

    private ShopCatalog catalog() {
        return plugin.getShopCatalogRepository().get(shopId).orElse(null);
    }

    private void scheduleRenameDebounced() {
        if (renameDebounceTask != null) {
            renameDebounceTask.cancel(false);
        }
        renameDebounceTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(this::performDebouncedRename,
                RENAME_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private void performDebouncedRename() {
        renameDebounceTask = null;
        Ref<EntityStore> liveRef = playerRef.getReference();
        if (liveRef == null || !liveRef.isValid() || liveRef.getStore() == null) {
            return;
        }
        EntityStore es = liveRef.getStore().getExternalData();
        if (es == null || es.getWorld() == null) {
            return;
        }
        es.getWorld().execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null || player.getPageManager() == null) {
                return;
            }
            if (player.getPageManager().getCustomPage() != this) {
                return;
            }
            ShopCatalog catalog = catalog();
            if (catalog == null || catalog.displayName == null || catalog.displayName.isBlank()) {
                return;
            }
            String slug = plugin.getShopCatalogRepository().slugify(catalog.displayName, shopId);
            String newId = plugin.getShopCatalogRepository().rename(shopId, slug);
            if (!newId.equals(shopId)) {
                plugin.getMerchantRegistry().renameShopId(shopId, newId);
                plugin.getShopRotationState().renameShopId(shopId, newId);
                plugin.getShopPurchaseTracker().renameShopId(shopId, newId);
                player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, newId));
            }
        });
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        if (firstBuild) {
            cmd.append(LAYOUT);
            firstBuild = false;
        }
        renderDynamic(ref, store, cmd, evt);
    }

    @Override
    public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
        if (renameDebounceTask != null) {
            renameDebounceTask.cancel(false);
            renameDebounceTask = null;
        }
    }

    private void ensureItemGridSize(UICommandBuilder cmd, int itemCount) {
        int slotCount = itemCount + 1; // trailing "+" tile
        int neededRows = (slotCount + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        if (neededRows == builtItemRows) {
            return;
        }
        cmd.clear("#ShopItemGrid");
        for (int row = 0; row < neededRows; row++) {
            cmd.append("#ShopItemGrid", "VaryonShop/Admin/ShopItemGridRow.ui");
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                cmd.append("#ShopItemGrid[" + row + "]", "VaryonShop/Admin/ShopItemTile.ui");
            }
        }
        builtItemRows = neededRows;
    }

    private void renderDynamic(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder cmd, UIEventBuilder evt) {
        ShopCatalog catalog = catalog();
        if (catalog == null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().openCustomPage(ref, store, new ShopAdminListPage(plugin, playerRef));
            }
            return;
        }

        cmd.set("#ShopTitle.Text", "BOUTIQUE: " + shopId.toUpperCase());
        if (!isFieldRefresh) {
            cmd.set("#DisplayNameInput.Value", catalog.displayName == null ? "" : catalog.displayName);
            cmd.set("#MaxVisibleInput.Value", String.valueOf(catalog.maxVisibleItems));
            cmd.set("#PriceMultMinInput.Value", String.valueOf(catalog.priceMultiplierMin));
            cmd.set("#PriceMultMaxInput.Value", String.valueOf(catalog.priceMultiplierMax));
        }

        EventData autoSaveEvent = new EventData().append("Action", "saveSettings")
                .append("@DisplayName", "#DisplayNameInput.Value")
                .append("@MaxVisible", "#MaxVisibleInput.Value")
                .append("@PriceMultMin", "#PriceMultMinInput.Value")
                .append("@PriceMultMax", "#PriceMultMaxInput.Value");
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DisplayNameInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MaxVisibleInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceMultMinInput", autoSaveEvent, false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceMultMaxInput", autoSaveEvent, false);

        ensureItemGridSize(cmd, catalog.items.size());
        int slotCount = catalog.items.size() + 1;
        int rowCount = (slotCount + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int i = row * ITEMS_PER_ROW + col;
                String selector = "#ShopItemGrid[" + row + "][" + col + "]";
                if (i >= slotCount) {
                    cmd.set(selector + ".Visible", false);
                    continue;
                }
                cmd.set(selector + ".Visible", true);
                if (i == catalog.items.size()) {
                    cmd.set(selector + " #ShopTileIconWrap.Visible", false);
                    cmd.set(selector + " #ShopTilePlus.Visible", true);
                    cmd.set(selector + " #ShopTileName.Text", "");
                    cmd.set(selector + " #ShopTilePrice.Text", "");
                    cmd.set(selector + " #ShopTileButton.TooltipText", "Ajouter un objet");
                    evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #ShopTileButton", EventData.of("Action", "addItem"), false);
                    continue;
                }
                ShopCatalog.Entry entry = catalog.items.get(i);
                cmd.set(selector + " #ShopTileIconWrap.Visible", true);
                cmd.set(selector + " #ShopTilePlus.Visible", false);
                cmd.set(selector + " #ShopTileIcon.ItemId", entry.displayIconItemId());
                double basePrice = (entry.itemId != null && !entry.itemId.isBlank()) ? plugin.getBuybackPriceRepository().priceOf(entry.itemId) : 0.0;
                String name = (entry.name != null && !entry.name.isBlank()) ? entry.name : formatItemName(entry.itemId);
                String priceText = String.format("%.2f", basePrice) + (entry.hasItemCurrency() ? "" : " Coins");
                cmd.set(selector + " #ShopTileName.Text", name.isBlank() ? "?" : name);
                cmd.set(selector + " #ShopTilePrice.Text", priceText);
                cmd.set(selector + " #ShopTileButton.TooltipText", name + (entry.isService() ? " (service)" : "") + " - " + priceText);
                String rarity = ItemRarityUtil.rarityOf(entry.itemId);
                cmd.set(selector + " #ShopTileRarityBg.AssetPath", ItemRarityUtil.rarityTexturePath(rarity));
                cmd.set(selector + " #ShopTileRarityBg.Visible", true);
                int index = i;
                evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #ShopTileButton",
                        EventData.of("Action", "editItem").append("Index", String.valueOf(index)), false);
            }
        }
        cmd.set("#ItemEmptyLabel.Visible", catalog.items.isEmpty());

        renderMerchants(cmd, evt);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BindNpcButton", EventData.of("Action", "bindNpc"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackToListButton", EventData.of("Action", "backToList"), false);
    }

    private void renderMerchants(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.clear("#MerchantList");
        java.util.List<UUID> bound = new java.util.ArrayList<>();
        for (UUID id : plugin.getMerchantRegistry().listBoundDenizenIds()) {
            MerchantRegistry.Binding binding = plugin.getMerchantRegistry().bindingOf(id);
            if (binding != null && shopId.equalsIgnoreCase(binding.shopId())) {
                bound.add(id);
            }
        }
        cmd.set("#MerchantEmptyLabel.Visible", bound.isEmpty());
        for (int i = 0; i < bound.size(); i++) {
            UUID id = bound.get(i);
            String name = plugin.getDenizensBridge().getName(id);
            String selector = "#MerchantList[" + i + "]";
            cmd.append("#MerchantList", "VaryonShop/Admin/NpcListItem.ui");
            cmd.set(selector + " #NpcRowLabel.Text", escape(name == null ? id.toString() : name));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #NpcUnbindButton",
                    EventData.of("Action", "unbindNpc").append("ShopId", id.toString()), false);
        }
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
        ShopCatalog catalog = catalog();
        if (catalog == null) {
            player.getPageManager().openCustomPage(ref, store, new ShopAdminListPage(plugin, playerRef));
            return;
        }

        switch (data.getAction()) {
            case "saveSettings" -> {
                String previousName = catalog.displayName;
                String displayName = data.getDisplayName();
                catalog.displayName = displayName == null ? "" : displayName.trim();
                catalog.maxVisibleItems = parseInt(data.getMaxVisible(), catalog.maxVisibleItems);
                catalog.priceMultiplierMin = parseDouble(data.getPriceMultMin(), catalog.priceMultiplierMin);
                catalog.priceMultiplierMax = parseDouble(data.getPriceMultMax(), catalog.priceMultiplierMax);
                plugin.getShopCatalogRepository().save(shopId);
                boolean nameChanged = !catalog.displayName.equals(previousName == null ? "" : previousName);
                if (nameChanged && !catalog.displayName.isBlank()) {
                    scheduleRenameDebounced();
                }
                isFieldRefresh = true;
            }
            case "addItem" -> {
                ShopCatalog.Entry entry = new ShopCatalog.Entry();
                entry.category = "Divers";
                catalog.items.add(entry);
                plugin.getShopCatalogRepository().save(shopId);
                player.getPageManager().openCustomPage(ref, store,
                        new ShopItemEditorPage(plugin, playerRef, shopId, catalog.items.size() - 1));
                return;
            }
            case "editItem" -> {
                int index = parseInt(data.getIndex(), -1);
                if (index >= 0 && index < catalog.items.size()) {
                    player.getPageManager().openCustomPage(ref, store, new ShopItemEditorPage(plugin, playerRef, shopId, index));
                    return;
                }
            }
            case "bindNpc" -> {
                UUID playerUuid = playerRef.getUuid();
                if (playerUuid != null) {
                    plugin.getBindWandManager().armBind(playerUuid, MerchantRegistry.MerchantType.MARKET_SHOP, null, shopId);
                    playerRef.sendMessage(Message.raw("Varyon-Shop: interagissez (clic droit) avec le NPC a lier a la boutique '" + shopId + "' (60s)."));
                }
                player.getPageManager().setPage(ref, store, Page.None);
                return;
            }
            case "unbindNpc" -> {
                String rawId = data.getShopId();
                if (rawId != null) {
                    try {
                        UUID id = UUID.fromString(rawId);
                        plugin.getMerchantRegistry().unbind(id);
                        playerRef.sendMessage(Message.raw("Varyon-Shop: NPC delie."));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case "backToList" -> {
                player.getPageManager().openCustomPage(ref, store, new ShopAdminListPage(plugin, playerRef));
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

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
