package com.varyon.tptoworld.portal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class PortalTemplateBreakCleanupListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public PortalTemplateBreakCleanupListener() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {
        try {
            if (event.getBlockType() == null) {
                return;
            }
            String blockId = event.getBlockType().getId();
            boolean isTemplateBlock = VaryonPortalConfig.TEMPLATE_ITEM_ID.equals(blockId)
                    || VaryonPortalConfig.TEMPLATE_X2_ITEM_ID.equals(blockId);
            if (!isTemplateBlock) {
                return;
            }
            Vector3i pos = event.getTargetBlock();
            if (pos == null) {
                return;
            }
            String world = resolveWorld(store);
            PortalTemplateRegistry.getInstance().remove(world, pos.x, pos.y, pos.z);
        } catch (Exception ignored) {
        }
    }

    private String resolveWorld(@Nonnull Store<EntityStore> store) {
        try {
            if (store.getExternalData() != null && ((EntityStore) store.getExternalData()).getWorld() != null) {
                return ((EntityStore) store.getExternalData()).getWorld().getName();
            }
        } catch (Exception ignored) {
        }
        return "world";
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
