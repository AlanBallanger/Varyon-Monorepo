package fr.varyon.stacktiers;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import fr.varyon.stacktiers.command.VaryonResearchCommand;
import fr.varyon.stacktiers.command.VaryonStackCommand;
import fr.varyon.stacktiers.research.PlayerResearchState;
import fr.varyon.stacktiers.research.ResearchManager;

import java.util.UUID;
import java.util.logging.Logger;

public class VaryonStackTiersPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("Varyon-StackTiers");

    private static VaryonStackTiersPlugin staticInstance;
    private PlayerTierStore tierStore;
    private ResearchManager researchManager;

    public VaryonStackTiersPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        staticInstance = this;
        tierStore = new PlayerTierStore(this.getDataDirectory());
        tierStore.loadFromDisk();

        researchManager = new ResearchManager(tierStore, this.getDataDirectory());
        researchManager.loadFromDisk();
        researchManager.start();

        this.getCommandRegistry().registerCommand(new VaryonStackCommand(tierStore, researchManager));
        this.getCommandRegistry().registerCommand(new VaryonResearchCommand(researchManager));

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
        // tierStore persiste déjà au fil de l'eau (set/remove) ; researchManager doit
        // flusher son état + arrêter son exécuteur planifié.
        if (researchManager != null) {
            researchManager.shutdown();
        }
    }

    public static VaryonStackTiersPlugin getInstance() {
        return staticInstance;
    }

    /** Exposé pour une future UI (varyon-UI) qui voudrait lire/écrire les paliers d'un joueur. */
    public PlayerTierStore getTierStore() {
        return tierStore;
    }

    public ResearchManager getResearchManager() {
        return researchManager;
    }
}
