package com.varyon.bossarena.system;

import com.varyon.bossarena.boss.BossModifiers;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.varyon.bossarena.util.BossHealthScale;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.hypixel.hytale.server.npc.movement.controllers.MotionControllerBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.RoleSystems;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BossSpeedScalingSystem extends TickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final long UPDATE_INTERVAL_MS = 250L;
    private static final float UPDATE_INTERVAL_SECONDS = UPDATE_INTERVAL_MS / 1000f;
    private static final float EPSILON = 0.0001f;
    private static final Field NPC_CACHED_SPEED_FIELD = resolveCachedSpeedField();
    private static final Field INTERACTION_MANAGER_COOLDOWN_HANDLER_FIELD = resolveField(InteractionManager.class, "cooldownHandler");
    private static final Field COOLDOWN_HANDLER_COOLDOWNS_FIELD = resolveField(CooldownHandler.class, "cooldowns");
    private static final Field MOTION_CONTROLLER_MAX_HEAD_ROTATION_SPEED_FIELD = resolveField(MotionControllerBase.class, "maxHeadRotationSpeed");
    /** Per-entity regen config on the Health stat; emptied to stop natural regen during HP-% waves. */
    private static final Field REGENERATING_VALUES_FIELD = resolveField(EntityStatValue.class, "regeneratingValues");

    private static final long REGEN_INTERVAL_MS = 1000L;

    private final BossTrackingSystem trackingSystem;
    private final Map<MotionControllerBase, Float> baseTurnRateByController =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<UUID> warnedLowSpeedBosses = ConcurrentHashMap.newKeySet();
    /** HP a boss had when its HP-% wave started; fallback path only, when reflection is unavailable. */
    private final Map<UUID, Float> heldHealthDuringHpWave = new ConcurrentHashMap<>();
    /** Regenerating values removed from a boss's Health stat during an HP-% wave, kept for restore. */
    private final Map<UUID, Object[]> suppressedRegenValues = new ConcurrentHashMap<>();
    /**
     * Per-world wall-clock deadlines so multi-world ticks don't accelerate regen/scalers.
     * tick() may fire more than once per real-time interval (observed in production), so pacing
     * is done against System.currentTimeMillis() rather than accumulated {@code dt} — summing dt
     * across redundant calls made regen apply several times too fast (boss regenerating "at full speed").
     */
    private final Map<String, Long> nextScalerRunAtMsByWorld = new ConcurrentHashMap<>();
    private final Map<String, Long> nextRegenRunAtMsByWorld = new ConcurrentHashMap<>();

    public BossSpeedScalingSystem(BossTrackingSystem trackingSystem) {
        this.trackingSystem = trackingSystem;
    }

    /**
     * Speed must be written after the engine clears the NPC speed cache and before behaviour/steer
     * reads it — otherwise the boss speed multiplier is wiped every NPC tick.
     */
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
                new SystemDependency<>(Order.AFTER, RoleSystems.PreBehaviourSupportTickSystem.class),
                new SystemDependency<>(Order.BEFORE, RoleSystems.BehaviourTickSystem.class)
        );
    }

    private static float clampMultiplier(float value) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            return 1.0f;
        }
        return value;
    }

    private static Field resolveField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            LOGGER.warning("Boss scaling support disabled for " + owner.getSimpleName() + "." + name + ".");
            return null;
        }
    }

    private static Field resolveCachedSpeedField() {
        try {
            Field field = NPCEntity.class.getDeclaredField("cachedEntityHorizontalSpeedMultiplier");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            LOGGER.warning("Boss speed scaling disabled: unable to access NPC speed cache field.");
            return null;
        }
    }

    @Override
    public void tick(float dt, int index, @Nonnull Store<EntityStore> store) {
        if (trackingSystem == null) {
            return;
        }

        EntityStore external = store.getExternalData();
        World tickWorld = external != null ? external.getWorld() : null;
        if (tickWorld == null || !tickWorld.isAlive()) {
            return;
        }
        String worldKey = tickWorld.getName();
        if (worldKey == null || worldKey.isBlank()) {
            worldKey = Integer.toHexString(System.identityHashCode(tickWorld));
        }

        long now = System.currentTimeMillis();

        boolean applyRegenTick = false;
        Long nextRegenAt = nextRegenRunAtMsByWorld.get(worldKey);
        if (nextRegenAt == null || now >= nextRegenAt) {
            nextRegenRunAtMsByWorld.put(worldKey, now + REGEN_INTERVAL_MS);
            applyRegenTick = true;
        }
        // Speed must re-apply every tick: PreBehaviour clears the NPC speed cache each frame.
        boolean applySpeed = NPC_CACHED_SPEED_FIELD != null;
        boolean applyPeriodicScalers = false;
        Long nextScalerAt = nextScalerRunAtMsByWorld.get(worldKey);
        if (nextScalerAt == null || now >= nextScalerAt) {
            nextScalerRunAtMsByWorld.put(worldKey, now + UPDATE_INTERVAL_MS);
            applyPeriodicScalers = true;
            // A boss that died mid-wave is no longer ticked, so its saved regen state would leak.
            // The entity is gone, so there is nothing left to restore it onto — just drop the entry.
            suppressedRegenValues.keySet().removeIf(uuid -> !trackingSystem.isTracked(uuid));
            heldHealthDuringHpWave.keySet().removeIf(uuid -> !trackingSystem.isTracked(uuid));
        }
        if (!applySpeed && !applyPeriodicScalers && !applyRegenTick) {
            return;
        }

        for (Map.Entry<UUID, BossTrackingSystem.BossData> entry : trackingSystem.snapshotTrackedBosses(tickWorld).entrySet()) {
            UUID entityUuid = entry.getKey();
            BossTrackingSystem.BossData data = entry.getValue();
            if (entityUuid == null || data == null) {
                continue;
            }
            applyRuntimeScalers(
                    entityUuid, store, external, tickWorld, data.modifiers,
                    applySpeed, applyPeriodicScalers, applyRegenTick
            );
        }

        for (Map.Entry<UUID, UUID> entry : trackingSystem.snapshotTrackedAdds(tickWorld).entrySet()) {
            UUID addUuid = entry.getKey();
            UUID bossUuid = entry.getValue();
            if (addUuid == null || bossUuid == null) {
                continue;
            }

            BossModifiers addModifiers = trackingSystem.getEntityModifiers(addUuid);
            applyRuntimeScalers(
                    addUuid, store, external, tickWorld, addModifiers,
                    applySpeed, applyPeriodicScalers, applyRegenTick
            );
        }
    }

    private void applyRuntimeScalers(
            UUID entityUuid,
            Store<EntityStore> store,
            EntityStore external,
            World world,
            BossModifiers modifiers,
            boolean applySpeed,
            boolean applyPeriodicScalers,
            boolean applyRegenTick
    ) {
        if (entityUuid == null || store == null || world == null || modifiers == null) {
            return;
        }

        if (applySpeed) {
            applySpeedMultiplier(entityUuid, world, modifiers);
        }
        if (applyPeriodicScalers) {
            applyTurnRateMultiplier(entityUuid, world, modifiers);
            applyInteractionCooldownScaling(entityUuid, world, modifiers);
            resyncHealthModifier(entityUuid, store, external, modifiers);
            holdHealthDuringHpWave(entityUuid, store, external);
        }
        if (applyRegenTick) {
            applyFlatRegeneration(entityUuid, store, external, modifiers);
        }
    }

    /**
     * Re-asserts the BossArena MAX-HP modifier, because Varyon's MobScalingRefSystem re-adds its own
     * {@code Varyon_Health} modifier whenever the entity is re-added to the store (chunk reload,
     * archetype change). That raises MAX without raising current HP, so the entity looks like it
     * spawned below full and then regenerates. Uses applyModifierOnly so player damage is preserved.
     */
    private void resyncHealthModifier(UUID entityUuid,
                                      Store<EntityStore> store,
                                      EntityStore external,
                                      BossModifiers modifiers) {
        float hpMultiplier = modifiers.hpMultiplier();
        if (!Float.isFinite(hpMultiplier) || hpMultiplier <= 0.0f) {
            return;
        }
        float worldFactor = trackingSystem.getWorldHealthFactor(entityUuid);
        if (worldFactor <= 0.01f) {
            return;
        }
        try {
            Ref<EntityStore> entityRef = external != null ? external.getRefFromUUID(entityUuid) : null;
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }
            Object statMapObj = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (statMapObj instanceof EntityStatMap statMap) {
                BossHealthScale.applyModifierOnly(statMap, hpMultiplier, worldFactor);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to resync health modifier for entity " + entityUuid, e);
        }
    }

    /**
     * Suppresses the engine's natural HP regeneration while an HP-% wave is up.
     *
     * <p>The boss cannot be damaged during the wave, so out-of-combat regen would heal it back to
     * full before players can resume. Rather than undoing the healing after the fact, the stat's
     * per-entity {@code regeneratingValues} are cleared so regen never runs, then restored when the
     * wave ends. Falls back to pinning current HP if the field cannot be reached.
     */
    private void holdHealthDuringHpWave(UUID entityUuid, Store<EntityStore> store, EntityStore external) {
        boolean locked = trackingSystem.isTracked(entityUuid)
                && trackingSystem.isBossDamageLockedByHpWave(entityUuid);
        if (!locked && !heldHealthDuringHpWave.containsKey(entityUuid)
                && !suppressedRegenValues.containsKey(entityUuid)) {
            return;
        }
        try {
            Ref<EntityStore> entityRef = external != null ? external.getRefFromUUID(entityUuid) : null;
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }
            Object statMapObj = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (!(statMapObj instanceof EntityStatMap statMap)) {
                return;
            }
            int healthIndex = DefaultEntityStatTypes.getHealth();
            if (healthIndex < 0) {
                return;
            }
            EntityStatValue health = statMap.get(healthIndex);
            if (health == null) {
                return;
            }

            if (!locked) {
                restoreRegeneration(entityUuid, health);
                heldHealthDuringHpWave.remove(entityUuid);
                return;
            }

            if (suppressRegeneration(entityUuid, health)) {
                return;
            }

            // Reflection unavailable: fall back to pinning HP at the value held when the wave began.
            float current = health.get();
            Float held = heldHealthDuringHpWave.get(entityUuid);
            if (held == null) {
                heldHealthDuringHpWave.put(entityUuid, current);
                return;
            }
            if (current > held + 0.01f) {
                statMap.setStatValue(EntityStatMap.Predictable.ALL, healthIndex, held);
            } else if (current < held) {
                heldHealthDuringHpWave.put(entityUuid, current);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to hold HP during HP wave for entity " + entityUuid, e);
        }
    }

    /** Empties the stat's regenerating values, remembering them for restore. True when applied. */
    private boolean suppressRegeneration(UUID entityUuid, EntityStatValue health) {
        if (REGENERATING_VALUES_FIELD == null) {
            return false;
        }
        if (suppressedRegenValues.containsKey(entityUuid)) {
            return true;
        }
        try {
            Object existing = REGENERATING_VALUES_FIELD.get(health);
            if (!(existing instanceof Object[] values) || values.length == 0) {
                return true;
            }
            suppressedRegenValues.put(entityUuid, values);
            REGENERATING_VALUES_FIELD.set(health, java.lang.reflect.Array.newInstance(
                    values.getClass().getComponentType(), 0));
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to suppress regeneration for entity " + entityUuid, e);
            return false;
        }
    }

    private void restoreRegeneration(UUID entityUuid, EntityStatValue health) {
        Object[] saved = suppressedRegenValues.remove(entityUuid);
        if (saved == null || REGENERATING_VALUES_FIELD == null) {
            return;
        }
        try {
            REGENERATING_VALUES_FIELD.set(health, saved);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to restore regeneration for entity " + entityUuid, e);
        }
    }

    private void applySpeedMultiplier(UUID entityUuid, World world, BossModifiers modifiers) {
        // While an HP-% wave is up the boss cannot be damaged, so it must not wander off either:
        // pin its speed to zero until the wave adds are cleared.
        boolean frozen = trackingSystem.isTracked(entityUuid)
                && trackingSystem.isBossDamageLockedByHpWave(entityUuid);
        float speedMultiplier = clampMultiplier(modifiers.speedMultiplier());
        if (!frozen
                && (!Float.isFinite(speedMultiplier) || Math.abs(speedMultiplier - 1.0f) <= EPSILON)) {
            return;
        }

        try {
            var entityRef = world.getEntityRef(entityUuid);
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Store<EntityStore> worldStore = world.getEntityStore().getStore();
            Object npcObj = worldStore.getComponent(entityRef, NPCEntity.getComponentType());
            if (!(npcObj instanceof NPCEntity npcEntity)) {
                return;
            }

            // PreBehaviour already invalidated the cache; rebuild natural speed from effects, then buff.
            npcEntity.invalidateCachedHorizontalSpeedMultiplier();
            float naturalSpeed = npcEntity.getCurrentHorizontalSpeedMultiplier(entityRef, worldStore);
            if (!Float.isFinite(naturalSpeed)) {
                naturalSpeed = 1.0f;
            }
            if (frozen) {
                NPC_CACHED_SPEED_FIELD.setFloat(npcEntity, 0.0f);
                return;
            }
            float desiredSpeed = clampMultiplier(naturalSpeed * speedMultiplier);
            if (desiredSpeed < 0.1f) {
                if (warnedLowSpeedBosses.add(entityUuid)) {
                    LOGGER.warning("Setting very low speed (" + desiredSpeed + ") for boss " + entityUuid +
                            ". Natural: " + naturalSpeed + ", Multiplier: " + speedMultiplier);
                }
            } else {
                warnedLowSpeedBosses.remove(entityUuid);
            }
            NPC_CACHED_SPEED_FIELD.setFloat(npcEntity, desiredSpeed);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to apply movement speed scaling for entity " + entityUuid, e);
        }
    }

    private void applyTurnRateMultiplier(UUID entityUuid, World world, BossModifiers modifiers) {
        if (MOTION_CONTROLLER_MAX_HEAD_ROTATION_SPEED_FIELD == null) {
            return;
        }

        float turnRateMultiplier = clampMultiplier(modifiers.turnRateMultiplier());
        if (!Float.isFinite(turnRateMultiplier)) {
            return;
        }

        try {
            var entityRef = world.getEntityRef(entityUuid);
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Store<EntityStore> worldStore = world.getEntityStore().getStore();
            Object npcObj = worldStore.getComponent(entityRef, NPCEntity.getComponentType());
            if (!(npcObj instanceof NPCEntity npcEntity)) {
                return;
            }

            Role role = npcEntity.getRole();
            if (role == null) {
                return;
            }
            MotionController controller = role.getActiveMotionController();
            if (!(controller instanceof MotionControllerBase motionController)) {
                return;
            }

            float baseRate = baseTurnRateByController.computeIfAbsent(motionController, ignored -> {
                try {
                    return MOTION_CONTROLLER_MAX_HEAD_ROTATION_SPEED_FIELD.getFloat(motionController);
                } catch (IllegalAccessException e) {
                    return 360.0f;
                }
            });

            float desired = Math.max(EPSILON, baseRate * turnRateMultiplier);
            float current = MOTION_CONTROLLER_MAX_HEAD_ROTATION_SPEED_FIELD.getFloat(motionController);
            if (Math.abs(current - desired) > EPSILON) {
                MOTION_CONTROLLER_MAX_HEAD_ROTATION_SPEED_FIELD.setFloat(motionController, desired);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to apply turn rate scaling for entity " + entityUuid, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyInteractionCooldownScaling(UUID entityUuid, World world, BossModifiers modifiers) {
        if (INTERACTION_MANAGER_COOLDOWN_HANDLER_FIELD == null || COOLDOWN_HANDLER_COOLDOWNS_FIELD == null) {
            return;
        }

        float attackRateMultiplier = clampMultiplier(modifiers.attackRateMultiplier());
        float abilityCooldownMultiplier = clampMultiplier(modifiers.abilityCooldownMultiplier());
        if (!Float.isFinite(attackRateMultiplier) || !Float.isFinite(abilityCooldownMultiplier)) {
            return;
        }

        float cooldownTickFactor = attackRateMultiplier / abilityCooldownMultiplier;
        if (!Float.isFinite(cooldownTickFactor) || Math.abs(cooldownTickFactor - 1.0f) <= EPSILON) {
            return;
        }

        try {
            var entityRef = world.getEntityRef(entityUuid);
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Store<EntityStore> worldStore = world.getEntityStore().getStore();
            Object interactionManagerObj = worldStore.getComponent(
                    entityRef,
                    InteractionModule.get().getInteractionManagerComponent()
            );
            if (!(interactionManagerObj instanceof InteractionManager interactionManager)) {
                return;
            }

            Object cooldownHandlerObj = INTERACTION_MANAGER_COOLDOWN_HANDLER_FIELD.get(interactionManager);
            if (!(cooldownHandlerObj instanceof CooldownHandler cooldownHandler)) {
                return;
            }

            float desiredTick = UPDATE_INTERVAL_SECONDS * cooldownTickFactor;
            float adjustment = desiredTick - UPDATE_INTERVAL_SECONDS;
            if (Math.abs(adjustment) <= EPSILON) {
                return;
            }

            if (adjustment > 0.0f) {
                cooldownHandler.tick(adjustment);
                return;
            }

            Object cooldownsObj = COOLDOWN_HANDLER_COOLDOWNS_FIELD.get(cooldownHandler);
            if (!(cooldownsObj instanceof Map<?, ?> cooldowns)) {
                return;
            }

            float increase = -adjustment;
            for (Object cooldownObj : cooldowns.values()) {
                if (cooldownObj instanceof CooldownHandler.Cooldown cooldown) {
                    cooldown.increaseTime(increase);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to apply cooldown scaling for entity " + entityUuid, e);
        }
    }

    /** Restores flat HP every second from {@link BossModifiers#regenMultiplier()} (HP/s semantic). */
    private void applyFlatRegeneration(
            UUID entityUuid,
            Store<EntityStore> store,
            EntityStore external,
            BossModifiers modifiers
    ) {
        float regenHp = modifiers.regenMultiplier();
        if (!Float.isFinite(regenHp) || regenHp <= EPSILON) {
            return;
        }
        if (trackingSystem.isTracked(entityUuid) && trackingSystem.isBossDamageLockedByHpWave(entityUuid)) {
            return;
        }

        try {
            Ref<EntityStore> entityRef = external != null ? external.getRefFromUUID(entityUuid) : null;
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Object statMapObj = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (!(statMapObj instanceof EntityStatMap statMap)) {
                return;
            }

            int healthIndex = DefaultEntityStatTypes.getHealth();
            if (healthIndex < 0) {
                return;
            }

            EntityStatValue health = statMap.get(healthIndex);
            if (health == null) {
                return;
            }

            float current = health.get();
            float max = health.getMax();
            float min = health.getMin();
            // Do not revive / pad a dying boss.
            if (!Float.isFinite(current) || !Float.isFinite(max)
                    || current <= min + EPSILON
                    || current >= max - EPSILON) {
                return;
            }

            // Predictable.ALL is required so clients / HP bars see the heal (NONE stays local-ish).
            float applied = statMap.addStatValue(EntityStatMap.Predictable.ALL, healthIndex, regenHp);
            if (!Float.isFinite(applied) || applied + EPSILON < current) {
                // Fallback if Add path is ignored for this entity.
                float target = Math.min(max, current + regenHp);
                statMap.setStatValue(EntityStatMap.Predictable.ALL, healthIndex, target);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to apply flat regeneration for entity " + entityUuid, e);
        }
    }
}
