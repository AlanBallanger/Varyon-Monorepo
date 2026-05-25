package com.varyon.integration;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

public final class FactionDepositEcoSync {

    private static final PluginIdentifier ECOTALE_PLUGIN_ID =
        PluginIdentifier.fromString("Varyon:Varyon-Ecotale");
    private static final String BRIDGE_CLASS = "fr.varyon.ecotale.integration.EcotaleFactionDepositIntegration";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FactionDepositEcoSync() {}

    public static void applyEcoFactionTokenForDeposit(@Nonnull PlayerRef playerRef, long depositedFactionPoints) {
        if (depositedFactionPoints <= 0) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        try {
            Class<?> bridgeClass = resolveEcotaleBridgeClass();
            if (bridgeClass == null) {
                return;
            }
            Method m = bridgeClass.getMethod("onVaryonFactionDeposit", UUID.class, long.class);
            m.invoke(null, uuid, depositedFactionPoints);
            LOGGER.at(Level.INFO).log("Faction deposit → Ecotale jetons faction: joueur=" + uuid + " montant=" + depositedFactionPoints);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LOGGER.at(Level.WARNING).log("Faction deposit banque Ecotale: " + cause.getMessage());
        }
    }

    private static Class<?> resolveEcotaleBridgeClass() {
        PluginManager pm = PluginManager.get();
        if (pm != null) {
            PluginBase ecotale = pm.getPlugin(ECOTALE_PLUGIN_ID);
            if (ecotale != null) {
                try {
                    return Class.forName(BRIDGE_CLASS, true, ecotale.getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    LOGGER.at(Level.SEVERE).log("Ecotale chargé mais classe bridge introuvable: " + BRIDGE_CLASS);
                    return null;
                }
            }
            LOGGER.at(Level.WARNING).log("Plugin " + ECOTALE_PLUGIN_ID + " absent ou pas encore chargé — pas de synchro jetons faction.");
            return null;
        }
        LOGGER.at(Level.WARNING).log("PluginManager indisponible — pas de synchro jetons faction.");
        return null;
    }
}
