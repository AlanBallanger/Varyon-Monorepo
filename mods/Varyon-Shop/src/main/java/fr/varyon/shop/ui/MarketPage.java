package fr.varyon.shop.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.config.BuybackPriceRepository;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.config.ShopPurchaseTracker;
import fr.varyon.shop.config.ShopRotationState;
import fr.varyon.shop.economy.VaultEconomyBridge;
import fr.varyon.shop.merchant.market.ShopCommandExecutor;
import fr.varyon.shop.util.EntityApiCompat;
import fr.varyon.shop.util.ItemRarityUtil;
import fr.varyon.shop.util.WorldDayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Purchase window for a shop catalog: categories on the left, an item grid in the middle (built
 * as explicit rows so items are left-aligned — LeftCenterWrap centers incomplete rows, which
 * isn't wanted here), and a detail panel on the right (icon, name, price, quantity input, buy
 * button).
 *
 * Rotation: only catalog.maxVisibleItems items (if set) are offered, re-picked once per Hytale
 * day (see ShopRotationState). Prices are looked up from BuybackPriceRepository by itemId (0 if
 * unlisted) times a multiplier drawn once per rotation from [priceMultiplierMin, priceMultiplierMax]
 * (same multiplier shop-wide). Services (no itemId, has command) have no buyback price and are
 * always free unless an admin sets one directly in buyback_prices.json under their iconItemId.
 */
public final class MarketPage extends InteractiveCustomUIPage<MarketPage.EventDataPayload> {
    public static final String LAYOUT = "VaryonShop/Market/MarketPage.ui";
    private static final int MAX_QTY = 999;
    private static final int ITEMS_PER_ROW = 5;

    private final ShopCatalog catalog;
    private final String shopId;
    private final VaultEconomyBridge economyBridge;
    private final BuybackPriceRepository priceRepository;
    private final ShopPurchaseTracker purchaseTracker;
    private final PlayerRef playerRef;
    private final double priceMultiplier;
    private final double purchaseBonus;
    private final List<Integer> visibleIndexes;
    private final java.util.Map<Integer, Integer> rolledStock;
    private boolean firstBuild = true;
    private int builtItemRows = 0;
    private int builtCategoryCount = 0;
    @Nullable
    private ScheduledFuture<?> rotationTimerTask;

    private final List<String> categories = new ArrayList<>();
    private String selectedCategory;
    private int selectedItemIndex = -1;
    private int chosenQty = 1;

    public MarketPage(
            @NotNull ShopCatalog catalog,
            @NotNull String shopId,
            @NotNull VaultEconomyBridge economyBridge,
            @NotNull BuybackPriceRepository priceRepository,
            @NotNull ShopPurchaseTracker purchaseTracker,
            @NotNull PlayerRef playerRef,
            @NotNull ShopRotationState.RotationEntry rotation
    ) {
        this(catalog, shopId, economyBridge, priceRepository, purchaseTracker, playerRef, rotation, 0.0);
    }

    /** purchaseBonus: server-wide bonus added on top of the shop's rolled price multiplier, e.g. 0.2 = +20 points. */
    public MarketPage(
            @NotNull ShopCatalog catalog,
            @NotNull String shopId,
            @NotNull VaultEconomyBridge economyBridge,
            @NotNull BuybackPriceRepository priceRepository,
            @NotNull ShopPurchaseTracker purchaseTracker,
            @NotNull PlayerRef playerRef,
            @NotNull ShopRotationState.RotationEntry rotation,
            double purchaseBonus
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, EventDataPayload.CODEC);
        this.catalog = catalog;
        this.shopId = shopId;
        this.economyBridge = economyBridge;
        this.priceRepository = priceRepository;
        this.purchaseTracker = purchaseTracker;
        this.playerRef = playerRef;
        this.priceMultiplier = rotation.priceMultiplier;
        this.purchaseBonus = Math.max(0.0, purchaseBonus);
        this.visibleIndexes = rotation.visibleIndexes.isEmpty() && catalog.maxVisibleItems <= 0
                ? allIndexes(catalog)
                : rotation.visibleIndexes;
        this.rolledStock = rotation.rolledStock;
        rebuildCategoryList();
    }

    private static List<Integer> allIndexes(ShopCatalog catalog) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < catalog.items.size(); i++) {
            all.add(i);
        }
        return all;
    }

    private static final String ALL_CATEGORY = "Tous";

    private void rebuildCategoryList() {
        Set<String> distinct = new LinkedHashSet<>();
        for (ShopCatalog.Entry entry : visibleEntries()) {
            distinct.add(entry.category == null || entry.category.isBlank() ? "Divers" : entry.category);
        }
        categories.clear();
        categories.add(ALL_CATEGORY);
        categories.addAll(distinct);
        if (selectedCategory == null) {
            selectedCategory = ALL_CATEGORY;
        }
    }

    private List<ShopCatalog.Entry> visibleEntries() {
        List<ShopCatalog.Entry> result = new ArrayList<>();
        for (int index : visibleIndexes) {
            if (index >= 0 && index < catalog.items.size()) {
                result.add(catalog.items.get(index));
            }
        }
        return result;
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        if (firstBuild) {
            cmd.append(LAYOUT);
            firstBuild = false;
            startRotationTimer();
        }
        renderDynamic(cmd, evt);
    }

    @Override
    public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
        cancelRotationTimer();
    }

    private void startRotationTimer() {
        try {
            rotationTimerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
                try {
                    tickRotationTimer();
                } catch (Exception ignored) {
                }
            }, 1L, 1L, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private void cancelRotationTimer() {
        ScheduledFuture<?> task = rotationTimerTask;
        if (task != null) {
            task.cancel(false);
            rotationTimerTask = null;
        }
    }

    @Nullable
    private World currentWorld() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || ref.getStore() == null) {
            return null;
        }
        EntityStore es = ref.getStore().getExternalData();
        return es == null ? null : es.getWorld();
    }

    private void tickRotationTimer() {
        if (rotationTimerTask == null) {
            return;
        }
        Ref<EntityStore> liveRef = playerRef.getReference();
        if (liveRef == null || !liveRef.isValid()) {
            cancelRotationTimer();
            return;
        }
        Player player = liveRef.getStore().getComponent(liveRef, Player.getComponentType());
        if (player == null || player.getPageManager() == null) {
            cancelRotationTimer();
            return;
        }
        CustomUIPage activePage = player.getPageManager().getCustomPage();
        if (activePage != this) {
            cancelRotationTimer();
            return;
        }
        World world = currentWorld();
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#RotationBannerLabel.Text", rotationCountdownText(world));
        sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private String rotationCountdownText(World world) {
        long remaining = WorldDayUtil.secondsUntilNextRotation(world);
        if (remaining < 0) {
            return "Prochaine rotation dans : --";
        }
        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;
        return String.format("Prochaine rotation dans : %02dh %02dm %02ds", hours, minutes, seconds);
    }

    private List<ShopCatalog.Entry> itemsInSelectedCategory() {
        if (ALL_CATEGORY.equals(selectedCategory)) {
            return visibleEntries();
        }
        List<ShopCatalog.Entry> result = new ArrayList<>();
        for (ShopCatalog.Entry entry : visibleEntries()) {
            String cat = entry.category == null || entry.category.isBlank() ? "Divers" : entry.category;
            if (cat.equals(selectedCategory)) {
                result.add(entry);
            }
        }
        return result;
    }

    private double basePrice(ShopCatalog.Entry entry) {
        if (entry.itemId == null || entry.itemId.isBlank()) {
            return 0.0;
        }
        return priceRepository.priceOf(entry.itemId);
    }

    private double effectivePrice(ShopCatalog.Entry entry) {
        double discount = Math.min(1.0, purchaseBonus);
        return basePrice(entry) * priceMultiplier * (1.0 - discount);
    }

    private String formatPrice(ShopCatalog.Entry entry, double price) {
        if (entry.hasItemCurrency()) {
            return formatPlain(price) + " x " + formatItemName(entry.currencyItemId);
        }
        return formatPlain(price) + " Coins";
    }

    private int remainingForPlayer(ShopCatalog.Entry entry, int entryIndex) {
        int stock = stockOf(entry, entryIndex);
        if (stock <= 0) {
            return Integer.MAX_VALUE;
        }
        int already = purchaseTracker.purchasedCount(playerRef.getUuid(), shopId, entryIndex);
        return Math.max(0, stock - already);
    }

    /** Stock rolled for this item this rotation (from [maxPerPlayerMin, maxPerPlayerMax]), or its min bound as a fallback. */
    private int stockOf(ShopCatalog.Entry entry, int entryIndex) {
        Integer rolled = rolledStock.get(entryIndex);
        if (rolled != null) {
            return rolled;
        }
        return Math.max(0, entry.maxPerPlayerMin);
    }

    /** Rebuilds only the category list container to match the current category count. */
    private void ensureCategoryListSize(UICommandBuilder cmd, UIEventBuilder evt) {
        if (categories.size() == builtCategoryCount) {
            return;
        }
        cmd.clear("#CategoryList");
        for (int i = 0; i < categories.size(); i++) {
            cmd.append("#CategoryList", "VaryonShop/Market/CategoryButton.ui");
        }
        builtCategoryCount = categories.size();
    }

    /** Rebuilds only the item grid rows to match the current item count (left-aligned rows, not LeftCenterWrap). */
    private void ensureItemGridSize(UICommandBuilder cmd, int itemCount) {
        int neededRows = (itemCount + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        if (neededRows == builtItemRows) {
            return;
        }
        cmd.clear("#ItemGrid");
        for (int row = 0; row < neededRows; row++) {
            cmd.append("#ItemGrid", "VaryonShop/Market/ItemRow.ui");
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                cmd.append("#ItemGrid[" + row + "]", "VaryonShop/Market/MarketItemSlot.ui");
            }
        }
        builtItemRows = neededRows;
    }

    private void renderDynamic(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.set("#MarketTitle.Text", catalog.displayName == null ? "BOUTIQUE" : catalog.displayName.toUpperCase());
        cmd.set("#WalletValue.Text", economyBridge.isAvailable()
                ? formatWallet(economyBridge.format(economyBridge.getBalance(playerRef.getUuid())))
                : "");
        cmd.set("#RotationBannerLabel.Text", rotationCountdownText(currentWorld()));

        ensureCategoryListSize(cmd, evt);
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            String selector = "#CategoryList[" + i + "] #CategoryButtonLabel";
            cmd.set(selector + ".Text", category);
            boolean selected = category.equals(selectedCategory);
            cmd.setNull("#CategoryList[" + i + "] #CategoryButton.TooltipText");
            cmd.set(selector + ".Style.TextColor", selected ? "#f4c542" : "#ffffff");
            EventData click = new EventData().append("Action", "selectCategory").append("Category", category);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#CategoryList[" + i + "] #CategoryButton", click, false);
        }

        List<ShopCatalog.Entry> visible = itemsInSelectedCategory();
        ensureItemGridSize(cmd, visible.size());
        int rowCount = (visible.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int i = row * ITEMS_PER_ROW + col;
                String selector = "#ItemGrid[" + row + "][" + col + "]";
                if (i >= visible.size()) {
                    cmd.set(selector + ".Visible", false);
                    continue;
                }
                cmd.set(selector + ".Visible", true);
                ShopCatalog.Entry entry = visible.get(i);
                int entryIndex = catalog.items.indexOf(entry);
                boolean outOfStock = remainingForPlayer(entry, entryIndex) <= 0;
                double price = effectivePrice(entry);
                String label = displayName(entry);
                String priceText = outOfStock ? "Épuisé" : formatPrice(entry, price);
                cmd.set(selector + " #MarketSlotIcon.ItemId", entry.displayIconItemId());
                cmd.set(selector + " #MarketSlotName.Text", label);
                cmd.set(selector + " #MarketSlotPrice.Text", priceText);
                String rarity = ItemRarityUtil.rarityOf(entry.itemId);
                cmd.set(selector + " #MarketSlotRarityBg.AssetPath", ItemRarityUtil.rarityTexturePath(rarity));
                cmd.set(selector + " #MarketSlotRarityBg.Visible", true);
                cmd.set(selector + " #MarketSlotButton.Disabled", outOfStock);
                cmd.set(selector + " #MarketSlotOutOfStockOverlay.Visible", outOfStock);
                cmd.set(selector + " #MarketSlotButton.TooltipText", outOfStock
                        ? label + " - Épuisé"
                        : label + " - " + priceText + (entry.isService() ? "" : " / unité"));
                if (!outOfStock) {
                    EventData click = new EventData().append("Action", "selectItem").append("Index", String.valueOf(entryIndex));
                    evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #MarketSlotButton", click, false);
                }
            }
        }

        renderDetail(cmd, evt);
    }

    private void renderDetail(UICommandBuilder cmd, UIEventBuilder evt) {
        ShopCatalog.Entry selected = selectedEntry();
        boolean hasSelection = selected != null;

        cmd.set("#DetailIconWrap.Visible", hasSelection);
        cmd.set("#DetailSep.Visible", hasSelection);
        cmd.set("#DetailTotalCard.Visible", hasSelection);
        cmd.set("#DetailBuyButton.Visible", hasSelection);

        if (!hasSelection) {
            cmd.set("#DetailQtyRow.Visible", false);
            cmd.set("#DetailName.Text", "Sélectionnez un objet");
            cmd.set("#DetailPriceLabel.Text", "");
            cmd.set("#DetailDiscountLabel.Text", "");
            cmd.set("#DetailStockLabel.Text", "");
            return;
        }

        if (selected.isService()) {
            chosenQty = 1;
        }
        cmd.set("#DetailQtyRow.Visible", hasSelection && !selected.isService());

        double price = effectivePrice(selected);
        int remaining = remainingForPlayer(selected, selectedItemIndex);
        cmd.set("#DetailIcon.ItemId", selected.displayIconItemId());
        cmd.set("#DetailName.Text", displayName(selected));
        cmd.set("#DetailPriceLabel.Text", formatPrice(selected, price) + (selected.isService() ? "" : " / u"));
        cmd.set("#DetailDiscountLabel.Text", "Réduction : " + Math.round(Math.min(1.0, purchaseBonus) * 100) + "%");
        cmd.set("#DetailStockLabel.Text", selected.isService() || remaining == Integer.MAX_VALUE
                ? ""
                : "Restant : " + remaining);
        cmd.set("#DetailQtyInput.Value", String.valueOf(chosenQty));
        cmd.set("#DetailTotalValue.Text", formatPrice(selected, price * chosenQty));
        cmd.set("#DetailBuyButton.TooltipText", remaining == Integer.MAX_VALUE ? "" : "Restant: " + remaining);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DetailQtyMinus", EventData.of("Action", "qtyMinus"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DetailQtyPlus", EventData.of("Action", "qtyPlus"), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DetailQtyInput",
                new EventData().append("Action", "qtySet").append("@QtyInput", "#DetailQtyInput.Value"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DetailQtyMax", EventData.of("Action", "qtyMax"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DetailBuyButton", EventData.of("Action", "buy"), false);
    }

    private boolean isSelectedService() {
        ShopCatalog.Entry entry = selectedEntry();
        return entry != null && entry.isService();
    }

    @Nullable
    private ShopCatalog.Entry selectedEntry() {
        if (selectedItemIndex < 0 || selectedItemIndex >= catalog.items.size() || !visibleIndexes.contains(selectedItemIndex)) {
            return null;
        }
        return catalog.items.get(selectedItemIndex);
    }

    @Override
    public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store, EventDataPayload data) {
        if (data == null || data.action == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        switch (data.action) {
            case "selectCategory" -> {
                if (data.category != null) {
                    selectedCategory = data.category;
                    selectedItemIndex = -1;
                }
            }
            case "selectItem" -> {
                selectedItemIndex = parseInt(data.index);
                chosenQty = 1;
            }
            case "qtyMinus" -> chosenQty = isSelectedService() ? 1 : Math.max(1, chosenQty - 1);
            case "qtyPlus" -> chosenQty = isSelectedService() ? 1 : Math.min(MAX_QTY, chosenQty + 1);
            case "qtySet" -> {
                int parsed = parseInt(data.qtyInput);
                if (isSelectedService()) {
                    chosenQty = 1;
                } else if (parsed > 0) {
                    chosenQty = Math.min(maxAffordableQty(), parsed);
                }
            }
            case "qtyMax" -> chosenQty = isSelectedService() ? 1 : maxAffordableQty();
            case "buy" -> buySelected(player);
            default -> { }
        }

        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        renderDynamic(refreshCmd, refreshEvt);
        sendUpdate(refreshCmd, refreshEvt, false);
    }

    private int maxAffordableQty() {
        ShopCatalog.Entry entry = selectedEntry();
        if (entry == null) {
            return 1;
        }
        int remaining = remainingForPlayer(entry, selectedItemIndex);
        int cap = remaining == Integer.MAX_VALUE ? MAX_QTY : Math.min(MAX_QTY, remaining);

        double unitPrice = effectivePrice(entry);
        if (unitPrice <= 0.0) {
            return Math.max(1, cap);
        }
        if (entry.hasItemCurrency()) {
            int owned = countCurrencyOwned(entry.currencyItemId);
            int affordable = (int) Math.floor(owned / unitPrice);
            return Math.max(1, Math.min(cap, affordable));
        }
        if (!economyBridge.isAvailable()) {
            return 1;
        }
        double balance = economyBridge.getBalance(playerRef.getUuid());
        int affordable = (int) Math.floor(balance / unitPrice);
        return Math.max(1, Math.min(cap, affordable));
    }

    private int countCurrencyOwned(String currencyItemId) {
        Player player = EntityApiCompat.getPlayer(playerRef);
        if (player == null) {
            return 0;
        }
        int count = 0;
        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
        if (storage != null) {
            count += storage.countItemStacks(stack -> currencyItemId.equals(stack.getItemId()));
        }
        if (hotbar != null) {
            count += hotbar.countItemStacks(stack -> currencyItemId.equals(stack.getItemId()));
        }
        return count;
    }

    private void buySelected(Player player) {
        ShopCatalog.Entry entry = selectedEntry();
        if (entry == null) {
            return;
        }
        int entryIndex = selectedItemIndex;
        int remaining = remainingForPlayer(entry, entryIndex);
        if (remaining <= 0) {
            playerRef.sendMessage(Message.raw("Boutique: limite d'achat atteinte pour cet objet."));
            return;
        }
        if (chosenQty > remaining) {
            chosenQty = remaining;
        }

        double unitPrice = effectivePrice(entry);
        double total = unitPrice * chosenQty;

        if (entry.hasItemCurrency()) {
            int cost = (int) Math.ceil(total);
            ItemContainer storage = EntityApiCompat.getStorageContainer(player);
            ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
            if (!takeCurrencyItems(storage, hotbar, entry.currencyItemId, cost)) {
                playerRef.sendMessage(Message.raw("Boutique: " + formatItemName(entry.currencyItemId) + " insuffisant(s)."));
                return;
            }
            if (entry.isService()) {
                buyService(entry, entryIndex, total, () -> giveCurrencyItems(storage, hotbar, entry.currencyItemId, cost));
                return;
            }
            buyItem(player, entry, entryIndex, total, () -> giveCurrencyItems(storage, hotbar, entry.currencyItemId, cost));
            return;
        }

        if (total > 0.0 && !economyBridge.isAvailable()) {
            playerRef.sendMessage(Message.raw("Boutique: économie indisponible."));
            return;
        }
        if (total > 0.0 && !economyBridge.takeFunds(playerRef.getUuid(), total)) {
            playerRef.sendMessage(Message.raw("Boutique: solde insuffisant."));
            return;
        }

        if (entry.isService()) {
            buyService(entry, entryIndex, total, () -> economyBridge.addFunds(playerRef.getUuid(), total));
            return;
        }
        buyItem(player, entry, entryIndex, total, () -> economyBridge.addFunds(playerRef.getUuid(), total));
    }

    private void buyService(ShopCatalog.Entry entry, int entryIndex, double total, Runnable refund) {
        int totalUnits = chosenQty * Math.max(1, entry.quantity);
        try {
            ShopCommandExecutor.run(entry.command, playerRef, totalUnits, entry.commandAsPlayer);
        } catch (Throwable t) {
            refund.run();
            playerRef.sendMessage(Message.raw("Boutique: erreur lors de l'exécution du service, montant remboursé."));
            return;
        }
        purchaseTracker.recordPurchase(playerRef.getUuid(), shopId, entryIndex, chosenQty);
        playerRef.sendMessage(Message.raw("Boutique: ")
                .insert(Message.raw(displayName(entry) + " (x" + chosenQty + ")").color("#f5d76e"))
                .insert(Message.raw(" acheté pour "))
                .insert(Message.raw(formatPrice(entry, total)).color("#6fd67a"))
                .insert(Message.raw(".")));
    }

    private void buyItem(Player player, ShopCatalog.Entry entry, int entryIndex, double total, Runnable refund) {
        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
        int totalUnits = chosenQty * Math.max(1, entry.quantity);
        int remaining = totalUnits;
        remaining -= giveItems(storage, entry.itemId, remaining);
        if (remaining > 0) {
            remaining -= giveItems(hotbar, entry.itemId, remaining);
        }
        int actuallyGiven = totalUnits - remaining;
        if (remaining > 0) {
            refund.run();
            playerRef.sendMessage(Message.raw("Boutique: inventaire plein, achat partiel. " + actuallyGiven + " objet(s) reçu(s), reste remboursé."));
            return;
        }
        purchaseTracker.recordPurchase(playerRef.getUuid(), shopId, entryIndex, chosenQty);
        playerRef.sendMessage(Message.raw("Boutique: ")
                .insert(Message.raw(actuallyGiven + " x " + displayName(entry)).color("#f5d76e"))
                .insert(Message.raw(" acheté pour "))
                .insert(Message.raw(formatPrice(entry, total)).color("#6fd67a"))
                .insert(Message.raw(".")));
    }

    /** Attempts to remove qty of currencyItemId from storage, then hotbar. All-or-nothing across both containers. */
    private boolean takeCurrencyItems(@Nullable ItemContainer storage, @Nullable ItemContainer hotbar, String currencyItemId, int qty) {
        if (qty <= 0) {
            return true;
        }
        int available = 0;
        if (storage != null) {
            available += storage.countItemStacks(stack -> currencyItemId.equals(stack.getItemId()));
        }
        if (hotbar != null) {
            available += hotbar.countItemStacks(stack -> currencyItemId.equals(stack.getItemId()));
        }
        if (available < qty) {
            return false;
        }
        int remaining = qty;
        if (storage != null && remaining > 0) {
            remaining -= removeUpTo(storage, currencyItemId, remaining);
        }
        if (hotbar != null && remaining > 0) {
            remaining -= removeUpTo(hotbar, currencyItemId, remaining);
        }
        if (remaining > 0) {
            giveCurrencyItems(storage, hotbar, currencyItemId, qty - remaining);
            return false;
        }
        return true;
    }

    private int removeUpTo(ItemContainer container, String itemId, int qty) {
        try {
            var tx = container.removeItemStack(new ItemStack(itemId, qty), false, true);
            ItemStack remainder = tx != null ? tx.getRemainder() : null;
            int notRemoved = remainder == null ? 0 : remainder.getQuantity();
            return qty - notRemoved;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void giveCurrencyItems(@Nullable ItemContainer storage, @Nullable ItemContainer hotbar, String currencyItemId, int qty) {
        if (qty <= 0) {
            return;
        }
        int remaining = qty;
        remaining -= giveItems(storage, currencyItemId, remaining);
        if (remaining > 0) {
            giveItems(hotbar, currencyItemId, remaining);
        }
    }

    /** Attempts to add up to qty of itemId to the container (stacked automatically). Returns how many were actually given. */
    private int giveItems(@Nullable ItemContainer container, String itemId, int qty) {
        if (container == null || qty <= 0) {
            return 0;
        }
        try {
            ItemStack stack = new ItemStack(itemId, qty);
            var tx = container.addItemStack(stack, false, false, true);
            int remainder = tx != null && tx.getRemainder() != null ? tx.getRemainder().getQuantity() : 0;
            return qty - remainder;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String displayName(ShopCatalog.Entry entry) {
        if (entry.name != null && !entry.name.isBlank()) {
            return entry.name;
        }
        return formatItemName(entry.itemId);
    }

    private String formatItemName(@Nullable String itemId) {
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

    private String formatPlain(double amount) {
        return String.format("%.2f", amount);
    }

    /** VaultUnlocked's format() puts the currency name before the amount (e.g. "Coins96,764.73");
     * this reorders it to "amount name" (e.g. "96,764.73 Coins") for display in the wallet label. */
    private String formatWallet(String rawFormatted) {
        if (rawFormatted == null || rawFormatted.isBlank()) {
            return rawFormatted;
        }
        int splitAt = -1;
        for (int i = 0; i < rawFormatted.length(); i++) {
            char c = rawFormatted.charAt(i);
            if (Character.isDigit(c) || c == '-') {
                splitAt = i;
                break;
            }
        }
        if (splitAt <= 0) {
            return rawFormatted;
        }
        String name = rawFormatted.substring(0, splitAt).trim();
        String amount = rawFormatted.substring(splitAt).trim();
        int decimalDot = amount.lastIndexOf('.');
        if (decimalDot >= 0 && amount.length() - decimalDot - 1 <= 2) {
            amount = amount.substring(0, decimalDot);
        }
        return name.isEmpty() ? amount : amount + " " + name;
    }

    private int parseInt(@Nullable String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static final class EventDataPayload {
        public String action;
        public String category;
        public String index;
        public String qtyInput;

        @SuppressWarnings("unchecked")
        public static final BuilderCodec<EventDataPayload> CODEC = buildCodec();

        @SuppressWarnings("unchecked")
        private static BuilderCodec<EventDataPayload> buildCodec() {
            BuilderCodec.Builder builder = BuilderCodec.builder(EventDataPayload.class, EventDataPayload::new);
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("Action", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.action = v, (Function<EventDataPayload, String>) d -> d.action).add();
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("Category", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.category = v, (Function<EventDataPayload, String>) d -> d.category).add();
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("Index", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.index = v, (Function<EventDataPayload, String>) d -> d.index).add();
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("@QtyInput", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.qtyInput = v, (Function<EventDataPayload, String>) d -> d.qtyInput).add();
            return builder.build();
        }
    }
}
