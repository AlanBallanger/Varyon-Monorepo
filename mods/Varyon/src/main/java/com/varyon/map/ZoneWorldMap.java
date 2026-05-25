package com.varyon.map;

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.map.WorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings;
import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ZoneWorldMap implements IWorldMap {
    public static final ZoneWorldMap INSTANCE = new ZoneWorldMap();

    @Override
    public WorldMapSettings getWorldMapSettings() {
        UpdateWorldMapSettings settingsPacket = new UpdateWorldMapSettings();
        settingsPacket.defaultScale = 128.0f;
        settingsPacket.minScale = 32.0f;
        settingsPacket.maxScale = 175.0f;
        return new WorldMapSettings(null, 3.0f, 1.0f, 16, 32, settingsPacket);
    }

    @Override
    public CompletableFuture<WorldMap> generate(World world, int imageWidth, int imageHeight, LongSet chunksToGenerate) {
        ConfigManager configManager = VaryonPlugin.getStaticConfigManager();

        // Safety check: if config not loaded yet, return empty map
        if (configManager == null) {
            return CompletableFuture.completedFuture(new WorldMap(0));
        }

        CompletableFuture<ZoneMapImageBuilder>[] futures = new CompletableFuture[chunksToGenerate.size()];
        int futureIndex = 0;
        LongIterator iterator = chunksToGenerate.iterator();

        while (iterator.hasNext()) {
            long chunkIndex = iterator.nextLong();
            futures[futureIndex++] = ZoneMapImageBuilder.build(chunkIndex, imageWidth, imageHeight, world, configManager);
        }

        return CompletableFuture.allOf(futures).thenApply(unused -> {
            WorldMap worldMap = new WorldMap(futures.length);
            for (int i = 0; i < futures.length; i++) {
                ZoneMapImageBuilder builder = futures[i].getNow(null);
                if (builder != null) {
                    worldMap.getChunks().put(builder.getIndex(), builder.getImage());
                }
            }
            return worldMap;
        });
    }

    @Override
    public CompletableFuture<Map<String, MapMarker>> generatePointsOfInterest(World world) {
        return CompletableFuture.completedFuture(new HashMap<>());
    }
}
