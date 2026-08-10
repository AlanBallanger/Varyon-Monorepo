package fr.varyon.ecotale.compat;

import javax.annotation.Nullable;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Optional check for Varyon's {@code NoLootComponent} marker: an entity carrying it must not
 * award coins on death (e.g. arena mobs spawned by BossArena, which handle their own rewards).
 *
 * <p>Some worlds do not load Varyon: in that case no marker can exist and every entity is
 * eligible for coins as before. The bridge class is only loaded once the plugin is confirmed
 * present, so a missing Varyon never causes a {@code NoClassDefFoundError}.
 */
public final class VaryonNoLoot {

    private static final PluginIdentifier VARYON_PLUGIN_ID = new PluginIdentifier("Varyon", "Varyon");

    private static volatile Boolean varyonLoaded;
    private static volatile Boolean bridgeAvailable;

    private VaryonNoLoot() {}

    public static boolean isVaryonLoaded() {
        Boolean cached = varyonLoaded;
        if (cached != null) {
            return cached;
        }
        boolean loaded;
        try {
            PluginManager pluginManager = PluginManager.get();
            loaded = pluginManager != null && pluginManager.hasPlugin(VARYON_PLUGIN_ID, SemverRange.WILDCARD);
        } catch (Throwable ignored) {
            loaded = false;
        }
        varyonLoaded = loaded;
        return loaded;
    }

    /** @return true if the entity carries Varyon's NoLootComponent marker (must not drop coins). */
    public static boolean isMarked(@Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> entityRef) {
        if (store == null || entityRef == null || !entityRef.isValid() || !isVaryonLoaded() || !ensureBridge()) {
            return false;
        }
        try {
            return VaryonNoLootBridge.isMarked(store, entityRef);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean ensureBridge() {
        Boolean cached = bridgeAvailable;
        if (cached != null) {
            return cached;
        }
        boolean available;
        try {
            Class.forName("com.varyon.component.NoLootComponent", false, VaryonNoLoot.class.getClassLoader());
            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        bridgeAvailable = available;
        return available;
    }
}
