package com.varyon.craftrestrict.events;

import com.varyon.craftrestrict.inventory.PossessionEnforcer;
import com.varyon.craftrestrict.inventory.PossessionInventoryListenerRegistry;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EventPlayerJoinWorld {

    private EventPlayerJoinWorld() {
    }

    public static void handle(AddPlayerToWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        if (holder == null) {
            return;
        }
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        PossessionInventoryListenerRegistry.registerForPlayer(holder, playerRef);
        PossessionEnforcer.enforceForPlayer(holder, playerRef);
    }

    public static void handleDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        PossessionInventoryListenerRegistry.forgetPlayer(playerRef.getUuid());
    }
}
