package fr.varyon.extendedteleporters.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TeleporterSelectForSubownersGui extends InteractiveCustomUIPage<TeleporterSelectForSubownersGui.SelectEventData> {
   private final UUID playerUuid;
   private final boolean isBypassing;
   private final List<TeleporterInfo> playerTeleporters;
   @Nullable
   private String selectedTeleporterKey = null;

   public TeleporterSelectForSubownersGui(@NonNullDecl PlayerRef playerRef) {
      super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, TeleporterSelectForSubownersGui.SelectEventData.CODEC);
      this.playerUuid = playerRef.getUuid();
      this.isBypassing = TeleporterManager.getInstance().isInBypassMode(this.playerUuid);
      TeleporterManager manager = TeleporterManager.getInstance();
      if (this.isBypassing) {
         this.playerTeleporters = new ArrayList<>(manager.getAllTeleporters());
      } else {
         this.playerTeleporters = new ArrayList<>(manager.getPlayerTeleportersSynced(this.playerUuid));
      }

      this.playerTeleporters.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
      if (!this.playerTeleporters.isEmpty()) {
         this.selectedTeleporterKey = this.playerTeleporters.getFirst().locationKey();
      }
   }

   public void handleDataEvent(
      @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl TeleporterSelectForSubownersGui.SelectEventData data
   ) {
      super.handleDataEvent(ref, store, data);
      if (data.selectedTeleporter != null && !data.selectedTeleporter.isEmpty()) {
         this.selectedTeleporterKey = data.selectedTeleporter;
      }

      if ("true".equals(data.manage) && this.selectedTeleporterKey != null) {
         this.openSubownerManagement(ref, store);
      } else if ("true".equals(data.cancel)) {
         this.close();
      } else {
         this.sendUpdate();
      }
   }

   private void openSubownerManagement(Ref<EntityStore> ref, Store<EntityStore> store) {
      TeleporterInfo teleporter = TeleporterManager.getInstance().getTeleporterByKey(this.selectedTeleporterKey);
      if (teleporter == null) {
         this.playerRef.sendMessage(Translations.msgError("subowner.teleporterNotFound"));
      } else if (!teleporter.isOwner(this.playerUuid) && !this.isBypassing) {
         this.playerRef.sendMessage(Translations.msgError("subowner.noPermission"));
      } else {
         Player player = (Player)store.getComponent(ref, Player.getComponentType());
         if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new SubownerManagementGui(this.playerRef, teleporter));
         }
      }
   }

   public void build(
      @NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt, @NonNullDecl Store<EntityStore> store
   ) {
      cmd.append("Pages/ExtendedTeleport_TeleporterSelect.ui");
      boolean hasTeleporters = !this.playerTeleporters.isEmpty();
      cmd.set("#TeleporterDropdown.Visible", hasTeleporters);
      cmd.set("#ManageButton.Visible", hasTeleporters);
      cmd.set("#EmptyLabel.Visible", !hasTeleporters);
      if (hasTeleporters) {
         List<DropdownEntryInfo> entries = new ArrayList<>();
         String restrictedLabel = Translations.tr("common.restricted");

         for (TeleporterInfo info : this.playerTeleporters) {
            String displayText = info.displayNameWithRestrictionStatus(restrictedLabel);
            int subownerCount = info.getTrustedPlayers().size();
            if (subownerCount > 0) {
               displayText = displayText + " (" + subownerCount + " subowners)";
            }

            entries.add(new DropdownEntryInfo(LocalizableString.fromString(displayText), info.locationKey()));
         }

         cmd.set("#TeleporterDropdown.Entries", entries);
         if (this.selectedTeleporterKey != null) {
            cmd.set("#TeleporterDropdown.Value", this.selectedTeleporterKey);
         }

         evt.addEventBinding(
            CustomUIEventBindingType.ValueChanged, "#TeleporterDropdown", EventData.of("@SelectedTeleporter", "#TeleporterDropdown.Value"), false
         );
         evt.addEventBinding(CustomUIEventBindingType.Activating, "#ManageButton", EventData.of("Manage", "true"), false);
      }

      evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Cancel", "true"), false);
   }

   public static final class SelectEventData {
      public static final BuilderCodec<TeleporterSelectForSubownersGui.SelectEventData> CODEC;

      static {
         Builder<TeleporterSelectForSubownersGui.SelectEventData> b = BuilderCodec.<TeleporterSelectForSubownersGui.SelectEventData>builder(
               TeleporterSelectForSubownersGui.SelectEventData.class,
               TeleporterSelectForSubownersGui.SelectEventData::new
            );
         b = b.addField(
            new KeyedCodec("@SelectedTeleporter", Codec.STRING),
            (TeleporterSelectForSubownersGui.SelectEventData d, String s) -> d.selectedTeleporter = s,
            (TeleporterSelectForSubownersGui.SelectEventData d) -> d.selectedTeleporter
         );
         b = b.addField(
            new KeyedCodec("Manage", Codec.STRING),
            (TeleporterSelectForSubownersGui.SelectEventData d, String s) -> d.manage = s,
            (TeleporterSelectForSubownersGui.SelectEventData d) -> d.manage
         );
         b = b.addField(
            new KeyedCodec("Cancel", Codec.STRING),
            (TeleporterSelectForSubownersGui.SelectEventData d, String s) -> d.cancel = s,
            (TeleporterSelectForSubownersGui.SelectEventData d) -> d.cancel
         );
         CODEC = b.build();
      }

      private String selectedTeleporter;
      private String manage;
      private String cancel;
   }
}
