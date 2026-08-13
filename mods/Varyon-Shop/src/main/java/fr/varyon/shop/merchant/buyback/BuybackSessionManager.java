package fr.varyon.shop.merchant.buyback;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.config.BuybackPriceRepository;
import fr.varyon.shop.config.ShopSettings;
import fr.varyon.shop.economy.VaultEconomyBridge;
import fr.varyon.shop.ui.BuybackGeneralPage;
import fr.varyon.shop.util.EntityApiCompat;

/**
 * Opens the "Racheteur General" custom UI page for a player (deposit grid + player inventory +
 * live summary + explicit "Vendre tout" button). No physical block, no native container window.
 *
 * Unrestricted (general) merchants pay ShopSettings.generalBuybackRate() of the listed price;
 * category-restricted "profession" merchants always pay 100%.
 */
public final class BuybackSessionManager {

    private final BuybackPriceRepository priceRepository;
    private final VaultEconomyBridge economyBridge;
    private final ShopSettings shopSettings;

    public BuybackSessionManager(BuybackPriceRepository priceRepository, VaultEconomyBridge economyBridge, ShopSettings shopSettings) {
        this.priceRepository = priceRepository;
        this.economyBridge = economyBridge;
        this.shopSettings = shopSettings;
    }

    /** Opens the buyback deposit window for a player. */
    public boolean open(PlayerRef playerRef) {
        return open(playerRef, null, null, false);
    }

    /** Opens the buyback deposit window for a player, titled after the given merchant NPC name. */
    public boolean open(PlayerRef playerRef, String merchantName) {
        return open(playerRef, merchantName, null, false);
    }

    /**
     * Opens the buyback deposit window for a player, titled after the given merchant NPC name
     * and restricted to the given item category (null/blank means unrestricted). fullRate true
     * pays 100% of the listed price (profession merchant); false applies the general rate.
     */
    public boolean open(PlayerRef playerRef, String merchantName, String categoryFilter, boolean fullRate) {
        if (playerRef == null) {
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

        double baseRate = fullRate ? 1.0 : Math.max(0.0, shopSettings.generalBuybackRate());
        double bonus = Math.max(0.0, shopSettings.buybackBonus());
        double rate = baseRate * (1.0 + bonus);
        try {
            BuybackGeneralPage page = new BuybackGeneralPage(priceRepository, economyBridge, playerRef, merchantName, categoryFilter, rate);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage) page);
            return true;
        } catch (Throwable t) {
            System.err.println("[Varyon-Shop] Failed to open buyback general page: " + t);
            t.printStackTrace();
            return false;
        }
    }
}
