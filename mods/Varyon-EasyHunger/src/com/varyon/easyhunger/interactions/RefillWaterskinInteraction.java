package com.varyon.easyhunger.interactions;

import com.varyon.easyhunger.EasyHunger;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.iterator.BlockIterator;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionConfiguration;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.WaitForDataFrom;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom interaction to refill Waterskin/WaterBowl when partially filled.
 * This allows players to refill their water containers even when not 100% empty.
 * If not looking at water, the interaction fails and allows fallback to drinking.
 */
public class RefillWaterskinInteraction extends SimpleInstantInteraction {

    // Allowed fluid IDs for refilling
    private String[] allowedFluids = new String[]{"Water_Source", "Water"};
    
    // Cached fluid IDs for performance
    private int[] allowedFluidIds = null;

    public static final BuilderCodec<RefillWaterskinInteraction> CODEC = 
        ((BuilderCodec.Builder<RefillWaterskinInteraction>) ((BuilderCodec.Builder<RefillWaterskinInteraction>) BuilderCodec.builder(
            RefillWaterskinInteraction.class, 
            RefillWaterskinInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("AllowedFluids", new ArrayCodec<>(Codec.STRING, String[]::new)), 
                (interaction, value) -> interaction.allowedFluids = value, 
                interaction -> interaction.allowedFluids)
            .add())
        ).build();

    public RefillWaterskinInteraction() {
        super();
    }

    /**
     * Get cached fluid IDs, computing them lazily if needed.
     */
    private int[] getAllowedFluidIds() {
        if (this.allowedFluidIds != null) {
            return this.allowedFluidIds;
        }
        this.allowedFluidIds = Arrays.stream(this.allowedFluids)
            .mapToInt(key -> Fluid.getAssetMap().getIndex(key))
            .sorted()
            .toArray();
        return this.allowedFluidIds;
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, 
                           @Nonnull InteractionContext context, 
                           @Nonnull CooldownHandler cooldownHandler) {
        try {
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
            if (commandBuffer == null) {
                context.getState().state = InteractionState.Failed;
                return;
            }

            InteractionSyncData state = context.getState();
            World world = commandBuffer.getExternalData().getWorld();
            Ref<EntityStore> ref = context.getEntity();

            // Don't refill if targeting an entity
            if (context.getTargetEntity() != null) {
                state.state = InteractionState.Failed;
                return;
            }

            Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                state.state = InteractionState.Failed;
                return;
            }

            TransformComponent transformComponent = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
            if (transformComponent == null) {
                state.state = InteractionState.Failed;
                return;
            }

            HeadRotation headRotationComponent = commandBuffer.getComponent(ref, HeadRotation.getComponentType());
            if (headRotationComponent == null) {
                state.state = InteractionState.Failed;
                return;
            }

            ModelComponent modelComponent = commandBuffer.getComponent(ref, ModelComponent.getComponentType());
            if (modelComponent == null) {
                state.state = InteractionState.Failed;
                return;
            }

            ItemStack heldItem = context.getHeldItem();
            if (heldItem == null) {
                state.state = InteractionState.Failed;
                return;
            }

            // Check if item already has max durability
            if (heldItem.getDurability() >= heldItem.getMaxDurability()) {
                state.state = InteractionState.Failed;
                return;
            }

            // Use fixed distance (typical interaction distance)
            float distance = 5.0f;

            // Calculate ray from player's eye position in look direction
            Vector3d fromPos = new Vector3d(transformComponent.getPosition());
            fromPos.y += (double) modelComponent.getModel().getEyeHeight(ref, commandBuffer);
            Vector3d lookDir = headRotationComponent.getDirection();
            Vector3d toPos = new Vector3d(fromPos).add(new Vector3d(lookDir).mul(distance));

            AtomicBoolean refilled = new AtomicBoolean(false);

            // Iterate through blocks in the ray to find water
            BlockIterator.iterateFromTo(fromPos, toPos, (x, y, z, px, py, pz, qx, qy, qz) -> {
                Ref<ChunkStore> section = world.getChunkStore().getChunkSectionReference(
                    ChunkUtil.chunkCoordinate(x), 
                    ChunkUtil.chunkCoordinate(y), 
                    ChunkUtil.chunkCoordinate(z)
                );
                
                if (section == null) {
                    return true; // Continue iterating
                }

                BlockSection blockSection = section.getStore().getComponent(section, BlockSection.getComponentType());
                if (blockSection == null) {
                    return true;
                }

                // Stop if we hit a solid block
                if (FluidTicker.isSolid(BlockType.getAssetMap().getAsset(blockSection.get(x, y, z)))) {
                    state.state = InteractionState.Failed;
                    return false; // Stop iterating
                }

                FluidSection fluidSection = section.getStore().getComponent(section, FluidSection.getComponentType());
                if (fluidSection == null) {
                    return true;
                }

                int fluidId = fluidSection.getFluidId(x, y, z);
                int[] allowedIds = this.getAllowedFluidIds();

                // Check if this fluid is in our allowed list
                if (allowedIds == null || Arrays.binarySearch(allowedIds, fluidId) < 0) {
                    return true; // Continue, not the fluid we're looking for
                }

                // Found water! Refill the item to max durability
                ItemStack current = context.getHeldItem();
                double maxDurability = current.getMaxDurability();
                
                if (maxDurability <= current.getDurability()) {
                    state.state = InteractionState.Failed;
                    return false;
                }

                // Create new item with max durability
                ItemStack newItem = current.withIncreasedDurability(maxDurability);
                ItemStackSlotTransaction transaction = context.getHeldItemContainer()
                    .setItemStackForSlot(context.getHeldItemSlot(), newItem);

                if (!transaction.succeeded()) {
                    state.state = InteractionState.Failed;
                    return false;
                }

                context.setHeldItem(newItem);
                refilled.set(true);
                return false; // Stop iterating, we're done
            });

            if (!refilled.get()) {
                state.state = InteractionState.Failed;
            }

        } catch (Exception e) {
            EasyHunger.logInfo("REFILL WATERSKIN ERROR: " + e.toString());
            context.getState().state = InteractionState.Failed;
        }
    }

    /**
     * Server-side interaction - client must wait for server to determine success/failure.
     * This prevents the client from executing the next interaction (Charging/drinking)
     * before the server confirms whether this interaction succeeded.
     */
    @Override
    @Nonnull
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    /**
     * This interaction needs to sync with remote clients.
     */
    @Override
    public boolean needsRemoteSync() {
        return true;
    }

    @Override
    @Nonnull
    public String toString() {
        return "RefillWaterskinInteraction{allowedFluids=" + Arrays.toString(this.allowedFluids) + "} " + super.toString();
    }
}
