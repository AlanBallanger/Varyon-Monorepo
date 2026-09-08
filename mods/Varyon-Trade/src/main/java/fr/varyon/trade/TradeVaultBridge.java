package fr.varyon.trade;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.HytaleServer;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;


public class TradeVaultBridge
{
    private static Economy economyObj = null;

    public static Economy getEconomyObj()
    {
        if (economyObj == null)
        {
            if (HytaleServer.get().getPluginManager().hasPlugin(
                    PluginIdentifier.fromString("TheNewEconomy:VaultUnlocked"),
                    SemverRange.WILDCARD
            ))
            {
                economyObj = VaultUnlockedServicesManager.get().economyObj();
            }
        }

        return economyObj;
    }
}

