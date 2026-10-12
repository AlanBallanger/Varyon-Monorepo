package com.varyon.debugging;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Listens to every NPC/dropped-item/projectile add/remove in every world and tallies them in
 * {@link SpawnRegistry}. Built to answer "what is EntityTickingSystem/SpatialSystem actually
 * iterating over right now" - NPCs alone (33 QLD_PlayerNPC, 2 Boar on Haven) turned out not to
 * explain a 36%/15% tick cost, so this also covers dropped items and projectiles, the two other
 * entity kinds those systems iterate. See /spawnreport live.
 */
public final class SpawnTrackingSystem extends RefSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(NPCEntity.getComponentType(), ItemComponent.getComponentType(),
                ProjectileComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (reason != AddReason.SPAWN) {
            return;
        }
        String world = worldName(store);
        if (world == null) {
            return;
        }
        String type = resolveType(ref, store);
        SpawnRegistry.get().recordSpawn(world, type);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        String world = worldName(store);
        if (world == null) {
            return;
        }
        String type = resolveType(ref, store);
        SpawnRegistry.get().recordRemoval(world, type);
    }

    @Nonnull
    private static String resolveType(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) {
            return resolveNpcType(npc);
        }
        ItemComponent itemComponent = store.getComponent(ref, ItemComponent.getComponentType());
        if (itemComponent != null) {
            ItemStack stack = itemComponent.getItemStack();
            return "item:" + (stack != null ? stack.getItemId() : "unknown");
        }
        ProjectileComponent projectile = store.getComponent(ref, ProjectileComponent.getComponentType());
        if (projectile != null) {
            return "projectile";
        }
        return "unknown";
    }

    @Nullable
    private static String worldName(@Nonnull Store<EntityStore> store) {
        try {
            Object ext = store.getExternalData();
            if (ext instanceof EntityStore es && es.getWorld() != null) {
                return es.getWorld().getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nonnull
    private static String resolveNpcType(@Nullable NPCEntity npc) {
        if (npc == null) {
            return "unknown";
        }
        try {
            String typeId = npc.getNPCTypeId();
            if (typeId != null && !typeId.isBlank()) {
                return typeId;
            }
        } catch (Exception ignored) {
        }
        try {
            String role = npc.getRoleName();
            if (role != null && !role.isBlank()) {
                return role;
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
