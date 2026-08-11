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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads/saves the item id -&gt; buyback unit price table used by the general buyback merchant.
 * File format: flat JSON object, e.g. {"item_wood_log": 2.5, "item_iron_ore": 8}.
 */
public final class BuybackPriceRepository {
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Double> prices = new LinkedHashMap<>();

    public BuybackPriceRepository(Path filePath) {
        this.filePath = filePath;
    }

    public void load() {
        try {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, gson.toJson(defaultPrices()), StandardCharsets.UTF_8);
                prices = defaultPrices();
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Double>>() {}.getType();
            Map<String, Double> parsed = gson.fromJson(raw, type);
            prices = parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (IOException e) {
            prices = new LinkedHashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, gson.toJson(new TreeMap<>(prices)), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    /** Returns the configured unit buyback price for an item id, or 0 if not sellable here. */
    public double priceOf(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0.0;
        }
        Double direct = prices.get(itemId);
        if (direct != null) {
            return Math.max(0.0, direct);
        }
        Double lower = prices.get(itemId.toLowerCase(Locale.ROOT));
        return lower == null ? 0.0 : Math.max(0.0, lower);
    }

    public boolean isSellable(String itemId) {
        return priceOf(itemId) > 0.0;
    }

    public void setPrice(String itemId, double price) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        prices.put(itemId, Math.max(0.0, price));
    }

    private static Map<String, Double> defaultPrices() {
        Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("item_wood_log", 2.0);
        defaults.put("item_stone", 1.0);
        defaults.put("item_iron_ore", 6.0);
        defaults.put("item_coal", 3.0);
        return defaults;
    }
}
