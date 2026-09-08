package fr.varyon.ecotale.jobs.systems;

import fr.varyon.ecotale.coins.currency.CoinDropper;
import fr.varyon.ecotale.jobs.util.JobsLogger;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

import javax.annotation.Nonnull;

/**
 * Spawns the physical coin drops queued by {@link MobRewardSystem} on a {@link PendingCoinDrop}
 * component, but only once the NPC corpse is ready to disappear — mirroring the gate used by
 * vanilla {@code NPCDamageSystems$DropDeathItems}. This makes coins appear at the same moment as
 * the vanilla loot instead of instantly at the kill.
 */
public class DeferredCoinDropSystem extends EntityTickingSystem<EntityStore> {

    private Query<EntityStore> query;

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        if (query == null) {
            query = Query.and(
                PendingCoinDrop.getComponentType(),
                NPCEntity.getComponentType()
            );
        }
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        PendingCoinDrop pending = chunk.getComponent(index, PendingCoinDrop.getComponentType());
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

        long amount = pending.getAmount();
        commandBuffer.removeComponent(chunk.getReferenceTo(index), PendingCoinDrop.getComponentType());

        if (amount <= 0) {
            return;
        }

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        org.joml.Vector3d raw = transform.getPosition();
        org.joml.Vector3d pos = new org.joml.Vector3d(raw.x, raw.y + 0.5, raw.z);

        CoinDropper.dropCoins(store, commandBuffer, pos, amount);
        JobsLogger.debug("Deferred coin drop: %d released at corpse", amount);
    }
}
