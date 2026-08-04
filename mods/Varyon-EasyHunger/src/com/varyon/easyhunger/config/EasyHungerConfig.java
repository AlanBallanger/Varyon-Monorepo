package com.varyon.easyhunger.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

public class EasyHungerConfig {
    
    // Original fields codecs
    private static final KeyedCodec<Integer> MAX_HUNGER = new KeyedCodec<>("MaxHunger", Codec.INTEGER);
    private static final KeyedCodec<Float> STARVATION_TICK_RATE = new KeyedCodec<>("StarvationTickRate", Codec.FLOAT);
    private static final KeyedCodec<Float> STARVATION_PER_TICK = new KeyedCodec<>("StarvationPerTick", Codec.FLOAT);
    private static final KeyedCodec<Float> STARVATION_STAMINA_MODIFIER = new KeyedCodec<>("StarvationStaminaModifier", Codec.FLOAT);
    private static final KeyedCodec<Float> HUNGRY_THRESHOLD = new KeyedCodec<>("HungryThreshold", Codec.FLOAT);
    private static final KeyedCodec<Float> STARVATION_DAMAGE = new KeyedCodec<>("StarvationDamage", Codec.FLOAT);


    private static final KeyedCodec<String> HUD_POSITION = new KeyedCodec<>("HudPosition", Codec.STRING);
    private static final KeyedCodec<Float> BLOCK_BREAK_HUNGER_COST = new KeyedCodec<>("BlockBreakHungerCost", Codec.FLOAT);
    private static final KeyedCodec<Float> JUMP_HUNGER_COST = new KeyedCodec<>("JumpHungerCost", Codec.FLOAT);
    
    // Thirst Configuration
    private static final KeyedCodec<Boolean> THIRST_ENABLED = new KeyedCodec<>("ThirstEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Integer> MAX_THIRST = new KeyedCodec<>("MaxThirst", Codec.INTEGER);
    private static final KeyedCodec<Float> THIRST_DECAY_RATE = new KeyedCodec<>("ThirstDecayRate", Codec.FLOAT);
    private static final KeyedCodec<Float> SPRINT_THIRST_MULTIPLIER = new KeyedCodec<>("SprintThirstMultiplier", Codec.FLOAT);
    private static final KeyedCodec<Float> THIRSTY_THRESHOLD = new KeyedCodec<>("ThirstyThreshold", Codec.FLOAT);


    private static final KeyedCodec<Float> THIRST_DAMAGE = new KeyedCodec<>("ThirstDamage", Codec.FLOAT);
    private static final KeyedCodec<Boolean> PAUSE_WHILE_SLEEPING = new KeyedCodec<>("PauseWhileSleeping", Codec.BOOLEAN);

    // Well Fed Configuration
    private static final KeyedCodec<Boolean> WELL_FED_ENABLED = new KeyedCodec<>("WellFedEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Float> WELL_FED_THRESHOLD = new KeyedCodec<>("WellFedThreshold", Codec.FLOAT);

    // Idle Pause Configuration
    private static final KeyedCodec<Boolean> IDLE_PAUSE_ENABLED = new KeyedCodec<>("IdlePauseEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Float> IDLE_PAUSE_SECONDS = new KeyedCodec<>("IdlePauseSeconds", Codec.FLOAT);


    public static final BuilderCodec<EasyHungerConfig> CODEC = BuilderCodec.builder(EasyHungerConfig.class, EasyHungerConfig::new)
            // === HUNGER / FOOD CONFIGS ===
            .addField(MAX_HUNGER, (c, v) -> c.maxHunger = v, EasyHungerConfig::getMaxHunger)
            .addField(STARVATION_TICK_RATE, (c, v) -> c.starvationTickRate = v, EasyHungerConfig::getStarvationTickRate)
            .addField(STARVATION_PER_TICK, (c, v) -> c.starvationPerTick = v, EasyHungerConfig::getStarvationPerTick)
            .addField(STARVATION_STAMINA_MODIFIER, (c, v) -> c.starvationStaminaModifier = v, EasyHungerConfig::getStarvationStaminaModifier)
            .addField(HUNGRY_THRESHOLD, (c, v) -> c.hungryThreshold = v, EasyHungerConfig::getHungryThreshold)
            .addField(STARVATION_DAMAGE, (c, v) -> c.starvationDamage = v, EasyHungerConfig::getStarvationDamage)
            .addField(BLOCK_BREAK_HUNGER_COST, (c, v) -> c.blockBreakHungerCost = v, EasyHungerConfig::getBlockBreakHungerCost)
            .addField(JUMP_HUNGER_COST, (c, v) -> c.jumpHungerCost = v, EasyHungerConfig::getJumpHungerCost)
            
            // === THIRST / WATER CONFIGS ===
            .addField(THIRST_ENABLED, (c, v) -> c.thirstEnabled = v, EasyHungerConfig::isThirstEnabled)
            .addField(MAX_THIRST, (c, v) -> c.maxThirst = v, EasyHungerConfig::getMaxThirst)
            .addField(THIRST_DECAY_RATE, (c, v) -> c.thirstDecayRate = v, EasyHungerConfig::getThirstDecayRate)
            .addField(SPRINT_THIRST_MULTIPLIER, (c, v) -> c.sprintThirstMultiplier = v, EasyHungerConfig::getSprintThirstMultiplier)
            .addField(THIRST_DAMAGE, (c, v) -> c.thirstDamage = v, EasyHungerConfig::getThirstDamage)
            .addField(THIRSTY_THRESHOLD, (c, v) -> c.thirstyThreshold = v, EasyHungerConfig::getThirstyThreshold)
            
            // === HUD POSITION ===
            .addField(HUD_POSITION, (c, v) -> c.hudPosition = HudPosition.valueOf(v), c -> c.getHudPosition().name())

            
            // === SLEEP PAUSE ===
            .addField(PAUSE_WHILE_SLEEPING, (c, v) -> c.pauseWhileSleeping = v, EasyHungerConfig::isPauseWhileSleeping)
            
            // === WELL FED BONUS ===
            .addField(WELL_FED_ENABLED, (c, v) -> c.wellFedEnabled = v, EasyHungerConfig::isWellFedEnabled)
            .addField(WELL_FED_THRESHOLD, (c, v) -> c.wellFedThreshold = v, EasyHungerConfig::getWellFedThreshold)
            
            // === IDLE PAUSE ===
            .addField(IDLE_PAUSE_ENABLED, (c, v) -> c.idlePauseEnabled = v, EasyHungerConfig::isIdlePauseEnabled)
            .addField(IDLE_PAUSE_SECONDS, (c, v) -> c.idlePauseSeconds = v, EasyHungerConfig::getIdlePauseSeconds)
            .build();

    private Integer maxHunger = 50;
    private float starvationTickRate = 2f;
    private float starvationPerTick = 0.04f;

    public Integer getMaxHunger() { return maxHunger; }
    private float starvationStaminaModifier = 0.177f;
    private float hungryThreshold = 20.0f;
    private float starvationDamage = 5.0f;
    private float thirstDamage = 5.0f;

    private HudPosition hudPosition = HudPosition.BOTTOM;
    private float blockBreakHungerCost = 0.005f;
    private float jumpHungerCost = 0.01f;

    // Thirst settings
    private boolean thirstEnabled = true; // Enable/disable thirst system
    private Integer maxThirst = 50; // Match default hunger for symmetry
    private float thirstDecayRate = 0.05f; // Slower than hunger? Or same.
    private float sprintThirstMultiplier = 1.5f;
    private float thirstyThreshold = 20.0f; // Same as hungryThreshold
    private boolean pauseWhileSleeping = true; // Pause hunger/thirst while sleeping

    // Well Fed settings
    private boolean wellFedEnabled = true;
    private float wellFedThreshold = 35.0f; // Absolute value (like HungryThreshold)

    // Idle Pause settings
    private boolean idlePauseEnabled = true;
    private float idlePauseSeconds = 60.0f;


    public EasyHungerConfig() {
    }

    public float getStarvationTickRate() {
        return starvationTickRate;
    }
    public float getStarvationPerTick() {
        return starvationPerTick;
    }
    public float getStarvationStaminaModifier() {
        return starvationStaminaModifier;
    }
    public float getHungryThreshold() {
        return hungryThreshold;
    }
    public float getStarvationDamage() {
        return starvationDamage;
    }
    public float getThirstDamage() { return thirstDamage; }

    public HudPosition getHudPosition() {
        return hudPosition;
    }
    public float getBlockBreakHungerCost() {
        return blockBreakHungerCost;
    }
    public float getJumpHungerCost() {
        return jumpHungerCost;
    }
    
    public boolean isThirstEnabled() { return thirstEnabled; }
    public Integer getMaxThirst() { return maxThirst; }
    public float getThirstDecayRate() { return thirstDecayRate; }
    public float getSprintThirstMultiplier() { return sprintThirstMultiplier; }
    public float getThirstyThreshold() { return thirstyThreshold; }
    public boolean isPauseWhileSleeping() { return pauseWhileSleeping; }
    
    // Well Fed getters
    public boolean isWellFedEnabled() { return wellFedEnabled; }
    public float getWellFedThreshold() { return wellFedThreshold; }

    // Idle Pause getters
    public boolean isIdlePauseEnabled() { return idlePauseEnabled; }
    public float getIdlePauseSeconds() { return idlePauseSeconds; }

    // ===================== SETTERS =====================
    
    // Hunger setters
    public void setMaxHunger(int value) { this.maxHunger = value; }
    public void setStarvationTickRate(float value) { this.starvationTickRate = value; }
    public void setStarvationPerTick(float value) { this.starvationPerTick = value; }
    public void setStarvationStaminaModifier(float value) { this.starvationStaminaModifier = value; }
    public void setHungryThreshold(float value) { this.hungryThreshold = value; }
    public void setStarvationDamage(float value) { this.starvationDamage = value; }
    public void setBlockBreakHungerCost(float value) { this.blockBreakHungerCost = value; }
    public void setJumpHungerCost(float value) { this.jumpHungerCost = value; }
    
    // Thirst setters
    public void setThirstEnabled(boolean value) { this.thirstEnabled = value; }
    public void setMaxThirst(int value) { this.maxThirst = value; }
    public void setThirstDecayRate(float value) { this.thirstDecayRate = value; }
    public void setSprintThirstMultiplier(float value) { this.sprintThirstMultiplier = value; }
    public void setThirstDamage(float value) { this.thirstDamage = value; }
    public void setThirstyThreshold(float value) { this.thirstyThreshold = value; }
    
    // Other setters
    public void setHudPosition(HudPosition value) { this.hudPosition = value; }
    public void setPauseWhileSleeping(boolean value) { this.pauseWhileSleeping = value; }
    public void setWellFedEnabled(boolean value) { this.wellFedEnabled = value; }
    public void setWellFedThreshold(float value) { this.wellFedThreshold = value; }

    // Idle Pause setters
    public void setIdlePauseEnabled(boolean value) { this.idlePauseEnabled = value; }
    public void setIdlePauseSeconds(float value) { this.idlePauseSeconds = value; }

}



