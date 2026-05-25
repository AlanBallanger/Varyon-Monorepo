package com.varyon.rtpv;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.Nullable;

public class RtpvConfirmManager {

    private static volatile RtpvConfirmManager instance;

    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingMenuTasks = new ConcurrentHashMap<>();

    public void schedulePendingMenu(UUID uuid, ScheduledFuture<?> future) {
        ScheduledFuture<?> existing = pendingMenuTasks.put(uuid, future);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    public void cancelPendingMenu(UUID uuid) {
        ScheduledFuture<?> task = pendingMenuTasks.remove(uuid);
        if (task != null) {
            task.cancel(false);
        }
    }

    public void onPlayerDisconnect(UUID uuid) {
        cancelPendingMenu(uuid);
    }

    public static void setInstance(@Nullable RtpvConfirmManager manager) {
        instance = manager;
    }

    @Nullable
    public static RtpvConfirmManager getInstance() {
        return instance;
    }
}
