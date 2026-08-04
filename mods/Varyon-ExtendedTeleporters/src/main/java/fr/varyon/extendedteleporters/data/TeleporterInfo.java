package fr.varyon.extendedteleporters.data;

import fr.varyon.extendedteleporters.TeleporterManager;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public final class TeleporterInfo {
   public static final long SELF_DESTRUCT_DURATION_MS = 300000L;
   @Nullable
   private final UUID ownerUuid;
   private final String dimension;
   private final int blockX;
   private final int blockY;
   private final int blockZ;
   private final long placedTimestamp;
   private final String locationKey;
   private volatile boolean isPrivate;
   private volatile boolean isPartyOnly;
   private volatile boolean isRestricted;
   private volatile boolean isInteractionLocked;
   private volatile boolean isBreakLocked;
   private volatile boolean displayWorld = true;
   private volatile boolean displayCoordinates = true;
   @Nullable
   private volatile String warpName;
   @Nullable
   private volatile String warpDestination;
   private volatile boolean isSelfDestruct;
   @Nullable
   private volatile Long selfDestructActivatedAt;
   private volatile boolean isServerTeleporter;
   private volatile boolean isSingleUse;
   private volatile boolean hideMapWaypoint;
   private volatile boolean allowPublicDestinationChange = true;
   private final Map<UUID, SubownerPermissions> subowners = new ConcurrentHashMap<>();

   public TeleporterInfo(@Nullable UUID ownerUuid, String dimension, int blockX, int blockY, int blockZ) {
      this(
         ownerUuid,
         dimension,
         blockX,
         blockY,
         blockZ,
         false,
         false,
         false,
         false,
         false,
         true,
         true,
         null,
         null,
         System.currentTimeMillis(),
         false,
         null,
         false,
         false,
         false,
         true,
         (Map<UUID, SubownerPermissions>)null
      );
   }

   public TeleporterInfo(
      @Nullable UUID ownerUuid,
      String dimension,
      int blockX,
      int blockY,
      int blockZ,
      boolean isPrivate,
      boolean isPartyOnly,
      boolean isRestricted,
      boolean isInteractionLocked,
      boolean isBreakLocked,
      boolean displayWorld,
      boolean displayCoordinates,
      @Nullable String warpName,
      @Nullable String warpDestination,
      long placedTimestamp
   ) {
      this(
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
         false,
         null,
         false,
         false,
         false,
         true,
         (Map<UUID, SubownerPermissions>)null
      );
   }

   public TeleporterInfo(
      @Nullable UUID ownerUuid,
      String dimension,
      int blockX,
      int blockY,
      int blockZ,
      boolean isPrivate,
      boolean isPartyOnly,
      boolean isRestricted,
      boolean isInteractionLocked,
      boolean isBreakLocked,
      boolean displayWorld,
      boolean displayCoordinates,
      @Nullable String warpName,
      @Nullable String warpDestination,
      long placedTimestamp,
      boolean isSelfDestruct,
      @Nullable Long selfDestructActivatedAt,
      boolean isServerTeleporter,
      boolean isSingleUse,
      boolean hideMapWaypoint,
      @Nullable Set<UUID> trustedPlayers
   ) {
      this(
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
         true,
         convertLegacyTrustedPlayers(trustedPlayers)
      );
   }

   public TeleporterInfo(
      @Nullable UUID ownerUuid,
      String dimension,
      int blockX,
      int blockY,
      int blockZ,
      boolean isPrivate,
      boolean isPartyOnly,
      boolean isRestricted,
      boolean isInteractionLocked,
      boolean isBreakLocked,
      boolean displayWorld,
      boolean displayCoordinates,
      @Nullable String warpName,
      @Nullable String warpDestination,
      long placedTimestamp,
      boolean isSelfDestruct,
      @Nullable Long selfDestructActivatedAt,
      boolean isServerTeleporter,
      boolean isSingleUse,
      boolean hideMapWaypoint,
      boolean allowPublicDestinationChange,
      @Nullable Map<UUID, SubownerPermissions> subowners
   ) {
      this.ownerUuid = ownerUuid;
      this.dimension = Objects.requireNonNull(dimension, "dimension cannot be null");
      this.blockX = blockX;
      this.blockY = blockY;
      this.blockZ = blockZ;
      this.isPrivate = isPrivate;
      this.isPartyOnly = isPartyOnly;
      this.isRestricted = isRestricted;
      this.isInteractionLocked = isInteractionLocked;
      this.isBreakLocked = isBreakLocked;
      this.displayWorld = displayWorld;
      this.displayCoordinates = displayCoordinates;
      this.warpName = warpName;
      this.warpDestination = warpDestination;
      this.placedTimestamp = placedTimestamp;
      this.isSelfDestruct = isSelfDestruct;
      this.selfDestructActivatedAt = selfDestructActivatedAt;
      this.isServerTeleporter = isServerTeleporter;
      this.isSingleUse = isSingleUse;
      this.hideMapWaypoint = hideMapWaypoint;
      this.allowPublicDestinationChange = allowPublicDestinationChange;
      this.locationKey = formatLocationKey(dimension, blockX, blockY, blockZ);
      if (subowners != null) {
         this.subowners.putAll(subowners);
      }
   }

   @Nullable
   private static Map<UUID, SubownerPermissions> convertLegacyTrustedPlayers(@Nullable Set<UUID> trustedPlayers) {
      if (trustedPlayers != null && !trustedPlayers.isEmpty()) {
         Map<UUID, SubownerPermissions> result = new ConcurrentHashMap<>();

         for (UUID uuid : trustedPlayers) {
            result.put(uuid, new SubownerPermissions(uuid));
         }

         return result;
      } else {
         return null;
      }
   }

   @Nullable
   public UUID ownerUuid() {
      return this.ownerUuid;
   }

   public String dimension() {
      return this.dimension;
   }

   public int blockX() {
      return this.blockX;
   }

   public int blockY() {
      return this.blockY;
   }

   public int blockZ() {
      return this.blockZ;
   }

   public long placedTimestamp() {
      return this.placedTimestamp;
   }

   public String locationKey() {
      return this.locationKey;
   }

   public boolean hasOwner() {
      return this.ownerUuid != null;
   }

   public boolean isOwner(UUID playerUuid) {
      return this.ownerUuid != null && this.ownerUuid.equals(playerUuid);
   }

   public boolean isTrusted(UUID playerUuid) {
      return playerUuid != null && this.subowners.containsKey(playerUuid);
   }

   public boolean isOwnerOrTrusted(UUID playerUuid) {
      return this.isOwner(playerUuid) || this.isTrusted(playerUuid);
   }

   public boolean isPrivate() {
      return this.isPrivate;
   }

   public void setPrivate(boolean isPrivate) {
      this.isPrivate = isPrivate;
   }

   public boolean isPartyOnly() {
      return this.isPartyOnly;
   }

   public void setPartyOnly(boolean isPartyOnly) {
      this.isPartyOnly = isPartyOnly;
   }

   public boolean isRestricted() {
      return this.isRestricted;
   }

   public void setRestricted(boolean isRestricted) {
      this.isRestricted = isRestricted;
   }

   public boolean addTrustedPlayer(UUID playerUuid) {
      if (playerUuid == null || this.isOwner(playerUuid)) {
         return false;
      }

      if (this.subowners.containsKey(playerUuid)) {
         return false;
      }

      this.subowners.put(playerUuid, new SubownerPermissions(playerUuid));
      TeleporterManager.getInstance().onTrustAdded(this, playerUuid);
      return true;
   }

   public boolean addTrustedPlayer(UUID playerUuid, SubownerPermissions permissions) {
      if (playerUuid == null || this.isOwner(playerUuid)) {
         return false;
      }

      if (this.subowners.containsKey(playerUuid)) {
         return false;
      }

      this.subowners.put(playerUuid, permissions);
      TeleporterManager.getInstance().onTrustAdded(this, playerUuid);
      return true;
   }

   public boolean removeTrustedPlayer(UUID playerUuid) {
      if (playerUuid == null) {
         return false;
      }

      boolean removed = this.subowners.remove(playerUuid) != null;
      if (removed) {
         TeleporterManager.getInstance().onTrustRemoved(this, playerUuid);
      }

      return removed;
   }

   public Set<UUID> getTrustedPlayers() {
      return Collections.unmodifiableSet(new HashSet<>(this.subowners.keySet()));
   }

   @Nullable
   public SubownerPermissions getSubownerPermissions(UUID playerUuid) {
      return playerUuid == null ? null : this.subowners.get(playerUuid);
   }

   public Collection<SubownerPermissions> getAllSubownerPermissions() {
      return Collections.unmodifiableCollection(this.subowners.values());
   }

   public Map<UUID, SubownerPermissions> getSubownersMap() {
      return Collections.unmodifiableMap(this.subowners);
   }

   public void clearTrustedPlayers() {
      TeleporterManager manager = TeleporterManager.getInstance();

      for (UUID trustedUuid : this.subowners.keySet()) {
         manager.onTrustRemoved(this, trustedUuid);
      }

      this.subowners.clear();
   }

   public boolean canSubownerRename(UUID playerUuid) {
      if (this.isOwner(playerUuid)) {
         return true;
      }

      SubownerPermissions perms = this.subowners.get(playerUuid);
      return perms != null && perms.canRename();
   }

   public boolean canSubownerSetDestination(UUID playerUuid) {
      if (this.isOwner(playerUuid)) {
         return true;
      }

      SubownerPermissions perms = this.subowners.get(playerUuid);
      return perms != null && perms.canSetDestination();
   }

   public boolean canSubownerModifySettings(UUID playerUuid) {
      if (this.isOwner(playerUuid)) {
         return true;
      }

      SubownerPermissions perms = this.subowners.get(playerUuid);
      return perms != null && perms.canModifySettings();
   }

   public boolean canSubownerBreak(UUID playerUuid) {
      if (this.isOwner(playerUuid)) {
         return true;
      }

      SubownerPermissions perms = this.subowners.get(playerUuid);
      return perms != null && perms.canBreak();
   }

   public boolean isInteractionLocked() {
      return this.isInteractionLocked;
   }

   public void setInteractionLocked(boolean isInteractionLocked) {
      this.isInteractionLocked = isInteractionLocked;
   }

   public boolean isBreakLocked() {
      return this.isBreakLocked;
   }

   public void setBreakLocked(boolean isBreakLocked) {
      this.isBreakLocked = isBreakLocked;
   }

   public boolean displayWorld() {
      return this.displayWorld;
   }

   public void setDisplayWorld(boolean displayWorld) {
      this.displayWorld = displayWorld;
      if (!displayWorld) {
         this.displayCoordinates = false;
      }
   }

   public boolean displayCoordinates() {
      return this.displayCoordinates;
   }

   public void setDisplayCoordinates(boolean displayCoordinates) {
      this.displayCoordinates = displayCoordinates && this.displayWorld;
   }

   public boolean isSelfDestruct() {
      return this.isSelfDestruct;
   }

   @Nullable
   public Long selfDestructActivatedAt() {
      return this.selfDestructActivatedAt;
   }

   public void activateSelfDestruct() {
      this.isSelfDestruct = true;
      this.selfDestructActivatedAt = System.currentTimeMillis();
      this.isPrivate = false;
      this.isPartyOnly = false;
      this.isRestricted = false;
      this.isInteractionLocked = true;
      this.isBreakLocked = true;
   }

   public boolean isSelfDestructExpired() {
      if (this.isSelfDestruct && this.selfDestructActivatedAt != null) {
         long elapsedMs = System.currentTimeMillis() - this.selfDestructActivatedAt;
         return elapsedMs >= 300000L;
      } else {
         return false;
      }
   }

   public long getSelfDestructRemainingMs() {
      if (this.isSelfDestruct && this.selfDestructActivatedAt != null) {
         long elapsedMs = System.currentTimeMillis() - this.selfDestructActivatedAt;
         long remaining = 300000L - elapsedMs;
         return Math.max(0L, remaining);
      } else {
         return -1L;
      }
   }

   @Nullable
   public String warpName() {
      return this.warpName;
   }

   public void setWarpName(@Nullable String warpName) {
      this.warpName = warpName;
   }

   @Nullable
   public String warpDestination() {
      return this.warpDestination;
   }

   public void setWarpDestination(@Nullable String warpDestination) {
      this.warpDestination = warpDestination;
   }

   public boolean isServerTeleporter() {
      return this.isServerTeleporter;
   }

   public void setServerTeleporter(boolean isServerTeleporter) {
      this.isServerTeleporter = isServerTeleporter;
   }

   public boolean isSingleUse() {
      return this.isSingleUse;
   }

   public void setSingleUse(boolean isSingleUse) {
      this.isSingleUse = isSingleUse;
   }

   public boolean hideMapWaypoint() {
      return this.hideMapWaypoint;
   }

   public void setHideMapWaypoint(boolean hideMapWaypoint) {
      this.hideMapWaypoint = hideMapWaypoint;
   }

   public boolean allowPublicDestinationChange() {
      return this.allowPublicDestinationChange;
   }

   public void setAllowPublicDestinationChange(boolean allowPublicDestinationChange) {
      this.allowPublicDestinationChange = allowPublicDestinationChange;
   }

   public boolean canPlayerUse(UUID playerUuid) {
      if (this.ownerUuid == null) {
         return true;
      } else if (this.isOwner(playerUuid)) {
         return true;
      } else if (this.isTrusted(playerUuid)) {
         return true;
      } else {
         return this.isRestricted ? false : !this.isPartyOnly;
      }
   }

   public boolean canPlayerSee(UUID playerUuid) {
      if (this.ownerUuid == null) {
         return true;
      } else {
         return !this.isPrivate ? true : this.isOwnerOrTrusted(playerUuid);
      }
   }

   public boolean canPlayerInteract(UUID playerUuid) {
      if (this.isSelfDestruct) {
         return false;
      } else if (this.ownerUuid == null) {
         return true;
      } else {
         return this.isOwnerOrTrusted(playerUuid) ? true : !this.isInteractionLocked;
      }
   }

   public boolean canPlayerBreak(UUID playerUuid) {
      if (this.isSelfDestruct) {
         return false;
      } else if (this.ownerUuid == null) {
         return true;
      } else if (this.isOwner(playerUuid)) {
         return true;
      } else {
         return this.canSubownerBreak(playerUuid) ? true : !this.isBreakLocked;
      }
   }

   public String displayName() {
      String name = this.warpName;
      return name != null && !name.isEmpty() ? name : "%s (%d, %d, %d)".formatted(this.dimension, this.blockX, this.blockY, this.blockZ);
   }

   public String displayNameWithRestrictionStatus(String restrictedLabel) {
      String baseName = this.displayName();
      return !this.isRestricted && !this.isPartyOnly ? baseName : baseName + " - (" + restrictedLabel + ")";
   }

   public static String formatLocationKey(String dimension, int x, int y, int z) {
      return "%s:%d:%d:%d".formatted(dimension, x, y, z);
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof TeleporterInfo other && this.locationKey.equals(other.locationKey);
   }

   @Override
   public int hashCode() {
      return this.locationKey.hashCode();
   }

   @Override
   public String toString() {
      return "TeleporterInfo[%s, owner=%s, private=%b, partyOnly=%b, restricted=%b, interactionLocked=%b, breakLocked=%b, displayWorld=%b, displayCoords=%b, selfDestruct=%b, serverTeleporter=%b, singleUse=%b, hideMapWaypoint=%b, allowPublicDest=%b, warp=%s, subowners=%d]"
         .formatted(
            this.locationKey,
            this.ownerUuid,
            this.isPrivate,
            this.isPartyOnly,
            this.isRestricted,
            this.isInteractionLocked,
            this.isBreakLocked,
            this.displayWorld,
            this.displayCoordinates,
            this.isSelfDestruct,
            this.isServerTeleporter,
            this.isSingleUse,
            this.hideMapWaypoint,
            this.allowPublicDestinationChange,
            this.warpName,
            this.subowners.size()
         );
   }

   @Deprecated(forRemoval = true)
   @Nullable
   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   @Deprecated(forRemoval = true)
   public String getDimension() {
      return this.dimension;
   }

   @Deprecated(forRemoval = true)
   public int getBlockX() {
      return this.blockX;
   }

   @Deprecated(forRemoval = true)
   public int getBlockY() {
      return this.blockY;
   }

   @Deprecated(forRemoval = true)
   public int getBlockZ() {
      return this.blockZ;
   }

   @Deprecated(forRemoval = true)
   @Nullable
   public String getWarpName() {
      return this.warpName;
   }

   @Deprecated(forRemoval = true)
   @Nullable
   public String getWarpDestination() {
      return this.warpDestination;
   }

   @Deprecated(forRemoval = true)
   public String getLocationKey() {
      return this.locationKey;
   }

   @Deprecated(forRemoval = true)
   public String getDisplayName() {
      return this.displayName();
   }

   @Deprecated(forRemoval = true)
   public long getPlacedTimestamp() {
      return this.placedTimestamp;
   }
}
