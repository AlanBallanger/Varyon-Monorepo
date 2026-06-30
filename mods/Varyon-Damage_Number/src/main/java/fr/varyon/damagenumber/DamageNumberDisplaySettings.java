package fr.varyon.damagenumber;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class DamageNumberDisplaySettings {

    @Nullable
    private static DamageNumberDisplaySettingsManager manager;

    private DamageNumberDisplaySettings() {}

    public static void bind(@Nonnull DamageNumberDisplaySettingsManager prefsManager) {
        manager = prefsManager;
    }

    public static void ensureLoaded(@Nonnull UUID uuid) {
        if (manager != null) {
            manager.ensureLoaded(uuid);
        }
    }

    public static boolean isEnabled(@Nullable UUID uuid) {
        if (uuid == null) {
            return true;
        }
        if (manager != null) {
            return manager.isEnabled(uuid);
        }
        return true;
    }

    public static void setEnabled(@Nullable UUID uuid, boolean enabled) {
        if (uuid == null) {
            return;
        }
        if (manager != null) {
            manager.setEnabled(uuid, enabled);
        }
    }

    public static boolean toggle(@Nullable UUID uuid) {
        if (uuid == null) {
            return true;
        }
        if (manager != null) {
            return manager.toggle(uuid);
        }
        return false;
    }

    public static boolean isViewerEnabled(Store<EntityStore> store, Ref<EntityStore> viewerRef) {
        return isViewerEnabled(store, null, viewerRef);
    }

    public static boolean isViewerEnabled(Store<EntityStore> store,
                                          @Nullable CommandBuffer<EntityStore> commandBuffer,
                                          Ref<EntityStore> viewerRef) {
        if (viewerRef == null || !viewerRef.isValid()) {
            return false;
        }
        UUID uuid = resolvePlayerUuid(store, commandBuffer, viewerRef);
        if (uuid == null) {
            return false;
        }
        return isEnabled(uuid);
    }

    @Nullable
    public static UUID resolveAttackerUuid(Store<EntityStore> store,
                                           @Nullable CommandBuffer<EntityStore> commandBuffer,
                                           @Nullable Damage damage) {
        if (damage == null) {
            return null;
        }
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }
        return resolvePlayerUuid(store, commandBuffer, attackerRef);
    }

    @Nullable
    public static UUID resolvePlayerUuid(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        return resolvePlayerUuid(store, null, entityRef);
    }

    @Nullable
    public static UUID resolvePlayerUuid(Store<EntityStore> store,
                                         @Nullable CommandBuffer<EntityStore> commandBuffer,
                                         Ref<EntityStore> entityRef) {
        if (entityRef == null || !entityRef.isValid() || store == null) {
            return null;
        }
        PlayerRef playerRef = null;
        if (commandBuffer != null) {
            playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        }
        if (playerRef == null) {
            playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        }
        if (playerRef != null) {
            return playerRef.getUuid();
        }
        return null;
    }
}
