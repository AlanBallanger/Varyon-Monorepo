package com.varyon.comet.config.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TierRewards {

    private List<RewardEntry> drops;
    private List<RewardEntry> bonusDrops;

    public TierRewards() {
        this.drops = new ArrayList<>();
        this.bonusDrops = new ArrayList<>();
    }

    public TierRewards(List<RewardEntry> drops, List<RewardEntry> bonusDrops) {
        this.drops = drops != null ? drops : new ArrayList<>();
        this.bonusDrops = bonusDrops != null ? bonusDrops : new ArrayList<>();
    }

    public List<RewardEntry> getDrops() {
        return drops;
    }

    public List<RewardEntry> getBonusDrops() {
        return bonusDrops;
    }

    public void setDrops(List<RewardEntry> drops) {
        this.drops = drops != null ? drops : new ArrayList<>();
    }

    public void setBonusDrops(List<RewardEntry> bonusDrops) {
        this.bonusDrops = bonusDrops != null ? bonusDrops : new ArrayList<>();
    }

    public void addDrop(RewardEntry drop) {
        this.drops.add(drop);
    }

    public void addBonusDrop(RewardEntry bonusDrop) {
        this.bonusDrops.add(bonusDrop);
    }

    public void generateRewards(Random random,
            List<com.hypixel.hytale.server.core.inventory.ItemStack> allItems,
            List<String> droppedItemIds) {

        for (RewardEntry drop : drops) {
            if (drop.shouldDrop(random)) {
                int count = drop.getRandomCount(random);
                allItems.add(new com.hypixel.hytale.server.core.inventory.ItemStack(drop.getId(), count));
                droppedItemIds.add(drop.getDisplayName() + " x" + count);
            }
        }

        for (RewardEntry bonusDrop : bonusDrops) {
            if (bonusDrop.shouldDrop(random)) {
                int count = bonusDrop.getRandomCount(random);
                allItems.add(new com.hypixel.hytale.server.core.inventory.ItemStack(bonusDrop.getId(), count));
                droppedItemIds.add(bonusDrop.getDisplayName() + " x" + count + " (bonus)");
            }
        }
    }

    @Override
    public String toString() {
        return "TierRewards{drops=" + drops.size() + ", bonusDrops=" + bonusDrops.size() + "}";
    }

    public static TierRewards getDefaultTier1() {
        TierRewards rewards = new TierRewards();
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Copper", 5, 7, 100, "Lingots de cuivre"));
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Iron", 5, 7, 100, "Lingots de fer"));
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Silver", 3, 6, 100, "Lingots d'argent"));
        rewards.addDrop(new RewardEntry("Ingredient_Life_Essence", 2, 4, 100, "Essence de vie"));
        rewards.addDrop(new RewardEntry("Ingredient_Leather_Light", 2, 4, 100, "Cuir léger"));
        rewards.addDrop(new RewardEntry("Potion_Health_Lesser", 1, 2, 5.0, "Potion de santé inférieure"));
        return rewards;
    }

    public static TierRewards getDefaultTier2() {
        TierRewards rewards = new TierRewards();
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Thorium", 5, 7, 50.0, "Lingots de thorium"));
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Gold", 3, 6, 100, "Lingots d'or"));
        rewards.addDrop(new RewardEntry("Ingredient_Leather_Light", 2, 3, 50.0, "Cuir léger"));
        rewards.addDrop(new RewardEntry("Ingredient_Leather_Medium", 2, 3, 50.0, "Cuir moyen"));
        rewards.addDrop(new RewardEntry("Ingredient_Fire_Essence", 3, 4, 100, "Essence de feu"));
        rewards.addDrop(new RewardEntry("Ingredient_Fabric_Scrap_Shadoweave", 5, 5, 100, "Chutes de tisseombre"));
        rewards.addDrop(new RewardEntry("Potion_Health", 1, 2, 5.0, "Potion de santé"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Copper", 2, 4, 35, "Lingots de cuivre"));
        rewards.addBonusDrop(new RewardEntry("Potion_Health_Lesser", 1, 1, 5.0, "Potion de santé inférieure"));
        return rewards;
    }

    public static TierRewards getDefaultTier3() {
        TierRewards rewards = new TierRewards();
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Cobalt", 5, 7, 50.0, "Lingots de cobalt"));
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Thorium", 5, 7, 50.0, "Lingots de thorium"));
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Gold", 3, 6, 100, "Lingots d'or"));
        rewards.addDrop(new RewardEntry("Ingredient_Leather_Medium", 2, 3, 50.0, "Cuir moyen"));
        rewards.addDrop(new RewardEntry("Ingredient_Leather_Heavy", 2, 3, 50.0, "Cuir lourd"));
        rewards.addDrop(new RewardEntry("Ingredient_Ice_Essence", 3, 4, 100, "Essence de glace"));
        rewards.addDrop(new RewardEntry("Potion_Health_Greater", 1, 2, 5.0, "Potion de santé supérieure"));
        rewards.addDrop(new RewardEntry("Ingredient_Fabric_Scrap_Shadoweave", 5, 5, 100, "Chutes de tisseombre"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Copper", 2, 4, 30, "Lingots de cuivre"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Iron", 2, 4, 30, "Lingots de fer"));
        rewards.addBonusDrop(new RewardEntry("Potion_Health_Lesser", 1, 1, 5.0, "Potion de santé inférieure"));
        rewards.addBonusDrop(new RewardEntry("Potion_Health", 1, 1, 5.0, "Potion de santé"));
        return rewards;
    }

    public static TierRewards getDefaultTier4() {
        TierRewards rewards = new TierRewards();
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Adamantite", 5, 8, 100, "Lingots d'adamantite"));
        rewards.addDrop(new RewardEntry("Ingredient_Leather_Heavy", 3, 4, 100, "Cuir lourd"));
        rewards.addDrop(new RewardEntry("Ingredient_Fire_Essence", 4, 6, 100, "Essence de feu"));
        rewards.addDrop(new RewardEntry("Ingredient_Fabric_Scrap_Shadoweave", 8, 10, 100, "Chutes de tisseombre"));
        rewards.addDrop(new RewardEntry("Potion_Health_Greater", 2, 3, 5.0, "Potion de santé supérieure"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Copper", 3, 5, 25, "Lingots de cuivre"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Iron", 3, 5, 25, "Lingots de fer"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Cobalt", 2, 4, 25, "Lingots de cobalt"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Thorium", 2, 4, 25, "Lingots de thorium"));
        rewards.addBonusDrop(new RewardEntry("Potion_Health_Lesser", 1, 2, 5.0, "Potion de santé inférieure"));
        rewards.addBonusDrop(new RewardEntry("Potion_Health", 1, 2, 5.0, "Potion de santé"));
        return rewards;
    }

    public static TierRewards getDefaultTier5() {
        TierRewards rewards = new TierRewards();
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Prisma", 6, 9, 100, "Lingots de prisma"));
        rewards.addDrop(new RewardEntry("Ingredient_Bar_Onyxium", 6, 9, 100, "Lingots d'onyxium"));
        rewards.addDrop(new RewardEntry("Ingredient_Hide_Prismic", 4, 6, 100, "Peau prismique"));
        rewards.addDrop(new RewardEntry("Ingredient_Hide_Storm", 4, 6, 100, "Peau de tempête"));
        rewards.addDrop(new RewardEntry("Ingredient_Fabric_Scrap_Cindercloth", 8, 12, 100, "Chutes de toile de cendres"));
        rewards.addDrop(new RewardEntry("Ingredient_Fabric_Scrap_Shadoweave", 8, 12, 100, "Chutes de tisseombre"));
        rewards.addDrop(new RewardEntry("Potion_Health_Greater", 2, 3, 5.0, "Potion de santé supérieure"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Bar_Adamantite", 2, 4, 30, "Lingots d'adamantite"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Hide_Prismic", 2, 3, 30, "Peau prismique"));
        rewards.addBonusDrop(new RewardEntry("Ingredient_Hide_Storm", 2, 3, 30, "Peau de tempête"));
        return rewards;
    }

    public static TierRewards getDefaultForTier(int tier) {
        switch (tier) {
            case 1:
                return getDefaultTier1();
            case 2:
                return getDefaultTier2();
            case 3:
                return getDefaultTier3();
            case 4:
                return getDefaultTier4();
            case 5:
                return getDefaultTier5();
            default:
                return getDefaultTier1();
        }
    }
}
