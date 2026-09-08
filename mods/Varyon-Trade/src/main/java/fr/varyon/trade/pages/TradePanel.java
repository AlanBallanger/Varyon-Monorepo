package fr.varyon.trade.pages;

import fr.varyon.trade.DynamicImageService;
import fr.varyon.trade.VaryonTradePlugin;
import fr.varyon.trade.TradeConfig;
import fr.varyon.trade.TradeVaultBridge;
import fr.varyon.trade.data.*;
import fr.varyon.trade.helpers.TranslationHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.milkbowl.vault2.economy.Economy;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


public class TradePanel extends InteractiveCustomUIPage<EventActionData>
{
    private final List<TradeContentLayoutData> contentLayouts;
    private int currentLayoutIndex = 0;

    private ScheduledFuture<?> securityChecksHandle = null;

    private PlayerConfigData playerConfigData = null;
    private String language = "en-US";

    private TradeData tradeData = null;
    private UUID playerUUID = null;
    private UUID otherPlayerUUID = null;
    private Integer emptySlotsCount = 0;

    private boolean closing = false;
    private boolean quantityDialogBuilt = false;

    private int pickedSlotId = -1;

    public TradePanel(PlayerRef playerRef, TradeData TradeData)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, EventActionData.CODEC);

        TradeData.subscribeRefresh(this::refresh);
        TradeData.subscribeRefreshMoney(this::refreshAllMoneyValues);

        this.tradeData = TradeData;
        playerUUID = playerRef.getUuid();
        otherPlayerUUID = TradeData.playerUUIDs.get(TradeData.playerUUIDs.indexOf(playerUUID) == 0 ? 1 : 0);

        contentLayouts = TradeContentLayoutData.getAllLayouts();
        loadConfig();
    }

    private void loadConfig()
    {
        playerConfigData = PlayerConfigData.getConfigData(playerUUID);

        for (int i = 0; i < contentLayouts.size(); i++)
        {
            if (contentLayouts.get(i).name.equals(playerConfigData.vars.tradePanelLayoutName.getValue()))
            {
                currentLayoutIndex = i;
                break;
            }
        }

        language = playerConfigData.vars.language.getValue();
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref,
                      @NonNullDecl UICommandBuilder uiBuilder,
                      @NonNullDecl UIEventBuilder eventBuilder,
                      @NonNullDecl Store<EntityStore> store)
    {
        //Send currency icon to player
        String currencyIconPath = TradeConfig.get().getFullCurrencyIconPath();
        if (currencyIconPath.length() > 0)
        {
            DynamicImageService.sendLocalToInterfaceSlot(playerRef, currencyIconPath, 0);
        }

        uiBuilder.append("Pages/TradePanel/TradePanel.ui");

        loadContentLayout(uiBuilder, eventBuilder);
        buildQuantityDialog(uiBuilder, eventBuilder);
        buildInventoryList(uiBuilder, eventBuilder, ref, store);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveLeftBtn",
                new EventData().append("ActionId", "POS_UPDATE")
                        .append("movePanelDirection", String.valueOf(-1))
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveRightBtn",
                new EventData().append("ActionId", "POS_UPDATE")
                        .append("movePanelDirection", String.valueOf(1))
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ChangeLayout",
                new EventData().append("ActionId", "CHANGE_LAYOUT")
        );

        refresh();

        playOpenSound();
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store)
    {
        if (securityChecksHandle != null) securityChecksHandle.cancel(true);
        playCloseSound();

        if (!closing)
        {
            closing = true;
            VaryonTradePlugin.cancelTrade(tradeData);
        }
    }

    private void playOpenSound()
    {
        int soundIndex = SoundEvent.getAssetMap().getIndex(TradeConfig.get().getTradePanelOpenSoundId());
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.UI);
    }

    public void playCloseSound()
    {
        int soundIndex = SoundEvent.getAssetMap().getIndex(TradeConfig.get().getTradePanelCloseSoundId());
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.UI);
    }

    private void loadContentLayout(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        //Clears all the content
        uiBuilder.clear("#TradePanel #Content");
        contentLayouts.get(currentLayoutIndex).buildFunction.accept(uiBuilder);
        uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/Elements/BottomButtons.ui");

        //Inits money
        initMoney(uiBuilder, eventBuilder);

        //Updates layout button icon
        int nextLayoutIndex = currentLayoutIndex + 1;
        if (nextLayoutIndex > contentLayouts.size() - 1) nextLayoutIndex = 0;
        uiBuilder.set(contentLayouts.get(currentLayoutIndex).layoutIconSelector + ".Visible", false);
        uiBuilder.set(contentLayouts.get(nextLayoutIndex).layoutIconSelector + ".Visible", true);

        String playerUsername = playerRef.getUsername();
        String otherPlayerUsername = Universe.get().getPlayer(otherPlayerUUID).getUsername();

        uiBuilder.set("#YourUsername.Text", playerUsername);
        uiBuilder.set("#TheirUsername.Text", otherPlayerUsername);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("ActionId", "CANCEL")
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ValidateBtn",
                new EventData().append("ActionId", "VALIDATE")
        );
    }

    private void initMoney(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        Economy economyObj = TradeVaultBridge.getEconomyObj();
        TradeConfig config = TradeConfig.get();
        if (economyObj == null || !config.getIsMoneyTradable()) return;

        uiBuilder.set("#TradePoolYours #MoneyGroup.Visible", true);
        uiBuilder.set("#TradePoolTheirs #MoneyGroup.Visible", true);

        //Apply downloaded currency icons
        if (TradeConfig.get().getFullCurrencyIconPath().length() > 0)
        {
            uiBuilder.set("#TradePoolYours #CurrencyIcon.Background", "Pages/Dynamic/DynamicImage1.png");
            uiBuilder.set("#TradePoolTheirs #CurrencyIcon.Background", "Pages/Dynamic/DynamicImage1.png");
        }

        if (!TradeConfig.get().getIsMoneyDecimal())
        {
            uiBuilder.set("#TradePoolYours #YourMoney.Format.MaxDecimalPlaces", 0);
            uiBuilder.set("#TradePoolYours #YourMoney.Format.Step", 1);
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#TradePoolYours #YourMoney",
                new EventData().append("ActionId", "MONEY_UPDATED").append("@Amount", "#YourMoney.Value")
        );
    }

    private void buildInventoryList(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder, Ref<EntityStore> ref, Store<EntityStore> store)
    {
        ItemContainer playerStorage = store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory();
        
        Map<Integer, ItemStack> playerStacks = new HashMap<>();

        for (short i = 0; i < playerStorage.getCapacity(); i++)
        {
            ItemStack itemStack = playerStorage.getItemStack(i);

            if (itemStack != null)
            {
                playerStacks.put((int)i, itemStack);
            }
            else
            {
                emptySlotsCount++;
            }
        }

        tradeData.playersInventory.put(playerUUID, playerStacks);
    }

    private void buildQuantityDialog(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        if (quantityDialogBuilt) return;
        quantityDialogBuilt = true;

        uiBuilder.append("#TradePanel", "Pages/TradePanel/Elements/QuantityDialog.ui");

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#QtyMinusBtn",
                new EventData().append("ActionId", "QTY_STEP").append("Delta", "-1").append("@Qty", "#QtyField.Value")
        );
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#QtyPlusBtn",
                new EventData().append("ActionId", "QTY_STEP").append("Delta", "1").append("@Qty", "#QtyField.Value")
        );
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#QtyHalfBtn",
                EventData.of("ActionId", "QTY_HALF")
        );
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#QtyMaxBtn",
                EventData.of("ActionId", "QTY_MAX")
        );
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#QtyConfirmBtn",
                new EventData().append("ActionId", "QTY_CONFIRM").append("@Qty", "#QtyField.Value")
        );
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#QtyCancelBtn",
                EventData.of("ActionId", "QTY_CANCEL")
        );
    }

    private void refresh()
    {
        if (securityChecksHandle != null) securityChecksHandle.cancel(true);

        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        updateTradePanelLayout(uiBuilder);
        updateTradePanelPos(uiBuilder, eventBuilder);
        refreshLists(uiBuilder, eventBuilder);
        refreshValidation(uiBuilder, eventBuilder);

        translate(uiBuilder);

        sendUpdate(uiBuilder, eventBuilder, false);

        securityChecksHandle = VaryonTradePlugin.SCHEDULER.scheduleAtFixedRate(this::doSecurityPass, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void doSecurityPass()
    {
        if (tradeData == null) return;

        UICommandBuilder uiBuilder = new UICommandBuilder();
        boolean isDirty = false;

        for (UUID uuid : tradeData.playerUUIDs)
        {
            Economy economyObj = TradeVaultBridge.getEconomyObj();
            if (economyObj != null)
            {
                Float moneyTradeAmount = tradeData.playersMoneyInTrade.get(uuid);
                Float balanceAmount = economyObj.balance("Varyon-Trade", uuid).floatValue();
                Float clampedMoneyTradeAmount = Math.clamp(moneyTradeAmount, 0, balanceAmount);

                if (!moneyTradeAmount.equals(clampedMoneyTradeAmount))
                {
                    tradeData.updateMoney(uuid, clampedMoneyTradeAmount);
                    isDirty = true;
                }
            }
        }

        if (isDirty) sendUpdate(uiBuilder);
    }

    private void refreshAllMoneyValues()
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();

        Float theirMoneyAmount = tradeData.playersMoneyInTrade.get(otherPlayerUUID);
        if (theirMoneyAmount != null)
        {
            String otherPlayerAmountStr = TradeConfig.get().getIsMoneyDecimal() ?
                                          theirMoneyAmount.toString() :
                                          String.valueOf(((int)(float) theirMoneyAmount));

            uiBuilder.set("#TheirMoney.Text", otherPlayerAmountStr);
        }

        Float yourMoneyAmount = tradeData.playersMoneyInTrade.get(playerUUID);
        uiBuilder.set("#YourMoney.Value", yourMoneyAmount);

        sendUpdate(uiBuilder);
    }

    private void translate(UICommandBuilder uiBuilder)
    {
        String language = playerConfigData.vars.language.getValue();

        uiBuilder.set("#PanelTitle.Text", TranslationHelper.getTranslation("ui.trade.title", language));
        uiBuilder.set("#ChangeLayout.TooltipText", TranslationHelper.getTranslation("ui.trade.change_layout", language));
        uiBuilder.set("#MoveLeftBtn.TooltipText", TranslationHelper.getTranslation("ui.trade.move_left", language));
        uiBuilder.set("#MoveRightBtn.TooltipText", TranslationHelper.getTranslation("ui.trade.move_right", language));
        uiBuilder.set("#CancelBtn #Label.Text", TranslationHelper.getTranslation("ui.trade.cancel", language));
        uiBuilder.set("#InventoryTitle.Text", TranslationHelper.getTranslation("ui.trade.inventory", language));

        uiBuilder.set("#QtyDialogTitle.Text", TranslationHelper.getTranslation("ui.trade.qty.title", language));
        uiBuilder.set("#QtyHalfBtn #Label.Text", TranslationHelper.getTranslation("ui.trade.qty.half", language));
        uiBuilder.set("#QtyMaxBtn #Label.Text", TranslationHelper.getTranslation("ui.trade.qty.max", language));
        uiBuilder.set("#QtyConfirmBtn #Label.Text", TranslationHelper.getTranslation("ui.trade.qty.confirm", language));
        uiBuilder.set("#QtyCancelBtn #Label.Text", TranslationHelper.getTranslation("ui.trade.qty.cancel", language));
    }

    private void updateTradePanelLayout(UICommandBuilder uiBuilder)
    {
        //uiBuilder.remove("#TradePools");
    }

    private void updateTradePanelPos(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(contentLayouts.get(currentLayoutIndex).size.x));
        anchor.setHeight(Value.of(contentLayouts.get(currentLayoutIndex).size.y));

        uiBuilder.set("#MoveLeftBtn.Disabled", false);
        uiBuilder.set("#MoveRightBtn.Disabled", false);

        switch (playerConfigData.vars.tradePanelPosition.getValue())
        {
            case -1 -> {
                anchor.setLeft(Value.of(20));
                uiBuilder.set("#MoveLeftBtn.Disabled", true);
            }

            case 1 -> {
                anchor.setRight(Value.of(20));
                uiBuilder.set("#MoveRightBtn.Disabled", true);
            }
        }

        uiBuilder.setObject("#TradePanel.Anchor", anchor);
    }

    private void refreshLists(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        uiBuilder.clear("#Inventory");
        uiBuilder.clear("#YoursList");
        uiBuilder.clear("#TheirsList");

        tradeData.playersInventory.get(playerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#Inventory", stackData, stackSelectorId, "ADD_ITEM", false);
        });

        tradeData.playersItemsPools.get(playerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#YoursList", stackData, stackSelectorId, "REMOVE_ITEM", false);
        });

        tradeData.playersItemsPools.get(otherPlayerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#TheirsList", stackData, stackSelectorId, "", true);
        });
    }

    private void refreshValidation(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        String errorMessage = null;
        boolean isWaiting = tradeData.hasPlayerValidated(playerUUID);

        if (emptySlotsCount + tradeData.playersItemsPools.get(playerUUID).size() < tradeData.playersItemsPools.get(otherPlayerUUID).size())
        {
            errorMessage = TranslationHelper.getTranslation("ui.trade.error.no_space", language);
        }
        else if (tradeData.isTradeEmpty())
        {
            errorMessage = TranslationHelper.getTranslation("ui.trade.error.empty", language);
        }
        else if (isWaiting)
        {
            errorMessage = TranslationHelper.getTranslation("ui.trade.error.waiting_other", language);
        }

        String label = isWaiting ?
                       TranslationHelper.getTranslation("ui.trade.waiting", language) :
                       TranslationHelper.getTranslation("ui.trade.button", language);
        boolean isDisabled = (errorMessage != null);

        updateValidationBtn(uiBuilder, isDisabled, label, errorMessage);
        updateValidationCheckmarks(uiBuilder);
    }

    private void updateValidationBtn(UICommandBuilder uiBuilder, boolean isDisabled, String text, String tooltip)
    {
        uiBuilder.set("#ValidateBtn.Disabled", isDisabled);
        uiBuilder.set("#ValidateBtn #Label.Text", text);

        if (tooltip != null && !tooltip.isEmpty())
        {
            uiBuilder.set("#ValidateBtn.TooltipText", tooltip);
        }
        else
        {
            uiBuilder.setNull("#ValidateBtn.TooltipText");
        }
    }

    private void updateValidationCheckmarks(UICommandBuilder uiBuilder)
    {
        uiBuilder.set("#YourCheckmark.Visible", tradeData.hasPlayerValidated(playerUUID));
        uiBuilder.set("#TheirCheckmark.Visible", tradeData.hasPlayerValidated(otherPlayerUUID));
    }

    private void addItemToList(UICommandBuilder uiBuilder,
                               UIEventBuilder eventBuilder,
                               String listSelector,
                               ItemStack itemStack,
                               int itemStackId,
                               String onClickActionId,
                               Boolean isDisabled)
    {
        if (listSelector == null || listSelector.length() == 0 || itemStack == null || itemStackId < 0) return;

        String groupId = "#Item%s".formatted(itemStackId);

        uiBuilder.appendInline(listSelector, """
            Group %s {
                LayoutMode: Top;
                Anchor: (Horizontal: 1);
                Padding: (Bottom: 2);
            }
        """.formatted(groupId));

        groupId = listSelector + " " + groupId;

        String itemName = TranslationHelper.getItemStackDisplayName(playerRef.getLanguage(), itemStack);
        itemName = itemName != null && itemName.length() > 0 ? itemName : TranslationHelper.getItemStackDisplayName("en-US", itemStack);
        itemName = itemName != null && itemName.length() > 0 ? itemName : "";

        uiBuilder.append(groupId, "Pages/TradePanel/Elements/ItemButton.ui");

        uiBuilder.set(groupId + " #ItemButtonLine.Disabled", isDisabled);

        uiBuilder.set(groupId + " #ItemIcon.ItemId", itemStack.getItemId());
        uiBuilder.set(groupId + " #ItemName.Text", itemName);
        uiBuilder.set(groupId + " #ItemQte.Text", "x" + itemStack.getQuantity());

        if (onClickActionId != null && !onClickActionId.isEmpty())
        {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    groupId + " #ItemButtonLine",
                    new EventData().append("ActionId", onClickActionId)
                            .append("stackId", String.valueOf(itemStackId))
            );
        }

        if ("ADD_ITEM".equals(onClickActionId) && itemStack.getQuantity() > 1)
        {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.RightClicking,
                    groupId + " #ItemButtonLine",
                    new EventData().append("ActionId", "OPEN_QTY")
                            .append("stackId", String.valueOf(itemStackId))
            );
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String rawData)
    {
        JsonObject jsonObj = JsonParser.parseString(rawData).getAsJsonObject();

        String actionId = jsonObj.get("ActionId").getAsString();
        switch (actionId)
        {
            case "CHANGE_LAYOUT" -> onChangeLayoutClicked();
            case "POS_UPDATE" -> onChangePagePositionClicked(jsonObj.get("movePanelDirection").getAsShort());
            case "ADD_ITEM" -> onAddItemClicked(jsonObj.get("stackId").getAsInt());
            case "REMOVE_ITEM" -> onRemoveItemClicked(jsonObj.get("stackId").getAsInt());
            case "OPEN_QTY" -> onOpenQuantityDialog(jsonObj.get("stackId").getAsInt());
            case "QTY_STEP" -> onQuantityStep(jsonObj.get("Delta").getAsInt(), jsonObj.get("@Qty").getAsString());
            case "QTY_HALF" -> onQuantityPreset(true);
            case "QTY_MAX" -> onQuantityPreset(false);
            case "QTY_CONFIRM" -> onQuantityConfirm(jsonObj.get("@Qty").getAsString());
            case "QTY_CANCEL" -> onQuantityCancel();
            case "MONEY_UPDATED" -> onMoneyAmountUpdated(jsonObj.get("@Amount").getAsString());
            case "CANCEL" -> onCancelClicked();
            case "VALIDATE" -> onValidateClicked();
        }

        sendUpdate();
    }

    private void onChangeLayoutClicked()
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        currentLayoutIndex++;
        if (currentLayoutIndex > contentLayouts.size() - 1) currentLayoutIndex = 0;
        playerConfigData.vars.tradePanelLayoutName.setValue(contentLayouts.get(currentLayoutIndex).name);

        loadContentLayout(uiBuilder, eventBuilder);

        sendUpdate(uiBuilder, eventBuilder, false);
        refresh();
    }

    private void onChangePagePositionClicked(int movePanelDirection)
    {
        int currentPos = playerConfigData.vars.tradePanelPosition.getValue();
        playerConfigData.vars.tradePanelPosition.setValue(currentPos + movePanelDirection);
        refresh();
    }

    private void onAddItemClicked(int slotId)
    {
        ItemStack invStack = tradeData.playersInventory.get(playerUUID).get(slotId);
        if (invStack == null) return;

        moveToPool(slotId, invStack.getQuantity());

        tradeData.resetValidation();
    }

    private void onRemoveItemClicked(int slotId)
    {
        Map<Integer, ItemStack> playerInventory = tradeData.playersInventory.get(playerUUID);
        Map<Integer, ItemStack> playerPool = tradeData.playersItemsPools.get(playerUUID);

        ItemStack poolStack = playerPool.remove(slotId);
        if (poolStack == null) return;

        ItemStack invStack = playerInventory.get(slotId);
        int merged = poolStack.getQuantity() + (invStack != null ? invStack.getQuantity() : 0);
        playerInventory.put(slotId, poolStack.withQuantity(merged));

        tradeData.resetValidation();
    }

    private void moveToPool(int slotId, int amount)
    {
        Map<Integer, ItemStack> playerInventory = tradeData.playersInventory.get(playerUUID);
        Map<Integer, ItemStack> playerPool = tradeData.playersItemsPools.get(playerUUID);

        ItemStack invStack = playerInventory.get(slotId);
        if (invStack == null) return;

        int move = Math.clamp(amount, 1, invStack.getQuantity());

        int remaining = invStack.getQuantity() - move;
        if (remaining > 0) playerInventory.put(slotId, invStack.withQuantity(remaining));
        else playerInventory.remove(slotId);

        ItemStack existingPool = playerPool.get(slotId);
        int pooled = move + (existingPool != null ? existingPool.getQuantity() : 0);
        playerPool.put(slotId, invStack.withQuantity(pooled));
    }

    private void onOpenQuantityDialog(int slotId)
    {
        ItemStack invStack = tradeData.playersInventory.get(playerUUID).get(slotId);
        if (invStack == null || invStack.getQuantity() <= 1) return;

        pickedSlotId = slotId;
        int maxQty = invStack.getQuantity();

        String itemName = TranslationHelper.getItemStackDisplayName(playerRef.getLanguage(), invStack);
        if (itemName == null || itemName.isEmpty()) itemName = TranslationHelper.getItemStackDisplayName("en-US", invStack);
        if (itemName == null) itemName = "";

        UICommandBuilder uiBuilder = new UICommandBuilder();
        uiBuilder.set("#QtyItemName.Text", itemName);
        uiBuilder.set("#QtyField.Format.MaxValue", maxQty);
        uiBuilder.set("#QtyField.Value", (float) maxQty);
        uiBuilder.set("#QuantityDialogOverlay.Visible", true);

        sendUpdate(uiBuilder);
    }

    private int pickedInventoryQty()
    {
        ItemStack invStack = tradeData.playersInventory.get(playerUUID).get(pickedSlotId);
        return invStack != null ? invStack.getQuantity() : 0;
    }

    private void setQuantityField(int value)
    {
        int max = pickedInventoryQty();
        if (max <= 0) { onQuantityCancel(); return; }

        UICommandBuilder uiBuilder = new UICommandBuilder();
        uiBuilder.set("#QtyField.Value", (float) Math.clamp(value, 1, max));
        sendUpdate(uiBuilder);
    }

    private void onQuantityStep(int delta, String rawQty)
    {
        if (pickedSlotId < 0) return;

        int current;
        try { current = (int) Float.parseFloat(rawQty); }
        catch (Exception e) { current = 1; }

        setQuantityField(current + delta);
    }

    private void onQuantityPreset(boolean half)
    {
        if (pickedSlotId < 0) return;

        int max = pickedInventoryQty();
        if (max <= 0) { onQuantityCancel(); return; }

        setQuantityField(half ? Math.max(1, (max + 1) / 2) : max);
    }

    private void onQuantityConfirm(String rawQty)
    {
        if (pickedSlotId < 0) return;

        int qty;
        try { qty = (int) Float.parseFloat(rawQty); }
        catch (Exception e) { qty = 1; }

        ItemStack invStack = tradeData.playersInventory.get(playerUUID).get(pickedSlotId);
        if (invStack != null)
        {
            qty = Math.clamp(qty, 1, invStack.getQuantity());
            moveToPool(pickedSlotId, qty);
            tradeData.resetValidation();
        }

        onQuantityCancel();
    }

    private void onQuantityCancel()
    {
        pickedSlotId = -1;

        UICommandBuilder uiBuilder = new UICommandBuilder();
        uiBuilder.set("#QuantityDialogOverlay.Visible", false);
        sendUpdate(uiBuilder);
    }

    private void onMoneyAmountUpdated(String strAmount)
    {
        Float amount = null;
        try
        {
            amount = Float.parseFloat(strAmount);
        }
        catch (Exception e) {}


        if (amount != null)
        {
            tradeData.updateMoney(playerUUID, amount);
            doSecurityPass();
        }
    }

    private void onCancelClicked()
    {
        cancelTrade();
    }

    private void onValidateClicked()
    {
        tradeData.addValidation(playerUUID, tradeData);
    }

    private void cancelTrade()
    {
        VaryonTradePlugin.cancelTrade(tradeData);
    }
}

