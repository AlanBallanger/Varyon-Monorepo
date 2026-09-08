package fr.varyon.bubble;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

public class BubbleColorPage extends InteractiveCustomUIPage<BubbleColorPage.PageData> {
   private final BubbleManager manager;
   private final UUID playerUuid;
   private String currentPickerHex;

   public BubbleColorPage(@Nonnull BubbleManager manager, @Nonnull PlayerRef playerRef) {
      super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
      this.manager = manager;
      this.playerUuid = playerRef.getUuid();
      Color color = manager.getColor(this.playerUuid);
      this.currentPickerHex = color != null ? BubbleManager.colorToHex(color) : "#F3E1CA";
   }

   public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
      cmd.append("BubbleColor.ui");
      this.applySettings(cmd, evt);
   }

   public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
      if (data.dropdownId != null && "Color".equals(data.dropdownId) && data.dropdownValue != null) {
         String hex = data.dropdownValue;
         if (!hex.startsWith("#")) {
            hex = "#" + hex;
         }

         if (hex.length() > 7) {
            hex = hex.substring(0, 7);
         }

         this.currentPickerHex = hex;
         UICommandBuilder cmd = new UICommandBuilder();
         cmd.set("#HexField.Value", hex.toUpperCase());
         cmd.set("#PreviewBubble.Background.Color", hex + "FF");
         this.sendUpdate(cmd, new UIEventBuilder(), false);
      } else if (data.action != null) {
         switch (data.action) {
            case "Apply":
               Color color = BubbleManager.parseHexColor(this.currentPickerHex);
               if (color != null) {
                  this.manager.setColor(this.playerUuid, color);
               }
               break;
            case "Reset":
               this.manager.clearColor(this.playerUuid);
               this.currentPickerHex = "#F3E1CA";
               this.refresh();
               break;
            case "Close":
               this.close();
         }
      }
   }

   private void refresh() {
      UICommandBuilder cmd = new UICommandBuilder();
      UIEventBuilder evt = new UIEventBuilder();
      this.applySettings(cmd, evt);
      this.sendUpdate(cmd, evt, false);
   }

   private void applySettings(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
      cmd.set("#ColorPicker.Value", this.currentPickerHex);
      evt.addEventBinding(
         CustomUIEventBindingType.ValueChanged, "#ColorPicker", EventData.of("DropdownId", "Color").append("@DropdownValue", "#ColorPicker.Value"), false
      );
      cmd.set("#HexField.Value", this.currentPickerHex.toUpperCase());
      cmd.set("#PreviewBubble.Background.Color", this.currentPickerHex + "FF");
      evt.addEventBinding(CustomUIEventBindingType.Activating, "#ApplyButton", new EventData().append("Action", "Apply"), false);
      evt.addEventBinding(CustomUIEventBindingType.Activating, "#ResetButton", new EventData().append("Action", "Reset"), false);
      evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", new EventData().append("Action", "Close"), false);
   }

   public static class PageData {
      @Nonnull
      public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
         .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
         .addField(new KeyedCodec<>("DropdownId", Codec.STRING), (d, v) -> d.dropdownId = v, d -> d.dropdownId)
         .addField(new KeyedCodec<>("@DropdownValue", Codec.STRING), (d, v) -> d.dropdownValue = v, d -> d.dropdownValue)
         .build();
      String action;
      String dropdownId;
      String dropdownValue;
   }
}
