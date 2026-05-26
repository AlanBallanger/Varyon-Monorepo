package com.varyon.extraction;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import com.hypixel.hytale.server.core.util.PositionUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ExtractionPortalMarkerProvider implements WorldMapManager.MarkerProvider {

    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();
        if (manager == null) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
        if (playerRef == null) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();

        ExtractionPortalManager.PortalData portalData = manager.getActivePortals().get(playerUuid);
        if (portalData == null) {
            return;
        }

        if (portalData.world() != world) {
            return;
        }

        org.joml.Vector3d position = new org.joml.Vector3d(portalData.x() + 0.5, portalData.y(), portalData.z() + 0.5);

        FormattedMessage name = new FormattedMessage();
        name.rawText = "Extraction Portal";

        MapMarker marker = new MapMarker(
            "extraction_portal_" + playerUuid,
            name,
            "Portal.png",
            PositionUtil.toTransformPacket(new Transform(position)),
            null,
            null
        );

        collector.addIgnoreViewDistance(marker);
    }
}
