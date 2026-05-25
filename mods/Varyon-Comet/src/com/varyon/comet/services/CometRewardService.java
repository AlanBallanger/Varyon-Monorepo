package com.varyon.comet.services;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.varyon.comet.config.model.TierRewards;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Encapsulates reward table resolution and item generation.
 */
public class CometRewardService {

    public void generateTierRewards(
            CometTier tier,
            String themeId,
            int zoneId,
            Random random,
            List<ItemStack> allItems,
            List<String> droppedItemIds,
            Logger logger) {

        tier = CometConfig.clampUnavailableTier(tier);
        int tierNum = toTierNum(tier);

        CometConfig config = CometConfig.getInstance();

        // 1) Theme-specific override takes absolute precedence if configured
        if (themeId != null && WaveThemeProvider.hasRewardOverride(themeId, tier)) {
            TierRewards override = WaveThemeProvider.getRewardOverride(themeId, tier);
            if (override != null) {
                logger.info("Using theme reward override for '" + themeId + "' tier " + tierNum);
                override.generateRewards(random, allItems, droppedItemIds);
                logger.info("Generated " + allItems.size() + " reward items for tier " + tier.getName());
                return;
            }
        }

        if (config != null) {
            TierRewards currentTierPool = config.getTierRewards(tierNum);
            currentTierPool.generateRewards(random, allItems, droppedItemIds);
            logger.info("Generated " + allItems.size() + " reward items for tier " + tier.getName()
                    + " (zone " + zoneId + ")");
            return;
        }

        // 3) Fallback: global tier rewards only
        TierRewards rewards = TierRewards.getDefaultForTier(tierNum);
        logger.info("Using default rewards for tier " + tierNum + " (config not available)");
        rewards.generateRewards(random, allItems, droppedItemIds);
        logger.info("Generated " + allItems.size() + " reward items for tier " + tier.getName());
    }

    private boolean hasAnyRewards(TierRewards rewards) {
        return rewards != null && (!rewards.getDrops().isEmpty() || !rewards.getBonusDrops().isEmpty());
    }

    private int toTierNum(CometTier tier) {
        switch (tier) {
            case UNCOMMON:
                return 1;
            case RARE:
                return 2;
            case EPIC:
                return 3;
            case LEGENDARY:
                return 4;
            case MYTHIC:
                return 5;
            default:
                return 1;
        }
    }
}
