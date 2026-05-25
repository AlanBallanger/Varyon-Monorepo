package com.varyon.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Component that stores the scaling multipliers for a scaled NPC.
 * These are calculated once at spawn based on distance from world origin.
 */
public class MobScalingComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, MobScalingComponent> COMPONENT_TYPE;

    private final int mobLevel;
    private final float healthMultiplier;
    private final float damageMultiplier;
    private final float lootMultiplier;
    private final float essenceMultiplier;

    public MobScalingComponent(int mobLevel, float healthMultiplier, float damageMultiplier, float lootMultiplier, float essenceMultiplier) {
        this.mobLevel = mobLevel;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.lootMultiplier = lootMultiplier;
        this.essenceMultiplier = essenceMultiplier;
    }

    public int getMobLevel() {
        return mobLevel;
    }

    public float getHealthMultiplier() {
        return healthMultiplier;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getLootMultiplier() {
        return lootMultiplier;
    }

    public float getEssenceMultiplier() {
        return essenceMultiplier;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new MobScalingComponent(mobLevel, healthMultiplier, damageMultiplier, lootMultiplier, essenceMultiplier);
    }

    @Nonnull
    public static ComponentType<EntityStore, MobScalingComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, MobScalingComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
