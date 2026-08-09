package com.varyon.easyhunger.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.EasyHunger;

import javax.annotation.Nullable;

public class ThirstComponent implements Component<EntityStore> {
    public static final BuilderCodec<ThirstComponent> CODEC = BuilderCodec.builder(ThirstComponent.class, ThirstComponent::new)
            .append(new KeyedCodec<>("ThirstLevel", Codec.FLOAT),
                    ThirstComponent::setThirstLevel,
                    ThirstComponent::getThirstLevel).add()
            .build();

    private float thirstLevel;
    private float elapsedTime = 0.0f;
    private float lastSentThirst = -1.0f;
    private float timeSinceHudResync = 0.0f;
    private float idleTime = 0.0f;

    public ThirstComponent() {
        this.thirstLevel = EasyHunger.get().getConfig().getMaxThirst(); // Init with Max
    }

    public ThirstComponent (float thirstLevel) {
        this.thirstLevel = thirstLevel;
    }

    public ThirstComponent (ThirstComponent other) {
        this.thirstLevel = other.thirstLevel;
        this.elapsedTime = other.elapsedTime;
        this.idleTime = other.idleTime;
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

    public float getIdleTime() { return idleTime; }
    public void addIdleTime(float dt) { idleTime += dt; }
    public void resetIdleTime() { idleTime = 0.0f; }

    public float getLastSentThirst() { return lastSentThirst; }
    public void setLastSentThirst(float v) { this.lastSentThirst = v; }

    public float getTimeSinceHudResync() { return timeSinceHudResync; }
    public void addTimeSinceHudResync(float dt) { timeSinceHudResync += dt; }
    public void resetTimeSinceHudResync() { timeSinceHudResync = 0.0f; }
    
    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new ThirstComponent(this);
    }

    public float getThirstLevel () {
        return this.thirstLevel;
    }

    public void setThirstLevel (float level) {
        float max = EasyHunger.get().getConfig().getMaxThirst();
        this.thirstLevel = Math.max(0.0f, Math.min(level, max));
    }

    public void drink (float amount) {
        float max = EasyHunger.get().getConfig().getMaxThirst();
        this.thirstLevel = Math.min(this.thirstLevel + amount, max);
    }

    public void dehydrate(float amount) {
        this.thirstLevel = Math.max(this.thirstLevel - amount, 0.0f);
    }

    public static ComponentType<EntityStore, ThirstComponent> getComponentType() {
        return EasyHunger.get().getThirstComponentType();
    }
}
