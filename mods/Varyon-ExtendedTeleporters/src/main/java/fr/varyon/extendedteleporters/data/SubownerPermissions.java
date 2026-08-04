package fr.varyon.extendedteleporters.data;

import java.util.Objects;
import java.util.UUID;

public final class SubownerPermissions {
   private final UUID playerUuid;
   private volatile boolean canRename;
   private volatile boolean canSetDestination;
   private volatile boolean canModifySettings;
   private volatile boolean canBreak;

   public SubownerPermissions(UUID playerUuid) {
      this(playerUuid, false, false, false, false);
   }

   public SubownerPermissions(UUID playerUuid, boolean canRename, boolean canSetDestination, boolean canModifySettings, boolean canBreak) {
      this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
      this.canRename = canRename;
      this.canSetDestination = canSetDestination;
      this.canModifySettings = canModifySettings;
      this.canBreak = canBreak;
   }

   public UUID playerUuid() {
      return this.playerUuid;
   }

   public boolean canRename() {
      return this.canRename;
   }

   public void setCanRename(boolean canRename) {
      this.canRename = canRename;
   }

   public boolean canSetDestination() {
      return this.canSetDestination;
   }

   public void setCanSetDestination(boolean canSetDestination) {
      this.canSetDestination = canSetDestination;
   }

   public boolean canModifySettings() {
      return this.canModifySettings;
   }

   public void setCanModifySettings(boolean canModifySettings) {
      this.canModifySettings = canModifySettings;
   }

   public boolean canBreak() {
      return this.canBreak;
   }

   public void setCanBreak(boolean canBreak) {
      this.canBreak = canBreak;
   }

   public boolean hasAnyPermission() {
      return this.canRename || this.canSetDestination || this.canModifySettings || this.canBreak;
   }

   public boolean hasAllPermissions() {
      return this.canRename && this.canSetDestination && this.canModifySettings && this.canBreak;
   }

   public void setAllPermissions(boolean value) {
      this.canRename = value;
      this.canSetDestination = value;
      this.canModifySettings = value;
      this.canBreak = value;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof SubownerPermissions other && this.playerUuid.equals(other.playerUuid);
   }

   @Override
   public int hashCode() {
      return this.playerUuid.hashCode();
   }

   @Override
   public String toString() {
      return "SubownerPermissions[uuid=%s, rename=%b, destination=%b, settings=%b, break=%b]"
         .formatted(this.playerUuid, this.canRename, this.canSetDestination, this.canModifySettings, this.canBreak);
   }
}
