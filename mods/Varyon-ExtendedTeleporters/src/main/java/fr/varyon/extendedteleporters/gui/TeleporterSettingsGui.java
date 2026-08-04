package fr.varyon.extendedteleporters.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import fr.varyon.extendedteleporters.i18n.Translations;
import fr.varyon.extendedteleporters.permission.PermissionProvider;
import fr.varyon.extendedteleporters.util.WarpNameValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TeleporterSettingsGui extends InteractiveCustomUIPage<TeleporterSettingsGui.TeleporterSettingsData> {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-GUI");
   private final UUID playerUuid;
   private List<TeleporterInfo> playerTeleporters;
   private String selectedTeleporterKey;
   private String warpName = "";
   private String warpDestination = "";
   private boolean isPrivate;
   private boolean isPartyOnly;
   private boolean isRestricted;
   private boolean isInteractionLocked;
   private boolean isBreakLocked;
   private boolean displayWorld = true;
   private boolean displayCoordinates = true;
   private boolean isSelfDestruct;
   private boolean wasSelfDestructAlreadyActive;
   private boolean isSingleUse;
   private boolean hideMapWaypoint;
   private boolean allowPublicDestinationChange = true;
   private boolean isBypassing;
   private boolean showOnlyMyWarps;
   private boolean needsRefresh = true;

   public TeleporterSettingsGui(@NonNullDecl PlayerRef playerRef) {
      super(playerRef, CustomPageLifetime.CanDismiss, TeleporterSettingsGui.TeleporterSettingsData.CODEC);
      this.playerUuid = playerRef.getUuid();
      this.isBypassing = TeleporterManager.getInstance().isInBypassMode(this.playerUuid);
      this.refreshTeleporterList();
      this.selectFirstTeleporter();
   }

   private void refreshTeleporterList() {
      this.playerTeleporters = TeleporterManager.getInstance().getPlayerTeleportersSynced(this.playerUuid);
      this.needsRefresh = false;
   }

   private void selectFirstTeleporter() {
      if (!this.playerTeleporters.isEmpty()) {
         this.loadTeleporterState(this.playerTeleporters.getFirst());
      } else {
         this.clearState();
      }
   }

   private void loadTeleporterState(TeleporterInfo info) {
      this.selectedTeleporterKey = info.locationKey();
      this.warpName = info.warpName() != null ? info.warpName() : "";
      this.warpDestination = info.warpDestination() != null ? info.warpDestination() : "";
      this.isPrivate = info.isPrivate();
      this.isPartyOnly = info.isPartyOnly();
      this.isRestricted = info.isRestricted();
      this.isInteractionLocked = info.isInteractionLocked();
      this.isBreakLocked = info.isBreakLocked();
      this.displayWorld = info.displayWorld();
      this.displayCoordinates = info.displayCoordinates();
      this.isSelfDestruct = info.isSelfDestruct();
      this.wasSelfDestructAlreadyActive = info.isSelfDestruct();
      this.isSingleUse = info.isSingleUse();
      this.hideMapWaypoint = info.hideMapWaypoint();
      this.allowPublicDestinationChange = info.allowPublicDestinationChange();
   }

   private void clearState() {
      this.selectedTeleporterKey = null;
      this.warpName = "";
      this.warpDestination = "";
      this.isPrivate = false;
      this.isPartyOnly = false;
      this.isRestricted = false;
      this.isInteractionLocked = false;
      this.isBreakLocked = false;
      this.displayWorld = true;
      this.displayCoordinates = true;
      this.isSelfDestruct = false;
      this.wasSelfDestructAlreadyActive = false;
      this.isSingleUse = false;
      this.hideMapWaypoint = false;
      this.allowPublicDestinationChange = true;
   }

   private boolean canModifySettings() {
      if (this.isBypassing) {
         return true;
      }

      if (this.selectedTeleporterKey != null) {
         TeleporterInfo info = TeleporterManager.getInstance().getTeleporterByKey(this.selectedTeleporterKey);
         if (info != null && info.isServerTeleporter()) {
            return false;
         }
      }

      return true;
   }

   public void handleDataEvent(
      @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl TeleporterSettingsGui.TeleporterSettingsData data
   ) {
      super.handleDataEvent(ref, store, data);
      if (data.selectedTeleporter != null && !data.selectedTeleporter.isEmpty()) {
         TeleporterInfo selected = TeleporterManager.getInstance().getTeleporterByKey(data.selectedTeleporter);
         if (selected != null && (selected.canSubownerSetDestination(this.playerUuid) || this.isBypassing)) {
            this.loadTeleporterState(selected);
            this.rebuildUI(ref, store);
         }
      } else {
         if (data.warpName != null) {
            this.warpName = data.warpName;
         }

         if (data.warpDestination != null) {
            this.warpDestination = data.warpDestination;
         }

         if (data.visibilitySetting != null) {
            this.isPrivate = "private".equalsIgnoreCase(data.visibilitySetting);
         }

         if (data.partyOnlySetting != null) {
            this.isPartyOnly = data.partyOnlySetting;
         }

         if (data.restrictedSetting != null) {
            this.isRestricted = data.restrictedSetting;
         }

         if (data.interactionLockedSetting != null) {
            this.isInteractionLocked = data.interactionLockedSetting;
         }

         if (data.breakLockedSetting != null) {
            this.isBreakLocked = data.breakLockedSetting;
         }

         if (data.displayWorldSetting != null) {
            boolean oldDisplayWorld = this.displayWorld;
            this.displayWorld = data.displayWorldSetting;
            if (!this.displayWorld) {
               this.displayCoordinates = false;
            }

            if (oldDisplayWorld != this.displayWorld) {
               this.rebuildUI(ref, store);
               return;
            }
         }

         if (data.displayCoordinatesSetting != null) {
            this.displayCoordinates = data.displayCoordinatesSetting && this.displayWorld;
         }

         if (data.selfDestructSetting != null && this.canModifySettings() && data.selfDestructSetting && !this.wasSelfDestructAlreadyActive) {
            this.isSelfDestruct = true;
         }

         if (data.singleUseSetting != null && this.canModifySettings()) {
            this.isSingleUse = data.singleUseSetting;
         }

         if (data.hideMapWaypointSetting != null && this.canModifySettings()) {
            this.hideMapWaypoint = data.hideMapWaypointSetting;
         }

         if (data.allowPublicDestinationChangeSetting != null && this.canModifySettings()) {
            this.allowPublicDestinationChange = data.allowPublicDestinationChangeSetting;
         }

         if (data.showOnlyMyWarpsFilter != null) {
            boolean oldValue = this.showOnlyMyWarps;
            this.showOnlyMyWarps = data.showOnlyMyWarpsFilter;
            if (oldValue != this.showOnlyMyWarps) {
               this.rebuildUI(ref, store);
               return;
            }
         }

         if (data.save != null) {
            this.saveCurrentSettings();
            this.needsRefresh = true;
         }

         if (data.manageTrust != null) {
            this.openSubownerManagement(ref, store);
         } else if (data.cancel != null) {
            this.close();
         } else {
            this.sendUpdate();
         }
      }
   }

   private void rebuildUI(Ref<EntityStore> ref, Store<EntityStore> store) {
      UICommandBuilder commandBuilder = new UICommandBuilder();
      UIEventBuilder eventBuilder = new UIEventBuilder();
      this.build(ref, commandBuilder, eventBuilder, store);
      this.sendUpdate(commandBuilder, eventBuilder, true);
   }

   private void openSubownerManagement(Ref<EntityStore> ref, Store<EntityStore> store) {
      if (this.selectedTeleporterKey != null) {
         TeleporterManager manager = TeleporterManager.getInstance();
         TeleporterInfo selected = manager.getTeleporterByKey(this.selectedTeleporterKey);
         if (selected != null) {
            if (!selected.isOwner(this.playerUuid) && !this.isBypassing) {
               this.playerRef.sendMessage(Translations.msgError("subowner.noPermission"));
            } else {
               Player player = (Player)store.getComponent(ref, Player.getComponentType());
               if (player != null) {
                  player.getPageManager().openCustomPage(ref, store, new SubownerManagementGui(this.playerRef, selected));
               }
            }
         }
      }
   }

   private void saveCurrentSettings() {
      if (this.selectedTeleporterKey != null) {
         TeleporterManager manager = TeleporterManager.getInstance();
         TeleporterInfo selected = manager.getTeleporterByKey(this.selectedTeleporterKey);
         if (selected != null && (selected.canSubownerSetDestination(this.playerUuid) || this.isBypassing)) {
            PermissionProvider provider = manager.getPermissionProvider();
            if (this.isPrivate && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.private")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.private"));
               this.isPrivate = false;
            }

            if (this.isPrivate && !selected.isPrivate()) {
               int privateLimit = provider.getMetaInteger(this.playerUuid, "extendedteleporters.limit.private", -1);
               if (privateLimit >= 0) {
                  int currentPrivateCount = manager.countPlayerPrivateTeleporters(this.playerUuid);
                  if (currentPrivateCount >= privateLimit) {
                     this.playerRef.sendMessage(Translations.msgError("msg.limit.private", "current", currentPrivateCount, "max", privateLimit));
                     this.isPrivate = false;
                  }
               }
            }

            if (!this.isPrivate && selected.isPrivate()) {
               int publicLimit = provider.getMetaInteger(this.playerUuid, "extendedteleporters.limit.public", -1);
               if (publicLimit >= 0) {
                  int currentPublicCount = manager.countPlayerPublicTeleporters(this.playerUuid);
                  if (currentPublicCount >= publicLimit) {
                     this.playerRef.sendMessage(Translations.msgError("msg.limit.public", "current", currentPublicCount, "max", publicLimit));
                     this.isPrivate = true;
                  }
               }
            }

            if (this.isPartyOnly && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.restricted")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.partyOnly"));
               this.isPartyOnly = false;
            }

            if (this.isRestricted && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.truerestricted")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.restricted"));
               this.isRestricted = false;
            }

            int restrictedLimit = provider.getMetaInteger(this.playerUuid, "extendedteleporters.limit.restricted", -1);
            if (restrictedLimit >= 0) {
               int currentRestrictedCount = manager.countPlayerRestrictedTeleporters(this.playerUuid);
               int currentPartyCount = manager.countPlayerPartyOnlyTeleporters(this.playerUuid);
               int totalRestricted = currentRestrictedCount + currentPartyCount;
               if (this.isRestricted && !selected.isRestricted() && totalRestricted >= restrictedLimit) {
                  this.playerRef.sendMessage(Translations.msgError("msg.limit.restricted", "current", totalRestricted, "max", restrictedLimit));
                  this.isRestricted = false;
               }

               if (this.isPartyOnly && !selected.isPartyOnly() && totalRestricted >= restrictedLimit) {
                  this.playerRef.sendMessage(Translations.msgError("msg.limit.restricted", "current", totalRestricted, "max", restrictedLimit));
                  this.isPartyOnly = false;
               }
            }

            if (this.isInteractionLocked && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.lock_interaction")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.lockInteraction"));
               this.isInteractionLocked = false;
            }

            if (this.isBreakLocked && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.lock_break")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.lockBreak"));
               this.isBreakLocked = false;
            }

            if (!this.displayWorld && selected.displayWorld() && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_world")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.hideWorld"));
               this.displayWorld = true;
            }

            if (!this.displayCoordinates
               && selected.displayCoordinates()
               && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_coords")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.hideCoords"));
               this.displayCoordinates = true;
            }

            if (this.isSelfDestruct && !selected.isSelfDestruct() && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.self_destruct")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.selfDestruct"));
               this.isSelfDestruct = false;
            }

            if (this.hideMapWaypoint
               && !selected.hideMapWaypoint()
               && !provider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_map_waypoint")) {
               this.playerRef.sendMessage(Translations.msgError("msg.noPermission.hideMapWaypoint"));
               this.hideMapWaypoint = false;
            }

            String effectiveDestination = this.warpDestination.isEmpty() ? selected.warpDestination() : this.warpDestination;
            if (this.isSelfDestruct && !selected.isSelfDestruct() && (effectiveDestination == null || effectiveDestination.isEmpty())) {
               this.playerRef.sendMessage(Translations.msgError("msg.selfDestruct.noDestination"));
               this.isSelfDestruct = false;
            }

            if (this.isSelfDestruct && !selected.isSelfDestruct() && (this.isSingleUse || selected.isSingleUse())) {
               this.playerRef.sendMessage(Translations.msgError("msg.selfDestruct.singleUseConflict"));
               this.isSelfDestruct = false;
            }

            if (this.isSingleUse && !selected.isSingleUse() && (this.isSelfDestruct || selected.isSelfDestruct())) {
               this.playerRef.sendMessage(Translations.msgError("msg.selfDestruct.selfDestructConflict"));
               this.isSingleUse = false;
            }

            String oldWarpName = selected.warpName();
            String newWarpName = this.warpName.isEmpty() ? null : this.warpName;
            String oldWarpDest = selected.warpDestination();
            String newWarpDest = this.warpDestination.isEmpty() ? null : this.warpDestination;
            boolean wasPrivate = selected.isPrivate();
            boolean wasRestricted = selected.isRestricted();
            boolean wasHideMapWaypoint = selected.hideMapWaypoint();
            if (newWarpName != null) {
               WarpNameValidator.ValidationResult validation = WarpNameValidator.validate(newWarpName);
               if (!validation.isValid()) {
                  this.playerRef.sendMessage(Translations.msgError("msg.warpName.invalid", "error", validation.getErrorMessage()));
                  this.playerRef.sendMessage(Translations.msgInfo("msg.warpName.reason", "reason", validation.getReason()));
                  return;
               }

               Set<String> existingNames = new HashSet<>(manager.getVisibleWarps(this.playerUuid));
               if (oldWarpName != null) {
                  existingNames.remove(oldWarpName.toLowerCase());
               }

               if (WarpNameValidator.isDuplicate(newWarpName, existingNames)) {
                  String uniqueName = WarpNameValidator.generateUniqueName(newWarpName, existingNames);
                  this.playerRef.sendMessage(Translations.msgWarning("msg.warpName.duplicate", "name", newWarpName, "uniqueName", uniqueName));
                  newWarpName = uniqueName;
               }
            }

            boolean warpNameChanged = newWarpName != null && !newWarpName.equals(oldWarpName);
            if (warpNameChanged) {
               if (manager.updateTeleporterComponentWarpName(selected, newWarpName)) {
                  this.playerRef.sendMessage(Translations.msgSuccess("msg.warpName.updated", "name", newWarpName));
                  if (this.hideMapWaypoint || this.isPrivate) {
                     manager.hideWarpFromRegistry(newWarpName);
                  }
               } else {
                  this.playerRef.sendMessage(Translations.msgWarning("msg.warpName.updateFailed"));
               }
            }

            boolean destChanged = newWarpDest == null && oldWarpDest != null || newWarpDest != null && !newWarpDest.equals(oldWarpDest);
            if (destChanged) {
               selected.setWarpDestination(newWarpDest);
               if (manager.updateTeleporterWarpDestination(selected, newWarpDest)) {
                  if (newWarpDest != null) {
                     this.playerRef.sendMessage(Translations.msgSuccess("msg.destination.set", "destination", newWarpDest));
                  } else {
                     this.playerRef.sendMessage(Translations.msgWarning("msg.destination.cleared"));
                  }
               }
            }

            if (this.isPrivate != wasPrivate) {
               String warpToManage = newWarpName != null ? newWarpName : oldWarpName;
               if (warpToManage != null && !warpToManage.isEmpty()) {
                  if (this.isPrivate) {
                     manager.hideWarpFromRegistry(warpToManage);
                     this.playerRef.sendMessage(Translations.msgWarning("msg.privacy.nowPrivate"));
                  } else {
                     if (!this.hideMapWaypoint) {
                        manager.restoreWarpToRegistry(warpToManage);
                     }

                     this.playerRef.sendMessage(Translations.msgSuccess("msg.privacy.nowPublic"));
                  }
               } else if (this.isPrivate) {
                  this.playerRef.sendMessage(Translations.msgError("msg.privacy.noWarpName"));
                  this.isPrivate = false;
               }
            }

            if (this.hideMapWaypoint != wasHideMapWaypoint && !this.isPrivate) {
               String warpToManage = newWarpName != null ? newWarpName : oldWarpName;
               if (warpToManage != null && !warpToManage.isEmpty()) {
                  if (this.hideMapWaypoint) {
                     manager.hideWarpFromRegistry(warpToManage);
                  } else {
                     manager.restoreWarpToRegistry(warpToManage);
                  }
               }
            }

            selected.setWarpName(newWarpName);
            selected.setAllowPublicDestinationChange(this.allowPublicDestinationChange);
            selected.setPrivate(this.isPrivate);
            selected.setPartyOnly(this.isPartyOnly);
            selected.setRestricted(this.isRestricted);
            selected.setInteractionLocked(this.isInteractionLocked);
            selected.setBreakLocked(this.isBreakLocked);
            selected.setDisplayWorld(this.displayWorld);
            selected.setDisplayCoordinates(this.displayCoordinates);
            selected.setSingleUse(this.isSingleUse);
            selected.setHideMapWaypoint(this.hideMapWaypoint);
            if (this.isRestricted != wasRestricted) {
               if (this.isRestricted) {
                  this.playerRef.sendMessage(Translations.msgWarning("msg.restriction.nowRestricted"));
               } else {
                  String warpToRestore = selected.warpDestination();
                  if (warpToRestore != null && !warpToRestore.isEmpty()) {
                     manager.updateTeleporterWarpDestination(selected, warpToRestore);
                  }

                  this.playerRef.sendMessage(Translations.msgSuccess("msg.restriction.nowUnrestricted"));
               }
            }

            if (this.isSelfDestruct && !selected.isSelfDestruct()) {
               selected.activateSelfDestruct();
               this.wasSelfDestructAlreadyActive = true;
               this.isPrivate = false;
               this.isPartyOnly = false;
               this.isRestricted = false;
               this.isInteractionLocked = true;
               this.isBreakLocked = true;
               selected.setPrivate(false);
               selected.setPartyOnly(false);
               selected.setRestricted(false);
               selected.setInteractionLocked(true);
               selected.setBreakLocked(true);
               if (wasPrivate) {
                  String warpToManage = newWarpName != null ? newWarpName : oldWarpName;
                  if (warpToManage != null && !warpToManage.isEmpty()) {
                     manager.restoreWarpToRegistry(warpToManage);
                  }
               }

               this.playerRef.sendMessage(Translations.msgWarning("msg.selfDestruct.activated"));
               this.playerRef.sendMessage(Translations.msg("msg.selfDestruct.activatedInfo", Translations.WARNING));
            }

            manager.forceSave();
            this.playerRef.sendMessage(Translations.msgSuccess("msg.save.success"));
         }
      }
   }

   public void build(
      @NonNullDecl Ref<EntityStore> ref,
      @NonNullDecl UICommandBuilder uiCommandBuilder,
      @NonNullDecl UIEventBuilder uiEventBuilder,
      @NonNullDecl Store<EntityStore> store
   ) {
      uiCommandBuilder.append("Pages/ExtendedTeleport_TeleporterSettings.ui");
      if (this.needsRefresh) {
         this.refreshTeleporterList();
      }

      TeleporterManager manager = TeleporterManager.getInstance();
      PermissionProvider provider = manager.getPermissionProvider();
      List<TeleporterInfo> sortedTeleporters = new ArrayList<>(this.playerTeleporters);
      sortedTeleporters.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
      List<DropdownEntryInfo> entries = new ArrayList<>();
      String restrictedLabel = Translations.tr("common.restricted");

      for (TeleporterInfo info : sortedTeleporters) {
         String displayName = info.displayNameWithRestrictionStatus(restrictedLabel);
         entries.add(new DropdownEntryInfo(LocalizableString.fromString(displayName), info.locationKey()));
      }

      uiCommandBuilder.set("#TeleporterDropdown.Entries", entries);
      if (this.selectedTeleporterKey != null) {
         uiCommandBuilder.set("#TeleporterDropdown.Value", this.selectedTeleporterKey);
      }

      uiEventBuilder.addEventBinding(
         CustomUIEventBindingType.ValueChanged, "#TeleporterDropdown", EventData.of("@SelectedTeleporter", "#TeleporterDropdown.Value"), false
      );
      if (this.playerTeleporters.isEmpty()) {
         uiCommandBuilder.set("#SelectLabel.Text", Translations.tr("gui.noTeleporters"));
      }

      uiCommandBuilder.set("#WarpNameField.Value", this.warpName);
      uiCommandBuilder.set("#WarpNameField.IsReadOnly", this.selectedTeleporterKey == null);
      uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WarpNameField", EventData.of("@WarpName", "#WarpNameField.Value"), false);
      if (this.selectedTeleporterKey != null && (this.warpName == null || this.warpName.isEmpty())) {
         uiCommandBuilder.set("#WarpNameField.PlaceholderText", Translations.tr("gui.warpNamePlaceholder"));
      }

      uiCommandBuilder.set("#ShowOnlyMyWarpsFilter #CheckBox.Value", this.showOnlyMyWarps);
      uiEventBuilder.addEventBinding(
         CustomUIEventBindingType.ValueChanged,
         "#ShowOnlyMyWarpsFilter #CheckBox",
         EventData.of("@ShowOnlyMyWarpsFilter", "#ShowOnlyMyWarpsFilter #CheckBox.Value"),
         false
      );
      List<DropdownEntryInfo> warpDestinations = new ArrayList<>();
      warpDestinations.add(new DropdownEntryInfo(Translations.loc("gui.destinationNone"), ""));
      List<String> visibleWarps = manager.getVisibleWarps(this.playerUuid);
      visibleWarps.sort(String::compareToIgnoreCase);
      Set<String> playerOwnWarps = new HashSet<>();
      if (this.showOnlyMyWarps) {
         for (TeleporterInfo info : manager.getPlayerTeleporters(this.playerUuid)) {
            if (info.warpName() != null && !info.warpName().isEmpty()) {
               playerOwnWarps.add(info.warpName().toLowerCase());
            }
         }
      }

      for (String warp : visibleWarps) {
         if ((this.warpName == null || this.warpName.isEmpty() || !warp.equalsIgnoreCase(this.warpName))
            && (!this.showOnlyMyWarps || playerOwnWarps.contains(warp.toLowerCase()))) {
            String displayName = manager.formatWarpDisplayName(warp);
            warpDestinations.add(new DropdownEntryInfo(LocalizableString.fromString(displayName), warp));
         }
      }

      uiCommandBuilder.set("#WarpDestinationDropdown.Entries", warpDestinations);
      uiCommandBuilder.set("#WarpDestinationDropdown.Value", this.warpDestination != null ? this.warpDestination : "");
      uiCommandBuilder.set("#WarpDestinationDropdown.Disabled", this.selectedTeleporterKey == null);
      uiEventBuilder.addEventBinding(
         CustomUIEventBindingType.ValueChanged, "#WarpDestinationDropdown", EventData.of("@WarpDestination", "#WarpDestinationDropdown.Value"), false
      );
      boolean hasPublicDestGroup = false;
      boolean hasVisibilityGroup = false;
      boolean hasAccessGroup = false;
      boolean hasProtectionGroup = false;
      boolean hasDisplayGroup = false;
      boolean hasSpecialGroup = false;
      boolean showAllowPublicDestOption = this.canModifySettings();
      hasPublicDestGroup = showAllowPublicDestOption;
      uiCommandBuilder.set("#AllowPublicDestinationChangeGroup.Visible", showAllowPublicDestOption);
      uiCommandBuilder.set("#AllowPublicDestinationChangeSetting.Visible", showAllowPublicDestOption);
      if (showAllowPublicDestOption) {
         boolean publicDestDisabled = this.selectedTeleporterKey == null || this.isSelfDestruct;
         uiCommandBuilder.set("#AllowPublicDestinationChangeSetting #CheckBox.Value", this.allowPublicDestinationChange);
         uiCommandBuilder.set("#AllowPublicDestinationChangeSetting #CheckBox.Disabled", publicDestDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#AllowPublicDestinationChangeSetting #CheckBox",
            EventData.of("@AllowPublicDestinationChangeSetting", "#AllowPublicDestinationChangeSetting #CheckBox.Value"),
            false
         );
      }

      boolean hasPrivatePerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.private");
      hasVisibilityGroup = hasPrivatePerm;
      uiCommandBuilder.set("#VisibilityGroup.Visible", hasPrivatePerm);
      if (hasPrivatePerm) {
         boolean visibilityDisabled = this.selectedTeleporterKey == null || this.isSelfDestruct;
         List<DropdownEntryInfo> visibilityOptions = new ArrayList<>();
         visibilityOptions.add(new DropdownEntryInfo(Translations.loc("gui.visibility.public"), "public"));
         visibilityOptions.add(new DropdownEntryInfo(Translations.loc("gui.visibility.private"), "private"));
         uiCommandBuilder.set("#VisibilityDropdown.Entries", visibilityOptions);
         uiCommandBuilder.set("#VisibilityDropdown.Value", this.isPrivate ? "private" : "public");
         uiCommandBuilder.set("#VisibilityDropdown.Disabled", visibilityDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged, "#VisibilityDropdown", EventData.of("@VisibilitySetting", "#VisibilityDropdown.Value"), false
         );
      }

      boolean hasPartyOnlyPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.restricted");
      boolean hasTrueRestrictedPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.truerestricted");
      hasAccessGroup = hasPartyOnlyPerm || hasTrueRestrictedPerm;
      uiCommandBuilder.set("#PartyOnlySetting.Visible", hasPartyOnlyPerm);
      if (hasPartyOnlyPerm) {
         boolean accessDisabled = this.selectedTeleporterKey == null || this.isSelfDestruct;
         uiCommandBuilder.set("#PartyOnlySetting #CheckBox.Value", this.isPartyOnly);
         uiCommandBuilder.set("#PartyOnlySetting #CheckBox.Disabled", accessDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged, "#PartyOnlySetting #CheckBox", EventData.of("@PartyOnlySetting", "#PartyOnlySetting #CheckBox.Value"), false
         );
      }

      uiCommandBuilder.set("#RestrictedSetting.Visible", hasTrueRestrictedPerm);
      if (hasTrueRestrictedPerm) {
         boolean accessDisabled = this.selectedTeleporterKey == null || this.isSelfDestruct;
         uiCommandBuilder.set("#RestrictedSetting #CheckBox.Value", this.isRestricted);
         uiCommandBuilder.set("#RestrictedSetting #CheckBox.Disabled", accessDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#RestrictedSetting #CheckBox",
            EventData.of("@RestrictedSetting", "#RestrictedSetting #CheckBox.Value"),
            false
         );
      }

      uiCommandBuilder.set("#AccessSettingsGroup.Visible", hasAccessGroup);
      boolean hasInteractionLockPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.lock_interaction");
      boolean hasBreakLockPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.lock_break");
      hasProtectionGroup = hasInteractionLockPerm || hasBreakLockPerm;
      uiCommandBuilder.set("#InteractionLockedSetting.Visible", hasInteractionLockPerm);
      if (hasInteractionLockPerm) {
         boolean interactionDisabled = this.selectedTeleporterKey == null || this.isSelfDestruct;
         uiCommandBuilder.set("#InteractionLockedSetting #CheckBox.Value", this.isInteractionLocked);
         uiCommandBuilder.set("#InteractionLockedSetting #CheckBox.Disabled", interactionDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#InteractionLockedSetting #CheckBox",
            EventData.of("@InteractionLockedSetting", "#InteractionLockedSetting #CheckBox.Value"),
            false
         );
      }

      uiCommandBuilder.set("#BreakLockedSetting.Visible", hasBreakLockPerm);
      if (hasBreakLockPerm) {
         boolean breakDisabled = this.selectedTeleporterKey == null || this.isSelfDestruct;
         uiCommandBuilder.set("#BreakLockedSetting #CheckBox.Value", this.isBreakLocked);
         uiCommandBuilder.set("#BreakLockedSetting #CheckBox.Disabled", breakDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#BreakLockedSetting #CheckBox",
            EventData.of("@BreakLockedSetting", "#BreakLockedSetting #CheckBox.Value"),
            false
         );
      }

      uiCommandBuilder.set("#ProtectionSettingsGroup.Visible", hasProtectionGroup);
      boolean hasHideWorldPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_world");
      boolean hasHideCoordsPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_coords");
      boolean hasHideMapWaypointPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_map_waypoint");
      hasDisplayGroup = hasHideWorldPerm || hasHideCoordsPerm || hasHideMapWaypointPerm;
      uiCommandBuilder.set("#DisplayWorldSetting.Visible", hasHideWorldPerm);
      if (hasHideWorldPerm) {
         boolean displayWorldDisabled = this.selectedTeleporterKey == null;
         uiCommandBuilder.set("#DisplayWorldSetting #CheckBox.Value", this.displayWorld);
         uiCommandBuilder.set("#DisplayWorldSetting #CheckBox.Disabled", displayWorldDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DisplayWorldSetting #CheckBox",
            EventData.of("@DisplayWorldSetting", "#DisplayWorldSetting #CheckBox.Value"),
            false
         );
      }

      uiCommandBuilder.set("#DisplayCoordinatesSetting.Visible", hasHideCoordsPerm);
      if (hasHideCoordsPerm) {
         boolean displayCoordsDisabled = this.selectedTeleporterKey == null || !this.displayWorld;
         uiCommandBuilder.set("#DisplayCoordinatesSetting #CheckBox.Value", this.displayCoordinates);
         uiCommandBuilder.set("#DisplayCoordinatesSetting #CheckBox.Disabled", displayCoordsDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DisplayCoordinatesSetting #CheckBox",
            EventData.of("@DisplayCoordinatesSetting", "#DisplayCoordinatesSetting #CheckBox.Value"),
            false
         );
      }

      uiCommandBuilder.set("#HideMapWaypointSetting.Visible", hasHideMapWaypointPerm);
      if (hasHideMapWaypointPerm) {
         boolean hideMapWaypointDisabled = this.selectedTeleporterKey == null;
         uiCommandBuilder.set("#HideMapWaypointSetting #CheckBox.Value", this.hideMapWaypoint);
         uiCommandBuilder.set("#HideMapWaypointSetting #CheckBox.Disabled", hideMapWaypointDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#HideMapWaypointSetting #CheckBox",
            EventData.of("@HideMapWaypointSetting", "#HideMapWaypointSetting #CheckBox.Value"),
            false
         );
      }

      uiCommandBuilder.set("#DisplaySettingsGroup.Visible", hasDisplayGroup);
      boolean hasSelfDestructPerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.self_destruct");
      boolean hasSingleUsePerm = provider.hasPermission(this.playerUuid, "extendedteleporters.feature.single_use");
      hasSpecialGroup = hasSelfDestructPerm || hasSingleUsePerm;
      uiCommandBuilder.set("#SelfDestructSetting.Visible", hasSelfDestructPerm);
      if (hasSelfDestructPerm) {
         boolean selfDestructDisabled = this.selectedTeleporterKey == null || this.wasSelfDestructAlreadyActive || !this.canModifySettings();
         uiCommandBuilder.set("#SelfDestructSetting #CheckBox.Value", this.isSelfDestruct);
         uiCommandBuilder.set("#SelfDestructSetting #CheckBox.Disabled", selfDestructDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SelfDestructSetting #CheckBox",
            EventData.of("@SelfDestructSetting", "#SelfDestructSetting #CheckBox.Value"),
            false
         );
      }

      if (this.wasSelfDestructAlreadyActive && this.selectedTeleporterKey != null && hasSelfDestructPerm) {
         TeleporterInfo selected = manager.getTeleporterByKey(this.selectedTeleporterKey);
         if (selected != null) {
            long remainingMs = selected.getSelfDestructRemainingMs();
            if (remainingMs > 0L) {
               long remainingSeconds = remainingMs / 1000L;
               long minutes = remainingSeconds / 60L;
               long seconds = remainingSeconds % 60L;
               uiCommandBuilder.set(
                  "#SelfDestructTimer.Text", Translations.tr("gui.selfDestructTimer", "minutes", minutes, "seconds", String.format("%02d", seconds))
               );
               uiCommandBuilder.set("#SelfDestructTimer.Visible", true);
            } else {
               uiCommandBuilder.set("#SelfDestructTimer.Text", Translations.tr("gui.selfDestructImminent"));
               uiCommandBuilder.set("#SelfDestructTimer.Visible", true);
            }
         }
      } else {
         uiCommandBuilder.set("#SelfDestructTimer.Visible", false);
      }

      uiCommandBuilder.set("#SingleUseSetting.Visible", hasSingleUsePerm);
      if (hasSingleUsePerm) {
         boolean singleUseDisabled = this.selectedTeleporterKey == null || !this.canModifySettings();
         uiCommandBuilder.set("#SingleUseSetting #CheckBox.Value", this.isSingleUse);
         uiCommandBuilder.set("#SingleUseSetting #CheckBox.Disabled", singleUseDisabled);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged, "#SingleUseSetting #CheckBox", EventData.of("@SingleUseSetting", "#SingleUseSetting #CheckBox.Value"), false
         );
      }

      uiCommandBuilder.set("#SpecialModesGroup.Visible", hasSpecialGroup);
      boolean hasAnySettings = hasPublicDestGroup || hasVisibilityGroup || hasAccessGroup || hasProtectionGroup || hasDisplayGroup || hasSpecialGroup;
      uiCommandBuilder.set("#SettingsSection.Visible", hasAnySettings);
      if (this.selectedTeleporterKey != null) {
         TeleporterInfo selected = manager.getTeleporterByKey(this.selectedTeleporterKey);
         if (selected != null) {
            uiCommandBuilder.set(
               "#LocationLabel.Text",
               Translations.tr("gui.location", "dimension", selected.dimension(), "x", selected.blockX(), "y", selected.blockY(), "z", selected.blockZ())
            );
            uiCommandBuilder.set("#OwnerLabel.Text", Translations.tr("gui.ownerYou"));
         }
      } else {
         uiCommandBuilder.set("#LocationLabel.Text", Translations.tr("gui.locationNotSelected"));
         uiCommandBuilder.set("#OwnerLabel.Text", "");
      }

      boolean canManageTrust = false;
      if (this.selectedTeleporterKey != null) {
         TeleporterInfo selectedForButton = manager.getTeleporterByKey(this.selectedTeleporterKey);
         canManageTrust = selectedForButton != null && (selectedForButton.isOwner(this.playerUuid) || this.isBypassing);
      }

      uiCommandBuilder.set("#ManageTrustButton.Visible", canManageTrust);
      if (canManageTrust) {
         uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ManageTrustButton", EventData.of("ManageTrust", "true"), false);
      }

      uiCommandBuilder.set("#SaveChangesButton.Disabled", this.selectedTeleporterKey == null);
      uiEventBuilder.addEventBinding(
         CustomUIEventBindingType.Activating, "#SaveChangesButton", EventData.of("Save", "true").append("@WarpName", "#WarpNameField.Value"), false
      );
      uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Cancel", "true"), false);
   }

   public static final class TeleporterSettingsData {
       public static final BuilderCodec<TeleporterSettingsGui.TeleporterSettingsData> CODEC;

       static {
          Builder<TeleporterSettingsGui.TeleporterSettingsData> b = BuilderCodec.<TeleporterSettingsGui.TeleporterSettingsData>builder(
             TeleporterSettingsGui.TeleporterSettingsData.class,
             TeleporterSettingsGui.TeleporterSettingsData::new
          );
          b = b.addField(
             new KeyedCodec("@SelectedTeleporter", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.selectedTeleporter = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.selectedTeleporter
          );
          b = b.addField(
             new KeyedCodec("@WarpName", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.warpName = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.warpName
          );
          b = b.addField(
             new KeyedCodec("@WarpDestination", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.warpDestination = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.warpDestination
          );
          b = b.addField(
             new KeyedCodec("@ShowOnlyMyWarpsFilter", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.showOnlyMyWarpsFilter = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.showOnlyMyWarpsFilter
          );
          b = b.addField(
             new KeyedCodec("@VisibilitySetting", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.visibilitySetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.visibilitySetting
          );
          b = b.addField(
             new KeyedCodec("@PartyOnlySetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.partyOnlySetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.partyOnlySetting
          );
          b = b.addField(
             new KeyedCodec("@RestrictedSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.restrictedSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.restrictedSetting
          );
          b = b.addField(
             new KeyedCodec("@InteractionLockedSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.interactionLockedSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.interactionLockedSetting
          );
          b = b.addField(
             new KeyedCodec("@BreakLockedSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.breakLockedSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.breakLockedSetting
          );
          b = b.addField(
             new KeyedCodec("@DisplayWorldSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.displayWorldSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.displayWorldSetting
          );
          b = b.addField(
             new KeyedCodec("@DisplayCoordinatesSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.displayCoordinatesSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.displayCoordinatesSetting
          );
          b = b.addField(
             new KeyedCodec("@SelfDestructSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.selfDestructSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.selfDestructSetting
          );
          b = b.addField(
             new KeyedCodec("@SingleUseSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.singleUseSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.singleUseSetting
          );
          b = b.addField(
             new KeyedCodec("@HideMapWaypointSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.hideMapWaypointSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.hideMapWaypointSetting
          );
          b = b.addField(
             new KeyedCodec("@AllowPublicDestinationChangeSetting", Codec.BOOLEAN),
             (TeleporterSettingsGui.TeleporterSettingsData d, Boolean s) -> d.allowPublicDestinationChangeSetting = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.allowPublicDestinationChangeSetting
          );
          b = b.addField(
             new KeyedCodec("ManageTrust", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.manageTrust = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.manageTrust
          );
          b = b.addField(
             new KeyedCodec("Save", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.save = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.save
          );
          b = b.addField(
             new KeyedCodec("Cancel", Codec.STRING),
             (TeleporterSettingsGui.TeleporterSettingsData d, String s) -> d.cancel = s,
             (TeleporterSettingsGui.TeleporterSettingsData d) -> d.cancel
          );
          CODEC = b.build();
       }
      private String selectedTeleporter;
      private String warpName;
      private String warpDestination;
      private Boolean showOnlyMyWarpsFilter;
      private String visibilitySetting;
      private Boolean partyOnlySetting;
      private Boolean restrictedSetting;
      private Boolean interactionLockedSetting;
      private Boolean breakLockedSetting;
      private Boolean displayWorldSetting;
      private Boolean displayCoordinatesSetting;
      private Boolean selfDestructSetting;
      private Boolean singleUseSetting;
      private Boolean hideMapWaypointSetting;
      private Boolean allowPublicDestinationChangeSetting;
      private String manageTrust;
      private String save;
      private String cancel;
   }
}
