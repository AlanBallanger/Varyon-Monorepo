package fr.varyon.shop.ui.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/** Shared event payload for all Varyon-Shop admin panel pages (list, shop detail, item editor). */
public final class ShopAdminEventData {

    public static final BuilderCodec<ShopAdminEventData> CODEC =
            BuilderCodec.builder(ShopAdminEventData.class, ShopAdminEventData::new)
                    .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                    .addField(new KeyedCodec<>("ShopId", Codec.STRING), (d, v) -> d.shopId = v, d -> d.shopId)
                    .addField(new KeyedCodec<>("Index", Codec.STRING), (d, v) -> d.index = v, d -> d.index)
                    .addField(new KeyedCodec<>("@CreateShopId", Codec.STRING), (d, v) -> d.createShopId = v, d -> d.createShopId)
                    .addField(new KeyedCodec<>("@DisplayName", Codec.STRING), (d, v) -> d.displayName = v, d -> d.displayName)
                    .addField(new KeyedCodec<>("@MaxVisible", Codec.STRING), (d, v) -> d.maxVisible = v, d -> d.maxVisible)
                    .addField(new KeyedCodec<>("@MaxPerPlayerMin", Codec.STRING), (d, v) -> d.maxPerPlayerMin = v, d -> d.maxPerPlayerMin)
                    .addField(new KeyedCodec<>("@MaxPerPlayerMax", Codec.STRING), (d, v) -> d.maxPerPlayerMax = v, d -> d.maxPerPlayerMax)
                    .addField(new KeyedCodec<>("@PriceMultMin", Codec.STRING), (d, v) -> d.priceMultMin = v, d -> d.priceMultMin)
                    .addField(new KeyedCodec<>("@PriceMultMax", Codec.STRING), (d, v) -> d.priceMultMax = v, d -> d.priceMultMax)
                    .addField(new KeyedCodec<>("@ItemId", Codec.STRING), (d, v) -> d.itemId = v, d -> d.itemId)
                    .addField(new KeyedCodec<>("@Command", Codec.STRING), (d, v) -> d.command = v, d -> d.command)
                    .addField(new KeyedCodec<>("@Name", Codec.STRING), (d, v) -> d.name = v, d -> d.name)
                    .addField(new KeyedCodec<>("@IconItemId", Codec.STRING), (d, v) -> d.iconItemId = v, d -> d.iconItemId)
                    .addField(new KeyedCodec<>("@Price", Codec.STRING), (d, v) -> d.price = v, d -> d.price)
                    .addField(new KeyedCodec<>("@OverridePrice", Codec.STRING), (d, v) -> d.overridePrice = v, d -> d.overridePrice)
                    .addField(new KeyedCodec<>("Category", Codec.STRING), (d, v) -> d.category = v, d -> d.category)
                    .addField(new KeyedCodec<>("@Category", Codec.STRING), (d, v) -> d.categoryFree = v, d -> d.categoryFree)
                    .addField(new KeyedCodec<>("@Quantity", Codec.STRING), (d, v) -> d.quantity = v, d -> d.quantity)
                    .addField(new KeyedCodec<>("@CurrencyItemId", Codec.STRING), (d, v) -> d.currencyItemId = v, d -> d.currencyItemId)
                    .addField(new KeyedCodec<>("@GeneralRate", Codec.STRING), (d, v) -> d.generalRate = v, d -> d.generalRate)
                    .addField(new KeyedCodec<>("@BuybackBonus", Codec.STRING), (d, v) -> d.buybackBonus = v, d -> d.buybackBonus)
                    .addField(new KeyedCodec<>("@PurchaseBonus", Codec.STRING), (d, v) -> d.purchaseBonus = v, d -> d.purchaseBonus)
                    .addField(new KeyedCodec<>("@NewCurrencyId", Codec.STRING), (d, v) -> d.newCurrencyId = v, d -> d.newCurrencyId)
                    .addField(new KeyedCodec<>("@NewCurrencyLabel", Codec.STRING), (d, v) -> d.newCurrencyLabel = v, d -> d.newCurrencyLabel)
                    .addField(new KeyedCodec<>("Slot", Codec.STRING), (d, v) -> d.slot = v, d -> d.slot)
                    .addField(new KeyedCodec<>("Source", Codec.STRING), (d, v) -> d.source = v, d -> d.source)
                    .addField(new KeyedCodec<>("Target", Codec.STRING), (d, v) -> d.target = v, d -> d.target)
                    .build();

    String action;
    String shopId;
    String index;
    String createShopId;
    String displayName;
    String maxVisible;
    String maxPerPlayerMin;
    String maxPerPlayerMax;
    String priceMultMin;
    String priceMultMax;
    String itemId;
    String command;
    String name;
    String iconItemId;
    String price;
    String overridePrice;
    String category;
    String categoryFree;
    String quantity;
    String currencyItemId;
    String generalRate;
    String buybackBonus;
    String purchaseBonus;
    String newCurrencyId;
    String newCurrencyLabel;
    String slot;
    String source;
    String target;

    public String getAction() { return action; }
    public String getShopId() { return shopId; }
    public String getIndex() { return index; }
    public String getCreateShopId() { return createShopId; }
    public String getDisplayName() { return displayName; }
    public String getMaxVisible() { return maxVisible; }
    public String getMaxPerPlayerMin() { return maxPerPlayerMin; }
    public String getMaxPerPlayerMax() { return maxPerPlayerMax; }
    public String getPriceMultMin() { return priceMultMin; }
    public String getPriceMultMax() { return priceMultMax; }
    public String getItemId() { return itemId; }
    public String getCommand() { return command; }
    public String getName() { return name; }
    public String getIconItemId() { return iconItemId; }
    public String getPrice() { return price; }
    public String getOverridePrice() { return overridePrice; }
    public String getCategory() { return category; }
    public String getCategoryFree() { return categoryFree; }
    public String getQuantity() { return quantity; }
    public String getCurrencyItemId() { return currencyItemId; }
    public String getGeneralRate() { return generalRate; }
    public String getBuybackBonus() { return buybackBonus; }
    public String getPurchaseBonus() { return purchaseBonus; }
    public String getNewCurrencyId() { return newCurrencyId; }
    public String getNewCurrencyLabel() { return newCurrencyLabel; }
    public String getSlot() { return slot; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
}
