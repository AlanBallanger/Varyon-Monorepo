package com.varyon.comet.services;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles comet world-map marker creation and updates.
 */
public class CometWaveUiService {

    public void addCometMapMarker(World world, Vector3i blockPos, CometTier tier, UUID ownerUUID, Logger logger) {
        try {
            if (world == null) {
                logger.warning("Cannot add map marker: world is null");
                return;
            }

            String markerId = markerId(blockPos);
            String markerName = "Comète (" + tier.getDisplayName() + ")";
            String iconPath = "Comet_Stone_" + tier.getAssetToken() + ".png";

            Vector3d markerPos = blockPos.toVector3d();
            Position position = new Position(markerPos.x, markerPos.y, markerPos.z);
            Direction direction = new Direction();
            Transform transform = new Transform(position, direction);

            FormattedMessage nameMsg = new FormattedMessage();
            nameMsg.rawText = markerName;

            MapMarker marker = CometMapMarkerUtil.create(markerId, nameMsg, iconPath, transform);

            CometConfig config = CometConfig.getInstance();
            boolean globalComets = (config != null && config.globalComets);
            sendMarkerToPlayers(world, marker, globalComets ? null : ownerUUID, logger);
        } catch (Throwable t) {
            logger.warning("Failed to add comet map marker to world " + (world != null ? world.getName() : "null")
                    + ": " + t.getMessage());
        }
    }

    public void removeCometMapMarker(World world, Vector3i blockPos, UUID ownerUUID, Logger logger) {
        try {
            if (world == null) {
                return;
            }

            String markerId = markerId(blockPos);
            world.getWorldMapManager().getPointsOfInterest().remove(markerId);

            CometConfig config = CometConfig.getInstance();
            boolean globalComets = (config != null && config.globalComets);

            String[] markersToRemove = new String[] { markerId };
            UpdateWorldMap updatePacket = new UpdateWorldMap(null, null, markersToRemove);

            for (PlayerRef pref : world.getPlayerRefs()) {
                try {
                    if (globalComets || ownerUUID == null || pref.getUuid().equals(ownerUUID)) {
                        pref.getPacketHandler().writeNoCache(updatePacket);
                    }
                } catch (Exception ignored) {
                    // Player disconnected or connection not ready.
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to remove comet map marker: " + e.getMessage());
        }
    }

    private void sendMarkerToPlayers(World world, MapMarker marker, UUID ownerUUID, Logger logger) {
        try {
            MapMarker[] markersToAdd = new MapMarker[] { marker };
            UpdateWorldMap updatePacket = new UpdateWorldMap(null, markersToAdd, null);

            for (PlayerRef pref : world.getPlayerRefs()) {
                try {
                    if (ownerUUID == null || pref.getUuid().equals(ownerUUID)) {
                        pref.getPacketHandler().writeNoCache(updatePacket);
                    }
                } catch (Exception ignored) {
                    // Player disconnected or connection not ready.
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to send comet marker packet: " + e.getMessage());
        }
    }

    private String markerId(Vector3i blockPos) {
        return "Comet-" + blockPos.x + "," + blockPos.y + "," + blockPos.z;
    }
}
