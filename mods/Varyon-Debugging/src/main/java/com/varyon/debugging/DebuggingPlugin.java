package com.varyon.debugging;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.varyon.debugging.command.SpawnReportCommand;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class DebuggingPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DebuggingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            this.getEntityStoreRegistry().registerSystem(new SpawnTrackingSystem());
            this.getCommandRegistry().registerCommand(new SpawnReportCommand());
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-Debugging] Echec initialisation");
        }
    }
}
