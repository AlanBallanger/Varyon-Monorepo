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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.components.HungerComponent;
import com.varyon.easyhunger.components.ThirstComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * System that applies regeneration buffs based on hunger and thirst levels.
 * - Nourished (Health regen) when hunger >= threshold
 * - Hydrated (Stamina regen) when thirst >= threshold
 */
public class WellFedSystem extends EntityTickingSystem<EntityStore> {
    
    private final float wellFedThreshold;
    private final float tickRate;
    
    // Effect IDs
    public static final String NOURISHED_EFFECT_ID = "Nourished";  // Health regen from food
    public static final String HYDRATED_EFFECT_ID = "Hydrated";    // Stamina regen from water
    
    private EntityEffect nourishedEffect;
    private EntityEffect hydratedEffect;
    private boolean effectsLoaded = false;
    
    private WellFedSystem(float wellFedThreshold, float tickRate) {
        this.wellFedThreshold = wellFedThreshold;
        this.tickRate = tickRate;
    }
    
    public static WellFedSystem create() {
        EasyHungerConfig conf = EasyHunger.get().getConfig();
        return new WellFedSystem(
            conf.getWellFedThreshold(),
            1.0f // Check every 1 second
        );
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
            ThirstComponent.getComponentType(),
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            Query.not(DeathComponent.getComponentType()),
            Query.not(Invulnerable.getComponentType())
        );
    }
    
    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Load effects on first tick
        if (!effectsLoaded) {
            nourishedEffect = EntityEffect.getAssetMap().getAsset(NOURISHED_EFFECT_ID);
            hydratedEffect = EntityEffect.getAssetMap().getAsset(HYDRATED_EFFECT_ID);
            effectsLoaded = true;
            
            if (nourishedEffect != null) {
                EasyHunger.logInfo("Nourished effect loaded: " + NOURISHED_EFFECT_ID);
            } else {
                EasyHunger.logInfo("Nourished effect NOT FOUND: " + NOURISHED_EFFECT_ID);
            }
            if (hydratedEffect != null) {
                EasyHunger.logInfo("Hydrated effect loaded: " + HYDRATED_EFFECT_ID);
            } else {
                EasyHunger.logInfo("Hydrated effect NOT FOUND: " + HYDRATED_EFFECT_ID);
            }
        }
        
        HungerComponent hunger = archetypeChunk.getComponent(index, HungerComponent.getComponentType());
        ThirstComponent thirst = archetypeChunk.getComponent(index, ThirstComponent.getComponentType());
        if (hunger == null || thirst == null) return;
        
        // Track elapsed time for tick rate
        hunger.addWellFedElapsedTime(dt);
        if (hunger.getWellFedElapsedTime() < this.tickRate) return;
        hunger.resetWellFedElapsedTime();
        
        // Compare against absolute threshold (like HungryThreshold)
        // Example: if threshold=45 and maxHunger=50, buff activates at 45+ hunger
        float hungerLevel = hunger.getHungerLevel();
        float thirstLevel = thirst.getThirstLevel();
        
        boolean shouldBeNourished = hungerLevel >= wellFedThreshold;
        boolean shouldBeHydrated = thirstLevel >= wellFedThreshold;
        
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
        if (effectController == null) return;
        
        // Handle Nourished effect (Health regen from food)
        if (nourishedEffect != null) {
            boolean hasNourished = hasEffect(effectController, NOURISHED_EFFECT_ID);
            if (shouldBeNourished && !hasNourished) {
                effectController.addEffect(ref, nourishedEffect, commandBuffer);
            } else if (!shouldBeNourished && hasNourished) {
                removeEffect(ref, commandBuffer, effectController, NOURISHED_EFFECT_ID);
            }
        }
        
        // Handle Hydrated effect (Stamina regen from water)
        if (hydratedEffect != null) {
            boolean hasHydrated = hasEffect(effectController, HYDRATED_EFFECT_ID);
            if (shouldBeHydrated && !hasHydrated) {
                effectController.addEffect(ref, hydratedEffect, commandBuffer);
            } else if (!shouldBeHydrated && hasHydrated) {
                removeEffect(ref, commandBuffer, effectController, HYDRATED_EFFECT_ID);
            }
        }
    }
    
    private boolean hasEffect(EffectControllerComponent effectController, String effectId) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (isEffect(effect, effectId)) return true;
            }
        }
        return false;
    }
    
    private boolean isEffect(ActiveEntityEffect effect, String effectId) {
        try {
            Field f = ActiveEntityEffect.class.getDeclaredField("entityEffectId");
            f.setAccessible(true);
            String id = (String) f.get(effect);
            return id.equals(effectId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }
    
    private void removeEffect(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, EffectControllerComponent effectController, String effectId) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (isEffect(effect, effectId)) {
                    effectController.removeEffect(ref, effect.getEntityEffectIndex(), accessor);
                }
            }
        }
    }
}
