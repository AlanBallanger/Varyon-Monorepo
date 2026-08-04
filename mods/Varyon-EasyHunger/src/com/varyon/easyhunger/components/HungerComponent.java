package com.varyon.easyhunger.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.EasyHunger;

import javax.annotation.Nullable;

public class HungerComponent implements Component<EntityStore> {
    public static final BuilderCodec<HungerComponent> CODEC = BuilderCodec.builder(HungerComponent.class, HungerComponent::new)
            .append(new KeyedCodec<>("HungerLevel", Codec.FLOAT),
                    HungerComponent::setHungerLevel,
                    HungerComponent::getHungerLevel).add()
            .build();

    private float elapsedTime = 0.0f;
    private float lowestStaminaSeen = 10.0f;
    private float hungerLevel;
    // Optimization: Track last sent level to avoid spamming HUD updates
    private float lastSentHunger = -1.0f;
    
    // Idle system tracking
    private float idleTime = 0.0f;

    public HungerComponent() {
        this.hungerLevel = EasyHunger.get().getConfig().getMaxHunger(); // Max hunger level from config
    }

    public HungerComponent (float hungerLevel) {
        this.hungerLevel = hungerLevel;
    }

    public HungerComponent (HungerComponent other) {
        this.hungerLevel = other.hungerLevel;
        this.elapsedTime = other.elapsedTime;
        this.lowestStaminaSeen = other.lowestStaminaSeen;
        this.idleTime = other.idleTime;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new HungerComponent(this);
    }

    public float getElapsedTime () {
        return this.elapsedTime;
    }
    public void addElapsedTime (float deltaTime) {
        this.elapsedTime += deltaTime;
    }
    public void resetElapsedTime () {
        this.elapsedTime = 0.0f;
    }

    public float getAndResetLowestStaminaSeen() {
        float lowestStaminaSeen = this.lowestStaminaSeen;
        this.lowestStaminaSeen = 10.0f;
        return lowestStaminaSeen;
    }
    public void setStaminaSeen(float stamina) {
        if (stamina > this.lowestStaminaSeen) return;
        this.lowestStaminaSeen = stamina;
    }

    public float getLastSentHunger() { return lastSentHunger; }
    public void setLastSentHunger(float v) { this.lastSentHunger = v; }

    // WellFed system elapsed time tracking
    private float wellFedElapsedTime = 0.0f;
    public float getWellFedElapsedTime() { return wellFedElapsedTime; }
    public void addWellFedElapsedTime(float dt) { wellFedElapsedTime += dt; }
    public void resetWellFedElapsedTime() { wellFedElapsedTime = 0.0f; }

    // Idle time tracking
    public float getIdleTime() { return idleTime; }
    public void addIdleTime(float dt) { idleTime += dt; }
    public void resetIdleTime() { idleTime = 0.0f; }

    public float getHungerLevel () {
        return this.hungerLevel;
    }
    public void setHungerLevel (float hungerLevel) {
        float max = EasyHunger.get().getConfig().getMaxHunger();
        this.hungerLevel = Math.max(0.0f, Math.min(hungerLevel, max));
    }
    public void feed (float amount) {
        float max = EasyHunger.get().getConfig().getMaxHunger();
        this.hungerLevel = Math.min(this.hungerLevel + amount, max);
    }
    public void starve(float amount) {
        this.hungerLevel = Math.max(this.hungerLevel - amount, 0.0f);
    }

    public static ComponentType<EntityStore, HungerComponent> getComponentType() {
        return EasyHunger.get().getHungerComponentType();
    }
}



