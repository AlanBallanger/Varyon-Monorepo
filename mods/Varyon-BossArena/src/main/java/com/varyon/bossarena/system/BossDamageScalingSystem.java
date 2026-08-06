package com.varyon.bossarena.system;

import com.varyon.bossarena.boss.BossModifiers;
import com.varyon.bossarena.compat.ZoneTier;
import com.varyon.bossarena.config.BossArenaConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossDamageScalingSystem extends DamageEventSystem {
    private static final float EPSILON = 0.0001f;
    /** Prune the overlevel cache once more bosses than this have entries. */
    private static final int MAX_CACHED_BOSSES = 32;

    private final BossTrackingSystem trackingSystem;
    /** boss UUID -> attacker UUID -> damage multiplier. Fixed for the boss's lifetime. */
    private final Map<UUID, Map<UUID, Float>> overlevelMultiplierCache = new ConcurrentHashMap<>();

    public BossDamageScalingSystem(BossTrackingSystem trackingSystem) {
        this.trackingSystem = trackingSystem;
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
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
        if (trackingSystem == null || damage == null) {
            return;
        }

        UUID targetUuid = extractTargetUuid(index, archetypeChunk);
        // HP-% wave shield: cancel all damage to the boss until wave adds are dead.
        if (targetUuid != null
                && trackingSystem.isTracked(targetUuid)
                && trackingSystem.isBossDamageLockedByHpWave(targetUuid)) {
            damage.setCancelled(true);
            damage.setAmount(0.0f);
            return;
        }

        BossModifiers targetMods = trackingSystem.getEntityModifiers(targetUuid);
        BossModifiers sourceMods = extractSourceModifiers(damage, store);

        if (sourceMods != null) {
            float current = Math.max(0.0f, damage.getAmount());
            float scaled = current * clampMultiplier(sourceMods.damageMultiplier());
            if (Float.isFinite(scaled)) {
                damage.setAmount(Math.max(0.0f, scaled));
            }
        }

        applyOverlevelPenalty(targetUuid, damage, store);

        KnockbackComponent knockback = damage.getIfPresentMetaObject(Damage.KNOCKBACK_COMPONENT);
        if (knockback == null) {
            return;
        }

        if (sourceMods != null) {
            float knockbackGiven = clampMultiplier(sourceMods.knockbackGivenMultiplier());
            if (Math.abs(knockbackGiven - 1.0f) > EPSILON) {
                knockback.addModifier(knockbackGiven);
            }
        }

        if (targetMods != null) {
            float knockbackTaken = clampMultiplier(targetMods.knockbackTakenMultiplier());
            if (Math.abs(knockbackTaken - 1.0f) > EPSILON) {
                knockback.addModifier(knockbackTaken);
            }
        }
    }

    /**
     * Reduces damage dealt by a player whose unlocked Varyon tier is above the boss zone's tier,
     * so farming low-tier world bosses with a high-tier character is less rewarding.
     */
    private void applyOverlevelPenalty(UUID targetUuid, Damage damage, Store<EntityStore> store) {
        if (targetUuid == null) {
            return;
        }
        BossTrackingSystem.BossData bossData = trackingSystem.getBossData(targetUuid);
        if (bossData == null || bossData.varyonZoneId <= 0 || bossData.world == null) {
            return;
        }
        UUID attackerUuid = extractSourceUuid(damage, store);
        if (attackerUuid == null) {
            return;
        }
        float multiplier = overlevelMultiplierFor(attackerUuid, targetUuid, bossData);
        if (multiplier >= 1.0f) {
            return;
        }
        float current = Math.max(0.0f, damage.getAmount());
        float scaled = current * multiplier;
        if (Float.isFinite(scaled)) {
            damage.setAmount(Math.max(0.0f, scaled));
        }
    }

    /**
     * Multiplier for this attacker against this boss, resolved once per (boss, player) pair.
     *
     * <p>Neither the player's unlocked tier nor the boss zone's tier can change while the boss is
     * alive, so the LuckPerms lookup and the player scan happen on the first hit only. Entries are
     * dropped when the boss is no longer tracked.
     */
    private float overlevelMultiplierFor(UUID attackerUuid, UUID bossUuid, BossTrackingSystem.BossData bossData) {
        Map<UUID, Float> perPlayer = overlevelMultiplierCache
                .computeIfAbsent(bossUuid, key -> new ConcurrentHashMap<>());
        Float cached = perPlayer.get(attackerUuid);
        if (cached != null) {
            return cached;
        }
        PlayerRef attacker = findPlayer(bossData.world, attackerUuid);
        // Not a player (pet, projectile owner, another mob): no penalty, and cache that too.
        float multiplier = attacker == null
                ? 1.0f
                : BossArenaConfig.overlevelDamageMultiplier(
                        ZoneTier.unlockedTier(attacker) - bossData.varyonZoneId);
        perPlayer.put(attackerUuid, multiplier);
        pruneStaleCacheEntries();
        return multiplier;
    }

    /** Drops cached multipliers for bosses that are no longer tracked, so the map cannot grow unbounded. */
    private void pruneStaleCacheEntries() {
        if (overlevelMultiplierCache.size() <= MAX_CACHED_BOSSES) {
            return;
        }
        overlevelMultiplierCache.keySet().removeIf(bossUuid -> !trackingSystem.isTracked(bossUuid));
    }

    private static PlayerRef findPlayer(World world, UUID playerUuid) {
        if (world == null) {
            return null;
        }
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef != null && playerRef.isValid() && playerUuid.equals(playerRef.getUuid())) {
                return playerRef;
            }
        }
        return null;
    }

    /** UUID of the damage source entity, or null when the source is not an entity. */
    private static UUID extractSourceUuid(Damage damage, Store<EntityStore> store) {
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        var sourceRef = entitySource.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return null;
        }
        Object sourceUuidObj = store.getComponent(sourceRef, UUIDComponent.getComponentType());
        if (sourceUuidObj instanceof UUIDComponent sourceUuidComp) {
            return sourceUuidComp.getUuid();
        }
        return null;
    }

    private static UUID extractTargetUuid(int index, ArchetypeChunk<EntityStore> archetypeChunk) {
        Object targetUuidObj = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (targetUuidObj instanceof UUIDComponent targetUuidComp) {
            return targetUuidComp.getUuid();
        }
        return null;
    }

    private BossModifiers extractSourceModifiers(Damage damage, Store<EntityStore> store) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        var sourceRef = entitySource.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return null;
        }

        Object sourceUuidObj = store.getComponent(sourceRef, UUIDComponent.getComponentType());
        if (sourceUuidObj instanceof UUIDComponent sourceUuidComp) {
            return trackingSystem.getEntityModifiers(sourceUuidComp.getUuid());
        }
        return null;
    }

    private static float clampMultiplier(float value) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            return 1.0f;
        }
        return value;
    }
}
