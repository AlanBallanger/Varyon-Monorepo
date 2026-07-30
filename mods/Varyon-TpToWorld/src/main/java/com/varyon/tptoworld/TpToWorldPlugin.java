package com.varyon.tptoworld;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class TpToWorldPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public TpToWorldPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            getCommandRegistry().registerCommand(new TpToWorldCommand());
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-TpToWorld] Échec enregistrement de la commande");
        }
    }
}
