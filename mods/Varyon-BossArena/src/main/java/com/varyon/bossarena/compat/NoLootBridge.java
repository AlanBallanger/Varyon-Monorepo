package com.varyon.bossarena.compat;

import com.varyon.component.NoLootComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Hard reference to Varyon's NoLootComponent type. Loaded only when {@link NoLoot} confirms
 * Varyon is present.
 */
final class NoLootBridge {

    private NoLootBridge() {}

    static void mark(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (NoLootComponent.getComponentType() == null) {
            return;
        }
        if (store.getComponent(entityRef, NoLootComponent.getComponentType()) != null) {
            return;
        }
        store.addComponent(entityRef, NoLootComponent.getComponentType(), new NoLootComponent());
    }
}
