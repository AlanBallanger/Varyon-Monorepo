package fr.varyon.extendedteleporters.gui;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.system.CreateWarpWhenTeleporterPlacedSystem;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.VaryonExtendedTeleportersPlugin;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.config.ExtendedTeleportConfig;
import fr.varyon.extendedteleporters.data.SubownerPermissions;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import fr.varyon.extendedteleporters.i18n.Translations;
import fr.varyon.extendedteleporters.permission.PermissionProvider;
import fr.varyon.extendedteleporters.util.WarpNameValidator;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TeleporterBlockSettingsGui extends InteractiveCustomUIPage<TeleporterBlockSettingsGui.PageEventData> {
   private final UUID playerUuid;
   private final Ref<ChunkStore> blockRef;
   private final World world;
   private final String activeState;
   private final String dimension;
   private final int blockX;
   private final int blockY;
   private final int blockZ;
   private TeleporterInfo teleporterInfo;
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
   private boolean isOwner;
   private boolean isSubowner;
   private boolean isSingleUse;
   private boolean hideMapWaypoint;
   private boolean allowPublicDestinationChange = true;
   private boolean isBypassing;
   private boolean showOnlyMyWarps;
   private boolean subownerCanRename;
   private boolean subownerCanSetDestination;
   private boolean subownerCanModifySettings;
   private boolean subownerCanBreak;

   public TeleporterBlockSettingsGui(
      @NonNullDecl PlayerRef playerRef,
      @NonNullDecl Ref<ChunkStore> blockRef,
      @Nullable TeleporterInfo info,
      @NonNullDecl World world,
      @Nullable String activeState
   ) {
      super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, TeleporterBlockSettingsGui.PageEventData.CODEC);
      this.playerUuid = playerRef.getUuid();
      this.blockRef = blockRef;
      this.world = world;
      this.activeState = activeState != null ? activeState : "active";
      this.dimension = world.getName();
      BlockStateInfo blockStateInfo = (BlockStateInfo)blockRef.getStore().getComponent(blockRef, BlockStateInfo.getComponentType());
      org.joml.Vector3i worldPos = new org.joml.Vector3i();
      if (blockStateInfo != null && blockStateInfo.fillWorldPos(blockRef.getStore(), worldPos)) {
         this.blockX = worldPos.x;
         this.blockY = worldPos.y;
         this.blockZ = worldPos.z;
      } else {
         this.blockX = 0;
         this.blockY = 0;
         this.blockZ = 0;
      }

      this.teleporterInfo = info;
      this.loadCurrentState();
   }

   private boolean canModifySettings() {
      if (this.isBypassing) {
         return true;
      } else if (this.teleporterInfo != null && this.teleporterInfo.isServerTeleporter()) {
         return false;
      } else {
         return this.isOwner ? true : this.isSubowner && this.subownerCanModifySettings;
      }
   }

   private boolean canRename() {
      if (this.isBypassing) {
         return true;
      } else if (this.teleporterInfo != null && this.teleporterInfo.isServerTeleporter()) {
         return false;
      } else {
         return this.isOwner ? true : this.isSubowner && this.subownerCanRename;
      }
   }

   private boolean canSetDestination() {
      if (this.isBypassing) {
         return true;
      } else {
         boolean isSelfDestructActive = this.teleporterInfo != null && this.teleporterInfo.isSelfDestruct();
         if (this.teleporterInfo != null && this.teleporterInfo.isServerTeleporter()) {
            return true;
         } else if (!isSelfDestructActive && this.teleporterInfo != null && this.teleporterInfo.allowPublicDestinationChange()) {
            return true;
         } else {
            return this.isOwner ? true : this.isSubowner && this.subownerCanSetDestination;
         }
      }
   }

   private void loadCurrentState() {
      TeleporterManager manager = TeleporterManager.getInstance();
      this.isBypassing = manager.isInBypassMode(this.playerUuid);
      Teleporter teleporter = (Teleporter)this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
      if (teleporter != null) {
         String componentWarpName = teleporter.getOwnedWarp();
         if (this.teleporterInfo != null) {
            if (componentWarpName != null && !componentWarpName.isEmpty()) {
               this.teleporterInfo.setWarpName(componentWarpName);
            }

            String infoWarpName = this.teleporterInfo.warpName();
            if (infoWarpName != null && !infoWarpName.isEmpty()) {
               this.warpName = infoWarpName;
            } else if (componentWarpName != null && !componentWarpName.isEmpty()) {
               this.warpName = componentWarpName;
            } else {
               this.warpName = "";
            }

            this.warpDestination = this.teleporterInfo.warpDestination() != null ? this.teleporterInfo.warpDestination() : "";
            this.isPrivate = this.teleporterInfo.isPrivate();
            this.isPartyOnly = this.teleporterInfo.isPartyOnly();
            this.isRestricted = this.teleporterInfo.isRestricted();
            this.isInteractionLocked = this.teleporterInfo.isInteractionLocked();
            this.isBreakLocked = this.teleporterInfo.isBreakLocked();
            this.displayWorld = this.teleporterInfo.displayWorld();
            this.displayCoordinates = this.teleporterInfo.displayCoordinates();
            this.isSelfDestruct = this.teleporterInfo.isSelfDestruct();
            this.wasSelfDestructAlreadyActive = this.teleporterInfo.isSelfDestruct();
            this.isOwner = this.teleporterInfo.isOwner(this.playerUuid);
            this.isSubowner = this.teleporterInfo.isTrusted(this.playerUuid);
            this.isSingleUse = this.teleporterInfo.isSingleUse();
            this.hideMapWaypoint = this.teleporterInfo.hideMapWaypoint();
            this.allowPublicDestinationChange = this.teleporterInfo.allowPublicDestinationChange();
            if (this.isSubowner) {
               SubownerPermissions perms = this.teleporterInfo.getSubownerPermissions(this.playerUuid);
               if (perms != null) {
                  this.subownerCanRename = perms.canRename();
                  this.subownerCanSetDestination = perms.canSetDestination();
                  this.subownerCanModifySettings = perms.canModifySettings();
                  this.subownerCanBreak = perms.canBreak();
               }
            }
         } else {
            this.warpName = componentWarpName != null ? componentWarpName : "";
            this.warpDestination = teleporter.getWarp() != null ? teleporter.getWarp() : "";
            this.isPrivate = false;
            this.isPartyOnly = false;
            this.isRestricted = false;
            this.isInteractionLocked = false;
            this.isBreakLocked = false;
            this.displayWorld = true;
            this.displayCoordinates = true;
            this.isSelfDestruct = false;
            this.wasSelfDestructAlreadyActive = false;
            this.isOwner = false;
            this.isSubowner = false;
            this.isSingleUse = false;
            this.hideMapWaypoint = false;
            this.allowPublicDestinationChange = true;
            this.subownerCanRename = false;
            this.subownerCanSetDestination = false;
            this.subownerCanModifySettings = false;
            this.subownerCanBreak = false;
         }
      }
   }

   public void handleDataEvent(
      @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl TeleporterBlockSettingsGui.PageEventData data
   ) {
      super.handleDataEvent(ref, store, data);
      if (data.warpName != null && this.canRename()) {
         this.warpName = data.warpName;
      }

      if (data.warpDestination != null && this.canSetDestination()) {
         this.warpDestination = data.warpDestination;
      }

      if (data.visibilitySetting != null && this.canModifySettings()) {
         this.isPrivate = "private".equalsIgnoreCase(data.visibilitySetting);
      }

      if (data.partyOnlySetting != null && this.canModifySettings()) {
         this.isPartyOnly = data.partyOnlySetting;
      }

      if (data.restrictedSetting != null && this.canModifySettings()) {
         this.isRestricted = data.restrictedSetting;
      }

      if (data.interactionLockedSetting != null && this.canModifySettings()) {
         this.isInteractionLocked = data.interactionLockedSetting;
      }

      if (data.breakLockedSetting != null && this.canModifySettings()) {
         this.isBreakLocked = data.breakLockedSetting;
      }

      if (data.displayWorldSetting != null && this.canModifySettings()) {
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

      if (data.displayCoordinatesSetting != null && this.canModifySettings()) {
         this.displayCoordinates = data.displayCoordinatesSetting && this.displayWorld;
      }

      if (data.selfDestructSetting != null && this.canModifySettings() && data.selfDestructSetting && !this.wasSelfDestructAlreadyActive) {
         this.isSelfDestruct = true;
      }

      if (data.singleUseSetting != null && this.canModifySettings()) {
         if (!data.singleUseSetting || !this.isSelfDestruct && !this.wasSelfDestructAlreadyActive) {
            this.isSingleUse = data.singleUseSetting;
         } else {
            this.playerRef.sendMessage(Translations.msgError("msg.selfDestruct.selfDestructConflict"));
         }
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

      if (data.manageTrust != null) {
         this.openTrustManagement(ref, store);
      } else if (data.save != null) {
         this.saveSettings();
         this.close();
      } else if (data.cancel != null) {
         this.close();
      } else {
         this.sendUpdate();
      }
   }

   private void rebuildUI(Ref<EntityStore> ref, Store<EntityStore> store) {
      UICommandBuilder commandBuilder = new UICommandBuilder();
      UIEventBuilder eventBuilder = new UIEventBuilder();
      this.build(ref, commandBuilder, eventBuilder, store);
      this.sendUpdate(commandBuilder, eventBuilder, true);
   }

   private void openTrustManagement(Ref<EntityStore> ref, Store<EntityStore> store) {
      if (this.teleporterInfo != null) {
         if (!this.isOwner && !this.isBypassing) {
            this.playerRef.sendMessage(Translations.msgError("subowner.noPermission"));
         } else {
            Player player = (Player)store.getComponent(ref, Player.getComponentType());
            if (player != null) {
               player.getPageManager().openCustomPage(ref, store, new SubownerManagementGui(this.playerRef, this.teleporterInfo));
            }
         }
      }
   }

   private void saveSettings() {
      Teleporter teleporter = (Teleporter)this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
      if (teleporter != null) {
         TeleporterManager manager = TeleporterManager.getInstance();
         String oldWarpName = teleporter.getOwnedWarp();
         String newWarpName = this.canRename() ? (this.warpName.isEmpty() ? null : this.warpName) : oldWarpName;
         String newWarpDest = this.canSetDestination()
            ? (this.warpDestination.isEmpty() ? null : this.warpDestination)
            : (this.teleporterInfo != null ? this.teleporterInfo.warpDestination() : null);
         if (newWarpName != null && this.canRename()) {
            WarpNameValidator.ValidationResult validation = WarpNameValidator.validate(newWarpName);
            if (!validation.isValid()) {
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.warpName.invalid", "error", validation.getErrorMessage())).color(Color.RED));
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.warpName.reason", "reason", validation.getReason())).color(Color.GRAY));
               return;
            }
         }

         if (this.isOwner && VaryonExtendedTeleportersPlugin.CONFIG != null) {
            if (this.isPrivate && !((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowPrivateTeleporters()) {
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.config.privateDisabled")).color(Color.RED));
               this.isPrivate = false;
            }

            if (this.isPartyOnly && !((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowRestrictedTeleporters()) {
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.config.partyDisabled")).color(Color.RED));
               this.isPartyOnly = false;
            }

            if (this.isRestricted && !((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowRestrictedTeleporters()) {
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.config.restrictedDisabled")).color(Color.RED));
               this.isRestricted = false;
            }

            if (this.isInteractionLocked && !((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowInteractionLockedTeleporters()) {
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.config.interactionLockDisabled")).color(Color.RED));
               this.isInteractionLocked = false;
            }

            if (this.isBreakLocked && !((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowBreakLockedTeleporters()) {
               this.playerRef.sendMessage(Message.raw(Translations.tr("msg.config.breakLockDisabled")).color(Color.RED));
               this.isBreakLocked = false;
            }
         }

         if (this.teleporterInfo != null && !this.isPrivate && this.teleporterInfo.isPrivate()) {
            PermissionProvider provider = manager.getPermissionProvider();
            int publicLimit = provider.getMetaInteger(this.playerUuid, "extendedteleporters.limit.public", -1);
            if (publicLimit >= 0) {
               int currentPublicCount = manager.countPlayerPublicTeleporters(this.playerUuid);
               if (currentPublicCount >= publicLimit) {
                  this.playerRef
                     .sendMessage(Message.raw(Translations.tr("msg.limit.public", "current", currentPublicCount, "max", publicLimit)).color(Color.RED));
                  this.isPrivate = true;
               }
            }
         }

         if (this.canRename() && newWarpName != null && !newWarpName.equalsIgnoreCase(oldWarpName)) {
            Set<String> existingNames = new HashSet<>(TeleportPlugin.get().getWarps().keySet());
            if (oldWarpName != null) {
               existingNames.remove(oldWarpName.toLowerCase());
            }

            String finalWarpName = newWarpName;
            if (WarpNameValidator.isDuplicate(newWarpName, existingNames)) {
               finalWarpName = WarpNameValidator.generateUniqueName(newWarpName, existingNames);
               this.playerRef
                  .sendMessage(Message.raw(Translations.tr("msg.warpName.duplicate", "name", newWarpName, "uniqueName", finalWarpName)).color(Color.YELLOW));
            }

            if (oldWarpName != null && !oldWarpName.isEmpty()) {
               TeleportPlugin.get().getWarps().remove(oldWarpName.toLowerCase());
               manager.renameWarpInRegistry(oldWarpName, finalWarpName);
            }

            BlockStateInfo blockStateInfo = (BlockStateInfo)this.blockRef.getStore().getComponent(this.blockRef, BlockStateInfo.getComponentType());
            if (blockStateInfo != null) {
               Ref<ChunkStore> chunkRef = blockStateInfo.getSectionRef();
               if (chunkRef != null && chunkRef.isValid()) {
                  CreateWarpWhenTeleporterPlacedSystem.createWarp(this.blockRef.getStore(), chunkRef, blockStateInfo, finalWarpName);
               }
            }

            String warpNameForLambda = finalWarpName;
            this.world.execute(() -> {
               teleporter.setOwnedWarp(warpNameForLambda);
               teleporter.setIsCustomName(true);
            });
            this.playerRef.sendMessage(Message.raw(Translations.tr("msg.warpName.set", "name", finalWarpName)).color(Color.GREEN));
            newWarpName = finalWarpName;
            if (this.hideMapWaypoint || this.isPrivate) {
               manager.hideWarpFromRegistry(finalWarpName);
            }
         }

         if (this.canSetDestination()) {
            String finalWarpDest = newWarpDest;
            this.world.execute(() -> {
               teleporter.setWarp(finalWarpDest);
               boolean hasDestination = finalWarpDest != null && !finalWarpDest.isEmpty();
               this.updateBlockState(hasDestination);
            });
         }

         if (this.teleporterInfo != null) {
            if (this.canRename()) {
               this.teleporterInfo.setWarpName(newWarpName);
            }

            if (this.canSetDestination()) {
               this.teleporterInfo.setWarpDestination(newWarpDest);
            }

            if (this.canModifySettings()) {
               boolean wasPrivate = this.teleporterInfo.isPrivate();
               boolean wasHideMapWaypoint = this.teleporterInfo.hideMapWaypoint();
               this.teleporterInfo.setAllowPublicDestinationChange(this.allowPublicDestinationChange);
               this.teleporterInfo.setSingleUse(this.isSingleUse);
               this.teleporterInfo.setHideMapWaypoint(this.hideMapWaypoint);
               this.teleporterInfo.setPrivate(this.isPrivate);
               this.teleporterInfo.setPartyOnly(this.isPartyOnly);
               this.teleporterInfo.setRestricted(this.isRestricted);
               this.teleporterInfo.setInteractionLocked(this.isInteractionLocked);
               this.teleporterInfo.setBreakLocked(this.isBreakLocked);
               this.teleporterInfo.setDisplayWorld(this.displayWorld);
               this.teleporterInfo.setDisplayCoordinates(this.displayCoordinates);
               String warpToManage = newWarpName != null && !newWarpName.isEmpty() ? newWarpName : oldWarpName;
               if ((warpToManage == null || warpToManage.isEmpty()) && teleporter != null) {
                  warpToManage = teleporter.getOwnedWarp();
               }

               if (warpToManage != null && !warpToManage.isEmpty() && wasPrivate != this.isPrivate) {
                  if (this.isPrivate) {
                     manager.hideWarpFromRegistry(warpToManage);
                     this.playerRef.sendMessage(Message.raw(Translations.tr("msg.privacy.nowPrivate")).color(Color.YELLOW));
                  } else {
                     if (!this.hideMapWaypoint) {
                        manager.restoreWarpToRegistry(warpToManage);
                     }

                     this.playerRef.sendMessage(Message.raw(Translations.tr("msg.privacy.nowPublic")).color(Color.GREEN));
                  }
               }

               if (warpToManage != null && !warpToManage.isEmpty() && this.hideMapWaypoint != wasHideMapWaypoint && !this.isPrivate) {
                  if (this.hideMapWaypoint) {
                     manager.hideWarpFromRegistry(warpToManage);
                  } else {
                     manager.restoreWarpToRegistry(warpToManage);
                  }
               }

               if (this.isSelfDestruct && !this.teleporterInfo.isSelfDestruct()) {
                  PermissionProvider permProvider = manager.getPermissionProvider();
                  if (!permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.self_destruct")) {
                     this.playerRef.sendMessage(Message.raw(Translations.tr("msg.noPermission.selfDestruct")).color(Color.RED));
                     this.isSelfDestruct = false;
                  } else if (!this.isSingleUse && !this.teleporterInfo.isSingleUse()) {
                     String effectiveDestination = newWarpDest != null ? newWarpDest : this.teleporterInfo.warpDestination();
                     if (effectiveDestination != null && !effectiveDestination.isEmpty()) {
                        this.teleporterInfo.activateSelfDestruct();
                        this.wasSelfDestructAlreadyActive = true;
                        this.isPrivate = false;
                        this.isPartyOnly = false;
                        this.isRestricted = false;
                        this.isInteractionLocked = true;
                        this.isBreakLocked = true;
                        this.teleporterInfo.setPrivate(false);
                        this.teleporterInfo.setPartyOnly(false);
                        this.teleporterInfo.setRestricted(false);
                        this.teleporterInfo.setInteractionLocked(true);
                        this.teleporterInfo.setBreakLocked(true);
                        if (wasPrivate && warpToManage != null && !warpToManage.isEmpty()) {
                           manager.restoreWarpToRegistry(warpToManage);
                        }

                        this.playerRef.sendMessage(Message.raw(Translations.tr("msg.selfDestruct.activated")).color(Color.ORANGE));
                        this.playerRef.sendMessage(Message.raw(Translations.tr("msg.selfDestruct.activatedInfo")).color(Color.YELLOW));
                     } else {
                        this.playerRef.sendMessage(Message.raw(Translations.tr("msg.selfDestruct.noDestination")).color(Color.RED));
                        this.isSelfDestruct = false;
                     }
                  } else {
                     this.playerRef.sendMessage(Message.raw(Translations.tr("msg.selfDestruct.singleUseConflict")).color(Color.RED));
                     this.isSelfDestruct = false;
                  }
               }
            }

            manager.forceSave();
         }

         BlockStateInfo blockStateInfo = (BlockStateInfo)this.blockRef.getStore().getComponent(this.blockRef, BlockStateInfo.getComponentType());
         if (blockStateInfo != null) {
            blockStateInfo.markNeedsSaving();
         }

         this.playerRef.sendMessage(Message.raw(Translations.tr("msg.save.teleporterSaved")).color(Color.GREEN));
      }
   }

   private void updateBlockState(boolean isActive) {
      try {
         BlockStateInfo blockStateInfo = (BlockStateInfo)this.blockRef.getStore().getComponent(this.blockRef, BlockStateInfo.getComponentType());
         if (blockStateInfo == null) {
            return;
         }

         Ref<ChunkStore> chunkRef = blockStateInfo.getSectionRef();
         if (chunkRef == null || !chunkRef.isValid()) {
            return;
         }

         long chunkIndex = ChunkUtil.indexChunkFromBlock(this.blockX, this.blockZ);
         WorldChunk worldChunk = (WorldChunk)this.world.getChunkStore().getChunkComponent(chunkIndex, WorldChunk.getComponentType());
         if (worldChunk == null) {
            return;
         }

         String newState = isActive ? this.activeState : "default";
         BlockType blockType = worldChunk.getBlockType(this.blockX, this.blockY, this.blockZ);
         if (blockType != null) {
            String currentState = blockType.getStateForBlock(blockType);
            if (currentState == null || !currentState.equals(newState)) {
               BlockType variantBlockType = blockType.getBlockForState(newState);
               if (variantBlockType != null) {
                  worldChunk.setBlockInteractionState(this.blockX, this.blockY, this.blockZ, variantBlockType, newState, true);
               }
            }
         }
      } catch (Exception var9) {
      }
   }

   public void build(
      @NonNullDecl Ref<EntityStore> ref,
      @NonNullDecl UICommandBuilder uiCommandBuilder,
      @NonNullDecl UIEventBuilder uiEventBuilder,
      @NonNullDecl Store<EntityStore> store
   ) {
      uiCommandBuilder.append("Pages/ExtendedTeleport_TeleporterSettings.ui");
      Teleporter teleporter = (Teleporter)this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
      if (teleporter == null) {
         uiCommandBuilder.set("#SelectLabel.Text", Translations.tr("msg.error.teleporterNotFound"));
      } else {
         TeleporterManager manager = TeleporterManager.getInstance();
         PermissionProvider permProvider = manager.getPermissionProvider();
         uiCommandBuilder.set("#TeleporterSelectionSection.Visible", false);
         if (this.teleporterInfo != null && this.teleporterInfo.isServerTeleporter() && !this.isBypassing) {
            boolean settingsLocked = true;
         } else {
            boolean settingsLocked = false;
         }

         uiCommandBuilder.set("#WarpNameField.Value", this.warpName);
         uiCommandBuilder.set("#WarpNameField.PlaceholderText", Translations.tr("gui.warpNamePlaceholder"));
         uiCommandBuilder.set("#WarpNameField.IsReadOnly", !this.canRename());
         uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WarpNameField", EventData.of("@WarpName", "#WarpNameField.Value"), false);
         uiCommandBuilder.set("#ShowOnlyMyWarpsFilter #CheckBox.Value", this.showOnlyMyWarps);
         uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#ShowOnlyMyWarpsFilter #CheckBox",
            EventData.of("@ShowOnlyMyWarpsFilter", "#ShowOnlyMyWarpsFilter #CheckBox.Value"),
            false
         );
         List<DropdownEntryInfo> warpDestinations = new ArrayList<>();
         warpDestinations.add(new DropdownEntryInfo(LocalizableString.fromString(Translations.tr("gui.destinationNone")), ""));
         List<String> visibleWarps = manager.getVisibleWarps(this.playerUuid);

         for (Warp warp : TeleportPlugin.get().getWarps().values()) {
            String warpId = warp.getId().toLowerCase();
            if (!manager.isCustomDestination(warpId) && !visibleWarps.contains(warpId)) {
               visibleWarps.add(warpId);
            }
         }

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
         uiCommandBuilder.set("#WarpDestinationDropdown.Disabled", !this.canSetDestination());
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
            boolean publicDestDisabled = this.isSelfDestruct;
            uiCommandBuilder.set("#AllowPublicDestinationChangeSetting #CheckBox.Value", this.allowPublicDestinationChange);
            uiCommandBuilder.set("#AllowPublicDestinationChangeSetting #CheckBox.Disabled", publicDestDisabled);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#AllowPublicDestinationChangeSetting #CheckBox",
               EventData.of("@AllowPublicDestinationChangeSetting", "#AllowPublicDestinationChangeSetting #CheckBox.Value"),
               false
            );
         }

         boolean privateAllowedByConfig = VaryonExtendedTeleportersPlugin.CONFIG == null || ((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowPrivateTeleporters();
         boolean hasPrivatePerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.private");
         boolean showVisibilityOption = privateAllowedByConfig && hasPrivatePerm && this.canModifySettings();
         hasVisibilityGroup = showVisibilityOption;
         uiCommandBuilder.set("#VisibilityGroup.Visible", showVisibilityOption);
         if (showVisibilityOption) {
            boolean visibilityDisabled = this.isSelfDestruct;
            List<DropdownEntryInfo> visibilityOptions = new ArrayList<>();
            visibilityOptions.add(new DropdownEntryInfo(LocalizableString.fromString(Translations.tr("gui.visibility.public")), "public"));
            visibilityOptions.add(new DropdownEntryInfo(LocalizableString.fromString(Translations.tr("gui.visibility.private")), "private"));
            uiCommandBuilder.set("#VisibilityDropdown.Entries", visibilityOptions);
            uiCommandBuilder.set("#VisibilityDropdown.Value", this.isPrivate ? "private" : "public");
            uiCommandBuilder.set("#VisibilityDropdown.Disabled", visibilityDisabled);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged, "#VisibilityDropdown", EventData.of("@VisibilitySetting", "#VisibilityDropdown.Value"), false
            );
         }

         boolean restrictedAllowedByConfig = VaryonExtendedTeleportersPlugin.CONFIG == null || ((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowRestrictedTeleporters();
         boolean hasPartyOnlyPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.restricted");
         boolean hasTrueRestrictedPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.truerestricted");
         boolean showPartyOnlyOption = restrictedAllowedByConfig && hasPartyOnlyPerm && this.canModifySettings();
         boolean showRestrictedOption = restrictedAllowedByConfig && hasTrueRestrictedPerm && this.canModifySettings();
         hasAccessGroup = showPartyOnlyOption || showRestrictedOption;
         uiCommandBuilder.set("#PartyOnlySetting.Visible", showPartyOnlyOption);
         if (showPartyOnlyOption) {
            boolean accessDisabled = this.isSelfDestruct;
            uiCommandBuilder.set("#PartyOnlySetting #CheckBox.Value", this.isPartyOnly);
            uiCommandBuilder.set("#PartyOnlySetting #CheckBox.Disabled", accessDisabled);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#PartyOnlySetting #CheckBox",
               EventData.of("@PartyOnlySetting", "#PartyOnlySetting #CheckBox.Value"),
               false
            );
         }

         uiCommandBuilder.set("#RestrictedSetting.Visible", showRestrictedOption);
         if (showRestrictedOption) {
            boolean accessDisabled = this.isSelfDestruct;
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
         boolean interactionLockAllowedByConfig = VaryonExtendedTeleportersPlugin.CONFIG == null || ((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowInteractionLockedTeleporters();
         boolean hasInteractionLockPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.lock_interaction");
         boolean showInteractionLockOption = interactionLockAllowedByConfig && hasInteractionLockPerm && this.canModifySettings();
         boolean breakLockAllowedByConfig = VaryonExtendedTeleportersPlugin.CONFIG == null || ((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowBreakLockedTeleporters();
         boolean hasBreakLockPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.lock_break");
         boolean showBreakLockOption = breakLockAllowedByConfig && hasBreakLockPerm && this.canModifySettings();
         hasProtectionGroup = showInteractionLockOption || showBreakLockOption;
         uiCommandBuilder.set("#InteractionLockedSetting.Visible", showInteractionLockOption);
         if (showInteractionLockOption) {
            boolean interactionDisabled = this.isSelfDestruct;
            uiCommandBuilder.set("#InteractionLockedSetting #CheckBox.Value", this.isInteractionLocked);
            uiCommandBuilder.set("#InteractionLockedSetting #CheckBox.Disabled", interactionDisabled);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#InteractionLockedSetting #CheckBox",
               EventData.of("@InteractionLockedSetting", "#InteractionLockedSetting #CheckBox.Value"),
               false
            );
         }

         uiCommandBuilder.set("#BreakLockedSetting.Visible", showBreakLockOption);
         if (showBreakLockOption) {
            boolean breakDisabled = this.isSelfDestruct;
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
         boolean hasHideWorldPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_world");
         boolean showDisplayWorldOption = hasHideWorldPerm && this.canModifySettings();
         boolean hasHideCoordsPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_coords");
         boolean showDisplayCoordsOption = hasHideCoordsPerm && this.canModifySettings();
         boolean hasHideMapWaypointPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.hide_map_waypoint");
         boolean showHideMapWaypointOption = hasHideMapWaypointPerm && this.canModifySettings();
         hasDisplayGroup = showDisplayWorldOption || showDisplayCoordsOption || showHideMapWaypointOption;
         uiCommandBuilder.set("#DisplayWorldSetting.Visible", showDisplayWorldOption);
         if (showDisplayWorldOption) {
            uiCommandBuilder.set("#DisplayWorldSetting #CheckBox.Value", this.displayWorld);
            uiCommandBuilder.set("#DisplayWorldSetting #CheckBox.Disabled", false);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#DisplayWorldSetting #CheckBox",
               EventData.of("@DisplayWorldSetting", "#DisplayWorldSetting #CheckBox.Value"),
               false
            );
         }

         uiCommandBuilder.set("#DisplayCoordinatesSetting.Visible", showDisplayCoordsOption);
         if (showDisplayCoordsOption) {
            boolean displayCoordsDisabled = !this.displayWorld;
            uiCommandBuilder.set("#DisplayCoordinatesSetting #CheckBox.Value", this.displayCoordinates);
            uiCommandBuilder.set("#DisplayCoordinatesSetting #CheckBox.Disabled", displayCoordsDisabled);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#DisplayCoordinatesSetting #CheckBox",
               EventData.of("@DisplayCoordinatesSetting", "#DisplayCoordinatesSetting #CheckBox.Value"),
               false
            );
         }

         uiCommandBuilder.set("#HideMapWaypointSetting.Visible", showHideMapWaypointOption);
         if (showHideMapWaypointOption) {
            uiCommandBuilder.set("#HideMapWaypointSetting #CheckBox.Value", this.hideMapWaypoint);
            uiCommandBuilder.set("#HideMapWaypointSetting #CheckBox.Disabled", false);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#HideMapWaypointSetting #CheckBox",
               EventData.of("@HideMapWaypointSetting", "#HideMapWaypointSetting #CheckBox.Value"),
               false
            );
         }

         uiCommandBuilder.set("#DisplaySettingsGroup.Visible", hasDisplayGroup);
         boolean selfDestructAllowedByConfig = VaryonExtendedTeleportersPlugin.CONFIG == null || ((ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get()).isAllowSelfDestructTeleporters();
         boolean hasSelfDestructPerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.self_destruct");
         boolean showSelfDestructOption = selfDestructAllowedByConfig && hasSelfDestructPerm && this.canModifySettings();
         boolean hasSingleUsePerm = permProvider.hasPermission(this.playerUuid, "extendedteleporters.feature.single_use");
         boolean showSingleUseOption = hasSingleUsePerm && this.canModifySettings();
         hasSpecialGroup = showSelfDestructOption || showSingleUseOption;
         uiCommandBuilder.set("#SelfDestructSetting.Visible", showSelfDestructOption);
         if (showSelfDestructOption) {
            boolean selfDestructDisabled = this.wasSelfDestructAlreadyActive;
            uiCommandBuilder.set("#SelfDestructSetting #CheckBox.Value", this.isSelfDestruct);
            uiCommandBuilder.set("#SelfDestructSetting #CheckBox.Disabled", selfDestructDisabled);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#SelfDestructSetting #CheckBox",
               EventData.of("@SelfDestructSetting", "#SelfDestructSetting #CheckBox.Value"),
               false
            );
         }

         if (this.wasSelfDestructAlreadyActive && this.teleporterInfo != null && showSelfDestructOption) {
            long remainingMs = this.teleporterInfo.getSelfDestructRemainingMs();
            if (remainingMs > 0L) {
               long remainingSeconds = remainingMs / 1000L;
               long minutes = remainingSeconds / 60L;
               long seconds = remainingSeconds % 60L;
               uiCommandBuilder.set("#SelfDestructTimer.Text", Translations.tr("gui.selfDestructTimer", "minutes", minutes, "seconds", seconds));
               uiCommandBuilder.set("#SelfDestructTimer.Visible", true);
            } else {
               uiCommandBuilder.set("#SelfDestructTimer.Text", Translations.tr("gui.selfDestructImminent"));
               uiCommandBuilder.set("#SelfDestructTimer.Visible", true);
            }
         } else {
            uiCommandBuilder.set("#SelfDestructTimer.Visible", false);
         }

         uiCommandBuilder.set("#SingleUseSetting.Visible", showSingleUseOption);
         if (showSingleUseOption) {
            uiCommandBuilder.set("#SingleUseSetting #CheckBox.Value", this.isSingleUse);
            uiCommandBuilder.set("#SingleUseSetting #CheckBox.Disabled", false);
            uiEventBuilder.addEventBinding(
               CustomUIEventBindingType.ValueChanged,
               "#SingleUseSetting #CheckBox",
               EventData.of("@SingleUseSetting", "#SingleUseSetting #CheckBox.Value"),
               false
            );
         }

         uiCommandBuilder.set("#SpecialModesGroup.Visible", hasSpecialGroup);
         boolean hasAnySettings = hasPublicDestGroup || hasVisibilityGroup || hasAccessGroup || hasProtectionGroup || hasDisplayGroup || hasSpecialGroup;
         uiCommandBuilder.set("#SettingsSection.Visible", hasAnySettings);
         uiCommandBuilder.set(
            "#LocationLabel.Text", Translations.tr("gui.location", "dimension", this.dimension, "x", this.blockX, "y", this.blockY, "z", this.blockZ)
         );
         if (this.teleporterInfo != null && this.teleporterInfo.hasOwner()) {
            uiCommandBuilder.set("#OwnerLabel.Text", this.isOwner ? Translations.tr("gui.ownerYou") : Translations.tr("gui.ownerOther"));
         } else {
            uiCommandBuilder.set("#OwnerLabel.Text", Translations.tr("gui.ownerUntracked"));
         }

         uiCommandBuilder.set("#ManageTrustButton.Visible", false);
         uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveChangesButton", EventData.of("Save", "true"), false);
         uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Cancel", "true"), false);
      }
   }

   public static final class PageEventData {
      public static final BuilderCodec<TeleporterBlockSettingsGui.PageEventData> CODEC;

      static {
         Builder<TeleporterBlockSettingsGui.PageEventData> b = BuilderCodec.<TeleporterBlockSettingsGui.PageEventData>builder(
               TeleporterBlockSettingsGui.PageEventData.class,
               TeleporterBlockSettingsGui.PageEventData::new
            );
         b = b.addField(new KeyedCodec("@WarpName", Codec.STRING), (TeleporterBlockSettingsGui.PageEventData d, String val) -> d.warpName = val, (TeleporterBlockSettingsGui.PageEventData d) -> d.warpName);
         b = b.addField(
            new KeyedCodec("@WarpDestination", Codec.STRING),
            (TeleporterBlockSettingsGui.PageEventData d, String val) -> d.warpDestination = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.warpDestination
         );
         b = b.addField(
            new KeyedCodec("@ShowOnlyMyWarpsFilter", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.showOnlyMyWarpsFilter = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.showOnlyMyWarpsFilter
         );
         b = b.addField(
            new KeyedCodec("@VisibilitySetting", Codec.STRING),
            (TeleporterBlockSettingsGui.PageEventData d, String val) -> d.visibilitySetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.visibilitySetting
         );
         b = b.addField(
            new KeyedCodec("@PartyOnlySetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.partyOnlySetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.partyOnlySetting
         );
         b = b.addField(
            new KeyedCodec("@RestrictedSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.restrictedSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.restrictedSetting
         );
         b = b.addField(
            new KeyedCodec("@InteractionLockedSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.interactionLockedSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.interactionLockedSetting
         );
         b = b.addField(
            new KeyedCodec("@BreakLockedSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.breakLockedSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.breakLockedSetting
         );
         b = b.addField(
            new KeyedCodec("@DisplayWorldSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.displayWorldSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.displayWorldSetting
         );
         b = b.addField(
            new KeyedCodec("@DisplayCoordinatesSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.displayCoordinatesSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.displayCoordinatesSetting
         );
         b = b.addField(
            new KeyedCodec("@SelfDestructSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.selfDestructSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.selfDestructSetting
         );
         b = b.addField(
            new KeyedCodec("@SingleUseSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.singleUseSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.singleUseSetting
         );
         b = b.addField(
            new KeyedCodec("@HideMapWaypointSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.hideMapWaypointSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.hideMapWaypointSetting
         );
         b = b.addField(
            new KeyedCodec("@AllowPublicDestinationChangeSetting", Codec.BOOLEAN),
            (TeleporterBlockSettingsGui.PageEventData d, Boolean val) -> d.allowPublicDestinationChangeSetting = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.allowPublicDestinationChangeSetting
         );
         b = b.addField(
            new KeyedCodec("ManageTrust", Codec.STRING),
            (TeleporterBlockSettingsGui.PageEventData d, String val) -> d.manageTrust = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.manageTrust
         );
         b = b.addField(
            new KeyedCodec("Save", Codec.STRING),
            (TeleporterBlockSettingsGui.PageEventData d, String val) -> d.save = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.save
         );
         b = b.addField(
            new KeyedCodec("Cancel", Codec.STRING),
            (TeleporterBlockSettingsGui.PageEventData d, String val) -> d.cancel = val,
            (TeleporterBlockSettingsGui.PageEventData d) -> d.cancel
         );
         CODEC = b.build();
      }
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
