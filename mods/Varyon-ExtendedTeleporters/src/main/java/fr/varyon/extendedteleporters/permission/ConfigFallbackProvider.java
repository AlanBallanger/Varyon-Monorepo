package fr.varyon.extendedteleporters.permission;

import com.hypixel.hytale.logger.HytaleLogger;
import fr.varyon.extendedteleporters.VaryonExtendedTeleportersPlugin;
import fr.varyon.extendedteleporters.config.ExtendedTeleportConfig;
import java.util.UUID;
import java.util.logging.Level;

public final class ConfigFallbackProvider implements PermissionProvider {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-ConfigFallback");

   public ConfigFallbackProvider() {
      logger.at(Level.INFO).log("Config fallback permission provider initialized (LuckPerms not available)");
   }

   @Override
   public String getName() {
      return "ConfigFallback";
   }

   private ExtendedTeleportConfig getConfig() {
      return VaryonExtendedTeleportersPlugin.CONFIG != null ? (ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get() : new ExtendedTeleportConfig();
   }

   @Override
   public boolean hasPermission(UUID playerUuid, String node) {
      if (playerUuid != null && node != null) {
         ExtendedTeleportConfig config = this.getConfig();

         return switch (node) {
            case "extendedteleporters.feature.private" -> config.isAllowPrivateTeleporters();
            case "extendedteleporters.feature.restricted" -> config.isAllowRestrictedTeleporters();
            case "extendedteleporters.feature.truerestricted" -> config.isAllowRestrictedTeleporters();
            case "extendedteleporters.feature.lock_interaction" -> config.isAllowInteractionLockedTeleporters();
            case "extendedteleporters.feature.lock_break" -> config.isAllowBreakLockedTeleporters();
            case "extendedteleporters.feature.hide_world" -> true;
            case "extendedteleporters.feature.hide_coords" -> true;
            case "extendedteleporters.feature.self_destruct" -> config.isAllowSelfDestructTeleporters();
            case "extendedteleporters.feature.hide_map_waypoint" -> config.isAllowHideMapWaypoint();
            default -> true;
         };
      } else {
         return false;
      }
   }

   @Override
   public int getMetaInteger(UUID playerUuid, String key, int fallback) {
      if (playerUuid != null && key != null) {
         ExtendedTeleportConfig config = this.getConfig();

         return switch (key) {
            case "extendedteleporters.limit" -> config.getMaxTeleportersPerPlayer();
            case "extendedteleporters.limit.private" -> config.getMaxPrivateTeleporters();
            case "extendedteleporters.limit.restricted" -> config.getMaxRestrictedTeleporters();
            case "extendedteleporters.limit.public" -> config.getMaxPublicTeleporters();
            default -> fallback;
         };
      } else {
         return fallback;
      }
   }
}
