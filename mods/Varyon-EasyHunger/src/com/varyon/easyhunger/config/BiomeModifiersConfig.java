package com.varyon.easyhunger.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for biome-based hunger and thirst modifiers.
 * Allows customizing how different biomes affect hunger/thirst drain rates.
 */
public class BiomeModifiersConfig {
    
    // Codec definitions
    private static final KeyedCodec<Boolean> ENABLED = new KeyedCodec<>("Enabled", Codec.BOOLEAN);
    private static final KeyedCodec<Float> DEFAULT_HUNGER_MULTIPLIER = new KeyedCodec<>("DefaultHungerMultiplier", Codec.FLOAT);
    private static final KeyedCodec<Float> DEFAULT_THIRST_MULTIPLIER = new KeyedCodec<>("DefaultThirstMultiplier", Codec.FLOAT);
    private static final KeyedCodec<Map<String, Float>> HUNGER_MODIFIERS = new KeyedCodec<>("HungerModifiers", new MapCodec<>(Codec.FLOAT, HashMap::new));
    private static final KeyedCodec<Map<String, Float>> THIRST_MODIFIERS = new KeyedCodec<>("ThirstModifiers", new MapCodec<>(Codec.FLOAT, HashMap::new));
    
    public static final BuilderCodec<BiomeModifiersConfig> CODEC = BuilderCodec.builder(BiomeModifiersConfig.class, BiomeModifiersConfig::new)
            .addField(ENABLED, (c, v) -> c.enabled = v, BiomeModifiersConfig::isEnabled)
            .addField(DEFAULT_HUNGER_MULTIPLIER, (c, v) -> c.defaultHungerMultiplier = v, BiomeModifiersConfig::getDefaultHungerMultiplier)
            .addField(DEFAULT_THIRST_MULTIPLIER, (c, v) -> c.defaultThirstMultiplier = v, BiomeModifiersConfig::getDefaultThirstMultiplier)
            .addField(HUNGER_MODIFIERS, (c, v) -> c.hungerModifiers = v, BiomeModifiersConfig::getHungerModifiers)
            .addField(THIRST_MODIFIERS, (c, v) -> c.thirstModifiers = v, BiomeModifiersConfig::getThirstModifiers)
            .build();
    
    // Fields with defaults
    private boolean enabled = true;
    private float defaultHungerMultiplier = 1.0f;
    private float defaultThirstMultiplier = 1.0f;
    private Map<String, Float> hungerModifiers;
    private Map<String, Float> thirstModifiers;
    
    public BiomeModifiersConfig() {
        // Default hunger modifiers - Keywords match partial biome names
        // Cold = more hunger, Hot = normal hunger
        hungerModifiers = new HashMap<>();
        // Cold biomes (more hunger to stay warm)
        hungerModifiers.put("Frozen", 1.5f);       // Valley_Forest_Frozen, Lake_Forest_Frozen, etc
        hungerModifiers.put("Glacier", 1.6f);      // Glacier, Valley_Glacier, Lake_Glacier
        hungerModifiers.put("Tundra", 1.4f);       // Valley_Forest_Tundra, Cliffs_Tundra
        hungerModifiers.put("Cold", 1.3f);         // Cold_Kelp, Cold_Reef, Cold_Trench, Island_Cold
        // Normal biomes
        hungerModifiers.put("Forest", 1.0f);       // All forest variants
        hungerModifiers.put("Plains", 1.0f);       // All plains variants
        hungerModifiers.put("River", 1.0f);        // All river variants
        hungerModifiers.put("Lake", 1.0f);         // All lake variants
        hungerModifiers.put("Desert", 1.0f);       // All desert variants
        hungerModifiers.put("Savannah", 1.0f);     // Plateau_Savannah_*
        hungerModifiers.put("Scrub", 1.0f);        // Scrub_Tar_Pits
        // Hot biomes (less hunger needed)
        hungerModifiers.put("Lava", 0.9f);         // *_Lava, Volcano_*
        hungerModifiers.put("Volcano", 0.9f);      // Volcano_Wastes_Lava
        hungerModifiers.put("Wastes", 1.0f);       // All wastes variants
        
        // Default thirst modifiers - Keywords match partial biome names
        // Hot = more thirst, Cold = less thirst, Water nearby = less thirst
        thirstModifiers = new HashMap<>();
        // Hot/Dry biomes (more thirst)
        thirstModifiers.put("Desert", 2.0f);       // Desert_*, Dunes_Desert_*, Plateau_Desert_*
        thirstModifiers.put("Dunes", 2.0f);        // Dunes_Desert_*
        thirstModifiers.put("Lava", 2.0f);         // *_Lava variants (extremely hot)
        thirstModifiers.put("Volcano", 1.8f);      // Volcano_Wastes_Lava
        thirstModifiers.put("Wastes", 1.5f);       // Canyon_Wastes_*, Mountain_Wastes_*
        thirstModifiers.put("Caldera", 1.8f);      // Caldera_Forest_Ghost, Caldera_Wastes_*
        thirstModifiers.put("Savannah", 1.4f);     // Plateau_Savannah_* (dry grasslands)
        thirstModifiers.put("Scrub", 1.3f);        // Scrub_Tar_Pits (arid)
        thirstModifiers.put("Ash", 1.4f);          // *_Ash variants (dry volcanic)
        thirstModifiers.put("Burned", 1.3f);       // Canyon_Forest_Burned (dry)
        // Normal biomes
        thirstModifiers.put("Forest", 1.0f);       // All forest variants
        thirstModifiers.put("Plains", 1.0f);       // All plains variants
        thirstModifiers.put("Mountain", 1.1f);     // Mountain_* (drier at altitude)
        thirstModifiers.put("Canyon", 1.2f);       // Canyon_* (dry canyons)
        thirstModifiers.put("Plateau", 1.1f);      // Plateau_* (except desert)
        // Cold/Wet biomes (less thirst)
        thirstModifiers.put("Frozen", 0.7f);       // Cold = less thirst
        thirstModifiers.put("Glacier", 0.6f);      // Ice = less thirst
        thirstModifiers.put("Tundra", 0.8f);       // Cold tundra
        thirstModifiers.put("Cold", 0.8f);         // Cold_* ocean variants
        thirstModifiers.put("Swamp", 0.7f);        // Canyon_Forest_Swamp, Lake_Swamp (wet)
        // Water biomes (much less thirst)
        thirstModifiers.put("River", 0.6f);        // River_* (near fresh water)
        thirstModifiers.put("Lake", 0.6f);         // Lake_* (near fresh water)
        thirstModifiers.put("Ocean", 0.8f);        // Ocean biomes (salty but cool)
        thirstModifiers.put("Kelp", 0.8f);         // *_Kelp (underwater)
        thirstModifiers.put("Reef", 0.8f);         // *_Reef (underwater)
        thirstModifiers.put("Trench", 0.8f);       // *_Trench (deep water)
        thirstModifiers.put("Island", 0.9f);       // Island_* (near water)
        thirstModifiers.put("Oasis", 0.5f);        // *_Oasis (water in desert!)
        thirstModifiers.put("Hotsprings", 0.7f);   // Desert_Hotsprings, *_Hotsprings (water)
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public float getDefaultHungerMultiplier() { return defaultHungerMultiplier; }
    public float getDefaultThirstMultiplier() { return defaultThirstMultiplier; }
    public Map<String, Float> getHungerModifiers() { return hungerModifiers; }
    public Map<String, Float> getThirstModifiers() { return thirstModifiers; }
    
    /**
     * Get hunger multiplier for a specific biome.
     * @param biomeName Name of the biome
     * @return Multiplier (1.0 = normal, 2.0 = double drain, 0.5 = half drain)
     */
    public float getHungerMultiplier(String biomeName) {
        if (!enabled || biomeName == null) return 1.0f;
        
        // Check for exact match first
        Float modifier = hungerModifiers.get(biomeName);
        if (modifier != null) return modifier;
        
        // Check for partial match (e.g., "Snow_Forest" contains "Snow")
        for (Map.Entry<String, Float> entry : hungerModifiers.entrySet()) {
            if (biomeName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        return defaultHungerMultiplier;
    }
    
    /**
     * Get thirst multiplier for a specific biome.
     * @param biomeName Name of the biome
     * @return Multiplier (1.0 = normal, 2.0 = double drain, 0.5 = half drain)
     */
    public float getThirstMultiplier(String biomeName) {
        if (!enabled || biomeName == null) return 1.0f;
        
        // Check for exact match first
        Float modifier = thirstModifiers.get(biomeName);
        if (modifier != null) return modifier;
        
        // Check for partial match
        for (Map.Entry<String, Float> entry : thirstModifiers.entrySet()) {
            if (biomeName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        return defaultThirstMultiplier;
    }
}
