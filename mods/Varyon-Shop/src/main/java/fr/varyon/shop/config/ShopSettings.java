package fr.varyon.shop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Global Varyon-Shop settings. Buyback rate applied by unrestricted (general) buyback merchants
 * — category-restricted "profession" merchants always pay 100% of the listed price, the general
 * rate is a fraction of it (e.g. 0.7 = 70%). Also holds the list of currencies shop items can be
 * priced in: "Coins" (VaultUnlocked, itemId null/blank) plus any admin-added physical item.
 */
public final class ShopSettings {
    private static final double DEFAULT_GENERAL_BUYBACK_RATE = 0.7;

    /** itemId null/blank means "Coins" (VaultUnlocked economy); otherwise a physical item currency. */
    public static final class CurrencyOption {
        public String itemId;
        public String label;

        public CurrencyOption() {
        }

        public CurrencyOption(String itemId, String label) {
            this.itemId = itemId;
            this.label = label;
        }
    }

    private static final class Data {
        double generalBuybackRate = DEFAULT_GENERAL_BUYBACK_RATE;
        /** Server-wide bonus added on top of every buyback rate (general and profession), e.g. 0.2 = +20 points. */
        double buybackBonus = 0.0;
        /** Server-wide bonus added on top of every shop's purchase price multiplier, e.g. 0.2 = +20 points. */
        double purchaseBonus = 0.0;
        List<CurrencyOption> currencies = defaultCurrencies();
    }

    private static List<CurrencyOption> defaultCurrencies() {
        List<CurrencyOption> list = new ArrayList<>();
        list.add(new CurrencyOption(null, "Coins"));
        return list;
    }

    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Data data = new Data();

    public ShopSettings(Path filePath) {
        this.filePath = filePath;
    }

    public void load() {
        try {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                data = new Data();
                save();
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Data parsed = gson.fromJson(raw, Data.class);
            data = parsed == null ? new Data() : parsed;
        } catch (IOException e) {
            data = new Data();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, gson.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public double generalBuybackRate() {
        return data.generalBuybackRate;
    }

    public void setGeneralBuybackRate(double rate) {
        data.generalBuybackRate = Math.max(0.0, rate);
        save();
    }

    public double buybackBonus() {
        return data.buybackBonus;
    }

    public void setBuybackBonus(double bonus) {
        data.buybackBonus = Math.max(0.0, bonus);
        save();
    }

    public double purchaseBonus() {
        return data.purchaseBonus;
    }

    public void setPurchaseBonus(double bonus) {
        data.purchaseBonus = Math.max(0.0, bonus);
        save();
    }

    public List<CurrencyOption> listCurrencies() {
        if (data.currencies == null || data.currencies.isEmpty()) {
            data.currencies = defaultCurrencies();
        }
        return data.currencies;
    }

    /** Adds a new physical-item currency if not already present (by itemId). No-op for blank ids. */
    public void addCurrency(String itemId, String label) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        String trimmedId = itemId.trim();
        for (CurrencyOption existing : listCurrencies()) {
            if (trimmedId.equalsIgnoreCase(existing.itemId)) {
                return;
            }
        }
        String trimmedLabel = (label == null || label.isBlank()) ? trimmedId : label.trim();
        listCurrencies().add(new CurrencyOption(trimmedId, trimmedLabel));
        save();
    }

    public void removeCurrency(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        listCurrencies().removeIf(c -> itemId.equalsIgnoreCase(c.itemId));
        save();
    }
}
