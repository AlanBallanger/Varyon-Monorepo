package fr.varyon.shop.integration;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Reflection bridge to the QuestLinesDenizens plugin's public DenizensAPI. No compile-time
 * dependency on that plugin's jar: mirrors the soft-dependency pattern QuestLinesDenizens itself
 * uses for QuestLines Core (see QuestLinesBridge in that mod).
 */
public final class DenizensBridge {
    private static final PluginIdentifier DENIZENS_ID = new PluginIdentifier("net.evilcraft", "QuestLinesDenizens");

    private final Object api;
    private final Method addInteractListenerMethod;
    private final Method findByNameOrIdMethod;
    private final Method getEntityUuidMethod;
    private final Method findDenizenIdByEntityUuidMethod;
    private final Method getSnapshotMethod;
    private final Method listAllIdsMethod;
    private final boolean available;

    private DenizensBridge(
            Object api,
            Method addInteractListenerMethod,
            Method findByNameOrIdMethod,
            Method getEntityUuidMethod,
            Method findDenizenIdByEntityUuidMethod,
            Method getSnapshotMethod,
            Method listAllIdsMethod
    ) {
        this.api = api;
        this.addInteractListenerMethod = addInteractListenerMethod;
        this.findByNameOrIdMethod = findByNameOrIdMethod;
        this.getEntityUuidMethod = getEntityUuidMethod;
        this.findDenizenIdByEntityUuidMethod = findDenizenIdByEntityUuidMethod;
        this.getSnapshotMethod = getSnapshotMethod;
        this.listAllIdsMethod = listAllIdsMethod;
        this.available = api != null;
    }

    public static DenizensBridge connect() {
        try {
            PluginBase core = HytaleServer.get().getPluginManager().getPlugin(DENIZENS_ID);
            if (core == null) {
                return unavailable();
            }
            Object api = core.getClass().getMethod("getApi").invoke(core);
            if (api == null) {
                return unavailable();
            }
            Class<?> apiClass = api.getClass();
            Method addInteractListenerMethod = apiClass.getMethod("addInteractListener", BiConsumer.class);
            Method findByNameOrIdMethod = apiClass.getMethod("findByNameOrId", String.class);
            Method getEntityUuidMethod = apiClass.getMethod("getEntityUuid", UUID.class);
            Method findDenizenIdByEntityUuidMethod = apiClass.getMethod("findDenizenIdByEntityUuid", UUID.class);
            Method getSnapshotMethod = apiClass.getMethod("getSnapshot", UUID.class);
            Method listAllIdsMethod = apiClass.getMethod("listAllIds");
            return new DenizensBridge(api, addInteractListenerMethod, findByNameOrIdMethod, getEntityUuidMethod, findDenizenIdByEntityUuidMethod, getSnapshotMethod, listAllIdsMethod);
        } catch (Exception e) {
            return unavailable();
        }
    }

    private static DenizensBridge unavailable() {
        return new DenizensBridge(null, null, null, null, null, null, null);
    }

    public boolean isAvailable() {
        return available;
    }

    /** Registers a listener fired whenever a player interacts with any Denizen NPC. */
    public boolean addInteractListener(BiConsumer<UUID, PlayerRef> listener) {
        if (!available || addInteractListenerMethod == null) {
            return false;
        }
        try {
            addInteractListenerMethod.invoke(api, listener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Resolves a Denizen's UUID from its configured name or short id. */
    public UUID findByNameOrId(String needle) {
        if (!available || findByNameOrIdMethod == null || needle == null) {
            return null;
        }
        try {
            Object result = findByNameOrIdMethod.invoke(api, needle);
            if (result instanceof java.util.Optional<?> opt) {
                return opt.map(v -> (UUID) v).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Resolves a Denizen's configured display name (from its Snapshot), or null if unknown. */
    public String getName(UUID denizenId) {
        if (!available || getSnapshotMethod == null || denizenId == null) {
            return null;
        }
        try {
            Object snapshot = getSnapshotMethod.invoke(api, denizenId);
            if (snapshot == null) {
                return null;
            }
            Object name = snapshot.getClass().getMethod("name").invoke(snapshot);
            return name instanceof String s ? s : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Lists every Denizen id known to the registry. */
    @SuppressWarnings("unchecked")
    public java.util.List<UUID> listAllIds() {
        if (!available || listAllIdsMethod == null) {
            return java.util.List.of();
        }
        try {
            Object result = listAllIdsMethod.invoke(api);
            if (result instanceof java.util.List<?> list) {
                return (java.util.List<UUID>) list;
            }
        } catch (Exception ignored) {
        }
        return java.util.List.of();
    }
}
