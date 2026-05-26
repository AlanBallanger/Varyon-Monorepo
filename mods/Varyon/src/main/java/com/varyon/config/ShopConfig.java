package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ShopConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "shop.toml";

    private final List<ShopItem> items;

    public ShopConfig(@Nonnull List<ShopItem> items) {
        this.items = new ArrayList<>(items);
    }

    @Nonnull
    public List<ShopItem> getItems() {
        return items;
    }

    @Nonnull
    public static ShopConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            ShopConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);
            Toml shopSection = toml.getTable("shop");
            List<ShopItem> items = new ArrayList<>();
            if (shopSection != null) {
                List<Toml> tables = shopSection.getTables("items");
                if (tables != null) {
                    for (Toml t : tables) {
                        String label = t.getString("label", "Clé");
                        String costItem = t.getString("cost_item", "Key_Fragment1");
                        int costAmount = t.getLong("cost_amount", 1L).intValue();
                        String keyItemId = t.getString("key_item_id", deriveKeyId(costItem));
                        String tierId = t.getString("tier_id", deriveTierId(costItem));
                        items.add(new ShopItem(label, costItem, costAmount, keyItemId, tierId));
                    }
                }
            }
            LOGGER.at(Level.INFO).log("Loaded shop.toml: %s items", items.size());
            return new ShopConfig(items);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load " + FILENAME + ", using defaults", e);
            return createDefault();
        }
    }

    public void save(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateToml());
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save " + FILENAME, e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Shop: acheter des clés (exécution de commande) contre des fragments de clé\n\n");
        for (ShopItem item : items) {
            sb.append("[[shop.items]]\n");
            sb.append("label = \"").append(escape(item.getLabel())).append("\"\n");
            sb.append("cost_item = \"").append(item.getCostItem()).append("\"\n");
            sb.append("cost_amount = ").append(item.getCostAmount()).append("\n");
            sb.append("key_item_id = \"").append(item.getKeyItemId()).append("\"\n");
            sb.append("tier_id = \"").append(item.getTierId()).append("\"\n\n");
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String deriveTierId(String costItem) {
        if (costItem != null && costItem.startsWith("Key_Fragment")) {
            String num = costItem.substring("Key_Fragment".length());
            return "tier" + num;
        }
        return "tier1";
    }

    private static String deriveKeyId(String costItem) {
        if (costItem != null && costItem.startsWith("Key_Fragment")) {
            try {
                int z = Integer.parseInt(costItem.substring("Key_Fragment".length()));
                return zoneKeyId(z);
            } catch (NumberFormatException ignored) {}
        }
        return "Key1";
    }

    private static String zoneKeyId(int zone) {
        return zone == 9 ? "Key12" : "Key" + zone;
    }

    @Nonnull
    public static ShopConfig createDefault() {
        List<ShopItem> items = new ArrayList<>();
        for (int z = 1; z <= 10; z++) {
            items.add(new ShopItem("Clé Zone " + z, "Key_Fragment" + z, 100, zoneKeyId(z), "tier" + z));
        }
        return new ShopConfig(items);
    }

    public static class ShopItem {
        private final String label;
        private final String costItem;
        private final int costAmount;
        private final String keyItemId;
        private final String tierId;

        public ShopItem(String label, String costItem, int costAmount, String keyItemId, String tierId) {
            this.label = label;
            this.costItem = costItem;
            this.costAmount = costAmount;
            this.keyItemId = keyItemId;
            this.tierId = tierId;
        }

        public String getLabel() { return label; }
        public String getCostItem() { return costItem; }
        public int getCostAmount() { return costAmount; }
        public String getKeyItemId() { return keyItemId; }
        public String getTierId() { return tierId; }
    }
}
