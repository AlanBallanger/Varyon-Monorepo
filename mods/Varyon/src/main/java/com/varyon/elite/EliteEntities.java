package com.varyon.elite;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** Small shared accessors for elite systems. */
public final class EliteEntities {

    private EliteEntities() {}

    @Nullable
    public static UUID uuid(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        try {
            UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
            return uc != null ? uc.getUuid() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String worldName(@Nonnull Store<EntityStore> store) {
        try {
            Object ext = store.getExternalData();
            if (ext instanceof EntityStore es && es.getWorld() != null) {
                return es.getWorld().getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
