package com.varyon.config;

import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;

public final class PointsEconomyConfig {
    private final double pvpPointsMultiplier;
    private final double defaultMobReward;
    private final double defaultOreReward;

    public PointsEconomyConfig(double pvpPointsMultiplier,
                              double defaultMobReward,
                              double defaultOreReward) {
        this.pvpPointsMultiplier = pvpPointsMultiplier;
        this.defaultMobReward = defaultMobReward;
        this.defaultOreReward = defaultOreReward;
    }

    @Nonnull
    public static PointsEconomyConfig createDefault() {
        return new PointsEconomyConfig(2.0, 0.0, 0.0);
    }

    @Nonnull
    public static PointsEconomyConfig parse(@Nonnull Toml toml) {
        Toml t = toml.getTable("points_economy");
        if (t == null) {
            t = toml.getTable("essence_economy");
        }
        if (t == null) {
            return createDefault();
        }
        return new PointsEconomyConfig(
                t.getDouble("pvpPointsMultiplier", t.getDouble("pvpEssenceMultiplier", 2.0)),
                t.getDouble("defaultMobReward", 0.0),
                t.getDouble("defaultOreReward", 0.0));
    }

    public double getPvpPointsMultiplier() {
        return pvpPointsMultiplier;
    }

    public double getDefaultMobReward() {
        return defaultMobReward;
    }

    public double getDefaultOreReward() {
        return defaultOreReward;
    }
}
