package com.varyon.rtpv;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RtpvConfirmManager {

    private static volatile RtpvConfirmManager instance;

    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingHideTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingConfirm> pendingConfirms = new ConcurrentHashMap<>();

    public void schedulePendingHide(UUID uuid, ScheduledFuture<?> future) {
        ScheduledFuture<?> existing = pendingHideTasks.put(uuid, future);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    public void setPendingConfirm(@Nonnull UUID uuid, @Nonnull PendingConfirm confirm) {
        pendingConfirms.put(uuid, confirm);
    }

    @Nullable
    public PendingConfirm getPendingConfirm(@Nonnull UUID uuid) {
        return pendingConfirms.get(uuid);
    }

    public void clearPending(UUID uuid) {
        ScheduledFuture<?> task = pendingHideTasks.remove(uuid);
        if (task != null) {
            task.cancel(false);
        }
        pendingConfirms.remove(uuid);
    }

    public void onPlayerDisconnect(UUID uuid) {
        clearPending(uuid);
    }

    public static void setInstance(@Nullable RtpvConfirmManager manager) {
        instance = manager;
    }

    @Nullable
    public static RtpvConfirmManager getInstance() {
        return instance;
    }

    public static final class PendingConfirm {
        public final int zoneId;
        @Nullable public final Boolean pvpFilter;
        public final int chainBase;
        public final int retryOrdinal;
        public final long expiresAt;

        public PendingConfirm(int zoneId, @Nullable Boolean pvpFilter, int chainBase, int retryOrdinal, long expiresAt) {
            this.zoneId = zoneId;
            this.pvpFilter = pvpFilter;
            this.chainBase = chainBase;
            this.retryOrdinal = retryOrdinal;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
