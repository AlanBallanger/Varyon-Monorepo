package fr.varyon.death.compat;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.component.MobScalingComponent;

/**
 * References dures vers les types Varyon. Chargee uniquement lorsque {@link VaryonMobLevel}
 * a confirme la presence du plugin.
 */
final class VaryonMobLevelBridge {

    private VaryonMobLevelBridge() {}

    static int read(@Nullable Store<EntityStore> store,
                    @Nullable CommandBuffer<EntityStore> commandBuffer,
                    Ref<EntityStore> entityRef) {
        ComponentType<EntityStore, MobScalingComponent> type = MobScalingComponent.getComponentType();
        if (type == null) {
            return VaryonMobLevel.NO_LEVEL;
        }
        MobScalingComponent scaling = null;
        if (commandBuffer != null) {
            scaling = commandBuffer.getComponent(entityRef, type);
        }
        if (scaling == null && store != null) {
            scaling = store.getComponent(entityRef, type);
        }
        if (scaling == null) {
            return VaryonMobLevel.NO_LEVEL;
        }
        return Math.max(VaryonMobLevel.NO_LEVEL, scaling.getMobLevel());
    }
}
