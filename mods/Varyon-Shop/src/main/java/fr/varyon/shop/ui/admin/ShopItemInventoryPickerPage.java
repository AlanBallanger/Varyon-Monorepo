package fr.varyon.shop.ui.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.VaryonShopPlugin;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.util.EntityApiCompat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lets the admin click a slot in their own inventory to pick an item id. For ITEM_ID/ICON_ITEM_ID
 * targets, writes it into the edited entry and returns to ShopItemEditorPage. For CURRENCY,
 * registers it as a new shop currency and returns to ShopAdminListPage instead. Reuses
 * BuybackGeneralPage's grid layout.
 */
public final class ShopItemInventoryPickerPage extends InteractiveCustomUIPage<ShopAdminEventData> {
    private static final String LAYOUT = "VaryonShop/Admin/ShopItemInventoryPicker.ui";
    private static final int INV_SLOTS = 36;
    private static final int HOTBAR_SLOTS = 9;

    /** Which entry field this picker fills in, or CURRENCY to add a new shop currency instead. */
    public enum Target { ITEM_ID, ICON_ITEM_ID, CURRENCY }

    private final VaryonShopPlugin plugin;
    private final PlayerRef playerRef;
    private final String shopId;
    private final int index;
    private final Target target;
    private boolean firstBuild = true;

    public ShopItemInventoryPickerPage(@NotNull VaryonShopPlugin plugin, @NotNull PlayerRef playerRef,
                                        @Nullable String shopId, int index, @NotNull Target target) {
        super(playerRef, CustomPageLifetime.CanDismiss, ShopAdminEventData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.shopId = shopId;
        this.index = index;
        this.target = target;
    }

    private ShopCatalog.Entry entry() {
        var catalog = plugin.getShopCatalogRepository().get(shopId).orElse(null);
        if (catalog == null || index < 0 || index >= catalog.items.size()) {
            return null;
        }
        return catalog.items.get(index);
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        if (firstBuild) {
            cmd.append(LAYOUT);
            for (int i = 0; i < INV_SLOTS; i++) {
                cmd.append("#PickerInventoryGrid", "VaryonShop/BuybackGeneral/Slot.ui");
            }
            for (int i = 0; i < HOTBAR_SLOTS; i++) {
                cmd.append("#PickerHotbarGrid", "VaryonShop/BuybackGeneral/Slot.ui");
            }
            firstBuild = false;
        }
        renderDynamic(ref, store, cmd, evt);
    }

    private void renderDynamic(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder cmd, UIEventBuilder evt) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        cmd.set("#PickerTitle.Text", switch (target) {
            case ITEM_ID -> "CHOISIR L'OBJET";
            case ICON_ITEM_ID -> "CHOISIR L'ICÔNE";
            case CURRENCY -> "CHOISIR LA DEVISE";
        });

        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
        int storageCap = storage == null ? 0 : storage.getCapacity();
        int hotbarCap = hotbar == null ? 0 : hotbar.getCapacity();

        for (int i = 0; i < INV_SLOTS; i++) {
            String selector = "#PickerInventoryGrid[" + i + "]";
            ItemStack stack = storage != null && i < storageCap ? storage.getItemStack((short) i) : null;
            applySlot(cmd, selector, stack);
            bindSlotClick(evt, selector, "inv", i);
        }
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            String selector = "#PickerHotbarGrid[" + i + "]";
            ItemStack stack = hotbar != null && i < hotbarCap ? hotbar.getItemStack((short) i) : null;
            applySlot(cmd, selector, stack);
            bindSlotClick(evt, selector, "hotbar", i);
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PickerCancelButton", EventData.of("Action", "cancelPick"), false);
    }

    private void bindSlotClick(UIEventBuilder evt, String selector, String source, int slot) {
        EventData click = new EventData().append("Action", "pick").append("Source", source).append("Slot", String.valueOf(slot));
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #SlotButton", click, false);
    }

    private void applySlot(UICommandBuilder cmd, String selector, @Nullable ItemStack stack) {
        String iconSel = selector + " #ItemIcon";
        String qtySel = selector + " #QtyBadge";
        String priceSel = selector + " #PriceBadge";
        String btnSel = selector + " #SlotButton";

        if (isValidStack(stack)) {
            cmd.set(iconSel + ".ItemId", stack.getItemId());
            cmd.set(iconSel + ".Visible", true);
            int qty = Math.max(1, stack.getQuantity());
            cmd.set(qtySel + ".Visible", qty > 1);
            if (qty > 1) {
                cmd.set(qtySel + ".Text", String.valueOf(qty));
            }
            cmd.set(priceSel + ".Visible", false);
            cmd.set(btnSel + ".TooltipText", stack.getItemId());
        } else {
            cmd.setNull(iconSel + ".ItemId");
            cmd.set(iconSel + ".Visible", false);
            cmd.set(qtySel + ".Visible", false);
            cmd.set(priceSel + ".Visible", false);
            cmd.setNull(btnSel + ".TooltipText");
        }
    }

    @Override
    public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store, ShopAdminEventData data) {
        if (data == null || data.getAction() == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if ("cancelPick".equals(data.getAction())) {
            returnToCaller(ref, store, player);
            return;
        }

        if ("pick".equals(data.getAction())) {
            ItemContainer container = "inv".equals(data.getSource()) ? EntityApiCompat.getStorageContainer(player)
                    : "hotbar".equals(data.getSource()) ? EntityApiCompat.getHotbarContainer(player) : null;
            int slot = parseInt(data.getSlot(), -1);
            ItemStack stack = container != null && slot >= 0 && slot < container.getCapacity() ? container.getItemStack((short) slot) : null;
            if (isValidStack(stack)) {
                if (target == Target.CURRENCY) {
                    plugin.getShopSettings().addCurrency(stack.getItemId(), null);
                } else {
                    ShopCatalog.Entry entry = entry();
                    if (entry != null) {
                        if (target == Target.ITEM_ID) {
                            entry.itemId = stack.getItemId();
                            if (entry.category == null || entry.category.isBlank() || "Divers".equalsIgnoreCase(entry.category)) {
                                String knownCategory = plugin.getBuybackPriceRepository().categoryOf(stack.getItemId());
                                if (knownCategory != null && !knownCategory.isBlank()) {
                                    entry.category = knownCategory;
                                }
                            }
                        } else {
                            entry.iconItemId = stack.getItemId();
                        }
                        plugin.getShopCatalogRepository().save(shopId);
                    }
                }
            }
            returnToCaller(ref, store, player);
        }
    }

    private void returnToCaller(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        if (target == Target.CURRENCY) {
            player.getPageManager().openCustomPage(ref, store, new ShopAdminListPage(plugin, playerRef));
        } else {
            player.getPageManager().openCustomPage(ref, store, new ShopItemEditorPage(plugin, playerRef, shopId, index));
        }
    }

    private boolean isValidStack(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isValid() && stack.getQuantity() > 0;
    }

    private int parseInt(@Nullable String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
