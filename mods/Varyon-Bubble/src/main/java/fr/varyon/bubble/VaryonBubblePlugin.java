package fr.varyon.bubble;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public final class VaryonBubblePlugin extends JavaPlugin {
   private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
   private static final String PLUGIN_NAME = "Varyon-Bubble";
   private static final long POLL_INTERVAL_MS = 100L;

   private BubbleManager bubbleManager;
   private ScheduledExecutorService scheduler;

   public VaryonBubblePlugin(@Nonnull JavaPluginInit init) {
      super(init);
   }

   @Override
   protected void setup() {
      ((Api)LOGGER.atInfo()).log("[%s] initializing...", PLUGIN_NAME);
      this.bubbleManager = new BubbleManager();
      VaryonBubbleAPI.init(this.bubbleManager);
   }

   @Override
   protected void start() {
      this.bubbleManager.registerPacketWatchers();
      this.getCommandRegistry().registerCommand(new BubbleCommand(this.bubbleManager));
      ((Api)LOGGER.atInfo()).log("[%s] Registered /bbub command", PLUGIN_NAME);

      this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread thread = new Thread(r, "Varyon-Bubble-Spawner");
         thread.setDaemon(true);
         return thread;
      });
      this.bubbleManager.setScheduler(this.scheduler);

      this.getEventRegistry()
         .register(
            PlayerConnectEvent.class,
            event -> this.scheduler.schedule(() -> this.bubbleManager.onPlayerConnect(event.getPlayerRef()), 2L, TimeUnit.SECONDS)
         );
      this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> this.bubbleManager.removePlayer(event.getPlayerRef().getUuid()));

      this.scheduler.scheduleAtFixedRate(() -> {
         try {
            this.bubbleManager.pollAndSpawn();
         } catch (Exception e) {
            ((Api)((Api)LOGGER.atSevere()).withCause(e)).log("[%s] Error in bubble poll loop", PLUGIN_NAME);
         }
      }, 1000L, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);

      ((Api)LOGGER.atInfo()).log("[%s] started (poll interval %dms)", PLUGIN_NAME, POLL_INTERVAL_MS);
   }

   @Override
   protected void shutdown() {
      ((Api)LOGGER.atInfo()).log("[%s] shutting down...", PLUGIN_NAME);
      if (this.scheduler != null && !this.scheduler.isShutdown()) {
         this.scheduler.shutdown();

         try {
            if (!this.scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
               this.scheduler.shutdownNow();
            }
         } catch (InterruptedException e) {
            this.scheduler.shutdownNow();
         }
      }

      ((Api)LOGGER.atInfo()).log("[%s] shutdown complete", PLUGIN_NAME);
   }
}
