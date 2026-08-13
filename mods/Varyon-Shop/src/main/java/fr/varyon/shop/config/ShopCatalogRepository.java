package fr.varyon.shop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Loads/saves purchasable shop catalogs. Each shop is a separate JSON file under
 * mods/Varyon_Shop/shops/&lt;shopId&gt;.json, keyed by shop id (the id used with /vshop bind).
 *
 * A catalog with a blank displayName is a "draft": held in memory (so its editor screen keeps
 * working), excluded from listShopIds(), and never written to disk. It becomes real the moment
 * displayName is set to something non-blank and save() is called.
 */
public final class ShopCatalogRepository {
    private final Path shopsDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, ShopCatalog> cache = new ConcurrentHashMap<>();
    private final AtomicInteger draftCounter = new AtomicInteger();

    public ShopCatalogRepository(Path shopsDir) {
        this.shopsDir = shopsDir;
    }

    public void load() {
        cache.clear();
        try {
            Files.createDirectories(shopsDir);
        } catch (IOException ignored) {
        }
        try (Stream<Path> files = Files.list(shopsDir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                String shopId = fileNameToShopId(file);
                try {
                    String raw = Files.readString(file, StandardCharsets.UTF_8);
                    ShopCatalog catalog = gson.fromJson(raw, ShopCatalog.class);
                    if (catalog != null) {
                        for (ShopCatalog.Entry entry : catalog.items) {
                            entry.migrateLegacyStock();
                        }
                        cache.put(shopId, catalog);
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public java.util.Optional<ShopCatalog> get(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(cache.get(shopId.toLowerCase(java.util.Locale.ROOT)));
    }

    public boolean exists(String shopId) {
        return get(shopId).isPresent();
    }

    private boolean isReal(ShopCatalog catalog) {
        return catalog.displayName != null && !catalog.displayName.isBlank();
    }

    /** Only lists real (non-draft) shops. */
    public List<String> listShopIds() {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, ShopCatalog> entry : cache.entrySet()) {
            if (isReal(entry.getValue())) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    /** Creates an empty shop catalog if it doesn't already exist. Returns false if it already exists. */
    public boolean createIfAbsent(String shopId, String displayName) {
        if (shopId == null || shopId.isBlank()) {
            return false;
        }
        String key = shopId.toLowerCase(java.util.Locale.ROOT);
        if (cache.containsKey(key)) {
            return false;
        }
        ShopCatalog catalog = new ShopCatalog();
        catalog.displayName = displayName == null ? "" : displayName;
        cache.put(key, catalog);
        save(key);
        return true;
    }

    /** Creates a new draft shop with an auto-generated id, held only in memory until named. Returns the new id. */
    public String createDraft() {
        String key;
        do {
            key = "boutique_" + draftCounter.incrementAndGet();
        } while (cache.containsKey(key));
        cache.put(key, new ShopCatalog());
        return key;
    }

    /**
     * Derives a shop id from a display name (lowercase, spaces/punctuation to underscores),
     * disambiguated with a numeric suffix if the slug is already taken. "Boutique de fruits" ->
     * "boutique_de_fruits" (or "boutique_de_fruits_2" if that id already exists).
     */
    public String slugify(String displayName, String currentKey) {
        String base = displayName == null ? "" : displayName.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isBlank()) {
            base = "boutique";
        }
        String candidate = base;
        int suffix = 2;
        while (cache.containsKey(candidate) && !candidate.equals(currentKey)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    /**
     * Renames a shop's id (its slug), moving it in the cache and on disk. Returns the new key,
     * or the original key unchanged if the rename couldn't happen (blank/taken/missing source).
     */
    public String rename(String oldShopId, String newShopId) {
        if (oldShopId == null || newShopId == null || newShopId.isBlank()) {
            return oldShopId;
        }
        String oldKey = oldShopId.toLowerCase(java.util.Locale.ROOT);
        String newKey = newShopId.toLowerCase(java.util.Locale.ROOT);
        if (oldKey.equals(newKey)) {
            return oldShopId;
        }
        ShopCatalog catalog = cache.get(oldKey);
        if (catalog == null || cache.containsKey(newKey)) {
            return oldShopId;
        }
        cache.remove(oldKey);
        cache.put(newKey, catalog);
        try {
            Files.deleteIfExists(shopsDir.resolve(oldKey + ".json"));
        } catch (IOException ignored) {
        }
        save(newKey);
        return newKey;
    }

    /** Deletes a shop catalog, both from memory and disk. Returns false if it didn't exist. */
    public boolean delete(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            return false;
        }
        String key = shopId.toLowerCase(java.util.Locale.ROOT);
        if (cache.remove(key) == null) {
            return false;
        }
        try {
            Files.deleteIfExists(shopsDir.resolve(key + ".json"));
        } catch (IOException ignored) {
        }
        return true;
    }

    /** Writes the catalog to disk, unless it's still a draft (blank displayName) — drafts stay in-memory only. */
    public void save(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            return;
        }
        String key = shopId.toLowerCase(java.util.Locale.ROOT);
        ShopCatalog catalog = cache.get(key);
        if (catalog == null) {
            return;
        }
        if (!isReal(catalog)) {
            try {
                Files.deleteIfExists(shopsDir.resolve(key + ".json"));
            } catch (IOException ignored) {
            }
            return;
        }
        try {
            Files.createDirectories(shopsDir);
            Files.writeString(shopsDir.resolve(key + ".json"), gson.toJson(catalog), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String fileNameToShopId(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".json".length()).toLowerCase(java.util.Locale.ROOT);
    }
}
