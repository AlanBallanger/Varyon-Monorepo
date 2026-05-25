package com.varyon.safezone;

public class SafeZoneCalculator {
    
    public static double calculateAngle(double x, double z) {
        double angle = Math.toDegrees(Math.atan2(z, x));
        return ((angle + 360) % 360);
    }
    
    public static boolean isInSafeZone(double x, double z, SafeZoneQuadrant activeQuadrant, SafeZoneQuadrant nextQuadrant, boolean isOverlapActive, SafeZoneConfig config) {
        if (!config.isEnabled()) {
            return false;
        }

        double distanceFromSpawn = Math.sqrt(x * x + z * z);
        if (config.getSpawnRadius() > 0 && distanceFromSpawn <= config.getSpawnRadius()) {
            return true;
        }
        
        if (config.getMaxRadius() > 0 && distanceFromSpawn > config.getMaxRadius()) {
            return false;
        }
        
        double angle = calculateAngle(x, z);
        
        if (isOverlapActive) {
            return activeQuadrant.containsAngle(angle) || nextQuadrant.containsAngle(angle);
        } else {
            return activeQuadrant.containsAngle(angle);
        }
    }
    
    public static SafeZoneQuadrant getQuadrantAtPosition(double x, double z) {
        double angle = calculateAngle(x, z);
        return SafeZoneQuadrant.fromAngle(angle);
    }
}
