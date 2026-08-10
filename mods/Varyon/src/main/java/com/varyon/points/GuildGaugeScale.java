package com.varyon.points;

public final class GuildGaugeScale {
    private static final int BASE_MAX = 3000;
    private static final int PER_EXTRA_PLAYER = 1500;

    private GuildGaugeScale() {
    }

    public static int maxAbsForOnlineCount(int onlinePlayers) {
        int n = Math.max(1, onlinePlayers);
        return BASE_MAX + (n - 1) * PER_EXTRA_PLAYER;
    }

    public static int clamp(int balance, int absMax) {
        return Math.max(-absMax, Math.min(absMax, balance));
    }
}
