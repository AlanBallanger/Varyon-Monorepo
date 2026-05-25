package com.varyon.rtpv;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

public final class RtpvCooldownStore {

    private static final int MAX_CONSECUTIVE_RTPV = 5;

    private static final ConcurrentHashMap<UUID, Long> LAST_SUCCESS_MS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Integer> CONSECUTIVE_RTPV_COUNT = new ConcurrentHashMap<>();

    private RtpvCooldownStore() {}

    public static int getRemainingCooldownSeconds(@Nonnull UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long last = LAST_SUCCESS_MS.get(uuid);
        if (last == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - last;
        long need = cooldownSeconds * 1000L;
        if (elapsed >= need) {
            return 0;
        }
        return (int) ((need - elapsed + 999) / 1000);
    }

    public static void recordSuccessfulRtpv(@Nonnull UUID uuid) {
        LAST_SUCCESS_MS.put(uuid, System.currentTimeMillis());
    }

    public static boolean isConsecutiveRtpvAllowed(@Nonnull UUID uuid, int cooldownSeconds) {
        refreshConsecutiveRtpvIfCooldownElapsed(uuid, cooldownSeconds);
        return CONSECUTIVE_RTPV_COUNT.getOrDefault(uuid, 0) < MAX_CONSECUTIVE_RTPV;
    }

    public static void incrementConsecutiveRtpv(@Nonnull UUID uuid) {
        CONSECUTIVE_RTPV_COUNT.merge(uuid, 1, Integer::sum);
    }

    public static void resetConsecutiveRtpv(@Nonnull UUID uuid) {
        CONSECUTIVE_RTPV_COUNT.remove(uuid);
    }

    private static void refreshConsecutiveRtpvIfCooldownElapsed(@Nonnull UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return;
        }
        Long lastMs = LAST_SUCCESS_MS.get(uuid);
        if (lastMs == null) {
            return;
        }
        if (System.currentTimeMillis() - lastMs >= cooldownSeconds * 1000L) {
            CONSECUTIVE_RTPV_COUNT.remove(uuid);
        }
    }

    public static void onPlayerDisconnect(@Nonnull UUID uuid) {
        LAST_SUCCESS_MS.remove(uuid);
        CONSECUTIVE_RTPV_COUNT.remove(uuid);
    }
}
