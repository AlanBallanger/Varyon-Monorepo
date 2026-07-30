package com.varyon.varyonui.input;

import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.varyonui.config.AccueilShortcutConfig;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VaryonAccueilAltKeyFilter implements PlayerPacketFilter {

    private static final double MAX_MOVE = 0.05;
    private static final long MAX_HOLD_MS = 500L;

    private final Map<UUID, AltState> byPlayer = new ConcurrentHashMap<>();

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (playerRef.getUuid() == null) {
            return false;
        }
        if (!(packet instanceof ClientMovement movement)) {
            return false;
        }
        processMovement(playerRef, playerRef.getUuid(), movement);
        return false;
    }

    public void clearPlayer(UUID uuid) {
        if (uuid != null) {
            byPlayer.remove(uuid);
        }
    }

    private void processMovement(PlayerRef playerRef, UUID uuid, ClientMovement packet) {
        if (uuid != null && AccueilShortcutConfig.getInstance().getMode(uuid) != AccueilShortcutConfig.Mode.ALT) {
            byPlayer.remove(uuid);
            return;
        }
        long now = System.currentTimeMillis();
        Position posObj = packet.absolutePosition;
        double[] pos = posObj != null ? new double[]{posObj.x, posObj.y, posObj.z} : null;
        MovementStates moveStates = packet.movementStates;
        if (moveStates == null) {
            return;
        }
        boolean walking = moveStates.walking;
        if (walking) {
            AltState s = byPlayer.get(uuid);
            if (s == null) {
                byPlayer.put(uuid, new AltState(now, pos != null ? pos.clone() : new double[3]));
            } else if (pos != null && s.startPos != null) {
                double dx = pos[0] - s.startPos[0];
                double dy = pos[1] - s.startPos[1];
                double dz = pos[2] - s.startPos[2];
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > s.maxFromStart) {
                    s.maxFromStart = dist;
                }
            }
            return;
        }
        AltState end = byPlayer.remove(uuid);
        if (end == null) {
            return;
        }
        long held = now - end.startTimeMs;
        double displacement = end.maxFromStart;
        if (pos != null && end.startPos != null) {
            double dx = pos[0] - end.startPos[0];
            double dy = pos[1] - end.startPos[1];
            double dz = pos[2] - end.startPos[2];
            double d0 = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d0 > displacement) {
                displacement = d0;
            }
        }
        if (held > MAX_HOLD_MS) {
            return;
        }
        if (displacement >= MAX_MOVE) {
            return;
        }
        VaryonAccueilKeyHelper.runAccueil(playerRef);
    }

    private static final class AltState {
        final long startTimeMs;
        final double[] startPos;
        double maxFromStart;

        AltState(long startTimeMs, double[] startPos) {
            this.startTimeMs = startTimeMs;
            this.startPos = startPos;
        }
    }

}
