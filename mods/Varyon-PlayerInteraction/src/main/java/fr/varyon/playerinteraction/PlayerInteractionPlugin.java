package fr.varyon.playerinteraction;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class PlayerInteractionPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String INTERACT_HINT = "Appuyez sur [{key}] pour interagir";
    private static final String ASSET_PACK = "Varyon:Varyon-PlayerInteraction";

    public PlayerInteractionPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new ShareCommand());

        Interaction.CODEC.register(
            "VaryonPlayerInteractionMenu",
            PlayerInteractionMenuInteraction.class,
            PlayerInteractionMenuInteraction.CODEC
        );
        Interaction.getAssetStore().loadAssets(
            ASSET_PACK,
            List.of(new PlayerInteractionMenuInteraction(PlayerInteractionMenuInteraction.ID))
        );
        RootInteraction.getAssetStore().loadAssets(
            ASSET_PACK,
            List.of(PlayerInteractionMenuInteraction.DEFAULT_ROOT)
        );

        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
            Holder<EntityStore> holder = event.getHolder();
            if (holder == null) return;
            holder.ensureComponent(Interactable.getComponentType());
            Interactions interactions = holder.ensureAndGetComponent(Interactions.getComponentType());
            interactions.setInteractionHint(INTERACT_HINT);
            interactions.setInteractionId(InteractionType.Use, PlayerInteractionMenuInteraction.ID);
        });

        LOGGER.atInfo().log("Varyon-PlayerInteraction loaded");
    }
}
