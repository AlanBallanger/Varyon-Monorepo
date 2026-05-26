package com.varyon.deposit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DepositBlockManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LOCATIONS_FILE = "deposit_blocks.json";
    
    private final Path dataDirectory;
    private final Map<String, Set<BlockLocation>> depositBlocks = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static class BlockLocation {
        private final String world;
        private final int x;
        private final int y;
        private final int z;
        
        public BlockLocation(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public String getWorld() {
            return world;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getZ() {
            return z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BlockLocation)) return false;
            BlockLocation other = (BlockLocation) obj;
            return world.equals(other.world) && x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }
        
        public String toKey() {
            return world + ":" + x + ":" + y + ":" + z;
        }
    }
    
    public DepositBlockManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        load();
    }
    
    public boolean addDepositBlock(@Nonnull String world, @Nonnull Vector3i pos) {
        BlockLocation location = new BlockLocation(world, pos.x, pos.y, pos.z);
        Set<BlockLocation> worldBlocks = depositBlocks.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet());
        
        if (worldBlocks.add(location)) {
            save();
            LOGGER.at(Level.INFO).log("Added deposit block at " + location.toKey());
            return true;
        }
        return false;
    }
    
    public boolean removeDepositBlock(@Nonnull String world, @Nonnull Vector3i pos) {
        BlockLocation location = new BlockLocation(world, pos.x, pos.y, pos.z);
        Set<BlockLocation> worldBlocks = depositBlocks.get(world);
        
        if (worldBlocks != null && worldBlocks.remove(location)) {
            if (worldBlocks.isEmpty()) {
                depositBlocks.remove(world);
            }
            save();
            LOGGER.at(Level.INFO).log("Removed deposit block at " + location.toKey());
            return true;
        }
        return false;
    }
    
    public boolean isDepositBlock(@Nonnull String world, @Nonnull Vector3i pos) {
        Set<BlockLocation> worldBlocks = depositBlocks.get(world);
        if (worldBlocks == null) {
            return false;
        }
        BlockLocation location = new BlockLocation(world, pos.x, pos.y, pos.z);
        return worldBlocks.contains(location);
    }
    
    public Set<BlockLocation> getDepositBlocks(@Nonnull String world) {
        return depositBlocks.getOrDefault(world, Collections.emptySet());
    }
    
    public int clearAll() {
        int total = depositBlocks.values().stream().mapToInt(Set::size).sum();
        depositBlocks.clear();
        save();
        LOGGER.at(Level.INFO).log("Cleared all deposit blocks (" + total + " total)");
        return total;
    }
    
    private void load() {
        File file = dataDirectory.resolve(LOCATIONS_FILE).toFile();
        if (!file.exists()) {
            LOGGER.at(Level.INFO).log("No deposit blocks file found, starting fresh");
            return;
        }
        
        try (Reader reader = new FileReader(file)) {
            Map<String, Set<BlockLocation>> loaded = gson.fromJson(reader, 
                new TypeToken<Map<String, Set<BlockLocation>>>(){}.getType());
            
            if (loaded != null) {
                depositBlocks.clear();
                depositBlocks.putAll(loaded);
                
                int total = depositBlocks.values().stream().mapToInt(Set::size).sum();
                LOGGER.at(Level.INFO).log("Loaded " + total + " deposit block(s) from " + depositBlocks.size() + " world(s)");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load deposit blocks: " + e.getMessage());
        }
    }
    
    private void save() {
        try {
            dataDirectory.toFile().mkdirs();
            File file = dataDirectory.resolve(LOCATIONS_FILE).toFile();
            
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(depositBlocks, writer);
                LOGGER.at(Level.FINE).log("Saved deposit blocks configuration");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to save deposit blocks: " + e.getMessage());
        }
    }
}
