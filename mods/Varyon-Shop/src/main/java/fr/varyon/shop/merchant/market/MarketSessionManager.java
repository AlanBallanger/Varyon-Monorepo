package fr.varyon.shop.merchant.market;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.config.BuybackPriceRepository;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.config.ShopCatalogRepository;
import fr.varyon.shop.config.ShopPurchaseTracker;
import fr.varyon.shop.config.ShopRotationState;
import fr.varyon.shop.config.ShopSettings;
import fr.varyon.shop.economy.VaultEconomyBridge;
import fr.varyon.shop.ui.MarketPage;
import fr.varyon.shop.util.EntityApiCompat;
import fr.varyon.shop.util.WorldDayUtil;

/** Opens the purchase window for a given shop catalog, applying its current daily rotation. */
public final class MarketSessionManager {

    private final ShopCatalogRepository catalogRepository;
    private final ShopRotationState rotationState;
    private final VaultEconomyBridge economyBridge;
    private final BuybackPriceRepository priceRepository;
    private final ShopPurchaseTracker purchaseTracker;
    private final ShopSettings shopSettings;

    public MarketSessionManager(ShopCatalogRepository catalogRepository, ShopRotationState rotationState,
                                 VaultEconomyBridge economyBridge, BuybackPriceRepository priceRepository,
                                 ShopPurchaseTracker purchaseTracker, ShopSettings shopSettings) {
        this.catalogRepository = catalogRepository;
        this.rotationState = rotationState;
        this.economyBridge = economyBridge;
        this.priceRepository = priceRepository;
        this.purchaseTracker = purchaseTracker;
        this.shopSettings = shopSettings;
    }

    public boolean open(PlayerRef playerRef, String shopId) {
        if (playerRef == null || shopId == null) {
            return false;
        }
        ShopCatalog catalog = catalogRepository.get(shopId).orElse(null);
        if (catalog == null) {
            playerRef.sendMessage(Message.raw("Varyon-Shop: catalogue '" + shopId + "' introuvable."));
            return false;
        }
        Player player = EntityApiCompat.getPlayer(playerRef);
        if (player == null || player.getPageManager() == null) {
            return false;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return false;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return false;
        }

        String worldUuid = String.valueOf(playerRef.getWorldUuid());
        int currentDay = resolveCurrentDay(playerRef);
        ShopRotationState.RotationEntry rotation = rotationState.ensureCurrent(shopId, worldUuid, currentDay, catalog);

        try {
            MarketPage page = new MarketPage(catalog, shopId, economyBridge, priceRepository, purchaseTracker, playerRef, rotation, shopSettings.purchaseBonus());
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage) page);
            return true;
        } catch (Throwable t) {
            System.err.println("[Varyon-Shop] Failed to open market page: " + t);
            t.printStackTrace();
            return false;
        }
    }

    private int resolveCurrentDay(PlayerRef playerRef) {
        try {
            java.util.UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) {
                return 0;
            }
            World world = Universe.get().getWorld(worldUuid);
            int day = WorldDayUtil.currentDayOfYear(world);
            return day < 0 ? 0 : day;
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
