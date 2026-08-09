package fr.varyon.death.config;

/**
 * Etat du reglage {@code logs_diagnostic} du fichier de configuration.
 *
 * <p>Accessible statiquement pour les systemes et les pages d'interface, qui n'ont pas de
 * reference vers la configuration. La valeur est fixee une fois au demarrage du plugin.
 */
public final class Diagnostic {

    private static volatile boolean actif;

    private Diagnostic() {}

    public static void definir(boolean valeur) {
        actif = valeur;
    }

    public static boolean estActif() {
        return actif;
    }
}
