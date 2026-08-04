package fr.varyon.extendedteleporters.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public final class TeleporterSelfDestructTickingSystem extends EntityTickingSystem<EntityStore> {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-SelfDestruct");
   private static final int CHECK_INTERVAL_TICKS = 20;
   private int tickCounter = 0;

   public void tick(
      float deltaTime,
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer
   ) {
      this.tickCounter++;
      if (this.tickCounter >= 20) {
         this.tickCounter = 0;
         Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
         if (index == 0) {
            Player player = (Player)store.getComponent(ref, Player.getComponentType());
            if (player != null) {
               TeleporterManager manager = TeleporterManager.getInstance();
               List<TeleporterInfo> expiredTeleporters = new ArrayList<>();

               for (TeleporterInfo info : manager.getAllTeleporters()) {
                  if (info.isSelfDestruct() && info.isSelfDestructExpired()) {
                     expiredTeleporters.add(info);
                  }
               }

               for (TeleporterInfo info : expiredTeleporters) {
                  logger.at(Level.INFO).log("Processing expired self-destruct teleporter: " + info.locationKey());
                  this.destroySelfDestructTeleporter(manager, info);
               }
            }
         }
      }
   }

   private void destroySelfDestructTeleporter(TeleporterManager manager, TeleporterInfo info) {
      World world = manager.getWorld(info.dimension());
      if (world == null) {
         logger.at(Level.WARNING).log("Self-destruct teleporter in unloaded world, removing from tracking: " + info.locationKey());
         manager.onTeleporterRemoved(info.dimension(), info.blockX(), info.blockY(), info.blockZ());
      } else {
         int blockX = info.blockX();
         int blockY = info.blockY();
         int blockZ = info.blockZ();
         String dimension = info.dimension();
         String locationKey = info.locationKey();
         String warpName = info.warpName();
         if (warpName != null && !warpName.isEmpty()) {
            manager.removeWarpFromRegistry(warpName);
         }

         world.execute(() -> {
            boolean blockDestroyed = false;

            try {
               ChunkStore chunkStore = world.getChunkStore();
               if (chunkStore == null) {
                  logger.at(Level.WARNING).log("Self-destruct: ChunkStore null at " + locationKey);
                  manager.onTeleporterRemoved(dimension, blockX, blockY, blockZ);
                  return;
               }

               long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
               WorldChunk worldChunk = (WorldChunk)chunkStore.getChunkComponent(chunkIndex, WorldChunk.getComponentType());
               if (worldChunk == null) {
                  logger.at(Level.WARNING).log("Self-destruct: Chunk not loaded at " + locationKey);
                  manager.onTeleporterRemoved(dimension, blockX, blockY, blockZ);
                  return;
               }

               worldChunk.setBlock(blockX, blockY, blockZ, 0);
               logger.at(Level.INFO).log("Self-destruct: Set block to air (ID 0) at " + locationKey);
               blockDestroyed = true;
            } catch (Exception e) {
               logger.at(Level.WARNING).log("Self-destruct: setBlock failed at " + locationKey + ": " + e.getMessage());
            }

            if (!blockDestroyed) {
               logger.at(Level.SEVERE).log("Self-destruct: Could not remove block at " + locationKey + " - manual removal may be required");
            }

            manager.onTeleporterRemoved(dimension, blockX, blockY, blockZ);
         });
      }
   }

   @NullableDecl
   public Query<EntityStore> getQuery() {
      return PlayerRef.getComponentType();
   }
}
