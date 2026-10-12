package com.varyon.elite.combat;

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
import com.varyon.elite.EliteStats;
import com.varyon.elite.component.EliteComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Scales up damage an elite deals, with an extra multiplier while it is enraged. */
public class EliteDamageDealtSystem extends DamageEventSystem {

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
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled() || damage.getAmount() <= 0f) {
            return;
        }
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }
        EliteComponent elite = store.getComponent(attackerRef, EliteComponent.getComponentType());
        if (elite == null) {
            return;
        }
        float mult = elite.getDamageMultiplier();
        if (elite.isEnraged()) {
            mult *= EliteStats.enrageBonusMultiplier();
        }
        if (elite.getStatBonus() > 0) {
            mult *= (float) (1.0 + elite.getStatBonus());
        }
        damage.setAmount(damage.getAmount() * mult);
    }
}
