package com.varyon.portal;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EconomyCoinItemHelper {

    private static final Logger LOG = Logger.getLogger("VaryonEconomyCoin");

    private static final String COIN_TYPE_CLASS = "fr.varyon.ecotale.coins.currency.CoinType";
    private static final String ITEM_COIN_LEGACY = "Coin_Copper";

    private EconomyCoinItemHelper() {}

    @Nonnull
    public static String resolveCoinItemId() {
        String modId = ecotaleCopperItemId();
        if (modId != null && !modId.isBlank()) {
            return modId;
        }
        return ITEM_COIN_LEGACY;
    }

    @Nullable
    private static String ecotaleCopperItemId() {
        try {
            Class<?> ct = Class.forName(COIN_TYPE_CLASS);
            Method getItemId = ct.getMethod("getItemId");
            for (Object e : ct.getEnumConstants()) {
                if (e instanceof Enum<?> en && "COPPER".equals(en.name())) {
                    Object id = getItemId.invoke(e);
                    if (id instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            }
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Ecotale copper item id reflection failed", t);
        }
        return null;
    }

    public static void applyCoinItem(@Nonnull UICommandBuilder ui, @Nonnull String elementIdSansHash) {
        String slot = "#" + elementIdSansHash;
        String itemId = resolveCoinItemId();
        LOG.log(Level.WARNING, "applyCoinItem slot=" + slot + " resolvedItemId=" + itemId);
        ui.setNull(slot + ".Background");
        ui.set(slot + ".ItemId", itemId);
    }
}
