package com.varyon.portal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArenasPortalPreInteractionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final String ARENAS_PORTAL_BLOCK_ID = "Varyon_Portal_Arenas";

    public ArenasPortalPreInteractionSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull UseBlockEvent.Pre event) {

        BlockType blockType = event.getBlockType();
        if (blockType == null || blockType.getId() == null) return;
        if (!blockType.getId().contains(ARENAS_PORTAL_BLOCK_ID)) return;

        cancelInteractionChain(event.getContext());
    }

    private void cancelInteractionChain(@Nullable InteractionContext context) {
        if (context == null) return;
        try {
            InteractionChain chain = context.getChain();
            InteractionManager manager = context.getInteractionManager();
            if (chain != null && manager != null) {
                manager.cancelChains(chain);
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
