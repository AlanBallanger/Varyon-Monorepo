package com.varyon.crashrestarter;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Thin bootstrap for {@link CrashRestarterMonitor}.
 * Independent of other Varyon mods — clean shutdown only; systemd restarts the process.
 */
public final class CrashRestarterPlugin extends JavaPlugin {

    private CrashRestarterMonitor monitor;

    public CrashRestarterPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        monitor = new CrashRestarterMonitor(getDataDirectory(), getLogger());
        monitor.start();

        getEventRegistry().registerGlobal(EventPriority.LAST, PlayerConnectEvent.class, event -> {
            World resolved = event.getWorld() != null ? event.getWorld() : Universe.get().getDefaultWorld();
            monitor.onConnectAttempt(event.getPlayerRef().getUuid(), resolved);
        });

        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
            PlayerRef ref = event.getHolder().getComponent(PlayerRef.getComponentType());
            if (ref != null) {
                monitor.onJoinSucceeded(ref.getUuid());
            } else {
                monitor.onJoinSucceeded();
            }
        });

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> monitor.onJoinSucceeded());

        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            PlayerRef ref = event.getPlayerRef();
            if (ref != null) {
                monitor.onDisconnectBeforeReady(ref.getUuid());
            }
        });

        getLogger().at(Level.INFO).log("[CrashRestarter] Monitor started (all worlds + any failed joins).");
    }

    @Override
    protected void shutdown() {
        if (monitor != null) {
            monitor.stop();
        }
        super.shutdown();
    }
}
