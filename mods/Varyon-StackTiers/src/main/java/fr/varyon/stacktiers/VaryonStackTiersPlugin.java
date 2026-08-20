package fr.varyon.stacktiers;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import fr.varyon.stacktiers.bench.StackTiersBenchPageSupplier;
import fr.varyon.stacktiers.command.VaryonResearchCommand;
import fr.varyon.stacktiers.command.VaryonStackCommand;
import fr.varyon.stacktiers.research.PlayerResearchState;
import fr.varyon.stacktiers.research.ResearchManager;

import java.util.UUID;
import java.util.logging.Logger;

public class VaryonStackTiersPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("Varyon-StackTiers");

    private static VaryonStackTiersPlugin staticInstance;
    private ResearchManager researchManager;

    public VaryonStackTiersPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        staticInstance = this;

        researchManager = new ResearchManager(this.getDataDirectory());
        researchManager.loadFromDisk();
        researchManager.start();

        this.getCommandRegistry().registerCommand(new VaryonStackCommand(researchManager));
        this.getCommandRegistry().registerCommand(new VaryonResearchCommand(researchManager));

        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC)
                .register("StackTiersBench", StackTiersBenchPageSupplier.class, StackTiersBenchPageSupplier.CODEC);

        this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::onPlayerConnect);
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        UUID uuid = event.getPlayerRef().getUuid();
        PlayerResearchState state = researchManager.snapshot(uuid);
        String inProgressBefore = state.inProgressNodeId;
        boolean resolved = researchManager.resolveIfDue(uuid, state, System.currentTimeMillis());
        if (resolved && inProgressBefore != null) {
            event.getPlayerRef().sendMessage(
                    com.hypixel.hytale.server.core.Message.raw(
                            "Une recherche s'est terminée pendant votre absence !")
                            .color(java.awt.Color.GREEN));
        }
    }

    @Override
    protected void shutdown() {
        if (researchManager != null) {
            researchManager.shutdown();
        }
    }

    public static VaryonStackTiersPlugin getInstance() {
        return staticInstance;
    }

    /** Exposé pour une future UI (varyon-UI) qui voudrait lire/écrire les paliers d'un joueur. */
    public ResearchManager getResearchManager() {
        return researchManager;
    }
}
