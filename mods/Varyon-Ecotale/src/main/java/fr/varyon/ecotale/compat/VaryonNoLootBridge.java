package fr.varyon.ecotale.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.component.NoLootComponent;

/**
 * Hard reference to Varyon's NoLootComponent type. Loaded only once {@link VaryonNoLoot}
 * confirms the plugin is present.
 */
final class VaryonNoLootBridge {

    private VaryonNoLootBridge() {}

    static boolean isMarked(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (NoLootComponent.getComponentType() == null) {
            return false;
        }
        return store.getComponent(entityRef, NoLootComponent.getComponentType()) != null;
    }
}
