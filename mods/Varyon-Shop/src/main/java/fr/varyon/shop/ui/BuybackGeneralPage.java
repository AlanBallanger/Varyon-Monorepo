package fr.varyon.shop.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.config.BuybackPriceRepository;
import fr.varyon.shop.economy.VaultEconomyBridge;
import fr.varyon.shop.util.EntityApiCompat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * "Racheteur General" merchant window: a deposit grid where the player clicks items in from
 * their inventory, sees a live buyback estimate, and confirms the sale explicitly via the
 * "Vendre tout" button. Items with no configured price are still sold, for 0.
 *
 * Slot interaction: left click transfers the whole stack at once (source <-> deposit); right
 * click transfers half of the stack (rounded down, minimum 1). There is no true overlay/modal
 * in this UI engine (PageManager only ever holds one active CustomUIPage per player), so a
 * separate quantity-picker page isn't viable here.
 */
public final class BuybackGeneralPage extends InteractiveCustomUIPage<BuybackGeneralPage.EventDataPayload> {
    public static final String LAYOUT = "VaryonShop/BuybackGeneral/BuybackGeneralPage.ui";
    public static final short DEPOSIT_SLOTS = 27;
    private static final int INV_SLOTS = 36;
    private static final int HOTBAR_SLOTS = 9;

    private final BuybackPriceRepository priceRepository;
    private final VaultEconomyBridge economyBridge;
    private final PlayerRef playerRef;
    private final ItemContainer depositContainer;
    private final String merchantName;
    private boolean firstBuild = true;

    public BuybackGeneralPage(
            @NotNull BuybackPriceRepository priceRepository,
            @NotNull VaultEconomyBridge economyBridge,
            @NotNull PlayerRef playerRef
    ) {
        this(priceRepository, economyBridge, playerRef, null);
    }

    public BuybackGeneralPage(
            @NotNull BuybackPriceRepository priceRepository,
            @NotNull VaultEconomyBridge economyBridge,
            @NotNull PlayerRef playerRef,
            @Nullable String merchantName
    ) {
        this(priceRepository, economyBridge, playerRef, new SimpleItemContainer(DEPOSIT_SLOTS), merchantName);
    }

    private BuybackGeneralPage(
            @NotNull BuybackPriceRepository priceRepository,
            @NotNull VaultEconomyBridge economyBridge,
            @NotNull PlayerRef playerRef,
            @NotNull ItemContainer depositContainer,
            @Nullable String merchantName
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, EventDataPayload.CODEC);
        this.priceRepository = priceRepository;
        this.economyBridge = economyBridge;
        this.playerRef = playerRef;
        this.depositContainer = depositContainer;
        this.merchantName = merchantName;
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        if (firstBuild) {
            cmd.append(LAYOUT);
            cmd.append("#MainSlot", "VaryonShop/BuybackGeneral/MainPanel.ui");
            cmd.append("#InventorySlot", "VaryonShop/BuybackGeneral/InventoryPanel.ui");
            for (int i = 0; i < DEPOSIT_SLOTS; i++) {
                cmd.append("#ChestGrid", "VaryonShop/BuybackGeneral/Slot.ui");
            }
            for (int i = 0; i < INV_SLOTS; i++) {
                cmd.append("#InventoryGrid", "VaryonShop/BuybackGeneral/Slot.ui");
            }
            for (int i = 0; i < HOTBAR_SLOTS; i++) {
                cmd.append("#HotbarGrid", "VaryonShop/BuybackGeneral/Slot.ui");
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

        cmd.set("#DepositTitle.Text", (merchantName == null || merchantName.isBlank()) ? "RACHETEUR GENERAL" : merchantName.toUpperCase());

        int usedSlots = 0;
        int sellableCount = 0;
        double totalEstimate = 0.0;

        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            String selector = "#ChestGrid[" + i + "]";
            ItemStack stack = depositContainer.getItemStack((short) i);
            double unitPrice = isValidStack(stack) ? priceRepository.priceOf(stack.getItemId()) : 0.0;
            applySlot(cmd, selector, stack, unitPrice);
            bindSlotClicks(evt, selector, "chest", i);

            if (isValidStack(stack)) {
                usedSlots++;
                if (unitPrice > 0.0) {
                    sellableCount++;
                    totalEstimate += unitPrice * Math.max(1, stack.getQuantity());
                }
            }
        }

        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
        int storageCap = storage == null ? 0 : storage.getCapacity();
        int hotbarCap = hotbar == null ? 0 : hotbar.getCapacity();

        for (int i = 0; i < INV_SLOTS; i++) {
            String selector = "#InventoryGrid[" + i + "]";
            ItemStack stack = storage != null && i < storageCap ? storage.getItemStack((short) i) : null;
            applySlot(cmd, selector, stack, 0.0);
            bindSlotClicks(evt, selector, "inv", i);
        }
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            String selector = "#HotbarGrid[" + i + "]";
            ItemStack stack = hotbar != null && i < hotbarCap ? hotbar.getItemStack((short) i) : null;
            applySlot(cmd, selector, stack, 0.0);
            bindSlotClicks(evt, selector, "hotbar", i);
        }

        cmd.set("#UsageCount.Text", usedSlots + " / " + DEPOSIT_SLOTS);

        String formattedTotal = formatPlain(totalEstimate) + " Coins";
        cmd.set("#EstimateValue.Text", formattedTotal);
        cmd.set("#DetailItemsValue.Text", String.valueOf(sellableCount));
        cmd.set("#DetailValueValue.Text", formattedTotal);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SellAllButton", EventData.of("Action", "sellAll"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ClearChestButton", EventData.of("Action", "clearChest"), false);
    }

    private void bindSlotClicks(UIEventBuilder evt, String selector, String source, int slot) {
        EventData leftClick = new EventData().append("Action", "transferAll").append("Source", source).append("Slot", String.valueOf(slot));
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #SlotButton", leftClick, false);

        EventData rightClick = new EventData().append("Action", "transferHalf").append("Source", source).append("Slot", String.valueOf(slot));
        evt.addEventBinding(CustomUIEventBindingType.RightClicking, selector + " #SlotButton", rightClick, false);
    }

    private void applySlot(UICommandBuilder cmd, String selector, @Nullable ItemStack stack, double unitPrice) {
        String iconSel = selector + " #ItemIcon";
        String qtySel = selector + " #QtyBadge";
        String priceSel = selector + " #PriceBadge";
        String btnSel = selector + " #SlotButton";

        if (isValidStack(stack)) {
            cmd.set(iconSel + ".ItemId", stack.getItemId());
            cmd.set(iconSel + ".Visible", true);

            int qty = Math.max(1, stack.getQuantity());
            if (qty > 1) {
                cmd.set(qtySel + ".Text", String.valueOf(qty));
                cmd.set(qtySel + ".Visible", true);
            } else {
                cmd.set(qtySel + ".Visible", false);
            }

            if (unitPrice > 0.0) {
                cmd.set(priceSel + ".Text", formatPlain(unitPrice * qty));
                cmd.set(priceSel + ".Visible", true);
                cmd.set(btnSel + ".TooltipText", stack.getItemId() + "\n" + formatPlain(unitPrice) + " / unite");
            } else {
                cmd.set(priceSel + ".Visible", false);
                cmd.set(btnSel + ".TooltipText", stack.getItemId());
            }
        } else {
            cmd.setNull(iconSel + ".ItemId");
            cmd.set(iconSel + ".Visible", false);
            cmd.set(qtySel + ".Visible", false);
            cmd.set(priceSel + ".Visible", false);
            cmd.set(btnSel + ".TooltipText", "");
        }
    }

    @Override
    public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store, EventDataPayload data) {
        if (data == null || data.action == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        switch (data.action) {
            case "transferAll" -> transferAll(player, data.source, parseInt(data.slot));
            case "transferHalf" -> transferHalf(player, data.source, parseInt(data.slot));
            case "clearChest" -> moveAllFromDepositToPlayer(player);
            case "sellAll" -> sellAll();
            default -> { }
        }

        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        renderDynamic(ref, store, refreshCmd, refreshEvt);
        sendUpdate(refreshCmd, false);
    }

    private void transferHalf(Player player, @Nullable String source, int slot) {
        if (source == null || slot < 0) {
            return;
        }
        ItemContainer sourceContainer = resolveContainer(player, source);
        if (sourceContainer == null || slot >= sourceContainer.getCapacity()) {
            return;
        }
        ItemStack stack = sourceContainer.getItemStack((short) slot);
        if (!isValidStack(stack)) {
            return;
        }
        int half = Math.max(1, stack.getQuantity() / 2);
        ItemContainer target = "chest".equals(source) ? resolvePlayerReceivingContainer(player) : depositContainer;
        if (target == null) {
            return;
        }
        movePartial(sourceContainer, (short) slot, target, half);
    }

    @Nullable
    private ItemContainer resolvePlayerReceivingContainer(Player player) {
        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        if (storage != null) {
            return storage;
        }
        return EntityApiCompat.getHotbarContainer(player);
    }

    @Nullable
    private ItemContainer resolveContainer(Player player, @Nullable String source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case "chest" -> depositContainer;
            case "inv" -> EntityApiCompat.getStorageContainer(player);
            case "hotbar" -> EntityApiCompat.getHotbarContainer(player);
            default -> null;
        };
    }

    private void transferAll(Player player, @Nullable String source, int slot) {
        if (source == null || slot < 0) {
            return;
        }
        if ("chest".equals(source)) {
            moveFromDepositToPlayer(player, slot);
            return;
        }
        ItemContainer container = resolveContainer(player, source);
        moveFromPlayerContainerToDeposit(container, slot);
    }

    private void moveFromDepositToPlayer(Player player, int depositSlot) {
        if (depositSlot < 0) {
            return;
        }
        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
        if (storage != null && nativeMove(depositContainer, (short) depositSlot, storage)) {
            return;
        }
        if (hotbar != null) {
            nativeMove(depositContainer, (short) depositSlot, hotbar);
        }
    }

    private void moveFromPlayerContainerToDeposit(@Nullable ItemContainer source, int slot) {
        if (source == null || slot < 0) {
            return;
        }
        nativeMove(source, (short) slot, depositContainer);
    }

    private void moveAllFromDepositToPlayer(Player player) {
        ItemContainer storage = EntityApiCompat.getStorageContainer(player);
        ItemContainer hotbar = EntityApiCompat.getHotbarContainer(player);
        short cap = depositContainer.getCapacity();
        for (short i = 0; i < cap; i++) {
            if (!isValidStack(depositContainer.getItemStack(i))) {
                continue;
            }
            if (storage != null && nativeMove(depositContainer, i, storage)) {
                continue;
            }
            if (hotbar != null) {
                nativeMove(depositContainer, i, hotbar);
            }
        }
    }

    private boolean nativeMove(@Nullable ItemContainer source, short sourceSlot, @Nullable ItemContainer target) {
        if (source == null || target == null || sourceSlot < 0 || sourceSlot >= source.getCapacity()) {
            return false;
        }
        try {
            MoveTransaction tx = source.moveItemStackFromSlot(sourceSlot, target);
            return tx != null && tx.succeeded();
        } catch (Throwable t) {
            return false;
        }
    }

    private void movePartial(ItemContainer source, short sourceSlot, ItemContainer target, int qty) {
        if (sourceSlot < 0 || sourceSlot >= source.getCapacity() || qty <= 0) {
            return;
        }
        ItemStack stack = source.getItemStack(sourceSlot);
        if (!isValidStack(stack)) {
            return;
        }
        int actualQty = Math.min(qty, stack.getQuantity());
        try {
            ItemStack portion = stack.withQuantity(actualQty);
            var removeTx = source.removeItemStack(portion, false, false);
            if (removeTx == null || !removeTx.succeeded()) {
                return;
            }
            int removedQty = actualQty - (removeTx.getRemainder() != null ? removeTx.getRemainder().getQuantity() : 0);
            if (removedQty <= 0) {
                return;
            }
            ItemStack toAdd = stack.withQuantity(removedQty);
            var addTx = target.addItemStack(toAdd, false, false, true);
            ItemStack overflow = addTx == null ? null : addTx.getRemainder();
            if (overflow != null && !overflow.isEmpty()) {
                source.addItemStack(overflow, false, false, true);
            }
        } catch (Throwable ignored) {
        }
    }

    private void sellAll() {
        if (!economyBridge.isAvailable()) {
            playerRef.sendMessage(Message.raw("Racheteur General: economie indisponible."));
            return;
        }
        short capacity = depositContainer.getCapacity();
        double totalEarned = 0.0;
        int soldUnits = 0;
        int soldForZero = 0;

        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = depositContainer.getItemStack(slot);
            if (!isValidStack(stack)) {
                continue;
            }
            double unitPrice = priceRepository.priceOf(stack.getItemId());
            int qty = Math.max(1, stack.getQuantity());
            try {
                depositContainer.removeItemStackFromSlot(slot, qty);
            } catch (Throwable t) {
                continue;
            }
            totalEarned += unitPrice * qty;
            soldUnits += qty;
            if (unitPrice <= 0.0) {
                soldForZero += qty;
            }
        }

        if (soldUnits <= 0) {
            playerRef.sendMessage(Message.raw("Racheteur General: le depot est vide."));
            return;
        }

        boolean credited = totalEarned <= 0.0 || economyBridge.addFunds(playerRef.getUuid(), totalEarned);
        if (!credited) {
            playerRef.sendMessage(Message.raw("Racheteur General: le paiement a echoue, contactez un admin."));
            return;
        }

        String formatted = economyBridge.format(totalEarned);
        StringBuilder msg = new StringBuilder("Racheteur General: ")
                .append(soldUnits).append(" objet(s) vendus pour ").append(formatted).append(".");
        if (soldForZero > 0) {
            msg.append(" ").append(soldForZero).append(" objet(s) non repertorie(s) vendu(s) pour 0.");
        }
        playerRef.sendMessage(Message.raw(msg.toString()));
    }

    private boolean isValidStack(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isValid() && stack.getQuantity() > 0;
    }

    private String formatPlain(double amount) {
        return String.format("%.0f", amount);
    }

    private int parseInt(@Nullable String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static final class EventDataPayload {
        public String action;
        public String source;
        public String slot;

        @SuppressWarnings("unchecked")
        public static final BuilderCodec<EventDataPayload> CODEC = buildCodec();

        @SuppressWarnings("unchecked")
        private static BuilderCodec<EventDataPayload> buildCodec() {
            BuilderCodec.Builder builder = BuilderCodec.builder(EventDataPayload.class, EventDataPayload::new);
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("Action", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.action = v, (Function<EventDataPayload, String>) d -> d.action).add();
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("Source", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.source = v, (Function<EventDataPayload, String>) d -> d.source).add();
            builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("Slot", (Codec) Codec.STRING), (BiConsumer<EventDataPayload, String>) (d, v) -> d.slot = v, (Function<EventDataPayload, String>) d -> d.slot).add();
            return builder.build();
        }
    }
}
