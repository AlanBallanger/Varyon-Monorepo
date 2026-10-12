package com.varyon.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Component that stores the scaling multipliers for a scaled NPC.
 * These are calculated once at spawn based on distance from world origin.
 *
 * <p>Registered with a {@link #CODEC} so it survives a server restart: without persistence a
 * scaled mob loaded after a reboot keeps its baked {@code Varyon_Health} stat modifier (HP /
 * damage of the original level) but loses this component, so its nameplate falls back to the
 * zone tier and kill XP resolves it as level 0.
 */
public class MobScalingComponent implements Component<EntityStore> {
    public static final String COMPONENT_NAME = "varyon:mob_scaling";

    public static final BuilderCodec<MobScalingComponent> CODEC =
            BuilderCodec.builder(MobScalingComponent.class, MobScalingComponent::new)
                    .append(new KeyedCodec<>("MobLevel", Codec.INTEGER),
                            (c, v) -> c.mobLevel = v, c -> c.mobLevel).add()
                    .append(new KeyedCodec<>("HealthMultiplier", Codec.FLOAT),
                            (c, v) -> c.healthMultiplier = v, c -> c.healthMultiplier).add()
                    .append(new KeyedCodec<>("DamageMultiplier", Codec.FLOAT),
                            (c, v) -> c.damageMultiplier = v, c -> c.damageMultiplier).add()
                    .append(new KeyedCodec<>("LootMultiplier", Codec.FLOAT),
                            (c, v) -> c.lootMultiplier = v, c -> c.lootMultiplier).add()
                    .append(new KeyedCodec<>("PointsMultiplier", Codec.FLOAT),
                            (c, v) -> c.pointsMultiplier = v, c -> c.pointsMultiplier).add()
                    .append(new KeyedCodec<>("BonusLootDropped", Codec.BOOLEAN),
                            (c, v) -> c.bonusLootDropped = v, c -> c.bonusLootDropped).add()
                    .build();

    private static ComponentType<EntityStore, MobScalingComponent> COMPONENT_TYPE;

    private int mobLevel;
    private float healthMultiplier = 1.0f;
    private float damageMultiplier = 1.0f;
    private float lootMultiplier = 1.0f;
    private float pointsMultiplier = 1.0f;
    /** Set once by MobLootScalingSystem after it has spawned the extra loot sets, so it never fires twice. */
    private boolean bonusLootDropped;

    /** No-arg constructor for codec deserialization. */
    public MobScalingComponent() {
    }

    public MobScalingComponent(int mobLevel, float healthMultiplier, float damageMultiplier, float lootMultiplier, float pointsMultiplier) {
        this.mobLevel = mobLevel;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.lootMultiplier = lootMultiplier;
        this.pointsMultiplier = pointsMultiplier;
    }

    public boolean isBonusLootDropped() {
        return bonusLootDropped;
    }

    public void setBonusLootDropped() {
        this.bonusLootDropped = true;
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

    public float getPointsMultiplier() {
        return pointsMultiplier;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        MobScalingComponent copy = new MobScalingComponent(mobLevel, healthMultiplier, damageMultiplier, lootMultiplier, pointsMultiplier);
        copy.bonusLootDropped = this.bonusLootDropped;
        return copy;
    }

    @Nonnull
    public static ComponentType<EntityStore, MobScalingComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, MobScalingComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
