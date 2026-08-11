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
 * it. Configured by an admin (future admin UI/command); persisted as denizenId -&gt; type name.
 */
public final class MerchantRegistry {

    public enum MerchantType {
        BUYBACK_GENERAL,
        BUYBACK_PROFESSION,
        MARKET_DECOR,
        MARKET_RESOURCES
    }

    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, MerchantType> bindings = new LinkedHashMap<>();

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
            Type type = new TypeToken<Map<UUID, MerchantType>>() {}.getType();
            Map<UUID, MerchantType> parsed = gson.fromJson(raw, type);
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
        return denizenId == null ? null : bindings.get(denizenId);
    }

    public void bind(UUID denizenId, MerchantType type) {
        if (denizenId == null || type == null) {
            return;
        }
        bindings.put(denizenId, type);
        save();
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
