package com.varyon.config;

public class RtpvConfig {
    private final double safeCostMultiplier;
    private final boolean economyEnabled;
    private final int joinDurationSeconds;
    private final int cooldownSeconds;

    public RtpvConfig(double safeCostMultiplier, boolean economyEnabled) {
        this(safeCostMultiplier, economyEnabled, 60, 60);
    }

    public RtpvConfig(double safeCostMultiplier, boolean economyEnabled, int joinDurationSeconds) {
        this(safeCostMultiplier, economyEnabled, joinDurationSeconds, 60);
    }

    public RtpvConfig(
        double safeCostMultiplier,
        boolean economyEnabled,
        int joinDurationSeconds,
        int cooldownSeconds
    ) {
        this.safeCostMultiplier = safeCostMultiplier;
        this.economyEnabled = economyEnabled;
        this.joinDurationSeconds = joinDurationSeconds > 0 ? joinDurationSeconds : 60;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    public static RtpvConfig createDefault() {
        return new RtpvConfig(2.0, true, 60, 60);
    }

    public double getSafeCostMultiplier() { return safeCostMultiplier; }
    public boolean isEconomyEnabled()     { return economyEnabled; }
    public int getJoinDurationSeconds()   { return joinDurationSeconds; }
    public int getCooldownSeconds()       { return cooldownSeconds; }
}
