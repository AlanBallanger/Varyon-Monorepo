package fr.varyon.death.config;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Facade statique vers {@link GestionnairePreferences}, accessible depuis les systemes ECS
 * qui n'ont pas de reference vers l'instance du plugin.
 *
 * <p>Tant qu'aucun gestionnaire n'est lie, le recapitulatif est considere actif : c'est le
 * comportement par defaut attendu pour un nouveau joueur.
 */
public final class PreferencesRecap {

    private static volatile GestionnairePreferences manager;

    private PreferencesRecap() {}

    public static void bind(@Nonnull GestionnairePreferences instance) {
        manager = instance;
    }

    @Nullable
    public static GestionnairePreferences manager() {
        return manager;
    }

    public static boolean estActif(@Nullable UUID uuid) {
        if (uuid == null) {
            return false;
        }
        GestionnairePreferences instance = manager;
        return instance == null || instance.estActif(uuid);
    }

    public static void ensureLoaded(@Nullable UUID uuid) {
        GestionnairePreferences instance = manager;
        if (instance != null && uuid != null) {
            instance.ensureLoaded(uuid);
        }
    }

    public static void setEnabled(@Nullable UUID uuid, boolean enabled) {
        GestionnairePreferences instance = manager;
        if (instance != null && uuid != null) {
            instance.setEnabled(uuid, enabled);
        }
    }

    public static boolean toggle(@Nullable UUID uuid) {
        GestionnairePreferences instance = manager;
        if (instance == null || uuid == null) {
            return true;
        }
        return instance.toggle(uuid);
    }
}
