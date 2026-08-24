package fr.varyon.stacktiers.research;

import java.util.Map;

/**
 * Libellés lisibles des items exigés par les recherches, pour l'affichage (carte + chat).
 * Les identifiants sont ceux des assets du jeu ({@code Server/Item/Items/**.json}). Noms en
 * anglais, repris tels quels de Server/Languages/en-US/server.lang (le jeu n'a pas de
 * traduction française officielle pour ces items).
 */
public final class ResearchItemNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("Wood_Ash_Trunk", "Ash Log"),
            Map.entry("Rock_Stone", "Stone"),
            Map.entry("Soil_Dirt", "Dirt"),
            Map.entry("Plant_Crop_Carrot_Item", "Carrot"),
            Map.entry("Ingredient_Fabric_Scrap_Linen", "Linen Scraps"),
            Map.entry("Ingredient_Fabric_Scrap_Shadoweave", "Shadoweave Scraps"),
            Map.entry("Ingredient_Fabric_Scrap_Cindercloth", "Cindercloth Scraps"),
            Map.entry("Ingredient_Bar_Iron", "Iron Ingot"),
            Map.entry("Fish_Catfish_Item", "Catfish"),
            Map.entry("Food_Bread", "Bread"),
            Map.entry("Potion_Health", "Health Potion")
    );

    private static final Map<String, String> EXTRA = Map.of(
            "Furniture_Ancient_Chest_Small", "Small Ancient Chest",
            "Ingredient_Crystal_Blue", "Blue Crystal Shards"
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
