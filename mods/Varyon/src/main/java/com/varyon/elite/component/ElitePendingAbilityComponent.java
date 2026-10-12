package com.varyon.elite.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.elite.ability.EliteAbility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A telegraphed ability that has been announced and will fire at {@link #fireAtMs}. The
 * target position is captured at cast time so a dodge during the wind-up actually works.
 */
public class ElitePendingAbilityComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, ElitePendingAbilityComponent> COMPONENT_TYPE;

    private EliteAbility ability;
    private long fireAtMs;
    private double targetX;
    private double targetY;
    private double targetZ;

    public ElitePendingAbilityComponent() {
    }

    public ElitePendingAbilityComponent(@Nonnull EliteAbility ability, long fireAtMs,
                                        double targetX, double targetY, double targetZ) {
        this.ability = ability;
        this.fireAtMs = fireAtMs;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    @Nullable
    public EliteAbility getAbility() {
        return ability;
    }

    public long getFireAtMs() {
        return fireAtMs;
    }

    @Nonnull
    public org.joml.Vector3d getTarget() {
        return new org.joml.Vector3d(targetX, targetY, targetZ);
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new ElitePendingAbilityComponent(ability, fireAtMs, targetX, targetY, targetZ);
    }

    @Nonnull
    public static ComponentType<EntityStore, ElitePendingAbilityComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, ElitePendingAbilityComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
