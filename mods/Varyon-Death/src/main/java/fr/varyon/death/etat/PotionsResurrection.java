package fr.varyon.death.etat;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Recherche et consommation des potions de resurrection dans l'inventaire d'un joueur.
 *
 * <p>La potion n'a pas besoin d'etre tenue en main : elle est cherchee dans tout l'inventaire
 * (hotbar puis sac), comme n'importe quel objet de fabrication ou de troc du serveur.
 */
public final class PotionsResurrection {

    private PotionsResurrection() {}

    /** Meilleur tier de potion de resurrection present dans l'inventaire du joueur. */
    @Nullable
    public static TierPotionResurrection meilleurTierDetenu(@Nullable UUID uuidJoueur) {
        CombinedItemContainer container = inventaireDe(uuidJoueur);
        if (container == null) {
            return null;
        }
        TierPotionResurrection meilleur = null;
        for (TierPotionResurrection tier : TierPotionResurrection.values()) {
            if (container.countItemStacks(stack -> tier.itemId().equals(stack.getItemId())) > 0) {
                meilleur = TierPotionResurrection.meilleur(meilleur, tier);
            }
        }
        return meilleur;
    }

    /**
     * Retire une potion du tier indique de l'inventaire du joueur.
     *
     * @return vrai si une potion a bien ete retiree
     */
    public static boolean consommer(@Nullable UUID uuidJoueur, @Nonnull TierPotionResurrection tier) {
        CombinedItemContainer container = inventaireDe(uuidJoueur);
        if (container == null) {
            return false;
        }
        try {
            ItemStack aRetirer = new ItemStack(tier.itemId(), 1);
            if (!container.canRemoveItemStack(aRetirer)) {
                return false;
            }
            container.removeItemStack(aRetirer);
            return true;
        } catch (RuntimeException ignore) {
            return false;
        }
    }

    @Nullable
    private static CombinedItemContainer inventaireDe(@Nullable UUID uuidJoueur) {
        if (uuidJoueur == null) {
            return null;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            PlayerRef playerRef = universe.getPlayer(uuidJoueur);
            if (playerRef == null || !playerRef.isValid()) {
                return null;
            }
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return null;
            }
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return null;
            }
            Player joueur = store.getComponent(ref, Player.getComponentType());
            if (joueur == null || joueur.getInventory() == null) {
                return null;
            }
            return joueur.getInventory().getCombinedHotbarFirst();
        } catch (RuntimeException ignore) {
            return null;
        }
    }
}
