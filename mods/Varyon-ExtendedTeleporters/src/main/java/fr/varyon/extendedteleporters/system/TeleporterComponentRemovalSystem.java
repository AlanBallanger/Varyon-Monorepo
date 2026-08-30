package fr.varyon.extendedteleporters.system;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import org.joml.Vector3i;
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
               Ref<ChunkStore> sectionRef = blockStateInfo.getSectionRef();
               if (sectionRef != null && sectionRef.isValid()) {
                  Vector3i worldPos = new Vector3i();
                  if (blockStateInfo.fillWorldPos(commandBuffer, worldPos)) {
                     WorldChunk worldChunk = (WorldChunk)commandBuffer.getComponent(sectionRef, WorldChunk.getComponentType());
                     String worldName = worldChunk != null
                        ? worldChunk.getWorld().getName()
                        : store.getExternalData().getWorld().getName();
                     TeleporterManager.getInstance().onTeleporterRemoved(worldName, worldPos.x, worldPos.y, worldPos.z);
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
