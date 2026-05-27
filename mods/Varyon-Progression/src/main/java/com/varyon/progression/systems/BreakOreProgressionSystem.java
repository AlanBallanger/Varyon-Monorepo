package com.varyon.progression.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.systems.BlackboardSystems;
import com.varyon.progression.config.OreRegistry;
import org.jetbrains.annotations.NotNull;

public class BreakOreProgressionSystem extends BlackboardSystems.BreakBlockEventSystem {

    @Override
    public void handle(int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull BreakBlockEvent event) {
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

        var cds = event.getTargetBlock();
        var slices = event.getBlockType().getId().split("_");
        var rockType = slices[slices.length - 1];

        event.setCancelled(true);
        player.getWorld().setBlock(cds.x, cds.y, cds.z, "Empty");

        ItemStack rockItem = new ItemStack("Rock_" + rockType, 1);

        player.getWorld().execute(() -> {
            var holder = ItemComponent.generateItemDrop(store, rockItem, new Vector3d(cds.x + 0.5, cds.y + 0.5, cds.z + 0.5), Rotation3f.ZERO, 0, 1, 0);
            if (holder != null) {
                store.addEntity(holder, AddReason.SPAWN);
            }
        });
    }
}
