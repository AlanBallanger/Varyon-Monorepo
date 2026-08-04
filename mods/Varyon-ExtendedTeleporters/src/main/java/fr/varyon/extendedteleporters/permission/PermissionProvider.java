package fr.varyon.extendedteleporters.permission;

import java.util.UUID;

public interface PermissionProvider {
   String PERM_FEATURE_PRIVATE = "extendedteleporters.feature.private";
   String PERM_FEATURE_RESTRICTED = "extendedteleporters.feature.restricted";
   String PERM_FEATURE_TRUE_RESTRICTED = "extendedteleporters.feature.truerestricted";
   String PERM_FEATURE_LOCK_INTERACTION = "extendedteleporters.feature.lock_interaction";
   String PERM_FEATURE_LOCK_BREAK = "extendedteleporters.feature.lock_break";
   String PERM_FEATURE_HIDE_WORLD = "extendedteleporters.feature.hide_world";
   String PERM_FEATURE_HIDE_COORDS = "extendedteleporters.feature.hide_coords";
   String PERM_FEATURE_SELF_DESTRUCT = "extendedteleporters.feature.self_destruct";
   String PERM_FEATURE_SINGLE_USE = "extendedteleporters.feature.single_use";
   String PERM_FEATURE_HIDE_MAP_WAYPOINT = "extendedteleporters.feature.hide_map_waypoint";
   String PERM_BYPASS_LIMITS = "extendedteleporters.bypass.limits";
   String META_LIMIT = "extendedteleporters.limit";
   String META_LIMIT_PRIVATE = "extendedteleporters.limit.private";
   String META_LIMIT_RESTRICTED = "extendedteleporters.limit.restricted";
   String META_LIMIT_PUBLIC = "extendedteleporters.limit.public";

   boolean hasPermission(UUID var1, String var2);

   int getMetaInteger(UUID var1, String var2, int var3);

   String getName();
}
