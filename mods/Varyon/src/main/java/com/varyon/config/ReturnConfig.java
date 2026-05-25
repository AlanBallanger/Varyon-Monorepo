package com.varyon.config;

public class ReturnConfig {
    private boolean enabled;
    private int cooldownSeconds;
    private int minDistance;
    private int maxDistance;
    private int expirationMinutes;

    public ReturnConfig(boolean enabled, int cooldownSeconds, int minDistance,
                        int maxDistance, int expirationMinutes) {
        this.enabled = enabled;
        this.cooldownSeconds = cooldownSeconds;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.expirationMinutes = expirationMinutes;
    }

    public static ReturnConfig createDefault() {
        return new ReturnConfig(true, 300, 100, 200, 30);
    }

    public boolean isEnabled()          { return enabled; }
    public int getCooldownSeconds()     { return cooldownSeconds; }
    public int getMinDistance()         { return minDistance; }
    public int getMaxDistance()         { return maxDistance; }
    public int getExpirationMinutes()   { return expirationMinutes; }
}
