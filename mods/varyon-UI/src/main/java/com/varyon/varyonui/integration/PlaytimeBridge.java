package com.varyon.varyonui.integration;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlaytimeBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");
    private static final String API_CLASS = "com.varyon.playtime.api.PlaytimeAPI";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter CONNECTION_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm").withZone(ZoneId.systemDefault());

    private static Boolean available = null;
    private static Object cachedApi = null;

    private PlaytimeBridge() {}

    public static boolean isAvailable() {
        if (available == null) {
            try {
                Class.forName(API_CLASS);
                available = true;
            } catch (ClassNotFoundException e) {
                available = false;
            }
        }
        return Boolean.TRUE.equals(available);
    }

    /** API Playtime censée être là mais inaccessible (init, refl., stockage distant, etc.). */
    public static boolean isBackendOperational() {
        return isAvailable() && api() != null;
    }

    @Nullable
    private static Object api() {
        if (!isAvailable()) return null;
        if (cachedApi != null) return cachedApi;
        try {
            Class<?> cls = Class.forName(API_CLASS);
            Method get = cls.getMethod("get");
            cachedApi = get.invoke(null);
            return cachedApi;
        } catch (Exception e) {
            LOG.log(Level.FINE, "PlaytimeBridge: cannot get API instance", e);
            return null;
        }
    }

    public static long getTotalPlaytime(UUID uuid) {
        try {
            Object a = api();
            if (a == null) return 0;
            Method m = a.getClass().getMethod("getTotalPlaytime", UUID.class);
            return (Long) m.invoke(a, uuid);
        } catch (Exception e) { return 0; }
    }

    public static long getDailyPlaytime(UUID uuid) {
        try {
            Object a = api();
            if (a == null) return 0;
            Method m = a.getClass().getMethod("getPlaytime", UUID.class, String.class);
            return (Long) m.invoke(a, uuid, "daily");
        } catch (Exception e) { return 0; }
    }

    public static long getFirstLogin(UUID uuid) {
        try {
            Object a = api();
            if (a == null) return 0;
            Method m = a.getClass().getMethod("getFirstLogin", UUID.class);
            return (Long) m.invoke(a, uuid);
        } catch (Exception e) { return 0; }
    }

    public static long getLastLogin(UUID uuid) {
        try {
            Object a = api();
            if (a == null) return 0;
            Method m = a.getClass().getMethod("getLastLogin", UUID.class);
            return (Long) m.invoke(a, uuid);
        } catch (Exception e) { return 0; }
    }

    public static long[] getDailyRewardData(UUID uuid) {
        try {
            Object a = api();
            if (a == null) return new long[0];
            Method m = a.getClass().getMethod("getDailyRewardData", UUID.class);
            return (long[]) m.invoke(a, uuid);
        } catch (Exception e) { return new long[0]; }
    }

    public static String[] getDailyRewardIds() {
        try {
            Object a = api();
            if (a == null) return new String[0];
            Method m = a.getClass().getMethod("getDailyRewardIds");
            return (String[]) m.invoke(a);
        } catch (Exception e) { return new String[0]; }
    }

    public static boolean claimReward(UUID uuid, String rewardId) {
        try {
            Object a = api();
            if (a == null) return false;
            Method m = a.getClass().getMethod("claimReward", UUID.class, String.class);
            return Boolean.TRUE.equals(m.invoke(a, uuid, rewardId));
        } catch (Exception e) { return false; }
    }

    public static int claimAllDailyRewards(UUID uuid) {
        try {
            Object a = api();
            if (a == null) return 0;
            Method m = a.getClass().getMethod("claimAllEligibleDailyRewards", UUID.class);
            Object r = m.invoke(a, uuid);
            if (r instanceof Integer) {
                return (Integer) r;
            }
            return ((Number) r).intValue();
        } catch (Exception e) { return 0; }
    }

    public static String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }

    public static String formatRewardTime(long ms) {
        long totalMin = ms / 60_000L;
        if (totalMin < 60) return totalMin + " min";
        long h = totalMin / 60;
        long m = totalMin % 60;
        return m > 0 ? h + "h " + m + "m" : h + "h";
    }

    public static String formatDate(long epochMs) {
        if (epochMs <= 0) return "—";
        return DATE_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    public static String formatConnectionDate(long epochMs) {
        if (epochMs <= 0) return "—";
        return CONNECTION_DATE.format(Instant.ofEpochMilli(epochMs));
    }
}
