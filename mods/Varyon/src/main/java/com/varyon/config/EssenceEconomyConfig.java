package com.varyon.config;

import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;

public final class EssenceEconomyConfig {
    private final double pvpEssenceMultiplier;
    private final double defaultMobReward;
    private final double defaultOreReward;

    public EssenceEconomyConfig(double pvpEssenceMultiplier,
                              double defaultMobReward,
                              double defaultOreReward) {
        this.pvpEssenceMultiplier = pvpEssenceMultiplier;
        this.defaultMobReward = defaultMobReward;
        this.defaultOreReward = defaultOreReward;
    }

    @Nonnull
    public static EssenceEconomyConfig createDefault() {
        return new EssenceEconomyConfig(2.0, 0.0, 0.0);
    }

    @Nonnull
    public static EssenceEconomyConfig parse(@Nonnull Toml toml) {
        Toml t = toml.getTable("essence_economy");
        if (t == null) {
            return createDefault();
        }
        return new EssenceEconomyConfig(
                t.getDouble("pvpEssenceMultiplier", 2.0),
                t.getDouble("defaultMobReward", 0.0),
                t.getDouble("defaultOreReward", 0.0));
    }

    public double getPvpEssenceMultiplier() {
        return pvpEssenceMultiplier;
    }

    public double getDefaultMobReward() {
        return defaultMobReward;
    }

    public double getDefaultOreReward() {
        return defaultOreReward;
    }
}
