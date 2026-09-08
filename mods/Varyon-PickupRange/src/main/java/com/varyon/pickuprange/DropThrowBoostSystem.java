package com.varyon.pickuprange;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Scales the throw speed of items a player drops/throws by {@code DropThrowMultiplier}.
 *
 * <p>This is an {@link EntityEventSystem}: it fires only on a {@code DropItemEvent.Drop}, i.e. when a
 * player actually drops something — never on a tick. When the multiplier is {@code 1.0} it is a
 * one-comparison no-op.
 */
public final class DropThrowBoostSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {

    private final MultiplierState state;

    public DropThrowBoostSystem(MultiplierState state) {
        super(DropItemEvent.Drop.class);
        this.state = state;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull DropItemEvent.Drop event) {
        float multiplier = this.state.getDropThrowMultiplier();
        if (multiplier == 1.0f) {
            return;
        }
        float speed = event.getThrowSpeed();
        if (speed <= 0.0f || !Float.isFinite(speed)) {
            return;
        }
        event.setThrowSpeed(speed * multiplier);
    }
}
