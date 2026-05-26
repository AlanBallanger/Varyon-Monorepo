package com.varyon.signaturepreservation;

import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.varyon.signaturepreservation.config.VaryonSignaturePreservationConfig;
import java.util.logging.Level;

public final class VaryonSignaturePreservationPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Config<VaryonSignaturePreservationConfig> config =
            this.withConfig("VaryonSignaturePreservation", VaryonSignaturePreservationConfig.CODEC);

    private SignatureEnergyPreservationSystem preservationSystem;

    public VaryonSignaturePreservationPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        this.config.save();
        VaryonSignaturePreservationConfig cfg = this.config.get();
        preservationSystem = new SignatureEnergyPreservationSystem(cfg);
        this.getEntityStoreRegistry().registerSystem((ISystem) preservationSystem);

        this.getEventRegistry()
                .registerGlobal(PlayerDisconnectEvent.class, event -> preservationSystem.cleanupPlayer(event.getPlayerRef().getUuid()));

        LOGGER.at(Level.INFO)
                .log(
                        "Varyon-SignaturePreservation | enabled=%s debug=%s restoreDelayMs=%s",
                        cfg.isEnabled(),
                        cfg.isDebug(),
                        cfg.getRestoreDelayMs());
    }

    @Override
    protected void shutdown() {
        if (preservationSystem != null) {
            preservationSystem.shutdownScheduler();
        }
        super.shutdown();
    }
}
