package com.varyon.config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ExtractionConfig {
    private boolean enabled;
    private int minDistance;
    private int maxDistance;
    private int portalDurationSeconds;
    private int cooldownSeconds;
    private List<ExtractionZoneDistance> zoneRanges;

    public ExtractionConfig() {
        this.enabled = true;
        this.minDistance = 100;
        this.maxDistance = 200;
        this.portalDurationSeconds = 60;
        this.cooldownSeconds = 60;
        this.zoneRanges = new ArrayList<>();
    }

    public boolean isEnabled()                      { return enabled; }
    public void setEnabled(boolean v)               { this.enabled = v; }
    public int getMinDistance()                     { return minDistance; }
    public void setMinDistance(int v)               { this.minDistance = v; }
    public int getMaxDistance()                     { return maxDistance; }
    public void setMaxDistance(int v)               { this.maxDistance = v; }
    public int getPortalDurationSeconds()           { return portalDurationSeconds; }
    public void setPortalDurationSeconds(int v)     { this.portalDurationSeconds = v; }
    public int getCooldownSeconds()                 { return cooldownSeconds; }
    public void setCooldownSeconds(int v)           { this.cooldownSeconds = v; }

    @Nonnull
    public List<ExtractionZoneDistance> getZoneRanges() {
        return zoneRanges;
    }

    public void setZoneRanges(@Nonnull List<ExtractionZoneDistance> zoneRanges) {
        this.zoneRanges = new ArrayList<>(zoneRanges);
    }

    public int getEffectiveMinDistance(int zoneId) {
        return resolveDistance(zoneId, true);
    }

    public int getEffectiveMaxDistance(int zoneId) {
        return resolveDistance(zoneId, false);
    }

    private int resolveDistance(int zoneId, boolean useMin) {
        if (zoneId <= 0) {
            return useMin ? minDistance : maxDistance;
        }
        List<ExtractionZoneDistance> anchors = sortedUniqueAnchors();
        if (anchors.isEmpty()) {
            return useMin ? minDistance : maxDistance;
        }
        if (anchors.size() == 1) {
            ExtractionZoneDistance a = anchors.get(0);
            if (zoneId == a.zoneId()) {
                return useMin ? a.minDistance() : a.maxDistance();
            }
            return useMin ? minDistance : maxDistance;
        }
        ExtractionZoneDistance first = anchors.get(0);
        ExtractionZoneDistance last = anchors.get(anchors.size() - 1);
        if (zoneId <= first.zoneId()) {
            return useMin ? first.minDistance() : first.maxDistance();
        }
        if (zoneId >= last.zoneId()) {
            return useMin ? last.minDistance() : last.maxDistance();
        }
        for (int i = 0; i < anchors.size() - 1; i++) {
            int zLo = anchors.get(i).zoneId();
            int zHi = anchors.get(i + 1).zoneId();
            if (zoneId >= zLo && zoneId <= zHi) {
                if (zHi == zLo) {
                    return useMin ? anchors.get(i).minDistance() : anchors.get(i).maxDistance();
                }
                double t = (zoneId - zLo) / (double) (zHi - zLo);
                int vLo = useMin ? anchors.get(i).minDistance() : anchors.get(i).maxDistance();
                int vHi = useMin ? anchors.get(i + 1).minDistance() : anchors.get(i + 1).maxDistance();
                return (int) Math.round(vLo + t * (vHi - vLo));
            }
        }
        return useMin ? minDistance : maxDistance;
    }

    @Nonnull
    private List<ExtractionZoneDistance> sortedUniqueAnchors() {
        Map<Integer, ExtractionZoneDistance> byId = new TreeMap<>();
        for (ExtractionZoneDistance z : zoneRanges) {
            byId.put(z.zoneId(), z);
        }
        return new ArrayList<>(byId.values());
    }

    public static void applyDefaultZoneRangesForNewConfig(@Nonnull ExtractionConfig config) {
        config.setZoneRanges(List.of(
            new ExtractionZoneDistance(1, 100, 200),
            new ExtractionZoneDistance(10, 400, 500)
        ));
    }
}
