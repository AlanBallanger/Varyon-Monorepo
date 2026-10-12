package com.varyon.elite.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.elite.ability.EliteAbility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class EliteAbilityStateComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, EliteAbilityStateComponent> COMPONENT_TYPE;

    private final Map<EliteAbility, Long> cooldownUntil = new EnumMap<>(EliteAbility.class);
    private long animationLockUntil;
    private long globalCooldownUntil;

    public boolean isAnimationLocked(long now) {
        return now < animationLockUntil;
    }

    public void lockAnimation(long until) {
        this.animationLockUntil = until;
    }

    /** True while the post-ability global pause is in effect (no ability may start). */
    public boolean isGlobalCooldown(long now) {
        return now < globalCooldownUntil;
    }

    public void startGlobalCooldown(long until) {
        this.globalCooldownUntil = Math.max(this.globalCooldownUntil, until);
    }

    public boolean isReady(@Nonnull EliteAbility ability, long now) {
        Long until = cooldownUntil.get(ability);
        return until == null || now >= until;
    }

    public void putOnCooldown(@Nonnull EliteAbility ability, long until) {
        cooldownUntil.put(ability, until);
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        EliteAbilityStateComponent copy = new EliteAbilityStateComponent();
        copy.cooldownUntil.putAll(this.cooldownUntil);
        copy.animationLockUntil = this.animationLockUntil;
        copy.globalCooldownUntil = this.globalCooldownUntil;
        return copy;
    }

    @Nonnull
    public static ComponentType<EntityStore, EliteAbilityStateComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, EliteAbilityStateComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
