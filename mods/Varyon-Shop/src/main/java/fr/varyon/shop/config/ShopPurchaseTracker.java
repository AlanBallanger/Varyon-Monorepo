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
import java.util.Map;
import java.util.UUID;

/**
 * Tracks how many units of each shop entry a player has ever bought, to enforce
 * ShopCatalog.Entry.maxPerPlayer. Key format: "playerUuid|shopId|itemIndex" -&gt; total bought.
 */
public final class ShopPurchaseTracker {
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Integer> purchased = new LinkedHashMap<>();

    public ShopPurchaseTracker(Path filePath) {
        this.filePath = filePath;
    }

    public void load() {
        try {
            if (!Files.exists(filePath)) {
                purchased = new LinkedHashMap<>();
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> parsed = gson.fromJson(raw, type);
            purchased = parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (IOException e) {
            purchased = new LinkedHashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, gson.toJson(purchased), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public int purchasedCount(UUID playerUuid, String shopId, int itemIndex) {
        Integer count = purchased.get(key(playerUuid, shopId, itemIndex));
        return count == null ? 0 : count;
    }

    public void recordPurchase(UUID playerUuid, String shopId, int itemIndex, int units) {
        if (units <= 0) {
            return;
        }
        String key = key(playerUuid, shopId, itemIndex);
        purchased.merge(key, units, Integer::sum);
        save();
    }

    private String key(UUID playerUuid, String shopId, int itemIndex) {
        return playerUuid + "|" + shopId + "|" + itemIndex;
    }

    /** Clears every player's purchase count for this shop, so per-rotation stock limits reset on a new rotation. */
    public void resetForShop(String shopId) {
        if (shopId == null) {
            return;
        }
        boolean changed = purchased.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split("\\|", 3);
            return parts.length == 3 && parts[1].equalsIgnoreCase(shopId);
        });
        if (changed) {
            save();
        }
    }

    /** Rewrites every purchase-count key's shopId segment from oldShopId to newShopId. Returns the count moved. */
    public int renameShopId(String oldShopId, String newShopId) {
        if (oldShopId == null || newShopId == null || oldShopId.equalsIgnoreCase(newShopId)) {
            return 0;
        }
        Map<String, Integer> updated = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : purchased.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 3);
            if (parts.length == 3 && parts[1].equalsIgnoreCase(oldShopId)) {
                updated.put(parts[0] + "|" + newShopId + "|" + parts[2], entry.getValue());
                count++;
            } else {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        if (count > 0) {
            purchased = updated;
            save();
        }
        return count;
    }
}
