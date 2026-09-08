package fr.varyon.itemframes;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.itemframes.component.BoundEntityComponent;
import fr.varyon.itemframes.component.ItemFrameComponent;
import fr.varyon.itemframes.interaction.ItemFrameEntityInteraction;
import fr.varyon.itemframes.interaction.ItemFrameInteraction;
import fr.varyon.itemframes.system.ItemFrameSystems;

import javax.annotation.Nonnull;

public class VaryonItemFramesPlugin extends JavaPlugin {
	public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static VaryonItemFramesPlugin instance;

	private ComponentType<EntityStore, ItemFrameComponent> itemFrameComponent;
	private ComponentType<ChunkStore, BoundEntityComponent> boundEntityComponent;

	public VaryonItemFramesPlugin(@Nonnull JavaPluginInit init) {
		super(init);
		instance = this;
	}

	public static VaryonItemFramesPlugin get() {
		return instance;
	}

	@Override
	protected void setup() {
		LOGGER.atInfo().log("Setting up plugin " + this.getName());

		this.itemFrameComponent = this.getEntityStoreRegistry().registerComponent(ItemFrameComponent.class, "Varyon_ItemFrame", ItemFrameComponent.CODEC);
		this.boundEntityComponent = this.getChunkStoreRegistry().registerComponent(BoundEntityComponent.class, "Varyon_BoundEntity", BoundEntityComponent.CODEC);

		this.getCodecRegistry(Interaction.CODEC).register("VaryonItemFrameInteraction", ItemFrameInteraction.class, ItemFrameInteraction.CODEC);
		this.getCodecRegistry(Interaction.CODEC).register("VaryonItemFrameEntityInteraction", ItemFrameEntityInteraction.class, ItemFrameEntityInteraction.CODEC);

		ComponentRegistryProxy<EntityStore> componentregistryproxy = this.getEntityStoreRegistry();
		componentregistryproxy.registerSystem(new ItemFrameSystems.PlaceSystem());
		componentregistryproxy.registerSystem(new ItemFrameSystems.BreakSystem());
		componentregistryproxy.registerSystem(new ItemFrameSystems.ItemFrameTick());
	}

	public ComponentType<EntityStore, ItemFrameComponent> getItemFrameComponent() {
		return itemFrameComponent;
	}

	public ComponentType<ChunkStore, BoundEntityComponent> getBoundEntityComponent() {
		return boundEntityComponent;
	}
}
