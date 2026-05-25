package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.component.MobScalingComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobDamageScalingSystem extends DamageEventSystem {

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        if (damage.isCancelled() || damage.getAmount() <= 0.0f) {
            return;
        }

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = ((Damage.EntitySource) source).getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        MobScalingComponent scalingComponent = store.getComponent(attackerRef, MobScalingComponent.getComponentType());
        if (scalingComponent == null) {
            return;
        }

        float damageMultiplier = scalingComponent.getDamageMultiplier();
        if (damageMultiplier != 1.0f) {
            damage.setAmount(damage.getAmount() * damageMultiplier);
        }
    }
}
