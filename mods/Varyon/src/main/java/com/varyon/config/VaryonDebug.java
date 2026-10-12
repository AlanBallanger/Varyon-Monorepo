package com.varyon.config;

public final class VaryonDebug {

    private static volatile boolean points = false;
    private static volatile boolean scaling = false;
    private static volatile boolean rewards = false;
    private static volatile boolean economy = false;

    private VaryonDebug() {}

    public static boolean isPoints()  { return points; }
    public static boolean isScaling() { return scaling; }
    public static boolean isRewards() { return rewards; }
    public static boolean isEconomy() { return economy; }

    static void set(boolean points, boolean scaling, boolean rewards, boolean economy) {
        VaryonDebug.points = points;
        VaryonDebug.scaling = scaling;
        VaryonDebug.rewards = rewards;
        VaryonDebug.economy = economy;
    }
}
