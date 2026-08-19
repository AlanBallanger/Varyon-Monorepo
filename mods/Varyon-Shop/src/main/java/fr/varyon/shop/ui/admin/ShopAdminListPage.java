package fr.varyon.shop.ui.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import fr.varyon.shop.VaryonShopPlugin;
import fr.varyon.shop.config.MerchantRegistry;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.config.ShopSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

/**
 * Root admin panel screen, with two tabs: "Réglages" (general buyback rate, currencies) and
 * "Boutiques" (list of shop catalogs — create/open/delete, showing how many NPCs each is bound
 * to). Opened via "/vshop" with no arguments. The list of merchant NPCs bound to a given shop
 * lives in that shop's detail screen (ShopAdminDetailPage), not here.
 */
public final class ShopAdminListPage extends InteractiveCustomUIPage<ShopAdminEventData> {
    private static final String LAYOUT = "VaryonShop/Admin/ShopAdminListPage.ui";

    private enum Tab { SETTINGS, SHOPS }

    private final VaryonShopPlugin plugin;
    private final PlayerRef playerRef;
    private boolean firstBuild = true;
    private Tab activeTab = Tab.SHOPS;

    public ShopAdminListPage(@NotNull VaryonShopPlugin plugin, @NotNull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ShopAdminEventData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        if (firstBuild) {
            cmd.append(LAYOUT);
            firstBuild = false;
        }
        renderDynamic(cmd, evt);
    }

    private void renderDynamic(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.set("#SettingsTabButton.TooltipText", activeTab == Tab.SETTINGS ? "Actif" : "");
        cmd.set("#ShopsTabButton.TooltipText", activeTab == Tab.SHOPS ? "Actif" : "");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsTabButton", EventData.of("Action", "tabSettings"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ShopsTabButton", EventData.of("Action", "tabShops"), false);

        cmd.set("#SettingsTabContent.Visible", activeTab == Tab.SETTINGS);
        cmd.set("#ShopsTabContent.Visible", activeTab == Tab.SHOPS);

        if (activeTab == Tab.SETTINGS) {
            renderSettingsTab(cmd, evt);
        } else {
            renderShopsTab(cmd, evt);
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#AdminCloseButton", EventData.of("Action", "close"), false);
    }

    private void renderSettingsTab(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.set("#GeneralRateInput.Value", String.valueOf(Math.round(plugin.getShopSettings().generalBuybackRate() * 100)));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GeneralRateInput",
                new EventData().append("Action", "setRate").append("@GeneralRate", "#GeneralRateInput.Value"), false);

        cmd.set("#BuybackBonusInput.Value", String.valueOf(Math.round(plugin.getShopSettings().buybackBonus() * 100)));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BuybackBonusInput",
                new EventData().append("Action", "setBuybackBonus").append("@BuybackBonus", "#BuybackBonusInput.Value"), false);

        cmd.set("#PurchaseBonusInput.Value", String.valueOf(Math.round(plugin.getShopSettings().purchaseBonus() * 100)));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PurchaseBonusInput",
                new EventData().append("Action", "setPurchaseBonus").append("@PurchaseBonus", "#PurchaseBonusInput.Value"), false);

        cmd.clear("#CurrencyList");
        List<ShopSettings.CurrencyOption> currencies = plugin.getShopSettings().listCurrencies();
        for (int i = 0; i < currencies.size(); i++) {
            ShopSettings.CurrencyOption currency = currencies.get(i);
            String selector = "#CurrencyList[" + i + "]";
            cmd.append("#CurrencyList", "VaryonShop/Admin/CurrencyListItem.ui");
            String label = (currency.label == null ? "Coins" : currency.label) + (currency.itemId == null ? "" : "  (" + currency.itemId + ")");
            cmd.set(selector + " #CurrencyRowLabel.Text", escape(label));
            cmd.set(selector + " #CurrencyRemoveButton.Visible", currency.itemId != null);
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #CurrencyRemoveButton",
                    EventData.of("Action", "removeCurrency").append("ShopId", currency.itemId == null ? "" : currency.itemId), false);
        }
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddCurrencyButton", EventData.of("Action", "pickCurrency"), false);
    }

    private void renderShopsTab(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.clear("#ShopList");
        List<String> shopIds = plugin.getShopCatalogRepository().listShopIds();
        Collections.sort(shopIds);
        cmd.set("#ShopEmptyLabel.Visible", shopIds.isEmpty());

        Map<String, Integer> merchantCounts = new HashMap<>();
        for (UUID id : plugin.getMerchantRegistry().listBoundDenizenIds()) {
            MerchantRegistry.Binding binding = plugin.getMerchantRegistry().bindingOf(id);
            if (binding != null && binding.shopId() != null) {
                merchantCounts.merge(binding.shopId(), 1, Integer::sum);
            }
        }

        for (int i = 0; i < shopIds.size(); i++) {
            String shopId = shopIds.get(i);
            ShopCatalog catalog = plugin.getShopCatalogRepository().get(shopId).orElse(null);
            String selector = "#ShopList[" + i + "]";
            cmd.append("#ShopList", "VaryonShop/Admin/ShopListItem.ui");
            int merchantCount = merchantCounts.getOrDefault(shopId, 0);
            String merchantText = merchantCount == 0 ? "aucun marchand"
                    : merchantCount == 1 ? "1 marchand" : merchantCount + " marchands";
            String label = catalog == null ? shopId : catalog.displayName + "  |  " + catalog.items.size() + " objet(s)  |  " + merchantText;
            cmd.set(selector + " #ShopRowLabel.Text", escape(label));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #ShopOpenButton",
                    EventData.of("Action", "openShop").append("ShopId", shopId), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #ShopDeleteButton",
                    EventData.of("Action", "deleteShop").append("ShopId", shopId), false);
        }

        int createIndex = shopIds.size();
        String createSelector = "#ShopList[" + createIndex + "]";
        cmd.append("#ShopList", "VaryonShop/Admin/ShopCreateRow.ui");
        evt.addEventBinding(CustomUIEventBindingType.Activating, createSelector + " #ShopCreateButton", EventData.of("Action", "createShop"), false);
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

        switch (data.getAction()) {
            case "tabSettings" -> activeTab = Tab.SETTINGS;
            case "tabShops" -> activeTab = Tab.SHOPS;
            case "createShop" -> {
                String shopId = plugin.getShopCatalogRepository().createDraft();
                player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, shopId));
                return;
            }
            case "deleteShop" -> {
                String shopId = data.getShopId();
                if (shopId != null && plugin.getShopCatalogRepository().delete(shopId)) {
                    playerRef.sendMessage(Message.raw("Varyon-Shop: boutique '" + shopId + "' supprimee."));
                }
            }
            case "openShop" -> {
                String shopId = data.getShopId();
                if (shopId != null && plugin.getShopCatalogRepository().exists(shopId)) {
                    player.getPageManager().openCustomPage(ref, store, new ShopAdminDetailPage(plugin, playerRef, shopId));
                    return;
                }
            }
            case "pickCurrency" -> {
                player.getPageManager().openCustomPage(ref, store,
                        new ShopItemInventoryPickerPage(plugin, playerRef, null, -1, ShopItemInventoryPickerPage.Target.CURRENCY));
                return;
            }
            case "removeCurrency" -> {
                String itemId = data.getShopId();
                if (itemId != null && !itemId.isBlank()) {
                    plugin.getShopSettings().removeCurrency(itemId);
                }
            }
            case "setRate" -> {
                String raw = data.getGeneralRate();
                try {
                    double value = Double.parseDouble(raw == null ? "" : raw.trim().replace(',', '.'));
                    double rate = value > 1.0 ? value / 100.0 : value;
                    plugin.getShopSettings().setGeneralBuybackRate(rate);
                    playerRef.sendMessage(Message.raw("Varyon-Shop: taux de rachat general regle sur " + Math.round(rate * 100) + "%."));
                } catch (NumberFormatException e) {
                    playerRef.sendMessage(Message.raw("Varyon-Shop: taux invalide."));
                }
            }
            case "setBuybackBonus" -> {
                String raw = data.getBuybackBonus();
                try {
                    double value = Double.parseDouble(raw == null ? "" : raw.trim().replace(',', '.'));
                    double bonus = value > 1.0 ? value / 100.0 : value;
                    plugin.getShopSettings().setBuybackBonus(bonus);
                } catch (NumberFormatException ignored) {
                }
            }
            case "setPurchaseBonus" -> {
                String raw = data.getPurchaseBonus();
                try {
                    double value = Double.parseDouble(raw == null ? "" : raw.trim().replace(',', '.'));
                    double bonus = value > 1.0 ? value / 100.0 : value;
                    plugin.getShopSettings().setPurchaseBonus(bonus);
                } catch (NumberFormatException ignored) {
                }
            }
            case "close" -> {
                player.getPageManager().setPage(ref, store, Page.None);
                return;
            }
            default -> { }
        }

        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        renderDynamic(refreshCmd, refreshEvt);
        sendUpdate(refreshCmd, refreshEvt, false);
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
