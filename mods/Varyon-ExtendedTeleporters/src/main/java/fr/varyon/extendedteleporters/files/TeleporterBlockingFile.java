package fr.varyon.extendedteleporters.files;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;
import fr.varyon.extendedteleporters.data.SubownerPermissions;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import fr.varyon.extendedteleporters.util.FileUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleporterBlockingFile extends BlockingDiskFile {
   private ConcurrentHashMap<String, TeleporterInfo> teleporters = new ConcurrentHashMap<>();

   public TeleporterBlockingFile() {
      super(Path.of(FileUtils.TELEPORTERS_PATH));
   }

   protected void read(BufferedReader reader) throws IOException {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      if (root != null) {
         this.teleporters = new ConcurrentHashMap<>();
         JsonArray teleportersArray = root.getAsJsonArray("Teleporters");
         if (teleportersArray != null) {
            teleportersArray.forEach(
               element -> {
                  JsonObject obj = element.getAsJsonObject();
                  UUID ownerUuid = null;
                  if (obj.has("OwnerUuid") && !obj.get("OwnerUuid").isJsonNull()) {
                     String ownerStr = obj.get("OwnerUuid").getAsString();
                     if (ownerStr != null && !ownerStr.isEmpty() && !"null".equals(ownerStr)) {
                        ownerUuid = UUID.fromString(ownerStr);
                     }
                  }

                  String dimension = obj.get("Dimension").getAsString();
                  int blockX = obj.get("BlockX").getAsInt();
                  int blockY = obj.get("BlockY").getAsInt();
                  int blockZ = obj.get("BlockZ").getAsInt();
                  boolean isPrivate = obj.has("IsPrivate") && obj.get("IsPrivate").getAsBoolean();
                  boolean isPartyOnly = obj.has("IsRestricted") && obj.get("IsRestricted").getAsBoolean();
                  boolean isRestricted = obj.has("IsTrueRestricted") && obj.get("IsTrueRestricted").getAsBoolean();
                  boolean isInteractionLocked = obj.has("IsInteractionLocked") && obj.get("IsInteractionLocked").getAsBoolean();
                  boolean isBreakLocked = obj.has("IsBreakLocked") && obj.get("IsBreakLocked").getAsBoolean();
                  boolean displayWorld = !obj.has("DisplayWorld") || obj.get("DisplayWorld").getAsBoolean();
                  boolean displayCoordinates = !obj.has("DisplayCoordinates") || obj.get("DisplayCoordinates").getAsBoolean();
                  String warpName = obj.has("WarpName") && !obj.get("WarpName").isJsonNull() ? obj.get("WarpName").getAsString() : null;
                  String warpDestination = obj.has("WarpDestination") && !obj.get("WarpDestination").isJsonNull()
                     ? obj.get("WarpDestination").getAsString()
                     : null;
                  long placedTimestamp = obj.has("PlacedTimestamp") ? obj.get("PlacedTimestamp").getAsLong() : System.currentTimeMillis();
                  boolean isSelfDestruct = obj.has("IsSelfDestruct") && obj.get("IsSelfDestruct").getAsBoolean();
                  Long selfDestructActivatedAt = obj.has("SelfDestructActivatedAt") && !obj.get("SelfDestructActivatedAt").isJsonNull()
                     ? obj.get("SelfDestructActivatedAt").getAsLong()
                     : null;
                  boolean isServerTeleporter = obj.has("IsServerTeleporter") && obj.get("IsServerTeleporter").getAsBoolean();
                  boolean isSingleUse = obj.has("IsSingleUse") && obj.get("IsSingleUse").getAsBoolean();
                  boolean hideMapWaypoint = obj.has("HideMapWaypoint") && obj.get("HideMapWaypoint").getAsBoolean();
                  boolean allowPublicDestinationChange = !obj.has("AllowPublicDestinationChange") || obj.get("AllowPublicDestinationChange").getAsBoolean();
                  Map<UUID, SubownerPermissions> subowners = new ConcurrentHashMap<>();
                  if (obj.has("Subowners") && obj.get("Subowners").isJsonArray()) {
                     for (JsonElement subownerElement : obj.getAsJsonArray("Subowners")) {
                        try {
                           JsonObject subownerObj = subownerElement.getAsJsonObject();
                           UUID subownerUuid = UUID.fromString(subownerObj.get("Uuid").getAsString());
                           boolean canRename = subownerObj.has("CanRename") && subownerObj.get("CanRename").getAsBoolean();
                           boolean canSetDestination = subownerObj.has("CanSetDestination") && subownerObj.get("CanSetDestination").getAsBoolean();
                           boolean canModifySettings = subownerObj.has("CanModifySettings") && subownerObj.get("CanModifySettings").getAsBoolean();
                           boolean canBreak = subownerObj.has("CanBreak") && subownerObj.get("CanBreak").getAsBoolean();
                           subowners.put(subownerUuid, new SubownerPermissions(subownerUuid, canRename, canSetDestination, canModifySettings, canBreak));
                        } catch (IllegalArgumentException var36) {
                        }
                     }
                  } else if (obj.has("TrustedPlayers") && obj.get("TrustedPlayers").isJsonArray()) {
                     for (JsonElement trustedElement : obj.getAsJsonArray("TrustedPlayers")) {
                        try {
                           UUID trustedUuid = UUID.fromString(trustedElement.getAsString());
                           subowners.put(trustedUuid, new SubownerPermissions(trustedUuid));
                        } catch (IllegalArgumentException var35) {
                        }
                     }
                  }

                  TeleporterInfo info = new TeleporterInfo(
                     ownerUuid,
                     dimension,
                     blockX,
                     blockY,
                     blockZ,
                     isPrivate,
                     isPartyOnly,
                     isRestricted,
                     isInteractionLocked,
                     isBreakLocked,
                     displayWorld,
                     displayCoordinates,
                     warpName,
                     warpDestination,
                     placedTimestamp,
                     isSelfDestruct,
                     selfDestructActivatedAt,
                     isServerTeleporter,
                     isSingleUse,
                     hideMapWaypoint,
                     allowPublicDestinationChange,
                     subowners
                  );
                  this.teleporters.put(info.locationKey(), info);
               }
            );
         }
      }
   }

   protected void write(BufferedWriter writer) throws IOException {
      JsonObject root = new JsonObject();
      JsonArray teleportersArray = new JsonArray();
      this.teleporters.values().forEach(info -> {
         JsonObject obj = new JsonObject();
         if (info.ownerUuid() != null) {
            obj.addProperty("OwnerUuid", info.ownerUuid().toString());
         } else {
            obj.addProperty("OwnerUuid", (String)null);
         }

         obj.addProperty("Dimension", info.dimension());
         obj.addProperty("BlockX", info.blockX());
         obj.addProperty("BlockY", info.blockY());
         obj.addProperty("BlockZ", info.blockZ());
         obj.addProperty("IsPrivate", info.isPrivate());
         obj.addProperty("IsRestricted", info.isPartyOnly());
         obj.addProperty("IsTrueRestricted", info.isRestricted());
         obj.addProperty("IsInteractionLocked", info.isInteractionLocked());
         obj.addProperty("IsBreakLocked", info.isBreakLocked());
         obj.addProperty("DisplayWorld", info.displayWorld());
         obj.addProperty("DisplayCoordinates", info.displayCoordinates());
         if (info.warpName() != null) {
            obj.addProperty("WarpName", info.warpName());
         }

         if (info.warpDestination() != null) {
            obj.addProperty("WarpDestination", info.warpDestination());
         }

         obj.addProperty("PlacedTimestamp", info.placedTimestamp());
         obj.addProperty("IsSelfDestruct", info.isSelfDestruct());
         if (info.selfDestructActivatedAt() != null) {
            obj.addProperty("SelfDestructActivatedAt", info.selfDestructActivatedAt());
         }

         obj.addProperty("IsServerTeleporter", info.isServerTeleporter());
         obj.addProperty("IsSingleUse", info.isSingleUse());
         obj.addProperty("HideMapWaypoint", info.hideMapWaypoint());
         obj.addProperty("AllowPublicDestinationChange", info.allowPublicDestinationChange());
         Map<UUID, SubownerPermissions> subowners = info.getSubownersMap();
         if (!subowners.isEmpty()) {
            JsonArray subownersArray = new JsonArray();

            for (SubownerPermissions perms : subowners.values()) {
               JsonObject subownerObj = new JsonObject();
               subownerObj.addProperty("Uuid", perms.playerUuid().toString());
               subownerObj.addProperty("CanRename", perms.canRename());
               subownerObj.addProperty("CanSetDestination", perms.canSetDestination());
               subownerObj.addProperty("CanModifySettings", perms.canModifySettings());
               subownerObj.addProperty("CanBreak", perms.canBreak());
               subownersArray.add(subownerObj);
            }

            obj.add("Subowners", subownersArray);
         }

         teleportersArray.add(obj);
      });
      root.add("Teleporters", teleportersArray);
      writer.write(root.toString());
   }

   protected void create(BufferedWriter writer) throws IOException {
      JsonObject root = new JsonObject();
      root.add("Teleporters", new JsonArray());
      writer.write(root.toString());
   }

   public ConcurrentHashMap<String, TeleporterInfo> getTeleporters() {
      return this.teleporters;
   }
}
