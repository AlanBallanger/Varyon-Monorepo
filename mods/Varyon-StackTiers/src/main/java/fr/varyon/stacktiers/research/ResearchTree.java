package fr.varyon.stacktiers.research;

import fr.varyon.stacktiers.StackCategories;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Topologie fixe de l'arbre de recherche : 39 nœuds (13 catégories x 3 tiers) répartis sur
 * 5 colonnes (pas toutes remplies à chaque tier, pour un rendu moins rigide qu'une grille) et
 * 3 bandes de rangées groupées par tier (0-2 = palier 1, 3-5 = palier 2, 6-8 = palier 3).
 * Codée en dur (pas un fichier ressource) — contrairement à stackable_items.json côté
 * Mixin-Varyon-StackSize (des milliers d'entrées dérivées des assets du jeu), cette table
 * est petite, fixe, et bénéficie d'être vérifiée au compile-time / fail-fast au démarrage.
 */
public final class ResearchTree {

    public static final List<ResearchNode> NODES = List.of(
        new ResearchNode("soil_1", "soil", 1, 0, 0, List.of(), "Soil_Dirt", 500, 600000L),
        new ResearchNode("soil_2", "soil", 2, 0, 3, List.of("fish_1", "soil_1"), "Soil_Dirt", 1250, 5400000L),
        new ResearchNode("soil_3", "soil", 3, 0, 6, List.of("ingredient_2", "soil_2"), "Soil_Dirt", 3125, 18000000L),
        new ResearchNode("wood_1", "wood", 1, 0, 1, List.of("soil_1"), "Wood_Ash_Trunk", 500, 600000L),
        new ResearchNode("wood_2", "wood", 2, 0, 4, List.of("fish_1", "wood_1"), "Wood_Ash_Trunk", 1250, 5400000L),
        new ResearchNode("wood_3", "wood", 3, 1, 6, List.of("rock_2", "wood_2"), "Wood_Ash_Trunk", 3125, 18000000L),
        new ResearchNode("rock_1", "rock", 1, 0, 2, List.of("soil_1"), "Rock_Stone", 500, 600000L),
        new ResearchNode("rock_2", "rock", 2, 0, 5, List.of("fish_1", "rock_1"), "Rock_Stone", 1250, 5400000L),
        new ResearchNode("rock_3", "rock", 3, 1, 7, List.of("rock_2"), "Rock_Stone", 3125, 18000000L),
        new ResearchNode("plant_1", "plant", 1, 1, 0, List.of("soil_1"), "Plant_Crop_Carrot_Item", 200, 600000L),
        new ResearchNode("plant_2", "plant", 2, 1, 3, List.of("metal_1", "plant_1"), "Plant_Crop_Carrot_Item", 500, 5400000L),
        new ResearchNode("plant_3", "plant", 3, 1, 8, List.of("plant_2", "rock_2"), "Plant_Crop_Carrot_Item", 1250, 18000000L),
        new ResearchNode("cloth_1", "cloth", 1, 1, 1, List.of("wood_1"), "Cloth_Block_Wool_Black", 100, 1200000L),
        new ResearchNode("cloth_2", "cloth", 2, 1, 4, List.of("cloth_1", "metal_1"), "Cloth_Block_Wool_Black", 250, 7200000L),
        new ResearchNode("cloth_3", "cloth", 3, 0, 7, List.of("cloth_2", "ingredient_2"), "Cloth_Block_Wool_Black", 625, 21600000L),
        new ResearchNode("ingredient_1", "ingredient", 1, 2, 0, List.of("plant_1"), "Ingredient_Crystal_Blue", 100, 1200000L),
        new ResearchNode("ingredient_2", "ingredient", 2, 1, 5, List.of("ingredient_1", "metal_1"), "Ingredient_Crystal_Blue", 250, 7200000L),
        new ResearchNode("ingredient_3", "ingredient", 3, 2, 6, List.of("ingredient_2"), "Ingredient_Crystal_Blue", 625, 21600000L),
        new ResearchNode("fish_1", "fish", 1, 1, 2, List.of("rock_1"), "Fish_Catfish_Item", 50, 1200000L),
        new ResearchNode("fish_2", "fish", 2, 2, 3, List.of("fish_1"), "Fish_Catfish_Item", 125, 7200000L),
        new ResearchNode("fish_3", "fish", 3, 2, 7, List.of("fish_2", "ingredient_2"), "Fish_Catfish_Item", 312, 21600000L),
        new ResearchNode("food_1", "food", 1, 2, 1, List.of("cloth_1"), "Food_Bread", 15, 1200000L),
        new ResearchNode("food_2", "food", 2, 2, 4, List.of("fish_1", "food_1"), "Food_Bread", 150, 7200000L),
        new ResearchNode("food_3", "food", 3, 3, 6, List.of("food_2"), "Food_Bread", 1200, 21600000L),
        new ResearchNode("potion_1", "potion", 1, 4, 0, List.of("resources_1"), "Potion_Health", 15, 1200000L),
        new ResearchNode("potion_2", "potion", 2, 3, 3, List.of("furniture_1", "potion_1"), "Potion_Health", 150, 7200000L),
        new ResearchNode("potion_3", "potion", 3, 3, 7, List.of("food_2", "potion_2"), "Potion_Health", 1200, 21600000L),
        new ResearchNode("metal_1", "metal", 1, 2, 2, List.of("fish_1"), "Metal_Iron", 100, 1800000L),
        new ResearchNode("metal_2", "metal", 2, 3, 4, List.of("furniture_1", "metal_1"), "Metal_Iron", 250, 9000000L),
        new ResearchNode("metal_3", "metal", 3, 3, 8, List.of("food_2", "metal_2"), "Metal_Iron", 625, 25200000L),
        new ResearchNode("ore_1", "ore", 1, 4, 1, List.of("resources_1"), "Ore_Iron", 100, 1800000L),
        new ResearchNode("ore_2", "ore", 2, 3, 5, List.of("furniture_1", "ore_1"), "Ore_Iron", 250, 9000000L),
        new ResearchNode("ore_3", "ore", 3, 4, 6, List.of("ore_2"), "Ore_Iron", 625, 25200000L),
        new ResearchNode("furniture_1", "furniture", 1, 4, 2, List.of("resources_1"), "Furniture_Ancient_Chest_Small", 20, 1800000L),
        new ResearchNode("furniture_2", "furniture", 2, 4, 3, List.of("furniture_1", "resources_1"), "Furniture_Ancient_Chest_Small", 200, 9000000L),
        new ResearchNode("furniture_3", "furniture", 3, 4, 7, List.of("furniture_2", "ore_2"), "Furniture_Ancient_Chest_Small", 1600, 25200000L),
        new ResearchNode("resources_1", "resources", 1, 3, 0, List.of("ingredient_1"), "Rock_Gem_Diamond", 100, 3600000L),
        new ResearchNode("resources_2", "resources", 2, 4, 4, List.of("resources_1"), "Rock_Gem_Diamond", 250, 14400000L),
        new ResearchNode("resources_3", "resources", 3, 4, 8, List.of("ore_2", "resources_2"), "Rock_Gem_Diamond", 625, 28800000L)
    );

    private static final Map<String, ResearchNode> BY_ID =
            NODES.stream().collect(Collectors.toMap(ResearchNode::id, n -> n));

    private static final Map<String, ResearchNode> BY_CATEGORY_TIER =
            NODES.stream().collect(Collectors.toMap(n -> n.category() + "|" + n.tier(), n -> n));

    static {
        if (NODES.size() != 39) {
            throw new IllegalStateException("Expected 39 research nodes, found " + NODES.size());
        }
        for (ResearchNode n : NODES) {
            if (!StackCategories.isValid(n.category())) {
                throw new IllegalStateException("Unknown category on node " + n.id() + ": " + n.category());
            }
            if (n.tier() < 1 || n.tier() > 3) {
                throw new IllegalStateException("Invalid tier on node " + n.id() + ": " + n.tier());
            }
            for (String prereq : n.prerequisiteIds()) {
                if (BY_ID.get(prereq) == null) {
                    throw new IllegalStateException("Node " + n.id() + " references unknown prerequisite " + prereq);
                }
            }
        }
    }

    public static ResearchNode byId(String id) {
        return BY_ID.get(id);
    }

    public static ResearchNode byCategoryTier(String category, int tier) {
        return BY_CATEGORY_TIER.get(category + "|" + tier);
    }

    public static List<ResearchNode> all() {
        return NODES;
    }

    /**
     * Un nœud est déblocable si TOUS ses prérequis sont satisfaits — à la fois les nœuds
     * de l'arbre qui y mènent (liens verticaux et transversaux) et, implicitement, le tier
     * précédent de sa propre catégorie puisque celui-ci figure dans la liste des prérequis.
     * Les nœuds racines (aucun prérequis) sont ouverts d'emblée.
     */
    public static boolean isUnlockable(ResearchNode node, java.util.Set<String> completedNodeIds) {
        for (String prereq : node.prerequisiteIds()) {
            if (!completedNodeIds.contains(prereq)) {
                return false;
            }
        }
        return true;
    }

    /** Prérequis d'un nœud qui ne sont pas encore débloqués. */
    public static List<String> missingPrerequisites(ResearchNode node, java.util.Set<String> completedNodeIds) {
        List<String> missing = new java.util.ArrayList<>();
        for (String prereq : node.prerequisiteIds()) {
            if (!completedNodeIds.contains(prereq)) {
                missing.add(prereq);
            }
        }
        return missing;
    }

    /** Chaîne de prérequis d'un nœud jusqu'à la racine (lui-même inclus), triée du tier le plus bas au plus haut. */
    public static List<String> chainUpTo(String category, int tier) {
        List<String> chain = new java.util.ArrayList<>();
        for (int t = 1; t <= tier; t++) {
            ResearchNode n = byCategoryTier(category, t);
            if (n != null) {
                chain.add(n.id());
            }
        }
        return chain;
    }

    private ResearchTree() {
    }
}
