package fr.varyon.vrpg.rpg;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import fr.varyon.vrpg.classes.PlayerClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * Attribue/retire des permissions natives (PermissionsModule, pas LuckPerms) reflétant
 * le métier et la classe actifs d'un joueur, pour qu'un autre mod puisse les lire via
 * PlayerRef/PermissionsModule.hasPermission sans dépendre du code de Varyon-RPG.
 *
 * Format : vrpg.job.<id> (un par slot actif, jusqu'à 2 simultanés) et vrpg.class.<id> (un seul actif).
 */
public final class JobPermissionSync {

    private static final String JOB_PREFIX = "vrpg.job.";
    private static final String CLASS_PREFIX = "vrpg.class.";

    private JobPermissionSync() {}

    public static void syncJob(@Nonnull UUID uuid, @Nullable Profession previous, @Nullable Profession next) {
        if (previous == next) return;
        PermissionsModule module = PermissionsModule.get();
        if (previous != null) module.removeUserPermission(uuid, Set.of(JOB_PREFIX + previous.getId()));
        if (next != null) module.addUserPermission(uuid, Set.of(JOB_PREFIX + next.getId()));
    }

    public static void syncClass(@Nonnull UUID uuid, @Nullable PlayerClass previous, @Nullable PlayerClass next) {
        if (previous == next) return;
        PermissionsModule module = PermissionsModule.get();
        if (previous != null) module.removeUserPermission(uuid, Set.of(CLASS_PREFIX + previous.getId()));
        if (next != null) module.addUserPermission(uuid, Set.of(CLASS_PREFIX + next.getId()));
    }

    /** Resynchronise les permissions de métiers (slot0 + slot1) avec l'état actuel du compte, à la connexion. */
    public static void resyncJobs(@Nonnull UUID uuid, @Nullable Profession slot0, @Nullable Profession slot1) {
        PermissionsModule module = PermissionsModule.get();
        for (Profession p : Profession.values()) {
            String node = JOB_PREFIX + p.getId();
            boolean shouldHave = p == slot0 || p == slot1;
            boolean has = module.hasPermission(uuid, node, false);
            if (shouldHave && !has) module.addUserPermission(uuid, Set.of(node));
            else if (!shouldHave && has) module.removeUserPermission(uuid, Set.of(node));
        }
    }

    /** Resynchronise la permission de classe active avec l'état actuel du compte, à la connexion. */
    public static void resyncClass(@Nonnull UUID uuid, @Nullable PlayerClass active) {
        PermissionsModule module = PermissionsModule.get();
        for (PlayerClass c : PlayerClass.values()) {
            String node = CLASS_PREFIX + c.getId();
            boolean shouldHave = c == active;
            boolean has = module.hasPermission(uuid, node, false);
            if (shouldHave && !has) module.addUserPermission(uuid, Set.of(node));
            else if (!shouldHave && has) module.removeUserPermission(uuid, Set.of(node));
        }
    }
}
