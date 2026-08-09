package com.varyon.easyhunger.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.protocol.MovementStates;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.EasyHungerUtils;
import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.ui.EasyHungerHud;
import com.varyon.easyhunger.components.HungerComponent;
import com.varyon.easyhunger.utils.HungerProtectionUtils;
import com.varyon.easyhunger.utils.BiomeUtils;
import com.varyon.easyhunger.config.BiomeModifiersConfig;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StarveSystem extends EntityTickingSystem<EntityStore> {

    private static final float HUD_RESYNC_INTERVAL = 5.0f;

    private StarveSystem() {
        // Empty constructor - we read config dynamically each tick
    }

    public static StarveSystem create () {
        return new StarveSystem();
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
            HungerComponent.getComponentType(),
            EntityStatMap.getComponentType(),
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            PlayerRef.getComponentType(),
            Query.not(DeathComponent.getComponentType()),
            Query.not(Invulnerable.getComponentType())
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
        HungerComponent hunger = archetypeChunk.getComponent(index, HungerComponent.getComponentType());
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (hunger == null || entityStatMap == null) return;

        hunger.addTimeSinceHudResync(dt);
        boolean forceHudResync = hunger.getTimeSinceHudResync() >= HUD_RESYNC_INTERVAL;
        if (forceHudResync) hunger.resetTimeSinceHudResync();

        hunger.setStaminaSeen(getStaminaValue(entityStatMap));
        hunger.addElapsedTime(dt);
        if (hunger.getElapsedTime() < EasyHunger.get().getConfig().getStarvationTickRate()) {
            if (forceHudResync) {
                PlayerRef pr = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
                if (pr != null) {
                    float hungerLevel = hunger.getHungerLevel();
                    hunger.setLastSentHunger(hungerLevel);
                    EasyHungerHud.updatePlayerHungerLevel(pr, hungerLevel);
                }
            }
            return;
        }
        hunger.resetElapsedTime();

        float lowestStaminaSeen = hunger.getAndResetLowestStaminaSeen();

        // Check if player is in a protected zone
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef != null && HungerProtectionUtils.isSafe(playerRef)) {
            // Skip hunger drain in safe zones, but still update HUD
            float hungerLevel = hunger.getHungerLevel();
            if (forceHudResync || Math.abs(hungerLevel - hunger.getLastSentHunger()) >= 0.01f) {
                hunger.setLastSentHunger(hungerLevel);
                EasyHungerHud.updatePlayerHungerLevel(playerRef, hungerLevel);
            }
            return;
        }
        
        // Check if player is sleeping (pause hunger while in bed)
        if (EasyHunger.get().getConfig().isPauseWhileSleeping() 
            && com.varyon.easyhunger.utils.SleepUtils.isSleeping(index, archetypeChunk)) {
            return;
        }

        // Check if player is idle (pause hunger while idle)
        if (EasyHunger.get().getConfig().isIdlePauseEnabled()) {
            MovementStatesComponent movementComp = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
            if (movementComp != null) {
                MovementStates states = movementComp.getMovementStates();
                if (states != null && states.idle) {
                    hunger.addIdleTime(EasyHunger.get().getConfig().getStarvationTickRate());
                    if (hunger.getIdleTime() >= EasyHunger.get().getConfig().getIdlePauseSeconds()) {
                        return; // Paused
                    }
                } else {
                    hunger.resetIdleTime();
                }
            }
        }
        
        float staminaModifier = ((10.0f - lowestStaminaSeen) / 10.0f) * EasyHunger.get().getConfig().getStarvationStaminaModifier();
        
        // Apply biome multiplier
        float biomeMultiplier = 1.0f;
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player != null && player.getWorld() != null) {
            TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            if (transform != null) {
                String biomeName = BiomeUtils.getBiomeNameAt(player.getWorld(), transform.getPosition());
                biomeMultiplier = EasyHunger.get().getBiomeConfig().getHungerMultiplier(biomeName);
            }
        }
        
        hunger.starve((EasyHunger.get().getConfig().getStarvationPerTick() + staminaModifier) * biomeMultiplier);

        float hungerLevel = hunger.getHungerLevel();
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        // Apply hungry effect when hunger level is below 20 (only if not already applied)
        if (hungerLevel != 0 && hungerLevel < EasyHunger.get().getConfig().getHungryThreshold()) {
            EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectController == null) return;
            
            // Only apply if not already hungry
            if (!hasHungerEffect(effectController)) {
                EntityEffect hungryEffect = EasyHungerUtils.getHungryEntityEffect();
                effectController.addEffect(ref, hungryEffect, commandBuffer);
            }
        }

        // Apply starvation effect when hunger level reaches 0
        else if (hungerLevel == 0) {
            EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectController == null) return;
            
            // Only apply starving effect if not already starving
            if (!hasStarvingEffect(effectController)) {
                // remove hungry effect if present
                EasyHungerUtils.removeHungerRelatedEffectsFromEntity(ref, commandBuffer, effectController);
                EntityEffect starvingEffect = EasyHungerUtils.getStarvingEntityEffect();
                effectController.addEffect(ref, starvingEffect, commandBuffer);
            }
            // apply starvation damage
            Damage damage = new Damage(Damage.NULL_SOURCE, EasyHungerUtils.getStarvationDamageCause(), EasyHunger.get().getConfig().getStarvationDamage());
            DamageSystems.executeDamage(ref, commandBuffer, damage);
        }
        
        // Remove effects if hunger is sufficient
        else if (hungerLevel >= EasyHunger.get().getConfig().getHungryThreshold()) {
              EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
              if (effectController != null) {
                  EasyHungerUtils.removeHungerRelatedEffectsFromEntity(ref, commandBuffer, effectController);
              }
        }

        if (playerRef == null) return;

        // Optimization: Only update HUD if value changed
        if (!forceHudResync && Math.abs(hungerLevel - hunger.getLastSentHunger()) < 0.01f) return;
        hunger.setLastSentHunger(hungerLevel);
        
        EasyHungerHud.updatePlayerHungerLevel(playerRef, hungerLevel);
    }

    public static boolean shouldRemoveEffectOnStarvation (ActiveEntityEffect effect) {
        // Only remove our specific hunger-related debuffs
        if (EasyHungerUtils.activeEntityEffectIsHungry(effect)) return true;
        if (EasyHungerUtils.activeEntityEffectIsStarving(effect)) return true;
        // Don't remove any other effects (like health regen from food)
        return false;
    }

    public static float getStaminaValue(@NonNullDecl EntityStatMap entityStatMap) {
        final int staminaRef = DefaultEntityStatTypes.getStamina();
        final EntityStatValue statValue = entityStatMap.get(staminaRef);
        if (statValue == null) return 10.0f; // Default stamina (max) value if not found
        return statValue.get();
    }
    
    private boolean hasHungerEffect(EffectControllerComponent effectController) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (EasyHungerUtils.activeEntityEffectIsHungry(effect)) return true;
            }
        }
        return false;
    }
    
    private boolean hasStarvingEffect(EffectControllerComponent effectController) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (EasyHungerUtils.activeEntityEffectIsStarving(effect)) return true;
            }
        }
        return false;
    }
}



