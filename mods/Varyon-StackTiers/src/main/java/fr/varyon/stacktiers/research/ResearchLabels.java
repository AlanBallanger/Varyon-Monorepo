package fr.varyon.stacktiers.research;

import java.util.Map;

/** Libellés affichés (cartes de l'arbre et messages de chat). */
public final class ResearchLabels {

    private static final Map<String, String> CATEGORIES = Map.ofEntries(
            Map.entry("wood", "Bois"),
            Map.entry("rock", "Pierre"),
            Map.entry("soil", "Terre"),
            Map.entry("plant", "Plante"),
            Map.entry("cloth", "Tissu"),
            Map.entry("ingredient", "Ingrédient"),
            Map.entry("fish", "Poisson"),
            Map.entry("food", "Nourriture"),
            Map.entry("potion", "Potion"),
            Map.entry("metal", "Métal"),
            Map.entry("furniture", "Mobilier")
    );

    /** Nom français de la catégorie, ou le slug brut si non traduit. */
    public static String category(String slug) {
        return CATEGORIES.getOrDefault(slug, slug);
    }

    /** Bonus de taille de stack accordé par un tier : +25%, +50% ou +100%. */
    public static String tierBonus(int tier) {
        return switch (tier) {
            case 1 -> "+25%";
            case 2 -> "+50%";
            case 3 -> "+100%";
            default -> "";
        };
    }

    /** Libellé complet d'un nœud, ex. "Bois — Tier 2 (+50% taille de stack)". */
    public static String nodeTitle(ResearchNode node) {
        return category(node.category()) + " — Tier " + node.tier()
                + " (" + tierBonus(node.tier()) + " taille de stack)";
    }

    private ResearchLabels() {
    }
}
