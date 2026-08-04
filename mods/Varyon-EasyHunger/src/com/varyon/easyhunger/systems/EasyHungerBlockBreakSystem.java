package com.varyon.easyhunger.systems;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.EasyHungerUtils;
import com.varyon.easyhunger.components.HungerComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.utils.HungerProtectionUtils;

import javax.annotation.Nonnull;

public class EasyHungerBlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public EasyHungerBlockBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        
        HungerComponent hunger = chunk.getComponent(index, HungerComponent.getComponentType());
        if (hunger == null) return;

        // Skip hunger drain in Creative mode
        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player != null && player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) return;

        // Skip hunger drain in safe zones
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef != null && HungerProtectionUtils.isSafe(playerRef)) return;

        float blockBreakCost = EasyHunger.get().getConfig().getBlockBreakHungerCost();
        if (blockBreakCost <= 0) return;

        float currentHunger = hunger.getHungerLevel();
        float newHunger = Math.max(0, currentHunger - blockBreakCost);

        hunger.setHungerLevel(newHunger);
        
        // Update HUD
        if (playerRef != null) {
            EasyHungerUtils.setPlayerHungerLevel(chunk.getReferenceTo(index), commandBuffer, newHunger);
        }
    }
}
