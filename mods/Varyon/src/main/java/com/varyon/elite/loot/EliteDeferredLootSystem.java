package com.varyon.elite.loot;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.varyon.elite.component.ElitePendingLootComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Spawns the currency items queued by {@link EliteLootSystem} once the elite corpse is ready
 * to disappear - same gate as vanilla {@code NPCDamageSystems$DropDeathItems}.
 */
public class EliteDeferredLootSystem extends EntityTickingSystem<EntityStore> {

    public static final String VOID_CRYSTAL_ITEM = "Varyon_Elite_VoidCrystal";
    public static final String CORRUPTION_CORE_ITEM = "Varyon_Elite_CorruptionHeart";

    private Query<EntityStore> query;

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        if (query == null) {
            query = Query.and(ElitePendingLootComponent.getComponentType(), NPCEntity.getComponentType());
        }
        return query;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        ElitePendingLootComponent pending = chunk.getComponent(index, ElitePendingLootComponent.getComponentType());
        if (pending == null) {
            return;
        }

        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        Role role = npc != null ? npc.getRole() : null;
        boolean instant = role != null && role.isDropDeathItemsInstantly();
        DeferredCorpseRemoval corpse = chunk.getComponent(index, DeferredCorpseRemoval.getComponentType());
        if (!instant && (corpse == null || !corpse.shouldRemove())) {
            return;
        }

        commandBuffer.removeComponent(chunk.getReferenceTo(index), ElitePendingLootComponent.getComponentType());

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        org.joml.Vector3d pos = new org.joml.Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);
        HeadRotation headRotation = chunk.getComponent(index, HeadRotation.getComponentType());
        com.hypixel.hytale.math.vector.Rotation3fc rot = headRotation != null
                ? headRotation.getRotation()
                : com.hypixel.hytale.math.vector.Rotation3f.ZERO;

        List<ItemStack> stacks = new ArrayList<>();
        if (pending.getVoidCrystals() > 0) {
            stacks.add(new ItemStack(VOID_CRYSTAL_ITEM, pending.getVoidCrystals()));
        }
        if (pending.getCorruptionCores() > 0) {
            stacks.add(new ItemStack(CORRUPTION_CORE_ITEM, pending.getCorruptionCores()));
        }
        if (stacks.isEmpty()) {
            return;
        }

        Holder[] drops = ItemComponent.generateItemDrops(store, stacks, pos, rot);
        commandBuffer.addEntities(drops, AddReason.SPAWN);
    }
}
