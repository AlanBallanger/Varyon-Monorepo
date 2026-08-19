package fr.varyon.stacktiers;

import java.util.Set;

/**
 * Catégories connues par le système de stack. Doit rester synchronisée manuellement
 * avec Mixin-Varyon-StackSize/src/main/data/stackable_items.json (deux modules Gradle
 * séparés, pas de partage automatisé — la taxonomie change rarement). "all" est
 * volontairement exclu : c'est un scope réservé à LuckPerms (varyon.stack.all.tierN),
 * pas un override individuel assignable via /varyonstack.
 */
public final class StackCategories {
    public static final Set<String> KNOWN = Set.of(
            "wood", "rock", "metal", "ore", "soil", "cloth", "plant", "ingredient", "fish",
            "resources", "furniture", "food", "potion"
    );

    public static boolean isValid(String category) {
        return KNOWN.contains(category);
    }

    private StackCategories() {
    }
}
