package fr.varyon.stacktiers.research;

import java.util.HashSet;
import java.util.Set;

/** État de progression d'un joueur dans l'arbre de recherche. */
public final class PlayerResearchState {
    public final Set<String> completedNodeIds = new HashSet<>();
    public String inProgressNodeId;
    public long startEpochMs;
    public long completionEpochMs;
}
