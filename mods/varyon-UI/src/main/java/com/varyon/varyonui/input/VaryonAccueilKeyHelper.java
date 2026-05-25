package com.varyon.varyonui.input;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.varyonui.config.MenuShortcutTargetConfig;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VaryonAccueilKeyHelper {

    private static final Logger LOG = Logger.getLogger("VaryonUI");
    private static final ConcurrentHashMap<UUID, Long> OPEN_KEY_DEBOUNCE_AT_MS = new ConcurrentHashMap<>();
    private static final long OPEN_KEY_DEBOUNCE_MS = 400L;

    private VaryonAccueilKeyHelper() {
    }

    public static void clearOpenKeyDebounce(@Nonnull UUID uuid) {
        OPEN_KEY_DEBOUNCE_AT_MS.remove(uuid);
    }

    public static void runAccueilOpenKeyDebounced(@Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            runAccueil(playerRef);
            return;
        }
        long now = System.currentTimeMillis();
        final boolean[] fire = {false};
        OPEN_KEY_DEBOUNCE_AT_MS.compute(uuid, (k, prev) -> {
            if (prev != null && now - prev < OPEN_KEY_DEBOUNCE_MS) {
                return prev;
            }
            fire[0] = true;
            return now;
        });
        if (fire[0]) {
            runAccueil(playerRef);
        }
    }

    public static void runAccueil(@Nonnull PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        try {
            Ref<?> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }
            @SuppressWarnings("rawtypes")
            Store store = ref.getStore();
            if (store == null) {
                return;
            }
            World world = ((EntityStore) store.getExternalData()).getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    String cmd = MenuShortcutTargetConfig.getInstance().getCommandName(playerRef.getUuid());
                    CommandManager.get().handleCommand(playerRef, cmd);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "VaryonUI: menu shortcut command from key", e);
                }
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "VaryonUI: schedule /accueil", e);
        }
    }
}
