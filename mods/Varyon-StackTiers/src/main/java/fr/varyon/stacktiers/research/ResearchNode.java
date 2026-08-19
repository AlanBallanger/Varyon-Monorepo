package fr.varyon.stacktiers.research;

import java.util.List;

/**
 * Un nœud de l'arbre de recherche : une paire (catégorie, tier) avec sa position visuelle
 * (colonne/ligne, purement structurelle — indépendante du tier), son prérequis, et son coût.
 */
public record ResearchNode(
        String id,
        String category,
        int tier,
        int column,
        int row,
        List<String> prerequisiteIds,
        String resourceItemId,
        int resourceQuantity,
        long durationMillis
) {
    public ResearchNode {
        prerequisiteIds = List.copyOf(prerequisiteIds);
    }
}
