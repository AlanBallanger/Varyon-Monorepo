package fr.varyon.shop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Loads/saves the item id -&gt; buyback entry table used by the buyback merchants.
 * File format: flat JSON object, e.g.
 * {"item_wood_log": {"price": 2.5, "category": "ressources"}, "item_iron_ore": {"price": 8}}.
 * "category" is optional; items without one only sell through a general (non-specialized)
 * buyback merchant.
 */
public final class BuybackPriceRepository {
    public record Entry(double price, String category) {
        public Entry {
            price = Math.max(0.0, price);
        }
    }

    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Entry> entries = new LinkedHashMap<>();

    public BuybackPriceRepository(Path filePath) {
        this.filePath = filePath;
    }

    public void load() {
        try {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, gson.toJson(defaultEntries()), StandardCharsets.UTF_8);
                entries = defaultEntries();
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Entry>>() {}.getType();
            Map<String, Entry> parsed = gson.fromJson(raw, type);
            entries = parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (IOException e) {
            entries = new LinkedHashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, gson.toJson(new TreeMap<>(entries)), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    /** Returns the configured unit buyback price for an item id, or 0 if not sellable here. */
    public double priceOf(String itemId) {
        Entry entry = entryOf(itemId);
        return entry == null ? 0.0 : entry.price();
    }

    /** Returns the configured category for an item id, or null if uncategorized. */
    public String categoryOf(String itemId) {
        Entry entry = entryOf(itemId);
        return entry == null ? null : entry.category();
    }

    /**
     * Returns the buyback unit price for an item, restricted to a given category filter.
     * A null/blank categoryFilter means "no restriction" (general merchant). Items whose
     * category doesn't match the filter are treated as unsellable (price 0) at that merchant.
     */
    public double priceOf(String itemId, String categoryFilter) {
        Entry entry = entryOf(itemId);
        if (entry == null) {
            return 0.0;
        }
        if (categoryFilter == null || categoryFilter.isBlank()) {
            return entry.price();
        }
        return categoryFilter.equalsIgnoreCase(entry.category()) ? entry.price() : 0.0;
    }

    public boolean isSellable(String itemId) {
        return priceOf(itemId) > 0.0;
    }

    public void setPrice(String itemId, double price, String category) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        entries.put(itemId, new Entry(price, category));
    }

    /** Lists every distinct category currently used across all configured items. */
    public Set<String> listCategories() {
        Set<String> categories = new LinkedHashSet<>();
        for (Entry entry : entries.values()) {
            if (entry.category() != null && !entry.category().isBlank()) {
                categories.add(entry.category());
            }
        }
        return categories;
    }

    private Entry entryOf(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        Entry direct = entries.get(itemId);
        if (direct != null) {
            return direct;
        }
        return entries.get(itemId.toLowerCase(Locale.ROOT));
    }

    private static Map<String, Entry> defaultEntries() {
        Map<String, Entry> defaults = new LinkedHashMap<>();
        defaults.put("item_wood_log", new Entry(2.0, "ressources"));
        defaults.put("item_stone", new Entry(1.0, "ressources"));
        defaults.put("item_iron_ore", new Entry(6.0, "ressources"));
        defaults.put("item_coal", new Entry(3.0, "ressources"));
        return defaults;
    }
}
