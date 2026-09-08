package fr.varyon.trade.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.varyon.trade.VaryonTradePlugin;
import fr.varyon.trade.TradeConfig;
import fr.varyon.trade.helpers.TranslationHelper;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;

public class TradeData
{
    public final List<UUID> playerUUIDs = new ArrayList<>();
    public final Map<UUID, Map<Integer, ItemStack>> playersInventory = new HashMap<>();
    public final Map<UUID, Map<Integer, ItemStack>> playersItemsPools = new HashMap<>();
    public final Map<UUID, Float> playersMoneyInTrade = new HashMap<>();

    private final List<Runnable> refreshFunctions = new ArrayList<>();
    private final List<Runnable> moneyRefreshFunctions = new ArrayList<>();
    private final List<UUID> uuidsValidation = new ArrayList<>();

    private final Map<UUID, ItemContainer> transitionContainers = new HashMap<>();
    private final Map<UUID, ItemContainer> playersInventoryContainers = new HashMap<>();

    public TradeData(UUID player1UUID, UUID player2UUID)
    {
        playersMoneyInTrade.put(player1UUID, 0.0f);
        playersMoneyInTrade.put(player2UUID, 0.0f);
    }

    public boolean isTradeEmpty()
    {
        // VÃ©rification des objets
        for (Map<Integer, ItemStack> pool : playersItemsPools.values())
        {
            if (pool != null && !pool.isEmpty())
            {
                return false;
            }
        }

        // VÃ©rification de l'argent
        for (Float amount : playersMoneyInTrade.values())
        {
            if (amount != null && amount > 0.0f)
            {
                return false;
            }
        }

        return true;
    }

    public void addValidation(UUID playerUUID, TradeData tradeData)
    {
        uuidsValidation.add(playerUUID);

        if (tradeData.isValidated())
        {
            VaryonTradePlugin.finalizeTrade(tradeData);
        }

        refresh();
    }

    public void updateMoney(UUID playerUUID, float newAmount)
    {
        playersMoneyInTrade.put(playerUUID, newAmount);
        resetValidation();
        refreshMoney();
    }

    public Boolean hasPlayerValidated(UUID playerUUID)
    {
        return uuidsValidation.contains(playerUUID);
    }

    public void resetValidation()
    {
        uuidsValidation.clear();
        refresh();
    }

    public Boolean isValidated()
    {
        return uuidsValidation.size() >= playerUUIDs.size();
    }

    public void subscribeRefreshMoney(Runnable refreshFunction)
    {
        if (!moneyRefreshFunctions.contains(refreshFunction))
        {
            moneyRefreshFunctions.add(refreshFunction);
        }
    }

    public void refreshMoney()
    {
        for (Runnable refreshFunction : moneyRefreshFunctions)
        {
            refreshFunction.run();
        }
    }

    public void subscribeRefresh(Runnable refreshFunction)
    {
        if (!refreshFunctions.contains(refreshFunction))
        {
            refreshFunctions.add(refreshFunction);
        }
    }

    public void refresh()
    {
        for (Runnable refreshFunction : refreshFunctions)
        {
            refreshFunction.run();
        }
    }

    public void addItemStacksToTransitionContainer(UUID playerUUID, ItemContainer inventoryContainer, List<ItemStack> itemStacks)
    {
        if (!playersInventoryContainers.containsKey(playerUUID))
        {
            playersInventoryContainers.put(playerUUID, inventoryContainer);
            transitionContainers.put(playerUUID, SimpleItemContainer.getNewContainer((short)itemStacks.size()));
        }

        ItemContainer itemContainer = transitionContainers.get(playerUUID);
        itemContainer.addItemStacks(itemStacks);

        if (transitionContainers.size() >= playerUUIDs.size()) distribute();
    }

    private void distribute()
    {
        if (playersInventoryContainers.size() < 2 || transitionContainers.size() < 2) return;

        List<ItemContainer> inventoryContainersList = playersInventoryContainers.values().stream().toList();
        List<ItemContainer> transitionContainersList = transitionContainers.values().stream().toList();

        transitionContainersList.get(0).moveAllItemStacksTo(inventoryContainersList.get(1));
        transitionContainersList.get(1).moveAllItemStacksTo(inventoryContainersList.get(0));

        VaryonTradePlugin.onItemsTraded(this);
    }
}

