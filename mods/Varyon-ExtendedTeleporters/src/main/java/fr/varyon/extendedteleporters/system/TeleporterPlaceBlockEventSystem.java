package fr.varyon.extendedteleporters.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.VaryonExtendedTeleportersPlugin;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.config.ExtendedTeleportConfig;
import java.awt.Color;
import java.util.Set;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TeleporterPlaceBlockEventSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
   private static final String TELEPORTER_BLOCK_NAME = "Teleporter";

   public TeleporterPlaceBlockEventSystem() {
      super(PlaceBlockEvent.class);
   }

   public void handle(
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull PlaceBlockEvent event
   ) {
      ItemStack itemInHand = event.getItemInHand();
      if (itemInHand != null && !itemInHand.isEmpty()) {
         String itemId = itemInHand.getItem().getId();
         if (itemId != null && itemId.endsWith("Teleporter")) {
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
            Player player = (Player)store.getComponent(ref, Player.getComponentType());
            if (playerRef != null && player != null) {
               TeleporterManager manager = TeleporterManager.getInstance();
               boolean defaultPrivate = false;
               boolean defaultRestricted = false;
               if (VaryonExtendedTeleportersPlugin.CONFIG != null) {
                  ExtendedTeleportConfig config = (ExtendedTeleportConfig)VaryonExtendedTeleportersPlugin.CONFIG.get();
                  if (config.isDefaultTeleporterPrivate()
                     && manager.getPermissionProvider().hasPermission(playerRef.getUuid(), "extendedteleporters.feature.private")) {
                     defaultPrivate = true;
                  }

                  if (config.isDefaultTeleporterRestricted()
                     && manager.getPermissionProvider().hasPermission(playerRef.getUuid(), "extendedteleporters.feature.restricted")) {
                     defaultRestricted = true;
                  }
               }

               TeleporterManager.PlacementCheckResult checkResult = manager.checkPlacementLimits(playerRef.getUuid(), defaultPrivate, defaultRestricted);
               if (!checkResult.allowed()) {
                  event.setCancelled(true);
                  playerRef.sendMessage(Message.raw(checkResult.errorMessage()).color(Color.RED));
               } else {
                  int blockX = event.getTargetBlock().x;
                  int blockY = event.getTargetBlock().y;
                  int blockZ = event.getTargetBlock().z;
                  manager.onTeleporterPlaced(playerRef.getUuid(), player.getWorld(), blockX, blockY, blockZ);
               }
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
