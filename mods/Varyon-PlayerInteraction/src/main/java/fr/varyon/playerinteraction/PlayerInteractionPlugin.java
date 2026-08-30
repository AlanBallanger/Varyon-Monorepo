package fr.varyon.playerinteraction;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerInteractionPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String INTERACT_HINT_KEY = "server.interactionHints.playerInteract";

    public PlayerInteractionPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new ShareCommand());

        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
            Holder<EntityStore> holder = event.getHolder();
            if (holder == null) return;
            holder.ensureComponent(Interactable.getComponentType());
            Interactions interactions = holder.ensureAndGetComponent(Interactions.getComponentType());
            interactions.setInteractionHint(INTERACT_HINT_KEY);
            // Deliberately NOT calling interactions.setInteractionId(InteractionType.Use, ...) here:
            // doing so on the player's OWN Interactions component broke that player's ability to
            // interact with anything else (chests, gathering, etc.) — the engine also consults this
            // slot for "what does this player's own Use key do", not only "what happens when someone
            // targets this player". The F-to-interact menu is instead driven passively by watching the
            // SyncInteractionChains packet in PlayerInteractionPacketListener, which never touches any
            // entity's Interactions component.
        });

        PlayerInteractionPacketListener.register();

        LOGGER.atInfo().log("Varyon-PlayerInteraction loaded");
    }
}
