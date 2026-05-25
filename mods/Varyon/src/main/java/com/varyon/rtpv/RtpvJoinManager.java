package com.varyon.rtpv;

import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RtpvJoinManager {
    private static RtpvJoinManager instance;

    private final Map<UUID, JoinableEntry> joinableByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, ConcurrentLinkedQueue<PendingJoin>> pendingByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> joinRequestUsedThisHostRtpv = new ConcurrentHashMap<>();

    public static void setInstance(@Nullable RtpvJoinManager manager) {
        instance = manager;
    }

    @Nullable
    public static RtpvJoinManager getInstance() {
        return instance;
    }

    public void markJoinable(@Nonnull UUID playerUuid, int zoneId, @Nonnull String worldName, long expireAtMillis,
                            double rtpX, double rtpY, double rtpZ,
                            float rotYaw, float rotPitch, float rotRoll) {
        joinRequestUsedThisHostRtpv.remove(playerUuid);
        joinableByUuid.put(playerUuid, new JoinableEntry(zoneId, worldName, expireAtMillis,
            rtpX, rtpY, rtpZ, rotYaw, rotPitch, rotRoll));
    }

    public boolean tryRegisterJoinRequestForCurrentHostRtpv(@Nonnull UUID hostUuid, @Nonnull UUID joinerUuid) {
        Set<UUID> set = joinRequestUsedThisHostRtpv.computeIfAbsent(hostUuid, k -> ConcurrentHashMap.newKeySet());
        return set.add(joinerUuid);
    }

    public void addPendingJoin(@Nonnull UUID targetUuid, @Nonnull UUID joinerUuid, @Nonnull String joinerName,
                              long pendingExpireAtMillis) {
        ConcurrentLinkedQueue<PendingJoin> q = pendingByTarget.computeIfAbsent(targetUuid,
            k -> new ConcurrentLinkedQueue<>());
        q.removeIf(p -> p.joinerUuid().equals(joinerUuid));
        q.add(new PendingJoin(joinerUuid, joinerName, pendingExpireAtMillis));
    }

    @Nullable
    public PendingJoin findPendingJoinByJoiner(@Nonnull UUID targetUuid, @Nonnull UUID joinerUuid) {
        ConcurrentLinkedQueue<PendingJoin> q = pendingByTarget.get(targetUuid);
        if (q == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        q.removeIf(p -> p.expiresAtMillis() < now);
        for (PendingJoin p : q) {
            if (p.joinerUuid().equals(joinerUuid)) {
                return p;
            }
        }
        return null;
    }

    public void removePendingJoin(@Nonnull UUID targetUuid, @Nonnull UUID joinerUuid) {
        ConcurrentLinkedQueue<PendingJoin> q = pendingByTarget.get(targetUuid);
        if (q != null) {
            q.removeIf(p -> p.joinerUuid().equals(joinerUuid));
            if (q.isEmpty()) {
                pendingByTarget.remove(targetUuid);
            }
        }
    }

    public void onPlayerDisconnect(@Nonnull UUID playerUuid) {
        joinableByUuid.remove(playerUuid);
        joinRequestUsedThisHostRtpv.remove(playerUuid);
        pendingByTarget.remove(playerUuid);
        for (ConcurrentLinkedQueue<PendingJoin> q : pendingByTarget.values()) {
            q.removeIf(p -> p.joinerUuid().equals(playerUuid));
        }
        pendingByTarget.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @Nullable
    public JoinableEntry getJoinable(@Nonnull UUID targetUuid, @Nonnull String targetWorldName,
                                     @Nonnull ZoneConfig zoneConfig) {
        JoinableEntry entry = joinableByUuid.get(targetUuid);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAtMillis) {
            joinableByUuid.remove(targetUuid);
            joinRequestUsedThisHostRtpv.remove(targetUuid);
            return null;
        }
        if (!targetWorldName.equals(entry.worldName)) {
            return null;
        }
        if (zoneConfig.getZoneIdForInstanceWorld(targetWorldName) != null) {
            return null;
        }
        return entry;
    }

    public record JoinableEntry(int zoneId, String worldName, long expireAtMillis,
                                double rtpX, double rtpY, double rtpZ,
                                float rotYaw, float rotPitch, float rotRoll) {}

    public record PendingJoin(UUID joinerUuid, String joinerName, long expiresAtMillis) {}
}
