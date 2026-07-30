package com.varyon.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.DifficultyZone;
import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ZoneCalculator {

    @Nullable
    public static DifficultyZone getZoneAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        return getZoneAtPosition(x, z, null, config);
    }

    @Nullable
    public static DifficultyZone getZoneAtPosition(double x, double z, @Nullable String worldName, @Nonnull ZoneConfig config) {
        return getZoneAtPositionIndexed(x, z, worldName, config).zone();
    }

    /** Same lookup as {@link #getZoneAtPosition}, but also returns the zone's index in the config
     *  list — avoids callers that need both from having to re-scan the zone list a second time. */
    @Nonnull
    public static ZoneWithIndex getZoneAtPositionIndexed(double x, double z, @Nullable String worldName, @Nonnull ZoneConfig config) {
        if (worldName != null && !worldName.isBlank()) {
            Integer instanceZoneId = config.getZoneIdForInstanceWorld(worldName);
            if (instanceZoneId != null) {
                DifficultyZone zone = config.getZoneById(instanceZoneId);
                if (zone == null) {
                    return new ZoneWithIndex(null, -1);
                }
                List<DifficultyZone> zones = config.getZones();
                for (int i = 0; i < zones.size(); i++) {
                    if (zones.get(i).getZoneId() == zone.getZoneId()) {
                        return new ZoneWithIndex(zone, i);
                    }
                }
                return new ZoneWithIndex(zone, -1);
            }
        }

        double distance = calculate2DDistance(x, z);
        DifficultyZone currentZone = null;
        int currentZoneIndex = -1;
        List<DifficultyZone> zones = config.getZones();

        for (int i = 0; i < zones.size(); i++) {
            DifficultyZone zone = zones.get(i);
            if (distance >= zone.getRadiusStart()) {
                if (currentZone == null || zone.getRadiusStart() > currentZone.getRadiusStart()) {
                    currentZone = zone;
                    currentZoneIndex = i;
                }
            }
        }

        return new ZoneWithIndex(currentZone, currentZoneIndex);
    }

    public record ZoneWithIndex(@Nullable DifficultyZone zone, int index) {
    }

    @Nullable
    public static DifficultyZone getCurrentZone(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull ZoneConfig config) {
        return getCurrentZone(store, ref, null, config);
    }

    @Nullable
    public static DifficultyZone getCurrentZone(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                               @Nullable String worldName, @Nonnull ZoneConfig config) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return null;

        double x = transform.getPosition().x;
        double z = transform.getPosition().z;
        if (worldName == null || worldName.isBlank()) {
            try {
                Object ext = store.getExternalData();
                if (ext instanceof EntityStore es && es.getWorld() != null) {
                    worldName = es.getWorld().getName();
                }
            } catch (Exception ignored) {}
        }
        return getZoneAtPosition(x, z, worldName, config);
    }

    @Nonnull
    public static ZoneWithIndex getCurrentZoneIndexed(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                                       @Nullable String worldName, @Nonnull ZoneConfig config) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return new ZoneWithIndex(null, -1);

        double x = transform.getPosition().x;
        double z = transform.getPosition().z;
        if (worldName == null || worldName.isBlank()) {
            try {
                Object ext = store.getExternalData();
                if (ext instanceof EntityStore es && es.getWorld() != null) {
                    worldName = es.getWorld().getName();
                }
            } catch (Exception ignored) {}
        }
        return getZoneAtPositionIndexed(x, z, worldName, config);
    }

    public static double calculate2DDistance(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static int getZoneIdAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        DifficultyZone zone = getZoneAtPosition(x, z, config);
        return zone != null ? zone.getZoneId() : 0;
    }

    public static double getMaxMultiplierAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        DifficultyZone zone = getZoneAtPosition(x, z, config);
        return zone != null ? zone.getMaxMultiplier() : 1.0;
    }
}
