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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maps a Denizen NPC id to the merchant type it should behave as when a player interacts with
 * it, plus an optional category filter (buyback merchants) or shop catalog id (market
 * merchants). Configured via /vshop bind; persisted as denizenId -&gt; binding.
 */
public final class MerchantRegistry {

    public enum MerchantType {
        BUYBACK_GENERAL,
        BUYBACK_PROFESSION,
        MARKET_SHOP
    }

    /**
     * category: used by buyback merchants to restrict which item category they buy (null =
     * unrestricted). shopId: used by MARKET_SHOP merchants to pick which catalog to sell from.
     */
    public record Binding(MerchantType type, String category, String shopId) {
        public Binding(MerchantType type, String category) {
            this(type, category, null);
        }
    }

    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, Binding> bindings = new LinkedHashMap<>();

    public MerchantRegistry(Path filePath) {
        this.filePath = filePath;
    }

    public void load() {
        try {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, gson.toJson(new LinkedHashMap<>()), StandardCharsets.UTF_8);
                bindings = new LinkedHashMap<>();
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<UUID, Binding>>() {}.getType();
            Map<UUID, Binding> parsed = gson.fromJson(raw, type);
            bindings = parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (IOException e) {
            bindings = new LinkedHashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, gson.toJson(bindings), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public MerchantType typeOf(UUID denizenId) {
        Binding binding = bindingOf(denizenId);
        return binding == null ? null : binding.type();
    }

    /** Returns the category filter for this NPC's binding, or null if unrestricted. */
    public String categoryOf(UUID denizenId) {
        Binding binding = bindingOf(denizenId);
        return binding == null ? null : binding.category();
    }

    /** Returns the shop catalog id for this NPC's binding, or null if not a market merchant. */
    public String shopIdOf(UUID denizenId) {
        Binding binding = bindingOf(denizenId);
        return binding == null ? null : binding.shopId();
    }

    public Binding bindingOf(UUID denizenId) {
        return denizenId == null ? null : bindings.get(denizenId);
    }

    public void bind(UUID denizenId, MerchantType type) {
        bind(denizenId, type, null, null);
    }

    public void bind(UUID denizenId, MerchantType type, String category) {
        bind(denizenId, type, category, null);
    }

    public void bind(UUID denizenId, MerchantType type, String category, String shopId) {
        if (denizenId == null || type == null) {
            return;
        }
        String normalizedCategory = (category == null || category.isBlank()) ? null : category;
        String normalizedShopId = (shopId == null || shopId.isBlank()) ? null : shopId;
        bindings.put(denizenId, new Binding(type, normalizedCategory, normalizedShopId));
        save();
    }

    /** Repoints every MARKET_SHOP binding that references oldShopId to newShopId (case-insensitive). Returns the count updated. */
    public int renameShopId(String oldShopId, String newShopId) {
        if (oldShopId == null || newShopId == null || oldShopId.equalsIgnoreCase(newShopId)) {
            return 0;
        }
        int count = 0;
        for (Map.Entry<UUID, Binding> entry : bindings.entrySet()) {
            Binding binding = entry.getValue();
            if (binding.shopId() != null && binding.shopId().equalsIgnoreCase(oldShopId)) {
                entry.setValue(new Binding(binding.type(), binding.category(), newShopId));
                count++;
            }
        }
        if (count > 0) {
            save();
        }
        return count;
    }

    public void unbind(UUID denizenId) {
        if (denizenId == null) {
            return;
        }
        bindings.remove(denizenId);
        save();
    }

    public List<UUID> listBoundDenizenIds() {
        return List.copyOf(bindings.keySet());
    }
}
