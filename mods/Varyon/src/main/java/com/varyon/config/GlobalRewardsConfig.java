package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class GlobalRewardsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private int rewardCooldownMinutes = 30;
    private final List<RewardTier> tiers = new ArrayList<>();
    
    public static class RewardTier {
        private final int threshold;
        private final List<RewardItem> items;
        private final List<String> commands;
        
        public RewardTier(int threshold, List<RewardItem> items, List<String> commands) {
            this.threshold = threshold;
            this.items = items;
            this.commands = commands;
        }
        
        public int getThreshold() {
            return threshold;
        }
        
        public List<RewardItem> getItems() {
            return items;
        }
        
        public List<String> getCommands() {
            return commands;
        }
    }
    
    public static class RewardItem {
        private final String itemId;
        private final int amount;
        
        public RewardItem(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public int getAmount() {
            return amount;
        }
    }
    
    public static GlobalRewardsConfig createDefault() {
        GlobalRewardsConfig config = new GlobalRewardsConfig();
        config.rewardCooldownMinutes = 30;
        
        List<RewardItem> tier1Items = new ArrayList<>();
        tier1Items.add(new RewardItem("Ingredient_Bar_Copper", 10));
        config.tiers.add(new RewardTier(3300, tier1Items, new ArrayList<>()));
        
        List<RewardItem> tier2Items = new ArrayList<>();
        tier2Items.add(new RewardItem("Ingredient_Bar_Iron", 20));
        config.tiers.add(new RewardTier(6600, tier2Items, new ArrayList<>()));
        
        List<RewardItem> tier3Items = new ArrayList<>();
        tier3Items.add(new RewardItem("Ingredient_Bar_Thorium", 50));
        config.tiers.add(new RewardTier(10000, tier3Items, new ArrayList<>()));
        
        return config;
    }
    
    public void setRewardCooldownMinutes(int minutes) {
        this.rewardCooldownMinutes = minutes;
    }
    
    public void addTier(RewardTier tier) {
        this.tiers.add(tier);
    }
    
    public int getRewardCooldownMinutes() {
        return rewardCooldownMinutes;
    }
    
    public List<RewardTier> getTiers() {
        return tiers;
    }
}
