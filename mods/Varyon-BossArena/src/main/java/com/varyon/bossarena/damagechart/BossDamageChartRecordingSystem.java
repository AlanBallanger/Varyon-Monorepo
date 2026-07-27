package com.varyon.bossarena.damagechart;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.varyon.bossarena.system.BossTrackingSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Records player damage to tracked bosses/adds as the real HP removed
 * ({@code hpBefore - hpAfter} around ApplyDamage).
 */
public final class BossDamageChartRecordingSystem extends DamageEventSystem {

    private final BossTrackingSystem trackingSystem;
    private final BossDamageChartTracker tracker;

    public BossDamageChartRecordingSystem(BossTrackingSystem trackingSystem, BossDamageChartTracker tracker) {
        this.trackingSystem = trackingSystem;
        this.tracker = tracker;
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        // Inspect runs after ApplyDamage. Staying in Filter + AFTER ApplyDamage creates a cycle.
        return DamageModule.get().getInspectDamageGroup();
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

        // This hit is fatal either when the stat map became unreadable (entity already removed
        // from the store by this hit — despawn/killing-blow branch of resolveHpRemoved), or when
        // the stat map is still readable but HP dropped to/below its minimum (the common case: the
        // entity is dying but hasn't been removed from the store yet this tick). Attribute the kill
        // to whoever dealt this hit.
        boolean fatal;
        if (hpBefore != null && Float.isFinite(hpBefore)) {
            float hpAfter = readCurrentHp(archetypeChunk, index);
            if (Float.isFinite(hpAfter)) {
                float min = readMinHp(archetypeChunk, index);
                fatal = hpAfter <= min;
            } else {
                fatal = true;
            }
        } else {
            fatal = false;
        }
        if (fatal) {
            trackingSystem.recordMobKillIfAdd(targetUuid, eventId, playerUuid);
        }
    }

    /**
     * Prefer measured HP delta; only fall back to the raw damage amount when the entity is
     * confirmed gone (killing blow / despawn — stat map unreadable after removal). A parried,
     * blocked, or knockback-only hit leaves HP unchanged and a readable stat map, and must credit
     * zero rather than the theoretical weapon damage — crediting {@code amount} in that case
     * inflates the damage chart with hits that dealt no real HP loss.
     */
    private static double resolveHpRemoved(ArchetypeChunk<EntityStore> archetypeChunk,
                                           int index,
                                           Damage damage,
                                           @Nullable Float hpBefore) {
        float amount = damage.getAmount();
        if (hpBefore != null && Float.isFinite(hpBefore)) {
            float hpAfter = readCurrentHp(archetypeChunk, index);
            if (Float.isFinite(hpAfter)) {
                // Stat map still readable: trust the measured delta, including zero
                // (parried/blocked/knockback-only hits). Never fall back to amount here.
                double delta = (double) hpBefore - (double) hpAfter;
                return Math.max(0.0d, delta);
            }
            // Stat map unreadable: entity was actually removed by this hit (killing blow/despawn).
            float min = readMinHp(archetypeChunk, index);
            double remaining = (double) hpBefore - (double) min;
            if (remaining > 0.0d) {
                if (Float.isFinite(amount) && amount > 0f) {
                    return Math.min(remaining, amount);
                }
                return remaining;
            }
            return 0.0d;
        }
        // No pre-hit snapshot at all: nothing to compare against, so don't guess from amount.
        return 0.0d;
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
