package com.varyon.elite.ability;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.elite.EliteStats;
import com.varyon.elite.component.EliteAbilityStateComponent;
import com.varyon.elite.component.EliteComponent;
import com.varyon.elite.component.ElitePendingAbilityComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-tick brain for elites: maintains enrage state, fires a pending telegraphed ability
 * when its wind-up elapses, otherwise picks a ready ability whose range fits the nearest
 * player and either casts it instantly or schedules a telegraph.
 */
public class EliteAbilityTickSystem extends EntityTickingSystem<EntityStore> {

    private static final double AGGRO_RANGE = 24.0;
    private static final long SCAN_INTERVAL_MS = 500;

    private final com.varyon.elite.EliteConfig config;
    private final java.util.Random rng = new java.util.Random();

    public EliteAbilityTickSystem(@Nonnull com.varyon.elite.EliteConfig config) {
        this.config = config;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(EliteComponent.getComponentType(), EliteAbilityStateComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        long now = System.currentTimeMillis();
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        EliteComponent elite = chunk.getComponent(index, EliteComponent.getComponentType());
        EliteAbilityStateComponent state = chunk.getComponent(index, EliteAbilityStateComponent.getComponentType());
        if (elite == null || state == null) {
            return;
        }

        TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        org.joml.Vector3d elitePos = new org.joml.Vector3d(tc.getPosition());

        // Spawn window: keep HP pinned to max until every scaling modifier has landed.
        if (elite.isHealToFull(now)) {
            EntityStatMap sm = store.getComponent(ref, EntityStatMap.getComponentType());
            if (sm != null) {
                int hi = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                if (hi >= 0) {
                    var hv = sm.get(hi);
                    if (hv != null && hv.getMax() > 0f && hv.get() + 0.5f < hv.getMax()) {
                        sm.setStatValue(EntityStatMap.Predictable.ALL, hi, hv.getMax());
                    }
                }
            }
        }

        updateEnrage(store, ref, elite);

        ElitePendingAbilityComponent pending = store.getComponent(ref, ElitePendingAbilityComponent.getComponentType());
        if (pending != null) {
            if (now >= pending.getFireAtMs() && pending.getAbility() != null) {
                EliteAbilityExecutor.execute(pending.getAbility(), elite, ref, store, commandBuffer,
                        elitePos, pending.getTarget());
                state.putOnCooldown(pending.getAbility(), now + EliteAbilitySpec.of(pending.getAbility()).cooldownMs);
                state.lockAnimation(now + 500);
                state.startGlobalCooldown(now + config.rollGlobalAbilityCooldownMs(rng));
                commandBuffer.removeComponent(ref, ElitePendingAbilityComponent.getComponentType());
            }
            return;
        }

        if (state.isAnimationLocked(now) || state.isGlobalCooldown(now) || !elite.isScanReady(now)) {
            return;
        }
        elite.markScanned(now + SCAN_INTERVAL_MS);

        org.joml.Vector3d targetPos = nearestPlayerPos(store, ref, elitePos);
        if (targetPos == null) {
            return;
        }
        double dist = elitePos.distance(targetPos);

        EliteAbility chosen = pickAbility(elite, state, now, dist);
        if (chosen == null) {
            return;
        }
        EliteAbilitySpec spec = EliteAbilitySpec.of(chosen);
        if (chosen.isTelegraphed() && spec.telegraphMs > 0) {
            EliteAbilityExecutor.telegraph(store, commandBuffer, targetPos);
            commandBuffer.addComponent(ref, ElitePendingAbilityComponent.getComponentType(),
                    new ElitePendingAbilityComponent(chosen, now + spec.telegraphMs,
                            targetPos.x, targetPos.y, targetPos.z));
            state.lockAnimation(now + spec.telegraphMs);
        } else {
            EliteAbilityExecutor.execute(chosen, elite, ref, store, commandBuffer, elitePos, targetPos);
            state.putOnCooldown(chosen, now + spec.cooldownMs);
            state.lockAnimation(now + 400);
            state.startGlobalCooldown(now + config.rollGlobalAbilityCooldownMs(rng));
        }
    }

    private static final double MELEE_RANGE = 3.0;

    /**
     * Picks one ready ability whose range fits {@code dist}. Prefers real attacks over
     * gap-closers, and never picks a gap-closer while already in melee - that was the cause
     * of elites blinking onto the player over and over.
     */
    @Nullable
    private EliteAbility pickAbility(@Nonnull EliteComponent elite, @Nonnull EliteAbilityStateComponent state,
                                    long now, double dist) {
        EliteAbility gapCloser = null;
        for (EliteAbility ability : elite.getAbilities()) {
            if (!state.isReady(ability, now)) {
                continue;
            }
            EliteAbilitySpec spec = EliteAbilitySpec.of(ability);
            if (dist < spec.minRange || dist > spec.maxRange) {
                continue;
            }
            if (isGapCloser(ability)) {
                if (dist > MELEE_RANGE && gapCloser == null) {
                    gapCloser = ability; // remember, but only as a fallback
                }
                continue;
            }
            return ability; // a real attack that fits - use it now
        }
        return gapCloser; // nothing better; close the distance
    }

    private static boolean isGapCloser(@Nonnull EliteAbility a) {
        return switch (a) {
            case CHARGE_LEAP, LEAP, FORWARD_CHARGE, DASH, DODGE_ROLL -> true;
            default -> false;
        };
    }

    private void updateEnrage(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                              @Nonnull EliteComponent elite) {
        if (!elite.getMobClass().hasEnrage()) {
            return;
        }
        EntityStatMap sm = store.getComponent(ref, EntityStatMap.getComponentType());
        if (sm == null) {
            return;
        }
        if (!elite.isEnraged() && EliteStats.healthFraction(sm) <= EliteStats.enrageThreshold()) {
            elite.setEnraged(true);
        }
    }

    private org.joml.Vector3d nearestPlayerPos(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> eliteRef,
                                               @Nonnull org.joml.Vector3d from) {
        AtomicReference<org.joml.Vector3d> best = new AtomicReference<>();
        AtomicReference<Double> bestDist = new AtomicReference<>(Double.MAX_VALUE);
        try {
            Selector.selectNearbyEntities(store, new org.joml.Vector3d(from), AGGRO_RANGE, t -> {
                try {
                    Player player = store.getComponent(t, Player.getComponentType());
                    if (player == null || player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) {
                        return;
                    }
                    TransformComponent tc = store.getComponent(t, TransformComponent.getComponentType());
                    if (tc == null) {
                        return;
                    }
                    org.joml.Vector3d p = new org.joml.Vector3d(tc.getPosition());
                    double d = from.distance(p);
                    if (d < bestDist.get()) {
                        bestDist.set(d);
                        best.set(p);
                    }
                } catch (Exception ignored) {
                }
            }, t -> true);
        } catch (Exception ignored) {
        }
        return best.get();
    }
}
