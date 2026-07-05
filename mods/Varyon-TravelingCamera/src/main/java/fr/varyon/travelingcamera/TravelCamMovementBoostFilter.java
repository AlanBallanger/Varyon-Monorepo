package fr.varyon.travelingcamera;

import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class TravelCamMovementBoostFilter implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof ClientMovement movement)) {
            return false;
        }
        if (!TravelingCameraManager.isPlaying(playerRef.getUuid())) {
            return false;
        }
        TravelingCameraManager.reportMovementIntent(playerRef.getUuid(), hasMovementIntent(movement));
        return false;
    }

    private static boolean hasMovementIntent(ClientMovement movement) {
        MovementStates states = movement.movementStates;
        if (states == null) {
            return false;
        }
        return !states.horizontalIdle;
    }
}
