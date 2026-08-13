package fr.varyon.shop;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.shop.commands.BuybackCommand;
import fr.varyon.shop.commands.VShopCommand;
import fr.varyon.shop.config.BindWandManager;
import fr.varyon.shop.config.BuybackPriceRepository;
import fr.varyon.shop.config.MerchantRegistry;
import fr.varyon.shop.config.ShopCatalogRepository;
import fr.varyon.shop.config.ShopPurchaseTracker;
import fr.varyon.shop.config.ShopRotationState;
import fr.varyon.shop.config.ShopSettings;
import fr.varyon.shop.economy.VaultEconomyBridge;
import fr.varyon.shop.integration.DenizensBridge;
import fr.varyon.shop.merchant.buyback.BuybackSessionManager;
import fr.varyon.shop.merchant.market.MarketSessionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;
import java.util.UUID;

public final class VaryonShopPlugin extends JavaPlugin {
    private static final String MOD_FOLDER = "Varyon_Shop";

    private Path modDataPath;
    private BuybackPriceRepository buybackPriceRepository;
    private ShopCatalogRepository shopCatalogRepository;
    private ShopRotationState shopRotationState;
    private ShopSettings shopSettings;
    private ShopPurchaseTracker shopPurchaseTracker;
    private MerchantRegistry merchantRegistry;
    private BindWandManager bindWandManager;
    private VaultEconomyBridge economyBridge;
    private DenizensBridge denizensBridge;
    private BuybackSessionManager buybackSessionManager;
    private MarketSessionManager marketSessionManager;

    public VaryonShopPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        modDataPath = Paths.get("mods", MOD_FOLDER);
        try {
            Files.createDirectories(modDataPath, new FileAttribute[0]);
        } catch (Exception ignored) {
        }

        buybackPriceRepository = new BuybackPriceRepository(modDataPath.resolve("buyback_prices.json"));
        buybackPriceRepository.load();

        shopCatalogRepository = new ShopCatalogRepository(modDataPath.resolve("shops"));
        shopCatalogRepository.load();

        shopRotationState = new ShopRotationState(modDataPath.resolve("shop_rotation_state.json"));
        shopRotationState.load();

        shopSettings = new ShopSettings(modDataPath.resolve("shop_settings.json"));
        shopSettings.load();

        shopPurchaseTracker = new ShopPurchaseTracker(modDataPath.resolve("shop_purchases.json"));
        shopPurchaseTracker.load();
        shopRotationState.setPurchaseTracker(shopPurchaseTracker);

        merchantRegistry = new MerchantRegistry(modDataPath.resolve("merchant_bindings.json"));
        merchantRegistry.load();

        bindWandManager = new BindWandManager();

        economyBridge = VaultEconomyBridge.connect();
        if (!economyBridge.isAvailable()) {
            System.err.println("[Varyon-Shop] VaultUnlocked not found — merchants will be unable to pay/charge players.");
        }

        denizensBridge = DenizensBridge.connect();
        if (!denizensBridge.isAvailable()) {
            System.out.println("[Varyon-Shop] QuestLinesDenizens not detected yet — /vshop bind will report unavailable until it loads.");
        }

        buybackSessionManager = new BuybackSessionManager(buybackPriceRepository, economyBridge, shopSettings);
        marketSessionManager = new MarketSessionManager(shopCatalogRepository, shopRotationState, economyBridge, buybackPriceRepository, shopPurchaseTracker, shopSettings);

        CommandManager.get().register(new BuybackCommand(buybackSessionManager));
        CommandManager.get().register(new VShopCommand(this, merchantRegistry, bindWandManager));

        System.out.println("[Varyon-Shop] Setup complete.");
    }

    @Override
    public void start() {
        if (!denizensBridge.isAvailable()) {
            denizensBridge = DenizensBridge.connect();
        }
        if (denizensBridge.isAvailable()) {
            denizensBridge.addInteractListener(this::handleDenizenInteract);
            System.out.println("[Varyon-Shop] QuestLinesDenizens detected — NPC merchants enabled.");
        } else {
            System.out.println("[Varyon-Shop] QuestLinesDenizens not detected — NPC merchants disabled, /racheteur still works.");
        }
        System.out.println("[Varyon-Shop] Started.");
    }

    private void handleDenizenInteract(UUID denizenId, PlayerRef playerRef) {
        if (denizenId == null || playerRef == null) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();
        Optional<MerchantRegistry.Binding> armedRequest = playerUuid == null ? null : bindWandManager.consume(playerUuid);
        if (armedRequest != null) {
            String name = denizensBridge.getName(denizenId);
            String displayName = name == null ? denizenId.toString() : name;
            if (armedRequest.isPresent()) {
                MerchantRegistry.Binding binding = armedRequest.get();
                merchantRegistry.bind(denizenId, binding.type(), binding.category(), binding.shopId());
                String suffix = binding.category() != null ? " (categorie: " + binding.category() + ")"
                        : binding.shopId() != null ? " (boutique: " + binding.shopId() + ")" : "";
                playerRef.sendMessage(Message.raw("Varyon-Shop: " + displayName + " lie au marchand " + binding.type().name() + suffix + "."));
            } else {
                merchantRegistry.unbind(denizenId);
                playerRef.sendMessage(Message.raw("Varyon-Shop: " + displayName + " delie de tout marchand."));
            }
            return;
        }

        MerchantRegistry.Binding binding = merchantRegistry.bindingOf(denizenId);
        if (binding == null) {
            return;
        }
        String merchantName = denizensBridge.getName(denizenId);
        switch (binding.type()) {
            case BUYBACK_GENERAL -> buybackSessionManager.open(playerRef, merchantName, binding.category(), false);
            case BUYBACK_PROFESSION -> buybackSessionManager.open(playerRef, merchantName, binding.category(), true);
            case MARKET_SHOP -> marketSessionManager.open(playerRef, binding.shopId());
            default -> { }
        }
    }

    @Override
    protected void shutdown() {
        if (buybackPriceRepository != null) {
            buybackPriceRepository.save();
        }
        if (merchantRegistry != null) {
            merchantRegistry.save();
        }
        if (shopRotationState != null) {
            shopRotationState.save();
        }
        System.out.println("[Varyon-Shop] Stopped.");
    }

    public BuybackPriceRepository getBuybackPriceRepository() {
        return buybackPriceRepository;
    }

    public ShopCatalogRepository getShopCatalogRepository() {
        return shopCatalogRepository;
    }

    public ShopRotationState getShopRotationState() {
        return shopRotationState;
    }

    public ShopSettings getShopSettings() {
        return shopSettings;
    }

    public ShopPurchaseTracker getShopPurchaseTracker() {
        return shopPurchaseTracker;
    }

    public MerchantRegistry getMerchantRegistry() {
        return merchantRegistry;
    }

    public BindWandManager getBindWandManager() {
        return bindWandManager;
    }

    public VaultEconomyBridge getEconomyBridge() {
        return economyBridge;
    }

    public DenizensBridge getDenizensBridge() {
        return denizensBridge;
    }

    public BuybackSessionManager getBuybackSessionManager() {
        return buybackSessionManager;
    }

    public MarketSessionManager getMarketSessionManager() {
        return marketSessionManager;
    }

    /** Reloads buyback prices, shop catalogs, and merchant bindings from disk without restarting the server. */
    public void reloadConfig() {
        buybackPriceRepository.load();
        shopCatalogRepository.load();
        merchantRegistry.load();
        shopSettings.load();
    }
}
