package fr.varyon.extendedteleporters.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.blocktrack.BlockCounter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

public class UnlimitedPlacementConditionInteraction extends SimpleInstantInteraction {
   private static final Set<String> UNLIMITED_BLOCKS = Set.of("Teleporter");
   public static final BuilderCodec<UnlimitedPlacementConditionInteraction> CODEC;

   static {
      Builder<UnlimitedPlacementConditionInteraction> b = BuilderCodec.<UnlimitedPlacementConditionInteraction>builder(
         UnlimitedPlacementConditionInteraction.class, UnlimitedPlacementConditionInteraction::new, SimpleInstantInteraction.CODEC
      );
      b = (Builder<UnlimitedPlacementConditionInteraction>) b.appendInherited(new KeyedCodec("Block", Codec.STRING), (UnlimitedPlacementConditionInteraction self, String val) -> self.blockType = val, (UnlimitedPlacementConditionInteraction self) -> self.blockType, (UnlimitedPlacementConditionInteraction self, UnlimitedPlacementConditionInteraction other) -> self.blockType = other.blockType)
         .addValidator(Validators.nonNull())
         .add();
      b = (Builder<UnlimitedPlacementConditionInteraction>) b.appendInherited(new KeyedCodec("Value", Codec.INTEGER), (UnlimitedPlacementConditionInteraction self, Integer val) -> self.value = val, (UnlimitedPlacementConditionInteraction self) -> self.value, (UnlimitedPlacementConditionInteraction self, UnlimitedPlacementConditionInteraction other) -> self.value = other.value)
         .add();
      b = (Builder<UnlimitedPlacementConditionInteraction>) b.appendInherited(new KeyedCodec("LessThan", Codec.BOOLEAN), (UnlimitedPlacementConditionInteraction self, Boolean val) -> self.lessThan = val, (UnlimitedPlacementConditionInteraction self) -> self.lessThan, (UnlimitedPlacementConditionInteraction self, UnlimitedPlacementConditionInteraction other) -> self.lessThan = other.lessThan)
         .add();
      CODEC = b.build();
   }
   private String blockType;
   private int value = 0;
   private boolean lessThan = true;

   protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
      if (UNLIMITED_BLOCKS.contains(this.blockType)) {
         context.getState().state = InteractionState.Finished;
      } else {
         BlockCounter counter = (BlockCounter)((EntityStore)context.getCommandBuffer().getExternalData())
            .getWorld()
            .getChunkStore()
            .getStore()
            .getResource(BlockCounter.getResourceType());
         int blockCount = counter.getBlockPlacementCount(this.blockType);
         if (this.lessThan) {
            if (blockCount < this.value) {
               context.getState().state = InteractionState.Finished;
            } else {
               context.getState().state = InteractionState.Failed;
            }
         } else if (blockCount > this.value) {
            context.getState().state = InteractionState.Finished;
         } else {
            context.getState().state = InteractionState.Failed;
         }
      }
   }

   @Nonnull
   public WaitForDataFrom getWaitForDataFrom() {
      return WaitForDataFrom.Server;
   }
}
