package fr.varyon.shop.config;

import java.util.ArrayList;
import java.util.List;

/**
 * A single shop's catalog. Persisted as one JSON file per shop, named after the shop's id.
 *
 * Rotation: if maxVisibleItems is greater than 0 and less than the item count, only that many
 * items are offered for sale at a time, re-picked at random once per Hytale day (tracked per
 * shop+world by ShopRotationState). 0 or negative means "show everything, no rotation".
 *
 * Random pricing: each item's effective sale price is basePrice * a multiplier drawn uniformly
 * from [priceMultiplierMin, priceMultiplierMax] once per rotation (same multiplier for every
 * item in the shop, re-rolled alongside the item rotation).
 */
public final class ShopCatalog {
    /** Blank means "draft": not yet listed, not yet written to disk. Set a name to make it real. */
    public String displayName = "";
    public int maxVisibleItems = 0;
    public double priceMultiplierMin = 1.0;
    public double priceMultiplierMax = 1.0;
    public List<Entry> items = new ArrayList<>();

    /**
     * An entry is either an item (itemId set, command null) or a service (command set): buying
     * a service runs the command server-side instead of giving an item. For a service, name and
     * iconItemId (an item id used only for its icon) are used for display since there's no real
     * item involved.
     *
     * Price is usually looked up from BuybackPriceRepository by itemId (0 if unlisted), then
     * multiplied by the owning shop's price multiplier — this keeps a single source of truth for
     * item values shared with buyback. overridePrice, if set, replaces the buyback-listed price
     * for this entry only, without affecting that item's buyback price anywhere else.
     */
    public static final class Entry {
        public String itemId;
        public String command;
        /** True = command runs as the buying player (their own permissions); false (default) = runs as server console. */
        public boolean commandAsPlayer = false;
        public String name;
        public String iconItemId;
        public String category = "Divers";
        public int quantity = 1;
        /**
         * Max units a single player may buy of this entry, drawn uniformly from
         * [maxPerPlayerMin, maxPerPlayerMax] once per shop rotation (see ShopRotationState).
         * Both 0 means unlimited. Equal non-zero values means a fixed stock.
         */
        public int maxPerPlayerMin = 0;
        public int maxPerPlayerMax = 0;
        /** @deprecated legacy fixed-stock field, migrated into maxPerPlayerMin/Max on load. */
        @Deprecated
        public Integer maxPerPlayer;
        public String currencyItemId;
        /**
         * Optional fixed price that overrides BuybackPriceRepository's listed price for this
         * item, in this shop only. Null means "use the buyback-listed price" (default). Set means
         * this shop sells the item at this price regardless of whether/how it's configured for
         * buyback — lets a shop stock items that aren't (and shouldn't be) sellable back to NPCs.
         */
        public Double overridePrice;

        public Entry() {
        }

        public Entry(String itemId, String category, int quantity) {
            this.itemId = itemId;
            this.category = (category == null || category.isBlank()) ? "Divers" : category;
            this.quantity = Math.max(1, quantity);
        }

        public boolean isService() {
            return command != null && !command.isBlank();
        }

        /** Migrates the legacy single-value maxPerPlayer field into the min/max pair, if present. */
        public void migrateLegacyStock() {
            if (maxPerPlayer != null) {
                maxPerPlayerMin = maxPerPlayer;
                maxPerPlayerMax = maxPerPlayer;
                maxPerPlayer = null;
            }
        }

        /** Null/blank means "pay with VaultUnlocked coins"; otherwise the item id required as payment. */
        public boolean hasItemCurrency() {
            return currencyItemId != null && !currencyItemId.isBlank();
        }

        /** The item id whose icon/name should represent this entry in the shop UI. */
        public String displayIconItemId() {
            return isService() ? iconItemId : itemId;
        }
    }
}
