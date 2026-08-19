package fr.varyon.stacktiers.research;

import java.util.Map;

/**
 * Libellés lisibles des items exigés par les recherches, pour l'affichage (carte + chat).
 * Les identifiants sont ceux des assets du jeu ({@code Server/Item/Items/**.json}).
 */
public final class ResearchItemNames {

    private static final Map<String, String> NAMES = Map.of(
            "Wood_Ash_Trunk", "Tronc",
            "Rock_Stone", "Pierre",
            "Soil_Dirt", "Terre",
            "Plant_Crop_Carrot_Item", "Carotte",
            "Cloth_Block_Wool_Black", "Laine",
            "Metal_Iron", "Fer",
            "Fish_Catfish_Item", "Poisson-chat",
            "Food_Bread", "Pain",
            "Potion_Health", "Potion de vie",
            "Ore_Iron", "Minerai de fer"
    );

    private static final Map<String, String> EXTRA = Map.of(
            "Furniture_Ancient_Chest_Small", "Coffre ancien",
            "Ingredient_Crystal_Blue", "Cristal bleu",
            "Rock_Gem_Diamond", "Diamant"
    );

    /** Nom lisible de l'item, ou l'identifiant brut si aucun libellé n'est défini. */
    public static String of(String itemId) {
        String name = NAMES.get(itemId);
        if (name != null) {
            return name;
        }
        name = EXTRA.get(itemId);
        return name != null ? name : itemId;
    }

    private ResearchItemNames() {
    }
}
