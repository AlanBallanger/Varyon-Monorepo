package com.varyon.easyhunger.utils;

import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.component.Holder;

import javax.annotation.Nullable;

/**
 * Utility class for detecting biome at player's position.
 * Uses the same approach as BetterMap mod.
 */
public class BiomeUtils {
    
    /**
     * Get the biome name at the player's current position.
     * @param player The player entity
     * @param holder The holder containing the player's components
     * @return Biome name, or null if unable to detect
     */
    @Nullable
    public static String getPlayerBiomeName(Player player, Holder holder) {
        if (player == null || holder == null) return null;
        
        World world = player.getWorld();
        if (world == null) return null;
        
        TransformComponent transform = (TransformComponent) holder.getComponent(TransformComponent.getComponentType());
        if (transform == null) return null;
        
        Vector3d position = transform.getPosition();
        if (position == null) return null;
        
        return getBiomeNameAt(world, position);
    }
    
    /**
     * Get the biome name at a specific position in the world.
     * @param world The world
     * @param position The position to check
     * @return Biome name, or null if unable to detect
     */
    @Nullable
    public static String getBiomeNameAt(World world, Vector3d position) {
        if (world == null || position == null) return null;
        
        try {
            IWorldGen worldGen = world.getChunkStore().getGenerator();
            if (!(worldGen instanceof ChunkGenerator)) return null;
            
            ChunkGenerator generator = (ChunkGenerator) worldGen;
            int seed = (int) world.getWorldConfig().getSeed();
            int x = (int) position.x;
            int z = (int) position.z;

            ZoneBiomeResult result = generator.generateZoneBiomeResultAt(seed, x, z);
            if (result == null) return null;

            Biome biome = result.getBiome();
            if (biome == null) return null;

            return biome.getName();
        } catch (Exception e) {
            // Silently fail - biome detection is optional
            return null;
        }
    }

    /**
     * Get the zone name at a specific position in the world.
     * @param world The world
     * @param position The position to check
     * @return Zone name, or null if unable to detect
     */
    @Nullable
    public static String getZoneNameAt(World world, Vector3d position) {
        if (world == null || position == null) return null;

        try {
            IWorldGen worldGen = world.getChunkStore().getGenerator();
            if (!(worldGen instanceof ChunkGenerator)) return null;

            ChunkGenerator generator = (ChunkGenerator) worldGen;
            int seed = (int) world.getWorldConfig().getSeed();
            int x = (int) position.x;
            int z = (int) position.z;
            
            ZoneBiomeResult result = generator.generateZoneBiomeResultAt(seed, x, z);
            if (result == null || result.getZoneResult() == null) return null;
            
            return result.getZoneResult().getZone().name();
        } catch (Exception e) {
            return null;
        }
    }
}
