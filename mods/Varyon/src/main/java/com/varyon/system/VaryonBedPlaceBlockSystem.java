package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.util.VaryonWorldAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class VaryonBedPlaceBlockSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public VaryonBedPlaceBlockSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String worldName = resolveWorld(store);
        if (!VaryonWorldAccess.isVaryonEnabledWorld(worldName)) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item == null) {
            return;
        }
        String itemId = item.getItemId();
        if (!isBedItem(itemId)) {
            return;
        }

        event.setCancelled(true);
        Ref ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw("Vous ne pouvez pas placer de lit dans ce monde.").color(Color.RED));
        }
    }

    private static String resolveWorld(@Nonnull Store<EntityStore> store) {
        try {
            if (store.getExternalData() != null && ((EntityStore) store.getExternalData()).getWorld() != null) {
                return ((EntityStore) store.getExternalData()).getWorld().getName();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static boolean isBedItem(@Nullable String id) {
        if (id == null) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("bedrock")) {
            return false;
        }
        return lower.contains("bed");
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
