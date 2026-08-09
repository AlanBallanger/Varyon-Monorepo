package com.varyon.easyhunger.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.EasyHungerUtils;
import com.varyon.easyhunger.ui.EasyWaterHud;
import com.varyon.easyhunger.components.ThirstComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.protocol.MovementStates;
import com.varyon.easyhunger.utils.HungerProtectionUtils;
import com.varyon.easyhunger.utils.BiomeUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EasyThirstSystem extends EntityTickingSystem<EntityStore> {

    private static final float HUD_RESYNC_INTERVAL = 5.0f;

    private EasyThirstSystem() {
        // Empty constructor - we read config dynamically each tick
    }

    public static EasyThirstSystem create() {
        return new EasyThirstSystem();
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            ThirstComponent.getComponentType(),
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            Query.not(DeathComponent.getComponentType()),
            Query.not(Invulnerable.getComponentType()),
            MovementStatesComponent.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Skip if thirst system is disabled
        if (!EasyHunger.get().getConfig().isThirstEnabled()) return;
        
        ThirstComponent thirst = archetypeChunk.getComponent(index, ThirstComponent.getComponentType());
        if (thirst == null) return;

        thirst.addTimeSinceHudResync(dt);
        boolean forceHudResync = thirst.getTimeSinceHudResync() >= HUD_RESYNC_INTERVAL;
        if (forceHudResync) thirst.resetTimeSinceHudResync();

        thirst.addElapsedTime(dt);
        if (thirst.getElapsedTime() < EasyHunger.get().getConfig().getStarvationTickRate()) {
            if (forceHudResync) {
                PlayerRef pr = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
                if (pr != null) {
                    float thirstLevel = thirst.getThirstLevel();
                    thirst.setLastSentThirst(thirstLevel);
                    EasyWaterHud.updatePlayerThirstLevel(pr, thirstLevel);
                }
            }
            return;
        }
        thirst.resetElapsedTime();


        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        float finalDecay = EasyHunger.get().getConfig().getThirstDecayRate();
        MovementStatesComponent movementComp = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (movementComp != null) {
             MovementStates states = movementComp.getMovementStates();
             if (states != null && states.sprinting) {
                 finalDecay *= EasyHunger.get().getConfig().getSprintThirstMultiplier();
             }
        }

        // Check if player is in a protected zone
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef != null && HungerProtectionUtils.isSafe(playerRef)) {
            // Skip thirst drain in safe zones, but still update HUD
            float thirstLevel = thirst.getThirstLevel();
            if (forceHudResync || Math.abs(thirstLevel - thirst.getLastSentThirst()) >= 0.01f) {
                thirst.setLastSentThirst(thirstLevel);
                EasyWaterHud.updatePlayerThirstLevel(playerRef, thirstLevel);
            }
            return;
        }
        
        // Check if player is sleeping (pause thirst while in bed)
        if (EasyHunger.get().getConfig().isPauseWhileSleeping() 
            && com.varyon.easyhunger.utils.SleepUtils.isSleeping(index, archetypeChunk)) {
            return;
        }

        // Check if player is idle (pause thirst while idle)
        if (EasyHunger.get().getConfig().isIdlePauseEnabled()) {
            if (movementComp != null) {
                MovementStates states = movementComp.getMovementStates();
                if (states != null && states.idle) {
                    thirst.addIdleTime(EasyHunger.get().getConfig().getStarvationTickRate());
                    if (thirst.getIdleTime() >= EasyHunger.get().getConfig().getIdlePauseSeconds()) {
                        return; // Paused
                    }
                } else {
                    thirst.resetIdleTime();
                }
            }
        }
        
        // Apply biome multiplier to thirst decay
        float biomeMultiplier = 1.0f;
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player != null && player.getWorld() != null) {
            TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            if (transform != null) {
                String biomeName = BiomeUtils.getBiomeNameAt(player.getWorld(), transform.getPosition());
                biomeMultiplier = EasyHunger.get().getBiomeConfig().getThirstMultiplier(biomeName);
            }
        }
        
        thirst.dehydrate(finalDecay * biomeMultiplier);
        float thirstLevel = thirst.getThirstLevel();
        float thirstyThreshold = EasyHunger.get().getConfig().getThirstyThreshold();

        // Get effect controller for applying effects
        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent effectController = 
            commandBuffer.getComponent(ref, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());

        // Apply dehydrated effect when thirst is low but not zero (only if not already applied)
        if (thirstLevel != 0 && thirstLevel < thirstyThreshold) {
            if (effectController != null && !EasyHungerUtils.activeEntityEffectIsDehydrated(effectController)) {
                effectController.addEffect(ref, EasyHungerUtils.getDehydratedEntityEffect(), commandBuffer);
            }
        }
        // Apply dehydration damage if thirst is 0
        else if (thirstLevel == 0) {
            if (effectController != null && !EasyHungerUtils.activeEntityEffectIsDehydrated(effectController)) {
                effectController.addEffect(ref, EasyHungerUtils.getDehydratedEntityEffect(), commandBuffer);
            }
            Damage damage = new Damage(Damage.NULL_SOURCE, EasyHungerUtils.getThirstDamageCause(), EasyHunger.get().getConfig().getThirstDamage());
            DamageSystems.executeDamage(ref, commandBuffer, damage);
        }
        // Remove effects if thirst is sufficient
        else if (thirstLevel >= thirstyThreshold) {
            if (effectController != null) {
                EasyHungerUtils.removeThirstRelatedEffectsFromEntity(ref, commandBuffer, effectController);
            }
        }

        if (playerRef == null) return;

        // Optimization: Only update HUD if value changed
        if (!forceHudResync && Math.abs(thirstLevel - thirst.getLastSentThirst()) < 0.01f) return;
        thirst.setLastSentThirst(thirstLevel);

        EasyWaterHud.updatePlayerThirstLevel(playerRef, thirstLevel);
    }
}
