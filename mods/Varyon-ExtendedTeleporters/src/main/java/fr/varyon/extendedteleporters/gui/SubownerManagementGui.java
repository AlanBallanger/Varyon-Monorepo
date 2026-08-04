package fr.varyon.extendedteleporters.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.SubownerPermissions;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import fr.varyon.extendedteleporters.i18n.Translations;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SubownerManagementGui extends InteractiveCustomUIPage<SubownerManagementGui.SubownerEventData> {
   private static final int MAX_DISPLAYED_SUBOWNERS = 10;
   private final UUID playerUuid;
   private final TeleporterInfo teleporterInfo;
   private final boolean isBypassing;
   private final Map<UUID, SubownerPermissions> editedSubowners = new ConcurrentHashMap<>();
   private List<UUID> subownerOrder = new ArrayList<>();
   @Nullable
   private String selectedPlayerUuid = null;

   public SubownerManagementGui(@NonNullDecl PlayerRef playerRef, @NonNullDecl TeleporterInfo teleporterInfo) {
      super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SubownerManagementGui.SubownerEventData.CODEC);
      this.playerUuid = playerRef.getUuid();
      this.teleporterInfo = teleporterInfo;
      this.isBypassing = TeleporterManager.getInstance().isInBypassMode(this.playerUuid);

      for (SubownerPermissions perms : teleporterInfo.getAllSubownerPermissions()) {
         this.editedSubowners
            .put(
               perms.playerUuid(),
               new SubownerPermissions(perms.playerUuid(), perms.canRename(), perms.canSetDestination(), perms.canModifySettings(), perms.canBreak())
            );
         this.subownerOrder.add(perms.playerUuid());
      }
   }

   public void handleDataEvent(
      @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl SubownerManagementGui.SubownerEventData data
   ) {
      super.handleDataEvent(ref, store, data);
      boolean needsRebuild = false;
      if (data.selectedPlayer != null && !data.selectedPlayer.isEmpty()) {
         this.selectedPlayerUuid = data.selectedPlayer;
      }

      if ("true".equals(data.addPlayer) && this.selectedPlayerUuid != null) {
         this.handleAddPlayer();
         needsRebuild = true;
      }

      if ("true".equals(data.remove0)) {
         this.handleRemovePlayer(0);
         needsRebuild = true;
      }

      if ("true".equals(data.remove1)) {
         this.handleRemovePlayer(1);
         needsRebuild = true;
      }

      if ("true".equals(data.remove2)) {
         this.handleRemovePlayer(2);
         needsRebuild = true;
      }

      if ("true".equals(data.remove3)) {
         this.handleRemovePlayer(3);
         needsRebuild = true;
      }

      if ("true".equals(data.remove4)) {
         this.handleRemovePlayer(4);
         needsRebuild = true;
      }

      if ("true".equals(data.remove5)) {
         this.handleRemovePlayer(5);
         needsRebuild = true;
      }

      if ("true".equals(data.remove6)) {
         this.handleRemovePlayer(6);
         needsRebuild = true;
      }

      if ("true".equals(data.remove7)) {
         this.handleRemovePlayer(7);
         needsRebuild = true;
      }

      if ("true".equals(data.remove8)) {
         this.handleRemovePlayer(8);
         needsRebuild = true;
      }

      if ("true".equals(data.remove9)) {
         this.handleRemovePlayer(9);
         needsRebuild = true;
      }

      this.handlePermissionChanges(data);
      if (data.save != null) {
         this.saveChanges();
         this.close();
      } else if (data.cancel != null) {
         this.close();
      } else {
         if (needsRebuild) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.build(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, true);
         } else {
            this.sendUpdate();
         }
      }
   }

   private void handlePermissionChanges(SubownerManagementGui.SubownerEventData data) {
      this.processPermChange(0, data.perm0Rename, data.perm0Dest, data.perm0Set, data.perm0Brk);
      this.processPermChange(1, data.perm1Rename, data.perm1Dest, data.perm1Set, data.perm1Brk);
      this.processPermChange(2, data.perm2Rename, data.perm2Dest, data.perm2Set, data.perm2Brk);
      this.processPermChange(3, data.perm3Rename, data.perm3Dest, data.perm3Set, data.perm3Brk);
      this.processPermChange(4, data.perm4Rename, data.perm4Dest, data.perm4Set, data.perm4Brk);
      this.processPermChange(5, data.perm5Rename, data.perm5Dest, data.perm5Set, data.perm5Brk);
      this.processPermChange(6, data.perm6Rename, data.perm6Dest, data.perm6Set, data.perm6Brk);
      this.processPermChange(7, data.perm7Rename, data.perm7Dest, data.perm7Set, data.perm7Brk);
      this.processPermChange(8, data.perm8Rename, data.perm8Dest, data.perm8Set, data.perm8Brk);
      this.processPermChange(9, data.perm9Rename, data.perm9Dest, data.perm9Set, data.perm9Brk);
   }

   private void processPermChange(int index, Boolean rename, Boolean dest, Boolean settings, Boolean brk) {
      if (index < this.subownerOrder.size()) {
         UUID uuid = this.subownerOrder.get(index);
         SubownerPermissions perms = this.editedSubowners.get(uuid);
         if (perms != null) {
            if (rename != null) {
               perms.setCanRename(rename);
            }

            if (dest != null) {
               perms.setCanSetDestination(dest);
            }

            if (settings != null) {
               perms.setCanModifySettings(settings);
            }

            if (brk != null) {
               perms.setCanBreak(brk);
            }
         }
      }
   }

   private void handleAddPlayer() {
      if (this.selectedPlayerUuid != null && !this.selectedPlayerUuid.isEmpty()) {
         try {
            UUID targetUuid = UUID.fromString(this.selectedPlayerUuid);
            if (targetUuid.equals(this.teleporterInfo.ownerUuid())) {
               this.playerRef.sendMessage(Translations.msgError("subowner.add.cannotAddOwner"));
               return;
            }

            if (this.editedSubowners.containsKey(targetUuid)) {
               this.playerRef.sendMessage(Translations.msgWarning("subowner.add.alreadySubowner"));
               return;
            }

            String playerName = "Unknown";

            for (PlayerRef p : Universe.get().getPlayers()) {
               if (p.getUuid().equals(targetUuid)) {
                  playerName = p.getUsername();
                  break;
               }
            }

            SubownerPermissions newPerms = new SubownerPermissions(targetUuid, false, false, false, false);
            this.editedSubowners.put(targetUuid, newPerms);
            this.subownerOrder.add(targetUuid);
            this.playerRef.sendMessage(Translations.msgSuccess("subowner.add.success", "name", playerName));
            this.selectedPlayerUuid = null;
         } catch (IllegalArgumentException e) {
            this.playerRef.sendMessage(Translations.msgError("subowner.add.invalidPlayer"));
         }
      }
   }

   private void handleRemovePlayer(int index) {
      if (index >= 0 && index < this.subownerOrder.size()) {
         UUID uuid = this.subownerOrder.remove(index);
         this.editedSubowners.remove(uuid);
      }
   }

   private void saveChanges() {
      this.teleporterInfo.clearTrustedPlayers();

      for (UUID uuid : this.subownerOrder) {
         SubownerPermissions perms = this.editedSubowners.get(uuid);
         if (perms != null) {
            this.teleporterInfo.addTrustedPlayer(uuid, perms);
         }
      }

      TeleporterManager.getInstance().markDirty();
      this.playerRef.sendMessage(Translations.msgSuccess("subowner.saved"));
   }

   private List<PlayerRef> getAvailableOnlinePlayers() {
      List<PlayerRef> available = new ArrayList<>();
      UUID ownerUuid = this.teleporterInfo.ownerUuid();

      for (PlayerRef p : Universe.get().getPlayers()) {
         UUID pUuid = p.getUuid();
         if (!pUuid.equals(ownerUuid) && !this.editedSubowners.containsKey(pUuid)) {
            available.add(p);
         }
      }

      available.sort((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()));
      return available;
   }

   private String getPlayerName(UUID uuid) {
      for (PlayerRef p : Universe.get().getPlayers()) {
         if (p.getUuid().equals(uuid)) {
            return p.getUsername();
         }
      }

      return Translations.tr("common.player.offline", "uuid", uuid.toString().substring(0, 8));
   }

   public void build(
      @NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt, @NonNullDecl Store<EntityStore> store
   ) {
      cmd.append("Pages/ExtendedTeleport_SubownerManagement.ui");
      String restrictedLabel = Translations.tr("common.restricted");
      cmd.set("#TeleporterName.Text", this.teleporterInfo.displayNameWithRestrictionStatus(restrictedLabel));
      cmd.set(
         "#TeleporterLocation.Text",
         Translations.tr(
            "subowner.location",
            "dimension",
            this.teleporterInfo.dimension(),
            "x",
            this.teleporterInfo.blockX(),
            "y",
            this.teleporterInfo.blockY(),
            "z",
            this.teleporterInfo.blockZ()
         )
      );
      List<PlayerRef> availablePlayers = this.getAvailableOnlinePlayers();
      boolean hasAvailablePlayers = !availablePlayers.isEmpty();
      cmd.set("#OnlinePlayersDropdown.Visible", hasAvailablePlayers);
      cmd.set("#AddButton.Visible", hasAvailablePlayers);
      cmd.set("#NoPlayersLabel.Visible", !hasAvailablePlayers);
      if (hasAvailablePlayers) {
         List<DropdownEntryInfo> entries = new ArrayList<>();

         for (PlayerRef p : availablePlayers) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(p.getUsername()), p.getUuid().toString()));
         }

         cmd.set("#OnlinePlayersDropdown.Entries", entries);
         if (this.selectedPlayerUuid != null) {
            cmd.set("#OnlinePlayersDropdown.Value", this.selectedPlayerUuid);
         }

         evt.addEventBinding(
            CustomUIEventBindingType.ValueChanged, "#OnlinePlayersDropdown", EventData.of("@SelectedPlayer", "#OnlinePlayersDropdown.Value"), false
         );
         evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton", EventData.of("AddPlayer", "true"), false);
      }

      boolean hasSubowners = !this.subownerOrder.isEmpty();
      cmd.set("#EmptyListLabel.Visible", !hasSubowners);
      int displayCount = Math.min(this.subownerOrder.size(), 10);

      for (int i = 0; i < 10; i++) {
         String entryId = "#Entry" + i;
         if (i < displayCount) {
            UUID uuid = this.subownerOrder.get(i);
            SubownerPermissions perms = this.editedSubowners.get(uuid);
            if (perms == null) {
               cmd.set(entryId + ".Visible", false);
            } else {
               cmd.set(entryId + ".Visible", true);
               cmd.set("#Name" + i + ".Text", this.getPlayerName(uuid));
               cmd.set("#Perm" + i + "Rename #CheckBox.Value", perms.canRename());
               cmd.set("#Perm" + i + "Dest #CheckBox.Value", perms.canSetDestination());
               cmd.set("#Perm" + i + "Set #CheckBox.Value", perms.canModifySettings());
               cmd.set("#Perm" + i + "Brk #CheckBox.Value", perms.canBreak());
               evt.addEventBinding(CustomUIEventBindingType.Activating, "#Remove" + i, EventData.of("Remove" + i, "true"), false);
               evt.addEventBinding(
                  CustomUIEventBindingType.ValueChanged,
                  "#Perm" + i + "Rename #CheckBox",
                  EventData.of("@Perm" + i + "Rename", "#Perm" + i + "Rename #CheckBox.Value"),
                  false
               );
               evt.addEventBinding(
                  CustomUIEventBindingType.ValueChanged,
                  "#Perm" + i + "Dest #CheckBox",
                  EventData.of("@Perm" + i + "Dest", "#Perm" + i + "Dest #CheckBox.Value"),
                  false
               );
               evt.addEventBinding(
                  CustomUIEventBindingType.ValueChanged,
                  "#Perm" + i + "Set #CheckBox",
                  EventData.of("@Perm" + i + "Set", "#Perm" + i + "Set #CheckBox.Value"),
                  false
               );
               evt.addEventBinding(
                  CustomUIEventBindingType.ValueChanged,
                  "#Perm" + i + "Brk #CheckBox",
                  EventData.of("@Perm" + i + "Brk", "#Perm" + i + "Brk #CheckBox.Value"),
                  false
               );
            }
         } else {
            cmd.set(entryId + ".Visible", false);
         }
      }

      evt.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Save", "true"), false);
      evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Cancel", "true"), false);
   }

    public static final class SubownerEventData {
       public static final BuilderCodec<SubownerManagementGui.SubownerEventData> CODEC;

       static {
          Builder<SubownerManagementGui.SubownerEventData> b = BuilderCodec.<SubownerManagementGui.SubownerEventData>builder(
                SubownerManagementGui.SubownerEventData.class,
                SubownerManagementGui.SubownerEventData::new
             );
          b = b.addField(
             new KeyedCodec("@SelectedPlayer", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.selectedPlayer = val,
             (SubownerManagementGui.SubownerEventData d) -> d.selectedPlayer
          );
          b = b.addField(
             new KeyedCodec("AddPlayer", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.addPlayer = val,
             (SubownerManagementGui.SubownerEventData d) -> d.addPlayer
          );
          b = b.addField(
             new KeyedCodec("Remove0", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove0 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove0
          );
          b = b.addField(
             new KeyedCodec("Remove1", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove1 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove1
          );
          b = b.addField(
             new KeyedCodec("Remove2", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove2 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove2
          );
          b = b.addField(
             new KeyedCodec("Remove3", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove3 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove3
          );
          b = b.addField(
             new KeyedCodec("Remove4", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove4 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove4
          );
          b = b.addField(
             new KeyedCodec("Remove5", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove5 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove5
          );
          b = b.addField(
             new KeyedCodec("Remove6", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove6 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove6
          );
          b = b.addField(
             new KeyedCodec("Remove7", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove7 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove7
          );
          b = b.addField(
             new KeyedCodec("Remove8", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove8 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove8
          );
          b = b.addField(
             new KeyedCodec("Remove9", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.remove9 = val,
             (SubownerManagementGui.SubownerEventData d) -> d.remove9
          );
          b = b.addField(
             new KeyedCodec("@Perm0Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm0Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm0Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm0Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm0Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm0Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm0Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm0Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm0Set
          );
          b = b.addField(
             new KeyedCodec("@Perm0Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm0Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm0Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm1Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm1Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm1Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm1Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm1Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm1Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm1Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm1Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm1Set
          );
          b = b.addField(
             new KeyedCodec("@Perm1Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm1Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm1Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm2Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm2Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm2Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm2Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm2Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm2Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm2Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm2Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm2Set
          );
          b = b.addField(
             new KeyedCodec("@Perm2Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm2Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm2Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm3Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm3Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm3Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm3Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm3Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm3Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm3Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm3Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm3Set
          );
          b = b.addField(
             new KeyedCodec("@Perm3Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm3Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm3Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm4Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm4Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm4Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm4Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm4Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm4Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm4Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm4Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm4Set
          );
          b = b.addField(
             new KeyedCodec("@Perm4Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm4Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm4Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm5Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm5Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm5Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm5Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm5Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm5Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm5Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm5Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm5Set
          );
          b = b.addField(
             new KeyedCodec("@Perm5Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm5Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm5Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm6Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm6Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm6Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm6Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm6Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm6Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm6Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm6Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm6Set
          );
          b = b.addField(
             new KeyedCodec("@Perm6Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm6Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm6Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm7Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm7Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm7Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm7Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm7Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm7Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm7Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm7Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm7Set
          );
          b = b.addField(
             new KeyedCodec("@Perm7Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm7Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm7Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm8Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm8Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm8Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm8Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm8Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm8Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm8Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm8Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm8Set
          );
          b = b.addField(
             new KeyedCodec("@Perm8Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm8Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm8Brk
          );
          b = b.addField(
             new KeyedCodec("@Perm9Rename", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm9Rename = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm9Rename
          );
          b = b.addField(
             new KeyedCodec("@Perm9Dest", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm9Dest = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm9Dest
          );
          b = b.addField(
             new KeyedCodec("@Perm9Set", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm9Set = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm9Set
          );
          b = b.addField(
             new KeyedCodec("@Perm9Brk", Codec.BOOLEAN),
             (SubownerManagementGui.SubownerEventData d, Boolean val) -> d.perm9Brk = val,
             (SubownerManagementGui.SubownerEventData d) -> d.perm9Brk
          );
          b = b.addField(
             new KeyedCodec("Save", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.save = val,
             (SubownerManagementGui.SubownerEventData d) -> d.save
          );
          b = b.addField(
             new KeyedCodec("Cancel", Codec.STRING),
             (SubownerManagementGui.SubownerEventData d, String val) -> d.cancel = val,
             (SubownerManagementGui.SubownerEventData d) -> d.cancel
          );
          CODEC = b.build();
       }
       public String selectedPlayer;
       public String addPlayer;
       public String remove0;
       public String remove1;
       public String remove2;
       public String remove3;
       public String remove4;
       public String remove5;
       public String remove6;
       public String remove7;
       public String remove8;
       public String remove9;
       public Boolean perm0Rename;
       public Boolean perm0Dest;
       public Boolean perm0Set;
       public Boolean perm0Brk;
       public Boolean perm1Rename;
       public Boolean perm1Dest;
       public Boolean perm1Set;
       public Boolean perm1Brk;
       public Boolean perm2Rename;
       public Boolean perm2Dest;
       public Boolean perm2Set;
       public Boolean perm2Brk;
       public Boolean perm3Rename;
       public Boolean perm3Dest;
       public Boolean perm3Set;
       public Boolean perm3Brk;
       public Boolean perm4Rename;
       public Boolean perm4Dest;
       public Boolean perm4Set;
       public Boolean perm4Brk;
       public Boolean perm5Rename;
       public Boolean perm5Dest;
       public Boolean perm5Set;
       public Boolean perm5Brk;
       public Boolean perm6Rename;
       public Boolean perm6Dest;
       public Boolean perm6Set;
       public Boolean perm6Brk;
       public Boolean perm7Rename;
       public Boolean perm7Dest;
       public Boolean perm7Set;
       public Boolean perm7Brk;
       public Boolean perm8Rename;
       public Boolean perm8Dest;
       public Boolean perm8Set;
       public Boolean perm8Brk;
       public Boolean perm9Rename;
       public Boolean perm9Dest;
       public Boolean perm9Set;
       public Boolean perm9Brk;
       public String save;
       public String cancel;
    }
}
