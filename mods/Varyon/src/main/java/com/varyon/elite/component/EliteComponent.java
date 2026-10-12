package com.varyon.elite.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.elite.MobClass;
import com.varyon.elite.ability.EliteAbility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EliteComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, EliteComponent> COMPONENT_TYPE;

    private final MobClass mobClass;
    private final int tier;
    private final long seed;
    private final List<EliteAbility> abilities;
    private final String namedId;
    private final double statBonus;

    private transient boolean enraged;
    private transient long scanCooldownUntil;
    private transient boolean registered;
    /** While set, a tick system keeps topping HP to max (covers Varyon zone scaling landing after us). */
    private transient long healToFullUntil;
    private final List<String> equipment = new ArrayList<>();
    private final List<Integer> effectIndices = new ArrayList<>();

    /** Rolled once at promotion from the tier's ScalingDef range. */
    private float damageMultiplier = 1.0f;
    private float sizeMultiplier = 1.0f;

    /** The NPC's model scale before any elite resizing, captured once. 0 = not captured yet. */
    private transient float baseModelScale = 0f;

    public EliteComponent(@Nonnull MobClass mobClass, int tier, long seed,
                          @Nonnull List<EliteAbility> abilities, @Nullable String namedId) {
        this(mobClass, tier, seed, abilities, namedId, 0.0);
    }

    public EliteComponent(@Nonnull MobClass mobClass, int tier, long seed,
                          @Nonnull List<EliteAbility> abilities, @Nullable String namedId, double statBonus) {
        this.mobClass = mobClass;
        this.tier = tier;
        this.seed = seed;
        this.abilities = new ArrayList<>(abilities);
        this.namedId = namedId;
        this.statBonus = statBonus;
    }

    public double getStatBonus() {
        return statBonus;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getSizeMultiplier() {
        return sizeMultiplier;
    }

    public void setRolledScaling(float damageMultiplier, float sizeMultiplier) {
        this.damageMultiplier = damageMultiplier;
        this.sizeMultiplier = sizeMultiplier;
    }

    public float getBaseModelScale() {
        return baseModelScale;
    }

    public void setBaseModelScale(float baseModelScale) {
        this.baseModelScale = baseModelScale;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public void setEnraged(boolean enraged) {
        this.enraged = enraged;
    }

    public boolean isScanReady(long now) {
        return now >= scanCooldownUntil;
    }

    public void markScanned(long until) {
        this.scanCooldownUntil = until;
    }

    public void armHealToFull(long until) {
        this.healToFullUntil = until;
    }

    public boolean isHealToFull(long now) {
        return now < healToFullUntil;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void markRegistered() {
        this.registered = true;
    }

    @Nonnull
    public List<String> getEquipment() {
        return equipment;
    }

    public void setEquipment(@Nonnull List<String> ids) {
        this.equipment.clear();
        this.equipment.addAll(ids);
    }

    @Nonnull
    public List<Integer> getEffectIndices() {
        return effectIndices;
    }

    public void setEffectIndices(@Nonnull List<Integer> indices) {
        this.effectIndices.clear();
        this.effectIndices.addAll(indices);
    }

    @Nonnull
    public MobClass getMobClass() {
        return mobClass;
    }

    public int getTier() {
        return tier;
    }

    public long getSeed() {
        return seed;
    }

    @Nonnull
    public List<EliteAbility> getAbilities() {
        return abilities;
    }

    @Nullable
    public String getNamedId() {
        return namedId;
    }

    public boolean isNamed() {
        return namedId != null;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        EliteComponent copy = new EliteComponent(mobClass, tier, seed, abilities, namedId, statBonus);
        copy.equipment.addAll(this.equipment);
        copy.effectIndices.addAll(this.effectIndices);
        copy.damageMultiplier = this.damageMultiplier;
        copy.sizeMultiplier = this.sizeMultiplier;
        return copy;
    }

    @Nonnull
    public static ComponentType<EntityStore, EliteComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, EliteComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
