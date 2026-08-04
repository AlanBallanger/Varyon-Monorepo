package fr.varyon.worldmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.player.SetGameMode;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Per-world game mode, opt-in: only worlds listed in worldModes.json are managed.
 * Entering a listed world switches the player to that mode (their previous mode is
 * remembered); entering any unlisted world restores the remembered mode. Operators
 * are exempt.
 */
public class GameModeManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String CONFIG_TEMPLATE = "{\n"
            + "  \"_info\": \"Varyon-World-Manager gamemode - per-world game mode. STRICTLY OPT-IN: only worlds listed\\n"
            + "under worldModes are managed. Entering a listed world switches the player to that mode\\n"
            + "(their previous mode is remembered); entering any unlisted world restores the remembered\\n"
            + "mode. Worlds run by other plugins (arenas, player worlds...) should stay unlisted.\\n"
            + "Valid modes: Adventure, Creative. Edit while the server is stopped and restart to apply.\\n"
            + "The two exampleworld entries below are just examples showing the format - replace them\\n"
            + "with your real world names. Entries for worlds that do not exist are harmless (they simply\\n"
            + "never match). Operators are exempt from everything this manager does.\",\n"
            + "  \"worldModes\": {\n"
            + "    \"exampleworld1\": \"Creative\",\n"
            + "    \"exampleworld2\": \"Adventure\"\n"
            + "  }\n"
            + "}";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path configFile;
    private final Path savedFile;
    private final ConcurrentHashMap<String, String> savedModes = new ConcurrentHashMap<>();
    private Map<String, String> worldModes = new LinkedHashMap<>();

    public GameModeManager(Path dataDirectory) {
        this.configFile = dataDirectory.resolve("worldModes.json");
        this.savedFile = dataDirectory.resolve("saved-gamemodes.json");
        loadConfig();
        loadSaved();
    }

    public void onWorldJoin(AddPlayerToWorldEvent event) {
        try {
            onWorldJoinUnsafe(event);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("GameModeManager onWorldJoin: %s", e.getMessage());
        }
    }

    private void onWorldJoinUnsafe(AddPlayerToWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        World world = event.getWorld();
        if (holder == null || world == null) {
            return;
        }
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        if (isOp(playerRef)) {
            return;
        }
        String mode = worldModes.get(world.getName());
        scheduleApply(playerRef, world, mode, 120);
    }

    private void scheduleApply(PlayerRef playerRef, World world, String mode, int attempt) {
        boolean materialised;
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            materialised = ref != null && ref.isValid()
                    && ref.getStore().getExternalData() != null
                    && ((EntityStore) ref.getStore().getExternalData()).getWorld() == world;
        } catch (Exception e) {
            materialised = false;
        }

        if (!materialised) {
            if (attempt <= 0) {
                return;
            }
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> scheduleApply(playerRef, world, mode, attempt - 1),
                    250, TimeUnit.MILLISECONDS);
            return;
        }

        world.execute(() -> {
            Applied applied = applyNow(playerRef, world, mode);
            if (applied != null) {
                finalizeApply(playerRef, world, applied);
            }
        });
    }

    private Applied applyNow(PlayerRef playerRef, World world, String mode) {
        if (!isMaterialised(playerRef, world)) {
            return null;
        }
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return null;
            }
            GameMode current = player.getGameMode();
            String playerId = playerRef.getUuid().toString();
            GameMode desired;
            String message;

            if (mode != null) {
                desired = parseMode(mode);
                if (desired == null) {
                    return null;
                }
                if (!savedModes.containsKey(playerId) && current != null && current != desired) {
                    savedModes.put(playerId, current.name());
                    saveSaved();
                }
                message = "Game mode: " + desired.name().toLowerCase();
            } else {
                String remembered = savedModes.remove(playerId);
                if (remembered == null) {
                    return null;
                }
                saveSaved();
                desired = parseMode(remembered);
                if (desired == null) {
                    return null;
                }
                message = "Game mode restored: " + desired.name().toLowerCase();
            }

            if (desired != GameMode.Creative && current != desired) {
                Player.setGameMode(ref, desired, store);
                playerRef.sendMessage(Message.raw(message));
            }
            return new Applied(desired, message);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("GameModeManager applyNow: %s", e.getMessage());
            return null;
        }
    }

    private void finalizeApply(PlayerRef playerRef, World world, Applied applied) {
        if (!isMaterialised(playerRef, world)) {
            return;
        }
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            GameMode current = player.getGameMode();
            if (current != applied.desired) {
                Player.setGameMode(ref, applied.desired, store);
                playerRef.sendMessage(Message.raw(applied.message));
            } else {
                playerRef.getPacketHandler().writeNoCache(new SetGameMode(applied.desired));
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("GameModeManager finalizeApply: %s", e.getMessage());
        }
    }

    private boolean isMaterialised(PlayerRef playerRef, World world) {
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return false;
            }
            Store<EntityStore> store = ref.getStore();
            if (store.getExternalData() == null
                    || ((EntityStore) store.getExternalData()).getWorld() != world) {
                return false;
            }
            return store.getComponent(ref, Player.getComponentType()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static GameMode parseMode(String name) {
        if (name == null) {
            return null;
        }
        for (GameMode value : GameMode.VALUES) {
            if (value.name().equalsIgnoreCase(name.trim())) {
                return value;
            }
        }
        return null;
    }

    private boolean isOp(PlayerRef playerRef) {
        try {
            return playerRef.hasPermission("*");
        } catch (Exception e) {
            return false;
        }
    }

    private void loadConfig() {
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                Files.writeString(configFile, CONFIG_TEMPLATE);
            }
            Map<String, Object> root = gson.fromJson(Files.readString(configFile), ROOT_TYPE);
            Object rawWorldModes = root != null ? root.get("worldModes") : null;
            Map<String, String> parsed = new LinkedHashMap<>();
            if (rawWorldModes instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getValue() instanceof String value) {
                        parsed.put(String.valueOf(entry.getKey()), value);
                    }
                }
            }
            worldModes = parsed;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("GameModeManager loadConfig: %s", e.getMessage());
        }
    }

    private void loadSaved() {
        try {
            if (!Files.exists(savedFile)) {
                return;
            }
            Map<String, String> saved = gson.fromJson(Files.readString(savedFile), SAVED_TYPE);
            if (saved != null) {
                savedModes.putAll(saved);
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("GameModeManager loadSaved: %s", e.getMessage());
        }
    }

    public synchronized void saveSaved() {
        try {
            writeAtomic(savedFile, gson.toJson(savedModes));
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("GameModeManager saveSaved: %s", e.getMessage());
        }
    }

    private void writeAtomic(Path target, String content) throws Exception {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static final Type ROOT_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Type SAVED_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private record Applied(GameMode desired, String message) {}
}
