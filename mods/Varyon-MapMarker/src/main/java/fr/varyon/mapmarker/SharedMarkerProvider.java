package fr.varyon.mapmarker;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerBuilder;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import fr.varyon.mapmarker.assets.MapMarkerAssetPack;
import javax.annotation.Nonnull;

final class SharedMarkerProvider implements WorldMapManager.MarkerProvider {

    static final String PROVIDER_KEY = "varyon-mapmarker";
    private static final double MARKER_HEIGHT = 100.0;

    private final VaryonMapMarkerPlugin plugin;

    SharedMarkerProvider(@Nonnull VaryonMapMarkerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        for (MarkerEntry entry : plugin.listMarkersInWorld(world)) {
            MapMarker marker = new MapMarkerBuilder(
                    entry.id(),
                    MapMarkerAssetPack.normalizeIconFileName(entry.imageName()),
                    new Transform(entry.x(), MARKER_HEIGHT, entry.z()))
                    .withCustomName(entry.markerName())
                    .build();
            collector.addIgnoreViewDistance(marker);
        }
    }
}
