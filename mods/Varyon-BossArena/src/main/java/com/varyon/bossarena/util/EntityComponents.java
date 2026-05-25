package com.varyon.bossarena.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

public final class EntityComponents {
    private EntityComponents() {}

    @Nullable
    public static UUID uuid(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        UUIDComponent c = store.getComponent(ref, UUIDComponent.getComponentType());
        return c != null ? c.getUuid() : null;
    }
}
