package com.varyon.easyhunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.components.HungerComponent;

public class EasyHungerPlayerReady {
    public static void handle(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            // Trigger recipe pruning (will only run if needed based on config)
            com.varyon.easyhunger.EasyHunger.get().pruneRecipes();

            HungerComponent hungerComponent = store.ensureAndGetComponent(ref, HungerComponent.getComponentType());
            float hungerLevel = hungerComponent.getHungerLevel();

            boolean thirstEnabled = com.varyon.easyhunger.EasyHunger.get().getConfig().isThirstEnabled();
            float thirstLevel = 0.0f;
            if (thirstEnabled) {
                com.varyon.easyhunger.components.ThirstComponent thirstComponent = store.ensureAndGetComponent(ref, com.varyon.easyhunger.components.ThirstComponent.getComponentType());
                thirstLevel = thirstComponent.getThirstLevel();
            }

            com.varyon.easyhunger.ui.EasyCombinedHud combinedHud = new com.varyon.easyhunger.ui.EasyCombinedHud(
                playerRef, player.getGameMode(), hungerLevel, thirstLevel, thirstEnabled
            );
            player.getHudManager().addCustomHud(playerRef, combinedHud);
        });
    }
}


