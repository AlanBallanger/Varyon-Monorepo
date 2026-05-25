package com.varyon.map;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapLoadException;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;

public class ZoneWorldMapProvider implements IWorldMapProvider {
    public static final String ID = "Varyon";
    public static final BuilderCodec<ZoneWorldMapProvider> CODEC = BuilderCodec.builder(ZoneWorldMapProvider.class, ZoneWorldMapProvider::new).build();

    @Override
    public IWorldMap getGenerator(World world) throws WorldMapLoadException {
        return ZoneWorldMap.INSTANCE;
    }
}
