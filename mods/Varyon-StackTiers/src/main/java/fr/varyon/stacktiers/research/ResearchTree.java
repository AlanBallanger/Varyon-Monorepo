package fr.varyon.stacktiers.research;

import fr.varyon.stacktiers.StackCategories;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Topologie fixe de l'arbre de recherche : 33 nœuds (11 catégories x 3 tiers) sur une grille
 * dessinée à la main (4 colonnes fixes, 4 rangées par palier — 0-3 = palier 1, 4-7 = palier 2,
 * 8-11 = palier 3), avec la même structure de liens répétée à l'identique à chaque palier :
 * Pierre/Bois/Poisson/Tissu en racines (rangée 0), Terre=Pierre+Bois et Plante=Poisson+Tissu
 * (rangée 1), Cristal=Bois+Terre+Plante et Mobilier=Tissu+Plante (rangée 2, décalés d'une
 * rangée par rapport à Terre/Plante), Métal=Terre+Cristal, Nourriture=Plante+Ingrédient+Mobilier
 * et Potion=Mobilier (rangée 3) — plus, pour chaque nœud de tier > 1, le tier précédent de sa
 * propre catégorie comme prérequis obligatoire supplémentaire (voir isUnlockable). Codée en dur
 * (pas un fichier ressource) — contrairement à stackable_items.json côté Mixin-Varyon-StackSize
 * (des milliers d'entrées dérivées des assets du jeu), cette table est petite, fixe, et
 * bénéficie d'être vérifiée au compile-time / fail-fast au démarrage.
 */
public final class ResearchTree {

    public static final List<ResearchNode> NODES = List.of(
        new ResearchNode("soil_1", "soil", 1, 0, 1, List.of("rock_1", "wood_1"), "Soil_Dirt", 500, 600000L),
        new ResearchNode("soil_2", "soil", 2, 0, 5, List.of("rock_2", "wood_2", "soil_1"), "Soil_Dirt", 1250, 5400000L),
        new ResearchNode("soil_3", "soil", 3, 0, 9, List.of("rock_3", "wood_3", "soil_2"), "Soil_Dirt", 3125, 18000000L),
        new ResearchNode("wood_1", "wood", 1, 1, 0, List.of(), "Wood_Ash_Trunk", 500, 600000L),
        new ResearchNode("wood_2", "wood", 2, 1, 4, List.of("wood_1", "ingredient_1"), "Wood_Ash_Trunk", 1250, 5400000L),
        new ResearchNode("wood_3", "wood", 3, 1, 8, List.of("wood_2", "ingredient_2"), "Wood_Ash_Trunk", 3125, 18000000L),
        new ResearchNode("rock_1", "rock", 1, 0, 0, List.of(), "Rock_Stone", 500, 600000L),
        new ResearchNode("rock_2", "rock", 2, 0, 4, List.of("rock_1", "metal_1"), "Rock_Stone", 1250, 5400000L),
        new ResearchNode("rock_3", "rock", 3, 0, 8, List.of("rock_2", "metal_2"), "Rock_Stone", 3125, 18000000L),
        new ResearchNode("plant_1", "plant", 1, 2, 1, List.of("fish_1", "cloth_1"), "Plant_Crop_Carrot_Item", 200, 600000L),
        new ResearchNode("plant_2", "plant", 2, 2, 5, List.of("fish_2", "cloth_2", "plant_1"), "Plant_Crop_Carrot_Item", 500, 5400000L),
        new ResearchNode("plant_3", "plant", 3, 2, 9, List.of("fish_3", "cloth_3", "plant_2"), "Plant_Crop_Carrot_Item", 1250, 18000000L),
        new ResearchNode("cloth_1", "cloth", 1, 3, 0, List.of(), "Cloth_Block_Wool_Black", 100, 1200000L),
        new ResearchNode("cloth_2", "cloth", 2, 3, 4, List.of("cloth_1", "potion_1"), "Cloth_Block_Wool_Black", 250, 7200000L),
        new ResearchNode("cloth_3", "cloth", 3, 3, 8, List.of("cloth_2", "potion_2"), "Cloth_Block_Wool_Black", 625, 21600000L),
        new ResearchNode("ingredient_1", "ingredient", 1, 1, 2, List.of("wood_1", "soil_1", "plant_1"), "Ingredient_Crystal_Blue", 100, 1200000L),
        new ResearchNode("ingredient_2", "ingredient", 2, 1, 6, List.of("wood_2", "soil_2", "plant_2", "ingredient_1"), "Ingredient_Crystal_Blue", 250, 7200000L),
        new ResearchNode("ingredient_3", "ingredient", 3, 1, 10, List.of("wood_3", "soil_3", "plant_3", "ingredient_2"), "Ingredient_Crystal_Blue", 625, 21600000L),
        new ResearchNode("fish_1", "fish", 1, 2, 0, List.of(), "Fish_Catfish_Item", 50, 1200000L),
        new ResearchNode("fish_2", "fish", 2, 2, 4, List.of("fish_1", "food_1"), "Fish_Catfish_Item", 125, 7200000L),
        new ResearchNode("fish_3", "fish", 3, 2, 8, List.of("fish_2", "food_2"), "Fish_Catfish_Item", 312, 21600000L),
        new ResearchNode("food_1", "food", 1, 2, 3, List.of("plant_1", "ingredient_1", "furniture_1"), "Food_Bread", 15, 1200000L),
        new ResearchNode("food_2", "food", 2, 2, 7, List.of("plant_2", "ingredient_2", "furniture_2", "food_1"), "Food_Bread", 150, 7200000L),
        new ResearchNode("food_3", "food", 3, 2, 11, List.of("plant_3", "ingredient_3", "furniture_3", "food_2"), "Food_Bread", 1200, 21600000L),
        new ResearchNode("potion_1", "potion", 1, 3, 3, List.of("furniture_1"), "Potion_Health", 15, 1200000L),
        new ResearchNode("potion_2", "potion", 2, 3, 7, List.of("furniture_2", "potion_1"), "Potion_Health", 150, 7200000L),
        new ResearchNode("potion_3", "potion", 3, 3, 11, List.of("furniture_3", "potion_2"), "Potion_Health", 1200, 21600000L),
        new ResearchNode("metal_1", "metal", 1, 0, 3, List.of("soil_1", "ingredient_1"), "Ingredient_Bar_Iron", 100, 1800000L),
        new ResearchNode("metal_2", "metal", 2, 0, 7, List.of("soil_2", "ingredient_2", "metal_1"), "Ingredient_Bar_Iron", 250, 9000000L),
        new ResearchNode("metal_3", "metal", 3, 0, 11, List.of("soil_3", "ingredient_3", "metal_2"), "Ingredient_Bar_Iron", 625, 25200000L),
        new ResearchNode("furniture_1", "furniture", 1, 3, 2, List.of("cloth_1", "plant_1"), "Furniture_Ancient_Chest_Small", 20, 1800000L),
        new ResearchNode("furniture_2", "furniture", 2, 3, 6, List.of("cloth_2", "plant_2", "furniture_1"), "Furniture_Ancient_Chest_Small", 200, 9000000L),
        new ResearchNode("furniture_3", "furniture", 3, 3, 10, List.of("cloth_3", "plant_3", "furniture_2"), "Furniture_Ancient_Chest_Small", 1600, 25200000L)
    );

    private static final Map<String, ResearchNode> BY_ID =
            NODES.stream().collect(Collectors.toMap(ResearchNode::id, n -> n));

    private static final Map<String, ResearchNode> BY_CATEGORY_TIER =
            NODES.stream().collect(Collectors.toMap(n -> n.category() + "|" + n.tier(), n -> n));

    static {
        if (NODES.size() != 33) {
            throw new IllegalStateException("Expected 33 research nodes, found " + NODES.size());
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
     * Un nœud est déblocable si :
     *  - le tier précédent de SA PROPRE catégorie est complété, quand il existe (condition
     *    obligatoire — impossible de sauter un palier au sein d'une catégorie) ;
     *  - ET au moins UN de ses prérequis DIRECTS (hors chaîne de tier) est complété — pas de
     *    transitivité : un ancêtre complété plus haut dans l'arbre ne débloque QUE le nœud
     *    immédiatement en dessous de lui, pas toute la chaîne descendante d'un coup.
     * Les nœuds racines (aucun prérequis) sont ouverts d'emblée.
     */
    public static boolean isUnlockable(ResearchNode node, java.util.Set<String> completedNodeIds) {
        String sameCategoryPrevTier = node.tier() > 1 ? node.category() + "_" + (node.tier() - 1) : null;
        if (sameCategoryPrevTier != null && !completedNodeIds.contains(sameCategoryPrevTier)) {
            return false;
        }
        List<String> otherPrereqs = otherPrereqs(node, sameCategoryPrevTier);
        if (otherPrereqs.isEmpty()) {
            return true;
        }
        for (String prereq : otherPrereqs) {
            if (completedNodeIds.contains(prereq)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> otherPrereqs(ResearchNode node, String sameCategoryPrevTier) {
        List<String> result = new java.util.ArrayList<>();
        for (String prereq : node.prerequisiteIds()) {
            if (!prereq.equals(sameCategoryPrevTier)) {
                result.add(prereq);
            }
        }
        return result;
    }

    /**
     * Prérequis d'un nœud qui ne sont pas encore débloqués — utilisé pour le message d'erreur
     * "il vous manque X". Sous la logique OU décrite dans isUnlockable (prérequis directs
     * uniquement, pas de transitivité), ce n'est pas "tout ce qui manque" mais le tier précédent
     * (s'il manque) plus, si aucun prérequis direct n'est déjà complété, la liste des options
     * directes pour que le joueur sache quoi viser.
     */
    public static List<String> missingPrerequisites(ResearchNode node, java.util.Set<String> completedNodeIds) {
        String sameCategoryPrevTier = node.tier() > 1 ? node.category() + "_" + (node.tier() - 1) : null;
        List<String> missing = new java.util.ArrayList<>();
        List<String> otherOptions = new java.util.ArrayList<>();
        boolean anyOtherCompleted = false;
        for (String prereq : node.prerequisiteIds()) {
            if (prereq.equals(sameCategoryPrevTier)) {
                if (!completedNodeIds.contains(prereq)) {
                    missing.add(prereq);
                }
                continue;
            }
            otherOptions.add(prereq);
            if (completedNodeIds.contains(prereq)) {
                anyOtherCompleted = true;
            }
        }
        if (!anyOtherCompleted) {
            missing.addAll(otherOptions);
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
