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
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import java.util.Set;
import java.util.UUID;

/**
 * Records player damage to tracked bosses/adds as the real HP removed
 * ({@code hpBefore - hpAfter} around {@link DamageSystems.ApplyDamage}).
 */
public final class BossDamageChartRecordingSystem extends DamageEventSystem {

    private final BossTrackingSystem trackingSystem;
    private final BossDamageChartTracker tracker;
    private final Set<Dependency<EntityStore>> dependencies =
            Set.of(new SystemDependency<>(Order.AFTER, DamageSystems.ApplyDamage.class));

    public BossDamageChartRecordingSystem(BossTrackingSystem trackingSystem, BossDamageChartTracker tracker) {
        this.trackingSystem = trackingSystem;
        this.tracker = tracker;
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
        Float hpBefore = BossDamageChartHpSnapshotSystem.takeHpBefore(damage);
        if (trackingSystem == null || tracker == null || damage == null) {
            return;
        }
        if (damage.isCancelled()) {
            return;
        }

        UUID targetUuid = extractTargetUuid(index, archetypeChunk);
        if (targetUuid == null) {
            return;
        }

        UUID eventId = trackingSystem.getEventIdForTrackedEntity(targetUuid);
        if (eventId == null) {
            return;
        }

        UUID playerUuid = extractPlayerUuidFromSource(damage, store);
        if (playerUuid == null) {
            return;
        }

        double removed = resolveHpRemoved(archetypeChunk, index, damage, hpBefore);
        if (!Double.isFinite(removed) || removed <= 0.0d) {
            return;
        }
        tracker.addDamage(eventId, playerUuid, removed);
    }

    /**
     * Prefer measured HP delta; fall back to clamped damage amount if snapshot missing
     * (e.g. entity already despawned / stat map unavailable after the killing blow).
     */
    private static double resolveHpRemoved(ArchetypeChunk<EntityStore> archetypeChunk,
                                           int index,
                                           Damage damage,
                                           @Nullable Float hpBefore) {
        float amount = damage.getAmount();
        if (hpBefore != null && Float.isFinite(hpBefore)) {
            float hpAfter = readCurrentHp(archetypeChunk, index);
            if (Float.isFinite(hpAfter)) {
                double delta = (double) hpBefore - (double) hpAfter;
                if (delta > 0.0d) {
                    return delta;
                }
            }
            // Killing blow / despawn: credit remaining HP at snapshot time.
            float min = readMinHp(archetypeChunk, index);
            double remaining = (double) hpBefore - (double) min;
            if (remaining > 0.0d) {
                if (Float.isFinite(amount) && amount > 0f) {
                    return Math.min(remaining, amount);
                }
                return remaining;
            }
        }
        if (!Float.isFinite(amount) || amount <= 0f) {
            return 0.0d;
        }
        float remaining = readRemainingHp(archetypeChunk, index);
        if (Float.isFinite(remaining) && remaining >= 0f) {
            // After apply, remaining is post-hit; cannot clamp usefully — use amount.
            return amount;
        }
        return amount;
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

    private static float readMinHp(ArchetypeChunk<EntityStore> archetypeChunk, int index) {
        Object statMapObj = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (!(statMapObj instanceof EntityStatMap statMap)) {
            return 0f;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = healthIndex >= 0 ? statMap.get(healthIndex) : null;
        return health != null ? health.getMin() : 0f;
    }

    private static float readRemainingHp(ArchetypeChunk<EntityStore> archetypeChunk, int index) {
        Object statMapObj = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (!(statMapObj instanceof EntityStatMap statMap)) {
            return Float.NaN;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = healthIndex >= 0 ? statMap.get(healthIndex) : null;
        if (health == null) {
            return Float.NaN;
        }
        return Math.max(0f, health.get() - health.getMin());
    }

    private static UUID extractTargetUuid(int index, ArchetypeChunk<EntityStore> archetypeChunk) {
        Object targetUuidObj = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (targetUuidObj instanceof UUIDComponent targetUuidComp) {
            return targetUuidComp.getUuid();
        }
        return null;
    }

    @Nullable
    private static UUID extractPlayerUuidFromSource(Damage damage, Store<EntityStore> store) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        var sourceRef = entitySource.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return null;
        }
        Object playerObj = store.getComponent(sourceRef, Player.getComponentType());
        if (playerObj == null) {
            return null;
        }
        Object uuidObj = store.getComponent(sourceRef, UUIDComponent.getComponentType());
        if (uuidObj instanceof UUIDComponent uuidComp) {
            return uuidComp.getUuid();
        }
        return null;
    }
}
