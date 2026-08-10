package com.varyon.config;

public class DeathConfig {
    private final double pointsLossPercent;

    public DeathConfig(double pointsLossPercent) {
        this.pointsLossPercent = Math.max(0, Math.min(100, pointsLossPercent));
    }

    public static DeathConfig createDefault() {
        return new DeathConfig(80);
    }

    public double getPointsLossPercent() {
        return pointsLossPercent;
    }
}
