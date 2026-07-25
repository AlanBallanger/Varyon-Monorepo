package com.varyon.bossarena.damagechart;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.varyon.bossarena.system.BossTrackingSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Snapshots target HP before {@link DamageSystems.ApplyDamage} so the recording system
 * can credit the real HP removed.
 */
public final class BossDamageChartHpSnapshotSystem extends DamageEventSystem {

    private static final Map<Damage, Float> HP_BEFORE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final BossTrackingSystem trackingSystem;
    private final Set<Dependency<EntityStore>> dependencies =
            Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class));

    public BossDamageChartHpSnapshotSystem(BossTrackingSystem trackingSystem) {
        this.trackingSystem = trackingSystem;
    }

    static void putHpBefore(Damage damage, float hp) {
        if (damage != null && Float.isFinite(hp)) {
            HP_BEFORE.put(damage, hp);
        }
    }

    @Nullable
    static Float takeHpBefore(Damage damage) {
        if (damage == null) {
            return null;
        }
        return HP_BEFORE.remove(damage);
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and(AllLegacyLivingEntityTypesQuery.INSTANCE, UUIDComponent.getComponentType());
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (trackingSystem == null || damage == null || damage.isCancelled()) {
            return;
        }
        UUID targetUuid = extractTargetUuid(index, archetypeChunk);
        if (targetUuid == null || trackingSystem.getEventIdForTrackedEntity(targetUuid) == null) {
            return;
        }
        float hp = readCurrentHp(archetypeChunk, index);
        if (Float.isFinite(hp)) {
            putHpBefore(damage, hp);
        }
    }

    private static float readCurrentHp(ArchetypeChunk<EntityStore> archetypeChunk, int index) {
        Object statMapObj = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (!(statMapObj instanceof EntityStatMap statMap)) {
            return Float.NaN;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = healthIndex >= 0 ? statMap.get(healthIndex) : null;
        return health != null ? health.get() : Float.NaN;
    }

    private static UUID extractTargetUuid(int index, ArchetypeChunk<EntityStore> archetypeChunk) {
        Object targetUuidObj = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (targetUuidObj instanceof UUIDComponent targetUuidComp) {
            return targetUuidComp.getUuid();
        }
        return null;
    }
}
