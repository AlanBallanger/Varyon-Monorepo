package com.varyon.bossarena.compat;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Varyon-RPG HUD bridge.
 *
 * <p>The BossArena fight HUD occupies the same screen slot as the RPG class/profession HUDs, so the
 * RPG panels are hidden while a player is in a fight and restored when they leave it. No-op when
 * Varyon-RPG is absent.
 */
public final class VaryonRpgHud {

    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final PluginIdentifier RPG_PLUGIN_ID = new PluginIdentifier("Varyon", "Varyon-RPG");

    private static volatile Boolean rpgLoaded;
    private static volatile Boolean bridgeAvailable;
    /** Players whose RPG HUD we hid, so it is only restored for those we actually touched. */
    private static final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    private VaryonRpgHud() {}

    public static boolean isRpgLoaded() {
        Boolean cached = rpgLoaded;
        if (cached != null) {
            return cached;
        }
        PluginManager pluginManager = PluginManager.get();
        boolean loaded = pluginManager != null
                && pluginManager.hasPlugin(RPG_PLUGIN_ID, SemverRange.WILDCARD);
        rpgLoaded = loaded;
        return loaded;
    }

    /** Hides the RPG HUD for a player entering a boss fight. Safe to call repeatedly. */
    public static void hideFor(UUID playerUuid) {
        if (playerUuid == null || !hiddenPlayers.add(playerUuid)) {
            return;
        }
        apply(playerUuid, true);
    }

    /** Restores the RPG HUD for a player leaving a boss fight. Safe to call repeatedly. */
    public static void restoreFor(UUID playerUuid) {
        if (playerUuid == null || !hiddenPlayers.remove(playerUuid)) {
            return;
        }
        apply(playerUuid, false);
    }

    private static void apply(UUID playerUuid, boolean hidden) {
        if (!isRpgLoaded() || !ensureBridge()) {
            return;
        }
        try {
            VaryonRpgHudBridge.setHidden(playerUuid, hidden);
        } catch (NoClassDefFoundError | Exception e) {
            bridgeAvailable = false;
            LOGGER.log(Level.FINE, "Varyon-RPG HUD toggle skipped", e);
        }
    }

    private static boolean ensureBridge() {
        Boolean available = bridgeAvailable;
        if (available != null) {
            return available;
        }
        try {
            Class.forName("com.varyon.bossarena.compat.VaryonRpgHudBridge");
            Class.forName("fr.varyon.vrpg.ui.ClassXpHud");
            bridgeAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            bridgeAvailable = false;
            LOGGER.info("Varyon-RPG not present; fight HUD will not hide RPG panels.");
        }
        return bridgeAvailable;
    }

    /** Test / reload helper. */
    public static void resetCache() {
        rpgLoaded = null;
        bridgeAvailable = null;
        hiddenPlayers.clear();
    }
}
