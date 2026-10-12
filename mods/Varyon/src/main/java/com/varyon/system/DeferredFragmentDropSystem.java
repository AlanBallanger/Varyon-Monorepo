package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.varyon.component.PendingFragmentDrop;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;

/**
 * Spawns the key-fragment items queued by {@code MobFragmentDropSystem.DropOnDeath} on a
 * {@link PendingFragmentDrop} component, but only once the NPC corpse is ready to disappear —
 * mirroring the gate used by vanilla {@code NPCDamageSystems$DropDeathItems}. Fragments then
 * appear at the same moment as the vanilla loot instead of instantly at the kill.
 */
public class DeferredFragmentDropSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Query<EntityStore> query;

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        if (query == null) {
            query = Query.and(
                PendingFragmentDrop.getComponentType(),
                NPCEntity.getComponentType()
            );
        }
        return query;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        PendingFragmentDrop pending = chunk.getComponent(index, PendingFragmentDrop.getComponentType());
        if (pending == null) {
            return;
        }

        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        Role role = npc != null ? npc.getRole() : null;

        // Same gate as vanilla DropDeathItems: wait for the corpse to be ready to vanish,
        // unless the role is configured to drop instantly.
        boolean instant = role != null && role.isDropDeathItemsInstantly();
        DeferredCorpseRemoval corpse = chunk.getComponent(index, DeferredCorpseRemoval.getComponentType());
        if (!instant && (corpse == null || !corpse.shouldRemove())) {
            return;
        }

        String itemId = pending.getItemId();
        int quantity = pending.getQuantity();
        commandBuffer.removeComponent(chunk.getReferenceTo(index), PendingFragmentDrop.getComponentType());

        if (itemId == null || itemId.isBlank() || quantity <= 0) {
            return;
        }

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        org.joml.Vector3d pos = new org.joml.Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);
        HeadRotation headRotation = chunk.getComponent(index, HeadRotation.getComponentType());
        com.hypixel.hytale.math.vector.Rotation3fc rot = headRotation != null
            ? headRotation.getRotation()
            : com.hypixel.hytale.math.vector.Rotation3f.ZERO;

        Holder[] itemEntities = ItemComponent.generateItemDrops(store, List.of(new ItemStack(itemId, quantity)), pos, rot);
        commandBuffer.addEntities(itemEntities, AddReason.SPAWN);
        LOGGER.at(Level.FINE).log("Deferred fragment drop: " + quantity + "x " + itemId + " released at corpse");
    }
}
