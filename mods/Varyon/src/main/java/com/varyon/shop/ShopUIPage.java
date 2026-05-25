package com.varyon.shop;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ShopConfig;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;

public class ShopUIPage extends InteractiveCustomUIPage<ShopUIPage.EventDataClass> {

    private static final int MAX_ITEMS = 10;

    public ShopUIPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("ShopMenu.ui");

        ShopConfig shopConfig = getShopConfig();
        if (shopConfig == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getInventory() == null) return;

        List<ShopConfig.ShopItem> items = shopConfig.getItems();
        for (int i = 1; i <= MAX_ITEMS; i++) {
            boolean visible = i <= items.size();
            commandBuilder.set("#Item" + i + ".Visible", visible);
            if (!visible) continue;

            ShopConfig.ShopItem item = items.get(i - 1);
            commandBuilder.set("#Item" + i + "Label.TextSpans", Message.raw(item.getLabel()));
            commandBuilder.set("#Item" + i + "Cost.TextSpans", Message.raw(item.getCostAmount() + "x"));
            commandBuilder.set("#Item" + i + "Reward.TextSpans", Message.raw("1x"));
            commandBuilder.set("#Item" + i + "FragmentIcon.ItemId", item.getCostItem());
            commandBuilder.set("#Item" + i + "KeyIcon.ItemId", item.getKeyItemId());

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BuyButton" + i,
                EventData.of("Action", "buy").append("Index", String.valueOf(i - 1)));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"));

        buildBuyButtonStates(ref, store, commandBuilder);
    }

    private int countItems(CombinedItemContainer container, String itemId) {
        try {
            return container.countItemStacks(stack -> itemId.equals(stack.getItemId()));
        } catch (Throwable t) {
            return 0;
        }
    }

    private void buildBuyButtonStates(@Nonnull Ref<EntityStore> ref,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull UICommandBuilder commandBuilder) {
        ShopConfig shopConfig = getShopConfig();
        if (shopConfig == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getInventory() == null) return;

        CombinedItemContainer container = player.getInventory().getCombinedHotbarFirst();
        List<ShopConfig.ShopItem> items = shopConfig.getItems();

        for (int i = 1; i <= Math.min(MAX_ITEMS, items.size()); i++) {
            ShopConfig.ShopItem item = items.get(i - 1);
            int fragmentCount = countItems(container, item.getCostItem());
            boolean canAfford = fragmentCount >= item.getCostAmount();
            commandBuilder.set("#BuyButton" + i + ".Disabled", !canAfford);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventDataClass data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if ("close".equals(data.action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if ("buy".equals(data.action) && data.index != null) {
            ShopConfig shopConfig = getShopConfig();
            if (shopConfig == null) return;

            int idx;
            try {
                idx = Integer.parseInt(data.index);
            } catch (NumberFormatException e) {
                return;
            }

            List<ShopConfig.ShopItem> items = shopConfig.getItems();
            if (idx < 0 || idx >= items.size()) return;

            ShopConfig.ShopItem item = items.get(idx);
            CombinedItemContainer container = player.getInventory().getCombinedHotbarFirst();

            int fragmentCount = countItems(container, item.getCostItem());
            if (fragmentCount < item.getCostAmount()) {
                player.sendMessage(Message.raw("Fragments insuffisants.").color(Color.RED));
                return;
            }

            ItemStack toRemove = new ItemStack(item.getCostItem(), item.getCostAmount());
            if (!container.canRemoveItemStack(toRemove)) {
                player.sendMessage(Message.raw("Fragments insuffisants.").color(Color.RED));
                return;
            }
            container.removeItemStack(toRemove);

            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                String cmd = "lb givekey " + playerRef.getUsername() + " " + item.getTierId() + " 1";
                CommandManager.get().handleCommand((CommandSender) ConsoleSender.INSTANCE, cmd);
            }

            UICommandBuilder cb = new UICommandBuilder();
            buildBuyButtonStates(ref, store, cb);
            sendUpdate(cb, new UIEventBuilder(), false);

            player.sendMessage(Message.raw("Achat réussi: " + item.getLabel()).color(Color.GREEN));
        }
    }

    private ShopConfig getShopConfig() {
        var configManager = VaryonPlugin.getStaticConfigManager();
        return configManager != null ? configManager.getShopConfig() : null;
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
                .addField(new KeyedCodec<>("Index", Codec.STRING), (e, s) -> e.index = s, e -> e.index)
                .build();

        public String action;
        public String index;
    }
}
