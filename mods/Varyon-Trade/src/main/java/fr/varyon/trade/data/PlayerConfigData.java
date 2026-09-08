package fr.varyon.trade.data;

import fr.varyon.trade.VaryonTradePlugin;
import fr.varyon.trade.TradeConfig;
import com.google.gson.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.Set;

public class PlayerConfigData
{
    private final static String CONFIG_FOLDER_PATH;
    private final static String PLAYER_CONFIG_FOLDER_NAME = "players_config";
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private final static Set<PlayerConfigData> DIRTY_CONFIGS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final ConcurrentHashMap<UUID, CachedConfig> CACHE = new ConcurrentHashMap<>();

    static
    {
        CONFIG_FOLDER_PATH = TradeConfig.getConfigFolderPath().toString();

        VaryonTradePlugin.SCHEDULER.scheduleAtFixedRate(() ->
        {
            if (!DIRTY_CONFIGS.isEmpty())
            {
                for (PlayerConfigData config : new ArrayList<>(DIRTY_CONFIGS))
                {
                    config.save();
                    DIRTY_CONFIGS.remove(config);
                }
            }

            CACHE.entrySet().removeIf(entry ->
                    entry.getValue().isExpired() && !DIRTY_CONFIGS.contains(entry.getValue().data)
            );

        }, 5, 5, TimeUnit.SECONDS);
    }

    private final UUID playerUUID;
    public final ConfigVars vars = new ConfigVars();

    private static class CachedConfig
    {
        PlayerConfigData data;
        long loadTime;

        CachedConfig(PlayerConfigData data)
        {
            this.data = data;
            this.loadTime = System.currentTimeMillis();
        }

        boolean isExpired()
        {
            return System.currentTimeMillis() - loadTime > CACHE_TTL_MS;
        }
    }

    public static class IgnoredPlayer
    {
        public String username;
        public String uuid;
        public IgnoredPlayer(String username, String uuid)
        {
            this.username = username;
            this.uuid = uuid;
        }
    }

    public static class LastTradeRequestSent
    {
        public long unixTime;
        public String targetPlayerUUID;
        public LastTradeRequestSent(long unixTime, String targetPlayerUUID)
        {
            this.unixTime = unixTime;
            this.targetPlayerUUID = targetPlayerUUID;
        }
    }

    public class ConfigVar<T>
    {
        private T value;
        public ConfigVar(T value) { this.value = value; }
        public T getValue() { return value; }

        public void setValue(T value)
        {
            if (this.value != null && this.value.equals(value) && !(value instanceof List)) return;
            this.value = value;
            DIRTY_CONFIGS.add(PlayerConfigData.this);
        }

        private void loadValue(T value) { this.value = value; }
    }

    public class ConfigVars
    {
        public final ConfigVar<String> language = new ConfigVar<>("en-US");
        public final ConfigVar<LastTradeRequestSent> lastTradeRequestEpochTime = new ConfigVar<>(new LastTradeRequestSent(0, null));
        public final ConfigVar<Integer> tradePanelPosition = new ConfigVar<>(0);
        public final ConfigVar<String> tradePanelLayoutName = new ConfigVar<>("Horizontal");
        public final ConfigVar<Boolean> tradeIgnore = new ConfigVar<>(false);
        public final ConfigVar<List<IgnoredPlayer>> ignoredPlayers = new ConfigVar<>(new ArrayList<>());
    }

    public PlayerConfigData(UUID playerUUID)
    {
        this.playerUUID = playerUUID;
    }

    public static PlayerConfigData getConfigData(UUID playerUUID)
    {
        CachedConfig cached = CACHE.get(playerUUID);
        if (cached != null && !cached.isExpired())
        {
            return cached.data;
        }

        Path filePath = Path.of(CONFIG_FOLDER_PATH, PLAYER_CONFIG_FOLDER_NAME, playerUUID.toString() + ".json");
        PlayerConfigData config = new PlayerConfigData(playerUUID);

        if (Files.exists(filePath))
        {
            try
            {
                JsonObject json = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
                boolean dataMigrated = false;

                for (java.lang.reflect.Field field : config.vars.getClass().getDeclaredFields())
                {
                    field.setAccessible(true);
                    if (json.has(field.getName()))
                    {
                        ConfigVar<Object> cv = (ConfigVar<Object>) field.get(config.vars);
                        JsonElement el = json.get(field.getName());

                        if (!el.isJsonNull())
                        {
                            java.lang.reflect.Type type = ((java.lang.reflect.ParameterizedType)
                                    field.getGenericType()).getActualTypeArguments()[0];

                            Object val = GSON.fromJson(el, type);
                            cv.loadValue(val);
                        }
                    }
                    else
                    {
                        dataMigrated = true;
                    }
                }
                if (dataMigrated) config.save();
            }
            catch (Exception e)
            {
                System.err.println("Load error for " + playerUUID + ": " + e.getMessage());
            }
        }
        else
        {
            config.save();
        }

        CACHE.put(playerUUID, new CachedConfig(config));
        return config;
    }

    public void stopTask()
    {
        if (DIRTY_CONFIGS.contains(this))
        {
            save();
            DIRTY_CONFIGS.remove(this);
        }
    }

    public synchronized void save()
    {
        Path filePath = Path.of(CONFIG_FOLDER_PATH, PLAYER_CONFIG_FOLDER_NAME, playerUUID.toString() + ".json");
        try
        {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, this.serializeVars(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            System.err.println("Save error: " + e.getMessage());
        }
    }

    public String serializeVars()
    {
        JsonObject json = new JsonObject();
        for (java.lang.reflect.Field field : vars.getClass().getDeclaredFields())
        {
            try
            {
                field.setAccessible(true);
                Object fieldObj = field.get(vars);
                if (fieldObj instanceof ConfigVar<?> cv)
                {
                    json.add(field.getName(), GSON.toJsonTree(cv.getValue()));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return GSON.toJson(json);
    }
}

