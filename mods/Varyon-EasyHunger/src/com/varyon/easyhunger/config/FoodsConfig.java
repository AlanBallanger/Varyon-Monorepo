package com.varyon.easyhunger.config;


import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration file for food hunger values.
 * Stored in: config/Foods.json
 */
public class FoodsConfig {
    
    private static final KeyedCodec<Map<String, Float>> FOOD_VALUES = new KeyedCodec<>("FoodValues", new MapCodec<>(Codec.FLOAT, LinkedHashMap::new));
    
    public static final BuilderCodec<FoodsConfig> CODEC = BuilderCodec.builder(FoodsConfig.class, FoodsConfig::new)
            .addField(FOOD_VALUES, (c, v) -> c.foodValues = v, FoodsConfig::getFoodValues)
            .build();
    
    private Map<String, Float> foodValues;
    
    public FoodsConfig() {
        foodValues = new LinkedHashMap<>();
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Cooked/Prepared Foods
        foodValues.put("Food_Beef_Raw", 3.75f);
        foodValues.put("Food_Bread", 5.5f);
        foodValues.put("Food_Candy_Cane", 2.25f);
        foodValues.put("Food_Cheese", 4.75f);
        foodValues.put("Food_Chicken_Raw", 2.75f);
        foodValues.put("Food_Egg", 1.75f);
        foodValues.put("Food_Fish_Grilled", 9.5f);
        foodValues.put("Food_Fish_Raw", 2.5f);
        foodValues.put("Food_Fish_Raw_Epic", 7.5f);
        foodValues.put("Food_Fish_Raw_Legendary", 10.25f);
        foodValues.put("Food_Fish_Raw_Rare", 5.25f);
        foodValues.put("Food_Fish_Raw_Uncommon", 3.75f);
        foodValues.put("Food_Kebab_Fruit", 7.5f);
        foodValues.put("Food_Kebab_Meat", 14.5f);
        foodValues.put("Food_Kebab_Mushroom", 9.75f);
        foodValues.put("Food_Kebab_Vegetable", 9.25f);
        foodValues.put("Food_Pie_Apple", 13.75f);
        foodValues.put("Food_Pie_Meat", 18.5f);
        foodValues.put("Food_Pie_Pumpkin", 15.25f);
        foodValues.put("Food_Popcorn", 1.5f);
        foodValues.put("Food_Pork_Raw", 3.5f);
        foodValues.put("Food_Salad_Berry", 5.75f);
        foodValues.put("Food_Salad_Caesar", 7.75f);
        foodValues.put("Food_Salad_Mushroom", 6.5f);
        foodValues.put("Food_Vegetable_Cooked", 7.25f);
        foodValues.put("Food_Wildmeat_Cooked", 11.5f);
        foodValues.put("Food_Wildmeat_Raw", 3.25f);
        
        // Halloween Items
        foodValues.put("Halloween_Basket_Pumpkin", 14.75f);
        foodValues.put("Halloween_Basket_Straw", 11.5f);
        
        // Ingredients
        foodValues.put("Ingredient_Dough", 0.75f);
        foodValues.put("Ingredient_Flour", 0.5f);
        foodValues.put("Ingredient_Salt", 0.0f);
        foodValues.put("Ingredient_Spices", 0.0f);
        
        // Crops
        foodValues.put("Plant_Crop_Aubergine_Item", 2.0f);
        foodValues.put("Plant_Crop_Carrot_Item", 1.75f);
        foodValues.put("Plant_Crop_Cauliflower_Item", 2.25f);
        foodValues.put("Plant_Crop_Chilli_Item", 1.75f);
        foodValues.put("Plant_Crop_Corn_Item", 2.75f);
        foodValues.put("Plant_Crop_Lettuce_Item", 1.25f);
        foodValues.put("Plant_Crop_Onion_Item", 0.75f);
        foodValues.put("Plant_Crop_Potato_Item", 2.25f);
        foodValues.put("Plant_Crop_Pumpkin_Item", 3.5f);
        foodValues.put("Plant_Crop_Rice_Item", 1.5f);
        foodValues.put("Plant_Crop_Tomato_Item", 1.5f);
        foodValues.put("Plant_Crop_Turnip_Item", 1.75f);
        
        // Fruits
        foodValues.put("Plant_Fruit_Apple", 3.5f);
        foodValues.put("Plant_Fruit_Azure", 2.75f);
        foodValues.put("Plant_Fruit_Berries_Red", 1.75f);
        foodValues.put("Plant_Fruit_Coconut", 3.75f);
        foodValues.put("Plant_Fruit_Mango", 3.75f);
        foodValues.put("Plant_Fruit_Pinkberry", 2.5f);
        foodValues.put("Plant_Fruit_Poison", 2.25f);
        foodValues.put("Plant_Fruit_Spiral", 2.75f);
        foodValues.put("Plant_Fruit_Windwillow", 2.5f);
        
        // NoCube Orchard - Fruits
        foodValues.put("NoCube_Plant_Fruit_Apricot", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Lemon", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Mandarin", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Orange", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Peach", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Pear", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Persimmon", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Plum", 5.0f);
        foodValues.put("NoCube_Plant_Fruit_Pomegranate", 5.0f);
        
        // NoCube Culinary - Prepared Foods
        foodValues.put("NoCube_Food_Breakfast", 25.0f);
        foodValues.put("NoCube_Food_Chicken_Nuggets", 20.0f);
        foodValues.put("NoCube_Food_Ratatouille", 22.0f);
        foodValues.put("NoCube_Food_Roast_Bird", 24.0f);
        
        // NoCube Bakehouse - Ingredients
        foodValues.put("NoCube_Ingredient_Corn_Dough", 3.0f);
        foodValues.put("NoCube_Ingredient_Corn_Meal", 3.0f);
        foodValues.put("NoCube_Ingredient_Dough", 3.0f);
        foodValues.put("NoCube_Ingredient_Fine_Dough", 3.0f);
        foodValues.put("NoCube_Ingredient_Rice_Dough", 3.0f);
        foodValues.put("NoCube_Ingredient_Rice_Powder", 3.0f);
        foodValues.put("NoCube_Ingredient_Sifted_Flour", 3.0f);
        foodValues.put("NoCube_Ingredient_Sugar", 3.0f);
        foodValues.put("NoCube_Ingredient_Flour", 3.0f);
        foodValues.put("NoCube_Ingredient_Flour_Grinding", 3.0f);
        foodValues.put("NoCube_Ingredient_Salt", 3.0f);
        
        // NoCube Bakehouse - Breads (Done folder)
        foodValues.put("NoCube_Food_Baguette", 28.0f);
        foodValues.put("NoCube_Food_Bread", 25.0f);
        foodValues.put("NoCube_Food_Brioche", 30.0f);
        foodValues.put("NoCube_Food_Carrot_Bread", 27.0f);
        foodValues.put("NoCube_Food_Coconut_Bread", 29.0f);
        foodValues.put("NoCube_Food_Corn_Bread", 26.0f);
        foodValues.put("NoCube_Food_Damper", 25.0f);
        foodValues.put("NoCube_Food_Fruitloaf", 32.0f);
        foodValues.put("NoCube_Food_Herb_Bread", 28.0f);
        foodValues.put("NoCube_Food_Peasant_Bread", 25.0f);
        foodValues.put("NoCube_Food_Potato_Bread", 27.0f);
        foodValues.put("NoCube_Food_Pumpkin_Bread", 29.0f);
        foodValues.put("NoCube_Food_Rice_Bread", 26.0f);
        foodValues.put("NoCube_Food_Rustic_Bread", 26.0f);
        foodValues.put("NoCube_Food_Wholegrain_Bread", 28.0f);
        
        // NoCube Bakehouse - Pastries
        foodValues.put("NoCube_Food_Berry_Croissant", 30.0f);
        foodValues.put("NoCube_Food_Bun", 25.0f);
        foodValues.put("NoCube_Food_Croissant", 20.0f);
        foodValues.put("NoCube_Food_Eclair", 35.0f);
        foodValues.put("NoCube_Food_Onion_Baguette", 32.0f);
        
        // AleAndHearth - Foods
        foodValues.put("Food_Plate_Egg", 20.0f);
        foodValues.put("Food_Plate_Sausages", 21.0f);
        foodValues.put("Food_Sandwich", 25.0f);
        
        // AleAndHearth - Soups (all Uncommon)
        foodValues.put("Bowl_Soup_Borsch", 18.0f);
        foodValues.put("Bowl_Soup_Cheese", 16.0f);
        foodValues.put("Bowl_Soup_Chicken", 17.0f);
        foodValues.put("Bowl_Soup_Fish", 19.0f);
        foodValues.put("Bowl_Soup_Mushroom", 15.0f);
        foodValues.put("Bowl_Soup_Pumpkin", 20.0f);
        
        // Food Galore - Foods
        foodValues.put("Burger", 18.0f);
        foodValues.put("EggToast", 17.0f);
        foodValues.put("FriedEgg", 16.0f);
        foodValues.put("GoldenApple", 25.0f);
        foodValues.put("GoldenCarrot", 23.0f);
        foodValues.put("IceCream", 17.0f);
        foodValues.put("Omelet", 18.0f);
        foodValues.put("Onigiri", 17.0f);
        foodValues.put("Pizza", 19.0f);
        foodValues.put("Pudding", 18.0f);
        foodValues.put("Sandwich", 15.0f);
        foodValues.put("SprinkleIceCream", 19.0f);
        foodValues.put("Sushi", 18.0f);
        foodValues.put("Toast", 16.0f);
        
        // HiddensHarvestDelights - Foods by Rarity
        // Unique (45 - highest)
        foodValues.put("Steak_Dinner", 45.0f);
        
        // Mythic (43)
        foodValues.put("Chicken_Buttered", 43.0f);
        
        // Legendary (38-42)
        foodValues.put("Beef_Well_Don", 42.0f);
        foodValues.put("General_Whos_Chicken", 41.0f);
        foodValues.put("Fajita_Skillet", 40.0f);
        foodValues.put("Chicken_Dumplings", 40.0f);
        foodValues.put("Omelette_Fried_Rice", 39.0f);
        foodValues.put("Pork_Cutlet_Sandwhich", 39.0f);
        foodValues.put("Three_Elote", 38.0f);
        
        // Epic (32-37)
        foodValues.put("Spaghetti", 37.0f);
        foodValues.put("Saucy_Katsudon", 36.0f);
        foodValues.put("Pizza_Flatbread", 35.0f);
        foodValues.put("Veggie_Pizza_Flatbread", 35.0f);
        foodValues.put("Mac_Cheese", 34.0f);
        foodValues.put("Fish_Tacos", 34.0f);
        foodValues.put("Burger_Fries", 33.0f);
        foodValues.put("Cheese_Steak", 33.0f);
        foodValues.put("Birthday_Cake", 33.0f);
        foodValues.put("Chocolate_Swissroll", 32.0f);
        foodValues.put("Egg_Benny_Toast", 32.0f);
        foodValues.put("Egg_Toast", 32.0f);
        foodValues.put("Junk_Cake", 32.0f);
        foodValues.put("Meat_Toast", 32.0f);
        
        // Rare (25-30)
        foodValues.put("Avacado_Toast", 30.0f);
        foodValues.put("Charcuterie_Board", 29.0f);
        foodValues.put("Completo_Plate", 28.0f);
        foodValues.put("Fish_Tacos_Corn", 28.0f);
        foodValues.put("Fried_Chicken_Dinner", 28.0f);
        foodValues.put("Hot_Dogs", 27.0f);
        foodValues.put("Kebab_Beef", 27.0f);
        foodValues.put("Minced_Meat_Cutlet", 27.0f);
        foodValues.put("Potato_Nachos", 26.0f);
        foodValues.put("Regular_Cheesecake", 26.0f);
        foodValues.put("Shepps_Pie", 26.0f);
        foodValues.put("Sushi_Platter", 26.0f);
        foodValues.put("Torta_Milanesa", 25.0f);
        foodValues.put("Tostada_Homemade", 25.0f);
        foodValues.put("Tuna_Rice", 25.0f);
        foodValues.put("Twice_Baked_Potato", 25.0f);
        foodValues.put("Wings_And_Fries", 25.0f);
        
        // Uncommon (20-24)
        foodValues.put("Berry_Glazed_Cod", 23.0f);
        foodValues.put("Hamburger_Steak", 22.0f);
        foodValues.put("Steak_Tower", 21.0f);
        
        // Common/Ingredients (20)
        foodValues.put("HiddenIsme_Pico", 20.0f);
        foodValues.put("Ingredient_Butter", 20.0f);
        foodValues.put("Ingredient_Sugar", 20.0f);
        
        // Connor's More Food Stuff - Foods
        foodValues.put("Food_Pie_Pink_Berry", 30.0f);
        foodValues.put("Food_Mango_Pie", 27.0f);
        foodValues.put("Food_Pie_Azure", 26.0f);
        foodValues.put("Food_Pie_Berry", 25.0f);
        foodValues.put("Food_Salad_Corn", 22.0f);
        foodValues.put("Food_Bread_Slices", 20.0f);
        foodValues.put("Food_Butter", 20.0f);
        
        // SNIP3_FoodPack - Foods by Rarity
        // Legendary (38-42)
        foodValues.put("Food_Mud_Cake", 42.0f);
        foodValues.put("Food_Red_Velvet_Cake", 41.0f);
        foodValues.put("Food_Special_Cake", 40.0f);
        foodValues.put("Food_Takoyaki", 39.0f);
        foodValues.put("Food_Taco", 38.0f);
        
        // Epic (32-37)
        foodValues.put("Food_Sandwich_Steak_Cheese", 37.0f);
        foodValues.put("Food_Ramen_Pork", 36.0f);
        foodValues.put("Food_Pizza_Pepperoni", 35.0f);
        foodValues.put("Food_Pizza_Kebab", 35.0f);
        foodValues.put("Food_Pasta_Carbonara", 34.0f);
        foodValues.put("Food_Pasta_Bologonese", 34.0f);
        foodValues.put("Food_Kebab_Bread", 33.0f);
        
        // Rare (25-30)
        foodValues.put("Food_Sushi_Tuna", 30.0f);
        foodValues.put("Food_Sushi_Salmon", 29.0f);
        foodValues.put("Food_Sushi_Roll", 29.0f);
        foodValues.put("Food_Sushi_Egg", 28.0f);
        foodValues.put("Food_Steak_Chip", 28.0f);
        foodValues.put("Food_Soup_Mushroom", 27.0f);
        foodValues.put("Food_Soup_Meat", 27.0f);
        foodValues.put("Food_Soup_BeetRoot", 26.0f);
        foodValues.put("Food_Sandwich_Fried_Egg", 26.0f);
        foodValues.put("Food_Roasted_Cauliflower", 26.0f);
        foodValues.put("Food_Pizza_Mushroom", 26.0f);
        foodValues.put("Food_Pizza_Cheese", 25.0f);
        foodValues.put("Food_Pasta_Tomato", 25.0f);
        foodValues.put("Food_Pasta_Mushroom", 25.0f);
        foodValues.put("Food_Omelette", 25.0f);
        foodValues.put("Food_Oat_Porridge_Berry", 25.0f);

        
        // Uncommon (20-24)
        foodValues.put("Food_Sauce_Dog", 24.0f);
        foodValues.put("Food_Pasta", 23.0f);
        foodValues.put("Food_Oat_Porridge", 22.0f);

        foodValues.put("Food_Bibimbap", 24.0f);
        foodValues.put("Food_Cheese_Burger", 23.0f);
        foodValues.put("Food_Cheese_Cake", 24.0f);
        foodValues.put("Food_Chicken_Burger", 23.0f);
        foodValues.put("Food_Chicken_Nuggets", 22.0f);
        foodValues.put("Food_Chicken_Tenders", 22.0f);
        foodValues.put("Food_Chocolate_Cake", 24.0f);
        foodValues.put("Food_Completo", 23.0f);
        foodValues.put("Food_Dumpling_Gyoza", 22.0f);
        foodValues.put("Food_Dumpling_Samosa", 22.0f);
        foodValues.put("Food_Egg_Bacon", 21.0f);
        foodValues.put("Food_Falafel_Bread", 23.0f);
        foodValues.put("Food_Fish_Chip", 22.0f);
        foodValues.put("Food_Fried_Egg", 20.0f);
        foodValues.put("Food_Fried_Potato", 20.0f);
        foodValues.put("Food_Grilled_Aubergine", 21.0f);
        foodValues.put("Food_Gyudon", 24.0f);
        foodValues.put("Food_Ham_Burger", 22.0f);
        foodValues.put("Food_Hot_Dog", 21.0f);
        
        // Common/Ingredients (15-20)
        foodValues.put("Ingredient_Raw_Pasta", 5.0f);
        foodValues.put("Ingredient_Raw_Fries_Potato", 5.0f);
        
        // AndiemgCheff - Foods
        foodValues.put("AndiemgCheff_Food_Nigiri", 40.0f);
        foodValues.put("AndiemgCheff_Food_Rollo", 27.0f);
        foodValues.put("AndiemgCheff_Food_Onigiri", 26.0f);
        foodValues.put("AndiemgCheff_YakimeshiBeef", 24.0f);
        foodValues.put("AndiemgCheff_YakimeshiChicken", 23.0f);
        foodValues.put("AndiemgCheff_YakimeshiFish", 23.0f);
        foodValues.put("AndiemgCheff_YakimeshiPork", 22.0f);
        
        // ZC Gaia - Foods by Rarity
        // Epic (32-37)

        
        // Rare (25-30)
        foodValues.put("ZC_Sushi", 30.0f);
        foodValues.put("ZC_FriedEggplant", 28.0f);
        foodValues.put("ZC_ChiliCaliflower", 27.0f);

        
        // Uncommon (15-20)
        foodValues.put("ZC_AuGrat", 20.0f);
        foodValues.put("ZC_BreakfastScramble", 20.0f);
        foodValues.put("ZC_FishChips", 20.0f);
        foodValues.put("ZC_FishEggplantSoup", 19.0f);
        foodValues.put("ZC_EggplantStirfry", 19.0f);
        foodValues.put("ZC_Sketti", 19.0f);
        foodValues.put("ZC_VeggieStew", 19.0f);
        foodValues.put("ZC_CarrotCake", 18.0f);
        foodValues.put("ZC_CheesyEggplant", 18.0f);
        foodValues.put("ZC_FishTaco", 18.0f);
        foodValues.put("ZC_SquashSuasageSoup", 18.0f);
        foodValues.put("ZC_PotatoSoup", 17.0f);
        foodValues.put("ZC_StuffedTurnip", 17.0f);
        foodValues.put("ZC_Tomato_Soup", 17.0f);
        foodValues.put("ZC_AzureMuffin", 16.0f);
        foodValues.put("ZC_BerryMuffin", 16.0f);
        foodValues.put("ZC_CauliTaco", 16.0f);
        foodValues.put("ZC_FriedEggs", 15.0f);
        foodValues.put("ZC_Sausage", 15.0f);

        // EndgameAndQoL - Foods
        foodValues.put("Food_Meat_Coooked_Alpha_Trex", 48.0f);
        foodValues.put("Food_Meat_Raw_Alpha_Trex", 12.5f);

        // Spice Of Life - Foods & Ingredients
        foodValues.put("SOL_Food_RibsBBQ", 42.0f);
        foodValues.put("SOL_Food_Bangers", 27.0f);
        foodValues.put("SOL_Food_Wellington_Beef", 44.0f);
        foodValues.put("SOL_Biscuits", 18.0f);
        foodValues.put("SOL_Food_Chips", 20.0f);
        foodValues.put("SOL_Food_CakeAzure", 40.0f);
        foodValues.put("SOL_Food_CakeBasic", 32.0f);
        foodValues.put("SOL_Food_CakeBerry", 33.0f);
        foodValues.put("SOL_Food_CakeCarrot", 34.0f);
        foodValues.put("SOL_Food_CakeChoc", 41.0f);
        foodValues.put("SOL_Food_CakeFrosted", 42.0f);
        foodValues.put("SOL_Food_Caramel", 20.0f);
        foodValues.put("SOL_Food_ChocolateDark", 22.0f);
        foodValues.put("SOL_Food_ChocolateMilk", 21.0f);
        foodValues.put("SOL_Food_ChocolateRuby", 23.0f);
        foodValues.put("SOL_Food_ChocolateWhite", 21.0f);
        foodValues.put("SOL_Plant_Fruit_CocoaBeans", 2.75f);
        foodValues.put("SOL_Ingredient_CocoaPowder", 3.0f);
        foodValues.put("SOL_Food_CookedEggs", 20.0f);
        foodValues.put("SOL_Cookies_Milk", 19.0f);
        foodValues.put("SOL_Cookies_Trip", 21.0f);
        foodValues.put("SOL_Cookies_BerryWhite", 20.0f);
        foodValues.put("SOL_Food_CornOnCob", 7.25f);
        foodValues.put("SOL_Ingredient_Cream", 20.0f);
        foodValues.put("SOL_Food_CrispSalad", 6.75f);
        foodValues.put("SOL_Food_SmokedSalmon", 29.0f);
        foodValues.put("SOL_Food_FishChips", 34.0f);
        foodValues.put("SOL_Food_ChickenFried", 28.0f);
        foodValues.put("SOL_Food_GrilledAubergine", 21.0f);
        foodValues.put("SOL_Food_SnackCreamCheese", 5.0f);
        foodValues.put("SOL_Ingredient_Honey", 20.0f);
        foodValues.put("SOL_Food_RoastHam", 40.0f);
        foodValues.put("SOL_Food_IceCreamAzure", 18.0f);
        foodValues.put("SOL_Food_IceCreamBerry", 19.0f);
        foodValues.put("SOL_Food_IceCreamChoc", 20.0f);
        foodValues.put("SOL_Food_IceCreamMint", 18.0f);
        foodValues.put("SOL_Food_IceCreamVanilla", 17.0f);
        foodValues.put("SOL_Food_Jello", 16.0f);
        foodValues.put("SOL_Food_Lasagna", 34.0f);
        foodValues.put("SOL_Food_Risotto_Mushroom", 27.0f);
        foodValues.put("SOL_Food_Pancakes", 22.0f);
        foodValues.put("SOL_Food_Pasta_Spicy", 34.0f);
        foodValues.put("SOL_Food_Pasta_Carb", 34.0f);
        foodValues.put("SOL_Food_Pasta_Meatball", 35.0f);
        foodValues.put("SOL_Food_Pasta_Tomato", 25.0f);
        foodValues.put("SOL_Food_LinksRaw", 3.5f);
        foodValues.put("SOL_Food_PastaRaw", 5.0f);
        foodValues.put("SOL_Food_RoastChicken", 41.0f);
        foodValues.put("SOL_Food_Wellington_Salmon", 42.0f);
        foodValues.put("SOL_Food_SteakChips", 28.0f);
        foodValues.put("SOL_Food_Waffles", 23.0f);
        foodValues.put("SOL_Food_CheesecakeBlueberry", 26.0f);
    }
    
    public Map<String, Float> getFoodValues() { return foodValues; }
    
    public Float getFoodValue(String foodId) {
        Float value = foodValues.get(foodId);
        return value != null ? value : 0.0f;
    }
    
    public void setFoodValue(String foodId, float value) {
        try {
            foodValues.put(foodId, value);
        } catch (UnsupportedOperationException e) {
            // Map is unmodifiable, create a mutable copy
            foodValues = new LinkedHashMap<>(foodValues);
            foodValues.put(foodId, value);
        }
    }
    
    /**
     * Merges default values into the current config.
     * Only adds entries that don't already exist - preserves user customizations.
     * @return true if any new entries were added
     */
    public boolean mergeDefaults() {
        FoodsConfig defaultConfig = new FoodsConfig();
        Map<String, Float> defaults = defaultConfig.getFoodValues();
        
        // Create mutable copy if current map is unmodifiable
        Map<String, Float> mutableValues = new LinkedHashMap<>(foodValues);
        
        boolean changed = false;
        for (Map.Entry<String, Float> entry : defaults.entrySet()) {
            if (!mutableValues.containsKey(entry.getKey())) {
                mutableValues.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        
        if (changed) {
            foodValues = mutableValues;
        }
        return changed;
    }
}
