package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EcotaleEconomyBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    private static final String ECONOMY_BRIDGE = "fr.varyon.ecotale.shared.EconomyBridge";
    private static final String COIN_TYPE_CLASS = "fr.varyon.ecotale.coins.currency.CoinType";
    private static final String ITEM_COIN_LEGACY = "Coin_Copper";

    private static final String BALANCE_SLOT = "#SidebarBalanceItemSlot";
    private static final java.util.regex.Pattern TRAILING_DECIMALS =
            java.util.regex.Pattern.compile("[.,]\\d+$");

    private static Boolean available = null;
    private static Class<?> economyBridgeClass = null;
    private static Method getBalanceMethod = null;

    private EcotaleEconomyBridge() {}

    public static boolean isAvailable() {
        if (available == null) {
            try {
                economyBridgeClass = Class.forName(ECONOMY_BRIDGE);
                getBalanceMethod = economyBridgeClass.getMethod("getBalance", UUID.class);
                available = true;
            } catch (Throwable t) {
                available = false;
            }
        }
        return Boolean.TRUE.equals(available);
    }

    public static void applySidebarBalance(@Nonnull PlayerRef playerRef, @Nonnull UICommandBuilder ui) {
        if (!isAvailable()) {
            LOG.log(Level.FINE, "[EcotaleSidebar] apply skip: EconomyBridge not available");
            clear(ui);
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            LOG.log(Level.WARNING, "[EcotaleSidebar] apply skip: player UUID null");
            clear(ui);
            return;
        }
        try {
            double bal = ((Number) getBalanceMethod.invoke(null, uuid)).doubleValue();
            String text = TRAILING_DECIMALS.matcher(formatAmountOnlyReflect(economyBridgeClass, bal)).replaceAll("");
            ui.set("#SidebarStatBalanceValueMain.TextSpans", Message.raw(text));
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "[EcotaleSidebar] apply failed uuid=" + uuid, t);
            clear(ui);
        }
    }

    @Nonnull
    private static String resolveCoinItemId() {
        String modId = ecotaleCopperItemId();
        if (modId != null && !modId.isBlank()) {
            return modId;
        }
        return ITEM_COIN_LEGACY;
    }

    @Nonnull
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
            LOG.log(Level.FINE, "[EcotaleSidebar] ecotaleCopperItemId reflection failed", t);
        }
        return null;
    }

    private static volatile Method formatAmountOnlyMethod;
    private static volatile Method formatLegacyMethod;
    private static volatile boolean formatMethodResolved;

    @Nonnull
    private static String formatAmountOnlyReflect(@Nonnull Class<?> economyBridge, double bal) throws Exception {
        if (!formatMethodResolved) {
            try {
                formatAmountOnlyMethod = economyBridge.getMethod("formatAmountOnly", double.class);
            } catch (NoSuchMethodException e) {
                formatLegacyMethod = economyBridge.getMethod("format", double.class);
            }
            formatMethodResolved = true;
        }
        if (formatAmountOnlyMethod != null) {
            return (String) formatAmountOnlyMethod.invoke(null, bal);
        }
        String withSymbol = (String) formatLegacyMethod.invoke(null, bal);
        return stripLeadingCurrencyNoise(withSymbol);
    }

    @Nonnull
    private static String stripLeadingCurrencyNoise(@Nonnull String formatted) {
        String s = formatted.trim();
        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (Character.isDigit(ch) || ch == '-' || ch == ',' || ch == '.' || ch == '\u00a0') {
                break;
            }
            i++;
        }
        return i > 0 ? s.substring(i).trim() : s;
    }

    private static void clear(@Nonnull UICommandBuilder ui) {
        ui.set("#SidebarStatBalanceValueMain.TextSpans", Message.raw(""));
    }
}
