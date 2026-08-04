package fr.varyon.extendedteleporters.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TeleporterBreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
   private static final String TELEPORTER_BLOCK_NAME = "Teleporter";

   public TeleporterBreakBlockEventSystem() {
      super(BreakBlockEvent.class);
   }

   public void handle(
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull BreakBlockEvent event
   ) {
      String blockId = event.getBlockType().getId();
      if (blockId != null && blockId.contains("Teleporter")) {
         Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
         Player player = (Player)store.getComponent(ref, Player.getComponentType());
         if (player != null) {
            int blockX = event.getTargetBlock().x;
            int blockY = event.getTargetBlock().y;
            int blockZ = event.getTargetBlock().z;
            if (player.getWorld() != null) {
               String dimension = player.getWorld().getName();
               TeleporterManager manager = TeleporterManager.getInstance();
               TeleporterInfo info = manager.getTeleporter(dimension, blockX, blockY, blockZ);
               if (info != null) {
                  PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
                  if (playerRef != null) {
                     UUID playerUuid = playerRef.getUuid();
                     boolean canBreak = info.canPlayerBreak(playerUuid);
                     boolean bypassMode = manager.isInBypassMode(playerUuid);
                     if (!canBreak && !bypassMode) {
                        event.setCancelled(true);
                        return;
                     }
                  }
               }

               manager.onTeleporterRemoved(dimension, blockX, blockY, blockZ);
            }
         }
      }
   }

   public Query<EntityStore> getQuery() {
      return PlayerRef.getComponentType();
   }

   @NonNullDecl
   public Set<Dependency<EntityStore>> getDependencies() {
      return Set.of(RootDependency.first());
   }
}
