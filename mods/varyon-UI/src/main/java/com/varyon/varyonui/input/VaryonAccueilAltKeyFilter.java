package com.varyon.varyonui.input;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.varyonui.config.AccueilShortcutConfig;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        if (!"ClientMovement".equals(packet.getClass().getSimpleName())) {
            return false;
        }
        processMovement(playerRef, playerRef.getUuid(), packet);
        return false;
    }

    public void clearPlayer(UUID uuid) {
        if (uuid != null) {
            byPlayer.remove(uuid);
        }
    }

    private void processMovement(PlayerRef playerRef, UUID uuid, Packet packet) {
        if (uuid != null && AccueilShortcutConfig.getInstance().getMode(uuid) != AccueilShortcutConfig.Mode.ALT) {
            byPlayer.remove(uuid);
            return;
        }
        long now = System.currentTimeMillis();
        Object posObj = getFieldOrGetter(packet, "absolutePosition");
        double[] pos = null;
        if (posObj != null) {
            pos = new double[]{
                getNumber(posObj, "x"), getNumber(posObj, "y"), getNumber(posObj, "z")
            };
        }
        Object moveStates = getFieldOrGetter(packet, "movementStates");
        if (moveStates == null) {
            return;
        }
        boolean walking = getBool(moveStates, "walking");
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

    private static Object getFieldOrGetter(Object o, String name) {
        if (o == null) {
            return null;
        }
        try {
            String cap = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            for (Method m : o.getClass().getMethods()) {
                if (m.getName().equalsIgnoreCase(cap) && m.getParameterCount() == 0) {
                    return m.invoke(o);
                }
            }
            for (Field f : o.getClass().getFields()) {
                if (f.getName().equalsIgnoreCase(name)) {
                    return f.get(o);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static double getNumber(Object o, String name) {
        try {
            String cap = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            for (Method m : o.getClass().getMethods()) {
                if (cap.equals(m.getName()) && m.getParameterCount() == 0) {
                    Object v = m.invoke(o);
                    if (v instanceof Number n) {
                        return n.doubleValue();
                    }
                }
            }
            for (Field f : o.getClass().getFields()) {
                if (f.getName().equalsIgnoreCase(name)) {
                    Object v = f.get(o);
                    if (v instanceof Number n) {
                        return n.doubleValue();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    private static boolean getBool(Object o, String name) {
        Object v = getFieldOrGetter(o, name);
        return v instanceof Boolean b && b;
    }
}
