package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.varyon.util.MiningOreBlockIds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class MobFragmentsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, Integer> fragmentsByMobId;
    private final Map<String, Double> miningFragments;

    public MobFragmentsConfig(@Nonnull Map<String, Integer> fragmentsByMobId,
                              @Nonnull Map<String, Double> miningFragments) {
        this.fragmentsByMobId = new HashMap<>(fragmentsByMobId);
        this.miningFragments  = new LinkedHashMap<>(miningFragments);
    }

    public int getFragments(@Nonnull String roleName) {
        String id = roleName.toLowerCase(Locale.ROOT);
        Integer exact = fragmentsByMobId.get(id);
        if (exact != null) return exact;
        String bestKey = null;
        for (String key : fragmentsByMobId.keySet()) {
            if (id.startsWith(key + "_")) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        return bestKey != null ? fragmentsByMobId.get(bestKey) : -1;
    }

    public double getMiningFragmentWeight(@Nonnull String blockId) {
        Double w = resolveMiningWeight(blockId.toLowerCase(Locale.ROOT));
        return w != null && w > 0 ? w : 0.0;
    }

    public int rollMiningFragmentDrops(@Nonnull String blockId) {
        Double wObj = resolveMiningWeight(blockId.toLowerCase(Locale.ROOT));
        if (wObj == null || wObj <= 0) {
            return 0;
        }
        double w = wObj;
        int base = (int) Math.floor(w);
        double frac = w - base;
        int extra = (frac > 0.0 && ThreadLocalRandom.current().nextDouble() < frac) ? 1 : 0;
        return base + extra;
    }

    @Nullable
    private Double resolveMiningWeight(@Nonnull String id) {
        if (MiningOreBlockIds.isExcludedFromVaryonOreRewards(id)) {
            return null;
        }
        Double exact = miningFragments.get(id);
        if (exact != null) {
            return exact;
        }
        String bestKey = null;
        for (String key : miningFragments.keySet()) {
            if (id.startsWith(key)) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        return bestKey != null ? miningFragments.get(bestKey) : null;
    }

    @Nonnull
    public static MobFragmentsConfig load(@Nonnull Path pluginDataFolder) {
        MobFragmentsConfig c = KeyFragmentReferenceLoader.tryLoad(
                pluginDataFolder, MobFragmentsConfig.class.getClassLoader());
        if (c != null) {
            return c;
        }
        LOGGER.at(Level.SEVERE).log("Could not load mob_categories / tier_mapping / mineral_weights (disk or jar).");
        return new MobFragmentsConfig(Map.of(), new LinkedHashMap<>());
    }
}
