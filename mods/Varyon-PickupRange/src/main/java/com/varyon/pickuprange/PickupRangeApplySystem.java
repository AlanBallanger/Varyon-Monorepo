package com.varyon.pickuprange;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerItemEntityPickupSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Applies the pickup-radius multiplier <b>once</b>, when an {@link ItemComponent} is attached to an
 * entity (i.e. an item is dropped or a chunk containing one loads). This replaces the original mod's
 * per-tick {@code EntityTickingSystem}, which re-did a reflective {@code set(-1)} / recompute /
 * {@code set(x)} round-trip on every ground item on every tick — the source of the CPU load and tick
 * latency.
 *
 * <p>Here the reflective write happens at most twice in the item's lifetime: on add, and again if the
 * stack is replaced ({@code onComponentSet}) and the value looks un-boosted. When the multiplier is
 * {@code 1.0} (vanilla) we do nothing at all.
 */
public final class PickupRangeApplySystem extends RefChangeSystem<EntityStore, ItemComponent> {

    private final MultiplierState state;

    public PickupRangeApplySystem(MultiplierState state) {
        this.state = state;
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, ItemComponent> componentType() {
        return ItemComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(ItemComponent.getComponentType());
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        // Make sure our value is in place before the vanilla pickup check reads it.
        return Set.of(new SystemDependency<>(Order.BEFORE, PlayerItemEntityPickupSystem.class, OrderPriority.CLOSEST));
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull ItemComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        applyIfNeeded(component, store);
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref,
                               @Nonnull ItemComponent oldComponent,
                               @Nonnull ItemComponent newComponent,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // A stack swap/merge can hand us a component whose cache was reset to the vanilla value.
        applyIfNeeded(newComponent, store);
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull ItemComponent component,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // nothing to clean up
    }

    private void applyIfNeeded(ItemComponent component, Store<EntityStore> store) {
        float multiplier = this.state.getPickupMultiplier();
        if (multiplier <= 1.0f || !PickupRangeField.available()) {
            return;
        }

        // Cheap guard against re-boosting an already-boosted component: if the cached value is
        // already clearly above what a single vanilla radius could be, assume we did it.
        float current = PickupRangeField.readRaw(component);
        float vanillaResolved = component.getPickupRadius(store); // resolves + caches if it was -1
        if (Float.isFinite(current) && current > vanillaResolved + 0.001f) {
            return;
        }

        PickupRangeField.applyMultiplier(component, store, multiplier);
    }
}
