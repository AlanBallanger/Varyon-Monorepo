package fr.varyon.itemframes.util;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.itemframes.VaryonItemFramesPlugin;
import fr.varyon.itemframes.component.BoundEntityComponent;
import fr.varyon.itemframes.component.ItemFrameComponent;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("UnusedReturnValue")
public class FrameUtil {
	/**
	 * The component type for item frame components (For mod compatibility)
	 */
	public static final Supplier<ComponentType<EntityStore, ItemFrameComponent>> ITEM_FRAME_COMPONENT = () ->
			VaryonItemFramesPlugin.get().getItemFrameComponent();
	/**
	 * List of item frame block IDs
	 */
	private static final List<String> itemFrameItems = List.of(
			"Varyon_Item_Frame",
			"Varyon_Item_Frame_Ancient",
			"Varyon_Item_Frame_Bamboo",
			"Varyon_Item_Frame_Kweebec",
			"Varyon_Item_Frame_Light",
			"Varyon_Item_Frame_Lumberjack",
			"Varyon_Item_Frame_Tavern"
	);

	/**
	 * Check if the given frame ID corresponds to an item frame
	 *
	 * @param frameId The frame ID to check
	 * @return True if the frame ID is an item frame, false otherwise
	 */
	public static boolean isItemFrame(String frameId) {
		return itemFrameItems.contains(frameId);
	}

	/**
	 * Scale applied to the held item's dropped-item render inside the frame.
	 */
	private static final float ITEM_SCALE = 0.55F;

	/**
	 * Vertical correction (blocks) added to the display position so the centred
	 * dropped-item render sits in the middle of the frame opening.
	 */
	private static final double ITEM_Y_OFFSET = -0.30;

	/**
	 * Yaw (radians) for a frame facing the given cardinal rotation, matching the
	 * NESW variant rotation of the frame block.
	 */
	public static float frameYawRadians(int yawDegrees) {
		return switch (((yawDegrees % 360) + 360) % 360) {
			case 90 -> (float) (-Math.PI / 2.0);
			case 180 -> (float) Math.PI;
			case 270 -> (float) (Math.PI / 2.0);
			default -> 0.0F;
		};
	}

	/**
	 * World-space rotation of the display entity so the item model faces outward
	 * from the wall, flat against the frame.
	 */
	public static Rotation3f frameRotation(int yawDegrees) {
		return new Rotation3f(0.0F, frameYawRadians(yawDegrees), 0.0F);
	}

	/**
	 * World-space position of the display entity: centred on the frame block and
	 * pushed slightly out of the wall on the facing side.
	 */
	public static Vector3d framePosition(Vector3i framePos, int yawDegrees, double yOffset) {
		Vector3d position = new Vector3d(framePos).add(0.5, 0.5 + yOffset, 0.5);
		switch (((yawDegrees % 360) + 360) % 360) {
			case 0 -> position.add(0, 0, -0.45);
			case 90 -> position.add(-0.45, 0, 0);
			case 180 -> position.add(0, 0, 0.45);
			case 270 -> position.add(0.45, 0, 0);
		}
		return position;
	}

	/**
	 * Get the item frame's entity store reference at the given position
	 *
	 * @param world The world
	 * @param pos   The position of the item frame block
	 * @return The entity store reference of the item frame, or null if not found
	 */
	public static Ref<EntityStore> getFrameEntity(World world, Vector3i pos) {
		int x = pos.x();
		int y = pos.y();
		int z = pos.z();
		long indexChunk = ChunkUtil.indexChunkFromBlock(x, z);
		WorldChunk worldchunk = world.getChunkStore().getChunkComponent(indexChunk, WorldChunk.getComponentType());
		if (worldchunk == null) {
			return null;
		}
		var chunkRef = worldchunk.getBlockComponentEntity(x, y, z);
		if (chunkRef == null) {
			chunkRef = BlockModule.getBlockEntity(world, x, y, z);
		}

		BlockType blockType = worldchunk.getBlockType(pos);
		if (blockType == null) {
			return null;
		}

		if (chunkRef == null) {
			return null;
		}

		Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
		BoundEntityComponent boundEntityComponent = chunkStore.getComponent(chunkRef, VaryonItemFramesPlugin.get().getBoundEntityComponent());
		if (boundEntityComponent != null && FrameUtil.isItemFrame(blockType.getId()) && boundEntityComponent.getAttachedEntity() != null) {
			return world.getEntityRef(boundEntityComponent.getAttachedEntity());
		}
		return null;
	}

	/**
	 * Check if the block at the given position is an item frame
	 *
	 * @param world The world
	 * @param pos   The position of the block to check
	 * @return True if the block is an item frame, false otherwise
	 */
	public static boolean isFrameBlock(World world, Vector3i pos) {
		Ref<EntityStore> frameRef = getFrameEntity(world, pos);
		return frameRef != null;
	}

	/**
	 * Set the item in the item frame at the given position
	 *
	 * @param world     The world
	 * @param pos       The position of the item frame block
	 * @param stack     The item stack to set in the item frame
	 * @param overwrite Whether to overwrite the existing item if present
	 * @return True if the item was set successfully, false otherwise
	 */
	public static boolean setFrameItem(CommandBuffer<EntityStore> commandBuffer, World world, Vector3i pos, @Nullable ItemStack stack, boolean overwrite) {
		if (!isFrameBlock(world, pos)) return false;

		Ref<EntityStore> frameRef = getFrameEntity(world, pos);
		if (frameRef == null) {
			return false;
		}

		Store<EntityStore> store = world.getEntityStore().getStore();
		ItemFrameComponent frameComponent = store.getComponent(frameRef, ItemFrameComponent.getComponentType());
		if (frameComponent == null) {
			return false;
		}

		if (frameComponent.getHeldStack() == null || overwrite) {
			int x = pos.x();
			int y = pos.y();
			int z = pos.z();
			frameComponent.setHeldStack(stack);
			commandBuffer.run(entityStore -> {
				FrameUtil.remakeItemEntity(entityStore, frameRef, stack, frameComponent.getFrameRotation());
				world.performBlockUpdate(x, y, z);
			});
			return true;
		}
		return false;
	}

	/**
	 * Get the item in the item frame at the given position
	 *
	 * @param world The world
	 * @param pos   The position of the item frame block
	 * @return The item stack in the item frame, or null if not found
	 */
	public static ItemStack getFrameItem(World world, Vector3i pos) {
		if (!isFrameBlock(world, pos)) return null;

		Ref<EntityStore> frameRef = getFrameEntity(world, pos);
		if (frameRef == null) {
			return null;
		}

		Store<EntityStore> store = world.getEntityStore().getStore();
		ItemFrameComponent frameComponent = store.getComponent(frameRef, ItemFrameComponent.getComponentType());
		if (frameComponent == null) {
			return null;
		}

		return frameComponent.getHeldStack();
	}

	/**
	 * Remake the item entity for the given item stack
	 *
	 * @param store  the entity store
	 * @param oldRef the old entity reference
	 * @param stack  the item stack to set
	 * @return the new entity reference
	 */
	public static Ref<EntityStore> remakeItemEntity(
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> oldRef,
			@Nullable ItemStack stack,
			int yawDegrees
	) {
		ItemFrameComponent frameComponent = store.getComponent(oldRef, ItemFrameComponent.getComponentType());
		Vector3i framePos = frameComponent != null ? frameComponent.getFramePosition() : null;

		// Remove existing visual components so a copy carries none of the old item
		removeVariantComponents(store, oldRef);
		store.removeComponentIfExists(oldRef, HeadRotation.getComponentType());

		// Copy the existing entity (keeps transform, network id, uuid, flags, interactions...)
		Holder<EntityStore> holder = store.copyEntity(oldRef);

		if (stack != null) {
			ItemStack newStack = new ItemStack(stack.getItemId(), 1);
			newStack.setOverrideDroppedItemAnimation(true);
			holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(newStack));
			holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(ITEM_SCALE));
		}

		// Re-anchor flat against the frame, facing outward
		if (framePos != null) {
			holder.putComponent(TransformComponent.getComponentType(),
					new TransformComponent(framePosition(framePos, yawDegrees, ITEM_Y_OFFSET), frameRotation(yawDegrees)));
		}

		// Remove old entity AFTER copying
		store.removeEntity(oldRef, RemoveReason.REMOVE);

		// Respawn cleanly
		return store.addEntity(holder, AddReason.SPAWN);
	}

	public static void removeVariantComponents(Store<EntityStore> store, Ref<EntityStore> ref) {
		store.removeComponentIfExists(ref, EntityScaleComponent.getComponentType());
		store.removeComponentIfExists(ref, ItemComponent.getComponentType());
	}
}
