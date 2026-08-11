package fr.varyon.shop.merchant.buyback;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.config.BuybackPriceRepository;
import fr.varyon.shop.economy.VaultEconomyBridge;
import fr.varyon.shop.ui.BuybackGeneralPage;
import fr.varyon.shop.util.EntityApiCompat;

/**
 * Opens the "Racheteur General" custom UI page for a player (deposit grid + player inventory +
 * live summary + explicit "Vendre tout" button). No physical block, no native container window.
 */
public final class BuybackSessionManager {

    private final BuybackPriceRepository priceRepository;
    private final VaultEconomyBridge economyBridge;

    public BuybackSessionManager(BuybackPriceRepository priceRepository, VaultEconomyBridge economyBridge) {
        this.priceRepository = priceRepository;
        this.economyBridge = economyBridge;
    }

    /** Opens the buyback deposit window for a player. */
    public boolean open(PlayerRef playerRef) {
        return open(playerRef, null);
    }

    /** Opens the buyback deposit window for a player, titled after the given merchant NPC name. */
    public boolean open(PlayerRef playerRef, String merchantName) {
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

        try {
            BuybackGeneralPage page = new BuybackGeneralPage(priceRepository, economyBridge, playerRef, merchantName);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage) page);
            return true;
        } catch (Throwable t) {
            System.err.println("[Varyon-Shop] Failed to open buyback general page: " + t);
            t.printStackTrace();
            return false;
        }
    }
}
