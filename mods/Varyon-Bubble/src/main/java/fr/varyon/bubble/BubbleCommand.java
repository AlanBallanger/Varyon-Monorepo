package fr.varyon.bubble;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

public class BubbleCommand extends AbstractPlayerCommand {
   private final BubbleManager manager;

   public BubbleCommand(BubbleManager manager) {
      super("bbub", "Toggle busy bubbles above your head");
      this.setPermissionGroup(GameMode.Adventure);
      this.setAllowsExtraArguments(true);
      this.manager = manager;
   }

   @Override
   protected void execute(
      @Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world
   ) {
      String raw = context.getInputString();
      String args = raw == null ? "" : raw.trim();
      String name = "bbub";
      if (args.toLowerCase().startsWith(name)) {
         args = args.substring(name.length()).trim();
      }

      if (args.equalsIgnoreCase("help") || args.equals("--help")) {
         this.sendHelp(context);
      } else if (args.toLowerCase().startsWith("anim")) {
         this.handleAnim(context, args, playerRef);
      } else if (args.toLowerCase().startsWith("color") || args.toLowerCase().startsWith("colour")) {
         this.handleColor(context, args, playerRef, ref, store);
      } else if (args.toLowerCase().startsWith("range")) {
         this.handleRange(context, args);
      } else if (args.equalsIgnoreCase("debug")) {
         boolean debug = this.manager.toggleDebug(playerRef.getUuid());
         context.sendMessage(
            Message.raw("Varyon-Bubble debug mode " + (debug ? "ON" : "OFF") + " - you " + (debug ? "WILL" : "will NOT") + " see your own bubble")
         );
      } else if (args.equalsIgnoreCase("status")) {
         boolean enabled = this.manager.isEnabled(playerRef.getUuid());
         BubbleState state = this.manager.getState(playerRef.getUuid());
         String stateName = state != null ? state.name() : "none";
         context.sendMessage(
            Message.raw(
               "Varyon-Bubble: "
                  + (enabled ? "enabled" : "disabled")
                  + " | Current state: "
                  + stateName
                  + " | Tracking "
                  + this.manager.getTrackedCount()
                  + " player(s)"
            )
         );
      } else if (args.isEmpty()) {
         boolean enabled = this.manager.togglePlayer(playerRef.getUuid());
         context.sendMessage(Message.raw("Varyon-Bubble " + (enabled ? "enabled" : "disabled") + " for you."));
      } else {
         context.sendMessage(Message.raw("Unknown subcommand: " + args));
         context.sendMessage(Message.raw("Use /bbub help for usage."));
      }
   }

   private void handleRange(@Nonnull CommandContext context, @Nonnull String trimmed) {
      String value = trimmed.length() > 5 ? trimmed.substring(5).trim() : "";
      if (value.isEmpty()) {
         context.sendMessage(Message.raw("Varyon-Bubble view range: " + (int)this.manager.getViewRange() + " blocks"));
         context.sendMessage(Message.raw("Usage: /bbub range <blocks>"));
         return;
      }

      try {
         double blocks = Double.parseDouble(value);
         if (blocks < 1.0) {
            context.sendMessage(Message.raw("Range must be at least 1 block."));
            return;
         }

         this.manager.setViewRange(blocks);
         context.sendMessage(Message.raw("Varyon-Bubble view range set to " + (int)blocks + " blocks"));
      } catch (NumberFormatException e) {
         context.sendMessage(Message.raw("Invalid number: " + value));
      }
   }

   private void handleColor(
      @Nonnull CommandContext context, @Nonnull String trimmed, @Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store
   ) {
      String lower = trimmed.toLowerCase();
      int prefixLen = lower.startsWith("colour") ? 6 : 5;
      String value = trimmed.length() > prefixLen ? trimmed.substring(prefixLen).trim() : "";
      UUID uuid = playerRef.getUuid();

      if (value.isEmpty()) {
         Player player = (Player)store.getComponent(ref, Player.getComponentType());
         if (player != null) {
            BubbleColorPage page = new BubbleColorPage(this.manager, playerRef);
            player.getPageManager().openCustomPage(ref, store, page);
         }
      } else if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("default")) {
         this.manager.clearColor(uuid);
         context.sendMessage(Message.raw("Varyon-Bubble color reset to default (beige)"));
      } else {
         Color color = BubbleManager.parseHexColor(value);
         if (color == null) {
            context.sendMessage(Message.raw("Invalid color: " + value));
            context.sendMessage(Message.raw("Use hex format: #RRGGBB (e.g. #FF5500, #88CCFF)"));
         } else {
            this.manager.setColor(uuid, color);
            context.sendMessage(Message.raw("Varyon-Bubble color set to " + BubbleManager.colorToHex(color)));
         }
      }
   }

   private void handleAnim(@Nonnull CommandContext context, @Nonnull String trimmed, @Nonnull PlayerRef playerRef) {
      String value = trimmed.length() > 4 ? trimmed.substring(4).trim() : "";
      UUID uuid = playerRef.getUuid();
      if (value.equalsIgnoreCase("on")) {
         this.manager.setAnim(uuid, true);
         context.sendMessage(Message.raw("Varyon-Bubble animation ON (animated ellipsis)"));
      } else if (value.equalsIgnoreCase("off")) {
         this.manager.setAnim(uuid, false);
         context.sendMessage(Message.raw("Varyon-Bubble animation OFF (static bubble, quick fade)"));
      } else {
         boolean enabled = this.manager.isAnimEnabled(uuid);
         context.sendMessage(Message.raw("Varyon-Bubble animation is " + (enabled ? "ON" : "OFF")));
         context.sendMessage(Message.raw("Usage: /bbub anim on|off"));
      }
   }

   private void sendHelp(@Nonnull CommandContext context) {
      context.sendMessage(Message.raw("--- Varyon-Bubble Help ---"));
      context.sendMessage(Message.raw("/bbub - Toggle busy bubbles on/off"));
      context.sendMessage(Message.raw("/bbub color - Open color picker GUI"));
      context.sendMessage(Message.raw("/bbub color #RRGGBB - Set custom bubble color"));
      context.sendMessage(Message.raw("/bbub color reset - Reset to default beige"));
      context.sendMessage(Message.raw("/bbub anim on|off - Toggle animated/static bubble (default: on)"));
      context.sendMessage(Message.raw("/bbub status - Show your current state"));
      context.sendMessage(Message.raw("/bbub range <blocks> - Set visibility range (default 40)"));
      context.sendMessage(Message.raw("/bbub debug - Toggle seeing your own bubble"));
      context.sendMessage(Message.raw("/bbub help - Show this help"));
      context.sendMessage(Message.raw(""));
      context.sendMessage(Message.raw("Detects: Inventory (Tab), Containers, Map, Pause,"));
      context.sendMessage(Message.raw("  Custom mod menus, Settings, Content Creation."));
      context.sendMessage(Message.raw("Bubbles clear automatically when you move or"));
      context.sendMessage(Message.raw("  close the menu."));
   }
}
