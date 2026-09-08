package fr.varyon.trade;

import fr.varyon.trade.commands.TradeAdminCommand;
import fr.varyon.trade.commands.TradeCommand;
import fr.varyon.trade.customEvents.DamageSystem;
import fr.varyon.trade.customEvents.DeathEvent;
import fr.varyon.trade.data.PlayerConfigData;
import fr.varyon.trade.data.TradeData;
import fr.varyon.trade.data.NotificationData;
import fr.varyon.trade.helpers.NotificationHelper;
import fr.varyon.trade.helpers.TranslationHelper;
import fr.varyon.trade.pages.Settings;
import fr.varyon.trade.pages.TradeDialog;
import fr.varyon.trade.pages.TradePanel;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.milkbowl.vault2.economy.Economy;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;


public class VaryonTradePlugin extends JavaPlugin
{
    public final static ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    public final static List<TradeData> ON_GOING_TRADES = new ArrayList<>();

    private final EventRegistry events = getEventRegistry();


    public VaryonTradePlugin(@NonNullDecl JavaPluginInit init)
    {
        super(init);
    }

    @Override
    protected void setup()
    {
        TranslationHelper.loadAllTranslations();

        getCommandRegistry().registerCommand(new TradeCommand());
        getCommandRegistry().registerCommand(new TradeAdminCommand());

        events.register(PlayerDisconnectEvent.class, event -> cancelPlayerTrades(event.getPlayerRef()));
        getEntityStoreRegistry().registerSystem(new DeathEvent());
        getEntityStoreRegistry().registerSystem(new DamageSystem());

        SCHEDULER.scheduleAtFixedRate(() -> {
            doSecurityPass();
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutdown()
    {
        SCHEDULER.shutdownNow();
    }

    private static void doSecurityPass()
    {
        List<Vector3d> playerPositions = new ArrayList<>();

        for (TradeData trade : ON_GOING_TRADES)
        {
            boolean hasFromFar = false;
            for (UUID playerUUID : trade.playerUUIDs)
            {
                PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
                playerPositions.add(playerRef.getTransform().getPosition());
                if (PermissionsModule.get().hasPermission(playerUUID, TradeConfig.get().getFullPermFromFar())) hasFromFar = true;
            }

            if (!hasFromFar && !areAllWithinDistance(playerPositions)) cancelTrade(trade);

            playerPositions.clear();
        }
    }

    private static boolean areAllWithinDistance(List<Vector3d> positions)
    {
        double maxDistanceSqr = TradeConfig.get().getTradeMaxDistance();
        maxDistanceSqr *= maxDistanceSqr;

        for (int i = 0; i < positions.size() - 1; i++)
        {
            for (int j = i + 1; j < positions.size(); j++)
            {
                if (positions.get(i).distanceSquared(positions.get(j)) > maxDistanceSqr)
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static void askForTrade(PlayerRef senderPlayerRef, PlayerRef targetPlayerRef)
    {
        Universe.get().getWorld(targetPlayerRef.getWorldUuid()).execute(() -> {
            Ref<EntityStore> playerStoreRef = targetPlayerRef.getReference();
            Store<EntityStore> playerEntityStore = playerStoreRef.getStore();
            Player player = playerEntityStore.getComponent(playerStoreRef, Player.getComponentType());

            PlayerConfigData senderPlayerConfigData = PlayerConfigData.getConfigData(senderPlayerRef.getUuid());
            senderPlayerConfigData.vars.lastTradeRequestEpochTime.setValue(new PlayerConfigData.LastTradeRequestSent(
                    Instant.now().getEpochSecond(),
                    targetPlayerRef.getUuid().toString()
            ));

            TradeDialog tradeDialog = new TradeDialog(senderPlayerRef, targetPlayerRef);
            player.getPageManager().openCustomPage(playerStoreRef, playerEntityStore, tradeDialog);
        });
    }

    public static void openTrade(PlayerRef senderPlayerRef, PlayerRef targetPlayerRef)
    {
        UUID playerUuid = senderPlayerRef.getUuid();
        UUID player2Uuid = targetPlayerRef.getUuid();

        TradeData TradeData = new TradeData(playerUuid, player2Uuid);

        TradeData.playerUUIDs.add(playerUuid);
        TradeData.playersInventory.put(playerUuid, new HashMap<>());
        TradeData.playersItemsPools.put(playerUuid, new HashMap<>());

        TradeData.playerUUIDs.add(player2Uuid);
        TradeData.playersInventory.put(player2Uuid, new HashMap<>());
        TradeData.playersItemsPools.put(player2Uuid, new HashMap<>());

        VaryonTradePlugin.ON_GOING_TRADES.add(TradeData);

        openPlayerTrade(senderPlayerRef, TradeData);
        if(!playerUuid.equals(player2Uuid)) openPlayerTrade(targetPlayerRef, TradeData);
    }

    private static void openPlayerTrade(PlayerRef playerRef, TradeData tradeData)
    {
        Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
            Ref<EntityStore> playerStoreRef = playerRef.getReference();
            Store<EntityStore> playerEntityStore = playerStoreRef.getStore();
            Player player = playerEntityStore.getComponent(playerStoreRef, Player.getComponentType());

            TradePanel playerTradePanel = new TradePanel(playerRef, tradeData);
            player.getPageManager().openCustomPage(playerStoreRef, playerEntityStore, playerTradePanel);
        });
    }

    public static void cancelPlayerTrades(PlayerRef playerRef)
    {
        for (TradeData tradeData : ON_GOING_TRADES)
        {
            if (tradeData.playerUUIDs.contains(playerRef.getUuid()))
            {
                cancelTrade(tradeData);
            }
        }
    }

    public static void cancelTrade(TradeData tradeData)
    {
        closeTrade(tradeData, false);
    }

    public static void closeTrade(TradeData tradeData, boolean success)
    {
        if (tradeData != null)
        {
            for (UUID playerUUID : tradeData.playerUUIDs)
            {
                PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
                if (playerRef == null) continue;

                Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
                    Ref<EntityStore> storeRef = playerRef.getReference();
                    Store<EntityStore> store = storeRef.getStore();
                    Player player = store.getComponent(storeRef, Player.getComponentType());

                    String language = PlayerConfigData.getConfigData(playerUUID).vars.language.getValue();
                    NotificationHelper.send(playerRef, new NotificationData(
                            TranslationHelper.getTranslation("notification.trade.title", language),
                            TranslationHelper.getTranslation(success ?
                                                             "notification.trade.success" :
                                                             "notification.trade.cancelled", language),
                            TradeConfig.get().getNotificationIconId(),
                            "#ffffff", success ? "#2ae917" : "#f57482"
                    ));

                    player.getPageManager().setPage(storeRef, store, Page.None);
                });
            }

            VaryonTradePlugin.ON_GOING_TRADES.remove(tradeData);
        }
    }

    public static void finalizeTrade(TradeData tradeData)
    {
        for (UUID playerUUID : tradeData.playerUUIDs)
        {
            PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
            Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
                Map<Integer, ItemStack> playerAddedStacks = tradeData.playersItemsPools.get(playerUUID);
                Ref<EntityStore> ref = playerRef.getReference();
                Store<EntityStore> store = ref.getStore();
                Player player = store.getComponent(ref, Player.getComponentType());
                ItemContainer itemContainer = store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory();
                List<ItemStack> playerItemStacks = playerAddedStacks.values().stream().toList();
                itemContainer.removeItemStacks(playerItemStacks);
                tradeData.addItemStacksToTransitionContainer(playerUUID, itemContainer, playerAddedStacks.values().stream().toList());
            });
        }
    }

    public static void onItemsTraded(TradeData tradeData)
    {
        VaryonTradePlugin.finalizeMoneyTrade(tradeData);
        VaryonTradePlugin.closeTrade(tradeData, true);
    }

    private static void finalizeMoneyTrade(TradeData tradeData)
    {
        Economy economyObj = TradeVaultBridge.getEconomyObj();
        if (economyObj == null) return;

        List<UUID> uuids = tradeData.playersMoneyInTrade.keySet().stream().toList();

        UUID player1UUID = uuids.get(0);
        BigDecimal player1Amount = BigDecimal.valueOf(tradeData.playersMoneyInTrade.get(player1UUID));

        UUID player2UUID = uuids.get(1);
        BigDecimal player2Amount = BigDecimal.valueOf(tradeData.playersMoneyInTrade.get(player2UUID));

        boolean player1HasEnoughMoney = economyObj.has("Varyon-Trade", player1UUID, player1Amount);
        boolean player2HasEnoughMoney = economyObj.has("Varyon-Trade", player2UUID, player2Amount);
        if (!player1HasEnoughMoney || !player2HasEnoughMoney)
        {
            String player1Lang = PlayerConfigData.getConfigData(player1UUID).vars.language.getValue();
            String player2Lang = PlayerConfigData.getConfigData(player2UUID).vars.language.getValue();

            Message player1Msg = Message.raw(TranslationHelper.getTranslation(
                    player1HasEnoughMoney ? "message.trade.other_no_money" : "message.trade.you_no_money",
                    player1Lang
            ));

            Message player2Msg = Message.raw(TranslationHelper.getTranslation(
                    player2HasEnoughMoney ? "message.trade.other_no_money" : "message.trade.you_no_money",
                    player2Lang
            ));

            PlayerRef p1 = Universe.get().getPlayer(player1UUID);
            if (p1 != null) p1.sendMessage(player1Msg);

            PlayerRef p2 = Universe.get().getPlayer(player2UUID);
            if (p2 != null) p2.sendMessage(player2Msg);

            return;
        }

        economyObj.withdraw("Varyon-Trade", player1UUID, player1Amount);
        economyObj.deposit("Varyon-Trade", player2UUID, player1Amount);

        economyObj.withdraw("Varyon-Trade", player2UUID, player2Amount);
        economyObj.deposit("Varyon-Trade", player1UUID, player2Amount);
    }
    
    public static Boolean isPlayerTrading(PlayerRef playerRef)
    {
        UUID playerUuid = playerRef.getUuid();
        for (TradeData tradeData : ON_GOING_TRADES)
        {
            if (tradeData.playerUUIDs.contains(playerUuid)) return true;
        }

        return false;
    }

    public static void openSettings(PlayerRef playerRef)
    {
        Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
            Ref<EntityStore> playerStoreRef = playerRef.getReference();
            Store<EntityStore> playerEntityStore = playerStoreRef.getStore();
            Player player = playerEntityStore.getComponent(playerStoreRef, Player.getComponentType());

            Settings playerTradePanel = new Settings(playerRef);
            player.getPageManager().openCustomPage(playerStoreRef, playerEntityStore, playerTradePanel);
        });
    }
}

