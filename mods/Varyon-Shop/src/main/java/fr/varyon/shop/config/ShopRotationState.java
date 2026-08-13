package fr.varyon.shop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists, per (shopId, worldUuid), which items are currently on offer and the currently
 * rolled price multiplier, re-rolled once per Hytale day in that world. Survives restarts.
 */
public final class ShopRotationState {
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Random random = new Random();
    private Map<String, RotationEntry> state = new ConcurrentHashMap<>();
    private ShopPurchaseTracker purchaseTracker;

    public static final class RotationEntry {
        public int lastRotationDay = -1;
        public double priceMultiplier = 1.0;
        public List<Integer> visibleIndexes = new ArrayList<>();
        /** Item index -> stock rolled for this rotation (from [maxPerPlayerMin, maxPerPlayerMax]). */
        public Map<Integer, Integer> rolledStock = new LinkedHashMap<>();
    }

    public ShopRotationState(Path filePath) {
        this.filePath = filePath;
    }

    /** Wired in after construction (both are built together in the plugin's setup) so a re-roll can clear per-player purchase counts. */
    public void setPurchaseTracker(ShopPurchaseTracker purchaseTracker) {
        this.purchaseTracker = purchaseTracker;
    }

    public void load() {
        try {
            if (!Files.exists(filePath)) {
                state = new ConcurrentHashMap<>();
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, RotationEntry>>() {}.getType();
            Map<String, RotationEntry> parsed = gson.fromJson(raw, type);
            state = parsed == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(parsed);
        } catch (IOException e) {
            state = new ConcurrentHashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, gson.toJson(new LinkedHashMap<>(state)), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String key(String shopId, String worldUuid) {
        return shopId.toLowerCase(java.util.Locale.ROOT) + "|" + worldUuid;
    }

    /** Moves every rotation entry keyed under oldShopId to newShopId. Returns the count moved. */
    public int renameShopId(String oldShopId, String newShopId) {
        if (oldShopId == null || newShopId == null || oldShopId.equalsIgnoreCase(newShopId)) {
            return 0;
        }
        String oldPrefix = oldShopId.toLowerCase(java.util.Locale.ROOT) + "|";
        String newKeyBase = newShopId.toLowerCase(java.util.Locale.ROOT) + "|";
        Map<String, RotationEntry> toMove = new LinkedHashMap<>();
        for (String key : state.keySet()) {
            if (key.startsWith(oldPrefix)) {
                toMove.put(key, state.get(key));
            }
        }
        for (Map.Entry<String, RotationEntry> entry : toMove.entrySet()) {
            state.remove(entry.getKey());
            String worldPart = entry.getKey().substring(oldPrefix.length());
            state.put(newKeyBase + worldPart, entry.getValue());
        }
        if (!toMove.isEmpty()) {
            save();
        }
        return toMove.size();
    }

    /**
     * Ensures the rotation for this shop+world is current for the given Hytale day, re-rolling
     * the visible items and price multiplier if the day advanced (or if never rolled before).
     * Returns the (possibly just-updated) rotation entry.
     */
    public RotationEntry ensureCurrent(String shopId, String worldUuid, int currentDayOfYear, ShopCatalog catalog) {
        String key = key(shopId, worldUuid);
        RotationEntry entry = state.computeIfAbsent(key, k -> new RotationEntry());
        boolean unrestricted = catalog.maxVisibleItems <= 0 || catalog.maxVisibleItems >= catalog.items.size();
        if (unrestricted) {
            // No rotation subset in play: always show every current item, never rely on a stale cached list.
            if (entry.lastRotationDay != currentDayOfYear) {
                roll(entry, currentDayOfYear, catalog, shopId);
                save();
            } else {
                entry.visibleIndexes = allIndexesOf(catalog);
            }
            return entry;
        }
        if (entry.lastRotationDay == currentDayOfYear && !entry.visibleIndexes.isEmpty()) {
            return entry;
        }
        if (entry.lastRotationDay == currentDayOfYear && catalog.items.isEmpty()) {
            return entry;
        }
        roll(entry, currentDayOfYear, catalog, shopId);
        save();
        return entry;
    }

    /** Forces an immediate re-roll (new price multiplier, new visible items) for this shop+world, ignoring the day check. */
    public void forceReroll(String shopId, String worldUuid, int currentDayOfYear, ShopCatalog catalog) {
        String key = key(shopId, worldUuid);
        RotationEntry entry = state.computeIfAbsent(key, k -> new RotationEntry());
        roll(entry, currentDayOfYear, catalog, shopId);
        save();
    }

    /** Forces an immediate re-roll for every world this shop currently has a rotation entry for. */
    public int forceRerollAllWorlds(String shopId, int currentDayOfYear, ShopCatalog catalog) {
        String prefix = shopId.toLowerCase(java.util.Locale.ROOT) + "|";
        int count = 0;
        for (String key : state.keySet()) {
            if (key.startsWith(prefix)) {
                roll(state.get(key), currentDayOfYear, catalog, shopId);
                count++;
            }
        }
        if (count > 0) {
            save();
        }
        return count;
    }

    private static List<Integer> allIndexesOf(ShopCatalog catalog) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < catalog.items.size(); i++) {
            all.add(i);
        }
        return all;
    }

    private void roll(RotationEntry entry, int currentDayOfYear, ShopCatalog catalog, String shopId) {
        entry.lastRotationDay = currentDayOfYear;

        double min = Math.min(catalog.priceMultiplierMin, catalog.priceMultiplierMax);
        double max = Math.max(catalog.priceMultiplierMin, catalog.priceMultiplierMax);
        entry.priceMultiplier = min >= max ? min : min + random.nextDouble() * (max - min);

        if (purchaseTracker != null) {
            purchaseTracker.resetForShop(shopId);
        }

        entry.rolledStock = new LinkedHashMap<>();
        for (int i = 0; i < catalog.items.size(); i++) {
            entry.rolledStock.put(i, rollStock(catalog.items.get(i)));
        }

        int totalItems = catalog.items.size();
        entry.visibleIndexes = new ArrayList<>();
        if (totalItems == 0) {
            return;
        }
        int visibleCount = catalog.maxVisibleItems <= 0 || catalog.maxVisibleItems >= totalItems
                ? totalItems
                : catalog.maxVisibleItems;

        List<Integer> allIndexes = new ArrayList<>();
        for (int i = 0; i < totalItems; i++) {
            allIndexes.add(i);
        }
        java.util.Collections.shuffle(allIndexes, random);
        for (int i = 0; i < visibleCount; i++) {
            entry.visibleIndexes.add(allIndexes.get(i));
        }
        java.util.Collections.sort(entry.visibleIndexes);
    }

    private int rollStock(ShopCatalog.Entry item) {
        int min = Math.min(item.maxPerPlayerMin, item.maxPerPlayerMax);
        int max = Math.max(item.maxPerPlayerMin, item.maxPerPlayerMax);
        if (min <= 0 && max <= 0) {
            return 0;
        }
        if (min <= 0) {
            min = max;
        }
        return min >= max ? min : min + random.nextInt(max - min + 1);
    }

    /** Stock rolled for this item this rotation, or its min bound if never rolled (e.g. item added mid-day). */
    public int stockOf(RotationEntry entry, int itemIndex, ShopCatalog.Entry item) {
        Integer rolled = entry.rolledStock.get(itemIndex);
        return rolled != null ? rolled : rollStock(item);
    }
}
