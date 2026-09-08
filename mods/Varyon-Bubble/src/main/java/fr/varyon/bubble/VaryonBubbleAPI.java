package fr.varyon.bubble;

import java.util.UUID;
import javax.annotation.Nonnull;

public class VaryonBubbleAPI {
   private static BubbleManager manager;

   static void init(BubbleManager mgr) {
      manager = mgr;
   }

   public static void clearBubble(@Nonnull UUID playerUuid) {
      if (manager != null) {
         manager.clearBubbleExternal(playerUuid);
      }
   }

   public static boolean hasActiveBubble(@Nonnull UUID playerUuid) {
      return manager != null && manager.hasActiveBubble(playerUuid);
   }

   public static boolean isReady() {
      return manager != null;
   }
}
