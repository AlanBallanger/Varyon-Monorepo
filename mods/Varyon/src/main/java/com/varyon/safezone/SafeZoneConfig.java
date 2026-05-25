package com.varyon.safezone;

public class SafeZoneConfig {
    private boolean enabled;
    private int minRotationTimeMinutes;
    private int maxRotationTimeMinutes;
    private int overlapDurationMinutes;
    private int maxRadius;
    private int spawnRadius;

    public SafeZoneConfig() {
        this.enabled = true;
        this.minRotationTimeMinutes = 60;
        this.maxRotationTimeMinutes = 120;
        this.overlapDurationMinutes = 10;
        this.maxRadius = -1;
        this.spawnRadius = 100;
    }

    public boolean isEnabled()                          { return enabled; }
    public void setEnabled(boolean v)                   { this.enabled = v; }
    public int getMinRotationTimeMinutes()              { return minRotationTimeMinutes; }
    public void setMinRotationTimeMinutes(int v)        { this.minRotationTimeMinutes = v; }
    public int getMaxRotationTimeMinutes()              { return maxRotationTimeMinutes; }
    public void setMaxRotationTimeMinutes(int v)        { this.maxRotationTimeMinutes = v; }
    public int getOverlapDurationMinutes()              { return overlapDurationMinutes; }
    public void setOverlapDurationMinutes(int v)        { this.overlapDurationMinutes = v; }
    public int getMaxRadius()                           { return maxRadius; }
    public void setMaxRadius(int v)                     { this.maxRadius = v; }
    public int getSpawnRadius()                         { return spawnRadius; }
    public void setSpawnRadius(int v)                   { this.spawnRadius = v; }

    public long getMinRotationTimeMillis()  { return minRotationTimeMinutes * 60L * 1000L; }
    public long getMaxRotationTimeMillis()  { return maxRotationTimeMinutes * 60L * 1000L; }
    public long getOverlapDurationMillis()  { return overlapDurationMinutes * 60L * 1000L; }
}
