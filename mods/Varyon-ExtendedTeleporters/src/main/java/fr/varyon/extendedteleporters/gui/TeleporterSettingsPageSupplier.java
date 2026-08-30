package fr.varyon.extendedteleporters.gui;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.TeleporterInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TeleporterSettingsPageSupplier implements CustomPageSupplier {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-PageSupplier");
   public static final BuilderCodec<TeleporterSettingsPageSupplier> CODEC = ((Builder)BuilderCodec.builder(
            TeleporterSettingsPageSupplier.class, TeleporterSettingsPageSupplier::new
         )
         .appendInherited(
            new KeyedCodec("ActiveState", Codec.STRING),
            (supplier, o) -> supplier.activeState = o,
            supplier -> supplier.activeState,
            (supplier, parent) -> supplier.activeState = parent.activeState
         )
         .add())
      .build();

   @Nullable
   private String activeState;

   @Nullable
   public CustomUIPage tryCreate(
      @Nonnull Ref<EntityStore> ref, ComponentAccessor<EntityStore> componentAccessor, @Nonnull PlayerRef playerRef, @Nonnull InteractionContext context
   ) {
      BlockPosition targetBlock = context.getTargetBlock();
      if (targetBlock == null) {
         return null;
      }

      World world = ((EntityStore)ref.getStore().getExternalData()).getWorld();
      ChunkStore chunkStore = world.getChunkStore();
      long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
      Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
      WorldChunk blockComponentChunk = chunkRef == null
         ? null
         : chunkStore.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
      if (blockComponentChunk == null) {
         return null;
      }

      Ref<ChunkStore> blockRef = blockComponentChunk.getBlockComponentEntity(targetBlock.x, targetBlock.y, targetBlock.z);
      if (blockRef != null && blockRef.isValid()) {
         Teleporter teleporter = (Teleporter)chunkStore.getStore().getComponent(blockRef, Teleporter.getComponentType());
         if (teleporter == null) {
            return null;
         }

         TeleporterManager manager = TeleporterManager.getInstance();
         TeleporterInfo info = manager.getTeleporter(world.getName(), targetBlock.x, targetBlock.y, targetBlock.z);
         return info != null && !info.canPlayerInteract(playerRef.getUuid()) && !manager.isInBypassMode(playerRef.getUuid())
            ? null
            : new TeleporterBlockSettingsGui(playerRef, blockRef, info, world, this.activeState);
      } else {
         return null;
      }
   }
}
