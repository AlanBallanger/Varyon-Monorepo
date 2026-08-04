package fr.varyon.extendedteleporters.system;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import javax.annotation.Nonnull;

public final class TeleporterComponentRemovalSystem extends RefSystem<ChunkStore> {
   public void onEntityAdded(
      @Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer
   ) {
   }

   public void onEntityRemove(
      @Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer
   ) {
      if (reason == RemoveReason.REMOVE) {
         Teleporter teleporter = (Teleporter)commandBuffer.getComponent(ref, Teleporter.getComponentType());
         if (teleporter != null) {
            BlockStateInfo blockStateInfo = (BlockStateInfo)commandBuffer.getComponent(ref, BlockStateInfo.getComponentType());
            if (blockStateInfo != null) {
               Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
               if (chunkRef != null && chunkRef.isValid()) {
                  WorldChunk worldChunk = (WorldChunk)chunkRef.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
                  if (worldChunk != null) {
                     int blockIndex = blockStateInfo.getIndex();
                     int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
                     int blockY = ChunkUtil.yFromBlockInColumn(blockIndex);
                     int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);
                     long chunkIndex = worldChunk.getIndex();
                     int chunkX = ChunkUtil.xOfChunkIndex(chunkIndex);
                     int chunkZ = ChunkUtil.zOfChunkIndex(chunkIndex);
                     int blockX = (chunkX << 5) + localX;
                     int blockZ = (chunkZ << 5) + localZ;
                     String worldName = worldChunk.getWorld().getName();
                     TeleporterManager.getInstance().onTeleporterRemoved(worldName, blockX, blockY, blockZ);
                  }
               }
            }
         }
      }
   }

   public Query<ChunkStore> getQuery() {
      return Teleporter.getComponentType();
   }
}
