package com.varyon.craftrestrict.events;

import com.varyon.craftrestrict.utils.PermissionsUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EventPlayerInteractBlock extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    public EventPlayerInteractBlock() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                        @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UseBlockEvent.Pre pre) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        BlockType blockType = pre.getBlockType();
        if (blockType.getBench() == null) {
            return;
        }
        boolean shouldRestrict = PermissionsUtils.shouldRestrict(playerRef, blockType.getBench().getId(), "bench");
        if (shouldRestrict) {
            PermissionsUtils.sendRestrictionNotifications(playerRef, "bench");
        }
        pre.setCancelled(shouldRestrict);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
