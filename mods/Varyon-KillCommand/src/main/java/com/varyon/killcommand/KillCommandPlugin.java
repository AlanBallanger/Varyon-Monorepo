package com.varyon.killcommand;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.varyon.killcommand.command.KillRootCommand;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class KillCommandPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public KillCommandPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            getCommandRegistry().registerCommand(new KillRootCommand());
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-KillCommand] Échec enregistrement de la commande /kill");
        }
    }
}
