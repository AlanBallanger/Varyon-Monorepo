package fr.varyon.trade;

import fr.varyon.trade.data.PlayerConfigData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import net.cfh.vault.VaultUnlockedServicesManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class TradeConfig
{
    private static TradeConfig instance;
    private final Path configPath;

    private final Map<String, ConfigElement> schema = new LinkedHashMap<>();

    private String
            permBase,
            permSettings,
            permTrade,
            permFromFar,
            notificationIconId,
            tradePanelOpenSoundId,
            tradePanelCloseSoundId,
            tradeDialogOpenSoundId,
            tradeDialogCloseSoundId,
            currencyIconFilename;

    private double
            tradeRequestCooldownSeconds,
            tradeMaxDistance;

    private boolean
            isMoneyTradable,
            isMoneyDecimal;

    public static TradeConfig get()
    {
        if (instance == null)
        {
            instance = new TradeConfig();
        }
        return instance;
    }

    private TradeConfig()
    {
        this.configPath = Path.of(TradeConfig.getConfigFolderPath(), "config.json");
        setupSchema();

        if (!Files.exists(configPath))
        {
            saveDefaultConfig();
        }

        reload();
    }

    private void setupSchema()
    {
        addCategoryTitle("Permissions (reload = \"varyon.trade.reload\")");
        addEntry("PERM_BASE", "varyon.trade");
        addEntry("PERM_SETTINGS", "settings");
        addEntry("PERM_TRADE", "trade");
        addEntry("PERM_FROMFAR", "fromfar");

        addCategoryTitle("Trade settings");
        addEntry("TRADE_REQUEST_COOLDOWN_SECONDS", 6.0);
        addEntry("TRADE_MAX_DISTANCE", 5.0);

        addCategoryTitle("Assets");
        addEntry("NOTIFICATION_ICON_ID", "Utility_Leather_Backpack");
        addEntry("TRADE_PANEL_OPEN_SOUND_ID", "SFX_Axe_Special_Swing");
        addEntry("TRADE_PANEL_CLOSE_SOUND_ID", "SFX_Drag_Items_Chest");
        addEntry("TRADE_DIALOG_OPEN_SOUND_ID", "SFX_Cactus_Hit");
        addEntry("TRADE_DIALOG_CLOSE_SOUND_ID", "SFX_Branch_Walk");

        addCategoryTitle("Money");
        addEntry("IS_MONEY_TRADABLE", false);
        addEntry("IS_MONEY_DECIMAL", false);
        addEntry("CURRENCY_ICON_FILENAME", "");
    }

    private void addCategoryTitle(String title)
    {
        schema.put("INTERNAL_TITLE_" + title, new ConfigTitle(title));
    }

    private void addEntry(String key, Object defaultValue)
    {
        schema.put(key, new ConfigValue(defaultValue));
    }

    private void saveDefaultConfig()
    {
        try
        {
            Files.createDirectories(configPath.getParent());
            StringBuilder sb = new StringBuilder("{\n");

            int elementsProcessed = 0;
            int totalEntries = (int) schema.values().stream().filter(e -> e instanceof ConfigValue).count();
            int currentEntryIndex = 0;

            for (ConfigElement element : schema.values())
            {
                if (element instanceof ConfigTitle title)
                {
                    if (elementsProcessed > 0) sb.append("\n");
                    sb.append("  // ").append(title.name).append("\n");
                }
                else if (element instanceof ConfigValue val)
                {
                    String key = "";
                    for (Map.Entry<String, ConfigElement> entry : schema.entrySet())
                    {
                        if (entry.getValue() == val) { key = entry.getKey(); break; }
                    }

                    sb.append("  \"").append(key).append("\": ");
                    if (val.defaultValue instanceof String) sb.append("\"").append(val.defaultValue).append("\"");
                    else sb.append(val.defaultValue);

                    if (++currentEntryIndex < totalEntries) sb.append(",");
                    sb.append("\n");
                }
                elementsProcessed++;
            }
            sb.append("}");

            Files.writeString(configPath, sb.toString(), StandardOpenOption.CREATE);
        }
        catch (Exception e)
        {
            HytaleLogger.forEnclosingClass().atSevere().log("Could not create config: " + e.getMessage());
        }
    }

    public String reload()
    {
        try
        {
            if (!Files.exists(configPath)) return "Config file not found.";

            String content = Files.readString(configPath);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            permBase = getStringOrNull(json, "PERM_BASE");
            permSettings = getStringOrNull(json, "PERM_SETTINGS");
            permTrade = getStringOrNull(json, "PERM_TRADE");
            permFromFar = getStringOrNull(json, "PERM_FROMFAR");

            tradeRequestCooldownSeconds = getDouble(json, "TRADE_REQUEST_COOLDOWN_SECONDS", (double) ((ConfigValue)schema.get("TRADE_REQUEST_COOLDOWN_SECONDS")).defaultValue);
            tradeMaxDistance = getDouble(json, "TRADE_MAX_DISTANCE", (double) ((ConfigValue)schema.get("TRADE_MAX_DISTANCE")).defaultValue);

            notificationIconId = getString(json, "NOTIFICATION_ICON_ID", (String) ((ConfigValue)schema.get("NOTIFICATION_ICON_ID")).defaultValue);
            tradePanelOpenSoundId = getString(json, "TRADE_PANEL_OPEN_SOUND_ID", (String) ((ConfigValue)schema.get("TRADE_PANEL_OPEN_SOUND_ID")).defaultValue);
            tradePanelCloseSoundId = getString(json, "TRADE_PANEL_CLOSE_SOUND_ID", (String) ((ConfigValue)schema.get("TRADE_PANEL_CLOSE_SOUND_ID")).defaultValue);
            tradeDialogOpenSoundId = getString(json, "TRADE_DIALOG_OPEN_SOUND_ID", (String) ((ConfigValue)schema.get("TRADE_DIALOG_OPEN_SOUND_ID")).defaultValue);
            tradeDialogCloseSoundId = getString(json, "TRADE_DIALOG_CLOSE_SOUND_ID", (String) ((ConfigValue)schema.get("TRADE_DIALOG_CLOSE_SOUND_ID")).defaultValue);

            isMoneyTradable = getBool(json, "IS_MONEY_TRADABLE", (boolean) ((ConfigValue)schema.get("IS_MONEY_TRADABLE")).defaultValue);
            isMoneyDecimal = getBool(json, "IS_MONEY_DECIMAL", (boolean) ((ConfigValue)schema.get("IS_MONEY_DECIMAL")).defaultValue);
            currencyIconFilename = getString(json, "CURRENCY_ICON_FILENAME", (String) ((ConfigValue)schema.get("CURRENCY_ICON_FILENAME")).defaultValue);
            if (!Files.exists(Path.of(getConfigFolderPath(), currencyIconFilename))) currencyIconFilename = "";

            return "Config reloaded.";
        }
        catch (Exception e)
        {
            HytaleLogger.forEnclosingClass().atSevere().log("Error reloading config: " + e.getMessage());
            return "An error occurred.";
        }
    }

    private interface ConfigElement {}
    private record ConfigTitle(String name) implements ConfigElement {}
    private record ConfigValue(Object defaultValue) implements ConfigElement {}

    private String getStringOrNull(JsonObject json, String key) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : null; }
    private String getString(JsonObject json, String key, String def) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : def; }
    private double getDouble(JsonObject json, String key, double def) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsDouble() : def; }
    private boolean getBool(JsonObject json, String key, boolean def) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsBoolean() : def; }

    public static String getConfigFolderPath()
    {
        try
        {
            Path jarLocation = Path.of(PlayerConfigData.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            return jarLocation.resolve("Varyon-Trade").toString();
        }
        catch (Exception e) { return "Varyon-Trade"; }
    }

    public boolean arePermsEmpty() { return permBase == null; }
    public String getFullPermSettings()
    {
        return !arePermsEmpty() && permSettings != null ? permBase + "." + permSettings : null;
    }
    public String getFullPermTrade()
    {
        return !arePermsEmpty() && permTrade != null ? permBase + "." + permTrade : null;
    }
    public String getFullPermFromFar()
    {
        return !arePermsEmpty() && permFromFar != null ? permBase + "." + permFromFar : null;
    }

    public double getTradeRequestCooldownSeconds() { return tradeRequestCooldownSeconds; }
    public double getTradeMaxDistance() { return tradeMaxDistance; }

    public String getNotificationIconId() { return notificationIconId; }
    public String getTradePanelOpenSoundId() { return tradePanelOpenSoundId; }
    public String getTradePanelCloseSoundId() { return tradePanelCloseSoundId; }
    public String getTradeDialogOpenSoundId() { return tradeDialogOpenSoundId; }
    public String getTradeDialogCloseSoundId() { return tradeDialogCloseSoundId; }

    public boolean getIsMoneyTradable() { return isMoneyTradable; }
    public boolean getIsMoneyDecimal() { return isMoneyDecimal; }
    public String getFullCurrencyIconPath()
    {
        return currencyIconFilename != null && currencyIconFilename.length() > 0 ?
               Path.of(getConfigFolderPath(), currencyIconFilename).toString() : "";
    }
}

