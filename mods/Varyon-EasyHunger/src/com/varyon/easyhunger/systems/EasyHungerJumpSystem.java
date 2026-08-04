package com.varyon.easyhunger.systems;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.EasyHungerUtils;
import com.varyon.easyhunger.components.HungerComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.utils.HungerProtectionUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that decreases hunger when a player jumps.
 * Uses a cooldown to prevent draining hunger every tick while int the air.
 */
public class EasyHungerJumpSystem extends EntityTickingSystem<EntityStore> {

    // Track when players last jumped (to prevent draining every tick while in air)
    private final java.util.Map<UUID, Long> lastJumpTime = new ConcurrentHashMap<>();
    
    // Minimum time between jump hunger costs (in milliseconds)
    private static final long JUMP_COOLDOWN_MS = 500; // 0.5 seconds

    public EasyHungerJumpSystem() {
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), MovementStatesComponent.getComponentType());
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        MovementStatesComponent movementComp = chunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (movementComp == null) return;

        MovementStates states = movementComp.getMovementStates();
        if (states == null) return;

        // Check if jumping (but not swimming or flying)
        if (states.jumping && !states.swimming && !states.flying) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;
            
            // Skip hunger drain in Creative mode
            Player player = chunk.getComponent(index, Player.getComponentType());
            if (player != null && player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) return;
            
            // Skip hunger drain in safe zones
            if (HungerProtectionUtils.isSafe(playerRef)) return;
            
            UUID uuid = playerRef.getUuid();
            if (uuid == null) return;

            long currentTime = System.currentTimeMillis();
            Long lastJump = lastJumpTime.get(uuid);

            // Only drain hunger if enough time has passed since last jump
            if (lastJump == null || (currentTime - lastJump) >= JUMP_COOLDOWN_MS) {
                float jumpCost = EasyHunger.get().getConfig().getJumpHungerCost();
                if (jumpCost <= 0) return;

                HungerComponent hunger = chunk.getComponent(index, HungerComponent.getComponentType());
                if (hunger != null) {
                   float currentHunger = hunger.getHungerLevel();
                   float newHunger = Math.max(0, currentHunger - jumpCost);
                   hunger.setHungerLevel(newHunger);
                   
                   // Update HUD
                   EasyHungerUtils.setPlayerHungerLevel(chunk.getReferenceTo(index), commandBuffer, newHunger);
                   
                   lastJumpTime.put(uuid, currentTime);
                }
            }
        }
    }
}
