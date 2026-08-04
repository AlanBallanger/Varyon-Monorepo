package com.varyon.easyhunger.utils;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Utility class for detecting player sleep state.
 */
public class SleepUtils {
    
    /**
     * Check if a player is currently sleeping (in bed, not awake).
     * 
     * @param index The entity index in the archetype chunk
     * @param archetypeChunk The archetype chunk containing the entity
     * @return true if player is sleeping, false if awake or component not found
     */
    public static boolean isSleeping(int index, ArchetypeChunk<EntityStore> archetypeChunk) {
        PlayerSomnolence somnolence = archetypeChunk.getComponent(index, PlayerSomnolence.getComponentType());
        if (somnolence == null) {
            return false;
        }
        // If state is not AWAKE, player is sleeping (NoddingOff, Slumber, or MorningWakeUp)
        return somnolence != PlayerSomnolence.AWAKE;
    }
}
