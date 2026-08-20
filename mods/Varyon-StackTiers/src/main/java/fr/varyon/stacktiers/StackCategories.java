package fr.varyon.stacktiers;

import java.util.Set;

/**
 * Catégories connues par le système de stack. Doit rester synchronisée manuellement
 * avec Mixin-Varyon-StackSize/src/main/data/stackable_items.json (deux modules Gradle
 * séparés, pas de partage automatisé — la taxonomie change rarement). "all" est
 * volontairement exclu : c'est un scope réservé à LuckPerms (varyon.stack.all.tierN),
 * pas un override individuel assignable via /varyonstack.
 *
 * "ore" a été fusionné dans "metal" (minerais bruts + blocs métalliques transformés,
 * débloqués ensemble en jeu via des lingots de fer) et "resources" a été supprimé —
 * c'était un doublon systématique de chaque autre catégorie (tout item "resources"
 * avait aussi rock/plant/wood/etc.), pas une vraie catégorie distincte ; le seul boost
 * réellement global reste le scope "all" de LuckPerms, volontairement hors de ce système.
 */
public final class StackCategories {
    public static final Set<String> KNOWN = Set.of(
            "wood", "rock", "metal", "soil", "cloth", "plant", "ingredient", "fish",
            "furniture", "food", "potion"
    );

    public static boolean isValid(String category) {
        return KNOWN.contains(category);
    }

    private StackCategories() {
    }
}
