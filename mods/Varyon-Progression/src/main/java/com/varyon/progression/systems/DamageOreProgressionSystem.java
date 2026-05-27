package com.varyon.progression.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.systems.BlackboardSystems;
import com.varyon.progression.config.OreRegistry;
import org.jetbrains.annotations.NotNull;

public class DamageOreProgressionSystem extends BlackboardSystems.DamageBlockEventSystem {

    @Override
    public void handle(int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull DamageBlockEvent event) {
        super.handle(index, archetypeChunk, store, commandBuffer, event);

        var ref = archetypeChunk.getReferenceTo(index);
        var player = store.getComponent(ref, Player.getComponentType());

        if (player == null || player.getGameMode() == GameMode.Creative) {
            return;
        }

        int requiredLevel = OreRegistry.getOreLevel(event.getBlockType().getId());
        int handLevel = OreRegistry.getPickaxeLevel(event.getItemInHand());

        if (handLevel >= requiredLevel) {
            return;
        }

        event.setDamage(event.getDamage() / 8);
    }
}
