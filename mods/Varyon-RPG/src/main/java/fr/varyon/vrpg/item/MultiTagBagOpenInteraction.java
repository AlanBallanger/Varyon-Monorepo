package fr.varyon.vrpg.item;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemStackContainerConfig;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemStackContainerWindow;
import com.hypixel.hytale.server.core.inventory.InventoryUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemStackItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Ouvre un ItemStackContainer en acceptant plusieurs ItemTag (OR), la
 * config native ItemStackContainerConfig.ItemTag ne supportant qu'un seul tag.
 */
public final class MultiTagBagOpenInteraction extends SimpleInstantInteraction {

    public static final String TYPE_NAME_ORE_AND_ROCK = "OreAndRockBagOpen";
    public static final String TYPE_NAME_PLANT_AND_SOIL = "PlantAndSoilBagOpen";

    @Nonnull
    public static final BuilderCodec<MultiTagBagOpenInteraction> CODEC =
        ((BuilderCodec.Builder<MultiTagBagOpenInteraction>) ((BuilderCodec.Builder<MultiTagBagOpenInteraction>) BuilderCodec.builder(
            MultiTagBagOpenInteraction.class, MultiTagBagOpenInteraction::new, SimpleInstantInteraction.CODEC
        ).append(new KeyedCodec<String[]>("AcceptTags", new ArrayCodec<>(Codec.STRING, String[]::new)),
            (interaction, tags) -> interaction.tags = tags,
            interaction -> interaction.tags)
        .add()))
        .build();

    private String[] tags = new String[0];

    protected MultiTagBagOpenInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) return;
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        PageManager pageManager = playerComponent.getPageManager();
        if (pageManager.getCustomPage() != null) return;

        ItemStack heldItem = context.getHeldItem();
        if (ItemStack.isEmpty(heldItem)) return;

        byte heldItemSlot = context.getHeldItemSlot();
        ItemContainer itemContainer = InventoryUtils.getSectionById(ref, context.getHeldItemSectionId(), commandBuffer);
        if (itemContainer == null) return;

        ItemStack itemStack = itemContainer.getItemStack(heldItemSlot);
        if (itemStack == null) return;

        Item item = itemStack.getItem();
        ItemStackContainerConfig config = item.getItemStackContainerConfig();
        ItemStackItemContainer itemStackItemContainer = ItemStackItemContainer.ensureConfiguredContainer(itemContainer, heldItemSlot, config);
        if (itemStackItemContainer == null) return;

        if (tags.length > 0) {
            int[] tagIndexes = new int[tags.length];
            for (int i = 0; i < tags.length; i++) {
                tagIndexes[i] = AssetRegistry.getOrCreateTagIndex(tags[i]);
            }
            for (short i = 0; i < itemStackItemContainer.getCapacity(); i++) {
                itemStackItemContainer.setSlotFilter(FilterActionType.ADD, i, (actionType, container, slot, addedStack) -> {
                    if (addedStack == null) return true;
                    Item addedItem = addedStack.getItem();
                    if (addedItem == null || addedItem.getData() == null) return false;
                    for (int tagIndex : tagIndexes) {
                        if (addedItem.getData().getExpandedTagIndexes().contains(tagIndex)) return true;
                    }
                    return false;
                });
            }
        }

        pageManager.setPageWithWindows(ref, store, Page.Bench, true, new ItemStackContainerWindow(itemStackItemContainer));
    }
}
