package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.varyon.component.MobScalingComponent;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * System that spawns additional loot drops for scaled NPCs based on their loot multiplier.
 * This system runs AFTER the vanilla DropDeathItems system to spawn extra copies of loot.
 *
 * If lootMultiplier is 5.0, this will spawn 4 additional sets of drops (total 5x vanilla).
 */
public class MobLootScalingSystem extends DeathSystems.OnDeathSystem {
    // Query is lazily initialized because component types may not be available during static init
    // We use MobScalingComponent as the primary query and filter for NPCs in onComponentAdded
    private Query<EntityStore> query;

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        if (query == null) {
            // Only query for MobScalingComponent - filter for NPCs and non-Players in onComponentAdded
            query = MobScalingComponent.getComponentType();
        }
        return query;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
                                  @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Skip players - only process NPCs
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player != null) {
            return;
        }

        // Only process if items loss mode is ALL (same check as vanilla DropDeathItems)
        if (component.getItemsLossMode() != DeathConfig.ItemsLossMode.ALL) {
            return;
        }

        // Get the scaling component
        MobScalingComponent scalingComponent = commandBuffer.getComponent(ref, MobScalingComponent.getComponentType());
        if (scalingComponent == null) {
            return;
        }

        float lootMultiplier = scalingComponent.getLootMultiplier();
        // Only spawn extra items if multiplier > 1
        if (lootMultiplier <= 1.0f) {
            return;
        }

        // Get NPC and role for drop list
        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null) {
            return;
        }

        Role role = npcComponent.getRole();
        if (role == null) {
            return;
        }

        String dropListId = role.getDropListId();
        ItemModule itemModule = ItemModule.get();
        if (dropListId == null || !itemModule.isEnabled()) {
            return;
        }

        // Get position for spawning items
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return;
        }

        HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
        if (headRotationComponent == null) {
            return;
        }

        Vector3d position = transformComponent.getPosition();
        Vector3f headRotation = headRotationComponent.getRotation();
        Vector3d dropPosition = position.clone().add(0.0, 1.0, 0.0);

        // Calculate how many extra sets of items to spawn
        // If lootMultiplier is 5.0, we spawn 4 extra sets (vanilla already spawned 1)
        int extraDropSets = (int) Math.floor(lootMultiplier) - 1;

        // Handle fractional part with probability
        float fractionalPart = lootMultiplier - (float) Math.floor(lootMultiplier);
        if (fractionalPart > 0 && Math.random() < fractionalPart) {
            extraDropSets++;
        }

        // Spawn extra item sets
        for (int i = 0; i < extraDropSets; i++) {
            List<ItemStack> randomItems = itemModule.getRandomItemDrops(dropListId);
            if (!randomItems.isEmpty()) {
                // Add slight position offset for each set to avoid stacking
                Vector3d offsetPosition = dropPosition.clone().add(
                        (Math.random() - 0.5) * 0.5,
                        0.1 * i,
                        (Math.random() - 0.5) * 0.5
                );

                Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(
                        store,
                        new ObjectArrayList<>(randomItems),
                        offsetPosition,
                        headRotation.clone()
                );
                commandBuffer.addEntities(drops, AddReason.SPAWN);
            }
        }
    }
}
