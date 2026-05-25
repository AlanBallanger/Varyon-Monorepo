package com.varyon.config;

public class DeathConfig {
    private final double essenceLossPercent;

    public DeathConfig(double essenceLossPercent) {
        this.essenceLossPercent = Math.max(0, Math.min(100, essenceLossPercent));
    }

    public static DeathConfig createDefault() {
        return new DeathConfig(80);
    }

    public double getEssenceLossPercent() {
        return essenceLossPercent;
    }
}
