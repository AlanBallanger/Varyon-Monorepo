package com.varyon.config;

import javax.annotation.Nonnull;

public class EssenceRewardsConfig {

    private static final double REFERENCE_TO_FACTION_POINTS = 1.5;

    private MobFragmentsConfig referenceFragments;
    private EssenceEconomyConfig economy = EssenceEconomyConfig.createDefault();

    public void attach(@Nonnull MobFragmentsConfig referenceFragments, @Nonnull EssenceEconomyConfig economy) {
        this.referenceFragments = referenceFragments;
        this.economy = economy;
    }

    public double getPvpEssenceMultiplier() {
        return economy.getPvpEssenceMultiplier();
    }

    public double getOreReward(@Nonnull String blockId) {
        if (com.varyon.util.MiningOreBlockIds.isExcludedFromVaryonOreRewards(blockId)) {
            return 0.0;
        }
        double raw = referenceFragments != null ? referenceFragments.getMiningFragmentWeight(blockId) : 0.0;
        double base = raw > 0 ? raw : economy.getDefaultOreReward();
        return base * REFERENCE_TO_FACTION_POINTS;
    }

    public double getMobReward(@Nonnull String mobId) {
        String id = mobId.toLowerCase(java.util.Locale.ROOT);
        int raw = referenceFragments != null ? referenceFragments.getFragments(id) : -1;
        double base = raw >= 0 ? raw : economy.getDefaultMobReward();
        return base * REFERENCE_TO_FACTION_POINTS;
    }
}
