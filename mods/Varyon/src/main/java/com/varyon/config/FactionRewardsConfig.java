package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class FactionRewardsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "faction_rewards.toml";
    private static final String SECTION  = "faction_rewards";

    private final int cooldownMinutes;
    private final double minParticipationPoints;
    private final double passiveRewardRate;
    private final List<RewardTier> tiers;

    public static class RewardTier {
        private final int threshold;
        private final int fragmentAmount;

        public RewardTier(int threshold, int fragmentAmount) {
            this.threshold = threshold;
            this.fragmentAmount = fragmentAmount;
        }

        public int getThreshold()      { return threshold; }
        public int getFragmentAmount() { return fragmentAmount; }
    }

    public FactionRewardsConfig(int cooldownMinutes, double minParticipationPoints,
                                double passiveRewardRate, @Nonnull List<RewardTier> tiers) {
        this.cooldownMinutes = cooldownMinutes;
        this.minParticipationPoints = minParticipationPoints;
        this.passiveRewardRate = passiveRewardRate;
        this.tiers = new ArrayList<>(tiers);
    }

    public int getCooldownMinutes()             { return cooldownMinutes; }
    public double getMinParticipationPoints()   { return minParticipationPoints; }
    public double getPassiveRewardRate()        { return passiveRewardRate; }
    @Nonnull public List<RewardTier> getTiers() { return Collections.unmodifiableList(tiers); }

    @Nonnull
    public static FactionRewardsConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            FactionRewardsConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);
            Toml section = toml.getTable(SECTION);
            if (section == null) return createDefault();

            int cooldown = section.getLong("cooldownMinutes", 30L).intValue();
            double minPoints = section.getDouble("minParticipationPoints",
                section.getDouble("minParticipationEssence", 10.0));
            double passRate = section.getDouble("passiveRewardRate", 0.20);

            List<RewardTier> tiers = new ArrayList<>();
            List<Toml> tierList = section.getTables("tiers");
            if (tierList != null) {
                for (Toml t : tierList) {
                    int threshold = t.getLong("threshold", 3300L).intValue();
                    int fragments = t.getLong("fragmentAmount", 100L).intValue();
                    tiers.add(new RewardTier(threshold, fragments));
                }
            }
            if (tiers.isEmpty()) return createDefault();
            return new FactionRewardsConfig(cooldown, minPoints, passRate, tiers);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load faction_rewards.toml, using defaults: " + e.getMessage());
            return createDefault();
        }
    }

    public void save(@Nonnull Path dataFolder) {
        try {
            dataFolder.toFile().mkdirs();
            File file = dataFolder.resolve(FILENAME).toFile();
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(SECTION).append("]\n");
            sb.append("cooldownMinutes = ").append(cooldownMinutes).append("\n");
            sb.append("minParticipationPoints = ").append(minParticipationPoints).append("\n");
            sb.append("passiveRewardRate = ").append(passiveRewardRate).append("\n\n");
            for (RewardTier tier : tiers) {
                sb.append("[[").append(SECTION).append(".tiers]]\n");
                sb.append("threshold = ").append(tier.threshold).append("\n");
                sb.append("fragmentAmount = ").append(tier.fragmentAmount).append("\n\n");
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(sb.toString());
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save faction_rewards.toml: " + e.getMessage());
        }
    }

    @Nonnull
    public static FactionRewardsConfig createDefault() {
        List<RewardTier> tiers = new ArrayList<>();
        tiers.add(new RewardTier(3300,  100));
        tiers.add(new RewardTier(6600,  200));
        tiers.add(new RewardTier(10000, 300));
        return new FactionRewardsConfig(30, 10.0, 0.20, tiers);
    }
}
