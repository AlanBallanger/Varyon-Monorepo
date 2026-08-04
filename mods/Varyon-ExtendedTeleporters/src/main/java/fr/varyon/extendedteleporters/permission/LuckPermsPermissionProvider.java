package fr.varyon.extendedteleporters.permission;

import com.hypixel.hytale.logger.HytaleLogger;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;

public final class LuckPermsPermissionProvider implements PermissionProvider {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-LuckPerms");
   private volatile LuckPerms luckPerms;

   public LuckPermsPermissionProvider() {
      logger.at(Level.INFO).log("LuckPerms permission provider created (will lazy-load API)");
   }

   private LuckPerms getLuckPerms() {
      if (this.luckPerms == null) {
         synchronized (this) {
            if (this.luckPerms == null) {
               try {
                  this.luckPerms = LuckPermsProvider.get();
                  logger.at(Level.INFO).log("LuckPerms API loaded successfully");
               } catch (Exception e) {
                  logger.at(Level.WARNING).log("Failed to get LuckPerms API: " + e.getMessage());
                  return null;
               }
            }
         }
      }

      return this.luckPerms;
   }

   @Override
   public String getName() {
      return "LuckPerms";
   }

   @Override
   public boolean hasPermission(UUID playerUuid, String node) {
      if (playerUuid != null && node != null) {
         LuckPerms api = this.getLuckPerms();
         if (api == null) {
            logger.at(Level.WARNING).log("LuckPerms API not available for permission check");
            return false;
         }

         try {
            User user = (User)api.getUserManager().loadUser(playerUuid).join();
            if (user == null) {
               return false;
            }

            CachedPermissionData permissionData = user.getCachedData().getPermissionData();
            Tristate result = permissionData.checkPermission(node);
            return result.asBoolean();
         } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to check permission for %s: %s".formatted(playerUuid, e.getMessage()));
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int getMetaInteger(UUID playerUuid, String key, int fallback) {
      if (playerUuid != null && key != null) {
         LuckPerms api = this.getLuckPerms();
         if (api == null) {
            logger.at(Level.WARNING).log("LuckPerms API not available for meta lookup");
            return fallback;
         }

         try {
            User user = (User)api.getUserManager().loadUser(playerUuid).join();
            if (user == null) {
               logger.at(Level.WARNING).log("LuckPerms loadUser returned null for %s".formatted(playerUuid));
               return fallback;
            } else {
               user.getCachedData().invalidate();
               CachedMetaData metaData = user.getCachedData().getMetaData();
               Optional<Integer> value = metaData.getMetaValue(key, Integer::parseInt);
               return value.orElse(fallback);
            }
         } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to get meta integer for %s key '%s': %s".formatted(playerUuid, key, e.getMessage()));
            return fallback;
         }
      } else {
         logger.at(Level.INFO).log("getMetaInteger called with null playerUuid or key");
         return fallback;
      }
   }

   public static boolean isAvailable() {
      try {
         Class.forName("net.luckperms.api.LuckPermsProvider");
         LuckPermsProvider.get();
         return true;
      } catch (Throwable t) {
         return false;
      }
   }
}
