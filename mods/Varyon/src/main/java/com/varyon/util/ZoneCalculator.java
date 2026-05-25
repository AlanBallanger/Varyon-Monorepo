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
        if (worldName != null && !worldName.isBlank()) {
            Integer instanceZoneId = config.getZoneIdForInstanceWorld(worldName);
            if (instanceZoneId != null) {
                return config.getZoneById(instanceZoneId);
            }
        }

        double distance = calculate2DDistance(x, z);
        DifficultyZone currentZone = null;
        List<DifficultyZone> zones = config.getZones();

        for (DifficultyZone zone : zones) {
            if (distance >= zone.getRadiusStart()) {
                if (currentZone == null || zone.getRadiusStart() > currentZone.getRadiusStart()) {
                    currentZone = zone;
                }
            }
        }

        return currentZone;
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

        double x = transform.getPosition().getX();
        double z = transform.getPosition().getZ();
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
